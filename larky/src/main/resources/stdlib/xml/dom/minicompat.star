"""Python version compatibility support for minidom.

This module contains internal implementation details and
should not be imported; use xml.dom.minidom instead.
"""

# This module should only be imported using "import *".
#
# The following names are defined:
#
#   NodeList      -- lightest possible NodeList implementation
#
#   EmptyNodeList -- lightest possible NodeList that is guaranteed to
#                    remain empty (immutable)
#
#   StringTypes   -- tuple of defined string types
#
#   defproperty   -- function used in conjunction with GetattrMagic;
#                    using these together is needed to make them work
#                    as efficiently as possible in both Python 2.2+
#                    and older versions.  For example:
#
#                        class MyClass(GetattrMagic):
#                            def _get_myattr(self):
#                                return something
#
#                        defproperty(MyClass, "myattr",
#                                    "return some value")
#
#                    For Python 2.2 and newer, this will construct a
#                    property object on the class, which avoids
#                    needing to override __getattr__().  It will only
#                    work for read-only attributes.
#
#                    For older versions of Python, inheriting from
#                    GetattrMagic will use the traditional
#                    __getattr__() hackery to achieve the same effect,
#                    but less efficiently.
#
#                    defproperty() should be used for each version of
#                    the relevant _get_<property>() function.

load("@stdlib//larky", larky="larky")
load("@stdlib//xml/dom", NoModificationAllowedErr="NoModificationAllowedErr")
load("@vendor//option/result", Error="Error")

__all__ = ["NodeList", "EmptyNodeList", "StringTypes", "defproperty"]


StringTypes = (str,)

def NodeList():
    self = larky.mutablestruct(__name__='NodeList', __class__=NodeList)

    def item(index):
        if (0 <= index) and (index < len(self)):
            return self[index]
    self.item = item

    def _get_length():
        return len(self)
    self._get_length = _get_length

    def _set_length(value):
        NoModificationAllowedErr("attempt to modify read-only attribute 'length'")
    self._set_length = _set_length

    #: The number of nodes in the NodeList.
    self.length = larky.property(
        _get_length, _set_length
    )

    return self


def EmptyNodeList():
    self = larky.mutablestruct(__name__='EmptyNodeList',
                               __class__=EmptyNodeList)

    def __add__(other):
        NL = NodeList()
        NL.extend(other)
        return NL
    self.__add__ = __add__

    def __radd__(other):
        NL = NodeList()
        NL.extend(other)
        return NL
    self.__radd__ = __radd__

    def item(index):
        return None
    self.item = item

    def _get_length():
        return 0
    self._get_length = _get_length

    def _set_length(value):
        NoModificationAllowedErr("attempt to modify read-only attribute 'length'")
    self._set_length = _set_length

    #: doc="The number of nodes in the NodeList."
    self.length = larky.property(
        _get_length, _set_length
    )
    return self


def defproperty(klass, name, doc):
    get = getattr(klass, ("_get_" + name))

    def set(self, value, name=name):
        NoModificationAllowedErr("attempt to modify read-only attribute " + repr(name))
    if not (not hasattr(klass, "_set_" + name)):
        fail("expected not to find _set_" + name)
    prop = larky.property(get, set)
    setattr(klass, name, prop)

