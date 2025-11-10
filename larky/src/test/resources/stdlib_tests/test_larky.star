"""Unit tests for larky.star."""
load("@stdlib//larky", "larky")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts",  "asserts")


def _test_namespace_exposes_larky_builtins():
    """
    This unit test really is testing to make sure that we are tracking
    that everything we need exported from `@stdlib/larky` to ensure that
    we do not break dependencies.

    :return: None
    """
    items = sorted(dir(larky))
    asserts.assert_that(items).is_length(19)
    asserts.assert_that(items).is_equal_to(sorted([
        "SENTINEL",
        "mutablestruct",
        "partial",
        "property",
        "struct",
        "to_dict",
        "WHILE_LOOP_EMULATION_ITERATION",
        "parametrize",
        "is_instance",
        "translate_bytes",
        "strings",
        "utils",
        "__dict__",
        "impl_function_name",
        "DeterministicGenerator",
        "dicts",
        "is_subclass",
        "type_cls",
        "while_true"
    ]))


def _test_while_true_basic():
    """Test basic _while_true functionality"""
    count = 0
    for _while_ in larky.while_true("test error"):
        count += 1
        if count >= 5:
            break
    asserts.assert_that(count).is_equal_to(5)


def _test_while_true_interrupted_with_custom_exception():
    """Test _while_true with custom error message"""
    def _test_loop():
        count = 0
        for _while_ in larky.while_true():
            count += 1
            if count >= 10:  # This should trigger the error
                fail("Iterable failed with error message")

    asserts.assert_fails(_test_loop, ".*Iterable failed with error message.*")


def _test_while_true_repr():
    """Test _while_true repr"""
    asserts.assert_that(repr(larky.while_true())).is_equal_to("bounded_while_true_iterator(limit_exceed_msg=\"Iteration limit exceeded! Loop bound (larky.WHILE_LOOP_EMULATION_ITERATION=16384) has been reached. Either increase the bound (larky.WHILE_LOOP_EMULATION_ITERATION=$NUMBER) OR chunk the work block to operate within the loop bound limit.\", max_iterations=16384)")


def _test_while_true_interrupted_with_globally_defined_limit():
    """Test _while_true with custom error message"""
    counter_container = []
    def _test_loop(counter_container):
        for _while_ in larky.while_true():
            counter_container.append(b'')
        fail("Should not have arrived here!")
    asserts.assert_fails(lambda: _test_loop(counter_container), ".*Iteration limit exceeded! Loop bound \\(larky.WHILE_LOOP_EMULATION_ITERATION=\\d+\\)*")
    asserts.assert_that(len(counter_container)).is_equal_to(larky.WHILE_LOOP_EMULATION_ITERATION)


def _test_while_true_exception_stack_trace_is_correct():
    """Test _while_true with custom error message"""
    counter = larky.utils.Counter()
    BOUND = 5
    def _test_loop(counter):
        for _while_ in larky.while_true(bound=BOUND):
            counter.add_and_get(1)
            if counter.get() >= 10:  # This should trigger the error
                fail("Iterable failed with error message")
    # a regex that matches the final line of file_context_0 (a Starlark stacktrace for _test_loop)
    #
    # Example: 
    #         File ".../test_larky.star", line 81, column 40, in _test_loop
    #                for _while_ in larky.while_true(bound=5):
    # Error in __next__: Iteration limit exceeded! Loop bound (larky.WHILE_LOOP_EMULATION_ITERATION=16384) ...
    #
    # This regex will match the expected Starlark trace line, regardless of file path or line number
    trace_regex = (
        r"Iteration limit exceeded! Loop bound \(larky\.WHILE_LOOP_EMULATION_ITERATION.+$"
    )    
    asserts.assert_fails(lambda: _test_loop(counter), trace_regex)
    asserts.assert_that(counter.get()).is_equal_to(BOUND)
    
    counter = larky.utils.Counter()
    
    def _loop_logical_error(counter):
        for _while_ in larky.while_true(bound=BOUND):
            counter.add_and_get(1)
            if counter.get() == 3:
                c = 1/0
    asserts.assert_fails(lambda: _loop_logical_error(counter), r".*floating-point division by zero.*")
    asserts.assert_that(counter.get()).is_equal_to(3)



def _suite():
    _suite = unittest.TestSuite()
    for t in [
        _test_namespace_exposes_larky_builtins,
        _test_while_true_basic,
        _test_while_true_interrupted_with_custom_exception,
        _test_while_true_interrupted_with_globally_defined_limit,
        _test_while_true_repr,
        _test_while_true_exception_stack_trace_is_correct,
    ]:
        _suite.addTest(unittest.FunctionTestCase(t))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())