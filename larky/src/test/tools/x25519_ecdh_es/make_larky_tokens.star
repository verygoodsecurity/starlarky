# Makes larky_tokens.json: JWEs encrypted by Larky, for check_larky_tokens.py.
#
# Larky test files cannot be run from this directory. To regenerate:
#
#   cp larky/src/test/tools/x25519_ecdh_es/make_larky_tokens.star \
#      larky/src/test/resources/vendor_tests/jose/test_make_larky_tokens.star
#   mvn -o -q -B -pl larky -am test -Dtest=VendorLibTests \
#       -Dlarky.vendor_test=test_make_larky_tokens.star \
#       -Dsurefire.failIfNoSpecifiedTests=false | grep -o 'LARKY_TOKENS_JSON=.*'
#   rm larky/src/test/resources/vendor_tests/jose/test_make_larky_tokens.star
#
# and save the JSON after "LARKY_TOKENS_JSON=" as larky_tokens.json.
load("@vendor//jose/jwe", jwe="jwe")
load("@stdlib//json", json="json")
load("./ecdh_es_fixtures", KEYS="KEYS", PLAINTEXT="PLAINTEXT")
CASES = [
    ("larky_x25519_dir_a256gcm", "x25519_a", "ECDH-ES", "A256GCM", None),
    ("larky_x25519_dir_a256gcm_apu_apv", "x25519_b", "ECDH-ES", "A256GCM", {"apu": "QWxpY2U", "apv": "Qm9i"}),
    ("larky_x25519_a256kw_a256gcm", "x25519_a", "ECDH-ES+A256KW", "A256GCM", None),
    ("larky_x25519_a128kw_a128gcm", "x25519_b", "ECDH-ES+A128KW", "A128GCM", None),
    ("larky_x25519_a192kw_a192gcm", "x25519_a", "ECDH-ES+A192KW", "A192GCM", None),
    ("larky_p256_dir_a256gcm", "p256", "ECDH-ES", "A256GCM", None),
    ("larky_p256_a256kw_a256gcm", "p256", "ECDH-ES+A256KW", "A256GCM", {"apu": "c2RrLWNsaWVudA", "apv": "dmdzLXJvdXRl"}),
]
def main():
    out = []
    for name, key_name, alg, enc, extra in CASES:
        token = jwe.encrypt(PLAINTEXT, KEYS[key_name]["public_pem"], encryption=enc, algorithm=alg, extra_headers=extra)
        out.append([name, key_name, alg, enc, token.decode("utf-8")])
    print("LARKY_TOKENS_JSON=" + json.dumps(out))
main()
