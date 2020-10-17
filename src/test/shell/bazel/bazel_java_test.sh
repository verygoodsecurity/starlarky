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

JAVA_TOOLCHAIN="$1"; shift
add_to_bazelrc "build --java_toolchain=${JAVA_TOOLCHAIN}"
add_to_bazelrc "build --host_java_toolchain=${JAVA_TOOLCHAIN}"

JAVA_TOOLS_ZIP="$1"; shift
if [[ "${JAVA_TOOLS_ZIP}" != "released" ]]; then
  if [[ "${JAVA_TOOLS_ZIP}" == file* ]]; then
    JAVA_TOOLS_ZIP_FILE_URL="${JAVA_TOOLS_ZIP}"
  elif "$is_windows"; then
    JAVA_TOOLS_ZIP_FILE_URL="file:///$(rlocation io_bazel/$JAVA_TOOLS_ZIP)"
  else
    JAVA_TOOLS_ZIP_FILE_URL="file://$(rlocation io_bazel/$JAVA_TOOLS_ZIP)"
  fi
fi
JAVA_TOOLS_ZIP_FILE_URL=${JAVA_TOOLS_ZIP_FILE_URL:-}

if [[ $# -gt 0 ]]; then
  JAVABASE_VALUE="$1"; shift
  add_to_bazelrc "build --javabase=${JAVABASE_VALUE}"
  add_to_bazelrc "build --host_javabase=${JAVABASE_VALUE}"
fi

export TESTENV_DONT_BAZEL_CLEAN=1

function set_up() {
  cat >>WORKSPACE <<EOF
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
# java_tools versions only used to test Bazel with various JDK toolchains.
EOF

  if [[ ! -z "${JAVA_TOOLS_ZIP_FILE_URL}" ]]; then
    cat >>WORKSPACE <<EOF
http_archive(
    name = "local_java_tools",
    urls = ["${JAVA_TOOLS_ZIP_FILE_URL}"]
)
EOF
  fi

  cat $(rlocation io_bazel/src/test/shell/bazel/testdata/jdk_http_archives) >> WORKSPACE
}

function tear_down() {
  rm -rf "$(bazel info bazel-bin)/java"
}

function write_hello_library_files() {
  mkdir -p java/main
  cat >java/main/BUILD <<EOF
java_binary(name = 'main',
    deps = ['//java/hello_library'],
    srcs = ['Main.java'],
    main_class = 'main.Main')
EOF

  cat >java/main/Main.java <<EOF
package main;
import hello_library.HelloLibrary;
public class Main {
  public static void main(String[] args) {
    HelloLibrary.funcHelloLibrary();
    System.out.println("Hello, World!");
  }
}
EOF

  mkdir -p java/hello_library
  cat >java/hello_library/BUILD <<EOF
package(default_visibility=['//visibility:public'])
java_library(name = 'hello_library',
             srcs = ['HelloLibrary.java']);
EOF

  cat >java/hello_library/HelloLibrary.java <<EOF
package hello_library;
public class HelloLibrary {
  public static void funcHelloLibrary() {
    System.out.print("Hello, Library!;");
  }
}
EOF
}

function write_files_for_java_provider_in_attr() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,{A,B,Main}.java,java_custom_library.bzl}

  rule_type="$1" # java_library / java_import
  attribute_name="$2" # exports / runtime_deps
  srcs_attribute_row="srcs = ['A.java']"
  if [ "$rule_type" = "java_import" ]; then
    srcs_attribute_row="jars = []"
  fi

  cat > java/com/google/sandwich/BUILD <<EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_binary(
  name = "Main",
EOF

  if [ "$attribute_name" = "runtime_deps" ]; then
    cat >> java/com/google/sandwich/BUILD <<EOF
  main_class = "com.google.sandwich.Main",
  runtime_deps = [":top"]
)

EOF
  else
    cat >> java/com/google/sandwich/BUILD <<EOF
  srcs = ["Main.java"],
  deps = [":top"]
)

EOF
  fi

  echo "$rule_type(" >> java/com/google/sandwich/BUILD

  cat >> java/com/google/sandwich/BUILD <<EOF
  name = "top",
EOF

  echo "  $srcs_attribute_row," >> java/com/google/sandwich/BUILD
  echo "  $attribute_name = [':middle']" >> java/com/google/sandwich/BUILD

  cat >> java/com/google/sandwich/BUILD <<EOF
)

java_custom_library(
  name = "middle",
EOF

  if [ "$attribute_name" = "runtime_deps" ]; then
    cat >> java/com/google/sandwich/BUILD <<EOF
  srcs = ["B.java", "Main.java"],
)
EOF
  else
    cat >> java/com/google/sandwich/BUILD <<EOF
  srcs = ["B.java"],
)
EOF
  fi

  cat > java/com/google/sandwich/B.java <<EOF
package com.google.sandwich;
class B {
  public void printB() {
    System.out.println("Message from B");
  }
}
EOF

if [ "$rule_type" = "java_library" ]; then
  cat > java/com/google/sandwich/A.java <<EOF
package com.google.sandwich;
class A {
  public void printA() {
    System.out.println("Message from A");
  }
}
EOF
fi

cat > java/com/google/sandwich/Main.java <<EOF
package com.google.sandwich;
class Main {
  public static void main(String[] args) {
EOF

if [[ "$rule_type" = "java_library" && "$attribute_name" = "exports" ]]; then
  cat >> java/com/google/sandwich/Main.java <<EOF
    A myObjectA = new A();
    myObjectA.printA();
EOF
fi

cat >> java/com/google/sandwich/Main.java <<EOF
    B myObjectB = new B();
    myObjectB.printB();
  }
}
EOF
}

function write_java_custom_rule() {
  cat > java/com/google/sandwich/java_custom_library.bzl << EOF
def _impl(ctx):
  deps = [dep[java_common.provider] for dep in ctx.attr.deps]
  exports = [export[java_common.provider] for export in ctx.attr.exports]

  output_jar = ctx.actions.declare_file("lib" + ctx.label.name + ".jar")

  compilation_provider = java_common.compile(
    ctx,
    source_files = ctx.files.srcs,
    output = output_jar,
    deps = deps,
    exports = exports,
    resources = ctx.files.resources,
    strict_deps = "ERROR",
    java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    host_javabase = ctx.attr._host_javabase[java_common.JavaRuntimeInfo],
  )
  return struct(
    files = depset([output_jar]),
    providers = [compilation_provider]
  )

java_custom_library = rule(
  implementation = _impl,
  attrs = {
    "srcs": attr.label_list(allow_files=True),
    "deps": attr.label_list(),
    "exports": attr.label_list(),
    "resources": attr.label_list(allow_files=True),
    "_java_toolchain": attr.label(default = Label("${JAVA_TOOLCHAIN}")),
    "_host_javabase": attr.label(default = Label("@bazel_tools//tools/jdk:current_host_java_runtime"))
  },
  fragments = ["java"]
)
EOF
}

function test_build_hello_world() {
  write_hello_library_files

  bazel build //java/main:main &> $TEST_log || fail "build failed"
}

function test_worker_strategy_is_default() {
  write_hello_library_files

  bazel build //java/main:main \
     &> $TEST_log || fail "build failed"
  # By default, Java rules use worker strategy
  expect_log " processes: .*worker"
}
function test_strategy_overrides_worker_default() {
  write_hello_library_files

  bazel build //java/main:main \
    --spawn_strategy=local &> $TEST_log || fail "build failed"
  # Java rules defaulting to worker do not override the strategy specified on
  # the cli
  expect_not_log " processes: .*worker"
}
function test_strategy_picks_first_preferred_worker() {
  write_hello_library_files

  bazel build //java/main:main \
    --spawn_strategy=worker,local &> $TEST_log || fail "build failed"
  expect_log " processes: .*worker"
}

function test_strategy_picks_first_preferred_local() {
  write_hello_library_files

  bazel build //java/main:main \
    --spawn_strategy=local,worker &> $TEST_log || fail "build failed"
  expect_not_log " processes: .*worker"
  expect_log " processes: .*local"
}

# This test verifies that jars named by deploy_env are excluded from the final
# deploy jar.
function test_build_with_deploy_env() {
  write_hello_library_files

  # Overwrite java/main to add deploy_env customizations and remove the
  # compile-time hello_library dependency.
  cat >java/main/BUILD <<EOF
java_binary(name = 'env', runtime_deps = ['//java/hello_library'])
java_binary(name = 'main',
    runtime_deps = ['//java/hello_library'],
    srcs = ['Main.java'],
    main_class = 'main.Main',
    deploy_env = ['env'])
EOF

  cat >java/main/Main.java <<EOF
package main;
public class Main {
  public static void main(String[] args) {
    System.out.println("Hello, World!");
  }
}
EOF

  bazel build //java/main:main_deploy.jar &> $TEST_log || fail "build failed"
  zipinfo -1 ${PRODUCT_NAME}-bin/java/main/main_deploy.jar &> $TEST_log \
     || fail "Failed to zipinfo ${PRODUCT_NAME}-bin/java/main/main_deploy.jar"
  expect_not_log "hello_library/HelloLibrary.class"
}

function test_build_with_sourcepath() {
  mkdir -p g
  cat >g/A.java <<'EOF'
package g;
public class A {
   public A() {
      new B();
   }
}
EOF

  cat >g/B.java <<'EOF'
package g;
public class B {
   public B() {
   }
}
EOF

  cat >g/BUILD <<'EOF'
genrule(
  name = "stub",
  srcs = ["B.java"],
  outs = ["B.jar"],
  cmd = "zip $@ $(SRCS)",
)
java_library(
  name = "test",
  srcs = ["A.java"],
  javacopts = ["-sourcepath $(GENDIR)/$(location :stub)", "-implicit:none"],
  deps = [":stub"]
)
EOF
  bazel build //g:test >$TEST_log || fail "Failed to build //g:test"
}

 function test_java_common_compile_sourcepath() {
   mkdir -p g
   cat >g/A.java <<'EOF'
package g;
public class A {
   public A() {
      new B();
   }
}
EOF

  cat >g/B.java <<'EOF'
package g;
public class B {
   public B() {
   }
}
EOF

   cat >g/BUILD <<'EOF'
load(':java_custom_library.bzl', 'java_custom_library')
genrule(
  name = "stub",
  srcs = ["B.java"],
  outs = ["B.jar"],
  cmd = "zip $@ $(SRCS)",
)

java_custom_library(
  name = "test",
  srcs = ["A.java"],
  sourcepath = [":stub"]
)
EOF

  cat >g/java_custom_library.bzl << EOF
def _impl(ctx):
  output_jar = ctx.actions.declare_file("lib" + ctx.label.name + ".jar")

  compilation_provider = java_common.compile(
    ctx,
    source_files = ctx.files.srcs,
    output = output_jar,
    deps = [],
    sourcepath = ctx.files.sourcepath,
    strict_deps = "ERROR",
    java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    host_javabase = ctx.attr._host_javabase[java_common.JavaRuntimeInfo],
  )
  return struct(
    files = depset([output_jar]),
    providers = [compilation_provider]
  )

java_custom_library = rule(
  implementation = _impl,
  attrs = {
    "srcs": attr.label_list(allow_files=True),
    "sourcepath": attr.label_list(),
    "_java_toolchain": attr.label(default = Label("${JAVA_TOOLCHAIN}")),
    "_host_javabase": attr.label(default = Label("@bazel_tools//tools/jdk:current_host_java_runtime"))
  },
  fragments = ["java"]
)
EOF
   bazel build //g:test &> $TEST_log || fail "Failed to build //g:test"
   zipinfo -1 bazel-bin/g/libtest.jar >> $TEST_log || fail "Failed to zipinfo -1 bazel-bin/g/libtest.jar"
   expect_log "g/A.class"
   expect_not_log "g/B.class"
 }

function test_java_common_compile_sourcepath_with_implicit_class() {
   mkdir -p g
   cat >g/A.java <<'EOF'
package g;
public class A {
   public A() {
      new B();
   }
}
EOF

  cat >g/B.java <<'EOF'
package g;
public class B {
   public B() {
   }
}
EOF

   cat >g/BUILD <<'EOF'
load(':java_custom_library.bzl', 'java_custom_library')
genrule(
  name = "stub",
  srcs = ["B.java"],
  outs = ["B.jar"],
  cmd = "zip $@ $(SRCS)",
)

java_custom_library(
  name = "test",
  srcs = ["A.java"],
  sourcepath = [":stub"]
)
EOF

  cat >g/java_custom_library.bzl << EOF
def _impl(ctx):
  output_jar = ctx.actions.declare_file("lib" + ctx.label.name + ".jar")

  compilation_provider = java_common.compile(
    ctx,
    source_files = ctx.files.srcs,
    output = output_jar,
    javac_opts = ["-implicit:class"],
    deps = [],
    sourcepath = ctx.files.sourcepath,
    strict_deps = "ERROR",
    java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    host_javabase = ctx.attr._host_javabase[java_common.JavaRuntimeInfo],
  )
  return struct(
    files = depset([output_jar]),
    providers = [compilation_provider]
  )

java_custom_library = rule(
  implementation = _impl,
  attrs = {
    "srcs": attr.label_list(allow_files=True),
    "sourcepath": attr.label_list(),
    "_java_toolchain": attr.label(default = Label("${JAVA_TOOLCHAIN}")),
    "_host_javabase": attr.label(default = Label("@bazel_tools//tools/jdk:current_host_java_runtime"))
  },
  fragments = ["java"]
)
EOF
   bazel build //g:test &> $TEST_log || fail "Failed to build //g:test"
   zipinfo -1 bazel-bin/g/libtest.jar >> $TEST_log || fail "Failed to zipinfo -1 bazel-bin/g/libtest.jar"
   expect_log "g/A.class"
   expect_log "g/B.class"
 }

# Runfiles is disabled by default on Windows, but we can test it on Unix by
# adding flag --enable_runfiles=0
function test_build_and_run_hello_world_without_runfiles() {
  write_hello_library_files

  bazel run --enable_runfiles=0 //java/main:main &> $TEST_log || fail "build failed"
  expect_log "Hello, Library!;Hello, World!"
}

function test_errorprone_error_fails_build_by_default() {
  write_hello_library_files
  # Trigger an error-prone error by comparing two arrays via #equals().
  cat >java/hello_library/HelloLibrary.java <<EOF
package hello_library;
public class HelloLibrary {
  public static boolean funcHelloLibrary() {
    int[] arr1 = {1, 2, 3};
    int[] arr2 = {1, 2, 3};
    return arr1.equals(arr2);
  }
}
EOF

  bazel build //java/main:main &> $TEST_log && fail "build should have failed" || true
  expect_log "error: \[ArrayEquals\] Reference equality used to compare arrays"
}

function test_extrachecks_off_disables_errorprone() {
  write_hello_library_files
  # Trigger an error-prone error by comparing two arrays via #equals().
  cat >java/hello_library/HelloLibrary.java <<EOF
package hello_library;
public class HelloLibrary {
  public static boolean funcHelloLibrary() {
    int[] arr1 = {1, 2, 3};
    int[] arr2 = {1, 2, 3};
    return arr1.equals(arr2);
  }
}
EOF
  # Disable error-prone for this target, though.
  cat >java/hello_library/BUILD <<EOF
package(default_visibility=['//visibility:public'])
java_library(name = 'hello_library',
             srcs = ['HelloLibrary.java'],
             javacopts = ['-XepDisableAllChecks'],);
EOF

  bazel build //java/main:main &> $TEST_log || fail "build failed"
  expect_not_log "error: \[ArrayEquals\] Reference equality used to compare arrays"
}

function test_java_test_main_class() {
  setup_javatest_support
  mkdir -p java/testrunners || fail "mkdir failed"
  cat > java/testrunners/TestRunner.java <<EOF
package testrunners;

import com.google.testing.junit.runner.BazelTestRunner;

public class TestRunner {
  public static void main(String[] argv) {
    System.out.println("Custom test runner was run");
    BazelTestRunner.main(argv);
  }
}
EOF

  cat > java/testrunners/Tests.java <<EOF
package testrunners;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;

@RunWith(JUnit4.class)
public class Tests {

  @Test
  public void testTest() {
    System.out.println("testTest was run");
  }
}
EOF

  cat > java/testrunners/BUILD <<EOF
java_library(name = "test_runner",
             srcs = ['TestRunner.java'],
             deps = ['@bazel_tools//tools/jdk:TestRunner'],
)

java_test(name = "Tests",
          srcs = ['Tests.java'],
          deps = ['//third_party:junit4'],
          main_class = "testrunners.TestRunner",
          runtime_deps = [':test_runner']
)
EOF
  bazel test --test_output=streamed //java/testrunners:Tests &> "$TEST_log"
  expect_log "Custom test runner was run"
  expect_log "testTest was run"
}

function test_basic_java_sandwich() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,{A,B,C,Main}.java,java_custom_library.bzl}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_binary(
  name = "Main",
  srcs = ["Main.java"],
  deps = [":top"]
)

java_library(
  name = "top",
  srcs = ["A.java"],
  deps = [":middle"]
)

java_custom_library(
  name = "middle",
  srcs = ["B.java"],
  deps = [":bottom"]
)

java_library(
  name = "bottom",
  srcs = ["C.java"]
)
EOF

  cat > java/com/google/sandwich/C.java << EOF
package com.google.sandwich;
class C {
  public void printC() {
    System.out.println("Message from C");
  }
}
EOF

  cat > java/com/google/sandwich/B.java << EOF
package com.google.sandwich;
class B {
  C myObject;
  public void printB() {
    System.out.println("Message from B");
    myObject = new C();
    myObject.printC();
  }
}
EOF

  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A {
  B myObject;
  public void printA() {
    System.out.println("Message from A");
    myObject = new B();
    myObject.printB();
  }
}
EOF

  cat > java/com/google/sandwich/Main.java << EOF
package com.google.sandwich;
class Main {
  public static void main(String[] args) {
    A myObject = new A();
    myObject.printA();
  }
}
EOF

  write_java_custom_rule

  bazel run java/com/google/sandwich:Main > $TEST_log || fail "Java sandwich build failed"
  expect_log "Message from A"
  expect_log "Message from B"
  expect_log "Message from C"
}

function test_java_library_exports_java_sandwich() {
  write_files_for_java_provider_in_attr "java_library" "exports"
  write_java_custom_rule

  bazel run java/com/google/sandwich:Main > $TEST_log || fail "Java sandwich build failed"
  expect_log "Message from A"
  expect_log "Message from B"
}

function test_java_library_runtime_deps_java_sandwich() {
  write_files_for_java_provider_in_attr "java_library" "runtime_deps"
  write_java_custom_rule

  bazel run java/com/google/sandwich:Main > $TEST_log || fail "Java sandwich build failed"
  expect_log "Message from B"
}

function test_java_import_exports_java_sandwich() {
  write_files_for_java_provider_in_attr "java_import" "exports"
  write_java_custom_rule

  bazel run java/com/google/sandwich:Main > $TEST_log || fail "Java sandwich build failed"
  expect_log "Message from B"
}

function test_java_import_runtime_deps_java_sandwich() {
  write_files_for_java_provider_in_attr "java_import" "runtime_deps"
  write_java_custom_rule

  bazel run java/com/google/sandwich:Main > $TEST_log || fail "Java sandwich build failed"
  expect_log "Message from B"
}

function test_java_binary_deps_java_sandwich() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,{A,B,Main}.java,java_custom_library.bzl}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_binary(
  name = "Main",
  srcs = ["Main.java"],
  deps = [":custom"]
)

java_custom_library(
  name = "custom",
  srcs = ["A.java"],
  deps = [":bottom"]
)

java_library(
  name = "bottom",
  srcs = ["B.java"]
)
EOF

  cat > java/com/google/sandwich/B.java << EOF
package com.google.sandwich;
class B {
  public void print() {
    System.out.println("Message from B");
  }
}
EOF

  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A {
  B myObject;
  public void print() {
    System.out.println("Message from A");
    myObject = new B();
    myObject.print();
  }
}
EOF

  cat > java/com/google/sandwich/Main.java << EOF
package com.google.sandwich;
class Main {
  public static void main(String[] args) {
    A myObject = new A();
    myObject.print();
  }
}
EOF

  write_java_custom_rule

  bazel run java/com/google/sandwich:Main > "$TEST_log" || fail "Java sandwich build failed"
  expect_log "Message from A"
  expect_log "Message from B"
}

function test_java_binary_runtime_deps_java_sandwich() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,{A,Main}.java,java_custom_library.bzl}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_binary(
  name = "Main",
  main_class = "com.google.sandwich.Main",
  runtime_deps = [":custom"]
)

java_custom_library(
  name = "custom",
  srcs = ["Main.java"],
  deps = [":bottom"]
)

java_library(
  name = "bottom",
  srcs = ["A.java"]
)
EOF

  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A {
  public void print() {
    System.out.println("Message from A");
  }
}
EOF

  cat > java/com/google/sandwich/Main.java << EOF
package com.google.sandwich;
class Main {
  public static void main(String[] args) {
    System.out.println("Message from Main");
    A myObject = new A();
    myObject.print();
  }
}
EOF

  write_java_custom_rule

  bazel run java/com/google/sandwich:Main > "$TEST_log" || fail "Java sandwich build failed"
  expect_log "Message from Main"
  expect_log "Message from A"
}

function test_java_test_java_sandwich() {
  setup_javatest_support
  mkdir -p java/com/google/sandwich
  touch BUILD java/com/google/sandwich/{{A,B,MainTest}.java,java_custom_library.bzl}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_test(
  name = "MainTest",
  size = "small",
  srcs = ["MainTest.java"],
  deps = [
      ":custom",
      "//third_party:junit4",
  ],
)

java_custom_library(
  name = "custom",
  srcs = ["A.java"],
  deps = [":bottom"]
)

java_library(
  name = "bottom",
  srcs = ["B.java"]
)
EOF

  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A {
  B myObj = new B();
  public boolean returnsTrue() {
    System.out.println("Message from A");
    return myObj.returnsTrue();
  }
}
EOF

  cat > java/com/google/sandwich/B.java << EOF
package com.google.sandwich;
class B {
  public boolean returnsTrue() {
    System.out.println("Message from B");
    return true;
  }
}
EOF

  cat > java/com/google/sandwich/MainTest.java << EOF
package com.google.sandwich;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MainTest {
  @Test
  public void testReturnsTrue() {
    A myObj = new A();
    assertTrue(myObj.returnsTrue());
    System.out.println("Test message");
  }
}
EOF

  write_java_custom_rule

  bazel test java/com/google/sandwich:MainTest --test_output=streamed > "$TEST_log" || fail "Java sandwich for java_test failed"
  expect_log "Message from A"
  expect_log "Message from B"
  expect_log "Test message"
}

function test_explicit_java_test_deps_flag() {
  setup_javatest_support
  mkdir -p java/testrunners || fail "mkdir failed"

  cat > java/testrunners/Tests.java <<EOF
package testrunners;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;

@RunWith(JUnit4.class)
public class Tests {

  @Test
  public void testTest() {
    System.out.println("testTest was run");
  }
}
EOF

  # With explicit_java_test_deps, we fail without explicitly specifying the JUnit deps.
  cat > java/testrunners/BUILD <<EOF
java_test(name = "Tests",
          srcs = ['Tests.java'],
)
EOF
  bazel test --test_output=streamed --explicit_java_test_deps //java/testrunners:Tests \
      &> "$TEST_log" && fail "Expected Failure" || true
  expect_log "cannot find symbol"

  # We start passing again with explicit_java_test_deps once we explicitly specify the deps.
  cat > java/testrunners/BUILD <<EOF
java_test(name = "Tests",
          srcs = ['Tests.java'],
          deps = ['//third_party:junit4'],
)
EOF
  bazel test --test_output=streamed --explicit_java_test_deps //java/testrunners:Tests \
      &> "$TEST_log" || fail "Expected success"
  expect_log "testTest was run"
}

function test_java_sandwich_resources_file() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,A.java,java_custom_library.bzl,my_precious_resource.txt}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')
java_custom_library(
  name = "custom",
  srcs = ["A.java"],
  resources = ["my_precious_resource.txt"]
)
EOF
  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A { }
EOF
  write_java_custom_rule

  bazel build java/com/google/sandwich:custom > "$TEST_log" || fail "Java sandwich build failed"
  unzip -l bazel-bin/java/com/google/sandwich/libcustom.jar  > "$TEST_log"
  expect_log "my_precious_resource.txt"
}

function test_java_sandwich_resources_filegroup() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,A.java,java_custom_library.bzl,my_precious_resource.txt,my_other_precious_resource.txt}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')
filegroup(
  name = "resources_group",
  srcs = ["my_precious_resource.txt", "my_other_precious_resource.txt"]
)
java_custom_library(
  name = "custom",
  srcs = ["A.java"],
  resources = [":resources_group"]
)
EOF
  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A { }
EOF
  write_java_custom_rule

  bazel build java/com/google/sandwich:custom > "$TEST_log" || fail "Java sandwich build failed"
  unzip -l bazel-bin/java/com/google/sandwich/libcustom.jar  > "$TEST_log"
  expect_log "my_precious_resource.txt"
  expect_log "my_other_precious_resource.txt"
}

function test_basic_java_sandwich_with_exports() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,{A,B,C,Main}.java,java_custom_library.bzl}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_binary(
  name = "Main",
  srcs = ["Main.java"],
  deps = [":custom"]
)

java_custom_library(
  name = "custom",
  srcs = ["A.java"],
  exports = [":lib-b"]
)

java_custom_library(
  name = "lib-b",
  srcs = ["B.java"],
  exports = [":lib-c"]
)

java_custom_library(
  name = "lib-c",
  srcs = ["C.java"],
)
EOF

  cat > java/com/google/sandwich/B.java << EOF
package com.google.sandwich;
class B {
  public static void print() {
    System.out.println("Message from B");
  }
}
EOF

cat > java/com/google/sandwich/C.java << EOF
package com.google.sandwich;
class C {
  public static void print() {
    System.out.println("Message from C");
  }
}
EOF

  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A {
  public static void print() {
    System.out.println("Message from A");
  }
}
EOF

  cat > java/com/google/sandwich/Main.java << EOF
package com.google.sandwich;
class Main {
  public static void main(String[] args) {
    A.print();
    B.print();
    C.print();
  }
}
EOF

  write_java_custom_rule

  bazel run java/com/google/sandwich:Main > "$TEST_log" || fail "Java sandwich build failed"
  expect_log "Message from A"
  expect_log "Message from B"
  expect_log "Message from C"
}


function test_basic_java_sandwich_with_exports_and_java_library() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,{A,B,C,Main}.java,java_custom_library.bzl}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_binary(
  name = "Main",
  srcs = ["Main.java"],
  deps = [":custom"]
)

java_custom_library(
  name = "custom",
  srcs = ["A.java"],
  exports = [":lib-b"]
)

java_library(
  name = "lib-b",
  srcs = ["B.java"],
  exports = [":lib-c"]
)

java_library(
  name = "lib-c",
  srcs = ["C.java"],
)
EOF

  cat > java/com/google/sandwich/B.java << EOF
package com.google.sandwich;
class B {
  public static void print() {
    System.out.println("Message from B");
  }
}
EOF

cat > java/com/google/sandwich/C.java << EOF
package com.google.sandwich;
class C {
  public static void print() {
    System.out.println("Message from C");
  }
}
EOF

  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A {
  public static void print() {
    System.out.println("Message from A");
  }
}
EOF

  cat > java/com/google/sandwich/Main.java << EOF
package com.google.sandwich;
class Main {
  public static void main(String[] args) {
    A.print();
    B.print();
    C.print();
  }
}
EOF
  write_java_custom_rule

  bazel run java/com/google/sandwich:Main > "$TEST_log" || fail "Java sandwich build failed"
  expect_log "Message from A"
  expect_log "Message from B"
  expect_log "Message from C"
}

function test_java_sandwich_default_strict_deps() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,A.java,java_custom_library.bzl}
  write_java_custom_rule

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_custom_library(
  name = "custom",
  srcs = ["A.java"]
)
EOF

  sed -i -- 's/ERROR/DEFAULT/g' 'java/com/google/sandwich/java_custom_library.bzl'
  bazel build java/com/google/sandwich:custom > $TEST_log || fail "Java sandwich build failed"

  sed -i -- 's/DEFAULT/WARN/g' 'java/com/google/sandwich/java_custom_library.bzl'
  bazel build java/com/google/sandwich:custom > $TEST_log || fail "Java sandwich build failed"
}

function test_basic_java_sandwich_with_transitive_deps_and_java_library_should_fail() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,{A,B,C,Main}.java,java_custom_library.bzl}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_binary(
  name = "Main",
  srcs = ["Main.java"],
  deps = [":custom"]
)

java_custom_library(
  name = "custom",
  srcs = ["A.java"],
  deps = [":lib-b"]
)

java_library(
  name = "lib-b",
  srcs = ["B.java"],
  deps = [":lib-c"]
)

java_library(
  name = "lib-c",
  srcs = ["C.java"],
)
EOF

  cat > java/com/google/sandwich/B.java << EOF
package com.google.sandwich;
class B {
  public static void print() {
    System.out.println("Message from B");
  }
}
EOF

cat > java/com/google/sandwich/C.java << EOF
package com.google.sandwich;
class C {
  public static void print() {
    System.out.println("Message from C");
  }
}
EOF

  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A {
  public static void print() {
    System.out.println("Message from A");
  }
}
EOF

  cat > java/com/google/sandwich/Main.java << EOF
package com.google.sandwich;
class Main {
  public static void main(String[] args) {
    A.print();
    B.print();
    C.print();
  }
}
EOF
  write_java_custom_rule

  bazel run java/com/google/sandwich:Main &> "$TEST_log" && fail "Java sandwich build shold have failed" || true
  expect_log "Using type com.google.sandwich.B from an indirect dependency"
  expect_log "Using type com.google.sandwich.C from an indirect dependency"
}

function test_basic_java_sandwich_with_deps_should_fail() {
  mkdir -p java/com/google/sandwich
  touch java/com/google/sandwich/{BUILD,{A,B,C,Main}.java,java_custom_library.bzl}

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_binary(
  name = "Main",
  srcs = ["Main.java"],
  deps = [":custom"]
)

java_custom_library(
  name = "custom",
  srcs = ["A.java"],
  deps = [":lib-b"]
)

java_custom_library(
  name = "lib-b",
  srcs = ["B.java"],
  deps = [":lib-c"]
)

java_custom_library(
  name = "lib-c",
  srcs = ["C.java"],
)
EOF

  cat > java/com/google/sandwich/B.java << EOF
package com.google.sandwich;
class B {
  public static void print() {
    System.out.println("Message from B");
  }
}
EOF

cat > java/com/google/sandwich/C.java << EOF
package com.google.sandwich;
class C {
  public static void print() {
    System.out.println("Message from C");
  }
}
EOF

  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A {
  public static void print() {
    System.out.println("Message from A");
  }
}
EOF

  cat > java/com/google/sandwich/Main.java << EOF
package com.google.sandwich;
class Main {
  public static void main(String[] args) {
    A.print();
    B.print();
    C.print();
  }
}
EOF
  write_java_custom_rule

  bazel run java/com/google/sandwich:Main &> "$TEST_log" && fail "Java sandwich build shold have failed" || true
  expect_log "Using type com.google.sandwich.B from an indirect dependency"
  expect_log "Using type com.google.sandwich.C from an indirect dependency"
}

function test_java_merge_outputs() {
  mkdir -p java/com/google/sandwich

  cat > java/com/google/sandwich/BUILD << EOF
load(':java_custom_library.bzl', 'java_custom_library')

java_custom_library(
  name = "custom",
  srcs = ["A.java"],
  jar = "libb.jar"
)

java_library(
  name = "b",
  srcs = ["B.java"]
)
EOF

  cat > java/com/google/sandwich/B.java << EOF
package com.google.sandwich;
class B {
  public void printB() {
    System.out.println("Message from B");
  }
}
EOF

  cat > java/com/google/sandwich/A.java << EOF
package com.google.sandwich;
class A {
  public void printA() {
    System.out.println("Message from A");
  }
}
EOF

  cat > java/com/google/sandwich/java_custom_library.bzl << EOF
def _impl(ctx):
  compiled_jar = ctx.actions.declare_file("lib" + ctx.label.name + ".jar")
  imported_jar = ctx.files.jar[0];

  compilation_provider = java_common.compile(
    ctx,
    source_files = ctx.files.srcs,
    output = compiled_jar,
    java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    host_javabase = ctx.attr._host_javabase[java_common.JavaRuntimeInfo],
  )

  imported_provider = JavaInfo(output_jar = imported_jar, compile_jar = imported_jar);

  final_provider = java_common.merge([compilation_provider, imported_provider])

  print(final_provider.outputs.jars[0].class_jar)
  print(final_provider.outputs.jars[1].class_jar)

  return struct(
    files = depset([compiled_jar, imported_jar]),
    providers = [final_provider]
  )

java_custom_library = rule(
  implementation = _impl,
  attrs = {
    "srcs": attr.label_list(allow_files=True),
    "jar": attr.label(allow_files=True),
    "_java_toolchain": attr.label(default = Label("${JAVA_TOOLCHAIN}")),
    "_host_javabase": attr.label(default = Label("@bazel_tools//tools/jdk:current_host_java_runtime"))
  },
  fragments = ["java"]
)
EOF

  bazel build java/com/google/sandwich:custom &> "$TEST_log" || fail "Java sandwich build failed"
  expect_log "<generated file java/com/google/sandwich/libcustom.jar>"
  expect_log "<generated file java/com/google/sandwich/libb.jar>"
}

function test_java_info_constructor_e2e() {
  mkdir -p java/com/google/foo
  touch java/com/google/foo/{BUILD,my_rule.bzl}
  cat > java/com/google/foo/BUILD << EOF
load(":my_rule.bzl", "my_rule")
my_rule(
  name = 'my_starlark_rule',
  output_jar = 'my_starlark_rule_lib.jar',
  source_jars = ['my_starlark_rule_src.jar'],
)
EOF

  cat > java/com/google/foo/my_rule.bzl << EOF
result = provider()
def _impl(ctx):
  compile_jar = java_common.run_ijar(
    ctx.actions,
    jar = ctx.file.output_jar,
    target_label = ctx.label,
    java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
  )
  source_jar = java_common.pack_sources(
    ctx.actions,
    output_jar = ctx.file.output_jar,
    source_jars = ctx.files.source_jars,
    java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    host_javabase = ctx.attr._host_javabase[java_common.JavaRuntimeInfo],
  )
  javaInfo = JavaInfo(
    output_jar = ctx.file.output_jar,
    compile_jar = compile_jar,
    source_jar = source_jar,
  )
  return [result(property = javaInfo)]

my_rule = rule(
  implementation = _impl,
  attrs = {
    'output_jar' : attr.label(allow_single_file=True),
    'source_jars' : attr.label_list(allow_files=['.jar']),
    "_java_toolchain": attr.label(default = Label("@bazel_tools//tools/jdk:remote_toolchain")),
    "_host_javabase": attr.label(default = Label("@bazel_tools//tools/jdk:current_host_java_runtime"))
  }
)
EOF

  bazel build java/com/google/foo:my_starlark_rule >& "$TEST_log" || fail "Expected success"
}

# This test builds a simple java deploy jar using remote singlejar and ijar
# targets which compile them from source.
function test_build_hello_world_with_remote_embedded_tool_targets() {
  write_hello_library_files

  bazel build //java/main:main_deploy.jar --define EXECUTOR=remote \
    &> $TEST_log || fail "build failed"
}


function test_target_exec_properties_java() {
  cat > Hello.java << 'EOF'
public class Hello {
  public static void main(String[] args) {
    System.out.println("Hello!");
  }
}
EOF
  cat > BUILD <<'EOF'
java_binary(
  name = "a",
  srcs = ["Hello.java"],
  main_class = "Hello",
  exec_properties = {"key3": "value3", "overridden": "child_value"},
)

platform(
    name = "my_platform",
    parents = ["@local_config_platform//:host"],
    exec_properties = {
        "key2": "value2",
        "overridden": "parent_value",
        }
)
EOF
  bazel build --extra_execution_platforms=":my_platform" --toolchain_resolution_debug :a --execution_log_json_file out.txt &> $TEST_log || fail "Build failed"
  grep "key3" out.txt || fail "Did not find the target attribute key"
  grep "child_value" out.txt || fail "Did not find the overriding value"
  grep "key2" out.txt || fail "Did not find the platform key"
}


function test_current_host_java_runtime_runfiles() {
  if "$is_windows"; then
    echo "Skipping test on Windows" && return
  fi
  local -r pkg="${FUNCNAME[0]}"
  mkdir "${pkg}" || fail "Expected success"

  touch "${pkg}"/BUILD "${pkg}"/run.sh

  cat > "${pkg}"/BUILD <<EOF
sh_test(
    name = "bar",
    args = ["\$(JAVA)"],
    data = ["@bazel_tools//tools/jdk:current_host_java_runtime"],
    srcs = ["run.sh"],
    toolchains = ["@bazel_tools//tools/jdk:current_host_java_runtime"],
)
EOF

  cat > "${pkg}"/run.sh <<EOF
#!/bin/bash

set -eu

JAVA=\$1
[[ "\$JAVA" =~ ^(/|[^/]+$) ]] || JAVA="\$PWD/\$JAVA"
"\${JAVA}" -fullversion
EOF
  chmod +x "${pkg}"/run.sh

  bazel test //"${pkg}":bar --test_output=all --verbose_failures >& "$TEST_log" \
      || fail "Expected success"
}


# Build and run a java_binary that calls a C++ function through JNI.
# This test exercises the built-in @bazel_tools//tools/jdk:jni target.
#
# The java_binary wrapper script specifies -Djava.library.path=$runfiles/test,
# and the Java program expects to find a DSO there---except on MS Windows,
# which lacks support for symbolic links. Really there needs to
# be a cleaner mechanism for finding and loading the JNI library (and better
# hygiene around the library namespace). By contrast, Blaze links all the
# native code and the JVM into a single executable, which is an elegant solution.
#
function test_jni() {
  # Skip on MS Windows, as Bazel does not create a runfiles symlink tree.
  # (MSYS_NT is the system name reported by MinGW uname.)
  # TODO(adonovan): make this work.
  uname -s | grep -q MSYS_NT && return

  # Skip on Darwin, as System.loadLibrary looks for a file named
  # .dylib, not .so, and that's not what the file is called.
  # TODO(adonovan): make this just work.
  uname -s | grep -q Darwin && return

  mkdir -p test/
  cat > test/BUILD <<'EOF'
java_binary(
  name = "app",
  srcs = ["App.java"],
  main_class = 'foo.App',
  data = [":libnative.so"],
  jvm_flags = ["-Djava.library.path=test"],
)
cc_binary(
  name = "libnative.so",
  srcs = ["native.cc"],
  linkshared = 1,
  deps = ["@bazel_tools//tools/jdk:jni"],
)
EOF
  cat > test/App.java <<'EOF'
package foo;

public class App {
  static { System.loadLibrary("native"); }
  public static void main(String[] args) { f(123); }
  private static native void f(int x);
}
EOF
  cat > test/native.cc <<'EOF'
#include <jni.h>
#include <stdio.h>

extern "C" JNIEXPORT void JNICALL Java_foo_App_f(JNIEnv *env, jclass clazz, jint x) {
  printf("hello %d\n", x);
}
EOF
  bazel run //test:app > $TEST_log || {
    find bazel-bin/ | native # helpful for debugging
    fail "bazel run command failed"
  }
  expect_log "hello 123"
}

run_suite "Java integration tests"
