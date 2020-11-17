# For compatibility help with Python, introduced globals are going to be using
# this as a namespace

larky = _struct(
    struct=_struct,
    mutablestruct=_mutablestruct,
    partial=_partial,
    property=_property,
)