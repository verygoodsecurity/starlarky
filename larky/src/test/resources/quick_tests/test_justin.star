load("@stdlib//unittest","unittest")
load("@stdlib//base64","base64")
load("@stdlib//json","json")
load("@stdlib//types", "types")

load("@vendor//Crypto/PublicKey/ECC", ECC="ECC")
load("@vendor//jose/jwk", jwk="jwk")
load("@vendor//jose/utils", base64url_encode="base64url_encode")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//asserts","asserts")

ec_private_key = """-----BEGIN EC PRIVATE KEY-----
MIHcAgEBBEIBzs13YUnYbLfYXTz4SG4DE4rPmsL3wBTdy34JcO+BDpI+NDZ0pqam
UM/1sGZT+8hqUjSeQo6oz+Mx0VS6SJh31zygBwYFK4EEACOhgYkDgYYABACYencK
8pm/iAeDVptaEZTZwNT0yW/muVwvvwkzS/D6GDCLsnLfI6e1FwEnTJF/GPFUlN5l
9JSLxsbbFdM1muI+NgBE6ZLR1GZWjsNzu7BOB8RMy/mvSTokZwyIaWvWSn3hOF4i
/4iczJnzJhUKDqHe5dJ//PLd7R3WVHxkvv7jFNTKYg==
-----END EC PRIVATE KEY-----"""

def _encode_header(algorithm, additional_headers=None):
    header = {
        "typ": "JWT",
        "alg": algorithm
    }

    if additional_headers:
        header.update(additional_headers)

    json_header = bytes(json.dumps(
        header,
    ), 'utf-8')

    return base64url_encode(json_header)

def _encode_payload(payload):
    payload = bytes(json.dumps(payload), 'utf-8')
    return base64url_encode(payload)

def _sign_header_and_claims(encoded_header, encoded_claims, algorithm, key):
    signing_input = bytes([0x2e]).join([encoded_header, encoded_claims])
    k = jwk.construct(key, algorithm)
    encoded_signature = base64url_encode(k.sign(signing_input))
    encoded_string = b".".join([encoded_header, encoded_claims, encoded_signature])
    print(encoded_string)
    return encoded_string


def sign(payload, key, headers=None, algorithm=ALGORITHMS.HS256):
    encoded_header = _encode_header(algorithm, additional_headers=headers)
    encoded_payload = _encode_payload(payload)
    print("======================")
    print(encoded_header)
    print(encoded_payload)
    print("======================")
    signed_output = _sign_header_and_claims(encoded_header, encoded_payload, algorithm, key)
    return signed_output


def test_justin_stuff():
  # signed = jws.sign({'a': 'b'}, 'secret', algorithm='HS256')
  encoded_headers = _encode_header('HS256')
  asserts.assert_that(encoded_headers).is_equal_to(b'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9')
  es_headers = _encode_header('ES256')
  asserts.assert_that(es_headers).is_equal_to(b'eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9')
  encoded_payload = _encode_payload({'a': 'b'})
  asserts.assert_that(encoded_payload).is_equal_to(b'eyJhIjoiYiJ9')

  print("Signing HS256:")
  signed = sign({'a': 'b'}, 'secret', algorithm='HS256')
  print(signed)
  asserts.assert_that(signed).is_equal_to(b'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYiJ9.jiMyrsmD8AoHWeQgmxZ5yq8z0lXS67_QGs52AzC8Ru8')
  print("Signed, Signing ES256:")
  ec_signed = sign({'a': 'b'}, ec_private_key, algorithm='ES256')
  print("#"*55)
  print(ec_signed)
  #asserts.assert_that(ec_signed).is_equal_to('eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYiJ9.AcyhDg1kRT7Al16OsYRUd-KjfC6VcizoYKwdd0PD7oLyjbCCbO90lqnQDriSF4dOSJMZ3fCWq3LjI7oofClxW_zHARYOPtFuEfsS7PraPzr1SUI-7oYsLIUnOS27BE7jGrlBvZN4Fre2sx_XcnF7vj8nfUJMCZ6toxlqUrlsonh9Tk7j')

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_justin_stuff))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
