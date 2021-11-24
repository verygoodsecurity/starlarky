"""W3C Document Object Model implementation for Python.

The Python mapping of the Document Object Model is documented in the
Python Library Reference in the section on the xml.dom package.

This package contains the following modules:

minidom -- A simple implementation of the Level 1 DOM with namespace
           support added (based on the Level 2 specification) and other
           minor Level 2 functionality.

pulldom -- DOM builder supporting on-demand tree-building for selected
           subtrees of the document.

"""
load("@stdlib//larky", larky="larky")
load("@stdlib//enum", enum="enum")
load("@vendor//option/result", Error="Error")


"""Class giving the NodeType constants."""

# DOM implementations may use this as a base class for their own
# Node implementations (not that Larky does not support "base classes").
#
# If they don't, the constants defined here
# should still be used as the canonical definitions as they match
# the values given in the W3C recommendation.  Client code can
# safely refer to these values in all tests of Node.nodeType
# values.
Node = enum.Enum("Node", dict(
    ELEMENT_NODE=1,
    ATTRIBUTE_NODE=2,
    TEXT_NODE=3,
    CDATA_SECTION_NODE=4,
    ENTITY_REFERENCE_NODE=5,
    ENTITY_NODE=6,
    PROCESSING_INSTRUCTION_NODE=7,
    COMMENT_NODE=8,
    DOCUMENT_NODE=9,
    DOCUMENT_TYPE_NODE=10,
    DOCUMENT_FRAGMENT_NODE=11,
    NOTATION_NODE=12,
).items())


# ExceptionCode
ExceptionCode = enum.Enum("ExceptionCode", dict(
    INDEX_SIZE_ERR=1,
    DOMSTRING_SIZE_ERR=2,
    HIERARCHY_REQUEST_ERR=3,
    WRONG_DOCUMENT_ERR=4,
    INVALID_CHARACTER_ERR=5,
    NO_DATA_ALLOWED_ERR=6,
    NO_MODIFICATION_ALLOWED_ERR=7,
    NOT_FOUND_ERR=8,
    NOT_SUPPORTED_ERR=9,
    INUSE_ATTRIBUTE_ERR=10,
    INVALID_STATE_ERR=11,
    SYNTAX_ERR=12,
    INVALID_MODIFICATION_ERR=13,
    NAMESPACE_ERR=14,
    INVALID_ACCESS_ERR=15,
    VALIDATION_ERR=16,
).items())


INDEX_SIZE_ERR = ExceptionCode.INDEX_SIZE_ERR
DOMSTRING_SIZE_ERR = ExceptionCode.DOMSTRING_SIZE_ERR
HIERARCHY_REQUEST_ERR = ExceptionCode.HIERARCHY_REQUEST_ERR
WRONG_DOCUMENT_ERR = ExceptionCode.WRONG_DOCUMENT_ERR
INVALID_CHARACTER_ERR = ExceptionCode.INVALID_CHARACTER_ERR
NO_DATA_ALLOWED_ERR = ExceptionCode.NO_DATA_ALLOWED_ERR
NO_MODIFICATION_ALLOWED_ERR = ExceptionCode.NO_MODIFICATION_ALLOWED_ERR
NOT_FOUND_ERR = ExceptionCode.NOT_FOUND_ERR
NOT_SUPPORTED_ERR = ExceptionCode.NOT_SUPPORTED_ERR
INUSE_ATTRIBUTE_ERR = ExceptionCode.INUSE_ATTRIBUTE_ERR
INVALID_STATE_ERR = ExceptionCode.INVALID_STATE_ERR
SYNTAX_ERR = ExceptionCode.SYNTAX_ERR
INVALID_MODIFICATION_ERR = ExceptionCode.INVALID_MODIFICATION_ERR
NAMESPACE_ERR = ExceptionCode.NAMESPACE_ERR
INVALID_ACCESS_ERR = ExceptionCode.INVALID_ACCESS_ERR
VALIDATION_ERR = ExceptionCode.VALIDATION_ERR


def _validate_clsname(cls):
    # todo: check if cls is valid identifier
    return cls


def _DOMException(cls, code, *args, **kw):
    """Abstract base class for DOM exceptions.
    Exceptions with specific codes are specializations of this class."""
    clsname = larky.impl_function_name(cls)
    if clsname == 'DOMException':
        fail("RuntimeError: DOMException should not be instantiated directly")

    def _clsname(clsname):
        clsname = _validate_clsname(clsname)
        return 'DOMException.' + clsname

    return larky.struct(
        __name__=_clsname(clsname),
        __class__=cls,
        code = larky.property(lambda : code),
        __call__= lambda *args: fail("%s: %s" % (clsname, args))
    )


def IndexSizeErr(*args, **kwargs):
    return _DOMException(IndexSizeErr, INDEX_SIZE_ERR)(*args)

def DomstringSizeErr(*args, **kwargs):
    return _DOMException(DomstringSizeErr, DOMSTRING_SIZE_ERR)(*args)

def HierarchyRequestErr(*args, **kwargs):
    return _DOMException(HierarchyRequestErr, HIERARCHY_REQUEST_ERR)(*args)

def WrongDocumentErr(*args, **kw):
    return _DOMException(WrongDocumentErr, WRONG_DOCUMENT_ERR)(*args)

def InvalidCharacterErr(*args, **kw):
    return _DOMException(InvalidCharacterErr, INVALID_CHARACTER_ERR)(*args)

def NoDataAllowedErr(*args, **kw):
    return _DOMException(NoDataAllowedErr, NO_DATA_ALLOWED_ERR)(*args)

def NoModificationAllowedErr(*args, **kw):
    return _DOMException(NoModificationAllowedErr, NO_MODIFICATION_ALLOWED_ERR)(*args)

def NotFoundErr(*args, **kw):
    return _DOMException(NotFoundErr, NOT_FOUND_ERR)(*args)

def NotSupportedErr(*args, **kw):
    return _DOMException(NotSupportedErr, NOT_SUPPORTED_ERR)(*args)

def InuseAttributeErr(*args, **kw):
    return _DOMException(InuseAttributeErr, INUSE_ATTRIBUTE_ERR)(*args)

def InvalidStateErr(*args, **kw):
    return _DOMException(InvalidStateErr, INVALID_STATE_ERR)(*args)

def SyntaxErr(*args, **kw):
    return _DOMException(SyntaxErr, SYNTAX_ERR)(*args)

def InvalidModificationErr(*args, **kw):
    return _DOMException(InvalidModificationErr, INVALID_MODIFICATION_ERR)(*args)

def NamespaceErr(*args, **kw):
    return _DOMException(NamespaceErr, NAMESPACE_ERR)(*args)

def InvalidAccessErr(*args, **kw):
    return _DOMException(InvalidAccessErr, INVALID_ACCESS_ERR)(*args)

def ValidationErr(*args, **kw):
    return _DOMException(ValidationErr, VALIDATION_ERR)(*args)


"""Class giving the operation constants for UserDataHandler.handle()."""
# Based on DOM Level 3 (WD 9 April 2002)
UserDataHandler = enum.Enum('UserDataHandler', dict(
    NODE_CLONED = 1,
    NODE_IMPORTED = 2,
    NODE_DELETED = 3,
    NODE_RENAMED = 4,
))


XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"
XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/"
XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml"
EMPTY_NAMESPACE = None
EMPTY_PREFIX = None


dom = larky.struct(
    __name__='dom',
    Node=Node,
    ExceptionCode=ExceptionCode,
    INDEX_SIZE_ERR=INDEX_SIZE_ERR,
    DOMSTRING_SIZE_ERR=DOMSTRING_SIZE_ERR,
    HIERARCHY_REQUEST_ERR=HIERARCHY_REQUEST_ERR,
    WRONG_DOCUMENT_ERR=WRONG_DOCUMENT_ERR,
    INVALID_CHARACTER_ERR=INVALID_CHARACTER_ERR,
    NO_DATA_ALLOWED_ERR=NO_DATA_ALLOWED_ERR,
    NO_MODIFICATION_ALLOWED_ERR=NO_MODIFICATION_ALLOWED_ERR,
    NOT_FOUND_ERR=NOT_FOUND_ERR,
    NOT_SUPPORTED_ERR=NOT_SUPPORTED_ERR,
    INUSE_ATTRIBUTE_ERR=INUSE_ATTRIBUTE_ERR,
    INVALID_STATE_ERR=INVALID_STATE_ERR,
    SYNTAX_ERR=SYNTAX_ERR,
    INVALID_MODIFICATION_ERR=INVALID_MODIFICATION_ERR,
    NAMESPACE_ERR=NAMESPACE_ERR,
    INVALID_ACCESS_ERR=INVALID_ACCESS_ERR,
    VALIDATION_ERR=VALIDATION_ERR,
    IndexSizeErr=IndexSizeErr,
    DomstringSizeErr=DomstringSizeErr,
    HierarchyRequestErr=HierarchyRequestErr,
    WrongDocumentErr=WrongDocumentErr,
    InvalidCharacterErr=InvalidCharacterErr,
    NoDataAllowedErr=NoDataAllowedErr,
    NoModificationAllowedErr=NoModificationAllowedErr,
    NotFoundErr=NotFoundErr,
    NotSupportedErr=NotSupportedErr,
    InuseAttributeErr=InuseAttributeErr,
    InvalidStateErr=InvalidStateErr,
    SyntaxErr=SyntaxErr,
    InvalidModificationErr=InvalidModificationErr,
    NamespaceErr=NamespaceErr,
    InvalidAccessErr=InvalidAccessErr,
    ValidationErr=ValidationErr,
    UserDataHandler=UserDataHandler,
    XML_NAMESPACE=XML_NAMESPACE,
    XMLNS_NAMESPACE=XMLNS_NAMESPACE,
    XHTML_NAMESPACE=XHTML_NAMESPACE,
    EMPTY_NAMESPACE=EMPTY_NAMESPACE,
    EMPTY_PREFIX=EMPTY_PREFIX,
)