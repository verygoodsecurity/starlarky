load("@stdlib//unittest", unittest="unittest")
load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//builtins","builtins")

load("@vendor//Crypto/Signature/pkcs1_15", pkcs1_15="pkcs1_15")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//asserts", "asserts")

# Need ability to generate a signed message
# This requires:
# - RSASSA-PKCS1-v1_5 signature scheme (sha256 hashing)
def test_pkcs1_15_sig_gen_test():
    utf_8_msg = codecs.encode("message", encoding='UTF-8')
    key_str = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpQIBAAKCAQEAnfuLiqUxJY7Zy34i6Nr2T3j1K85o5o0WmFGUPk2Ks4Jojy+zIT2jzDXxr5EE/f8JrhOFC4uAeuI+OyAaNO75NppU27UWiR/sLTXQOcekoF8bmue+lfzGvr4YDriRQVsKMUa5wxc8KbJDjaFFFMqJlG8NgMQgwktvZ9TnE5AyecIg1Hj68CeINiiJdv5QeiHIMsP2eCpyUig7JA0HC8LyGqyfNXHdp42B8tcFB1018af8JEwydAp21/13YcmYJXkLFd/7WocUnH2hwLJzn5j7z1E+K8UwpuFYPhr101cWA09YUphWEIKHWVUHvoGnYyPmkNjUTHN5xZubWmRet0HQCwIDAQABAoIBADh8vbs4L6d3OWaqVj8eM53c6QTdy+JLpj2WRcQ1I5fl1A8Ghi0nhg65Zw5YcwmCjGqCrjqfibPme0vWwagVnmQosJjSWhlzoZXUvPw75yA0gYFUxW+Jr2J6Q83XrpBIxg8yc69O+oMKK19Tv4iclq7NOm3FMtVl9ZKRrIHxOCsSb4rd8OkbQByRSkpToWRGjYdz0SJz79jBxhtcgjefqK+9pnQUxKdse3C2RfGDiQ+iVwm+3cJYAja/cO3NhcFtrZvXsdowV7RrPODy1k4NP+1wg7FbcGO+AK/rtGRW7SKyXOVYEpYQeIoTCrlM8GBEc77tadsvGY7BtxPIhYh1vIECgYEA0deF0tS1/S3kBKGDRxrh3WJmkBCy77Ts7Sjn6OY4trGL6pPH8HRhO35KgfE0FLf1B4yB2OTlVNA9QNyzu3dOWjYPRceHtsttlM0L6wRPMoiZxMGNxO4fC3b2LyetwyrYASfNLMMlEB4OHxNxvew3wtIw6+uVQfb8I5ML6AxJQEsCgYEAwLvBqLO7pLUdaOdJ8Zyaj+it+YObtivm/SAYxDZovolGkZZ5cgKZlwMp891/5h7dCoz/cE1enOooz2jGP0bhGe6apVz2Jap87G08PmrwqYhCrRnzZ+3Ik7PKp32kkO9kTZbfZQ8l37tdDjlLm8Ei31+ZB9MCrT/5rOwXBVANV0ECgYEAqyMz0CuMtQxdP22eDDqfZJdhADOuS5Lp+811vdss+8q3Srv1v2Sa/BNWnr1h9VfGLb7pE0QyyMYAfsrAhq6ZpzcZo5ZEV8928nl8M9LS6GrBxdb5UVfG1+nQg8q/i0GA4whb7BrHOmrYp32LMRLaszgLkA0VMLRHFJhTiI+kmhsCgYEAiuVEtU2CBzs3DKjSUzsm8FshNdYw22JcP0NoVc9YQSgxIS6uKwnQolH3Y/XW/IJ1jer7E3XOn4rpCkBZzrRH+SZJcK7D/XUWPlSiu+DZu3OliZZQIi/tAvTpMTeM555+TKRULkJyawBeGagU4xSXCx+b61WYcj7Osl4aYQvF8YECgYEAs4UvSC+Ik72T9ywKOpmOG1TSyDI1tZsFvRmbk2+1cmlErb0STDk8Cmf0xQl3ya4WAU/GbDKVxS91kzz6i7s6zKVGZrlR0Q4Tfs4srgNCuaL8hI0nShH1Yugn2PlFulzY3i3il2vy1tMPdNtoGptO2C9c3JmmIvlIEz9kUgb4khk=\n-----END RSA PRIVATE KEY-----"
    key = RSA.import_key(key_str)
    hash = SHA256.new(utf_8_msg)
    signature = pkcs1_15.new(key).sign(hash)
    b64_signature = binascii.b2a_base64(signature).strip()
    expected_b64_signature = b'FJUFBAfk8+YlpDYRw43qpqS3HsxQ1wo5G5aI6EuEVlNX9lz6/479rRW3fjiMy54BQSm94CtTUFLVwNZ+9KE2Ier4+16NxzDScyrzHHP49E0fBoFPFhwzBkO9ZqFnB3CrZIc3jT9SZSpVz1xC/d5BPTgrOOLFEEAU9HCQ7Ef3XYK5hI94v5KTOhGVR6YiHyDVp4wltcUmRmItG5HVxGffiiEdMnD1Ux370oj4pjNYyJUcy5SvXjrmrxQMKlXEsZeFLek2s9hGyd+kd0NMA8FAw+mX/dfrVZRjNoqeA7VF0rdFaJY2+QcxLhBqXXRXoH6iAITwmw9umZdiL68QmQ/6xQ=='
    asserts.assert_that(b64_signature).is_equal_to(expected_b64_signature)

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_pkcs1_15_sig_gen_test))
    return _suite
_runner = unittest.TextTestRunner()
_runner.run(_testsuite())