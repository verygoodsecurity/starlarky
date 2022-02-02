load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//larky", larky="larky")

def orderlen(order):
    return (1 + len("%x" % order)) // 2

def string_to_number(string):
    return int(hexlify(string), 16)

util = larky.struct(
    orderlen=orderlen,
    string_to_number=string_to_number,
)