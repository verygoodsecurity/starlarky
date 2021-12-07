# A drop-in replacement for lxml.etree package.
# Does not support namespaces, xpath and many other features.
# Written by Ilya Zverev, licensed WTFPL.
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load(
    "@stdlib//larky",
    WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION",
    larky="larky",
)
load("@stdlib//operator", operator="operator")
load("@vendor//option/result", Error="Error")


def DocInfo():
    self = larky.mutablestruct(__name__="DocInfo", __class__=DocInfo)

    def __init__():
        self.xml_version = None
        self.encoding = None
        self.doctype = None
        return self

    self = __init__()
    return self


def ElementTree(root):
    self = larky.mutablestruct(__name__="ElementTree", __class__=ElementTree)

    def __init__(root):
        self.root = root
        if root.tree != None:
            self.docinfo = root.tree.docinfo
        else:
            self.docinfo = DocInfo()
        return self

    self = __init__(root)

    def getroot():
        return self.root

    self.getroot = getroot
    return self


def ElementMatchIterator(first, tag=None, *tags):
    self = larky.mutablestruct(
        __name__="ElementMatchIterator", __class__=ElementMatchIterator
    )

    def __init__(
        first,
        tag,
    ):
        self.first = first
        self.tags = {}
        if tag != None:
            self.tags[tag] = True
        for tag in tags:
            self.tags[tag] = True
        self.done = False
        return self

    self = __init__(first, tag)

    def __iter__():
        return self

    self.__iter__ = __iter__

    def _next():
        if not self.done:
            self.done = True
            return self.first
        return None

    self._next = _next

    def next():
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            n = self._next()
            if n == None:
                fail()
            if len(self.tags) == 0 or n.tag in self.tags:
                return n

    self.next = next

    def __next__():
        return self.next()

    self.__next__ = __next__
    return self


def ElementDepthFirstIterator(first, tag=None, inclusive=True, tags=()):
    self = ElementMatchIterator(first, tag, *tags)
    self.__name__="ElementDepthFirstIterator"
    self.__class__=ElementDepthFirstIterator

    def __init__(inclusive):
        self.stack = []
        if not inclusive:
            self._next()
        return self

    self = __init__(inclusive)

    def _next():
        if self.first == None:
            return None
        res = self.first
        if len(self.first):
            self.stack.append(self.first)
            self.first = self.first[0]
        elif len(self.stack) > 0:
            nxt = self.first.getnext()
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                if not nxt == None and len(self.stack) > 0:
                    break
                self.first = self.stack.pop()
                nxt = self.first.getnext()
            if len(self.stack) == 0:
                self.first = None
            else:
                self.first = nxt
        else:
            self.first = None

        return res

    self._next = _next
    return self


def ElementChildIterator(first, tag=None, reversed=False, tags=()):
    self = ElementMatchIterator(first, tag, *tags)
    self.__name__="ElementChildIterator"
    self.__class__=ElementChildIterator

    def __init__(first, reversed):
        self.d = -1 if reversed else 1
        self.i = len(first) - 1 if reversed else 0
        return self

    self = __init__(first,reversed)

    def _next():
        if self.i < 0 or self.i >= len(self.first):
            return None
        res = self.first[self.i]
        self.i += self.d
        return res

    self._next = _next
    return self


def SiblingsIterator(first, tag=None, preceding=False, tags=()):
    self = ElementMatchIterator(first, tag, *tags)
    self.__name__="SiblingsIterator"
    self.__class__=SiblingsIterator

    def __init__(preceding):
        self.preceding = preceding
        return self

    self = __init__(preceding)

    def _next():
        if self.first == None:
            return None
        if self.preceding:
            self.first = self.first.getprevious()
        else:
            self.first = self.first.getnext()
        return self.first

    self._next = _next
    return self


def AncestorsIterator(first, tag=None, tags=()):
    self = ElementMatchIterator(first, tag, *tags)
    self.__name__="AncestorsIterator"
    self.__class__=AncestorsIterator

    def _next():
        if self.first == None:
            return None
        self.first = self.first.getparent()
        return self.first

    self._next = _next
    return self


def Element(tag, attrib=None, **kwargs):
    self = larky.mutablestruct(__name__="Element", __class__=Element)

    def __init__(tag, attrib, kwargs):
        self.tag = tag
        self.attrib = {}
        if attrib != None:
            self.attrib = attrib
        for k, v in kwargs.iteritems():
            self.attrib[k] = v
        self.children = []
        self.parent = None
        self.text = None
        self.tail = None
        self.tree = None
        return self

    self = __init__(tag, attrib, kwargs)

    def getroottree():
        el = self
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if el == None:
                break
            if el.tree != None:
                return el.tree
            elif el.parent == None:
                return ElementTree(el)
            el = el.parent
        return None

    self.getroottree = getroottree

    def getchildren():
        return self.children

    self.getchildren = getchildren

    def getparent():
        return self.parent

    self.getparent = getparent

    def getnext():
        if self.parent == None:
            return None
        i = self.parent.children.index(self)
        if i + 1 < len(self.parent.children):
            return self.parent.children[i + 1]
        return None

    self.getnext = getnext

    def getprevious():
        if self.parent == None:
            return None
        i = self.parent.children.index(self)
        if i > 0:
            return self.parent.children[i - 1]
        return None

    self.getprevious = getprevious

    def addnext(el):
        if self.parent == None:
            fail("ValueError: No parent specified, thus no next item")
        i = self.parent.children.index(self)
        self.parent.insert(i + 1, el)

    self.addnext = addnext

    def addprevious(el):
        if self.parent == None:
            fail("ValueError: No parent specified, thus no next item")
        i = self.parent.children.index(self)
        self.parent.insert(i, el)

    self.addprevious = addprevious

    def find(tag):
        for el in self.children:
            if el.tag == tag:
                return el
        return None

    self.find = find

    def findall(tag):
        return [el for el in self.children if el.tag == tag]

    self.findall = findall

    def remove(el):
        i = self.children.index(el)
        self.__delitem__(i)

    self.remove = remove

    def replace(el, el2):
        i = self.children.index(el)
        self.children[i].parent = None
        self.children[i] = el2
        el2.parent = self

    self.replace = replace

    # Advanced iterators
    def iter(tag=None, *tags):
        return ElementDepthFirstIterator(self, tag=tag, inclusive=True, tags=tags)

    self.iter = iter

    def iterchildren(tag=None, reversed=False, *tags):
        return ElementChildIterator(self, tag=tag, reversed=reversed, tags=tags)

    self.iterchildren = iterchildren

    def itersiblings(tag=None, preceding=False, *tags):
        return SiblingsIterator(self, tag=tag, preceding=preceding, tags=tags)

    self.itersiblings = itersiblings

    def iterancestors(tag=None, *tags):
        return AncestorsIterator(self, tag=tag, tags=tags)

    self.iterancestors = iterancestors

    def iterdescendants(tag=None, *tags):
        return ElementDepthFirstIterator(self, tag=tag, inclusive=False, tags=tags)

    self.iterdescendants = iterdescendants

    # Emulating dict for attributes
    def get(k, default=None):
        if k in self.attrib:
            return self.attrib[k]
        else:
            return default

    self.get = get

    def set(k, v):
        self.attrib[k] = v

    self.set = set

    def keys():
        return self.attrib.keys()

    self.keys = keys

    def values():
        return self.attrib.values()

    self.values = values

    def items():
        return self.attrib.items()

    self.items = items

    # Emulating list for children
    def insert(idx, child):
        self.children.insert(idx, child)
        child.parent = self

    self.insert = insert

    def append(child):
        self.children.append(child)
        child.parent = self

    self.append = append

    def index(el):
        return self.children.index(el)

    self.index = index

    def __getitem__(idx):
        return self.children.__getitem__(idx)

    self.__getitem__ = __getitem__

    def __delitem__(idx):
        self.children[idx].parent = None
        operator.delitem(self.children, idx)

    self.__delitem__ = __delitem__

    def __len__():
        return len(self.children)

    self.__len__ = __len__

    def __iter__():
        return self.children.__iter__()

    self.__iter__ = __iter__

    def next():
        return self.children.next()

    self.next = next
    return self


def SubElement(parent, tag, attrib=None, **kwargs):
    self = Element(tag, attrib, **kwargs)
    self.__name__="SubElement"
    self.__class__=SubElement

    def __init__(parent):
        parent.append(self)
        return self

    self = __init__(parent)
    return self


def iselement(el):
    return builtins.isinstance(el, Element)


def XMLSyntaxError(parent, tag, attrib=None, **kwargs):
    fail("XMLSyntaxError: %r, %r, %r, %r" % (parent, tag, attrib, kwargs))


def _xml_decode(s):
    replacements = {"quot": '"', "apos": "'", "amp": "&", "lt": "<", "gt": ">"}
    pos = s.find("&")
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if pos < 0:
            break
        semic = s.find(";", pos + 1)
        repl = None
        if semic > pos + 1:
            entity = s[pos + 1 : semic]
            if entity in replacements:
                repl = replacements[entity]
            elif entity[0] == "#":
                ishex = entity[1] == "x"
                code = int(entity[1 + (1 if ishex else 0) :], 16 if ishex else 10)
                if code < 128:
                    repl = chr(code)
        if repl != None:
            s = s[:pos] + repl + s[semic + 1 :]
        pos = s.find("&", semic + 1)
    return s


def _parse_attributes(tag):
    """Parses string '<tag k="..." ...>' to an element."""
    pos = 1
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not (tag[pos].isalnum() or tag[pos] in ("-", ":", "_", ".", "?")):
            break
        pos += 1
    el = Element(tag[1:pos])
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not (
                pos < len(tag)
                and not tag[pos].isalnum()
                and tag[pos] not in ("/", ">")
            ):
                break
            pos += 1
        if pos == len(tag) or not tag[pos].isalnum():
            break
        eq = tag.find("=", pos)
        quote = tag[eq + 1]
        qend = tag.find(quote, eq + 2)
        el.set(tag[pos:eq], _xml_decode(tag[eq + 2 : qend]))
        pos = qend + 1
    return el


def _read_element(s, pos):
    """Reads one element, returns tuple (element, pos_of_next_element)."""
    end = s.find(">", pos) + 1
    el = _parse_attributes(s[pos:end])
    if el == None:
        return (None, len(s))
    if s[end - 2] != "/":
        # Non-empty tag
        nxt = s.find("<", end)
        if nxt > end:
            text = s[end:nxt].strip()
            if len(text):
                el.text = _xml_decode(text)
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if s[nxt + 1] == "/":
                break
            child, nxt = _read_element(s, nxt)
            el.append(child)
        if s[nxt + 2 : nxt + len(el.tag) + 2] != el.tag:
            fail()
        end = s.find(">", nxt + 2) + 1
    pos = s.find("<", end)
    if pos > end:
        tail = s[end:pos].strip()
        if len(tail):
            el.tail = _xml_decode(tail)
    return (el, pos)


def fromstring(s):
    docinfo = DocInfo()
    root = None
    pos = s.find("<")
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not root == None and pos < len(s):
            break
        if s[pos + 1] == "?":
            end = s.find("?>", pos)
            decl = _parse_attributes(s[pos : end + 1])
            docinfo.xml_version = decl.get("version", None)
            docinfo.encoding = decl.get("encoding", "utf-8")
            pos = s.find("<", end + 2)
        elif s[pos + 1] == "!":
            # Does not support internal DTD
            end = s.find(">", pos)
            sqbr = s.find("[", pos)
            if sqbr > 0 and sqbr < end:
                # Skip entities
                end = s.find(">", s.find("]", sqbr))
            docinfo.doctype = s[pos : end + 1]
            sqbr = docinfo.doctype.find("[")
            if sqbr > 0:
                docinfo.doctype = (
                    docinfo.doctype[:sqbr].strip()
                    + docinfo.doctype[docinfo.doctype.find("]", sqbr) + 1 :].strip()
                )
            pos = s.find("<", end + 1)
        else:
            root, pos = _read_element(s, pos)
            if root == None:
                break
    if root != None:
        root.tree = ElementTree(root)
        root.tree.docinfo = docinfo
    return root


def XML(s):
    return fromstring(s)


def parse(f):
    root = fromstring(f.read())
    if root == None:
        return None
    return root.tree


def _xml_encode(s):
    return (
        s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "&apos;")
    )


def tostring(
    el,
    pretty_print=False,
    with_tail=True,
    xml_declaration=False,
    encoding=None,
    prefix="",
):
    result = ""
    if builtins.isinstance(el, ElementTree):
        if el.docinfo.xml_version != None:
            if encoding == None:
                if el.docinfo.encoding != None:
                    encoding = el.docinfo.encoding
                else:
                    encoding = "utf-8"
            result += '<?xml version="{0}" encoding="{1}"?>\n'.format(
                el.docinfo.xml_version, _xml_encode(encoding)
            )
        if el.docinfo.doctype != None:
            result += el.docinfo.doctype + "\n"
        result += tostring(el.getroot(), pretty_print=pretty_print, with_tail=with_tail)
        return result

    if not len(prefix) and xml_declaration:
        if encoding == None:
            encoding = "utf-8"
        result += '<?xml version="1.0" encoding="{0}"?>\n'.format(_xml_encode(encoding))
    if pretty_print:
        result += prefix
    result += "<" + el.tag
    for k, v in el.attrib.iteritems():
        result += ' {0}="{1}"'.format(k, _xml_encode(v))
    if len(el) == 0 and el.text == None:
        result += "/>"
        if pretty_print:
            result += "\n"
    else:
        result += ">"
        if pretty_print:
            result += "\n"
        sub_prefix = prefix + "  "
        if el.text != None:
            if pretty_print:
                result += sub_prefix + el.text + "\n"
            else:
                result += el.text
        for child in el:
            result += tostring(
                child, pretty_print=pretty_print, with_tail=with_tail, prefix=sub_prefix
            )
        if pretty_print:
            result += prefix + "</" + el.tag + ">\n"
        else:
            result += "</" + el.tag + ">"
    if with_tail and el.tail != None:
        if pretty_print:
            result += prefix + el.tail + "\n"
        else:
            result += el.tail
    if encoding == None or len(prefix):
        return result
    else:
        return codecs.encode(result, encoding=encoding)


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

# https://github.com/guillaume-humbert/python-xmlschema/blob/master/xmlschema/etree.py

# def etree_tostring(elem, namespaces=None, indent='', max_lines=None, spaces_for_tab=4, xml_declaration=False):
#     """
#     Serialize an Element tree to a string. Tab characters are replaced by whitespaces.
#     :param elem: the Element instance.
#     :param namespaces: is an optional mapping from namespace prefix to URI. Provided namespaces are \
#     registered before serialization.
#     :param indent: the base line indentation.
#     :param max_lines: if truncate serialization after a number of lines (default: do not truncate).
#     :param spaces_for_tab: number of spaces for replacing tab characters (default is 4).
#     :param xml_declaration: if set to `True` inserts the XML declaration at the head.
#     :return: a Unicode string.
#     """
#     def reindent(line):
#         if not line:
#             return line
#         elif line.startswith(min_indent):
#             return line[start:] if start >= 0 else indent[start:] + line
#         else:
#             return indent + line
#
#     if isinstance(elem, etree_element):
#         if namespaces:
#             for prefix, uri in namespaces.items():
#                 if not re.match(r'ns\d+$', prefix):
#                     etree_register_namespace(prefix, uri)
#         tostring = ElementTree.tostring
#
#     elif isinstance(elem, py_etree_element):
#         if namespaces:
#             for prefix, uri in namespaces.items():
#                 if not re.match(r'ns\d+$', prefix):
#                     PyElementTree.register_namespace(prefix, uri)
#         tostring = PyElementTree.tostring
#
#     elif lxml_etree is not None:
#         if namespaces:
#             for prefix, uri in namespaces.items():
#                 if prefix and not re.match(r'ns\d+$', prefix):
#                     lxml_etree_register_namespace(prefix, uri)
#         tostring = lxml_etree.tostring
#     else:
#         raise XMLSchemaTypeError("cannot serialize %r: lxml library not available." % type(elem))
#
#     if PY3:
#         xml_text = tostring(elem, encoding="unicode").replace('\t', ' ' * spaces_for_tab)
#     else:
#         xml_text = unicode(tostring(elem)).replace('\t', ' ' * spaces_for_tab)
#
#     lines = ['<?xml version="1.0" encoding="UTF-8"?>'] if xml_declaration else []
#     lines.extend(xml_text.splitlines())
#     while lines and not lines[-1].strip():
#         lines.pop(-1)
#
#     last_indent = ' ' * min(k for k in range(len(lines[-1])) if lines[-1][k] != ' ')
#     if len(lines) > 2:
#         child_indent = ' ' * min(k for line in lines[1:-1] for k in range(len(line)) if line[k] != ' ')
#         min_indent = min(child_indent, last_indent)
#     else:
#         min_indent = child_indent = last_indent
#
#     start = len(min_indent) - len(indent)
#
#     if max_lines is not None and len(lines) > max_lines + 2:
#         lines = lines[:max_lines] + [child_indent + '...'] * 2 + lines[-1:]
#
#     return '\n'.join(reindent(line) for line in lines)
#
#
# def etree_iterpath(elem, tag=None, path='.', namespaces=None, add_position=False):
#     """
#     Creates an iterator for the element and its subelements that yield elements and paths.
#     If tag is not `None` or '*', only elements whose matches tag are returned from the iterator.
#     :param elem: the element to iterate.
#     :param tag: tag filtering.
#     :param path: the current path, '.' for default.
#     :param add_position: add context position to child elements that appear multiple times.
#     :param namespaces: is an optional mapping from namespace prefix to URI.
#     """
#     if tag == "*":
#         tag = None
#     if tag is None or elem.tag == tag:
#         yield elem, path
#
#     if add_position:
#         children_tags = Counter([e.tag for e in elem])
#         positions = Counter([t for t in children_tags if children_tags[t] > 1])
#     else:
#         positions = ()
#
#     for child in elem:
#         if callable(child.tag):
#             continue  # Skip lxml comments
#
#         child_name = child.tag if namespaces is None else qname_to_prefixed(child.tag, namespaces)
#         if path == '/':
#             child_path = '/%s' % child_name
#         elif path:
#             child_path = '/'.join((path, child_name))
#         else:
#             child_path = child_name
#
#         if child.tag in positions:
#             child_path += '[%d]' % positions[child.tag]
#             positions[child.tag] += 1
#
#         for _child, _child_path in etree_iterpath(child, tag, child_path, namespaces):
#             yield _child, _child_path
#
#
# def etree_getpath(elem, root, namespaces=None, relative=True, add_position=False):
#     """
#     Returns the XPath path from *root* to descendant *elem* element.
#     :param elem: the descendant element.
#     :param root: the root element.
#     :param namespaces: is an optional mapping from namespace prefix to URI.
#     :param relative: returns a relative path.
#     :param add_position: add context position to child elements that appear multiple times.
#     :return: An XPath expression or `None` if *elem* is not a descendant of *root*.
#     """
#     if relative:
#         path = '.'
#     elif namespaces:
#         path = '/%s' % qname_to_prefixed(root.tag, namespaces)
#     else:
#         path = '/%s' % root.tag
#
#     for e, path in etree_iterpath(root, elem.tag, path, namespaces, add_position):
#         if e is elem:
#             return path