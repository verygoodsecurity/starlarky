load("@stdlib//enum", enum="enum")
load("@stdlib//larky", larky="larky")
load("@stdlib//xml/dom", dom="dom")


WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION

Node = dom.Node

_IterWalkState = enum.Enum('_IterWalkState', [
    ('PRE', 0),
    ('POST', 1)
])


def _purge_children(elem):
    q = [(_IterWalkState.PRE, (elem,))]

    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not q:
            break
        state, (node,) = q.pop(0)
        if state == _IterWalkState.PRE:
            cn = node.childNodes[:]
            children = []
            for child in cn:
                if child.nodeType == Node.ELEMENT_NODE:
                    children.append((_IterWalkState.PRE, (child,)))
                node.removeChild(child)
            children = [
                (_IterWalkState.PRE, (e, events, tag,))
                for e in element
            ]
            # post visit
            q = children + q
        elif state == _IterWalkState.POST:
            pass


def ReleaseNode(elem):
    _purge_children(elem)
    if elem.nodeType == Node.ELEMENT_NODE:
        for ctr in range(elem.attributes.length):
            attr = elem.attributes.item(0)
            elem.removeAttributeNode(attr)
            _purge_children(attr)