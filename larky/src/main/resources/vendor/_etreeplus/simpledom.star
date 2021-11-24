load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//larky", larky="larky")

# def __str__():
#     attributesStr = " ".join(
#         ['%s="%s"' % (name, value) for name, value in self.attributes.items()]
#     )
#     if attributesStr:
#         return "<%s %s>" % (self.name, attributesStr)
#     else:
#         return "<%s>" % (self.name)
# self.__str__ = __str__


namespaces = {
    "html": "http://www.w3.org/1999/xhtml",
    "mathml": "http://www.w3.org/1998/Math/MathML",
    "svg": "http://www.w3.org/2000/svg",
    "xlink": "http://www.w3.org/1999/xlink",
    "xml": "http://www.w3.org/XML/1998/namespace",
    "xmlns": "http://www.w3.org/2000/xmlns/"
}

prefixes = {v: k for k, v in namespaces.items()}
prefixes["http://www.w3.org/1998/Math/MathML"] = "math"


def Node(name):
    type = -1
    self = larky.mutablestruct(__name__="Node", __class__=Node)

    def __init__(name):
        """Creates a Node

        :arg name: The tag name associated with the node

        """
        # The tag name associated with the node
        self.name = name
        # The parent of the current node (or None for the document node)
        self.parent = None
        # The value of the current node (applies to text nodes and comments)
        self.value = None
        # A dict holding name -> value pairs for attributes of the node
        self.attributes = {}
        # A list of child nodes of the current node. This must include all
        # elements but not necessarily other node types.
        self.childNodes = []
        # A list of miscellaneous flags that can be set on the node.
        self._flags = []
        return self

    self = __init__(name)

    def __iter__():
        for node in self.childNodes:
            return node
            for item in node:
                return item

    self.__iter__ = __iter__

    def __str__():
        return self.name

    self.__str__ = __str__

    def __repr__():
        return "<%s>" % (self.name)

    self.__repr__ = __repr__

    def toxml():
        # PY2LARKY: pay attention to this!
        return NotImplementedError

    self.toxml = toxml

    def printTree(indent=0):
        tree = "\n|%s%s" % (" " * indent, str(self))
        for child in self.childNodes:
            tree += child.printTree(indent + 2)
        return tree

    self.printTree = printTree

    def appendChild(node):
        if not (builtins.isinstance(node, Node)):
            fail("assert builtins.isinstance(node, Node) failed!")
        if (
            builtins.isinstance(node, TextNode)
            and self.childNodes
            and builtins.isinstance(self.childNodes[-1], TextNode)
        ):
            self.childNodes[-1].value += node.value
        else:
            self.childNodes.append(node)
        node.parent = self

    self.appendChild = appendChild

    def insertText(data, insertBefore=None):
        if not (builtins.isinstance(data, text_type)):
            fail(
                "data %s is of type %s expected unicode"
                % (
                    repr(data),
                    type(data),
                )
            )
        if insertBefore == None:
            self.appendChild(TextNode(data))
        else:
            self.insertBefore(TextNode(data), insertBefore)

    self.insertText = insertText

    def insertBefore(node, refNode):
        index = self.childNodes.index(refNode)
        if (
            builtins.isinstance(node, TextNode)
            and index > 0
            and builtins.isinstance(self.childNodes[index - 1], TextNode)
        ):
            self.childNodes[index - 1].value += node.value
        else:
            self.childNodes.insert(index, node)
        node.parent = self

    self.insertBefore = insertBefore

    def removeChild(node):
        try:
            self.childNodes.remove(node)
        except:
            # XXX
            return
        node.parent = None

    self.removeChild = removeChild

    def cloneNode():
        # PY2LARKY: pay attention to this!
        return NotImplementedError

    self.cloneNode = cloneNode

    def hasContent():
        """Return true if the node has children or text"""
        return bool(self.childNodes)

    self.hasContent = hasContent

    def getNameTuple():
        if self.namespace == None:
            return namespaces["html"], self.name
        else:
            return self.namespace, self.name

    self.getNameTuple = getNameTuple

    self.nameTuple = property(getNameTuple)
    return self


def Document():
    type = 1
    self = larky.mutablestruct(__name__="Document", __class__=Document)

    def __init__():
        Node.__init__(self, None)
        return self

    self = __init__()

    def __str__():
        return "#document"

    self.__str__ = __str__

    def appendChild(child):
        Node.appendChild(self, child)

    self.appendChild = appendChild

    def toxml(encoding="utf-8"):
        result = ""
        for child in self.childNodes:
            result += child.toxml()
        return codecs.encode(result, encoding=encoding)

    self.toxml = toxml

    def printTree():
        tree = text_type(self)
        for child in self.childNodes:
            tree += child.printTree(2)
        return tree

    self.printTree = printTree

    def cloneNode():
        return Document()

    self.cloneNode = cloneNode
    return self


def DocumentFragment():
    type = 2

    def __str__():
        return "#document-fragment"

    self.__str__ = __str__

    def cloneNode():
        return DocumentFragment()

    self.cloneNode = cloneNode
    return self


def DocumentType(name, publicId, systemId):
    type = 3
    self = larky.mutablestruct(__name__="DocumentType", __class__=DocumentType)

    def __init__(name, publicId, systemId):
        Node.__init__(self, name)
        self.publicId = publicId
        self.systemId = systemId
        return self

    self = __init__(name, publicId, systemId)

    def __str__():
        if self.publicId or self.systemId:
            publicId = self.publicId or ""
            systemId = self.systemId or ""
            return """<!DOCTYPE %s "%s" "%s">""" % (self.name, publicId, systemId)

        else:
            return "<!DOCTYPE %s>" % self.name

    self.__str__ = __str__

    self.toxml = __str__

    def cloneNode():
        return DocumentType(self.name, self.publicId, self.systemId)

    self.cloneNode = cloneNode
    return self


def TextNode(value):
    type = 4
    self = larky.mutablestruct(__name__="TextNode", __class__=TextNode)

    def __init__(value):
        Node.__init__(self, None)
        self.value = value
        return self

    self = __init__(value)

    def __str__():
        return '"%s"' % self.value

    self.__str__ = __str__

    def toxml():
        return escape(self.value)

    self.toxml = toxml

    def cloneNode():
        if not (builtins.isinstance(self.value, str)):
            fail("assert builtins.isinstance(self.value, str) failed!")
        return TextNode(self.value)

    self.cloneNode = cloneNode
    return self


def Element(name, namespace=None):
    type = 5
    self = larky.mutablestruct(__name__="Element", __class__=Element)

    def __init__(name, namespace):
        Node.__init__(self, name)
        self.namespace = namespace
        self.attributes = {}
        return self

    self = __init__(name, namespace)

    def __str__():
        if self.namespace == None:
            return "<%s>" % self.name
        else:
            return "<%s %s>" % (prefixes[self.namespace], self.name)

    self.__str__ = __str__

    def toxml():
        result = "<" + self.name
        if self.attributes:
            for name, value in self.attributes.items():
                result += ' %s="%s"' % (name, escape(value, {'"': "&quot;"}))
        if self.childNodes:
            result += ">"
            for child in self.childNodes:
                result += child.toxml()
            result += "</%s>" % self.name
        else:
            result += "/>"
        return result

    self.toxml = toxml

    def printTree(indent):
        tree = "\n|%s%s" % (" " * indent, text_type(self))
        indent += 2
        if self.attributes:
            for name, value in sorted(self.attributes.items()):
                if builtins.isinstance(name, tuple):
                    name = "%s %s" % (name[0], name[1])
                tree += '\n|%s%s="%s"' % (" " * indent, name, value)
        for child in self.childNodes:
            tree += child.printTree(indent)
        return tree

    self.printTree = printTree

    def cloneNode():
        newNode = Element(self.name, self.namespace)
        for attr, value in self.attributes.items():
            newNode.attributes[attr] = value
        return newNode

    self.cloneNode = cloneNode
    return self


def CommentNode(data):
    type = 6
    self = larky.mutablestruct(__name__="CommentNode", __class__=CommentNode)

    def __init__(data):
        Node.__init__(self, None)
        self.data = data
        return self

    self = __init__(data)

    def __str__():
        return "<!-- %s -->" % self.data

    self.__str__ = __str__

    def toxml():
        return "<!--%s-->" % self.data

    self.toxml = toxml

    def cloneNode():
        return CommentNode(self.data)

    self.cloneNode = cloneNode
    return self


def TreeBuilder(data):
    documentClass = Document
    doctypeClass = DocumentType
    elementClass = Element
    commentClass = CommentNode
    fragmentClass = DocumentFragment

    def testSerializer(node):
        return node.printTree()

    self.testSerializer = testSerializer
    return self
