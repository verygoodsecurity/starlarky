# Ported from pycryptodome 3.10.0-3.10.1, pinned to 3.10.1 (339259c): lib/Crypto/Hash/SHA.py. See larky/src/main/resources/VENDORED.md.
load("@vendor//Crypto/Hash/SHA1", SHA1="SHA1")

# backward compability
SHA = SHA1