load("@stdlib//larky", larky="larky")
load("@stdlib//base64", base64="base64")
load("@stdlib//io", io="io")
load("@vgs//jks/jks2pycryptodome", jks="jks2pycryptodome")

load("@vendor//cryptography/hazmat/backends/pycryptodome", backend="backend")
load("@stdlib//xml/etree/ElementTree", etree="ElementTree")
load("@vgs//xmlsig-java-compatible", xmlsig="xmlsig")


def jks_sign(xml_string, sign_element_xpath, keystore_base64, keystore_password, key_alias, key_password):
    """Facade function for simplified usage of Java compatible version of xmlsig library together with Java KeyStore API adapter.

    Encapsulates XML signature template creation and signature context initialization

    :param xml_string: XML payload to sign
    :param sign_element_xpath: XML element name to put signature element after
    :param keystore_base64: Base64 encoded bytes of Java keystore
    :param keystore_password: the password used to check the integrity of the keystore, the password used to unlock the keystore
    :param key_alias: key alias used for signature
    :param key_password: password for recovering the key
    """

    # Load document file.
    tree = etree.parse(io.StringIO(xml_string))
    root = tree.getroot()
    template = root.find(sign_element_xpath)

    # Create a signature template for RSA-SHA1 enveloped signature.
    sign = xmlsig.template.create(
        c14n_method=xmlsig.constants.TransformInclC14N,
        sign_method=xmlsig.constants.TransformRsaSha1,
        ns=None,
    )

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

    key_file = io.StringIO(base64.b64decode(keystore_base64))
    keystore_password = bytearray(keystore_password, "UTF-8")
    key_password = bytearray(key_password, "UTF-8")
    key_alias = bytearray(key_alias, "UTF-8")

    (
        private_key,
        certificate,
        ca_certificates,
    ) = jks.load_key_and_certificates(key_file.read(), keystore_password, key_alias, key_password)

    ctx.load_pkcs12((private_key, certificate))
    ctx.ca_certificates = ca_certificates

    # Sign the template.
    ctx.sign(sign)

    return etree.tostring(template.getroot())

jks_sign = larky.struct(
    __name__='jks_sign',
    jks_sign=jks_sign
)