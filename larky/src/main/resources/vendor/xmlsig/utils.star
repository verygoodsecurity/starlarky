load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//codecs", codecs="codecs")

load("@vendor//Crypto/Util/number",
     _long_to_bytes="long_to_bytes",
     _bytes_to_long="bytes_to_long")
load("@vendor//cryptography/x509", oid="oid")
load("@vendor//cryptography/x509/name", name="name")
load("@vendor//lxml/etree", etree="etree")
load("@vendor//option/result", Result="Result", Error="Error")

load("@vendor//xmlsig/ns", ns="ns")


_NAMEOID_TO_NAME = name._NAMEOID_TO_NAME
_escape_dn_value = name._escape_dn_value


b64_intro = 64
long_to_bytes = _long_to_bytes
bytes_to_long = _bytes_to_long


def b64_print(s):
    """
    Prints a string with spaces at every b64_intro characters
    :param s: String to print
    :return: String
    """
    string = s
    if types.is_string(s):
        string = codecs.encode(s, encoding="utf-8")
    if not types.is_bytelike(string):
        fail("expected bytelike object, not %s", type(string))
    r = []
    for pos in range(0, len(string), b64_intro):
        r.append(string[pos : pos + b64_intro])  # noqa: E203

    return b"\n".join(r)


def os2ip(arr):
    x_len = len(arr)
    x = 0
    for i in range(x_len):
        val = arr[i]
        x = x + (val * pow(256, x_len - i - 1))
    return x


def create_node(name, parent=None, ns="", tail=False, text=False):
    """
    Creates a new node
    :param name: Node name
    :param parent: Node parent
    :param ns: Namespace to use
    :param tail: Tail to add
    :param text: Text of the node
    :return: New node
    """
    node = etree.Element(etree.QName(ns, name))
    if parent != None:
        parent.append(node)
    if tail:
        node.tail = tail
    if text:
        node.text = text
    return node


def get_rdns_name(rdns):
    """
    Gets the rdns String name
    :param rdns: RDNS object
    :type rdns: cryptography.x509.RelativeDistinguishedName
    :return: RDNS name
    """
    data = []
    XMLSIG_NAMEOID_TO_NAME = dict(**_NAMEOID_TO_NAME)
    XMLSIG_NAMEOID_TO_NAME[name.NameOID.SERIAL_NUMBER] = "SERIALNUMBER"
    for dn in rdns:
        dn_data = []
        for attribute in dn._attributes:
            key = XMLSIG_NAMEOID_TO_NAME.get(
                attribute.oid, "OID.%s" % attribute.oid.dotted_string
            )
            dn_data.insert(0, "{}={}".format(key, _escape_dn_value(attribute.value)))
        data.insert(0, "+".join(dn_data))
    return ", ".join(data)