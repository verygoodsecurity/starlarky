def ElementInfo(object):
    """
    '_attr_info'
    """
    def __init__(self, tagName, model=None):
        """
        (
        """
    def getAttributeTypeNS(self, namespaceURI, localName):
        """
        ID
        """
    def isIdNS(self, euri, ename, auri, aname):
        """
         not sure this is meaningful

        """
def _intern(builder, s):
    """
    ' '
    """
def ExpatBuilder:
    """
    Document builder that uses Expat to build a ParsedXML.DOM document
        instance.
    """
    def __init__(self, options=None):
        """
         This *really* doesn't do anything in this case, so
         override it with something fast & minimal.

        """
    def createParser(self):
        """
        Create a new parser object.
        """
    def getParser(self):
        """
        Return the parser object, creating a new one if needed.
        """
    def reset(self):
        """
        Free all data structures used during DOM construction.
        """
    def install(self, parser):
        """
        Install the callbacks needed to build the DOM into the parser.
        """
    def parseFile(self, file):
        """
        Parse a document from a file object, returning the document
                node.
        """
    def parseString(self, string):
        """
        Parse a document from a string, returning the document node.
        """
    def _setup_subset(self, buffer):
        """
        Load the internal subset if there might be one.
        """
2021-03-02 20:53:41,424 : INFO : tokenize_signature : --> do i ever get here?
    def start_doctype_decl_handler(self, doctypeName, systemId, publicId,
                                   has_internal_subset):
        """
         we don't care about parameter entities for the DOM

        """
    def notation_decl_handler(self, notationName, base, systemId, publicId):
        """
         To be general, we'd have to call isSameNode(), but this
         is sufficient for minidom:

        """
    def end_element_handler(self, name):
        """
         We have element type information and should remove ignorable
         whitespace; identify for text nodes which contain only
         whitespace.

        """
    def element_decl_handler(self, name, model):
        """
         This is still a little ugly, thanks to the pyexpat API. ;-(

        """
def FilterVisibilityController(object):
    """
    Wrapper around a DOMBuilderFilter which implements the checks
        to make the whatToShow filter attribute work.
    """
    def __init__(self, filter):
        """
        startContainer() returned illegal value: 
        """
    def acceptNode(self, node):
        """
         move all child nodes to the parent, and remove this node

        """
def FilterCrutch(object):
    """
    '_builder'
    """
    def __init__(self, builder):
        """
        ProcessingInstructionHandler
        """
    def start_element_handler(self, *args):
        """
         restore the old handlers

        """
def Skipper(FilterCrutch):
    """
     We're popping back out of the node we're skipping, so we
     shouldn't need to do anything but reset the handlers.

    """
def FragmentBuilder(ExpatBuilder):
    """
    Builder which constructs document fragments given XML source
        text and a context node.

        The context node is expected to provide information about the
        namespace declarations which are in scope at the start of the
        fragment.
    
    """
    def __init__(self, context, options=None):
        """
        Parse a document fragment from a file object, returning the
                fragment node.
        """
    def parseString(self, string):
        """
        Parse a document fragment from a string, returning the
                fragment node.
        """
    def _getDeclarations(self):
        """
        Re-create the internal subset from the DocumentType node.

                This is only needed if we don't already have the
                internalSubset as a string.
        
        """
    def _getNSattrs(self):
        """

        """
    def external_entity_ref_handler(self, context, base, systemId, publicId):
        """
         this entref is the one that we made to put the subtree
         in; all of our given input is parsed in here.

        """
def Namespaces:
    """
    Mix-in class for builders; adds support for namespaces.
    """
    def _initNamespaces(self):
        """
         list of (prefix, uri) ns declarations.  Namespace attrs are
         constructed from this and added to the element's attrs.

        """
    def createParser(self):
        """
        Create a new namespace-handling parser.
        """
    def install(self, parser):
        """
        Insert the namespace-handlers onto the parser.
        """
    def start_namespace_decl_handler(self, prefix, uri):
        """
        Push this namespace declaration on our storage.
        """
    def start_element_handler(self, name, attributes):
        """
        ' '
        """
        def end_element_handler(self, name):
            """
            ' '
            """
def ExpatBuilderNS(Namespaces, ExpatBuilder):
    """
    Document builder that supports namespaces.
    """
    def reset(self):
        """
        Fragment builder that supports namespaces.
        """
    def reset(self):
        """
        Return string of namespace attributes from this element and
                ancestors.
        """
def ParseEscape(Exception):
    """
    Exception raised to short-circuit parsing in InternalSubsetExtractor.
    """
def InternalSubsetExtractor(ExpatBuilder):
    """
    XML processor which can rip out the internal document type subset.
    """
    def getSubset(self):
        """
        Return the internal subset as a string.
        """
    def parseFile(self, file):
        """
        ''
        """
    def start_element_handler(self, name, attrs):
        """
        Parse a document, returning the resulting Document node.

            'file' may be either a file name or an open file object.
    
        """
def parseString(string, namespaces=True):
    """
    Parse a document from a string, returning the resulting
        Document node.
    
    """
def parseFragment(file, context, namespaces=True):
    """
    Parse a fragment of a document, given the context from which it
        was originally extracted.  context should be the parent of the
        node(s) which are in the fragment.

        'file' may be either a file name or an open file object.
    
    """
def parseFragmentString(string, context, namespaces=True):
    """
    Parse a fragment of a document from a string, given the context
        from which it was originally extracted.  context should be the
        parent of the node(s) which are in the fragment.
    
    """
def makeBuilder(options):
    """
    Create a builder based on an Options object.
    """
