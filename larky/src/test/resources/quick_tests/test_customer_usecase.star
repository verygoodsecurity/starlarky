load("@stdlib//larky", "larky")
load("@stdlib//unittest","unittest")
load("@stdlib//json","json")
load("@stdlib//types", "types")
load("@stdlib//builtins", builtins="builtins")

load("@vendor//asserts","asserts")

load("@vgs//http/request", "VGSHttpRequest")
load("@vgs//vault", "vault")

def process(input_http, ctx):
  # Extract card BIN and add it to the body
  body = json.loads(input_http.body.decode("utf-8"))
  BIN = body['cardNumber'][:6]
  body['BIN'] = BIN
  body['cardNumber'] = vault.redact(body['cardNumber'], "persistent")

  # Add the BIN to the headers and change the X-Custom-Header 
  headers = input_http.headers
  headers['X-BIN-Header'] = BIN
  headers['X-Custom-Header'] = "I am a changed header"

  # Set the body as builtins.bytes of the updated body and update the headers
  input_http.body = builtins.bytes(json.dumps(body))
  input_http.headers = headers

  return input_http

def test_customer_case():
    req_body = b'{"cardNumber": "4111111111111111"}'
    headers = {"X-Custom-Header":"Header Value"}
    request = VGSHttpRequest("http://example.com", data=req_body, headers=headers, method='POST')
    context_variables = {} 
    larky_output = process(request, context_variables)
    
    body = json.loads(larky_output._data.decode("utf-8"))
    headers = larky_output.headers
    print(body)
    # Test that the code has executed properly on your request
    asserts.assert_that(body['BIN']).is_equal_to("411111")
    asserts.assert_that(headers['X-Custom-Header']).is_equal_to("I am a changed header")
    asserts.assert_that(headers['X-BIN-Header']).is_equal_to("411111")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_customer_case))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
