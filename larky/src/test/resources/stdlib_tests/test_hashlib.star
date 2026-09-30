load("@stdlib//builtins", "builtins")
load("@stdlib//hashlib", "hashlib")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


b = builtins.bytes

def _test_md5_basic():
    asserts.eq(hashlib.md5(b("hello")).hexdigest(), '5d41402abc4b2a76b9719d911017c592')
    # TypeError: Unicode-objects must be encoded before hashing
    # asserts.eq(hashlib.md5("hello").hexdigest(), '5d41402abc4b2a76b9719d911017c592')


def _test_new():
    # names and digests from CPython 3's hashlib.new(name, b"abc")
    abc_hexdigests = {
        "md5": "900150983cd24fb0d6963f7d28e17f72",
        "MD5": "900150983cd24fb0d6963f7d28e17f72",
        "ssl3-md5": "900150983cd24fb0d6963f7d28e17f72",
        "sha1": "a9993e364706816aba3e25717850c26c9cd0d89d",
        "SHA-1": "a9993e364706816aba3e25717850c26c9cd0d89d",
        "SHA224": "23097d223405d8228642a477bda255b32aadbce4bda0b3f7e36c9da7",
        "sha2-224": "23097d223405d8228642a477bda255b32aadbce4bda0b3f7e36c9da7",
        "sha256": "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        "SHA256": "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        "sha-256": "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        "SHA-384": "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7",
        "SHA512": "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
        "blake2s": "508c5e8c327c14e2e1a72ba34eeb452f37458b209ed63a294d999b4c86675982",
        "BLAKE2S256": "508c5e8c327c14e2e1a72ba34eeb452f37458b209ed63a294d999b4c86675982",
    }
    for name, expected in abc_hexdigests.items():
        asserts.assert_that(hashlib.new(name, b"abc").hexdigest()).is_equal_to(expected)
    # (Larky's shake_128 object has pycryptodome's read(), not hexdigest(length))
    asserts.assert_that(hashlib.new("SHAKE128", b"abc").read(16)).is_equal_to(
        b"\x58\x81\x09\x2d\xd8\x18\xbf\x5c\xf8\xa3\xdd\xb7\x93\xfb\xcb\xa7")
    asserts.assert_that(hashlib.blake2s(b"abc").hexdigest()).is_equal_to(
        "508c5e8c327c14e2e1a72ba34eeb452f37458b209ed63a294d999b4c86675982")

    for name in ("nope", "BLAKE2s", "RSA-SHA256", ""):
        asserts.assert_fails(lambda: hashlib.new(name), "unsupported hash type %s$" % name)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_md5_basic))
    _suite.addTest(unittest.FunctionTestCase(_test_new))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())