---
layout: documentation
title: Macros
---

# Macros

<!-- [TOC] -->

## Introduction

A macro is a function called from the BUILD file that can instantiate rules.
Macros are mainly used for encapsulation and code reuse of existing rules
and other macros. By the end of the
[loading phase](concepts.md#evaluation-model), macros don't exist anymore,
and Bazel sees only the concrete set of instantiated rules.

## Usage

The typical use-case for a macro is when you want to reuse a rule.

For example, we have a genrule in a BUILD file that generates a file
using `//:generator` with a `some_arg` argument hardcoded in the command:

```python
genrule(
    name = "file",
    outs = ["file.txt"],
    cmd = "$(location //:generator) some_arg > $@",
    tools = ["//:generator"],
)
```

> Tip: `$@` is a [Make variable](../be/make-variables.html#predefined_genrule_variables)
> that refers to the execution-time locations of the files in the `outs` attribute list.
> It is equivalent to `$(locations :file.txt)`.

If you want to generate more files with different arguments, you may want to
extract this code to a macro function. Let's call the macro `file_generator`, which
has `name` and `arg` parameters. Replace the genrule with the following:

```python
load("//path:generator.bzl", "file_generator")

file_generator(
    name = "file",
    arg = "some_arg",
)

file_generator(
    name = "file-two",
    arg = "some_arg_two",
)

file_generator(
    name = "file-three",
    arg = "some_arg_three",
)
```

Here, we are loading the `file_generator` symbol from a `.bzl` file located
in the `//path` package. By putting macro function definitions in a separate
`.bzl` file, we can keep our BUILD files clean and declarative, The `.bzl`
file can be loaded from any package in the workspace.

Finally, in `path/generator.bzl`, let's write the definition of the macro to
encapsulate and parameterize our original genrule definition:

```python
def file_generator(name, arg, visibility=None):
  native.genrule(
    name = name,
    outs = [name + ".txt"],
    cmd = "$(location //:generator) %s > $@" % arg,
    tools = ["//:generator"],
    visibility = visibility,
  )
```

You can also use macros to chain rules together. This example shows chained
genrules, where a genrule uses the outputs of a previous genrule as inputs:

```python
def chained_genrules(name, visibility=None):
  native.genrule(
    name = name + "-one",
    outs = [name + ".one"],
    cmd = "$(location :tool-one) $@",
    tools = [":tool-one"],
    visibility = ["//visibility:private"],
  )

  native.genrule(
    name = name + "-two",
    srcs = [name + ".one"],
    outs = [name + ".two"],
    cmd = "$(location :tool-two) $< $@",
    tools = [":tool-two"],
    visibility = visibility,
  )
```

Note that we only assigned the value of `visibility` to the second genrule.
This enables macro authors hide outputs of intermediate rules from being
depended upon by other targets in the workspace.

> Tip: Similar to `$@` for outputs, `$<` expands to the locations of files in
the `srcs` attribute list.

## Expanding macros

When you want to investigate what a macro does, use the `query` command with
`--output=build` to see the expanded form:

```
$ bazel query --output=build :file
# /absolute/path/test/ext.bzl:42:3
genrule(
  name = "file",
  tools = ["//:generator"],
  outs = ["//test:file.txt"],
  cmd = "$(location //:generator) some_arg > $@",
)
```

## Instantiating native rules

Native rules (i.e. rules that don't need a `load()` statement) can be
instantiated from the [native](lib/native.html) module, e.g.

```python
def my_macro(name, visibility=None):
  native.cc_library(
    name = name,
    srcs = ["main.cc"],
    visibility = visibility,
  )
```

If you need to know the package name (i.e. which BUILD file is calling the
macro), use the function [native.package_name()](lib/native.html#package_name).

## Debugging

*   `bazel query --output=build //my/path:all` will show you how the BUILD file
    looks after evaluation. All macros, globs, loops are expanded. Known
    limitation: `select` expressions are currently not shown in the output.

*   You may filter the output based on `generator_function` (which function
    generated the rules) or `generator_name` (the name attribute of the macro),
    e.g.
    ```bash
    $ bazel query --output=build 'attr(generator_function, my_macro, //my/path:all)'
    ```

*   To find out where exactly the rule `foo` is generated in a BUILD file, you
    can try the following trick. Insert this line near the top of the BUILD
    file: `cc_library(name = "foo")`. Run Bazel. You will get an exception when
    the rule `foo` is created (due to a name conflict), which will show you the
    full stack trace.

*   You can also use [print](lib/globals.html#print) for debugging. It displays
    the message as a `DEBUG` log line during the loading phase. Except in rare
    cases, either remove `print` calls, or make them conditional under a
    `debugging` parameter that defaults to `False` before submitting the code to
    the depot.

## Errors

If you want to throw an error, use the [fail](lib/globals.html#fail) function.
Explain clearly to the user what went wrong and how to fix their BUILD file. It
is not possible to catch an error.

```python
def my_macro(name, deps, visibility=None):
  if len(deps) < 2:
    fail("Expected at least two values in deps")
  # ...
```

## Conventions

*   All public functions (functions that don't start with underscore) that
    instantiate rules must have a `name` argument. This argument should not be
    optional (don't give a default value).

*   Public functions should use a docstring following [Python
    conventions](https://www.python.org/dev/peps/pep-0257/#one-line-docstrings).

*   In BUILD files, the `name` argument of the macros must be a keyword argument
    (not a positional argument).

*   The `name` attribute of rules generated by a macro should include the name
    argument as a prefix. For example, `macro(name = "foo")` can generate a
    `cc_library` `foo` and a genrule `foo_gen`.

*   In most cases, optional parameters should have a default value of `None`.
    `None` can be passed directly to native rules, which treat it the same as if
    you had not passed in any argument. Thus, there is no need to replace it
    with `0`, `False`, or `[]` for this purpose. Instead, the macro should defer
    to the rules it creates, as their defaults may be complex or may change over
    time. Additionally, a parameter that is explicitly set to its default value
    looks different than one that is never set (or set to `None`) when accessed
    through the query language or build-system internals.

*   Macros should have an optional `visibility` argument.

