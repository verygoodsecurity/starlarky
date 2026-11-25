# Ported from option 1.0.3 (result.py docstrings unchanged since 0.3.0) (d885a88): option/result.py. See larky/src/main/resources/VENDORED.md.

"""Basic Result constructors shared by option/result and multidict."""
load("@stdlib//jresult", _JResult="jresult")

def Ok(val):

    return _JResult.Ok(val)


def Error(val):
    """
    Contains the error value.
    Args:
        err: The error value.
    Returns:
        The :class:`Result` containing the error value.
    Examples:
        >>> res = Result.Error('Oh No')
        >>> res
        Err('Oh No')
        >>> res.is_err
        True
    """
    return _JResult.Error(val)
