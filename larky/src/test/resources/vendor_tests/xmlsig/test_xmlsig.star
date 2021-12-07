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


# TEST START
load("./base", load_xml="load_xml", compare="compare")
load("./data/sign1_in_xml", SIGN1_IN_XML="SIGN1_IN_XML")
load("./data/sign1_out_xml", SIGN1_OUT_XML="SIGN1_OUT_XML")
load("./data/rsacert_pem", RSACERT_PEM="RSACERT_PEM")
load("./data/rsakey_pem", RSAKEY_PEM="RSAKEY_PEM")
load("./data/rsapub_pem", RSAPUB_PEM="RSAPUB_PEM")


namespaces = {
    'soap-env': xmlsig.ns.SOAP_ENV_11,
    'wsse': xmlsig.ns.WSSE,
    'ds': xmlsig.ns.DS,
    'ns0': "http://schemas.mastercard.com.chssecure/2011/01",
    "ns1": "http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request",
}


def find_node_by_attribute(nodes, attribute, value):
    """Search among nodes by attribute.
    Args:
        nodes: list of nodes
        attribute: attribute name
        value: attribute value
    Returns:
        First matched node
    Raises:
        IndexError if unable to find the node
    """
    # query = "//*[@*[local-name() = '{}' ] = '{}']"
    # node = reference.getroottree()
    return [
        node for node in nodes if node.get(attribute) == value
    ][0]


def test_xmlsig_sign_case1():
    tree = etree.parse(io.StringIO(SIGN1_IN_XML))
    root = tree.getroot()
    sign = root.find(
        ".//ds:" + xmlsig.constants.NodeSignature,
        namespaces={"ds": xmlsig.constants.DSigNs}
    )
    # or:
    # qname = QName(xmlsig.constants.DSigNs, xmlsig.constants.NodeSignature)
    # sign = root.find(qname)
    # print(sign)
    asserts.assert_that(sign).is_not_none()
    ctx = xmlsig.SignatureContext()
    loaded = serialization.load_pem_private_key(RSAKEY_PEM, password=None)
    # print("loaded: ", loaded)
    asserts.assert_that(loaded).is_not_none()
    ctx.private_key = loaded
    ctx.key_name = "rsakey.pem"
    asserts.assert_that(ctx.key_name).is_equal_to("rsakey.pem")
    ctx.sign(sign)
    compare(SIGN1_OUT_XML, root)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_xmlsig_sign_case1))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
