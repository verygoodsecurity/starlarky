
load("@stdlib//builtins", "builtins")
load("@stdlib//io/StringIO", "StringIO")
load("@stdlib//larky", "larky")
load("@stdlib//types", "types")
load("@stdlib//operator", operator="operator")
load("@stdlib//unittest", "unittest")
load("@stdlib//xml/etree/ElementTree", ElementTree="ElementTree")
load("@vendor//asserts", "asserts")
load("@vendor//elementtree/SimpleXMLTreeBuilder", SimpleXMLTreeBuilder="SimpleXMLTreeBuilder")


def _bytes(s):
    return builtins.bytes(s, encoding='utf-8')


def parse(text, parser=None):
    #f = BytesIO(text) if isinstance(text, bytes) else StringIO(text)
    f = StringIO(text)
    return ElementTree.parse(f, parser=parser)


def _rootstring(self, tree):
    return ElementTree.tostring(
        tree.getroot())\
        .replace(_bytes(' '), _bytes(''))\
        .replace(_bytes('\n'), _bytes(''))


def _test_elementtree():
    f = StringIO(
        '<doc><one>One</one><two>Two</two>hm<three>Three</three></doc>'
    )
    parser = SimpleXMLTreeBuilder.TreeBuilder()
    # doc = ElementTree.ElementTree(file=f)
    doc = parse(
        '<doc><one>One</one><two>Two</two>hm<three>Three</three></doc>',
        parser
    )
    # doc = parse("""<root>
    #          <element key='value'>text</element>
    #          <element>text</element>tail
    #          <empty-element/>
    #       </root>""", parser
    # )
    root = doc.getroot()
    asserts.eq(3, operator.length_hint(root))
    asserts.eq('one', operator.getitem(root, 0).tag)
    asserts.eq('two', operator.getitem(root, 1).tag)
    asserts.eq('three', operator.getitem(root, 2).tag)

def _test_xpath():
    
    # doc = ElementTree.ElementTree(file=f)
    # root = ElementTree.fromstring('<doc><one>One</one><two>Two</two>hm<three>Three</three></doc>')
    # print('xpath:', root.findall('.'))
    parser = SimpleXMLTreeBuilder.TreeBuilder()

    tree = parse(
        '<doc><one>One</one><two>Two</two>hm<three>Three</three></doc>',
        parser
    )
    root = tree.getroot()
    print('xpath:', root.findall("./one")[0])
    asserts.eq('one', root.findall("./one")[0].tag)
    asserts.eq('One', root.findall("./one")[0].text)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_elementtree))
    _suite.addTest(unittest.FunctionTestCase(_test_xpath))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
