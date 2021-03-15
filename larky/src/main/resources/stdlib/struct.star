load("@stdlib//larky", "larky")
load("@stdlib//jstruct", _jstruct="jstruct")


def _pack(fmt, *values):
    return _jstruct.pack(fmt, *values)


def _pack_into(fmt, buffer, offset, *v):
    return _jstruct.pack_into(fmt, buffer, offset, *v)


def _unpack(fmt, buffer):
    return _jstruct.unpack(fmt, buffer)


def _unpack_from(fmt, buffer, offset):
    return _jstruct.unpack_from(fmt, buffer, offset)


def _calcsize(fmt):
    return _jstruct.calcsize(fmt)


struct = larky.struct(
    pack=_pack,
    pack_into=_pack_into,
    unpack=_unpack,
    unpack_from=_unpack_from,
    calcsize=_calcsize
)