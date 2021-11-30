"""XML Canonicalization

This module generates canonical XML of a document or element.
    http://www.w3.org/TR/2001/REC-xml-c14n-20010315
and includes a prototype of exclusive canonicalization
    http://www.w3.org/Signature/Drafts/xml-exc-c14n

Requires PyXML 0.7.0 or later.

Known issues if using Ft.Lib.pDomlette:
    1. Unicode
    2. does not white space normalize attributes of type NMTOKEN and ID?
    3. seems to be include "\n" after importing external entities?

Note, this version processes a DOM tree, and consequently it processes
namespace nodes as attributes, not from a node's namespace axis. This
permits simple document and element canonicalization without
XPath. When XPath is used, the XPath result node list is passed and used to
determine if the node is in the XPath result list, but little else.

Authors:
    "Joseph M. Reagle Jr." <reagle@w3.org>
    "Rich Salz" <rsalz@zolera.com>

$Date: 2003/01/25 11:41:21 $ by $Author: loewis $
"""
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

_SerializeState = enum.Enum('_SerializeState', [
    ('PRE', 0),
    ('POST', 1)
])

XMLNS = larky.struct(
    __name__='XMLNS',
    BASE = "http://www.w3.org/2000/xmlns/",
    XML = "http://www.w3.org/XML/1998/namespace",
)


_attrs = lambda E: E.getattributes().items() or []
_children = lambda E: E.getchildrenref() or []
_IN_XML_NS = lambda n: n.tag.startswith("xmlns")
_inclusive = lambda n: n.unsuppressedPrefixes == None

cmp = six.cmp

# Does a document/PI has lesser/greater document order than the
# first element?
_LesserElement, _Element, _GreaterElement = list(range(3))

def _sorter(n1, n2):
    '''_sorter(n1,n2) -> int
    Sorting predicate for non-NS attributes.'''
    print("_sorter", n1, n2)
    i = cmp(n1.namespaceURI, n2.namespaceURI)
    if i: return i
    return cmp(n1.localName, n2.localName)


def _sorter_ns(n1, n2):
    '''_sorter_ns((n,v),(n,v)) -> int
    "(an empty namespace URI is lexicographically least)."'''
    print("_sorter_ns",n1, n2)
    # n1,n2 = x
    if n1 == 'xmlns': return -1
    if n2 == 'xmlns': return 1
    return cmp(n1, n2)


def _utilized(n, node, other_attrs, unsuppressedPrefixes):
    '''_utilized(n, node, other_attrs, unsuppressedPrefixes) -> boolean
    Return true if that nodespace is utilized within the node'''

    if n.startswith('xmlns:'):
        n = n[6:]
    elif n.startswith('xmlns'):
        n = n[5:]
    if (n=="" and node.prefix in ["#default", None]) or \
        n == node.prefix or n in unsuppressedPrefixes:
        return 1
    for attr in other_attrs:
        if n == attr.prefix: return 1
    return 0

#_in_subset = lambda subset, node: not subset or node in subset
_in_subset = lambda subset, node: subset == None or node in subset # rich's tweak
def _implementation(node, write, **kw):
    '''Implementation class for C14N. This accompanies a node during it's
    processing and includes the parameters and processing state.'''

    # Handler for each node type; populated during module instantiation.
    handlers = {}
    self = larky.mutablestruct(__name__='_implementation', __class__=_implementation)

    def _inherit_context(node):
        '''_inherit_context(self, node) -> list
        Scan ancestors of attribute and namespace context.  Used only
        for single element node canonicalization, not for subset
        canonicalization.'''

        # Collect the initial list of xml:foo attributes.
        xmlattrs =  [a for a in _attrs(node) if _IN_XML_NS(a)]

        # Walk up and get all xml:XXX attributes we inherit.
        inherited, parent = [], node.getparent()
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not parent and type(node) in ('Element', 'XMLNode',):
                break
            for a in _attrs(parent):
                if not _IN_XML_NS(a):
                    continue
                n = a.localName
                if n not in xmlattrs:
                    xmlattrs.append(n)
                    inherited.append(a)
            parent = parent.parentNode
        return inherited
    self._inherit_context = _inherit_context

    def _do_document(node):
        '''_do_document(self, node) -> None

        Process a document node. documentOrder holds whether the document
        element has been encountered such that PIs/comments can be written
        as specified.'''

        self.documentOrder = _LesserElement
        queue = [(_SerializeState.PRE, (0, node))]
        stack = []
        for _while_ in range(larky.WHILE_LOOP_EMULATION_ITERATION):
            if not queue:
                break
            # print("queue: ", queue)
            state, payload = queue.pop(0)
            if state == _SerializeState.PRE:
                level, child = payload
                # print("!!!", type(child), "==>", child.nodetype())
            # for child in node.iter():
                if child.nodetype() == 'Document':
                    if child != node:
                        fail('TypeError: %s (child: %r)' % (type(child), child))
                    # queue here should be empty
                    if queue:
                        fail("Queue should be empty. More than one document?")
                    queue = [
                        (_SerializeState.PRE, (level + 1, c))
                        for c in child
                    ]
                    continue
                if child.nodetype() == 'ProcessingInstruction':
                    self._do_pi(child)
                elif child.nodetype() == 'Comment':
                    self._do_comment(child)
                elif child.nodetype() == 'DocumentType':
                    pass
                elif type(child) in ('Element', 'XMLNode',):
                    self.documentOrder = _Element        # At document element
                    new_state = self._do_element(child)
                    if child.text:  # b/c we do not have "text" nodes..
                        handlers["Text"](child)
                    children = [
                        (_SerializeState.PRE, (level + 1, c))
                        for c in child
                    ]
                    # if children:
                    #     stack.append(self.state)
                    #     self.state = new_state
                    children.append((_SerializeState.POST, child)) # post visit
                    stack.append(self.state)
                    self.state = new_state
                    queue = children + queue
                else:
                    fail('TypeError: %s (child: %r)' % (type(child), child))
            elif state == _SerializeState.POST:
                # Push state, recurse, pop state.
                if stack:
                    state = stack.pop()
                    self.state = state
                child = payload
                if child.tail:  # b/c we do not have "text" nodes..
                    self._do_tail(child)
                if child.name:
                    self.write('</%s>' % child.name)
                self.documentOrder = _GreaterElement # After document element

    self._do_document = _do_document
    handlers["Document"] = _do_document
    handlers["XMLTree"] = _do_document
    handlers["ElementTree"] = _do_document

    def _do_text(node):
        '''_do_text(self, node) -> None
        Process a text or CDATA node.  Render various special characters
        as their C14N entity representations.'''
        if not _in_subset(self.subset, node): return
        s = node.text.replace("&", "&amp;")
        s = s.replace("<", "&lt;")
        s = s.replace(">", "&gt;")
        s = s.replace("\015", "&#xD;")
        if s: self.write(s)
    self._do_text = _do_text
    handlers["Text"] = _do_text
    handlers["CDATA"] = _do_text

    def _do_tail(node):
        '''_do_tail(self, node) -> None
        Process an Element Node *tail*.  Render various special characters
        as their C14N entity representations.'''
        if not node.tail:
            return
        s = node.tail.replace("&", "&amp;")
        s = s.replace("<", "&lt;")
        s = s.replace(">", "&gt;")
        s = s.replace("\015", "&#xD;")
        if s: self.write(s)
    self._do_tail = _do_tail
    handlers["Tail"] = _do_tail

    def _do_pi(node):
        '''_do_pi(self, node) -> None
        Process a PI node. Render a leading or trailing #xA if the
        document order of the PI is greater or lesser (respectively)
        than the document element.
        '''
        if not _in_subset(self.subset, node): return
        W = self.write
        if self.documentOrder == _GreaterElement: W('\n')
        W('<?')
        # W(node.)
        s = node.text
        if s:
            # W(' ')
            W(s)
        W('?>')
        if self.documentOrder == _LesserElement: W('\n')
    self._do_pi = _do_pi
    handlers["ProcessingInstruction"] = _do_pi

    def _do_comment(node):
        '''_do_comment(self, node) -> None
        Process a comment node. Render a leading or trailing #xA if the
        document order of the comment is greater or lesser (respectively)
        than the document element.
        '''
        if not _in_subset(self.subset, node): return
        if self.comments:
            W = self.write
            if self.documentOrder == _GreaterElement: W('\n')
            W('<!--')
            W(node.text)
            W('-->')
            if self.documentOrder == _LesserElement: W('\n')
    self._do_comment = _do_comment
    handlers["Comment"] = _do_comment

    def _do_attr(n, value):
        ''''_do_attr(self, node) -> None
        Process an attribute.'''

        W = self.write
        W(' ')
        W(n)
        W('="')
        s = value.replace("&", "&amp;")
        s = s.replace("<", "&lt;")
        s = s.replace('"', '&quot;')
        s = s.replace('\011', '&#x9')
        s = s.replace('\012', '&#xA')
        s = s.replace('\015', '&#xD')
        W(s)
        W('"')
    self._do_attr = _do_attr

    def _do_element(node, initial_other_attrs=None):
        '''_do_element(self, node, initial_other_attrs = []) -> None
        Process an element (and its children).'''

        # Get state (from the stack) make local copies.
        #   ns_parent -- NS declarations in parent
        #   ns_rendered -- NS nodes rendered by ancestors
        #        ns_local -- NS declarations relevant to this element
        #   xml_attrs -- Attributes in XML namespace from parent
        #       xml_attrs_local -- Local attributes in XML namespace.
        if initial_other_attrs == None:
            initial_other_attrs = []
        ns_parent, ns_rendered, xml_attrs = \
                self.state[0], dict(**self.state[1]), dict(**self.state[2]) #0422
        ns_local = dict(**ns_parent)
        xml_attrs_local = {}

        # Divide attributes into NS, XML, and others.
        other_attrs = initial_other_attrs[:]
        in_subset = _in_subset(self.subset, node)
        for name, href in node.attrib.items():
            if href == XMLNS.BASE:
                n = name
                if n == "xmlns:": n = "xmlns"        # DOM bug workaround
                fail("if got here?")
                ns_local[n] = "{%s}%s" % (name, href) # no idea..
            elif href == XMLNS.XML:
                if _inclusive(self) or (in_subset and  _in_subset(self.subset, (name, href))): #020925 Test to see if attribute node in subset
                    xml_attrs_local[name] = (name, href) #0426
            else:
                if  _in_subset(self.subset, (name, href)):     #020925 Test to see if attribute node in subset
                    other_attrs.append((name, href))
            # add local xml:foo attributes to ancestor's xml:foo attributes
            xml_attrs.update(xml_attrs_local)

        # Render the node
        W, name = self.write, None
        if in_subset:
            name = node.name
            W('<')
            W(name)

            # Create list of NS attributes to render.
            ns_to_render = []
            for n,v in list(ns_local.items()):

                # If default namespace is XMLNS.BASE or empty,
                # and if an ancestor was the same
                if n == "xmlns" and v in [ XMLNS.BASE, '' ] \
                and ns_rendered.get('xmlns') in [ XMLNS.BASE, '', None ]:
                    continue

                # "omit namespace node with local name xml, which defines
                # the xml prefix, if its string value is
                # http://www.w3.org/XML/1998/namespace."
                if n in ["xmlns:xml", "xml"] \
                and v in [ 'http://www.w3.org/XML/1998/namespace' ]:
                    continue

                # If not previously rendered and it's inclusive or utilized
                if (n,v) not in list(ns_rendered.items()) \
                  and (_inclusive(self) or
                       _utilized(n, node, other_attrs, self.unsuppressedPrefixes)):
                    ns_to_render.append((n, v))

            # Sort and render the ns, marking what was rendered.
            ns_to_render = sorted(ns_to_render, key=cmp_to_key(_sorter_ns))
            for n,v in ns_to_render:
                self._do_attr(n, v)
                ns_rendered[n]=v    #0417

            # If exclusive or the parent is in the subset, add the local xml attributes
            # Else, add all local and ancestor xml attributes
            # Sort and render the attributes.
            if not _inclusive(self) or _in_subset(self.subset, node.getparent()):  #0426
                other_attrs.extend(list(xml_attrs_local.values()))
            else:
                other_attrs.extend(list(xml_attrs.values()))
            other_attrs = sorted(other_attrs, key=cmp_to_key(_sorter))
            for a in other_attrs:
                print(a)
                self._do_attr(a[0], a[1])
            W('>')
        return ns_local, ns_rendered, xml_attrs

    self._do_element = _do_element
    handlers["Element"] = _do_element
    handlers["XMLNode"] = _do_element

    def __init__(node, write, kw):
        '''Create and run the implementation.'''
        self.write = write
        self.subset = kw.get('subset')
        self.comments = kw.get('comments', 0)
        self.unsuppressedPrefixes = kw.get('unsuppressedPrefixes')
        nsdict = kw.get('nsdict', { 'xml': XMLNS.XML, 'xmlns': XMLNS.BASE })

        # Processing state.
        self.state = (nsdict, {'xml':''}, {})  #0422
        # print("xxxxx", type(node), "==>", node.nodetype())
        if node.nodetype() == 'Document':
            self._do_document(node)
        elif type(node) in ('Element', 'XMLNode',):
            self.documentOrder = _Element        # At document element
            if not _inclusive(self):
                self._do_element(node)
            else:
                inherited = self._inherit_context(node)
                self._do_element(node, inherited)
        elif type(node) == 'DocumentType':
            pass
        else:
            fail('TypeError: %s (node: %r)' % (type(node), node))
        return self
    self = __init__(node, write, kw)
    self.handlers = handlers
    return self


def Canonicalize(node, output=None, **kw):
    '''Canonicalize(node, output=None, **kw) -> UTF-8

    Canonicalize a DOM document/element node and all descendents.
    Return the text; if output is specified then output.write will
    be called to output the text and None will be returned
    Keyword parameters:
        nsdict: a dictionary of prefix:uri namespace entries
                assumed to exist in the surrounding context
        comments: keep comments if non-zero (default is 0)
        subset: Canonical XML subsetting resulting from XPath
                (default is [])
        unsuppressedPrefixes: do exclusive C14N, and this specifies the
                prefixes that should be inherited.
    '''
    if output:
        _implementation(*(node, output.write), **kw)
    else:
        s = io.StringIO()
        _implementation(*(node, s.write), **kw)
        return s.getvalue()


c14n = larky.struct(
    __name__='c14n',
    Canonicalize=Canonicalize,
)
# How to use this:
# print("--" * 100)
# sio = StringIO()
# qnames, namespaces = ElementTree._namespaces(root, None)
# c14n.Canonicalize(tree, sio, nsdict=namespaces, comments=1)
# print("ElementC14N.Canonicalize", "\n", sio.getvalue())