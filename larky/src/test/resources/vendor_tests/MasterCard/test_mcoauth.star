# -*- coding: utf-8 -*-#
#
#
# Copyright 2019-2021 Mastercard
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification, are
# permitted provided that the following conditions are met:
#
# Redistributions of source code must retain the above copyright notice, this list of
# conditions and the following disclaimer.
# Redistributions in binary form must reproduce the above copyright notice, this list of
# conditions and the following disclaimer in the documentation and/or other materials
# provided with the distribution.
# Neither the name of the MasterCard International Incorporated nor the names of its
# contributors may be used to endorse or promote products derived from this software
# without specific prior written permission.
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
# OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
# SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
# TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
# OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
# IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
# IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# Copied from https://github.com/Mastercard/oauth1-signer-python/blob/main/tests/test_oauth.py


load("@stdlib//unittest","unittest")
load("@stdlib//json", "json")
load("@stdlib//base64", "base64")
load("@vendor//asserts","asserts")
load("@vgs//MasterCard/oauth1", OAuth="OAuth", OAuthParameters="OAuthParameters",
    authenticationutils="authenticationutils", util="coreutils")

_signing_key = """-----BEGIN PRIVATE KEY-----
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
uri = 'https://www.example.com'

def test_get_authorization_header_nominal():
    header = OAuth().get_authorization_header(uri, 'POST', 'payload', 'dummy', _signing_key, "1111111111")
    asserts.assert_true("OAuth" in header)
    asserts.assert_that("dummy" in header)

def test_get_authorization_header_should_compute_body_hash():
    header = OAuth().get_authorization_header(uri, 'POST', '{}', 'dummy', _signing_key, '1111111111')
    asserts.assert_that('RBNvo1WzZ4oRRq0W9+hknpT7T8If536DEMBg9hyq/4o=' in header).is_equal_to(True)

def test_get_authorization_header_should_return_empty_string_body_hash():
    header = OAuth().get_authorization_header(uri, 'GET', None, 'dummy', _signing_key, '1111111111')
    asserts.assert_that('47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=' in header).is_equal_to(True)

def test_sign_message():
    base_string = 'POST&https%3A%2F%2Fsandbox.api.mastercard.com%2Ffraud%2Fmerchant%2Fv1%2Ftermination-inquiry&Format%3DXML%26PageLength%3D10%26PageOffset%3D0%26oauth_body_hash%3DWhqqH%252BTU95VgZMItpdq78BWb4cE%253D%26oauth_consumer_key%3Dxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx%26oauth_nonce%3D1111111111111111111%26oauth_signature_method%3DRSA-SHA1%26oauth_timestamp%3D1111111111%26oauth_version%3D1.0'
    signature = OAuth().sign_message(base_string, _signing_key)
    signature = util.percent_encode(signature)
    asserts.assert_that(signature).is_equal_to("DvyS3R795sUb%2FcvBfiFYZzPDU%2BRVefW6X%2BAfyu%2B9fxjudQft%2BShXhpounzJxYCwOkkjZWXOR0ICTMn6MOuG04TTtmPMrOxj5feGwD3leMBsi%2B3XxcFLPi8BhZKqgapcAqlGfjEhq0COZ%2FF9aYDcjswLu0zgrTMSTp4cqXYMr9mbQVB4HL%2FjiHni5ejQu9f6JB9wWW%2BLXYhe8F6b4niETtzIe5o77%2B%2BkKK67v9wFIZ9pgREz7ug8K5DlxX0DuwdUKFhsenA5z%2FNNCZrJE%2BtLu0tSjuF5Gsjw5GRrvW33MSoZ0AYfeleh5V3nLGgHrhVjl5%2BiS40pnG2po%2F5hIAUT5ag%3D%3D")

def test_get_nonce():
    nonce = util.get_nonce()
    asserts.assert_that(len(nonce)).is_equal_to(16)

def test_signature_base_string2():
    signing_key = authenticationutils.load_signing_key(_signing_key)
    body = "<?xml version=\"1.0\" encoding=\"Windows-1252\"?><ns2:TerminationInquiryRequest xmlns:ns2=\"http://mastercard.com/termination\"><AcquirerId>1996</AcquirerId><TransactionReferenceNumber>1</TransactionReferenceNumber><Merchant><Name>TEST</Name><DoingBusinessAsName>TEST</DoingBusinessAsName><PhoneNumber>5555555555</PhoneNumber><NationalTaxId>1234567890</NationalTaxId><Address><Line1>5555 Test Lane</Line1><City>TEST</City><CountrySubdivision>XX</CountrySubdivision><PostalCode>12345</PostalCode><Country>USA</Country></Address><Principal><FirstName>John</FirstName><LastName>Smith</LastName><NationalId>1234567890</NationalId><PhoneNumber>5555555555</PhoneNumber><Address><Line1>5555 Test Lane</Line1><City>TEST</City><CountrySubdivision>XX</CountrySubdivision><PostalCode>12345</PostalCode><Country>USA</Country></Address><DriversLicense><Number>1234567890</Number><CountrySubdivision>XX</CountrySubdivision></DriversLicense></Principal></Merchant></ns2:TerminationInquiryRequest>"
    url = "https://sandbox.api.mastercard.com/fraud/merchant/v1/termination-inquiry?Format=XML&PageOffset=0&PageLength=10"
    method = "POST"
    oauth_parameters = OAuthParameters()
    oauth_parameters.set_oauth_consumer_key("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
    oauth_parameters.set_oauth_nonce("1111111111111111111")
    oauth_parameters.set_oauth_timestamp("1111111111")
    oauth_parameters.set_oauth_version("1.0")
    oauth_parameters.set_oauth_body_hash("body/hash")
    encoded_hash = util.base64_encode(util.sha256_encode(body))
    oauth_parameters.set_oauth_body_hash(encoded_hash)

    base_string = OAuth().get_base_string(url, method, oauth_parameters.get_base_parameters_dict())
    expected = "POST&https%3A%2F%2Fsandbox.api.mastercard.com%2Ffraud%2Fmerchant%2Fv1%2Ftermination-inquiry&Format%3DXML%26PageLength%3D10%26PageOffset%3D0%26oauth_body_hash%3Dh2Pd7zlzEZjZVIKB4j94UZn%2FxxoR3RoCjYQ9%2FJdadGQ%3D%26oauth_consumer_key%3Dxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx%26oauth_nonce%3D1111111111111111111%26oauth_timestamp%3D1111111111%26oauth_version%3D1.0"

    asserts.assert_that(base_string).is_equal_to(expected)

def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_get_authorization_header_nominal))
    _suite.addTest(unittest.FunctionTestCase(test_get_authorization_header_should_compute_body_hash))
    _suite.addTest(unittest.FunctionTestCase(test_get_authorization_header_should_return_empty_string_body_hash))
    _suite.addTest(unittest.FunctionTestCase(test_sign_message))
    _suite.addTest(unittest.FunctionTestCase(test_get_nonce))
    _suite.addTest(unittest.FunctionTestCase(test_signature_base_string2))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())