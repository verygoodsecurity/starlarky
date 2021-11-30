"""An XML Reader is the SAX 2 name for an XML parser. XML Parsers
should be based on this code. """
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//xml/sax/handler", handler="handler")
load("@stdlib//xml/sax/saxutils", saxutils="saxutils")
load("@stdlib//xml/sax/_exceptions", SAXNotSupportedException="SAXNotSupportedException", SAXNotRecognizedException="SAXNotRecognizedException")
load("@vendor//option/result", Error="Error")


def XMLReader():
    """Interface for reading an XML document using callbacks.

    XMLReader is the interface that an XML parser's SAX2 driver must
    implement. This interface allows an application to set and query
    features and properties in the parser, to register event handlers
    for document processing, and to initiate a document parse.

    All SAX interfaces are assumed to be synchronous: the parse
    methods must not return until parsing is complete, and readers
    must wait for an event-handler callback to return before reporting
    the next event."""
    self = larky.mutablestruct(__name__='XMLReader', __class__=XMLReader)

    def __init__():
        self._cont_handler = handler.ContentHandler()
        self._dtd_handler = handler.DTDHandler()
        self._ent_handler = handler.EntityResolver()
        self._err_handler = handler.ErrorHandler()
        return self
    self = __init__()

    def parse(source):
        "Parse an XML document from a system identifier or an InputSource."
        fail("NotImplementedError: This method must be implemented!")
    self.parse = parse

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

    def setLocale(locale):
        """Allow an application to set the locale for errors and warnings.

        SAX parsers are not required to provide localization for errors
        and warnings; if they cannot support the requested locale,
        however, they must raise a SAX exception. Applications may
        request a locale change in the middle of a parse."""
        fail("SAXNotSupportedException: Locale support not implemented")
    self.setLocale = setLocale

    def getFeature(name):
        "Looks up and returns the state of a SAX2 feature."
        fail("SAXNotRecognizedException: " + "Feature '%s' not recognized" % name)
    self.getFeature = getFeature

    def setFeature(name, state):
        "Sets the state of a SAX2 feature."
        fail("SAXNotRecognizedException: " + "Feature '%s' not recognized" % name)
    self.setFeature = setFeature

    def getProperty(name):
        "Looks up and returns the value of a SAX2 property."
        fail("SAXNotRecognizedException: " + "Property '%s' not recognized" % name)
    self.getProperty = getProperty

    def setProperty(name, value):
        "Sets the value of a SAX2 property."
        fail("SAXNotRecognizedException: " + "Property '%s' not recognized" % name)
    self.setProperty = setProperty
    return self


# Here so we can have a directed dependency (w/o forward declaration)
def InputSource(system_id=None):
    """Encapsulation of the information needed by the XMLReader to
    read entities.

    This class may include information about the public identifier,
    system identifier, byte stream (possibly with character encoding
    information) and/or the character stream of an entity.

    Applications will create objects of this class for use in the
    XMLReader.parse method and for returning from
    EntityResolver.resolveEntity.

    An InputSource belongs to the application, the XMLReader is not
    allowed to modify InputSource objects passed to it from the
    application, although it may make copies and modify those."""
    self = larky.mutablestruct(__name__='InputSource', __class__=InputSource)

    def __init__(system_id):
        self.__system_id = system_id
        self.__public_id = None
        self.__encoding = None
        self.__bytefile = None
        self.__charfile = None
        return self
    self = __init__(system_id)

    def setPublicId(public_id):
        "Sets the public identifier of this InputSource."
        self.__public_id = public_id
    self.setPublicId = setPublicId

    def getPublicId():
        "Returns the public identifier of this InputSource."
        return self.__public_id
    self.getPublicId = getPublicId

    def setSystemId(system_id):
        "Sets the system identifier of this InputSource."
        self.__system_id = system_id
    self.setSystemId = setSystemId

    def getSystemId():
        "Returns the system identifier of this InputSource."
        return self.__system_id
    self.getSystemId = getSystemId

    def setEncoding(encoding):
        """Sets the character encoding of this InputSource.

        The encoding must be a string acceptable for an XML encoding
        declaration (see section 4.3.3 of the XML recommendation).

        The encoding attribute of the InputSource is ignored if the
        InputSource also contains a character stream."""
        self.__encoding = encoding
    self.setEncoding = setEncoding

    def getEncoding():
        "Get the character encoding of this InputSource."
        return self.__encoding
    self.getEncoding = getEncoding

    def setByteStream(bytefile):
        """Set the byte stream (a Python file-like object which does
        not perform byte-to-character conversion) for this input
        source.

        The SAX parser will ignore this if there is also a character
        stream specified, but it will use a byte stream in preference
        to opening a URI connection itself.

        If the application knows the character encoding of the byte
        stream, it should set it with the setEncoding method."""
        self.__bytefile = bytefile
    self.setByteStream = setByteStream

    def getByteStream():
        """Get the byte stream for this input source.

        The getEncoding method will return the character encoding for
        this byte stream, or None if unknown."""
        return self.__bytefile
    self.getByteStream = getByteStream

    def setCharacterStream(charfile):
        """Set the character stream for this input source. (The stream
        must be a Python 2.0 Unicode-wrapped file-like that performs
        conversion to Unicode strings.)

        If there is a character stream specified, the SAX parser will
        ignore any byte stream and will not attempt to open a URI
        connection to the system identifier."""
        self.__charfile = charfile
    self.setCharacterStream = setCharacterStream

    def getCharacterStream():
        "Get the character stream for this input source."
        return self.__charfile
    self.getCharacterStream = getCharacterStream
    return self


def IncrementalParser(bufsize=pow(2, 16)):
    """This interface adds three extra methods to the XMLReader
    interface that allow XML parsers to support incremental
    parsing. Support for this interface is optional, since not all
    underlying XML parsers support this functionality.

    When the parser is instantiated it is ready to begin accepting
    data from the feed method immediately. After parsing has been
    finished with a call to close the reset method must be called to
    make the parser ready to accept new data, either from feed or
    using the parse method.

    Note that these methods must _not_ be called during parsing, that
    is, after parse has been called and before it returns.

    By default, the class also implements the parse method of the XMLReader
    interface using the feed, close and reset methods of the
    IncrementalParser interface as a convenience to SAX 2.0 driver
    writers."""
    self = XMLReader()
    self.__name__ = 'IncrementalParser'
    self.__class__ = IncrementalParser

    def __init__(bufsize):
        self._bufsize = bufsize
        return self
    self = __init__(bufsize)

    def prepareParser(source):
        """This method is called by the parse implementation to allow
        the SAX 2.0 driver to prepare itself for parsing."""
        fail("NotImplementedError: prepareParser must be overridden!")
    self.prepareParser = prepareParser

    def feed(data):
        """This method gives the raw XML data in the data parameter to
        the parser and makes it parse the data, emitting the
        corresponding events. It is allowed for XML constructs to be
        split across several calls to feed.

        feed may raise SAXException."""
        fail("NotImplementedError: This method must be implemented!")
    self.feed = feed

    def close():
        """This method is called when the entire XML document has been
        passed to the parser through the feed method, to notify the
        parser that there are no more data. This allows the parser to
        do the final checks on the document and empty the internal
        data buffer.

        The parser will not be ready to parse another document until
        the reset method has been called.

        close may raise SAXException."""
        fail("NotImplementedError: This method must be implemented!")
    self.close = close

    def reset():
        """This method is called after close has been called to reset
        the parser so that it is ready to parse new documents. The
        results of calling parse or feed after close without calling
        reset are undefined."""
        fail("NotImplementedError: This method must be implemented!")
    self.reset = reset

    def parse(source):
        source = saxutils.prepare_input_source(source,
                                               input_source_factory=InputSource)

        self.prepareParser(source)
        file = source.getCharacterStream()
        if file == None:
            file = source.getByteStream()
        buffer = file.read(self._bufsize)
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not buffer:
                break
            self.feed(buffer)
            buffer = file.read(self._bufsize)
        self.close()
    self.parse = parse

    return self


def Locator():
    """Interface for associating a SAX event with a document
    location. A locator object will return valid results only during
    calls to DocumentHandler methods; at any other time, the
    results are unpredictable."""
    self = larky.mutablestruct(__name__='Locator', __class__=Locator)

    def getColumnNumber():
        "Return the column number where the current event ends."
        return -1
    self.getColumnNumber = getColumnNumber

    def getLineNumber():
        "Return the line number where the current event ends."
        return -1
    self.getLineNumber = getLineNumber

    def getPublicId():
        "Return the public identifier for the current event."
        return None
    self.getPublicId = getPublicId

    def getSystemId():
        "Return the system identifier for the current event."
        return None
    self.getSystemId = getSystemId
    return self


def AttributesImpl(attrs):
    self = larky.mutablestruct(__name__='AttributesImpl', __class__=AttributesImpl)
    def __init__(attrs):
        """Non-NS-aware implementation.

        attrs should be of the form {name : value}."""
        self._attrs = attrs
        return self
    self = __init__(attrs)

    def getLength():
        return len(self._attrs)
    self.getLength = getLength

    def getType(name):
        return "CDATA"
    self.getType = getType

    def getValue(name):
        return self._attrs[name]
    self.getValue = getValue

    def getValueByQName(name):
        return self._attrs[name]
    self.getValueByQName = getValueByQName

    def getNameByQName(name):
        if name not in self._attrs:
            fail()
        return name
    self.getNameByQName = getNameByQName

    def getQNameByName(name):
        if name not in self._attrs:
            fail("KeyError: " + name)
        return name
    self.getQNameByName = getQNameByName

    def getNames():
        return list(self._attrs.keys())
    self.getNames = getNames

    def getQNames():
        return list(self._attrs.keys())
    self.getQNames = getQNames

    def __len__():
        return len(self._attrs)
    self.__len__ = __len__

    def __getitem__(name):
        return self._attrs[name]
    self.__getitem__ = __getitem__

    def keys():
        return list(self._attrs.keys())
    self.keys = keys

    def __contains__(name):
        return name in self._attrs
    self.__contains__ = __contains__

    def get(name, alternative=None):
        return self._attrs.get(name, alternative)
    self.get = get

    def copy():
        return self.__class__(self._attrs)
    self.copy = copy

    def items():
        return list(self._attrs.items())
    self.items = items

    def values():
        return list(self._attrs.values())
    self.values = values
    return self


def AttributesNSImpl(attrs, qnames):
    #  attrs should be of the form {(ns_uri, lname): value, ...}.
    self = AttributesImpl(attrs)
    self.__name__ = 'AttributesNSImpl'
    self.__class__ = AttributesNSImpl

    def __init__(qnames):
        """NS-aware implementation.

        attrs should be of the form {(ns_uri, lname): value, ...}.
        qnames of the form {(ns_uri, lname): qname, ...}."""
        self._qnames = qnames
        return self
    self = __init__(qnames)

    def getValueByQName(name):
        for (nsname, qname) in self._qnames.items():
            if qname == name:
                return self._attrs[nsname]

        fail("KeyError: " + name)
    self.getValueByQName = getValueByQName

    def getNameByQName(name):
        for (nsname, qname) in self._qnames.items():
            if qname == name:
                return nsname

        fail("KeyError: " + name)
    self.getNameByQName = getNameByQName

    def getQNameByName(name):
        return self._qnames[name]
    self.getQNameByName = getQNameByName

    def getQNames():
        return list(self._qnames.values())
    self.getQNames = getQNames

    def copy():
        return self.__class__(self._attrs, self._qnames)
    self.copy = copy
    return self


def _test():
    XMLReader()
    IncrementalParser()
    Locator()

test=_test