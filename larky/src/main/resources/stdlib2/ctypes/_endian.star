def _other_endian(typ):
    """
    Return the type with the 'other' byte order.  Simple types like
        c_int and so on already have __ctype_be__ and __ctype_le__
        attributes which contain the types, for more complicated types
        arrays and structures are supported.
    
    """
def _swapped_meta(type(Structure)):
    """
    _fields_
    """
    def BigEndianStructure(Structure, metadef=_swapped_meta):
    """
    Structure with big endian byte order
    """
    def LittleEndianStructure(Structure, metadef=_swapped_meta):
    """
    Structure with little endian byte order
    """
