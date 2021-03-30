load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//types", "types")


def strxor(term1, term2, output=None):
    """
    XOR two byte strings.

        Args:
          term1 (bytes/bytearray/memoryview):
            The first term of the XOR operation.
          term2 (bytes/bytearray/memoryview):
            The second term of the XOR operation.
          output (bytearray/memoryview):
            The location where the result must be written to.
            If ``None``, the result is returned.
        :Return:
            If ``output`` is ``None``, a new ``bytes`` string with the result.
            Otherwise ``None``.

    """
    if len(term1) != len(term2):
        fail('ValueError("Only byte strings of equal length can be xored")')
    if output != None:
        if types.is_bytes(output):
            fail('TypeError("output must be a bytearray or a writeable memoryview")')
        if len(term1) != len(output):
            fail('ValueError("output must have the same length as the input' +
                                         '  (%d bytes)' % len(term1))

    val = _JCrypto.Util.strxor.strxor(term1, term2)
    if output != None:
        output.insert(0, val)
    return val



def strxor_c(term, c, output=None):
    """
    XOR a byte string with a repeated sequence of characters.

        Args:
            term(bytes/bytearray/memoryview):
                The first term of the XOR operation.
            c (bytes):
                The byte that makes up the second term of the XOR operation.
            output (None or bytearray/memoryview):
                If not ``None``, the location where the result is stored into.

        Return:
            If ``output`` is ``None``, a new ``bytes`` string with the result.
            Otherwise ``None``.

    """
    if not ((0 <= c) and (c < 256)):
        fail('ValueError("c must be in range(256)")')

    if output != None:
        if types.is_bytes(output):
            fail('TypeError("output must be a bytearray or a writeable memoryview")')
        if len(term) != len(output):
            fail('ValueError("output must have the same length as the input' +
                                         '  (%d bytes)' % len(term))

    val = _JCrypto.Util.strxor.strxor_c(term, c)
    if output != None:
        output.insert(0, val)
    return val

