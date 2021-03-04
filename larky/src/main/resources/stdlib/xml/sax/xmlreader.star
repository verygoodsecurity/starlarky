def XMLReader:
    """
    Interface for reading an XML document using callbacks.

        XMLReader is the interface that an XML parser's SAX2 driver must
        implement. This interface allows an application to set and query
        features and properties in the parser, to register event handlers
        for document processing, and to initiate a document parse.

        All SAX interfaces are assumed to be synchronous: the parse
        methods must not return until parsing is complete, and readers
        must wait for an event-handler callback to return before reporting
        the next event.
    """
    def __init__(self):
        """
        Parse an XML document from a system identifier or an InputSource.
        """
    def getContentHandler(self):
        """
        Returns the current ContentHandler.
        """
    def setContentHandler(self, handler):
        """
        Registers a new object to receive document content events.
        """
    def getDTDHandler(self):
        """
        Returns the current DTD handler.
        """
    def setDTDHandler(self, handler):
        """
        Register an object to receive basic DTD-related events.
        """
    def getEntityResolver(self):
        """
        Returns the current EntityResolver.
        """
    def setEntityResolver(self, resolver):
        """
        Register an object to resolve external entities.
        """
    def getErrorHandler(self):
        """
        Returns the current ErrorHandler.
        """
    def setErrorHandler(self, handler):
        """
        Register an object to receive error-message events.
        """
    def setLocale(self, locale):
        """
        Allow an application to set the locale for errors and warnings.

                SAX parsers are not required to provide localization for errors
                and warnings; if they cannot support the requested locale,
                however, they must raise a SAX exception. Applications may
                request a locale change in the middle of a parse.
        """
    def getFeature(self, name):
        """
        Looks up and returns the state of a SAX2 feature.
        """
    def setFeature(self, name, state):
        """
        Sets the state of a SAX2 feature.
        """
    def getProperty(self, name):
        """
        Looks up and returns the value of a SAX2 property.
        """
    def setProperty(self, name, value):
        """
        Sets the value of a SAX2 property.
        """
def IncrementalParser(XMLReader):
    """
    This interface adds three extra methods to the XMLReader
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
        writers.
    """
    def __init__(self, bufsize=2**16):
        """
        This method gives the raw XML data in the data parameter to
                the parser and makes it parse the data, emitting the
                corresponding events. It is allowed for XML constructs to be
                split across several calls to feed.

                feed may raise SAXException.
        """
    def prepareParser(self, source):
        """
        This method is called by the parse implementation to allow
                the SAX 2.0 driver to prepare itself for parsing.
        """
    def close(self):
        """
        This method is called when the entire XML document has been
                passed to the parser through the feed method, to notify the
                parser that there are no more data. This allows the parser to
                do the final checks on the document and empty the internal
                data buffer.

                The parser will not be ready to parse another document until
                the reset method has been called.

                close may raise SAXException.
        """
    def reset(self):
        """
        This method is called after close has been called to reset
                the parser so that it is ready to parse new documents. The
                results of calling parse or feed after close without calling
                reset are undefined.
        """
def Locator:
    """
    Interface for associating a SAX event with a document
        location. A locator object will return valid results only during
        calls to DocumentHandler methods; at any other time, the
        results are unpredictable.
    """
    def getColumnNumber(self):
        """
        Return the column number where the current event ends.
        """
    def getLineNumber(self):
        """
        Return the line number where the current event ends.
        """
    def getPublicId(self):
        """
        Return the public identifier for the current event.
        """
    def getSystemId(self):
        """
        Return the system identifier for the current event.
        """
def InputSource:
    """
    Encapsulation of the information needed by the XMLReader to
        read entities.

        This class may include information about the public identifier,
        system identifier, byte stream (possibly with character encoding
        information) and/or the character stream of an entity.

        Applications will create objects of this class for use in the
        XMLReader.parse method and for returning from
        EntityResolver.resolveEntity.

        An InputSource belongs to the application, the XMLReader is not
        allowed to modify InputSource objects passed to it from the
        application, although it may make copies and modify those.
    """
    def __init__(self, system_id = None):
        """
        Sets the public identifier of this InputSource.
        """
    def getPublicId(self):
        """
        Returns the public identifier of this InputSource.
        """
    def setSystemId(self, system_id):
        """
        Sets the system identifier of this InputSource.
        """
    def getSystemId(self):
        """
        Returns the system identifier of this InputSource.
        """
    def setEncoding(self, encoding):
        """
        Sets the character encoding of this InputSource.

                The encoding must be a string acceptable for an XML encoding
                declaration (see section 4.3.3 of the XML recommendation).

                The encoding attribute of the InputSource is ignored if the
                InputSource also contains a character stream.
        """
    def getEncoding(self):
        """
        Get the character encoding of this InputSource.
        """
    def setByteStream(self, bytefile):
        """
        Set the byte stream (a Python file-like object which does
                not perform byte-to-character conversion) for this input
                source.

                The SAX parser will ignore this if there is also a character
                stream specified, but it will use a byte stream in preference
                to opening a URI connection itself.

                If the application knows the character encoding of the byte
                stream, it should set it with the setEncoding method.
        """
    def getByteStream(self):
        """
        Get the byte stream for this input source.

                The getEncoding method will return the character encoding for
                this byte stream, or None if unknown.
        """
    def setCharacterStream(self, charfile):
        """
        Set the character stream for this input source. (The stream
                must be a Python 2.0 Unicode-wrapped file-like that performs
                conversion to Unicode strings.)

                If there is a character stream specified, the SAX parser will
                ignore any byte stream and will not attempt to open a URI
                connection to the system identifier.
        """
    def getCharacterStream(self):
        """
        Get the character stream for this input source.
        """
def AttributesImpl:
    """
    Non-NS-aware implementation.

            attrs should be of the form {name : value}.
    """
    def getLength(self):
        """
        CDATA
        """
    def getValue(self, name):
        """
         ===== ATTRIBUTESNSIMPL =====


        """
def AttributesNSImpl(AttributesImpl):
    """
    NS-aware implementation.

            attrs should be of the form {(ns_uri, lname): value, ...}.
            qnames of the form {(ns_uri, lname): qname, ...}.
    """
    def getValueByQName(self, name):
        """
        __main__
        """
