load("@stdlib//base64", base64="base64")
load("@stdlib//unittest","unittest")
load("@vendor//asserts","asserts")
load("@vendor//larky_ecdh", "LarkyECDH")

# https://verygoodsecurity.atlassian.net/browse/SI-203
# nosemgrep: secrets.misc.generic_private_key.generic_private_key
private_key_pkcs8 = b"""-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgmz1M0Qw9vHLQlIR2
6LK2CEUyHFARQ7KrrBSWJjqVOlmhRANCAARFpzXaoR1VyfqAyUgHjiwKpbrMVmIZ
yCXyD37wThLvGNNquN7uD1ANxvsXnMNiSqhUUbylZ/MYcK00IrnO/S5Q
-----END PRIVATE KEY-----"""

pkcs12_keystore = b"""MIIBmwIBAzCCAWIGCSqGSIb3DQEHAaCCAVMEggFPMIIBSzCCAUcGCSqGSIb3DQEHAaCCATgEggE0MIIBMDCCASwGCyqGSIb3DQEMCgECoIG8MIG5MBwGCiqGSIb3DQEMAQMwDgQIBUdleOxVSwkCAggABIGYuv4nS1kIU2SgJhyMh9A1oew/yGmKfm60mDnRqcnAd2hoISSOhg+ieCoWQWRjSL/4i0hph1ArhRLWaPYYPuQCHXBIE681QEMe9a4GeVrF2DFIzsOczn19RQAYb3LaXMgcdKiiCGxC58duPgIt91yes/vsfk84iHcRY86q/iV7yh87WqeqqhDe+VUFn9DoUGcQcr8p6TQ+rh0xXjA3BgkqhkiG9w0BCRQxKh4oAFYAZQByAHkARwBvAG8AZABTAGUAYwB1AHIAaQB0AHkAIABEAGUAdjAjBgkqhkiG9w0BCRUxFgQUr3uI24KVinVD+7ITCMI/HigMdWwwMDAhMAkGBSsOAwIaBQAEFEky3g45rWcjiU5+WmIZMNQ5P8kmBAjBn2X9CBPm9AIBAQ=="""

# https://verygoodsecurity.atlassian.net/browse/SI-203
# nosemgrep: secrets.misc.generic_private_key.generic_private_key
private_key_PEM = b"""-----BEGIN EC PRIVATE KEY-----
MHcCAQEEIDqrpF0KEFW4Ncb76vyBi3StFLiT222sFC0wC3LsP1M9oAoGCCqGSM49
AwEHoUQDQgAED44gNZExKHUk9sMuXeZEBazNA+agV/VJK8vFug/rwuzqmzKE5v7q
UTNRkR3gNi2lU68AJ6RoaDtBE6mBdjbuFQ==
-----END EC PRIVATE KEY-----"""

public_key_x509 = b"""-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEgJOZmQJwFpa4reM893yNFSxXV5/f
aPBDWKsJNnEgfrH8hhKqPrR5f3dhxhkGLJgOB/PAk1XuUfUbZ7hz1rSX8A==
-----END PUBLIC KEY-----"""

public_key_ephemeral = b"""MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEMwliotf2ICjiMwREdqyHSilqZzuV2fZey86nBIDlTY8sNMJv9CPpL5/DKg4bIEMe6qaj67mz4LWdr7Er0Ld5qA=="""

# Throwaway P-256 key pairs (generated with openssl for this test only).
# nosemgrep: secrets.misc.generic_private_key.generic_private_key
multiline_private_a = b"""-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgVmbZNl4Jkc8DBmKC
tXGP2/tGdZs/nU42YX0nbrN1tw2hRANCAASjU9PHSFdlJlGH1ZkDovsz5KRwC3aD
lH4V20FWrasgSRNLp6UFMfpyEi07R5V9Et9zLGLmPG25x8bZGEMJWPHU
-----END PRIVATE KEY-----"""

multiline_public_a = b"""-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEo1PTx0hXZSZRh9WZA6L7M+SkcAt2
g5R+FdtBVq2rIEkTS6elBTH6chItO0eVfRLfcyxi5jxtucfG2RhDCVjx1A==
-----END PUBLIC KEY-----"""

# nosemgrep: secrets.misc.generic_private_key.generic_private_key
multiline_private_b = b"""-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgM+lA0a1Bb05Cb2Lw
EL5CUNB9due1SaWsdvFqG0HvuhihRANCAAQRwuaVauRpxBjc5W0v8Z451LDZ5GZO
vXvWaAoUd1Dd9+EJBGi08oCnn4k30izlemYDQp+wYCvAOjSkfLRmpLaw
-----END PRIVATE KEY-----"""

multiline_public_b = b"""-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEcLmlWrkacQY3OVtL/GeOdSw2eRm
Tr171mgKFHdQ3ffhCQRotPKAp5+JN9Is5XpmA0KfsGArwDo0pHy0ZqS2sA==
-----END PUBLIC KEY-----"""

def test_generated_keypair():
    ecdh = LarkyECDH()
    ecdh.set_private_key(private_key_pkcs8, type="PKCS8")
    ecdh.set_public_key(public_key_x509, "X509")
    shared_secret = ecdh.exchange()
    expected = b"9/mnqq+gMfEbBiKX+pvzat+aCFLvn415Ez4NGbzLUHM="
    asserts.assert_that(base64.b64encode(shared_secret)).is_equal_to(expected)


def test_halturin_keypair():
    """
    Test the inputs from Halturin's Apple Pay library that this is being imported for
    """
    ecdh = LarkyECDH()
    ecdh.set_private_key(private_key_PEM, type="PEM")
    ecdh.set_public_key(public_key_ephemeral, "X509")
    shared_secret = ecdh.exchange()
    expected = b"a2pPfemSdA560FnzLSv8zfdlWdGJTonApOLq1zfgx8w="
    asserts.assert_that(base64.b64encode(shared_secret)).is_equal_to(expected)


def test_pkcs12_keypair():
    """
    PKCS12 Keystores require a password to be compatible.
    """
    ecdh = LarkyECDH()
    ecdh.set_private_key(pkcs12_keystore, type="PKCS12", passwd="vgs")
    ecdh.set_public_key(public_key_x509, "X509")
    shared_secret = ecdh.exchange()
    print(base64.b64encode(shared_secret))
    expected = b"QbflyOdxe/N3bl44eMZplnVen0RAZweSUUyG+wEpwOk="
    asserts.assert_that(base64.b64encode(shared_secret)).is_equal_to(expected)


def test_bad_keystore_pass():
    ecdh = LarkyECDH()
    ecdh.set_private_key(pkcs12_keystore, type="PKCS12", passwd="badpass")
    ecdh.set_public_key(public_key_x509, "X509")
    asserts.assert_fails(lambda: ecdh.exchange(), "Integrity check failed: java.security.UnrecoverableKeyException: Failed PKCS12 integrity checking")


def test_no_public():
    ecdh = LarkyECDH()
    ecdh.set_private_key(private_key_pkcs8, type="PKCS8")
    asserts.assert_fails(lambda: ecdh.exchange(), "No public key set")


def test_no_private():
    ecdh = LarkyECDH()
    ecdh.set_public_key(public_key_x509, type="X509")
    asserts.assert_fails(lambda: ecdh.exchange(), "No private key set")


def test_no_public_type():
    ecdh = LarkyECDH()
    ecdh.set_private_key(private_key_pkcs8, type="PKCS8")
    asserts.assert_fails(lambda: ecdh.set_public_key(public_key_x509, "SomethingWrong"), "Unsupported public key type")


def test_no_private_type():
    ecdh = LarkyECDH()
    asserts.assert_fails(lambda: ecdh.set_private_key(private_key_pkcs8, type="SomethingWrong"), "Unsupported private key type")


def test_bad_public_key():
    ecdh = LarkyECDH()
    ecdh.set_private_key(private_key_pkcs8, type="PKCS8")
    # Corrupt a correctly structured key (ie, it is bytes, and leaves only valid base64 when stripping out
    # key headers/footers and newlines), and it will fail when you do the exchange.
    ecdh.set_public_key(public_key_x509.replace(b"a", b"b"), type="X509")
    asserts.assert_fails(lambda: ecdh.exchange(), "")

    # It expects bytes structured like above, not a string.
    asserts.assert_fails(lambda: ecdh.set_public_key(public_key_x509.decode("utf-8"), "X509"), "value has no field or method 'decode'")
    asserts.assert_fails(lambda: ecdh.set_public_key(b"A random phrase that isn't a key", "X509"), "Invalid base64-encoded string")


def test_bad_private_key():
    ecdh = LarkyECDH()
    ecdh.set_private_key(private_key_pkcs8.replace(b"a",b"b"), type="PKCS8")
    ecdh.set_public_key(public_key_x509, "X509")
    asserts.assert_fails(lambda: ecdh.exchange(), "")
    asserts.assert_fails(lambda: ecdh.set_private_key(private_key_pkcs8.decode("utf-8"), type="PKCS8"), "value has no field or method 'decode'")
    asserts.assert_fails(lambda: ecdh.set_private_key(b"A random phrase that isn't a key", type="PKCS8"), "Invalid base64-encoded string")


def _exchange(private_key, public_key):
    ecdh = LarkyECDH()
    ecdh.set_private_key(private_key, type="PKCS8")
    ecdh.set_public_key(public_key, "X509")
    return ecdh.exchange()


def test_multiline_pem_both_sides_agree():
    """
    Multi-line PEM-wrapped PKCS8 private keys and X509 public keys (64-column bodies, as openssl
    writes them) must give the same secret on both sides of the exchange.
    """
    secret_a = _exchange(multiline_private_a, multiline_public_b)
    secret_b = _exchange(multiline_private_b, multiline_public_a)
    asserts.assert_that(len(secret_a)).is_equal_to(32)
    asserts.assert_that(base64.b64encode(secret_a)).is_equal_to(base64.b64encode(secret_b))


def test_multiline_pem_crlf_both_sides_agree():
    """Same as above with CRLF line endings, as keys pasted from Windows tools have."""
    secret_lf = _exchange(multiline_private_a, multiline_public_b)
    secret_crlf_a = _exchange(
        multiline_private_a.replace(b"\n", b"\r\n"),
        multiline_public_b.replace(b"\n", b"\r\n"),
    )
    secret_crlf_b = _exchange(
        multiline_private_b.replace(b"\n", b"\r\n"),
        multiline_public_a.replace(b"\n", b"\r\n"),
    )
    asserts.assert_that(base64.b64encode(secret_crlf_a)).is_equal_to(base64.b64encode(secret_lf))
    asserts.assert_that(base64.b64encode(secret_crlf_b)).is_equal_to(base64.b64encode(secret_lf))


def test_multiline_equals_single_line_body():
    """The body with its newlines removed by hand must give the same secret as the multi-line PEM."""
    one_line = b"".join([
        l for l in multiline_public_b.split(b"\n") if not l.startswith(b"-----")
    ])
    asserts.assert_that(base64.b64encode(_exchange(multiline_private_a, one_line))).is_equal_to(
        base64.b64encode(_exchange(multiline_private_a, multiline_public_b))
    )



def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_generated_keypair))
    _suite.addTest(unittest.FunctionTestCase(test_halturin_keypair))
    _suite.addTest(unittest.FunctionTestCase(test_pkcs12_keypair))
    _suite.addTest(unittest.FunctionTestCase(test_bad_keystore_pass))
    _suite.addTest(unittest.FunctionTestCase(test_no_public))
    _suite.addTest(unittest.FunctionTestCase(test_no_private))
    _suite.addTest(unittest.FunctionTestCase(test_no_public_type))
    _suite.addTest(unittest.FunctionTestCase(test_no_private_type))
    _suite.addTest(unittest.FunctionTestCase(test_bad_public_key))
    _suite.addTest(unittest.FunctionTestCase(test_bad_private_key))
    _suite.addTest(unittest.FunctionTestCase(test_multiline_pem_both_sides_agree))
    _suite.addTest(unittest.FunctionTestCase(test_multiline_pem_crlf_both_sides_agree))
    _suite.addTest(unittest.FunctionTestCase(test_multiline_equals_single_line_body))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
