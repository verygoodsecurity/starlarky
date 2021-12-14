load("@stdlib//builtins", builtins="builtins")
load("@stdlib//io", io="io")
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//re", re="re")
load("@stdlib//sets", sets="sets")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
#
# load("@stdlib//xml/etree/ElementTree", ElementTree="ElementTree")
# load("@vendor//elementtree/SimpleXMLTreeBuilder", SimpleXMLTreeBuilder="SimpleXMLTreeBuilder")
load("@vendor//lxml/etree", etree="etree")
#
# def _bytes(s):
#     return builtins.bytes(s, encoding='utf-8')

#
# def parse(text, parser=None, tree_factory=etree.ElementTree):
#     #f = BytesIO(text) if isinstance(text, bytes) else StringIO(text)
#     f = StringIO(text)
#     return ElementTree.parse(f, parser=parser, tree_factory=tree_factory)

def normalize_newlines(s):
    return re.sub(r'(\r?\n)+', '\n', s)


def normalize_ws(s):
    _s = normalize_newlines(s)
    return ''.join([l.strip() for l in _s.split('\n')])
    # return re.sub(r'(\s|\x0B)+', replace, s).strip()


def _test_ns_events():
    # https://github.com/lxml/lxml/blob/ea954da3c87bd8f6874f6bf4203e2ef5269ea383/src/lxml/tests/selftest.py#L458-L474
    simple_ns = """
<root xmlns='http://namespace/'>
   <element key='value'>text</element>
   <element>text</element>tail
   <empty-element/>
</root>    
    """
    data = normalize_ws(simple_ns)
    iterparse = etree.iterparse
    events = ("start", "end", "start-ns", "end-ns")
    context = iterparse(io.StringIO(data), events)
    actual_events = []
    for action, elem in context:
        if action in ("start", "end"):
            actual_events.append((action, elem.tag))
        else:
            actual_events.append((action, elem))

    expected = [
        ("start-ns", ("", "http://namespace/")),
        ("start", "{http://namespace/}root"),
        ("start", "{http://namespace/}element"),
        ("end", "{http://namespace/}element"),
        ("start", "{http://namespace/}element"),
        ("end", "{http://namespace/}element"),
        ("start", "{http://namespace/}empty-element"),
        ("end", "{http://namespace/}empty-element"),
        ("end", "{http://namespace/}root"),
        ("end-ns", None),
    ]

    for i, actual in enumerate(actual_events, start=0):
        expected_event, expected_payload = expected[i]
        actual_event, actual_payload = actual
        asserts.assert_that(expected_event).is_equal_to(actual_event)
        asserts.assert_that(expected_payload).is_equal_to(actual_payload)


# the below is not standard in python
#
# def _test_doctype_parser():
#     # https://github.com/lxml/lxml/blob/ea954da3c87bd8f6874f6bf4203e2ef5269ea383/src/lxml/tests/test_htmlparser.py#L475-L497
#     full_doctype = """
# <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN" "sys.dtd">
# <html><body></body></html>
#     """
#     # parser = AdvancedXMLTreeBuilder.TreeBuilder(
#     #     element_factory=XMLTreeNode.XMLNode,
#     #     capture_event_queue=True,
#     #     doctype_factory=XMLTreeNode.DocumentType,
#     #     document_factory=XMLTreeNode.Document,
#     #     comment_factory=XMLTreeNode.Comment,
#     #     insert_comments=True,
#     #     insert_pis=True,
#     # )
#     events = []
#     def Target():
#         self = larky.mutablestruct(__name__='Target', __class__=Target)
#         def doctype()
#         def start(self, tag, attrib):
#             events.append("start")
#             assertFalse(attrib)
#             assertEqual("TAG", tag)
#         def end(self, tag):
#             events.append("end")
#             assertEqual("TAG", tag)
#         def close(self):
#             return Element("DONE")
#
#     parser = etree.XMLparser(target=Target())
#     data = normalize_ws(full_doctype)
#     tree = parse(data, parser)
#
#     expected_events = [
#         ("doctype", ("html", "-//W3C//DTD HTML 4.01//EN", "sys.dtd")),
#         ("start", "html"), ("start", "body"),
#         ("end", "body"), ("end", "html")
#     ]
#     actual_events = list(parser.read_events())
#     asserts.assert_that(actual_events).is_equal_to(expected_events)
#
#     target_doctype_html = "<!DOCTYPE html><html><body></body></html>"
#     data = normalize_ws(target_doctype_html)
#     parse(data, parser)
#     expected_events = [
#         ("doctype", ("html", None, None)),
#         ("start", "html"), ("start", "body"),
#         ("end", "body"), ("end", "html")
#     ]
#     actual_events = list(parser.read_events())
#     asserts.assert_that(actual_events).is_equal_to(expected_events)


def _test_doctype_internal():
    # https://github.com/lxml/lxml/blob/f163e6395668e315c74489183070ce2ed3878e83/src/lxml/tests/test_dtd.py#L93-L103
    doctype_internal = """
    <!DOCTYPE b SYSTEM "none" [
    <!ELEMENT b (a)>
    <!ELEMENT a EMPTY>
    ]>
    <b><a/></b>
    """
    root = etree.XML(normalize_ws(doctype_internal))
    dtd = etree.ElementTree(root).docinfo.internalDTD
    asserts.assert_that(dtd).is_true()


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_ns_events))
    # _suite.addTest(unittest.FunctionTestCase(_test_doctype_parser))
    _suite.addTest(unittest.FunctionTestCase(_test_doctype_internal))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())