load("@stdlib//base64", base64="base64")
load("@stdlib//unittest","unittest")
load("@vendor//asserts","asserts")
load("@vendor//larky_ecdh", "LarkyECDH")

private_key_pkcs8 = b"""-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgmz1M0Qw9vHLQlIR2
6LK2CEUyHFARQ7KrrBSWJjqVOlmhRANCAARFpzXaoR1VyfqAyUgHjiwKpbrMVmIZ
yCXyD37wThLvGNNquN7uD1ANxvsXnMNiSqhUUbylZ/MYcK00IrnO/S5Q
-----END PRIVATE KEY-----"""

pkcs12_keystore = b"""MIIBmwIBAzCCAWIGCSqGSIb3DQEHAaCCAVMEggFPMIIBSzCCAUcGCSqGSIb3DQEHAaCCATgEggE0MIIBMDCCASwGCyqGSIb3DQEMCgECoIG8MIG5MBwGCiqGSIb3DQEMAQMwDgQIBUdleOxVSwkCAggABIGYuv4nS1kIU2SgJhyMh9A1oew/yGmKfm60mDnRqcnAd2hoISSOhg+ieCoWQWRjSL/4i0hph1ArhRLWaPYYPuQCHXBIE681QEMe9a4GeVrF2DFIzsOczn19RQAYb3LaXMgcdKiiCGxC58duPgIt91yes/vsfk84iHcRY86q/iV7yh87WqeqqhDe+VUFn9DoUGcQcr8p6TQ+rh0xXjA3BgkqhkiG9w0BCRQxKh4oAFYAZQByAHkARwBvAG8AZABTAGUAYwB1AHIAaQB0AHkAIABEAGUAdjAjBgkqhkiG9w0BCRUxFgQUr3uI24KVinVD+7ITCMI/HigMdWwwMDAhMAkGBSsOAwIaBQAEFEky3g45rWcjiU5+WmIZMNQ5P8kmBAjBn2X9CBPm9AIBAQ=="""

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
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
