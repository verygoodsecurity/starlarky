# For compatibility help with Python, introduced globals are going to be using
# this as a namespace


def _to_dict(s):
    """Converts a `struct` to a `dict`.
    Args:
      s: A `struct`.
    Returns:
      A `dict` whose keys and values are the same as the fields in `s`. The
      transformation is only applied to the struct's fields and not to any
      nested values.
    """
    # NOTE: This will evaluate properties on getattr(), so it is *IMPORTANT*
    # that if you use this function, to not expect properties to fail()
    #
    # You might be better of using repr() instead!
    attributes = dir(s)
    if "to_json" in attributes:
        attributes.remove("to_json")
    if "to_proto" in attributes:
        attributes.remove("to_proto")
    return {key: getattr(s, key) for key in attributes}


# emulates while loop but will iterate *only* for 100 steps.
WHILE_LOOP_EMULATION_ITERATION = 4096

_SENTINEL = _sentinel()

larky = _struct(
    struct=_struct,
    mutablestruct=_mutablestruct,
    to_dict=_to_dict,
    partial=_partial,
    property=_property,
    WHILE_LOOP_EMULATION_ITERATION=WHILE_LOOP_EMULATION_ITERATION,
    SENTINEL=_SENTINEL
)