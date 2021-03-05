# For compatibility help with Python, introduced globals are going to be using
# this as a namespace
load("@stdlib/assertions", _assertions="assertions")


def _to_dict(s):
    """Converts a `struct` to a `dict`.
    Args:
      s: A `struct`.
    Returns:
      A `dict` whose keys and values are the same as the fields in `s`. The
      transformation is only applied to the struct's fields and not to any
      nested values.
    """
    attributes = dir(s)
    if "to_json" in attributes:
        attributes.remove("to_json")
    if "to_proto" in attributes:
        attributes.remove("to_proto")
    return {key: getattr(s, key) for key in attributes}


def _struct__init__(**kwargs):
    if "to_dict" in kwargs:
        kwargs.remove("to_dict")

    return _struct(to_dict=_to_dict, **kwargs)


larky = _struct(
    struct=_struct__init__,
    mutablestruct=_mutablestruct,
    partial=_partial,
    property=_property,
    assertions=_assertions
)