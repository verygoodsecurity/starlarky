# ===================================================================
#
# Copyright (c) 2014, Legrandin <helderijs@gmail.com>
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in
#    the documentation and/or other materials provided with the
#    distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# ===================================================================
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//builtins", builtins="builtins")
load("@vendor//Crypto/Signature/pkcs1_15", pkcs1_15="pkcs1_15")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//asserts", asserts="asserts")

# Need ability to generate a signed message
# This requires:
# - RSASSA-PKCS1-v1_5 signature scheme (sha256 hashing)
def test_pkcs1_15_sig_gen_RSASSA():
    utf_8_msg = codecs.encode("message", encoding='UTF-8')
    key_str = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpQIBAAKCAQEAnfuLiqUxJY7Zy34i6Nr2T3j1K85o5o0WmFGUPk2Ks4Jojy+zIT2jzDXxr5EE/f8JrhOFC4uAeuI+OyAaNO75NppU27UWiR/sLTXQOcekoF8bmue+lfzGvr4YDriRQVsKMUa5wxc8KbJDjaFFFMqJlG8NgMQgwktvZ9TnE5AyecIg1Hj68CeINiiJdv5QeiHIMsP2eCpyUig7JA0HC8LyGqyfNXHdp42B8tcFB1018af8JEwydAp21/13YcmYJXkLFd/7WocUnH2hwLJzn5j7z1E+K8UwpuFYPhr101cWA09YUphWEIKHWVUHvoGnYyPmkNjUTHN5xZubWmRet0HQCwIDAQABAoIBADh8vbs4L6d3OWaqVj8eM53c6QTdy+JLpj2WRcQ1I5fl1A8Ghi0nhg65Zw5YcwmCjGqCrjqfibPme0vWwagVnmQosJjSWhlzoZXUvPw75yA0gYFUxW+Jr2J6Q83XrpBIxg8yc69O+oMKK19Tv4iclq7NOm3FMtVl9ZKRrIHxOCsSb4rd8OkbQByRSkpToWRGjYdz0SJz79jBxhtcgjefqK+9pnQUxKdse3C2RfGDiQ+iVwm+3cJYAja/cO3NhcFtrZvXsdowV7RrPODy1k4NP+1wg7FbcGO+AK/rtGRW7SKyXOVYEpYQeIoTCrlM8GBEc77tadsvGY7BtxPIhYh1vIECgYEA0deF0tS1/S3kBKGDRxrh3WJmkBCy77Ts7Sjn6OY4trGL6pPH8HRhO35KgfE0FLf1B4yB2OTlVNA9QNyzu3dOWjYPRceHtsttlM0L6wRPMoiZxMGNxO4fC3b2LyetwyrYASfNLMMlEB4OHxNxvew3wtIw6+uVQfb8I5ML6AxJQEsCgYEAwLvBqLO7pLUdaOdJ8Zyaj+it+YObtivm/SAYxDZovolGkZZ5cgKZlwMp891/5h7dCoz/cE1enOooz2jGP0bhGe6apVz2Jap87G08PmrwqYhCrRnzZ+3Ik7PKp32kkO9kTZbfZQ8l37tdDjlLm8Ei31+ZB9MCrT/5rOwXBVANV0ECgYEAqyMz0CuMtQxdP22eDDqfZJdhADOuS5Lp+811vdss+8q3Srv1v2Sa/BNWnr1h9VfGLb7pE0QyyMYAfsrAhq6ZpzcZo5ZEV8928nl8M9LS6GrBxdb5UVfG1+nQg8q/i0GA4whb7BrHOmrYp32LMRLaszgLkA0VMLRHFJhTiI+kmhsCgYEAiuVEtU2CBzs3DKjSUzsm8FshNdYw22JcP0NoVc9YQSgxIS6uKwnQolH3Y/XW/IJ1jer7E3XOn4rpCkBZzrRH+SZJcK7D/XUWPlSiu+DZu3OliZZQIi/tAvTpMTeM555+TKRULkJyawBeGagU4xSXCx+b61WYcj7Osl4aYQvF8YECgYEAs4UvSC+Ik72T9ywKOpmOG1TSyDI1tZsFvRmbk2+1cmlErb0STDk8Cmf0xQl3ya4WAU/GbDKVxS91kzz6i7s6zKVGZrlR0Q4Tfs4srgNCuaL8hI0nShH1Yugn2PlFulzY3i3il2vy1tMPdNtoGptO2C9c3JmmIvlIEz9kUgb4khk=\n-----END RSA PRIVATE KEY-----"
    key = RSA.import_key(key_str)
    hash = SHA256.new(utf_8_msg)
    signature = pkcs1_15.new(key).sign(hash)
    b64_signature = binascii.b2a_base64(signature).strip()
    expected_b64_signature = b'DiBXbvwvNDtcIszL9JDGFb5JrkJNaBlBAwfJ05f1wN5V5tmNjj5HP0k7zs0XEyNPYpYe2+7CAC+WJO16WeHb1IuKNCNv/jtXT2YjfMPDR/PjgoldgJhhQMlydO3lGhxm4SeIBRE0Sa/nwZgf1uzb3Ro1Iyy07KSpkAmvn99A01DpVT64t9FcPzrjTq1UesoYwiRLbiwi2AWUyVllVoAlTKO/9Mfo7TF6QgiS35fhoGPoRexBFO+1bwT0BRDFF/CX0hv2Lh1sZA7x6JXUdWpEA74+P8eMSBn+NRZjhd7m+9EWtZHn9+CWbV2Y4PaPGrCzsDA3ZSSs4ZzKFYHTw7VTcQ=='
    asserts.assert_that(b64_signature).is_equal_to(expected_b64_signature)


def test_pkcs1_15_sig_gen_PKCS8PEM():
    concat_string = '2021-09-20T10:10:10-7:00|5150681234567890|1223'
    concat_string_utf8 = codecs.encode(concat_string, encoding='UTF-8')
    private_pkcs8_pem = '-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDZ7kMpLeyVtAw70GxlLSGtDo0fzU5/7YcjeLycqVQ5feAB13hbkzcXa48EiBLZ7je0w9USiXTKaWwQhkVltgxQrLSu/n+ZdAKicDg8O4glYQQ8T2nZzdqvzyS3L/Ntd4TGKcbyqZyev6VVURAb5tZH11HzgD2whuYQCLPR/uXVP0OMDfE7UE4SZIJeLHBit2dYGCmb3VhsEZwxsHHnqeiYSau9AiDBW8JgBmC906/OJvCA7SskP9FJw9EIZp9P3RnIlN+mh+u4q/zEMiOYtu/NtUGZ+j/M+THcMfqR9qMAi2JY4tGlxzfypW2DIN1cC1DnssuenDlJnqB4kyx/We5dAgMBAAECggEAYT+mbrDUmzU+JE7sn/WUyoHszd0zA3k+TcDqAcq6D/GwzYGWbt7xvZy8tFv4dZGNADhHU79NA6opk/Im861aL4HyGwjsbdDMACqtnYs/RNbXKgaSCKSIyt1X1/so2Jd2A2vPIKg5gMmqgLjsxFatFl/qbQuz+5sm/M61V6c0boS61JzBFW9BsZyBe9x08OhzEaox536sYkrx9pC6/rf4cBRk9MtFDIv2WvXhQf9TWovcIc5Ajsh2lVr6o+qOOXlC1tlSnT5SRLYwx8FTY3s6m96tMEazNuqtpZBhs3gn9iU2cAxn1eh1sBhqSW+FJy8SpY3YZXfRLowR4iG/JoXp7QKBgQDsf24BYTIUqFKVy+APg4CqTRbEzBwYes1K8ZMtP765JxR+va3WlDQ+YsCIE2bnVLGS93957lqnEch2dmxckSY22QCdc5nY9iCMSLg6US/UJnkSAoxIu3U6g/Y91ygp92y+IHHfQcEgAVQIQRmKeKSMTsYzGfy8Z6nN/4xRveFHpwKBgQDr5t+8bni+laKV9/RusNMswX+2ZescV3rrmUpPOfSy+40182Dp8wV5O1fBHpkZhEatIvSA9c9E1NDWYe1bTHUt0D+bXgQTT6Y0JN3pV6WEcTALSmYHr6jZXIE/XIo3ic4qCzHuMepZGY64W4Maj64Bqn0KN1yPhBSGKCV9YN2aWwKBgAU30o+JO3elNx1DAFCXUDFxvfzdnAkWPI1YwyyOaYDgUd1yBfHkFzsBJspZ0z1raveZKmDgV3O0Or4SYtWwBi3Tn+XZdR2KYLF3sUjoHawKpUU2dS3tpOqDmt1GcAZtBSM0yA5q/v3byaTc+tlJTCgkXM8n1aG6j86RwuDS1bOlAoGBALPfJ4vzJAgV6pG1LeDU3UBMDohScBH9NhG8oWeFhmqO2h5gA+5+yhalRAVRwmUtcgKEG7DVPQR7zP0aUDSEOPkEx+s8pZjf65fo4HgSx3cUIxEI2oFT1Ehge4CHnDA62ENApAK+drUXgrZZuL6g/YnxqEQI8H/RsyCULfnKyU3vAoGAekY5orJhgbe5f7LB2w9OPnh2kN8s5aFhs//MrYuBcADtEnNTS/cQPd6VHwDbFBtz4j0L4/ZCa3S3cGCXCYDOg5BnwtNsinKTyKPfCEhFi5tv1KZqF/almzvSaXuCJG+tymPZUpz5S9so/GUqnaHs/jXCc4ZEJ5XceCBsOhMLAIU=\n-----END PRIVATE KEY-----'
    private_key = RSA.import_key(private_pkcs8_pem)
    h = SHA256.new(concat_string_utf8)
    signature = pkcs1_15.new(private_key).sign(h)
    b64_signature = binascii.b2a_base64(signature).strip()
    expected_b64_signature = b's7PrWZavRhAt3Rx36I4vgwuQ3u9vt57PtpQFV7kUC3BGI/9CpuM2zXdBwQVsiLEKPhZD7hyTloHzbR/il+7145NhzcPx/XXuqeZA3LHr/DAT98YN6faeF+10cH8EtDbERnrQq3cViAiTFZAfW6xAVk2nu31kDVKtTt1HX4Na939aV2iTP++kvXQqpkixJpUm9moFECg1tmSySxKxYyRCBDxqTcgadGi/yKOgnEHShP++QMRm4EmHvFt7AKaUhhaGKJzGypm7JIpbvqKUqoSsnAeO2JMw9KNPx/REx/G3A8RS8xNqUu15JZezpiihA141DEGiaB8oVbwig/Jy5XfaKA=='
    asserts.assert_that(expected_b64_signature).is_equal_to(b64_signature)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_pkcs1_15_sig_gen_RSASSA))
    _suite.addTest(unittest.FunctionTestCase(test_pkcs1_15_sig_gen_PKCS8PEM))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
