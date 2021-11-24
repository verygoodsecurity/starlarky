load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//io", io="io")
load("@stdlib//enum", enum="enum")
load("@stdlib//types", types="types")
load("@stdlib//sets", sets="sets")
load("@stdlib//zlib", zlib="zlib")
load("@stdlib//operator", operator="operator")
load("@vendor//option/result", Error="Error")
load("@stdlib//xml/etree/ElementTree", element_tree="ElementTree")
load("@stdlib//xml/etree/ElementPath", ElementPath="ElementPath")
load("@vendor//_etreeplus/xmlwriter", xmlwriter="xmlwriter")
load("@vendor//_etreeplus/dom/ext/c14n", c14n="c14n")
load("@vendor//elementtree/ElementC14N", ElementC14N="ElementC14N")


ELEMENT_TYPES = ['Element', 'XMLNode']


def iselement(element):
    """iselement(element)
    Checks if an object appears to be a valid element object or
    if *element* appears to be an Element.
    """
    if hasattr(element, "tag"):
        return True
    return type(element) in ELEMENT_TYPES


ELEMENT_TREE_TYPES = ['ElementTree', 'XMLTree']


def iselementtree(element_or_tree):
    """iselementtree(element_or_tree)
    Checks if an object appears to be a valid element tree object or
    if *element_or_tree* appears to be an Element.
    """
    if hasattr(element_or_tree, "getroot"):
        return True
    return type(element_or_tree) in ELEMENT_TREE_TYPES


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


def _get_ns_tag(tag):
    """Split the namespace URL out of a fully-qualified lxml tag
    name.

    Copied from lxml's src/lxml/sax.py and modified for larky
    """
    # this is a special Comment, ProcessingInstruction, etc.
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
    return "{%s}%s" % (ns_utf, tag_utf)


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


# class _MultiTagMatcher(object):
#     def __init__(self, tag):
#         self.tags = {tag} if isinstance(tag, str) else set(tag)
#
#     def __call__(self, element_or_tree):
#         # type: (ElementTreeType or ElementType) -> bool
#         return element_or_tree.tag in self.tags
#
# def _iterwalk_impl(tree, events, matcher):
#     if 'start' in events and (matcher is None or matcher(tree)):
#         yield ('start', tree)
#     for child in tree:
#         for event, node in _iterwalk_impl(child, events, matcher):
#             yield (event, node)
#     if 'end' in events and (matcher is None or matcher(tree)):
#         yield ('end', tree)
#
# def iterwalk(element_or_tree, events=(u"end",), tag=None):
#     matcher = None if tag is None or tag == "*" else _MultiTagMatcher(tag)
#     for event, node in _iterwalk_impl(element_or_tree, events, matcher):
#         yield (event, node)

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
        self._setroot(parser.close())
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


def _convert_ns_prefixes(c_dict, ns_prefixes):
    keys = sets.Set(c_dict.keys())
    return keys.intersection(sets.Set(ns_prefixes)).to_list()


def tofilelikeC14N(f, element, exclusive, with_comments,
                     compression, inclusive_ns_prefixes):

    if compression == None or compression < 0:
        compression = 0

    # c_doc = element if iselement(element) else element.getroot()
    c_doc = element.owner_doc if iselement(element) else element.getroot().owner_doc

    # c_inclusive_ns_prefixes = (
    #     # TODO: c_doc.dict == all namespaces?
    #     _convert_ns_prefixes(c_doc, inclusive_ns_prefixes)
    #     if inclusive_ns_prefixes else None
    # )
    if not hasattr(f, 'write'):
        fail("TypeError: File (or something that has 'write') expected, got %s " % type(f))

    _qnames, namespaces = element_tree._namespaces(element.getroot(), None)
    if exclusive:
        c14n.Canonicalize(
            c_doc, f,
            comments=with_comments,
            unsuppressedPrefixes=inclusive_ns_prefixes
        )
    else:
        c14n.Canonicalize(c_doc, f, comments=with_comments, nsdict=namespaces)
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
              with_tail, standalone):
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
        encoding = bytes(encoding, encoding='utf-8')
    else:
        c_enc = bytes(encoding, encoding='utf-8')
    if doctype == None:
        c_doctype = None
    else:
        c_doctype = bytes(doctype, encoding='utf-8')
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
    )
    result = c_result_buffer.getvalue()
    if encoding == b"unicode":
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
        encoding = 'ASCII'
    if standalone == None:
        is_standalone = False
    elif standalone:
        write_declaration = True
        is_standalone = True
    else:
        write_declaration = True
        is_standalone = False

    if iselement(element_or_tree):
        return _tostring(XMLTree(element_or_tree), encoding, doctype, method,
                         write_declaration, False, pretty_print, with_tail,
                         is_standalone)
    elif iselementtree(element_or_tree):
        return _tostring(element_or_tree,
                         encoding, doctype, method, write_declaration, True,
                         pretty_print, with_tail, is_standalone)
    else:
        fail("TypeError: Type '%s' cannot be serialized." % (type(element_or_tree)))


def parse(source, parser=None, base_url=None, tree_factory=XMLTree):
    """Parse XML document into element tree.

    *source* is a filename or file object containing XML data,
    *parser* is an optional parser instance defaulting to XMLParser.

    Return an ElementTree instance.

    """
    tree = tree_factory()
    tree.parse(source, parser, base_url=base_url)
    return tree


xmltree = larky.struct(
    __name__='xmltree',
    namespaced_name=_namespaced_name,
    get_ns_tag=_get_ns_tag,
    get_ns=_get_ns,
    namespaced_name_from_ns_name=_namespaced_name_from_ns_name,
    tag_matches=_tag_matches,
    XMLTree=XMLTree,
    tostring=tostring,
    tostringC14N=tostringC14N,
    tofilelikeC14N=tofilelikeC14N,
)