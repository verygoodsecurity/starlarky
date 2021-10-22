load("@stdlib//larky", larky="larky")
load("@stdlib//enum", enum="Enum")
load("@stdlib//uuid", uuid="uuid")
load("@stdlib//xml/etree/ElementTree", etree="ElementTree", QName="QName")
load("@vendor//xmlsec/constants", constants="constants")
load("@vendor//xmlsec/elementmaker", ElementMaker="ElementMaker")
load("@vendor//xmlsec/ns", ns="ns")
load("@vendor//xmlsec/template", template="template")
load("@vendor//xmlsec/utils", get_or_create_header="get_or_create_header", detect_soap_env="detect_soap_env")
load("@vendor//option/result", Result="Result", Ok="Ok", Error="Error")
load("@stdlib//jopenssl", _JOpenSSL="jopenssl")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes")

# SOAP envelope
SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/"
NSMAP = {"wsse": ns.WSSE, "wsu": ns.WSU}
ID_ATTR = etree.QName(NSMAP["wsu"], "Id")
WSSE = ElementMaker(namespace=NSMAP["wsse"], nsmap={"wsse": ns.WSSE})
WSU = ElementMaker(namespace=NSMAP["wsu"], nsmap={"wsu": ns.WSU})


InternalError = Error("InternalError")
VerificationError = Error("VerificationError")


Transform = larky.struct(
    __name__='Transform',
    EXCL_C14N=constants.TransformExclC14N,
    RSA_SHA1=constants.TransformRsaSha1,
    SHA1=constants.TransformSha1,
)


KeyFormat = larky.struct(
    __name__='KeyFormat',
    UNKNOWN=constants.KeyDataFormatUnknown,
    BINARY=constants.KeyDataFormatBinary,
    PEM=constants.KeyDataFormatPem,
    DER=constants.KeyDataFormatDer,
    PKCS8_PEM=constants.KeyDataFormatPkcs8Pem,
    PKCS8_DER=constants.KeyDataFormatPkcs8Der,
    PKCS12_PEM=constants.KeyDataFormatPkcs12,
    CERT_PEM=constants.KeyDataFormatCertPem,
    CERT_DER=constants.KeyDataFormatCertDer,
    _value=lambda z: [k for k, v in KeyFormat.__dict__.items() if v == z][0],
)


def _Key(key):

    self = larky.mutablestruct(__name__='Key', __class__=_Key)
    def __init__(key):
        self.key = key
        self.cert = None
        return self
    self = __init__(key)

    def load_cert_from_memory(certdata, keyformat):
        self.cert = _JOpenSSL.OpenSSL.load_certificate(certdata, KeyFormat._value(keyformat))
    self.load_cert_from_memory = load_cert_from_memory
    return self


def _from_memory(key_data, keyformat, password=None):
    key = _JOpenSSL.OpenSSL.load_privatekey(key_data, password)
    return _Key(key)


def _from_binary_data(klass, data):
    pass


def _from_binary_file(klass, filename):
    pass


def _from_file(file, format,password = None):
    pass


def _generate(klass, size, type):
    pass


Key = larky.struct(
    from_memory=_from_memory,
    from_binary_data=_from_binary_data,
    from_binary_file=_from_binary_file,
    from_file=_from_file,
    __call__=_Key,
)


def KeysManager():
    self = larky.mutablestruct(__name__='KeysManager', __class__=KeysManager)

    def add_key(key):
        pass
    self.add_key = add_key

    def load_cert(filename, format, type):
        pass
    self.load_cert = load_cert

    def load_cert_from_memory(data, format, type):
        pass
    self.load_cert_from_memory = load_cert_from_memory
    return self


def EncryptionContext(manager=None):
    # type: (Optional[KeysManager]) -> EncryptionContext
    self = larky.mutablestruct(__name__='EncryptionContext', __class__=EncryptionContext)

    def __init__(manager):
        # type: (Optional[KeysManager]) -> Any
        self.key = None
        return self
    self = __init__(manager)

    def decrypt(node):
        pass
    self.decrypt = decrypt

    def encrypt_binary(template, data):
        pass
    self.encrypt_binary = encrypt_binary

    def encrypt_uri(template, uri):
        pass
    self.encrypt_uri = encrypt_uri

    def encrypt_xml(template, node):
        pass
    self.encrypt_xml = encrypt_xml

    def reset():
        pass
    self.reset = reset

    return self


def _SignatureContext():
    self = larky.mutablestruct(__class__=_SignatureContext, __name__='SignatureContext')

    def enable_reference_transform(transform):
        pass
    self.enable_reference_transform = enable_reference_transform

    def enable_signature_transform(transform):
        pass
    self.enable_signature_transform = enable_signature_transform

    def register_id(node, id_attr="ID", id_ns=None):
        pass
    self.register_id = register_id

    def set_enabled_key_data(keydata_list):
        pass
    self.set_enabled_key_data = set_enabled_key_data

    def sign(node):
        pass
    self.sign = sign

    def sign_binary(bytes, transform):
        pass
    self.sign_binary = sign_binary

    def verify(node):
        pass
    self.verify = verify

    def verify_binary(bytes, transform, signature):
        pass
    self.verify_binary = verify_binary
    return self


def get_unique_id():
    return "id-%s" % uuid.uuid4()


def ensure_id(node):
    """Ensure given node has a wsu:Id attribute; add unique one if not.

    Return found/created attribute value.

    """
    if node == None:
        fail("ensure_id received a Node which is None")
    id_val = node.get(ID_ATTR.text)
    if not id_val:
        id_val = get_unique_id()
        node.set(ID_ATTR.text, id_val)
    return id_val


def get_security_header(doc):
    """Return the security header. If the header doesn't exist it will be
    created.

    """
    header = get_or_create_header(doc)
    security = header.find("wsse:Security", namespaces=NSMAP)
    if security == None:
        security = WSSE.Security()
        header.append(security)
    return security


def _make_sign_key(key_data, cert_data, password):
    key = Key.from_memory(tobytes(key_data), KeyFormat.PEM, tobytes(password))
    key.load_cert_from_memory(cert_data, KeyFormat.PEM)
    return key


def _make_verify_key(cert_data):
    key = Key.from_memory(cert_data, KeyFormat.CERT_PEM, None)
    return key


def MemorySignature(key_data,
    cert_data,
    password=None,
    signature_method=None,
    digest_method=None,
):
    """Sign given SOAP envelope with WSSE sig using given key and cert."""
    self = larky.mutablestruct(__name__='MemorySignature', __class__=MemorySignature)

    def __init__(
        key_data,
        cert_data,
        password,
        signature_method,
        digest_method,
    ):

        self.key_data = key_data
        self.cert_data = cert_data
        self.password = password
        self.digest_method = digest_method
        self.signature_method = signature_method
        return self
    self = __init__(key_data, cert_data, password, signature_method, digest_method)

    def apply(envelope, headers):
        key = _make_sign_key(self.key_data, self.cert_data, self.password)
        _sign_envelope_with_key(
            envelope, key, self.signature_method, self.digest_method
        )
        return envelope, headers
    self.apply = apply

    def verify(envelope):
        key = _make_verify_key(self.cert_data)
        _verify_envelope_with_key(envelope, key)
        return envelope
    self.verify = verify
    return self


def Signature(key_file,
    certfile,
    password=None,
    signature_method=None,
    digest_method=None,
):
    """Sign given SOAP envelope with WSSE sig using given key file and cert file."""
    self = MemorySignature(key_file, certfile, password, signature_method, digest_method)
    self.__name__ = 'Signature'
    self.__class__ = Signature
    return self


def BinarySignature(key_file,
    certfile,
    password=None,
    signature_method=None,
    digest_method=None,
):
    """Sign given SOAP envelope with WSSE sig using given key file and cert file.

    Place the key information into BinarySecurityElement."""
    self = MemorySignature(key_file, certfile, password, signature_method, digest_method)
    self.__name__ = 'BinarySignature'
    self.__class__ = BinarySignature

    def apply(envelope, headers):
        print("did i get here?")
        key = _make_sign_key(self.key_data, self.cert_data, self.password)
        _sign_envelope_with_key_binary(
            envelope, key, self.signature_method, self.digest_method
        )
        return envelope, headers
    self.apply = apply
    return self


def sign_envelope(
    envelope,
    keyfile,
    certfile,
    password=None,
    signature_method=None,
    digest_method=None,
):
    """Sign given SOAP envelope with WSSE sig using given key and cert.

    Sign the wsu:Timestamp node in the wsse:Security header and the soap:Body;
    both must be present.

    Add a ds:Signature node in the wsse:Security header containing the
    signature.

    Use EXCL-C14N transforms to normalize the signed XML (so that irrelevant
    whitespace or attribute ordering changes don't invalidate the
    signature). Use SHA1 signatures.

    Expects to sign an incoming document something like this (xmlns attributes
    omitted for readability):

    <soap:Envelope>
      <soap:Header>
        <wsse:Security mustUnderstand="true">
          <wsu:Timestamp>
            <wsu:Created>2015-06-25T21:53:25.246276+00:00</wsu:Created>
            <wsu:Expires>2015-06-25T21:58:25.246276+00:00</wsu:Expires>
          </wsu:Timestamp>
        </wsse:Security>
      </soap:Header>
      <soap:Body>
        ...
      </soap:Body>
    </soap:Envelope>

    After signing, the sample document would look something like this (note the
    added wsu:Id attr on the soap:Body and wsu:Timestamp nodes, and the added
    ds:Signature node in the header, with ds:Reference nodes with URI attribute
    referencing the wsu:Id of the signed nodes):

    <soap:Envelope>
      <soap:Header>
        <wsse:Security mustUnderstand="true">
          <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
            <SignedInfo>
              <CanonicalizationMethod
                  Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
              <SignatureMethod
                  Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
              <Reference URI="#id-d0f9fd77-f193-471f-8bab-ba9c5afa3e76">
                <Transforms>
                  <Transform
                      Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                </Transforms>
                <DigestMethod
                    Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
                <DigestValue>nnjjqTKxwl1hT/2RUsBuszgjTbI=</DigestValue>
              </Reference>
              <Reference URI="#id-7c425ac1-534a-4478-b5fe-6cae0690f08d">
                <Transforms>
                  <Transform
                      Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                </Transforms>
                <DigestMethod
                    Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
                <DigestValue>qAATZaSqAr9fta9ApbGrFWDuCCQ=</DigestValue>
              </Reference>
            </SignedInfo>
            <SignatureValue>Hz8jtQb...bOdT6ZdTQ==</SignatureValue>
            <KeyInfo>
              <wsse:SecurityTokenReference>
                <X509Data>
                  <X509Certificate>MIIDnzC...Ia2qKQ==</X509Certificate>
                  <X509IssuerSerial>
                    <X509IssuerName>...</X509IssuerName>
                    <X509SerialNumber>...</X509SerialNumber>
                  </X509IssuerSerial>
                </X509Data>
              </wsse:SecurityTokenReference>
            </KeyInfo>
          </Signature>
          <wsu:Timestamp wsu:Id="id-7c425ac1-534a-4478-b5fe-6cae0690f08d">
            <wsu:Created>2015-06-25T22:00:29.821700+00:00</wsu:Created>
            <wsu:Expires>2015-06-25T22:05:29.821700+00:00</wsu:Expires>
          </wsu:Timestamp>
        </wsse:Security>
      </soap:Header>
      <soap:Body wsu:Id="id-d0f9fd77-f193-471f-8bab-ba9c5afa3e76">
        ...
      </soap:Body>
    </soap:Envelope>

    """
    # Load the signing key and certificate.
    key = _make_sign_key(keyfile, certfile, password)
    return _sign_envelope_with_key(envelope, key, signature_method, digest_method)


def _signature_prepare(envelope, key, signature_method, digest_method):
    """Prepare envelope and sign."""
    soap_env = detect_soap_env(envelope)

    # Create the Signature node.
    signature = template.create(
        envelope,
        Transform.EXCL_C14N,
        signature_method or Transform.RSA_SHA1,
    )

    # Add a KeyInfo node with X509Data child to the Signature. XMLSec will fill
    # in this template with the actual certificate details when it signs.
    key_info = template.ensure_key_info(signature)
    x509_data = template.add_x509_data(key_info)
    template.x509_data_add_issuer_serial(x509_data)
    template.x509_data_add_certificate(x509_data)

    # Insert the Signature node in the wsse:Security header.
    security = get_security_header(envelope)
    security.insert(0, signature)

    # Perform the actual signing.
    ctx = _SignatureContext()
    ctx.key = key
    _sign_node(ctx, signature, envelope.find(QName(soap_env, "Body")), digest_method)
    timestamp = security.find(QName(ns.WSU, "Timestamp"))
    if timestamp != None:
        _sign_node(ctx, signature, timestamp, digest_method)
    ctx.sign(signature)

    # Place the X509 data inside a WSSE SecurityTokenReference within
    # KeyInfo. The recipient expects this structure, but we can't rearrange
    # like this until after signing, because otherwise xmlsec won't populate
    # the X509 data (because it doesn't understand WSSE).
    sec_token_ref = etree.SubElement(key_info, QName(ns.WSSE, "SecurityTokenReference"))
    return security, sec_token_ref, x509_data


def _sign_envelope_with_key(envelope, key, signature_method, digest_method):
    _, sec_token_ref, x509_data = _signature_prepare(
        envelope, key, signature_method, digest_method
    )
    sec_token_ref.append(x509_data)


def _sign_envelope_with_key_binary(envelope, key, signature_method, digest_method):
    security, sec_token_ref, x509_data = _signature_prepare(
        envelope, key, signature_method, digest_method
    )
    ref = etree.SubElement(
        sec_token_ref,
        QName(ns.WSSE, "Reference"),
        {
            "ValueType": "http://docs.oasis-open.org/wss/2004/01/" +
            "oasis-200401-wss-x509-token-profile-1.0#X509v3"
        },
    )
    bintok = etree.Element(
        QName(ns.WSSE, "BinarySecurityToken"),
        {
            "ValueType": "http://docs.oasis-open.org/wss/2004/01/" +
            "oasis-200401-wss-x509-token-profile-1.0#X509v3",
            "EncodingType": "http://docs.oasis-open.org/wss/2004/01/" +
            "oasis-200401-wss-soap-message-security-1.0#Base64Binary",
        },
    )
    ref.attrib["URI"] = "#" + ensure_id(bintok)
    bintok.text = x509_data.find(QName(ns.DS, "X509Certificate")).text
    security.insert(1, bintok)
    x509_data.getparent().remove(x509_data)


def verify_envelope(envelope, certfile):
    """Verify WS-Security signature on given SOAP envelope with given cert.

    Expects a document like that found in the sample XML in the ``sign()``
    docstring.

    Raise SignatureVerificationFailed on failure, silent on success.

    """
    key = _make_verify_key(certfile)
    return _verify_envelope_with_key(envelope, key)


def xp(node, xpath, namespaces):
    """Utility to do xpath search with namespaces."""
    # lxml.etree => node.xpath(xpath, namespaces=self.namespaces)
    return node.findall(xpath, namespaces=namespaces)


def _verify_envelope_with_key(envelope, key):
    soap_env = detect_soap_env(envelope)

    header = envelope.find(QName(soap_env, "Header"))
    if header == None:
        return Error("SignatureVerificationFailed").unwrap()

    security = header.find(QName(ns.WSSE, "Security"))
    signature = security.find(QName(ns.DS, "Signature"))

    ctx = _SignatureContext()

    # Find each signed element and register its ID with the signing context.
    refs = xp(signature, "ds:SignedInfo/ds:Reference", namespaces={"ds": ns.DS})
    for ref in refs:
        print(ref)
        # Get the reference URI and cut off the initial '#'
        referenced_id = ref.get("URI")[1:]
        referenced = xp(
            envelope,
            "//*[@wsu:Id='%s']" % referenced_id,
            namespaces={"wsu": ns.WSU}
        )[0]
        ctx.register_id(referenced, "Id", ns.WSU)

    ctx.key = key

    xmlsec_Error = Result.Ok(ctx.verify).map(lambda x: x(signature))
    # Sadly xmlsec gives us no details about the reason for the failure, so
    # we have nothing to pass on except that verification failed.
    if xmlsec_Error.is_err:
        fail("SignatureVerificationFailed()")


def _sign_node(ctx, signature, target, digest_method=None):
    """Add sig for ``target`` in ``signature`` node, using ``ctx`` context.

    Doesn't actually perform the signing; ``ctx.sign(signature)`` should be
    called later to do that.

    Adds a Reference node to the signature with URI attribute pointing to the
    target node, and registers the target node's ID so XMLSec will be able to
    find the target node by ID when it signs.

    """

    # Ensure the target node has a wsu:Id attribute and get its value.
    node_id = ensure_id(target)

    # Unlike HTML, XML doesn't have a single standardized Id. WSSE suggests the
    # use of the wsu:Id attribute for this purpose, but XMLSec doesn't
    # understand that natively. So for XMLSec to be able to find the referenced
    # node by id, we have to tell xmlsec about it using the register_id method.
    ctx.register_id(target, "Id", ns.WSU)

    # Add reference to signature with URI attribute pointing to that ID.
    ref = template.add_reference(
        signature, digest_method or Transform.SHA1, uri="#" + node_id
    )
    # This is an XML normalization transform which will be performed on the
    # target node contents before signing. This ensures that changes to
    # irrelevant whitespace, attribute ordering, etc won't invalidate the
    # signature.
    template.add_transform(ref, Transform.EXCL_C14N)


xmlsec = larky.struct(
    ns=ns,
)