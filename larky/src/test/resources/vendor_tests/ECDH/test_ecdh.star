load("@stdlib//base64", base64="base64")
load("@stdlib//unittest","unittest")
load("@vendor//asserts","asserts")
load("@vendor//larky_ecdh", "ecdh")

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
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAERac12qEdVcn6gMlIB44sCqW6zFZi
Gcgl8g9+8E4S7xjTarje7g9QDcb7F5zDYkqoVFG8pWfzGHCtNCK5zv0uUA==
-----END PUBLIC KEY-----"""

public_key_ephemeral = b"""MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEMwliotf2ICjiMwREdqyHSilqZzuV2fZey86nBIDlTY8sNMJv9CPpL5/DKg4bIEMe6qaj67mz4LWdr7Er0Ld5qA=="""


def test_generated_keypair():
    shared_secret = ecdh.exchange(
        private_key_pkcs8, "PKCS8",
        public_key_x509, "X509")
    expected = b"yIk6UBFxABvvo1fao/V4DyZ1DX7TE66T58f05zEM0LA="
    asserts.assert_that(base64.b64encode(shared_secret)).is_equal_to(expected)


def test_halturin_keypair():
    """
    Test the inputs from Halturin's Apple Pay library
    """
    shared_secret = ecdh.exchange(
        private_key_PEM, "PEM",
        public_key_ephemeral, "X509")
    expected = b"a2pPfemSdA560FnzLSv8zfdlWdGJTonApOLq1zfgx8w="
    asserts.assert_that(base64.b64encode(shared_secret)).is_equal_to(expected)


def test_pkcs12_keypair():
    """
    PKCS12 Keystores require a password to be compatible.
    """
    shared_secret = ecdh.exchange(
        pkcs12_keystore, "PKCS12",
        public_key_x509, "X509",
        private_pass="vgs"
    )
    expected = b"FOc7tXpSy1xM/4hw7lVyjf8QqYfg0agsnbIdgdCn+Tk="
    asserts.assert_that(base64.b64encode(shared_secret)).is_equal_to(expected)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_generated_keypair))
    _suite.addTest(unittest.FunctionTestCase(test_halturin_keypair))
    _suite.addTest(unittest.FunctionTestCase(test_pkcs12_keypair))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
