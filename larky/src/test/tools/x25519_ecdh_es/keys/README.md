# Test-only keys: not secrets

`x25519_a.pem`, `x25519_b.pem` and `p256.pem` are private keys made for
Larky's X25519, ECDH and JWE ECDH-ES tests with `../make_keys.sh`. They protect
nothing, are not used outside these tests, and are committed on purpose so the
fixtures can be regenerated. The `.pub.pem` files are their public keys. The
same keys are embedded in
`larky/src/test/resources/vendor_tests/Crypto/PublicKey/x25519_key_fixtures.star`
and `larky/src/test/resources/vendor_tests/jose/ecdh_es_fixtures.star`.

Secret scanners that flag them are reporting a false positive. The embedded
copies carry the repository's
`# nosemgrep: secrets.misc.generic_private_key.generic_private_key` annotation
(SI-203); `.trufflehogignore` already excludes `larky/src/test/`.
