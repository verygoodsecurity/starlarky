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
    def _test_loop():
        count = 0
        for _while_ in larky.while_true():
            count += 1
        fail("Should not have arrived here!")
    asserts.assert_fails(_test_loop, ".*Iteration limit exceeded! Loop bound \\(larky.WHILE_LOOP_EMULATION_ITERATION=\\d+\\)*")
    
       
def _suite():
    _suite = unittest.TestSuite()
    for t in [
        _test_namespace_exposes_larky_builtins,
        _test_while_true_basic,
        _test_while_true_interrupted_with_custom_exception,
        _test_while_true_interrupted_with_globally_defined_limit,
        _test_while_true_repr,
    ]:
        _suite.addTest(unittest.FunctionTestCase(t))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())