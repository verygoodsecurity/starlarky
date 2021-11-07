load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io", StringIO="StringIO")
load("@stdlib//operator", operator="operator")
load("@stdlib//string", string="string")
load("@stdlib//types", types="types")
load("@stdlib//xml/etree/ElementTree", Comment="Comment", ProcessingInstruction="ProcessingInstruction", ElementTree="ElementTree")
load("@vendor//option/result", Error="Error")


def XMLWriter(tree, namespaces=None, encoding="utf-8"):
    """
    A modification of the _write method of ElementTree
    which supports namespaces in a reasonable way
    """

    self = larky.mutablestruct(
        __name__='XMLWriter', __class__=XMLWriter,
        default_namespaces={"http://www.w3.org/XML/1998/namespace": "xml"}
    )

    def __init__(tree, namespaces, encoding):
        self.tree = tree
        self.encoding = encoding
        self.setupDeclaredNamespaces(namespaces)
        return self
    self = __init__(tree, namespaces, encoding)

    def __call__(file=None, tree=None, namespaces=None, encoding=None):
        """
        namespace-aware serialization of a XML elementtree
        """
        if tree != None:
            self.tree = tree
        if namespaces != None:
            self.setupDeclaredNamespaces(namespaces)
        if encoding != None:
            self.encoding = encoding
        if file == None:
            file = StringIO()
        if not hasattr(file, "write"):
            file = open(file, "wb")
        self.file = file
        if not (types.is_instance(self.tree, ElementTree)):
            fail("assert types.is_instance(self.tree, ElementTree) failed!")
        if not (self.tree._root != None):
            fail("assert self.tree._root != None failed!")
        root = self.tree._root
        ns = dict(**self.declared_namespaces)
        # need a copy here, because original must stay intact
        self.writeXMLHeader()
        self.write(root, ns)
        if types.is_instance(self.file, StringIO):
            return self.file.getvalue()
    self.__call__ = __call__

    def escapeText(text):
        text = self.encode(text)
        text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        return text
    self.escapeText = escapeText

    def escapeAttr(text):
        return self.escapeText(text).replace('"', '&quot;')
    self.escapeAttr = escapeAttr

    def encode(text):
        if types.is_instance(text, str):
            return codecs.encode(text, encoding=self.encoding)
        elif types.is_instance(text, str):
            return text
        fail('TypeError: XMLWriter: Cannot encode objects of type %s' % type(text))
    self.encode = encode

    def cdata(text):
        if text.find("]]>") >= 0:
            fail("ValueError: ']]>' not allowed in a CDATA section")
        return "<![CDATA[%s]]>" % text
    self.cdata = cdata

    def writeXMLHeader():
        self.file.write('<?xml version="1.0" encoding="%s"?>\n' % self.encoding)
    self.writeXMLHeader = writeXMLHeader

    def writeAttr(name, value):
        self.file.write(" %s=\"%s\"" % (self.encode(name), self.escapeAttr(value)))
    self.writeAttr = writeAttr

    def writeStartTagOpen(name):
        self.file.write("<%s" % self.encode(name))
    self.writeStartTagOpen = writeStartTagOpen

    def writeStartTagClose():
        self.file.write(">")
    self.writeStartTagClose = writeStartTagClose

    def writeContent(text):
        self.file.write(self.escapeText(text))
    self.writeContent = writeContent

    def writeEmptyTagClose():
        self.file.write(" />")
    self.writeEmptyTagClose = writeEmptyTagClose

    def writeEndTag(name):
        self.file.write("</%s>" % self.encode(name))
    self.writeEndTag = writeEndTag

    def writeComment(node):
        self.file.write("<!-- %s -->" % self.escapeText(node.text))
    self.writeComment = writeComment

    def writePI(node):
        self.file.write("<?%s?>" % self.escapeText(node.text))
    self.writePI = writePI

    def setupDeclaredNamespaces(namespaces):
        """
        set up predeclared namespace declarations
        """
        self.xmlns_namespace = ''
        self.declared_namespaces = {}
        if not namespaces == None:
            self.declared_namespaces = namespaces
        self.namespaces_by_prefix = {}
        self.prefixes = []
        ns_uris = list(self.declared_namespaces.keys())
        for uri in ns_uris:
            prefix = namespaces[uri]
            if not (prefix not in self.prefixes):
                fail("assert prefix not in self.prefixes failed!")
            self.prefixes.append(prefix)
            self.namespaces_by_prefix[prefix] = uri
        self.prefixes.sort()
    self.setupDeclaredNamespaces = setupDeclaredNamespaces

    def getNamespaceByPrefix(prefix):
        return self.namespaces_by_prefix[prefix]
    self.getNamespaceByPrefix = getNamespaceByPrefix

    def getXMLNS(prefix, namespace_uri):
        """
        return a "xmlns"-prefixed namespace declaration
        """
        if prefix == '':
            # unprefixed namespace ("xmlns" attribute)
            return ("xmlns", namespace_uri)
        else:
            # prefixed namespace ("xmlns:xy=" attribute)
            return ("xmlns:%s" % prefix, namespace_uri)
    self.getXMLNS = getXMLNS

    def addPrefix(name, namespaces, attr=True):
        """
        given a decorated name (of the form {uri}tag),
        return prefixed name and namespace declaration
        """
        if not name[:1] == "{":
            # no Namespace
            return name, None
        namespace_uri, name = string.split(name[1:], "}", 1)
        prefix = namespaces.get(namespace_uri, None)
        if prefix == None:
            # test for "xml" namespace
            prefix = self.default_namespaces.get(namespace_uri, None)
        if prefix == None:
            if attr:
                # namespaced attributes always need to be prefixed
                # (even if they are in the default namespace)
                prefix = "ns%d" % len(namespaces)
            else:
                # make this the default namespace (for tags)
                prefix = ''
        if prefix == '':
            # tag names remain unchanged
            if attr:
                # namespaced attributes always need to be prefixed
                # (even if they are in the default namespace)
                prefix = "ns%d" % len(namespaces)
                name = "%s:%s" % (prefix, name)
            else:
                if not namespace_uri == self.xmlns_namespace:
                # we redefine the namespace for the empty prefix
                    if self.xmlns_namespace in namespaces:
                        operator.delitem(namespaces, self.xmlns_namespace)
                    self.xmlns_namespace = namespace_uri
        else:
            # set prefix to name
            name = "%s:%s" % (prefix, name)
        if self.default_namespaces.get(namespace_uri, None) == prefix:
            # XML namespace etc., needs no declaration
            return name, None
        if namespaces.get(namespace_uri, None) == prefix:
            # namespace has already been declared before
            return name, None
        # get the appropriate declarations
        xmlns = self.getXMLNS(prefix, namespace_uri)
        namespaces[namespace_uri] = prefix
        return name, xmlns
    self.addPrefix = addPrefix

    def write(node, namespaces):
        # write XML to file
        tag = node.tag
        print("xmlwriter.write(): ", tag, type(tag))
        if tag == Comment:
            # comments are not parsed by ElementTree!
            self.writeComment(node)
        elif tag == ProcessingInstruction:
            # PI's are not parsed by ElementTree!
            self.writePI(node)
        else:
            xmlns_items = [] # collects new namespaces in this scope
            attributes = list(node.items())
            for attrname, value in attributes:
                # (the elementtree parser discards these attributes)
                if attrname.startswith('xmlns:'):
                    namespaces[value] = attrname[6:]
                if attrname == "xmlns":
                    namespaces[value] = ''
            # get namespace for tag
            tag, xmlns = self.addPrefix(tag, namespaces, attr=False)
            # insert all declared namespaces into the root element
            if node == self.tree._root:
                for prefix in self.prefixes:
                    decl = self.getXMLNS(prefix, self.getNamespaceByPrefix(prefix))
                    if not prefix == '':
                        xmlns_items.append(decl)
                    else:
                        # a prefixless namespace has been declared
                        if node.tag.startswith("{"):
                            # insert the declaration only if root has a namespace
                            xmlns_items.append(decl)
                            if xmlns and xmlns[1] == decl[1]:
                                # root has the same namespace, so don't redeclare it
                                xmlns = None
            if xmlns:
                xmlns_items.append(xmlns)
            self.writeStartTagOpen(tag)
            # write attribute nodes
            for attrname, value in attributes:
                attrname, xmlns = self.addPrefix(attrname, namespaces)
                if xmlns:
                    xmlns_items.append(xmlns)
                self.writeAttr(attrname, value)
            # write collected xmlns attributes
            for attrname, value in xmlns_items:
                self.writeAttr(attrname, value)
            if node.text or len(node):
                self.writeStartTagClose()
                if node.text:
                    self.writeContent(node.text)
                for n in node:
                    self.write(n, namespaces.copy())
                self.writeEndTag(tag)
            else:
                self.writeEmptyTagClose()
            # for attrname, value in xmlns_items:
            #    del namespaces[value]
        if node.tail:
            self.writeContent(node.tail)
    self.write = write
    return self

