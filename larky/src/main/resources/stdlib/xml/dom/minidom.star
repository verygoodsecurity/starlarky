def Node(xml.dom.Node):
    """
     this is non-null only for elements and attributes
    """
    def __bool__(self):
        """

        """
    def toprettyxml(self, indent="\t", newl="\n", encoding=None):
        """
        xmlcharrefreplace
        """
    def hasChildNodes(self):
        """
         The DOM does not clearly specify what to return in this case

        """
    def appendChild(self, node):
        """
         The DOM does not clearly specify what to return in this case

        """
    def replaceChild(self, newChild, oldChild):
        """
        %s cannot be child of %s
        """
    def removeChild(self, oldChild):
        """
         empty text node; discard

        """
    def cloneNode(self, deep):
        """
         Overridden in Element and Attr where localName can be Non-Null

        """
    def isSameNode(self, other):
        """
         The "user data" functions use a dictionary that is only present
         if some user data has been set, so be careful not to assume it
         exists.


        """
    def getUserData(self, key):
        """
         ignore handlers passed for None

        """
    def _call_user_data_handler(self, operation, src, dst):
        """
        _user_data
        """
    def unlink(self):
        """
         A Node is its own context manager, to ensure that an unlink() call occurs.
         This is similar to how a file object works.

        """
    def __enter__(self):
        """
        firstChild
        """
def _append_child(self, node):
    """
     fast path with less checks; usable by DOM builders if careful

    """
def _in_document(node):
    """
     return True iff node is part of a document tree

    """
def _write_data(writer, data):
    """
    Writes datachars to writer.
    """
def _get_elements_by_tagName_helper(parent, name, rc):
    """
    *
    """
def _get_elements_by_tagName_ns_helper(parent, nsURI, localName, rc):
    """
    *
    """
def DocumentFragment(Node):
    """
    document-fragment
    """
    def __init__(self):
        """
        '_name'
        """
2021-03-02 20:53:42,627 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, qName, namespaceURI=EMPTY_NAMESPACE, localName=None,
                 prefix=None):
        """
         Add the single child node that represents the value of the attr

        """
    def _get_localName(self):
        """
        :
        """
    def _get_specified(self):
        """
        xmlns
        """
    def unlink(self):
        """
         This implementation does not call the base implementation
         since most of that is not needed, and the expense of the
         method call is not warranted.  We duplicate the removal of
         children, but that's all we needed from the base class.

        """
    def _get_isId(self):
        """
        isId
        """
def NamedNodeMap(object):
    """
    The attribute list is a transient interface to the underlying
        dictionaries.  Mutations here will change the underlying element's
        dictionary.

        Ordering is imposed artificially and does not reflect the order of
        attributes as found in an input document.
    
    """
    def __init__(self, attrs, attrsNS, ownerElement):
        """
        _attrs
        """
    def __eq__(self, other):
        """
         same as set

        """
    def __setitem__(self, attname, value):
        """
        value must be a string or Attr object
        """
    def getNamedItem(self, name):
        """
        'ownerElement'
        """
    def removeNamedItemNS(self, namespaceURI, localName):
        """
        'ownerElement'
        """
    def setNamedItem(self, node):
        """
        %s cannot be child of %s
        """
    def setNamedItemNS(self, node):
        """
        length
        """
def TypeInfo(object):
    """
    'namespace'
    """
    def __init__(self, namespace, name):
        """
        <%s %r (from %r)>
        """
    def _get_name(self):
        """
        'ownerDocument'
        """
2021-03-02 20:53:42,640 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, tagName, namespaceURI=EMPTY_NAMESPACE, prefix=None,
                 localName=None):
        """
         Attribute dictionaries are lazily created
         attributes are double-indexed:
            tagName -> Attribute
            URI,localName -> Attribute
         in the future: consider lazy generation
         of attribute objects this is too tricky
         for now because of headaches with
         namespaces.

        """
    def _ensure_attributes(self):
        """
        :
        """
    def _get_tagName(self):
        """

        """
    def getAttributeNS(self, namespaceURI, localName):
        """

        """
    def setAttribute(self, attname, value):
        """
         also sets nodeValue
        """
    def setAttributeNS(self, namespaceURI, qualifiedName, value):
        """
        attribute node already owned
        """
    def removeAttribute(self, name):
        """
         Restore this since the node is still useful and otherwise
         unlinked

        """
    def hasAttribute(self, name):
        """
        <DOM Element: %s at %#x>
        """
    def writexml(self, writer, indent="", addindent="", newl=""):
        """
         indent = current indentation
         addindent = indentation to add to higher levels
         newl = newline string

        """
    def _get_attributes(self):
        """
         DOM Level 3 attributes, based on the 22 Oct 2002 draft


        """
    def setIdAttribute(self, name):
        """
        attributes
        """
def _set_attribute_node(element, attr):
    """
     This creates a circular reference, but Element.unlink()
     breaks the cycle since the references to the attribute
     dictionaries are tossed.

    """
def Childless:
    """
    Mixin that makes childless-ness easy to implement and avoids
        the complexity of the Node methods that deal with children.
    
    """
    def _get_firstChild(self):
        """
         nodes cannot have children
        """
    def hasChildNodes(self):
        """
         nodes do not have children
        """
    def removeChild(self, oldChild):
        """
         nodes do not have children
        """
    def normalize(self):
        """
         For childless nodes, normalize() has nothing to do.

        """
    def replaceChild(self, newChild, oldChild):
        """
         nodes do not have children
        """
def ProcessingInstruction(Childless, Node):
    """
    'target'
    """
    def __init__(self, target, data):
        """
         nodeValue is an alias for data

        """
    def _get_nodeValue(self):
        """
         nodeName is an alias for target

        """
    def _set_nodeName(self, value):
        """

        """
def CharacterData(Childless, Node):
    """
    '_data'
    """
    def __init__(self):
        """
        ''
        """
    def _get_length(self):
        """
        ...
        """
    def substringData(self, offset, count):
        """
        offset cannot be negative
        """
    def appendData(self, arg):
        """
        offset cannot be negative
        """
    def deleteData(self, offset, count):
        """
        offset cannot be negative
        """
    def replaceData(self, offset, count, arg):
        """
        offset cannot be negative
        """
def Text(CharacterData):
    """
    text
    """
    def splitText(self, offset):
        """
        illegal offset value
        """
    def writexml(self, writer, indent="", addindent="", newl=""):
        """
        %s%s%s
        """
    def _get_wholeText(self):
        """
        ''
        """
    def replaceWholeText(self, content):
        """
         XXX This needs to be seriously changed if minidom ever
         supports EntityReference nodes.

        """
    def _get_isWhitespaceInElementContent(self):
        """
        isWhitespaceInElementContent
        """
def _get_containing_element(node):
    """
    comment
    """
    def __init__(self, data):
        """

        """
def CDATASection(Text):
    """
    cdata-section
    """
    def writexml(self, writer, indent="", addindent="", newl=""):
        """
        ]]>
        """
def ReadOnlySequentialNamedNodeMap(object):
    """
    '_seq'
    """
    def __init__(self, seq=()):
        """
         seq should be a list or tuple

        """
    def __len__(self):
        """
        NamedNodeMap instance is read-only
        """
    def removeNamedItemNS(self, namespaceURI, localName):
        """
        NamedNodeMap instance is read-only
        """
    def setNamedItem(self, node):
        """
        NamedNodeMap instance is read-only
        """
    def setNamedItemNS(self, node):
        """
        NamedNodeMap instance is read-only
        """
    def __getstate__(self):
        """
        length
        """
def Identified:
    """
    Mix-in class that supports the publicId and systemId attributes.
    """
    def _identified_mixin_init(self, publicId, systemId):
        """
         it's ok

        """
    def writexml(self, writer, indent="", addindent="", newl=""):
        """
        <!DOCTYPE 
        """
def Entity(Identified, Node):
    """
    cannot append children to an entity node
    """
    def insertBefore(self, newChild, refChild):
        """
        cannot insert children below an entity node
        """
    def removeChild(self, oldChild):
        """
        cannot remove children from an entity node
        """
    def replaceChild(self, newChild, oldChild):
        """
        cannot replace children of an entity node
        """
def Notation(Identified, Childless, Node):
    """
    core
    """
    def hasFeature(self, feature, version):
        """

        """
    def createDocument(self, namespaceURI, qualifiedName, doctype):
        """
        doctype object owned by another DOM tree
        """
    def createDocumentType(self, qualifiedName, publicId, systemId):
        """
         DOM Level 3 (WD 9 April 2002)


        """
    def getInterface(self, feature):
        """
         internal

        """
    def _create_document(self):
        """
        Object that represents content-model information for an element.

            This implementation is not expected to be used in practice; DOM
            builders should provide implementations which do the right thing
            using information available to it.

    
        """
    def __init__(self, name):
        """
        Returns true iff this element is declared to have an EMPTY
                content model.
        """
    def isId(self, aname):
        """
        Returns true iff the named attribute is a DTD-style ID.
        """
    def isIdNS(self, namespaceURI, localName):
        """
        Returns true iff the identified attribute is a DTD-style ID.
        """
    def __getstate__(self):
        """
        '_elem_info'
        """
    def __init__(self):
        """
         mapping of (namespaceURI, localName) -> ElementInfo
                and tagName -> ElementInfo

        """
    def _get_elem_info(self, element):
        """
        %s cannot be child of %s
        """
    def removeChild(self, oldChild):
        """
        node contents must be a string
        """
    def createCDATASection(self, data):
        """
        node contents must be a string
        """
    def createComment(self, data):
        """

        """
    def createElementNS(self, namespaceURI, qualifiedName):
        """

        """
    def _create_entity(self, name, publicId, systemId, notationName):
        """
         we never searched before, or the cache has been cleared

        """
    def getElementsByTagName(self, name):
        """
        cannot import document nodes
        """
    def writexml(self, writer, indent="", addindent="", newl="", encoding=None):
        """
        '<?xml version="1.0" ?>'
        """
    def renameNode(self, n, namespaceURI, name):
        """
        cannot rename nodes from other documents;\n
        expected %s,\nfound %s
        """
def _clone_node(node, deep, newOwnerDocument):
    """

        Clone a node and give it the new owner document.
        Called by Node.cloneNode and Document.importNode
    
    """
def _nssplit(qualifiedName):
    """
    ':'
    """
def _do_pulldom_parse(func, args, kwargs):
    """
    Parse a file into a DOM by filename or file object.
    """
def parseString(string, parser=None):
    """
    Parse a file into a DOM from a string.
    """
def getDOMImplementation(features=None):
