#!/bin/bash
#
# Copyright 2016 The Bazel Authors. All rights reserved.
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
# This test exercises action progress reporting.

# --- begin runfiles.bash initialization ---
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
  declare -r WORKSPACE_STATUS="$(cygpath -m "$(mktemp -d "${TEST_TMPDIR}/wscXXXXXXXX")/wsc.bat")"
  touch "$WORKSPACE_STATUS"
else
  declare -r WORKSPACE_STATUS="$(mktemp -d "${TEST_TMPDIR}/wscXXXXXXXX")/wsc.sh"
  echo "#!$(which true)" > "$WORKSPACE_STATUS"
  chmod +x "$WORKSPACE_STATUS"
fi

add_to_bazelrc "build --workspace_status_command=\"$WORKSPACE_STATUS\" --nostamp"
add_to_bazelrc "build --show_progress_rate_limit=-1"
add_to_bazelrc "build --genrule_strategy=local"

# Match progress messages like [42 / 1,337]
declare -r PROGRESS_RX="\[[0-9, /]\+\]"

# Run a command with a timeout, kill it if too slow or hanging.
#
# To avoid timing out in the case of blocking commands that never return, this
# wrapper will start the command, wait the specified number of seconds, and
# then kill the command and return an error. The caller can then fail the test.
#
# This function does not fail directly because it would have no effect if run in
# a subshell (unless with -e).
function wait_for_command() {
  local -r wait_time="$1"
  shift 1
  ($@) &
  local -r pid="$!"
  for i in $(seq 1 $wait_time); do
    # kill -0 checks to see if the process is still alive. If it is, kill
    # succeeds and we sleep. If it's not, kill fails and we return.
    kill -0 "$pid" >& /dev/null || return 0
    sleep 1
  done
  kill -9 "$pid"
  # A fail() here would not end the script if we are in a subshell, so the
  # caller must check the return value.
  echo "Command $* did not die within $wait_time seconds"
  return 1
}

function test_respects_progress_interval() {
  local -r pkg="${FUNCNAME[0]}"
  mkdir "$pkg" || fail "mkdir $pkg"

  local -r MATCHER="Executing genrule //${pkg}:x; [0-9]s local"

  cat >"${pkg}/BUILD" <<'EOF'
genrule(
    name = "x",
    outs = ["y"],
    cmd = "sleep 5; touch $@",
)
EOF

  bazel build "//${pkg}:x" --progress_report_interval=1 --curses=no --color=no \
    >& "$TEST_log" || fail "Expected success"

  # Do not assert exactly how much time elapsed between "Still waiting" messages
  # or even whether there were more than one at all. Just assert there was at
  # least one message.
  # Logging these messages appears to be a low priority process in bazel and
  # they are not always reported in a timely manner.
  expect_log "$MATCHER"
}

function assert_show_task_finish() {
  local -r show="$1"  # either "show" or "noshow"
  local -r pkg="$2"

  cat >${pkg}/BUILD <<'EOF'
genrule(
    name = "x",
    outs = ["x.out"],
    cmd = "touch $@",
)
EOF

  bazel build "//${pkg}:x" "--${show}_task_finish" \
      --experimental_ui_debug_all_events --color=no \
      --curses=no --nocache_test_results >& "$TEST_log" || fail "bazel test"

  expect_log "START.*: $PROGRESS_RX Executing genrule //${pkg}:x"
  if [ "$show" == "show" ]; then
    expect_log "FINISH.*: $PROGRESS_RX Executing genrule //${pkg}:x"
  else
    # Negative matching should be as permissive as possible.
    expect_not_log "DONE"
  fi
}

function test_show_task_finish() {
  local -r pkg="${FUNCNAME[0]}"
  mkdir "$pkg" || fail "mkdir $pkg"
  assert_show_task_finish "show" "$pkg"
}

function test_noshow_task_finish() {
  local -r pkg="${FUNCNAME[0]}"
  mkdir "$pkg" || fail "mkdir $pkg"
  assert_show_task_finish "noshow" "$pkg"
}

function test_action_counters_dont_account_for_actions_without_progress_msg() {
  local -r pkg="${FUNCNAME[0]}"
  mkdir "$pkg" || fail "mkdir $pkg"

  cat >"${pkg}/BUILD" <<'EOF'
genrule(
    name = "x",
    srcs = ["y"],
    outs = ["x.out"],
    cmd = "echo $< > $@",
)

genrule(
    name = "y",
    srcs = ["z"],
    outs = ["y.out"],
    cmd = "echo $< > $@",
)

genrule(
    name = "z",
    outs = ["z.out"],
    cmd = "echo z > $@",
)
EOF

  # Make the workspace_status_command slow, so it will show up in a "Still
  # waiting" message. Do not modify the workspace status writer action
  # implementation to have a progress message, because it breaks all kinds of
  # things.
  if "$is_windows"; then
    local -r wsc="$(cygpath -m "$(mktemp -d "${TEST_TMPDIR}/wscXXXXXXXX")/wsc.bat")"
    # Wait for an event that never comes, give up after 5 seconds (exits with
    # nonzero), then "cd ." to reset %ERRORLEVEL%.
    echo "%SYSTEMROOT%\\system32\\waitfor.exe DummyEventToWaitFor /T 5 2>NUL & cd ." > "$wsc"
  else
    local -r wsc="$(mktemp -d "${TEST_TMPDIR}/wscXXXXXXXX")/wsc.sh"
    echo -e "#!$(which sh)\nsleep 5" > "$wsc"
    chmod +x "$wsc"
  fi

  bazel build "//${pkg}:x" --experimental_ui_debug_all_events \
      --show_task_finish --color=no --curses=no \
      --workspace_status_command="$wsc" \
      --progress_report_interval=1 \
      >& "$TEST_log" || fail "build failed"

  # We expect a total of 4 actions but only 3 execution messages:
  # - the 3 genrule actions and their messages
  # - the workspace status writer action, only counted but not reported as an
  #   executed action, only as a "Still waiting" one.
  # The counter should reflect no other actions; if it does, it's probably a
  # bug.
  #
  # This used to be buggy. Prior to the bugfix the counter was accounting for 5
  # actions, the extra one being the target completion middleman. Since that's
  # not a real action that the user cares about, we don't want to count it so
  # the numbers look saner.

  # It may happen that Skyframe does not discover (enque) the workspace status
  # writer action immediately, so the counter may initially report 3 total
  # actions instead of 4.
  expect_log "START.*: \[0 / [34]\] Executing genrule //${pkg}:z\s*$"
  expect_log "FINISH.*: \[1 / [34]\] Executing genrule //${pkg}:z\s*$"
  expect_log "START.*: \[1 / [34]\] Executing genrule //${pkg}:y\s*$"
  expect_log "FINISH.*: \[2 / [34]\] Executing genrule //${pkg}:y\s*$"
  expect_log "START.*: \[2 / 4\] Executing genrule //${pkg}:x\s*$"
  expect_log "FINISH.*: \[3 / 4\] Executing genrule //${pkg}:x\s*$"
  expect_log "PROGRESS.*: \[3 / 4\] Still waiting for 1 job to complete:"

  # Open-source Bazel calls this file stable-status.txt, Google internal version
  # calls it build-info.txt.
  expect_log "\b\(stable-status\|build-info\).txt\b.*, [0-9] s"
}

function test_counts_cached_actions_as_completed_ones() {
  local -r pkg="${FUNCNAME[0]}"
  mkdir "$pkg" || fail "mkdir $pkg"

  # DO NOT use cmd="touch $@". The genrules would produce empty files, which are
  # compared by time stamp rather than content (since they are empty), so change
  # pruning won't kick in and this test won't work.
  cat >"${pkg}/BUILD" <<'EOF'
genrule(
    name = "dep1",
    srcs = ["input"],
    outs = ["out1"],
    cmd = "echo foo > $@",
)

[genrule(
    name = "dep%d" % i,
    srcs = [":dep%d" % (i - 1)],
    outs = ["out%d" % i],
    cmd = "echo foo > $@",
) for i in [2, 3, 4, 5, 6, 7]]

genrule(
    name = "x",
    srcs = [
        "input",
        ":dep7",
    ],
    outs = ["outx"],
    cmd = "echo foo > $@",
)
EOF

  # Run a clean then an incremental build.
  #
  # In the first build we should see 9 actions on the right side of the action
  # counter (8 genrules + 1 workspace status writer), 8 DONE messages, the last
  # one of which should be target "x".
  #
  # In the second build, we should again see 9 actions (9 dirtied) in the
  # counter, but only two DONE actions (dep1 and x) due to change pruning.
  # The last action should again be target "x", its completion index 8 or 9 (the
  # last one might be the workspace status writer action).

  echo "input-clean" > "${pkg}/input"
  bazel build "//${pkg}:x" --experimental_ui_debug_all_events \
      --show_task_finish --color=no --curses=no \
      >& "$TEST_log" || fail "build failed"
  expect_log_once "FINISH.*: \[[89] / 9\] Executing genrule //${pkg}:x"
  expect_log_n "FINISH.*: \[[1-9] / 9\] Executing genrule //${pkg}:.*" 8

  echo "input-incremental" > "${pkg}/input"
  bazel build "//${pkg}:x" --experimental_ui_debug_all_events \
      --show_task_finish --color=no --curses=no \
      >& "$TEST_log" || fail "build failed"
  expect_log_once "FINISH.*: \[[89] / 9\] Executing genrule //${pkg}:x"
  expect_log_n "FINISH.*: \[[1-9] / 9\] Executing genrule //${pkg}:.*" 2
}

function test_failed_actions_with_keep_going() {
  local -r pkg="${FUNCNAME[0]}"
  mkdir "$pkg" || fail "mkdir $pkg"

  local -r dep_file="${TEST_TMPDIR}/${pkg}_dep_file"
  cat >"${pkg}/BUILD" <<EOF
genrule(
    name = "dep",
    outs = ["dep.out"],
    cmd = "touch $dep_file; false",
)

genrule(
    name = "top",
    srcs = [":dep.out"],
    outs = ["top.out"],
    cmd = "",
)

genrule(
    name = "longrun",
    outs = ["longrun.out"],
    cmd = "while [ ! -f \"$dep_file\" ]; do " +
          "sleep 1; done; sleep 5",
)
EOF

  # The whole test setup relies on bazel running the actions for
  # :dep and :longrun in parallel. However, bazel normally analyzes
  # the environment and bases the action scheduling on this; so we
  # need to tell bazel to (maybe even contrafactually) believe that
  # there are enough local resources for two genrules to run in parallel.
  # Give enough head room so that the test won't break again if we tweak
  # our assumptions about local resource usage.
  bazel build --experimental_ui_debug_all_events -j 2 \
      --local_ram_resources=2048000 --local_cpu_resources=32 \
      -k -s "//${pkg}:"{top,longrun} --progress_report_interval=1 \
      >& "$TEST_log" && fail "build succeeded"
  expect_log "\[3 / 4\] Still waiting for 1 job to complete:"
  expect_log "^ *Executing genrule //${pkg}:longrun"
}

function test_seemingly_too_many_total_actions_due_to_change_pruning() {
  local -r pkg="${FUNCNAME[0]}"
  mkdir "$pkg" || fail "mkdir $pkg"

  # DO NOT use cmd="touch $@". The genrules would produce empty files, which are
  # compared by time stamp rather than content (since they are empty), so change
  # pruning won't kick in and this test won't work.
  cat >"${pkg}/BUILD" <<'EOF'
genrule(
    name = "dep1",
    srcs = ["input"],
    outs = ["out1"],
    cmd = "echo foo > $@",
)

[genrule(
    name = "dep%d" % i,
    srcs = [":dep%d" % (i - 1)],
    outs = ["out%d" % i],
    cmd = "echo foo > $@",
) for i in [2, 3, 4, 5, 6, 7]]

genrule(
    name = "x",
    srcs = [":dep7"],
    outs = ["outx"],
    cmd = "echo foo > $@",
)
EOF

  # Run a clean then an incremental build.
  #
  # In the first build we should see 9 actions on the right side of the action
  # counter (8 genrules + 1 workspace status writer action), 8 DONE messages,
  # the last one of which should be target "x".
  #
  # In the second build, we should again see 9 actions (9 dirtied) in the
  # counter, but only two DONE actions (dep1 and x) due to change pruning.
  # The last action should again be target "x", its completion index 8 or 9 (the
  # last one might be the workspace status writer action).
  echo "input-clean" > "${pkg}/input"
  bazel build "//${pkg}:x" --experimental_ui_debug_all_events \
      --show_task_finish --color=no --curses=no \
      >& "$TEST_log" || fail "build failed"
  expect_log_once "FINISH.*: \[[89] / 9\] Executing genrule //${pkg}:x"
  expect_log_n "FINISH.*: \[[1-9] / 9\] Executing genrule //${pkg}:.*" 8

  echo "input-incremental" > "${pkg}/input"
  bazel build "//${pkg}:x" --experimental_ui_debug_all_events \
      --show_task_finish --color=no --curses=no \
      >& "$TEST_log" || fail "build failed"
  expect_log_once "FINISH.*: \[[12] / 9\] Executing genrule //${pkg}:dep1"
  expect_log_once "FINISH.*:.* Executing genrule .*"
}

function test_counts_exclusive_tests_in_total_work() {
  local -r pkg="${FUNCNAME[0]}"
  mkdir "$pkg" || fail "mkdir $pkg"

  cat >"${pkg}/BUILD" <<'EOF'
[sh_test(
    name = "t%d" % i,
    srcs = ["test.sh"],
    tags = ["exclusive", "local"],
) for i in [1, 2, 3]]
EOF
  echo "#!$(which true)" > "${pkg}/test.sh"
  chmod +x "${pkg}/test.sh"

  bazel test --nocache_test_results --show_task_finish \
      "//${pkg}:all" --color=no --curses=no >& "$TEST_log" \
      || fail "build failed"

  # Extract the numbers from the last [123 / 4,567] progress message.
  local -r numbers="$(cat "$TEST_log" | grep "Testing //${pkg}:x" \
    | sed 's,^.*\[\([0-9, /]*\)\].*$,\1,;s|,||g;s|/| |' | sort -n | tail -n 1)"
  local -r completed_last="$(echo "$numbers" | awk '{print $2}')"
  local -r total="$(echo "$numbers" | awk '{print $2}')"

  assert_equals "$completed_last" "$total"
}

run_suite "Tests for execution phase progress reporting"
