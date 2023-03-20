load("@stdlib//unittest", unittest="unittest")
load("@stdlib//xml/etree/ElementTree", etree="ElementTree")

load("@vgs//util/crypt", crypt="crypt")

# TEST START
load("./base", parse_xml="parse_xml", compare="compare")
load("./data/sign_in_xml", SIGN_IN_XML="SIGN_IN_XML")
load("./data/sign_out_xml", SIGN_OUT_XML="SIGN_OUT_XML")
load("./data/keystore.jks", KEYSTORE="KEYSTORE")


def test_crypt():
    signed_xml = crypt.mcInControlSignature(SIGN_IN_XML, "OBOAuthenticateRequest", KEYSTORE, "FOJ0g5iUiuw0", "mc-ic-mtf", "FOJ0g5iUiuw0")
    compare(SIGN_OUT_XML, etree.fromstring(signed_xml).getroot())

def suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.FunctionTestCase(test_crypt))
    return suite


runner = unittest.TextTestRunner()
runner.run(suite())
