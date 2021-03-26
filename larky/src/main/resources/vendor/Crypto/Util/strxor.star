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
def _strxor_direct(term1, term2, result):
    """
    Very fast XOR - check conditions!
    """
