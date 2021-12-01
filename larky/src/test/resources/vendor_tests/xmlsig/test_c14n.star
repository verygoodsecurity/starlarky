load("@stdlib//builtins", "builtins")
load("@stdlib//base64", base64="base64")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io", io="io")
load("@stdlib//larky", "larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", "types")
load("@stdlib//unittest", "unittest")
load("@stdlib//xml/etree/ElementTree", QName="QName", ElementTree="ElementTree")

load("@vendor//asserts", "asserts")
load("@vendor//elementtree/AdvancedXMLTreeBuilder", AdvancedXMLTreeBuilder="AdvancedXMLTreeBuilder")
load("@vendor//elementtree/ElementC14N", ElementC14N="ElementC14N")
load("@vendor//_etreeplus/C14NParser", C14NParser="C14NParser")
load("@vendor//_etreeplus/xmlwriter", xmlwriter="xmlwriter")
load("@vendor//_etreeplus/xmltreenode", etree="XMLTreeNode")
load("@vendor//_etreeplus/xmltree", xmltree="xmltree")


# TEST START
load("./base", load_xml="load_xml")

eg1 = """<?xml version="1.0"?>

<?xml-stylesheet   href="doc.xsl"
   type="text/xsl"   ?>

<!DOCTYPE doc SYSTEM "doc.dtd">

<doc>Hello, world!<!-- Comment 1
--></doc>

<?pi-without-data     ?>

<!-- Comment 2 -->

<!-- Comment 3 -->
"""

eg2 = """<doc>
   <clean>   </clean>
   <dirty>   A   B   </dirty>
   <mixed>
      A
      <clean>   </clean>
      B
      <dirty>   A   B   </dirty>
      C
   </mixed>
</doc>
"""

eg3 = """<!DOCTYPE doc [<!ATTLIST e9 attr CDATA "default">]>
<doc xmlns:foo="http://www.bar.org">
   <e1   />
   <e2   ></e2>
   <e3    name = "elem3"   id="elem3"    />
   <e4    name="elem4"   id="elem4"    ></e4>
   <e5 a:attr="out" b:attr="sorted" attr2="all" attr="I'm"
       xmlns:b="http://www.ietf.org"
       xmlns:a="http://www.w3.org"
       xmlns="http://example.org"/>
   <e6 xmlns="" xmlns:a="http://www.w3.org">
       <e7 xmlns="http://www.ietf.org">
           <e8 xmlns="" xmlns:a="http://www.w3.org" a:foo="bar">
               <e9 xmlns="" xmlns:a="http://www.ietf.org"/>
           </e8>
       </e7>
   </e6>
</doc>
"""

eg4 = """<!DOCTYPE doc [ <!ATTLIST normId id ID #IMPLIED> <!ATTLIST normNames attr NMTOKENS #IMPLIED> ]> <doc>
   <text>First line&#x0d;&#10;Second line</text>
   <value>&#x32;</value>
   <compute><![CDATA[value>"0" && value<"10" ?"valid":"error"]]></compute>
   <compute expr='value>"0" &amp;&amp; value&lt;"10" ?"valid":"error"'>valid</compute>
   <norm attr=' &apos;   &#x20;&#13;&#xa;&#9;   &apos; '/>
   <normNames attr='   A   &#x20;&#13;&#xa;&#9;   B   '/>
   <normId id=' &apos;   &#x20;&#13;&#xa;&#9;   &apos; '/>
</doc>"""

eg5 = """<!DOCTYPE doc [
<!ATTLIST doc attrExtEnt ENTITY #IMPLIED>
<!ENTITY ent1 "Hello">
<!ENTITY ent2 SYSTEM "world.txt">
<!ENTITY entExt SYSTEM "earth.gif" NDATA gif>
<!NOTATION gif SYSTEM "viewgif.exe">
]>
<doc attrExtEnt="entExt">
   &ent1;, &ent2;!
</doc>

<!-- Let world.txt contain "world" (excluding the quotes) -->
"""

eg6 = """<?xml version="1.0" encoding="ISO-8859-1"?>
<doc>&#169;</doc>"""

eg7 = """<!DOCTYPE doc [
<!ATTLIST e2 xml:space (default|preserve) 'preserve'>
<!ATTLIST e3 id ID #IMPLIED>
]>
<doc xmlns="http://www.ietf.org" xmlns:w3c="http://www.w3.org">
   <e1>
      <e2 xmlns="">
         <e3 id="E3"/>
      </e2>
   </e1>
</doc>"""

examples = [eg1, eg2, eg3, eg4, eg5, eg6, eg7]

test_results = {
    eg1: '''PD94bWwtc3R5bGVzaGVldCBocmVmPSJkb2MueHNsIgogICB0eXBlPSJ0ZXh0L3hz
    bCIgICA/Pgo8ZG9jPkhlbGxvLCB3b3JsZCE8IS0tIENvbW1lbnQgMQotLT48L2Rv
    Yz4KPD9waS13aXRob3V0LWRhdGE/Pgo8IS0tIENvbW1lbnQgMiAtLT4KPCEtLSBD
    b21tZW50IDMgLS0+''',

    eg2: '''PGRvYz4KICAgPGNsZWFuPiAgIDwvY2xlYW4+CiAgIDxkaXJ0eT4gICBBICAgQiAg
    IDwvZGlydHk+CiAgIDxtaXhlZD4KICAgICAgQQogICAgICA8Y2xlYW4+ICAgPC9j
    bGVhbj4KICAgICAgQgogICAgICA8ZGlydHk+ICAgQSAgIEIgICA8L2RpcnR5Pgog
    ICAgICBDCiAgIDwvbWl4ZWQ+CjwvZG9jPg==''',

    eg3: '''PGRvYyB4bWxuczpmb289Imh0dHA6Ly93d3cuYmFyLm9yZyI+CiAgIDxlMT48L2Ux
    PgogICA8ZTI+PC9lMj4KICAgPGUzIGlkPSJlbGVtMyIgbmFtZT0iZWxlbTMiPjwv
    ZTM+CiAgIDxlNCBpZD0iZWxlbTQiIG5hbWU9ImVsZW00Ij48L2U0PgogICA8ZTUg
    eG1sbnM9Imh0dHA6Ly9leGFtcGxlLm9yZyIgeG1sbnM6YT0iaHR0cDovL3d3dy53
    My5vcmciIHhtbG5zOmI9Imh0dHA6Ly93d3cuaWV0Zi5vcmciIGF0dHI9IkknbSIg
    YXR0cjI9ImFsbCIgYjphdHRyPSJzb3J0ZWQiIGE6YXR0cj0ib3V0Ij48L2U1Pgog
    ICA8ZTYgeG1sbnM6YT0iaHR0cDovL3d3dy53My5vcmciPgogICAgICAgPGU3IHht
    bG5zPSJodHRwOi8vd3d3LmlldGYub3JnIj4KICAgICAgICAgICA8ZTggeG1sbnM9
    IiIgYTpmb289ImJhciI+CiAgICAgICAgICAgICAgIDxlOSB4bWxuczphPSJodHRw
    Oi8vd3d3LmlldGYub3JnIiBhdHRyPSJkZWZhdWx0Ij48L2U5PgogICAgICAgICAg
    IDwvZTg+CiAgICAgICA8L2U3PgogICA8L2U2Pgo8L2RvYz4=''',

    eg4: '''PGRvYz4KICAgPHRleHQ+Rmlyc3QgbGluZSYjeEQ7ClNlY29uZCBsaW5lPC90ZXh0
    PgogICA8dmFsdWU+MjwvdmFsdWU+CiAgIDxjb21wdXRlPnZhbHVlJmd0OyIwIiAm
    YW1wOyZhbXA7IHZhbHVlJmx0OyIxMCIgPyJ2YWxpZCI6ImVycm9yIjwvY29tcHV0
    ZT4KICAgPGNvbXB1dGUgZXhwcj0idmFsdWU+JnF1b3Q7MCZxdW90OyAmYW1wOyZh
    bXA7IHZhbHVlJmx0OyZxdW90OzEwJnF1b3Q7ID8mcXVvdDt2YWxpZCZxdW90Ozom
    cXVvdDtlcnJvciZxdW90OyI+dmFsaWQ8L2NvbXB1dGU+CiAgIDxub3JtIGF0dHI9
    IiAnICAgICYjeEQmI3hBJiN4OSAgICcgIj48L25vcm0+CiAgIDxub3JtTmFtZXMg
    YXR0cj0iQSAmI3hEJiN4QSYjeDkgQiI+PC9ub3JtTmFtZXM+CiAgIDxub3JtSWQg
    aWQ9IicgJiN4RCYjeEEmI3g5ICciPjwvbm9ybUlkPgo8L2RvYz4=''',

    eg5: '''PGRvYyBhdHRyRXh0RW50PSJlbnRFeHQiPgogICBIZWxsbywgd29ybGQhCjwvZG9j
    Pg==''',

    eg6: '''PGRvYz7CqTwvZG9jPg==''',

    eg7: '''PGRvYyB4bWxucz0iaHR0cDovL3d3dy5pZXRmLm9yZyIgeG1sbnM6dzNjPSJodHRw
    Oi8vd3d3LnczLm9yZyI+CiAgIDxlMT4KICAgICAgPGUyIHhtbG5zPSIiIHhtbDpz
    cGFjZT0icHJlc2VydmUiPgogICAgICAgICA8ZTMgaWQ9IkUzIj48L2UzPgogICAg
    ICA8L2UyPgogICA8L2UxPgo8L2RvYz4=''',

}


def test_c14n_1():
    parser = AdvancedXMLTreeBuilder.TreeBuilder(
        element_factory=etree.XMLNode,
        capture_event_queue=True,
        doctype_factory=etree.DocumentType,
        document_factory=etree.Document,
        comment_factory=etree.Comment,
        pi_factory=etree.ProcessingInstruction,
        insert_comments=True,
        insert_pis=True,
    )
    root = load_xml(eg1, parser=parser)
    tree = xmltree.XMLTree(root)
    # print(xmltree.tostring(tree))
    print(xmltree.tostring(tree, method='c14n'))
    print("--" * 50)
    print(base64.b64decode((test_results[eg1])))


def test_c14n_2():
    tree = etree.parse(io.StringIO(eg1))

    # builder = XMLTreeNode.TreeBuilder(debug=True)
    # tree = builder.fromstring(eg1)
    # In [30]: print(etree.tostring(z).decode('utf-8'))
    expected = ('<?xml-stylesheet href="doc.xsl"\n   type="text/xsl"   ?>' +
                     '<!DOCTYPE doc SYSTEM "doc.dtd">\n<doc>Hello, world!' +
                     '<!-- Comment 1\n--></doc><?pi-without-data?>' +
                     '<!-- Comment 2 --><!-- Comment 3 -->')
    actual = etree.tostring(tree)
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)
    # print("--" * 50)
    actual = etree.tostring(tree, method='c14n')
    expected = base64.b64decode((test_results[eg1])).decode('utf-8')
    # print(repr(actual))
    # print(repr(expected))
    asserts.assert_that(actual).is_equal_to(expected)



def test_xmlsig_sign_case1():
    parser = AdvancedXMLTreeBuilder.TreeBuilder(
        element_factory=etree.XMLNode,
        capture_event_queue=True,
        doctype_factory=etree.DocumentType,
        document_factory=etree.Document,
        comment_factory=etree.Comment,
        insert_comments=True,
        insert_pis=True,
    )
    root = load_xml(eg1, parser=parser)
    tree = xmltree.XMLTree(root)
    # print(xmltree.tostring(tree, method='c14n2')) # <- works!
    # print(xmltree.tostringC14N(root, True, True, False))
    print(xmltree.tostring(tree))
    # print(xmltree.tostring(tree, method='c14n'))
    sio = io.StringIO()
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
    sio = io.StringIO()
    parser = C14NParser.C14nCanonicalizer(ElementTree.C14NWriterTarget, element_factory=sio.write)
    ElementTree.canonicalize(eg1, out=sio, parser=parser)
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
    # _suite.addTest(unittest.FunctionTestCase(test_c14n_1))
    _suite.addTest(unittest.FunctionTestCase(test_c14n_2))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
