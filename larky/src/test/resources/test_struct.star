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
    attributes.remove("to_json")
    attributes.remove("to_proto")
    return {key: getattr(s, key) for key in attributes}

structs = struct(
    to_dict = _to_dict,
)

s = struct(x = 2, y = 3)
v = s.x + getattr(s, "y")
print(v)