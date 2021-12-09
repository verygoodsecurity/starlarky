#
# ElementTree
# $Id: ElementPath.py 3375 2008-02-13 08:05:08Z fredrik $
#
# limited xpath support for element trees
#
# history:
# 2003-05-23 fl   created
# 2003-05-28 fl   added support for // etc
# 2003-08-27 fl   fixed parsing of periods in element names
# 2007-09-10 fl   new selection engine
# 2007-09-12 fl   fixed parent selector
# 2007-09-13 fl   added iterfind; changed findall to return a list
# 2007-11-30 fl   added namespaces support
# 2009-10-30 fl   added child element value filter
#
# Copyright (c) 2003-2009 by Fredrik Lundh.  All rights reserved.
#
# fredrik@pythonware.com
# http://www.pythonware.com
#
# --------------------------------------------------------------------
# The ElementTree toolkit is
#
# Copyright (c) 1999-2009 by Fredrik Lundh
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

# Licensed to PSF under a Contributor Agreement.
# See http://www.python.org/psf/license for licensing details.

##
# Implementation module for XPath support.  There's usually no reason
# to import this module directly; the <b>ElementTree</b> does this for
# you, if needed.
##
load("@stdlib//larky", larky="larky")
load("@stdlib//re", re="re")
load("@stdlib//types", types="types")
load("@vendor//option/result", Error="Error")

_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION


xpath_tokenizer_re = re.compile(r"".join([
    r"(",
    r"'[^']*'|\"[^\"]*\"|",
    r"::|",
    r"//?|",
    r"\.\.|",
    r"\(\)|",
    r"!=|",
    r"[/.*:\[\]\(\)@=])|",
    r"((?:\{[^}]+\})?[^/\[\]\(\)@!=\s]+)|",
    r"\s+",
]))


StopIterating = StopIteration


def generator(seq):
    self = larky.mutablestruct(__class__='generator')
    def __init__(seq):
        self._sequence = seq
        self._length = len(seq)
        self._current = -1
        return self
    self = __init__(seq)

    def next():
        if self._current >= self._length-1:
            return StopIterating
        self._current += 1
        return self._sequence[self._current]
    self.next = next
    self.__next__ = next
    return self


def xpath_tokenizer(pattern, namespaces=None):
    default_namespace = namespaces.get('') if namespaces else None
    parsing_attribute = False
    result = []
    for token in xpath_tokenizer_re.findall(pattern):
        ttype, tag = token
        if tag and tag[0] != "{":
            if ":" in tag:
                prefix, uri = tag.split(":", 1)
                if not namespaces:
                    return Error(
                        "SyntaxError: prefix %r not found in prefix map" %
                         prefix
                    ).unwrap()
                result.append((ttype, "{%s}%s" % (namespaces[prefix], uri)))
            elif default_namespace and not parsing_attribute:
                result.append((ttype, "{%s}%s" % (default_namespace, tag)))
            else:
                result.append(token)
            parsing_attribute = False
        else:
            result.append(token)
            parsing_attribute = ttype == '@'
    return generator(result)


def get_parent_map(context):
    parent_map = context.parent_map
    if parent_map == None:
        context.parent_map = {}
        parent_map = context.parent_map
        # for p in context.root.iter():
        #     for e in p:
        #         parent_map[e] = p
        qu = [context.root]
        for _ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if len(qu) == 0:
                break
            parent = qu.pop(0)
            for e in parent._children:
                # t = tuple(sorted(larky.to_dict(e).items()))
                # could not use tuple as dict key somehow, err: unhashable type: 'dict'
                k = str(sorted(larky.to_dict(e).items()))
                parent_map[k] = parent
            qu.extend(parent._children)
    return parent_map


def _is_wildcard_tag(tag):
    return tag[:3] == '{*}' or tag[-2:] == '}*'


def _prepare_tag(tag):
    _isinstance, _str = types.is_instance, str
    if tag == '{*}*':
        # Same as '*', but no comments or processing instructions.
        # It can be a surprise that '*' includes those, but there is no
        # justification for '{*}*' doing the same.
        def select(context, result):
            result = []
            for elem in result:
                if _isinstance(elem.tag, _str):
                    result.append(elem)
            return result
    elif tag == '{}*':
        # Any tag that is not in a namespace.
        def select(context, result):
            result = []
            for elem in result:
                el_tag = elem.tag
                if _isinstance(el_tag, _str) and el_tag[0] != '{':
                    result.append(elem)
            return result
    elif tag[:3] == '{*}':
        # The tag in any (or no) namespace.
        suffix = tag[2:]  # '}name'
        # no_ns = slice(-len(suffix), None)
        tag = tag[3:]
        def select(context, result):
            result = []
            for elem in result:
                el_tag = elem.tag
                if el_tag == tag or _isinstance(el_tag, _str) and el_tag[-len(suffix):] == suffix:
                    result.append(elem)
            return result
    elif tag[-2:] == '}*':
        # Any tag in the given namespace.
        ns = tag[:-1]
        # ns_only = slice(None, len(ns))
        def select(context, result):
            result = []
            for elem in result:
                el_tag = elem.tag
                if _isinstance(el_tag, _str) and el_tag[:len(ns)] == ns:
                    result.append(elem)
            return result
    else:
        return Error("internal parser error, got %s" % tag).unwrap()
    return select


def prepare_child(peekable, token):
    tag = token[1]
    if _is_wildcard_tag(tag):
        select_tag = _prepare_tag(tag)
        def select(context, result):
            def select_child(result):
                return list(result)
            return select_tag(context, select_child(result))
    else:
        if tag[:2] == '{}':
            tag = tag[2:]  # '{}tag' == 'tag'
        def select(context, result):
            rval = []
            for elem in result:
                #for e in elem:
                for e in elem._children:
                    needle = e.tag
                    if type(needle) == "QName":  # to avoid circular imports..
                        needle = needle.text
                    if needle == tag:
                        rval.append(e)
            return rval
    return select

def prepare_star(peekable, token):
    def select(context, result):
        # return list(result)
        rval = []
        for elem in result:
            for e in elem._children:
                rval.append(e)
        return rval
    return select


def prepare_self(peekable, token):
    def select(context, result):
        return list(result)
    return select


def traverse_descendant(e, tag, rval):
    qu = e._children[0:] # duplicate arr
    for _ in range(_WHILE_LOOP_EMULATION_ITERATION):
        if len(qu) == 0:
            break
        current = qu.pop(0)
        if tag == None or tag == '*' or current.tag == tag:
            rval.append(current)
        qu.extend(current._children)


def prepare_descendant(peekable, token):
    token = peekable.next()
    if not token or token == StopIterating:
        return
    if token[0] == "*":
        tag = "*"
    # elif not token[0]:
    elif token[0] == "None":
        tag = token[1]
    else:
        return Error("SyntaxError: invalid descendant").unwrap()

    if _is_wildcard_tag(tag):
        select_tag = _prepare_tag(tag)
        def select(context, result):
            def select_child(result):
                rval = []
                for e in result:
                    # for e in elem.iter():
                    #     if e != elem:
                    #         rval.append(e)
                    traverse_descendant(e, None, rval)
                return rval
            return select_tag(context, select_child(result))
    else:
        if tag[:2] == '{}':
            tag = tag[2:]  # '{}tag' == 'tag'
        def select(context, result):
            rval = []
            for e in result:
                # for e in elem.iter(tag):
                # if e != elem:
                #     rval.append(e)
                traverse_descendant(e, tag, rval)
            return rval
    return select


def prepare_parent(peekable, token):
    def select(context, result):
        # FIXME: raise error if .. is applied at toplevel?
        rval = []
        parent_map = get_parent_map(context)
        result_map = {}
        for elem in result:
            if elem in parent_map:
                parent = parent_map[elem]
                if parent not in result_map:
                    result_map[parent] = None
                    rval.append(parent)
        return rval
    return select


def prepare_predicate(peekable, token):
    # FIXME: replace with real parser!!! refs:
    # http://javascript.crockford.com/tdop/tdop.html
    signature = []
    predicate = []
    # print("old token:", token)
    for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
        token = peekable.next()
        if not token or token == StopIterating:
            return
        # print('token?', token)
        if token[0] == "]":
            peeked = peekable.peek()
            if not peeked or peeked == StopIterating:
                break

            p_ttype, p_tag = peeked
            if p_ttype != '=':
                break
            # print('peeked!', peeked)
        if token == ('None', 'None'):
            # ignore whitespace
            continue
        if token[0] and token[0][:1] in "'\"":
            token = "'", token[0][1:-1]
        # signature.append(token[0] or "-")
        if (not token[0]) or (token[0] == "None"):
            signature.append("-")
        else:
            signature.append(token[0])
        predicate.append(token[1])
    signature = "".join(signature)
    # print("===> predicate:", predicate)
    # print("===> signature:", signature)
    # print("===> token:", token)
    # use signature to determine predicate type
    if signature == "@-":
        # [@attribute] predicate
        key = predicate[1]
        def select(context, result):
            rval = []
            for elem in result:
                if elem.get(key) != None:
                    rval.append(elem)
            return rval
        return select
    if signature == "@-='" or signature == "@-!='":
        # [@attribute='value'] or [@attribute!='value']
        key = predicate[1]
        value = predicate[-1]
        def select(context, result):
            rval = []
            for elem in result:
                if elem.get(key) == value:
                    rval.append(elem)
            return rval
        def select_negated(context, result):
            rval = []
            for elem in result:
                attr_value = elem.get(key)
                if attr_value != None and attr_value != value:
                    rval.append(elem)
            return rval
        return select_negated if '!=' in signature else select
    if signature == "-" and not re.match(r"\-?\d+$", predicate[0]):
        # [tag]
        tag = predicate[0]
        def select(context, result):
            rval = []
            for elem in result:
                if elem.find(tag) != None:
                    rval.append(elem)
            return rval
        return select
    if signature == ".='" or signature == ".!='" or (
            (signature == "-='" or signature == "-!='")
            and not re.match(r"\-?\d+$", predicate[0])):
        # [.='value'] or [tag='value'] or [.!='value'] or [tag!='value']
        tag = predicate[0]
        value = predicate[-1]
        if tag:
            def select(context, result):
                rval = []
                for elem in result:
                    for e in elem.findall(tag):
                        if "".join(e.itertext()) == value:
                            rval.append(elem)
                            break
                return rval
            def select_negated(context, result):
                rval = []
                for elem in result:
                    for e in elem.iterfind(tag):
                        if "".join(e.itertext()) != value:
                            rval.append(elem)
                            break
                return rval
        else:
            def select(context, result):
                rval = []
                for elem in result:
                    if "".join(elem.itertext()) == value:
                        rval.append(elem)
                return rval
            def select_negated(context, result):
                rval = []
                for elem in result:
                    if "".join(elem.itertext()) != value:
                        rval.append(elem)
                return rval
        return select_negated if '!=' in signature else select
    if signature == "-" or signature == "-()" or signature == "-()-":
        # [index] or [last()] or [last()-index]
        if signature == "-":
            # [index]
            index = int(predicate[0]) - 1
            if index < 0:
                return Error("SyntaxError: XPath position >= 1 expected").unwrap()
        else:
            if predicate[0] != "last":
                return Error("SyntaxError: unsupported function").unwrap()
            if signature == "-()-":
                if not types.is_int(predicate[2]):
                    return Error("SyntaxError: unsupported expression").unwrap()
                index = int(predicate[2]) - 1
                if index > -2:
                    return Error("SyntaxError: XPath offset from last() must be negative").unwrap()
            else:
                index = -1
        def select(context, result):
            parent_map = get_parent_map(context)
            rval = []
            for elem in result:
                k = str(sorted(larky.to_dict(elem).items()))
                if k not in parent_map:
                    continue
                parent = parent_map[k]
                # FIXME: what if the selector is "*" ?
                # recursion, need to convert:
                # elems = list(parent.findall(elem.tag))
                elems = []
                for e in parent._children:
                    if e.tag == elem.tag:
                        elems.append(e)
                if elem not in elems or index >= len(elems):
                    continue
                if elems[index] == elem:
                    rval.append(elem)
            return rval
        return select
    if signature == "@*[-()=']='":
        # [@attribute[local-name() = 'KEY' ]='VALUE']
        xpath_function = predicate[3]
        xpath_funcargs = [] if predicate[4] == "None" else predicate[4]
        # print(xpath_funcargs)
        # print(signature.find("'"))
        key = predicate[signature.find("'") - 1]
        value = predicate[-1]
        def select(context, result):
            # print("func", xpath_function, "args", xpath_funcargs)
            # print("key", key, "value", value)
            # print("context", context, repr(context))
            # print("result", result)
            rval = []
            for elem in result:
                context.node = elem
                attrs = eval_function(xpath_function, [[elem.attrib]], context)
                # print(attrs)
                if attrs.get(key) == value:
                    rval.append(elem)
            return rval
        return select
    return fail("SyntaxError: invalid predicate")


def eval_function(xpath_func_name, xpath_func_args, context):
    if xpath_func_name == 'local-name':
        if xpath_func_args:
            # fail("function arguments are not support for now")
            node_set = xpath_func_args[0]
            if len(node_set) == 0:
                return ""
            node = node_set[0]
        else:
            node = context.node

        if node == context.root:
            uri, local_part = "", ""
        elif iselement(node):
            uri, local_part = get_expanded_name(node.tag)
        elif types.is_dict(node):
            _node = {}
            for k, v in node.items():
                _, lp = get_expanded_name(k)
                _node[lp] = v
            return _node
        else:
            print(node, type(node))
            # Text
            fail('%s() not supported for this node type.' % xpath_func_name)

        if xpath_func_name == "local-name":
            return local_part
        elif xpath_func_name == "namespace-uri":
            return uri
        else:
            if xpath_func_name != "name":
                fail("xpath_func_name != 'name'")
            if uri:
                fail('name() not supported for qualified names.')
            else:
                return local_part


def iselement(element):
    """iselement(element)
    Checks if an object appears to be a valid element object or
    if *element* appears to be an Element.
    """
    return type(element) == 'Element' or hasattr(element, "tag")


def is_node_set(x):
    return types.is_list(x) or types.is_tuple(x) or iselement(x)


def get_expanded_name(tag):
    if tag[:1] != "{":
        return (None, tag)
    else:
        return tag[1:].split("}", 1)


def get_attribute_value(node, uri, name):
    if iselement(node):
        if uri:
            name = "{%s}%s" % (uri, name)
        return node.get(name)
    else:
        return None

ops = {
    "None": prepare_child,
    "*": prepare_star,
    ".": prepare_self,
    "..": prepare_parent,
    "//": prepare_descendant,
    "[": prepare_predicate,
    }


_cache = {}


def SelectorContext(root):
    self = larky.mutablestruct(__class__='_SelectorContext')
    def __init__(root):
        self.root = root
        self.node = None
        self.__parentmap = None
        return self
    self = __init__(root)

    def _get_parent_map():
        if self.__parentmap != None:
            return self.__parentmap
        hasparent = getattr(self.root, 'getparent', None)
        if hasparent:
            self.__parentmap = hasparent()
            return self.__parentmap

    def _set_parent_map(parent_map):
        self.__parentmap = parent_map

    self.parent_map = larky.property(_get_parent_map, _set_parent_map)
    return self

# --------------------------------------------------------------------

##
# Generate all matching objects.

def iterfind(start_elem, path, namespaces=None):
    # compile selector pattern
    if path[-1:] == "/":
        path = path + "*"  # implicit all (FIXME: keep this?)

    cache_key = (path,)
    if namespaces:
        cache_key += tuple(sorted(namespaces.items()))

    if cache_key in _cache:
        selector = _cache[cache_key]
    else:
        if len(_cache) > 100:
            _cache.clear()
        if path[:1] == "/":
            return Error("SyntaxError: cannot use absolute path on element").unwrap()
        tokenizer = larky.utils.Peekable(xpath_tokenizer(path, namespaces))
        token = tokenizer.next()
        selector = []
        for _ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if token == StopIterating:
                break
            rval = ops[token[0]](tokenizer, token)
            if rval == StopIterating:
                return Error("SyntaxError: invalid path").unwrap()
            selector.append(rval)
            token = tokenizer.next()
            if token == StopIterating:
                break
            if token[0] == "/":
                token = tokenizer.next()
                if token == StopIterating:
                    break
            # _cache[cache_key] = selector
    result = [start_elem]
    context = SelectorContext(start_elem)
    for select in selector:
        result = select(context, result)
    return result

##
# Find first matching object.

def find(elem, path, namespaces=None):
    rval = iterfind(elem, path, namespaces)
    if rval == StopIterating:
       return rval
    if len(rval) != 0:
        return rval[0]

##
# Find all matching objects.

def findall(elem, path, namespaces=None):
    return iterfind(elem, path, namespaces)

##
# Find text for first matching object.

def findtext(elem, path, default=None, namespaces=None):
    rval = find(elem, path, namespaces)
    if rval == StopIterating:
        return default
    return rval.text or ""


ElementPath = larky.struct(
    iterfind=iterfind,
    find=find,
    findall=findall,
    findtext=findtext,
    generator=generator,
    xpath_tokenizer=xpath_tokenizer,
)