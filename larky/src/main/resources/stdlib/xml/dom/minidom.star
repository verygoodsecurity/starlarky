"""Simple implementation of the Level 1 DOM.

Namespaces and other minor Level 2 features are also supported.

parse("foo.xml")

parseString("<foo><bar/></foo>")

Todo:
=====
 * convenience methods for getting elements and text.
 * more testing
 * bring some of the writer and linearizer code into conformance with this
        interface
 * SAX 2 namespaces
"""

# import xml.dom

# from xml.dom import EMPTY_NAMESPACE, EMPTY_PREFIX, XMLNS_NAMESPACE, domreg
# from xml.dom.minicompat import *
# from xml.dom.xmlbuilder import DOMImplementationLS, DocumentLS

# This is used by the ID-cache invalidation checks; the list isn't
# actually complete, since the nodes being checked will never be the
# DOCUMENT_NODE or DOCUMENT_FRAGMENT_NODE.  (The node being checked is
# the node being added or removed, not the node being modified.)

load("@stdlib//builtins", builtins="builtins")
load("@stdlib//io", io="io")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//xml/dom", dom="dom")
load("@stdlib//xml/dom/domreg", domreg="domreg")
load("@stdlib//xml/dom/minicompat", NodeList="NodeList", EmptyNodeList="EmptyNodeList")
load("@stdlib//xml/dom/xmlbuilder",
     DOMImplementationLS="DOMImplementationLS",
     DocumentLS="DocumentLS")
load("@vendor//option/result", Result="Result", Error="Error")


EMPTY_NAMESPACE = dom.EMPTY_NAMESPACE
EMPTY_PREFIX = dom.EMPTY_PREFIX
XMLNS_NAMESPACE = dom.XMLNS_NAMESPACE


_nodeTypes_with_children = (dom.Node.ELEMENT_NODE, dom.Node.ENTITY_REFERENCE_NODE)


def _append_child(self, node):
    # fast path with less checks; usable by DOM builders if careful
    childNodes = self.childNodes
    if childNodes:
        last = childNodes[-1]
        node.previousSibling = last
        last.nextSibling = node
    childNodes.append(node)
    node.parentNode = self


def _in_document(node):
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if node == None:
            break
        if node.nodeType == Node.DOCUMENT_NODE:
            return True
        node = node.parentNode
    return False


def _write_data(writer, data):
    "Writes datachars to writer."
    if data:
        data = (
            data.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace('"', "&quot;")
            .replace(">", "&gt;")
        )
        writer.write(data)


def _get_elements_by_tagName_helper(parent, name, rc):
    for node in parent.childNodes:
        if node.nodeType == Node.ELEMENT_NODE and (name == "*" or node.tagName == name):
            rc.append(node)
        _get_elements_by_tagName_helper(node, name, rc)
    return rc


def _get_elements_by_tagName_ns_helper(parent, nsURI, localName, rc):
    for node in parent.childNodes:
        if node.nodeType == Node.ELEMENT_NODE:
            if (localName == "*" or node.localName == localName) and (
                nsURI == "*" or node.namespaceURI == nsURI
            ):
                rc.append(node)
            _get_elements_by_tagName_ns_helper(node, nsURI, localName, rc)
    return rc



def _clone_node(node, deep, newOwnerDocument):
    """
    Clone a node and give it the new owner document.
    Called by Node.cloneNode and Document.importNode
    """
    if node.ownerDocument.isSameNode(newOwnerDocument):
        operation = dom.UserDataHandler.NODE_CLONED
    else:
        operation = dom.UserDataHandler.NODE_IMPORTED
    if node.nodeType == Node.ELEMENT_NODE:
        clone = newOwnerDocument.createElementNS(node.namespaceURI, node.nodeName)
        for attr in node.attributes.values():
            clone.setAttributeNS(attr.namespaceURI, attr.nodeName, attr.value)
            a = clone.getAttributeNodeNS(attr.namespaceURI, attr.localName)
            a.specified = attr.specified

        if deep:
            for child in node.childNodes:
                c = _clone_node(child, deep, newOwnerDocument)
                clone.appendChild(c)

    elif node.nodeType == Node.DOCUMENT_FRAGMENT_NODE:
        clone = newOwnerDocument.createDocumentFragment()
        if deep:
            for child in node.childNodes:
                c = _clone_node(child, deep, newOwnerDocument)
                clone.appendChild(c)

    elif node.nodeType == Node.TEXT_NODE:
        clone = newOwnerDocument.createTextNode(node.data)
    elif node.nodeType == Node.CDATA_SECTION_NODE:
        clone = newOwnerDocument.createCDATASection(node.data)
    elif node.nodeType == Node.PROCESSING_INSTRUCTION_NODE:
        clone = newOwnerDocument.createProcessingInstruction(node.target, node.data)
    elif node.nodeType == Node.COMMENT_NODE:
        clone = newOwnerDocument.createComment(node.data)
    elif node.nodeType == Node.ATTRIBUTE_NODE:
        clone = newOwnerDocument.createAttributeNS(node.namespaceURI, node.nodeName)
        clone.specified = True
        clone.value = node.value
    elif node.nodeType == Node.DOCUMENT_TYPE_NODE:
        if not (node.ownerDocument != newOwnerDocument):
            fail("assert node.ownerDocument != newOwnerDocument failed!")
        operation = dom.UserDataHandler.NODE_IMPORTED
        clone = newOwnerDocument.implementation.createDocumentType(
            node.name, node.publicId, node.systemId
        )
        clone.ownerDocument = newOwnerDocument
        if deep:
            clone.entities._seq = []
            clone.notations._seq = []
            for n in node.notations._seq:
                notation = Notation(n.nodeName, n.publicId, n.systemId)
                notation.ownerDocument = newOwnerDocument
                clone.notations._seq.append(notation)
                if hasattr(n, "_call_user_data_handler"):
                    n._call_user_data_handler(operation, n, notation)
            for e in node.entities._seq:
                entity = Entity(e.nodeName, e.publicId, e.systemId, e.notationName)
                entity.actualEncoding = e.actualEncoding
                entity.encoding = e.encoding
                entity.version = e.version
                entity.ownerDocument = newOwnerDocument
                clone.entities._seq.append(entity)
                if hasattr(e, "_call_user_data_handler"):
                    e._call_user_data_handler(operation, e, entity)
    else:
        # Note the cloning of Document and DocumentType nodes is
        # implementation specific.  minidom handles those cases
        # directly in the cloneNode() methods.
        dom.NotSupportedErr("Cannot clone node %s" % repr(node))

    # Check for _call_user_data_handler() since this could conceivably
    # used with other DOM implementations (one of the FourThought
    # DOMs, perhaps?).
    if hasattr(node, "_call_user_data_handler"):
        node._call_user_data_handler(operation, node, clone)
    return clone


def _clear_id_cache(node):
    if node.nodeType == Node.DOCUMENT_NODE:
        node._id_cache.clear()
        node._id_search_stack = None
    elif _in_document(node):
        node.ownerDocument._id_cache.clear()
        node.ownerDocument._id_search_stack = None


def Node():
    self = larky.mutablestruct(__name__='Node', __class__=Node)
    self.namespaceURI = None  # this is non-null only for elements and attributes
    self.parentNode = None
    self.ownerDocument = None
    self.nextSibling = None
    self.previousSibling = None

    self.prefix = EMPTY_PREFIX  # non-null only for NS elements and attributes

    def __bool__():
        return True
    self.__bool__ = __bool__

    def toxml(encoding=None):
        return self.toprettyxml("", "", encoding)
    self.toxml = toxml

    def toprettyxml(indent="\t", newl="\n", encoding=None):
        if encoding == None:
            writer = io.StringIO()
        else:
            writer = io.TextIOWrapper(
                io.BytesIO(),
                encoding=encoding,
                errors="xmlcharrefreplace",
                newline="\n",
            )
        if self.nodeType == Node.DOCUMENT_NODE:
            # Can pass encoding only to document, to put it into XML header
            self.writexml(writer, "", indent, newl, encoding)
        else:
            self.writexml(writer, "", indent, newl)
        if encoding == None:
            return writer.getvalue()
        else:
            return writer.detach().getvalue()
    self.toprettyxml = toprettyxml

    def hasChildNodes():
        return bool(self.childNodes)
    self.hasChildNodes = hasChildNodes

    def _get_childNodes():
        return self.childNodes
    self._get_childNodes = _get_childNodes

    def _get_firstChild():
        if self.childNodes:
            return self.childNodes[0]
    self._get_firstChild = _get_firstChild

    def _get_lastChild():
        if self.childNodes:
            return self.childNodes[-1]
    self._get_lastChild = _get_lastChild

    def insertBefore(newChild, refChild):
        if newChild.nodeType == self.DOCUMENT_FRAGMENT_NODE:
            for c in tuple(newChild.childNodes):
                self.insertBefore(c, refChild)
            ### The DOM does not clearly specify what to return in this case
            return newChild
        if newChild.nodeType not in self._child_node_types:
            dom.HierarchyRequestErr(
                "%s cannot be child of %s" % (repr(newChild), repr(self))
            )
        if newChild.parentNode != None:
            newChild.parentNode.removeChild(newChild)
        if refChild == None:
            self.appendChild(newChild)
        else:
            res = Result.Ok(refChild).map(self.childNodes.index)
            index = res.unwrap_or_else(dom.NotFoundErr)
            if newChild.nodeType in _nodeTypes_with_children:
                _clear_id_cache(self)
            self.childNodes.insert(index, newChild)
            newChild.nextSibling = refChild
            refChild.previousSibling = newChild
            if index:
                node = self.childNodes[index - 1]
                node.nextSibling = newChild
                newChild.previousSibling = node
            else:
                newChild.previousSibling = None
            newChild.parentNode = self
        return newChild
    self.insertBefore = insertBefore

    def appendChild(node):
        if node.nodeType == self.DOCUMENT_FRAGMENT_NODE:
            for c in tuple(node.childNodes):
                self.appendChild(c)
            ### The DOM does not clearly specify what to return in this case
            return node
        if node.nodeType not in self._child_node_types:
            dom.HierarchyRequestErr(
                "%s cannot be child of %s" % (repr(node), repr(self))
            )
        elif node.nodeType in _nodeTypes_with_children:
            _clear_id_cache(self)
        if node.parentNode != None:
            node.parentNode.removeChild(node)
        _append_child(self, node)
        node.nextSibling = None
        return node
    self.appendChild = appendChild

    def replaceChild(newChild, oldChild):
        if newChild.nodeType == self.DOCUMENT_FRAGMENT_NODE:
            refChild = oldChild.nextSibling
            self.removeChild(oldChild)
            return self.insertBefore(newChild, refChild)
        if newChild.nodeType not in self._child_node_types:
            dom.HierarchyRequestErr(
                "%s cannot be child of %s" % (repr(newChild), repr(self))
            )
        if newChild == oldChild:
            return
        if newChild.parentNode != None:
            newChild.parentNode.removeChild(newChild)
        res = Result.Ok(oldChild).map(self.childNodes.index)
        index = res.unwrap_or_else(dom.NotFoundErr)
        self.childNodes[index] = newChild
        newChild.parentNode = self
        oldChild.parentNode = None
        if (
            newChild.nodeType in _nodeTypes_with_children
            or oldChild.nodeType in _nodeTypes_with_children
        ):
            _clear_id_cache(self)
        newChild.nextSibling = oldChild.nextSibling
        newChild.previousSibling = oldChild.previousSibling
        oldChild.nextSibling = None
        oldChild.previousSibling = None
        if newChild.previousSibling:
            newChild.previousSibling.nextSibling = newChild
        if newChild.nextSibling:
            newChild.nextSibling.previousSibling = newChild
        return oldChild
    self.replaceChild = replaceChild

    def removeChild(oldChild):
        res = Result.Ok(oldChild).map(self.childNodes.remove)
        res.unwrap_or_else(dom.NotFoundErr)
        if oldChild.nextSibling != None:
            oldChild.nextSibling.previousSibling = oldChild.previousSibling
        if oldChild.previousSibling != None:
            oldChild.previousSibling.nextSibling = oldChild.nextSibling
        oldChild.nextSibling = None
        oldChild.previousSibling = oldChild.nextSibling
        if oldChild.nodeType in _nodeTypes_with_children:
            _clear_id_cache(self)

        oldChild.parentNode = None
        return oldChild
    self.removeChild = removeChild

    def normalize():
        L = []
        for child in self.childNodes:
            if child.nodeType == Node.TEXT_NODE:
                if not child.data:
                    # empty text node; discard
                    if L:
                        L[-1].nextSibling = child.nextSibling
                    if child.nextSibling:
                        child.nextSibling.previousSibling = child.previousSibling
                    child.unlink()
                elif L and L[-1].nodeType == child.nodeType:
                    # collapse text node
                    node = L[-1]
                    node.data = node.data + child.data
                    node.nextSibling = child.nextSibling
                    if child.nextSibling:
                        child.nextSibling.previousSibling = node
                    child.unlink()
                else:
                    L.append(child)
            else:
                L.append(child)
                if child.nodeType == Node.ELEMENT_NODE:
                    child.normalize()
        self.childNodes.clear()
        self.childNodes.extend(L)
    self.normalize = normalize

    def cloneNode(deep):
        return _clone_node(self, deep, self.ownerDocument or self)
    self.cloneNode = cloneNode

    def isSupported(feature, version):
        return self.ownerDocument.implementation.hasFeature(feature, version)
    self.isSupported = isSupported

    def _get_localName():
        # Overridden in Element and Attr where localName can be Non-Null
        return None
    self._get_localName = _get_localName

    # Node interfaces from Level 3 (WD 9 April 2002)

    def isSameNode(other):
        return self == other
    self.isSameNode = isSameNode

    def getInterface(feature):
        if self.isSupported(feature, None):
            return self
        else:
            return None
    self.getInterface = getInterface

    # The "user data" functions use a dictionary that is only present
    # if some user data has been set, so be careful not to assume it
    # exists.

    def getUserData(key):
        res = (
            Result.Ok(self._user_data)
                .map(operator.itemgetter(key))
                .map(operator.itemgetter(0))
        )
        return res.unwrap_or(None)
    self.getUserData = getUserData

    def setUserData(key, data, handler):
        old = None
        d = getattr(self, "_user_data", {})
        self._user_data = d
        if key in d:
            old = d[key][0]
        if data == None:
            # ignore handlers passed for None
            handler = None
            if old != None:
                operator.delitem(d, key)
        else:
            d[key] = (data, handler)
        return old
    self.setUserData = setUserData

    def _call_user_data_handler(operation, src, dst):
        if hasattr(self, "_user_data"):
            for key, (data, handler) in list(self._user_data.items()):
                if handler != None:
                    handler.handle(operation, key, data, src, dst)
    self._call_user_data_handler = _call_user_data_handler

    # minidom-specific API:

    def unlink():
        self.parentNode = None
        self.ownerDocument = self.parentNode
        if self.childNodes:
            for child in self.childNodes:
                child.unlink()
            self.childNodes = NodeList()
        self.previousSibling = None
        self.nextSibling = None
    self.unlink = unlink

    # A Node is its own context manager, to ensure that an unlink() call occurs.
    # This is similar to how a file object works.
    def __enter__():
        return self
    self.__enter__ = __enter__

    def __exit__(et, ev, tb):
        self.unlink()
    self.__exit__ = __exit__

    "First child node, or None."
    self.firstChild = larky.property(self._get_firstChild)
    "Last child node, or None."
    self.lastChild = larky.property(self._get_lastChild)
    "Namespace-local name of this node."
    self.localName = larky.property(self._get_localName)
    return self
#
#
# def DocumentFragment():
#     nodeType = Node.DOCUMENT_FRAGMENT_NODE
#     nodeName = "#document-fragment"
#     nodeValue = None
#     attributes = None
#     parentNode = None
#     _child_node_types = (
#         Node.ELEMENT_NODE,
#         Node.TEXT_NODE,
#         Node.CDATA_SECTION_NODE,
#         Node.ENTITY_REFERENCE_NODE,
#         Node.PROCESSING_INSTRUCTION_NODE,
#         Node.COMMENT_NODE,
#         Node.NOTATION_NODE,
#     )
#     self = larky.mutablestruct(__name__='DocumentFragment', __class__=DocumentFragment)
#
#     def __init__():
#         self.childNodes = NodeList()
#         return self
#     self = __init__()
#     return self
#
#
# def Attr(qName, namespaceURI=EMPTY_NAMESPACE, localName=None, prefix=None
# ):
#     __slots__ = (
#         "_name",
#         "_value",
#         "namespaceURI",
#         "_prefix",
#         "childNodes",
#         "_localName",
#         "ownerDocument",
#         "ownerElement",
#     )
#     nodeType = Node.ATTRIBUTE_NODE
#     attributes = None
#     specified = False
#     _is_id = False
#
#     _child_node_types = (Node.TEXT_NODE, Node.ENTITY_REFERENCE_NODE)
#     self = larky.mutablestruct(__name__='Attr', __class__=Attr)
#
#     def __init__(
#         qName, namespaceURI, localName, prefix
#     ):
#         self.ownerElement = None
#         self._name = qName
#         self.namespaceURI = namespaceURI
#         self._prefix = prefix
#         self.childNodes = NodeList()
#
#         # Add the single child node that represents the value of the attr
#         self.childNodes.append(Text())
#         return self
#
#         # nodeValue and value are set elsewhere
#     self = __init__(qName, namespaceURI, localName, prefix)
#
#     def _get_localName():
#         try:
#             return self._localName
#         except AttributeError:
#             return self.nodeName.split(":", 1)[-1]
#     self._get_localName = _get_localName
#
#     def _get_specified():
#         return self.specified
#     self._get_specified = _get_specified
#
#     def _get_name():
#         return self._name
#     self._get_name = _get_name
#
#     def _set_name(value):
#         self._name = value
#         if self.ownerElement != None:
#             _clear_id_cache(self.ownerElement)
#     self._set_name = _set_name
#     nodeName = property(_get_name, _set_name)
#     name = nodeName
#
#     def _get_value():
#         return self._value
#     self._get_value = _get_value
#
#     def _set_value(value):
#         self._value = value
#         self.childNodes[0].data = value
#         if self.ownerElement != None:
#             _clear_id_cache(self.ownerElement)
#         self.childNodes[0].data = value
#     self._set_value = _set_value
#     nodeValue = property(_get_value, _set_value)
#     value = nodeValue
#
#     def _get_prefix():
#         return self._prefix
#     self._get_prefix = _get_prefix
#
#     def _set_prefix(prefix):
#         nsuri = self.namespaceURI
#         if prefix == "xmlns":
#             if nsuri and nsuri != XMLNS_NAMESPACE:
#                 dom.NamespaceErr(
#                     "illegal use of 'xmlns' prefix for the wrong namespace"
#                 )
#         self._prefix = prefix
#         if prefix == None:
#             newName = self.localName
#         else:
#             newName = "%s:%s" % (prefix, self.localName)
#         if self.ownerElement:
#             _clear_id_cache(self.ownerElement)
#         self.name = newName
#     self._set_prefix = _set_prefix
#
#     prefix = property(_get_prefix, _set_prefix)
#
#     def unlink():
#         # This implementation does not call the base implementation
#         # since most of that is not needed, and the expense of the
#         # method call is not warranted.  We duplicate the removal of
#         # children, but that's all we needed from the base class.
#         elem = self.ownerElement
#         if elem != None:
#             operator.delitem(elem._attrs, self.nodeName)
#             operator.delitem(elem._attrsNS, (self.namespaceURI, self.localName))
#             if self._is_id:
#                 self._is_id = False
#                 elem._magic_id_nodes -= 1
#                 self.ownerDocument._magic_id_count -= 1
#         for child in self.childNodes:
#             child.unlink()
#         self.childNodes.clear()
#     self.unlink = unlink
#
#     def _get_isId():
#         if self._is_id:
#             return True
#         doc = self.ownerDocument
#         elem = self.ownerElement
#         if doc == None or elem == None:
#             return False
#
#         info = doc._get_elem_info(elem)
#         if info == None:
#             return False
#         if self.namespaceURI:
#             return info.isIdNS(self.namespaceURI, self.localName)
#         else:
#             return info.isId(self.nodeName)
#     self._get_isId = _get_isId
#
#     def _get_schemaType():
#         doc = self.ownerDocument
#         elem = self.ownerElement
#         if doc == None or elem == None:
#             return _no_type
#
#         info = doc._get_elem_info(elem)
#         if info == None:
#             return _no_type
#         if self.namespaceURI:
#             return info.getAttributeTypeNS(self.namespaceURI, self.localName)
#         else:
#             return info.getAttributeType(self.nodeName)
#     self._get_schemaType = _get_schemaType
#     return self
#
#
# defproperty(Attr, "isId", doc="True if this attribute is an ID.")
# defproperty(Attr, "localName", doc="Namespace-local name of this attribute.")
# defproperty(Attr, "schemaType", doc="Schema type for this attribute.")
# def NamedNodeMap(attrs, attrsNS, ownerElement):
#     """The attribute list is a transient interface to the underlying
#     dictionaries.  Mutations here will change the underlying element's
#     dictionary.
#
#     Ordering is imposed artificially and does not reflect the order of
#     attributes as found in an input document.
#     """
#
#     __slots__ = ("_attrs", "_attrsNS", "_ownerElement")
#     self = larky.mutablestruct(__name__='NamedNodeMap', __class__=NamedNodeMap)
#
#     def __init__(attrs, attrsNS, ownerElement):
#         self._attrs = attrs
#         self._attrsNS = attrsNS
#         self._ownerElement = ownerElement
#         return self
#     self = __init__(attrs, attrsNS, ownerElement)
#
#     def _get_length():
#         return len(self._attrs)
#     self._get_length = _get_length
#
#     def item(index):
#         try:
#             return self[list(self._attrs.keys())[index]]
#         except IndexError:
#             return None
#     self.item = item
#
#     def items():
#         L = []
#         for node in self._attrs.values():
#             L.append((node.nodeName, node.value))
#         return L
#     self.items = items
#
#     def itemsNS():
#         L = []
#         for node in self._attrs.values():
#             L.append(((node.namespaceURI, node.localName), node.value))
#         return L
#     self.itemsNS = itemsNS
#
#     def __contains__(key):
#         if builtins.isinstance(key, str):
#             return key in self._attrs
#         else:
#             return key in self._attrsNS
#     self.__contains__ = __contains__
#
#     def keys():
#         return self._attrs.keys()
#     self.keys = keys
#
#     def keysNS():
#         return self._attrsNS.keys()
#     self.keysNS = keysNS
#
#     def values():
#         return self._attrs.values()
#     self.values = values
#
#     def get(name, value=None):
#         return self._attrs.get(name, value)
#     self.get = get
#
#     __len__ = _get_length
#
#     def _cmp(other):
#         if self._attrs == getattr(other, "_attrs", None):
#             return 0
#         else:
#             return (id(self) > id(other)) - (id(self) < id(other))
#     self._cmp = _cmp
#
#     def __eq__(other):
#         return self._cmp(other) == 0
#     self.__eq__ = __eq__
#
#     def __ge__(other):
#         return self._cmp(other) >= 0
#     self.__ge__ = __ge__
#
#     def __gt__(other):
#         return self._cmp(other) > 0
#     self.__gt__ = __gt__
#
#     def __le__(other):
#         return self._cmp(other) <= 0
#     self.__le__ = __le__
#
#     def __lt__(other):
#         return self._cmp(other) < 0
#     self.__lt__ = __lt__
#
#     def __getitem__(attname_or_tuple):
#         if builtins.isinstance(attname_or_tuple, tuple):
#             return self._attrsNS[attname_or_tuple]
#         else:
#             return self._attrs[attname_or_tuple]
#     self.__getitem__ = __getitem__
#
#     # same as set
#     def __setitem__(attname, value):
#         if builtins.isinstance(value, str):
#             try:
#                 node = self._attrs[attname]
#             except KeyError:
#                 node = Attr(attname)
#                 node.ownerDocument = self._ownerElement.ownerDocument
#                 self.setNamedItem(node)
#             node.value = value
#         else:
#             if not builtins.isinstance(value, Attr):
#                 fail("TypeError: value must be a string or Attr object")
#             node = value
#             self.setNamedItem(node)
#     self.__setitem__ = __setitem__
#
#     def getNamedItem(name):
#         try:
#             return self._attrs[name]
#         except KeyError:
#             return None
#     self.getNamedItem = getNamedItem
#
#     def getNamedItemNS(namespaceURI, localName):
#         try:
#             return self._attrsNS[(namespaceURI, localName)]
#         except KeyError:
#             return None
#     self.getNamedItemNS = getNamedItemNS
#
#     def removeNamedItem(name):
#         n = self.getNamedItem(name)
#         if n != None:
#             _clear_id_cache(self._ownerElement)
#             operator.delitem(self._attrs, n.nodeName)
#             operator.delitem(self._attrsNS, (n.namespaceURI, n.localName))
#             if hasattr(n, "ownerElement"):
#                 n.ownerElement = None
#             return n
#         else:
#             dom.NotFoundErr()
#     self.removeNamedItem = removeNamedItem
#
#     def removeNamedItemNS(namespaceURI, localName):
#         n = self.getNamedItemNS(namespaceURI, localName)
#         if n != None:
#             _clear_id_cache(self._ownerElement)
#             operator.delitem(self._attrsNS, (n.namespaceURI, n.localName))
#             operator.delitem(self._attrs, n.nodeName)
#             if hasattr(n, "ownerElement"):
#                 n.ownerElement = None
#             return n
#         else:
#             dom.NotFoundErr()
#     self.removeNamedItemNS = removeNamedItemNS
#
#     def setNamedItem(node):
#         if not builtins.isinstance(node, Attr):
#             dom.HierarchyRequestErr(
#                 "%s cannot be child of %s" % (repr(node), repr(self))
#             )
#         old = self._attrs.get(node.name)
#         if old:
#             old.unlink()
#         self._attrs[node.name] = node
#         self._attrsNS[(node.namespaceURI, node.localName)] = node
#         node.ownerElement = self._ownerElement
#         _clear_id_cache(node.ownerElement)
#         return old
#     self.setNamedItem = setNamedItem
#
#     def setNamedItemNS(node):
#         return self.setNamedItem(node)
#     self.setNamedItemNS = setNamedItemNS
#
#     def __delitem__(attname_or_tuple):
#         node = self[attname_or_tuple]
#         _clear_id_cache(node.ownerElement)
#         node.unlink()
#     self.__delitem__ = __delitem__
#
#     def __getstate__():
#         return self._attrs, self._attrsNS, self._ownerElement
#     self.__getstate__ = __getstate__
#
#     def __setstate__(state):
#         self._attrs, self._attrsNS, self._ownerElement = state
#     self.__setstate__ = __setstate__
#     return self
#
#
# defproperty(NamedNodeMap, "length", doc="Number of nodes in the NamedNodeMap.")
#
# AttributeList = NamedNodeMap
# def TypeInfo(namespace, name):
#     __slots__ = "namespace", "name"
#     self = larky.mutablestruct(__name__='TypeInfo', __class__=TypeInfo)
#
#     def __init__(namespace, name):
#         self.namespace = namespace
#         self.name = name
#         return self
#     self = __init__(namespace, name)
#
#     def __repr__():
#         if self.namespace:
#             return "<%s %r (from %r)>" % (
#                 self.__class__.__name__,
#                 self.name,
#                 self.namespace,
#             )
#         else:
#             return "<%s %r>" % (self.__class__.__name__, self.name)
#     self.__repr__ = __repr__
#
#     def _get_name():
#         return self.name
#     self._get_name = _get_name
#
#     def _get_namespace():
#         return self.namespace
#     self._get_namespace = _get_namespace
#     return self
#
#
# _no_type = TypeInfo(None, None)
# def Element(tagName, namespaceURI=EMPTY_NAMESPACE, prefix=None, localName=None
# ):
#     __slots__ = (
#         "ownerDocument",
#         "parentNode",
#         "tagName",
#         "nodeName",
#         "prefix",
#         "namespaceURI",
#         "_localName",
#         "childNodes",
#         "_attrs",
#         "_attrsNS",
#         "nextSibling",
#         "previousSibling",
#     )
#     nodeType = Node.ELEMENT_NODE
#     nodeValue = None
#     schemaType = _no_type
#
#     _magic_id_nodes = 0
#
#     _child_node_types = (
#         Node.ELEMENT_NODE,
#         Node.PROCESSING_INSTRUCTION_NODE,
#         Node.COMMENT_NODE,
#         Node.TEXT_NODE,
#         Node.CDATA_SECTION_NODE,
#         Node.ENTITY_REFERENCE_NODE,
#     )
#     self = larky.mutablestruct(__name__='Element', __class__=Element)
#
#     def __init__(
#         tagName, namespaceURI, prefix, localName
#     ):
#         self.parentNode = None
#         self.tagName = tagName
#         self.nodeName = self.tagName
#         self.prefix = prefix
#         self.namespaceURI = namespaceURI
#         self.childNodes = NodeList()
#         self.nextSibling = None
#         self.previousSibling = self.nextSibling
#
#         # Attribute dictionaries are lazily created
#         # attributes are double-indexed:
#         #    tagName -> Attribute
#         #    URI,localName -> Attribute
#         # in the future: consider lazy generation
#         # of attribute objects this is too tricky
#         # for now because of headaches with
#         # namespaces.
#         self._attrs = None
#         self._attrsNS = None
#         return self
#     self = __init__(tagName, namespaceURI, prefix, localName)
#
#     def _ensure_attributes():
#         if self._attrs == None:
#             self._attrs = {}
#             self._attrsNS = {}
#     self._ensure_attributes = _ensure_attributes
#
#     def _get_localName():
#         try:
#             return self._localName
#         except AttributeError:
#             return self.tagName.split(":", 1)[-1]
#     self._get_localName = _get_localName
#
#     def _get_tagName():
#         return self.tagName
#     self._get_tagName = _get_tagName
#
#     def unlink():
#         if self._attrs != None:
#             for attr in list(self._attrs.values()):
#                 attr.unlink()
#         self._attrs = None
#         self._attrsNS = None
#         Node.unlink(self)
#     self.unlink = unlink
#
#     def getAttribute(attname):
#         if self._attrs == None:
#             return ""
#         try:
#             return self._attrs[attname].value
#         except KeyError:
#             return ""
#     self.getAttribute = getAttribute
#
#     def getAttributeNS(namespaceURI, localName):
#         if self._attrsNS == None:
#             return ""
#         try:
#             return self._attrsNS[(namespaceURI, localName)].value
#         except KeyError:
#             return ""
#     self.getAttributeNS = getAttributeNS
#
#     def setAttribute(attname, value):
#         attr = self.getAttributeNode(attname)
#         if attr == None:
#             attr = Attr(attname)
#             attr.value = value  # also sets nodeValue
#             attr.ownerDocument = self.ownerDocument
#             self.setAttributeNode(attr)
#         elif value != attr.value:
#             attr.value = value
#             if attr.isId:
#                 _clear_id_cache(self)
#     self.setAttribute = setAttribute
#
#     def setAttributeNS(namespaceURI, qualifiedName, value):
#         prefix, localname = _nssplit(qualifiedName)
#         attr = self.getAttributeNodeNS(namespaceURI, localname)
#         if attr == None:
#             attr = Attr(qualifiedName, namespaceURI, localname, prefix)
#             attr.value = value
#             attr.ownerDocument = self.ownerDocument
#             self.setAttributeNode(attr)
#         else:
#             if value != attr.value:
#                 attr.value = value
#                 if attr.isId:
#                     _clear_id_cache(self)
#             if attr.prefix != prefix:
#                 attr.prefix = prefix
#                 attr.nodeName = qualifiedName
#     self.setAttributeNS = setAttributeNS
#
#     def getAttributeNode(attrname):
#         if self._attrs == None:
#             return None
#         return self._attrs.get(attrname)
#     self.getAttributeNode = getAttributeNode
#
#     def getAttributeNodeNS(namespaceURI, localName):
#         if self._attrsNS == None:
#             return None
#         return self._attrsNS.get((namespaceURI, localName))
#     self.getAttributeNodeNS = getAttributeNodeNS
#
#     def setAttributeNode(attr):
#         if attr.ownerElement not in (None, self):
#             dom.InuseAttributeErr("attribute node already owned")
#         self._ensure_attributes()
#         old1 = self._attrs.get(attr.name, None)
#         if old1 != None:
#             self.removeAttributeNode(old1)
#         old2 = self._attrsNS.get((attr.namespaceURI, attr.localName), None)
#         if old2 != None and old2 != old1:
#             self.removeAttributeNode(old2)
#         _set_attribute_node(self, attr)
#
#         if old1 != attr:
#             # It might have already been part of this node, in which case
#             # it doesn't represent a change, and should not be returned.
#             return old1
#         if old2 != attr:
#             return old2
#     self.setAttributeNode = setAttributeNode
#
#     setAttributeNodeNS = setAttributeNode
#
#     def removeAttribute(name):
#         if self._attrsNS == None:
#             dom.NotFoundErr()
#         try:
#             attr = self._attrs[name]
#         except KeyError:
#             dom.NotFoundErr()
#         self.removeAttributeNode(attr)
#     self.removeAttribute = removeAttribute
#
#     def removeAttributeNS(namespaceURI, localName):
#         if self._attrsNS == None:
#             dom.NotFoundErr()
#         try:
#             attr = self._attrsNS[(namespaceURI, localName)]
#         except KeyError:
#             dom.NotFoundErr()
#         self.removeAttributeNode(attr)
#     self.removeAttributeNS = removeAttributeNS
#
#     def removeAttributeNode(node):
#         if node == None:
#             dom.NotFoundErr()
#         try:
#             self._attrs[node.name]
#         except KeyError:
#             dom.NotFoundErr()
#         _clear_id_cache(self)
#         node.unlink()
#         # Restore this since the node is still useful and otherwise
#         # unlinked
#         node.ownerDocument = self.ownerDocument
#         return node
#     self.removeAttributeNode = removeAttributeNode
#
#     removeAttributeNodeNS = removeAttributeNode
#
#     def hasAttribute(name):
#         if self._attrs == None:
#             return False
#         return name in self._attrs
#     self.hasAttribute = hasAttribute
#
#     def hasAttributeNS(namespaceURI, localName):
#         if self._attrsNS == None:
#             return False
#         return (namespaceURI, localName) in self._attrsNS
#     self.hasAttributeNS = hasAttributeNS
#
#     def getElementsByTagName(name):
#         return _get_elements_by_tagName_helper(self, name, NodeList())
#     self.getElementsByTagName = getElementsByTagName
#
#     def getElementsByTagNameNS(namespaceURI, localName):
#         return _get_elements_by_tagName_ns_helper(
#             self, namespaceURI, localName, NodeList()
#         )
#     self.getElementsByTagNameNS = getElementsByTagNameNS
#
#     def __repr__():
#         return "<DOM Element: %s at %#x>" % (self.tagName, id(self))
#     self.__repr__ = __repr__
#
#     def writexml(writer, indent="", addindent="", newl=""):
#         # indent = current indentation
#         # addindent = indentation to add to higher levels
#         # newl = newline string
#         writer.write(indent + "<" + self.tagName)
#
#         attrs = self._get_attributes()
#
#         for a_name in attrs.keys():
#             writer.write(' %s="' % a_name)
#             _write_data(writer, attrs[a_name].value)
#             writer.write('"')
#         if self.childNodes:
#             writer.write(">")
#             if len(self.childNodes) == 1 and self.childNodes[0].nodeType in (
#                 Node.TEXT_NODE,
#                 Node.CDATA_SECTION_NODE,
#             ):
#                 self.childNodes[0].writexml(writer, "", "", "")
#             else:
#                 writer.write(newl)
#                 for node in self.childNodes:
#                     node.writexml(writer, indent + addindent, addindent, newl)
#                 writer.write(indent)
#             writer.write("</%s>%s" % (self.tagName, newl))
#         else:
#             writer.write("/>%s" % (newl))
#     self.writexml = writexml
#
#     def _get_attributes():
#         self._ensure_attributes()
#         return NamedNodeMap(self._attrs, self._attrsNS, self)
#     self._get_attributes = _get_attributes
#
#     def hasAttributes():
#         if self._attrs:
#             return True
#         else:
#             return False
#     self.hasAttributes = hasAttributes
#
#     # DOM Level 3 attributes, based on the 22 Oct 2002 draft
#
#     def setIdAttribute(name):
#         idAttr = self.getAttributeNode(name)
#         self.setIdAttributeNode(idAttr)
#     self.setIdAttribute = setIdAttribute
#
#     def setIdAttributeNS(namespaceURI, localName):
#         idAttr = self.getAttributeNodeNS(namespaceURI, localName)
#         self.setIdAttributeNode(idAttr)
#     self.setIdAttributeNS = setIdAttributeNS
#
#     def setIdAttributeNode(idAttr):
#         if idAttr == None or not self.isSameNode(idAttr.ownerElement):
#             dom.NotFoundErr()
#         if _get_containing_entref(self) != None:
#             dom.NoModificationAllowedErr()
#         if not idAttr._is_id:
#             idAttr._is_id = True
#             self._magic_id_nodes += 1
#             self.ownerDocument._magic_id_count += 1
#             _clear_id_cache(self)
#     self.setIdAttributeNode = setIdAttributeNode
#     return self
#
#
# defproperty(Element, "attributes", doc="NamedNodeMap of attributes on the element.")
# defproperty(Element, "localName", doc="Namespace-local name of this element.")
#
#
# def _set_attribute_node(element, attr):
#     _clear_id_cache(element)
#     element._ensure_attributes()
#     element._attrs[attr.name] = attr
#     element._attrsNS[(attr.namespaceURI, attr.localName)] = attr
#
#     # This creates a circular reference, but Element.unlink()
#     # breaks the cycle since the references to the attribute
#     # dictionaries are tossed.
#     attr.ownerElement = element
# def Childless(tagName, namespaceURI=EMPTY_NAMESPACE, prefix=None, localName=None
# ):
#     """Mixin that makes childless-ness easy to implement and avoids
#     the complexity of the Node methods that deal with children.
#     """
#
#     __slots__ = ()
#
#     attributes = None
#     childNodes = EmptyNodeList()
#     firstChild = None
#     lastChild = None
#
#     def _get_firstChild():
#         return None
#     self._get_firstChild = _get_firstChild
#
#     def _get_lastChild():
#         return None
#     self._get_lastChild = _get_lastChild
#
#     def appendChild(node):
#         dom.HierarchyRequestErr(self.nodeName + " nodes cannot have children")
#     self.appendChild = appendChild
#
#     def hasChildNodes():
#         return False
#     self.hasChildNodes = hasChildNodes
#
#     def insertBefore(newChild, refChild):
#         dom.HierarchyRequestErr(self.nodeName + " nodes do not have children")
#     self.insertBefore = insertBefore
#
#     def removeChild(oldChild):
#         dom.NotFoundErr(self.nodeName + " nodes do not have children")
#     self.removeChild = removeChild
#
#     def normalize():
#         # For childless nodes, normalize() has nothing to do.
#         pass
#     self.normalize = normalize
#
#     def replaceChild(newChild, oldChild):
#         dom.HierarchyRequestErr(self.nodeName + " nodes do not have children")
#     self.replaceChild = replaceChild
#     return self
# def ProcessingInstruction(target, data):
#     nodeType = Node.PROCESSING_INSTRUCTION_NODE
#     __slots__ = ("target", "data")
#     self = larky.mutablestruct(__name__='ProcessingInstruction', __class__=ProcessingInstruction)
#
#     def __init__(target, data):
#         self.target = target
#         self.data = data
#         return self
#     self = __init__(target, data)
#
#     # nodeValue is an alias for data
#     def _get_nodeValue():
#         return self.data
#     self._get_nodeValue = _get_nodeValue
#
#     def _set_nodeValue(value):
#         self.data = value
#     self._set_nodeValue = _set_nodeValue
#
#     nodeValue = property(_get_nodeValue, _set_nodeValue)
#
#     # nodeName is an alias for target
#     def _get_nodeName():
#         return self.target
#     self._get_nodeName = _get_nodeName
#
#     def _set_nodeName(value):
#         self.target = value
#     self._set_nodeName = _set_nodeName
#
#     nodeName = property(_get_nodeName, _set_nodeName)
#
#     def writexml(writer, indent="", addindent="", newl=""):
#         writer.write("%s<?%s %s?>%s" % (indent, self.target, self.data, newl))
#     self.writexml = writexml
#     return self
# def CharacterData():
#     __slots__ = (
#         "_data",
#         "ownerDocument",
#         "parentNode",
#         "previousSibling",
#         "nextSibling",
#     )
#     self = larky.mutablestruct(__name__='CharacterData', __class__=CharacterData)
#
#     def __init__():
#         self.ownerDocument = None
#         self.parentNode = self.ownerDocument
#         self.previousSibling = None
#         self.nextSibling = self.previousSibling
#         self._data = ""
#         Node.__init__(self)
#         return self
#     self = __init__()
#
#     def _get_length():
#         return len(self.data)
#     self._get_length = _get_length
#
#     __len__ = _get_length
#
#     def _get_data():
#         return self._data
#     self._get_data = _get_data
#
#     def _set_data(data):
#         self._data = data
#     self._set_data = _set_data
#     data = property(_get_data, _set_data)
#     nodeValue = data
#
#     def __repr__():
#         data = self.data
#         if len(data) > 10:
#             dotdotdot = "..."
#         else:
#             dotdotdot = ""
#         return '<DOM %s node "%r%s">' % (self.__class__.__name__, data[0:10], dotdotdot)
#     self.__repr__ = __repr__
#
#     def substringData(offset, count):
#         if offset < 0:
#             dom.IndexSizeErr("offset cannot be negative")
#         if offset >= len(self.data):
#             dom.IndexSizeErr("offset cannot be beyond end of data")
#         if count < 0:
#             dom.IndexSizeErr("count cannot be negative")
#         return self.data[offset : offset + count]
#     self.substringData = substringData
#
#     def appendData(arg):
#         self.data = self.data + arg
#     self.appendData = appendData
#
#     def insertData(offset, arg):
#         if offset < 0:
#             dom.IndexSizeErr("offset cannot be negative")
#         if offset >= len(self.data):
#             dom.IndexSizeErr("offset cannot be beyond end of data")
#         if arg:
#             self.data = "%s%s%s" % (self.data[:offset], arg, self.data[offset:])
#     self.insertData = insertData
#
#     def deleteData(offset, count):
#         if offset < 0:
#             dom.IndexSizeErr("offset cannot be negative")
#         if offset >= len(self.data):
#             dom.IndexSizeErr("offset cannot be beyond end of data")
#         if count < 0:
#             dom.IndexSizeErr("count cannot be negative")
#         if count:
#             self.data = self.data[:offset] + self.data[offset + count :]
#     self.deleteData = deleteData
#
#     def replaceData(offset, count, arg):
#         if offset < 0:
#             dom.IndexSizeErr("offset cannot be negative")
#         if offset >= len(self.data):
#             dom.IndexSizeErr("offset cannot be beyond end of data")
#         if count < 0:
#             dom.IndexSizeErr("count cannot be negative")
#         if count:
#             self.data = "%s%s%s" % (
#                 self.data[:offset],
#                 arg,
#                 self.data[offset + count :],
#             )
#     self.replaceData = replaceData
#     return self
#
#
# defproperty(CharacterData, "length", doc="Length of the string data.")
# def Text():
#     __slots__ = ()
#
#     nodeType = Node.TEXT_NODE
#     nodeName = "#text"
#     attributes = None
#
#     def splitText(offset):
#         if offset < 0 or offset > len(self.data):
#             dom.IndexSizeErr("illegal offset value")
#         newText = self.__class__()
#         newText.data = self.data[offset:]
#         newText.ownerDocument = self.ownerDocument
#         next = self.nextSibling
#         if self.parentNode and self in self.parentNode.childNodes:
#             if next == None:
#                 self.parentNode.appendChild(newText)
#             else:
#                 self.parentNode.insertBefore(newText, next)
#         self.data = self.data[:offset]
#         return newText
#     self.splitText = splitText
#
#     def writexml(writer, indent="", addindent="", newl=""):
#         _write_data(writer, "%s%s%s" % (indent, self.data, newl))
#     self.writexml = writexml
#
#     # DOM Level 3 (WD 9 April 2002)
#
#     def _get_wholeText():
#         L = [self.data]
#         n = self.previousSibling
#         for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
#             if n == None:
#                 break
#             if n.nodeType in (Node.TEXT_NODE, Node.CDATA_SECTION_NODE):
#                 L.insert(0, n.data)
#                 n = n.previousSibling
#             else:
#                 break
#         n = self.nextSibling
#         for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
#             if n == None:
#                 break
#             if n.nodeType in (Node.TEXT_NODE, Node.CDATA_SECTION_NODE):
#                 L.append(n.data)
#                 n = n.nextSibling
#             else:
#                 break
#         return "".join(L)
#     self._get_wholeText = _get_wholeText
#
#     def replaceWholeText(content):
#         # XXX This needs to be seriously changed if minidom ever
#         # supports EntityReference nodes.
#         parent = self.parentNode
#         n = self.previousSibling
#         for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
#             if n == None:
#                 break
#             if n.nodeType in (Node.TEXT_NODE, Node.CDATA_SECTION_NODE):
#                 next = n.previousSibling
#                 parent.removeChild(n)
#                 n = next
#             else:
#                 break
#         n = self.nextSibling
#         if not content:
#             parent.removeChild(self)
#         for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
#             if n == None:
#                 break
#             if n.nodeType in (Node.TEXT_NODE, Node.CDATA_SECTION_NODE):
#                 next = n.nextSibling
#                 parent.removeChild(n)
#                 n = next
#             else:
#                 break
#         if content:
#             self.data = content
#             return self
#         else:
#             return None
#     self.replaceWholeText = replaceWholeText
#
#     def _get_isWhitespaceInElementContent():
#         if self.data.strip():
#             return False
#         elem = _get_containing_element(self)
#         if elem == None:
#             return False
#         info = self.ownerDocument._get_elem_info(elem)
#         if info == None:
#             return False
#         else:
#             return info.isElementContent()
#     self._get_isWhitespaceInElementContent = _get_isWhitespaceInElementContent
#     return self
#
#
# defproperty(
#     Text,
#     "isWhitespaceInElementContent",
#     doc=("True iff this text node contains only whitespace" +
#     " and is in element content."),
# )
# defproperty(Text, "wholeText", doc="The text of all logically-adjacent text nodes.")
#
#
# def _get_containing_element(node):
#     c = node.parentNode
#     for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
#         if c == None:
#             break
#         if c.nodeType == Node.ELEMENT_NODE:
#             return c
#         c = c.parentNode
#     return None
#
#
# def _get_containing_entref(node):
#     c = node.parentNode
#     for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
#         if c == None:
#             break
#         if c.nodeType == Node.ENTITY_REFERENCE_NODE:
#             return c
#         c = c.parentNode
#     return None
# def Comment(data):
#     nodeType = Node.COMMENT_NODE
#     nodeName = "#comment"
#     self = larky.mutablestruct(__name__='Comment', __class__=Comment)
#
#     def __init__(data):
#         CharacterData.__init__(self)
#         self._data = data
#         return self
#     self = __init__(data)
#
#     def writexml(writer, indent="", addindent="", newl=""):
#         if "--" in self.data:
#             fail("ValueError: '--' is not allowed in a comment node")
#         writer.write("%s<!--%s-->%s" % (indent, self.data, newl))
#     self.writexml = writexml
#     return self
# def CDATASection(data):
#     __slots__ = ()
#
#     nodeType = Node.CDATA_SECTION_NODE
#     nodeName = "#cdata-section"
#
#     def writexml(writer, indent="", addindent="", newl=""):
#         if self.data.find("]]>") >= 0:
#             fail("ValueError: ']]>' not allowed in a CDATA section")
#         writer.write("<![CDATA[%s]]>" % self.data)
#     self.writexml = writexml
#     return self
# def ReadOnlySequentialNamedNodeMap(seq=()):
#     __slots__ = ("_seq",)
#     self = larky.mutablestruct(__name__='ReadOnlySequentialNamedNodeMap', __class__=ReadOnlySequentialNamedNodeMap)
#
#     def __init__(seq):
#         # seq should be a list or tuple
#         self._seq = seq
#         return self
#     self = __init__(seq)
#
#     def __len__():
#         return len(self._seq)
#     self.__len__ = __len__
#
#     def _get_length():
#         return len(self._seq)
#     self._get_length = _get_length
#
#     def getNamedItem(name):
#         for n in self._seq:
#             if n.nodeName == name:
#                 return n
#     self.getNamedItem = getNamedItem
#
#     def getNamedItemNS(namespaceURI, localName):
#         for n in self._seq:
#             if n.namespaceURI == namespaceURI and n.localName == localName:
#                 return n
#     self.getNamedItemNS = getNamedItemNS
#
#     def __getitem__(name_or_tuple):
#         if builtins.isinstance(name_or_tuple, tuple):
#             node = self.getNamedItemNS(*name_or_tuple)
#         else:
#             node = self.getNamedItem(name_or_tuple)
#         if node == None:
#             fail()
#         return node
#     self.__getitem__ = __getitem__
#
#     def item(index):
#         if index < 0:
#             return None
#         try:
#             return self._seq[index]
#         except IndexError:
#             return None
#     self.item = item
#
#     def removeNamedItem(name):
#         dom.NoModificationAllowedErr("NamedNodeMap instance is read-only")
#     self.removeNamedItem = removeNamedItem
#
#     def removeNamedItemNS(namespaceURI, localName):
#         dom.NoModificationAllowedErr("NamedNodeMap instance is read-only")
#     self.removeNamedItemNS = removeNamedItemNS
#
#     def setNamedItem(node):
#         dom.NoModificationAllowedErr("NamedNodeMap instance is read-only")
#     self.setNamedItem = setNamedItem
#
#     def setNamedItemNS(node):
#         dom.NoModificationAllowedErr("NamedNodeMap instance is read-only")
#     self.setNamedItemNS = setNamedItemNS
#
#     def __getstate__():
#         return [self._seq]
#     self.__getstate__ = __getstate__
#
#     def __setstate__(state):
#         self._seq = state[0]
#     self.__setstate__ = __setstate__
#     return self
#
#
# defproperty(
#     ReadOnlySequentialNamedNodeMap,
#     "length",
#     doc="Number of entries in the NamedNodeMap.",
# )
# def Identified(seq=()):
#     """Mix-in class that supports the publicId and systemId attributes."""
#
#     __slots__ = "publicId", "systemId"
#
#     def _identified_mixin_init(publicId, systemId):
#         self.publicId = publicId
#         self.systemId = systemId
#     self._identified_mixin_init = _identified_mixin_init
#
#     def _get_publicId():
#         return self.publicId
#     self._get_publicId = _get_publicId
#
#     def _get_systemId():
#         return self.systemId
#     self._get_systemId = _get_systemId
#     return self
# def DocumentType(qualifiedName):
#     nodeType = Node.DOCUMENT_TYPE_NODE
#     nodeValue = None
#     name = None
#     publicId = None
#     systemId = None
#     internalSubset = None
#     self = larky.mutablestruct(__name__='DocumentType', __class__=DocumentType)
#
#     def __init__(qualifiedName):
#         self.entities = ReadOnlySequentialNamedNodeMap()
#         self.notations = ReadOnlySequentialNamedNodeMap()
#         if qualifiedName:
#             prefix, localname = _nssplit(qualifiedName)
#             self.name = localname
#         self.nodeName = self.name
#         return self
#     self = __init__(qualifiedName)
#
#     def _get_internalSubset():
#         return self.internalSubset
#     self._get_internalSubset = _get_internalSubset
#
#     def cloneNode(deep):
#         if self.ownerDocument == None:
#             # it's ok
#             clone = DocumentType(None)
#             clone.name = self.name
#             clone.nodeName = self.name
#             operation = dom.UserDataHandler.NODE_CLONED
#             if deep:
#                 clone.entities._seq = []
#                 clone.notations._seq = []
#                 for n in self.notations._seq:
#                     notation = Notation(n.nodeName, n.publicId, n.systemId)
#                     clone.notations._seq.append(notation)
#                     n._call_user_data_handler(operation, n, notation)
#                 for e in self.entities._seq:
#                     entity = Entity(e.nodeName, e.publicId, e.systemId, e.notationName)
#                     entity.actualEncoding = e.actualEncoding
#                     entity.encoding = e.encoding
#                     entity.version = e.version
#                     clone.entities._seq.append(entity)
#                     e._call_user_data_handler(operation, e, entity)
#             self._call_user_data_handler(operation, self, clone)
#             return clone
#         else:
#             return None
#     self.cloneNode = cloneNode
#
#     def writexml(writer, indent="", addindent="", newl=""):
#         writer.write("<!DOCTYPE ")
#         writer.write(self.name)
#         if self.publicId:
#             writer.write(
#                 "%s  PUBLIC '%s'%s  '%s'" % (newl, self.publicId, newl, self.systemId)
#             )
#         elif self.systemId:
#             writer.write("%s  SYSTEM '%s'" % (newl, self.systemId))
#         if self.internalSubset != None:
#             writer.write(" [")
#             writer.write(self.internalSubset)
#             writer.write("]")
#         writer.write(">" + newl)
#     self.writexml = writexml
#     return self
# def Entity(name, publicId, systemId, notation):
#     attributes = None
#     nodeType = Node.ENTITY_NODE
#     nodeValue = None
#
#     actualEncoding = None
#     encoding = None
#     version = None
#     self = larky.mutablestruct(__name__='Entity', __class__=Entity)
#
#     def __init__(name, publicId, systemId, notation):
#         self.nodeName = name
#         self.notationName = notation
#         self.childNodes = NodeList()
#         self._identified_mixin_init(publicId, systemId)
#         return self
#     self = __init__(name, publicId, systemId, notation)
#
#     def _get_actualEncoding():
#         return self.actualEncoding
#     self._get_actualEncoding = _get_actualEncoding
#
#     def _get_encoding():
#         return self.encoding
#     self._get_encoding = _get_encoding
#
#     def _get_version():
#         return self.version
#     self._get_version = _get_version
#
#     def appendChild(newChild):
#         dom.HierarchyRequestErr("cannot append children to an entity node")
#     self.appendChild = appendChild
#
#     def insertBefore(newChild, refChild):
#         dom.HierarchyRequestErr("cannot insert children below an entity node")
#     self.insertBefore = insertBefore
#
#     def removeChild(oldChild):
#         dom.HierarchyRequestErr("cannot remove children from an entity node")
#     self.removeChild = removeChild
#
#     def replaceChild(newChild, oldChild):
#         dom.HierarchyRequestErr("cannot replace children of an entity node")
#     self.replaceChild = replaceChild
#     return self
# def Notation(name, publicId, systemId):
#     nodeType = Node.NOTATION_NODE
#     nodeValue = None
#     self = larky.mutablestruct(__name__='Notation', __class__=Notation)
#
#     def __init__(name, publicId, systemId):
#         self.nodeName = name
#         self._identified_mixin_init(publicId, systemId)
#         return self
#     self = __init__(name, publicId, systemId)
#     return self
# def DOMImplementation(name, publicId, systemId):
#     _features = [
#         ("core", "1.0"),
#         ("core", "2.0"),
#         ("core", None),
#         ("xml", "1.0"),
#         ("xml", "2.0"),
#         ("xml", None),
#         ("ls-load", "3.0"),
#         ("ls-load", None),
#     ]
#
#     def hasFeature(feature, version):
#         if version == "":
#             version = None
#         return (feature.lower(), version) in self._features
#     self.hasFeature = hasFeature
#
#     def createDocument(namespaceURI, qualifiedName, doctype):
#         if doctype and doctype.parentNode != None:
#             dom.WrongDocumentErr("doctype object owned by another DOM tree")
#         doc = self._create_document()
#
#         add_root_element = not (
#             namespaceURI == None and qualifiedName == None and doctype == None
#         )
#
#         if not qualifiedName and add_root_element:
#             # The spec is unclear what to raise here; SyntaxErr
#             # would be the other obvious candidate. Since Xerces raises
#             # InvalidCharacterErr, and since SyntaxErr is not listed
#             # for createDocument, that seems to be the better choice.
#             # XXX: need to check for illegal characters here and in
#             # createElement.
#
#             # DOM Level III clears this up when talking about the return value
#             # of this function.  If namespaceURI, qName and DocType are
#             # Null the document is returned without a document element
#             # Otherwise if doctype or namespaceURI are not None
#             # Then we go back to the above problem
#             dom.InvalidCharacterErr("Element with no name")
#
#         if add_root_element:
#             prefix, localname = _nssplit(qualifiedName)
#             if (
#                 prefix == "xml"
#                 and namespaceURI != "http://www.w3.org/XML/1998/namespace"
#             ):
#                 dom.NamespaceErr("illegal use of 'xml' prefix")
#             if prefix and not namespaceURI:
#                 dom.NamespaceErr("illegal use of prefix without namespaces")
#             element = doc.createElementNS(namespaceURI, qualifiedName)
#             if doctype:
#                 doc.appendChild(doctype)
#             doc.appendChild(element)
#
#         if doctype:
#             doctype.parentNode = doc
#             doctype.ownerDocument = doctype.parentNode
#
#         doc.doctype = doctype
#         doc.implementation = self
#         return doc
#     self.createDocument = createDocument
#
#     def createDocumentType(qualifiedName, publicId, systemId):
#         doctype = DocumentType(qualifiedName)
#         doctype.publicId = publicId
#         doctype.systemId = systemId
#         return doctype
#     self.createDocumentType = createDocumentType
#
#     # DOM Level 3 (WD 9 April 2002)
#
#     def getInterface(feature):
#         if self.hasFeature(feature, None):
#             return self
#         else:
#             return None
#     self.getInterface = getInterface
#
#     # internal
#     def _create_document():
#         return Document()
#     self._create_document = _create_document
#     return self
# def ElementInfo(name):
#     """Object that represents content-model information for an element.
#
#     This implementation is not expected to be used in practice; DOM
#     builders should provide implementations which do the right thing
#     using information available to it.
#
#     """
#
#     __slots__ = ("tagName",)
#     self = larky.mutablestruct(__name__='ElementInfo', __class__=ElementInfo)
#
#     def __init__(name):
#         self.tagName = name
#         return self
#     self = __init__(name)
#
#     def getAttributeType(aname):
#         return _no_type
#     self.getAttributeType = getAttributeType
#
#     def getAttributeTypeNS(namespaceURI, localName):
#         return _no_type
#     self.getAttributeTypeNS = getAttributeTypeNS
#
#     def isElementContent():
#         return False
#     self.isElementContent = isElementContent
#
#     def isEmpty():
#         """Returns true iff this element is declared to have an EMPTY
#         content model."""
#         return False
#     self.isEmpty = isEmpty
#
#     def isId(aname):
#         """Returns true iff the named attribute is a DTD-style ID."""
#         return False
#     self.isId = isId
#
#     def isIdNS(namespaceURI, localName):
#         """Returns true iff the identified attribute is a DTD-style ID."""
#         return False
#     self.isIdNS = isIdNS
#
#     def __getstate__():
#         return self.tagName
#     self.__getstate__ = __getstate__
#
#     def __setstate__(state):
#         self.tagName = state
#     self.__setstate__ = __setstate__
#     return self
#

# def Document():
#     __slots__ = ("_elem_info", "doctype", "_id_search_stack", "childNodes", "_id_cache")
#     _child_node_types = (
#         Node.ELEMENT_NODE,
#         Node.PROCESSING_INSTRUCTION_NODE,
#         Node.COMMENT_NODE,
#         Node.DOCUMENT_TYPE_NODE,
#     )
#
#     implementation = DOMImplementation()
#     nodeType = Node.DOCUMENT_NODE
#     nodeName = "#document"
#     nodeValue = None
#     attributes = None
#     parentNode = None
#     previousSibling = None
#     nextSibling = previousSibling
#
#     # Document attributes from Level 3 (WD 9 April 2002)
#
#     actualEncoding = None
#     encoding = None
#     standalone = None
#     version = None
#     strictErrorChecking = False
#     errorHandler = None
#     documentURI = None
#
#     _magic_id_count = 0
#     self = larky.mutablestruct(__name__='Document', __class__=Document)
#
#     def __init__():
#         self.doctype = None
#         self.childNodes = NodeList()
#         # mapping of (namespaceURI, localName) -> ElementInfo
#         #        and tagName -> ElementInfo
#         self._elem_info = {}
#         self._id_cache = {}
#         self._id_search_stack = None
#         return self
#     self = __init__()
#
#     def _get_elem_info(element):
#         if element.namespaceURI:
#             key = element.namespaceURI, element.localName
#         else:
#             key = element.tagName
#         return self._elem_info.get(key)
#     self._get_elem_info = _get_elem_info
#
#     def _get_actualEncoding():
#         return self.actualEncoding
#     self._get_actualEncoding = _get_actualEncoding
#
#     def _get_doctype():
#         return self.doctype
#     self._get_doctype = _get_doctype
#
#     def _get_documentURI():
#         return self.documentURI
#     self._get_documentURI = _get_documentURI
#
#     def _get_encoding():
#         return self.encoding
#     self._get_encoding = _get_encoding
#
#     def _get_errorHandler():
#         return self.errorHandler
#     self._get_errorHandler = _get_errorHandler
#
#     def _get_standalone():
#         return self.standalone
#     self._get_standalone = _get_standalone
#
#     def _get_strictErrorChecking():
#         return self.strictErrorChecking
#     self._get_strictErrorChecking = _get_strictErrorChecking
#
#     def _get_version():
#         return self.version
#     self._get_version = _get_version
#
#     def appendChild(node):
#         if node.nodeType not in self._child_node_types:
#             dom.HierarchyRequestErr(
#                 "%s cannot be child of %s" % (repr(node), repr(self))
#             )
#         if node.parentNode != None:
#             # This needs to be done before the next test since this
#             # may *be* the document element, in which case it should
#             # end up re-ordered to the end.
#             node.parentNode.removeChild(node)
#
#         if node.nodeType == Node.ELEMENT_NODE and self._get_documentElement():
#             dom.HierarchyRequestErr("two document elements disallowed")
#         return Node.appendChild(self, node)
#     self.appendChild = appendChild
#
#     def removeChild(oldChild):
#         try:
#             self.childNodes.remove(oldChild)
#         except ValueError:
#             dom.NotFoundErr()
#         oldChild.nextSibling = None
#         oldChild.previousSibling = oldChild.nextSibling
#         oldChild.parentNode = None
#         if self.documentElement == oldChild:
#             self.documentElement = None
#
#         return oldChild
#     self.removeChild = removeChild
#
#     def _get_documentElement():
#         for node in self.childNodes:
#             if node.nodeType == Node.ELEMENT_NODE:
#                 return node
#     self._get_documentElement = _get_documentElement
#
#     def unlink():
#         if self.doctype != None:
#             self.doctype.unlink()
#             self.doctype = None
#         Node.unlink(self)
#     self.unlink = unlink
#
#     def cloneNode(deep):
#         if not deep:
#             return None
#         clone = self.implementation.createDocument(None, None, None)
#         clone.encoding = self.encoding
#         clone.standalone = self.standalone
#         clone.version = self.version
#         for n in self.childNodes:
#             childclone = _clone_node(n, deep, clone)
#             if not (childclone.ownerDocument.isSameNode(clone)):
#                 fail("assert childclone.ownerDocument.isSameNode(clone) failed!")
#             clone.childNodes.append(childclone)
#             if childclone.nodeType == Node.DOCUMENT_NODE:
#                 if not (clone.documentElement == None):
#                     fail("assert clone.documentElement == None failed!")
#             elif childclone.nodeType == Node.DOCUMENT_TYPE_NODE:
#                 if not (clone.doctype == None):
#                     fail("assert clone.doctype == None failed!")
#                 clone.doctype = childclone
#             childclone.parentNode = clone
#         self._call_user_data_handler(dom.UserDataHandler.NODE_CLONED, self, clone)
#         return clone
#     self.cloneNode = cloneNode
#
#     def createDocumentFragment():
#         d = DocumentFragment()
#         d.ownerDocument = self
#         return d
#     self.createDocumentFragment = createDocumentFragment
#
#     def createElement(tagName):
#         e = Element(tagName)
#         e.ownerDocument = self
#         return e
#     self.createElement = createElement
#
#     def createTextNode(data):
#         if not builtins.isinstance(data, str):
#             fail("TypeError: node contents must be a string")
#         t = Text()
#         t.data = data
#         t.ownerDocument = self
#         return t
#     self.createTextNode = createTextNode
#
#     def createCDATASection(data):
#         if not builtins.isinstance(data, str):
#             fail("TypeError: node contents must be a string")
#         c = CDATASection()
#         c.data = data
#         c.ownerDocument = self
#         return c
#     self.createCDATASection = createCDATASection
#
#     def createComment(data):
#         c = Comment(data)
#         c.ownerDocument = self
#         return c
#     self.createComment = createComment
#
#     def createProcessingInstruction(target, data):
#         p = ProcessingInstruction(target, data)
#         p.ownerDocument = self
#         return p
#     self.createProcessingInstruction = createProcessingInstruction
#
#     def createAttribute(qName):
#         a = Attr(qName)
#         a.ownerDocument = self
#         a.value = ""
#         return a
#     self.createAttribute = createAttribute
#
#     def createElementNS(namespaceURI, qualifiedName):
#         prefix, localName = _nssplit(qualifiedName)
#         e = Element(qualifiedName, namespaceURI, prefix)
#         e.ownerDocument = self
#         return e
#     self.createElementNS = createElementNS
#
#     def createAttributeNS(namespaceURI, qualifiedName):
#         prefix, localName = _nssplit(qualifiedName)
#         a = Attr(qualifiedName, namespaceURI, localName, prefix)
#         a.ownerDocument = self
#         a.value = ""
#         return a
#     self.createAttributeNS = createAttributeNS
#
#     # A couple of implementation-specific helpers to create node types
#     # not supported by the W3C DOM specs:
#
#     def _create_entity(name, publicId, systemId, notationName):
#         e = Entity(name, publicId, systemId, notationName)
#         e.ownerDocument = self
#         return e
#     self._create_entity = _create_entity
#
#     def _create_notation(name, publicId, systemId):
#         n = Notation(name, publicId, systemId)
#         n.ownerDocument = self
#         return n
#     self._create_notation = _create_notation
#
#     def getElementById(id):
#         if id in self._id_cache:
#             return self._id_cache[id]
#         if not (self._elem_info or self._magic_id_count):
#             return None
#
#         stack = self._id_search_stack
#         if stack == None:
#             # we never searched before, or the cache has been cleared
#             stack = [self.documentElement]
#             self._id_search_stack = stack
#         elif not stack:
#             # Previous search was completed and cache is still valid;
#             # no matching node.
#             return None
#
#         result = None
#         for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
#             if not stack:
#                 break
#             node = stack.pop()
#             # add child elements to stack for continued searching
#             stack.extend(
#                 [
#                     child
#                     for child in node.childNodes
#                     if child.nodeType in _nodeTypes_with_children
#                 ]
#             )
#             # check this node
#             info = self._get_elem_info(node)
#             if info:
#                 # We have to process all ID attributes before
#                 # returning in order to get all the attributes set to
#                 # be IDs using Element.setIdAttribute*().
#                 for attr in node.attributes.values():
#                     if attr.namespaceURI:
#                         if info.isIdNS(attr.namespaceURI, attr.localName):
#                             self._id_cache[attr.value] = node
#                             if attr.value == id:
#                                 result = node
#                             elif not node._magic_id_nodes:
#                                 break
#                     elif info.isId(attr.name):
#                         self._id_cache[attr.value] = node
#                         if attr.value == id:
#                             result = node
#                         elif not node._magic_id_nodes:
#                             break
#                     elif attr._is_id:
#                         self._id_cache[attr.value] = node
#                         if attr.value == id:
#                             result = node
#                         elif node._magic_id_nodes == 1:
#                             break
#             elif node._magic_id_nodes:
#                 for attr in node.attributes.values():
#                     if attr._is_id:
#                         self._id_cache[attr.value] = node
#                         if attr.value == id:
#                             result = node
#             if result != None:
#                 break
#         return result
#     self.getElementById = getElementById
#
#     def getElementsByTagName(name):
#         return _get_elements_by_tagName_helper(self, name, NodeList())
#     self.getElementsByTagName = getElementsByTagName
#
#     def getElementsByTagNameNS(namespaceURI, localName):
#         return _get_elements_by_tagName_ns_helper(
#             self, namespaceURI, localName, NodeList()
#         )
#     self.getElementsByTagNameNS = getElementsByTagNameNS
#
#     def isSupported(feature, version):
#         return self.implementation.hasFeature(feature, version)
#     self.isSupported = isSupported
#
#     def importNode(node, deep):
#         if node.nodeType == Node.DOCUMENT_NODE:
#             dom.NotSupportedErr("cannot import document nodes")
#         elif node.nodeType == Node.DOCUMENT_TYPE_NODE:
#             dom.NotSupportedErr("cannot import document type nodes")
#         return _clone_node(node, deep, self)
#     self.importNode = importNode
#
#     def writexml(writer, indent="", addindent="", newl="", encoding=None):
#         if encoding == None:
#             writer.write('<?xml version="1.0" ?>' + newl)
#         else:
#             writer.write('<?xml version="1.0" encoding="%s"?>%s' % (encoding, newl))
#         for node in self.childNodes:
#             node.writexml(writer, indent, addindent, newl)
#     self.writexml = writexml
#
#     # DOM Level 3 (WD 9 April 2002)
#
#     def renameNode(n, namespaceURI, name):
#         if n.ownerDocument != self:
#             dom.WrongDocumentErr(
#                 ("cannot rename nodes from other documents;\n" +
#                 "expected %s,\nfound %s") % (self, n.ownerDocument)
#             )
#         if n.nodeType not in (Node.ELEMENT_NODE, Node.ATTRIBUTE_NODE):
#             dom.NotSupportedErr(
#                 "renameNode() only applies to element and attribute nodes"
#             )
#         if namespaceURI != EMPTY_NAMESPACE:
#             if ":" in name:
#                 prefix, localName = name.split(":", 1)
#                 if prefix == "xmlns" and namespaceURI != dom.XMLNS_NAMESPACE:
#                     dom.NamespaceErr("illegal use of 'xmlns' prefix")
#             else:
#                 if (
#                     name == "xmlns"
#                     and namespaceURI != dom.XMLNS_NAMESPACE
#                     and n.nodeType == Node.ATTRIBUTE_NODE
#                 ):
#                     dom.NamespaceErr("illegal use of the 'xmlns' attribute")
#                 prefix = None
#                 localName = name
#         else:
#             prefix = None
#             localName = None
#         if n.nodeType == Node.ATTRIBUTE_NODE:
#             element = n.ownerElement
#             if element != None:
#                 is_id = n._is_id
#                 element.removeAttributeNode(n)
#         else:
#             element = None
#         n.prefix = prefix
#         n._localName = localName
#         n.namespaceURI = namespaceURI
#         n.nodeName = name
#         if n.nodeType == Node.ELEMENT_NODE:
#             n.tagName = name
#         else:
#             # attribute node
#             n.name = name
#             if element != None:
#                 element.setAttributeNode(n)
#                 if is_id:
#                     element.setIdAttributeNode(n)
#         # It's not clear from a semantic perspective whether we should
#         # call the user data handlers for the NODE_RENAMED event since
#         # we're re-using the existing node.  The draft spec has been
#         # interpreted as meaning "no, don't call the handler unless a
#         # new node is created."
#         return n
#     self.renameNode = renameNode
#     return self
#
#
# defproperty(Document, "documentElement", doc="Top-level element of this document.")
#



def _nssplit(qualifiedName):
    fields = qualifiedName.split(":", 1)
    if len(fields) == 2:
        return fields
    else:
        return (None, fields[0])


def _do_pulldom_parse(func, args, kwargs):
    events = func(*args, **kwargs)
    toktype, rootNode = events.getEvent()
    events.expandNode(rootNode)
    events.clear()
    return rootNode


def parse(file, parser=None, bufsize=None):
    """Parse a file into a DOM by filename or file object."""
    # if parser is None and not bufsize:
    #     from dom.import expatbuilder
    #     return expatbuilder.parse(file)
    # else:
    #     from dom.import pulldom
    #     return _do_pulldom_parse(pulldom.parse, (file,),
    #         {'parser': parser, 'bufsize': bufsize})
    fail("not supported")


def parseString(string, parser=None):
    """Parse a file into a DOM from a string."""
    # if parser is None:
    #     from dom.import expatbuilder
    #     return expatbuilder.parseString(string)
    # else:
    #     from dom.import pulldom
    #     return _do_pulldom_parse(pulldom.parseString, (string,),
    #                              {'parser': parser})
    fail("not supported")

#
# def getDOMImplementation(features=None):
#     if features:
#         if builtins.isinstance(features, str):
#             features = domreg._parse_feature_string(features)
#         for f, v in features:
#             if not Document.implementation.hasFeature(f, v):
#                 return None
#     return Document.implementation

