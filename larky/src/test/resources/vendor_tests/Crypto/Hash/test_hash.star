load("@stdlib//builtins", builtins="builtins")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//Crypto/Hash/MD5", MD5="MD5")
load("@vendor//Crypto/Hash/SHA1", SHA1="SHA1")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/Hash/SHA384", SHA384="SHA384")
load("@vendor//Crypto/Hash/SHA512", SHA512="SHA512")
load("@vendor//Crypto/Hash/SHA3_256", SHA3_256="SHA3_256")
load("@vendor//Crypto/Hash/keccak", keccak="keccak")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")


def b(s):
    return tobytes(s)

eq = asserts.eq

def MD5_test():
    h = MD5.new()
    h.update(b("hello"))
    eq(h.hexdigest(), '5d41402abc4b2a76b9719d911017c592')

def SHA1_test():
    h = SHA1.new()
    h.update(b("hello"))
    eq(h.hexdigest(), 'aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d')

def SHA512_test():
    h = SHA512.new()
    h.update(b("hello"))
    eq(h.hexdigest(), '9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043')
    
    h = SHA512.new(b('testtest'), truncate="256")
    eq(h.hexdigest(), '14f314274868f80ab1fe84e219c7a0e30e5645593509dc67b50edd2a59d0500d')

    h = SHA512.new(b('testtest'), truncate="224")
    eq(h.hexdigest(), '353f2beed3409bae708d05b8c33dc4b01ce1723194b215f9b0f2f40e')

def SHA3_test():
    h = SHA384.new()
    h.update(b"Some data")
    asserts.assert_that(h.hexdigest()).is_equal_to('eda5fa67268805ce0ffd8b7e9f955400b48d035096215104645d0f127353f5cb9334c2ddcba11e40bd7dada5df895ab6')

def SHA256_test():
    h = SHA256.new()
    h.update(b"Test Data!")
    asserts.assert_that(h.hexdigest()).is_equal_to('375990365a2852e275370f4c4250b0ae900d78cda4d8195296991616067b8402')

def SHA3_256_test():
    h = SHA3_256.new()
    h.update(b("Some data"))
    # print('sha3_256 hexdigest: ', h.hexdigest())
    eq(h.hexdigest(), '86b8648658d163a47203c7101c327eb8434d741aa4b14b1b3ff9c08ba723bcd1')
    # update_after_digest is false by default
    asserts.assert_fails(lambda : h.update(b("new text")), ".*?TypeError")

    asserts.assert_fails(lambda : SHA3_256.new(data='test', invalid_key='key'), ".*?TypeError")

    h = SHA3_256.new(b("Some data"), update_after_digest=True)
    h.hexdigest()
    h.update(b("new text"))

def Keccak_test():
    h = keccak.new(digest_bits=256)
    h.update(b("test"))
    eq(h.hexdigest(), '9c22ff5f21f0b81b113e63f7db6da94fedef11b2119b4088b89664fb9a3cb658')

    h = keccak.new(digest_bits=512)
    h.update(b("test"))
    eq(h.hexdigest(), '1e2e9fc2002b002d75198b7503210c05a1baac4560916a3c6d93bcce3a50d7f00fd395bf1647b9abb8d1afcc9c76c289b0c9383ba386a956da4b38934417789e')


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(SHA1_test))
    _suite.addTest(unittest.FunctionTestCase(SHA512_test))
    _suite.addTest(unittest.FunctionTestCase(SHA3_256_test))
    _suite.addTest(unittest.FunctionTestCase(SHA3_test))
    _suite.addTest(unittest.FunctionTestCase(SHA256_test))
    _suite.addTest(unittest.FunctionTestCase(Keccak_test))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())