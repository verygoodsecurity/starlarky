load("@stdlib//larky", larky="larky")
load("@stdlib//xml/etree/ElementTree", etree="ElementTree")
load("@vendor//xmlsec/constants", Transform="Transform", ID_ATTR="ID_ATTR")
load("@vendor//xmlsec/utils", create_node="create_node")
load("@vendor//xmlsec/ns", ns="ns")


Element = etree.Element

DSigNs = ns.DSigNs


def add_encrypted_key(node, method, id=None, type=None, recipient=None):
    pass


def add_key_name(node, name):
    node.text = "\n"
    key_name = create_node("KeyName", node, DSigNs, tail="\n")
    if name:
        key_name.text = name
    return key_name


def add_key_value(node):
    return create_node("KeyValue", node, DSigNs, tail="\n")


def add_reference(node, digest_method, name=False, uri=False, uri_type=False):
    reference = create_node(
        "Reference",
        node.find("{" + DSigNs + "}SignedInfo"),
        DSigNs,
        tail="\n",
        text="\n",
    )
    if name:
        reference.set(ID_ATTR, name)
    if uri == "":
        reference.set("URI", "")
    if uri:
        reference.set("URI", uri)
    if uri_type:
        reference.set("Type", uri_type)
    digest_method_node = create_node("DigestMethod", reference, DSigNs, tail="\n")
    digest_method_node.set("Algorithm", digest_method)
    create_node("DigestValue", reference, DSigNs, tail="\n")
    return reference


def add_transform(node, transform):
    transforms_node = node.find("ds:Transforms", namespaces=ns.NS_MAP)
    if transforms_node == None:
        transforms_node = create_node("Transforms", ns=DSigNs, tail="\n", text="\n")
        node.insert(0, transforms_node)
    transform_node = create_node("Transform", transforms_node, DSigNs, tail="\n")
    transform_node.set("Algorithm", transform)
    return transform_node


def add_x509_data(node):
    node.text = "\n"
    return create_node("X509Data", node, DSigNs, tail="\n")


def create(c14n_method=False, sign_method=False, name=False, ns="ds"):
    node = etree.Element(etree.QName(DSigNs, "Signature"), nsmap={DSigNs: ns})
    node.text = "\n"
    if name:
        node.set(ID_ATTR, name)
    signed_info = create_node("SignedInfo", node, DSigNs, tail="\n", text="\n")
    canonicalization = create_node(
        "CanonicalizationMethod", signed_info, DSigNs, tail="\n"
    )
    canonicalization.set("Algorithm", c14n_method)
    signature_method = create_node("SignatureMethod", signed_info, DSigNs, tail="\n")
    signature_method.set("Algorithm", sign_method)
    create_node("SignatureValue", node, DSigNs, tail="\n")
    return node


def encrypted_data_create(node, method, id=None, type=None, mime_type=None, encoding=None, ns=None):
    pass


def encrypted_data_ensure_cipher_value(node):
    pass


def encrypted_data_ensure_key_info(node, id=None, ns=None):
    pass


def ensure_key_info(node, name=False):
    key_info = node.find("{" + DSigNs + "}KeyInfo")
    if key_info == None:
        key_info = create_node("KeyInfo", ns=DSigNs, tail="\n")
        node.insert(2, key_info)
    if name:
        key_info.set(ID_ATTR, name)
    return key_info


def transform_add_c14n_inclusive_namespaces(node, prefixes):
    pass


def x509_data_add_certificate(node):
    node.text = "\n"
    return create_node("X509Certificate", node, DSigNs, tail="\n")


def x509_data_add_crl(node):
    node.text = "\n"
    return create_node("X509CRL", node, DSigNs, tail="\n")


def x509_data_add_issuer_serial(node):
    node.text = "\n"
    return create_node("X509IssuerSerial", node, DSigNs, tail="\n")


def x509_data_add_ski(node):
    node.text = "\n"
    return create_node("X509SKI", node, DSigNs, tail="\n")


def x509_data_add_subject_name(node):
    node.text = "\n"
    return create_node("X509SubjectName", node, DSigNs, tail="\n")


def x509_issuer_serial_add_issuer_name(node):
    node.text = "\n"
    return create_node("X509IssuerName", node, DSigNs, tail="\n")


def x509_issuer_serial_add_serial_number(node):
    node.text = "\n"
    return create_node("X509SerialNumber", node, DSigNs, tail="\n")


template = larky.struct(
    add_encrypted_key=add_encrypted_key,
    add_key_name=add_key_name,
    add_key_value=add_key_value,
    add_reference=add_reference,
    add_transform=add_transform,
    add_x509_data=add_x509_data,
    create=create,
    encrypted_data_create=encrypted_data_create,
    encrypted_data_ensure_cipher_value=encrypted_data_ensure_cipher_value,
    encrypted_data_ensure_key_info=encrypted_data_ensure_key_info,
    ensure_key_info=ensure_key_info,
    transform_add_c14n_inclusive_namespaces=transform_add_c14n_inclusive_namespaces,
    x509_data_add_certificate=x509_data_add_certificate,
    x509_data_add_crl=x509_data_add_crl,
    x509_data_add_issuer_serial=x509_data_add_issuer_serial,
    x509_data_add_ski=x509_data_add_ski,
    x509_data_add_subject_name=x509_data_add_subject_name,
    x509_issuer_serial_add_issuer_name=x509_issuer_serial_add_issuer_name,
    x509_issuer_serial_add_serial_number=x509_issuer_serial_add_serial_number
)