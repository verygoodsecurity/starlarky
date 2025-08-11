load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io", io="io")
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//xml/etree/ElementTree", QName="QName", ElementTree="ElementTree")

load("@vendor//asserts", asserts="asserts")
load("@vendor//cryptography/hazmat/backends", default_backend="default_backend")
load("@vendor//cryptography/hazmat/primitives", serialization="serialization")
load("@vendor//cryptography/hazmat/primitives/serialization", serialization="serialization")
load("@vendor//cryptography/x509", load_pem_x509_certificate="load_pem_x509_certificate")
load("@vendor//lxml/etree", etree="etree")

load("@vendor//xmlsig", xmlsig="xmlsig")


namespaces = {
    'soap-env': xmlsig.ns.SOAP_ENV_11,
    'wsse': xmlsig.ns.WSSE,
    'ds': "http://www.w3.org/2000/09/xmldsig",
    'ns0': "http://schemas.mastercard.com.chssecure/2011/01",
    "ns1": "http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request",
}


soap_string = """
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Header>
    <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                   xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                   SOAP-ENV:mustUnderstand="1">
      <wsse:BinarySecurityToken
          EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
          ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
          wsu:Id="X509-32cf6c96-474d-4406-8154-00dfb819608e">
        DUMMY_BINARY_SECURITY_TOKEN
      </wsse:BinarySecurityToken>
      <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#" Id="SIG-79a699c8-8a55-4488-977c-ef7f6c31bc02">
      </ds:Signature>
    </wsse:Security>
  </SOAP-ENV:Header>
  <SOAP-ENV:Body xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                 wsu:Id="id-b2deab14-2399-4e0f-93fa-e7dd030c2fe6">
    <ns2:requestMessage xmlns:ns2="urn:schemas-cybersource-com:transaction-data-0.000">
    </ns2:requestMessage>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
"""

def test_xmlsig_sign_case1():

    # case 1: Create signature template
    signature = xmlsig.template.create(
        xmlsig.constants.TransformExclC14N,
        xmlsig.constants.TransformRsaSha256
    )

    # case 2: Parse SOAP and locate Signature element
    tree = etree.parse(io.StringIO(soap_string))
    root = tree.getroot()
    sign = None
    
    # sign = root.find(
    # ".//ds:" + xmlsig.constants.NodeSignature,
    # namespaces={"ds": xmlsig.constants.DSigNs}
    # )
    
    for element in root.iter():
      if str(element.tag).endswith("Signature"):
          sign = element
          break
    print(sign)
    # asserts.assert_true(sign)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_xmlsig_sign_case1))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
