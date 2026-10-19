# Ported from pycryptodome 3.21.0 (v3.21.0, 5e40a1810e610ae45bb84aa79efe405515337a13): lib/Crypto/SelfTest/Protocol/test_ecdh.py. See larky/src/main/resources/VENDORED.md.
#
# Ported: ECDH_Tests (P-256, all twelve) and X25519_Tests (RFC 7748
# Sections 5.2 and 6.1, and the weak keys). Not ported: the NIST KAT and
# Wycheproof vector files (Larky tests cannot read files) and X448.
#
# The tests after "Larky additions" are not upstream.

load("@stdlib//binascii", hexlify="hexlify", unhexlify="unhexlify")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//asserts", asserts="asserts")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/PublicKey/ECC", ECC="ECC")
load("@vendor//Crypto/Protocol/DH", DH="DH")
load("@vendor//option/result", Result="Result")

key_agreement = DH.key_agreement

static_priv = ECC.import_key('-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg9VHFVKh2a1aVFifH\n+BiyNaRa2kttEg3165Ye/dJxJ7KhRANCAARImIEXro5ZOcyWU2mq/+d79FEZXtTA\nbKkz1aICQXihQdCMzRNbeNtC9LFLzhu1slRKJ2xsDAlw9r6w6vwtkRzr\n-----END PRIVATE KEY-----')
static_pub = ECC.import_key('-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgHhmv8zmZ+Nw8fsZd\ns8tlZflyfw2NE1CRS9DWr3Y3O46hRANCAAS3hZVUCbk+uk3w4S/YOraEVGG+WYpk\nNO/vrwzufUUks2GV2OnBQESe0EBk4Jq8gn4ij8Lvs3rZX2yT+XfeATYd\n-----END PRIVATE KEY-----').public_key()

eph_priv = ECC.import_key('-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgGPdJmFFFKzLPspIr\nE1T2cEjeIf4ajS9CpneP0e2b3AyhRANCAAQBexAA5BYDcXHs2KOksTYUsst4HhPt\nkp0zkgI2virc3OGJFNGPaCCPfFCQJHwLRaEpiq3SoQlgoBwSc8ZPsl3y\n-----END PRIVATE KEY-----')

eph_pub = ECC.import_key('-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQghaVZXElSEGEojFKF\nOU0JCpxWUWHvWQUR81gwWrOp76ShRANCAATi1Ib2K+YR3AckD8wxypWef7pw5PRw\ntBaB3RDPyE7IjHZC6yu1DbcXoCdtaw+F5DM+4zpl59n5ZaIy/Yl1BdIy\n-----END PRIVATE KEY-----')


def kdf(x):
    return SHA256.new(x).digest()


def ECDH_Tests_test_1():
    # C(0, 2s)
    z = key_agreement(
            kdf=kdf,
            static_pub=static_pub,
            static_priv=static_priv)
    asserts.assert_that(hexlify(z)).is_equal_to(
                     b"3960a1101d1193cbaffef4cc7202ebff783c22c6d2e0d5d530ffc66dc197ea9c")


def ECDH_Tests_test_2():
    # C(2e, 2s)
    z = key_agreement(
            kdf=kdf,
            static_pub=static_pub,
            static_priv=static_priv,
            eph_pub=eph_pub,
            eph_priv=eph_priv)
    asserts.assert_that(hexlify(z)).is_equal_to(
                     b"7447b733d40c8fab2c633b3dc61e4a8c742f3a6af7e16fb0cc486f5bdb5d6ba2")


def ECDH_Tests_test_3():
    # C(1e, 2s)
    z = key_agreement(
            kdf=kdf,
            static_pub=static_pub,
            static_priv=static_priv,
            eph_priv=eph_priv)
    asserts.assert_that(hexlify(z)).is_equal_to(
                     b"9e977ae45f33bf67f285d064d83e6632bcafe3a7d33fe571233bab4794ace759")


def ECDH_Tests_test_4():
    # C(1e, 2s)
    z = key_agreement(
            kdf=kdf,
            static_pub=static_pub,
            static_priv=static_priv,
            eph_pub=eph_pub)
    asserts.assert_that(hexlify(z)).is_equal_to(
                     b"c9532df6aa7e9dbe5fe85da31ee25ff19c179c88691ec4b8328cc2036dcdadf2")


def ECDH_Tests_test_5():
    # C(2e, 1s) is not supported
    asserts.assert_fails(lambda: key_agreement(
            kdf=kdf,
            static_priv=static_priv,
            eph_pub=eph_pub,
            eph_priv=eph_priv), "^ValueError: DH mode C\\(2e, 1s\\) is not supported$")


def ECDH_Tests_test_6():
    # C(2e, 1s) is not supported
    asserts.assert_fails(lambda: key_agreement(
            kdf=kdf,
            static_pub=static_pub,
            eph_pub=eph_pub,
            eph_priv=eph_priv), "^ValueError: DH mode C\\(2e, 1s\\) is not supported$")


def ECDH_Tests_test_7():
    # C(2e, 0)
    z = key_agreement(
            kdf=kdf,
            eph_pub=eph_pub,
            eph_priv=eph_priv)
    asserts.assert_that(hexlify(z)).is_equal_to(
                     b"feb257ebe063078b1391aac07913283d7b642ad7df61b46dfc9cd6f420bb896a")


def ECDH_Tests_test_8():
    # C(1e, 1s)
    z = key_agreement(
            kdf=kdf,
            static_priv=static_priv,
            eph_pub=eph_pub)
    asserts.assert_that(hexlify(z)).is_equal_to(
                     b"ee4dc995117476ed57fd17ff0ed44e9f0466d46b929443bc0db9380317583b04")


def ECDH_Tests_test_9():
    # C(1e, 1s)
    z = key_agreement(
            kdf=kdf,
            static_pub=static_pub,
            eph_priv=eph_priv)
    asserts.assert_that(hexlify(z)).is_equal_to(
                     b"2351cc2014f7c40468fa072b5d30f706eeaeef7507311cd8e59bab3b43f03c51")


def ECDH_Tests_test_10():
    # No private (local) keys
    asserts.assert_fails(lambda: key_agreement(
            kdf=kdf,
            static_pub=static_pub,
            eph_pub=eph_pub), "^ValueError: Too few keys for the ECDH key agreement$")


def ECDH_Tests_test_11():
    # No public (peer) keys
    asserts.assert_fails(lambda: key_agreement(
            kdf=kdf,
            static_priv=static_priv,
            eph_priv=eph_priv), "^ValueError: Too few keys for the ECDH key agreement$")


def ECDH_Tests_test_12():
    # failure if kdf is missing
    asserts.assert_fails(lambda: key_agreement(
            static_pub=static_pub,
            static_priv=static_priv), "^ValueError: 'kdf' is mandatory$")


def X25519_Tests_test_rfc7748_1():
    tvs = (
            ("a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4",
             "e6db6867583030db3594c1a424b15f7c726624ec26b3353b10a903a6d0ab1c4c",
             "c3da55379de9c6908e94ea4df28d084f32eccf03491c71f754b4075577a28552"),
            ("4b66e9d4d1b4673c5ad22691957d6af5c11b6421e0ea01d42ca4169e7918ba0d",
             "e5210f12786811d3f4b7959d0538ae2c31dbe7106fc03c3efc4cd549c715a493",
             "95cbde9476e8907d7aade45cb4b873f88b595a68799fa152e6f8f7647aac7957"),
    )

    for tv1, tv2, tv3 in tvs:
        priv_key = DH.import_x25519_private_key(unhexlify(tv1))
        pub_key = DH.import_x25519_public_key(unhexlify(tv2))
        result = key_agreement(static_pub=pub_key,
                               static_priv=priv_key,
                               kdf=lambda x: x)
        asserts.assert_that(result).is_equal_to(unhexlify(tv3))


def X25519_Tests_test_rfc7748_2():
    k = unhexlify("0900000000000000000000000000000000000000000000000000000000000000")

    priv_key = DH.import_x25519_private_key(k)
    pub_key = DH.import_x25519_public_key(k)
    result = key_agreement(static_pub=pub_key,
                           static_priv=priv_key,
                           kdf=lambda x: x)
    asserts.assert_that(result).is_equal_to(
        unhexlify("422c8e7a6227d7bca1350b3e2bb7279f7897b87bb6854b783c60e80311ae3079")
    )

    for _ in range(999):
        priv_key = DH.import_x25519_private_key(result)
        pub_key = DH.import_x25519_public_key(k)
        k = result
        result = key_agreement(static_pub=pub_key,
                               static_priv=priv_key,
                               kdf=lambda x: x)

    asserts.assert_that(result).is_equal_to(
        unhexlify("684cf59ba83309552800ef566f2f4d3c1c3887c49360e3875f2eb94d99532c51")
    )


def X25519_Tests_test_rfc7748_3():
    tv1 = "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"
    tv2 = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a"
    tv3 = "5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb"
    tv4 = "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f"
    tv5 = "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742"

    alice_priv_key = DH.import_x25519_private_key(unhexlify(tv1))
    alice_pub_key = DH.import_x25519_public_key(unhexlify(tv2))
    bob_priv_key = DH.import_x25519_private_key(unhexlify(tv3))
    bob_pub_key = DH.import_x25519_public_key(unhexlify(tv4))
    secret = unhexlify(tv5)

    result1 = key_agreement(static_pub=alice_pub_key,
                            static_priv=bob_priv_key,
                            kdf=lambda x: x)
    result2 = key_agreement(static_pub=bob_pub_key,
                            static_priv=alice_priv_key,
                            kdf=lambda x: x)
    asserts.assert_that(result1).is_equal_to(secret)
    asserts.assert_that(result2).is_equal_to(secret)


def X25519_Tests_test_weak():

    weak_keys = (
        "0000000000000000000000000000000000000000000000000000000000000000",
        "0100000000000000000000000000000000000000000000000000000000000000",
        "e0eb7a7c3b41b8ae1656e3faf19fc46ada098deb9c32b1fd866205165f49b800",
        "5f9c95bca3508c24b1d0b1559c83ef5b04445cc4581c8e86d8224eddd09f1157",
        "ecffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f",
        "edffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f",
        "eeffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f",
        # The implementation will accept these value, but only because
        # it will set the MSB to zero (as required by RFC7748, Section 5),
        # therefore leading to another public key (and to a point which is
        # not of low order anymore).
        # "cdeb7a7c3b41b8ae1656e3faf19fc46ada098deb9c32b1fd866205165f49b880",
        # "4c9c95bca3508c24b1d0b1559c83ef5b04445cc4581c8e86d8224eddd09f11d7",
        # "d9ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        # "daffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        # "dbffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
    )

    for x in weak_keys:
        asserts.assert_fails(lambda: DH.import_x25519_public_key(unhexlify(x)),
                             "^ValueError: Invalid Curve25519 public key$")


# Larky additions

def _iterate_x25519(k, u, n):
    # RFC 7748, Section 5.2: k, u = X25519(k, u), k
    X25519 = _JCrypto.PublicKey.ECC.X25519
    for _ in range(n):
        k, u = X25519.x25519(k, u), k
    return k


def Larky_test_rfc7748_iterations_primitive():
    """RFC 7748, Section 5.2, iterated on the jcrypto primitive directly."""
    nine = unhexlify("0900000000000000000000000000000000000000000000000000000000000000")
    asserts.assert_that(hexlify(_iterate_x25519(nine, nine, 1))).is_equal_to(
        b"422c8e7a6227d7bca1350b3e2bb7279f7897b87bb6854b783c60e80311ae3079")
    asserts.assert_that(hexlify(_iterate_x25519(nine, nine, 1000))).is_equal_to(
        b"684cf59ba83309552800ef566f2f4d3c1c3887c49360e3875f2eb94d99532c51")
    # The 1,000,000-iteration vector (7c3911e0...f665424) is not run: it
    # takes minutes.


def Larky_test_primitive_rejects_bad_lengths():
    X25519 = _JCrypto.PublicKey.ECC.X25519
    k = b"\x01" * 32
    asserts.assert_fails(lambda: X25519.x25519(k, b"\x09"),
                         "x25519\\(u=...\\) expected 32 bytes; got 1 bytes")
    asserts.assert_fails(lambda: X25519.x25519(b"", k),
                         "x25519\\(scalar=...\\) expected 32 bytes; got 0 bytes")
    asserts.assert_fails(lambda: X25519.x25519("x" * 32, k), "got .*string")


def Larky_test_primitive_all_zero_is_none():
    """X25519 of a small-order u is all zero; the primitive returns None."""
    X25519 = _JCrypto.PublicKey.ECC.X25519
    k = unhexlify("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a")
    for u in ("0000000000000000000000000000000000000000000000000000000000000000",
              "0100000000000000000000000000000000000000000000000000000000000000",
              "e0eb7a7c3b41b8ae1656e3faf19fc46ada098deb9c32b1fd866205165f49b800",
              "5f9c95bca3508c24b1d0b1559c83ef5b04445cc4581c8e86d8224eddd09f1157",
              "ecffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f"):
        asserts.assert_that(X25519.x25519(k, unhexlify(u))).is_none()


def Larky_test_all_zero_shared_secret_refused():
    """A key that bypasses import validation still cannot yield Z = 0."""
    priv = DH.import_x25519_private_key(
        unhexlify("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"))
    x1 = 325606250916557431795983626356110631294008115727848805560023387167927233504
    for x in (0, 1, x1):
        # EccKey() does not validate the point, construct() and the importers do.
        weak = ECC.EccKey(curve="Curve25519", point=ECC.EccXPoint(x, "Curve25519"))
        asserts.assert_fails(lambda: key_agreement(static_priv=priv, static_pub=weak, kdf=lambda z: z),
                             "^ValueError: Invalid ECDH point$")
        asserts.assert_fails(lambda: ECC.construct(curve="Curve25519", point_x=x),
                             "^ValueError: Invalid Curve25519 public key$")


def Larky_test_rfc7518_appendix_c_z():
    """Z of RFC 7518, Appendix C (P-256), through the JCA-backed path."""
    # The JWKs of RFC 7518, Appendix C, as integers.
    alice = ECC.construct(curve="P-256",
                          d=0xd3f3716913d4310a0026de741b3f18893afc8114f0c84682ba677e313a13988a,
                          point_x=0x808d060082c176eed3e776a4ac598cc8672c1779f974eecc9b03411ca5b9495d,
                          point_y=0x48b5bfc527dfce53d6ac7115237d031ccff87a0570b77350a9e503ee7305a69b)
    bob = ECC.construct(curve="P-256",
                        d=0x5449836690d75caf29f0dd029ddb31b3ddb8aba9d2d515c5012465e817d4a9dc,
                        point_x=0xc1e349cb61ec70248ce801034c3834e1b88ebe1161cb25af38741f785fcfc4c4,
                        point_y=0x7bc96708ef80952b53f8d2555fe72b841ed04588628b1d378a594939500ec9c9)
    z = bytes([158, 86, 217, 29, 129, 113, 53, 211, 114, 131, 66, 131, 191, 132,
               38, 156, 251, 49, 110, 163, 218, 128, 106, 72, 246, 218, 167, 121,
               140, 254, 144, 196])
    asserts.assert_that(key_agreement(eph_priv=alice, static_pub=bob.public_key(), kdf=lambda x: x)).is_equal_to(z)
    asserts.assert_that(key_agreement(static_priv=bob, eph_pub=alice.public_key(), kdf=lambda x: x)).is_equal_to(z)


def Larky_test_curve25519_generate_agreement():
    a = ECC.generate(curve="Curve25519")
    b = ECC.generate(curve="X25519")
    z1 = key_agreement(static_priv=a, static_pub=b.public_key(), kdf=lambda x: x)
    z2 = key_agreement(static_priv=b, static_pub=a.public_key(), kdf=lambda x: x)
    asserts.assert_that(len(z1)).is_equal_to(32)
    asserts.assert_that(z1).is_equal_to(z2)


def Larky_test_argument_errors():
    x = ECC.generate(curve="Curve25519")
    p = ECC.generate(curve="P-256")
    asserts.assert_fails(lambda: key_agreement(static_priv=x, static_pub=p.public_key(), kdf=kdf),
                         "^TypeError: 'static_pub' is defined on an incompatible curve$")
    asserts.assert_fails(lambda: key_agreement(static_priv=x.public_key(), static_pub=x, kdf=kdf),
                         "^TypeError: 'static_priv' must be a private ECC key$")
    asserts.assert_fails(lambda: key_agreement(static_priv="key", static_pub=x, kdf=kdf),
                         "^TypeError: 'static_priv' must be an ECC key$")
    asserts.assert_fails(lambda: DH.import_x25519_public_key("ab" * 16),
                         "^TypeError: import_x25519_public_key\\(encoded=...\\) expected `bytes`; got string$")
    asserts.assert_fails(lambda: DH.import_x25519_private_key(None),
                         "^TypeError: import_x25519_private_key\\(encoded=...\\) expected `bytes`; got NoneType$")
    asserts.assert_fails(lambda: DH.import_x25519_public_key(b"\x09" * 31),
                         "^ValueError: Incorrect Curve25519 key length$")
    asserts.assert_fails(lambda: DH.import_x25519_private_key(b"\x09" * 33),
                         "^ValueError: Parameter seed must be 32 bytes long for Curve25519$")


def Larky_test_unvalidated_p256_point_refused():
    # Every import and construct path checks that the point is on the curve,
    # so put a P-384 point on a P-256 key by hand to reach the check on
    # @stdlib//ECDH's result.
    p384 = ECC.generate(curve="P-384").pointQ
    pub = ECC.generate(curve="P-256").public_key()
    pub._point = ECC.EccPoint(p384.x, p384.y, "p384")
    asserts.assert_fails(lambda: key_agreement(static_priv=static_priv, static_pub=pub, kdf=kdf),
                         "^ValueError: Invalid ECDH point$")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_1))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_2))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_3))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_4))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_5))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_6))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_7))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_8))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_9))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_10))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_11))
    _suite.addTest(unittest.FunctionTestCase(ECDH_Tests_test_12))
    _suite.addTest(unittest.FunctionTestCase(X25519_Tests_test_rfc7748_1))
    _suite.addTest(unittest.FunctionTestCase(X25519_Tests_test_rfc7748_2))
    _suite.addTest(unittest.FunctionTestCase(X25519_Tests_test_rfc7748_3))
    _suite.addTest(unittest.FunctionTestCase(X25519_Tests_test_weak))
    # Larky additions
    _suite.addTest(unittest.FunctionTestCase(Larky_test_rfc7748_iterations_primitive))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_primitive_rejects_bad_lengths))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_primitive_all_zero_is_none))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_all_zero_shared_secret_refused))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_rfc7518_appendix_c_z))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_curve25519_generate_agreement))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_argument_errors))
    _suite.addTest(unittest.FunctionTestCase(Larky_test_unvalidated_p256_point_refused))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
