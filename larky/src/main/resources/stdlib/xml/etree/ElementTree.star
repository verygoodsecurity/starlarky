"""Lightweight XML support for Python.

 XML is an inherently hierarchical data format, and the most natural way to
 represent it is with a tree.  This module has two classes for this purpose:

    1. ElementTree represents the whole XML document as a tree and

    2. Element represents a single node in this tree.

 Interactions with the whole document (reading and writing to/from files) are
 usually done on the ElementTree level.  Interactions with a single XML element
 and its sub-elements are done on the Element level.

 Element is a flexible container object designed to store hierarchical data
 structures in memory. It can be described as a cross between a list and a
 dictionary.  Each Element has a number of properties associated with it:

    'tag' - a string containing the element's name.

    'attributes' - a Python dictionary storing the element's attributes.

    'text' - a string containing the element's text content.

    'tail' - an optional string containing text after the element's end tag.

    And a number of child elements stored in a Python sequence.

 To create an element instance, use the Element constructor,
 or the SubElement factory function.

 You can also use the ElementTree class to wrap an element structure
 and convert it to and from XML.

"""

# ---------------------------------------------------------------------
# Licensed to PSF under a Contributor Agreement.
# See http://www.python.org/psf/license for licensing details.
#
# ElementTree
# Copyright (c) 1999-2008 by Fredrik Lundh.  All rights reserved.
#
# fredrik@pythonware.com
# http://www.pythonware.com
# --------------------------------------------------------------------
# The ElementTree toolkit is
#
# Copyright (c) 1999-2008 by Fredrik Lundh
#
# By obtaining, using, and/or copying this software and/or its
# associated documentation, you agree that you have read, understood,
# and will comply with the following terms and conditions:
#
# Permission to use, copy, modify, and distribute this software and
# its associated documentation for any purpose and without fee is
# hereby granted, provided that the above copyright notice appears in
# all copies, and that both that copyright notice and this permission
# notice appear in supporting documentation, and that the name of
# Secret Labs AB or the author not be used in advertising or publicity
# pertaining to distribution of the software without specific, written
# prior permission.
#
# SECRET LABS AB AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD
# TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANT-
# ABILITY AND FITNESS.  IN NO EVENT SHALL SECRET LABS AB OR THE AUTHOR
# BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY
# DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
# WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
# ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
# OF THIS SOFTWARE.
# --------------------------------------------------------------------
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io", io="io")
load("@stdlib//jxml", _JXML="jxml")
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//re", re="re")
load("@stdlib//sets", sets="sets")
load("@stdlib//string", string="string")
load("@stdlib//types", types="types")
load("@stdlib//xml/etree/ElementPath", ElementPath="ElementPath")
load("@stdlib//xmllib", xmllib="xmllib")
load("@vendor//elementtree/_SimpleXMLTreeBuilderHelper",
     SimpleXMLTreeBuilderHelper="SimpleXMLTreeBuilderHelper")
load("@vendor//option/result", Result="Result", try_="try_", Error="Error")
load("@vendor//six", six="six")

__all__ = [
    # public symbols
    "Comment",
    "dump",
    "Element",
    "ElementTree",
    "fromstring",
    "fromstringlist",
    "iselement",
    "iterparse",
    "parse",
    "ParseError",
    "PI",
    "ProcessingInstruction",
    "QName",
    "SubElement",
    "tostring",
    "tostringlist",
    "TreeBuilder",
    "VERSION",
    "XML",
    "XMLID",
    "XMLParser",
    "register_namespace",
]

VERSION = "1.3.0"
_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION
StringIO = io.StringIO
BytesIO = io.BytesIO
Set = sets.Set


def ParseError(code, position, msg=""):
    """An error when parsing an XML document.

    In addition to its exception value, a ParseError contains
    two extra attributes:
        'code'     - the specific exception code
        'position' - the line and column of the error

    """
    if msg:
        msg = (" %s " % msg)
    return Error("ParseError:%s(code: %s) (position: %s)" % (msg, code, position))


# --------------------------------------------------------------------


def Element(tag, attrib=None, **extra):
    """An XML element.

    This class is the reference implementation of the Element interface.

    An element's length is its number of subelements.  That means if you
    want to check if an element is truly empty, you should check BOTH
    its length AND its text attribute.

    The element tag, attribute names, and attribute values can be either
    bytes or strings.

    *tag* is the element name.  *attrib* is an optional dictionary containing
    element attributes. *extra* are additional element attributes given as
    keyword arguments.

    Example form:
        <tag attrib>text<child/>...</tag>tail

    """
    self = larky.mutablestruct(__class__=Element, __name__='Element')

    self.tag = tag
    """The element's name."""

    self.attrib = attrib or {}
    """Dictionary of the element's attributes."""

    self.text = None
    """
    Text before first subelement. This is either a string or the value None.
    Note that if there is no text, this attribute may be either
    None or the empty string, depending on the parser.

    """

    self.tail = None
    """
    Text after this element's end tag, but before the next sibling element's
    start tag.  This is either a string or the value None.  Note that if there
    was no text, this attribute may be either None or an empty string,
    depending on the parser.

    """

    def __init__(tag, attrib, **extra):
        if not types.is_dict(attrib):
            return Error("TypeError: attrib must be dict, not %s" % type(attrib))
        attrib = dict(**attrib)

        # non-std extension
        self._nsmap = attrib.pop('nsmap', {})
        self._parent = extra.pop('parent', None)
        # _namespace_map.update(self._nsmap)
        # print(self._nsmap)
        # end non-std extensions

        attrib.update(extra)
        self.tag = tag
        self.attrib = attrib
        self._children = []
        return self
    self = __init__(tag, attrib or {}, **extra)

    def __repr__():
        # non-std repr (easier for debugging)
        _children = [c.tag for c in self.getchildren()]
        return "<Element %r, Children: %s>" % (
            self.tag,
            ", ".join(_children) if _children else _children
        )
    self.__repr__ = __repr__

    def makeelement(tag, attrib):
        """Create a new element with the same type.

        *tag* is a string containing the element name.
        *attrib* is a dictionary containing the element attributes.

        Do not call this method, use the SubElement factory function instead.

        """
        return Element(tag, attrib, parent=self)
    self.makeelement = makeelement

    def copy():
        """Return copy of current element.

        This creates a shallow copy. Subelements will be shared with the
        original tree.

        """
        elem = self.makeelement(self.tag, self.attrib)
        elem._parent = self._parent
        elem.text = self.text
        elem.tail = self.tail
        for i, c in enumerate(self._children):
            operator.setitem(elem, i, c)
        return elem
    self.copy = copy

    def __len__():
        return len(self._children)
    self.__len__ = __len__

    def __bool__():
        return len(self.getchildren()) != 0  # emulate old behaviour, for now
    self.__bool__ = __bool__

    def __getitem__(index):
        return self._children[index]
    self.__getitem__ = __getitem__

    def __setitem__(index, element):
        # if isinstance(index, slice):
        #     for elt in element:
        #         assert iselement(elt)
        # else:
        #     assert iselement(element)
        self._children[index] = element
    self.__setitem__ = __setitem__

    def __delitem__(index):
        operator.delitem(self._children, index)
    self.__delitem__ = __delitem__

    def append(subelement):
        """Add *subelement* to the end of this element.

        The new element will appear in document order after the last existing
        subelement (or directly after the text, if it's the first subelement),
        but before the end tag for this element.

        """
        self._assert_is_element(subelement)
        if not subelement.getparent():
            subelement._parent = self
            # print("append()", self.__class__)
        self._children.append(subelement)

    self.append = append

    def extend(elements):
        """Append subelements from a sequence.

        *elements* is a sequence with zero or more elements.

        """
        elems = []
        for element in elements:
            self._assert_is_element(element)
            if not element.getparent():
                element._parent = self
            elems.append(element)
        self._children.extend(elems)
        # if elems:
        #     print("extend()", self.__class__)
    self.extend = extend

    def insert(index, subelement):
        """Insert *subelement* at position *index*."""
        self._assert_is_element(subelement)
        if not subelement.getparent():
            subelement._parent = self
            # print("insert()", self.__class__)
        self._children.insert(index, subelement)
    self.insert = insert

    def _assert_is_element(e):
        # Need to refer to the actual Python implementation, not the
        # shadowing C implementation.
        if not iselement(e):
            fail("TypeError: expected an Element, not %s" % type(e))
    self._assert_is_element = _assert_is_element

    def remove(subelement):
        """Remove matching subelement.

        Unlike the find methods, this method compares elements based on
        identity, NOT ON tag value or contents.  To remove subelements by
        other means, the easiest way is to use a list comprehension to
        select what elements to keep, and then use slice assignment to update
        the parent element.

        ValueError is raised if a matching element could not be found.

        """
        # assert iselement(element)
        # fast path
        if subelement in self.getchildren():
            self._children.remove(subelement)
            return
        # else, iteratively search through all children to find the child
        # and remove it from the node
        children = [(self, x) for x in self.getchildren()]
        for _ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if not children:
                break
            parent, child = children.pop(0)
            if child == subelement:
                parent._children.remove(subelement)
                return
            children.extend([(child, c) for c in child.getchildren()])
        fail("ValueError: unable to remove %r because it was not found" % subelement)
    self.remove = remove

    def getchildren():
        """(Deprecated) Return all subelements.

        Elements are returned in document order.

        """
        return self._children
    self.getchildren = getchildren

    def getparent():
        """Return node parent

        XMLTreeNode if this XMLTreeNode has a parent, None if this is the root node
        """
        return self._parent
    self.getparent = getparent

    def find(path, namespaces=None):
        """Find first matching element by tag name or path.

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Return the first matching element, or None if no element was found.

        """
        if types.is_instance(path, QName):
            path = path.text
        return ElementPath.find(self, path, namespaces)
    self.find = find

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

    def findall(path, namespaces=None):
        """Find all matching subelements by tag name or path.

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Returns list containing all matching elements in document order.

        """
        return ElementPath.findall(self, path, namespaces)
    self.findall = findall

    def iterfind(path, namespaces=None):
        """Find all matching subelements by tag name or path.

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Return an iterable yielding all matching elements in document order.

        """
        return ElementPath.iterfind(self, path, namespaces)
    self.iterfind = iterfind

    def clear():
        """Reset element.

        This function removes all subelements, clears all attributes, and sets
        the text and tail attributes to None.

        """
        self.attrib.clear()
        self._children = []
        self._parent = None
        self._nsmap = {}
        self.text = None
        self.tail = None
    self.clear = clear

    def get(key, default=None):
        """Get element attribute.

        Equivalent to attrib.get, but some implementations may handle this a
        bit more efficiently.  *key* is what attribute to look for, and
        *default* is what to return if the attribute was not found.

        Returns a string containing the attribute value, or the default if
        attribute was not found.

        """
        return self.attrib.get(key, default)
    self.get = get

    def set(key, value):
        """Set element attribute.

        Equivalent to attrib[key] = value, but some implementations may handle
        this a bit more efficiently.  *key* is what attribute to set, and
        *value* is the attribute value to set it to.

        """
        self.attrib[key] = value
    self.set = set

    def keys():
        """Get list of attribute names.

        Names are returned in an arbitrary order, just like an ordinary
        Python dict.  Equivalent to attrib.keys()

        """
        return self.attrib.keys()
    self.keys = keys

    def items():
        """Get element attributes as a sequence.

        The attributes are returned in arbitrary order.  Equivalent to
        attrib.items().

        Return a list of (name, value) tuples.

        """
        return self.attrib.items()
    self.items = items

    # replace recursion with bfs
    def iter(tag=None):
        """Create tree iterator.

        The iterator loops over the element and all subelements in document
        order, returning all elements with a matching tag.

        If the tree structure is modified during iteration, new or removed
        elements may or may not be included.  To get a stable set, use the
        list() function on the iterator, and loop over the resulting list.

        *tag* is what tags to look for (default is to return all elements)

        Return an iterator containing all the matching elements.

        """

        # """
        # We're building an iterative traversal due to lack of recursion support
        # however, we're keeping the order of seen children to have a
        # deterministic depth first search.
        # """
        # if tag == "*":
        #     tag = None
        # if tag is None or self.tag == tag:
        #     yield self
        # for e in self._children:
        #     yield from e.iter(tag)
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

    # compatibility
    def getiterator(tag=None):
        # Change for a DeprecationWarning in 1.4
        return list(self.iter(tag))
    self.getiterator = getiterator

    def itertext():
        """Create text iterator.

        The iterator loops over the element and all subelements in document
        order, returning all inner text.

        """
        tag = self.tag
        if not types.is_string(tag) and tag != None:
            return
        if self.text:
            return self.text
        s = []
        for e in self._children:
            s.extend(e.itertext())
            if e.tail:
                s.append(e.tail)
        return s

    self.itertext = itertext
    return self


def SubElement(parent, tag, attrib=None, **extra):
    """Subelement factory which creates an element instance, and appends it
    to an existing parent.

    The element tag, attribute names, and attribute values can be either
    bytes or Unicode strings.

    *parent* is the parent element, *tag* is the subelements name, *attrib* is
    an optional directory containing element attributes, *extra* are
    additional attributes given as keyword arguments.

    """
    if not attrib:
        attrib = {}
    else:
        attrib = dict(**attrib)
    attrib.update(extra)
    element = parent.makeelement(tag, attrib)
    parent.append(element)
    return element


def Comment(text=None):
    """Comment element factory.

    This function creates a special element which the standard serializer
    serializes as an XML comment.

    *text* is a string containing the comment string.

    """
    element = Element(Comment)
    element.text = text
    return element


def ProcessingInstruction(target, text=None):
    """Processing Instruction element factory.

    This function creates a special element which the standard serializer
    serializes as an XML comment.

    *target* is a string containing the processing instruction, *text* is a
    string containing the processing instruction contents, if any.

    """
    element = Element(ProcessingInstruction)
    element.text = target
    if text:
        element.text = element.text + " " + text
    return element


def _getNsTag(tag):
    # Split the namespace URL out of a fully-qualified lxml tag
    # name. Copied from lxml's src/lxml/sax.py.
    if tag[0] == '{':
        return tuple(tag[1:].split('}', 1))
    else:
        return (None, tag)


PI = ProcessingInstruction


def QName(text_or_uri, tag=None):
    """Qualified name wrapper.

    This class can be used to wrap a QName attribute value in order to get
    proper namespace handing on output.

    *text_or_uri* is a string containing the QName value either in the form
    {uri}local, or if the tag argument is given, the URI part of a QName.

    *tag* is an optional argument which if given, will make the first
    argument (text_or_uri) be interpreted as a URI, and this argument (tag)
    be interpreted as a local name.

    """
    self = larky.mutablestruct(__name__='QName', __class__=QName)

    def __init__(text_or_uri, tag):
        if not types.is_string(text_or_uri):
            if iselement(text_or_uri):
                text_or_uri = text_or_uri.tag
                if not types.is_string(text_or_uri):
                    fail("Invalid input tag of type %r" % type(text_or_uri))
                elif types.is_instance(text_or_uri, QName):
                    text_or_uri = text_or_uri.text
                else:
                    text_or_uri = text_or_uri
        ns_utf, tag_utf = _getNsTag(text_or_uri)
        if tag:
            # either ('ns', 'tag') or ('{ns}oldtag', 'newtag')
            if ns_utf == None:
                ns_utf = tag_utf # case 1: namespace ended up as tag name
            tag_utf = tag

        self.localname = tag_utf
        if ns_utf == None:
            self.namespace = None
            self.text = self.localname
        else:
            self.namespace = ns_utf
            self.text = "{%s}%s" % (self.namespace, self.localname)
        return self
    self = __init__(text_or_uri, tag)

    def __str__():
        return self.text
    self.__str__ = __str__

    def __repr__():
        return "<QName %r>" % (self.text,)
    self.__repr__ = __repr__

    def __hash__():
        return hash(self.text)
    self.__hash__ = __hash__

    def __le__(other):
        if types.is_instance(other, QName):
            return self.text <= other.text
        return self.text <= other
    self.__le__ = __le__

    def __lt__(other):
        if types.is_instance(other, QName):
            return self.text < other.text
        return self.text < other
    self.__lt__ = __lt__

    def __ge__(other):
        if types.is_instance(other, QName):
            return self.text >= other.text
        return self.text >= other
    self.__ge__ = __ge__

    def __gt__(other):
        if types.is_instance(other, QName):
            return self.text > other.text
        return self.text > other
    self.__gt__ = __gt__

    def __eq__(other):
        if types.is_instance(other, QName):
            return self.text == other.text
        return self.text == other
    self.__eq__ = __eq__

    def __ne__(other):
        if types.is_instance(other, QName):
            return self.text != other.text
        return self.text != other
    self.__ne__ = __ne__
    return self


def CDATA(data):
    """CDATA(data)
    CDATA factory.  This factory creates an opaque data object that
    can be used to set Element text.  The usual way to use it is::
        >>> from xml.etree import ElementTree s etree
        >>> el = etree.Element('content')
        >>> el.text = etree.CDATA('a string')
    """
    element = Element(CDATA)
    if types.is_string(data):
        element.text = codecs.encode(data, encoding='utf-8')
    else:
        element.text = data
    return element


def iselement(element):
    """iselement(element)
    Checks if an object appears to be a valid element object or
    if *element* appears to be an Element.
    """
    return types.is_instance(element, Element) or hasattr(element, "tag")


def _ElementTree(element=None, file=None):
    """An XML element hierarchy.

    This class also provides support for serialization to and from
    standard XML.

    *element* is an optional root element node,
    *file* is an optional file handle or file name of an XML file whose
    contents will be used to initialize the tree with.

    """
    self = larky.mutablestruct(__class__='ElementTree')

    def parse(source, parser=None):
        """Load external XML document into element tree.

        *source* is a file name or file object, *parser* is an optional parser
        instance that defaults to XMLParser.

        ParseError is raised if the parser fails to parse the document.

        Returns the root element of the given source document.

        """
        """Load external XML document into element tree.

        *source* is a file name or file object, *parser* is an optional parser
        instance that defaults to XMLParser.

        ParseError is raised if the parser fails to parse the document.

        Returns the root element of the given source document.

        """
        close_source = False
        if not hasattr(source, "read"):
            fail('source = open(source, "rb")')
            close_source = True

        def _parse_safe(parser):
            if parser == None:
                # If no parser was specified, create a default XMLParser
                parser = XMLParser()
                if hasattr(parser, "_parse_whole"):
                    # The default XMLParser, when it comes from an accelerator,
                    # can define an internal _parse_whole API for efficiency.
                    # It can be used to parse the whole source without feeding
                    # it with chunks.
                    self._setroot(parser._parse_whole(source))
                    return self._root
            for _i in range(_WHILE_LOOP_EMULATION_ITERATION):
                data = source.read(65536)
                if not data:
                    break
                parser.feed(data)
            self._setroot(parser.close())
            return self.getroot()

        rval = Result.Ok(_parse_safe).map(lambda f: f(parser))
        if close_source:
            source.close()
        return rval.unwrap()

    self.parse = parse

    def __init__(element, file):
        # assert element is None or iselement(element)
        self._root = element  # first node
        if file:
            self.parse(file)
        return self
    self = __init__(element, file)

    def getroot():
        """Return root element of this tree."""
        return self._root
    self.getroot = getroot

    def _setroot(element):
        """Replace root element of this tree.

        This will discard the current contents of the tree and replace it
        with the given element.  Use with care!

        """
        # assert iselement(element)
        self._root = element
    self._setroot = _setroot

    def iter(tag=None):
        """Create and return tree iterator for the root element.

        The iterator loops over all elements in this tree, in document order.

        *tag* is a string with the tag name to iterate over
        (default is to return all elements).

        """
        # assert self._root is not None
        return self._root.iter(tag)
    self.iter = iter

    # compatibility
    def getiterator(tag=None):
        # Change for a DeprecationWarning in 1.4
        return list(self.iter(tag))
    self.getiterator = getiterator

    def find(path, namespaces=None):
        """Find first matching element by tag name or path.

        Same as getroot().find(path), which is Element.find()

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Return the first matching element, or None if no element was found.

        """
        # assert self._root is not None
        if path[:1] == "/":
            path = "." + path
        return self._root.find(path, namespaces)
    self.find = find

    def findtext(path, default=None, namespaces=None):
        """Find first matching element by tag name or path.

        Same as getroot().findtext(path),  which is Element.findtext()

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Return the first matching element, or None if no element was found.

        """
        # assert self._root is not None
        if path[:1] == "/":
            path = "." + path
        return self._root.findtext(path, default, namespaces)
    self.findtext = findtext

    def findall(path, namespaces=None):
        """Find all matching subelements by tag name or path.

        Same as getroot().findall(path), which is Element.findall().

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Return list containing all matching elements in document order.

        """
        # assert self._root is not None
        if path[:1] == "/":
            path = "." + path
        return self._root.findall(path, namespaces)
    self.findall = findall

    def iterfind(path, namespaces=None):
        """Find all matching subelements by tag name or path.

        Same as getroot().iterfind(path), which is element.iterfind()

        *path* is a string having either an element tag or an XPath,
        *namespaces* is an optional mapping from namespace prefix to full name.

        Return an iterable yielding all matching elements in document order.

        """
        # assert self._root is not None
        if path[:1] == "/":
            path = "." + path
        return self._root.iterfind(path, namespaces)
    self.iterfind = iterfind

    def write(
        file_or_filename,
        encoding=None,
        xml_declaration=None,
        default_namespace=None,
        method=None,
        *,
        short_empty_elements=True
    ):
        """Write element tree to a file as XML.

        Arguments:
          *file_or_filename* -- file name or a file object opened for writing

          *encoding* -- the output encoding (default: US-ASCII)

          *xml_declaration* -- bool indicating if an XML declaration should be
                               added to the output. If None, an XML declaration
                               is added if encoding IS NOT either of:
                               US-ASCII, UTF-8, or Unicode

          *default_namespace* -- sets the default XML namespace (for "xmlns")

          *method* -- either "xml" (default), "html, "text", or "c14n"

          *short_empty_elements* -- controls the formatting of elements
                                    that contain no content. If True (default)
                                    they are emitted as a single self-closed
                                    tag, otherwise they are emitted as a pair
                                    of start/end tags

        """
        fail('Error("Unsupported write method")')
    self.write = write

    # for internal use by tostring() only
    def _write(
        stream,
        encoding=None,
        xml_declaration=None,
        default_namespace=None,
        method=None,
        short_empty_elements=True,
    ):
        if not encoding:
            if method == "c14n":
                encoding = "utf-8"
            else:
                encoding = "us-ascii"
        # print('xml_declaration:', xml_declaration)
        if xml_declaration or (xml_declaration == None and encoding not in ("utf-8", "us-ascii")):
            stream.write('<?xml version="1.0" encoding="%s"?>\n' % encoding)

        qnames, namespaces = _namespaces(self._root, default_namespace)
        # print("qnames:", qnames)
        # print("namespaces:", namespaces)
        _serialize_xml(
            stream.write,
            self._root,
            qnames,
            namespaces,
            short_empty_elements=short_empty_elements,
        )
    self._write = _write


    def write_c14n(file):
        # lxml.etree compatibility.  use output method instead
        return self.write(file, method="c14n")
    self.write_c14n = write_c14n
    return self


# --------------------------------------------------------------------
# serialization support


def _get_writer(file_or_filename, encoding):
    # returns text write method and release all resources after using
    def _get_writer_try():
        write = file_or_filename.write

    def _get_writer_except_0():
        # file_or_filename is a file name
        file = StringIO()
        return file.write
        # file = open(
        #     file_or_filename,
        #     "w",
        #     encoding=encoding,
        #     errors="xmlcharrefreplace",
        # )


    def _get_writer_else(rval):
        # file_or_filename is a file-like object
        # encoding determines if it is a text or binary writer
        if encoding == "unicode":
            # use a text writer as is
            return rval
        file = StringIO()
        return file.write

    return try_(_get_writer_try)\
        .except_(_get_writer_except_0)\
        .else_(_get_writer_else)\
        .build()


def add_qname(qname, qnames, namespaces, prefix_finder, default_namespace=None):
    # calculate serialized qname representation
    def _namespaces_add_qname_try():
        if qname[:1] == "{":
            uri, tag = qname[1:].rsplit("}", 1)
            prefix = prefix_finder(uri)
            namespaces[uri] = prefix
            # print("prefix %r found for %s for this tag %s" % (prefix, uri, tag))
            if prefix:
                qnames[qname] = "%s:%s" % (prefix, tag)
            else:
                qnames[qname] = tag  # default element
        else:
            if default_namespace:
                # FIXME: can this be handled in XML 1.0?
                fail("can this be handled in XML 1.0?")
            qnames[qname] = qname

    def _namespaces_add_qname_error_01(val):
        fail(
            "TypeError: cannot serialize qname=%r (type %s), msg: %s" %
            (qname, type(qname), val)
        )

    return try_(_namespaces_add_qname_try)\
            .except_(_namespaces_add_qname_error_01)\
            .build()


def _namespaces(elem, default_namespace=None):
    # identify namespaces used in this tree

    # maps qnames to *encoded* prefix:local names
    qnames = {None: None}
    # maps uri:s to prefixes
    namespaces, new_nspaces = _collect_namespaces(
        getattr(elem, '_nsmap', {}),
        elem.getparent()
    )
    if default_namespace:
        namespaces[default_namespace] = ""
        new_nspaces.append(('', 'xmlns', default_namespace))
    prefix_finder = larky.partial(_find_prefix,
                                  flat_namespaces_map=namespaces,
                                  new_namespaces=new_nspaces)
    # populate qname and namespaces table
    qu = [elem]
    for _ in range(_WHILE_LOOP_EMULATION_ITERATION):
        if len(qu) == 0:
            break
        current = qu.pop(0)
        tag = current.tag
        _qname = None
        if types.is_instance(tag, QName):
            if tag.text not in qnames:
                _qname = tag.text
        elif types.is_instance(tag, str):
            if tag not in qnames:
                _qname = tag
        elif tag != None and tag != Comment and tag != PI:
            if not hasattr(current, 'nodetype'):
                _raise_serialization_error(tag)

        _nsmap = getattr(current, '_nsmap', {})
        # print("ns: ", namespaces)
        # print("elem._nsmap: ", _nsmap)
        # if _nsmap:
        #     # print(_nsmap)
        #     for _href, _prefix in _nsmap.items():
        #         namespaces[_href] = _prefix
        #         if _prefix == None:
        #             # use empty bytes rather than None to allow sorting
        #             _entry = ('', 'xmlns', _href)
        #         else:
        #             _entry = ('xmlns', _prefix, _href)
        #         if _entry not in new_nspaces:
        #             new_nspaces.append(_entry)
        # flat_ns_map, new_nspaces = _collect_namespaces(_nsmap, None)
        # if _nsmap:
        # print("namespaces:", namespaces, new_nspaces)
        # prefix_finder = larky.partial(_find_prefix,
        #                               flat_namespaces_map=namespaces,
        #                               new_namespaces=new_nspaces)
        if _qname != None:
            add_qname(_qname, qnames, namespaces,
                      prefix_finder,
                      default_namespace=default_namespace)

        for key, value in current.items():
            if types.is_instance(key, QName):
                key = key.text
            if key not in qnames:
                add_qname(key, qnames, namespaces,
                          prefix_finder,
                          default_namespace=default_namespace)
            if types.is_instance(value, QName) and value.text not in qnames:
                add_qname(_qname, qnames, namespaces,
                          prefix_finder,
                          default_namespace=default_namespace)

        text = current.text
        if types.is_instance(text, QName) and text.text not in qnames:
            add_qname(_qname, qnames, namespaces,
                      prefix_finder,
                      default_namespace=default_namespace)

        qu.extend(current._children)

    return qnames, namespaces


def _collect_namespaces2(nsmap, node):
    new_namespaces = []
    flat_namespaces_map = {}
    for ns_href_url, prefix in nsmap.items():
        flat_namespaces_map[ns_href_url] = prefix
        if prefix == None:
            # use empty bytes rather than None to allow sorting
            new_namespaces.append(('', 'xmlns', ns_href_url))
        else:
            new_namespaces.append(('xmlns', prefix, ns_href_url))
    # merge in flat namespace map of parent
    # print(new_namespaces, flat_namespaces_map)
    if node:
        parent = node.getparent()
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if parent == None:
                break
            for ns_href_url, prefix in parent.items():
                if flat_namespaces_map.get(ns_href_url) == None:
                    # unknown or empty prefix => prefer a 'real' prefix
                    flat_namespaces_map[ns_href_url] = prefix
            lastnode = parent
            parent = lastnode.getparent()
        # for elem in flatten_nested_elements(parent):
        #     print(elem.tag, getattr(elem, '_nsmap'))
    return flat_namespaces_map, new_namespaces


def _collect_namespaces(nsmap, parent):
    new_namespaces = []
    flat_namespaces_map = {}
    for ns_href_url, prefix in nsmap.items():
        flat_namespaces_map[ns_href_url] = prefix
        if prefix == None:
            # use empty bytes rather than None to allow sorting
            new_namespaces.append(('', 'xmlns', ns_href_url))
        else:
            new_namespaces.append(('xmlns', prefix, ns_href_url))
    # merge in flat namespace map of parent
    if parent:
        for ns_href_url, prefix in parent.items():
            if flat_namespaces_map.get(ns_href_url) == None:
                # unknown or empty prefix => prefer a 'real' prefix
                flat_namespaces_map[ns_href_url] = prefix
    # print("flat: ", flat_namespaces_map)
    # print("new-ns: ", new_namespaces)
    return flat_namespaces_map, new_namespaces


def _find_prefix(href, flat_namespaces_map, new_namespaces):
    if href == None:
        # print("href is none, no prefix: ", href)
        return None
    if href in flat_namespaces_map:
        # print("prefix found in flat_namespaces_map: ", href)
        return flat_namespaces_map[href]
    # need to create a new prefix
    prefixes = flat_namespaces_map.values()
    for i in range(_WHILE_LOOP_EMULATION_ITERATION):
        prefix = 'ns%d' % i
        if prefix not in prefixes:
            new_namespaces.append(('xmlns', prefix, href))
            flat_namespaces_map[href] = prefix
            # print("prefix generated: ", prefix, "for ", href)
            return prefix


def flatten_nested_elements(tops_level_elems):
    """
    We're building an iterative traversal due to lack of recursion support
    however, we're keeping the order of seen children to have a
    deterministic depth first search.
    """
    new_elems = []
    for el in tops_level_elems:
        new_elems.append(el)
        qu = el._children[0:]
        for _ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if not qu:
                break
            else:
                current = qu.pop(0)
                new_elems.append(current)
                qu = current._children + qu
    return new_elems


def _serialize_xml(
    write, start_elem, qnames, namespaces, short_empty_elements, **_kwargs
):
    elems = flatten_nested_elements([start_elem])
    unclosed_elems = []
    for elem in elems:
        tag = elem.tag
        # print('iter elem:', tag)
        text = elem.text
        # attrib = elem.attrib
        for _ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if len(unclosed_elems) == 0:
                break
            if elem in unclosed_elems[-1]._children:
                break
            elem_to_close = unclosed_elems.pop()
            _tag = elem_to_close.tag
            if types.is_instance(_tag, QName):
                _tag = _tag.text
            _tag = qnames.get(_tag, _tag)  # support QNames in XML Element and Attribute Names
            # print("_serialize_xml", "pre-body", "_tag?", _tag, "_repr(text)", repr(elem_to_close.text), "_tail?", repr(elem_to_close.tail))
            write("</%s>" % _tag)
            if elem_to_close.tail:
                # print("_serialize_xml", "elem to close which has tail:", elem_to_close.tag)
                write(_escape_cdata(elem_to_close.tail))

        if tag == Comment:
            write("<!--%s-->" % _escape_cdata(text))
        elif tag == ProcessingInstruction:
            write("<?%s?>" % _escape_cdata(text))
        else:
            if types.is_instance(tag, QName):
                tag = tag.text
            tag = qnames[tag]  # support QNames in XML Element and Attribute Names
            if tag == None:
                if text:
                    write(_escape_cdata(text))
                if len(elem._children) == 0:
                    write("</%s>" % tag)
                else:
                    unclosed_elems.append(elem)
                    # print('add unclosed elem:', elem)
            else:
                # this is the start element
                write("<" + tag)
                items = elem.items()  # get attrib
                if items or namespaces:
                    if namespaces:
                        for v, k in sorted(namespaces.items(), key=lambda x: x[1]):  # sort on prefix
                            if k:
                                k = ":" + k
                            write(" xmlns%s=\"%s\"" % (k,_escape_attrib(v)))
                        namespaces = None  # each xml tree only has one dict of namespaces
                    for k, v in items:
                        # print("=====>", elem, "k, v:", k, v)
                        if types.is_instance(k, QName):
                            k = k.text
                        if types.is_instance(v, QName):
                            v = qnames[v.text]
                        if not types.is_string(v) and hasattr(v, 'href'):
                            v = v.href
                        else:
                            v = _escape_attrib(v)
                        write(' %s="%s"' % (qnames[k], v))
                if any((
                        (text and text.strip()),
                        len(elem._children),
                        not short_empty_elements,
                )):
                    write(">")
                    if text:
                        write(_escape_cdata(text))
                    if len(elem._children) == 0:
                        write("</%s>" % tag)
                        if elem.tail:
                            # print('no child elem which has tail:', elem.tag)
                            write(_escape_cdata(elem.tail))
                    else:
                        unclosed_elems.append(elem)
                        # print('add unclosed elem:', elem)
                else:
                    write(" />")
                    if elem.tail:
                        # print('self closing elem which has tail:', elem.tag)
                        write(_escape_cdata(elem.tail))
    for _ in range(_WHILE_LOOP_EMULATION_ITERATION):
        if len(unclosed_elems) == 0:
            break
        elem_to_close = unclosed_elems.pop()
        tag = elem_to_close.tag
        if types.is_instance(tag, QName):
            tag = tag.text
        tag = qnames.get(tag, tag)  # support QNames in XML Element and Attribute Names
        # print("_serialize_xml", "post-body", "_tag?", tag, "_repr(text)", repr(elem_to_close.text), "_tail?", repr(elem_to_close.tail))
        write("</%s>" % tag)
        if elem_to_close.tail:
            # print('remaining elem to close which has tail:', elem_to_close.tag)
            write(_escape_cdata(elem_to_close.tail))

HTML_EMPTY = (
    "area",
    "base",
    "basefont",
    "br",
    "col",
    "frame",
    "hr",
    "img",
    "input",
    "isindex",
    "link",
    "meta",
    "param",
)

HTML_EMPTY = sets.Set(HTML_EMPTY)


def _serialize_html(write, elem, qnames, namespaces, **kwargs):
    elems = [(elem, namespaces)]

    for i in range(_WHILE_LOOP_EMULATION_ITERATION):
        if not elems:
            break
        elem, namespaces = elems.pop()

        tag = elem.tag
        text = elem.text
        if tag == Comment:
            write("<!--%s-->" % _escape_cdata(text))
        elif tag == ProcessingInstruction:
            write("<?%s?>" % _escape_cdata(text))
        else:
            tag = qnames[tag]
            if tag == None:
                if text:
                    write(_escape_cdata(text))
                for e in elem:
                    # _serialize_html(write, e, qnames, None)
                    elems.append((e, None))
            else:
                write("<" + tag)
                items = list(elem.items())
                if items or namespaces:
                    if namespaces:
                        for v, k in sorted(
                            namespaces.items(), key=lambda x: x[1]
                        ):  # sort on prefix
                            if k:
                                k = ":" + k
                            write(' xmlns%s="%s"' % (k, _escape_attrib(v)))
                    for k, v in sorted(items):  # lexical order
                        if types.is_instance(k, QName):
                            k = k.text
                        if types.is_instance(v, QName):
                            v = qnames[v.text]
                        else:
                            v = _escape_attrib_html(v)
                        # FIXME: handle boolean attributes
                        write(' %s="%s"' % (qnames[k], v))
                write(">")
                ltag = tag.lower()
                if text:
                    if ltag == "script" or ltag == "style":
                        write(text)
                    else:
                        write(_escape_cdata(text))
                for e in elem:
                    # _serialize_html(write, e, qnames, None)
                    elems.append((e, None))
                if not HTML_EMPTY.contains(ltag):
                    write("</" + tag + ">")
        if elem.tail:
            write(_escape_cdata(elem.tail))


def _serialize_text(write, elem):
    for part in elem.itertext():
        write(part)
    if elem.tail:
        write(elem.tail)


_serialize = {
    "xml": _serialize_xml,
    "html": _serialize_html,
    "text": _serialize_text,
    # this optional method is imported at the end of the module
    #   "c14n": _serialize_c14n,
}


_namespace_map = _JXML._namespace_map()


def register_namespace(prefix, uri):
    """Register a namespace prefix.

    The registry is global, and any existing mapping for either the
    given prefix or the namespace URI will be removed.

    *prefix* is the namespace prefix, *uri* is a namespace uri. Tags and
    attributes in this namespace will be serialized with prefix if possible.

    ValueError is raised if prefix is reserved or is invalid.

    """
    # if re.match(r"ns\d+$", prefix):
    #     return Error("ValueError: Prefix format reserved for internal use")
    _namespace_map.register_namespace(prefix, uri)


def _raise_serialization_error(text):
    fail(
        "TypeError: cannot serialize %r (type %s)" % (text, type(text))
    )

def _escape_cdata(text):
    # escape character data
    # it's worth avoiding do-nothing calls for strings that are
    # shorter than 500 character, or so.  assume that's, by far,
    # the most common case in most applications.
    if types.is_bytelike(text):
        text = text.decode('utf-8').strip()

    if types.is_string(text):
        if "&" in text:
            text = text.replace("&", "&amp;")
        if "<" in text:
            text = text.replace("<", "&lt;")
        if ">" in text:
            text = text.replace(">", "&gt;")
    else:
        _raise_serialization_error(text)
    return text


def _escape_attrib(text):
    # escape attribute value
    if types.is_string(text):
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
    else:
        _raise_serialization_error(text)
    return text


def _escape_attrib_html(text):
    # escape attribute value
    def _escape_attrib_html_try():
        if "&" in text:
            text = text.replace("&", "&amp;")
        if ">" in text:
            text = text.replace(">", "&gt;")
        if '"' in text:
            text = text.replace('"', "&quot;")
        return text

    return try_(_escape_attrib_html_try)\
            .except_(lambda x: _raise_serialization_error(text))\
            .build()\
            .unwrap()

# --------------------------------------------------------------------

# Support convertion to xml only for now
def tostring(element,
             encoding="us-ascii",
             method="xml",
             xml_declaration=None,
             default_namespace=None,
             short_empty_elements=True,
             pretty_print=False,
             with_comments=False,
             exclusive=False):
    """Generate string representation of XML element.

    All subelements are included.  If encoding is "unicode", a string
    is returned. Otherwise a bytestring is returned.

    *element* is an Element instance, *encoding* is an optional output
    encoding defaulting to US-ASCII, *method* is an optional output which can
    be one of "xml" (default), "html", "text" or "c14n".

    Returns an (optionally) encoded string containing the XML data.

    """
    if encoding in ["us-ascii", "unicode", "utf-8"]:
        stream = io.StringIO()
    else:
        stream = io.BytesIO()

    if type(element) != 'ElementTree':
        element = _ElementTree(element)

    element._write(
        stream,
        encoding=encoding,
        xml_declaration=xml_declaration,
        default_namespace=default_namespace,
        short_empty_elements=short_empty_elements,
    )
    return stream.getvalue()


def _ListDataStream(lst):
    """An auxiliary stream accumulating into a list reference."""
    self = larky.mutablestruct(__class__=_ListDataStream, __name__='ListDataStream')

    def __init__(lst):
        self.lst = lst
        return self
    self = __init__(lst)

    def writable():
        return True
    self.writable = writable

    def seekable():
        return True
    self.seekable = seekable

    def write(b):
        self.lst.append(b)
    self.write = write

    def tell():
        return len(self.lst)
    self.tell = tell
    return self


def tostringlist(
    element, encoding=None, method=None, *, short_empty_elements=True
):
    lst = []
    stream = _ListDataStream(lst)
    _ElementTree(element).write(
        stream,
        encoding,
        method=method,
        short_empty_elements=short_empty_elements,
    )
    return lst


def dump(elem):
    """Write element tree or element structure to sys.stdout.

    This function should be used for debugging only.

    *elem* is either an ElementTree, or a single Element.  The exact output
    format is implementation dependent.  In this version, it's written as an
    ordinary XML file.

    """
    # debugging
    if not types.is_instance(elem, _ElementTree):
        elem = _ElementTree(elem)
    f = StringIO()
    elem.write(f, encoding="unicode")
    print(f)
    tail = elem.getroot().tail
    if not tail or tail[-1] != "\n":
        print("\n")


# Note, the indent() function was added in Python 3.9.
# https://github.com/python/cpython/pull/15200/files
def indent(tree, space="  ", level=0):
    """Indent an XML document by inserting newlines and indentation space
    after elements.
    *tree* is the ElementTree or Element to modify.  The (root) element
    itself will not be changed, but the tail text of all elements in its
    subtree will be adapted.
    *space* is the whitespace to insert for each indentation level, two
    space characters by default.
    *level* is the initial indentation level. Setting this to a higher
    value than 0 can be used for indenting subtrees that are more deeply
    nested inside of a document.
    """
    if hasattr(tree, 'getroot') or type(tree) == 'ElementTree':
        tree = tree.getroot()
    if level < 0:
        fail("ValueError: Initial indentation level must be >= 0, got %s" % level)
    if not len(tree):
        return

    # Reduce the memory consumption by reusing indentation strings.
    indentations = ["\n" + level * space]

    # No recursion support in Larky means we have to iteratively indent.

    def _indent_children(root, lvl):
        element = root
        queue = [(lvl, element)]  # (level, element)
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if not queue:
                break
            level, element = queue.pop(0)
            child_level = level + 1
            children = [(child_level, child) for child in element]

            if len(indentations) > child_level:
                child_indentation = indentations[child_level]
            else:
                child_indentation = indentations[level] + space
                indentations.append(child_indentation)

            if children:
                # only if children exist and there's no text, then we should
                # give it indentation!
                if not element.text or not element.text.strip():
                    element.text = child_indentation

                if element != root:
                    element.tail = indentations[child_level]  # for child open

            if queue and (not element.tail or not element.tail.strip()):
                element.tail = indentations[queue[0][0]]  # for sibling open
            else:
                if element != root and (not element.tail or not element.tail.strip()):
                    element.tail = indentations[level - 1] # for parent close

            # queue[0:0] = children
            for c in reversed(children):
                queue.insert(0, c)  # prepend so children come before siblings

    _indent_children(tree, 0)

# --------------------------------------------------------------------
# parsing


def parse(source, parser=None, tree_factory=_ElementTree):
    """Parse XML document into element tree.

    *source* is a filename or file object containing XML data,
    *parser* is an optional parser instance defaulting to XMLParser.

    Return an ElementTree instance.

    """
    tree = tree_factory()
    tree.parse(source, parser)
    return tree


def iterparse(source, events=None, parser=None):
    """Incrementally parse XML document into ElementTree.

    This class also reports what's going on to the user based on the
    *events* it is initialized with.  The supported events are the strings
    "start", "end", "start-ns" and "end-ns" (the "ns" events are used to get
    detailed namespace information).  If *events* is omitted, only
    "end" events are reported.

    *source* is a filename or file object containing XML data, *events* is
    a list of events to report back, *parser* is an optional parser instance.

    Returns an iterator providing (event, elem) pairs.

    """
    if not hasattr(source, "read"):
        fail("TypeError: source (in Larky) must have a read function")
    tree = parse(source, parser)
    event_queue = list(parser.read_events())
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


def XMLPullParser(events=None, _parser=None):
    self = larky.mutablestruct(__class__='XMLPullParser')
    def __init__(events, _parser):
        # The _parser argument is for internal use only and must not be relied
        # upon in user code. It will be removed in a future release.
        # See http://bugs.python.org/issue17741 for more details.

        # _elementtree.c expects a list, not a deque
        self._events_queue = []
        self._index = 0
        self._parser = _parser or XMLParser(target=TreeBuilder())
        # wire up the parser for event reporting
        if events == None:
            events = tuple(("end",))
        self._parser._setevents(self._events_queue, events)
        return self
    self = __init__(events, _parser)

    def feed(data):
        """Feed encoded data to parser."""
        if self._parser == None:
            return Error("ValueError: feed() called after end of stream")
        if not data:
            return
        rval = Result.Ok(self._parser.feed).map(lambda f: f(data))
        if rval.is_err:
            self._events_queue.append(rval)
    self.feed = feed

    def _close_and_return_root():
        # iterparse needs this to set its root attribute properly :(
        root = self._parser.close()
        self._parser = None
        return root
    self._close_and_return_root = _close_and_return_root

    def close():
        """Finish feeding data to parser.

        Unlike XMLParser, does not return the root element. Use
        read_events() to consume elements from XMLPullParser.
        """
        self._close_and_return_root()
    self.close = close

    def read_events():
        """Return an iterator over currently available (event, elem) pairs.

        Events are consumed from the internal event queue as they are
        retrieved from the iterator.
        """
        events = self._events_queue
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if len(events) == 0:
                break
            index = self._index
            if self._index not in events:
                break
            event = events[self._index]
            # Avoid retaining references to past events
            events[self._index] = None
            index += 1
            # Compact the list in a O(1) amortized fashion
            # As noted above, _elementree.c needs a list, not a deque
            if index * 2 >= len(events):
                for i in range(index):
                    events.pop(0)
                self._index = 0
            else:
                self._index = index
            if hasattr(event, 'unwrap'):
                return event.unwrap()
            return event
    self.read_events = read_events
    return self



def XML(text, parser=None):
    """Parse XML document from string constant.

    This function can be used to embed "XML Literals" in Python code.

    *text* is a string containing XML data, *parser* is an
    optional parser instance, defaulting to the standard XMLParser.

    Returns an Element instance.

    """
    if not parser:
        parser = SimpleXMLTreeBuilderHelper.TreeBuilderHelper(
            TreeBuilder,
            element_factory=None,
            parser=xmllib.XMLParser()
        )
        # parser = XMLParser(target=TreeBuilder())
    parser.feed(text)
    return parser.close()


def XMLID(text, parser=None):
    """Parse XML document from string constant for its IDs.

    *text* is a string containing XML data, *parser* is an
    optional parser instance, defaulting to the standard XMLParser.

    Returns an (Element, dict) tuple, in which the
    dict maps element id:s to elements.

    """
    if not parser:
        parser = XMLParser(target=TreeBuilder())
    parser.feed(text)
    tree = parser.close()
    ids = {}
    for elem in tree.iter():
        id = elem.get("id")
        if id:
            ids[id] = elem
    return tree, ids


# Parse XML document from string constant.  Alias for XML().
fromstring = XML


def fromstringlist(sequence, parser=None):
    """Parse XML document from sequence of string fragments.

    *sequence* is a list of other sequence, *parser* is an optional parser
    instance, defaulting to the standard XMLParser.

    Returns an Element instance.

    """
    if not parser:
        parser = XMLParser(target=TreeBuilder())
    for text in sequence:
        parser.feed(text)
    return parser.close()


def TreeBuilder(
    element_factory=None,
    comment_factory=None,
    pi_factory=None,
    insert_comments=False,
    insert_pis=False,
    **options
):
    """Generic element structure builder.

    This builder converts a sequence of start, data, and end method
    calls to a well-formed element structure.

    You can use this class to build an element structure using a custom XML
    parser, or a parser for some other XML-like format.

    *element_factory* is an optional element factory which is called
    to create new Element instances, as necessary.

    *comment_factory* is a factory to create comments to be used instead of
    the standard factory.  If *insert_comments* is false (the default),
    comments will not be inserted into the tree.

    *pi_factory* is a factory to create processing instructions to be used
    instead of the standard factory.  If *insert_pis* is false (the default),
    processing instructions will not be inserted into the tree.

    """
    self = larky.mutablestruct(__name__='TreeBuilder', __class__=TreeBuilder)

    def __init__(
        element_factory,
        comment_factory,
        pi_factory,
        insert_comments,
        insert_pis,
        **options
    ):
        self._data = []  # data collector
        self._elem = []  # element stack
        self._last = None  # last element
        self._root = None  # root element
        self._tail = None  # true if we're after an end tag
        if comment_factory == None:
            comment_factory = Comment
        self._comment_factory = comment_factory
        self.insert_comments = insert_comments
        if pi_factory == None:
            pi_factory = ProcessingInstruction
        self._pi_factory = pi_factory
        self.insert_pis = insert_pis
        if element_factory == None:
            element_factory = Element
        self._factory = element_factory
        return self
    self = __init__(element_factory, comment_factory, pi_factory, insert_comments, insert_pis, **options)

    def close():
        """Flush builder buffers and return toplevel document Element."""
        if not (len(self._elem) == 0):
            fail("missing end tags")
        if not (self._root != None):
            fail("missing toplevel element")
        return self._root
    self.close = close

    def _flush():
        if not self._data:
            return
        if self._last == None:
            self._data = []
            return

        text = "".join(self._data)
        if self._tail:
            if not (self._last.tail == None):
                fail("internal error (tail)")
            self._last.tail = text
        else:
            if not (self._last.text == None):
                fail("internal error (text)")
            self._last.text = text
        self._data = []
    self._flush = _flush

    def data(data):
        """Add text to current element."""
        self._data.append(data)
    self.data = data

    def start(tag, attrs):
        """Open new element and return it.

        *tag* is the element name, *attrs* is a dict containing element
        attributes.

        """
        self._flush()
        elem = self._factory(tag, attrs)
        self._last = elem
        if self._elem:
            self._elem[-1].append(elem)
        elif self._root == None:
            self._root = elem
        self._elem.append(elem)
        self._tail = 0
        return elem

    self.start = start

    def end(tag):
        """Close and return current Element.

        *tag* is the element name.

        """
        self._flush()
        self._last = self._elem.pop()
        if self._last.tag != tag:
            fail(
                "end tag mismatch (expected %s, got %s)"
                % (
                    self._last.tag,
                    tag,
                )
            )
        self._tail = 1
        return self._last

    self.end = end

    def comment(text):
        """Create a comment using the comment_factory.

        *text* is the text of the comment.
        """
        return self._handle_single(self._comment_factory, self.insert_comments, text)

    self.comment = comment

    def pi(target, text=None):
        """Create a processing instruction using the pi_factory.

        *target* is the target name of the processing instruction.

        *text* is the data of the processing instruction, or ''.
        """
        return self._handle_single(self._pi_factory, self.insert_pis, target, text)

    self.pi = pi

    def _handle_single(factory, insert, *args):
        elem = factory(*args)
        if insert:
            self._flush()
            self._last = elem
            if self._elem:
                self._elem[-1].append(elem)
            self._tail = 1
        return elem

    self._handle_single = _handle_single
    return self



def XMLParser(html=0, target=None, encoding=None):
    """Element structure builder for XML source data based on the expat parser.

    *html* are predefined HTML entities (not supported currently),
    *target* is an optional target object which defaults to an instance of the
    standard TreeBuilder class, *encoding* is an optional encoding string
    which if given, overrides the encoding specified in the XML file:
    http://www.iana.org/assignments/character-sets

    """
    self = larky.mutablestruct(__class__='XMLParser')

    def _setevents(events_queue, events_to_report):
        # Internal API for XMLPullParser
        # events_to_report: a list of events to report during parsing (same as
        # the *events* of XMLPullParser's constructor.
        # events_queue: a list of actual parsing events that will be populated
        # by the underlying parser.
        #
        parser = self._parser
        append = events_queue.append
        for event_name in events_to_report:
            if event_name == "start":
                parser.ordered_attributes = 1
                parser.specified_attributes = 1

                def handler(
                    tag,
                    attrib_in,
                    event=event_name,
                    append=append,
                    start=self._start,
                ):
                    append((event, start(tag, attrib_in)))
                self.handler = handler

                parser.StartElementHandler = handler
            elif event_name == "end":

                def handler(
                    tag, event=event_name, append=append, end=self._end
                ):
                    append((event, end(tag)))
                self.handler = handler

                parser.EndElementHandler = handler
            elif event_name == "start-ns":

                def handler(prefix, uri, event=event_name, append=append):
                    append((event, (prefix or "", uri or "")))
                self.handler = handler

                parser.StartNamespaceDeclHandler = handler
            elif event_name == "end-ns":

                def handler(prefix, event=event_name, append=append):
                    append((event, None))
                self.handler = handler

                parser.EndNamespaceDeclHandler = handler
            else:
                return Error("ValueError: unknown event %r" % event_name)
    self._setevents = _setevents

    def _raiseerror(value):
        return ParseError(value, value.code, (value.lineno, value.offset))
    self._raiseerror = _raiseerror

    def _fixname(key):
        # expand qname, and convert name string to ascii, if possible
        if key in self._names:
            return self._names[key]

        name = key
        if "}" in name:
            name = "{" + name
        self._names[key]= name
        return name
    self._fixname = _fixname

    def _start(tag, attr_list):
        # Handler for expat's StartElementHandler. Since ordered_attributes
        # is set, the attributes are reported as a list of alternating
        # attribute name,value.
        fixname = self._fixname
        tag = fixname(tag)
        attrib = {}
        if attr_list:
            for i in range(0, len(attr_list), 2):
                attrib[fixname(attr_list[i])] = attr_list[i + 1]
        return self.target.start(tag, attrib)
    self._start = _start

    def _end(tag):
        return self.target.end(self._fixname(tag))
    self._end = _end

    def _default(text):
        prefix = text[:1]
        if prefix == "&":
            # deal with undefined entities
            if not hasattr(self.target, "data"):
                return

            data_handler = self.target.data
            if text[1:-1] not in self.entity:
                return ParseError(
                    code=11,  # XML_ERROR_UNDEFINED_ENTITY
                    position=(
                        self.parser.ErrorLineNumber,
                        self.parser.ErrorColumnNumber
                    ),
                    msg="undefined entity %s: line %d, column %d" % (
                        text,
                        self.parser.ErrorLineNumber,
                        self.parser.ErrorColumnNumber,
                ))
            data_handler(self.entity[text[1:-1]])
        elif prefix == "<" and text[:9] == "<!DOCTYPE":
            self._doctype = []  # inside a doctype declaration
        elif self._doctype != None:
            # parse doctype contents
            if prefix == ">":
                self._doctype = None
                return
            text = text.strip()
            if not text:
                return
            self._doctype.append(text)
            n = len(self._doctype)
            if n > 2:
                type = self._doctype[1]
                if type == "PUBLIC" and n == 4:
                    name, type, pubid, system = self._doctype
                    if pubid:
                        pubid = pubid[1:-1]
                elif type == "SYSTEM" and n == 3:
                    name, type, system = self._doctype
                    pubid = None
                else:
                    return
                if hasattr(self.target, "doctype"):
                    self.target.doctype(name, pubid, system[1:-1])
                elif self.doctype != self._XMLParser__doctype:
                    # warn about deprecated call
                    self._XMLParser__doctype(name, pubid, system[1:-1])
                    self.doctype(name, pubid, system[1:-1])
                self._doctype = None
    self._default = _default

    def doctype(name, pubid, system):
        """(Deprecated)  Handle doctype declaration

        *name* is the Doctype name, *pubid* is the public identifier,
        and *system* is the system identifier.

        """
        pass
    self.doctype = doctype

    # sentinel, if doctype is redefined in a subclass
    __doctype = doctype

    def feed(data):
        """Feed encoded data to parser."""
        rval = Result.Ok(self.parser.Parse).map(lambda f: f(data, 0))
        if rval.is_err:
            return self._raiseerror(rval.unwrap_err("XMLParser.feed() "))
    self.feed = feed

    def close():
        """Finish feeding data to parser and return element structure."""
        rval = Result.Ok(self.parser.Parse).map(lambda f: f("", 1))  # end of data
        if rval.is_err:
            return self._raiseerror(rval.unwrap_err("XMLParser.close() "))

        def _XMLParser_close_finally(x):
            self.parser = None
            self._parser = None
            self.target = None
            self._target = None

        rval = try_(lambda: self.target.close)\
        .except_(lambda x: x)\
        .else_(lambda x: x())\
        .finally_(_XMLParser_close_finally)\
        .build()

        return rval.unwrap()
    self.close = close

    def __init__(html, target, encoding):
        parser = larky.mutablestruct(__class__='XMLParser.parser', encoding=encoding, end="}")
        if target == None:
            target = TreeBuilder()
        # underscored names are provided for compatibility only
        self.parser = parser
        self._parser = parser
        self.target = target
        self._target = target
        # self._error = expat.error
        self._names = {}  # name memo cache
        # main callbacks
        parser.DefaultHandlerExpand = self._default
        if hasattr(target, "start"):
            parser.StartElementHandler = self._start
        if hasattr(target, "end"):
            parser.EndElementHandler = self._end
        if hasattr(target, "data"):
            parser.CharacterDataHandler = target.data
        # miscellaneous callbacks
        if hasattr(target, "comment"):
            parser.CommentHandler = target.comment
        if hasattr(target, "pi"):
            parser.ProcessingInstructionHandler = target.pi
        # Configure pyexpat: buffering, new-style attribute handling.
        parser.buffer_text = 1
        parser.ordered_attributes = 1
        parser.specified_attributes = 1
        self._doctype = None
        self.entity = {}
        self.version = "LARKY!"
        return self
    self = __init__(html, target, encoding)
    return self


def canonicalize(xml_data=None, out=None, from_file=None, **options):
    """Convert XML to its C14N 2.0 serialised form.
    If *out* is provided, it must be a file or file-like object that receives
    the serialised canonical XML output (text, not bytes) through its ``.write()``
    method.  To write to a file, open it in text mode with encoding "utf-8".
    If *out* is not provided, this function returns the output as text string.
    Either *xml_data* (an XML string) or *from_file* (a file path or
    file-like object) must be provided as input.
    The configuration options are the same as for the ``C14NWriterTarget``.
    """
    if xml_data == None and from_file == None:
        fail("ValueError: Either 'xml_data' or 'from_file' must be provided as input")
    sio = None
    if out == None:
        sio = io.StringIO()
        out = sio

    if 'parser' in options:
        parser = options.pop('parser')
    else:
        parser = XMLParser(target=C14NWriterTarget(out.write, **options))

    if xml_data != None:
        parser.feed(xml_data)
        parser.close()
    elif from_file != None:
        parse(from_file, parser=parser)

    return sio.getvalue() if sio != None else None


_looks_like_prefix_name = re.compile(r"^\w+:\w+$", re.UNICODE).match


def _escape_cdata_c14n(text):
    # escape character data
    # it's worth avoiding do-nothing calls for strings that are
    # shorter than 500 character, or so.  assume that's, by far,
    # the most common case in most applications.
    if "&" in text:
        text = text.replace("&", "&amp;")
    if "<" in text:
        text = text.replace("<", "&lt;")
    if ">" in text:
        text = text.replace(">", "&gt;")
    if "\r" in text:
        text = text.replace("\n", "&#xD;")
    return six.ensure_str(text)


def _escape_attrib_c14n(text):
    # escape attribute value
    if "&" in text:
        text = text.replace("&", "&amp;")
    if "<" in text:
        text = text.replace("<", "&lt;")
    if '"' in text:
        text = text.replace('"', "&quot;")
    if "\t" in text:
        text = text.replace("\t", "&#x9;")
    if "\n" in text:
        text = text.replace("\n", "&#xA;")
    if "\r" in text:
        text = text.replace("\r", "&#xD;")
    return six.ensure_str(text)


def C14NWriterTarget(write,
    with_comments=False,
    strip_text=False,
    rewrite_prefixes=False,
    qname_aware_tags=None,
    qname_aware_attrs=None,
    exclude_attrs=None,
    exclude_tags=None,
):
    """
    Canonicalization writer target for the XMLParser.
    Serialises parse events to XML C14N 2.0.
    The *write* function is used for writing out the resulting data stream
    as text (not bytes).  To write to a file, open it in text mode with encoding
    "utf-8" and pass its ``.write`` method.
    Configuration options:
    - *with_comments*: set to true to include comments
    - *strip_text*: set to true to strip whitespace before and after text content
    - *rewrite_prefixes*: set to true to replace namespace prefixes by "n{number}"
    - *qname_aware_tags*: a set of qname aware tag names in which prefixes
                          should be replaced in text content
    - *qname_aware_attrs*: a set of qname aware attribute names in which prefixes
                           should be replaced in text content
    - *exclude_attrs*: a set of attribute names that should not be serialised
    - *exclude_tags*: a set of tag names that should not be serialised
    """
    self = larky.mutablestruct(__name__='C14NWriterTarget', __class__=C14NWriterTarget)

    def __init__(
        write,
        with_comments,
        strip_text,
        rewrite_prefixes,
        qname_aware_tags,
        qname_aware_attrs,
        exclude_attrs,
        exclude_tags,
    ):
        self._write = write
        self._data = []
        self._with_comments = with_comments
        self._strip_text = strip_text
        self._exclude_attrs = Set(exclude_attrs) if exclude_attrs else None
        self._exclude_tags = Set(exclude_tags) if exclude_tags else None

        self._rewrite_prefixes = rewrite_prefixes
        if qname_aware_tags:
            self._qname_aware_tags = Set(qname_aware_tags)
        else:
            self._qname_aware_tags = None
        if qname_aware_attrs:
            self._find_qname_aware_attrs = Set(qname_aware_attrs).intersection
        else:
            self._find_qname_aware_attrs = None

        # Stack with globally and newly declared namespaces as (uri, prefix) pairs.
        self._declared_ns_stack = [
            [
                ("http://www.w3.org/XML/1998/namespace", "xml"),
            ]
        ]
        # Stack with user declared namespace prefixes as (uri, prefix) pairs.
        self._ns_stack = []
        if not rewrite_prefixes:
            self._ns_stack.append(list(_namespace_map.items()))
        self._ns_stack.append([])
        self._prefix_map = {}
        self._preserve_space = [False]
        self._pending_start = None
        self._root_seen = False
        self._root_done = False
        self._ignored_depth = 0
        return self
    self = __init__(write, with_comments, strip_text, rewrite_prefixes, qname_aware_tags, qname_aware_attrs, exclude_attrs, exclude_tags)

    def _iter_namespaces(ns_stack, _reversed=reversed):
        ns = []
        for namespaces in _reversed(ns_stack):
            if namespaces:  # almost no element declares new namespaces
                ns.extend(namespaces)
        return ns
    self._iter_namespaces = _iter_namespaces

    def _resolve_prefix_name(prefixed_name):
        prefix, name = prefixed_name.split(":", 1)
        for uri, p in self._iter_namespaces(self._ns_stack):
            if p == prefix:
                return "{{%s}}%s" % (uri, name)
        fail()
    self._resolve_prefix_name = _resolve_prefix_name

    def _qname(qname, uri=None):
        if uri == None:
            uri, tag = qname[1:].rsplit("}", 1) if qname[:1] == "{" else ("", qname)
        else:
            tag = qname

        prefixes_seen = Set()
        for u, prefix in self._iter_namespaces(self._declared_ns_stack):
            if u == uri and prefix not in prefixes_seen:
                return "%s:%s" % (prefix, tag) if prefix else tag, tag, uri
            prefixes_seen.add(prefix)

        # Not declared yet => add new declaration.
        if self._rewrite_prefixes:
            if uri in self._prefix_map:
                prefix = self._prefix_map[uri]
            else:
                prefix = "n%s" % len(self._prefix_map)
                self._prefix_map[uri] = prefix
            self._declared_ns_stack[-1].append((uri, prefix))
            return "%s:%s" % (prefix, tag), tag, uri

        if not uri and "" not in prefixes_seen:
            # No default namespace declared => no prefix needed.
            return tag, tag, uri

        for u, prefix in self._iter_namespaces(self._ns_stack):
            if u == uri:
                self._declared_ns_stack[-1].append((uri, prefix))
                return "%s:%s" % (prefix, tag) if prefix else tag, tag, uri

        if not uri:
            # As soon as a default namespace is defined,
            # anything that has no namespace (and thus, no prefix) goes there.
            return tag, tag, uri

        fail("ValueError: Namespace %s is not declared in scope" % uri)

    self._qname = _qname

    def data(data):
        if not self._ignored_depth:
            self._data.append(data)
    self.data = data

    def _flush(_join_text="".join):
        data = _join_text(self._data)
        self._data.clear()
        if self._strip_text and not self._preserve_space[-1]:
            data = data.strip()
        if self._pending_start != None:
            args, self._pending_start = self._pending_start, None
            qname_text = data if data and _looks_like_prefix_name(data) else None
            _args = list(args)
            _args.append(qname_text)
            self._start(*_args)
            if qname_text != None:
                return
        if data and self._root_seen:
            self._write(_escape_cdata_c14n(data))
    self._flush = _flush

    def start_ns(prefix, uri):
        if self._ignored_depth:
            return
        # we may have to resolve qnames in text content
        if self._data:
            self._flush()
        self._ns_stack[-1].append((uri, prefix))
    self.start_ns = start_ns

    def start(tag, attrs):
        if self._exclude_tags != None and (
            self._ignored_depth or tag in self._exclude_tags
        ):
            self._ignored_depth += 1
            return
        if self._data:
            self._flush()
        # ("http://www.w3.org/XML/1998/namespace", "xml")
        new_namespaces = attrs.pop('nsmap').items() if 'nsmap' in attrs else []
        self._declared_ns_stack.append(new_namespaces)

        if self._qname_aware_tags != None and tag in self._qname_aware_tags:
            # Need to parse text first to see if it requires a prefix declaration.
            self._pending_start = (tag, attrs, new_namespaces)
            return
        self._start(tag, attrs, new_namespaces)
    self.start = start

    def _start(tag, attrs, new_namespaces, qname_text=None):
        if self._exclude_attrs != None and attrs:
            attrs = {k: v for k, v in attrs.items() if k not in self._exclude_attrs}

        _x = [tag] + list(attrs.keys())
        qnames = Set(_x)
        resolved_names = {}

        # Resolve prefixes in attribute and tag text.
        if qname_text != None:
            qname = self._resolve_prefix_name(qname_text)
            resolved_names[qname_text] = qname
            qnames.add(qname)
        if self._find_qname_aware_attrs != None and attrs:
            qattrs = self._find_qname_aware_attrs(attrs)
            if qattrs:
                for attr_name in qattrs:
                    value = attrs[attr_name]
                    if _looks_like_prefix_name(value):
                        qname = self._resolve_prefix_name(value)
                        resolved_names[value] = qname
                        qnames.add(qname)
            else:
                qattrs = None
        else:
            qattrs = None

        # Assign prefixes in lexicographical order of used URIs.
        parse_qname = self._qname
        parsed_qnames = {
            n: parse_qname(n) for n in sorted(qnames, key=lambda n: n.split("}", 1))
        }

        # Write namespace declarations in prefix order ...
        if new_namespaces:
            attr_list = [
                ("xmlns:" + prefix if prefix else "xmlns", uri)
                for uri, prefix in new_namespaces
            ]
            attr_list = sorted(attr_list)
        else:
            # almost always empty
            attr_list = []

        # ... followed by attributes in URI+name order
        if attrs:
            for k, v in sorted(attrs.items()):
                if qattrs != None and k in qattrs and v in resolved_names:
                    v = parsed_qnames[resolved_names[v]][0]
                attr_qname, attr_name, uri = parsed_qnames[k]
                # No prefix for attributes in default ('') namespace.
                attr_list.append((attr_qname if uri else attr_name, v))

        # Honour xml:space attributes.
        space_behaviour = attrs.get("{http://www.w3.org/XML/1998/namespace}space")
        self._preserve_space.append(
            space_behaviour == "preserve"
            if space_behaviour
            else self._preserve_space[-1]
        )

        # Write the tag.
        write = self._write
        write("<" + parsed_qnames[tag][0])
        if attr_list:
            write("".join([' %s="%s"' % (k, _escape_attrib_c14n(v)) for k, v in attr_list]))
        write(">")

        # Write the resolved qname text content.
        if qname_text != None:
            write(_escape_cdata_c14n(parsed_qnames[resolved_names[qname_text]][0]))

        self._root_seen = True
        self._ns_stack.append([])
    self._start = _start

    def end(tag):
        if self._ignored_depth:
            self._ignored_depth -= 1
            return
        if self._data:
            self._flush()
        self._write("</%s>" % (self._qname(tag)[0]))
        self._preserve_space.pop()
        self._root_done = len(self._preserve_space) == 1
        self._declared_ns_stack.pop()
        self._ns_stack.pop()
    self.end = end

    def comment(text):
        if not self._with_comments:
            return
        if self._ignored_depth:
            return
        if self._root_done:
            self._write("\n")
        elif self._root_seen and self._data:
            self._flush()
        self._write("<!--%s-->" % _escape_cdata_c14n(text))
        if not self._root_seen:
            self._write("\n")
    self.comment = comment

    def pi(target, data):
        if self._ignored_depth:
            return
        if self._root_done:
            self._write("\n")
        elif self._root_seen and self._data:
            self._flush()
        self._write(
            "<?%s %s?>" % (target, _escape_cdata_c14n(data))
            if data
            else "<?%s?>" % (_escape_cdata_c14n(data))
        )
        if not self._root_seen:
            self._write("\n")
    self.pi = pi

    def close():
        pass
    self.close = close
    return self



ElementTree = larky.struct(
    Comment=Comment,
    Element=Element,
    ElementTree=_ElementTree,
    HTML_EMPTY=HTML_EMPTY,
    PI=PI,
    ParseError=ParseError,
    ProcessingInstruction=ProcessingInstruction,
    QName=QName,
    SubElement=SubElement,
    TreeBuilder=TreeBuilder,
    XML=XML,
    XMLID=XMLID,
    XMLParser=XMLParser,
    XMLPullParser=XMLPullParser,
    _ListDataStream=_ListDataStream,
    _escape_attrib=_escape_attrib,
    _escape_attrib_html=_escape_attrib_html,
    _escape_cdata=_escape_cdata,
    _get_writer=_get_writer,
    _namespace_map=_namespace_map,
    _namespaces=_namespaces,
    _raise_serialization_error=_raise_serialization_error,
    _serialize=_serialize,
    _serialize_html=_serialize_html,
    _serialize_text=_serialize_text,
    _serialize_xml=_serialize_xml,
    dump=dump,
    fromstring=fromstring,
    fromstringlist=fromstringlist,
    iselement=iselement,
    iterparse=iterparse,
    parse=parse,
    register_namespace=register_namespace,
    tostring=tostring,
    tostringlist=tostringlist,
    indent=indent,
    canonicalize=canonicalize,
    C14NWriterTarget=C14NWriterTarget
)