load("@stdlib//base64", base64="base64")
load("@stdlib//io", io="io")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//jks", jks="jks")

load("@vendor//asserts", asserts="asserts")
load("@vendor//cryptography/hazmat/backends/pycryptodome", backend="backend")
load("@stdlib//xml/etree/ElementTree", etree="ElementTree")
load("@vendor//xmlsig", xmlsig="xmlsig")


# TEST START
load("./base", parse_xml="parse_xml", compare="compare")
load("./data/sign2_in_xml", SIGN_IN_XML="SIGN_IN_XML")
load("./data/sign2_out_xml", SIGN_OUT_XML="SIGN_OUT_XML")
load("./data/keystore.jks", KEYSTORE="KEYSTORE")


def test_xmlsig_sign_jks():

    # Load document file.
    tree = etree.parse(io.StringIO(SIGN_IN_XML))
    template = tree.getroot()
    template = template[0]

    # Create a signature template for RSA-SHA1 enveloped signature.
    sign = xmlsig.template.create(
        c14n_method=xmlsig.constants.TransformInclC14N,
        sign_method=xmlsig.constants.TransformRsaSha1,
        ns=None,
    )

    asserts.assert_that(sign).is_not_none()

    # Add the <ds:Signature/> node to the document.
    template.append(sign)

    # Add the <ds:Reference/> node to the signature template.
    ref = xmlsig.template.add_reference(sign, xmlsig.constants.TransformSha1, uri="")

    # Add the enveloped transform descriptor.
    xmlsig.template.add_transform(ref, xmlsig.constants.TransformEnveloped)

    # Add the <ds:KeyInfo/> and <ds:KeyName/> nodes.
    key_info = xmlsig.template.ensure_key_info(sign)
    x509_data = xmlsig.template.add_x509_data(key_info)
    xmlsig.template.x509_data_add_subject_name(x509_data)
    xmlsig.template.x509_data_add_certificate(x509_data)

    for e in etree.flatten_nested_elements(sign):
        e.tail = ""
        e.text = ""

    # Create a digital signature context (no key manager is needed).
    # Load private key.
    # Set the key on the context.

    ctx = xmlsig.SignatureContext()

    key_file = io.StringIO(base64.b64decode(KEYSTORE))
    password = bytearray("FOJ0g5iUiuw0", "UTF-8")
    key_alias = bytearray("mc-ic-mtf", "UTF-8")

    (
        private_key,
        certificate,
        ca_certificates,
    ) = backend().load_key_and_certificates_from_jks(key_file.read(), password, key_alias, password)

    ctx.load_pkcs12((private_key, certificate))
    ctx.ca_certificates = ca_certificates

    # Sign the template.
    ctx.sign(sign)
    ctx.verify(sign)
    # Assert the contents of the XML document against the expected result.
    compare(SIGN_OUT_XML, template.getroot())


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_xmlsig_sign_jks))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
