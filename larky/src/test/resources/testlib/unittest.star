"""Unit testing support.

Unlike most Skylib files, this exports two modules: `unittest` which contains
functions to declare and define unit tests, and `asserts` which contains the
assertions used to within tests.
"""

load("sets", "sets")
load("types", "types")


def _suite(name, *test_rules):
    """Defines a `test_suite` target that contains multiple tests.
    After defining your test rules in a `.bzl` file, you need to create targets
    from those rules so that `blaze test` can execute them. Doing this manually
    in a BUILD file would consist of listing each test in your `load` statement
    and then creating each target one by one. To reduce duplication, we recommend
    writing a macro in your `.bzl` file to instantiate all targets, and calling
    that macro from your BUILD file so you only have to load one symbol.
    For the case where your unit tests do not take any (non-default) attributes --
    i.e., if your unit tests do not test rules -- you can use this function to
    create the targets and wrap them in a single test_suite target. In your
    `.bzl` file, write:
    ```
    def your_test_suite():
      unittest.suite(
          "your_test_suite",
          your_test,
          your_other_test,
          yet_another_test,
      )
    ```
    Then, in your `BUILD` file, simply load the macro and invoke it to have all
    of the targets created:
    ```
    load("//path/to/your/package:tests.bzl", "your_test_suite")
    your_test_suite()
    ```
    If you pass _N_ unit test rules to `unittest.suite`, _N_ + 1 targets will be
    created: a `test_suite` target named `${name}` (where `${name}` is the name
    argument passed in here) and targets named `${name}_test_${i}`, where `${i}`
    is the index of the test in the `test_rules` list, which is used to uniquely
    name each target.
    Args:
      name: The name of the `test_suite` target, and the prefix of all the test
          target names.
      *test_rules: A list of test rules defines by `unittest.test`.
    """
    test_names = []
    for index, test_rule in enumerate(test_rules):
        test_name = "%s_test_%d" % (name, index)
        test_rule(name = test_name)
        test_names.append(test_name)

    native.test_suite(
        name = name,
        tests = [":%s" % t for t in test_names],
    )

def _begin(ctx):
    """Begins a unit test.
    This should be the first function called in a unit test implementation
    function. It initializes a "test environment" that is used to collect
    assertion failures so that they can be reported and logged at the end of the
    test.
    Args:
      ctx: The Starlark context. Pass the implementation function's `ctx` argument
          in verbatim.
    Returns:
      A test environment struct that must be passed to assertions and finally to
      `unittest.end`. Do not rely on internal details about the fields in this
      struct as it may change.
    """
    return struct(ctx = ctx, failures = [])

def _end(env):
    """Ends a unit test and logs the results.
    This must be called and returned at the end of a unit test implementation function so
    that the results are reported.
    Args:
      env: The test environment returned by `unittest.begin`.
    Returns:
      A list of providers needed to automatically register the test result.
    """

    tc = env.ctx.toolchains[TOOLCHAIN_TYPE].unittest_toolchain_info
    testbin = env.ctx.actions.declare_file(env.ctx.label.name + tc.file_ext)
    if env.failures:
        cmd = tc.failure_templ % tc.join_on.join(env.failures)
    else:
        cmd = tc.success_templ

    env.ctx.actions.write(
        output = testbin,
        content = cmd,
        is_executable = True,
    )
    return [DefaultInfo(executable = testbin)]

def _fail(env, msg):
    """Unconditionally causes the current test to fail.
    Args:
      env: The test environment returned by `unittest.begin`.
      msg: The message to log describing the failure.
    """
    full_msg = "In test %s: %s" % (env.ctx.attr._impl_name, msg)

    # There isn't a better way to output the message in Starlark, so use print.
    # buildifier: disable=print
    print(full_msg)
    env.failures.append(full_msg)

def _assert_true(
        env,
        condition,
        msg = "Expected condition to be true, but was false."):
    """Asserts that the given `condition` is true.
    Args:
      env: The test environment returned by `unittest.begin`.
      condition: A value that will be evaluated in a Boolean context.
      msg: An optional message that will be printed that describes the failure.
          If omitted, a default will be used.
    """
    if not condition:
        _fail(env, msg)

def _assert_false(
        env,
        condition,
        msg = "Expected condition to be false, but was true."):
    """Asserts that the given `condition` is false.
    Args:
      env: The test environment returned by `unittest.begin`.
      condition: A value that will be evaluated in a Boolean context.
      msg: An optional message that will be printed that describes the failure.
          If omitted, a default will be used.
    """
    if condition:
        _fail(env, msg)

def _assert_equals(env, expected, actual, msg = None):
    """Asserts that the given `expected` and `actual` values are equal.
    Args:
      env: The test environment returned by `unittest.begin`.
      expected: The expected value of some computation.
      actual: The actual value returned by some computation.
      msg: An optional message that will be printed that describes the failure.
          If omitted, a default will be used.
    """
    if expected != actual:
        expectation_msg = 'Expected "%s", but got "%s"' % (expected, actual)
        if msg:
            full_msg = "%s (%s)" % (msg, expectation_msg)
        else:
            full_msg = expectation_msg
        _fail(env, full_msg)

def _assert_set_equals(env, expected, actual, msg = None):
    """Asserts that the given `expected` and `actual` sets are equal.
    Args:
      env: The test environment returned by `unittest.begin`.
      expected: The expected set resulting from some computation.
      actual: The actual set returned by some computation.
      msg: An optional message that will be printed that describes the failure.
          If omitted, a default will be used.
    """
    if not new_sets.is_equal(expected, actual):
        missing = new_sets.difference(expected, actual)
        unexpected = new_sets.difference(actual, expected)
        expectation_msg = "Expected %s, but got %s" % (new_sets.str(expected), new_sets.str(actual))
        if new_sets.length(missing) > 0:
            expectation_msg += ", missing are %s" % (new_sets.str(missing))
        if new_sets.length(unexpected) > 0:
            expectation_msg += ", unexpected are %s" % (new_sets.str(unexpected))
        if msg:
            full_msg = "%s (%s)" % (msg, expectation_msg)
        else:
            full_msg = expectation_msg
        _fail(env, full_msg)

_assert_new_set_equals = _assert_set_equals

def _expect_failure(env, expected_failure_msg = ""):
    """Asserts that the target under test has failed with a given error message.
    This requires that the analysis test is created with `analysistest.make()` and
    `expect_failures = True` is specified.
    Args:
      env: The test environment returned by `analysistest.begin`.
      expected_failure_msg: The error message to expect as a result of analysis failures.
    """
    dep = _target_under_test(env)
    if AnalysisFailureInfo in dep:
        actual_errors = ""
        for cause in dep[AnalysisFailureInfo].causes.to_list():
            actual_errors += cause.message + "\n"
        if actual_errors.find(expected_failure_msg) < 0:
            expectation_msg = "Expected errors to contain '%s' but did not. " % expected_failure_msg
            expectation_msg += "Actual errors:%s" % actual_errors
            _fail(env, expectation_msg)
    else:
        _fail(env, "Expected failure of target_under_test, but found success")

def _target_under_test(env):
    """Returns the target under test.
    Args:
      env: The test environment returned by `analysistest.begin`.
    Returns:
      The target under test.
    """
    result = getattr(env.ctx.attr, "target_under_test")
    if types.is_list(result):
        if result:
            return result[0]
        else:
            fail("test rule does not have a target_under_test")
    return result

asserts = struct(
    expect_failure = _expect_failure,
    equals = _assert_equals,
    false = _assert_false,
    set_equals = _assert_set_equals,
    new_set_equals = _assert_new_set_equals,
    true = _assert_true,
)

unittest = struct(
    make = _make,
    suite = _suite,
    begin = _begin,
    end = _end,
    fail = _fail,
)
