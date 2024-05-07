load("@stdlib//larky", "larky")
load("@stdlib//unittest","unittest")
load("@stdlib//json","json")
load("@stdlib//types", "types")
load("@stdlib//builtins", builtins="builtins")

load("@vendor//asserts","asserts")

load("@vgs//http/request", "VGSHttpRequest")

actual_header_key = 'case-sensitive'
expected_header_key = actual_header_key.upper()
header_value = 'Header Value'
headers = {actual_header_key : header_value}


def test_read_case_insensitive_headers():
    request = VGSHttpRequest("http://example.com", data=b'{"cardNumber": "4111111111111111"}', headers=headers, method='POST')

    asserts.assert_that(request.headers[expected_header_key]).is_equal_to(header_value)
    asserts.assert_that(request.headers.get(expected_header_key)).is_equal_to(header_value)
    asserts.assert_that(request.headers.pop(expected_header_key)).is_equal_to(header_value)

def test_write_case_insensitive_headers():
    request = VGSHttpRequest("http://example.com", data=b'{"cardNumber": "4111111111111111"}', headers=headers, method='POST')

    asserts.assert_that(len(request.headers)).is_equal_to(1)
    request.headers[expected_header_key] = 'New Value'
    asserts.assert_that(len(request.headers)).is_equal_to(1)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_read_case_insensitive_headers))
    _suite.addTest(unittest.FunctionTestCase(test_write_case_insensitive_headers))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
