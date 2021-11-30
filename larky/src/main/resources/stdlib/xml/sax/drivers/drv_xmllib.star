"""
SAX driver for xmllib.py
"""
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//xmllib", xmllib="xmllib")
load("@stdlib//xml/sax/_exceptions",
     SAXException="SAXException",
     SAXNotRecognizedException="SAXNotRecognizedException",
     SAXParseException="SAXParseException",
     SAXNotSupportedException="SAXNotSupportedException",
     SAXReaderNotAvailable="SAXReaderNotAvailable")
load("@stdlib//xml/sax/handler", handler="handler")
load("@stdlib//xml/sax/saxlib", saxlib="saxlib")
load("@stdlib//xml/sax/saxutils", saxutils="saxutils")

version = "0.91"
WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION


def noop():
    pass


def SAX_XLParser():
    "SAX driver for xmllib.py."
    self = xmllib.XMLParser()
    self.__name__ = 'SAX_XLParser'
    self.__class__ = SAX_XLParser

    # xmllib -- overridden methods
    xmllib_XMLParser_reset = self.reset

    def reset():
        xmllib_XMLParser_reset()
        self.unfed_so_far = True
        self.encoding = "utf-8"
    self.reset = reset

    xmllib_XMLParser_feed = self.feed

    def feed(data):
        if self.unfed_so_far:
            self._cont_handler.startDocument()
            self.unfed_so_far = False

        xmllib_XMLParser_feed(data)
    self.feed = feed

    xmllib_XMLParser_close = self.close

    def close():
        xmllib_XMLParser_close()
        self._cont_handler.endDocument()
    self.close = close

    # start driver

    def __init__():
        # defaults..
        self._cont_handler = saxlib.ContentHandler()
        self._dtd_handler = saxlib.DTDHandler()
        self._ent_handler = saxlib.EntityResolver()
        self._err_handler = saxlib.ErrorHandler()
        self.standalone = None
        self.reset()
        # set by the properties/features
        self.__lex_handler = None
        self.__decl_handler = None
        return self
    self = __init__()

    def _convert(str):
        return codecs.encode(str, encoding=self.encoding)
    self._convert = _convert

    def _can_locate():
        "Internal: returns true if location info is available."
        return True
    self._can_locate = _can_locate

    # this is libparser/saxlib parser interface, but we deviate in
    # larky with xmlreader.XMLReader

    def parse(sysID):
        "Parse an XML document from a system identifier."
        self.sysID = sysID
        self.parseFile(sysID)
    self.parse = parse

    def parseFile(source):
        "Parse an XML document from a file-like object."
        if self._can_locate():
            self._cont_handler.setDocumentLocator(self)
        self.reset()
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            data = source.read(16384)
            if not data:
                break
            self.feed(data)
        self.close()
    self.parseFile = parseFile

    def getContentHandler():
        "Returns the current ContentHandler."
        return self._cont_handler
    self.getContentHandler = getContentHandler

    def setContentHandler(handler):
        "Registers a new object to receive document content events."
        self._cont_handler = handler
    self.setContentHandler = setContentHandler

    def getDTDHandler():
        "Returns the current DTD handler."
        return self._dtd_handler
    self.getDTDHandler = getDTDHandler

    def setDTDHandler(handler):
        "Register an object to receive basic DTD-related events."
        self._dtd_handler = handler
    self.setDTDHandler = setDTDHandler

    def getEntityResolver():
        "Returns the current EntityResolver."
        return self._ent_handler
    self.getEntityResolver = getEntityResolver

    def setEntityResolver(resolver):
        "Register an object to resolve external entities."
        self._ent_handler = resolver
    self.setEntityResolver = setEntityResolver

    def getErrorHandler():
        "Returns the current ErrorHandler."
        return self._err_handler
    self.getErrorHandler = getErrorHandler

    def setErrorHandler(handler):
        "Register an object to receive error-message events."
        self._err_handler = handler
    self.setErrorHandler = setErrorHandler

    # Locator interface

    def getColumnNumber():
        "Return the column number where the current event ends."
        return -1
    self.getColumnNumber = getColumnNumber

    def getLineNumber():
        "Return the line number where the current event ends."
        return self.lineno
    self.getLineNumber = getLineNumber

    def getPublicId():
        "Return the public identifier for the current event."
        return None
    self.getPublicId = getPublicId

    def getSystemId():
        "Return the system identifier for the current event."
        return self.sysID
    self.getSystemId = getSystemId

    # --- EXPERIMENTAL SAX PYTHON EXTENSIONS
    #  Experimental unofficial SAX level 2 extended parser interface.
    def get_parser_name():
        return "xmllib"
    self.get_parser_name = get_parser_name

    def get_parser_version():
        return xmllib.version
    self.get_parser_version = get_parser_version

    def get_driver_version():
        return version
    self.get_driver_version = get_driver_version

    def is_validating():
        return False
    self.is_validating = is_validating

    def is_dtd_reading():
        return False
    self.is_dtd_reading = is_dtd_reading

    # errors

    def syntax_error(message):
        "Handles fatal errors."
        if self._can_locate():
            self._err_handler.fatalError(SAXParseException(message, None, self))
        else:
            self._err_handler.fatalError(SAXException(message, None))
    self.syntax_error = syntax_error

    # handlers
    def handle_xml(encoding, standalone):
        """Remembers whether the document is standalone."""
        # version is defaulted to 1.0
        self.standalone = (standalone and
                           standalone in ("no", "yes") and
                           standalone == "yes")
        if encoding != None:
            self.encoding = encoding
        self.__lex_handler.xmlDecl(
            version="1.0",
            encoding=self.encoding,
            standalone=self.standalone
        )
    self.handle_xml = handle_xml

    def handle_doctype(tag, pubid, syslit, data):
        # DTDHandler methods
        self.__lex_handler.startDTD(tag, pubid, syslit)
        # TODO: figure out how to parse the entity decl
        # though it's not needed right now.
        # print("1.", tag, "2.", pubid, "3.",syslit, "4.",data)
        # self._dtd_handler.unparsedEntityDecl(tag, pubid, syslit, data)
        self.__lex_handler.endDTD()
    self.handle_doctype = handle_doctype

    def handle_comment(data):
        self.__lex_handler.comment(data)
    self.handle_comment = handle_comment

    def handle_starttag(tag, method, attrs):
        """This method is only invoked if we define special element
        tag handlers via the xmllib.XMLParser.elements attribute. By
         default, we define none. So, this is a noop and dispatches
         to unknown_starttag."""
        self._cont_handler.startElement(tag, attrs)
    self.handle_starttag = handle_starttag

    def handle_endtag(tag, method):
        """This method is only invoked if we define special element
        tag handlers via the xmllib.XMLParser.elements attribute. By
         default, we define none. So, this is a noop and dispatches
         to unknown_endtag."""
        self._cont_handler.endElement(self._convert(tag))
    self.handle_endtag = handle_endtag

    def handle_startns(prefix, qualified, href):
        self._cont_handler.startPrefixMapping(prefix, href)
    self.handle_startns = handle_startns

    def handle_endns(prefix):
        self._cont_handler.endPrefixMapping(prefix)
    self.handle_endns = handle_endns

    def handle_data(data):
        "Handles PCDATA."
        data = self._convert(data)
        self._cont_handler.characters(data)
    self.handle_data = handle_data

    def handle_cdata(data):
        "Handles CDATA marked sections."
        self.__lex_handler.startCDATA()
        data = self._convert(data)
        self._cont_handler.characters(data)
        self.__lex_handler.endCDATA()
    self.handle_cdata = handle_cdata

    def handle_proc(name, data):
        self._cont_handler.processingInstruction(name, data[1:])
    self.handle_proc = handle_proc

    xmllib_XMLParser_handle_charref = self.handle_charref

    def handle_charref(name):
        xmllib_XMLParser_handle_charref(name)
    self.handle_charref = handle_charref

    def unknown_starttag(tag, attributes):
        tag = self._convert(tag)
        newattr = {}
        for k, v in attributes.items():
            newattr[self._convert(k)] = self._convert(v)
        self._cont_handler.startElement(tag, saxutils.AttributeMap(newattr))
    self.unknown_starttag = unknown_starttag

    def unknown_endtag(tag):
        "Handles end tags."
        self._cont_handler.endElement(tag)
    self.unknown_endtag = unknown_endtag

    def unknown_charref(ref):
        pass
    self.unknown_charref = unknown_charref

    def unknown_entityref(name):
        self.syntax_error("reference to unknown entity `&%s;'" % name)
    self.unknown_entityref = unknown_entityref

    _features = {}

    # properties
    def getFeature(name):
        if name == handler.feature_namespaces:
            return _features.get(handler.feature_namespaces, False)
        elif name == handler.feature_namespace_prefixes:
            return _features.get(handler.feature_namespace_prefixes, False)
        elif name == handler.feature_validation:
            return _features.get(handler.feature_validation, False)
        elif name == handler.feature_external_ges:
            return _features.get(handler.feature_external_ges, False)
        elif name == handler.feature_external_pes:
            return _features.get(handler.feature_external_pes, False)
        else:
            fail("Feature '%s' not recognized" % name)
    self.getFeature = getFeature

    def setFeature(name, state):
        if not self.unfed_so_far:
            fail("Cannot set feature %s while parsing" % name)
        if name == handler.feature_namespaces:
            _features[handler.feature_namespaces] = state
        elif name == handler.feature_namespace_prefixes:
            _features[handler.feature_namespace_prefixes] = state
        elif name == handler.feature_validation:
            _features[handler.feature_validation] = state
        elif name == handler.feature_external_ges:
            if state == 0:
                # TODO (does that relate to PARSER_LOADDTD)?
                fail("Feature '%s' not supported" % name)

        elif name == handler.feature_external_pes:
            _features[handler.feature_external_pes] = state
        else:
            fail("Feature '%s' not recognized" % name)
    self.setFeature = setFeature

    def getProperty(name):
        if name == handler.property_lexical_handler:
            return self.__lex_handler
        elif name == handler.property_declaration_handler:
            return self.__decl_handler
        else:
            fail("Property '%s' not recognized" % name)
    self.getProperty = getProperty

    def setProperty(name, value):
        if name == handler.property_lexical_handler:
            self.__lex_handler = value
        elif name == handler.property_declaration_handler:
            # TODO: remove if/when we support dtd events
            fail("Property '%s' not supported" % name)
            # self.__decl_handler = value
        else:
            fail("Property '%s' not recognized" % name)
    self.setProperty = setProperty

    return self


def create_parser():
    return SAX_XLParser()


drv_xmllib = larky.struct(
    __name__='drv_xmllib',
    create_parser=create_parser,
    SAX_XLParser=SAX_XLParser,
)