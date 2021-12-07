load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//enum", enum="enum")
load("@stdlib//io", io="io")
load("@stdlib//itertools", itertools="itertools")
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//sets", sets="sets")
load("@stdlib//types", types="types")
load("@stdlib//zlib", zlib="zlib")
load("@vendor//option/result", Error="Error")


_IterWalkState = enum.Enum('_IterWalkState', [
    ('PRE', 0),
    ('POST', 1)
])


BASIC = "BASIC"
EXCLUSIVE = "EXCLUSIVE"
XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"
XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/"


def XMLWriter(tree, namespaces=None, encoding="utf-8"):
    """
    A modification of the _write method of ElementTree
    which supports namespaces in a reasonable way
    """

    self = larky.mutablestruct(
        __name__='XMLWriter',
        __class__=XMLWriter,
        default_namespaces={
            "http://www.w3.org/XML/1998/namespace": "xml"
        }
    )

    def setup_declared_namespaces(namespaces):
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
        self.prefixes = sorted(self.prefixes)
    self.setup_declared_namespaces = setup_declared_namespaces

    def __init__(tree, namespaces, encoding, **options):
        self.tree = tree
        self.encoding = encoding
        self.options = options or {}
        self.setup_declared_namespaces(namespaces)
        return self
    self = __init__(tree, namespaces, encoding)

    def __call__(file=None,
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

        Some additional non-standard options are below:

        :param options
         - tree - replace the tree from __init__, default = None
         - namespaces - overwrite declared namespaces, default = None
         - short_empty_elements - <p></p> vs <p />, default = True
         - space_inside_empty_tag - <br /> vs <br/>, default = False
         - compression, not used but is 0

        """
        if file == None:
            file = io.StringIO()
        elif not getattr(file, "write", False):
            fail("expected file parameter to have a `write()` " +
                 "method. Try passing in io.StringIO() or io.BytesIO()")
        self.file = file

        if options and self.options:
            self.options.update(options)
        elif options:
            self.options = options

        self.tree = self.options.get('tree', self.tree)
        if encoding != None:
            self.encoding = encoding

        _namespaces = self.options.get('namespaces', None)
        if _namespaces != None:
            self.setup_declared_namespaces(_namespaces)

        self.options['method'] = method
        self.options['pretty_print'] = pretty_print
        self.options['with_tail'] = with_tail
        self.options['standalone'] = standalone
        self.options['doctype'] = doctype
        self.options['exclusive'] = exclusive
        self.options['inclusive_ns_prefixes'] = inclusive_ns_prefixes
        self.options['with_comments'] = with_comments
        self.options['strip_text'] = strip_text

        if not any((
                hasattr(self.tree, "getroot"),
                type(self.tree) == 'ElementTree',
        )):
            fail("self.tree is of type %s. Expected type of ElementTree" % (
                type(self.tree)
            ))

        ns = dict(**self.declared_namespaces)
        supported_methods = ("xml", "c14n", "c14n2")
        if self.options['method'] not in supported_methods:
            fail(
                "ValueError: " +
                ("method %s is not supported" % self.options['method']) +
                "following methods are currently supported in Larky:" +
                ','.join(supported_methods)
            )
        # need a copy here, because original must stay intact
        if self.options.get('autowrite', True):
            self.write(namespaces=ns, xml_declaration=xml_declaration)
            return self.file.getvalue()
        return self
    self.__call__ = __call__

    def escape_text(text):
        text = self.encode(text)
        text = (text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\r", "&#13;")
                )
        return text
    self.escape_text = escape_text

    #TODO:remove?
    def escape_spaces(text):
        return (text
                .replace("\t", "&#9;")
                .replace("\n", "&#10;")
                )
    self.escape_spaces = escape_spaces

    def escape_attr(text):
        return self.escape_spaces(
            self.escape_text(text)
        ).replace('"', '&quot;')
    self.escape_attr = escape_attr

    def encode(text):
        encoding = self.encoding
        if types.is_bytes(encoding):
            encoding = encoding.decode('utf-8')
        if types.is_bytes(text):
            # TODO(mahmoudimus): support encoding
            # return text
            return text.decode('utf-8')
        elif types.is_string(text):
            return text
            # TODO(mahmoudimus): support encoding
            # return codecs.encode(text, encoding=encoding)
        fail('TypeError: XMLWriter: Cannot encode objects of type %s' % type(text))
    self.encode = encode

    def cdata(text):
        if text.find("]]>") >= 0:
            fail("ValueError: ']]>' not allowed in a CDATA section")
        return "<![CDATA[%s]]>" % text
    self.cdata = cdata

    def write_dtd(doctype):
        """
        Handles a doctype event.

        Writes a document type declaration to the stream.
        """
        if not doctype or not hasattr(doctype, '_name'):
            return
        # Name in document type declaration must match the root element tag.
        # For XML, case sensitive match, for HTML insensitive.
        h = [
            "<!DOCTYPE ",
            doctype._name,
        ]
        if doctype.public_id:
            h.append(' PUBLIC "')
            h.append(doctype.public_id)
            if doctype.system_id:
                h.append('" ')
            else:
                h.append('"')
        elif doctype.system_id:
            h.append(' SYSTEM ')

        if doctype.system_id:
            if '"' in doctype.system_id:
                quotechar = '\''
            else:
                quotechar = '"'
            h.append(quotechar)
            h.append(doctype.system_id)
            h.append(quotechar)

        # TODO: support the below?
        #  if (not c_dtd.entities and not c_dtd.elements and
        #        not c_dtd.attributes and not c_dtd.notations and
        #        not c_dtd.pentities):
        #     tree.xmlOutputBufferWrite(c_buffer, 2, '>\n')
        #     return
        if not doctype.data:
            h.append('>\n')
            self.file.write(''.join(h))
            return

        h.append(' [\n')
        # TODO: support notations?
        #  if c_dtd.notations:
        #     tree.xmlDumpNotationTable(c_buf, <tree.xmlNotationTable*>c_dtd.notations)
        #     tree.xmlOutputBufferWrite(...)
        h.append(doctype.data.strip().replace("> <", ">\n<"))
        h.append('\n')
        h.append("]>\n")
        self.file.write(''.join(h))
        return
    self.write_dtd = write_dtd

    def write_xml_header():
        """
        Mimics lxml _writeDeclarationToBuffer() in pure Larky
        :return:
        :rtype:
        """
        # self.file.write('<?xml version="1.0" encoding="%s"?>\n' % self.encoding)
        version = self.options.get('version', "1.0")
        standalone = self.options.get('standalone', None)
        header = ['<?xml version="', version, '" encoding="', self.encoding]
        if standalone == None:
            header.append('"?>\n')
        elif not standalone:
            header.append('" standalone="no"?>\n')
        else:
            header.append('" standalone="yes"?>\n')
        self.file.write("".join(header))
    self.write_xml_header = write_xml_header

    def write_attr(name, value):
        self.file.write(" %s=\"%s\"" % (self.encode(name), self.escape_attr(value)))
    self.write_attr = write_attr

    def write_start_tag_open(name):
        self.file.write("<%s" % self.encode(name))
    self.write_start_tag_open = write_start_tag_open

    def write_start_tag_close():
        self.file.write(">")
    self.write_start_tag_close = write_start_tag_close

    def write_content(text):
        self.file.write(self.escape_text(text))
    self.write_content = write_content

    def write_empty_tag_close():
        s = " />" if self.options.get('space_inside_empty_tag', True) else "/>"
        self.file.write(s)
    self.write_empty_tag_close = write_empty_tag_close

    def write_end_tag(name):
        self.file.write("</%s>" % self.encode(name))
    self.write_end_tag = write_end_tag

    def write_comment(node):
        self.file.write("<!--%s-->" % node.text)
    self.write_comment = write_comment

    def write_pi(node):
        self.file.write("<?%s?>" % node.text)
    self.write_pi = write_pi

    def get_namespace_by_prefix(prefix):
        return self.namespaces_by_prefix[prefix]
    self.get_namespace_by_prefix = get_namespace_by_prefix

    def get_xmlns(prefix, namespace_uri):
        """
        return a "xmlns"-prefixed namespace declaration
        """
        if prefix == '':
            # unprefixed namespace ("xmlns" attribute)
            return "xmlns", namespace_uri
        else:
            # prefixed namespace ("xmlns:xy=" attribute)
            return "xmlns:%s" % prefix, namespace_uri
    self.get_xmlns = get_xmlns

    def add_prefix(name, namespaces, attr=True):
        """
        given a decorated name (of the form {uri}tag),
        return prefixed name and namespace declaration
        """
        if not name[:1] == "{":
            # no Namespace
            return name, None
        namespace_uri, name = name[1:].split("}", 1)
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
                        namespaces.pop(self.xmlns_namespace)
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
        xmlns = self.get_xmlns(prefix, namespace_uri)
        namespaces[namespace_uri] = prefix
        return name, xmlns
    self.add_prefix = add_prefix

    def _add_node_namespaces_to_root(node, xmlns, xmlns_items):
        for prefix in self.prefixes:
            decl = self.get_xmlns(prefix, self.get_namespace_by_prefix(prefix))
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
        return xmlns
    self._add_node_namespaces_to_root = _add_node_namespaces_to_root

    def _nodetype(node):
        if hasattr(node, 'nodetype'):
            return node.nodetype()
        # default to checking the tag
        print(repr(node))
        tag = node.tag
        # this is a special marker to denote a special tag subtype of Element
        # (i.e. Comment, CData, etc)
        # this helps with backwards compatibility with ElementTree
        if builtins.callable(tag):
            return larky.impl_function_name(tag)
        return type(tag)
    self._nodetype = _nodetype

    def _is_element(node):
        return type(node) not in ("XMLTree",)
    self._is_element = _is_element

    def write(namespaces, xml_declaration):
        root = self.tree.getroot()
        if root == None:
            fail("self.tree.getroot() == None!")

        c_doc = getattr(root, 'doc', None)  # TODO: introduce fakedoc?
        if self.options.get('debug'):
            print("DEBUG", "root owner doc?", repr(c_doc))
        write_complete_document = self.options.get('write_complete_document',
                                                   True)
        # if write_xml_declaration and c_method == OUTPUT_METHOD_XML:
        if xml_declaration and self.options['method'] == 'xml':
            # _writeDeclarationToBuffer(c_buffer, c_doc.version, encoding, standalone)
            self.write_xml_header()

        # comments/processing instructions before doctype declaration
        if write_complete_document and c_doc.docinfo:
            if self.options.get('debug'):
                print("DEBUG", "comments/processing instructions before doctype")
            # for n in c_doc:
            #     print("DEBUG", "n:", repr(n), "n.next_sibling():", repr(n.next_sibling()))
            #     if self._nodetype(n) == 'DocumentType':
            #         break
            #     self.write_node(
            #         n,
            #         namespaces=namespaces,
            #         with_tail=self.options["with_tail"],
            #         write_complete_document=write_complete_document)
            self.write_previous_siblings(c_doc.docinfo, namespaces)
        if self.options['doctype']:
            self.write_content(self.options['doctype'])
            self.write_content('\n')

        # write internal DTD subset, preceding PIs/comments, etc.
        if write_complete_document and not self.options['doctype']:
            if self.options.get('debug'):
                print("DEBUG", "write internal DTD subset")
            self.write_dtd(c_doc.docinfo)
            self.write_previous_siblings(root, namespaces)
            # _write = False
            # for n in c_doc:
            #     # print("DEBUG", "n:", repr(n))
            #     if n == root:
            #         _write = False
            #         break
            #     if self._nodetype(n) == 'DocumentType':
            #         _write = True
            #         continue
            #     if _write:
            #         print("DEBUG", "writing--", repr(n))
            #         self.write_node(
            #             n,
            #             namespaces=namespaces,
            #             with_tail=self.options["with_tail"],
            #             write_complete_document=write_complete_document)
            # self.write_content('\n')

        # c_nsdecl_node = c_node
        # if not c_node.parent or c_node.parent.type != tree.XML_DOCUMENT_NODE:
        #     # copy the node and add namespaces from parents
        #     # this is required to make libxml write them
        #     c_nsdecl_node = tree.xmlCopyNode(c_node, 2)
        #     if not c_nsdecl_node:
        #         c_buffer.error = xmlerror.XML_ERR_NO_MEMORY
        #         return
        #     _copyParentNamespaces(c_node, c_nsdecl_node)
        #
        #     c_nsdecl_node.parent = c_node.parent
        #     c_nsdecl_node.children = c_node.children
        #     c_nsdecl_node.last = c_node.last
        self.write_node(
            root, namespaces,
            with_tail=self.options["with_tail"],
            write_complete_document=self.options.get(
                'write_complete_document', True
            ))
        # write tail, trailing comments, etc.
        # if with_tail:
        #     _writeTail(c_buffer, c_node, encoding, c_method, pretty_print)
        if write_complete_document:
            self.write_next_siblings(root, namespaces)
        if self.options['pretty_print']:
            self.write_content('\n')

    self.write = write

    def write_previous_siblings(c_node, namespaces):
        if c_node.getparent() and (
                self._nodetype(c_node.getparent()) != 'Document'
        ):
            return
        # we are at a root node, so add PI and comment siblings
        c_sibling = c_node
        for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
            c_prev_sibling = c_sibling.previous_sibling()
            # print(repr(c_node), repr(c_sibling.previous_sibling()))
            if not c_prev_sibling or self._nodetype(c_prev_sibling) not in (
                'Comment',
                'ProcessingInstruction',
            ):
                break
            c_sibling = c_prev_sibling

        for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
            if not c_sibling or c_sibling == c_node:
                break
            self.write_node(c_sibling, namespaces)
            if self.options['pretty_print']:
                self.write_content('\n')
            c_sibling = c_sibling.next_sibling()
    self.write_previous_siblings = write_previous_siblings

    def write_next_siblings(c_node, namespaces):
        if c_node.getparent() and (
                self._nodetype(c_node.getparent()) != 'Document'
        ):
            return
        # we are at a root node, so add PI and comment siblings
        c_sibling = c_node.next_sibling()
        for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
            if not c_sibling or self._nodetype(c_sibling) not in (
                'Comment',
                'ProcessingInstruction',
            ):
                break
            if self.options['pretty_print']:
                self.write_content('\n')
            self.write_node(c_sibling, namespaces)
            c_sibling = c_sibling.next_sibling()

    self.write_next_siblings = write_next_siblings

    def write_node(node, namespaces, with_tail=True, write_complete_document=True):
        q = [(_IterWalkState.PRE, (node, namespaces, None))]
        if self.options.get('debug'):
            for n in node:
                print("node:", repr(node), "child:", repr(n))
        for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
            if not q:
                break
            state, payload = q.pop(0)
            if state == _IterWalkState.PRE:
                (node, namespaces, _parent) = payload
                # write XML to file
                tag = node.tag
                tag_type = self._nodetype(node)
                # tag_type = larky.impl_function_name(tag) if types.is_function(tag) else type(tag)
                if self.options.get('debug'):
                    print("xmlwriter.write(): ", tag, tag_type)
                if tag_type == 'Document':
                    children = [
                        (_IterWalkState.PRE, (n, dict(**namespaces), node,))
                        for n in node
                    ]
                    q = children + q
                    continue
                elif tag_type == 'Comment':
                    # comments are not parsed by ElementTree!
                    self.write_comment(node)
                    if (write_complete_document or with_tail) and node.tail:
                        self.write_content(node.tail)
                    continue
                elif tag_type == 'Text':
                    # Text are not parsed by ElementTree!
                    if self.options.get('debug'):
                        print("text node:", repr(node.text), repr(node.tail))
                    self.write_content(node.text)
                    if (write_complete_document or with_tail) and node.tail:
                        self.write_content(node.tail)
                    continue
                elif tag_type == 'ProcessingInstruction':
                    # PI's are not parsed by ElementTree!
                    self.write_pi(node)
                    if (write_complete_document or with_tail) and node.tail:
                        self.write_content(node.tail)
                    continue
                elif all((
                        tag_type == 'DocumentType',
                        not self.options['doctype']
                )):
                    self.write_dtd(node)
                    if (write_complete_document or with_tail) and node.tail:
                        self.write_content(node.tail)
                    continue
                else:
                    xmlns_items = [] # collects new namespaces in this scope
                    attributes = list(node.items())
                    for attrname, value in attributes:
                        # (the elementtree parser discards these attributes)
                        if attrname.startswith("xmlns:"):
                            namespaces[value] = attrname[6:]
                        if attrname == "xmlns":
                            namespaces[value] = ''
                    # get namespace for tag
                    tag, xmlns = self.add_prefix(tag, namespaces, attr=False)
                    # insert all declared namespaces into the root element
                    if node == self.tree.getroot():
                        xmlns = self._add_node_namespaces_to_root(
                            node, xmlns, xmlns_items
                        )
                    if xmlns:
                        xmlns_items.append(xmlns)
                    self.write_start_tag_open(tag)
                    # write attribute nodes
                    for attrname, value in attributes:
                        attrname, xmlns = self.add_prefix(attrname, namespaces)
                        if xmlns:
                            xmlns_items.append(xmlns)
                        self.write_attr(attrname, value)
                    # write collected xmlns attributes
                    for attrname, value in xmlns_items:
                        self.write_attr(attrname, value)

                    if not (node.text or len(node)):
                        if self.options.get('short_empty_elements', True):
                            self.write_empty_tag_close()
                        else:
                            self.write_start_tag_close()
                            self.write_end_tag(tag)

                        if (write_complete_document or with_tail) and node.tail:
                            self.write_content(node.tail)
                    else:
                        self.write_start_tag_close()
                        if node.text:
                            self.write_content(node.text)

                        # self.write(n, dict(**namespaces))
                        children = [
                            (_IterWalkState.PRE, (n, dict(**namespaces), node,))
                            for n in node
                        ]
                        # post visit
                        children.append((_IterWalkState.POST, (node, tag)))
                        q = children + q
                        # self.write_end_tag(tag)
                    # for attrname, value in xmlns_items:
                    #    del namespaces[value]
            elif state == _IterWalkState.POST:
                node, tag = payload
                self.write_end_tag(tag)
                # if (write_complete_document or with_tail) and node.tail:
                #     self.write_content(node.tail)

    self.write_node = write_node
    return self


def TreeSerializer():
    self = larky.mutablestruct(__name__='TreeSerializer',
                               __class__=TreeSerializer,
                               _queue=[])
    # API
    def walk_starttag(tag, attribs):
        pass
    self.walk_starttag = walk_starttag

    def walk_endtag(tag):
        pass
    self.walk_endtag = walk_endtag

    def walk_data(data):
        pass
    self.walk_data = walk_data

    def walk_emptytag(tag, attribs):
        self.walk_starttag(tag, attribs)
        self.walk_endtag(tag)
    self.walk_emptytag = walk_emptytag

    def walk_comment(text):
        pass
    self.walk_comment = walk_comment

    def walk_pi(target, text):
        pass
    self.walk_pi = walk_pi

    def serialize(root):
        """ walk the tree """
        self._queue.clear()
        if len(root) or root.text:
            self.walk_starttag(root.tag, root.attrib)
            if root.text:
                self.walk_data(root.text)
            if len(root):
                self._queue.append((root, -1))
                for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
                    if not self._queue:
                        break
                    # parent, child_index
                    this, i = self._queue.pop()
                    i += 1
                    if i >= len(this):
                        # we're done
                        self.walk_endtag(this.tag)
                        if this.tail:
                            self.walk_data(this.tail)
                    else:
                        # go deeper
                        self._queue.append((this, i))
                        e = this[i]
                        if len(e) or e.text:
                            self.walk_starttag(e.tag, e.attrib)
                            if e.text:
                                self.walk_data(e.text)
                            if len(e):
                                self._queue.append((e, -1))
                            else:
                                self.walk_endtag(e.tag)
                                if e.tail:
                                    self.walk_data(e.tail)
                        else:
                            self.walk_emptytag(e.tag, e.attrib)
                            if e.tail:
                                self.walk_data(e.tail)
            else:
                self.walk_endtag(root.tag)
        else:
            self.walk_emptytag(root.tag, root.attrib)
    self.serialize = serialize
    return self


def XMLSerializer(file):
    self = TreeSerializer()
    self.__name__ = 'XMLSerializer'
    self.__class__ = XMLSerializer

    def __init__(file):
        self.file = file
        return self
    self = __init__(file)

    def _escape_data(text):
        if "&" in text:
            text = text.replace("&", "&amp;")
        if "<" in text:
            text = text.replace("<", "&lt;")
        if ">" in text:
            text = text.replace(">", "&gt;")
        return text
    self._escape_data = _escape_data

    def _escape_attrib(text):
        if "&" in text:
            text = text.replace("&", "&amp;")
        if "<" in text:
            text = text.replace("<", "&lt;")
        if ">" in text:
            text = text.replace(">", "&gt;")
        if '"' in text:
            text = text.replace('"', "&quot;")
        if "\n" in text:
            text = text.replace("\n", "&#10;")
        return text

    self._escape_attrib = _escape_attrib

    def walk_starttag(tag, attrib):
        if attrib:
            self.file.write(
                "<%s %s>"
                % (
                    tag,
                    " ".join(
                        ['%s="%s"' % (k, self._escape_attrib(v))
                            for k, v in sorted(attrib.items())]
                    ),
                )
            )
        else:
            self.file.write("<%s>" % tag)
    self.walk_starttag = walk_starttag

    def walk_endtag(tag):
        self.file.write("</%s>" % tag)
    self.walk_endtag = walk_endtag

    def walk_emptytag(tag, attrib):
        if attrib:
            self.file.write(
                "<%s %s />"
                % (
                    tag,
                    " ".join(
                        ['%s="%s"' % (k, self._escape_attrib(v))
                            for k, v in sorted(attrib.items())]
                    ),
                )
            )
        else:
            self.file.write("<%s/>" % tag)
    self.walk_emptytag = walk_emptytag

    def walk_data(data):
        self.file.write(self._escape_data(data))
    self.walk_data = walk_data
    return self


# below is modified from python amara2 library


def xmlprinter(stream, encoding):
    """
    An `xmlprinter` instance provides functions for serializing an XML or
    XML-like document to a stream, based on SAX-like event calls
    initiated by a _Visitor instance.
    The methods in this base class attempt to emit a well-formed parsed
    general entity conformant to XML 1.0 syntax, with no extra
    whitespace added for visual formatting. Subclasses may emit
    documents conformant to other syntax specifications or with
    additional whitespace for indenting.
    The degree of well-formedness of the output depends on the data
    supplied in the event calls; no checks are done for conditions that
    would result in syntax errors, such as two attributes with the same
    name, "--" in a comment, etc. However, attribute() will do nothing
    if the previous event was not startElement(), thus preventing
    spurious attribute serializations.
    `canonical_form` must be None, BASIC or EXCLUSIVE
    It controls c14n of the serialization, according to
    http://www.w3.org/TR/xml-c14n ("basic") or
    http://www.w3.org/TR/xml-exc-c14n/ ("exclusive")
    """

    _canonical_form = None
    self = larky.mutablestruct(__name__='xmlprinter', __class__=xmlprinter)

    def __init__(stream, encoding):
        """
        `stream` must be a file-like object open for writing binary
        data. `encoding` specifies the encoding which is to be used for
        writing to the stream.
        """
        self.stream = stream
        xs = self.stream
        self.encoding = encoding
        self.write_ascii = xs.write # xs.write_ascii
        self.write_encode = xs.write # xs.write_encode
        self.write_escape = xs.write #xs.write_escape
        self._element_name = None
        self.omit_declaration = False
        return self
    self = __init__(stream, encoding)

    def start_document(version="1.0", standalone=None):
        """
        Handles a startDocument event.
        Writes XML declaration or text declaration to the stream.
        """
        if not self.omit_declaration:
            self.write_ascii(
                '<?xml version="%s" encoding="%s"' % (version, self.encoding)
            )
            if standalone != None:
                self.write_ascii(' standalone="%s"' % standalone)
            self.write_ascii("?>\n")
        return
    self.start_document = start_document

    def end_document():
        """
        Handles an endDocument event.
        Writes any necessary final output to the stream.
        """
        if self._element_name:
            if self._canonical_form:
                self.write_ascii("</")
                self.write_encode(self._element_name, "end-tag name")
                self.write_ascii(">")
            else:
                # No element content, use minimized form
                self.write_ascii("/>")
            self._element_name = None
        return
    self.end_document = end_document

    def doctype(name, publicid, systemid):
        """
        Handles a doctype event.
        Writes a document type declaration to the stream.
        """
        if self._canonical_form:
            return
        if self._element_name:
            self.write_ascii(">")
            self._element_name = None
        if publicid and systemid:
            self.write_ascii("<!DOCTYPE ")
            self.write_encode(name, "document type name")
            self.write_ascii(' PUBLIC "')
            self.write_encode(publicid, "document type public-id")
            self.write_ascii('" "')
            self.write_encode(systemid, "document type system-id")
            self.write_ascii('">\n')
        elif systemid:
            self.write_ascii("<!DOCTYPE ")
            self.write_encode(name, "document type name")
            self.write_ascii(' SYSTEM "')
            self.write_encode(systemid, "document type system-id")
            self.write_ascii('">\n')
        return
    self.doctype = doctype

    def start_element(namespace, name, nsdecls, attributes):
        """
        Handles a start-tag event.
        Writes part of an element's start-tag or empty-element tag to
        the stream, and closes the start tag of the previous element,
        if one remained open. Writes the xmlns attributes for the given
        sequence of prefix/namespace-uri pairs, and invokes attribute() as
        neeeded to write the given sequence of attribute qname/value pairs.
        Note, the `namespace` argument is ignored in this class.
        """
        write_ascii, write_escape, write_encode = (
            self.write_ascii,
            self.write_escape,
            self.write_encode,
        )

        if self._element_name:
            # Close current start tag
            write_ascii(">")
        self._element_name = name

        write_ascii("<")
        write_encode(name, "start-tag name")

        if self._canonical_form:
            # Create the namespace "attributes"
            namespace_attrs = sorted([
                (prefix and "xmlns:" + prefix or "xmlns", uri)
                for prefix, uri in nsdecls
            ])
            # Write the namespaces decls, in alphabetical order of prefixes, with
            # the default coming first (easy since None comes before any actual
            # Unicode value)
            # sorted_attributes = [ name, value for (name, value) in sorted(attributes) ]
            sorted_attributes = sorted(attributes)
            # FIXME: attributes must be sorted using nsuri / local combo (where nsuri is "" for null namespace)
            for name, value in namespace_attrs:
                # FIXME: check there are no redundant NSDecls
                write_ascii(" ")
                write_encode(name, "attribute name")
                # Replace characters illegal in attribute values and wrap
                # the value with quotes (") in accordance with Canonical XML.
                write_ascii('="')
                write_escape(value, self._attr_entities_quot)
                write_ascii('"')
            for name, value in sorted_attributes:
                write_ascii(" ")
                write_encode(name, "attribute name")
                # Replace characters illegal in attribute values and wrap
                # the value with quotes (") in accordance with Canonical XML.
                write_ascii('="')
                write_escape(value, self._attr_entities_quot)
                write_ascii('"')

        else:
            # Create the namespace "attributes"
            nsdecls = [
                (prefix and "xmlns:" + prefix or "xmlns", uri)
                for prefix, uri in nsdecls
            ]
            # Merge the namespace and attribute sequences for output
            attributes = itertools.chain(nsdecls, attributes)
            for name, value in attributes:
                # Writes an attribute to the stream as a space followed by
                # the name, '=', and quote-delimited value. It is the caller's
                # responsibility to ensure that this is called in the correct
                # context, if well-formed output is desired.

                # Preference is given to quotes (") around attribute values,
                # in accordance with the DomWriter interface in DOM Level 3
                # Load and Save (25 July 2002 WD), although a value that
                # contains quotes but no apostrophes will be delimited by
                # apostrophes (') instead.
                write_ascii(" ")
                write_encode(name, "attribute name")
                # Special case for HTML boolean attributes (just a name)
                if value != None:
                    # Replace characters illegal in attribute values and wrap
                    # the value with appropriate quoting in accordance with
                    # DOM Level 3 Load and Save:
                    # 1. Attributes not containing quotes are serialized in
                    #    quotes.
                    # 2. Attributes containing quotes but no apostrophes are
                    #    serialized in apostrophes.
                    # 3. Attributes containing both forms of quotes are
                    #    serialized in quotes, with quotes within the value
                    #    represented by the predefined entity `&quot;`.
                    if '"' in value and "'" not in value:
                        # Use apostrophes (#2)
                        entitymap = self._attr_entities_apos
                        quote = "'"
                    else:
                        # Use quotes (#1 and #3)
                        entitymap = self._attr_entities_quot
                        quote = '"'
                    write_ascii("=")
                    write_ascii(quote)
                    write_escape(value, entitymap)
                    write_ascii(quote)
        return
    self.start_element = start_element

    def end_element(namespace, name):
        """
        Handles an end-tag event.
        Writes the closing tag for an element to the stream, or, if the
        element had no content, finishes writing the empty element tag.
        Note, the `namespace` argument is ignored in this class.
        """
        if self._element_name:
            self._element_name = None
            if self._canonical_form:
                self.write_ascii(">")
            else:
                # No element content, use minimized form
                self.write_ascii("/>")
                return
        self.write_ascii("</")
        self.write_encode(name, "end-tag name")
        self.write_ascii(">")
        return
    self.end_element = end_element

    def text(text, disable_escaping=False):
        """
        Handles a text event.
        Writes character data to the stream. If the `disable_escaping` flag
        is not set, then unencodable characters are replaced with
        numeric character references; "&" and "<" are escaped as "&amp;"
        and "&lt;"; and ">" is escaped as "&gt;" if it is preceded by
        "]]". If the `disable_escaping` flag is set, then the characters
        are written to the stream with no escaping of any kind, which
        will result in an exception if there are unencodable characters.
        """
        if self._canonical_form:
            text.replace("\r\n", "\r")

        if self._element_name:
            self.write_ascii(">")
            self._element_name = None

        if disable_escaping:
            # Try to write the raw encoded string (may throw exception)
            self.write_encode(text, "text")
        else:
            # FIXME: only escape ">" if after "]]"
            # (may not be worth the trouble)
            self.write_escape(text, self._text_entities)
        return
    self.text = text

    def cdata_section(data):
        """
        Handles a cdataSection event.
        Writes character data to the stream as a CDATA section.
        """
        if self._element_name:
            self.write_ascii(">")
            self._element_name = None

        if self._canonical_form:
            # No CDATA sections in c14n
            text.replace("\r\n", "\r")
            self.write_escape(data, self._text_entities)
        else:
            sections = data.split("]]>")
            self.write_ascii("<![CDATA[")
            self.write_encode(sections[0], "CDATA section")
            for section in sections[1:]:
                self.write_ascii("]]]]><![CDATA[>")
                self.write_encode(section, "CDATA section")
            self.write_ascii("]]>")
        return
    self.cdata_section = cdata_section

    def processing_instruction(target, data):
        """
        Handles a processingInstruction event.
        Writes a processing instruction to the stream.
        """
        if self._element_name:
            self.write_ascii(">")
            self._element_name = None

        self.write_ascii("<?")
        self.write_encode(target, "processing instruction target")
        if data:
            self.write_ascii(" ")
            self.write_encode(data, "processing instruction data")
        self.write_ascii("?>")
        return
    self.processing_instruction = processing_instruction

    def comment(data):
        """
        Handles a comment event.
        Writes a comment to the stream.
        """
        if self._element_name:
            self.write_ascii(">")
            self._element_name = None

        self.write_ascii("<!--")
        self.write_encode(data, "comment")
        self.write_ascii("-->")
        return
    self.comment = comment

    # Entities as defined by Canonical XML 1.0 (http://www.w3.org/TR/xml-c14n)
    # For XML 1.1, add u'\u0085': '&#133;' and u'\u2028': '&#8232;' to all
    self._text_entities = {
            "<": "&lt;",
            ">": "&gt;",
            "&": "&amp;",
            "\r": "&#13;",
        }

    self._attr_entities_quot = {
            "<": "&lt;",
            "&": "&amp;",
            "\t": "&#9;",
            "\n": "&#10;",
            "\r": "&#13;",
            '"': "&quot;",
        }


    self._attr_entities_apos = {
            "<": "&lt;",
            "&": "&amp;",
            "\t": "&#9;",
            "\n": "&#10;",
            "\r": "&#13;",
            "'": "&apos;",
        }

    return self


def canonicalxmlprinter(stream, encoding):
    """
    `canonicalxmlprinter` emits only c14n XML:
      http://www.ibm.com/developerworks/xml/library/x-c14n/
      http://www.w3.org/TR/xml-c14n
    Does not yet:
      * Normalize all attribute values
      * Specify all default attributes
    Note: this class is fully compatible with exclusive c14n:
      http://www.w3.org/TR/xml-exc-c14n/
    whether or not the operation is exclusive depends on preprocessing
    operations appplied by the caller.
    """
    # FIXME: A bit inelegant to require the encoding, then throw it away.  Perhaps
    # we should at least issue a warning if they send a non-UTF8 encoding
    def __init__(stream, encoding):
        """
        `stream` must be a file-like object open for writing binary
        data.
        """
        self = xmlprinter(stream, "utf-8")
        self.__name__ = 'canonicalxmlprinter'
        self.__class__ = canonicalxmlprinter
        # Enable canonical-form output.
        self._canonical_form = True
        self.omit_declaration = True
        return self
    self = __init__(stream, encoding)

    def prepare(node, kwargs):
        """
        `inclusive_prefixes` is a list (or None) of namespaces representing the
        "InclusiveNamespacesPrefixList" list in exclusive c14n.
        It may only be a non-empty list if `canonical_form` == EXCLUSIVE
        """
        exclusive = kwargs.get("exclusive", False)
        nshints = kwargs.get("nshints", {})
        inclusive_prefixes = kwargs.get("inclusive_prefixes", [])
        added_attributes = {}  # All the contents should be XML NS attrs
        if not exclusive:
            # Roll in ancestral xml:* attributes
            parent_xml_attrs = node.xml_select("ancestor::*/@xml:*")
            for attr in parent_xml_attrs:
                aname = (attr.xml_namespace, attr.xml_qname)
                if aname not in added_attributes and aname not in node.xml_attributes:
                    added_attributes[attr.xml_qname] = attr.xml_value
        nsnodes = node.xml_select("namespace::*")
        inclusive_prefixes = inclusive_prefixes or []
        if "#default" in inclusive_prefixes:
            inclusive_prefixes.remove("#default")
            inclusive_prefixes.append("")
        decls_to_remove = []
        if exclusive:
            used_prefixes = [n.xml_prefix for n in node.xml_select("self::*|@*")]
            declared_prefixes = []
            for ans, anodename in node.xml_attributes:
                if ans == XMLNS_NAMESPACE:
                    attr = node.xml_attributes[ans, anodename]
                    prefix = attr.xml_local
                    declared_prefixes.append(prefix)
                    # print attr.prefix, attr.localName, attr.nodeName
                    if attr.xml_local not in used_prefixes:
                        decls_to_remove.append(prefix)
            # for prefix in used_prefixes:
            #    if prefix not in declared_prefixes:
            #        nshints[ns.nodeName] = ns.value
        # Roll in ancestral  NS nodes
        for ns in nsnodes:
            # prefix = ns.xml_qname if isinstance(ns, tree.namespace) else ns.xml_qname
            # print (ns.xml_name, ns.xml_value)
            prefix = ns.xml_name
            if (
                ns.xml_value != XML_NAMESPACE
                and ns.xml_name not in node.xml_namespaces
                and (not exclusive or ns.xml_name in inclusive_prefixes)
            ):
                # added_attributes[(XMLNS_NAMESPACE, ns.nodeName)] = ns.value
                nshints[prefix] = ns.xml_value
            elif (
                exclusive
                and prefix in used_prefixes
                and prefix not in declared_prefixes
            ):
                nshints[prefix] = ns.xml_value
        kwargs["nshints"] = nshints
        if "inclusive_prefixes" in kwargs:
            operator.delitem(kwargs, "inclusive_prefixes")
        if "exclusive" in kwargs:
            operator.delitem(kwargs, "exclusive")
        if "nshints" in kwargs:
            operator.delitem(kwargs, "nshints")
        # FIXME: nshints not yet actually used.  Required for c14n of nodes below the top-level
        self._nshints = nshints or {}
        return kwargs
    self.prepare = prepare
    return self


def xmlprettyprinter(stream, encoding):
    """
    An xmlprettyprinter instance provides functions for serializing an
    XML or XML-like document to a stream, based on SAX-like event calls.
    The methods in this subclass of xmlprinter produce the same output
    as the base class, but with extra whitespace added for visual
    formatting. The indent attribute is the string used for each level
    of indenting. It defaults to 2 spaces.
    """

    def __init__(stream, encoding):
        self = xmlprinter(stream, encoding)
        self.__name__ = 'xmlprettyprinter'
        self.__class__ = xmlprettyprinter
        # The amount of indent for each level of nesting
        self.indent = "  "
        self._level = 0
        self._can_indent = False  # don't indent first element
        return self

    self = __init__(stream, encoding)

    def start_element(namespace, name, namespaces, attributes):
        if self._element_name:
            self.write_ascii(">")
            self._element_name = None
        if self._can_indent:
            self.write_ascii("\n" + (self.indent * self._level))
        xmlprinter.start_element(self, namespace, name, namespaces, attributes)
        self._level += 1
        self._can_indent = True
        return
    self.start_element = start_element

    def end_element(namespace, name):
        self._level -= 1
        # Do not break short tag form (<tag/>)
        if self._can_indent and not self._element_name:
            self.write_ascii("\n" + (self.indent * self._level))
        xmlprinter.end_element(self, namespace, name)
        # Allow indenting after endtags
        self._can_indent = True
        return
    self.end_element = end_element

    def text(data, disable_escaping=False):
        xmlprinter.text(self, data, disable_escaping)
        # Do not allow indenting for elements with mixed content
        self._can_indent = False
        return
    self.text = text

    def cdata_section(data):
        xmlprinter.cdata_section(self, data)
        # Do not allow indenting for elements with mixed content
        self._can_indent = False
        return
    self.cdata_section = cdata_section

    def processing_instruction(target, data):
        if self._element_name:
            self.write_ascii(">")
            self._element_name = None
        if self._can_indent:
            self.write_ascii("\n" + (self.indent * self._level))
        xmlprinter.processing_instruction(self, target, data)
        # Allow indenting after processing instructions
        self._can_indent = True
        return
    self.processing_instruction = processing_instruction

    def comment(data):
        if self._element_name:
            self.write_ascii(">")
            self._element_name = None
        if self._can_indent:
            self.write_ascii("\n" + (self.indent * self._level))
        xmlprinter.comment(self, data)
        # Allow indenting after comments
        self._can_indent = True
        return
    self.comment = comment
    return self




xmlwriter = larky.struct(
    __name__='xmlwriter',
    XMLWriter=XMLWriter,
    xmlprinter=xmlprinter,
    canonicalxmlprinter=canonicalxmlprinter,
    xmlprettyprinter=xmlprettyprinter,
    TreeSerializer=TreeSerializer,
    XMLSerializer=XMLSerializer,
)