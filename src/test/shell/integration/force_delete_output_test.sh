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
# Test to ensure blaze forcibly deletes outputs from previous runs even if
# they reside in a write protected directory.

# Load the test setup defined in the parent directory
CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${CURRENT_DIR}/../integration_test_setup.sh" \
  || { echo "integration_test_setup.sh not found!" >&2; exit 1; }


function tear_down() {
  bazel clean
  bazel shutdown
  rm -rf x
}

function test_delete_in_unwritable_dir() {
  mkdir -p x || fail "Can't create x"
  cat > x/BUILD << 'EOF'
genrule(
  name = "unwritable",
  srcs = [],
  outs = ["unwritable/somefile.out"],
  local = 1,
  cmd = "echo 'Some output' > $@; chmod -w $$(dirname $@)"
)
EOF

  bazel build x:unwritable || fail "Failed the first compilation"

  # Now modify the build file to force a rebuild while creating the same output
  # within the write-protected directory.
  cat > x/BUILD << 'EOF'
genrule(
  name = "unwritable",
  srcs = [],
  outs = ["unwritable/somefile.out"],
  local = 1,
  cmd = "echo 'Some other output' > $@; chmod -w $$(dirname $@)"
)
EOF

  bazel build x:unwritable || fail "Failed 2nd compilation due to failure to delete output."
}

function test_delete_tree_in_unwritable_dir() {
  mkdir -p x || fail "Can't create x"
  cat > x/BUILD << 'EOF'
genrule(
  name = "unwritable",
  srcs = [],
  outs = ["unwritable/somedir"],
  local = 1,
  cmd = "mkdir -p $@; echo 'some output' > $@/somefile.out; chmod -w $$(dirname $@)"
)
EOF

  bazel build x:unwritable || fail "Failed the first compilation"

  # Now modify the build file to force a rebuild while creating the same output
  # within the write-protected directory.
  cat > x/BUILD << 'EOF'
genrule(
  name = "unwritable",
  srcs = [],
  outs = ["unwritable/somedir"],
  local = 1,
  cmd = "mkdir -p $@; echo 'some other output' > $@/somefile.out; chmod -w $$(dirname $@)"
)
EOF

  bazel build x:unwritable || fail "Failed 2nd compilation due to failure to delete output."
}

run_suite "Force delete output tests"