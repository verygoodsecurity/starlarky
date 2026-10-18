# Larky tests for JWE ECDH-ES and ECDH-ES+A*KW (jose/backends/ecdh_es.star).
# python-jose has no ECDH-ES, so nothing here is ported.
#
# - RFC 7518, Appendix C (P-256 Concat KDF) and RFC 8037, Appendix A.6
#   (X25519) known-answer tests.
# - ecdh_es_fixtures.star: JWEs made by joserfc 1.7.5 to OpenSSL keys,
#   decrypted here (larky/src/test/tools/x25519_ecdh_es/gen_jwe_fixtures.py).
# - ecdh_es_larky_tokens.star: JWEs made by Larky that joserfc 1.7.5
#   decrypted (check_larky_tokens.py and check_larky_tokens.out there).
# - Round trips, tampering and malformed headers.

load("@stdlib//binascii", hexlify="hexlify", unhexlify="unhexlify")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//json", json="json")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//Crypto/Protocol/DH", DH="DH")
load("@vendor//Crypto/PublicKey/ECC", ECC="ECC")
load("@vendor//jose/backends/ecdh_es", ecdh_es="ecdh_es")
load("@vendor//jose", jose_jwe="jwe")
load("@vendor//jose/jwe", jwe="jwe")
load("@vendor//jose/jwk", jwk="jwk")
load("@vendor//jose/utils", base64url_decode="base64url_decode", base64url_encode="base64url_encode")
load("@vendor//option/result", Result="Result", safe="safe")
load("./ecdh_es_fixtures", KEYS="KEYS", PLAINTEXT="PLAINTEXT", TOKENS="TOKENS",
     RFC8037_A6_KEK_HEX="RFC8037_A6_KEK_HEX")
load("./ecdh_es_larky_tokens", LARKY_TOKENS="LARKY_TOKENS")

ERR_EPK = "^JWEError: ECDH-ES epk header is malformed$"
ERR_CURVE = "^JWEError: ECDH-ES epk is not on the curve of the key$"
ERR_ZERO = "^JWEError: ECDH-ES shared secret is all zero$"
ERR_TAG = "^JWEError: Invalid JWE Auth Tag$"
ERR_UNWRAP = "^JWEError: ECDH-ES key unwrap failed$"
ERR_PARTY_INFO = "^JWEError: ECDH-ES apu or apv header is malformed$"

ALL_ALGS = ["ECDH-ES", "ECDH-ES+A128KW", "ECDH-ES+A192KW", "ECDH-ES+A256KW"]
ALL_ENCS = ["A128GCM", "A192GCM", "A256GCM", "A128CBC-HS256", "A256CBC-HS512"]


def _b64(data):
    if types.is_string(data):
        data = codecs.encode(data, encoding="utf-8")
    return base64url_encode(data).decode("utf-8")


def _unb64(s):
    return base64url_decode(codecs.encode(s, encoding="ascii"))


def _parts(token):
    if not types.is_string(token):
        token = token.decode("utf-8")
    return token.split(".")


def _header(token):
    return json.loads(_unb64(_parts(token)[0]).decode("utf-8"))


def _with_header(token, header):
    parts = _parts(token)
    parts[0] = _b64(json.dumps(header))
    return ".".join(parts)


def _flip(segment):
    # Change one character in the middle of a base64url segment.
    i = len(segment) // 2
    c = "A" if segment[i] != "A" else "B"
    return segment[:i] + c + segment[i + 1:]


def _token(name):
    for row in TOKENS:
        if row[0] == name:
            return row
    fail("no fixture " + name)


# RFC 7518, Appendix C

def Test_rfc7518_appendix_c_concat_kdf():
    z = bytes([158, 86, 217, 29, 129, 113, 53, 211, 114, 131, 66, 131, 191, 132,
               38, 156, 251, 49, 110, 163, 218, 128, 106, 72, 246, 218, 167, 121,
               140, 254, 144, 196])
    key = ecdh_es.concat_kdf(z, 128, "A128GCM", b"Alice", b"Bob")
    asserts.assert_that([b for b in key]).is_equal_to(
        [86, 170, 141, 234, 248, 35, 109, 32, 92, 34, 40, 205, 113, 167, 16, 26])
    asserts.assert_that(_b64(key)).is_equal_to("VqqN6vgjbSBcIijNcacQGg")


def Test_rfc7518_appendix_c_full():
    """From Bob's JWK and the example header to the derived key."""
    bob = jwk.construct({"kty": "EC", "crv": "P-256",
                         "x": "weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ",
                         "y": "e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck",
                         "d": "VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw"}, "ECDH-ES")
    header = {"alg": "ECDH-ES", "enc": "A128GCM", "apu": "QWxpY2U", "apv": "Qm9i",
              "epk": {"kty": "EC", "crv": "P-256",
                      "x": "gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0",
                      "y": "SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps"}}
    key = ecdh_es.derive_for_decrypt(bob.prepared_key, header, "ECDH-ES", "A128GCM")
    asserts.assert_that(_b64(key)).is_equal_to("VqqN6vgjbSBcIijNcacQGg")


# RFC 8037, Appendix A.6

def Test_rfc8037_a6():
    # Bob's private key is RFC 7748, Section 6.1's b.
    bob_seed = unhexlify("5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb")
    bob = jwk.construct({"kty": "OKP", "crv": "X25519", "kid": "Bob",
                         "x": "3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08",
                         "d": _b64(bob_seed)}, "ECDH-ES+A128KW")
    epk_jwk = {"kty": "OKP", "crv": "X25519",
               "x": "hSDwCYkwp1R0i33ctD73Wg2_Og0mOBr066SpjqqbTmo"}
    header = {"alg": "ECDH-ES+A128KW", "epk": epk_jwk, "enc": "A128GCM", "kid": "Bob"}

    # The public key from the target key and the ephemeral public key (hex).
    asserts.assert_that(hexlify(bob.prepared_key.public_key().export_key(format="raw"))).is_equal_to(
        b"de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f")
    eph = DH.import_x25519_private_key(
        unhexlify("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"))
    asserts.assert_that(ecdh_es.epk_to_jwk(eph)).is_equal_to(epk_jwk)

    # Z, computed by the sender and by the receiver.
    z_hex = b"4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742"
    epk = ecdh_es.import_epk(epk_jwk)
    z_receiver = DH.key_agreement(static_priv=bob.prepared_key, eph_pub=epk, kdf=lambda z: z)
    z_sender = DH.key_agreement(eph_priv=eph, static_pub=bob.prepared_key.public_key(), kdf=lambda z: z)
    asserts.assert_that(hexlify(z_receiver)).is_equal_to(z_hex)
    asserts.assert_that(hexlify(z_sender)).is_equal_to(z_hex)

    # The KEK (the RFC stops at Z; see ecdh_es_fixtures.star).
    kek = ecdh_es.derive_for_decrypt(bob.prepared_key, header, "ECDH-ES+A128KW", "A128GCM")
    asserts.assert_that(hexlify(kek)).is_equal_to(bytes(RFC8037_A6_KEK_HEX, encoding="utf-8"))


# Interoperability

def Test_decrypt_reference_tokens():
    """JWEs made by joserfc decrypt with the PEM, the JWK or an EccKey."""
    for name, key_name, alg, enc, token in TOKENS:
        k = KEYS[key_name]
        asserts.assert_that(jwe.decrypt(token, k["private_pem"])).is_equal_to(PLAINTEXT)
        asserts.assert_that(jwe.decrypt(bytes(token, encoding="utf-8"), k["private_pem"])).is_equal_to(PLAINTEXT)
        asserts.assert_that(jwe.decrypt(token, k["jwk"])).is_equal_to(PLAINTEXT)
        asserts.assert_that(jwe.decrypt(token, ECC.import_key(k["private_pem"]))).is_equal_to(PLAINTEXT)
        header = _header(token)
        asserts.assert_that(header["alg"]).is_equal_to(alg)
        asserts.assert_that(header["enc"]).is_equal_to(enc)


def Test_package_jwe_decrypt_with_pem():
    """What a route does: jose's jwe.decrypt(token, private_key_pem)."""
    name, key_name, alg, enc, token = _token("x25519_dir_a256gcm")
    asserts.assert_that(jose_jwe.decrypt(token, KEYS[key_name]["private_pem"])).is_equal_to(PLAINTEXT)


def Test_decrypt_larky_tokens():
    """JWEs made by Larky, which joserfc decrypted (check_larky_tokens.out)."""
    for name, key_name, alg, enc, token in LARKY_TOKENS:
        asserts.assert_that(jwe.decrypt(token, KEYS[key_name]["private_pem"])).is_equal_to(PLAINTEXT)


def Test_round_trip():
    for key_name in ("x25519_a", "p256"):
        k = KEYS[key_name]
        for alg in ALL_ALGS:
            for enc in ALL_ENCS:
                token = jwe.encrypt(PLAINTEXT, k["public_pem"], encryption=enc, algorithm=alg)
                header = _header(token)
                asserts.assert_that(header["alg"]).is_equal_to(alg)
                asserts.assert_that(header["enc"]).is_equal_to(enc)
                asserts.assert_that("d" in header["epk"]).is_false()
                asserts.assert_that(header["epk"]["kty"]).is_equal_to(
                    "OKP" if key_name == "x25519_a" else "EC")
                encrypted_key = _parts(token)[1]
                if alg == "ECDH-ES":
                    asserts.assert_that(encrypted_key).is_equal_to("")
                else:
                    asserts.assert_that(len(encrypted_key) > 0).is_true()
                asserts.assert_that(jwe.decrypt(token, k["private_pem"])).is_equal_to(PLAINTEXT)


def Test_round_trip_key_forms_and_party_info():
    k = KEYS["x25519_b"]
    pub_jwk = {"kty": "OKP", "crv": "X25519", "x": k["jwk"]["x"]}
    for key in (k["public_pem"], pub_jwk, ECC.import_key(k["public_pem"]), k["private_pem"]):
        token = jwe.encrypt(PLAINTEXT, key, algorithm="ECDH-ES", encryption="A256GCM",
                            kid="key-1", extra_headers={"apu": "QWxpY2U", "apv": "Qm9i"})
        header = _header(token)
        asserts.assert_that(header["apu"]).is_equal_to("QWxpY2U")
        asserts.assert_that(header["apv"]).is_equal_to("Qm9i")
        asserts.assert_that(header["kid"]).is_equal_to("key-1")
        asserts.assert_that(jwe.decrypt(token, k["jwk"])).is_equal_to(PLAINTEXT)
        # apu/apv enter the KDF: dropping them changes the key.
        h = dict(header)
        h.pop("apu")
        asserts.assert_fails(lambda: jwe.decrypt(_with_header(token, h), k["private_pem"]), ERR_TAG)


def Test_jwk_to_dict():
    for key_name in ("x25519_a", "x25519_b", "p256"):
        k = KEYS[key_name]
        d = jwk.construct(k["private_pem"], "ECDH-ES").to_dict()
        for member in ("kty", "crv", "x", "d"):
            asserts.assert_that(d[member]).is_equal_to(k["jwk"][member])
        pub = jwk.construct(k["public_pem"], "ECDH-ES").to_dict()
        asserts.assert_that("d" in pub).is_false()
        asserts.assert_that(pub["x"]).is_equal_to(k["jwk"]["x"])


def Test_decrypt_inside_safe():
    name, key_name, alg, enc, token = _token("x25519_dir_a256gcm")
    res = safe(jwe.decrypt)(token, KEYS[key_name]["private_pem"])
    asserts.assert_that(res.is_ok).is_true()
    asserts.assert_that(res.unwrap()).is_equal_to(PLAINTEXT)
    parts = _parts(token)
    parts[4] = _flip(parts[4])
    res = safe(jwe.decrypt)(".".join(parts), KEYS[key_name]["private_pem"])
    asserts.assert_that(res.is_err).is_true()
    asserts.assert_fails(lambda: res.unwrap(), ERR_TAG)
    res = safe(jwe.encrypt)(PLAINTEXT, KEYS[key_name]["public_pem"], algorithm="ECDH-ES")
    asserts.assert_that(res.is_ok).is_true()


# Tampering

def Test_tampered_ciphertext_and_tag():
    for name, key_name, alg, enc, token in TOKENS:
        pem = KEYS[key_name]["private_pem"]
        for index in (3, 4):
            parts = _parts(token)
            parts[index] = _flip(parts[index])
            asserts.assert_fails(lambda: jwe.decrypt(".".join(parts), pem), ERR_TAG)
        # IV
        parts = _parts(token)
        parts[2] = _flip(parts[2])
        asserts.assert_fails(lambda: jwe.decrypt(".".join(parts), pem), ERR_TAG)


def Test_tampered_protected_header():
    for name, key_name, alg, enc, token in TOKENS:
        header = _header(token)
        header["cty"] = "text/plain"
        asserts.assert_fails(lambda: jwe.decrypt(_with_header(token, header), KEYS[key_name]["private_pem"]),
                             ERR_TAG)


def Test_tampered_epk():
    """Another valid public key in epk gives another key."""
    other_x25519 = {"kty": "OKP", "crv": "X25519", "x": KEYS["x25519_b"]["jwk"]["x"]}
    other_p256 = {"kty": "EC", "crv": "P-256",
                  "x": "gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0",
                  "y": "SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps"}
    for name, key_name, alg, enc, token in TOKENS:
        if key_name == "x25519_b":
            continue
        header = _header(token)
        header["epk"] = other_p256 if key_name == "p256" else other_x25519
        expected = ERR_TAG if alg == "ECDH-ES" else ERR_UNWRAP
        asserts.assert_fails(lambda: jwe.decrypt(_with_header(token, header), KEYS[key_name]["private_pem"]),
                             expected)


def Test_tampered_encrypted_key():
    for name, key_name, alg, enc, token in TOKENS:
        parts = _parts(token)
        pem = KEYS[key_name]["private_pem"]
        if alg == "ECDH-ES":
            parts[1] = "AAAAAAAAAAAAAAAAAAAAAA"
            asserts.assert_fails(lambda: jwe.decrypt(".".join(parts), pem),
                                 "^JWEError: ECDH-ES requires an empty JWE Encrypted Key$")
        else:
            parts[1] = _flip(parts[1])
            asserts.assert_fails(lambda: jwe.decrypt(".".join(parts), pem), ERR_UNWRAP)
            parts[1] = ""
            asserts.assert_fails(lambda: jwe.decrypt(".".join(parts), pem), ERR_UNWRAP)


def Test_malformed_epk():
    name, key_name, alg, enc, token = _token("x25519_dir_a256gcm")
    pem = KEYS[key_name]["private_pem"]
    good = _header(token)["epk"]
    x = good["x"]
    bad_epks = [
        None,                                                    # missing
        "epk",                                                   # not an object
        {},
        {"kty": "OKP", "crv": "X25519"},                         # no x
        {"kty": "OKP", "crv": "X25519", "x": 7},                 # x not a string
        {"kty": "OKP", "crv": "X25519", "x": "!!!!"},            # not base64url
        {"kty": "OKP", "crv": "X25519", "x": x[:20]},            # wrong length
        {"kty": "OKP", "crv": "X448", "x": x},                   # unsupported curve
        {"kty": "OKP", "crv": "Ed25519", "x": x},
        {"kty": "RSA", "crv": "X25519", "x": x},                 # wrong kty
        {"kty": "OKP", "crv": "X25519", "x": x, "d": x},         # a private key
        {"kty": "EC", "crv": "P-256", "x": x},                   # EC without y
        {"kty": "EC", "crv": "P-256", "x": x, "y": x},           # not on P-256
    ]
    for epk in bad_epks:
        header = _header(token)
        if epk == None:
            header.pop("epk")
        else:
            header["epk"] = epk
        asserts.assert_fails(lambda: jwe.decrypt(_with_header(token, header), pem), ERR_EPK)


def Test_wrong_curve():
    x25519_token = _token("x25519_dir_a256gcm")[4]
    p256_token = _token("p256_dir_a256gcm")[4]
    asserts.assert_fails(lambda: jwe.decrypt(x25519_token, KEYS["p256"]["private_pem"]), ERR_CURVE)
    asserts.assert_fails(lambda: jwe.decrypt(p256_token, KEYS["x25519_a"]["private_pem"]), ERR_CURVE)


def Test_all_zero_shared_secret():
    """Small-order X25519 epk values (RFC 7748, Section 6.1; RFC 8037, Section 3.2.1)."""
    name, key_name, alg, enc, token = _token("x25519_dir_a256gcm")
    for u in ("0000000000000000000000000000000000000000000000000000000000000000",
              "0100000000000000000000000000000000000000000000000000000000000000",
              "e0eb7a7c3b41b8ae1656e3faf19fc46ada098deb9c32b1fd866205165f49b800",
              "5f9c95bca3508c24b1d0b1559c83ef5b04445cc4581c8e86d8224eddd09f1157",
              "ecffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f",
              "edffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f",
              "eeffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f"):
        header = _header(token)
        header["epk"] = {"kty": "OKP", "crv": "X25519", "x": _b64(unhexlify(u))}
        asserts.assert_fails(lambda: jwe.decrypt(_with_header(token, header), KEYS[key_name]["private_pem"]),
                             ERR_ZERO)


def Test_malformed_party_info():
    name, key_name, alg, enc, token = _token("x25519_dir_a256gcm_apu_apv")
    for member, value in (("apu", 5), ("apv", "!!!!"), ("apu", None)):
        header = _header(token)
        header[member] = value
        asserts.assert_fails(lambda: jwe.decrypt(_with_header(token, header), KEYS[key_name]["private_pem"]),
                             ERR_PARTY_INFO)
    asserts.assert_fails(lambda: jwe.encrypt(PLAINTEXT, KEYS[key_name]["public_pem"], algorithm="ECDH-ES",
                                             extra_headers={"apu": b"raw bytes"}),
                         ERR_PARTY_INFO)


def Test_unsupported_alg_and_enc_not_echoed():
    name, key_name, alg, enc, token = _token("x25519_dir_a256gcm")
    pem = KEYS[key_name]["private_pem"]
    for bad in ("ECDH-1PU", "<script>", "ECDH-ES+A512KW"):
        header = _header(token)
        header["alg"] = bad
        asserts.assert_fails(lambda: jwe.decrypt(_with_header(token, header), pem),
                             "^JWEError: Algorithm not supported\\.$")
    for bad in ("A512GCM", "ECDH-ES", "dir", "HS256"):
        header = _header(token)
        header["enc"] = bad
        asserts.assert_fails(lambda: jwe.decrypt(_with_header(token, header), pem),
                             "^JWEError: Encryption algorithm not supported\\.$")
    asserts.assert_fails(lambda: jwe.encrypt(PLAINTEXT, KEYS[key_name]["public_pem"], algorithm="ECDH-ES",
                                             encryption="A512GCM"),
                         "^JWEError: Encryption algorithm not supported\\.$")


def Test_key_errors():
    token = _token("x25519_dir_a256gcm")[4]
    asserts.assert_fails(lambda: jwe.decrypt(token, KEYS["x25519_a"]["public_pem"]),
                         "^JWEError: ECDH-ES decryption needs a private key$")
    asserts.assert_fails(lambda: jwe.encrypt(PLAINTEXT, KEYS["x25519_a"]["public_pem"], algorithm="ECDH-ES",
                                             extra_headers={"epk": {}}),
                         "^JWEError: ECDH-ES generates the epk header$")
    asserts.assert_fails(lambda: jwk.construct({"kty": "OKP", "crv": "Ed25519", "x": "AA"}, "ECDH-ES"),
                         "^JWKError: Unsupported OKP curve. Only X25519 is supported$")
    bad = dict(KEYS["x25519_a"]["jwk"])
    bad["x"] = KEYS["x25519_b"]["jwk"]["x"]
    asserts.assert_fails(lambda: jwk.construct(bad, "ECDH-ES"),
                         "^JWKError: OKP public key does not match the private key$")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(Test_rfc7518_appendix_c_concat_kdf))
    _suite.addTest(unittest.FunctionTestCase(Test_rfc7518_appendix_c_full))
    _suite.addTest(unittest.FunctionTestCase(Test_rfc8037_a6))
    _suite.addTest(unittest.FunctionTestCase(Test_decrypt_reference_tokens))
    _suite.addTest(unittest.FunctionTestCase(Test_package_jwe_decrypt_with_pem))
    _suite.addTest(unittest.FunctionTestCase(Test_decrypt_larky_tokens))
    _suite.addTest(unittest.FunctionTestCase(Test_round_trip))
    _suite.addTest(unittest.FunctionTestCase(Test_round_trip_key_forms_and_party_info))
    _suite.addTest(unittest.FunctionTestCase(Test_jwk_to_dict))
    _suite.addTest(unittest.FunctionTestCase(Test_decrypt_inside_safe))
    _suite.addTest(unittest.FunctionTestCase(Test_tampered_ciphertext_and_tag))
    _suite.addTest(unittest.FunctionTestCase(Test_tampered_protected_header))
    _suite.addTest(unittest.FunctionTestCase(Test_tampered_epk))
    _suite.addTest(unittest.FunctionTestCase(Test_tampered_encrypted_key))
    _suite.addTest(unittest.FunctionTestCase(Test_malformed_epk))
    _suite.addTest(unittest.FunctionTestCase(Test_wrong_curve))
    _suite.addTest(unittest.FunctionTestCase(Test_all_zero_shared_secret))
    _suite.addTest(unittest.FunctionTestCase(Test_malformed_party_info))
    _suite.addTest(unittest.FunctionTestCase(Test_unsupported_alg_and_enc_not_echoed))
    _suite.addTest(unittest.FunctionTestCase(Test_key_errors))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
