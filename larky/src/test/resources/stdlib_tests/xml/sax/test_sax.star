load("@stdlib//builtins", "builtins")
load("@stdlib//larky", "larky")
load("@stdlib//types", "types")
load("@stdlib//io/StringIO", "StringIO")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//operator", operator="operator")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")

# test starts..
load("@stdlib//xml/sax", make_parser="make_parser")
load("@stdlib//xml/sax/handler", handler="handler")
load("@stdlib//xml/sax/_exceptions", SAXException="SAXException")

load("@stdlib//xml/sax/xmlreader", test="test")
load("@stdlib//xml/sax/drivers/drv_xmllib", drv_xmllib="drv_xmllib")

# test imports..
# load("@stdlib//xml/dom/domreg", getDOMImplementation="getDOMImplementation")
version= handler.version
ContentHandler = handler.ContentHandler


def TracingSaxHandler():
    self = larky.mutablestruct(__name__='TracingSaxHandler',
                               __class__=TracingSaxHandler)
    def __init__():
        self.visited = []
        return self
    self = __init__()

    # Error handler
    def fatalError(saxexc):
        saxexc.unwrap()
    self.fatalError = fatalError

    ## content handler #########################################################
    def setDocumentLocator(locator):
        self.visited.append(('setDocumentLocator', locator))
    self.setDocumentLocator = setDocumentLocator
    def startDocument():
        self.visited.append(('startDocument',))
    self.startDocument = startDocument
    def endDocument():
        self.visited.append(('endDocument',))
    self.endDocument = endDocument
    def startElement(name, attrs):
        self.visited.append(('startElement', name))
        for key, val in list(attrs.items()):
            self.visited.append(('attribute', key,  val))
    self.startElement = startElement
    def endElement (name):
        self.visited.append(('endElement', name))
    self.endElement = endElement
    def startElementNS(name, qname, attrs):
        self.visited.append(('startElementNS', name, qname))
        for key, val in list(attrs.items()):
            self.visited.append(('attribute', key,  val))
    self.startElementNS = startElementNS
    def endElementNS (name, qname):
        self.visited.append(('endElementNS', name, qname))
    self.endElementNS = endElementNS
    def startPrefixMapping(prefix, uri):
        self.visited.append(('startPrefixMapping', prefix, uri))
    self.startPrefixMapping = startPrefixMapping
    def endPrefixMapping(prefix):
        self.visited.append(('endPrefixMapping', prefix))
    self.endPrefixMapping = endPrefixMapping
    def processingInstruction(target, data):
        self.visited.append(('processingInstruction', target,  data))
    self.processingInstruction = processingInstruction
    def ignorableWhitespace(whitespace):
        self.visited.append(('ignorableWhitespace', whitespace))
    self.ignorableWhitespace = ignorableWhitespace
    def characters(ch):
        self.visited.append(('characters', repr(ch)))
    self.characters = characters

    ## lexical handler #########################################################
    def xmlDecl(version, encoding, standalone):
        self.visited.append(('xmlDecl', version, encoding, standalone))
    self.xmlDecl = xmlDecl
    def comment(machin):
        self.visited.append(('comment', repr(machin)))
    self.comment = comment
    def startEntity(name):
        self.visited.append(('startEntity', name))
    self.startEntity = startEntity
    def endEntity(name):
        self.visited.append(('endEntity', name))
    self.endEntity = endEntity
    def startCDATA():
        self.visited.append(('startCDATA',))
    self.startCDATA = startCDATA
    def endCDATA():
        self.visited.append(('endCDATA',))
    self.endCDATA = endCDATA
    def startDTD(name, public_id, system_id):
        self.visited.append(('startDTD', name, public_id, system_id))
    self.startDTD = startDTD
    def endDTD():
        self.visited.append(('endDTD',))
    self.endDTD = endDTD

    ## DTD decl handler ########################################################
    def attributeDecl(elem_name, attr_name, type, value_def, value):
        self.visited.append(('attributeDecl', elem_name, attr_name, type, value_def, value))
    self.attributeDecl = attributeDecl
    def elementDecl(elem_name, content_model):
        self.visited.append(('elementDecl', elem_name, content_model))
    self.elementDecl = elementDecl
    def internalEntityDecl(name, value):
        self.visited.append(('internalEntityDecl', name, codecs.encode(value, encoding='UTF-8')))
    self.internalEntityDecl = internalEntityDecl
    def externalEntityDecl(name, public_id, system_id):
        self.visited.append(('externalEntityDecl', name, public_id, system_id))
    self.externalEntityDecl = externalEntityDecl

    def notationDecl(name, publicId, systemId):
        self.visited.append(('notationDecl',name, publicId, systemId))
    self.notationDecl = notationDecl

    def unparsedEntityDecl(self, name, publicId, systemId, ndata):
        self.visited.append(('unparsedEntityDecl', name, publicId, systemId, ndata))
    self.unparsedEntityDecl = unparsedEntityDecl

    return self
#
#
# load("@stdlib//builtins", "builtins")
# load("@stdlib//base64", base64="base64")
# load("@stdlib//codecs", codecs="codecs")
# load("@stdlib//io/StringIO", "StringIO")
# load("@stdlib//larky", "larky")
# load("@stdlib//operator", operator="operator")
# load("@stdlib//types", "types")
# load("@stdlib//unittest", "unittest")
# load("@stdlib//xml/etree/ElementTree", QName="QName", ElementTree="ElementTree")
#
# load("@vendor//asserts", "asserts")
# load("@vendor//elementtree/SimpleXMLTreeBuilder", SimpleXMLTreeBuilder="SimpleXMLTreeBuilder")
# load("@vendor//elementtree/ElementC14N", ElementC14N="ElementC14N")
# load("@vendor//_etreeplus/C14NParser", C14NParser="C14NParser")
# load("@vendor//_etreeplus/xmlwriter", xmlwriter="xmlwriter")
# load("@vendor//_etreeplus/xmltreenode", XMLTreeNode="XMLTreeNode")
# load("@vendor//_etreeplus/xmltree", xmltree="xmltree")


# TEST START
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

def setup(saxHandler):
    parser = make_parser()
    parser.setProperty(handler.property_lexical_handler, saxHandler)
    parser.setContentHandler(saxHandler)
    parser.setDTDHandler(saxHandler)
    parser.setErrorHandler(saxHandler)
    return parser


def _test_sax_parser():
    saxHandler = TracingSaxHandler()
    parser = setup(saxHandler)
    parser.parse(StringIO('''<!DOCTYPE doc [
    <!ENTITY img SYSTEM "expat.gif" NDATA GIF>
    <!NOTATION GIF PUBLIC "-//CompuServe//NOTATION Graphics Interchange Format 89a//EN">
    ]>
    <doc></doc>'''))
    expected = [
        "setDocumentLocator",
        "startDocument",
        "startDTD",
        "endDTD",
        "characters",
        "startElement",
        "endElement",
        "endDocument"
    ]
    for v, e in zip(saxHandler.visited, expected):
        asserts.assert_that(v[0]).is_equal_to(e)


def _test_sax_parser_eg1():
    saxHandler = TracingSaxHandler()
    parser = setup(saxHandler)
    parser.parse(StringIO(eg1))
    expected = [
        "setDocumentLocator",
        "startDocument",
            "xmlDecl",
                "characters",
                "processingInstruction",
                "characters",
                "characters",
                    "startElement",
                        "characters",
                        "comment",
                    "endElement",
                "characters",
                "processingInstruction",
                "characters",
                "comment",
                "characters",
                "comment",
                "characters",
        "endDocument",
    ]

    for v, e in zip(saxHandler.visited, expected):
        asserts.assert_that(v[0]).is_equal_to(e)

def _test_sax_handler_version():
    asserts.assert_that(version).is_equal_to("2.0beta-larky")
    test()


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_sax_handler_version))
    _suite.addTest(unittest.FunctionTestCase(_test_sax_parser))
    _suite.addTest(unittest.FunctionTestCase(_test_sax_parser_eg1))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
