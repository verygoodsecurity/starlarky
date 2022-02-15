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
    string = binascii.unhexlify((fmt_str % num).encode())
    # assert len(string) == l, (len(string), l)
    if len(string) != l:
        fail("AssertionError:(%d, %d)" % (len(string), l))
    return string

def number_to_string_crop(num, order):
    l = orderlen(order)
    fmt_str = "%0" + str(2 * l) + "x"
    string = binascii.unhexlify((fmt_str % num).encode())
    return string[:l]

util = larky.struct(
    orderlen=orderlen,
    string_to_number=string_to_number,
    number_to_string=number_to_string,
    number_to_string_crop=number_to_string_crop,
    bit_length=bit_length
)   