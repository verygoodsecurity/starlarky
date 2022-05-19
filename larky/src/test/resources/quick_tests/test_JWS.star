load("@stdlib//unittest","unittest")
load("@stdlib//base64","base64")
load("@stdlib//json","json")
load("@stdlib//types", "types")
load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//jose/jwk", jwk="jwk")
load("@vendor//jose/utils", base64url_encode="base64url_encode")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//asserts","asserts")

# These keys were generated for the tests in this file.
es_private_key = """-----BEGIN EC PRIVATE KEY-----
                    MHcCAQEEIDcv6AeZhfUH20LCzlHKr6SZyWK5LnQQrWN5TigDmTcwoAoGCCqGSM49
                    AwEHoUQDQgAEQ4+x/eCyT+7mnjPgT0iIf7PBB2W7YHDi3qvbNMZm+Its/M+6eCGk
                    qhcJT26DWf2EMncfCX4okTxVST/r/ohGXA==
                    -----END EC PRIVATE KEY-----"""

rsa_private_key = """-----BEGIN RSA PRIVATE KEY-----
                    MIIEogIBAAKCAQEAnzJokBF2RIiyOIS8iCJmUKDvg1kFvs+elNeNWb9s+xfJk/ka
                    pqvs4b5wFzLGmZbmWeWWw4reGyYFk1bmfCKbC+58yciH/Iy2hwOWsvL1MLmQG0lo
                    wD9joD3uuvFbaPecyHQ9VLE7SBx2w8uffn/PNUtu7/h+9vLP+RFOCIkDVdfHRD1F
                    Llx1rtXVecpfXu+RE5fVv+FIitzB3VTE3ZBO/oBYp9K/M9xELQ9G9bKpK3Jddw/Z
                    MJ2rF3Kf6SQHY0907LA9RTIbdg4MCBNdzHeOE42KliOjsxs6ZqFkeIBRuEINlkaC
                    Y0/Ki+q2pEgHwgIdlfCYC+qA0xOJsk01rhV6LQIDAQABAoIBABjKA6gzX+QqGP5e
                    BPF50c7KUKF2UkO1Fq98pSENgs3SlWv6RSEg8mDwg4nJOU2fRr1G1+QZEp7Nm96m
                    kHtR9Cy2dS4CkNJNovqBH4078dwleQAWvT36YOwJ2Mu5W4+LxxkbqJUwh9ehvwic
                    A3jyM/TLiznIyxRvGN1iyXDgzOkn0mAOHL+m+iwiqVb9xS/7NELbOFxYxyzlqmUn
                    HC61t2zLRS2aUCGv+HVfhfdBo7jNsH9NnXqNU2ghV9oi9Ssgv8xyQC4nlRmHe1+s
                    vw4ZFKT1Ybjyhj3rG1LXyDJHmZwc/ktNXSuPHiypwM8KLU6Tx2uBheGwKVciVbbV
                    WCvnbKECgYEA3AI22eXKc7LgjunQNeIn00W6IJV3zDVt2ELMymYTVpUKILpZkBJG
                    l8QrMdkyhdMPcRbQhJL427uNWWkKiuRNVWOwU8Yr6/eOjQ7qBmrNRrjL3STt4uW4
                    pFd1qO9gjvdJ7PdL7sf0LLGeqQ0jfguSCN3L19I33YTsjgyQ8HktTMkCgYEAuT1y
                    fk3rpLzYt1TfV0ZvSHOYcMSJDjcoo9hBJyYjPDsi1YtmoJgP4zalCJLwssZAbENl
                    XbFQ38dYF+EJH20GHRjQooS5ffWkBfSrCpkTfqrno+bBbQE6hKapNeCT/Fubk25Y
                    nHbM/Fswf0JUBDzR6lej4C3oC3QwvsKVIu7tiEUCgYBtdJiPbZh8WUkJMOAKfrpr
                    jOwd297e9NHyXsF+DKygTcPdJnj6iW0fglQvF6zgEXJERCJ3Ypt2zmdzTSQWl6C1
                    08Pc2eHuIpqEKSBbEvWPss6R9hZ35Oworu51nqo+Vl8sCph3cnlTZwbVehjnU6BS
                    0u3gMNDkX4ZE4ZGWYeMcqQKBgCmHmjSw+YSshhXMJGpnsylWKTYXCfcy7JyXLjw9
                    s6acR3oCz9ZvYRh6ttNORkJ+ahEbpw0zZMNW6Owpmqb+BWHen1/gS8numYBwUyyE
                    FzfNzzMS2Ai9PsZgqw0WNXddfmq2UY16oQhu1Veioj10+UcFsQrgn+Z5fTg3XcJA
                    OObVAoGAPk7GL8WELvJGBNYcLFZ4TxgtD7uPxkA+PwMMv/xwHjZjiCKsL5HlYYuD
                    Qgz4dQy0OW02mfC5LDkCHzOO3m5suiodTJpDiU0arcbtXh3QW3IWwKXMwG2pDLsh
                    vyExhzkL/cPs+hXX5Uvuwh3RATzdlGx9Vf6NDaF20EvegzK42wY=
                    -----END RSA PRIVATE KEY-----"""

def test_sign_with_rsa():
    header = {
        "typ": "JWT",
        "alg": 'RS256'
    }

    json_header = bytes(json.dumps(header,), 'utf-8')

    headers = base64url_encode(json_header)
    encoded_payload = base64url_encode(bytes(json.dumps({'a': 'b'}), 'utf-8'))

    signing_input = bytes([0x2e]).join([headers, encoded_payload])
    k = jwk.construct(rsa_private_key, 'RS256')
    sign = k.sign(signing_input)
    encoded_signature = base64url_encode(sign)
    
    encoded_string = b".".join([headers, encoded_payload, encoded_signature])

def test_sign_with_ecc():
    header = {
        "typ": "JWT",
        "alg": 'ES256'
    }

    json_header = bytes(json.dumps(header), 'utf-8')

    headers = base64url_encode(json_header)
    encoded_payload = base64url_encode(bytes(json.dumps({'a': 'b'}), 'utf-8'))

    signing_input = bytes([0x2e]).join([headers, encoded_payload])
    k = jwk.construct(es_private_key, 'ES256')
    sign = k.sign(signing_input)
    encoded_signature = base64url_encode(sign)
    
    encoded_string = b".".join([headers, encoded_payload, encoded_signature])

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_ecc))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_rsa))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())