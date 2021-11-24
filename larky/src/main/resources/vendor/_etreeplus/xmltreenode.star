load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//operator", operator="operator")
load("@stdlib//xml/dom", dom="dom")
load("@stdlib//xml/etree/ElementPath", ElementPath="ElementPath")
load("@stdlib//xml/etree/ElementTree", element_tree="ElementTree")
load("@vendor//_etreeplus/xmltree", xmltree="xmltree", iselement="iselement", ELEMENT_TYPES="ELEMENT_TYPES")


QName = element_tree.QName

_namespaced_name = xmltree.namespaced_name
_get_ns_tag = xmltree.get_ns_tag
_get_ns = xmltree.get_ns
_namespaced_name_from_ns_name = xmltree.namespaced_name_from_ns_name
_tag_matches = xmltree.tag_matches


def XMLNodeList():
    """ List of tree nodes with special accessibility helpers
    """
    self = larky.mutablestruct(__name__='XMLNodeList',
                               __class__=XMLNodeList)

    def __init__():
        """ Initialize XMLNodeList
        """
        self.node_list = []
        return self

    self = __init__()

    def __solvedata(instance):
        """ Solve the data from the XMLNode
        @param instance Any XMLNode instance
        @returns The instance or it's contents depending on the count of children, etc.
        """
        childs = instance._children
        if len(childs) == 1:
            return childs[0].getdata()
        return instance
    self.__solvedata = __solvedata

    def __getitem__(index):
        """ Get item from list by index or name
        @param index Index of the item or it's name
        @returns The item or None
        """
        if not self.node_list:
            return None
        ll = len(self.node_list)

        if type(index) == int:
            if index < ll:
                return self.__solvedata(self.node_list[index])
            return None

        for node in self.node_list:
            if index in node:
                return self.__solvedata(node)
        return None
    self.__getitem__ = __getitem__

    def __contains__(index):
        """ Check whether list contains certain named item
        @param index Name of the item
        @returns True if item found, False otherwise
        """
        for instance in self.node_list:
            if index in instance:
                return True
        return False
    self.__contains__ = __contains__

    def __iter__():
        """ Returns iterator of the items
        @returns iterator of the items
        """
        return builtins.iter(self.node_list)
    self.__iter__ = __iter__

    def __len__():
        """ Get number of items
        @returns Number of items
        """
        return len(self.node_list)
    self.__len__ = __len__

    def append(var):
        """ Append item to node list
        @param var Item to be appended
        """
        self.node_list.append(var)
    self.append = append
    return self


lxml_xml_node_structs = """
    ctypedef enum xmlElementType:
        XML_ELEMENT_NODE=           1
        XML_ATTRIBUTE_NODE=         2
        XML_TEXT_NODE=              3
        XML_CDATA_SECTION_NODE=     4
        XML_ENTITY_REF_NODE=        5
        XML_ENTITY_NODE=            6
        XML_PI_NODE=                7
        XML_COMMENT_NODE=           8
        XML_DOCUMENT_NODE=          9
        XML_DOCUMENT_TYPE_NODE=     10
        XML_DOCUMENT_FRAG_NODE=     11
        XML_NOTATION_NODE=          12
        XML_HTML_DOCUMENT_NODE=     13
        XML_DTD_NODE=               14
        XML_ELEMENT_DECL=           15
        XML_ATTRIBUTE_DECL=         16
        XML_ENTITY_DECL=            17
        XML_NAMESPACE_DECL=         18
        XML_XINCLUDE_START=         19
        XML_XINCLUDE_END=           20
        
    ctypedef enum xmlAttributeType:
        XML_ATTRIBUTE_CDATA =      1
        XML_ATTRIBUTE_ID=          2
        XML_ATTRIBUTE_IDREF=       3
        XML_ATTRIBUTE_IDREFS=      4
        XML_ATTRIBUTE_ENTITY=      5
        XML_ATTRIBUTE_ENTITIES=    6
        XML_ATTRIBUTE_NMTOKEN=     7
        XML_ATTRIBUTE_NMTOKENS=    8
        XML_ATTRIBUTE_ENUMERATION= 9
        XML_ATTRIBUTE_NOTATION=    10
        
    ctypedef struct xmlAttr:
        void* _private
        xmlElementType type
        const_xmlChar* name
        xmlNode* children
        xmlNode* last
        xmlNode* parent
        xmlAttr* next
        xmlAttr* prev
        xmlDoc* doc
        xmlNs* ns
        xmlAttributeType atype
        
    ctypedef struct xmlNs:
        const_xmlChar* href
        const_xmlChar* prefix
        xmlNs* next

    # https://github.com/lxml/lxml/blob/982f8d5612925010a12a70748a077af846def6be/src/lxml/includes/tree.pxd#L162-L176
    ctypedef struct xmlNode:
        void * _private  # application data
        xmlElementType type  # type number, must be second !
        const xmlChar * name  # the name of the node, or the entity
        struct _xmlNode * children  # parent->childs link
        struct _xmlNode * last  # last child link
        struct _xmlNode * parent  # child->parent link
        struct _xmlNode * next  # next sibling link
        struct _xmlNode * prev  # previous sibling link
        struct _xmlDoc * doc  # the containing document End of common p
        xmlChar * content  # the content
        struct _xmlAttr * properties  # properties list
        xmlNs * ns  # pointer to the associated namespace
        xmlNs * nsDef  # namespace definitions on this node
        unsigned short line  # line number
    # from: https://mail.gnome.org/archives/xml/2005-May/msg00145.html        
    # Libxml2 handles the corresponding DOM Node methods namespaceURI() and
    # prefix() in the following way:
    # node->ns->prefix == result of node.prefix()
    # node->ns->href   == result of node.namespaceURI()
    # 
    # The node->ns field is a pointer to an xmlNs struct, which
    # is held in the elem->nsDef field of element-nodes
    
"""

def XMLNode(tag, attrib=None, **extra):
    """
    Custom Tree structure, may contain any number of children.

    XMLNode can contain about any value or data, any number of
    children and sub-children.

    Contains definitions and parsing capabilities.
    """
    self = larky.mutablestruct(__name__='XMLNode', __class__=XMLNode)

    # XMLNodeList is a nested class
    self.XMLNodeList = XMLNodeList

    # comes first so we can easily remember to clear() anything we add
    # in __init__
    def clear():
        """Reset element.

        This function removes all subelements, clears all attributes, and sets
        the text and tail attributes to None.

        """
        self._nsmap = {}
        self.__cached_nsmap = None
        self.__cached_prefix = None

        self.attrib.clear()

        self._children = []

        self.text = None
        self.tail = None

        self._owner_doc = None
        self._doctype = None
        self.__parent = None
        self.__nodeType = None
    self.clear = clear

    def __init__(tag, attrib, **extra):
        """
        Create a new element and initialize text content, attributes, and namespaces.

        @param tag Setup the data of this node, can be overridden later with setdata
        @param attrib Element attribute dictionary
        @param nsmap Namespace map
        @param **extra Additional attributes, given as keyword arguments
        """

        # ----- NOTE -----
        # ADDING ANY INSTANCE VARIABLES HERE SHOULD ALSO BE ADDED IN
        # clear() above (except tag -- which will never change)
        # --- END NOTE----

        # tag(1): data => prefix = None, href => None
        # tag(2): {http://example.com/ns/foo}p => prefix = None, href => http://example.com/ns/foo
        # nsmap: {"http://example.com/ns/foo": "x"}
        # print("DEBUG:::", tag, attrib, nsmap)
        self.tag = tag  # could be a qualified tag
        # get the ns/prefix (if any)
        self._href, self._name = _get_ns_tag(tag)  # won't change
        # nsmap's type: Dict["http://example.com/ns/foo", "x"]
        attrib = attrib or {}
        self._nsmap = attrib.pop('nsmap', {})
        # used in _build_nsmap below (accessible through self.nsmap property)
        self.__cached_nsmap = None
        # used in prefix below
        self.__cached_prefix = None

        self.attrib = dict(**attrib) if attrib else {}
        self.attrib.update(extra)
        self._children = []

        self.text = None
        self.tail = None

        self._doctype = None
        self._owner_doc = None
        self.__parent = attrib.pop('parent', None)
        self.__nodeType = None
        return self
    self = __init__(tag, attrib, **extra)

    # read-only properties
    self.ns = larky.property(lambda: dict(**self._nsmap))
    self.name = larky.property(lambda: self._name)
    self.href = larky.property(lambda: self._href)
    self._index = larky.property(lambda: self.__parent.index(self))

    def _owner_document():
        """Document: An associated document."""
        current = self
        doc = self._owner_doc
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if current == None:
                break
            if doc != None:
                return doc
            parent = current.getparent()
            if parent == None:
                break
            current = parent
            doc = current._owner_doc
        return None
    self.owner_doc = larky.property(_owner_document)

    def _build_nsmap():
        """
        Namespace prefix->URI mapping known in the context of this Element.
        This includes all namespace declarations of the parents.
        """
        if self.__cached_nsmap:
            return self.__cached_nsmap

        nsmap = {}
        qu = [self]
        for _ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not qu:
                break
            cnode = qu.pop(0)
            # while c_node is not NULL and c_node.type == tree.XML_ELEMENT_NODE:
            if cnode == None or not iselement(cnode):# or not cnode.ns:
                continue

            c_ns = cnode.ns
            for uri, prefix in c_ns.items():
                if prefix not in nsmap:
                    nsmap[prefix] = uri
            qu.append(cnode.getparent())
        self.__cached_nsmap = nsmap
        return self.__cached_nsmap
    self.nsmap = larky.property(_build_nsmap)

    def _prefix():
        """Namespace prefix or None.
        """
        if self.__cached_prefix:
            return self.__cached_prefix

        self.__cached_prefix = self.nsmap.get(self._href, None)
        return self.__cached_prefix

    self.prefix = larky.property(_prefix)

    def appendvalue(value):
        """ Set value of the Node, or the text
        @param value Any value
        """
        self.text = "%s%s" % ('' if not self.text else self.text, value)

    self.appendvalue = appendvalue

    def setvalue(value):
        """ Set value of the Node, or the text
        @param value Any value
        """
        self.text = value

    self.setvalue = setvalue

    def getattrib(key):
        """ Get attribute value by name
        @param key Attribute name
        @returns Attribute value or raises error
        """
        return self.attrib[key]

    self.getattrib = getattrib

    def getattributes():
        """ Get all attributes as a dictionary
        @returns Dictionary containing all attributes
        """
        return self.attrib

    self.getattributes = getattributes

    def delattrib(key):
        """ Remove attribute
        @param key Attribute name
        """
        self.attrib.pop(key)
    self.delattrib = delattrib

    def getdata():
        """ Get the data under this XMLNode
        @returns Data set under this XMLNode
        """
        return self.tag

    self.getdata = getdata

    def setdata(data):
        """ Set node data, can be anything
        @param data Any data to set under this XMLNode
        """
        self.tag = data

    self.setdata = setdata

    def getchildrenref():
        """ Get list of children, don't make copy just get reference
        @returns List of all children under this XMLNode
        """
        return self._children

    self.getchildrenref = getchildrenref

    def get_self_and_subtree_nodes_by_name(name):
        """ Check also if self/root matches for the name,
        after that take the subtree nodes
        @param name Value to be search for
        @returns List of CustomXMLParser of the corresponding child trees
        """
        trees = []
        if self.isdata(name):
            trees.append(self)

        trees += self.get_sub_tree_nodes_by_name(name)
        return trees

    self.get_self_and_subtree_nodes_by_name = get_self_and_subtree_nodes_by_name

    def get_sub_tree_nodes_by_name(name):
        """Get ALL nodes and their subtrees which contains certain named value as list of XMLNodes.
        This allows future manipulation or queries to the tree. Also identifying each node's parent is easy with getparent()
        @param name Value to be search for
        @returns List of CustomXMLParser of the corresponding child trees
        @code
        For example:
        root = XMLNode("root")
        a = XMLNode("ChildA")
        b = XMLNode("ChildB")
        c = XMLNode("ChildC")
        root.addchild( a )
        root.addchild( b )
        root.addchild( c )
        a.addchild( XMLNode("Test") )
        a.addchild( XMLNode("Test2") )
        n = TreeNode("Test")
        b.addchild( n )
        n.addchild( XMLNode("SubTest") )
        n.addchild( XMLNode("SubTest2") )
        c.addchild( XMLNode("Test") )
        c.addchild( XMLNode("Other") )

        This would create tree like:

                         - Test
                       /
              -- ChildA--- Test2    - Subtest
            /                     /
        root ---- ChildB--- Test ----- SubTest2
            \
              -- ChildC -- Test
                       \
                         - Other
        @endcode
        Consider a call:
        root.getSubTreesByName("Test")
        It would return all XMLNode named as "Test"
        As a note we just get exact matches, not partial matches
        We get only the named node and it's subtree, no parents or what so ever
        """

        trees = []
        qu = self._children[0:]
        for _ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not qu:
                break
            child = qu.pop(0)
            if child.isdata(name):
                trees.append(child)
            if child._children:
                qu = list(child._children) + list(qu)
        return trees

    self.get_sub_tree_nodes_by_name = get_sub_tree_nodes_by_name

    def get_treenode_by_name(name):
        """ This is like get_sub_tree_nodes_by_name but return just
        FIRST matching subtree

        @param name Value to be search for
        @returns Corresponding child tree item
        """
        if self.isdata(name):
            return self
        qu = self._children[0:]
        for _ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not qu:
                break
            child = qu.pop(0)
            if child.isdata(name):
                return child
            if child._children:
                qu = list(child._children) + list(qu)
        return None

    self.get_treenode_by_name = get_treenode_by_name

    def getvalue():
        """ Return value of the XMLNode
        @returns Value of the XMLNode
        """
        return self.text

    self.getvalue = getvalue

    def insertafterchild(afterchild, child, reparent=True):
        """ Add a child node after another child
        @param afterchild Child instance to be before the new child
        @param child Any instance of XMLNode
        @param reparent True if should be reparented, False to skip
        """
        if reparent:
            child.reparent(self, addchild=False)

        chs = self._children
        if child not in chs:
            pos = self._children.index(afterchild)
            if pos != -1:
                self.insert(pos + 1, child)
            else:
                self.append(child)

    self.insertafterchild = insertafterchild

    def isattrib(key):
        """ Checks if this node contains attribute
        @param key Attribute name
        @returns True if found, False otherwise
        """
        return (key in self.attrib)

    self.isattrib = isattrib
    #
    def isdata(name):
        """ Compare the data on this XMLNode to name
        @param name The name to compare
        @returns True if name matches to the data, False otherwise
        """
        return (self.tag == name)

    self.isdata = isdata

    def nodetype():
        data = self.getdata()
        if types.is_function(data):
            data = larky.impl_function_name(data)
        return data
    self.nodetype = nodetype

    def numchildren():
        """ Return the number of children under this XMLNode
        @returns Number of children
        """
        return len(self._children)
    self.numchildren = numchildren

    def reparent(newparent, addchild=True):
        """ Reparent this XMLNode under another XMLNode
        @param newparent XMLNode instance to become new parent of this XMLNode
        @param addchild True if child needs to be added into parents list of children, False if not
        """
        if self.__parent == newparent:
            return

        # Need to make sure we're removed from possible old parent
        if self.__parent != None:
            self.__parent.removechild(self)

        # Set new parent
        self.__parent = newparent
        if addchild:
            self.__parent.addchild(self, reparent=False)

        # clear cached nsmap which traverses parents.
        self.__cached_nsmap = None
    self.reparent = reparent

    def to_recursive_sort_string():
        """Get sortable string presentation of this and all children
        @returns Sortable string presentation of this object and it's children
        """
        data = sorted(self.iter()[1:], key=lambda k: k.to_sort_string())
        s = ["%s%s" % (self.getdata(), self.attrib)]
        for i in data:
            s.append("%s%s" % (i.getdata(), i.attrib))
        return "".join(s).replace(" ", "")

    self.to_recursive_sort_string = to_recursive_sort_string

    def to_simple_string():
        """ Convert to XML string, does not do any formatting
        @returns XML presentation of the tree
        """
        res = element_tree.tostring(self)
        res = res.decode('utf-8', 'replace')
        return res

    self.to_simple_string = to_simple_string

    def to_sort_string():
        """ Get sortable string presentation
        @returns Sortable string presentation of this object
        """
        s = self.getdata()
        if type(s) != str:
            return s

        sortedkeys = self.attrib.keys()
        sortedkeys = sorted(sortedkeys)
        arr = ""
        for key in sortedkeys:
            if arr:
                arr += ","
            arr += "'%s':'%s'" % (key, self.attrib[key])

        s += "{%s}" % (arr)
        s = s.replace(" ", "")
        return s

    self.to_sort_string = to_sort_string

    def tostring(doctype=''):
        """ Convert to XML string. Does formatting to a pretty presentation.
        Performance hit because copying the tree...
        @param doctype Documentation type added in the beginning of the return string
        @returns XML presentation of the tree
        """
        self.indent(self)
        res = element_tree.tostring(self)
        res = res.decode('utf-8', 'replace')
        return '%s%s' % (doctype, res)

    self.tostring = tostring

    def getroot():
        """ Get the root node
        @returns Root node instance or None if not found
        """
        if self.nodetype() == 'Document':
            return self._children[-1]

        root = self
        parent = root.getparent()
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if parent == None:
                break
            root = parent
            parent = root.getparent()
        return root
    self.getroot = getroot

    def _do_copy(e):
        _tmp = XMLNode(e.tag, e.attrib, e.nsmap)
        _tmp.__nodeType = e.__nodeType
        _tmp.text = e.text
        _tmp.__parent = e.__parent
        return _tmp

    self._do_copy = _do_copy

    def copy():
        """ Copy this XMLNode. Makes sure everything needed will be copied.
        @returns New XMLNode which is copy of the current
        """
        # Let's do a little bit deeper copy, but not that deep
        tmp = self._do_copy(self)
        tmp.__parent = None
        tmp._children = self._children
        return tmp

    self.copy = copy

    def deepcopy():
        """ Copy this XMLNode.

        Makes sure everything needed will be copied.
        @returns New XMLNode which is copy of the current
        """
        # Custom solution to ensure few things
        tmp = self._do_copy(self)
        for c in self._children:
            newch = self._do_copy(c)
            tmp.addchild(newch)
        return tmp

    self.deepcopy = deepcopy

    # not in ElementTree but in lxml etree

    def set_doctype(doctype):
        if self._name != "DOCUMENT_ROOT":
            fail("can only set doctype on document node")
        self._doctype = doctype
    self.set_doctype = set_doctype
    self.set_docinfo = set_doctype

    """Information about the document provided by parser and DTD."""
    self.docinfo = larky.property(lambda: getattr(self, '_doctype', None))
    self.doctype = larky.property(lambda: getattr(self, '_doctype', None))

    def attach_document(document):
        """Attaches an associated document.
        Arguments:
            document (Document): A document that is associated with the node.
        Returns:
            bool: Returns True if successful; otherwise False.
        """
        if document == None:
            return False
        self._owner_doc = document
        return True
    self.attach_document = attach_document

    def detach_document():
        """Detaches an associated document.
        Returns:
            Document: A document to be detached.
        """
        owner_document = self._owner_doc
        self._owner_doc = None
        return owner_document
    self.deattach_document = detach_document

    # @cython.final
    # cdef getdoctype(self):
    #     # get doctype info: root tag, public/system ID (or None if not known)
    #     cdef tree.xmlDtd* c_dtd
    #     cdef xmlNode* c_root_node
    #     public_id = None
    #     sys_url   = None
    #     c_dtd = self._c_doc.intSubset
    #     if c_dtd is not NULL:
    #         if c_dtd.ExternalID is not NULL:
    #             public_id = funicode(c_dtd.ExternalID)
    #         if c_dtd.SystemID is not NULL:
    #             sys_url = funicode(c_dtd.SystemID)
    #     c_dtd = self._c_doc.extSubset
    #     if c_dtd is not NULL:
    #         if not public_id and c_dtd.ExternalID is not NULL:
    #             public_id = funicode(c_dtd.ExternalID)
    #         if not sys_url and c_dtd.SystemID is not NULL:
    #             sys_url = funicode(c_dtd.SystemID)
    #     c_root_node = tree.xmlDocGetRootElement(self._c_doc)
    #     if c_root_node is NULL:
    #         root_name = None
    #     else:
    #         root_name = funicode(c_root_node.name)
    #     return root_name, public_id, sys_url
    # etree api
    def addnext(elem):
        """
        Adds the element as a following sibling directly after this element.
        This is normally used to set a processing instruction or comment after
        the root node of a document.

        Note that tail text is automatically discarded when adding at the root level.
        """
        self.__parent.insert(self._index + 1, elem)

    self.addnext = addnext

    def addprevious(elem):
        """
        Adds the element as a preceding sibling directly before this element.
        This is normally used to set a processing instruction or comment before
         the root node of a document.

         Note that tail text is automatically discarded when adding at the root level.
        """
        # if type(elem) not in ('Element', 'XMLNode', 'ProcessingInstruction', 'Comment',):
        #     fail(
        #        ("HierarchyRequestError: This node type '{}' cannot insert " +
        #         "as a sibling node of type '{}'")
        #            .format(elem.__name__, self.__name__))
        # elem.attach_document(self.owner_doc)
        self.__parent.insert(self._index, elem)

    self.addprevious = addprevious

    def append(child, reparent=True):
        """Add a subelement node to the end of this element.

        @param child Any instance of XMLNode
        @param reparent True if should be reparented, False to skip
        """
        if reparent:
            # This will call addchild again after reparenting is done
            # but reparent flags as False
            #
            # This way it's safe to assume we just call reparent
            # and do nothing else here.
            child.reparent(self, addchild=False)

        if child not in self._children:
            # If we don't have the reparent flag then do the real add...
            # Prevent adding if already there
            self._children.append(child)

    self.append = append

    def cssselect(expr, translator='xml'):
        """
        Run the CSS expression on this element and its children,
        returning a list of the results.

        Equivalent to cssselect.CSSSelect(expr)(self) -- note
        that pre-compiling the expression can provide a substantial
        speedup.
        """
        # Do the import here to make the dependency optional.
        # from lxml.cssselect import CSSSelector
        # return CSSSelector(expr, translator=translator)(self)
        fail("not implemented")
    self.cssselect = cssselect

    def extend(elements):
        """
        Extends the current children by the elements in the iterable.
        """
        for elem in elements:
            self.append(elem)

    self.extend = extend

    def find(path, namespaces=None):
        """Find first matching element by tag name or path.

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Return the first matching element, or None if no element was found.

        """
        if builtins.isinstance(path, QName):
            path = path.text
        return ElementPath.find(self, path, namespaces)
    self.find = find

    def findall(path, namespaces=None):
        """Find all matching subelements by tag name or path.

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Returns list containing all matching elements in document order.

        """
        if builtins.isinstance(path, QName):
            path = path.text
        return ElementPath.findall(self, path, namespaces)
    self.findall = findall

    def findtext(path, default=None, namespaces=None):
        """Find text for first matching element by tag name or path.

        *path* is a string having either an element tag or an XPath,
        *default* is the value to return if the element was not found,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Return text content of first matching element, or default value if
        none was found.  Note that if an element is found having no text
        content, the empty string is returned.

        """
        return ElementPath.findtext(self, path, default, namespaces)
    self.findtext = findtext

    def finditer(name):
        """ Find iterative data which has given name as tag
        @param name Name of searched iter item
        @returns Item which matches the searched name
        """
        for item in self._children:
            if item.tag == name:
                return item

    self.finditer = finditer

    def get(key, default=None):
        """ get(key, default=None)

        Gets an element attribute.
        """
        return self.attrib.get(key, default)
    self.get = get

    def getchildren():
        """Returns all direct children.  The elements are returned in document
         order.

        :deprecated: Note that this method has been deprecated as of
          ElementTree 1.3 and lxml 2.0.  New code should use
          ``list(element)`` or simply iterate over elements.
        """
        # Make copy of the list to prevent weird
        # reference manipulating errors...
        # return self._children[0:]
        return self._children
    self.getchildren = getchildren

    def getiterator(tag=None, *tags):
        """getiterator(tag=None, *tags)

        Returns a sequence or iterator of all elements in the subtree in
        document order (depth first pre-order), starting with this
        element.

        Can be restricted to find only elements with specific tags,
        see `iter`.

        :deprecated: Note that this method is deprecated as of
          ElementTree 1.3 and lxml 2.0.  It returns an iterator in
          lxml, which diverges from the original ElementTree
          behaviour.  If you want an efficient iterator, use the
          ``element.iter()`` method instead.  You should only use this
          method in new code if you require backwards compatibility
          with older versions of lxml or ElementTree.
        """
        # TODO: port this.
        # if tag != None:
        #     tags += (tag,)
        # return ElementDepthFirstIterator(self, tags)
        return list(self.iter(tag))
    self.getiterator = getiterator

    def getnext():
        """
        Returns the following sibling of this element or None.
        """
        ix = self._index
        if ix == (len(self.__parent) - 1):
            return None
        return self.__parent[ix + 1]

    self.getnext = getnext

    def getparent():
        """ Return node parent
        @returns XMLNode if this XMLNode has a parent, None if this is the root node
        """
        return self.__parent

    self.getparent = getparent

    def getprevious():
        """
        Returns the preceding sibling of this element or None.
        """
        ix = self._index
        if ix == 0:
            return None
        return self.__parent[ix - 1]

    self.getprevious = getprevious

    def getroottree(tree_cls=None):
        """ Get the root node and pass it to the tree_cls

        @returns tree_cls(Root node instance) or None if not found
        """
        if not tree_cls:
            tree_cls = element_tree.ElementTree

        rootnode = self.getroot()
        if rootnode:
            return tree_cls(rootnode)

    self.getroottree = getroottree

    def index(elem, start=None, stop=None):
        """
        Find the position of the child within the parent.
        """
        if stop == None and start in (None, 0):
            return self._children.index(elem)

        if start == None:
            start = 0
        if stop == None:
            stop = len(self._children) - 1

        if elem not in self._children:
            fail("ValueError: list.index(x): x not in list")
        elif elem not in self._children[start:stop]:
            fail(
                "ValueError: list.index(x): x not in slice(start(%d):stop(%d))"
                 % (start, stop)
            )
        else:
            return self._children.index(elem)
    self.index = index

    def insert(index, element, reparent=True):
        """Inserts a subelement at the given position in this element

        @param index Position where to insert
        @param element Item to be added
        @param reparent True if should be reparented, False to skip
        """
        if reparent:
            element.reparent(self, addchild=False)

        if element not in self._children:
            self._children.insert(index, element)

    self.insert = insert

    def items():
        """ Get all attribute items
        @returns List of all attribute items
        """
        return self.attrib.items()

    self.items = items

    def iter(tag=None, *tags):
        """iter(tag=None, *tags)

        Iterate over all elements in the subtree in document order (depth
        first pre-order), starting with this element.

        Can be restricted to find only elements with specific tags:
        pass ``"{ns}localname"`` as tag. Either or both of ``ns`` and
        ``localname`` can be ``*`` for a wildcard; ``ns`` can be empty
        for no namespace. ``"localname"`` is equivalent to ``"{}localname"``
        (i.e. no namespace) but ``"*"`` is ``"{*}*"`` (any or no namespace),
        not ``"{}*"``.

        You can also pass the Element, Comment, ProcessingInstruction and
        Entity factory functions to look only for the specific element type.

        Passing multiple tags (or a sequence of tags) instead of a single tag
        will let the iterator return all elements matching any of these tags,
        in document order.
        """
        if tag == "*":
            tag = None

        items = []
        if tag == None or self.tag == tag:
            items.append(self)
        for el in self:
            if tag == None or el.tag == tag:
                items.append(el)
            qu = el._children[0:]
            for _ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
                if not qu:
                    break
                current = qu.pop(0)
                if tag == None or current.tag == tag:
                    items.append(current)
                qu = list(current._children) + list(qu)
        return items

    self.iter = iter

    def iterancestors(*tags):
        """
        Iterate over the ancestors of this element (from parent to parent).
        """
        x = self.__parent
        res = []
        for _ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
            if x == None:
                break
            if not tags or x.tag in tags:
                res.append(x)
            x = x.getparent()
        return res

    self.iterancestors = iterancestors

    def iterchildren(tag=None, reversed=False, *tags):  # @ReservedAssignment
        """
        Iterate over the children of this element.
        As opposed to using normal iteration on this element, the returned
        elements can be reversed with the 'reversed' keyword and restricted
        to find only elements with a specific tag
        """
        tags = list(tags)
        if tag != None:
            tags.insert(0, tag)
        x = builtins.reversed(self._children) if reversed else iter(self._children)
        if tags:
            return [child for child in x if child.tag in tags]
        else:
            return x

    self.iterchildren = iterchildren

    def iterdescendants(*tags):
        """
        Iterate over the descendants of this element in document order.
        As opposed to el.iter(), this iterator does not yield the element itself.

        The returned elements can be restricted to find only elements with a
        specific tag, see iter.
        """
        stack = list(reversed(self._children))
        nodes = []
        for _ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
            if not stack:
                break
            node = stack.pop()
            if tags and node.tag in tags:
                nodes.append(node)
            else:
                nodes.append(node)
            stack.extend(reversed(node))
        return nodes

    self.iterdescendants = iterdescendants

    def iterfind(path, namespaces=None):
        """Find all matching subelements by tag name or path.

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Return an iterable yielding all matching elements in document order.

        """
        if builtins.isinstance(path, QName):
            path = path.text
        return ElementPath.iterfind(self, path, namespaces)
    self.iterfind = iterfind

    def itersiblings(tag=None, preceding=False, *tags):
        """
        Iterate over the following or preceding siblings of this element.
        The direction is determined by the 'preceding' keyword which defaults
        to False, i.e. forward iteration over the following siblings.

        When True, the iterator yields the preceding siblings in reverse
        document order, i.e. starting right before the current element and
        going backwards.

        Can be restricted to find only elements with a specific tag
        """
        tags = list(tags)
        if tag != None:
            tags.insert(0, tag)
        x = reversed(self.__parent[:self._index]) if preceding else iter(
            self.__parent[self._index + 1:])
        if tags:
            return [sib for sib in x if sib.tag in tags]
        else:
            return x

    self.itersiblings = itersiblings

    def itertext(tag=None, *tags, with_tail=True):
        """itertext(tag=None, *tags, with_tail=True)

        Iterates over the text content of a subtree.

        You can pass tag names to restrict text content to specific elements,
        see `iter`.

        You can set the ``with_tail`` keyword argument to ``False`` to skip
        over tail text.
        """
        if not types.is_string(self.tag) and self.tag != None:
            return

        s = []
        for elem in self.iter(tag, *tags):
            s.append(elem.text)
            if with_tail and elem.tail:
                s.append(elem.tail)
        return s
    self.itertext = itertext

    def keys():
        """keys()

        Gets a list of attribute names."""
        return self.attrib.keys()
    self.keys = keys

    def makeelement(_tag, attrib=None, nsmap=None, **_extra):
        """makeelement(_tag, attrib=None, nsmap=None, **_extra)

        Creates a new element associated with the same document.
        """
        return self.__class__(_tag, attrib, nsmap, **_extra)
    self.makeelement = makeelement

    def remove(child):
        """Remove defined child from this XMLNode's children list (if possible)

        @param child Instance of a XMLNode
        @returns True on success, False otherwise (invalid child, child not under this XMLNode)
        """
        if child.__parent != self:
            return False

        child.__parent = None

        if child not in self._children:
            return False
        self._children.remove(child)
        return True

    self.remove = remove

    def replace(old_element, new_element):
        """
        Replaces a subelement with the element passed as second argument.
        """
        new_element.reparent(self, addchild=False)
        self._children[self._children.index(old_element)] = new_element

    self.replace = replace

    def set(key, val):
        """ set(key, value)

        Sets or overwrite an element attribute.
        """
        self.attrib[key] = val

    self.set = set

    def values():
        """values()

        Gets a list of attribute names."""
        return self.attrib.values()
    self.values = values

    def xpath(_path, namespaces=None, extensions=None, smart_strings=True, **_variables):
        """xpath(_path, namespaces=None, extensions=None, smart_strings=True, **_variables)

        Evaluate an xpath expression using the element as context node.
        """
        # evaluator = XPathElementEvaluator(self, namespaces=namespaces,
        #                                   extensions=extensions,
        #                                   smart_strings=smart_strings)
        # return evaluator(_path, **_variables)
        # TODO(mahmoudimus): should this basically fail?
        if builtins.isinstance(_path, QName):
            _path = _path.text
        return ElementPath.find(self, _path, namespaces=namespaces)
    self.xpath = xpath
    # dunders

    def __contains__(index):
        """ Checks if treenode contains some data
        @param index Key or data to check
        @returns True if data is found, False otherwise
        """
        if self.isdata(index):
            return True
        if index in self._children:
            return True
        for child in self._children:
            if child.isdata(index):
                return True

        if index in self.attrib:
            return True

        return False

    self.__contains__ = __contains__

    def __getitem__(index):
        """ Get certain child by index. Returns the sub-element at the
        given position or the requested slice.

        @param index the index to retrieve from the node's children
        @returns XMLNode instance
        """
        # indexing
        if len(self._children) <= index:
            fail("IndexError: list index out of range")
        return self._children[index]
    self.__getitem__ = __getitem__

    def __delitem__(subelement):
        """__delitem__(subelement)
        Deletes the given subelement or a slice.
        """
        fail("not implemented!")

    self.__delitem__ = __delitem__

    def __setitem__(index, elem):
        """__setitem__(index, elem)

        Replaces the given subelement index or slice.
        """
        elem.reparent(self, addchild=False)
        self._children[index] = elem

    self.__setitem__ = __setitem__

    def __iter__():
        """ Iterates thorough the children
        @returns Iterator
        """
        # use builtins here because iter() is shadowed in this scope
        # for larky
        return builtins.iter(self._children)

    self.__iter__ = __iter__

    def __len__():
        """ Return the length of XMLNode instance, amount of children.
        @returns Length of the XMLNode structure, number of children
        """
        return len(self._children)

    self.__len__ = __len__

    def __bool__():
        return len(self._children)

    self.__bool__ = __bool__

    def __reversed__():
        return builtins.reversed(self._children)

    self.__reversed__ = __reversed__

    def __repr__():
        """ String presentation of this object
        @returns String presentation of this object
        """
        return "%s[%s]" % (self.__name__, "tag=%s, attrib=%s" % (
            ("%s" % self.nodetype())
                .replace(" ", "")
                .replace("'", ""),
            self.attrib
        ))

    self.__repr__ = __repr__

    def __str__():
        """ String presentation of this object
        @returns String presentation of this object
        """
        return self.to_simple_string()

    self.__str__ = __str__
    return self


def Comment(text=None):
    """Comment element factory.

    This function creates a special element which the standard serializer
    serializes as an XML comment.

    *text* is a string containing the comment string.

    """
    # def XMLNode(tag, attrib, nsmap=None, **extra):
    element = element_tree.Comment(text=text, element_factory=XMLNode)
    element.text = text
    return element


def ProcessingInstruction(target, text=None):
    """Processing Instruction element factory.

    This function creates a special element which the standard serializer
    serializes as an XML comment.

    *target* is a string containing the processing instruction, *text* is a
    string containing the processing instruction contents, if any.

    """
    element = element_tree.PI(target, text=text, element_factory=XMLNode)
    element.target = target
    element.data = text
    return element


def CDATA(data):
    """CDATA(data)

    CDATA factory.  This factory creates an opaque data object that
    can be used to set Element text.  The usual way to use it is::
        >>> from xml.etree import ElementTree as etree
        >>> el = etree.Element('content')
        >>> el.text = etree.CDATA('a string')
    """
    element = element_tree.CDATA(data, element_factory=XMLNode)
    return element


def getelementpath(tree, element):
    """getelementpath(tree, element)

    Returns a structural, absolute ElementPath expression to find the
    element.  This path can be used in the .find() method to look up
    the element, provided that the elements along the path and their
    list of immediate children were not modified in between.
    ElementPath has the advantage over an XPath expression (as returned
    by the .getpath() method) that it does not require additional prefix
    declarations.  It is always self-contained.
    """
    root = None

    if not iselement(element):
        fail("ValueError: input is not an Element")
    if tree.getroot() != None:
        root = tree.getroot()
    else:
        fail("ValueError: Element is not in this tree")

    path = []
    c_element = element
    for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
        if c_element == root:
            break
        c_name = c_element.tag
        c_href = _get_ns(c_name)
        # print(c_href)
        tag = _namespaced_name_from_ns_name(c_href, c_name)
        # print(tag)
        if c_href == None:
            c_href = b""  # no namespace (NULL is wildcard)
        # use tag[N] if there are preceding siblings with the same tag
        count = 0
        loc = c_element.getparent().getchildren().index(c_element)
        for i in range(0, loc):
            c_node = c_element.getparent().getchildren()[i]
            if iselement(c_node):
                if _tag_matches(c_node, c_href, c_name):
                    count += 1
        if count:
            tag = "%s[%d]" % (tag, count + 1)
        else:
            # use tag[1] if there are following siblings with the same tag
            end = len(c_element.getparent().getchildren())
            for i in range(loc, end):
                c_node = c_element.getparent().getchildren()[i]
                if iselement(c_node):
                    if _tag_matches(c_node, c_href, c_name):
                        tag += "[1]"
                        break
        path.append(tag)
        c_element = c_element.getparent()
        if c_element == None or not iselement(c_element):
            fail("ValueError: Element is not in this tree.")
    if not path:
        return "."
    path = reversed(path) # .reverse()
    return "/".join(path)


#
# implements the depth-first iterator
#

def ElementDepthFirstIterator(node):
    self = larky.mutablestruct(
        __name__='ElementDepthFirstIterator',
        __class__=ElementDepthFirstIterator
    )

    def __init__(node):
        self.node = node
        self.parents = []
        return self
    self = __init__(node)

    def __iter__():
        return self
    self.__iter__ = __iter__

    def __next__():
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if self.node:
                ret = self.node
                self.parents.append(self.node)
                children = self.node.getchildren()
                self.node = children[0] if children else None
                return ret

            if not self.parents:
                return StopIteration()

            parent = self.parents.pop()
            self.node = parent.getnext()
    self.__next__ = __next__
    return self


def ElementBreadthFirstIterator(node):
    self = larky.mutablestruct(
        __name__='ElementBreadthFirstIterator',
        __class__=ElementBreadthFirstIterator
    )

    def __init__(node):
        self.node = node
        self.parents = []
        return self
    self = __init__(node)

    def __iter__():
        return self
    self.__iter__ = __iter__

    def __next__():
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if self.node:
                ret = self.node
                self.parents.append(self.node)
                self.node = self.node.getnext()
                return ret

            if not self.parents:
                return StopIteration()
            parent = self.parents.pop()
            children = parent.getchildren()
            self.node = children[0] if children else None
    self.__next__ = __next__
    return self


# non standard factories
def DocumentType(name, public_id, system_id, data):
    """Doctype node

    """
    node = XMLNode(DocumentType)
    node.text = name
    node.public_id = public_id
    node.system_id = system_id
    node.data = data
    node._name = "<!DOCTYPE>"
    node.internalDTD = larky.property(lambda: data)
    # print("DocumentType factory:", type(node))
    return node


def Document(encoding=None, standalone=None, version=None, doctype=None):
    """Document
    """
    self = XMLNode(Document)
    self.text = "DOCUMENT_ROOT"
    self._name = "DOCUMENT_ROOT"
    # nodeName = "#document"
    self.encoding = encoding
    self.standalone = standalone
    self.version = version
    self.set_doctype(doctype)

    def xml_comment_factory(data):
        node = Comment(data)
        node._owner_doc = self
        return node
    self.xml_comment_factory = xml_comment_factory

    def xml_processing_instruction_factory(target, text=None):
        node = ProcessingInstruction(target, text=text)
        node.attach_document(self)
        return node
    self.xml_processing_instruction_factory = xml_processing_instruction_factory

    self.createComment = xml_comment_factory
    self.createProcessingInstruction = xml_processing_instruction_factory

    def xml_text_factory(data):
        fail("not implemented")
        # node = Text(data)
        # node._owner_doc = self
        # return node
    self.xml_text_factory = xml_text_factory

    # def xml_element_factory(ns, local):
    #     node = XMLNode(ns, local)
    #     node._owner_doc = self
    #     return node

    def createDocumentFragment():
        fail("not implemented")
    self.createDocumentFragment = createDocumentFragment

    def createElement(tagName):
        e = XMLNode(tagName)
        e._owner_doc = self
        return e
    self.createElement = createElement

    def createTextNode(data):
        if not types.is_string(data):
            fail("TypeError: node contents must be a string")
        fail("not supported. use .text or .tail on XMLNode")
        # t = XMLNode(tagName)
        # t.data = data
        # t.ownerDocument = self
        # return t
    self.createTextNode = createTextNode

    def createCDATASection(data):
        if not types.is_string(data):
            fail("TypeError: node contents must be a string")
        c = CDATA(data)
        c._owner_doc = self
        return c
    self.createCDATASection = createCDATASection

    def createAttribute(qName):
        fail("not supported yet")
        # a = Attr(qName)
        # a.ownerDocument = self
        # a.xml_value = ""
        # return a
    self.createAttribute = createAttribute

    def createElementNS(namespaceURI, qualifiedName):
        prefix, localName = _get_ns_tag(qualifiedName)
        attrib = {'nsmap': {namespaceURI: prefix}}
        e = XMLNode(qualifiedName, attrib)
        e._owner_doc = self
        return e
    self.createElementNS = createElementNS

    def createAttributeNS(namespaceURI, qualifiedName):
        fail("not supported yet")
        # prefix, localName = _get_ns_tag(qualifiedName)
        # a = Attr(qualifiedName, namespaceURI, localName, prefix)
        # a.ownerDocument = self
        # a.xml_value = ""
        # return a
    self.createAttributeNS = createAttributeNS

    # A couple of implementation-specific helpers to create node types
    # not supported by the W3C DOM specs:

    # def _create_entity(self, name, publicId, systemId, notationName):
    #     e = Entity(name, publicId, systemId, notationName)
    #     e.ownerDocument = self
    #     return e
    #
    # def _create_notation(self, name, publicId, systemId):
    #     n = Notation(name, publicId, systemId)
    #     n.ownerDocument = self
    #     return n

    # print("Document factory:", type(node))
    return self


XMLTreeNode = larky.struct(
    __name__='XMLTreeNode',
    get_ns_tag=_get_ns_tag,
    get_ns=_get_ns,
    namespaced_name_from_ns_name=_namespaced_name_from_ns_name,
    tag_matches=_tag_matches,
    ELEMENT_TYPES=ELEMENT_TYPES,
    iselement=iselement,
    getelementpath=getelementpath,
    XMLNode=XMLNode,
    Comment=Comment,
    ProcessingInstruction=ProcessingInstruction,
    DocumentType=DocumentType,
    Document=Document,
)