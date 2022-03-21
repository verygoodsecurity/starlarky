# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//builtins", builtins="builtins")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long", long_to_bytes="long_to_bytes")


def _check_bytes(name, value):
    # type: (str, bytes) -> None
    if not types.is_bytes(value):
        fail("{} must be bytes".format(name))


def _check_byteslike(name, value):
    # type: (str, bytes) -> None
    if not types.is_bytelike(value):
        fail("{} must be bytes-like".format(name))


int_from_bytes = bytes_to_long


int_to_bytes = long_to_bytes


def read_only_property(prop):
    return larky.property(lambda : prop)


utils = larky.struct(
    __name__='utils',
    _check_bytes=_check_bytes,
    _check_byteslike=_check_byteslike,
    int_from_bytes=bytes_to_long,
    int_to_bytes=long_to_bytes,
    read_only_property=read_only_property,
)