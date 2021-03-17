load("@stdlib/larky", "larky")
load("@stdlib/types", "types")
load("@stdlib/codecs", "codecs")


def _bytes(s, encoding='utf-8', errors='strict'):
    """
    While bytes literals and representations are based on ASCII text, bytes
    objects actually behave like immutable sequences of integers, with each
    value in the sequence restricted such that 0 <= x < 256 (attempts to violate
    this restriction will trigger ValueError). This is done deliberately to
    emphasise that while many binary formats include ASCII based elements and
    can be usefully manipulated with some text-oriented algorithms, this is not
    generally the case for arbitrary binary data (blindly applying text
    processing algorithms to binary data formats that are not ASCII compatible
    will usually lead to data corruption).

    In addition to the literal forms, bytes objects can be created in a number
    of other ways:

    - A zero-filled bytes object of a specified length: bytes(10)
    - From an iterable of integers: bytes(range(20))
    - Copying existing binary data via the buffer protocol: bytes(obj)

    Since bytes objects are sequences of integers (akin to a tuple), for a bytes
    object b, b[0] will be an integer, while b[0:1] will be a bytes object of
    length 1. (This contrasts with text strings, where both indexing and slicing
    will produce a string of length 1)

    The representation of bytes objects uses the literal format (b'...') since
    it is often more useful than e.g. bytes([46, 46, 46]). You can always
    convert a bytes object into a list of integers using list(b).

    :param s:
    :return:
    """
    if hasattr(s, '__bytes__'):
        return s.__bytes__()

    # utf-8 encoding by default
    return larky.bytes(s, encoding, errors)
    # if types.is_iterable(s):
    #     return larky.bytearray(list(s))
    # else:
    #     fail("TypeError: cannot convert '%s' object to bytes" % type(s))


builtins = larky.struct(
    bytes=_bytes,
    b=_bytes
)
