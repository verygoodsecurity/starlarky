# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//larky", larky="larky")
load("@stdlib//builtins", builtins="builtins")
load("@vendor//Crypto/Hash", _Hash="Hash")
load("@vendor//cryptography", utils="utils")
load("@vendor//cryptography/hazmat/primitives/_hashes",
     HashAlgorithm="HashAlgorithm",
     HashContext="HashContext",
     Hash="Hash",
     )


SHA1 = HashAlgorithm(
    name="sha1",
    digest_size=20,
    block_size=64,
)

SHA512_224 = HashAlgorithm(
    name="sha512-224",
    digest_size=28,
    block_size=128,
)

SHA512_256 = HashAlgorithm(
    name="sha512-256",
    digest_size=32,
    block_size=128,
)

SHA224 = HashAlgorithm(
    name="sha224",
    digest_size=28,
    block_size=64,
)

SHA256 = HashAlgorithm(
    name="sha256",
    digest_size=32,
    block_size=64,
)

SHA384 = HashAlgorithm(
    name="sha384",
    digest_size=48,
    block_size=128,
)

SHA512 = HashAlgorithm(
    name="sha512",
    digest_size=64,
    block_size=128,
)

SHA3_224 = HashAlgorithm(
    name="sha3-224",
    digest_size=28,
    block_size=None,
)

SHA3_256 = HashAlgorithm(
    name="sha3-256",
    digest_size=32,
    block_size=None,
)

SHA3_384 = HashAlgorithm(
    name="sha3-384",
    digest_size=48,
    block_size=None,
)

SHA3_512 = HashAlgorithm(
    name="sha3-512",
    digest_size=64,
    block_size=None,
)

MD5 = HashAlgorithm(
    name="md5",
    digest_size=16,
    block_size=64,
)

SM3 = HashAlgorithm(
    name="sm3",
    digest_size=32,
    block_size=64,
)

hashes = larky.struct(
    __name__='hashes',
    SHA1=SHA1,
    SHA512_224=SHA512_224,
    SHA512_256=SHA512_256,
    SHA224=SHA224,
    SHA256=SHA256,
    SHA384=SHA384,
    SHA512=SHA512,
    SHA3_224=SHA3_224,
    SHA3_256=SHA3_256,
    SHA3_384=SHA3_384,
    SHA3_512=SHA3_512,
    # SHAKE128=SHAKE128,
    # SHAKE256=SHAKE256,
    MD5=MD5,
    # BLAKE2b=BLAKE2b,
    # BLAKE2s=BLAKE2s,
    SM3=SM3,
)
