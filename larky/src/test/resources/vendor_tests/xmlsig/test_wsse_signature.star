load("@stdlib//builtins", "builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io/StringIO", "StringIO")
load("@stdlib//larky", "larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", "types")
load("@stdlib//unittest", "unittest")
load("@stdlib//xml/etree/ElementTree", ElementTree="ElementTree")

load("@vendor//asserts", "asserts")
load("@vendor//cryptography/hazmat/backends", default_backend="default_backend")
load("@vendor//cryptography/hazmat/primitives", serialization="serialization")
load("@vendor//cryptography/hazmat/primitives/serialization/pkcs12", pkcs12="pkcs12")
load("@vendor//cryptography/x509", load_pem_x509_certificate="load_pem_x509_certificate")
load("@vendor//elementtree/SimpleXMLTreeBuilder", SimpleXMLTreeBuilder="SimpleXMLTreeBuilder")

load("@vendor//xmlsig", xmlsig="xmlsig")

# TEST START
load("./base", load_xml="load_xml", compare="compare")
load("./data/pkcs12_pfx_encoded_pw", FIXTURE="FIXTURE", HttpHeader="HttpHeader")


namespaces = {
    'soap-env': xmlsig.ns.SOAP_ENV_11,
    'wsse': xmlsig.ns.WSSE,
    'ds': xmlsig.ns.DS,
    'ns0': "http://schemas.mastercard.com.chssecure/2011/01",
    "ns1": "http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request",
}

# securityConfiguration = "<xwss:SecurityConfiguration xmlns:xwss=\"http://java.sun.com/xml/ns/xwss/config\" dumpMessages=\"true\"><xwss:Timestamp /><xwss:Sign includeTimestamp=\"false\"><xwss:SignatureMethod algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha512\" /><xwss:SignatureTarget type=\"xpath\" value=\"/*[local-name()='Envelope']/*[local-name()='Body']\"><xwss:DigestMethod algorithm=\"http://www.w3.org/2001/04/xmlenc#sha512\" /><xwss:Transform algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><xwss:AlgorithmParameter name=\"CanonicalizationMethod\" value=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></xwss:Transform></xwss:SignatureTarget><xwss:SignatureTarget type=\"xpath\" value=\"/*[local-name()='Envelope']/*[local-name()='Header']/*[local-name()='Security']/*[local-name()='Timestamp']\"><xwss:DigestMethod algorithm=\"http://www.w3.org/2001/04/xmlenc#sha512\" /><xwss:Transform algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><xwss:AlgorithmParameter name=\"CanonicalizationMethod\" value=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></xwss:Transform></xwss:SignatureTarget><xwss:SignatureTarget type=\"xpath\" value=\"/*[local-name()='Envelope']/*[local-name()='Header']/*[local-name()='Security']/*[local-name()='BinarySecurityToken']\"><xwss:DigestMethod algorithm=\"http://www.w3.org/2001/04/xmlenc#sha512\" /><xwss:Transform algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><xwss:AlgorithmParameter name=\"CanonicalizationMethod\" value=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></xwss:Transform></xwss:SignatureTarget><xwss:SignatureTarget type=\"xpath\" value=\"/*[local-name()='Envelope']/*[local-name()='Header']/*[local-name()='identity']\"><xwss:DigestMethod algorithm=\"http://www.w3.org/2001/04/xmlenc#sha512\" /><xwss:Transform algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><xwss:AlgorithmParameter name=\"CanonicalizationMethod\" value=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></xwss:Transform></xwss:SignatureTarget></xwss:Sign></xwss:SecurityConfiguration>"
security_config  = """
<?xml version="1.0" encoding="UTF-8"?>
<xwss:SecurityConfiguration xmlns:xwss="http://java.sun.com/xml/ns/xwss/config" dumpMessages="true">
    <xwss:Timestamp />
    <xwss:Sign includeTimestamp="false">
        <xwss:SignatureMethod algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha512" />
        <xwss:SignatureTarget type="xpath" value="/*[local-name()='Envelope']/*[local-name()='Body']">
          <xwss:DigestMethod algorithm="http://www.w3.org/2001/04/xmlenc#sha512" />
          <xwss:Transform algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
              <xwss:AlgorithmParameter name="CanonicalizationMethod" value="http://www.w3.org/2001/10/xml-exc-c14n#" />
          </xwss:Transform>
        </xwss:SignatureTarget>
        <xwss:SignatureTarget type="xpath" value="/*[local-name()='Envelope']/*[local-name()='Header']/*[local-name()='Security']/*[local-name()='Timestamp']">
          <xwss:DigestMethod algorithm="http://www.w3.org/2001/04/xmlenc#sha512" />
          <xwss:Transform algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
              <xwss:AlgorithmParameter name="CanonicalizationMethod" value="http://www.w3.org/2001/10/xml-exc-c14n#" />
          </xwss:Transform>
        </xwss:SignatureTarget>
        <xwss:SignatureTarget type="xpath" value="/*[local-name()='Envelope']/*[local-name()='Header']/*[local-name()='Security']/*[local-name()='BinarySecurityToken']">
          <xwss:DigestMethod algorithm="http://www.w3.org/2001/04/xmlenc#sha512" />
          <xwss:Transform algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
              <xwss:AlgorithmParameter name="CanonicalizationMethod" value="http://www.w3.org/2001/10/xml-exc-c14n#" />
          </xwss:Transform>
        </xwss:SignatureTarget>
    </xwss:Sign>
</xwss:SecurityConfiguration>
"""


def test_xmlsig_sign():
    envelope = load_xml(
        b"""\
        <?xml version='1.0' encoding='utf-8'?>
        <soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/" soap-env:mustUnderstand="1">
            <soap-env:Header xmlns:wsa="http://www.w3.org/2005/08/addressing">
                <wsa:Action>http://schemas.mastercard.com.chssecure/2011/01/MasterCardCHSService/GetBenefitInformation</wsa:Action>
                <wsa:MessageID>urn:uuid:2cdb9e0c-6a7d-419b-b243-ebd76866cec0</wsa:MessageID>
                <wsa:To>https://X.mastercard.com/X/ALB/CHSSecureWebService</wsa:To>
                <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
                    <wsu:Timestamp xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
                        <wsu:Created>2021-02-09T20:57:37Z</wsu:Created>
                        <wsu:Expires>2021-02-09T21:57:37Z</wsu:Expires>
                    </wsu:Timestamp>
                </wsse:Security>
            </soap-env:Header>
            <soap-env:Body>
                <ns0:GetBenefitInformation xmlns:ns0="http://schemas.mastercard.com.chssecure/2011/01">
                    <ns0:benefitInfoReq>
                        <ns1:CardNumber xmlns:ns1="http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request">0000000000000000</ns1:CardNumber>
                        <ns2:ServiceProviderCode xmlns:ns2="http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request">01</ns2:ServiceProviderCode>
                        <ns3:SrcSysCode xmlns:ns3="http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request">X</ns3:SrcSysCode>
                    </ns0:benefitInfoReq>
                </ns0:GetBenefitInformation>
            </soap-env:Body>
        </soap-env:Envelope>""")

    # Create a signature template for RSA-SHA1 enveloped signature.
    sign = xmlsig.template.create(
        c14n_method=xmlsig.constants.TransformExclC14N,
        sign_method=xmlsig.constants.TransformRsaSha1,
    )
    asserts.assert_that(sign).is_not_none()

    # Add the <ds:Signature/> node to the document.
    envelope.append(sign)

    # Add the <ds:Reference/> node to the signature template.
    ref = xmlsig.template.add_reference(sign, xmlsig.constants.TransformSha1)

    # Add the enveloped transform descriptor.
    xmlsig.template.add_transform(ref, xmlsig.constants.TransformEnveloped)

    # Add the <ds:KeyInfo/> and <ds:KeyName/> nodes.
    key_info = xmlsig.template.ensure_key_info(sign)
    x509_data = xmlsig.template.add_x509_data(key_info)
    xmlsig.template.x509_data_add_issuer_serial(x509_data)
    xmlsig.template.x509_data_add_certificate(x509_data)
    ctx = xmlsig.SignatureContext()
    loaded = pkcs12.load_key_and_certificates(FIXTURE, HttpHeader['X-Keystore-Pass'])
    print("pkcs12.load_key_and_certificates: ", loaded)
    ctx.load_pkcs12(loaded)
    # Sign the template.
    ctx.sign(sign)
    signed = ElementTree.tostring(
                       envelope,
                       method="xml",
                       encoding="utf-8",
                       xml_declaration=True,
                       pretty_print=True,
    )
    print(signed)
    # plugin = BinarySignature(
    #     key_file=KEY_FILE_PW_STR,
    #     certfile=KEY_FILE_PW_STR,
    #     password=HttpHeader['X-Keystore-Pass'],
    #     signature_method=constants.TransformRsaSha512,
    #     digest_method=constants.TransformSha512,
    # )
    # envelope, headers = plugin.apply(envelope, {})
    # bintok = xp(envelope,
    #         "./soap-env:Header/wsse:Security/wsse:BinarySecurityToken",
    #         ns=namespaces,
    # )[0]
    # # element_or_tree,
    # #   encoding=None,
    # #   method="xml",
    # #   xml_declaration=None,
    # #   pretty_print=False,
    # #   with_tail=True,
    # #   standalone=None,
    # #   doctype=None,
    # # exclusive=
    # signed = ElementTree.tostring(
    #                 envelope,
    #                 method="xml",
    #                 encoding="utf-8",
    #                 xml_declaration=True,
    #                 pretty_print=True,
    # )
    # print(signed)
    # # doc = ElementTree.fromstring(signed)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_xmlsig_sign))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
