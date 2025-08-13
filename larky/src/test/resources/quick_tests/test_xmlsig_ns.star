load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io", io="io")
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//xml/etree/ElementTree", QName="QName", etree="ElementTree")

load("@vendor//asserts", asserts="asserts")
load("@vendor//cryptography/hazmat/backends", default_backend="default_backend")
load("@vendor//cryptography/hazmat/primitives", serialization="serialization")
load("@vendor//cryptography/hazmat/primitives/serialization", serialization="serialization")
load("@vendor//cryptography/x509", load_pem_x509_certificate="load_pem_x509_certificate")

load("@vendor//xmlsig", xmlsig="xmlsig")


namespaces = {
    'soap-env': xmlsig.ns.SOAP_ENV_11,
    'wsse': xmlsig.ns.WSSE,
    'ds': "http://www.w3.org/2000/09/xmldsig",
    'ns0': "http://schemas.mastercard.com.chssecure/2011/01",
    "ns1": "http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request",
}



xml_data = """
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

def test_xmlsig_sign_find_element():
  # 1. Define the namespaces used in the XML
  namespaces = {
      'SOAP-ENV': 'http://schemas.xmlsoap.org/soap/envelope/',
      'wsse': 'http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd',
      'ds': 'http://www.w3.org/2000/09/xmldsig#'
  }

  # 2. Parse the XML string
  root =  etree.parse(io.StringIO(xml_data)).getroot()

  # 3. Define the XPath to the Signature element
  # The path is: Envelope -> Header -> Security -> Signature
  path = './SOAP-ENV:Header/wsse:Security/ds:Signature'

  # 4. Use find() with the path and the namespaces dictionary
  signature_element = root.find(path, namespaces)

  # 5. Check the result and print its attributes
  if signature_element != None:
      print("Found the <Signature> element!")
      print("Tag:", signature_element.tag)
      print("Attributes:", signature_element.attrib)
  else:
      print("Signature element not found.")


def test_xmlsig_create_sign_template():
  # 1. Define the namespaces used in the XML
  signature = xmlsig.template.create(
   xmlsig.constants.TransformExclC14N,
   xmlsig.constants.TransformRsaSha256
  )
  
  asserts.assert_true(signature)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_xmlsig_sign_find_element))
    _suite.addTest(unittest.FunctionTestCase(test_xmlsig_create_sign_template))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
