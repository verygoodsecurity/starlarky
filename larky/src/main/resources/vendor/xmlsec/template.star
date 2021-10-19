load("@stdlib//larky", larky="larky")
load("@stdlib//xml/etree/ElementTree", etree="ElementTree")
load("@vendor//xmlsec/constants", Transform="Transform")


Element = etree.Element


def add_encrypted_key(node, method, id=None, type=None, recipient=None):
    pass


def add_key_name(node, name):
    pass


def add_key_value(node):
    pass


def add_reference(node, digest_method, id=None, uri=None, type=None):
    pass


def add_transform(node, transform):
    pass


def add_x509_data(node):
    pass


def create(node, c14n_method, sign_method):
    pass


def encrypted_data_create(node, method, id=None, type=None, mime_type=None, encoding=None, ns=None):
    pass


def encrypted_data_ensure_cipher_value(node):
    pass


def encrypted_data_ensure_key_info(node, id=None, ns=None):
    pass


def ensure_key_info(node, id=None):
    pass


def transform_add_c14n_inclusive_namespaces(node, prefixes):
    pass


def x509_data_add_certificate(node):
    pass


def x509_data_add_crl(node):
    pass


def x509_data_add_issuer_serial(node):
    pass


def x509_data_add_ski(node):
    pass


def x509_data_add_subject_name(node):
    pass


def x509_issuer_serial_add_issuer_name(node, name=None):
    pass


def x509_issuer_serial_add_serial_number(node, serial=None):
    pass


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