
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//larky", larky="larky")
load("@vendor//asserts", "asserts")
load("@vendor//ecdsa/util", string_to_number="string_to_number")

def AbstractPoint():
    """Class for common methods of elliptic curve points."""

    self = larky.mustablestruct(__class__=AbstractPoint, __name__="AbstractPoint")
    # @staticmethod
    def _from_raw_encoding(data, raw_encoding_length):
        """
        Decode public point from :term:`raw encoding`.
        :term:`raw encoding` is the same as the :term:`uncompressed` encoding,
        but without the 0x04 byte at the beginning.
        """
        # real assert, from_bytes() should not call us with different length
        asserts.eq(len(data), raw_encoding_length)
        
        xs = data[: raw_encoding_length // 2]
        ys = data[raw_encoding_length // 2 :]
        # real assert, raw_encoding_length is calculated by multiplying an
        # integer by two so it will always be even
        asserts.eq(len(xs), raw_encoding_length // 2)
        asserts.eq(len(ys), raw_encoding_length // 2)
        coord_x = string_to_number(xs)
        coord_y = string_to_number(ys)

        return coord_x, coord_y
    self._from_raw_encoding=_from_raw_encoding

    return self

ellpticcurve=larky.struct(
    AbstractPoint=AbstractPoint,
)