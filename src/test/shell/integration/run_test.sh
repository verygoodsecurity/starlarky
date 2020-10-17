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
# Integration tests for "bazel run"

NO_SIGNAL_OVERRIDE=1

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

add_to_bazelrc "test --notest_loasd"

#### HELPER FUNCTIONS ##################################################

function write_py_files() {
  mkdir -p py || fail "mkdir py failed"

  echo "py_binary(name = 'binary', srcs = ['binary.py'])" > py/BUILD
  echo "py_test(name = 'test', srcs = ['test.py'])" >> py/BUILD

  echo "print('Hello, Python World!')" >py/py.py
  chmod +x py/py.py

  ln -sf py.py py/binary.py
  ln -sf py.py py/test.py
}

function write_cc_source_files() {
  mkdir -p cc
  cat > cc/kitty.cc <<EOF
#include <stdio.h>

int main(void) {
  FILE* f;
  char buf[256];

  f = fopen("cc/hello_kitty.txt", "r");
  if (f == NULL) {
    f = fopen("cc/pussycat.txt", "r");
  }
  if (f == NULL) {
    return 1;
  }

  fgets(buf, 255, f);
  fclose(f);
  printf("%s", buf);
  return 0;
}
EOF

  cat > cc/BUILD <<EOF
cc_binary(name='kitty',
          srcs=['kitty.cc'],
          data=glob(['*.txt']))
EOF
}

#### TESTS #############################################################

function test_run_py_binary() {
  write_py_files
  bazel run //py:binary >& $TEST_log || fail "Expected success"
  expect_log_once 'Hello, Python World'
}

function test_run_py_test() {
  write_py_files
  bazel run //py:test >& $TEST_log || fail "Expected success"
  expect_log_once 'Hello, Python World'
}

function test_runfiles_present_cc_binary() {
  if "$is_windows"; then
    # TODO(laszlocsomor): fix this test on Windows, and enable it.
    return
  fi
  write_cc_source_files

  cat > cc/hello_kitty.txt <<EOF
Hello, kitty.
EOF

  bazel run --nobuild_runfile_links //cc:kitty > output \
    || fail "${PRODUCT_NAME} run failed."
  assert_contains "Hello, kitty" output || fail "Output is not OK."

  bazel run --nobuild_runfile_links //cc:kitty > output2 \
    || fail "Second ${PRODUCT_NAME} run failed."
  assert_contains "Hello, kitty" output2 || fail "Output is not OK."
}

function test_runfiles_updated_correctly_with_nobuild_runfile_links {
  if "$is_windows"; then
    # TODO(laszlocsomor): fix this test on Windows, and enable it.
    return
  fi
  write_cc_source_files

  cat > cc/hello_kitty.txt <<EOF
Hello, kitty.
EOF

  bazel run --nobuild_runfile_links //cc:kitty > output \
    || fail "${PRODUCT_NAME} run failed."
  assert_contains "Hello, kitty" output || fail "Output is not OK."

  rm cc/hello_kitty.txt
  cat > cc/pussycat.txt <<EOF
A pussycat.
EOF

  bazel run --nobuild_runfile_links //cc:kitty > output \
    || fail "${PRODUCT_NAME} run failed."
  assert_contains "pussycat" output || fail "Output is not OK."
}

function test_run_with_no_build_runfile_manifests {
  write_cc_source_files

  bazel run --nobuild_runfile_manifests //cc:kitty >& $TEST_log && fail "should have failed"
  expect_log_once "--nobuild_runfile_manifests is incompatible with the \"run\" command"
}

function test_script_file_generation {
  if "$is_windows"; then
    # TODO(laszlocsomor): fix this test on Windows, and enable it.
    return
  fi
  mkdir -p fubar || fail "mkdir fubar failed"
  echo 'sh_binary(name = "fubar", srcs = ["fubar.sh"])' > fubar/BUILD
  echo 'for t in "$@"; do echo "arg: $t"; done' > fubar/fubar.sh
  chmod +x fubar/fubar.sh

  bazel run --script_path=$(pwd)/fubar/output.sh //fubar \
     || fail "${PRODUCT_NAME} run failed (--script_path)."
  grep "fubar \"\$\@\"" ./fubar/output.sh \
     || fail "${PRODUCT_NAME} run --script_path output was incorrect."

  $(pwd)/fubar/output.sh a "b c" d > ./fubar/fubar.output \
     || fail "Generated script exited with an error."
  grep "arg: b c" ./fubar/fubar.output \
     || fail "Generated script did not handle arguments correctly."
}

function test_consistent_command_line_encoding {
  # todo(aehlig): reenable: https://github.com/bazelbuild/bazel/issues/1775
  return 0

  # TODO(bazel-team): fix bazel to have consistent encoding, also on darwin;
  # see https://github.com/bazelbuild/bazel/issues/1766
  [ "$PLATFORM" != "darwin" ] || warn "test disabled on darwin, see Github issue 1766"
  [ "$PLATFORM" != "darwin" ] || return 0

  # äöüÄÖÜß in UTF8
  local arg=$(echo -e '\xC3\xA4\xC3\xB6\xC3\xBC\xC3\x84\xC3\x96\xC3\x9C\xC3\x9F')

  mkdir -p foo || fail "mkdir foo failed"
  echo 'sh_binary(name = "foo", srcs = ["foo.sh"])' > foo/BUILD
  echo 'sh_test(name = "foo_test", srcs = ["foo.sh"])' >> foo/BUILD
  echo 'test "$1" = "'"$arg"'"' > foo/foo.sh
  chmod +x foo/foo.sh

  bazel run //foo -- "$arg" > output \
    || fail "${PRODUCT_NAME} run failed."

  bazel test //foo:foo_test --test_arg="$arg" \
    || fail "${PRODUCT_NAME} test failed"

  bazel --batch run //foo -- "$arg" > output \
    || fail "${PRODUCT_NAME} run failed (--batch)."
  bazel --batch test //foo:foo_test --test_arg="$arg" \
    || fail "${PRODUCT_NAME} test failed (--batch)"
}

# Tests bazel run with --color=no on a failed build does not produce color.
function test_no_color_on_failed_run() {
  mkdir -p x || fail "mkdir failed"
  echo "cc_binary(name = 'x', srcs = ['x.cc'])" > x/BUILD
  cat > x/x.cc <<EOF
int main(int, char**) {
  // Missing semicolon
  return 0
}
EOF

  bazel run //x:x &>$TEST_log --color=no && fail "expected failure"
  cat $TEST_log
  # Verify that the failure is a build failure.
  if $is_windows; then
    expect_log "missing ';'"
  else
    expect_log "expected ';'"
  fi
  # Hack to make up for grep -P not being supported.
  grep $(echo -e '\x1b') $TEST_log && fail "Expected colorless output"
  true
}


function test_no_ansi_stripping_in_stdout_or_stderr() {
  if $is_windows; then
    # TODO(laszlocsomor): fix this test on Windows, and enable it.
    return
  fi
  mkdir -p x || fail "mkdir failed"
  echo "cc_binary(name = 'x', srcs = ['x.cc'])" > x/BUILD
  cat > x/x.cc <<EOF
#include <unistd.h>
#include <stdio.h>
int main(int, char**) {
  fprintf(stderr, "\nRUN START\n");
  const char out[] = {'<', 0x1B, '[', 'a', ',', 0x1B, '[', '1', '>', 0x0A};
  const char err[] = {'<', 0x1B, '[', 'b', ',', 0x1B, '[', '2', '>', 0x0A};
  write(1, out, 10);
  write(2, err, 10);
  return 0;
}
EOF
  out1color=$(mktemp x/XXXXXX)
  out1nocolor=$(mktemp x/XXXXXX)
  out2=$(mktemp x/XXXXXX)
  err1raw_color=$(mktemp x/XXXXXX)
  err1raw_nocolor=$(mktemp x/XXXXXX)
  err1color=$(mktemp x/XXXXXX)
  err1nocolor=$(mktemp x/XXXXXX)
  err2=$(mktemp x/XXXXXX)
  err2raw=$(mktemp x/XXXXXX)


  # TODO(katre): Figure out why progress rate limiting is required for this on darwin.
  add_to_bazelrc common --show_progress_rate_limit=0.03
  bazel run //x:x --color=yes >$out1color 2>$err1raw_color || fail "expected success"
  bazel run //x:x --color=no >$out1nocolor 2>$err1raw_nocolor || fail "expected success"
  echo >> $err1raw_color
  echo >> $err1raw_nocolor

  ${PRODUCT_NAME}-bin/x/x >$out2 2>$err2raw
  echo >> $err2raw

  # Remove the first newline that is printed so that the output of Bazel can be
  # separated from the output of the test binary
  tail -n +2 $err2raw > $err2

  # Extract the binary's stderr from the raw stderr, which also contains bazel's
  # stderr; if present, remove a trailing ^[[0m (reset terminal to defaults).
  bazel_stderr_line_count_color=$(cat $err1raw_color \
    | grep -n 'RUN START' \
    | awk -F ':' '{print $1}')
  start="$bazel_stderr_line_count_color"
  tail -n +$start $err1raw_color | sed -e 's/.\[0m$//' >$err1color

  cat $err1raw_nocolor
  bazel_stderr_line_count_nocolor=$(cat $err1raw_nocolor \
    | grep -n 'RUN START' \
    | awk -F ':' '{print $1}')
  start="$bazel_stderr_line_count_nocolor"
  tail -n +$start $err1raw_nocolor >$err1nocolor

  diff $out1color $out2 >&$TEST_log || fail "stdout with --color=yes differs"
  diff $out1nocolor $out2 >&$TEST_log || fail "stdout with --color=no differs"
  diff $err1color $err2 >&$TEST_log || fail "stderr with --color=yes differs"
  diff $err1nocolor $err2 >&$TEST_log || fail "stderr with --color=no differs"

  rm -rf x
}

# Test for $(location) in args list of sh_binary
function test_location_in_args() {
  mkdir -p some/testing
  cat > some/testing/BUILD <<'EOF'
genrule(
    name = "generated",
    cmd = "echo 2 > $@",
    outs = ["generated.txt"],
)

sh_binary(
    name = "testing",
    srcs = ["test.sh"],
    data = ["data", ":generated"],
    args = ["$(location :data)", "$(location :generated)"],
)
EOF

  cat > some/testing/test.sh <<'EOF'
#!/usr/bin/env bash
set -ex
echo "Got $@"
i=1
for arg in $@; do
  [[ $((i++)) = $(cat $arg) ]]
done
EOF
  chmod +x some/testing/test.sh

  echo "1" >some/testing/data

  # Arguments are only provided through bazel run, we cannot test it
  # with bazel-bin/some/testing/testing
  bazel run --enable_runfiles=yes //some/testing >$TEST_log || fail "Expected success"
  expect_log "Got .*some/testing/data.*some/testing/generated.txt"
}

function test_run_for_alias() {
  mkdir -p a
  cat > a/BUILD <<EOF
sh_binary(name='a', srcs=['a.sh'])
alias(name='b', actual='a')
EOF

  cat > a/a.sh <<EOF
#!/bin/sh
echo "Dancing with wolves"
exit 0
EOF

  chmod +x a/a.sh
  bazel run //a:b >"$TEST_log" || fail "Expected success"
  expect_log "Dancing with wolves"
}

function test_run_for_custom_executable() {
  mkdir -p a
  if "$is_windows"; then
    local -r IsWindows=True
  else
    local -r IsWindows=False
  fi
  cat > a/x.bzl <<EOF
def _impl(ctx):
    if $IsWindows:
        f = ctx.actions.declare_file("x.bat")
        content = "@if '%1' equ '' (echo Run Forest run) else (echo>%1 Run Forest run)"
    else:
        f = ctx.actions.declare_file("x.sh")
        content = ("#!/bin/sh\n" +
                   "if [ -z \$1 ]; then\\n" +
                   "   echo Run Forest run\\n" +
                   "else\\n" +
                   "   echo Run Forest run > \$1\\n" +
                   "fi")
    ctx.actions.write(f, content, is_executable = True)
    return [DefaultInfo(executable = f)]

my_rule = rule(_impl, executable = True)

def _tool_impl(ctx):
  f = ctx.actions.declare_file("output")
  ctx.actions.run(executable = ctx.executable.tool,
    inputs = [],
    outputs = [f],
    arguments = [f.path]
  )
  return DefaultInfo(files = depset([f]))
my_tool_rule = rule(_tool_impl, attrs = { 'tool' : attr.label(executable = True, cfg = "host") })
EOF

cat > a/BUILD <<EOF
load(":x.bzl", "my_rule", "my_tool_rule")
my_rule(name = "zzz")
my_tool_rule(name = "kkk", tool = ":zzz")
EOF
  bazel run //a:zzz > "$TEST_log" || fail "Expected success"
  expect_log "Run Forest run"
  bazel build //a:kkk > "$TEST_log" || fail "Expected success"
  grep "Run Forest run" bazel-bin/a/output || fail "Output file wrong"
}

# Integration test for https://github.com/bazelbuild/bazel/pull/8322
# "bazel run" forwards input from stdin to the test binary, to support interactive test re-execution
# (when running browser-based tests) and to support debugging tests.
# See also test_a_test_rule_with_input_from_stdin() in //src/test/shell/integration:test_test
function test_run_a_test_and_a_binary_rule_with_input_from_stdin() {
  if "$is_windows"; then
    # TODO(laszlocsomor): fix this test on Windows, and enable it.
    return
  fi
  mkdir -p a
  cat > a/BUILD <<'eof'
sh_test(name = "x", srcs = ["x.sh"])
sh_binary(name = "control", srcs = ["x.sh"])
eof
  cat > a/x.sh <<'eof'
#!/bin/bash
read -n5 FOO
echo "foo=($FOO)"
eof
  chmod +x a/x.sh
  echo helloworld | bazel run //a:control > "$TEST_log" || fail "Expected success"
  expect_log "foo=(hello)"
  echo hallowelt | bazel run //a:x > "$TEST_log" || fail "Expected success"
  expect_log "foo=(hallo)"
}

function test_default_test_tmpdir() {
  local -r pkg="pkg${LINENO}"
  mkdir -p ${pkg}
  echo "echo \${TEST_TMPDIR} > ${TEST_TMPDIR}/tmpdir_value" > ${pkg}/write.sh
  chmod +x ${pkg}/write.sh

  cat > ${pkg}/BUILD <<'EOF'
sh_test(name="a", srcs=["write.sh"])
EOF

  bazel run //${pkg}:a
  local tmpdir_value
  tmpdir_value="$(cat "${TEST_TMPDIR}/tmpdir_value")"
  if ${is_windows}; then
    # Work-around replacing the path with a short DOS path.
    tmpdir_value="$(cygpath -m -l "${tmpdir_value}")"
  fi
  assert_starts_with "${TEST_TMPDIR}/" "${tmpdir_value}"
}

function test_blaze_run_with_custom_test_tmpdir() {
  local -r pkg="pkg${LINENO}"
  mkdir -p ${pkg}
  local tmpdir
  tmpdir="$(mktemp -d)"
  if "${is_windows}"; then
    # Translate from `/*` to a windows path.
    tmpdir="$(cygpath -m "${tmpdir}")"
  fi
  # Check that we execute the intended scenario.
  if [[ "${tmpdir}" == "${TEST_TMPDIR}"* ]]; then
    fail "Temp folder potentially overlaps with the exec root"
  fi
  echo "echo \${TEST_TMPDIR} > ${TEST_TMPDIR}/tmpdir_value" > ${pkg}/write.sh
  chmod +x ${pkg}/write.sh

  cat > ${pkg}/BUILD <<'EOF'
sh_test(name="a", srcs=["write.sh"])
EOF

  bazel run --test_tmpdir="${tmpdir}/test_bazel_run_with_custom_tmpdir" //${pkg}:a
  assert_starts_with "${tmpdir}/test_bazel_run_with_custom_tmpdir" "$(cat "${TEST_TMPDIR}/tmpdir_value")"
}

# Usage: assert_starts_with PREFIX STRING_TO_CHECK.
# Asserts that `$1` is a prefix of `$2`.
function assert_starts_with() {
  if [[ "${2}" != "${1}"* ]]; then
    fail "${2} does not start with ${1}"
  fi
}

run_suite "'${PRODUCT_NAME} run' integration tests"
