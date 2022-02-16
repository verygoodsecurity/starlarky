load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//larky", larky="larky")

def orderlen(order):
    return (1 + len("%x" % order)) // 2

def bit_length(x):
    return x.bit_length() or 1

def string_to_number(string):
#   return int(binascii.hexlify(string), 16)
    return int(str(hexlify(string)), 16)

def number_to_string(num, order):
    l = orderlen(order)
    fmt_str = "%0" + str(2 * l) + "x"
    string = unhexlify((fmt_str % num).encode())
    # assert len(string) == l, (len(string), l)
    if len(string) != l:
        fail("AssertionError:(%d, %d)" % (len(string), l))
    return string

def number_to_string_crop(num, order):
    l = orderlen(order)
    fmt_str = "%0" + str(2 * l) + "x"
    string = unhexlify((fmt_str % num).encode())
    return string[:l]

def sigencode_strings(r, s, order):
    r_str = number_to_string(r, order)
    s_str = number_to_string(s, order)
    return (r_str, s_str)


def sigencode_string(r, s, order):
    """
    Encode the signature to raw format (:term:`raw encoding`)

    It's expected that this function will be used as a `sigencode=` parameter
    in :func:`ecdsa.keys.SigningKey.sign` method.

    :param int r: first parameter of the signature
    :param int s: second parameter of the signature
    :param int order: the order of the curve over which the signature was
        computed

    :return: raw encoding of ECDSA signature
    :rtype: bytes
    """
    # for any given curve, the size of the signature numbers is
    # fixed, so just use simple concatenation
    r_str, s_str = sigencode_strings(r, s, order)
    return r_str + s_str

util = larky.struct(
    orderlen=orderlen,
    string_to_number=string_to_number,
    number_to_string=number_to_string,
    number_to_string_crop=number_to_string_crop,
    bit_length=bit_length,
    sigencode_string=sigencode_string
)   