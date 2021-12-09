load("@stdlib//larky", larky="larky")
load("@stdlib//io", io="io", BytesIO="BytesIO")
load("@stdlib//base64", base64="base64")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//types", types="types")
# load("@stdlib//xml/etree/ElementTree", etree="ElementTree")

# load("@vendor//elementtree/SimpleXMLTreeBuilder", SimpleXMLTreeBuilder="SimpleXMLTreeBuilder")
# load("@vendor//_etreeplus/C14NParser", C14NParser="C14NParser")
# load("@vendor//_etreeplus/xmlwriter", XMLWriter="XMLWriter")
# load("@vendor//_etreeplus/xmltree", xmltree="xmltree")
load("@vendor//cryptography/hazmat/primitives", serialization="serialization")
load("@vendor//lxml/etree", etree="etree")

load("@vendor//Crypto/PublicKey/RSA", RSA="RSA")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes")
load("@vendor//cryptography/x509/oid", ExtensionOID="ExtensionOID")
load("@vendor//option/result", Error="Error", Result="Result")

load("@vendor//xmlsig/constants", constants="constants")
load("@vendor//xmlsig/utils",
     b64_print="b64_print",
     create_node="create_node",
     get_rdns_name="get_rdns_name")


def SignatureContext():
    """
    Signature context is used to sign and verify Signature nodes with keys
    """
    self = larky.mutablestruct(__name__='SignatureContext', __class__=SignatureContext)

    def __init__():
        self.x509 = None
        self.crl = None
        self.private_key = None
        self.public_key = None
        self.key_name = None
        self.ca_certificates = []
        return self
    self = __init__()

    def sign(node):
        """
        Signs a Signature node
        :param node: Signature node
        :type node: xml.etree.Element
        :return: None
        """
        signed_info = node.find("ds:SignedInfo", namespaces=constants.NS_MAP)
        signature_method = signed_info.find(
            "ds:SignatureMethod", namespaces=constants.NS_MAP
        ).get("Algorithm")
        key_info = node.find("ds:KeyInfo", namespaces=constants.NS_MAP)
        if key_info != None:
            self.fill_key_info(key_info, signature_method)
        self.fill_signed_info(signed_info)
        self.calculate_signature(node)
    self.sign = sign

    def fill_key_info(key_info, signature_method):
        """
        Fills the KeyInfo node
        :param key_info: KeyInfo node
        :type key_info: xml.etree.Element
        :param signature_method: Signature node to use
        :type signature_method: str
        :return: None
        """
        x509_data = key_info.find("ds:X509Data", namespaces=constants.NS_MAP)
        if x509_data != None:
            self.fill_x509_data(x509_data)
        key_name = key_info.find("ds:KeyName", namespaces=constants.NS_MAP)
        if key_name != None and self.key_name != None:
            key_name.text = self.key_name
        key_value = key_info.find("ds:KeyValue", namespaces=constants.NS_MAP)
        if key_value != None:
            key_value.text = "\n"

            signature = constants.TransformUsageSignatureMethod[signature_method]
            key = self.public_key
            if self.public_key == None:
                key = self.private_key.public_key()
            if not types.is_instance(key, signature["method"].public_key_class):
                fail("Exception: Key not compatible with signature method")
            signature["method"].key_value(key_value, key)
    self.fill_key_info = fill_key_info

    def fill_x509_data(x509_data):
        """
        Fills the X509Data Node
        :param x509_data: X509Data Node
        :type x509_data: xml.etree.Element
        :return: None
        """
        x509_issuer_serial = x509_data.find(
            "ds:X509IssuerSerial", namespaces=constants.NS_MAP
        )
        if x509_issuer_serial != None:
            self.fill_x509_issuer_name(x509_issuer_serial)

        x509_crl = x509_data.find("ds:X509CRL", namespaces=constants.NS_MAP)
        if x509_crl != None and self.crl != None:
            x509_data.text = base64.b64encode(
                self.crl.public_bytes(serialization.Encoding.DER)
            )
        x509_subject = x509_data.find("ds:X509SubjectName", namespaces=constants.NS_MAP)
        if x509_subject != None:
            x509_subject.text = get_rdns_name(self.x509.subject.rdns)
        x509_ski = x509_data.find("ds:X509SKI", namespaces=constants.NS_MAP)
        if x509_ski != None:
            x509_ski.text = base64.b64encode(
                self.x509.extensions.get_extension_for_oid(
                    ExtensionOID.SUBJECT_KEY_IDENTIFIER
                ).value.digest
            )
        x509_certificate = x509_data.find(
            "ds:X509Certificate", namespaces=constants.NS_MAP
        )
        if x509_certificate != None:
            s = base64.b64encode(
                self.x509.public_bytes(serialization.Encoding.DER)
            )
            x509_certificate.text = b64_print(s)
            for certificate in self.ca_certificates:
                certificate_node = create_node(
                    "X509Certificate", None, constants.DSigNs, tail="\n"
                )
                certificate_node.text = b64_print(
                    base64.b64encode(
                        certificate.public_bytes(serialization.Encoding.DER)
                    )
                )
                x509_certificate.addnext(certificate_node)
    self.fill_x509_data = fill_x509_data

    def fill_x509_issuer_name(x509_issuer_serial):
        """
        Fills the X509IssuerSerial node
        :param x509_issuer_serial: X509IssuerSerial node
        :type x509_issuer_serial: xml.etree.Element
        :return: None
        """
        x509_issuer_name = x509_issuer_serial.find(
            "ds:X509IssuerName", namespaces=constants.NS_MAP
        )
        if x509_issuer_name != None:
            x509_issuer_name.text = get_rdns_name(self.x509.issuer.rdns)
        x509_issuer_number = x509_issuer_serial.find(
            "ds:X509SerialNumber", namespaces=constants.NS_MAP
        )
        if x509_issuer_number != None:
            x509_issuer_number.text = str(self.x509.serial_number)
    self.fill_x509_issuer_name = fill_x509_issuer_name

    def fill_signed_info(signed_info):
        """
        Fills the SignedInfo node
        :param signed_info: SignedInfo node
        :type signed_info: xml.etree.Element
        :return: None
        """
        for reference in signed_info.findall(
            "ds:Reference", namespaces=constants.NS_MAP
        ):
            self.calculate_reference(reference, True)
    self.fill_signed_info = fill_signed_info

    def verify(node):
        """
        Verifies a signature
        :param node: Signature node
        :type node: xml.etree.Element
        :return: None
        """
        # Added XSD Validation
        # with open(
        #     path.join(path.dirname(__file__), "data/xmldsig-core-schema.xsd"), "rb"
        # ) as file:
        #     schema = etree.XMLSchema(etree.fromstring(file.read()))
        # schema.assertValid(node)
        # Validates reference value
        signed_info = node.find("ds:SignedInfo", namespaces=constants.NS_MAP)
        for reference in signed_info.findall(
            "ds:Reference", namespaces=constants.NS_MAP
        ):
            if not self.calculate_reference(reference, False):
                return Error("Exception: " + 'Reference with URI:"' + reference.get("URI", "") + '" failed'
                )
        # Validates signature value
        self.calculate_signature(node, False)
    self.verify = verify

    def transform(transform, node):
        """
        Transforms a node following the transform especification
        :param transform: Transform node
        :type transform: xml.etree.Element
        :param node: Element to transform
        :type node: str
        :return: Transformed node in a String
        """
        method = transform.get("Algorithm")
        if method not in constants.TransformUsageDSigTransform:
            fail("Exception: Method not allowed")
        # C14N methods are allowed
        if method in constants.TransformUsageC14NMethod:
            return self.canonicalization(method, etree.fromstring(node))
        # print("here2?", method)

        # Enveloped method removes the Signature Node from the element
        if method == constants.TransformEnveloped:
            # print("here3?", method)
            tree = transform.getroottree()
            root = etree.fromstring(node)
            pointer2parent = tree.getelementpath(
                transform.getparent().getparent().getparent().getparent()
            )
            signature = root.find(pointer2parent)
            root.remove(signature)
            return self.canonicalization(constants.TransformInclC14N, root)
        if method == constants.TransformBase64:
            # print("here4?", method)

            rval = (Result.Ok(node)
                    .map(etree.fromstring)
                    .map(lambda r: r.text)
                    .map(base64.b64decode))
            if rval.is_ok:
                return rval.unwrap()
            # print("here5?", method)
            return base64.b64decode(node)

        fail("Exception: Method not found")
    self.transform = transform

    def canonicalization(method, node):
        """
        Canonicalizes a node following the method
        :param method: Method identification
        :type method: str
        :param node: object to canonicalize
        :type node: str
        :return: Canonicalized node in a String
        """
        if method not in constants.TransformUsageC14NMethod:
            fail("Exception: " + "Method not allowed: " + method)
        c14n_method = constants.TransformUsageC14NMethod[method]
        c14n_node = etree.tostring(
            node,
            method=c14n_method["method"],
            with_comments=c14n_method["comments"],
            exclusive=c14n_method["exclusive"],
        )
        if c14n_method["exclusive"] == False:
            # TODO: there must be a nicer way to do this. See also:
            # http://www.w3.org/TR/xml-c14n, "namespace axis"
            # http://www.w3.org/TR/xml-c14n2/#sec-Namespace-Processing
            c14n_node = c14n_node.replace(' xmlns=""', '')
        return c14n_node
    self.canonicalization = canonicalization

    def digest(method, node):
        """
        Returns the digest of an object from a method name
        :param method: hash method
        :type method: str
        :param node: Object to hash
        :type node: str
        :return: hash result
        """
        # b'ZLwUdyUkLUovpCNNO8VloO1jtxY='
        if method not in constants.TransformUsageDigestMethod:
            fail("Exception: Method not allowed")
        lib = hashlib.new(constants.TransformUsageDigestMethod[method])
        # root = etree.fromstring(node, parser=SimpleXMLTreeBuilder.TreeBuilder())
        # writer0 = XMLWriter(etree.ElementTree(root))
        # lib.update(tobytes(writer0()))
        lib.update(tobytes(node))
        return base64.b64encode(lib.digest())
    self.digest = digest

    def get_uri(uri, reference):
        """
        It returns the node of the specified URI
        :param uri: uri of the
        :type uri: str
        :param reference: Reference node
        :type reference: xml.etree.Element
        :return: Element of the URI in a String
        """
        if uri == "":
            return self.canonicalization(
                constants.TransformInclC14N, reference.getroottree()
            )
        if uri.startswith("#"):
            query = "//*[@*[local-name() = '{}' ] = '{}']"
            node = reference.getroottree()
            results = self.check_uri_attr(node, query, uri, constants.ID_ATTR)
            if len(results) == 0:
                results = self.check_uri_attr(node, query, uri, "ID")
            if len(results) == 0:
                results = self.check_uri_attr(node, query, uri, "Id")
            if len(results) == 0:
                results = self.check_uri_attr(node, query, uri, "id")
            if len(results) > 1:
                fail("Ambiguous reference URI {} resolved to {} nodes"
                    .format(uri, len(results)))
            elif len(results) == 1:
                return self.canonicalization(constants.TransformInclC14N, results[0])
        return fail("Exception: URI " + uri + ' cannot be read')
    self.get_uri = get_uri

    def check_uri_attr(node, xpath_query, uri, attr):
        return node.findall(xpath_query.format(attr, uri.lstrip("#")))
    self.check_uri_attr = check_uri_attr

    def calculate_reference(reference, sign=True):
        """
        Calculates or verifies the digest of the reference
        :param reference: Reference node
        :type reference: xml.etree.Element
        :param sign: It marks if we must sign or check a signature
        :type sign: bool
        :return: None
        """
        node = self.get_uri(reference.get("URI", ""), reference)
        transforms = reference.find("ds:Transforms", namespaces=constants.NS_MAP)
        if transforms != None:
            for transform in transforms.findall(
                "ds:Transform", namespaces=constants.NS_MAP
            ):
                node = self.transform(transform, node)
        digest_value = self.digest(
            reference.find("ds:DigestMethod", namespaces=constants.NS_MAP).get(
                "Algorithm"
            ),
            node,
        )
        if not sign:
            return (
                codecs.decode(digest_value, encoding="utf-8")
                == reference.find("ds:DigestValue", namespaces=constants.NS_MAP).text
            )

        reference.find(
            "ds:DigestValue", namespaces=constants.NS_MAP
        ).text = digest_value
    self.calculate_reference = calculate_reference

    def calculate_signature(node, sign=True):
        """
        Calculate or verifies the signature
        :param node: Signature node
        :type node: xml.etree.Element
        :param sign: It checks if it must calculate or verify
        :type sign: bool
        :return: None
        """
        signed_info_xml = node.find("ds:SignedInfo", namespaces=constants.NS_MAP)
        canonicalization_method = signed_info_xml.find(
            "ds:CanonicalizationMethod", namespaces=constants.NS_MAP
        ).get("Algorithm")
        signature_method = signed_info_xml.find(
            "ds:SignatureMethod", namespaces=constants.NS_MAP
        ).get("Algorithm")
        if signature_method not in constants.TransformUsageSignatureMethod:
            fail("Exception: Method " + signature_method + " not accepted")
        signature = constants.TransformUsageSignatureMethod[signature_method]
        signed_info = self.canonicalization(canonicalization_method, signed_info_xml)
        if not sign:
            signature_value = node.find(
                "ds:SignatureValue", namespaces=constants.NS_MAP
            ).text
            public_key = signature["method"].get_public_key(node, self)
            signature["method"].verify(
                signature_value, tobytes(signed_info), public_key, signature["digest"]
            )
        else:
            node.find(
                "ds:SignatureValue", namespaces=constants.NS_MAP
            ).text = b64_print(
                base64.b64encode(
                    signature["method"].sign(
                        tobytes(signed_info), self.private_key, signature["digest"]
                    )
                )
            )
    self.calculate_signature = calculate_signature

    def load_pkcs12(key):
        """
        This function fills the context public_key, private_key and x509 from
        PKCS12 Object
        :param key: the PKCS12 Object
        :type key: Union[OpenSSL.crypto.PKCS12, tuple]
        :return: None
        """
        self.x509 = key[1]
        self.public_key = key[1].public_key()
        self.private_key = key[0]
        # self.x509 = key.cert
        # self.private_key = RSA.import_key(key.key.private_key())
        # self.public_key = self.private_key.public_key()
    self.load_pkcs12 = load_pkcs12
    return self

