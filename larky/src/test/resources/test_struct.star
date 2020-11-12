load("@stdlib/larky", "larky")
load("@stdlib/asserts", "asserts")
load("@stdlib/proto", "proto")


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

structs = larky.struct(
    to_dict = _to_dict,
)

s = larky.struct(x = 2, y = 3)
v = s.x + getattr(s, "y")
print("v is: ", v, " and expected value is 5?: ", v == 5)

print(structs)
print(structs.to_dict(s))
print("--" * 5)

pb_struct = larky.struct(field=larky.struct(inner_field=larky.struct(inner_inner_field='text')))
print(proto.encode_text(pb_struct))
# Prints:
# field {
#   inner_field {
#     inner_inner_field: "text"
#   }
# }

# Test descriptors
def _get_data():
    return {'foo': 1}

c = larky.struct(
    data=larky.descriptor(_get_data)
)
asserts.assert_that(c.data).is_equal_to(_get_data())
