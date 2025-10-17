load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//cybersource", "cybersource")

# Test private key and certificate
# nosemgrep: secrets.misc.generic_private_key.generic_private_key
TEST_PRIVATE_KEY = """-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCYoc5Ue4MKxHIQ
eSESKQiIv341EFDtfAlAsXP74modJuwnSLOfSkFNgKH4y6vSKiUK7BxU2KFy7FkR
J9/vceJmP9MD6bWPgT2Wg4iSQxgPtAHEVps9MYvkhW0lt0hyhAcGLUR3kb4YjSkG
fa8EzG/G2g+/VKdL0mnSgWhCnSBnR0xRwWccgdRTLm20/jzXkmHD92DBR7kDgiBU
rPWTfLHDnsVoIUut6BAPI83TIjHjVG1Jn8K0prbGeQU9ALwaL36qvppYpmCqaAGH
OM2fXsEPFNhEZxQpbyW2M4PtXHnjSqlNOKN2tmdF3jWwm9hKZ9xeaWJkBmBnLe3t
Nz0OdO0pAgMBAAECggEBAJHQGn5JFJJnw5SLM5XWz4lcb2SgNr/5/BjqriQXVEqP
UZHh+X+Wf7ZbyeEWKgp4KrU5hYNlBS/2LMyf7GYixSfrl1qoncP/suektwcLw+PU
ks+P8XRPbhadhP1AEJ0eFlvHSR51hEaOLIA/98C80ZgF4H9njv93f5MT/5eL5lXi
pFX1dcxUB55q9QOtQ7uCg++NyG5F6u4FxbNtOtsjyNzWZSjYsjSyGHDip9ScDOPN
sGQfznxo/oifdXvc25BgWvRflIIYEP08eeUSuGW2nUnx+Joc0oZTkC0wfU+aqKla
Zp8zfOEIm0gUDgWtgnq5I5JHJMuW6BtA4K3E+nyP0lECgYEAzIbNx/lVxmFPbPp+
AG9LD3JLycjdmTzwpHK44MsaUBOZ9PkLZs0NpR5z0/qcFb8YGGz3qN6E/TTydmfX
CpZ3bxP3+x81gL9SVG/y2GP/ky/REA0jFycwVlONeVnd09xPNNLZLUgZhWyAQIA2
pmVMh8W+pX6ojxGgOe+KIGutJCUCgYEAvwuNciTzkjBz9nFCjLONvP05WMdIAXo1
uxd17iQ0lhRtmHbphojFAPcHYocm2oUXJo5nLvy+u8xnxbyXaZHmRqm98AzmBTtp
phFtgfTtv/cSvOsBpdyyaJaN12IUs2XYACGBRa2DUkgxxvHtbmjFGFIU+5VgjOG8
g0LfoPhLM7UCgYAmdRaOioihY7zOjg9RP5wKjIBJsfZREQ9irJus0SPieL0TPhzx
uI7fRGmdK1tcD3GVbi/nVegFwIXy07WwrPhKL6QKWSTzT4ZIkEBGhg8RewVBkmbN
vLWvFcjdT5ORebR/B0KE7DC4UN2Qw0sDYLrSMNGXRsilFjhdjHgZfoWw7QKBgAZr
QvNk3nI5AoxzPcMwfUCuWXDsMTUrgAarQSEhQksQoKYQyMPmcIgZxLvAwsNw2VhI
TJs9jsMMmSgBsCyx5ETXizQ3mrruRhx4VW+aZSqgCJckZkfGZJAzDsz/1KY6c8l9
VrSaoeDv4AxJMKsXBhhNGbtiR340T3sxkgX8kbpJAoGBAII2aFeQ4oE8DhSZZo2b
pJxO072xy1P9PRlyasYBJ2sNiF0TTguXJB1Ncu0TM0+FLZXIFddalPgv1hY98vNX
22dZWKvD3xJ7HRUx/Hyk+VEkH11lsLZ/8AhcwZAr76cE/HLz1XtkKKBCnnlOLPZN
03j+WKU3p1fzeWqfW4nyCALQ
-----END PRIVATE KEY-----"""

# Certificate:
# Data:
#     Version: 3 (0x2)
#     Serial Number:
#         41:26:36:06:cf:1b:3b:81:9b:85:f0:fe:fb:8e:d8:7f:79:d1:9c:a8
#     Signature Algorithm: sha256WithRSAEncryption
#     Issuer: C=US, ST=Some-State, O=Internet Widgits Pty Ltd
#     Validity
#         Not Before: Sep 12 12:28:45 2025 GMT
#         Not After : Sep 12 12:28:45 2026 GMT
#     Subject: C=US, ST=Some-State, O=Internet Widgits Pty Ltd
TEST_CERTIFICATE = """-----BEGIN CERTIFICATE-----
MIIDazCCAlOgAwIBAgIUQSY2Bs8bO4GbhfD++47Yf3nRnKgwDQYJKoZIhvcNAQEL
BQAwRTELMAkGA1UEBhMCVVMxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM
GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yNTA5MTIxMjI4NDVaFw0yNjA5
MTIxMjI4NDVaMEUxCzAJBgNVBAYTAlVTMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw
HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB
AQUAA4IBDwAwggEKAoIBAQCYoc5Ue4MKxHIQeSESKQiIv341EFDtfAlAsXP74mod
JuwnSLOfSkFNgKH4y6vSKiUK7BxU2KFy7FkRJ9/vceJmP9MD6bWPgT2Wg4iSQxgP
tAHEVps9MYvkhW0lt0hyhAcGLUR3kb4YjSkGfa8EzG/G2g+/VKdL0mnSgWhCnSBn
R0xRwWccgdRTLm20/jzXkmHD92DBR7kDgiBUrPWTfLHDnsVoIUut6BAPI83TIjHj
VG1Jn8K0prbGeQU9ALwaL36qvppYpmCqaAGHOM2fXsEPFNhEZxQpbyW2M4PtXHnj
SqlNOKN2tmdF3jWwm9hKZ9xeaWJkBmBnLe3tNz0OdO0pAgMBAAGjUzBRMB0GA1Ud
DgQWBBSDGv6WihmH3vwAiHynmqExEmmclDAfBgNVHSMEGDAWgBSDGv6WihmH3vwA
iHynmqExEmmclDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAx
CpMM5w+vmMbsUnq6/vFdaY17PAYL3k1lrVDIkWZa5hVd7/ZaF2GbYIRLSy7j+37J
b+/x0Fqx2S/IPooO6u3ASl9y7+IFW8ampAsLH2wvQ6X7xaP2EH24+pT3aceGyFB5
xptK9O0MpmXD0JMbJvyGkWo2wXhZr6sYb89n2KfyvchaWCc0E83PxMUNSnrLw8Nz
vsbLcR4UPV6y1W9+Viu1iiUxGsAck5q/UItS0qN9L8jfzhkaFg8OPSaKm3PUde2c
g+PaDPWu+J/2rdWK/Zx3osMiXHYUWDOA7XeBQ4F4tetwmiSIdZEjJtA5TQ4Yr3Dd
ZkVx8LN7GvhT2Hj2WsT/
-----END CERTIFICATE-----"""

TEST_SOAP_XML = """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <requestMessage xmlns="urn:schemas-cybersource-com:transaction-data-1.219">
      <merchantID>test_merchant_id</merchantID>
      <merchantReferenceCode>test_merchant_reference_code</merchantReferenceCode>
      <billTo>
        <firstName>John</firstName>
        <lastName>Doe</lastName>
        <street1>1295 Charleston Road</street1>
        <city>Mountain View</city>
        <state>CA</state>
        <postalCode>94043</postalCode>
        <country>US</country>
        <phoneNumber>650-121-6000</phoneNumber>
        <email>nobody@hello.com</email>
        <ipAddress>10.7.7.7</ipAddress>
      </billTo>
      <shipTo>
        <firstName>Jane</firstName>
        <lastName>Doe</lastName>
        <street1>100 Elm Street</street1>
        <city>San Mateo</city>
        <state>CA</state>
        <postalCode>94401</postalCode>
        <country>US</country>
      </shipTo>
      <item id="0">
        <unitPrice>12.34</unitPrice>
      </item>
      <item id="1">
        <unitPrice>56.78</unitPrice>
      </item>
      <purchaseTotals>
        <currency>USD</currency>
      </purchaseTotals>
      <card>
        <accountNumber>4111111111111111</accountNumber>
        <expirationMonth>12</expirationMonth>
        <expirationYear>2025</expirationYear>
      </card>
      <ccAuthService run="true">
      </ccAuthService>
    </requestMessage>
  </soap:Body>
</soap:Envelope>"""

def test_cybersource_module_exists():
    """Test that the CyberSource module is properly loaded"""
    asserts.assert_that(cybersource).is_not_none()
    asserts.assert_that(hasattr(cybersource, "sign")).is_true()

def test_sign_with_valid_inputs():
    """Test signing with valid SOAP XML, private key, and certificate"""
    signed_request = cybersource.sign(
        TEST_SOAP_XML,
        TEST_PRIVATE_KEY,
        TEST_CERTIFICATE
    )
    
    # Verify the signed request is not None and is a string
    asserts.assert_that(signed_request).is_not_none()
    asserts.assert_that(signed_request).is_instance_of(str)
    
    # Verify it contains expected SOAP elements
    asserts.assert_that(signed_request).contains("soap:Envelope")
    asserts.assert_that(signed_request).contains("soap:Body")
    
    # Verify WS-Security elements are added
    asserts.assert_that(signed_request).contains("wsse:Security")
    asserts.assert_that(signed_request).contains("ds:Signature")
    asserts.assert_that(signed_request).contains("BinarySecurityToken")
    asserts.assert_that(signed_request).contains("SignedInfo")
    asserts.assert_that(signed_request).contains("SignatureValue")
    
    # Verify original content is preserved
    asserts.assert_that(signed_request).contains("test_merchant_id")
    asserts.assert_that(signed_request).contains("test_merchant_reference_code")
    asserts.assert_that(signed_request).contains("John")
    asserts.assert_that(signed_request).contains("Doe")
    asserts.assert_that(signed_request).contains("nobody@hello.com")
    asserts.assert_that(signed_request).contains("1295 Charleston Road")
    asserts.assert_that(signed_request).contains("Mountain View")
    asserts.assert_that(signed_request).contains("4111111111111111")

def test_sign_with_empty_request():
    """Test signing with empty request should raise error"""
    def sign_empty():
        cybersource.sign(
            "",
            TEST_PRIVATE_KEY,
            TEST_CERTIFICATE
        )
    
    asserts.assert_fails(sign_empty, ".*Request cannot be empty.*")

def test_sign_with_empty_private_key():
    """Test signing with empty private key should raise error"""
    def sign_empty_key():
        cybersource.sign(
            TEST_SOAP_XML,
            "",
            TEST_CERTIFICATE
        )
    
    asserts.assert_fails(sign_empty_key, ".*Private key cannot be empty.*")

def test_sign_with_empty_certificate():
    """Test signing with empty certificate should raise error"""
    def sign_empty_cert():
        cybersource.sign(
            TEST_SOAP_XML,
            TEST_PRIVATE_KEY,
            ""
        )
    
    asserts.assert_fails(sign_empty_cert, ".*Certificate cannot be empty.*")

def test_sign_with_invalid_xml():
    """Test signing with invalid XML should raise error"""
    def sign_invalid_xml():
        cybersource.sign(
            "This is not valid XML",
            TEST_PRIVATE_KEY,
            TEST_CERTIFICATE
        )
    
    asserts.assert_fails(sign_invalid_xml, ".*Failed to sign request.*")

def test_sign_with_invalid_private_key():
    """Test signing with invalid private key should raise error"""
    invalid_key = """-----BEGIN PRIVATE KEY-----
INVALID_KEY_DATA
-----END PRIVATE KEY-----"""
    
    def sign_invalid_key():
        cybersource.sign(
            TEST_SOAP_XML,
            invalid_key,
            TEST_CERTIFICATE
        )
    
    asserts.assert_fails(sign_invalid_key, ".*Failed to sign request.*")

def test_sign_with_invalid_certificate():
    """Test signing with invalid certificate should raise error"""
    invalid_cert = """-----BEGIN CERTIFICATE-----
INVALID_CERT_DATA
-----END CERTIFICATE-----"""
    
    def sign_invalid_cert():
        cybersource.sign(
            TEST_SOAP_XML,
            TEST_PRIVATE_KEY,
            invalid_cert
        )
    
    asserts.assert_fails(sign_invalid_cert, ".*Failed to sign request.*")

def test_sign_preserves_xml_structure():
    """Test that signing preserves the original XML structure and content"""
    signed_request = cybersource.sign(
        TEST_SOAP_XML,
        TEST_PRIVATE_KEY,
        TEST_CERTIFICATE
    )
    
    # Check all the original data is still present
    test_values = [
        "test_merchant_id",
        "test_merchant_reference_code",
        "John",
        "Jane",
        "1295 Charleston Road",
        "100 Elm Street",
        "Mountain View",
        "San Mateo",
        "CA",
        "94043",
        "94401",
        "US",
        "650-121-6000",
        "nobody@hello.com",
        "10.7.7.7",
        "12.34",
        "56.78",
        "USD",
        "4111111111111111",
        "12",
        "2025"
    ]
    
    for value in test_values:
        asserts.assert_that(signed_request).contains(value)

def test_sign_adds_security_headers():
    """Test that signing adds proper WS-Security headers"""
    signed_request = cybersource.sign(
        TEST_SOAP_XML,
        TEST_PRIVATE_KEY,
        TEST_CERTIFICATE
    )
    
    # Check for WS-Security namespace
    asserts.assert_that(signed_request).contains("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd")
    
    # Check for XML Digital Signature namespace
    asserts.assert_that(signed_request).contains("http://www.w3.org/2000/09/xmldsig#")
    
    # Check signature algorithm (RSA-SHA256)
    asserts.assert_that(
        signed_request.find("rsa-sha256") >= 0 or 
        signed_request.find("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256") >= 0
    ).is_true()

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_cybersource_module_exists))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_valid_inputs))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_empty_request))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_empty_private_key))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_empty_certificate))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_invalid_xml))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_invalid_private_key))
    _suite.addTest(unittest.FunctionTestCase(test_sign_with_invalid_certificate))
    _suite.addTest(unittest.FunctionTestCase(test_sign_preserves_xml_structure))
    _suite.addTest(unittest.FunctionTestCase(test_sign_adds_security_headers))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())