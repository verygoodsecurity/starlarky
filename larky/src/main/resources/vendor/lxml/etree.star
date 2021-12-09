load("@stdlib//builtins", builtins="builtins")
load("@stdlib//enum", enum="enum")
load("@stdlib//io", io="io")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//re", re="re")
load("@stdlib//sets", sets="sets")
load("@stdlib//string", string="string")
load("@stdlib//types", types="types")
load("@stdlib//xml/etree/ElementPath", ElementPath="ElementPath")
load("@stdlib//xml/etree/ElementTree", element_tree="ElementTree")
load("@stdlib//xmllib", xmllib="xmllib")
load("@stdlib//zlib", zlib="zlib")
load("@vendor//option/result", Result="Result", Ok="Ok", Error="Error")

load("@vendor//lxml/_c14n", c14n="c14n")
load("@vendor//lxml/_xmlwriter", xmlwriter="xmlwriter")


# element_tree = ElementTree
QName = element_tree.QName
tag_regexp = re.compile("{([^}]*)}(.*)")


fullTree = True
# ElementTreeImplementation = ElementTree
# ElementTreeCommentType = ElementTree.Comment("asd").tag


def _invert(d):
    "Invert a dictionary."
    return {v: k for k, v in d.items()}


def _namespaced_name(c_node):
    """
    Port of lxml's _namespacedName function.

    Taken from:
       https://github.com/lxml/lxml/blob/0fec986cff7e0078fcc9aff1661dedd09f10560c/src/lxml/etree.pyx#L1417-L1430
    """
    name = c_node.name
    if not c_node.ns or not c_node.ns['href']:
        return name

    href = c_node.ns['href']
    s = "{%s}%s" % (href, name)
    # if href/name is bytelike, then return
    # s as a utf-8encoded string
    return s


def fixname(name, split=None):
    # xmllib in 2.0 and later provides limited (and slightly broken)
    # support for XML namespaces.
    if " " not in name:
        return name
    if not split:
        split = name.split
    return "{%s}%s" % tuple(split(" ", 1))


def _get_ns_tag(tag):
    """Split the namespace URL out of a fully-qualified lxml tag
    name.

    Copied from lxml's src/lxml/sax.py and modified for larky
    """
    # this is a special Comment, ProcessingInstruction, etc.
    # if tag == None:
    #     return None, ""
    if types.is_function(tag) or tag[0] != '{':
        return None, tag
    return tuple(tag[1:].split('}', 1))


def _get_ns(element):
    if type(element) == 'QName':
        ns_utf, tag_utf = _get_ns_tag(element.text)
    else:
        ns_utf, tag_utf = _get_ns_tag(element)

    if ns_utf:
        return ns_utf
    if not element.nsmap:
        return None
    for pfx in element.nsmap:
        return element.nsmap[pfx]
    # (((c_node)->ns == 0) ? 0 : ((c_node)->ns->href))


# equivalent to lxml's _namespacedName
def _namespaced_name_from_ns_name(href, name):
    if href == None:
        return name
    if type(name) == 'QName':
        ns_utf, tag_utf = _get_ns_tag(name.text)
    else:
        ns_utf, tag_utf = _get_ns_tag(name)
    # return ("{%s}%s" % (ns_utf, tag_utf)) if ns_utf else tag_utf
    return "{%s}%s" % (ns_utf, tag_utf)


_IterWalkState = enum.Enum('_IterWalkState', [
    ('PRE', 0),
    ('POST', 1)
])


def _iterwalk(_element, _events, _tag):
    q = [(_IterWalkState.PRE, (_element, _events, _tag,))]
    result = []
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not q:
            break
        state, (element, events, tag) = q.pop(0)
        include = tag == None or element.tag == tag

        if state == _IterWalkState.PRE:
            if include and "start" in events:
                result.append(("start", element,))

            children = [
                (_IterWalkState.PRE, (e, events, tag,))
                for e in element
            ]
            # post visit
            children.append((_IterWalkState.POST, (element, events, tag,)))
            q = children + q
        elif state == _IterWalkState.POST:
            if include:
                result.append(("end", element))
    return result


def iterwalk(element_or_tree, events=("end",), tag=None):
    """A tree walker that generates events from an existing tree as
    if it was parsing XML data with iterparse().

    Drop-in replacement for lxml.etree.iterwalk.
    """
    if iselement(element_or_tree):
        element = element_or_tree
    else:
        element = element_or_tree.getroot()
    if tag == "*":
        tag = None
    return [item for item in _iterwalk(element, events, tag)]


def _tree_to_target(element, target):
    for event, elem in iterwalk(element, events=('start', 'end', 'start-ns', 'comment', 'pi')):
        attrib = dict(**elem.attrib)
        attrib["nsmap"] = elem.ns
        text = None
        if event == 'start':
            target.start(elem.tag, attrib)
            text = elem.text
        elif event == 'end':
            target.end(elem.tag)
            text = elem.tail
        elif event == 'start-ns':
            target.start_ns(*elem)
            continue
        elif event == 'comment':
            target.comment(elem.text)
            text = elem.tail
        elif event == 'pi':
            target.pi(elem.target, elem.text)
            text = elem.tail
        if text:
            target.data(text)
    return target.close()


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
        children = instance._children
        if len(children) == 1:
            return children[0].getdata()
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
        self._flags = []

        self.data = None
        # self.text = None
        # self.tail = None

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
        if types.is_string(tag):
            qname = fixname(tag)
            self.tag = qname
            self._href, tag = _get_ns_tag(qname)
            self._prefix, _ = split_qname(tag)
        else:
            self.tag = tag
            self._href = None
            self._prefix = None
        # get the ns/prefix (if any)
        # self._href, self._name = _get_ns_tag(self.tag)  # won't change

        # A dict holding name -> value pairs for attributes of the node
        attrib = attrib or {}
        attrib.update(extra)
        self.attrib = dict(**attrib)

        # nsmap's type: Dict["http://example.com/ns/foo", "x"]
        self._nsmap = self.attrib.pop('nsmap', {}) or {}
        if self._nsmap and not self._href and not self._prefix:
            self._prefix = ""
            self._href = _invert(self._nsmap).get(self._prefix)
            if self._href:
                self.tag = "{%s}%s" % (self._href, self.tag)

        # used in _build_nsmap below (accessible through self.nsmap property)
        self.__cached_nsmap = None
        # used in prefix below
        self.__cached_prefix = None

        # A list of child nodes of the current node. This must include all
        # elements but not necessarily other node types.
        self._children = []
        # A list of miscellaneous flags that can be set on the node.
        self._flags = []

        # self.text = None
        # self.tail = None
        # Raw data representing the underlying node data
        self.data = self.attrib.pop('data', None)

        self._doctype = None
        self._owner_doc = None
        self.__parent = self.attrib.pop('parent', None)
        self.__nodeType = self.attrib.pop('nodetype', None)
        return self
    self = __init__(tag, attrib, **extra)

    # read-only properties
    self.ns = larky.property(lambda: dict(**self._nsmap))
    self.name = larky.property(lambda: self.tag)
    self.qname = larky.property(lambda: QName(self))
    self.prefix = larky.property(lambda: split_qname(self.qname)[0])
    self.href = larky.property(lambda: self._href)
    self.parent = larky.property(lambda: self.__parent)
    self._index = larky.property(lambda: self.parent.index(self) if self.parent else 0)

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
    # aliased
    self._doc = larky.property(_owner_document)
    self.doc = larky.property(_owner_document)

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

    def cloneNode():
        """Return a shallow copy of the current node i.e. a node with the same
        name and attributes but with no parent or child nodes
        """
        fail("NotImplementedError")
    self.cloneNode = cloneNode

    def hasContent():
        """Return true if the node has children or text"""
        return bool(self.text or len(self._children) or self.tail)
    self.hasContent = hasContent

    def appendvalue(value):
        """ Set value of the Node, or the text
        @param value Any value
        """
        fail("do not call")
        self.text = "%s%s" % ('' if not self.text else self.text, value)

    self.appendvalue = appendvalue

    def setvalue(value):
        """ Set value of the Node, or the text
        @param value Any value
        """
        fail("do not call")
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

    # def getdata():
    #     """ Get the data under this XMLNode
    #     @returns Data set under this XMLNode
    #     """
    #     return self.tag
    #
    # self.getdata = getdata
    #
    # def setdata(data):
    #     """ Set node data, can be anything
    #     @param data Any data to set under this XMLNode
    #     """
    #     self.tag = data
    #
    # self.setdata = setdata

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
        root.append( a )
        root.append( b )
        root.append( c )
        a.append( XMLNode("Test") )
        a.append( XMLNode("Test2") )
        n = TreeNode("Test")
        b.append( n )
        n.append( XMLNode("SubTest") )
        n.append( XMLNode("SubTest2") )
        c.append( XMLNode("Test") )
        c.append( XMLNode("Other") )

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

    def gettextaslist(reverse=False):
        # iterate through entire children
        if not self._children:
            return []

        _txt = []
        iterable = self._children if not reverse else reversed(self._children)
        for c in iterable:
            if c.nodetype() != 'Text':
                break
            text = c.data
            _txt.append(text)
        return _txt
    self.gettextaslist = gettextaslist

    def gettext(strip=None):
        """ Return value of the XMLNode
        @returns Value of the XMLNode
        """
        # iterate through entire children
        return self.data

    self.gettext = gettext

    def settext(data):
        """ set value of the XMLNode
        @returns None
        """
        text = Text(data)
        text.attach_document(self.owner_doc)
        self.insertText(text)
        # self.data = data
    self.settext = settext

    self.text = larky.property(gettext, self.settext)

    def gettail(strip=None):
        _txt = []
        for text in self.gettextaslist(reverse=True):
            # print(repr(self), repr(text))
            if strip:
                if strip == True:
                    text = text.strip()
                else:
                    text = strip(text)
            _txt.append(text)
        return ''.join(_txt)
    self.gettail = gettail
    self.tail = larky.property(gettail)

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
        ntype = self.tag
        if types.is_function(ntype):
            ntype = larky.impl_function_name(ntype)
        return ntype
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
            self.__parent.append(self, reparent=False)

        # clear cached nsmap which traverses parents.
        self.__cached_nsmap = None
    self.reparent = reparent
    self.reparentChildren = reparent

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
        res = tostring(self)
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
        res = tostring(self)
        res = res.decode('utf-8', 'replace')
        return '%s%s' % (doctype, res)

    self.tostring = tostring

    def getroot():
        """ Get the root node
        @returns Root node instance or None if not found
        """
        current = self
        # print("current.nodetype():", current.nodetype())
        if current.nodetype() != 'Document':
            current = self.owner_doc
            # print("current owner doc:", current)

        for c in current:
            # print("current.children", c.nodetype())
            if c.nodetype() not in (
                    'ProcessingInstruction',
                    'Comment',
                    'CDATA',
                    'DocumentType',
            ):
                return c
        #
        # root = self
        # parent = root.getparent()
        # for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        #     if parent == None:
        #         break
        #     root = parent
        #     parent = root.getparent()
        # return root
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
            tmp.append(newch)
        return tmp

    self.deepcopy = deepcopy

    # not in ElementTree but in lxml etree

    def set_doctype(doctype):
        if self._name != "DOCUMENT_ROOT":
            fail("can only set doctype on document node")
        # if we already have a self._doctype..
        if self._doctype:
            self.replace(self._doctype, doctype)
        elif doctype != None:
            self.append(doctype)
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
        # print("child ", repr(child), "appending to ", repr(self))
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
    self.appendChild = append

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

    def next_sibling():
        """
        Returns the following sibling of this element or None.
        """
        ix = self._index
        if not self.parent or ix == (self.parent.numchildren() - 1):
            return None
        return self.__parent[ix + 1]

    self.next_sibling = next_sibling

    def getparent():
        """ Return node parent
        @returns XMLNode if this XMLNode has a parent, None if this is the root node
        """
        return self.__parent

    self.getparent = getparent

    def previous_sibling():
        """
        Returns the preceding sibling of this element or None.
        """
        ix = self._index
        if ix == 0:
            return None
        return self.__parent[ix - 1]

    self.previous_sibling = previous_sibling

    def last_child():
        if self._children:
            return self._children[-1]
    self.last_child = last_child

    def first_child():
        if self._children:
            return self._children[0]
    self.first_child = first_child

    def getroottree():
        """ Get the root node and pass it to the tree_cls

        @returns tree_cls(Root node instance) or None if not found
        """
        rootnode = self.getroot()
        if rootnode:
            return XMLTree(rootnode)

    self.getroottree = getroottree

    def index(elem, start=None, stop=None):
        """
        Find the position of the child within the parent.
        """
        if not elem:
            return -1

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

    def insertText(data, insertBefore=None):
        """Insert data as text in the current node, positioned before the
        start of node insertBefore or to the end of the node's text.

        :arg data: the data to insert

        :arg insertBefore: The reference node you want to insert the text
                           before (the tail of the previous sibling of
                           the reference node).

                           If False, then it appends to the current
                           node's text.
        """
        if self.numchildren() == 0:
            self.appendChild(data)
        elif insertBefore == None:
            # if self.getparent():
            #     self.addnext(data)
            # else:
            self.appendChild(data)
            # self._children[-1].appendChild(data)
        else:
            index = self.index(insertBefore)
            if index > 0:
                self._children[index - 1].appendChild(data)
            else:
                self.appendChild(data)
        # if self.numchildren() == 0:
        #     self.text = (self.text or "") + data
        # elif insertBefore == None:
        #     # Insert the text as the tail of the last child element
        #     tail = self._children[-1].tail
        #     self._children[-1].tail = (tail or "") + data
        # else:
        #     # Insert the text before the specified node
        #     index = self.index(insertBefore)
        #     if index > 0:
        #         tail = self._children[index - 1].tail
        #         self._children[index - 1].tail = (tail or "") + data
        #     else:
        #         self.text = (self.text or "") + data
    self.insertText = insertText

    def insertBefore(node, refNode):
        """Insert node as a child of the current node, before refNode in the
        list of child nodes. Raises ValueError if refNode is not a child of
        the current node

        :arg node: the node to insert

        :arg refNode: the child node to insert the node before

        """
        idx = self.index(refNode)
        self.insert(idx, node)
    self.insertBefore = insertBefore

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
    self.removeChild = remove

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


def iselement(element):
    """iselement(element)
    Checks if an object appears to be a valid element object or
    if *element* appears to be an Element.
    """
    return hasattr(element, "tag") or type(element) == 'XMLNode'


def _tag_matches(c_node, c_href, c_name):
    """Tests if the node matches namespace URI and tag name.
    A node matches if it matches both c_href and c_name.
    A node matches c_href if any of the following is true:
    * c_href is NULL
    * its namespace is NULL and c_href is the empty string
    * its namespace string equals the c_href string
    A node matches c_name if any of the following is true:
    * c_name is NULL
    * its name string equals the c_name string
    """
    if c_node == None:
        return False
    if not iselement(c_node):
        # not an element, only succeed if we match everything
        return c_name == None and c_href == None
    if c_name == None:
        if c_href == None:
            # always match
            return True
        else:
            c_node_href = _get_ns(c_node)
            if c_node_href == None:
                return c_href[0] == ""
            else:
                return c_node_href == c_href
    elif c_href == None:
        if _get_ns(c_node) != None:
            return False
        return c_node.tag == c_name
    elif c_node.tag == c_name:
        c_node_href = _get_ns(c_node)
        if c_node_href == None:
            return c_href[0] == ""
        else:
            return c_node_href == c_href
    else:
        return False


def XMLTree(root=None):
    """
    A DOM Tree

    """
    self = larky.mutablestruct(__name__='XMLTree', __class__=XMLTree)

    def _setroot(root):
        """
        Relocate the ElementTree to a new root node.
        In lxml, _context_node == root
        """
        self._root = root
        self._doc = getattr(root, "owner_doc", None)  # for lxml compatibility purposes
        # print("setting root!", repr(root), "document:", repr(self._doc))
    self._setroot = _setroot

    def __init__(root):
        self._doc = None
        self._root = None
        self._setroot(root)
        return self
    self = __init__(root)

    """Information about the document provided by parser and DTD."""
    self.docinfo = larky.property(lambda: getattr(self._doc, 'docinfo', None))

    def getroot():
        """
        Gets the root element for this tree.
        """
        # return self._context_node # in lxml => _context_node is root
        return self._root
    self.getroot = getroot
    self._context_node = getroot

    def __repr__():
        return "<%s at larky-address>" % self.__name__
    self.__repr__ = __repr__

    def __str__():
        return "<%s mutablestruct @ larky-address>" % self.__name__
    self.__str__ = __str__

    def _absfindpath(path):
        # work around ElementPath quirks
        if path.startswith("//"):
            return "." + path  # // is descendant-*or-self*, so this is legal!
        elif path.startswith("/"):
            fail("NotImplementedError: absolute paths are not implemented!")
        return path
    self._absfindpath = _absfindpath

    def parse(source, parser, base_url=None):
        """
        Updates self with the content of source and returns its root.
        """
        close_src = False
        if not hasattr(source, "read"):
            close_src = True

        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            data = source.read(65536)
            if not data:
                break
            parser.feed(data)
        tree = parser.close()
        self._setroot(tree.getroot())
        if close_src:
            source.close()
        return self.getroot()
    self.parse = parse

    def write(
        file,
        encoding=None,
        method="xml",
        pretty_print=False,
        xml_declaration=None,
        with_tail=True,
        standalone=None,
        doctype=None,
        compression=0,
        exclusive=False,
        inclusive_ns_prefixes=None,
        with_comments=True,
        strip_text=False,
        docstring=None,
        **options
    ):
        """
        Write the tree to a filename, file or file-like object.

        Defaults to ASCII encoding and writing a declaration as needed.

        The keyword argument 'method' selects the output method:
        'xml', 'html', 'text' or 'c14n'.  Default is 'xml'.

        With ``method="c14n"`` (C14N version 1), the options ``exclusive``,
        ``with_comments`` and ``inclusive_ns_prefixes`` request exclusive
        C14N, include comments, and list the inclusive prefixes respectively.

        With ``method="c14n2"`` (C14N version 2), the ``with_comments`` and
        ``strip_text`` options control the output of comments and text space
        according to C14N 2.0.

        Passing a boolean value to the ``standalone`` option will
        output an XML declaration with the corresponding
        ``standalone`` flag.

        The ``doctype`` option allows passing in a plain string that will
        be serialised before the XML tree.  Note that passing in non
        well-formed content here will make the XML output non well-formed.
        Also, an existing doctype in the document tree will not be removed
        when serialising an ElementTree instance.

        The ``compression`` option enables GZip compression level 1-9.

        The ``inclusive_ns_prefixes`` should be a list of namespace strings
        (i.e. ['xs', 'xsi']) that will be promoted to the top-level element
        during exclusive C14N serialisation.  This parameter is ignored if
        exclusive mode=False.

        If exclusive=True and no list is provided, a namespace will only be
        rendered if it is used by the immediate parent or one of its attributes
        and its prefix and values have not already been rendered by an ancestor
        of the namespace node's parent element.
        """
        if compression == None or compression < 0:
            compression = 0

        if docstring != None and doctype == None:
            # "The 'docstring' option is deprecated. Use 'doctype' instead."
            doctype = docstring

        tostr = tostring(
            self,
            encoding=encoding,
            method=method,
            xml_declaration=xml_declaration,
            pretty_print=pretty_print,
            with_tail=with_tail,
            standalone=standalone,
            doctype=doctype,
            exclusive=exclusive,
            inclusive_ns_prefixes=inclusive_ns_prefixes,
            with_comments=with_comments,
            strip_text=strip_text,
            **options)

        if compression != 0:
            co = zlib.compressobj(level=compression)
            tostr = (co.compress(tostr) + co.flush())

        file.write(tostr)

    self.write = write

    def getpath(element):
        """
        Returns a structural, absolute XPath expression to find the element.

        For namespaced elements, the expression uses prefixes from the
        document, which therefore need to be provided in order to make any
        use of the expression in XPath.

        Also see the method getelementpath(self, element), which returns a
        self-contained ElementPath expression.
        """
        fail("NotImplementedError")
    self.getpath = getpath

    def getelementpath(element):
        """
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
        if self.getroot() != None:
            root = self.getroot()
        else:
            fail("ValueError: Element is not in this tree")

        path = []
        c_element = element
        tag = None
        for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
            if c_element == root:
                break
            c_name = c_element.tag
            c_href = _get_ns(c_name)
            # print(c_href)
            tag = _namespaced_name_from_ns_name(c_href, c_name)
            # print(tag)
            if c_href == None:
                c_href = ""  # no namespace (NULL is wildcard)
            # use tag[N] if there are preceding siblings with the same tag

            count = 0
            c_node = c_element.previous_sibling()
            for _while2_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
                if c_node == None:
                    break
                if iselement(c_node):
                    if _tag_matches(c_node, c_href, c_name):
                        count += 1
                c_node = c_node.previous_sibling()
            # count = 0
            # loc = c_element.getparent().getchildren().index(c_element)
            # for i in range(0, loc):
            #     c_node = c_element.getparent().getchildren()[i]
            #     if iselement(c_node):
            #         if _tag_matches(c_node, c_href, c_name):
            #             count += 1
            if count:
                tag = "%s[%d]" % (tag, count + 1)
            else:
                # use tag[1] if there are following siblings with the same tag
                # if c_node.type == tree.XML_ELEMENT_NODE:
                #     if _tagMatches(c_node, c_href, c_name):
                #         tag += '[1]'
                #         break
                # c_node = c_node.next
                c_node = c_element.next_sibling()
                for _while2_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
                    if c_node == None:
                        break
                    if iselement(c_node):
                        if _tag_matches(c_node, c_href, c_name):
                            tag += '[1]'
                            break
                    c_node = c_node.next_sibling()
                # end = len(c_element.getparent().getchildren())
                # for i in range(loc, end):
                #     c_node = c_element.getparent().getchildren()[i]
                #     if iselement(c_node):
                #         if _tag_matches(c_node, c_href, c_name):
                #             tag += "[1]"
                #             break

            c_element = c_element.getparent()
            if c_element == None or not iselement(c_element):
                fail("ValueError: Element is not in this tree.")

        path.append(tag)
        c_element = c_element.getparent()
        if c_element == None or not iselement(c_element):
            fail("ValueError: Element is not in this tree.")

        if not path:
            return "."
        path = reversed(path)
        return "/".join(path)
    self.getelementpath = getelementpath

    def iter(tag=None, *tags):
        """iter(self, tag=None, *tags)

        Creates an iterator for the root element.  The iterator loops over
        all elements in this tree, in document order.  Note that siblings
        of the root element (comments or processing instructions) are not
        returned by the iterator.

        Can be restricted to find only elements with specific tags,
        see `_Element.iter`.
        """
        root = self.getroot()
        if root == None:
            return []
        tags = list(tags)
        if tag != None:
            tags.insert(0, tag)
        return root.iter(*tags)
    self.iter = iter

    def iterfind(path, namespaces=None):
        """
        Iterates over all elements matching the ElementPath expression. Same as getroot().iterfind(path).
        """
        return self._root.iterfind(path, namespaces)
    self.iterfind = iterfind

    def find(path, namespaces=None):
        """find(path, namespaces=None)

        Finds the first toplevel element with given tag.  Same as
        ``tree.getroot().find(path)``.

        The optional ``namespaces`` argument accepts a
        prefix-to-namespace mapping that allows the usage of XPath
        prefixes in the path expression.
        """
        root = self.getroot()
        if types.is_string(path):
            path = self._absfindpath(path)
        return root.find(path, namespaces)
    self.find = find

    def findtext(path, default=None, namespaces=None):
        """findtext(path, default=None, namespaces=None)

        Finds the text for the first element matching the ElementPath
        expression.  Same as getroot().findtext(path)

        The optional ``namespaces`` argument accepts a
        prefix-to-namespace mapping that allows the usage of XPath
        prefixes in the path expression.
        """
        root = self.getroot()
        if types.is_string(path):
            path = self._absfindpath(path)
        return root.findtext(path, default, namespaces)
    self.findtext = findtext

    def findall(path, namespaces=None):
        """findall(path, namespaces=None)

        Finds all elements matching the ElementPath expression.  Same as
        getroot().findall(path).

        The optional ``namespaces`` argument accepts a
        prefix-to-namespace mapping that allows the usage of XPath
        prefixes in the path expression.
        """
        root = self.getroot()
        if types.is_string(path):
            path = self._absfindpath(path)
        return root.findall(path, namespaces)
    self.findall = findall

    def iterfind(path, namespaces=None):
        """"iterfind(self, path, namespaces=None)

        Iterates over all elements matching the ElementPath expression.
        Same as getroot().iterfind(path).

        The optional ``namespaces`` argument accepts a
        prefix-to-namespace mapping that allows the usage of XPath
        prefixes in the path expression.
        """
        root = self.getroot()
        if types.is_string(path):
            path = self._absfindpath(path)
        return root.iterfind(path, namespaces)
    self.iterfind = iterfind

    def xpath(
        path, namespaces=None, extensions=None, smart_strings=True, **_variables
    ):
        """
        XPath evaluate in context of document.

        ``namespaces`` is an optional dictionary with prefix to namespace URI
        mappings, used by XPath.  ``extensions`` defines additional extension
        functions.

        Returns a list (nodeset), or bool, float or string.

        In case of a list result, return Element for element nodes,
        string for text and attribute values.

        Note: if you are going to apply multiple XPath expressions
        against the same document, it is more efficient to use
        XPathEvaluator directly.
        """
        fail("NotImplementedError")
    self.xpath = xpath

    def xslt(_xslt, extensions=None, access_control=None, **_kw):
        """
        Transform this document using other document.

        xslt is a tree that should be XSLT
        keyword parameters are XSLT transformation parameters.

        Returns the transformed tree.

        Note: if you are going to apply the same XSLT stylesheet against
        multiple documents, it is more efficient to use the XSLT
        class directly.
        """
        fail("NotImplementedError")
    self.xslt = xslt

    def relaxng(relaxng):
        """
        Validate this document using other document.

        The relaxng argument is a tree that should contain a Relax NG schema.

        Returns True or False, depending on whether validation
        succeeded.

        Note: if you are going to apply the same Relax NG schema against
        multiple documents, it is more efficient to use the RelaxNG
        class directly.
        """
        fail("NotImplementedError")
    self.relaxng = relaxng

    def xmlschema(xmlschema):
        """
        Validate this document using other document.

        The xmlschema argument is a tree that should contain an XML Schema.

        Returns True or False, depending on whether validation
        succeeded.

        Note: If you are going to apply the same XML Schema against
        multiple documents, it is more efficient to use the XMLSchema
        class directly.        """
        fail("NotImplementedError")
    self.xmlschema = xmlschema

    def xinclude():
        """
        Process the XInclude nodes in this document and include the
        referenced XML fragments.

        There is support for loading files through the file system, HTTP and
        FTP.

        Note that XInclude does not support custom resolvers in Python space
        due to restrictions of libxml2 <= 2.6.29.
        """
        fail("NotImplementedError")
    self.xinclude = xinclude

    def write_c14n(
        file,
        exclusive=False,
        with_comments=True,
        compression=0,
        inclusive_ns_prefixes=None,
    ):
        """
        C14N write of document. Always writes UTF-8.

        The ``compression`` option enables GZip compression level 1-9.

        The ``inclusive_ns_prefixes`` should be a list of namespace strings
        (i.e. ['xs', 'xsi']) that will be promoted to the top-level element
        during exclusive C14N serialisation.  This parameter is ignored if
        exclusive mode=False.

        If exclusive=True and no list is provided, a namespace will only be
        rendered if it is used by the immediate parent or one of its attributes
        and its prefix and values have not already been rendered by an ancestor
        of the namespace node's parent element.

        NOTE: This method is deprecated as of lxml 4.4 and will be removed in a
        future release.  Use ``.write(f, method="c14n")`` instead.
        """
        if compression == None or compression < 0:
            compression = 0
        return self.write(file, method="c14n",
                          exclusive=exclusive,
                          with_comments=with_comments,
                          compression=compression,
                          inclusive_ns_prefixes=inclusive_ns_prefixes)

    self.write_c14n = write_c14n
    return self


def iselementtree(element_or_tree):
    """iselementtree(element_or_tree)
    Checks if an object appears to be a valid element tree object or
    if *element_or_tree* appears to be an Element.
    """
    return type(element_or_tree) == 'XMLTree'


def _convert_ns_prefixes(c_dict, ns_prefixes):
    keys = sets.Set(c_dict.keys())
    return keys.intersection(sets.Set(ns_prefixes)).to_list()


def tofilelikeC14N(f, element, exclusive, with_comments,
                     compression, inclusive_ns_prefixes):

    if compression == None or compression < 0:
        compression = 0

    # c_doc = element if iselement(element) else element.getroot()
    # c_doc = element.owner_doc if iselement(element) else element.getroot().owner_doc
    # c_doc = XMLTree(element) if iselement(element) else element
    c_doc = element if iselement(element) else element.getroot().owner_doc
    # c_inclusive_ns_prefixes = (
    #     # TODO: c_doc.dict == all namespaces?
    #     _convert_ns_prefixes(c_doc, inclusive_ns_prefixes)
    #     if inclusive_ns_prefixes else None
    # )
    if not hasattr(f, 'write'):
        fail("TypeError: File (or something that has 'write') expected, got %s " % type(f))

    # _qnames, namespaces = element_tree._namespaces(element.getroot(), None)
    if exclusive:
        c14n.Canonicalize(
            c_doc, f,
            comments=with_comments,
            unsuppressedPrefixes=inclusive_ns_prefixes
        )
    else:
        c14n.Canonicalize(c_doc, f, comments=with_comments) #, nsdict=namespaces)
    # print("ElementC14N.Canonicalize", "\n", sio.getvalue())
    # ElementC14N.write(c_doc,
    #                   f,
    #                   # subset=with_comments,
    #                   exclusive=exclusive,
    #                   inclusive_namespaces=inclusive_ns_prefixes)

    if compression != 0:
        co = zlib.compressobj(level=compression)
        out = co.compress(f.getvalue()) + co.flush()
        f.seek(0)
        f.write(out)


def _tostring(element, encoding, doctype,
              method, write_xml_declaration,
              write_complete_document, pretty_print,
              with_tail, standalone, **options):
    """Serialize an element to an encoded string representation of its XML
    tree.
    """
    if element == None:
        return None
    if method == "text":
        fail("method '%s' is not supported. please file a PR!" % method)
        # return _textToString(element._c_node, encoding, with_tail)
    if encoding == None or encoding == "unicode":
        c_enc = None
        encoding = "unicode"
    else:
        c_enc = encoding
    if doctype == None:
        c_doctype = None
    else:
        c_doctype = doctype
    # it is necessary to *and* find the encoding handler *and* use
    # encoding during output
    # print("encoding", encoding, type(encoding))
    if encoding != None and types.is_bytelike(encoding):
        encoding = encoding.decode('utf-8')
    c_result_buffer = io.StringIO()
    writer = xmlwriter.XMLWriter(element)
    writer(
        file=c_result_buffer,
        encoding=c_enc,
        method=method,
        xml_declaration=write_xml_declaration,
        pretty_print=pretty_print,
        with_tail=with_tail,
        standalone=standalone,
        doctype=c_doctype,
        write_complete_document=write_complete_document,
        **options
    )
    result = c_result_buffer.getvalue()
    if encoding == "unicode":
        result = result.decode('utf-8')
    return result


def tostringC14N(element_or_tree, exclusive, with_comments, inclusive_ns_prefixes):
    buf = io.StringIO()
    tofilelikeC14N(buf, element_or_tree, exclusive, with_comments, 0, inclusive_ns_prefixes)
    return buf.getvalue()


def tostring(element_or_tree,
            encoding=None,
            method="xml",
            xml_declaration=None,
            pretty_print=False,
            with_tail=True,
            standalone=None,
            doctype=None,
            # method='c14n'
            exclusive=False, inclusive_ns_prefixes=False,
            # method='c14n2'
            with_comments=True, strip_text=False,
            **options):
    """
    Namespace-aware serialization of either an XML elementtree or an element
    to an encoded string representation of its XML tree.

    Defaults to ASCII encoding without XML declaration.  This
    behaviour can be configured with the keyword arguments 'encoding'
    (string) and 'xml_declaration' (bool).  Note that changing the
    encoding to a non UTF-8 compatible encoding will enable a
    declaration by default.

    You can also serialise to a Unicode string without declaration by
    passing the name ``'unicode'`` as encoding (or the ``str`` function
    in Py3 or ``unicode`` in Py2).  This changes the return value from
    a byte string to an unencoded unicode string.

    The keyword argument 'pretty_print' (bool) enables formatted XML.

    The keyword argument 'method' selects the output method: 'xml',
    'html', plain 'text' (text content without tags), 'c14n' or 'c14n2'.
    Default is 'xml'.

    With ``method="c14n"`` (C14N version 1), the options ``exclusive``,
    ``with_comments`` and ``inclusive_ns_prefixes`` request exclusive
    C14N, include comments, and list the inclusive prefixes respectively.

    With ``method="c14n2"`` (C14N version 2), the ``with_comments`` and
    ``strip_text`` options control the output of comments and text space
    according to C14N 2.0.

    Passing a boolean value to the ``standalone`` option will output
    an XML declaration with the corresponding ``standalone`` flag.

    The ``doctype`` option allows passing in a plain string that will
    be serialised before the XML tree.  Note that passing in non
    well-formed content here will make the XML output non well-formed.
    Also, an existing doctype in the document tree will not be removed
    when serialising an ElementTree instance.

    You can prevent the tail text of the element from being serialised
    by passing the boolean ``with_tail`` option.  This has no impact
    on the tail text of children, which will always be serialised.
    """

    write_declaration = xml_declaration
    is_standalone = standalone
    # C14N serialisation
    if method in ('c14n', 'c14n2'):
        if encoding != None:
            fail("ValueError: Cannot specify encoding with C14N")
        if xml_declaration:
            fail("ValueError: Cannot enable XML declaration in C14N")
        if method == 'c14n':
            return tostringC14N(element_or_tree, exclusive, with_comments, inclusive_ns_prefixes)
        else:
            # element_or_tree.
            out = io.BytesIO()
            target = element_tree.C14NWriterTarget(
                out.write,
                with_comments=with_comments,
                strip_text=strip_text
            )
            _tree_to_target(element_or_tree, target)
            return out.getvalue()
    if not with_comments:
        fail("ValueError: Can only discard comments in C14N serialisation")
    if strip_text:
        fail("ValueError: Can only strip text in C14N 2.0 serialisation")
    if encoding != None and encoding.lower() == 'unicode':
        if xml_declaration:
            fail("ValueError: Serialisation to unicode must not request an XML declaration")
        write_declaration = False
        encoding = "unicode"
    elif xml_declaration == None:
        # by default, write an XML declaration only for non-standard encodings
        write_declaration = encoding != None and encoding.upper() not in ('ASCII', 'UTF-8', 'UTF8', 'US-ASCII')
    else:
        write_declaration = xml_declaration
    if encoding == None:
        # TODO(mahmoudimus): support encoding
        # owner_doc = getattr(element_or_tree, 'owner_doc', None)
        # if not owner_doc:
        #     owner_doc = getattr(element_or_tree, '_doc', None)
        #
        # if not owner_doc or not getattr(owner_doc, 'encoding', None):
        #     encoding = 'ASCII'
        # else:
        #     encoding = owner_doc.encoding
        encoding = 'ASCII'
    if standalone == None:
        is_standalone = None
    elif standalone:
        write_declaration = True
        is_standalone = True
    else:
        write_declaration = True
        is_standalone = False

    if iselement(element_or_tree):
        doc = element_or_tree.owner_doc
        return _tostring(XMLTree(element_or_tree), encoding, doctype, method,
                         write_declaration, False, pretty_print, with_tail,
                         is_standalone, **options)
    elif iselementtree(element_or_tree):
        doc = element_or_tree._doc
        return _tostring(element_or_tree,
                         encoding, doctype, method, write_declaration, True,
                         pretty_print, with_tail, is_standalone, **options)
    else:
        fail("TypeError: Type '%s' cannot be serialized." % (type(element_or_tree)))


def ElementTree(element=None, file=None, parser=None):
    """ElementTree(element=None, file=None, parser=None)

    ElementTree wrapper class.
    """

    if element != None:
        return XMLTree(element)
    elif file != None:
        return parse(file, parser=parser, base_url=None)

    return XMLTree()


def parse(source, parser=None, base_url=None, **options):
    """parse(source, parser=None, base_url=None)

    Return an ElementTree object loaded with source elements.  If no parser
    is provided as second argument, the default parser is used.

    The ``source`` can be any of the following:

    - a file name/path
    - a file object
    - a file-like object
    - a URL using the HTTP or FTP protocol

    To parse from a string, use the ``fromstring()`` function instead.

    Note that it is generally faster to parse from a file path or URL
    than from an open file object or file-like object.  Transparent
    decompression from gzip compressed sources is supported (unless
    explicitly disabled in libxml2).

    The ``base_url`` keyword allows setting a URL for the document
    when parsing from a file-like object.  This is needed when looking
    up external entities (DTD, XInclude, ...) with relative paths.
    """
    preservews = options.pop("preservews", True)
    if not parser:
        parser = XMLParser(TreeBuilder(preservews=preservews, **options))
    tree = XMLTree()
    tree.parse(source, parser, base_url=base_url)
    return tree


def XML(text, parser=None, base_url=None):
    """XML(text, parser=None, base_url=None)

    Parses an XML document or fragment from a string constant.
    Returns the root node (or the result returned by a parser target).
    This function can be used to embed "XML literals" in Python code,
    like in

       >>> root = XML("<root><test/></root>")
       >>> print(root.tag)
       root

    To override the parser with a different ``XMLParser`` you can pass it to
    the ``parser`` keyword argument.

    The ``base_url`` keyword argument allows to set the original base URL of
    the document to support relative Paths when looking up external entities
    (DTD, XInclude, ...).
    """
    if not parser:
        parser = XMLParser(TreeBuilder(preservews=True))
    tree = XMLTree()
    src = io.StringIO(text) if types.is_string(text) else io.BytesIO(text)
    tree.parse(src, parser, base_url=base_url)
    return tree.getroot()


def fromstring(text, parser=None, base_url=None):
    """fromstring(text, parser=None, base_url=None)

    Parses an XML document or fragment from a string.  Returns the
    root node (or the result returned by a parser target).

    To override the default parser with a different parser you can pass it to
    the ``parser`` keyword argument.

    The ``base_url`` keyword argument allows to set the original base URL of
    the document to support relative Paths when looking up external entities
    (DTD, XInclude, ...).
    """
    return XML(text, parser, base_url)

#
# def Element(name, namespace=None):
#     self = larky.mutablestruct(__name__='Element', __class__=Element)
#     def __init__(name, namespace):
#         self._name = name
#         self._namespace = namespace
#         self._element = ElementTree.Element(self._getETreeTag(name, namespace))
#         if namespace == None:
#             self.nameTuple = namespaces["html"], self._name
#         else:
#             self.nameTuple = self._namespace, self._name
#         self.parent = None
#         self._childNodes = []
#         self._flags = []
#         return self
#     self = __init__(name, namespace)
#
#     def _getETreeTag(name, namespace):
#         if namespace == None:
#             etree_tag = name
#         else:
#             etree_tag = "{%s}%s" % (namespace, name)
#         return etree_tag
#     self._getETreeTag = _getETreeTag
#
#     def _setName(name):
#         self._name = name
#         self._element.tag = self._getETreeTag(self._name, self._namespace)
#     self._setName = _setName
#
#     def _getName():
#         return self._name
#     self._getName = _getName
#
#     self.name = larky.property(_getName, _setName)
#
#     def _setNamespace(namespace):
#         self._namespace = namespace
#         self._element.tag = self._getETreeTag(self._name, self._namespace)
#     self._setNamespace = _setNamespace
#
#     def _getNamespace():
#         return self._namespace
#     self._getNamespace = _getNamespace
#
#     self.namespace = larky.property(_getNamespace, _setNamespace)
#
#     def _getAttributes():
#         return self._element.attrib
#     self._getAttributes = _getAttributes
#
#     def _setAttributes(attributes):
#         el_attrib = self._element.attrib
#         el_attrib.clear()
#         if attributes:
#             # calling .items _always_ allocates, and the above truthy check is cheaper than the
#             # allocation on average
#             for key, value in attributes.items():
#                 if builtins.isinstance(key, tuple):
#                     name = "{%s}%s" % (key[2], key[1])
#                 else:
#                     name = key
#                 el_attrib[name] = value
#     self._setAttributes = _setAttributes
#
#     self.attributes = larky.property(_getAttributes, _setAttributes)
#
#     def _getChildNodes():
#         return self._childNodes
#     self._getChildNodes = _getChildNodes
#
#     def _setChildNodes(value):
#         self._element.clear()
#         self._childNodes = []
#         for element in value:
#             self.insertChild(element)
#     self._setChildNodes = _setChildNodes
#
#     self.childNodes = larky.property(_getChildNodes, _setChildNodes)
#
#     def hasContent():
#         """Return true if the node has children or text"""
#         return bool(self._element.text or len(self._element))
#     self.hasContent = hasContent
#
#     def appendChild(node):
#         self._childNodes.append(node)
#         self._element.append(node._element)
#         node.parent = self
#     self.appendChild = appendChild
#
#     def insertBefore(node, refNode):
#         index = list(self._element).index(refNode._element)
#         self._element.insert(index, node._element)
#         node.parent = self
#     self.insertBefore = insertBefore
#
#     def removeChild(node):
#         self._childNodes.remove(node)
#         self._element.remove(node._element)
#         node.parent = None
#     self.removeChild = removeChild
#
#     def insertText(data, insertBefore=None):
#         if not (len(self._element)):
#             if not self._element.text:
#                 self._element.text = ""
#             self._element.text += data
#         elif insertBefore == None:
#             # Insert the text as the tail of the last child element
#             if not self._element[-1].tail:
#                 self._element[-1].tail = ""
#             self._element[-1].tail += data
#         else:
#             # Insert the text before the specified node
#             children = list(self._element)
#             index = children.index(insertBefore._element)
#             if index > 0:
#                 if not self._element[index - 1].tail:
#                     self._element[index - 1].tail = ""
#                 self._element[index - 1].tail += data
#             else:
#                 if not self._element.text:
#                     self._element.text = ""
#                 self._element.text += data
#     self.insertText = insertText
#
#     def cloneNode():
#         element = self.__class__(self.name, self.namespace)
#         if self._element.attrib:
#             element._element.attrib = copy(self._element.attrib)
#         return element
#     self.cloneNode = cloneNode
#
#     def reparentChildren(newParent):
#         if newParent.childNodes:
#             newParent.childNodes[-1]._element.tail += self._element.text
#         else:
#             if not newParent._element.text:
#                 newParent._element.text = ""
#             if self._element.text != None:
#                 newParent._element.text += self._element.text
#         self._element.text = ""
#         base.Node.reparentChildren(self, newParent)  # <- TODO..
#     self.reparentChildren = reparentChildren
#     return self
#
#
# def Comment(data):
#     self = larky.mutablestruct(__name__='Comment', __class__=Comment)
#     def __init__(data):
#         # Use the superclass constructor to set all properties on the
#         # wrapper element
#         self._element = ElementTree.Comment(data)
#         self.parent = None
#         self._childNodes = []
#         self._flags = []
#         return self
#     self = __init__(data)
#
#     def _getData():
#         return self._element.text
#     self._getData = _getData
#
#     def _setData(value):
#         self._element.text = value
#     self._setData = _setData
#
#     self.data = property(_getData, _setData)
#     return self
#
#
# def DocumentType(name, publicId, systemId):
#     self = larky.mutablestruct(__name__='DocumentType', __class__=DocumentType)
#     def __init__(name, publicId, systemId):
#         Element.__init__(self, "<!DOCTYPE>")
#         self._element.text = name
#         self.publicId = publicId
#         self.systemId = systemId
#         return self
#     self = __init__(name, publicId, systemId)
#
#     def _getPublicId():
#         return self._element.get("publicId", "")
#     self._getPublicId = _getPublicId
#
#     def _setPublicId(value):
#         if value != None:
#             self._element.set("publicId", value)
#     self._setPublicId = _setPublicId
#
#     self.publicId = larky.property(_getPublicId, _setPublicId)
#
#     def _getSystemId():
#         return self._element.get("systemId", "")
#     self._getSystemId = _getSystemId
#
#     def _setSystemId(value):
#         if value != None:
#             self._element.set("systemId", value)
#     self._setSystemId = _setSystemId
#
#     self.systemId = larky.property(_getSystemId, _setSystemId)
#     return self
#
#
# def Document():
#     self = larky.mutablestruct(__name__='Document', __class__=Document)
#     def __init__():
#         Element.__init__(self, "DOCUMENT_ROOT")
#         return self
#     self = __init__()
#     return self
#
#
# def DocumentFragment():
#     self = larky.mutablestruct(__name__='DocumentFragment', __class__=DocumentFragment)
#     def __init__():
#         Element.__init__(self, "DOCUMENT_FRAGMENT")
#         return self
#     self = __init__()
#     return self


__fakeq = larky.struct(append=lambda *args: None)

##
# ElementTree builder for XML source data.
#
# @see elementtree.ElementTree

# builder is equivalent to "target"

def Text(value):
    """Text element factory.

    This function creates a special element which the standard serializer
    serializes as XML text comment.

    *value* is a string containing the text, whitespace is ok.

    """
    self = XMLNode(Text)
    self.data = value
    return self


def Comment(text=None):
    """Comment element factory.

    This function creates a special element which the standard serializer
    serializes as an XML comment.

    *text* is a string containing the comment string.

    """
    # def XMLNode(tag, attrib, nsmap=None, **extra):
    element = XMLNode(element_tree.Comment)
    element.data = text
    element.text = larky.property(lambda: element.data)
    # element.tail = larky.property(lambda: None)
    return element


def ProcessingInstruction(name, data=None):
    """Processing Instruction element factory.

    This function creates a special element which the standard serializer
    serializes as an XML comment.

    *target* is a string containing the processing instruction, *text* is a
    string containing the processing instruction contents, if any.

    """
    element = XMLNode(element_tree.PI)
    element._name = name
    element.data = data
    element.text = larky.property(lambda: "%s%s" %
                                          (name, " " + data if data else ''))
    return element


def CDATA(data):
    """CDATA(data)

    CDATA factory.  This factory creates an opaque data object that
    can be used to set Element text.  The usual way to use it is::
        >>> from xml.etree import ElementTree as etree
        >>> el = etree.Element('content')
        >>> el.text = etree.CDATA('a string')
    """
    element = XMLNode(element_tree.CDATA)
    element.data = data
    # TODO(mahmoudimus): fix encoding
    # if types.is_string(data):
    #     element.text = codecs.encode(data, encoding='utf-8')
    # else:
    #     element.text = data
    element.text = larky.property(lambda: element.data)
    # element.tail = larky.property(lambda: None)
    return element


def Attr(ownerDocument, name, namespaceURI, prefix, localName):
    self = XMLNode(Attr, attrib={
        "nsmap": {
            namespaceURI: prefix,
        },
        localName: localName,
        name: name,
    })
    self._name = name
    self._owner_doc = ownerDocument

    def _get_value():
        return ''.join(self.gettextaslist())
    self._get_value = _get_value

    def _set_value(value):
        pass
        # old_value = self.value
        # if value != old_value or len(self.childNodes) > 1:
        #     # Remove previous childNodes
        #     while self.firstChild:
        #         self.removeChild(self.firstChild)
        #     if value:
        #         self.appendChild(self.ownerDocument.createTextNode(value))
        #     owner = self._ownerElement
        #     if owner:
        #         owner._4dom_fireMutationEvent('DOMAttrModified',
        #                                       relatedNode=self,
        #                                       prevValue=old_value,
        #                                       newValue=value,
        #                                       attrName=self.name,
        #                                       attrChange=MutationEvent.MODIFICATION)
        #         owner._4dom_fireMutationEvent('DOMSubtreeModified')
    self._set_value = _set_value

    def __repr__():
        return '<Attribute Node: Name="%s", Value="%s">' % (
            self._name,
            self._get_value()
            )
    self.__repr__ = __repr__


def DocumentType(name, public_id, system_id, data):
    """Doctype node

    """
    self = XMLNode(DocumentType)
    self._name = name
    self.public_id = public_id
    self.system_id = system_id
    self.data = data
    self.internalDTD = larky.property(lambda: data)
    self.text = larky.property(lambda: self.data if self.data else '')
    # self.tail = larky.property(lambda: None)

    # self._name = "<!DOCTYPE>"
    return self


def Document(encoding=None, standalone=None, version=None, doctype=None):

    self = XMLNode(Document)
    # self.value = "DOCUMENT_ROOT"
    self._name = "DOCUMENT_ROOT"
    # nodeName = "#document"
    self.encoding = encoding
    self.standalone = standalone
    self.version = version
    self.set_doctype(doctype)
    self.intSubset = larky.property(
        lambda: self.docinfo.internalDTD if self.docinfo else None
    )
    self._root = None

    def getroot():
        return self._root
    self.getroot = getroot

    def setroot(root):
        self._root = root
        self._root.attach_document(self)
    self.setroot = setroot

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


ParserEvents = enum.Enum('ParserEvents', dict(
    START_DOCUMENT='START_DOCUMENT',
    END_DOCUMENT='END_DOCUMENT',
    ERROR='ERROR',

    # <?xml ......>  => Declaration
    DECLARATION='DECLARATION',

    # <!DOCTYPE ..>  => DTD
    DTD='DTD',
    NOTATION_DECLARATION='NOTATION_DECLARATION',
    ENTITY_DECLARATION='ENTITY_DECLARATION',
    ENTITY_REFERENCE='ENTITY_REFERENCE',

    # <?target ...>  => Processing instruction
    PROCESSING_INSTRUCTION='PROCESSING_INSTRUCTION',
    # <!-- .......>  => Comment
    COMMENT = 'COMMENT',

    # <name ......>  => Element
    # <name>         => Element without attributes
    # <name/>        => Empty Element
    START_ELEMENT='START_ELEMENT',
    END_ELEMENT='END_ELEMENT',
    ATTRIBUTE='ATTRIBUTE',
    NAMESPACE='NAMESPACE',
    END_NAMESPACE='END_NAMESPACE',

    # Text...can come before or after Element
    CHARACTERS = 'CHARACTERS',
    # <![CDATA[ ..>  => CDATA
    CDATA='CDATA',
    # ignorable whitespace is any whitespace encountered except if:
    #   in attribute values
    #   CDATA sections
    #   comments
    SPACE = 'SPACE',
).items())


def XMLParser(target):
    self = xmllib.XMLParser()
    self.__name__ = 'XMLParser'
    self.__class__ = XMLParser

    # xmllib -- overridden methods
    xmllib_XMLParser_reset = self.reset

    def reset():
        self.elements.clear()
        xmllib_XMLParser_reset()
        self.unfed_so_far = True

    self.reset = reset

    xmllib_XMLParser_feed = self.feed

    def feed(data):
        if self.unfed_so_far:
            self.target.publish(ParserEvents.START_DOCUMENT)
            self.unfed_so_far = False

        xmllib_XMLParser_feed(data)

    self.feed = feed

    xmllib_XMLParser_close = self.close

    def close():
        xmllib_XMLParser_close()
        self.target.publish(ParserEvents.END_DOCUMENT)
        return self.target.getDocument()

    self.close = close

    # start driver
    def __init__(target):
        self.target = target
        self.reset()
        return self

    self = __init__(target)

    # errors

    def syntax_error(message):
        "Handles fatal errors."
        self.target.publish(ParserEvents.ERROR, message=message)

    self.syntax_error = syntax_error

    # handlers
    def handle_xml(encoding, standalone):
        """Remembers whether the document is standalone."""
        # version is defaulted to 1.0
        _standalone = None
        if standalone and standalone in ("no", "yes",):
            # trinary state (none (omit), yes, no)
            _standalone = (standalone == "yes")
        self.target.publish(ParserEvents.DECLARATION,
                            version="1.0",
                            encoding=encoding,
                            standalone=_standalone)

    self.handle_xml = handle_xml

    def handle_doctype(tag, pubid, syslit, data):
        self.target.publish(ParserEvents.DTD,
                            name=tag,
                            public_id=pubid,
                            system=syslit,
                            data=data)

    self.handle_doctype = handle_doctype

    def unknown_entityref(name):
        self.syntax_error("reference to unknown entity `&%s;'" % name)
    self.unknown_entityref = unknown_entityref

    def handle_proc(name, data):
        self.target.publish(ParserEvents.PROCESSING_INSTRUCTION, name=name,
                            data=data)

    self.handle_proc = handle_proc

    def handle_comment(data):
        self.target.publish(ParserEvents.COMMENT, comment=data)

    self.handle_comment = handle_comment

    # This method is only invoked if we define special element
    # tag handlers via the xmllib.XMLParser.elements attribute. By
    # default, we define none. So, this is a no-operation and dispatches
    # to unknown_starttag.
    xmllib_XMLParser_handle_starttag = self.handle_starttag
    self.handle_starttag = xmllib_XMLParser_handle_starttag

    # This method is only invoked if we define special element
    # tag handlers via the xmllib.XMLParser.elements attribute. By
    # default, we define none. So, this is a no-operation and dispatches
    # to unknown_endtag.
    xmllib_XMLParser_handle_endtag = self.handle_endtag
    self.handle_endtag = xmllib_XMLParser_handle_endtag

    def unknown_starttag(tag, attrs):
        self.target.publish(ParserEvents.START_ELEMENT, tag=tag, attrs=attrs)
        for k, v in attrs.items():
            self.target.publish(ParserEvents.ATTRIBUTE, tag=tag, name=k, value=v)
    self.unknown_starttag = unknown_starttag

    def unknown_endtag(tag):
        "Handles end tags."
        self.target.publish(ParserEvents.END_ELEMENT, tag=tag)
    self.unknown_endtag = unknown_endtag

    def handle_startns(prefix, qualified, href):
        self.target.publish(ParserEvents.NAMESPACE, prefix=prefix,
                            qualified=qualified, href=href)
    self.handle_startns = handle_startns

    def handle_endns(prefix):
        self.target.publish(ParserEvents.END_NAMESPACE, prefix=prefix)
    self.handle_endns = handle_endns

    def handle_data(data):
        "Handles PCDATA."
        if data.strip():
            self.target.publish(ParserEvents.CHARACTERS, data=data)
        else:
            # ignorable whitespace is any whitespace encountered except if:
            #   in attribute values
            #   CDATA sections
            #   comments
            self.target.publish(ParserEvents.SPACE, data=data)
    self.handle_data = handle_data

    def handle_cdata(data):
        "Handles CDATA marked sections."
        self.target.publish(ParserEvents.CDATA, data=data)
    self.handle_cdata = handle_cdata

    xmllib_XMLParser_handle_charref = self.handle_charref
    # Example -- handle character reference, no need to override
    def handle_charref(name):
        if name[0] == 'x':
            rval = Ok(name[1:]).map(lambda x: int(x, 16))
        else:
            rval = Ok(name).map(int)

        if rval.is_err:
            self.unknown_charref(name)
            return

        n = rval.unwrap()
        if not ((0 <= n) and (n <= 255)):
            self.unknown_charref(name)
            return
        self.handle_data(chr(n))
    self.handle_charref = handle_charref

    xmllib_XMLParser_unknown_charref = self.unknown_charref
    self.unknown_entityref = xmllib_XMLParser_unknown_charref
    return self


def BaseTreeBuilder(namespaceHTMLElements):
    """Base treebuilder implementation

    * documentClass - the class to use for the bottommost node of a document
    * elementClass - the class to use for HTML Elements
    * commentClass - the class to use for comments
    * doctypeClass - the class to use for doctypes

    """
    self = larky.mutablestruct(__name__='BaseTreeBuilder', __class__=BaseTreeBuilder)

    # Document class
    self.documentClass = None

    # The class to use for creating a node
    self.elementClass = None

    # The class to use for creating comments
    self.commentClass = None

    # The class to use for creating doctypes
    self.doctypeClass = None

    # Fragment class
    self.fragmentClass = None

    def reset():
       self.openElements = []
       # XXX - rename these to headElement, formElement
       self.headPointer = None
       self.formPointer = None
       self.seenRoot = False
       self.document = None
    self.reset = reset

    def __init__(namespaceHTMLElements):
        """Create a TreeBuilder

        :arg namespaceHTMLElements: whether or not to namespace HTML elements

        """
        if namespaceHTMLElements:
            self.defaultNamespace = "http://www.w3.org/1999/xhtml"
        else:
            self.defaultNamespace = None
        return self
    self = __init__(namespaceHTMLElements)
    #
    # def elementInScope(target, variant=None):
    #     # If we pass a node in we match that. if we pass a string
    #     # match any node with that name
    #     exactNode = hasattr(target, "nameTuple")
    #     if not exactNode:
    #         if builtins.isinstance(target, text_type):
    #             target = (namespaces["html"], target)
    #         if not (builtins.isinstance(target, tuple)):
    #             fail("assert builtins.isinstance(target, tuple) failed!")
    #
    #     listElements, invert = listElementsMap[variant]
    #
    #     for node in reversed(self.openElements):
    #         if exactNode and node == target:
    #             return True
    #         elif not exactNode and node.nameTuple == target:
    #             return True
    #         elif invert ^ (node.nameTuple in listElements):
    #             return False
    #     if not (False):
    #         fail("assert False failed!")
    # self.elementInScope = elementInScope
    #
    # def reconstructActiveFormattingElements():
    #     # Within this algorithm the order of steps described in the
    #     # specification is not quite the same as the order of steps in the
    #     # code. It should still do the same though.
    #
    #     # Step 1: stop the algorithm when there's nothing to do.
    #     if not self.activeFormattingElements:
    #         return
    #
    #     # Step 2 and step 3: we start with the last element. So i is -1.
    #     i = len(self.activeFormattingElements) - 1
    #     entry = self.activeFormattingElements[i]
    #     if entry == Marker or entry in self.openElements:
    #         return
    #     for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
    #         if not entry != Marker and entry not in self.openElements:
    #             break
    #         if i == 0:
    #             # This will be reset to 0 below
    #             i = -1
    #             break
    #         i -= 1
    #         # Step 5: let entry be one earlier in the list.
    #         entry = self.activeFormattingElements[i]
    #     for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
    #         if not True:
    #             break
    #         # Step 7
    #         i += 1
    #
    #         # Step 8
    #         entry = self.activeFormattingElements[i]
    #         clone = entry.cloneNode()  # Mainly to get a new copy of the attributes
    #
    #         # Step 9
    #         element = self.insertElement(
    #             {
    #                 "type": "StartTag",
    #                 "name": clone.name,
    #                 "namespace": clone.namespace,
    #                 "data": clone.attributes,
    #             }
    #         )
    #
    #         # Step 10
    #         self.activeFormattingElements[i] = element
    #
    #         # Step 11
    #         if element == self.activeFormattingElements[-1]:
    #             break
    # self.reconstructActiveFormattingElements = reconstructActiveFormattingElements
    #
    # def clearActiveFormattingElements():
    #     entry = self.activeFormattingElements.pop()
    #     for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
    #         if not self.activeFormattingElements and entry != Marker:
    #             break
    #         entry = self.activeFormattingElements.pop()
    # self.clearActiveFormattingElements = clearActiveFormattingElements
    #
    # def elementInActiveFormattingElements(name):
    #     """Check if an element exists between the end of the active
    #     formatting elements and the last marker. If it does, return it, else
    #     return false"""
    #
    #     for item in self.activeFormattingElements[::-1]:
    #         # Check for Marker first because if it's a Marker it doesn't have a
    #         # name attribute.
    #         if item == Marker:
    #             break
    #         elif item.name == name:
    #             return item
    #     return False
    # self.elementInActiveFormattingElements = elementInActiveFormattingElements

    def insertRoot(**token):
        element = self.createElement(**token)
        self.openElements.append(element)
        self.document.appendChild(element)
        element.attach_document(self.document)
    self.insertRoot = insertRoot

    def insertDoctype(token):
        name = token["name"]
        publicId = token["public_id"]
        systemId = token["system"]
        data = token.get('data')
        doctype = self.doctypeClass(name, publicId, systemId, data)
        doctype.attach_document(self.document)
        self.document.set_doctype(doctype)
    self.insertDoctype = insertDoctype

    def insertComment(token, parent=None):
        if parent == None:
            parent = self.openElements[-1]
        comment = self.commentClass(token["comment"])
        parent.appendChild(comment)
        comment.attach_document(self.document)
    self.insertComment = insertComment

    def insertProcessingInstruction(token, parent=None):
        if parent == None:
            parent = self.openElements[-1]
        pi_node = self.piClass(token["name"], token["data"])
        parent.appendChild(pi_node)
        pi_node.attach_document(self.document)
    self.insertProcessingInstruction = insertProcessingInstruction

    def createElement(**token):
        """Create an element but don't insert it anywhere"""
        name = token["tag"]
        namespace = token.get("namespace", self.defaultNamespace)
        element = self.elementClass(
            _namespaced_name_from_ns_name(namespace, name),
            {fixname(key): value for key, value in token["attrs"].items()}
        )
        element.attach_document(self.document)
        return element
    self.createElement = createElement

    def insertElementNormal(**token):
        name = token["tag"]
        if not types.is_string(name):
            fail("Element %s not string!" % name)
        namespace = token.get("namespace", self.defaultNamespace)
        element = self.elementClass(
            _namespaced_name_from_ns_name(namespace, name),
            {fixname(key): value for key, value in token["attrs"].items()}
        )
        element.attach_document(self.document)
        self.openElements[-1].appendChild(element)
        self.openElements.append(element)
        return element
    self.insertElementNormal = insertElementNormal

    def insertText(data, parent=None):
        """Insert text data."""
        if parent == None:
            parent = self.openElements[-1]
        text = Text(data)
        text.attach_document(self.document)
        parent.insertText(text)
    self.insertText = insertText

    def generateImpliedEndTags(exclude=None):
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not self.openElements:
                break
            name = self.openElements[-1].name
            if name != exclude:
                self.openElements.pop()
    self.generateImpliedEndTags = generateImpliedEndTags

    def getDocument():
        """Return the final tree"""
        return self.document
    self.getDocument = getDocument

    def getFragment():
        """Return the final fragment"""
        # assert self.innerHTML
        fragment = self.fragmentClass()
        self.openElements[0].reparentChildren(fragment)
        return fragment
    self.getFragment = getFragment

    return self


def split_qname(qname):
    """
    Input a QName according to XML Namespaces 1.0
    http://www.w3.org/TR/REC-xml-names
    Return the name parts according to the spec
    In the case of namespace declarations the tuple returned
    is (prefix, 'xmlns')
    Note that this won't hurt users since prefixes and local parts starting
    with "xml" are reserved, but it makes ns-aware builders easier to write
    """
    fields = qname.split(':')
    if len(fields) == 1:
        return None, 'xmlns' if qname == 'xmlns' else qname
    elif len(fields) == 2:
        if fields[0] == 'xmlns':
            return fields[1], 'xmlns'
        else:
            return fields[0], fields[1]
    return None, None


XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"
def TreeBuilder(namespaceHTMLElements=False, **options):
    # self = larky.mutablestruct(__name__='TreeBuilder', __class__=TreeBuilder)
    self = BaseTreeBuilder(namespaceHTMLElements)
    self.documentClass = options.pop('document_factory', Document)
    self.doctypeClass = options.pop('doctype_factory', DocumentType)
    self.elementClass = options.pop('element_factory', XMLNode)
    self.commentClass = options.pop('comment_factory', Comment)
    self.piClass = options.pop('pi_factory', ProcessingInstruction)
    self.debug = options.pop('debug', False)
    self.preservews = options.pop('preservews', False)
    self._ev_q = [] if options.pop('capture_event_queue', False) else __fakeq

    def __init__(**options):
        self.__options = options

        self.firstEvent = [None, None]
        self.lastEvent = self.firstEvent
        self.elementStack = []
        self.push = self.elementStack.append
        self.pop = self.elementStack.pop
        self._xmlns_attrs = []
        self._ns_contexts = [{XML_NAMESPACE:'xml'}] # contains uri -> prefix dicts
        self._current_context = self._ns_contexts[-1]
        self.pending_events = []
        self.reset()
        return self
    self = __init__(**options)  # builder, element_factory, parser,

    def fromstring(xmlstr):
        parser = XMLParser(self)
        return parse(io.StringIO(xmlstr), parser=parser)
    self.fromstring = fromstring

    def _buildTag(ns_name_tuple):
        ns_uri, local_name = ns_name_tuple
        if ns_uri:
            el_tag = "{%s}%s" % ns_name_tuple
        elif self._default_ns:
            el_tag = "{%s}%s" % (self._default_ns, local_name)
        else:
            el_tag = local_name
        return el_tag
    self._buildTag = _buildTag

    def publish(event, **payload):
        if self.debug:
            print("** SEEN EVENT: ", event, payload)

        if event == ParserEvents.START_DOCUMENT:
            if self.document:
                fail("Unexpected event - document already set")
            self.document = self.documentClass()
            self.lastEvent[1] = [(ParserEvents.START_DOCUMENT, self.document), None]
            self.lastEvent = self.lastEvent[1]
            self.push(self.document)
        elif event == ParserEvents.END_DOCUMENT:
            self.lastEvent[1] = [(ParserEvents.END_DOCUMENT, self.document), None]
            self.pop()
        elif event == ParserEvents.ERROR:
            fail("unexpected error encountered: %s" % payload["message"])
        elif event == ParserEvents.DECLARATION:
            # <?xml ......>  => Declaration
            self.document.encoding = payload["encoding"]
            self.document.standalone = payload["standalone"]
            self.document.version = payload["version"]
        elif event == ParserEvents.DTD:
            # <!DOCTYPE ..>  => DTD
            self.insertDoctype(payload)
            node = self.document.last_child()
            self.lastEvent[1] = [(ParserEvents.DTD, node), None]
            self.lastEvent = self.lastEvent[1]
            self._ev_q.append(("doctype", node))
        elif event == ParserEvents.PROCESSING_INSTRUCTION:
            # <?target ...>  => Processing instruction
            parent = self.elementStack[-1]
            self.insertProcessingInstruction(payload, parent)
            node = parent.last_child()
            self.lastEvent[1] = [(ParserEvents.PROCESSING_INSTRUCTION, node), None]
            self.lastEvent = self.lastEvent[1]
        elif event == ParserEvents.COMMENT:
            # <!-- .......>  => Comment
            parent = self.elementStack[-1]
            self.insertComment(payload, parent)
            node = parent.last_child()
            self.lastEvent[1] = [(ParserEvents.COMMENT, node), None]
            self.lastEvent = self.lastEvent[1]
        elif event == ParserEvents.START_ELEMENT:
            # <name ......>  => Element
            # <name>         => Element without attributes
            # <name/>        => Empty Element
            xmlns_uri = 'http://www.w3.org/2000/xmlns/'
            ns_attrs = {}
            if self._xmlns_attrs:
                for aname, value in self._xmlns_attrs:
                    ns_attrs[(xmlns_uri, aname or "")] = value
                self._xmlns_attrs = []
            localname = payload['tag']
            nsmap = payload['attrs'].pop('nsmap', {})
            if nsmap:
                # uri = node_attrib['nsmap'].keys()[0] or ""
                uri, localname = _get_ns_tag(fixname(localname))
                # When using namespaces, the reader may or may not
                # provide us with the original name. If not, create
                # *a* valid tagName from the current context.
                prefix = self._current_context.get(uri)
                if self.debug:
                    print("uri:", uri, "ctx[uri]..(prefix): ", prefix)
                if prefix:
                    tagName = prefix
                    if localname:
                        tagName += (":" + localname)
                else:
                    tagName = localname
                localname = tagName

            if not self.seenRoot:
                self.insertRoot(tag=localname, attrs={'nsmap': nsmap})
                self.seenRoot = True
                node = self.document.last_child()
                self.document.setroot(node)
            else:
                # print("openElements[-1]", repr(self.openElements[-1]))
                node = self.insertElementNormal(tag=localname, attrs={'nsmap': nsmap})

            node_attrib = dict(**payload['attrs'])
            if self.debug:
                print("tagname", localname, "node_attrib: ", node_attrib)
            # to preserve expected insertion order
            # _attribs = node.attrib
            # node.attrib = {}
            ns_attrs.update(node_attrib)
            for aname,value in ns_attrs.items():
                if types.is_tuple(aname):
                    a_uri, a_localname = aname
                else:
                    a_uri, a_localname = _get_ns_tag(fixname(aname))

                if a_uri == xmlns_uri:
                    if a_localname == 'xmlns':
                        qname = a_localname
                    else:
                        qname = 'xmlns'
                        if a_localname:
                            qname += (":" + a_localname)
                    node._nsmap[a_uri] = qname
                    key = qname
                    # attr = self.document.createAttributeNS(a_uri, qname)
                    # node.setAttributeNodeNS(attr)
                elif a_uri:
                    prefix = self._current_context[a_uri]
                    if prefix:
                        qname = prefix
                        if a_localname:
                            qname += (":" + a_localname)
                    else:
                        qname = a_localname
                    node._nsmap[a_uri] = qname
                    key = qname
                    # attr = self.document.createAttributeNS(a_uri, qname)
                    # node.setAttributeNodeNS(attr)
                else:
                    node._nsmap[None] = a_localname
                    # attr = self.document.createAttribute(a_localname)
                    # node.setAttributeNode(attr)
                    # a_uri, a_localname = aname
                    key = _namespaced_name_from_ns_name(a_uri, a_localname)
                if self.debug:
                    print("params = ", a_uri, a_localname, "value=", value, "key=",key)
                node.attrib[key] = value
                # node.attrib[] = value
            # node.attrib.update(_attribs)
            self.lastEvent[1] = [(ParserEvents.START_ELEMENT, node), None]
            self.lastEvent = self.lastEvent[1]
            self.push(node)
            self._ev_q.append(("start", node))
        elif event == ParserEvents.ATTRIBUTE:
            pass
            # top = self.elementStack[-1]
            # xmlns_uri = 'http://www.w3.org/2000/xmlns/'
            # if self._xmlns_attrs != None:
            #     for aname, value in self._xmlns_attrs:
            #         key = _namespaced_name_from_ns_name(xmlns_uri, aname)
            #         top.attrib[key] = value
            #     self._xmlns_attrs = []
            #
            # if top.tag != fixname(payload['tag']):
            #     fail(("received attribute event for (%s) and its not " +
            #           "related to current node: %s") %
            #          (fixname(payload['tag']), repr(top)))
            # key = payload['name']
            # value = payload['value']
            # if key == 'nsmap':
            #     top._nsmap.update(value)
            # else:
            #     top.attrib[key] = value
            #
            ## {"tag": "e9", "name": "nsmap", "value": {None: "", "http://www.ietf.org": "a"}}
            #
            # attrs = payload['attrs']
            # if self._xmlns_attrs is not None:
            #     for aname, value in _xmlns_attrs:
            #         attrs._attrs[(xmlns_uri, aname)] = value
            #     self._xmlns_attrs = []
        elif event == ParserEvents.NAMESPACE:
            self._xmlns_attrs.append((payload.get('prefix', 'xmlns'), payload['href']))
            self._ns_contexts.append(dict(**self._current_context))
            self._current_context[payload['href']] = payload.get('prefix') or None
            self._ev_q.append(
                ("start-ns", (payload.get('prefix') or "", payload['href']))
            )
        elif event == ParserEvents.END_NAMESPACE:
            self._ev_q.append(("end-ns", None))
            self._current_context = self._ns_contexts.pop()
        elif event == ParserEvents.END_ELEMENT:
            self._ev_q.append(("end", self.openElements.pop()))
            self.lastEvent[1] = [(ParserEvents.END_ELEMENT, self.pop()), None]
            self.lastEvent = self.lastEvent[1]
        elif event == ParserEvents.CHARACTERS:
            # Text...can come before or after Element
            node = self.elementStack[-1]
            self.insertText(payload["data"], node)
            self.lastEvent[1] = [(ParserEvents.CHARACTERS, node), None]
            self.lastEvent = self.lastEvent[1]
        elif event == ParserEvents.CDATA:
            # <![CDATA[ ..>  => CDATA
            node = self.elementStack[-1]
            self.insertText(payload["data"], node)
            self.lastEvent[1] = [(ParserEvents.CDATA, node), None]
            self.lastEvent = self.lastEvent[1]
        elif event == ParserEvents.SPACE:
            # ignorable whitespace is any whitespace encountered except if:
            #   in attribute values
            #   CDATA sections
            #   comments
            if self.preservews and self.seenRoot:
                if self.debug:
                    print("insertWS to this parent:", repr(self.elementStack[-1]))
                self.insertText(payload["data"], self.elementStack[-1])
            else:
                if self.debug:
                    print("skipping ws, last event",
                          self.lastEvent[0],
                          "stack top",
                          repr(self.elementStack[-1]))
            # skip any whitespace that is associated to the document
            ## if self.lastEvent[0][1] == self.document:
            # if self.elementStack[-1] == self.document:
            #     return
            # self.insertText(payload["data"], self.lastEvent[0][1])
        # ignore the below events
        elif event == ParserEvents.NOTATION_DECLARATION:
            pass
        elif event == ParserEvents.ENTITY_DECLARATION:
            pass
        elif event == ParserEvents.ENTITY_REFERENCE:
            pass

    self.publish = publish

    def _read(i):
        if i >= len(self._ev_q):
            self._ev_q.clear()
            return StopIteration()
        return Ok(self._ev_q[i])

    def read_events():
        if self._ev_q == __fakeq:
            return []
        return larky.DeterministicGenerator(_read)

    self.read_events = read_events
    return self


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


def iterparse(source, events=("end",), tag=None,
            attribute_defaults=False, dtd_validation=False,
            load_dtd=False, no_network=True, remove_blank_text=False,
            compact=True, resolve_entities=True, remove_comments=False,
            remove_pis=False, strip_cdata=True, encoding=None,
            html=False, recover=None, huge_tree=False, collect_ids=True,
            schema=None):
    """
    iterparse(self, source, events=("end",), tag=None, \
                      attribute_defaults=False, dtd_validation=False, \
                      load_dtd=False, no_network=True, remove_blank_text=False, \
                      remove_comments=False, remove_pis=False, encoding=None, \
                      html=False, recover=None, huge_tree=False, schema=None)

    Incremental parser.

    Parses XML into a tree and generates tuples (event, element) in a
    SAX-like fashion. ``event`` is any of 'start', 'end', 'start-ns',
    'end-ns'.

    For 'start' and 'end', ``element`` is the Element that the parser just
    found opening or closing.  For 'start-ns', it is a tuple (prefix, URI) of
    a new namespace declaration.  For 'end-ns', it is simply None.  Note that
    all start and end events are guaranteed to be properly nested.

    The keyword argument ``events`` specifies a sequence of event type names
    that should be generated.  By default, only 'end' events will be
    generated.

    The additional ``tag`` argument restricts the 'start' and 'end' events to
    those elements that match the given tag.  The ``tag`` argument can also be
    a sequence of tags to allow matching more than one tag.  By default,
    events are generated for all elements.  Note that the 'start-ns' and
    'end-ns' events are not impacted by this restriction.

    The other keyword arguments in the constructor are mainly based on the
    libxml2 parser configuration.  A DTD will also be loaded if validation or
    attribute default values are requested.

    Available boolean keyword arguments:
     - attribute_defaults: read default attributes from DTD
     - dtd_validation: validate (if DTD is available)
     - load_dtd: use DTD for parsing
     - no_network: prevent network access for related files
     - remove_blank_text: discard blank text nodes
     - remove_comments: discard comments
     - remove_pis: discard processing instructions
     - strip_cdata: replace CDATA sections by normal text content (default: True)
     - compact: safe memory for short text content (default: True)
     - resolve_entities: replace entities by their text value (default: True)
     - huge_tree: disable security restrictions and support very deep trees
                  and very long text content (only affects libxml2 2.7+)
     - html: parse input as HTML (default: XML)
     - recover: try hard to parse through broken input (default: True for HTML,
                False otherwise)

    Other keyword arguments:
     - encoding: override the document encoding
     - schema: an XMLSchema to validate against
    """
    if not hasattr(source, "read"):
        fail("TypeError: source (in Larky) must have a read function")
    builder = TreeBuilder(capture_event_queue=bool(events))
    parser = XMLParser(builder)
    parse(source, parser=parser)

    event_queue = list(builder.read_events())
    if events:
        event_queue = [e for e in event_queue if e[0] in events]

    def _iterate(idx):
        if idx >= len(event_queue):
            return StopIteration()
        return Result.Ok(event_queue[idx])

    it = larky.DeterministicGenerator(_iterate)
    # does not work!
    # it.root = tree.getroot()
    return it


etree = larky.struct(
    __name__='etree',

    namespaced_name=_namespaced_name,
    get_ns_tag=_get_ns_tag,
    get_ns=_get_ns,
    namespaced_name_from_ns_name=_namespaced_name_from_ns_name,
    tag_matches=_tag_matches,
    # TODO: rename..
    XMLTree=XMLTree,
    XMLNode=XMLNode,

    tostring=tostring,
    tostringC14N=tostringC14N,
    tofilelikeC14N=tofilelikeC14N,
    iselement=iselement,
    Comment=Comment,
    ProcessingInstruction=ProcessingInstruction,
    DocumentType=DocumentType,
    Document=Document,
    # stuff
    XMLParser=XMLParser,
    TreeBuilder=TreeBuilder,
    parse=parse,
    iterparse=iterparse,
    fromstring=fromstring,
    XML=XML,
    ElementTree=ElementTree,
    QName=QName,
)