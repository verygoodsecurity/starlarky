load("@stdlib//larky", "larky")
load("@stdlib//jstruct", _jstruct="jstruct")


def pack(fmt, *values):
    return _jstruct.pack(fmt, *values)


def pack_into(fmt, buffer, offset, *v):
    return _jstruct.pack_into(fmt, buffer, offset, *v)


def unpack(fmt, buffer):
    return _jstruct.unpack(fmt, buffer)


def unpack_from(fmt, buffer, offset=0):
    return _jstruct.unpack_from(fmt, buffer, offset)


def calcsize(fmt):
    return _jstruct.calcsize(fmt)


struct = larky.struct(
    pack=pack,
    pack_into=pack_into,
    unpack=unpack,
    unpack_from=unpack_from,
    calcsize=calcsize
)