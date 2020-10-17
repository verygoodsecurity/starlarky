#!/bin/bash
#
# Copyright 2017 The Bazel Authors. All rights reserved.
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
# Verify that declared arguments of a repository rule are present before
# the first execution attempt of the rule is done.

# Load the test setup defined in the parent directory
CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${CURRENT_DIR}/../integration_test_setup.sh" \
  || { echo "integration_test_setup.sh not found!" >&2; exit 1; }

test_label_arg() {
  # Verify that a repository rule does not get restarted, if accessing
  # one of its label arguments as file.
  WRKDIR=`pwd`
  rm -rf repo
  rm -rf log
  mkdir repo
  cd repo
  touch BUILD
  cat > rule.bzl <<EOF
def _impl(ctx):
  # Access the build file late
  ctx.execute(["/bin/sh", "-c", "date +%s >> ${WRKDIR}/log"])
  ctx.file("WORKSPACE", "workspace(name = \"%s\")\n" % ctx.name)
  ctx.symlink(ctx.attr.build_file, "BUILD")

myrule=repository_rule(implementation=_impl,
 attrs={
   "build_file" : attr.label(),
 })
EOF
  cat > ext.BUILD <<'EOF'
genrule(
  name="foo",
  outs=["foo.txt"],
  cmd = "echo foo > $@",
)
EOF
  cat > WORKSPACE <<'EOF'
load("//:rule.bzl", "myrule")
myrule(name="ext", build_file="//:ext.BUILD")
EOF
  bazel build @ext//:foo || fail "expected success"
  [ `cat "${WRKDIR}/log" | wc -l` -eq 1 ] \
      || fail "did not find precisely one invocation of the action"
}

test_unused_invalid_label_arg() {
  # Verify that we preserve the behavior of allowing to pass labels that
  # do referring to an non-existing path, if they are never used.
  WRKDIR=`pwd`
  rm -rf repo
  mkdir repo
  cd repo
  touch BUILD
  cat > rule.bzl <<'EOF'
def _impl(ctx):
  ctx.file("WORKSPACE", "workspace(name = \"%s\")\n" % ctx.name)
  ctx.file("BUILD",
           "genrule(name=\"foo\", outs=[\"foo.txt\"], cmd = \"echo foo > $@\")")

myrule=repository_rule(implementation=_impl,
 attrs={
   "unused" : attr.label(),
 })
EOF
  cat > WORKSPACE <<'EOF'
load("//:rule.bzl", "myrule")
myrule(name="ext", unused="//does/not/exist:file")
EOF
  bazel build @ext//:foo || fail "expected success"
}


test_label_list_arg() {
  # Verify that a repository rule does not get restarted, if accessing
  # the entries of a label list as files.
  WRKDIR=`pwd`
  rm -rf repo
  rm -rf log
  mkdir repo
  cd repo
  touch BUILD
  cat > rule.bzl <<EOF
def _impl(ctx):
  ctx.execute(["/bin/sh", "-c", "date +%s >> ${WRKDIR}/log"])
  ctx.file("WORKSPACE", "workspace(name = \"%s\")\n" % ctx.name)
  ctx.file("BUILD",  """
genrule(
  name="foo",
  srcs= ["src.txt"],
  outs=["foo.txt"],
  cmd = "cp \$< \$@",
)
""")
  for f in ctx.attr.data:
    ctx.execute(["/bin/sh", "-c", "cat %s >> src.txt" % ctx.path(f)])

myrule=repository_rule(implementation=_impl,
 attrs={
   "data" : attr.label_list(),
 })
EOF
  cat > WORKSPACE <<'EOF'
load("//:rule.bzl", "myrule")
myrule(name="ext", data = ["//:a.txt", "//:b.txt"])
EOF
  echo Hello > a.txt
  echo World > b.txt
  bazel build @ext//:foo || fail "expected success"
  [ `cat "${WRKDIR}/log" | wc -l` -eq 1 ] \
      || fail "did not find precisely one invocation of the action"
}

test_unused_invalid_label_list_arg() {
  # Verify that we preserve the behavior of allowing to pass labels that
  # do referring to an non-existing path, if they are never used.
  # Here, test it if such labels are passed in a label list.
  WRKDIR=`pwd`
  rm -rf repo
  mkdir repo
  cd repo
  touch BUILD
  cat > rule.bzl <<'EOF'
def _impl(ctx):
  ctx.file("WORKSPACE", "workspace(name = \"%s\")\n" % ctx.name)
  ctx.file("BUILD",
           "genrule(name=\"foo\", outs=[\"foo.txt\"], cmd = \"echo foo > $@\")")

myrule=repository_rule(implementation=_impl,
 attrs={
   "unused_list" : attr.label_list(),
 })
EOF
  cat > WORKSPACE <<'EOF'
load("//:rule.bzl", "myrule")
myrule(name="ext", unused_list=["//does/not/exist:file1",
                                "//does/not/exists:file2"])
EOF
  bazel build @ext//:foo || fail "expected success"
}

# Regression test for https://github.com/bazelbuild/bazel/issues/10515
test_label_keyed_string_dict_arg() {
  # Verify that Bazel preloads Labels from label_keyed_string_dict, and as a
  # result, it runs the repository's implementation only once (i.e. it won't
  # restart the corresponding SkyFunction).
  WRKDIR=`pwd`
  rm -rf repo
  rm -rf log
  mkdir repo
  cd repo
  touch BUILD
  cat > rule.bzl <<EOF
def _impl(ctx):
    ctx.execute(["/bin/sh", "-c", "date +%s >> ${WRKDIR}/log"])
    ctx.file("WORKSPACE", "workspace(name = \"%s\")\n" % ctx.name)
    ctx.file("BUILD", """
genrule(
    name = "foo",
    srcs = ["src.txt"],
    outs = ["foo.txt"],
    cmd = "cp \$< \$@",
)
""")
    for f in ctx.attr.data:
        # ctx.path(f) shouldn't trigger a restart since we've prefetched the value.
        ctx.execute(["/bin/sh", "-c", "cat %s >> src.txt" % ctx.path(f)])

myrule = repository_rule(
    implementation = _impl,
    attrs = {
        "data": attr.label_keyed_string_dict(),
    },
)
EOF
  cat > WORKSPACE <<'EOF'
load("//:rule.bzl", "myrule")
myrule(name="ext", data = {"//:a.txt": "a", "//:b.txt": "b"})
EOF
  echo Hello > a.txt
  echo World > b.txt
  bazel build @ext//:foo || fail "expected success"
  [ `cat "${WRKDIR}/log" | wc -l` -eq 1 ] \
      || fail "did not find precisely one invocation of the action"
}

test_unused_invalid_label_keyed_string_dict_arg() {
  # Verify that we preserve the behavior of allowing to pass labels that
  # do referring to an non-existing path, if they are never used.
  # Here, test it if such labels are passed in a label_keyed_string_dict.
  WRKDIR=`pwd`
  rm -rf repo
  mkdir repo
  cd repo
  touch BUILD
  cat > rule.bzl <<'EOF'
def _impl(ctx):
  ctx.file("WORKSPACE", "workspace(name = \"%s\")\n" % ctx.name)
  ctx.file("BUILD",
           "genrule(name=\"foo\", outs=[\"foo.txt\"], cmd = \"echo foo > $@\")")

myrule=repository_rule(implementation=_impl,
 attrs={
   "unused_dict" : attr.label_keyed_string_dict(),
 })
EOF
  cat > WORKSPACE <<'EOF'
load("//:rule.bzl", "myrule")
myrule(name="ext", unused_dict={"//does/not/exist:file1": "file1",
                                "//does/not/exists:file2": "file2"})
EOF
  bazel build @ext//:foo || fail "expected success"
}

run_suite "Starlark repo prefetching tests"
