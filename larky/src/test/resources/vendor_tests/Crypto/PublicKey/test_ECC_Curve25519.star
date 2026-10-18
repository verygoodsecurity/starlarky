# Ported from pycryptodome 3.21.0 (v3.21.0, 5e40a1810e610ae45bb84aa79efe405515337a13): lib/Crypto/SelfTest/PublicKey/test_ECC_Curve25519.py. See larky/src/main/resources/VENDORED.md.
#
# Changes from upstream:
# - Integer.from_bytes(b, byteorder='little') is bytes_to_long(b[::-1]).
# - 2**255 is (1 << 255): Starlark has no ** operator.
# - TestEccPoint_Curve25519.test_scalar_multiply: Larky's EccXPoint is
#   multiplied with BouncyCastle's X25519, which clamps the scalar, so only
#   clamped scalars are accepted. The upstream table (scalars 0, 1, 2, ...,
#   order + 1) is replaced by a check that those scalars are refused and by
#   clamped-scalar cases checked against RFC 7748.
#
# The tests after "Larky additions" are not upstream. They import and export
# the OpenSSL keys of x25519_key_fixtures.star in place of upstream's
# test_import_Curve25519.py, whose key files Larky tests cannot read.

load("@stdlib//binascii", hexlify="hexlify", unhexlify="unhexlify")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//Crypto/Hash/SHAKE128", SHAKE128="SHAKE128")
load("@vendor//Crypto/PublicKey/ECC", ECC="ECC")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long")
load("./x25519_key_fixtures", OPENSSL_X25519_KEYS="OPENSSL_X25519_KEYS")

EccKey = ECC.EccKey
EccXPoint = ECC.EccXPoint
_curves = ECC.curves


def _from_le(b):
    return bytes_to_long(b[::-1])


v1 = 0x09fa78b39b00a72930bcd8039be789a0997830bb99f79aeeb93493715390b4e8
v2 = 0x15210f12786811d3f4b7959d0538ae2c31dbe7106fc03c3efc4cd549c715a493


def TestEccPoint_Curve25519_test_init():
    EccXPoint(9, "curve25519")
    EccXPoint((1 << 255) - 19 + 5, "curve25519")


def TestEccPoint_Curve25519_test_curve_attribute():
    point = EccXPoint(9, "curve25519")
    asserts.assert_that(point.curve).is_equal_to("Curve25519")


def TestEccPoint_Curve25519_test_init_fail():
    asserts.assert_fails(lambda: EccXPoint(3*((1 << 255) - 19), "curve25519"),
                         "^ValueError: Incorrect coordinate length$")
    asserts.assert_fails(lambda: EccXPoint(9, "curve25518"),
                         "^ValueError: Unknown curve name curve25518$")


def TestEccPoint_Curve25519_test_equal_set():
    point1 = EccXPoint(v1, "curve25519")
    point2 = EccXPoint(v2, "curve25519")

    asserts.assert_that(point1).is_equal_to(point1)
    asserts.assert_that(point1 == point2).is_false()

    point2.set(point1)
    asserts.assert_that(point1.x).is_equal_to(point2.x)


def TestEccPoint_Curve25519_test_copy():
    point1 = EccXPoint(v1, "curve25519")
    point2 = point1.copy()
    asserts.assert_that(point1.x).is_equal_to(point2.x)


def TestEccPoint_Curve25519_test_pai():
    point1 = EccXPoint(v1, "curve25519")
    pai = point1.point_at_infinity()
    asserts.assert_that(pai.is_point_at_infinity()).is_true()

    point2 = EccXPoint(None, "curve25519")
    asserts.assert_that(point2.is_point_at_infinity()).is_true()


def TestEccPoint_Curve25519_test_scalar_multiply():
    base = EccXPoint(9, "curve25519")
    order = 0x1000000000000000000000000000000014def9dea2f79cd65812631a5cf5d3ed

    # Larky: scalars that are not clamped (RFC 7748, Section 5) are refused.
    for d in (0, 1, 2, 3, 9, 255, 0x10101, order, order + 1):
        asserts.assert_fails(lambda: d * base,
                             "^ValueError: Larky multiplies Curve25519 points only by clamped scalars$")

    # RFC 7748, Section 6.1: Alice's clamped scalar times the base point.
    alice_seed = unhexlify("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a")
    alice_pub = unhexlify("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a")
    d = EccKey(curve="curve25519", seed=alice_seed).d
    asserts.assert_that((d * base).x).is_equal_to(_from_le(alice_pub))
    asserts.assert_that((base * d).x).is_equal_to(_from_le(alice_pub))

    # The point at infinity stays there.
    asserts.assert_that((d * base.point_at_infinity()).is_point_at_infinity()).is_true()


def TestEccPoint_Curve25519_test_sizes():
    point = EccXPoint(9, "curve25519")
    asserts.assert_that(point.size_in_bits()).is_equal_to(255)
    asserts.assert_that(point.size_in_bytes()).is_equal_to(32)


def TestEccKey_Curve25519_test_private_key():
    # RFC7748 Section 6.1 - Alice
    alice_priv = unhexlify("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a")
    alice_pub = unhexlify("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a")
    alice_pub_x = _from_le(alice_pub)

    key = EccKey(curve="Curve25519", seed=alice_priv)
    asserts.assert_that(key.seed).is_equal_to(alice_priv)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ.x).is_equal_to(alice_pub_x)

    # RFC7748 Section 6.1 - Bob
    bob_priv = unhexlify("5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb")
    bob_pub = unhexlify("de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f")
    bob_pub_x = _from_le(bob_pub)

    key = EccKey(curve="Curve25519", seed=bob_priv)
    asserts.assert_that(key.seed).is_equal_to(bob_priv)
    asserts.assert_that(key.has_private()).is_true()
    asserts.assert_that(key.pointQ.x).is_equal_to(bob_pub_x)

    # Other names
    key = EccKey(curve="curve25519", seed=alice_priv)

    # Must not accept d parameter
    asserts.assert_fails(lambda: EccKey(curve="curve25519", d=1),
                         "^ValueError: Parameter d can only be used with NIST P curves$")


def TestEccKey_Curve25519_test_public_key():
    point = EccXPoint(_curves['curve25519'].Gx,
                      curve='curve25519')
    key = EccKey(curve="curve25519", point=point)
    asserts.assert_that(key.has_private()).is_false()
    asserts.assert_that(key.pointQ).is_equal_to(point)


def TestEccKey_Curve25519_test_public_key_derived():
    priv_key = EccKey(curve="curve25519", seed=b'H'*32)
    pub_key = priv_key.public_key()
    asserts.assert_that(pub_key.has_private()).is_false()
    asserts.assert_that(priv_key.pointQ).is_equal_to(pub_key.pointQ)


def TestEccKey_Curve25519_test_invalid_seed():
    asserts.assert_fails(lambda: EccKey(curve="curve25519", seed=b'H' * 31),
                         "^ValueError: Parameter seed must be 32 bytes long for Curve25519$")


def TestEccKey_Curve25519_test_equality():
    private_key = ECC.construct(seed=b'H'*32, curve="Curve25519")
    private_key2 = ECC.construct(seed=b'H'*32, curve="curve25519")
    private_key3 = ECC.construct(seed=b'C'*32, curve="Curve25519")

    public_key = private_key.public_key()
    public_key2 = private_key2.public_key()
    public_key3 = private_key3.public_key()

    asserts.assert_that(private_key).is_equal_to(private_key2)
    asserts.assert_that(private_key == private_key3).is_false()

    asserts.assert_that(public_key).is_equal_to(public_key2)
    asserts.assert_that(public_key == public_key3).is_false()

    asserts.assert_that(public_key == private_key).is_false()


def TestEccKey_Curve25519_test_name_consistency():
    key = ECC.generate(curve='curve25519')
    asserts.assert_that(repr(key)).contains("curve='Curve25519'")
    asserts.assert_that(key.curve).is_equal_to('Curve25519')
    asserts.assert_that(key.public_key().curve).is_equal_to('Curve25519')


def TestEccModule_Curve25519_test_generate():
    key = ECC.generate(curve="Curve25519")
    asserts.assert_that(key.has_private()).is_true()
    point = EccXPoint(_curves['Curve25519'].Gx, curve="Curve25519") * key.d
    asserts.assert_that(key.pointQ).is_equal_to(point)

    # Always random
    key2 = ECC.generate(curve="Curve25519")
    asserts.assert_that(key == key2).is_false()

    # Other names
    ECC.generate(curve="curve25519")

    # Random source
    key1 = ECC.generate(curve="Curve25519", randfunc=SHAKE128.new().read)
    key2 = ECC.generate(curve="Curve25519", randfunc=SHAKE128.new().read)
    asserts.assert_that(key1).is_equal_to(key2)


def TestEccModule_Curve25519_test_construct():
    seed = unhexlify("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a")
    point_hex = unhexlify("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a")
    Px = _from_le(point_hex)
    point = EccXPoint(Px, curve="Curve25519")

    # Private key only
    key = ECC.construct(curve="Curve25519", seed=seed)
    asserts.assert_that(key.pointQ).is_equal_to(point)
    asserts.assert_that(key.has_private()).is_true()

    # Public key only
    key = ECC.construct(curve="Curve25519", point_x=Px)
    asserts.assert_that(key.pointQ).is_equal_to(point)
    asserts.assert_that(key.has_private()).is_false()

    # Private and public key
    key = ECC.construct(curve="Curve25519", seed=seed, point_x=Px)
    asserts.assert_that(key.pointQ).is_equal_to(point)
    asserts.assert_that(key.has_private()).is_true()

    # Other names
    key = ECC.construct(curve="curve25519", seed=seed)


def TestEccModule_Curve25519_test_negative_construct():
    Gx = _curves['curve25519'].Gx

    asserts.assert_fails(lambda: ECC.construct(curve="Curve25519", d=2, point_x=Gx),
                         "^ValueError: Parameter d can only be used with NIST P curves$")
    asserts.assert_fails(lambda: ECC.construct(curve="Curve25519", seed=b'H'*31),
                         "^ValueError: Parameter seed must be 32 bytes long for Curve25519$")

    # Verify you cannot construct weak keys (small-order points)
    p = (1 << 255) - 19
    for x in (0,
              1,
              325606250916557431795983626356110631294008115727848805560023387167927233504,
              39382357235489614581723060781553021112529911719440698176882885853963445705823,
              p-1,
              p,
              p+1,
              p+325606250916557431795983626356110631294008115727848805560023387167927233504,
              p+39382357235489614581723060781553021112529911719440698176882885853963445705823,
              p*2-1,
              p*2,
              p*2+1):
        asserts.assert_fails(lambda: ECC.construct(curve="Curve25519", point_x=x),
                             "^ValueError: Invalid Curve25519 public key$")


# Larky additions

def _fixture_keys():
    return [(k, ECC.import_key(k["private_pem"])) for k in OPENSSL_X25519_KEYS]


def Larky_test_import_openssl_private():
    for k in OPENSSL_X25519_KEYS:
        from_pem = ECC.import_key(k["private_pem"])
        from_der = ECC.import_key(unhexlify(k["private_der_hex"]))
        asserts.assert_that(from_pem.curve).is_equal_to("Curve25519")
        asserts.assert_that(from_pem.has_private()).is_true()
        asserts.assert_that(hexlify(from_pem.seed)).is_equal_to(bytes(k["raw_private_hex"], encoding="utf-8"))
        asserts.assert_that(from_der).is_equal_to(from_pem)
        asserts.assert_that(hexlify(from_pem.public_key().export_key(format="raw"))).is_equal_to(
            bytes(k["raw_public_hex"], encoding="utf-8"))


def Larky_test_import_openssl_public():
    for k in OPENSSL_X25519_KEYS:
        from_pem = ECC.import_key(k["public_pem"])
        from_der = ECC.import_key(unhexlify(k["public_der_hex"]))
        asserts.assert_that(from_pem.has_private()).is_false()
        asserts.assert_that(from_der).is_equal_to(from_pem)
        asserts.assert_that(hexlify(from_pem.export_key(format="raw"))).is_equal_to(
            bytes(k["raw_public_hex"], encoding="utf-8"))
        asserts.assert_that(from_pem).is_equal_to(ECC.import_key(k["private_pem"]).public_key())


def Larky_test_export_matches_openssl():
    """PKCS#8 and SubjectPublicKeyInfo are byte-for-byte OpenSSL's (RFC 8410)."""
    for k, key in _fixture_keys():
        asserts.assert_that(hexlify(key.export_key(format="DER"))).is_equal_to(
            bytes(k["private_der_hex"], encoding="utf-8"))
        asserts.assert_that(key.export_key(format="PEM").strip()).is_equal_to(k["private_pem"].strip())
        pub = key.public_key()
        asserts.assert_that(hexlify(pub.export_key(format="DER"))).is_equal_to(
            bytes(k["public_der_hex"], encoding="utf-8"))
        asserts.assert_that(pub.export_key(format="PEM").strip()).is_equal_to(k["public_pem"].strip())


def Larky_test_raw_round_trip():
    for k, key in _fixture_keys():
        raw = key.public_key().export_key(format="raw")
        asserts.assert_that(len(raw)).is_equal_to(32)
        again = ECC.construct(curve="Curve25519", point_x=_from_le(raw))
        asserts.assert_that(again).is_equal_to(key.public_key())
        asserts.assert_that(ECC.construct(curve="Curve25519", seed=key.seed)).is_equal_to(key)


def Larky_test_generated_key_round_trip():
    key = ECC.generate(curve="X25519")
    asserts.assert_that(len(key.seed)).is_equal_to(32)
    for fmt in ("DER", "PEM"):
        asserts.assert_that(ECC.import_key(key.export_key(format=fmt))).is_equal_to(key)
        pub = key.public_key()
        asserts.assert_that(ECC.import_key(pub.export_key(format=fmt))).is_equal_to(pub)


def Larky_test_encrypted_pkcs8_export():
    # Only the export is checked: importing an encrypted PKCS#8 key fails in
    # Larky's Crypto/IO/_PBES.star for every curve, not only Curve25519.
    key = ECC.import_key(OPENSSL_X25519_KEYS[0]["private_pem"])
    encoded = key.export_key(format="PEM", passphrase="secret",
                             protection="PBKDF2WithHMAC-SHA1AndAES128-CBC")
    asserts.assert_that(encoded).contains("-----BEGIN ENCRYPTED PRIVATE KEY-----")
    asserts.assert_fails(lambda: key.export_key(format="DER", passphrase="secret"),
                         "^ValueError: At least the 'protection' parameter should be present$")


def Larky_test_export_errors():
    key = ECC.import_key(OPENSSL_X25519_KEYS[0]["private_pem"])
    asserts.assert_fails(lambda: key.export_key(format="PEM", use_pkcs8=False),
                         "^ValueError: 'pkcs8' must be True for Curve25519$")
    asserts.assert_fails(lambda: key.export_key(format="raw"),
                         "^ValueError: Private keys cannot be exported in the 'raw' format$")
    asserts.assert_fails(lambda: key.export_key(format="XXX"),
                         "^ValueError: Unknown format 'XXX'$")
    asserts.assert_fails(lambda: key.public_key().export_key(format="SEC1"),
                         "^ValueError: SEC1 format is only supported for NIST P curves$")
    asserts.assert_fails(lambda: key.public_key().export_key(format="OpenSSH"),
                         "^ValueError: Cannot export Curve25519 keys as OpenSSH$")


def Larky_test_import_errors():
    raw_pub = OPENSSL_X25519_KEYS[0]["raw_public_hex"]
    # As upstream, a NULL in the parameters counts as absent...
    spki_with_null = unhexlify("302c300706032b656e0500032100" + raw_pub)
    asserts.assert_that(hexlify(ECC.import_key(spki_with_null).export_key(format="raw"))).is_equal_to(
        bytes(raw_pub, encoding="utf-8"))
    # ...and any other parameters are refused (RFC 8410).
    spki_with_oid = unhexlify("302f300a06032b656e06032b656e032100" + raw_pub)
    asserts.assert_fails(lambda: ECC.import_key(spki_with_oid),
                         "^ValueError: Not an ECC DER key$")
    # A low-order public key inside a valid SubjectPublicKeyInfo.
    spki_weak = unhexlify("302a300506032b656e032100" + "00" * 32)
    asserts.assert_fails(lambda: ECC.import_key(spki_weak),
                         "^ValueError: Not an ECC DER key$")
    asserts.assert_fails(lambda: ECC.EccPoint(9, 1, "Curve25519"),
                         "^ValueError: EccPoint cannot be created for Curve25519$")


def Larky_test_nist_raw_and_sec1():
    key = ECC.generate(curve="P-256").public_key()
    raw = key.export_key(format="raw")
    asserts.assert_that(len(raw)).is_equal_to(65)
    asserts.assert_that(raw[0]).is_equal_to(4)
    asserts.assert_that(key.export_key(format="SEC1")).is_equal_to(raw)
    asserts.assert_that(len(key.export_key(format="SEC1", compress=True))).is_equal_to(33)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_Curve25519_test_init))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_Curve25519_test_curve_attribute))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_Curve25519_test_init_fail))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_Curve25519_test_equal_set))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_Curve25519_test_copy))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_Curve25519_test_pai))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_Curve25519_test_scalar_multiply))
    _suite.addTest(unittest.FunctionTestCase(TestEccPoint_Curve25519_test_sizes))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_Curve25519_test_private_key))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_Curve25519_test_public_key))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_Curve25519_test_public_key_derived))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_Curve25519_test_invalid_seed))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_Curve25519_test_equality))
    _suite.addTest(unittest.FunctionTestCase(TestEccKey_Curve25519_test_name_consistency))
    _suite.addTest(unittest.FunctionTestCase(TestEccModule_Curve25519_test_generate))
    _suite.addTest(unittest.FunctionTestCase(TestEccModule_Curve25519_test_construct))
    _suite.addTest(unittest.FunctionTestCase(TestEccModule_Curve25519_test_negative_construct))
    # Larky additions
    _suite.addTest(unittest.FunctionTestCase(Larky_test_import_openssl_private))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_import_openssl_public))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_export_matches_openssl))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_raw_round_trip))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_generated_key_round_trip))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_encrypted_pkcs8_export))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_export_errors))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_import_errors))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_nist_raw_and_sec1))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
