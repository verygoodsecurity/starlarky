load("@vendor//ecdsa/keys", SigningKey="SigningKey")
load("@vendor//ecdsa/der", unpem="unpem")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")

eq = asserts.eq

converters = [lambda x: x]
# for modifier, convert in [
#     ("bytes", lambda x: x),
    # ("bytes memoryview", buffer),
    # ("bytearray", bytearray),
    # ("bytearray memoryview", lambda x: buffer(bytearray(x))),
    # ("array.array of bytes", lambda x: array.array("B", x)),
    # ("array.array of bytes memoryview", lambda x: buffer(array.array("B", x))),
    # ("array.array of ints", lambda x: array.array("I", x)),
    # ("array.array of ints memoryview", lambda x: buffer(array.array("I", x))),
# ]:
    # converters.append(pytest.param(convert, id=modifier))

# test SigningKey.from_der()
prv_key_str = (
'''-----BEGIN EC PRIVATE KEY-----
MHQCAQEEIC0Z8jqyougsFYzTNxM1Vk4lqh8lDDMUPt9V4rJD1OKYoAcGBSuBBAAK
oUQDQgAEcTo5LIxrMAOSAwu6fHKB0BtlCAvOzhEecZ5N4f5xLfkPrPFSSHxgLDcD
MDUzwBcKAPBV+zBdKsM8wDyO47GYqw==
-----END EC PRIVATE KEY-----'''
)
key_bytes = unpem(prv_key_str)

def test_SigningKey_from_der():
    for convert in converters:
        key = convert(key_bytes)
        sk = SigningKey(True).from_der(key)

        # eq(sk.to_string(), prv_key_bytes)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_SigningKey_from_der))
    return _suite

# _runner = unittest.TextTestRunner()
# _runner.run(_testsuite())