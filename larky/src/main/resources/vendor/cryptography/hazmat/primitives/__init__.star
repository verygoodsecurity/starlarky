# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
# Ported from cryptography 35.0.0 (c7fbef7): src/cryptography/hazmat/primitives/__init__.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//larky", larky="larky")
load("@vendor//cryptography/hazmat/primitives/hashes", _hashes="hashes")
load("@vendor//cryptography/hazmat/primitives/serialization",
     _serialization="serialization")


hashes = _hashes
serialization = _serialization


primitives = larky.struct(
    __name__='primitives',
    hashes=hashes,
)