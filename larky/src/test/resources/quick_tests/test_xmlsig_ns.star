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
      'wsu': 'http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd',
      'ds': 'http://www.w3.org/2000/09/xmldsig#',
      'ns2': 'urn:schemas-cybersource-com:transaction-data-0.000'
  }

  # 2. Parse the XML string
  root = etree.fromstring(xml_data)

  # 3. Test finding various elements with namespace resolution
  
  # Test root element (Envelope)
  envelope_element = root
  asserts.assert_true(envelope_element != None)
  asserts.assert_true(envelope_element.tag.endswith('}Envelope'))
  
  # Test Header element
  header_element = root.find('./SOAP-ENV:Header', namespaces)
  asserts.assert_true(header_element != None)
  
  # Test Security element
  security_element = root.find('./SOAP-ENV:Header/wsse:Security', namespaces)
  asserts.assert_true(security_element != None)
  
  # Test BinarySecurityToken element
  binary_token = root.find('./SOAP-ENV:Header/wsse:Security/wsse:BinarySecurityToken', namespaces)
  asserts.assert_true(binary_token != None)
  
  # Test Signature element (original test case)
  signature_element = root.find('./SOAP-ENV:Header/wsse:Security/ds:Signature', namespaces)
  asserts.assert_true(signature_element != None)
  
  # Test Body element
  body_element = root.find('./SOAP-ENV:Body', namespaces)
  asserts.assert_true(body_element != None)
  
  # Test requestMessage element
  request_message = root.find('./SOAP-ENV:Body/ns2:requestMessage', namespaces)
  asserts.assert_true(request_message != None)
  
  # Test finding elements using different path styles
  
  # Test descendant search (//)
  security_descendant = root.find('.//wsse:Security', namespaces)
  asserts.assert_true(security_descendant != None)
  
  # Test finding from non-root elements
  signature_from_security = security_element.find('./ds:Signature', namespaces)
  asserts.assert_true(signature_from_security != None)
  
  binary_from_security = security_element.find('./wsse:BinarySecurityToken', namespaces)
  asserts.assert_true(binary_from_security != None)
  
  # Test attribute access on found elements
  asserts.assert_true(signature_element.get('Id') == 'SIG-79a699c8-8a55-4488-977c-ef7f6c31bc02')
  asserts.assert_true(binary_token.get('wsu:Id') != None)
  
  # Test findall() for specific elements and ensure it works with namespaces
  all_wsse_security = root.findall('.//wsse:Security', namespaces)
  asserts.assert_true(len(all_wsse_security) == 1)  # Should find exactly one Security element
  
  all_wsse_tokens = root.findall('.//wsse:BinarySecurityToken', namespaces)
  asserts.assert_true(len(all_wsse_tokens) == 1)  # Should find exactly one BinarySecurityToken
  
  # Test finding direct children
  header_children = header_element.findall('./*')
  asserts.assert_true(len(header_children) >= 1)  # Should find Security element


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
