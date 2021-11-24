#
# ElementTree
# $Id: ElementC14N.py 3392 2008-03-10 23:32:45Z fredrik $
#
# canonicalisation (c14n) support for element trees
#
# history:
# 2007-12-14 fl   created (normalized version)
# 2008-02-12 fl   roundtrip support
# 2008-03-03 fl   fixed parent map and scope setting/sorting bugs
# 2008-03-05 fl   fixed namespace declarations in exclusive mode
# 2008-03-10 fl   added inclusive subset support
#
# Copyright (c) 2007-2008 by Fredrik Lundh.  All rights reserved.
#
# fredrik@pythonware.com
# http://www.pythonware.com
#
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
load("@stdlib//enum", enum="enum")
load("@stdlib//functools", cmp_to_key="cmp_to_key")
load("@stdlib//io", io="io")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//sets", Set="Set")
load("@stdlib//xml/etree/ElementTree", ElementTree="ElementTree")
load("@vendor//option/result", Error="Error")
load("@vendor//six", six="six")


_namespaces = ElementTree._namespaces
iterparse = ElementTree.iterparse
QName = ElementTree.QName

# C14N escape methods


def _escape_cdata_c14n(text):
    # escape character data
    # it's worth avoiding do-nothing calls for strings that are
    # shorter than 500 character, or so.  assume that's, by far,
    # the most common case in most applications.
    text = six.ensure_str(text)

    if "&" in text:
        text = text.replace("&", "&amp;")
    if "<" in text:
        text = text.replace("<", "&lt;")
    if ">" in text:
        text = text.replace(">", "&gt;")
    if "\r" in text:
        text = text.replace("\n", "&#xD;")
    return text



def _escape_attrib_c14n(text):
    # escape attribute value
    text = six.ensure_str(text)

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
    return text


def WriteC14N(write):
    self = larky.mutablestruct(__name__='WriteC14N', __class__=WriteC14N)
    # C14N writer target

    def __init__(write):
        self.write = write
        return self
    self = __init__(write)

    def start(tag, attrs):
        # expects to get the attributes as a list of pairs, *in order*
        # FIXME: pass in prefix/uri/tag triples instead?
        write = self.write
        # print("tag?", tag, type(tag))
        write("<" + six.ensure_str(tag))
        for k, v in attrs:
            write(' %s="%s"' % (
                six.ensure_str(k),
                _escape_attrib_c14n(v)
            ))
        write(">")
    self.start = start

    def data(data):
        self.write(_escape_cdata_c14n(data))
    self.data = data

    def end(tag):
        self.write("</" + six.ensure_str(tag) + ">")
    self.end = end
    return self


_SerializeState = enum.Enum('_SerializeState', [
    ('PRE', 0),
    ('POST', 1)
])


def _serialize(elem, target, qnames, namespaces):

    # event generator
    def emit(root, nsmap=None):
        elem = root
        queue = [(_SerializeState.PRE, (0, elem, nsmap))]  # (level, element, nsmap)
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not queue:
                break
            state, payload = queue.pop(0)

            if state == _SerializeState.PRE:
                level, elem, namespaces = payload
                if larky.impl_function_name(elem.tag) == 'Document':
                    print("here!")
                    print("elem.children", elem.getchildren())
                    if elem.docinfo:
                        if elem.docinfo.doctype:
                            dtd_str = elem.docinfo.doctype
                        else:
                            dtd_str = "<!DOCTYPE %s>" % elem.docinfo._name
                        target.data(dtd_str)
                    queue.append((_SerializeState.PRE, (0, elem.getroot(), namespaces)))
                    continue
                tag = qnames[elem.tag]
                attrib = []
                # namespaces first, sorted by prefix
                if namespaces:
                    for v, k in sorted(list(namespaces.items()), key=lambda x: x[1]):
                        attrib.append(("xmlns:" + k, v))

                # attributes next, sorted by (uri, local)
                for k, v in sorted(elem.attrib.items()):
                    attrib.append((qnames[k], v))
                target.start(tag, attrib)
                if elem.text:
                    target.data(elem.text)

                # depth first search (mimicking recursion)
                children = [
                    (_SerializeState.PRE, (level + 1, child, namespaces))
                    for child in elem
                ]
                children.append((_SerializeState.POST, elem)) # post visit
                queue = children + queue
            elif state == _SerializeState.POST:
                elem = payload
                tag = qnames[elem.tag]
                target.end(tag)
                if elem.tail:
                    target.data(elem.tail)
    emit(elem, namespaces)


def _serialize_inclusive(elem, target, scope, parent, nsmap):
    def qname(elem, qname):
        if qname[:1] == "{":
            uri, tag = qname[1:].split("}", 1)
            _execute_for_else = True
            for prefix, u in _listscopes(elem, scope, parent):
                if u == uri:
                    _execute_for_else = False
                    break
            if _execute_for_else:
                fail("IOError: %s not in scope" % uri)  # FIXME
            if prefix == "":
                return tag  # default namespace
            return prefix + ":" + tag
        else:
            return qname

    def emit(root, nsmap):
        elem = root
        queue = [(_SerializeState.PRE, (0, elem, nsmap))]  # (level, element, nsmap)
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not queue:
                break
            state, payload = queue.pop(0)

            if state == _SerializeState.PRE:
                level, elem, namespaces = payload
                tag = qname(elem, elem.tag)
                attrib = []
                # namespaces first, sorted by prefix
                namespaces = scope.get(elem.tag)
                if namespaces or nsmap:
                    if not namespaces:
                        namespaces = []
                    if nsmap:
                        nsdict = dict(namespaces)
                        for p, u in nsmap:
                            if p not in nsdict:
                                namespaces.append((p, u))
                    for p, u in sorted(namespaces):
                        if p:
                            attrib.append(("xmlns:" + p, u))
                # attributes next, sorted by (uri, local)
                for k, v in sorted(elem.attrib.items()):
                    attrib.append((qname(elem, k), v))
                target.start(tag, attrib)
                if elem.text:
                    target.data(elem.text)

                # depth first search (mimicking recursion)
                children = [
                    (_SerializeState.PRE, (level + 1, child, None))
                    for child in elem
                ]
                children.append((_SerializeState.POST, elem)) # post visit
                queue = children + queue
            elif state == _SerializeState.POST:
                elem = payload
                tag = qname(elem, elem.tag)
                target.end(tag)
                if elem.tail:
                    target.data(elem.tail)

    emit(elem, nsmap)


def _serialize_exclusive(elem, target, scope, parent, nsinclude):
    print("nsinclude:", nsinclude)
    def qname(elem, qname):
        if qname[:1] == "{":
            uri, tag = qname[1:].split("}", 1)
            _execute_for_else = True
            for prefix, u in _listscopes(elem, scope, parent):
                if u == uri:
                    _execute_for_else = False
                    break
            if _execute_for_else:
                fail("IOError: " + "%s not in scope" % uri)
            return prefix, uri, prefix + ":" + tag
        else:
            return None, None, qname

    stack = [{}]

    def _emit(root):
        elem = root
        queue = [(_SerializeState.PRE, (0, elem))]  # (level, element, nsmap)
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not queue:
                break
            state, payload = queue.pop(0)
            if state == _SerializeState.PRE:
                level, elem = payload
                # identify target namespaces
                namespaces = {}
                rendered = dict(**stack[-1])
                # element tag
                prefix, uri, tag = qname(elem, elem.tag)
                if prefix:
                    namespaces[prefix] = uri
                # attributes
                attrib = []
                for k, v in sorted(elem.attrib.items()):
                    prefix, uri, k = qname(elem, k)
                    if prefix:
                        namespaces[prefix] = uri
                    attrib.append((k, v))
                # explicitly included namespaces
                if nsinclude and elem.tag in scope:
                    for p, u in scope[elem.tag]:
                        if p not in namespaces and p in nsinclude:
                            namespaces[p] = u
                # build namespace attribute list
                xmlns = []
                for p, u in sorted(namespaces.items()):
                    if p and rendered.get(p) != u:
                        xmlns.append(("xmlns:" + p, u))
                    rendered[p] = u
                # serialize
                target.start(tag, xmlns + attrib)
                if elem.text:
                    target.data(elem.text)
                stack.append(rendered)

                children = [
                    (_SerializeState.PRE, (level + 1, child))
                    for child in elem
                ]
                children.append((_SerializeState.POST, elem)) # post visit
                queue = children + queue
            elif state == _SerializeState.POST:
                stack.pop()
                elem = payload
                prefix, uri, tag = qname(elem, elem.tag)
                target.end(tag)
                if elem.tail:
                    target.data(elem.tail)

    _emit(elem)


##
# (Internal) Hook used by ElementTree's c14n output method


def _serialize_c14n(write, elem, encoding, qnames, namespaces):
    if encoding != "utf-8":
        fail("ValueError: " + "invalid encoding (%s)" % encoding)
    _serialize(elem, WriteC14N(write), qnames, namespaces)


##
# Writes a canonicalized document.
#
# @def write(elem, file, subset=None, **options)
# @param elem Element or ElementTree.  If passed a tree created by {@link
#     parse}, the function attempts to preserve existing prefixes.
#     Otherwise, new prefixes are allocated.
# @param file Output file.  Can be either a filename or a file-like object.
# @param subset Subset element, if applicable.
# @param **options Options, given as keyword arguments.
# @keyparam exclusive Use exclusive C14N.  In this mode, namespaces
#     declarations are moved to the first element (in document order)
#     that actually uses the namespace.
# @keyparam inclusive_namespaces If given, a list or set of prefxies
#     that should be retained in the serialized document, even if
#     they're not used.  This applies to exclusive serialization only
#     (for inclusive subsets, all prefixes are always included).


def write(
    elem, file_or_filename, subset=None, exclusive=False, inclusive_namespaces=None
):
    if not getattr(file_or_filename, "write", False):
        fail("need .write() method passed in to file_or_filename")
    file = file_or_filename
    out = WriteC14N(file.write)

    if not hasattr(elem, "_scope"):
        # ordinary tree; allocate new prefixes up front
        if subset != None:
            fail("ValueError: subset only works for scoped trees")
        root = elem
        tag = getattr(root, 'tag', None)
        if tag and larky.impl_function_name(tag) == 'Document':
            root = elem.getroot()
        qnames, namespaces = _namespaces(root, None)
        _serialize(elem, out, qnames, namespaces)
    else:
        # scoped tree
        scope = elem._scope
        parent = elem._parent
        if exclusive:
            # exclusive mode
            if subset == None:
                elem = elem.getroot()
            else:
                elem = subset
            nsinclude = Set(inclusive_namespaces or [])
            _serialize_exclusive(elem, out, scope, parent, nsinclude)
        else:
            # inclusive mode
            if subset == None:
                elem = elem.getroot()
                nsmap = []
            else:
                # bring used namespaces into scope
                nsmap = {}
                elem = subset
                for p, u in _listscopes(elem, scope, parent):
                    if p not in nsmap:
                        nsmap[p] = u
                nsmap = list(nsmap.items())
            _serialize_inclusive(elem, out, scope, parent, nsmap)


##
# Parses an XML file, and builds a tree annotated with scope and parent
# information.  To parse from a string, use the StringIO module.
#
# @param file A file name or file object.
# @return An extended ElementTree, with extra scope and parent information
#    attached to the ElementTree object.


def parse(file, parser, tree_factory=ElementTree.ElementTree):

    events = "start", "start-ns", "end"

    root = None
    ns_map = []

    scope = {}
    parent = {}

    stack = []

    for event, elem in iterparse(file, events, parser):
        if event == "start-ns":
            ns_map.append(elem)

        elif event == "start":
            if stack:
                parent[elem.tag] = stack[-1]
            stack.append(elem)
            if root == None:
                root = elem
            if ns_map:
                scope[elem.tag] = ns_map
                ns_map = []

        elif event == "end":
            stack.pop()
    if not (parent == dict([(c.tag, p) for p in root.iter() for c in p])):
        # print(parent)
        # print( dict([(c.tag, p.tag) for p in root.iter() for c in p]))
        fail("assert parent == dict([(c, p) for p in root.getiterator() for c in p]) failed!")
    # print(scope)
    # print(parent)
    tree = tree_factory(root)
    tree._scope = scope
    tree._parent = parent

    return tree


##
# (Internal) Finds undefined URI:s in a scoped tree.


def _find_open_uris(elem, scope, parent):
    uris = {}  # set of open URIs
    stack = [{}]  # stack of namespace maps

    def qname(qname):
        if qname[:1] == "{":
            uri, tag = qname[1:].split("}", 1)
            if uri not in stack[-1]:
                uris[uri] = None

    def check(root):
        elem = root
        queue = [((0, elem), None)]  # (level, element), stacktop
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not queue:
                if stack:
                    stack.pop()
                break
            (level, elem), _top = queue.pop(0)
            if _top:
                stack.append(_top)
            ns = dict(**stack[-1])
            if elem in scope:
                for prefix, uri in scope[elem]:
                    ns[uri] = prefix
            stack.append(ns)
            qname(elem.tag)
            for k in elem:
                queue.insert(0, ((level + 1, k), ns))
            stack.pop()
    check(elem)
    return list(uris.keys())


##
# (Internal) Returns a sequence of (prefix, uri) pairs.


def _listscopes(elem, scope, parent):
    q = []
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if elem == None:
            break
        ns = scope.get(elem.tag)
        if ns:
            for prefix_uri in ns:
                q.append(prefix_uri)
                # yield prefix_uri
        elem = parent.get(elem.tag)
    return q


##
# (Internal) Finds prefix for given URI in a scoped tree.


def _findprefix(elem, scope, parent, uri):
    for p, u in _listscopes(elem, scope, parent):
        if u == uri:
            return p
    return None


ElementC14N = larky.struct(
    __name__='ElementC14N',
    _escape_attrib_c14n=_escape_attrib_c14n,
    _escape_cdata_c14n=_escape_cdata_c14n,
    _find_open_uris=_find_open_uris,
    _findprefix=_findprefix,
    _listscopes=_listscopes,
    _serialize=_serialize,
    _serialize_c14n=_serialize_c14n,
    _serialize_exclusive=_serialize_exclusive,
    _serialize_inclusive=_serialize_inclusive,
    parse=parse,
    WriteC14N=WriteC14N,
    write=write
)