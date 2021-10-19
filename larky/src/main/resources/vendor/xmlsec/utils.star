load("@stdlib//xml/etree/ElementTree", etree="ElementTree")
load("@vendor//xmlsec/ns", ns="ns")


def detect_soap_env(envelope):
    root_tag = etree.QName(envelope)
    return root_tag.namespace


def get_or_create_header(envelope):
    soap_env = detect_soap_env(envelope)

    # look for the Header element and create it if not found
    header_qname = "{%s}Header" % soap_env
    header = envelope.find(header_qname)
    if header == None:
        header = etree.Element(header_qname)
        envelope.insert(0, header)
    return header