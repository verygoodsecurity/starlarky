# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
# Ported from cryptography 35.0.0 (c7fbef7): src/cryptography/x509/general_name.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//larky", larky="larky")


general_name = larky.struct(
    __name__='general_name',
)