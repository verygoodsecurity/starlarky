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
# Tests the examples provided in Bazel
#

# Load the test setup defined in the parent directory
CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${CURRENT_DIR}/../integration_test_setup.sh" \
  || { echo "integration_test_setup.sh not found!" >&2; exit 1; }

# Appends to "WORKSPACE" the declaration of 2 local repositories.
# Assumes the main content of WORKSPACE was created previously.
function add_local_repos_to_workspace() {
  cat >> WORKSPACE <<EOF
local_repository(
  name = "repo",
  path = "a/b"
)

local_repository(
  name = "main_repo",
  path = "c/d"
)
EOF
}

# Appends to the WORKSPACE file under a given path (the first argument) the dependencies needed
# for proto_library.
function write_workspace() {
  workspace=""
  if [ ! -z "$1" ];
  then
    workspace=$1
    mkdir -p "$workspace"
  fi

  cat >> "$workspace"WORKSPACE << EOF
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "rules_proto",
    strip_prefix = "rules_proto-97d8af4dc474595af3900dd85cb3a29ad28cc313",
    sha256 = "602e7161d9195e50246177e7c55b2f39950a9cf7366f74ed5f22fd45750cd208",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
        "https://github.com/bazelbuild/rules_proto/archive/97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
    ],
)
load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")
rules_proto_dependencies()
rules_proto_toolchains()

# @com_google_protobuf//:protoc depends on @io_bazel//third_party/zlib.
new_local_repository(
    name = "io_bazel",
    path = "$(dirname $(rlocation io_bazel/third_party/rules_python/rules_python.WORKSPACE))/../..",
    build_file_content = "# Intentionally left empty.",
    workspace_file_content = "workspace(name = 'io_bazel')",
)

# TODO(#9029): May require some adjustment if/when we depend on the real
# @rules_python in the real source tree, since this third_party/ package won't
# be available.
new_local_repository(
    name = "rules_python",
    path = "$(dirname $(rlocation io_bazel/third_party/rules_python/rules_python.WORKSPACE))",
    build_file = "$(rlocation io_bazel/third_party/rules_python/BUILD)",
    workspace_file = "$(rlocation io_bazel/third_party/rules_python/rules_python.WORKSPACE)",
)
EOF
}

# Creates directories and files with the structure:
#  x/
#   person/
#     BUILD
#     person.proto (imports "bar/bar.proto", has strip_import_prefix = "")
#     phonenumber/
#       phonenumber.proto
#   phonebook/
#     BUILD
#     phonebook.proto (imports "person.proto" & "phonenumber/phonenumber.proto")
#
# The BUILD files could use directly "proto_library" rules or a macro called
# "proto_library_macro" that should be created beforehand using write_macro().
#
# Expected arguments:
# 1. The name of the proto library rule. Can be "proto_library" or
#    "proto_library_macro".
# 2. A row in the BUILD file that specifies the strip_import_prefix attribute on
#    proto_library. Should be left empty if a macro is used.
# 3. A load statement that includes a macro containing a wrapper around
#     proto_library.
function write_setup() {
  mkdir -p x/person/phonenumber
  proto_library_name=$1
  extra_attribute=$2
  include_macro=$3
  if [ "${include_macro}" -eq "" ]; then
    include_macro="load('@rules_proto//proto:defs.bzl', 'proto_library')"
  fi

  cat > x/person/BUILD << EOF
package(default_visibility = ["//visibility:public"])
$include_macro
$proto_library_name(
  name = "person_proto",
  srcs = ["person.proto"],
  deps = [":phonenumber_proto"],
  $extra_attribute
)

$proto_library_name(
  name = "phonenumber_proto",
  srcs = ["phonenumber/phonenumber.proto"],
  $extra_attribute
)
EOF

  cat > x/person/person.proto << EOF
syntax = "proto2";

option java_package = "person";
option java_outer_classname = "Person";

import "phonenumber/phonenumber.proto";

message PersonProto {
  optional string name = 1;

  optional PhoneNumberProto phone = 2;
}
EOF

  cat > x/person/phonenumber/phonenumber.proto << EOF
syntax = "proto2";

option java_package = "person.phonenumber";
option java_outer_classname = "PhoneNumber";

message PhoneNumberProto {
  required string number = 1;
}
EOF

  mkdir -p x/phonebook

  cat > x/phonebook/BUILD << EOF
$include_macro
$proto_library_name(
  name = "phonebook",
  srcs = ["phonebook.proto"],
  deps = ["//x/person:person_proto"],
)
EOF

  cat > x/phonebook/phonebook.proto << EOF
import "person.proto";
import "phonenumber/phonenumber.proto";

message Agenda {
  required PersonProto person = 1;
  required PhoneNumberProto number = 2;
}
EOF
}

# Creates the files in the following directory structure:
#  proto_library/
#                BUILD <- empty
#                src/
#                    BUILD <- has target "all" for all .proto
#                    address.proto  <- imports "person.proto"
#                    person.proto   <- imports "address.proto"
#                    zip_code.proto
function write_regression_setup() {
  mkdir -p proto_library/src
  touch proto_library/BUILD

  cat > proto_library/src/BUILD << EOF
load("@rules_proto//proto:defs.bzl", "proto_library")
proto_library(
    name = "all",
    srcs = glob(["*.proto"]),
    strip_import_prefix = "",
)
EOF

  cat > proto_library/src/address.proto <<EOF
syntax = "proto3";

package demo; // Required to generate valid code.

// Always import protos with a full path relative to the WORKSPACE file.
import "zip_code.proto";

message Address {
  string city = 1;
  ZipCode zip_code = 2;
}
EOF

  cat > proto_library/src/person.proto <<EOF
syntax = "proto3";

package demo; // Required to generate valid code.

// Always import protos with a full path relative to the WORKSPACE file.
import "address.proto";

message Person {
  string name = 1;
  int32 id = 2;
  string email = 3;
  Address address = 4;
}
EOF

  cat > proto_library/src/zip_code.proto <<EOF
syntax = "proto3";

package demo; // Required to generate valid code.

message ZipCode {
  string code = 1;
}
EOF
}

# Creates the directories and files in following the structure:
#  a/b/
#      WORKSPACE <- workspace referenced as "repo"
#      BUILD <- empty
#      src/
#          BUILD <- "all_protos" with strip_import_prefix
#          address.proto <- imports "zip_code.proto"
#          zip_code.proto
#  c/d/
#      WORKSPACE <- workspace referenced as "main_repo"
#      BUILD <- empty
#      src/
#          BUILD <- "all_protos" with strip_import_prefix
#          person.proto <- imports "address.proto" and depends on @repo//src:all_protos
function write_workspaces_setup() {
  mkdir -p a/b/src

  touch a/b/BUILD
  cat > a/b/src/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
load("@rules_proto//proto:defs.bzl", "proto_library")
proto_library(
  name = "all_protos",
  srcs = glob(["*.proto"]),
  strip_import_prefix = "",
)
EOF


  cat > a/b/src/address.proto <<EOF
syntax = "proto3";

package demo; // Required to generate valid code.

// Always import protos with a full path relative to the WORKSPACE file.
import "zip_code.proto";

message Address {
  string city = 1;
  ZipCode zip_code = 2;
}
EOF

  cat > a/b/src/zip_code.proto <<EOF
syntax = "proto3";

package demo; // Required to generate valid code.

message ZipCode {
  string code = 1;
}
EOF

  #### WORKSPACE(c/d) ####
  mkdir -p c/d/src

  touch c/d/BUILD

  cat > c/d/src/BUILD <<EOF
package(default_visibility = ["//visibility:public"])
load("@rules_proto//proto:defs.bzl", "proto_library")
proto_library(
  name = "all_protos",
  srcs = glob(["*.proto"]),
  strip_import_prefix = "",
  deps = ["@repo//src:all_protos"]
)
EOF

  cat > c/d/src/person.proto <<EOF
syntax = "proto3";

package demo; // Required to generate valid code.

// Always import protos with a full path relative to the WORKSPACE file.
import "address.proto";

message Person {
  string name = 1;
  int32 id = 2;
  string email = 3;
  Address address = 4;
}
EOF

}


# Creates macros/BUILD and macros/proto_library_macro.bzl, which contains a
# macro that wraps the proto_library rule. The macro passes to proto_library the
# same "name", "srcs", "deps" and adds "strip_import_prefix=''"
# This will be a common use case for the "strip_import_prefix" attribute.
function write_macro() {
  mkdir macros
  cat > macros/BUILD << EOF
export_files(["proto_library_macro.bzl])
EOF

  cat > macros/proto_library_macro.bzl << EOF
load("@rules_proto//proto:defs.bzl", "proto_library")
def proto_library_macro(name, srcs, deps = []):
  proto_library(
      name = name,
      srcs = srcs,
      deps = deps,
      strip_import_prefix = "",
  )
EOF
}

# Creates the directories and files from the following structure
#   java/com/google/src/
#                       BUILD
#                       A.java
function write_java_library() {
  # should depend on x/foo:foo
  mkdir -p java/com/google/src
  cat > java/com/google/src/BUILD << EOF
java_library(
  name = "top",
  srcs = ["A.java"],
  deps = [":jpl"]
)

java_proto_library(
  name = "jpl",
  deps = ["//x/person:person_proto"]
)
EOF

  cat > java/com/google/src/A.java << EOF
import person.Person.PersonProto;
import person.phonenumber.PhoneNumber.PhoneNumberProto;

public class A {
  private PersonProto person;

  public A(PersonProto person) {
    this.person = person;
    PhoneNumberProto number = person.getPhone();
  }
}

EOF

}

############# TESTS #############

function test_legacy_proto_library_include_well_known_protos() {
  write_workspace ""

  mkdir -p a
  cat > a/BUILD <<EOF
proto_library(
  name="a",
  srcs=["a.proto"],
  deps=[":b"],
)

proto_library(
  name="b",
  srcs=["b.proto"],
  deps=["@com_google_protobuf//:duration_proto"],
)
EOF

  cat > a/a.proto <<EOF
syntax = "proto3";

package a;

import "a/b.proto";

message A {
  int32 a = 1;
  b.B b = 2;
}
EOF

  cat > a/b.proto <<EOF
syntax = "proto3";

package b;

import "google/protobuf/duration.proto";

message B {
 int32 b = 1;
 google.protobuf.Duration timing = 2;
}
EOF

  bazel build //a:a || fail "build failed"
}

function test_javainfo_proto_aspect() {
  write_workspace ""

  mkdir -p java/proto/
  touch java/proto/my.proto
  cat > java/proto/BUILD << EOF
load(':my_rule_with_aspect.bzl', 'my_rule_with_aspect')
my_rule_with_aspect(
  name = 'my_rule',
  deps = [':my_java_proto']
)

java_proto_library(
  name = 'my_java_proto',
  deps = [':my_proto'],
)

proto_library(
  name = 'my_proto',
  srcs = ['my.proto'],
)
EOF

  cat > java/proto/my_rule_with_aspect.bzl <<EOF
def _my_rule_impl(ctx):
  aspect_java_infos = []
  for dep in ctx.attr.deps:
    aspect_java_infos += dep.my_aspect_providers
  merged_java_info = java_common.merge(aspect_java_infos)
  for jar in merged_java_info.transitive_runtime_jars.to_list():
    print('Transitive runtime jar', jar)

def _my_aspect_impl(target, ctx):
  aspect_java_infos = []
  for dep in ctx.rule.attr.deps:
    aspect_java_infos += dep.my_aspect_providers
  aspect_java_infos.append(target[JavaInfo])
  return struct(
    my_aspect_providers = aspect_java_infos
  )

my_aspect = aspect(
  attr_aspects = ['deps'],
  fragments = ['java'],
  implementation = _my_aspect_impl,
  required_aspect_providers = [[JavaInfo]]
)

my_rule_with_aspect = rule(
  implementation = _my_rule_impl,
  attrs = {
    'deps': attr.label_list(aspects = [my_aspect]),
  }
)
EOF
  bazel build java/proto:my_rule &> "$TEST_log"  || fail "build failed"
  expect_log "Transitive runtime jar <generated file java/proto/libmy_proto-speed.jar>"
}

function test_strip_import_prefix() {
  write_workspace ""
  write_setup "proto_library" "strip_import_prefix = '/x/person'" ""
  bazel build --verbose_failures //x/person:person_proto > "$TEST_log" || fail "Expected success"
}

function test_strip_import_prefix_fails() {
  write_workspace ""
  # Don't specify the "strip_import_prefix" attribute and expect failure.
  write_setup "proto_library" "" ""
  bazel build //x/person:person_proto >& "$TEST_log"  && fail "Expected failure"
  expect_log "phonenumber/phonenumber.proto: File not found."
}

function test_strip_import_prefix_macro() {
  write_workspace ""
  write_macro
  write_setup "proto_library_macro" "" "load('//macros:proto_library_macro.bzl', 'proto_library_macro')"
  bazel build //x/person:person_proto > "$TEST_log" || fail "Expected success"
}

# Fails with "IllegalArgumentException: external/lcocal_jdk in
# DumpPlatformClassPath.dumpJDK9AndNewerBootClassPath.java:67
function DISABLED_test_strip_import_prefix_with_java_library() {
  write_workspace ""
  write_setup "proto_library" "strip_import_prefix = '/x/person'" ""
  write_java_library
  bazel build //java/com/google/src:top \
      --strict_java_deps=off > "$TEST_log"  || fail "Expected success"
}

function test_strip_import_prefix_glob() {
  write_workspace ""
  write_regression_setup
  bazel build //proto_library/src:all >& "$TEST_log" || fail "Expected success"
}

function test_strip_import_prefix_multiple_workspaces() {
  write_workspace "a/b/"
  write_workspace "c/d/"
  write_workspace ""
  add_local_repos_to_workspace
  write_workspaces_setup

  bazel build @main_repo//src:all_protos >& "$TEST_log" || fail "Expected success"
}

function test_cc_proto_library() {
  write_workspace ""
  mkdir -p a
  cat > a/BUILD <<EOF
load("@rules_proto//proto:defs.bzl", "proto_library")
proto_library(name='p', srcs=['p.proto'])
cc_proto_library(name='cp', deps=[':p'])
cc_library(name='c', srcs=['c.cc'], deps=[':cp'])
EOF

  cat > a/p.proto <<EOF
syntax = "proto2";
package a;
message A {
  optional int32 a = 1;
}
EOF

  cat > a/c.cc <<EOF
#include "a/p.pb.h"

void f() {
  a::A a;
}
EOF

  bazel build //a:c || fail "build failed"
}

function test_cc_proto_library_import_prefix_stripping() {
  write_workspace ""
  mkdir -p a/dir
  cat > a/BUILD <<EOF
load("@rules_proto//proto:defs.bzl", "proto_library")
proto_library(name='p', srcs=['dir/p.proto'], strip_import_prefix='/a')
cc_proto_library(name='cp', deps=[':p'])
cc_library(name='c', srcs=['c.cc'], deps=[':cp'])
EOF

  cat > a/dir/p.proto <<EOF
syntax = "proto2";
package a;
message A {
  optional int32 a = 1;
}
EOF

  cat > a/c.cc <<EOF
#include "dir/p.pb.h"

void f() {
  a::A a;
}
EOF

  bazel build //a:c || fail "build failed"
}

function test_import_prefix_stripping() {
  mkdir -p e
  touch e/WORKSPACE
  write_workspace ""

  cat >> WORKSPACE <<EOF
local_repository(
  name = "repo",
  path = "e"
)
EOF

  mkdir -p e/f/bad
  cat > e/f/BUILD <<EOF
load("@rules_proto//proto:defs.bzl", "proto_library")
proto_library(
  name = "f",
  strip_import_prefix = "bad",
  import_prefix = "good",
  srcs = ["bad/f.proto"],
  visibility = ["//visibility:public"],
)
EOF

  cat > e/f/bad/f.proto <<EOF
syntax = "proto2";
package f;

message F {
  optional int32 f = 1;
}
EOF

  mkdir -p g/bad
  cat > g/BUILD << EOF
load("@rules_proto//proto:defs.bzl", "proto_library")
proto_library(
  name = 'g',
  strip_import_prefix = "/g/bad",
  import_prefix = "good",
  srcs = ['bad/g.proto'],
  visibility = ["//visibility:public"],
)
EOF

  cat > g/bad/g.proto <<EOF
syntax = "proto2";
package g;

message G {
  optional int32 g = 1;
}
EOF

  mkdir -p h
  cat > h/BUILD <<EOF
load("@rules_proto//proto:defs.bzl", "proto_library")
proto_library(
  name = "h",
  srcs = ["h.proto"],
  deps = ["//g", "@repo//f"],
)
EOF

  cat > h/h.proto <<EOF
syntax = "proto2";
package h;

import "good/f.proto";
import "good/g.proto";

message H {
  optional f.F f = 1;
  optional g.G g = 2;
}
EOF

  bazel build //h || fail "build failed"
}

run_suite "Integration tests for proto_library"
