# Test-only keys: not secrets

`rsa.pem` is an RSA private key made for Larky's jose tests with
`../make_keys.sh`. It protects nothing, is not used outside these tests, and is
committed on purpose so the fixtures can be regenerated. `rsa.pub.pem` is its
public key. The same key is embedded in
`larky/src/test/resources/vendor_tests/jose/parity_fixtures.star`.

Secret scanners that flag it are reporting a false positive. The embedded copy
carries the repository's `# nosemgrep: secrets.misc.generic_private_key.generic_private_key`
annotation (SI-203); `.trufflehogignore` already excludes `larky/src/test/`.
