load("@stdlib//larky", "larky")
load("@stdlib//jresult", _JResult="jresult")


def _Ok(val):
    """
    Contains the success value.
    Args:
         val: The success value.
    Returns:
         The :class:`Result` containing the success value.
    Examples:
        >>> res = Result.Ok(1)
        >>> res
        Ok(1)
        >>> res.is_ok
        True
    """
    return _JResult.Ok(val)


def _Error(val):
    """
    Contains the error value.
    Args:
        err: The error value.
    Returns:
        The :class:`Result` containing the error value.
    Examples:
        >>> res = Result.Err('Oh No')
        >>> res
        Err('Oh No')
        >>> res.is_err
        True
    """
    return _JResult.Error(val)


def _of(o):
    return _JResult.of(o)


Result = larky.struct(
    Ok=_Ok,
    Error=_Error,
    of=_of
)