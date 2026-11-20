# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
# Ported from cryptography 35.0.0 (c7fbef7): src/cryptography/hazmat/primitives/serialization/ssh.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//larky", larky="larky")

#      "load_ssh_private_key",
#      "load_ssh_public_key",

ssh = larky.struct(
    __name__='ssh',
)