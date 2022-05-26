load("@vendor//asserts","asserts")
load("@stdlib//unittest","unittest")
load("@vgs//http/request", "VGSHttpRequest")

load("@stdlib//json", json="json") # import json library
load("@stdlib//builtins", builtins="builtins")
load("@vgs//vault", "vault")

def process(input, ctx):
    body = json.loads(input.body.decode("utf-8"))
    # store body in a vault and replace it with an alias
    body['account_number'] = vault.redact(body['account_number'])
    redacted_body = builtins.bytes(json.dumps(body))
    redacted_headers = {}
    for k in input.headers:
        redacted_headers[k] = vault.redact(input.headers[k])
    # construct a redacted response
    input.body = redacted_body
    input.headers = redacted_headers
    return input

def test_process():
    body = builtins.bytes('{"account_number": "543212345", "name": "Tom Hiddleston"}')
    headers = {
        'key1': 'val1',
        'key2': 'val2',
    }
    input = VGSHttpRequest("https://test.com", data=body, headers=headers, method='POST')
    response = process(input, None)

    response_body = json.loads(response.body.decode("utf-8"))

    asserts.assert_that(response_body['account_number'].startswith('tok_')).is_true()
    asserts.assert_that(response.headers['key1'].startswith('tok_')).is_true()
    asserts.assert_that(response.headers['key2'].startswith('tok_')).is_true()


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_process))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())