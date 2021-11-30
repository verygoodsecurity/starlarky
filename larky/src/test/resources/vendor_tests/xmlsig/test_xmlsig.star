load("@stdlib//builtins", "builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io/StringIO", "StringIO")
load("@stdlib//larky", "larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", "types")
load("@stdlib//unittest", "unittest")
load("@stdlib//xml/etree/ElementTree", QName="QName", ElementTree="ElementTree")

load("@vendor//asserts", "asserts")
load("@vendor//cryptography/hazmat/backends", default_backend="default_backend")
load("@vendor//cryptography/hazmat/primitives", serialization="serialization")
load("@vendor//cryptography/hazmat/primitives/serialization", serialization="serialization")
load("@vendor//cryptography/x509", load_pem_x509_certificate="load_pem_x509_certificate")
load("@vendor//elementtree/AdvancedXMLTreeBuilder", AdvancedXMLTreeBuilder="AdvancedXMLTreeBuilder")
load("@vendor//elementtree/ElementC14N", ElementC14N="ElementC14N")
load("@vendor//_etreeplus/C14NParser", C14NParser="C14NParser")
load("@vendor//_etreeplus/xmlwriter", xmlwriter="xmlwriter")
load("@vendor//_etreeplus/xmltreenode", XMLTreeNode="XMLTreeNode")
load("@vendor//_etreeplus/xmltree", xmltree="xmltree")


load("@vendor//xmlsig", xmlsig="xmlsig")

# TEST START
load("./base", load_xml="load_xml", compare="compare")
load("./data/sign1_in_xml", SIGN1_IN_XML="SIGN1_IN_XML")
load("./data/sign1_out_xml", SIGN1_OUT_XML="SIGN1_OUT_XML")
load("./data/rsacert_pem", RSACERT_PEM="RSACERT_PEM")
load("./data/rsakey_pem", RSAKEY_PEM="RSAKEY_PEM")
load("./data/rsapub_pem", RSAPUB_PEM="RSAPUB_PEM")


namespaces = {
    'soap-env': xmlsig.ns.SOAP_ENV_11,
    'wsse': xmlsig.ns.WSSE,
    'ds': xmlsig.ns.DS,
    'ns0': "http://schemas.mastercard.com.chssecure/2011/01",
    "ns1": "http://schemas.datacontract.org/2004/07/CHSSecureBusinessServices.Request",
}


def find_node_by_attribute(nodes, attribute, value):
    """Search among nodes by attribute.
    Args:
        nodes: list of nodes
        attribute: attribute name
        value: attribute value
    Returns:
        First matched node
    Raises:
        IndexError if unable to find the node
    """
    # query = "//*[@*[local-name() = '{}' ] = '{}']"
    # node = reference.getroottree()
    return [
        node for node in nodes if node.get(attribute) == value
    ][0]


def test_xmlsig_sign_case1():
    parser = AdvancedXMLTreeBuilder.TreeBuilder(
        element_factory=XMLTreeNode.XMLNode,
        capture_event_queue=True,
        doctype_factory=XMLTreeNode.DocumentType,
        document_factory=XMLTreeNode.Document,
        comment_factory=XMLTreeNode.Comment,
        insert_comments=True,
        insert_pis=True,
    )
    root = load_xml(SIGN1_IN_XML, parser=parser)
    tree = xmltree.XMLTree(root)
    # print(xmltree.tostring(tree, method='c14n2')) # <- works!
    # print(xmltree.tostringC14N(root, True, True, False))
    print(xmltree.tostring(tree))
    # print(xmltree.tostring(tree, method='c14n'))
    sio = StringIO()
    qnames, namespaces = ElementTree._namespaces(root, None)
    ElementC14N._serialize_c14n(write=sio.write, elem=root, encoding='utf-8', qnames=qnames, namespaces=namespaces)
    print("C14N 1.0", "\n", sio.getvalue())
    print("--" * 100)
    print(xmltree.tostring(tree))

    # writer0 = xmlwriter.XMLWriter(xmltree.XMLTree(root))
    # print(writer0())

    # tree = ElementC14N.parse(StringIO(SIGN1_IN_XML), parser)
    # ElementC14N.write(tree, sio, exclusive=True)
    # print("C14N 1.0", "\n", sio.getvalue())
    # print("--" * 100)
    # serializer = xmlwriter.XMLSerializer(sio)
    # serializer.serialize(root)
    # print("serializer", "\n", sio.getvalue())
    print("--" * 100)
    # ----
    sio = StringIO()
    parser = C14NParser.C14nCanonicalizer(ElementTree.C14NWriterTarget, element_factory=sio.write)
    ElementTree.canonicalize(SIGN1_IN_XML, out=sio, parser=parser)
    print("C14N 2.0", "\n", sio.getvalue())
    # ----
    # writer0 = xmlwriter.XMLWriter(ElementTree.ElementTree(root))
    # print(writer0())
    # return node.findall(xpath, namespaces=namespaces)
    # print(ElementTree.tostring(
    #     root,
    #     method="xml",
    #     encoding="utf-8",
    #     xml_declaration=True,
    #     pretty_print=True,
    # ))
    # replace this:
    # sign = xmlsec.tree.find_node(root, consts.NodeSignature)
    # with this:
    sign = root.find(
        ".//ds:" + xmlsig.constants.NodeSignature,
        namespaces={"ds": xmlsig.constants.DSigNs}
    )
    # or:
    # qname = QName(xmlsig.constants.DSigNs, xmlsig.constants.NodeSignature)
    # sign = root.find(qname)
    # print(sign)
    asserts.assert_that(sign).is_not_none()
    ctx = xmlsig.SignatureContext()
    loaded = serialization.load_pem_private_key(RSAKEY_PEM, password=None)
    # print("loaded: ", loaded)
    asserts.assert_that(loaded).is_not_none()
    ctx.private_key = loaded
    ctx.key_name = "rsakey.pem"
    asserts.assert_that(ctx.key_name).is_equal_to("rsakey.pem")
    ctx.sign(sign)
    print(xmltree.tostring(tree))
    # writer0 = xmlwriter.XMLWriter(ElementTree.ElementTree(root))
    # signed = writer0(
    #     method="xml",
    #     encoding="utf-8",
    #     xml_declaration=True,
    #     pretty_print=True,
    # )
    # signed = ElementTree.tostring(
    #    root,
    #    method="xml",
    #    encoding="utf-8",
    #    xml_declaration=True,
    #    pretty_print=True,
    # )
    # print(signed)
    # ctx.verify(sign)
    # compare("sign1-out.xml", root)
    # self.assertIsNotNone(ctx.key)
    # ctx.key.name = 'rsakey.pem'
    # self.assertEqual("rsakey.pem", ctx.key.name)
    #
    # ctx.sign(sign)
    # self.assertEqual(self.load_xml("sign1-out.xml"), root)
    #
    # # Create a signature template for RSA-SHA1 enveloped signature.
    # sign = xmlsig.template.create(
    #     c14n_method=xmlsig.constants.TransformExclC14N,
    #     sign_method=xmlsig.constants.TransformRsaSha1,
    # )
    # asserts.assert_that(sign).is_not_none()
    #
    # # Add the <ds:Signature/> node to the document.
    # envelope.append(sign)
    #
    # # Add the <ds:Reference/> node to the signature template.
    # ref = xmlsig.template.add_reference(sign, xmlsig.constants.TransformSha1)
    #
    # # Add the enveloped transform descriptor.
    # xmlsig.template.add_transform(ref, xmlsig.constants.TransformEnveloped)
    #
    # # Add the <ds:KeyInfo/> and <ds:KeyName/> nodes.
    # key_info = xmlsig.template.ensure_key_info(sign)
    # x509_data = xmlsig.template.add_x509_data(key_info)
    # xmlsig.template.x509_data_add_issuer_serial(x509_data)
    # xmlsig.template.x509_data_add_certificate(x509_data)
    # ctx = xmlsig.SignatureContext()
    # loaded = pkcs12.load_key_and_certificates(FIXTURE, HttpHeader['X-Keystore-Pass'])
    # print("pkcs12.load_key_and_certificates: ", loaded)
    # ctx.load_pkcs12(loaded)
    # # Sign the template.
    # ctx.sign(sign)
    # signed = ElementTree.tostring(
    #                    envelope,
    #                    method="xml",
    #                    encoding="utf-8",
    #                    xml_declaration=True,
    #                    pretty_print=True,
    # )
    # print(signed)
    # plugin = BinarySignature(
    #     key_file=KEY_FILE_PW_STR,
    #     certfile=KEY_FILE_PW_STR,
    #     password=HttpHeader['X-Keystore-Pass'],
    #     signature_method=constants.TransformRsaSha512,
    #     digest_method=constants.TransformSha512,
    # )
    # envelope, headers = plugin.apply(envelope, {})
    # bintok = xp(envelope,
    #         "./soap-env:Header/wsse:Security/wsse:BinarySecurityToken",
    #         ns=namespaces,
    # )[0]
    # # element_or_tree,
    # #   encoding=None,
    # #   method="xml",
    # #   xml_declaration=None,
    # #   pretty_print=False,
    # #   with_tail=True,
    # #   standalone=None,
    # #   doctype=None,
    # # exclusive=
    # signed = ElementTree.tostring(
    #                 envelope,
    #                 method="xml",
    #                 encoding="utf-8",
    #                 xml_declaration=True,
    #                 pretty_print=True,
    # )
    # print(signed)
    # # doc = ElementTree.fromstring(signed)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_xmlsig_sign_case1))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
