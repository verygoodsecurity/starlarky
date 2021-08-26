load("@stdlib//larky", "larky")
load("@stdlib//jbinascii", _binascii="jbinascii")


def a2b_base64(data):
    'Decode a line of base64 data.'
    return _binascii.a2b_base64(data)


def a2b_hex(hexstr):
    'Binary data of hexadecimal representation.\n\nhexstr must contain an even number of hex digits (upper or lower case).\nThis function is also available as "unhexlify()".'
    return _binascii.a2b_hex(hexstr)


def unhexlify(hexstr):
    'Binary data of hexadecimal representation.\n\nhexstr must contain an even number of hex digits (upper or lower case).'
    return a2b_hex(hexstr)


def b2a_base64(data):
    'Base64-code line of data.'
    return _binascii.b2a_base64(data)


def b2a_hex(data):
    'Hexadecimal representation of binary data.\n\n  sep\n    An optional single character or byte to separate hex bytes.\n  bytes_per_sep\n    How many bytes between separators.  Positive values count from the\n    right, negative values count from the left.\n\nThe return value is a bytes object.  This function is also\navailable as "hexlify()".\n\nExample:\n>>> binascii.b2a_hex(b\'\\xb9\\x01\\xef\')\nb\'b901ef\'\n>>> binascii.hexlify(b\'\\xb9\\x01\\xef\', \':\')\nb\'b9:01:ef\'\n>>> binascii.b2a_hex(b\'\\xb9\\x01\\xef\', b\'_\', 2)\nb\'b9_01ef\''
    return _binascii.b2a_hex(data)


def hexlify(data):
    'Hexadecimal representation of binary data.\n\nThe return value is a bytes object.'
    return b2a_hex(data)


def crc32(data, value=0):
    'Compute CRC-32 incrementally.'
    return _binascii.crc32(data, value)


def a2b_hqx(data):
    'Decode .hqx coding.'
    fail("not implemented")


def a2b_qp(data, header):
    'Decode a string of qp-encoded data.'
    fail("not implemented")


def a2b_uu(data):
    'Decode a line of uuencoded data.'
    fail("not implemented")


def b2a_hqx(data):
    'Encode .hqx data.'
    fail("not implemented")


def b2a_qp(data, quotetabs, istext, header):
    'Encode a string using quoted-printable encoding.\n\nOn encoding, when istext is set, newlines are not encoded, and white\nspace at end of lines is.  When istext is not set, \\r and \\n (CR/LF)\nare both encoded.  When quotetabs is set, space and tabs are encoded.'
    fail("not implemented")


def b2a_uu(data):
    'Uuencode line of data.'
    fail("not implemented")


def crc_hqx(data, crc):
    'Compute CRC-CCITT incrementally.'
    fail("not implemented")


def rlecode_hqx(data):
    'Binhex RLE-code binary data.'
    fail("not implemented")


def rledecode_hqx(data):
    'Decode hexbin RLE-coded string.'
    fail("not implemented")


binascii = larky.struct(
    a2b_base64=a2b_base64,
    a2b_hex=a2b_hex,
    unhexlify=unhexlify,
    b2a_base64=b2a_base64,
    b2a_hex=b2a_hex,
    hexlify=hexlify,
    crc32=crc32,
    a2b_hqx=a2b_hqx,
    a2b_qp=a2b_qp,
    a2b_uu=a2b_uu,
    b2a_hqx=b2a_hqx,
    b2a_qp=b2a_qp,
    b2a_uu=b2a_uu,
    crc_hqx=crc_hqx,
    rlecode_hqx=rlecode_hqx,
    rledecode_hqx=rledecode_hqx,
)