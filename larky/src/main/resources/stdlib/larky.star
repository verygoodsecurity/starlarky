# For compatibility help with Python, introduced globals are going to be using
# this as a namespace


def _to_dict(s):
    """Converts a `struct` to a `dict`.
    Args:
      s: A `struct`.
    Returns:
      A `dict` whose keys and values are the same as the fields in `s`. The
      transformation is only applied to the struct's fields and not to any
      nested values.
    """
    # NOTE: This will evaluate properties on getattr(), so it is *IMPORTANT*
    # that if you use this function, to not expect properties to fail()
    #
    # You might be better of using repr() instead!
    attributes = dir(s)
    if "to_json" in attributes:
        attributes.remove("to_json")
    if "to_proto" in attributes:
        attributes.remove("to_proto")
    return {key: getattr(s, key) for key in attributes}


# emulates while loop but will iterate *only* for 100 steps.
WHILE_LOOP_EMULATION_ITERATION = 4096

_SENTINEL = _sentinel()

# TODO: maybe move to a testutils?
def _parametrize(testaddr, testcase, param, args):
    """
    Emulates the pytest.parametrize() but with some larky differences.

    Decorator a testaddr to create a testcase that is parameterized by `param`
    and a list of `args` that are set to the param.

    :param testaddr: in Larky, this would be `suite.addTest`
    :param testcase: in Larky, this would be `unittest.FunctionTestCase`
    :param param: the parameter to set
    :param args: The variable parameters to set param to
    :return: None

    Examples:
        >>> load("@stdlib//larky", "larky")
        >>> load("@stdlib//unittest", "unittest")
        >>> load("@vendor//asserts", "asserts")
        >>>
        >>> def _test(val):
        ...    asserts.assert_eq(val).is_equal_to(val)
        >>>
        >>> _suite = unittest.TestSuite()
        >>> larky.parametrize(
        ...    _suite.addTest,
        ...    unittest.FunctionTestCase,
        ...    'val',
        ...    [0, None, {}, [], False])(_test)
    """
    split_on = ',' if ',' in param else None
    # cannot import types in larky module, so this test allows
    # to see if multiple params exist
    if not hasattr(param, 'append') and split_on:
        param = param.split(split_on)
    elif not hasattr(param, 'append'):
        param = [param]

    def parametrized(func):
        if len(param) == 1:
            for arg in args:
                testaddr(testcase(_partial(func, **dict(zip(param, [arg])))))
        else:
            for arg in args:
                testaddr(testcase(_partial(func, **dict(zip(param, arg)))))
    return parametrized


larky = _struct(
    struct=_struct,
    mutablestruct=_mutablestruct,
    to_dict=_to_dict,
    partial=_partial,
    property=_property,
    WHILE_LOOP_EMULATION_ITERATION=WHILE_LOOP_EMULATION_ITERATION,
    SENTINEL=_SENTINEL,
    parametrize=_parametrize,
)