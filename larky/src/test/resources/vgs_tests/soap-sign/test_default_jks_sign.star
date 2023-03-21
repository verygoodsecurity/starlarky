load("@stdlib//unittest", unittest="unittest")
load("@stdlib//xml/etree/ElementTree", etree="ElementTree")

load("@vgs//soap-sign/signer", signer="signer")

# TEST START
load("../xmlsig-java-compatible/base", parse_xml="parse_xml", compare="compare")
load("../xmlsig-java-compatible/data/sign_in_xml", SIGN_IN_XML="SIGN_IN_XML")
load("../xmlsig-java-compatible/data/sign_out_xml", SIGN_OUT_XML="SIGN_OUT_XML")
load("../xmlsig-java-compatible/data/keystore.jks", KEYSTORE="KEYSTORE")


def test_sign_with_jks():
    signed_xml = signer.sign_with_jks(SIGN_IN_XML, "OBOAuthenticateRequest", KEYSTORE, "FOJ0g5iUiuw0", "mc-ic-mtf", "FOJ0g5iUiuw0")
    compare(SIGN_OUT_XML, etree.fromstring(signed_xml).getroot())

def suite():
    suite = unittest.TestSuite()
    suite.addTest(unittest.FunctionTestCase(test_sign_with_jks))
    return suite


runner = unittest.TextTestRunner()
runner.run(suite())
