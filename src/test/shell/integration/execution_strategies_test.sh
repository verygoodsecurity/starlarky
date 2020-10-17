#!/bin/bash
#
# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# execution_phase_tests.sh: miscellaneous integration tests of Bazel for
# behaviors that affect the execution phase.
#

# --- begin runfiles.bash initialization ---
# Copy-pasted from Bazel's Bash runfiles library (tools/bash/runfiles/runfiles.bash).
set -euo pipefail
if [[ ! -d "${RUNFILES_DIR:-/dev/null}" && ! -f "${RUNFILES_MANIFEST_FILE:-/dev/null}" ]]; then
  if [[ -f "$0.runfiles_manifest" ]]; then
    export RUNFILES_MANIFEST_FILE="$0.runfiles_manifest"
  elif [[ -f "$0.runfiles/MANIFEST" ]]; then
    export RUNFILES_MANIFEST_FILE="$0.runfiles/MANIFEST"
  elif [[ -f "$0.runfiles/bazel_tools/tools/bash/runfiles/runfiles.bash" ]]; then
    export RUNFILES_DIR="$0.runfiles"
  fi
fi
if [[ -f "${RUNFILES_DIR:-/dev/null}/bazel_tools/tools/bash/runfiles/runfiles.bash" ]]; then
  source "${RUNFILES_DIR}/bazel_tools/tools/bash/runfiles/runfiles.bash"
elif [[ -f "${RUNFILES_MANIFEST_FILE:-/dev/null}" ]]; then
  source "$(grep -m1 "^bazel_tools/tools/bash/runfiles/runfiles.bash " \
            "$RUNFILES_MANIFEST_FILE" | cut -d ' ' -f 2-)"
else
  echo >&2 "ERROR: cannot find @bazel_tools//tools/bash/runfiles:runfiles.bash"
  exit 1
fi
# --- end runfiles.bash initialization ---

source "$(rlocation "io_bazel/src/test/shell/integration_test_setup.sh")" \
  || { echo "integration_test_setup.sh not found!" >&2; exit 1; }

case "$(uname -s | tr [:upper:] [:lower:])" in
msys*|mingw*|cygwin*)
  declare -r is_windows=true
  ;;
*)
  declare -r is_windows=false
  ;;
esac

if "$is_windows"; then
  export MSYS_NO_PATHCONV=1
  export MSYS2_ARG_CONV_EXCL="*"
fi

# Tests that you can set the spawn strategy flags to a list of strategies.
function test_multiple_strategies() {
  bazel build --spawn_strategy=worker,local --debug_print_action_contexts &> $TEST_log || fail
  # Can't test for exact strategy names here, because they differ between platforms and products.
  expect_log "DefaultStrategyImplementations: \[.*, .*\]"
}

# Tests that the hardcoded Worker strategies are not introduced with the new
# strategy selection
function test_no_worker_defaults() {
  bazel build --debug_print_action_contexts &> $TEST_log || fail
  # Can't test for exact strategy names here, because they differ between platforms and products.
  expect_not_log "\"Closure\""
  expect_not_log "\"DexBuilder\""
  expect_not_log "\"Javac\""
}

# Tests that Bazel catches an invalid strategy list that has an empty string as an element.
function test_empty_strategy_in_list_is_forbidden() {
  bazel build --spawn_strategy=worker,,local --debug_print_action_contexts &> $TEST_log || true
  expect_log "--spawn_strategy=worker,,local: Empty values are not allowed as part of this comma-separated list of options"
}

# Test that when you set a strategy to the empty string, it gets removed from the map of strategies
# and thus results in the default strategy being used (the one set via --spawn_strategy=).
function test_empty_strategy_means_default() {
  bazel build --spawn_strategy=worker,local --strategy=FooBar=local \
      --debug_print_action_contexts &> $TEST_log || fail
  expect_log "\"FooBar\" = "

  bazel build --spawn_strategy=worker,local --strategy=FooBar=local --strategy=FooBar= \
      --debug_print_action_contexts &> $TEST_log || fail
  expect_not_log "\"FooBar\" = "
}

# Runs a build, waits for the given dir and file to appear, and then kills
# Bazel to check what happens with said files.
function build_and_interrupt() {
  local dir="${1}"; shift
  local file="${1}"; shift

  bazel clean
  bazel build --genrule_strategy=local --nolegacy_spawn_scheduler \
    "${@}" //pkg &> $TEST_log &
  local pid=$!
  while [[ ! -e "${dir}" && ! -e "${file}" ]]; do
    echo "Still waiting for action to create outputs" >>$TEST_log
    sleep 1
  done
  kill "${pid}"
  wait || true
}

function test_local_deletes_plain_outputs_on_interrupt() {
  if "$is_windows"; then
    cat 1>&2 <<EOF
This test is known to be broken on Windows because the kill+wait sequence
in build_and_interrupt doesn't seem to do the right thing.
Skipping...
EOF
    return 0
  fi

  mkdir -p pkg
  cat >pkg/BUILD <<'EOF'
genrule(
  name = "pkg",
  srcs = ["pkg.txt"],
  outs = ["dir", "file"],
  cmd = ("d=$(location :dir) f=$(location :file); "
         + "mkdir -p $$d; touch $$d/subfile $$f; sleep 60"),
)
EOF
  touch pkg/pkg.txt
  local dir="bazel-genfiles/pkg/dir"
  local file="bazel-genfiles/pkg/file"

  build_and_interrupt "${dir}" "${file}" --noexperimental_local_lockfree_output
  [[ -d "${dir}" ]] || fail "Expected directory output to not exist"
  [[ -f "${file}" ]] || fail "Expected regular output to exist"

  build_and_interrupt "${dir}" "${file}" --experimental_local_lockfree_output
  if [[ -d "${dir}" ]]; then
    fail "Expected directory output to not exist"
  fi
  if [[ -f "${file}" ]]; then
     fail "Expected regular output to not exist"
  fi
}

function test_local_deletes_tree_artifacts_on_interrupt() {
  if "$is_windows"; then
    cat 1>&2 <<EOF
This test is known to be broken on Windows because the kill+wait sequence
in build_and_interrupt doesn't seem to do the right thing.
Skipping...
EOF
    return 0
  fi

  mkdir -p pkg
  cat >pkg/rules.bzl <<'EOF'
def _test_tree_artifact_impl(ctx):
  tree = ctx.actions.declare_directory(ctx.attr.name + ".dir")
  cmd = """
mkdir -p {path} && touch {path}/file && sleep 60
""".format(path = tree.path)
  ctx.actions.run_shell(outputs = [tree], command = cmd)
  return DefaultInfo(files = depset([tree]))

test_tree_artifact = rule(
  implementation = _test_tree_artifact_impl,
)
EOF
  cat >pkg/BUILD <<'EOF'
load(":rules.bzl", "test_tree_artifact")
test_tree_artifact(name = "pkg")
EOF
  local dir="bazel-bin/pkg/pkg.dir"
  local file="bazel-bin/pkg/pkg.dir/file"

  build_and_interrupt "${dir}" "${file}" --noexperimental_local_lockfree_output
  [[ -d "${dir}" ]] || fail "Expected tree artifact root to exist"

  build_and_interrupt "${dir}" "${file}" --experimental_local_lockfree_output
  [[ -d "${dir}" ]] || fail "Expected tree artifact root to exist"
  if [[ -f "${file}" ]]; then
     fail "Expected tree artifact contents to not exist"
  fi
}

run_suite "Tests for the execution strategy selection."
