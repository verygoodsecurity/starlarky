
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")

listElementsMap = {
    None: (frozenset(scopingElements), False),
    "button": (frozenset(scopingElements | {(namespaces["html"], "button")}), False),
    "list": (
        frozenset(
            scopingElements | {(namespaces["html"], "ol"), (namespaces["html"], "ul")}
        ),
        False,
    ),
    "table": (
        frozenset([(namespaces["html"], "html"), (namespaces["html"], "table")]),
        False,
    ),
    "select": (
        frozenset([(namespaces["html"], "optgroup"), (namespaces["html"], "option")]),
        True,
    ),
}
def Node(name):
    """Represents an item in the tree"""
    self = larky.mutablestruct(__name__='Node', __class__=Node)

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

    def __str__():
        attributesStr = " ".join(
            ['%s="%s"' % (name, value) for name, value in self.attributes.items()]
        )
        if attributesStr:
            return "<%s %s>" % (self.name, attributesStr)
        else:
            return "<%s>" % (self.name)
    self.__str__ = __str__

    def __repr__():
        return "<%s>" % (self.name)
    self.__repr__ = __repr__

    def appendChild(node):
        """Insert node as a child of the current node

        :arg node: the node to insert

        """
        # PY2LARKY: pay attention to this!
        return NotImplementedError
    self.appendChild = appendChild

    def insertText(data, insertBefore=None):
        """Insert data as text in the current node, positioned before the
        start of node insertBefore or to the end of the node's text.

        :arg data: the data to insert

        :arg insertBefore: True if you want to insert the text before the node
            and False if you want to insert it after the node

        """
        # PY2LARKY: pay attention to this!
        return NotImplementedError
    self.insertText = insertText

    def insertBefore(node, refNode):
        """Insert node as a child of the current node, before refNode in the
        list of child nodes. Raises ValueError if refNode is not a child of
        the current node

        :arg node: the node to insert

        :arg refNode: the child node to insert the node before

        """
        # PY2LARKY: pay attention to this!
        return NotImplementedError
    self.insertBefore = insertBefore

    def removeChild(node):
        """Remove node from the children of the current node

        :arg node: the child node to remove

        """
        # PY2LARKY: pay attention to this!
        return NotImplementedError
    self.removeChild = removeChild

    def reparentChildren(newParent):
        """Move all the children of the current node to newParent.
        This is needed so that trees that don't store text as nodes move the
        text in the correct way

        :arg newParent: the node to move all this node's children to

        """
        # XXX - should this method be made more general?
        for child in self.childNodes:
            newParent.appendChild(child)
        self.childNodes = []
    self.reparentChildren = reparentChildren

    def cloneNode():
        """Return a shallow copy of the current node i.e. a node with the same
        name and attributes but with no parent or child nodes
        """
        # PY2LARKY: pay attention to this!
        return NotImplementedError
    self.cloneNode = cloneNode

    def hasContent():
        """Return true if the node has children or text, false otherwise"""
        # PY2LARKY: pay attention to this!
        return NotImplementedError
    self.hasContent = hasContent
    return self
def ActiveFormattingElements(name):
    def append(node):
        equalCount = 0
        if node != Marker:
            for element in self[::-1]:
                if element == Marker:
                    break
                if self.nodesEqual(element, node):
                    equalCount += 1
                if equalCount == 3:
                    self.remove(element)
                    break
        list.append(self, node)
    self.append = append

    def nodesEqual(node1, node2):
        if not node1.nameTuple == node2.nameTuple:
            return False

        if not node1.attributes == node2.attributes:
            return False

        return True
    self.nodesEqual = nodesEqual
    return self
def TreeBuilder(namespaceHTMLElements):
    """Base treebuilder implementation

    * documentClass - the class to use for the bottommost node of a document
    * elementClass - the class to use for HTML Elements
    * commentClass - the class to use for comments
    * doctypeClass - the class to use for doctypes

    """

    # pylint:disable=not-callable

    # Document class
    documentClass = None

    # The class to use for creating a node
    elementClass = None

    # The class to use for creating comments
    commentClass = None

    # The class to use for creating doctypes
    doctypeClass = None

    # Fragment class
    fragmentClass = None
    self = larky.mutablestruct(__name__='TreeBuilder', __class__=TreeBuilder)

    def __init__(namespaceHTMLElements):
        """Create a TreeBuilder

        :arg namespaceHTMLElements: whether or not to namespace HTML elements

        """
        if namespaceHTMLElements:
            self.defaultNamespace = "http://www.w3.org/1999/xhtml"
        else:
            self.defaultNamespace = None
        self.reset()
        return self
    self = __init__(namespaceHTMLElements)

    def reset():
        self.openElements = []
        self.activeFormattingElements = ActiveFormattingElements()

        # XXX - rename these to headElement, formElement
        self.headPointer = None
        self.formPointer = None

        self.insertFromTable = False

        self.document = self.documentClass()
    self.reset = reset

    def elementInScope(target, variant=None):

        # If we pass a node in we match that. if we pass a string
        # match any node with that name
        exactNode = hasattr(target, "nameTuple")
        if not exactNode:
            if builtins.isinstance(target, text_type):
                target = (namespaces["html"], target)
            if not (builtins.isinstance(target, tuple)):
                fail("assert builtins.isinstance(target, tuple) failed!")

        listElements, invert = listElementsMap[variant]

        for node in reversed(self.openElements):
            if exactNode and node == target:
                return True
            elif not exactNode and node.nameTuple == target:
                return True
            elif invert ^ (node.nameTuple in listElements):
                return False
        if not (False):
            fail("assert False failed!")
    self.elementInScope = elementInScope

    def reconstructActiveFormattingElements():
        # Within this algorithm the order of steps described in the
        # specification is not quite the same as the order of steps in the
        # code. It should still do the same though.

        # Step 1: stop the algorithm when there's nothing to do.
        if not self.activeFormattingElements:
            return

        # Step 2 and step 3: we start with the last element. So i is -1.
        i = len(self.activeFormattingElements) - 1
        entry = self.activeFormattingElements[i]
        if entry == Marker or entry in self.openElements:
            return
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not entry != Marker and entry not in self.openElements:
                break
            if i == 0:
                # This will be reset to 0 below
                i = -1
                break
            i -= 1
            # Step 5: let entry be one earlier in the list.
            entry = self.activeFormattingElements[i]
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not True:
                break
            # Step 7
            i += 1

            # Step 8
            entry = self.activeFormattingElements[i]
            clone = entry.cloneNode()  # Mainly to get a new copy of the attributes

            # Step 9
            element = self.insertElement(
                {
                    "type": "StartTag",
                    "name": clone.name,
                    "namespace": clone.namespace,
                    "data": clone.attributes,
                }
            )

            # Step 10
            self.activeFormattingElements[i] = element

            # Step 11
            if element == self.activeFormattingElements[-1]:
                break
    self.reconstructActiveFormattingElements = reconstructActiveFormattingElements

    def clearActiveFormattingElements():
        entry = self.activeFormattingElements.pop()
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not self.activeFormattingElements and entry != Marker:
                break
            entry = self.activeFormattingElements.pop()
    self.clearActiveFormattingElements = clearActiveFormattingElements

    def elementInActiveFormattingElements(name):
        """Check if an element exists between the end of the active
        formatting elements and the last marker. If it does, return it, else
        return false"""

        for item in self.activeFormattingElements[::-1]:
            # Check for Marker first because if it's a Marker it doesn't have a
            # name attribute.
            if item == Marker:
                break
            elif item.name == name:
                return item
        return False
    self.elementInActiveFormattingElements = elementInActiveFormattingElements

    def insertRoot(token):
        element = self.createElement(token)
        self.openElements.append(element)
        self.document.appendChild(element)
    self.insertRoot = insertRoot

    def insertDoctype(token):
        name = token["name"]
        publicId = token["publicId"]
        systemId = token["systemId"]

        doctype = self.doctypeClass(name, publicId, systemId)
        self.document.appendChild(doctype)
    self.insertDoctype = insertDoctype

    def insertComment(token, parent=None):
        if parent == None:
            parent = self.openElements[-1]
        parent.appendChild(self.commentClass(token["data"]))
    self.insertComment = insertComment

    def createElement(token):
        """Create an element but don't insert it anywhere"""
        name = token["name"]
        namespace = token.get("namespace", self.defaultNamespace)
        element = self.elementClass(name, namespace)
        element.attributes = token["data"]
        return element
    self.createElement = createElement

    def _getInsertFromTable():
        return self._insertFromTable
    self._getInsertFromTable = _getInsertFromTable

    def _setInsertFromTable(value):
        """Switch the function used to insert an element from the
        normal one to the misnested table one and back again"""
        self._insertFromTable = value
        if value:
            self.insertElement = self.insertElementTable
        else:
            self.insertElement = self.insertElementNormal
    self._setInsertFromTable = _setInsertFromTable

    insertFromTable = property(_getInsertFromTable, _setInsertFromTable)

    def insertElementNormal(token):
        name = token["name"]
        if not (builtins.isinstance(name, text_type)):
            fail("Element %s not unicode" % name)
        namespace = token.get("namespace", self.defaultNamespace)
        element = self.elementClass(name, namespace)
        element.attributes = token["data"]
        self.openElements[-1].appendChild(element)
        self.openElements.append(element)
        return element
    self.insertElementNormal = insertElementNormal

    def insertElementTable(token):
        """Create an element and insert it into the tree"""
        element = self.createElement(token)
        if self.openElements[-1].name not in tableInsertModeElements:
            return self.insertElementNormal(token)
        else:
            # We should be in the InTable mode. This means we want to do
            # special magic element rearranging
            parent, insertBefore = self.getTableMisnestedNodePosition()
            if insertBefore == None:
                parent.appendChild(element)
            else:
                parent.insertBefore(element, insertBefore)
            self.openElements.append(element)
        return element
    self.insertElementTable = insertElementTable

    def insertText(data, parent=None):
        """Insert text data."""
        if parent == None:
            parent = self.openElements[-1]

        if not self.insertFromTable or (
            self.insertFromTable
            and self.openElements[-1].name not in tableInsertModeElements
        ):
            parent.insertText(data)
        else:
            # We should be in the InTable mode. This means we want to do
            # special magic element rearranging
            parent, insertBefore = self.getTableMisnestedNodePosition()
            parent.insertText(data, insertBefore)
    self.insertText = insertText

    def getTableMisnestedNodePosition():
        """Get the foster parent element, and sibling to insert before
        (or None) when inserting a misnested table node"""
        # The foster parent element is the one which comes before the most
        # recently opened table element
        # XXX - this is really inelegant
        lastTable = None
        fosterParent = None
        insertBefore = None
        for elm in self.openElements[::-1]:
            if elm.name == "table":
                lastTable = elm
                break
        if lastTable:
            # XXX - we should really check that this parent is actually a
            # node here
            if lastTable.parent:
                fosterParent = lastTable.parent
                insertBefore = lastTable
            else:
                fosterParent = self.openElements[self.openElements.index(lastTable) - 1]
        else:
            fosterParent = self.openElements[0]
        return fosterParent, insertBefore
    self.getTableMisnestedNodePosition = getTableMisnestedNodePosition

    def generateImpliedEndTags(exclude=None):
        name = self.openElements[-1].name
        # XXX td, th and tr are not actually needed
        if (
            name in frozenset(("dd", "dt", "li", "option", "optgroup", "p", "rp", "rt"))
            and name != exclude
        ):
            self.openElements.pop()
            # XXX This is not entirely what the specification says. We should
            # investigate it more closely.
            self.generateImpliedEndTags(exclude)
    self.generateImpliedEndTags = generateImpliedEndTags

    def getDocument():
        """Return the final tree"""
        return self.document
    self.getDocument = getDocument

    def getFragment():
        """Return the final fragment"""
        # assert self.innerHTML
        fragment = self.fragmentClass()
        self.openElements[0].reparentChildren(fragment)
        return fragment
    self.getFragment = getFragment

    def testSerializer(node):
        """Serialize the subtree of node in the format required by unit tests

        :arg node: the node from which to start serializing

        """
        # PY2LARKY: pay attention to this!
        return NotImplementedError
    self.testSerializer = testSerializer
    return self

