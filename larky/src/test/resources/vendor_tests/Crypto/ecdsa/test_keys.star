load("@vendor//ecdsa/keys", SigningKey="SigningKey")
load("@vendor//ecdsa/der", unpem="unpem")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")

eq = asserts.eq

converters = []
for modifier, convert in [
    ("bytes", lambda x: x),
    # ("bytes memoryview", buffer),
    # ("bytearray", bytearray),
    # ("bytearray memoryview", lambda x: buffer(bytearray(x))),
    # ("array.array of bytes", lambda x: array.array("B", x)),
    # ("array.array of bytes memoryview", lambda x: buffer(array.array("B", x))),
    # ("array.array of ints", lambda x: array.array("I", x)),
    # ("array.array of ints memoryview", lambda x: buffer(array.array("I", x))),
]:
    # converters.append(pytest.param(convert, id=modifier))
    converters.append(convert)

# test SigningKey.from_der()
prv_key_str = (
    "-----BEGIN EC PRIVATE KEY-----\n"
    "MF8CAQEEGF7IQgvW75JSqULpiQQ8op9WH6Uldw6xxaAKBggqhkjOPQMBAaE0AzIA\n"
    "BLiBd9CE7xf15FY5QIAoNg+fWbSk1yZOYtoGUdzkejWkxbRc9RWTQjqLVXucIJnz\n"
    "bA==\n"
    "-----END EC PRIVATE KEY-----\n"
)
key_bytes = unpem(prv_key_str)

def test_SigningKey_from_der():
    for convert in converters:
        key = convert(key_bytes)
        sk = SigningKey().from_der(key)

        # eq(sk.to_string(), prv_key_bytes)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_SigningKey_from_der))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())