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
# Tests remote execution and caching.

set -euo pipefail

# --- begin runfiles.bash initialization ---
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
source "$(rlocation "io_bazel/src/test/shell/bazel/remote/remote_utils.sh")" \
  || { echo "remote_utils.sh not found!" >&2; exit 1; }

function set_up() {
  start_worker \
        --incompatible_remote_symlinks
}

function tear_down() {
  bazel clean >& $TEST_log
  stop_worker
}

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
  declare -r EXE_EXT=".exe"
else
  declare -r EXE_EXT=""
fi

function test_remote_grpc_cache_with_protocol() {
  # Test that if 'grpc' is provided as a scheme for --remote_cache flag, remote cache works.
  mkdir -p a
  cat > a/BUILD <<EOF
genrule(
  name = 'foo',
  outs = ["foo.txt"],
  cmd = "echo \"foo bar\" > \$@",
)
EOF

  bazel build \
      --remote_cache=grpc://localhost:${worker_port} \
      //a:foo \
      || fail "Failed to build //a:foo with remote cache"
}

function test_remote_grpc_via_unix_socket_proxy() {
  case "$PLATFORM" in
  darwin|freebsd|linux|openbsd)
    ;;
  *)
    return 0
    ;;
  esac

  # Test that remote execution can be routed via a UNIX domain socket if
  # supported by the platform.
  mkdir -p a
  cat > a/BUILD <<EOF
genrule(
  name = 'foo',
  outs = ["foo.txt"],
  cmd = "echo \"foo bar\" > \$@",
)
EOF

  # Note: not using $TEST_TMPDIR because many OSes, notably macOS, have
  # small maximum length limits for UNIX domain sockets.
  socket_dir=$(mktemp -d -t "remote_executor.XXXXXXXX")
  PROXY="$(rlocation io_bazel/src/test/shell/bazel/remote/uds_proxy.py)"
  python "${PROXY}" "${socket_dir}/executor-socket" "localhost:${worker_port}" &
  proxy_pid=$!

  bazel build \
      --remote_executor=grpc://noexist.invalid \
      --remote_proxy="unix:${socket_dir}/executor-socket" \
      //a:foo \
      || fail "Failed to build //a:foo with remote cache"

  kill ${proxy_pid}
  rm "${socket_dir}/executor-socket"
  rmdir "${socket_dir}"
}

function test_remote_grpc_via_unix_socket_direct() {
  case "$PLATFORM" in
  darwin|freebsd|linux|openbsd)
    ;;
  *)
    return 0
    ;;
  esac

  # Test that remote execution can be routed via a UNIX domain socket if
  # supported by the platform.
  mkdir -p a
  cat > a/BUILD <<EOF
genrule(
  name = 'foo',
  outs = ["foo.txt"],
  cmd = "echo \"foo bar\" > \$@",
)
EOF

  # Note: not using $TEST_TMPDIR because many OSes, notably macOS, have
  # small maximum length limits for UNIX domain sockets.
  socket_dir=$(mktemp -d -t "remote_executor.XXXXXXXX")
  PROXY="$(rlocation io_bazel/src/test/shell/bazel/remote/uds_proxy.py)"
  python "${PROXY}" "${socket_dir}/executor-socket" "localhost:${worker_port}" &
  proxy_pid=$!

  bazel build \
      --remote_executor="unix:${socket_dir}/executor-socket" \
      //a:foo \
      || fail "Failed to build //a:foo with remote cache"

  kill ${proxy_pid}
  rm "${socket_dir}/executor-socket"
  rmdir "${socket_dir}"
}

function test_cc_binary() {
  if [[ "$PLATFORM" == "darwin" ]]; then
    # TODO(b/37355380): This test is disabled due to RemoteWorker not supporting
    # setting SDKROOT and DEVELOPER_DIR appropriately, as is required of
    # action executors in order to select the appropriate Xcode toolchain.
    return 0
  fi

  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
cc_binary(
name = 'test',
srcs = [ 'test.cc' ],
)
EOF
  cat > a/test.cc <<EOF
#include <iostream>
int main() { std::cout << "Hello world!" << std::endl; return 0; }
EOF
  bazel build //a:test >& $TEST_log \
    || fail "Failed to build //a:test without remote execution"
  cp -f bazel-bin/a/test ${TEST_TMPDIR}/test_expected

  bazel clean >& $TEST_log
  bazel build \
      --remote_executor=grpc://localhost:${worker_port} \
      //a:test >& $TEST_log \
      || fail "Failed to build //a:test with remote execution"
  expect_log "6 processes: 4 internal, 2 remote"
  diff bazel-bin/a/test ${TEST_TMPDIR}/test_expected \
      || fail "Remote execution generated different result"
}

function test_cc_tree() {
  if [[ "$PLATFORM" == "darwin" ]]; then
    # TODO(b/37355380): This test is disabled due to RemoteWorker not supporting
    # setting SDKROOT and DEVELOPER_DIR appropriately, as is required of
    # action executors in order to select the appropriate Xcode toolchain.
    return 0
  fi

  mkdir -p a
  cat > a/BUILD <<EOF
load(":tree.bzl", "mytree")
mytree(name = "tree")
cc_library(name = "tree_cc", srcs = [":tree"])
EOF
  cat > a/tree.bzl <<EOF
def _tree_impl(ctx):
    tree = ctx.actions.declare_directory("file.cc")
    ctx.actions.run_shell(outputs = [tree],
                          command = "mkdir -p %s && touch %s/one.cc" % (tree.path, tree.path))
    return [DefaultInfo(files = depset([tree]))]

mytree = rule(implementation = _tree_impl)
EOF
  bazel build \
      --remote_executor=grpc://localhost:${worker_port} \
      --remote_download_minimal \
      //a:tree_cc >& "$TEST_log" \
      || fail "Failed to build //a:tree_cc with minimal downloads"
}

function test_cc_test() {
  if [[ "$PLATFORM" == "darwin" ]]; then
    # TODO(b/37355380): This test is disabled due to RemoteWorker not supporting
    # setting SDKROOT and DEVELOPER_DIR appropriately, as is required of
    # action executors in order to select the appropriate Xcode toolchain.
    return 0
  fi

  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
cc_test(
name = 'test',
srcs = [ 'test.cc' ],
)
EOF
  cat > a/test.cc <<EOF
#include <iostream>
int main() { std::cout << "Hello test!" << std::endl; return 0; }
EOF
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --test_output=errors \
      --noexperimental_split_xml_generation \
      //a:test >& $TEST_log \
      || fail "Failed to run //a:test with remote execution"
}

function test_cc_test_split_xml() {
  if [[ "$PLATFORM" == "darwin" ]]; then
    # TODO(b/37355380): This test is disabled due to RemoteWorker not supporting
    # setting SDKROOT and DEVELOPER_DIR appropriately, as is required of
    # action executors in order to select the appropriate Xcode toolchain.
    return 0
  fi

  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
cc_test(
name = 'test',
srcs = [ 'test.cc' ],
)
EOF
  cat > a/test.cc <<EOF
#include <iostream>
int main() { std::cout << "Hello test!" << std::endl; return 0; }
EOF
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --test_output=errors \
      --experimental_split_xml_generation \
      //a:test >& $TEST_log \
      || fail "Failed to run //a:test with remote execution"
}

function test_cc_binary_grpc_cache() {
  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
cc_binary(
name = 'test',
srcs = [ 'test.cc' ],
)
EOF
  cat > a/test.cc <<EOF
#include <iostream>
int main() { std::cout << "Hello world!" << std::endl; return 0; }
EOF
  bazel build //a:test >& $TEST_log \
    || fail "Failed to build //a:test without remote cache"
  cp -f bazel-bin/a/test ${TEST_TMPDIR}/test_expected

  bazel clean >& $TEST_log
  bazel build \
      --remote_cache=grpc://localhost:${worker_port} \
      //a:test >& $TEST_log \
      || fail "Failed to build //a:test with remote gRPC cache service"
  diff bazel-bin/a/test ${TEST_TMPDIR}/test_expected \
      || fail "Remote cache generated different result"
}

function test_cc_binary_grpc_cache_statsline() {
  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
cc_binary(
name = 'test',
srcs = [ 'test.cc' ],
)
EOF
  cat > a/test.cc <<EOF
#include <iostream>
int main() { std::cout << "Hello world!" << std::endl; return 0; }
EOF
  bazel build \
      --remote_cache=grpc://localhost:${worker_port} \
      //a:test >& $TEST_log \
      || fail "Failed to build //a:test with remote gRPC cache service"
  bazel clean >& $TEST_log
  bazel build \
      --remote_cache=grpc://localhost:${worker_port} \
      //a:test 2>&1 | tee $TEST_log | grep "remote cache hit" \
      || fail "Output does not contain remote cache hits"
}

function test_failing_cc_test() {
  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
cc_test(
name = 'test',
srcs = [ 'test.cc' ],
)
EOF
  cat > a/test.cc <<EOF
#include <iostream>
int main() { std::cout << "Fail me!" << std::endl; return 1; }
EOF
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --test_output=errors \
      //a:test >& $TEST_log \
      && fail "Expected test failure" || true
  # TODO(ulfjack): Check that the test failure gets reported correctly.
}

function test_local_fallback_works_with_local_strategy() {
  mkdir -p gen1
  cat > gen1/BUILD <<'EOF'
genrule(
name = "gen1",
srcs = [],
outs = ["out1"],
cmd = "touch \"$@\"",
tags = ["no-remote"],
)
EOF

  bazel build \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --remote_local_fallback_strategy=local \
      --build_event_text_file=gen1.log \
      //gen1 >& $TEST_log \
      && fail "Expected failure" || true
}

function test_local_fallback_with_local_strategy_lists() {
  mkdir -p gen1
  cat > gen1/BUILD <<'EOF'
genrule(
name = "gen1",
srcs = [],
outs = ["out1"],
cmd = "touch \"$@\"",
tags = ["no-remote"],
)
EOF

  bazel build \
      --spawn_strategy=remote,local \
      --remote_executor=grpc://localhost:${worker_port} \
      --build_event_text_file=gen1.log \
      //gen1 >& $TEST_log \
      || fail "Expected success"

  mv gen1.log $TEST_log
  expect_log "2 processes: 1 internal, 1 local"
}

function test_local_fallback_with_sandbox_strategy_lists() {
  mkdir -p gen1
  cat > gen1/BUILD <<'EOF'
genrule(
name = "gen1",
srcs = [],
outs = ["out1"],
cmd = "touch \"$@\"",
tags = ["no-remote"],
)
EOF

  bazel build \
      --spawn_strategy=remote,sandboxed,local \
      --remote_executor=grpc://localhost:${worker_port} \
      --build_event_text_file=gen1.log \
      //gen1 >& $TEST_log \
      || fail "Expected success"

  mv gen1.log $TEST_log
  expect_log "2 processes: 1 internal, 1 .*-sandbox"
}

function test_local_fallback_to_sandbox_by_default() {
  mkdir -p gen1
  cat > gen1/BUILD <<'EOF'
genrule(
name = "gen1",
srcs = [],
outs = ["out1"],
cmd = "touch \"$@\"",
tags = ["no-remote"],
)
EOF

  bazel build \
      --remote_executor=grpc://localhost:${worker_port} \
      --build_event_text_file=gen1.log \
      //gen1 >& $TEST_log \
      || fail "Expected success"

  mv gen1.log $TEST_log
  expect_log "2 processes: 1 internal, 1 .*-sandbox"
}

function test_local_fallback_works_with_sandboxed_strategy() {
  mkdir -p gen2
  cat > gen2/BUILD <<'EOF'
genrule(
name = "gen2",
srcs = [],
outs = ["out2"],
cmd = "touch \"$@\"",
tags = ["no-remote"],
)
EOF

  bazel build \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --remote_local_fallback_strategy=sandboxed \
      --build_event_text_file=gen2.log \
      //gen2 >& $TEST_log \
      && fail "Expected failure" || true
}

function is_file_uploaded() {
  h=$(shasum -a256 < $1)
  if [ -e "$cas_path/${h:0:64}" ]; then return 0; else return 1; fi
}

function test_failed_test_outputs_not_uploaded() {
  # Test that outputs of a failed test/action are not uploaded to the remote
  # cache. This is a regression test for https://github.com/bazelbuild/bazel/issues/7232
  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
cc_test(
  name = 'test',
  srcs = [ 'test.cc' ],
)
EOF
  cat > a/test.cc <<EOF
#include <iostream>
int main() { std::cout << "Fail me!" << std::endl; return 1; }
EOF
  bazel test \
      --remote_cache=grpc://localhost:${worker_port} \
      --test_output=errors \
      //a:test >& $TEST_log \
      && fail "Expected test failure" || true
   ($(is_file_uploaded bazel-testlogs/a/test/test.log) \
     && fail "Expected test log to not be uploaded to remote execution") || true
   ($(is_file_uploaded bazel-testlogs/a/test/test.xml) \
     && fail "Expected test xml to not be uploaded to remote execution") || true
}

# Tests that the remote worker can return a 200MB blob that requires chunking.
# Blob has to be that large in order to exceed the grpc default max message size.
function test_genrule_large_output_chunking() {
  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
genrule(
name = "large_output",
srcs = ["small_blob.txt"],
outs = ["large_blob.txt"],
cmd = "cp \$(location small_blob.txt) tmp.txt; " +
"(for i in {1..22} ; do cat tmp.txt >> \$@; cp \$@ tmp.txt; done)",
)
EOF
  cat > a/small_blob.txt <<EOF
0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
EOF
  bazel build //a:large_output >& $TEST_log \
    || fail "Failed to build //a:large_output without remote execution"
  cp -f bazel-genfiles/a/large_blob.txt ${TEST_TMPDIR}/large_blob_expected.txt

  bazel clean >& $TEST_log
  bazel build \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      //a:large_output >& $TEST_log \
      || fail "Failed to build //a:large_output with remote execution"
  diff bazel-genfiles/a/large_blob.txt ${TEST_TMPDIR}/large_blob_expected.txt \
      || fail "Remote execution generated different result"
}

function test_py_test() {
  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
py_test(
name = 'test',
srcs = [ 'test.py' ],
)
EOF
  cat > a/test.py <<'EOF'
import sys
if __name__ == "__main__":
    sys.exit(0)
EOF
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --test_output=errors \
      //a:test >& $TEST_log \
      || fail "Failed to run //a:test with remote execution"
}

function test_py_test_with_xml_output() {
  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
py_test(
name = 'test',
srcs = [ 'test.py' ],
)
EOF
  cat > a/test.py <<'EOF'
import sys
import os
if __name__ == "__main__":
    f = open(os.environ['XML_OUTPUT_FILE'], "w")
    f.write('''
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="test" tests="1" failures="1" errors="1">
    <testcase name="first" status="run">
      <failure>That did not work!</failure>
    </testcase>
  </testsuite>
</testsuites>
''')
    sys.exit(0)
EOF
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --test_output=errors \
      //a:test >& $TEST_log \
      || fail "Failed to run //a:test with remote execution"
  xml=bazel-testlogs/a/test/test.xml
  [ -e $xml ] || fail "Expected to find XML output"
  cat $xml > $TEST_log
  expect_log 'That did not work!'
}

function test_failing_py_test_with_xml_output() {
  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
py_test(
name = 'test',
srcs = [ 'test.py' ],
)
EOF
  cat > a/test.py <<'EOF'
import sys
import os
if __name__ == "__main__":
    f = open(os.environ['XML_OUTPUT_FILE'], "w")
    f.write('''
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="test" tests="1" failures="1" errors="1">
    <testcase name="first" status="run">
      <failure>That did not work!</failure>
    </testcase>
  </testsuite>
</testsuites>
''')
    sys.exit(1)
EOF
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --test_output=errors \
      //a:test >& $TEST_log \
      && fail "Expected test failure" || true
  xml=bazel-testlogs/a/test/test.xml
  [ -e $xml ] || fail "Expected to find XML output"
  cat $xml > $TEST_log
  expect_log 'That did not work!'
}

function test_noinput_action() {
  mkdir -p a
  cat > a/rule.bzl <<'EOF'
def _impl(ctx):
  output = ctx.outputs.out
  ctx.actions.run_shell(
      outputs=[output],
      command="echo 'Hello World' > %s" % (output.path))

empty = rule(
    implementation=_impl,
    outputs={"out": "%{name}.txt"},
)
EOF
  cat > a/BUILD <<'EOF'
load("//a:rule.bzl", "empty")
package(default_visibility = ["//visibility:public"])
empty(name = 'test')
EOF
  bazel build \
      --remote_cache=grpc://localhost:${worker_port} \
      --test_output=errors \
      //a:test >& $TEST_log \
      || fail "Failed to run //a:test with remote execution"
}

function test_timeout() {
  mkdir -p a
  cat > a/BUILD <<'EOF'
sh_test(
  name = "sleep",
  timeout = "short",
  srcs = ["sleep.sh"],
)
EOF

  cat > a/sleep.sh <<'EOF'
#!/bin/bash
for i in {1..3}
do
    echo "Sleeping $i..."
    sleep 1
done
EOF
  chmod +x a/sleep.sh
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --test_output=errors \
      --test_timeout=1,1,1,1 \
      //a:sleep >& $TEST_log \
      && fail "Test failure (timeout) expected" || true
  expect_log "TIMEOUT"
  expect_log "Sleeping 1..."
  # The current implementation of the remote worker does not terminate actions
  # when they time out, therefore we cannot verify that:
  # expect_not_log "Sleeping 3..."
}

function test_passed_env_user() {
  mkdir -p a
  cat > a/BUILD <<'EOF'
sh_test(
  name = "user_test",
  timeout = "short",
  srcs = ["user_test.sh"],
)
EOF

  cat > a/user_test.sh <<'EOF'
#!/bin/sh
echo "user=$USER"
EOF
  chmod +x a/user_test.sh
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --test_output=all \
      --test_env=USER=boo \
      //a:user_test >& $TEST_log \
      || fail "Failed to run //a:user_test with remote execution"
  expect_log "user=boo"

  # Rely on the test-setup script to set USER value to whoami.
  export USER=
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://localhost:${worker_port} \
      --test_output=all \
      //a:user_test >& $TEST_log \
      || fail "Failed to run //a:user_test with remote execution"
  expect_log "user=$(whoami)"
}

function test_exitcode() {
  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "foo",
  srcs = [],
  outs = ["foo.txt"],
  cmd = "echo \"hello world\" > \"$@\"",
)
EOF

  (set +e
    bazel build \
      --genrule_strategy=remote \
      --remote_executor=bazel-test-does-not-exist \
      //a:foo >& $TEST_log
    [ $? -eq 34 ]) || fail "Test failed due to wrong exit code"
}

# Bazel should display non-test errors to the user, instead of hiding them behind test failures.
# For example, if the network connection to the remote executor fails it shouldn't be displayed as
# a test error.
function test_display_non_testerrors() {
  mkdir -p a
  cat > a/BUILD <<'EOF'
sh_test(
  name = "test",
  timeout = "short",
  srcs = ["test.sh"],
)
EOF
  cat > a/test.sh <<'EOF'
#!/bin/sh
#This will never run, because the remote side is not reachable.
EOF
  chmod +x a/test.sh
  bazel test \
      --spawn_strategy=remote \
      --remote_executor=grpc://bazel.does.not.exist:1234 \
      --remote_retries=0 \
      --test_output=all \
      --test_env=USER=boo \
      //a:test >& $TEST_log \
      && fail "Test failure expected" || true
  expect_not_log "test.log"
  expect_log "Failed to query remote execution capabilities"
}

function set_symlinks_in_directory_testfixtures() {
    cat > BUILD <<'EOF'
genrule(
    name = 'make-links',
    outs = ['dir', 'r', 'a', 'rd', 'ad'],
    cmd = ('mkdir $(location dir) && ' +
        'cd $(location dir) && ' +
        'echo hello > foo && ' + # Regular file.
        'ln -s foo r && ' +  # Relative symlink, will be passed as symlink.
        'ln -s $$PWD/foo a && ' +  # Absolute symlink, will be copied.
        'mkdir bar && ' + # Regular directory.
        'echo bla > bar/baz && ' +
        'ln -s bar rd && ' +  # Relative symlink, will be passed as symlink.
        'ln -s $$PWD/bar ad && ' + # Absolute symlink, will be copied.
        'cd .. && ' +
        'ln -s dir/foo r && ' +  # Relative symlink, will be passed as symlink.
        'ln -s $$PWD/dir/foo a && ' +  # Absolute symlink, will be copied.
        'ln -s dir rd && ' +  # Relative symlink, will be passed as symlink.
        'ln -s $$PWD/dir ad' # Absolute symlink, will be copied.
    ),
)
EOF
    cat > "${TEST_TMPDIR}/expected_links" <<'EOF'
./ad/r
./ad/rd
./dir/r
./dir/rd
./r
./rd
EOF
}

function test_symlinks_in_directory() {
    set_symlinks_in_directory_testfixtures
    bazel build \
          --incompatible_remote_symlinks \
          --remote_executor=grpc://localhost:${worker_port} \
          --spawn_strategy=remote \
          //:make-links &> $TEST_log \
          || fail "Failed to build //:make-links with remote execution"
    expect_log "1 remote"
    find -L bazel-genfiles -type f -exec cat {} \; | sort | uniq -c &> $TEST_log
    expect_log "9 bla"
    expect_log "11 hello"
    CUR=$PWD && cd bazel-genfiles && \
      find . -type l | sort > "${TEST_TMPDIR}/links" && cd $CUR
    diff "${TEST_TMPDIR}/links" "${TEST_TMPDIR}/expected_links" \
      || fail "Remote execution created different symbolic links"
}

function test_symlinks_in_directory_cache_only() {
    # This test is the same as test_symlinks_in_directory, except it works
    # locally and uses the remote cache to query results.
    set_symlinks_in_directory_testfixtures
    bazel build \
          --incompatible_remote_symlinks \
          --remote_cache=grpc://localhost:${worker_port} \
          --spawn_strategy=local \
          //:make-links &> $TEST_log \
          || fail "Failed to build //:make-links with remote cache service"
    expect_log "1 local"
    bazel clean # Get rid of local results, rely on remote cache.
    bazel build \
          --incompatible_remote_symlinks \
          --remote_cache=grpc://localhost:${worker_port} \
          --spawn_strategy=local \
          //:make-links &> $TEST_log \
          || fail "Failed to build //:make-links with remote cache service"
    expect_log "1 remote cache hit"
    # Check that the results downloaded from remote cache are the same as local.
    find -L bazel-genfiles -type f -exec cat {} \; | sort | uniq -c &> $TEST_log
    expect_log "9 bla"
    expect_log "11 hello"
    CUR=$PWD && cd bazel-genfiles && \
      find . -type l | sort > "${TEST_TMPDIR}/links" && cd $CUR
    diff "${TEST_TMPDIR}/links" "${TEST_TMPDIR}/expected_links" \
      || fail "Cached result created different symbolic links"
}

function test_treeartifact_in_runfiles() {
     mkdir -p a
    cat > a/BUILD <<'EOF'
load(":output_directory.bzl", "gen_output_dir", "gen_output_dir_test")

gen_output_dir(
    name = "starlark_output_dir",
    outdir = "dir",
)

gen_output_dir_test(
    name = "starlark_output_dir_test",
    dir = ":starlark_output_dir",
)
EOF
     cat > a/output_directory.bzl <<'EOF'
def _gen_output_dir_impl(ctx):
  output_dir = ctx.actions.declare_directory(ctx.attr.outdir)
  ctx.actions.run_shell(
      outputs = [output_dir],
      inputs = [],
      command = """
        mkdir -p $1/sub; \
        echo "foo" > $1/foo; \
        echo "bar" > $1/sub/bar
      """,
      arguments = [output_dir.path],
  )
  return [
      DefaultInfo(files=depset(direct=[output_dir]),
                  runfiles = ctx.runfiles(files = [output_dir]))
  ]
gen_output_dir = rule(
    implementation = _gen_output_dir_impl,
    attrs = {
        "outdir": attr.string(mandatory = True),
    },
)
def _gen_output_dir_test_impl(ctx):
    test = ctx.actions.declare_file(ctx.label.name)
    ctx.actions.write(test, "echo hello world")
    myrunfiles = ctx.runfiles(files=ctx.attr.dir.default_runfiles.files.to_list())
    return [
        DefaultInfo(
            executable = test,
            runfiles = myrunfiles,
        ),
    ]
gen_output_dir_test = rule(
    implementation = _gen_output_dir_test_impl,
    test = True,
    attrs = {
        "dir":  attr.label(mandatory = True),
    },
)
EOF
     # Also test this directory inputs with sandboxing. Ideally we would add such
     # a test into the sandboxing module.
     bazel test \
           --spawn_strategy=sandboxed \
           //a:starlark_output_dir_test \
           || fail "Failed to run //a:starlark_output_dir_test with sandboxing"

     bazel test \
           --spawn_strategy=remote \
           --remote_executor=grpc://localhost:${worker_port} \
           //a:starlark_output_dir_test \
           || fail "Failed to run //a:starlark_output_dir_test with remote execution"
}

function test_downloads_minimal() {
  # Test that genrule outputs are not downloaded when using
  # --remote_download_minimal
  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "foo",
  srcs = [],
  outs = ["foo.txt"],
  cmd = "echo \"foo\" > \"$@\"",
)

genrule(
  name = "foobar",
  srcs = [":foo"],
  outs = ["foobar.txt"],
  cmd = "cat $(location :foo) > \"$@\" && echo \"bar\" >> \"$@\"",
)
EOF

  bazel build \
    --genrule_strategy=remote \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    //a:foobar || fail "Failed to build //a:foobar"

  (! [[ -f bazel-bin/a/foo.txt ]] && ! [[ -f bazel-bin/a/foobar.txt ]]) \
  || fail "Expected no files to have been downloaded"
}

function test_downloads_minimal_failure() {
  # Test that outputs of failing actions are downloaded when using
  # --remote_download_minimal
  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "fail",
  srcs = [],
  outs = ["fail.txt"],
  cmd = "echo \"foo\" > \"$@\" && exit 1",
)
EOF

  bazel build \
    --spawn_strategy=remote \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    //a:fail && fail "Expected test failure" || true

  [[ -f bazel-bin/a/fail.txt ]] \
  || fail "Expected fail.txt of failing target //a:fail to be downloaded"
}

function test_downloads_minimal_prefetch() {
  # Test that when using --remote_download_minimal a remote-only output that's
  # an input to a local action is downloaded lazily before executing the local action.
  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "remote",
  srcs = [],
  outs = ["remote.txt"],
  cmd = "echo -n \"remote\" > \"$@\"",
)

genrule(
  name = "local",
  srcs = [":remote"],
  outs = ["local.txt"],
  cmd = "cat $(location :remote) > \"$@\" && echo -n \"local\" >> \"$@\"",
  tags = ["no-remote"],
)
EOF

  bazel build \
    --genrule_strategy=remote \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    //a:remote || fail "Failed to build //a:remote"

  (! [[ -f bazel-bin/a/remote.txt ]]) \
  || fail "Expected bazel-bin/a/remote.txt to have not been downloaded"

  bazel build \
    --genrule_strategy=remote,local \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    //a:local || fail "Failed to build //a:local"

  localtxt="bazel-bin/a/local.txt"
  [[ $(< ${localtxt}) == "remotelocal" ]] \
  || fail "Unexpected contents in " ${localtxt} ": " $(< ${localtxt})

  (! [[ -f bazel-bin/a/remote.txt ]]) \
  || fail "Expected bazel-bin/a/remote.txt to have been deleted again"
}

function test_download_outputs_invalidation() {
  # Test that when changing values of --remote_download_minimal all actions are
  # invalidated.
  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "remote",
  srcs = [],
  outs = ["remote.txt"],
  cmd = "echo -n \"remote\" > \"$@\"",
)
EOF

  bazel build \
    --genrule_strategy=remote \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    //a:remote >& $TEST_log || fail "Failed to build //a:remote"

  expect_log "2 processes: 1 internal, 1 remote"

  bazel build \
    --genrule_strategy=remote \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_outputs=all \
    //a:remote >& $TEST_log || fail "Failed to build //a:remote"

  # Changing --remote_download_outputs to "all" should invalidate SkyFrames in-memory
  # caching and make it re-run the action.
  expect_log "2 processes: 1 remote cache hit, 1 internal"
}

function test_downloads_minimal_native_prefetch() {
  # Test that when using --remote_download_outputs=minimal a remotely stored output
  # that's an input to a native action (ctx.actions.expand_template) is staged lazily for action
  # execution.
  mkdir -p a
  cat > a/substitute_username.bzl <<'EOF'
def _substitute_username_impl(ctx):
    ctx.actions.expand_template(
        template = ctx.file.template,
        output = ctx.outputs.out,
        substitutions = {
            "{USERNAME}": ctx.attr.username,
        },
    )

substitute_username = rule(
    implementation = _substitute_username_impl,
    attrs = {
        "username": attr.string(mandatory = True),
        "template": attr.label(
            allow_single_file = True,
            mandatory = True,
        ),
    },
    outputs = {"out": "%{name}.txt"},
)
EOF

  cat > a/BUILD <<'EOF'
load(":substitute_username.bzl", "substitute_username")
genrule(
    name = "generate-template",
    cmd = "echo -n \"Hello {USERNAME}!\" > $@",
    outs = ["template.txt"],
    srcs = [],
)

substitute_username(
    name = "substitute-buchgr",
    username = "buchgr",
    template = ":generate-template",
)
EOF

  bazel build \
    --genrule_strategy=remote \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    //a:substitute-buchgr >& $TEST_log || fail "Failed to build //a:substitute-buchgr"

  # The genrule //a:generate-template should run remotely and //a:substitute-buchgr
  # should be a native action running locally.
  expect_log "3 processes: 2 internal, 1 remote"

  outtxt="bazel-bin/a/substitute-buchgr.txt"
  [[ $(< ${outtxt}) == "Hello buchgr!" ]] \
  || fail "Unexpected contents in "${outtxt}":" $(< ${outtxt})

  (! [[ -f bazel-bin/a/template.txt ]]) \
  || fail "Expected bazel-bin/a/template.txt to have been deleted again"
}

function test_downloads_toplevel() {
  # Test that when using --remote_download_outputs=toplevel only the output of the
  # toplevel target is being downloaded.
  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "foo",
  srcs = [],
  outs = ["foo.txt"],
  cmd = "echo \"foo\" > \"$@\"",
)

genrule(
  name = "foobar",
  srcs = [":foo"],
  outs = ["foobar.txt"],
  cmd = "cat $(location :foo) > \"$@\" && echo \"bar\" >> \"$@\"",
)
EOF

  bazel build \
    --genrule_strategy=remote \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_toplevel \
    //a:foobar || fail "Failed to build //a:foobar"

  (! [[ -f bazel-bin/a/foo.txt ]]) \
  || fail "Expected intermediate output bazel-bin/a/foo.txt to not be downloaded"

  [[ -f bazel-bin/a/foobar.txt ]] \
  || fail "Expected toplevel output bazel-bin/a/foobar.txt to be downloaded"


  # Delete the file to test that the action is re-run
  rm -f bazel-bin/a/foobar.txt

  bazel build \
    --genrule_strategy=remote \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_toplevel \
    //a:foobar >& $TEST_log || fail "Failed to build //a:foobar"

  expect_log "2 processes: 1 remote cache hit, 1 internal"

  [[ -f bazel-bin/a/foobar.txt ]] \
  || fail "Expected toplevel output bazel-bin/a/foobar.txt to be re-downloaded"
}

function test_downloads_toplevel_runfiles() {
  # Test that --remote_download_toplevel fetches only the top level binaries
  # and generated runfiles.
  if [[ "$PLATFORM" == "darwin" ]]; then
    # TODO(b/37355380): This test is disabled due to RemoteWorker not supporting
    # setting SDKROOT and DEVELOPER_DIR appropriately, as is required of
    # action executors in order to select the appropriate Xcode toolchain.
    return 0
  fi

  mkdir -p a

  cat > a/create_bar.tmpl <<'EOF'
#!/bin/sh
echo "bar runfiles"
exit 0
EOF

  cat > a/foo.cc <<'EOF'
#include <iostream>
int main() { std::cout << "foo" << std::endl; return 0; }
EOF

  cat > a/BUILD <<'EOF'
genrule(
  name = "bar",
  srcs = ["create_bar.tmpl"],
  outs = ["create_bar.sh"],
  cmd = "cat $(location create_bar.tmpl) > \"$@\"",
)

cc_binary(
  name = "foo",
  srcs = ["foo.cc"],
  data = [":bar"],
)
EOF

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_toplevel \
    //a:foo || fail "Failed to build //a:foobar"

  [[ -f bazel-bin/a/foo${EXE_EXT} ]] \
  || fail "Expected toplevel output bazel-bin/a/foo${EXE_EXT} to be downloaded"

  [[ -f bazel-bin/a/create_bar.sh ]] \
  || fail "Expected runfile bazel-bin/a/create_bar.sh to be downloaded"
}

# Test that --remote_download_toplevel fetches inputs to symlink actions. In
# particular, cc_binary links against a symlinked imported .so file, and only
# the symlink is in the runfiles.
function test_downloads_toplevel_symlinks() {
  if [[ "$PLATFORM" == "darwin" ]]; then
    # TODO(b/37355380): This test is disabled due to RemoteWorker not supporting
    # setting SDKROOT and DEVELOPER_DIR appropriately, as is required of
    # action executors in order to select the appropriate Xcode toolchain.
    return 0
  fi

  mkdir -p a

  cat > a/bar.cc <<'EOF'
int f() {
  return 42;
}
EOF

  cat > a/foo.cc <<'EOF'
extern int f();
int main() { return f() == 42 ? 0 : 1; }
EOF

  cat > a/BUILD <<'EOF'
cc_binary(
  name = "foo",
  srcs = ["foo.cc"],
  deps = [":libbar_lib"],
)

cc_import(
  name = "libbar_lib",
  shared_library = ":libbar.so",
)

cc_binary(
  name = "libbar.so",
  srcs = ["bar.cc"],
  linkshared = 1,
  linkstatic = 1,
)
EOF

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_toplevel \
    //a:foo || fail "Failed to build //a:foobar"

  ./bazel-bin/a/foo${EXE_EXT} || fail "bazel-bin/a/foo${EXE_EXT} failed to run"
}

function test_symlink_outputs_not_allowed_with_minimial() {
  mkdir -p a
  cat > a/input.txt <<'EOF'
Input file
EOF
  cat > a/BUILD <<'EOF'
genrule(
  name = "foo",
  srcs = ["input.txt"],
  outs = ["output.txt", "output_symlink"],
  cmd = "cp $< $(location :output.txt) && ln -s output.txt $(location output_symlink)",
)
EOF

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    //a:foo >& $TEST_log && fail "Expected failure to build //a:foo"
  expect_log "Symlinks in action outputs are not yet supported"
}

# Regression test that --remote_download_toplevel does not crash when the
# top-level output is a tree artifact.
function test_downloads_toplevel_tree_artifact() {
  if [[ "$PLATFORM" == "darwin" ]]; then
    # TODO(b/37355380): This test is disabled due to RemoteWorker not supporting
    # setting SDKROOT and DEVELOPER_DIR appropriately, as is required of
    # action executors in order to select the appropriate Xcode toolchain.
    return 0
  fi

  mkdir -p a

  # We need the top-level output to be a tree artifact generated by a template
  # action. This is one way to do that: generate a tree artifact of C++ source
  # files, and then compile them with a cc_library / cc_binary rule.
  #
  # The default top-level output of a cc_binary is the final binary, which is
  # not what we want. Instead, we use --output_groups=compilation_outputs to
  # fetch the .o files as the top-level outputs.

  cat > a/gentree.bzl <<'EOF'
def _gentree(ctx):
    out = ctx.actions.declare_directory("dir.cc")
    ctx.actions.run_shell(
        outputs = [out],
        command = "mkdir -p %s && echo 'int main(int c, char** v){return 1;}' > %s/foo.cc" %
            (out.path, out.path),
    )
    return DefaultInfo(files = depset([out]))

gentree = rule(implementation = _gentree)
EOF

  cat > a/BUILD <<'EOF'
load(":gentree.bzl", "gentree")
gentree(name = "tree")
cc_binary(name = "main", srcs = [":tree"])
EOF

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_toplevel \
    --output_groups=compilation_outputs \
    //a:main || fail "Failed to build //a:main"
}

function test_downloads_toplevel_src_runfiles() {
  # Test that using --remote_download_toplevel with a non-generated (source)
  # runfile dependency works.
  mkdir -p a
  cat > a/create_foo.sh <<'EOF'
#!/bin/sh
echo "foo runfiles"
exit 0
EOF
  chmod +x a/create_foo.sh
  cat > a/BUILD <<'EOF'
genrule(
  name = "foo",
  srcs = [],
  tools = ["create_foo.sh"],
  outs = ["foo.txt"],
  cmd = "./$(location create_foo.sh) > \"$@\"",
)
EOF

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_toplevel \
    //a:foo || fail "Failed to build //a:foobar"

  [[ -f bazel-bin/a/foo.txt ]] \
  || fail "Expected toplevel output bazel-bin/a/foo.txt to be downloaded"
}

function test_download_toplevel_test_rule() {
  # Test that when using --remote_download_toplevel with bazel test only
  # the test.log and test.xml file are downloaded but not the test binary.
  # However when building a test then the test binary should be downloaded.

  if [[ "$PLATFORM" == "darwin" ]]; then
    # TODO(b/37355380): This test is disabled due to RemoteWorker not supporting
    # setting SDKROOT and DEVELOPER_DIR appropriately, as is required of
    # action executors in order to select the appropriate Xcode toolchain.
    return 0
  fi

  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
cc_test(
  name = 'test',
  srcs = [ 'test.cc' ],
)
EOF
  cat > a/test.cc <<EOF
#include <iostream>
int main() { std::cout << "Hello test!" << std::endl; return 0; }
EOF

  # When invoking bazel test only test.log and test.xml should be downloaded.
  bazel test \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_toplevel \
    //a:test >& $TEST_log || fail "Failed to test //a:test with remote execution"

  (! [[ -f bazel-bin/a/test ]]) \
  || fail "Expected test binary bazel-bin/a/test to not be downloaded"

  [[ -f bazel-testlogs/a/test/test.log ]] \
  || fail "Expected toplevel output bazel-testlogs/a/test/test.log to be downloaded"

  [[ -f bazel-testlogs/a/test/test.xml ]] \
  || fail "Expected toplevel output bazel-testlogs/a/test/test.log to be downloaded"

  bazel clean

  # When invoking bazel build the test binary should be downloaded.
  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_toplevel \
    //a:test >& $TEST_log || fail "Failed to build //a:test with remote execution"

  ([[ -f bazel-bin/a/test ]]) \
  || fail "Expected test binary bazel-bin/a/test to be downloaded"
}

function test_downloads_minimal_bep() {
  # Test that when using --remote_download_minimal all URI's in the BEP
  # are rewritten as bytestream://..
  mkdir -p a
  cat > a/success.sh <<'EOF'
#!/bin/sh
exit 0
EOF
  chmod 755 a/success.sh
  cat > a/BUILD <<'EOF'
sh_test(
  name = "success_test",
  srcs = ["success.sh"],
)

genrule(
  name = "foo",
  srcs = [],
  outs = ["foo.txt"],
  cmd = "echo \"foo\" > \"$@\"",
)
EOF

  bazel test \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    --build_event_text_file=$TEST_log \
    //a:foo //a:success_test || fail "Failed to test //a:foo //a:success_test"

  expect_not_log 'uri:.*file://'
  expect_log "uri:.*bytestream://localhost"
}

# This test is derivative of test_bep_output_groups in
# build_event_stream_test.sh, which verifies that successful output groups'
# artifacts appear in BEP when a top-level target fails to build.
function test_downloads_minimal_bep_partially_failed_target() {
  # Test that when using --remote_download_minimal all URI's in the BEP
  # are rewritten as bytestream://.. *even when* a target fails to be built and
  # some output groups within that target are successfully built.
  mkdir -p outputgroups
  cat > outputgroups/rules.bzl <<EOF
def _my_rule_impl(ctx):
    group_kwargs = {}
    for name, exit in (("foo", 0), ("bar", 0)):
        outfile = ctx.actions.declare_file(ctx.label.name + "-" + name + ".out")
        ctx.actions.run_shell(
            outputs = [outfile],
            command = "printf %s > %s && exit %d" % (name, outfile.path, exit),
        )
        group_kwargs[name + "_outputs"] = depset([outfile])
    for name, exit, suffix in (
      ("foo", 1, ".fail.out"), ("bar", 0, ".ok.out"), ("bar", 0, ".ok.out2")):
        outfile = ctx.actions.declare_file(ctx.label.name + "-" + name + suffix)
        ctx.actions.run_shell(
            outputs = [outfile],
            command = "printf %s > %s && exit %d" % (name, outfile.path, exit),
        )
        group_kwargs[name + "_outputs"] = depset(
            [outfile], transitive=[group_kwargs[name + "_outputs"]])
    return [OutputGroupInfo(**group_kwargs)]

my_rule = rule(implementation = _my_rule_impl, attrs = {
    "outs": attr.output_list(),
})
EOF
  cat > outputgroups/BUILD <<EOF
load("//outputgroups:rules.bzl", "my_rule")
my_rule(name = "my_lib", outs=[])
EOF

  # In outputgroups/rules.bzl, the `my_rule` definition defines four output
  # groups with different (successful/failed) action counts:
  #    1. foo_outputs (1 successful/1 failed)
  #    2. bar_outputs (1/0)
  #
  # We request both output groups and expect artifacts produced by bar_outputs
  # to appear in BEP with bytestream URIs.
  bazel build //outputgroups:my_lib \
    --remote_executor=grpc://localhost:${worker_port} \
    --keep_going \
    --remote_download_minimal \
    --build_event_text_file=$TEST_log \
    --output_groups=foo_outputs,bar_outputs \
    && fail "expected failure" || true

  expect_not_log 'uri:.*file://'
  expect_log "uri:.*bytestream://localhost"
}

# This test is derivative of test_failing_aspect_bep_output_groups in
# build_event_stream_test.sh, which verifies that successful output groups'
# artifacts appear in BEP when a top-level aspect fails to build.
function test_downloads_minimal_bep_partially_failed_aspect() {
  # Test that when using --remote_download_minimal all URI's in the BEP
  # are rewritten as bytestream://.. *even when* an aspect fails to be built and
  # some output groups within that aspect are successfully built.
  touch BUILD
  cat > semifailingaspect.bzl <<'EOF'
def _semifailing_aspect_impl(target, ctx):
    if not ctx.rule.attr.outs:
        return struct(output_groups = {})
    bad_outputs = list()
    good_outputs = list()
    for out in ctx.rule.attr.outs:
        if out.name[0] == "f":
            aspect_out = ctx.actions.declare_file(out.name + ".aspect.bad")
            bad_outputs.append(aspect_out)
            cmd = "false"
        else:
            aspect_out = ctx.actions.declare_file(out.name + ".aspect.good")
            good_outputs.append(aspect_out)
            cmd = "echo %s > %s" % (out.name, aspect_out.path)
        ctx.actions.run_shell(
            inputs = [],
            outputs = [aspect_out],
            command = cmd,
        )
    return [OutputGroupInfo(**{
        "bad-aspect-out": depset(bad_outputs),
        "good-aspect-out": depset(good_outputs),
    })]

semifailing_aspect = aspect(implementation = _semifailing_aspect_impl)
EOF
  mkdir -p semifailingpkg/
  cat > semifailingpkg/BUILD <<'EOF'
genrule(
  name = "semifail",
  outs = ["out1.txt", "out2.txt", "failingout1.txt"],
  cmd = "for f in $(OUTS); do echo foo > $(RULEDIR)/$$f; done"
)
EOF

  # In semifailingaspect.bzl, the `semifailing_aspect` definition defines two
  # output groups: good-aspect-out and bad-aspect-out. We expect the artifacts
  # produced by good-aspect-out to have bytestream URIs in BEP.
  bazel build //semifailingpkg:semifail \
    --remote_executor=grpc://localhost:${worker_port} \
    --keep_going \
    --remote_download_minimal \
    --build_event_text_file=$TEST_log \
    --aspects=semifailingaspect.bzl%semifailing_aspect \
    --output_groups=good-aspect-out,bad-aspect-out \
    && fail "expected failure" || true

  expect_not_log 'uri:.*file://'
  expect_log "uri:.*bytestream://localhost"
}

function test_remote_exec_properties() {
  # Test that setting remote exec properties works.
  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "foo",
  srcs = [],
  outs = ["foo.txt"],
  cmd = "echo \"foo\" > \"$@\"",
)
EOF

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_default_exec_properties=OSFamily=linux \
    //a:foo || fail "Failed to build //a:foo"

  bazel clean

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_default_exec_properties=OSFamily=windows \
    //a:foo >& $TEST_log || fail "Failed to build //a:foo"

  expect_not_log "remote cache hit"
}

function test_downloads_minimal_stable_status() {
  # Regression test for #8385

  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "foo",
  srcs = [],
  outs = ["foo.txt"],
  cmd = "echo \"foo\" > \"$@\"",
)
EOF

cat > status.sh << 'EOF'
#!/bin/sh
echo "STABLE_FOO 1"
EOF
chmod +x status.sh

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    --workspace_status_command=status.sh \
    //a:foo || "Failed to build //a:foo"

cat > status.sh << 'EOF'
#!/bin/sh
echo "STABLE_FOO 2"
EOF

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_download_minimal \
    --workspace_status_command=status.sh \
    //a:foo || "Failed to build //a:foo"
}

function test_testxml_download_toplevel() {
  # Test that a test action generating its own test.xml file works with
  # --remote_download_toplevel.
  mkdir -p a

  cat > a/test.sh <<'EOF'
#!/bin/sh

cat > "$XML_OUTPUT_FILE" <<EOF2
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="test" tests="1" failures="0" errors="0">
    <testcase name="test_case" status="run">
      <system-out>test_case succeeded.</system-out>
    </testcase>
  </testsuite>
</testsuites>
EOF2
EOF

  chmod +x a/test.sh

  cat > a/BUILD <<EOF
sh_test(
  name = 'test',
  srcs = [ 'test.sh' ],
)
EOF

  bazel test \
      --remote_executor=grpc://localhost:${worker_port} \
      --remote_download_toplevel \
      //a:test \
      || fail "Failed to run //a:test with remote execution"

  TESTXML="bazel-testlogs/a/test/test.xml"
  assert_contains "test_case succeeded" "$TESTXML"
}

# Regression test that Bazel does not crash if remote execution is disabled,
# but --remote_download_toplevel is enabled.
function test_download_toplevel_no_remote_execution() {
  bazel build --remote_download_toplevel \
      || fail "Failed to run bazel build --remote_download_toplevel"
}

function test_tag_no_remote_cache() {
  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "foo",
  srcs = [],
  outs = ["foo.txt"],
  cmd = "echo \"foo\" > \"$@\"",
  tags = ["no-remote-cache"]
)
EOF

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    //a:foo >& $TEST_log || "Failed to build //a:foo"

  expect_log "1 remote"

  bazel clean

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    //a:foo || "Failed to build //a:foo"

  expect_log "1 remote"
  expect_not_log "remote cache hit"
}

function test_tag_no_remote_exec() {
  mkdir -p a
  cat > a/BUILD <<'EOF'
genrule(
  name = "foo",
  srcs = [],
  outs = ["foo.txt"],
  cmd = "echo \"foo\" > \"$@\"",
  tags = ["no-remote-exec"]
)
EOF

  bazel build \
    --spawn_strategy=remote,local \
    --remote_executor=grpc://localhost:${worker_port} \
    //a:foo >& $TEST_log || "Failed to build //a:foo"

  expect_log "1 local"
  expect_not_log "1 remote"

  bazel clean

  bazel build \
    --spawn_strategy=remote,local \
    --remote_executor=grpc://localhost:${worker_port} \
    //a:foo >& $TEST_log || "Failed to build //a:foo"

  expect_log "1 remote cache hit"
  expect_not_log "1 local"
}

function test_nobuild_runfile_links() {
  mkdir data && echo "hello" > data/hello && echo "world" > data/world
    cat > WORKSPACE <<EOF
workspace(name = "foo")
EOF

  cat > test.sh <<'EOF'
#!/bin/bash
set -e
[[ -f ${RUNFILES_DIR}/foo/data/hello ]]
[[ -f ${RUNFILES_DIR}/foo/data/world ]]
exit 0
EOF
  chmod 755 test.sh
  cat > BUILD <<'EOF'
filegroup(
  name = "runfiles",
  srcs = ["data/hello", "data/world"],
)

sh_test(
  name = "test",
  srcs = ["test.sh"],
  data = [":runfiles"],
)
EOF

  bazel test \
    --nobuild_runfile_links \
    --remote_executor=grpc://localhost:${worker_port} \
    //:test || fail "Testing //:test failed"

  [[ ! -f bazel-bin/test.runfiles/foo/data/hello ]] || fail "expected no runfile data/hello"
  [[ ! -f bazel-bin/test.runfiles/foo/data/world ]] || fail "expected no runfile data/world"
  [[ ! -f bazel-bin/test.runfiles/MANIFEST ]] || fail "expected output manifest to exist"
}

function test_platform_default_properties_invalidation() {
  # Test that when changing values of --remote_default_platform_properties all actions are
  # invalidated.
mkdir -p test
  cat > test/BUILD << 'EOF'
genrule(
    name = "test",
    srcs = [],
    outs = ["output.txt"],
    cmd = "echo \"foo\" > \"$@\""
)
EOF

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_default_exec_properties="build=1234" \
    //test:test >& $TEST_log || fail "Failed to build //a:remote"

  expect_log "2 processes: 1 internal, 1 remote"

  bazel build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_default_exec_properties="build=88888" \
    //test:test >& $TEST_log || fail "Failed to build //a:remote"

  # Changing --remote_default_platform_properties value should invalidate SkyFrames in-memory
  # caching and make it re-run the action.
  expect_log "2 processes: 1 internal, 1 remote"

  bazel  build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_default_exec_properties="build=88888" \
    //test:test >& $TEST_log || fail "Failed to build //a:remote"

  # The same value of --remote_default_platform_properties should NOT invalidate SkyFrames in-memory cache
  #  and make the action should not be re-run.
  expect_log "1 process: 1 internal"

  bazel shutdown

  bazel  build \
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_default_exec_properties="build=88888" \
    //test:test >& $TEST_log || fail "Failed to build //a:remote"

  # The same value of --remote_default_platform_properties should NOT invalidate SkyFrames od-disk cache
  #  and the action should not be re-run.
  expect_log "1 process: 1 internal"

  bazel build\
    --remote_executor=grpc://localhost:${worker_port} \
    --remote_default_exec_properties="build=88888" \
    --remote_default_platform_properties='properties:{name:"build" value:"1234"}' \
    //test:test >& $TEST_log && fail "Should fail" || true

  # Build should fail with a proper error message if both
  # --remote_default_platform_properties and --remote_default_exec_properties
  # are provided via command line
  expect_log "Setting both --remote_default_platform_properties and --remote_default_exec_properties is not allowed"
}

function test_genrule_combined_disk_grpc_cache() {
  # Test for the combined disk and grpc cache.
  # Built items should be pushed to both the disk and grpc cache.
  # If --noremote_upload_local_results flag is set,
  # built items should only be pushed to the disk cache.
  # If --noremote_accept_cached flag is set,
  # built items should only be checked from the disk cache.
  # If an item is missing on disk cache, but present on grpc cache,
  # then bazel should copy it from grpc cache to disk cache on fetch.

  local cache="${TEST_TMPDIR}/cache"
  local disk_flags="--disk_cache=$cache"
  local grpc_flags="--remote_cache=grpc://localhost:${worker_port}"

  mkdir -p a
  cat > a/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
genrule(
name = 'test',
cmd = 'echo "Hello world" > \$@',
outs = [ 'test.txt' ],
)
EOF
  rm -rf $cache
  mkdir $cache

  # Build and push to disk cache but not grpc cache
  bazel build $disk_flags $grpc_flags --incompatible_remote_results_ignore_disk=true --noremote_upload_local_results //a:test \
    || fail "Failed to build //a:test with combined disk grpc cache"
  cp -f bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected

  # Fetch from disk cache
  bazel clean
  bazel build $disk_flags //a:test --incompatible_remote_results_ignore_disk=true --noremote_upload_local_results &> $TEST_log \
    || fail "Failed to fetch //a:test from disk cache"
  expect_log "1 remote cache hit" "Fetch from disk cache failed"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "Disk cache generated different result"

  # No cache result from grpc cache, rebuild target
  bazel clean
  bazel build $grpc_flags //a:test --incompatible_remote_results_ignore_disk=true --noremote_upload_local_results &> $TEST_log \
    || fail "Failed to build //a:test"
  expect_not_log "1 remote cache hit" "Should not get cache hit from grpc cache"
  expect_log "1 .*-sandbox" "Rebuild target failed"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "Rebuilt target generated different result"

  rm -rf $cache
  mkdir $cache

  # No cache result from grpc cache, rebuild target, and upload result to grpc cache
  bazel clean
  bazel build $grpc_flags //a:test --incompatible_remote_results_ignore_disk=true --noremote_accept_cached &> $TEST_log \
    || fail "Failed to build //a:test"
  expect_not_log "1 remote cache hit" "Should not get cache hit from grpc cache"
  expect_log "1 .*-sandbox" "Rebuild target failed"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "Rebuilt target generated different result"

  # No cache result from grpc cache, rebuild target, and upload result to disk cache
  bazel clean
  bazel build $disk_flags $grpc_flags //a:test --incompatible_remote_results_ignore_disk=true --noremote_accept_cached &> $TEST_log \
    || fail "Failed to build //a:test"
  expect_not_log "1 remote cache hit" "Should not get cache hit from grpc cache"
  expect_log "1 .*-sandbox" "Rebuild target failed"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "Rebuilt target generated different result"

  # Fetch from disk cache
  bazel clean
  bazel build $disk_flags $grpc_flags //a:test --incompatible_remote_results_ignore_disk=true --noremote_accept_cached &> $TEST_log \
    || fail "Failed to build //a:test"
  expect_log "1 remote cache hit" "Fetch from disk cache failed"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "Disk cache generated different result"

  rm -rf $cache
  mkdir $cache

  # Build and push to disk cache and grpc cache
  bazel clean
  bazel build $disk_flags $grpc_flags //a:test \
    || fail "Failed to build //a:test with combined disk grpc cache"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "Built target generated different result"

  # Fetch from disk cache
  bazel clean
  bazel build $disk_flags //a:test &> $TEST_log \
    || fail "Failed to fetch //a:test from disk cache"
  expect_log "1 remote cache hit" "Fetch from disk cache failed"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "Disk cache generated different result"

  # Fetch from grpc cache
  bazel clean
  bazel build $grpc_flags //a:test &> $TEST_log \
    || fail "Failed to fetch //a:test from grpc cache"
  expect_log "1 remote cache hit" "Fetch from grpc cache failed"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "HTTP cache generated different result"

  rm -rf $cache
  mkdir $cache

  # Copy from grpc cache to disk cache
  bazel clean
  bazel build $disk_flags $grpc_flags //a:test &> $TEST_log \
    || fail "Failed to copy //a:test from grpc cache to disk cache"
  expect_log "1 remote cache hit" "Copy from grpc cache to disk cache failed"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "HTTP cache generated different result"

  # Fetch from disk cache
  bazel clean
  bazel build $disk_flags //a:test &> $TEST_log \
    || fail "Failed to fetch //a:test from disk cache"
  expect_log "1 remote cache hit" "Fetch from disk cache after copy from grpc cache failed"
  diff bazel-genfiles/a/test.txt ${TEST_TMPDIR}/test_expected \
    || fail "Disk cache generated different result"

  rm -rf $cache
}

function test_repo_remote_exec() {
  # Test that repository_ctx.execute can execute a command remotely.

  touch BUILD

  cat > test.bzl <<'EOF'
def _impl(ctx):
  res = ctx.execute(["/bin/bash", "-c", "echo -n $BAZEL_REMOTE_PLATFORM"])
  if res.return_code != 0:
    fail("Return code 0 expected, but was " + res.return_code)

  entries = res.stdout.split(",")
  if len(entries) != 2:
    fail("Two platform kv pairs expected. Got:" + str(entries))
  if entries[0] != "ISA=x86-64":
    fail("'ISA' expected in remote platform'")
  if entries[1] != "OSFamily=Linux":
    fail("'OSFamily' expected in remote platform'")

  ctx.file("BUILD")

foo_configure = repository_rule(
  implementation = _impl,
  remotable = True,
)
EOF

  cat > WORKSPACE <<'EOF'
load("//:test.bzl", "foo_configure")

foo_configure(
  name = "default_foo",
  exec_properties = {
    "OSFamily" : "Linux",
    "ISA" : "x86-64",
  }
)
EOF

  bazel fetch \
    --remote_executor=grpc://localhost:${worker_port} \
    --experimental_repo_remote_exec \
    @default_foo//:all
}

function test_repo_remote_exec_path_argument() {
  # Test that repository_ctx.execute fails with a descriptive error message
  # if a path argument is provided. The upload of files as part of command
  # execution is not yet supported.

  touch BUILD

  echo "hello world" > input.txt

  cat > test.bzl <<'EOF'
def _impl(ctx):
  ctx.execute(["cat", ctx.path("input.txt")])
  ctx.file("BUILD")

foo_configure = repository_rule(
  implementation = _impl,
  remotable = True,
)
EOF

  cat > WORKSPACE <<'EOF'
load("//:test.bzl", "foo_configure")

foo_configure(
  name = "default_foo",
)
EOF

  bazel fetch \
    --remote_executor=grpc://localhost:${worker_port} \
    --experimental_repo_remote_exec \
    @default_foo//:all  >& $TEST_log && fail "Should fail" || true

  expect_log "Argument 1 of execute is neither a label nor a string"
}

function test_repo_remote_exec_timeout() {
  # Test that a remote job is killed if it exceeds the timeout.

  touch BUILD

  cat > test.bzl <<'EOF'
def _impl(ctx):
  ctx.execute(["/bin/bash","-c",
    "for i in {1..3}; do echo \"Sleeping $i...\" && sleep 1; done"], timeout=1)
  ctx.file("BUILD")

foo_configure = repository_rule(
  implementation = _impl,
  remotable = True,
)
EOF

  cat > WORKSPACE <<'EOF'
load("//:test.bzl", "foo_configure")

foo_configure(
  name = "default_foo",
)
EOF

  bazel fetch \
    --remote_executor=grpc://localhost:${worker_port} \
    --experimental_repo_remote_exec \
    @default_foo//:all >& $TEST_log && fail "Should fail" || true

  expect_log "exceeded deadline"
}

function test_repo_remote_exec_file_upload() {
  # Test that repository_ctx.execute accepts arguments of type label and can upload files and
  # execute them remotely.

cat > BUILD <<'EOF'
  exports_files(["cmd.sh", "hello.txt"])
EOF

  cat > cmd.sh <<'EOF'
#!/bin/sh
cat $1
EOF

  chmod +x cmd.sh

  echo "hello world" > hello.txt

  cat > test.bzl <<'EOF'
def _impl(ctx):
  script = Label("//:cmd.sh")
  file = Label("//:hello.txt")

  res = ctx.execute([script, file])

  if res.return_code != 0:
    fail("Return code 0 expected, but was " + res.return_code)

  if res.stdout.strip() != "hello world":
    fail("Stdout 'hello world' expected, but was '" + res.stdout + "'");

  ctx.file("BUILD")

remote_foo_configure = repository_rule(
  implementation = _impl,
  remotable = True,
)

local_foo_configure = repository_rule(
  implementation = _impl,
)
EOF

  cat > WORKSPACE <<'EOF'
load("//:test.bzl", "remote_foo_configure", "local_foo_configure")

remote_foo_configure(
  name = "remote_foo",
)

local_foo_configure(
  name = "local_foo",
)
EOF

  bazel fetch \
    --remote_executor=grpc://localhost:${worker_port} \
    --experimental_repo_remote_exec \
    @remote_foo//:all

  # '--expunge' is necessary in order to ensure that the repository is re-executed.
  bazel clean --expunge

  # Run on the host machine to test that the rule works for both local and remote execution.
  # In particular, that arguments of type label are accepted when doing local execution.
  bazel fetch \
    --experimental_repo_remote_exec \
    @remote_foo//:all

  bazel clean --expunge

  # Execute @local_foo which has the same implementation as @remote_foo but not the 'remotable'
  # attribute. This tests that a non-remotable repo rule can also run a remotable implementation
  # function.
  bazel fetch \
    --experimental_repo_remote_exec \
    @local_foo//:all
}

function test_exclusive_tag() {
  # Test that the exclusive tag works with the remote cache.
  mkdir -p a
  cat > a/success.sh <<'EOF'
#!/bin/sh
exit 0
EOF
  chmod 755 a/success.sh
  cat > a/BUILD <<'EOF'
sh_test(
  name = "success_test",
  srcs = ["success.sh"],
  tags = ["exclusive"],
)
EOF

  bazel test \
    --incompatible_exclusive_test_sandboxed \
    --remote_cache=grpc://localhost:${worker_port} \
    //a:success_test || fail "Failed to test //a:success_test"

  bazel test \
    --incompatible_exclusive_test_sandboxed \
    --remote_cache=grpc://localhost:${worker_port} \
    --nocache_test_results \
    //a:success_test >& $TEST_log || fail "Failed to test //a:success_test"

  expect_log "remote cache hit"
}

# TODO(alpha): Add a test that fails remote execution when remote worker
# supports sandbox.

function test_remote_download_toplevel_with_non_toplevel_unused_inputs_list() {
  # Test that --remote_download_toplevel should download non-toplevel
  # unused_inputs_list for starlark action. See #11732.

  touch WORKSPACE

  cat > test.bzl <<'EOF'
def _test_rule_impl(ctx):
    inputs = ctx.files.inputs
    output = ctx.outputs.out
    unused_inputs_list = ctx.actions.declare_file(ctx.label.name + ".unused")
    arguments = []
    arguments += [output.path]
    arguments += [unused_inputs_list.path]
    for input in inputs:
        arguments += [input.path]
    ctx.actions.run(
        inputs = inputs,
        outputs = [output, unused_inputs_list],
        arguments = arguments,
        executable = ctx.executable._executable,
        unused_inputs_list = unused_inputs_list,
    )

test_rule = rule(
    implementation = _test_rule_impl,
    attrs = {
        "inputs": attr.label_list(allow_files = True),
        "out": attr.output(),
        "_executable": attr.label(executable = True, cfg = "host", default = "//:exe"),
    },
)
EOF

  cat > BUILD <<'EOF'
load(":test.bzl", "test_rule")

test_rule(
    name = "test_non_toplevel",
    inputs = ["1.txt", "2.txt"],
    out = "3.txt",
)

sh_binary(
    name = "exe",
    srcs = ["a.sh"],
)

genrule(
    name = "test",
    srcs = [":test_non_toplevel"],
    outs = ["4.txt"],
    cmd = "cat $< > $@",
)
EOF

  cat > a.sh <<'EOF'
#!/bin/sh

output="$1"
shift
unused="$1"
shift
inp0="$1"
shift

cat "$inp0" > "$output"
echo "$1" > "$unused"
EOF

  chmod a+x a.sh

  touch 1.txt 2.txt

  CACHEDIR=$(mktemp -d)

  bazel build --disk_cache="$CACHEDIR" --remote_download_toplevel :test || fail "Failed to build :test"

  bazel clean || fail "Failed to clean"

  bazel build --disk_cache="$CACHEDIR" --remote_download_toplevel :test >& $TEST_log

  expect_log "INFO: Build completed successfully"
}

run_suite "Remote execution and remote cache tests"
