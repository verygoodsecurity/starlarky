load("@stdlib//larky", larky="larky")
load("@stdlib//io", io="io")
load("@stdlib//types", types="types")
load("@vendor//asserts", asserts="asserts")
load("@stdlib//xml/etree/ElementTree", etree="ElementTree")
load("@vendor//elementtree/SimpleXMLTreeBuilder", SimpleXMLTreeBuilder="SimpleXMLTreeBuilder")


def load_xml(xml, parser=SimpleXMLTreeBuilder.TreeBuilder(), tree_factory=etree.ElementTree):
    if types.is_bytelike(xml):
        xml = xml.decode('utf-8')
    f = io.StringIO(xml)
    return etree.parse(f, parser=parser, tree_factory=tree_factory).getroot()


def parse_xml(name, parser=None):
    return load_xml(name, parser=parser)


def xp(node, xpath, ns):
    """Utility to do xpath search with namespaces."""
    # lxml.etree => node.xpath(xpath, namespaces=self.namespaces)
    return node.findall(xpath, namespaces=ns)


def compare(name, result):
    # Parse the expected file.
    xml = parse_xml(name)

    # Stringify the root, <Envelope/> nodes of the two documents.
    expected_text = etree.tostring(xml, pretty_print=False)
    result_text = etree.tostring(result, pretty_print=False)
    # Compare the results.
    if expected_text != result_text:
        print("expected: ", expected_text, sep="\n")
        print("result: ", result_text, sep="\n")
    asserts.eq(expected_text, result_text)