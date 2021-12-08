# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
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