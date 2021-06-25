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
    
    parser = SimpleXMLTreeBuilder.TreeBuilder()
    tree = parse(
        '<doc level="A"><one updated="Y">One</one><two updated="N">Two</two>hm<three>Three</three></doc>',
        parser
    )
    root = tree.getroot()
    # test top-level elements and children
    asserts.eq(1, len(root.findall(".")))
    asserts.eq(3, len(root.findall("./")))
    asserts.eq(3, len(root.findall("./*")))
    # test basic expression with tag and text
    asserts.eq('one', root.findall("./one")[0].tag)
    asserts.eq('One', root.findall("./one")[0].text)
    # test attrib
    asserts.eq('A', root.findall(".")[0].attrib['level'])
    asserts.eq('Y', root.findall("./")[0].attrib['updated'])
    asserts.eq('N', root.findall("./two")[0].attrib['updated'])


    tree = parse(
        '<data><actress name="Jenny"><tv>5</tv><born>1989</born></actress><actor name="Tim"><tv updated="Yes">3</tv><born>1990</born></actor><actor name="John"><film>8</film><born>1984</born></actor></data>',
        parser
    )
    root = tree.getroot()
    # test grand-children
    asserts.eq(4, len(root.findall("./actor/*")))
    asserts.eq('5', root.findall("./actress/tv")[0].text)
    # test all descendant
    asserts.eq(9, len(root.findall(".//")))
    asserts.eq(2, len(root.findall(".//tv")))
    asserts.eq(4, len(root.findall("./actor//")))
    asserts.eq(0, len(root.findall("./actress//film")))
    # test children in certain position
    asserts.eq(0, len(root.findall(".//tv[2]")))
    asserts.eq(2, len(root.findall(".//tv[1]")))
    asserts.eq(1, len(root.findall("./actress[1]")))
    asserts.eq('1984', root.findall("./actor[2]/born")[0].text)
    asserts.eq('8', root.findall("./actor/film[1]")[0].text)
    test search by attrib
    asserts.eq('3', root.findall("./actor/*[@updated='Yes']")[0].text)
    asserts.eq('actress', root.findall(".//*[@name='Jenny']")[0].tag)


def _test_update_and_serialize():
    parser = SimpleXMLTreeBuilder.TreeBuilder()
    data = ''.join(['<data><teacher name="Jenny"><born>1983</born></teacher>',
    '<student name="Tim"><performance><Grade>A+</Grade></performance><info><born>2005</born></info></student>',
    '<student name="John"><performance><Grade>B</Grade></performance><info><born>2004</born></info></student></data>'])

    tree = parse(data, parser)
    root = tree.getroot()

    # test update node text
    root.findall(".//*[@name='John']/performance/Grade")[0].text = 'A-'
    
    # order of nodes on the same level is reversed but vertical nested order is correct
    expected_xml = ''.join(['<data><student name="John"><info><born>2004</born></info><performance><Grade>A-</Grade></performance></student>',
    '<student name="Tim"><info><born>2005</born></info><performance><Grade>A+</Grade></performance></student>',
    '<teacher name="Jenny"><born>1983</born></teacher></data>'])

    asserts.eq(expected_xml, ElementTree.tostring(root))


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_elementtree))
    _suite.addTest(unittest.FunctionTestCase(_test_xpath))
    _suite.addTest(unittest.FunctionTestCase(_test_update_and_serialize))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
