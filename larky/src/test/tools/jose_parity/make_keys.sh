#!/bin/sh
# Generates the test-only keys in keys/ with OpenSSL (see keys/README.md). They
# are not secrets. The keys are committed so the fixtures are reproducible;
# regenerating them changes parity_fixtures.star.
#
# Used with: OpenSSL 3.6.5 29 Sep 2026
set -eu
cd "$(dirname "$0")/keys"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out rsa.pem
openssl pkey -in rsa.pem -pubout -out rsa.pub.pem
