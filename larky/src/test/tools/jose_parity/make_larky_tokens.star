# Makes larky_tokens.json: JWEs encrypted by Larky, for check_larky_tokens.py.
#
# Larky test files cannot be run from this directory. To regenerate:
#
#   cp larky/src/test/tools/jose_parity/make_larky_tokens.star \
#      larky/src/test/resources/vendor_tests/jose/test_make_parity_tokens.star
#   mvn -o -q -B -pl larky -am test -Dtest=VendorLibTests \
#       -Dlarky.vendor_test=test_make_parity_tokens.star \
#       -Dsurefire.failIfNoSpecifiedTests=false | grep -o 'LARKY_TOKENS_JSON=.*'
#   rm larky/src/test/resources/vendor_tests/jose/test_make_parity_tokens.star
#
# and save the JSON after "LARKY_TOKENS_JSON=" as larky_tokens.json.
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//json", json="json")
load("@vendor//jose/jwe", jwe="jwe")
load("./parity_fixtures", "PLAINTEXT", "K16", "K24", "K32")
CASES = [
    ("A128GCMKW", "A128GCM", K16, None),
    ("A128GCMKW", "A256CBC-HS512", K16, None),
    ("A192GCMKW", "A192GCM", K24, None),
    ("A256GCMKW", "A256GCM", K32, None),
    ("A256GCMKW", "A128CBC-HS256", K32, None),
    ("dir", "A128GCM", K16, "DEF"),
]
def main():
    rows = []
    for alg, enc, key, zip in CASES:
        token = jwe.encrypt(PLAINTEXT, key, algorithm=alg, encryption=enc, zip=zip)
        rows.append([alg, enc, codecs.decode(key, encoding="ascii"), zip, codecs.decode(token, encoding="ascii")])
    print("LARKY_TOKENS_JSON=" + json.dumps(rows))
main()
