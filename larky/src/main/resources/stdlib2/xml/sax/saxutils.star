def __dict_replace(s, d):
    """
    Replace substrings of a string using a dictionary.
    """
def escape(data, entities={}):
    """
    Escape &, <, and > in a string of data.

        You can escape other strings of data by passing a dictionary as
        the optional entities parameter.  The keys and values must all be
        strings; each key will be replaced with its corresponding value.
    
    """
def unescape(data, entities={}):
    """
    Unescape &amp;, &lt;, and &gt; in a string of data.

        You can unescape other strings of data by passing a dictionary as
        the optional entities parameter.  The keys and values must all be
        strings; each key will be replaced with its corresponding value.
    
    """
def quoteattr(data, entities={}):
    """
    Escape and quote an attribute value.

        Escape &, <, and > in a string of data, then quote it for use as
        an attribute value.  The \" character will be escaped as well, if
        necessary.

        You can escape other strings of data by passing a dictionary as
        the optional entities parameter.  The keys and values must all be
        strings; each key will be replaced with its corresponding value.
    
    """
def _gettextwriter(out, encoding):
    """
     use a text writer as is

    """
        def _wrapper:
    """
     This is to handle passed objects that aren't in the
     IOBase hierarchy, but just have a write method

    """
def XMLGenerator(handler.ContentHandler):
    """
    iso-8859-1
    """
    def _qname(self, name):
        """
        Builds a qualified name from a (ns_url, localname) pair
        """
    def _finish_pending_start_element(self,endElement=False):
        """
        '>'
        """
    def startDocument(self):
        """
        '<?xml version="1.0" encoding="%s"?>\n'
        """
    def endDocument(self):
        """
        '<'
        """
    def endElement(self, name):
        """
        '/>'
        """
    def startElementNS(self, name, qname, attrs):
        """
        '<'
        """
    def endElementNS(self, name, qname):
        """
        '/>'
        """
    def characters(self, content):
        """
        '<?%s %s?>'
        """
def XMLFilterBase(xmlreader.XMLReader):
    """
    This class is designed to sit between an XMLReader and the
        client application's event handlers.  By default, it does nothing
        but pass requests up to the reader and events on to the handlers
        unmodified, but subclasses can override specific methods to modify
        the event stream or the configuration requests as they pass
        through.
    """
    def __init__(self, parent = None):
        """
         ErrorHandler methods


        """
    def error(self, exception):
        """
         ContentHandler methods


        """
    def setDocumentLocator(self, locator):
        """
         DTDHandler methods


        """
    def notationDecl(self, name, publicId, systemId):
        """
         EntityResolver methods


        """
    def resolveEntity(self, publicId, systemId):
        """
         XMLReader methods


        """
    def parse(self, source):
        """
         XMLFilter methods


        """
    def getParent(self):
        """
         --- Utility functions


        """
def prepare_input_source(source, base=""):
    """
    This function takes an InputSource and an optional base URL and
        returns a fully resolved InputSource object ready for reading.
    """
