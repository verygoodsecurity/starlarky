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

# --- begin runfiles.bash initialization v2 ---
# Copy-pasted from the Bazel Bash runfiles library v2.
set -uo pipefail; f=bazel_tools/tools/bash/runfiles/runfiles.bash
source "${RUNFILES_DIR:-/dev/null}/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "${RUNFILES_MANIFEST_FILE:-/dev/null}" | cut -f2- -d' ')" 2>/dev/null || \
  source "$0.runfiles/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.exe.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  { echo>&2 "ERROR: cannot find $f"; exit 1; }; f=; set -e
# --- end runfiles.bash initialization v2 ---

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

add_to_bazelrc "build --package_path=%workspace%"

function assert_only_action_foo() {
  expect_log_n "^action '" 1 "Expected exactly one action."
  assert_contains "action.*foo" $1
  assert_not_contains "action.*wrong_input" $1
  assert_not_contains "action.*wrong_output" $1
  assert_not_contains "action.*wrong_mnemonic" $1

  assert_contains "Inputs: \[.*foo_matching_in.java.*\]" $1
  assert_contains "Outputs: \[.*foo_matching_out.*\]" $1
  assert_contains "Mnemonic: Genrule" $1

  return 0
}

function assert_only_action_foo_textproto() {
  expect_log_once "actions {"
  assert_contains "input_dep_set_ids: \"0\"" $1
  assert_contains "output_ids: \"2\"" $1
  assert_contains "mnemonic: \"Genrule\"" $1
  return 0
}

function test_basic_aquery() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = [":bar"],
    outs = ["foo_out.txt"],
    cmd = "cat $(SRCS) > $(OUTS)",
)

genrule(
    name = "bar",
    srcs = ["dummy.txt"],
    outs = ["bar_out.txt"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  echo "hello aquery" > "$pkg/in.txt"

  bazel aquery "//$pkg:foo" > output 2> "$TEST_log" || fail "Expected success"
  assert_contains "//$pkg:foo" output
  assert_not_contains "//$pkg:bar" output

  bazel aquery "deps(//$pkg:foo)" > output 2> "$TEST_log" \
    || fail "Expected success"
  assert_contains "//$pkg:foo" output
  assert_contains "//$pkg:bar" output
}

function test_aquery_text() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "bar",
    srcs = ["dummy.txt"],
    outs = ["bar_out.txt"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  echo "hello aquery" > "$pkg/in.txt"

  bazel aquery --output=text "//$pkg:bar" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  assert_contains "action 'Executing genrule //$pkg:bar'" output
  assert_contains "Mnemonic: Genrule" output
  assert_contains "Target: //$pkg:bar" output
  assert_contains "Configuration: .*-fastbuild" output
  # Only check that the inputs/outputs/command line/environment exist, but not
  # their actual contents since that would be too much.
  assert_contains "Inputs: \[" output
  assert_contains "Outputs: \[" output
  if $is_windows; then
    assert_contains "Command Line: .*bash\.exe" output
  else
    assert_contains "Command Line: (" output
  fi

  assert_contains "echo unused" output
  bazel aquery --output=text --noinclude_commandline "//$pkg:bar" > output \
    2> "$TEST_log" || fail "Expected success"
  assert_not_contains "echo unused" output
}

function test_aquery_include_artifacts() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "bar",
    srcs = ["dummy.txt"],
    outs = ["bar_out.txt"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  echo "hello aquery" > "$pkg/in.txt"

  bazel aquery --include_artifacts "//$pkg:bar" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_contains "Inputs: \[" output
  assert_contains "Outputs: \[" output

  bazel aquery --noinclude_artifacts "//$pkg:bar" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_not_contains "Inputs: \[" output
  assert_not_contains "Outputs: \[" output
}

function test_aquery_textproto() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "bar",
    srcs = ["dummy.txt"],
    outs = ["bar_out.txt"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  echo "hello aquery" > "$pkg/in.txt"

  bazel aquery --output=textproto "//$pkg:bar" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  assert_contains "exec_path: \"$pkg/dummy.txt\"" output
  assert_contains "nemonic: \"Genrule\"" output
  assert_contains "mnemonic: \".*-fastbuild\"" output
  assert_contains "echo unused" output

  bazel aquery --output=textproto --noinclude_commandline "//$pkg:bar" > output \
    2> "$TEST_log" || fail "Expected success"
  assert_not_contains "echo unused" output
}

function test_aquery_jsonproto() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "bar",
    srcs = ["dummy.txt"],
    outs = ["bar_out.txt"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  echo "hello aquery" > "$pkg/in.txt"

  bazel aquery --output=jsonproto "//$pkg:bar" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  assert_contains "\"execPath\": \"$pkg/dummy.txt\"" output
  assert_contains "\"mnemonic\": \"Genrule\"" output
  assert_contains "\"mnemonic\": \".*-fastbuild\"" output
  assert_contains "echo unused" output

  bazel aquery --output=jsonproto --noinclude_commandline "//$pkg:bar" > output \
    2> "$TEST_log" || fail "Expected success"
  assert_not_contains "echo unused" output
}

function test_aquery_starlark_env() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/rule.bzl" <<'EOF'
def _impl(ctx):
    output = ctx.outputs.out
    input = ctx.file.source
    env = {}
    env["foo"] = "bar"

    ctx.actions.run_shell(
        inputs = [input],
        outputs = [output],
        command = "cat '%s' > '%s'" % (input.path, output.path),
        env = env,
    )

copy = rule(
    implementation = _impl,
    attrs = {"source": attr.label(mandatory = True, allow_single_file = True)},
    outputs = {"out": "%{name}.copy"},
)
EOF

  cat > "$pkg/BUILD" <<'EOF'
load(":rule.bzl", "copy")
copy(
    name = "goo",
    source = "dummy.txt",
)
EOF
  echo "hello aquery" > "$pkg/dummy.txt"

  bazel aquery --output=text "//$pkg:goo" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  assert_contains "Mnemonic: Action" output
  assert_contains "Target: //$pkg:goo" output
  assert_contains "Environment: \[.*foo=bar" output
}

function test_aquery_aspect() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/foobar.bzl" <<'EOF'
DummyProvider = provider(fields = {'dummies' : 'files'})

def _aspect_impl(target, ctx):
    ins = []
    if hasattr(ctx.rule.attr, 'srcs'):
        ins = depset(transitive = [src.files for src in ctx.rule.attr.srcs]).to_list()
    dummy = ctx.actions.declare_file("%s-aspect" % (target.label.name))
    all_dummies = depset([dummy], transitive = [dep[DummyProvider].dummies for dep in ctx.rule.attr.deps])
    ctx.actions.run_shell(inputs = ins, outputs = [dummy], command = "echo {} > {}".format(ctx.attr.bar, dummy.path))
    return [DummyProvider(dummies = all_dummies)]

bar_aspect = aspect(implementation = _aspect_impl,
    attr_aspects = ['deps'],
    attrs = {
        'bar' : attr.string(values = ['one', 'two', 'three']),
    }
)

def _bar_impl(ctx):
    ctx.actions.write(content = "hello world", output = ctx.outputs.out)
    return struct(files = depset(transitive = [dep[DummyProvider].dummies for dep in ctx.attr.deps]))

bar_rule = rule(
    implementation = _bar_impl,
    attrs = {
        'deps' : attr.label_list(aspects = [bar_aspect]),
        'bar' : attr.string(default = 'two'),
    },
    outputs = {"out": "%{name}.count"},
)

def _foo_library_impl(ctx):
    ctx.actions.run_shell(
        command = "touch {}".format(ctx.outputs.out.path),
        inputs = ctx.files.srcs,
        outputs = [ctx.outputs.out],
    )

foo_library = rule(
    implementation = _foo_library_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "deps": attr.label_list(),
    },
    outputs = {"out": "%{name}.foo_object"},
)
EOF
  cat > "$pkg/BUILD" <<'EOF'
load(":foobar.bzl", "bar_rule", "foo_library")

foo_library(
    name = "a",
    srcs = ["a.foo"],
)

foo_library(
    name = "b",
    srcs = ["b.foo"],
    deps = [":a"],
)

foo_library(
    name = "c",
    srcs = ["c.foo"],
    deps = [":b"],
)

bar_rule(
    name = 'bar',
    deps = [':c'],
    bar = 'three',
)
EOF

  # Test without considering aspects.
  bazel aquery --output=text "//$pkg:a" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  expect_log_n "^action '" 1 "Expected exactly one action when not considering aspects."
  assert_not_contains "AspectDescriptors" output

  # Test considering aspects but without triggering it.
  bazel aquery --include_aspects --output=text "//$pkg:a" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  expect_log_n "^action '" 1 "Expected exactly one action without universe scope."
  assert_not_contains "AspectDescriptors" output

  # Trigger the aspect.
  bazel aquery --include_aspects --output=text "//$pkg:a" --universe_scope="//$pkg:bar" \
    > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  expect_log_n "^action '" 2 "Expected exactly two actions."

  assert_contains "AspectDescriptors: \[.*foobar.bzl%bar_aspect.*bar='three'" output
  assert_contains "Outputs: \[.*a.foo_object" output
  assert_contains "Outputs: \[.*a-aspect" output
}

function test_aquery_all_filters_only_match_foo() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)

genrule(
    name = "wrong_input",
    srcs = ["wrong_input.java"],
    outs = ["wi_matching_out"],
    cmd = "echo unused > $(OUTS)",
)

genrule(
    name = "wrong_output",
    srcs = ["wo_matching_in.java"],
    outs = ["wrong_out"],
    cmd = "echo unused > $(OUTS)",
)

java_library(
    name = "wrong_mnemonic",
    srcs = ["wm_matching_in.java"],
)
EOF

  QUERY="inputs(
    '.*matching_in.java', outputs('.*matching_out', mnemonic('Genrule', //$pkg:all)))"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_inputs_filter_only_mach_foo() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)

genrule(
    name = "wrong_input",
    srcs = ["wrong_input.java"],
    outs = ["wi_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  QUERY="inputs('.*matching_in.java', //$pkg:all)"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_outputs_filter_only_mach_foo() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)

genrule(
    name = "wrong_output",
    srcs = ["wo_matching_in.java"],
    outs = ["wrong_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  QUERY="outputs('.*matching_out', //$pkg:all)"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_mnemonic_filter_only_mach_foo() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)

java_library(
    name = "wrong_mnemonic",
    srcs = ["wm_matching_in.java"],
)
EOF

  QUERY="mnemonic('.*rule', //$pkg:all)"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_inputs_filter_exact_filename_only_mach_foo() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)

genrule(
    name = "wrong_input",
    srcs = ["wrong_input.java"],
    outs = ["wi_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  QUERY="inputs('$pkg/foo_matching_in.java', //$pkg:all)"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_outputs_filter_exact_filename_only_mach_foo() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)

genrule(
    name = "wrong_output",
    srcs = ["wo_matching_in.java"],
    outs = ["wrong_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  QUERY="outputs('.*/$pkg/foo_matching_out', //$pkg:all)"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_mnemonic_filter_exact_mnemonic_only_mach_foo() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)

java_library(
    name = "wrong_mnemonic",
    srcs = ["wm_matching_in.java"],
)
EOF

  QUERY="mnemonic('Genrule', //$pkg:all)"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_filters_non_aquery_enclosing_function_query_error() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  QUERY="deps(inputs('.*matching_in.java', outputs('.*matching_out', mnemonic('Genrule', //$pkg:all))))"
  EXPECTED_LOG="ERROR: aquery filter functions (inputs, outputs, mnemonic) produce actions, and therefore can't be the input of other function types: deps
${QUERY}"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    && fail "Expected failure"
  expect_log "${EXPECTED_LOG}"
}

function test_aquery_filters_chain_inputs_only_match_one() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name='foo',
    srcs=['foo_matching_in.java'],
    outs=['foo_matching_out'],
    cmd='cat $(SRCS) > $(OUTS)'
)

java_library(
    name='bar',
    srcs=['in_bar.java']
)

genrule(
    name='foo2',
    srcs=['foo_matching_in.java_not'],
    outs=['foo_matching_out2'],
    cmd='cat $(SRCS) > $(OUTS)'
)
EOF

  QUERY="inputs('.*java', inputs('.*foo.*', //$pkg:all))"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_filters_chain_outputs_only_match_one() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name='foo',
    srcs=['foo_matching_in.java'],
    outs=['foo_matching_out'],
    cmd='cat $(SRCS) > $(OUTS)'
)

genrule(
    name='foo2',
    srcs=['foo_matching_in.java'],
    outs=['foo_matching_out_not'],
    cmd='cat $(SRCS) > $(OUTS)'
)

genrule(
    name='foo3',
    srcs=['foo_matching_in.java'],
    outs=['not_matching_out'],
    cmd='cat $(SRCS) > $(OUTS)'
)
EOF

  QUERY="outputs('.*out', outputs('.*foo.*', //$pkg:all))"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_filters_chain_mnemonic_only_match_one() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
java_library(
    name='foo',
    srcs=['Foo.java']
)
EOF

  # java_library targets generate actions of the following mnemonics:
  # - Javac
  # - JavaSourceJar
  # - Turbine
  QUERY="mnemonic('Java.*', mnemonic('.*e.*', //$pkg:all))"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  expect_log_n "^action '" 1 "Expected exactly one action."
  assert_contains "action.*foo" output
  assert_contains "Inputs: \[.*Foo.java.*\]" output
  assert_contains "Outputs: \[.*jar\]" output
  assert_contains "Mnemonic: JavaSourceJar" output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_include_param_file_cc_binary() {
  if is_darwin; then
    return 0
  fi

  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
cc_binary(
    name='foo',
    srcs=['foo.cc']
)
EOF

  # cc_binary targets write param files and use them in CppLinkActions.
  QUERY="//$pkg:foo"

  bazel aquery --output=text --include_param_files ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_contains "Command Line: " output
  assert_contains "Params File Content (.*-2.params):" output

  bazel aquery --output=textproto --include_param_files ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_include_param_file_starlark_rule() {
  if is_darwin; then
    return 0
  fi

  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/test_rule.bzl" <<'EOF'
def _impl(ctx):
  args = ctx.actions.args()
  args.add('--param_file_arg')
  args.set_param_file_format('multiline')
  args.use_param_file('--param_file=%s', use_always = True)
  ctx.actions.run(
    inputs = ctx.files.srcs,
    outputs = [ctx.outputs.outfile],
    executable = 'dummy',
    arguments = ['--non-param-file-flag', args],
    mnemonic = 'Action'
  )

test_rule = rule(
  implementation = _impl,
  attrs = {
    'srcs': attr.label_list(allow_files=True)
  },
  outputs = {
    'outfile': '{name}.out'
  },
)
EOF

cat > "$pkg/BUILD" <<'EOF'
load(':test_rule.bzl', 'test_rule')
test_rule(
    name='foo',
    srcs=['foo.java']
)
EOF

  # Actions from Starlark rules don't explicitly write out param file,
  # but includes all arguments (including those in the param file) in the command line.
  QUERY="//$pkg:foo"

  bazel aquery --output=text --include_param_files ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_not_contains "Params File Content" output

  bazel aquery --output=textproto --include_param_files ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_include_param_file_not_enabled_by_default() {
  if is_darwin; then
    return 0
  fi

  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
cc_binary(
    name='foo',
    srcs=['foo.cc']
)
EOF

  # cc_binary targets write param files and use them in CppLinkActions.
  QUERY="//$pkg:foo"

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_not_contains "Params File Content" output

  bazel aquery --output=textproto ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_cpp_action_template_treeartifact_output() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/a.bzl" <<'EOF'
def _impl(ctx):
  directory = ctx.actions.declare_directory(ctx.attr.name + "_artifact.cc")
  ctx.actions.run_shell(
    inputs = ctx.files.srcs,
    outputs = [directory],
    mnemonic = 'MoveTreeArtifact',
    command = "echo abc",
  )
  return [DefaultInfo(files = depset([directory]))]

cc_tree_artifact_files = rule(
  implementation = _impl,
  attrs = {
    'srcs': attr.label_list(allow_files=True),
  },
)
EOF

  cat > "$pkg/BUILD" <<'EOF'
load(':a.bzl', 'cc_tree_artifact_files')
cc_tree_artifact_files(
    name = 'tree_artifact',
    srcs = ['a1.cc', 'a2.cc'],
)

cc_binary(
    name = 'bin',
    srcs = ['b1.h', 'b2.cc', ':tree_artifact'],
)
EOF

  QUERY="//$pkg:all"

  # Darwin and Windows only produce 1 CppCompileActionTemplate with PIC,
  # while Linux has both PIC and non-PIC CppCompileActionTemplates
  if (is_darwin || $is_windows); then
    expected_num_actions=1
  else
    expected_num_actions=2
  fi

  bazel aquery --output=text ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  expect_log_n "CppCompileActionTemplate compiling .*.cc" $expected_num_actions "Expected exactly $expected_num_actions CppCompileActionTemplates."

  assert_contains "Outputs:.*tree_artifact_artifact (TreeArtifact)\]$" output

  bazel aquery --output=textproto --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_aspect_on_aspect() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/rule.bzl" <<'EOF'
IntermediateProvider = provider(
  fields = {
    'dummy_field': 'dummy field'
  }
)

MyBaseRuleProvider = provider(
  fields = {
    'dummy_field': 'dummy field'
  }
)

def _base_rule_impl(ctx):
  return [MyBaseRuleProvider(dummy_field = 1)]

base_rule = rule(
  attrs = {
    'srcs': attr.label_list(allow_files = True),
  },
  implementation = _base_rule_impl
)

def _intermediate_aspect_imp(target, ctx):
  if hasattr(ctx.rule.attr, 'srcs'):
    out = ctx.actions.declare_file('out_jpl_{}'.format(target))
    ctx.actions.run(
      inputs = [f for src in ctx.rule.attr.srcs for f in src.files.to_list()],
      outputs = [out],
      executable = 'dummy',
      mnemonic = 'MyIntermediateAspect'
    )

  return [IntermediateProvider(dummy_field = 1)]

intermediate_aspect = aspect(
  attr_aspects = ['deps', 'exports'],
  required_aspect_providers = [[MyBaseRuleProvider]],
  provides = [IntermediateProvider],
  implementation = _intermediate_aspect_imp,
)

def _int_rule_impl(ctx):
  return struct()

intermediate_rule = rule(
  attrs = {
    'deps': attr.label_list(aspects = [intermediate_aspect]),
    'srcs': attr.label_list(allow_files = True),
  },
  implementation = _int_rule_impl
)

def _aspect_impl(target, ctx):
  if hasattr(ctx.rule.attr, 'srcs'):
    out = ctx.actions.declare_file('out{}'.format(target))
    ctx.actions.run(
      inputs = [f for src in ctx.rule.attr.srcs for f in src.files.to_list()],
      outputs = [out],
      executable = 'dummy',
      mnemonic = 'MyAspect'
    )

  return [struct()]

my_aspect = aspect(
  attr_aspects = ['deps', 'exports'],
  required_aspect_providers = [[IntermediateProvider], [MyBaseRuleProvider]],
  attrs = {
    'aspect_param': attr.string(default = 'x', values = ['x', 'y'])
  },
  implementation = _aspect_impl,
)

def _rule_impl(ctx):
  return struct()

my_rule = rule(
  attrs = {
    'deps': attr.label_list(aspects = [my_aspect]),
    'srcs': attr.label_list(allow_files = True),
    'aspect_param': attr.string(default = 'x', values = ['x', 'y'])
  },
  implementation = _rule_impl
)
EOF

  cat > "$pkg/BUILD" <<'EOF'
load(':rule.bzl', 'my_rule', 'intermediate_rule', 'base_rule')

base_rule(
    name = 'x',
    srcs = [':x.java'],
)

intermediate_rule(
    name = 'int_target',
    deps = [':x'],
)

my_rule(
    name = 'my_target',
    srcs = ['foo.java'],
    aspect_param = 'y',
    deps = [':int_target'],
)
EOF

  QUERY="deps(//$pkg:my_target)"

  bazel aquery --output=text --include_aspects ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_contains "Mnemonic: MyAspect" output
  assert_contains "AspectDescriptors: \[.*:rule.bzl%my_aspect(aspect_param='y')" output
  assert_contains "^.*->.*:rule.bzl%intermediate_aspect()\]$" output

  bazel aquery --output=textproto --include_aspects --noinclude_commandline ${QUERY} > output \
    2> "$TEST_log" || fail "Expected success"
}

function test_aquery_skyframe_state_no_filter_no_previous_build_empty() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"

  bazel clean

  bazel aquery --output=textproto --skyframe_state > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_not_contains "actions" output
}

function test_aquery_skyframe_state_wrong_syntax() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"

  EXPECTED_LOG="Specifying build target(s) \[//some_target\] with --skyframe_state is currently not supported"
  bazel aquery --output=textproto --skyframe_state "//some_target" > output 2> "$TEST_log" \
    && fail "Expected failure"
  expect_log "${EXPECTED_LOG}"

  bazel aquery --output=textproto --skyframe_state "inputs('pattern', //some_target)" > output 2> "$TEST_log" \
    && fail "Expected failure"
  expect_log "${EXPECTED_LOG}"
}

function test_aquery_skyframe_state_no_filter_with_previous_build() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  bazel clean

  bazel aquery --output=textproto --skyframe_state > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  assert_not_contains "actions" output

  bazel build --nobuild "//$pkg:foo"

  bazel aquery --output=textproto --skyframe_state > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo_textproto output
}

function test_aquery_skyframe_state_with_filter_with_previous_build() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)

genrule(
    name = "wrong_input",
    srcs = ["wrong_input.java"],
    outs = ["wi_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  QUERY="inputs('.*matching_in.java', outputs('.*matching_out', mnemonic('Genrule')))"

  bazel clean
  bazel build --nobuild "//$pkg:foo"

  bazel aquery --output=textproto --skyframe_state ${QUERY} > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  assert_only_action_foo_textproto output
}

function test_basic_aquery_proto_v2() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "bar",
    srcs = ["dummy.txt"],
    outs = ["bar_out.txt"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  bazel aquery --output=proto "//$pkg:bar" > output_v1 || fail "Expected success"
  bazel clean

  bazel aquery --incompatible_proto_output_v2 --output=proto "//$pkg:bar" > output_v2 2> "$TEST_log" \
    || fail "Expected success"
  [[ output_v1 != output_v2 ]] || fail "proto content should be different."
}

function test_basic_aquery_textproto_v2() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "bar",
    srcs = ["dummy.txt"],
    outs = ["bar_out.txt"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  bazel aquery --incompatible_proto_output_v2 --output=proto "//$pkg:bar" \
    || fail "Expected success"

  bazel aquery --incompatible_proto_output_v2 --output=textproto "//$pkg:bar" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  # Verify than ids come in integers instead of strings.
  assert_contains "id: 1" output
  assert_not_contains "id: \"1\"" output

  # Verify that paths are broken down to path fragments.
  assert_contains "path_fragments {" output
  assert_contains "primary_output_id" output
  # Verify that the appropriate action was included.
  assert_contains "label: \"dummy.txt\"" output
  assert_contains "mnemonic: \"Genrule\"" output
  assert_contains "mnemonic: \".*-fastbuild\"" output
  assert_contains "echo unused" output
}

function test_basic_aquery_jsonproto_v2() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "bar",
    srcs = ["dummy.txt"],
    outs = ["bar_out.txt"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  bazel aquery --incompatible_proto_output_v2 --output=jsonproto "//$pkg:bar" > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  # Verify than ids come in integers instead of strings.
  assert_contains "\"id\": 1," output
  assert_not_contains "\"id\": \"1\"" output

  # Verify that paths are broken down to path fragments.
  assert_contains "\"pathFragments\": \[{" output

  # Verify that the appropriate action was included.
  assert_contains "\"label\": \"dummy.txt\"" output
  assert_contains "\"mnemonic\": \"Genrule\"" output
  assert_contains "\"mnemonic\": \".*-fastbuild\"" output
  assert_contains "echo unused" output
}

function test_aquery_textproto_v2_skyframe_state() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  bazel clean

  bazel aquery --incompatible_proto_output_v2 --output=textproto --skyframe_state > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  assert_not_contains "actions" output

  bazel build --nobuild "//$pkg:foo"

  bazel aquery --incompatible_proto_output_v2 --output=textproto --skyframe_state > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  expect_log_once "actions {"
  assert_contains "input_dep_set_ids: 1" output
  assert_contains "output_ids: 3" output
  assert_contains "mnemonic: \"Genrule\"" output
}

function test_aquery_json_v2_skyframe_state() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  bazel clean

  bazel aquery --incompatible_proto_output_v2 --output=jsonproto --skyframe_state > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"
  assert_not_contains "actions" output

  bazel build --nobuild "//$pkg:foo"

  bazel aquery --incompatible_proto_output_v2 --output=jsonproto --skyframe_state > output 2> "$TEST_log" \
    || fail "Expected success"
  cat output >> "$TEST_log"

  expect_log_once "\"actionKey\":"
}

function test_aquery_json_v2_skyframe_state_invalid_format() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF

  bazel clean

  bazel aquery --incompatible_proto_output_v2 --output=text --skyframe_state &> "$TEST_log" \
    && fail "Expected failure"
  expect_log "--skyframe_state must be used with --output=proto\|textproto\|jsonproto. Invalid aquery output format: text"
}

function test_dump_skyframe_state_after_build_default_output() {
    local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  touch $pkg/foo_matching_in.java

  bazel clean
  OUTPUT_BASE=$(bazel info output_base)
  bazel build --experimental_aquery_dump_after_build_format=textproto "//$pkg:foo" &> "$TEST_log" \
    || fail "Expected success"

  assert_contains "actions {" "$OUTPUT_BASE/aquery_dump.textproto"
  assert_contains "input_dep_set_ids: 1" "$OUTPUT_BASE/aquery_dump.textproto"
  assert_contains "output_ids: 3" "$OUTPUT_BASE/aquery_dump.textproto"
  assert_contains "mnemonic: \"Genrule\"" "$OUTPUT_BASE/aquery_dump.textproto"
}

function test_dump_skyframe_state_after_build_to_specified_file() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  touch $pkg/foo_matching_in.java

  bazel clean

  bazel build --experimental_aquery_dump_after_build_format=textproto --experimental_aquery_dump_after_build_output_file="$TEST_TMPDIR/foo.out" "//$pkg:foo" \
    &> "$TEST_log" || fail "Expected success"

  assert_contains "actions {" "$TEST_TMPDIR/foo.out"
  assert_contains "input_dep_set_ids: 1" "$TEST_TMPDIR/foo.out"
  assert_contains "output_ids: 3" "$TEST_TMPDIR/foo.out"
  assert_contains "mnemonic: \"Genrule\"" "$TEST_TMPDIR/foo.out"
}

function test_dump_skyframe_state_after_build_invalid_format() {
  local pkg="${FUNCNAME[0]}"
  mkdir -p "$pkg" || fail "mkdir -p $pkg"
  cat > "$pkg/BUILD" <<'EOF'
genrule(
    name = "foo",
    srcs = ["foo_matching_in.java"],
    outs = ["foo_matching_out"],
    cmd = "echo unused > $(OUTS)",
)
EOF
  touch $pkg/foo_matching_in.java

  bazel clean

  bazel build --experimental_aquery_dump_after_build_format=text --experimental_aquery_dump_after_build_output_file="$TEST_TMPDIR/foo.out" "//$pkg:foo" \
    &> "$TEST_log" && fail "Expected success"
  expect_log "--skyframe_state must be used with --output=proto\|textproto\|jsonproto. Invalid aquery output format: text"
}

run_suite "${PRODUCT_NAME} action graph query tests"
