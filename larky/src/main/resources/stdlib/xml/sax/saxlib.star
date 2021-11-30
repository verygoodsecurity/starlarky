"""
This module contains the core classes of version 2.0 of SAX for Python.
This file provides only default classes with absolutely minimum
functionality, from which drivers and applications can be subclassed.

Many of these classes are empty and are included only as documentation
of the interfaces.

$Id: saxlib.py,v 1.12 2002/05/10 14:49:21 akuchling Exp $
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//xml/sax/handler",
     feature_namespaces="feature_namespaces",
     feature_namespace_prefixes="feature_namespace_prefixes",
     feature_string_interning="feature_string_interning",
     feature_validation="feature_validation",
     feature_external_ges="feature_external_ges",
     feature_external_pes="feature_external_pes",
     all_features="all_features",
     property_lexical_handler="property_lexical_handler",
     property_declaration_handler="property_declaration_handler",
     property_dom_node="property_dom_node",
     property_xml_string="property_xml_string",
     all_properties="all_properties")
load("@stdlib//xml/sax/handler",
     ErrorHandler="ErrorHandler",
     ContentHandler="ContentHandler",
     DTDHandler="DTDHandler",
     EntityResolver="EntityResolver",
     LexicalHandler="LexicalHandler")
load("@stdlib//xml/sax/xmlreader",
     XMLReader="XMLReader",
     InputSource="InputSource",
     Locator="Locator",
     IncrementalParser="IncrementalParser")
load("@vendor//option/result", Error="Error")

version = "2.0beta"


def XMLFilter(parent=None):
    """Interface for a SAX2 parser filter.

    A parser filter is an XMLReader that gets its events from another
    XMLReader (which may in turn also be a filter) rather than from a
    primary source like a document or other non-SAX data source.
    Filters can modify a stream of events before passing it on to its
    handlers."""
    self = XMLFilter()
    self.__name__ = 'XMLFilter'
    self.__class__ = XMLFilter

    def __init__(parent):
        """Creates a filter instance, allowing applications to set the
        parent on instantiation."""
        self._parent = parent
        return self
    self = __init__(parent)

    def setParent(parent):
        """Sets the parent XMLReader of this filter. The argument may
        not be None."""
        self._parent = parent
    self.setParent = setParent

    def getParent():
        "Returns the parent of this filter."
        return self._parent
    self.getParent = getParent
    return self


def Attributes():
    """Interface for a list of XML attributes.

    Contains a list of XML attributes, accessible by name."""
    self = larky.mutablestruct(__name__='Attributes', __class__=Attributes)

    def getLength():
        "Returns the number of attributes in the list."
        fail("NotImplementedError: This method must be implemented!")
    self.getLength = getLength

    def getType(name):
        "Returns the type of the attribute with the given name."
        fail("NotImplementedError: This method must be implemented!")
    self.getType = getType

    def getValue(name):
        "Returns the value of the attribute with the given name."
        fail("NotImplementedError: This method must be implemented!")
    self.getValue = getValue

    def getValueByQName(name):
        """Returns the value of the attribute with the given raw (or
        qualified) name."""
        fail("NotImplementedError: This method must be implemented!")
    self.getValueByQName = getValueByQName

    def getNameByQName(name):
        """Returns the namespace name of the attribute with the given
        raw (or qualified) name."""
        fail("NotImplementedError: This method must be implemented!")
    self.getNameByQName = getNameByQName

    def getNames():
        """Returns a list of the names of all attributes
        in the list."""
        fail("NotImplementedError: This method must be implemented!")
    self.getNames = getNames

    def getQNames():
        """Returns a list of the raw qualified names of all attributes
        in the list."""
        fail("NotImplementedError: This method must be implemented!")
    self.getQNames = getQNames

    def __len__():
        "Alias for getLength."
        fail("NotImplementedError: This method must be implemented!")
    self.__len__ = __len__

    def __getitem__(name):
        "Alias for getValue."
        fail("NotImplementedError: This method must be implemented!")
    self.__getitem__ = __getitem__

    def keys():
        "Returns a list of the attribute names in the list."
        fail("NotImplementedError: This method must be implemented!")
    self.keys = keys

    def has_key(name):
        "True if the attribute is in the list, false otherwise."
        fail("NotImplementedError: This method must be implemented!")
    self.has_key = has_key

    def get(name, alternative=None):
        """Return the value associated with attribute name; if it is not
        available, then return the alternative."""
        fail("NotImplementedError: This method must be implemented!")
    self.get = get

    def copy():
        "Return a copy of the Attributes object."
        fail("NotImplementedError: This method must be implemented!")
    self.copy = copy

    def items():
        "Return a list of (attribute_name, value) pairs."
        fail("NotImplementedError: This method must be implemented!")
    self.items = items

    def values():
        "Return a list of all attribute values."
        fail("NotImplementedError: This method must be implemented!")
    self.values = values
    return self


def DeclHandler():
    """Optional SAX2 handler for DTD declaration events.

    Note that some DTD declarations are already reported through the
    DTDHandler interface. All events reported to this handler will
    occur between the startDTD and endDTD events of the
    LexicalHandler.

    To set the DeclHandler for an XMLReader, use the setProperty method
    with the identifier http://xml.org/sax/handlers/DeclHandler."""
    self = larky.mutablestruct(__name__='DeclHandler', __class__=DeclHandler)

    def attributeDecl(elem_name, attr_name, type, value_def, value):
        """Report an attribute type declaration.

        Only the first declaration will be reported. The type will be
        one of the strings "CDATA", "ID", "IDREF", "IDREFS",
        "NMTOKEN", "NMTOKENS", "ENTITY", "ENTITIES", or "NOTATION", or
        a list of names (in the case of enumerated definitions).

        elem_name is the element type name, attr_name the attribute
        type name, type a string representing the attribute type,
        value_def a string representing the default declaration
        ('#IMPLIED', '#REQUIRED', '#FIXED' or None). value is a string
        representing the attribute's default value, or None if there
        is none."""
    self.attributeDecl = attributeDecl

    def elementDecl(elem_name, content_model):
        """Report an element type declaration.

        Only the first declaration will be reported.

        content_model is the string 'EMPTY', the string 'ANY' or the content
        model structure represented as tuple (separator, tokens, modifier)
        where separator is the separator in the token list (that is, '|' or
        ','), tokens is the list of tokens (element type names or tuples
        representing parentheses) and modifier is the quantity modifier
        ('*', '?' or '+')."""
    self.elementDecl = elementDecl

    def internalEntityDecl(name, value):
        """Report an internal entity declaration.

        Only the first declaration of an entity will be reported.

        name is the name of the entity. If it is a parameter entity,
        the name will begin with '%'. value is the replacement text of
        the entity."""
    self.internalEntityDecl = internalEntityDecl

    def externalEntityDecl(name, public_id, system_id):
        """Report a parsed entity declaration. (Unparsed entities are
        reported to the DTDHandler.)

        Only the first declaration for each entity will be reported.

        name is the name of the entity. If it is a parameter entity,
        the name will begin with '%'. public_id and system_id are the
        public and system identifiers of the entity. public_id will be
        None if none were declared."""
    self.externalEntityDecl = externalEntityDecl
    return self


def AttributeList():
    """Interface for an attribute list. This interface provides
    information about a list of attributes for an element (only
    specified or defaulted attributes will be reported). Note that the
    information returned by this object will be valid only during the
    scope of the DocumentHandler.startElement callback, and the
    attributes will not necessarily be provided in the order declared
    or specified."""
    self = larky.mutablestruct(__name__='AttributeList',
                               __class__=AttributeList)

    def getLength():
        "Return the number of attributes in list."
    self.getLength = getLength

    def getName(i):
        "Return the name of an attribute in the list."
    self.getName = getName

    def getType(i):
        """Return the type of an attribute in the list. (Parameter can be
        either integer index or attribute name.)"""
    self.getType = getType

    def getValue(i):
        """Return the value of an attribute in the list. (Parameter can be
        either integer index or attribute name.)"""
    self.getValue = getValue

    def __len__():
        "Alias for getLength."
    self.__len__ = __len__

    def __getitem__(key):
        "Alias for getName (if key is an integer) and getValue (if string)."
    self.__getitem__ = __getitem__

    def keys():
        "Returns a list of the attribute names."
    self.keys = keys

    def has_key(key):
        "True if the attribute is in the list, false otherwise."
    self.has_key = has_key

    def get(key, alternative=None):
        """Return the value associated with attribute name; if it is not
        available, then return the alternative."""
    self.get = get

    def copy():
        "Return a copy of the AttributeList."
    self.copy = copy

    def items():
        "Return a list of (attribute_name,value) pairs."
    self.items = items

    def values():
        "Return a list of all attribute values."
    self.values = values
    return self


def DocumentHandler():
    """Handle general document events. This is the main client
    interface for SAX: it contains callbacks for the most important
    document events, such as the start and end of elements. You need
    to create an object that implements this interface, and then
    register it with the Parser. If you do not want to implement
    the entire interface, you can derive a class from HandlerBase,
    which implements the default functionality. You can find the
    location of any document event using the Locator interface
    supplied by setDocumentLocator()."""
    self = larky.mutablestruct(__name__='DocumentHandler',
                               __class__=DocumentHandler)

    def characters(ch, start, length):
        "Handle a character data event."
    self.characters = characters

    def endDocument():
        "Handle an event for the end of a document."
    self.endDocument = endDocument

    def endElement(name):
        "Handle an event for the end of an element."
    self.endElement = endElement

    def ignorableWhitespace(ch, start, length):
        "Handle an event for ignorable whitespace in element content."
    self.ignorableWhitespace = ignorableWhitespace

    def processingInstruction(target, data):
        "Handle a processing instruction event."
    self.processingInstruction = processingInstruction

    def setDocumentLocator(locator):
        "Receive an object for locating the origin of SAX document events."
    self.setDocumentLocator = setDocumentLocator

    def startDocument():
        "Handle an event for the beginning of a document."
    self.startDocument = startDocument

    def startElement(name, atts):
        "Handle an event for the beginning of an element."
    self.startElement = startElement
    return self


def HandlerBase():
    """Default base class for handlers. This class implements the
    default behaviour for four SAX interfaces: EntityResolver,
    DTDHandler, DocumentHandler, and ErrorHandler: rather
    than implementing those full interfaces, you may simply extend
    this class and override the methods that you need. Note that the
    use of this class is optional (you are free to implement the
    interfaces directly if you wish)."""

    # EntityResolver, DTDHandler, DocumentHandler, ErrorHandler
    pass


def Parser():
    """Basic interface for SAX (Simple API for XML) parsers. All SAX
    parsers must implement this basic interface: it allows users to
    register handlers for different types of events and to initiate a
    parse from a URI, a character stream, or a byte stream. SAX
    parsers should also implement a zero-argument constructor."""
    self = larky.mutablestruct(__name__='Parser', __class__=Parser)

    def __init__():
        self.doc_handler = DocumentHandler()
        self.dtd_handler = DTDHandler()
        self.ent_handler = EntityResolver()
        self._err_handler = ErrorHandler()
        return self
    self = __init__()

    def parse(systemId):
        "Parse an XML document from a system identifier."
    self.parse = parse

    def parseFile(fileobj):
        "Parse an XML document from a file-like object."
    self.parseFile = parseFile

    def setDocumentHandler(handler):
        "Register an object to receive basic document-related events."
        self.doc_handler = handler
    self.setDocumentHandler = setDocumentHandler

    def setDTDHandler(handler):
        "Register an object to receive basic DTD-related events."
        self.dtd_handler = handler
    self.setDTDHandler = setDTDHandler

    def setEntityResolver(resolver):
        "Register an object to resolve external entities."
        self.ent_handler = resolver
    self.setEntityResolver = setEntityResolver

    def setErrorHandler(handler):
        "Register an object to receive error-message events."
        self._err_handler = handler
    self.setErrorHandler = setErrorHandler

    def setLocale(locale):
        """Allow an application to set the locale for errors and warnings.

        SAX parsers are not required to provide localisation for errors
        and warnings; if they cannot support the requested locale,
        however, they must throw a SAX exception. Applications may
        request a locale change in the middle of a parse."""
        fail("SAXNotSupportedException: Locale support not implemented")
    self.setLocale = setLocale
    return self


saxlib = larky.struct(
    __name__='saxlib',
    # backward compatible imports..
    feature_namespaces=feature_namespaces,
    feature_namespace_prefixes=feature_namespace_prefixes,
    feature_string_interning=feature_string_interning,
    feature_validation=feature_validation,
    feature_external_ges=feature_external_ges,
    feature_external_pes=feature_external_pes,
    all_features=all_features,
    property_lexical_handler=property_lexical_handler,
    property_declaration_handler=property_declaration_handler,
    property_dom_node=property_dom_node,
    property_xml_string=property_xml_string,
    all_properties=all_properties,
    ErrorHandler=ErrorHandler,
    ContentHandler=ContentHandler,
    DTDHandler=DTDHandler,
    EntityResolver=EntityResolver,
    LexicalHandler=LexicalHandler,
    XMLReader=XMLReader,
    InputSource=InputSource,
    Locator=Locator,
    IncrementalParser=IncrementalParser,
    # saxlib exports
    version=version,
    XMLFilter=XMLFilter,
    Attributes=Attributes,
    DeclHandler=DeclHandler,
    AttributeList=AttributeList,
    DocumentHandler=DocumentHandler,
    HandlerBase=HandlerBase,
    Parser=Parser,
)