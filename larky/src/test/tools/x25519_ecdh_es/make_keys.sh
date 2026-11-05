#!/bin/sh
# Generates the test-only keys in keys/ with OpenSSL. The keys are committed
# so the fixtures are reproducible; regenerating them changes every fixture.
#
# Used with: OpenSSL 3.6.5 29 Sep 2026
set -eu
cd "$(dirname "$0")/keys"
for name in x25519_a x25519_b; do
  openssl genpkey -algorithm X25519 -out "$name.pem"
  openssl pkey -in "$name.pem" -pubout -out "$name.pub.pem"
done
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out p256.pem
openssl pkey -in p256.pem -pubout -out p256.pub.pem
