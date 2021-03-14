load("@stdlib//larky", "larky")
load("@vendor//asserts", "asserts")
load("@stdlib//proto", "proto")


# def _to_dict(s):
#     """Converts a `struct` to a `dict`.
#     Args:
#       s: A `struct`.
#     Returns:
#       A `dict` whose keys and values are the same as the fields in `s`. The
#       transformation is only applied to the struct's fields and not to any
#       nested values.
#     """
#     attributes = dir(s)
#     if "to_json" in attributes:
#         attributes.remove("to_json")
#     if "to_proto" in attributes:
#         attributes.remove("to_proto")
#     return {key: getattr(s, key) for key in attributes}
#
# structs = larky.struct(
#     to_dict = _to_dict,
# )
#
# s = larky.struct(x = 2, y = 3)
# v = s.x + getattr(s, "y")
# print("v is: ", v, " and expected value is 5?: ", v == 5)
#
# print(structs)
# print(structs.to_dict(s))
# print("--" * 5)
#
# pb_struct = larky.struct(field=larky.struct(inner_field=larky.struct(inner_inner_field='text')))
# print(proto.encode_text(pb_struct))
# # Prints:
# # field {
# #   inner_field {
# #     inner_inner_field: "text"
# #   }
# # }


s1 = larky.mutablestruct(state=None)


def _set_data(self, val):
    self.state = val

# Test propertys
def _get_data(self):
    return self.state


c = larky.mutablestruct(
    data=larky.property(
        larky.partial(_get_data, s1),
        larky.partial(_set_data, s1),
    )
)
asserts.assert_that(c.data).is_equal_to(_get_data(s1))
asserts.assert_that(_get_data(s1)).is_equal_to(None)

c.data = {'herpa': '2'}
asserts.assert_that(c.data).is_equal_to(_get_data(s1))
asserts.assert_that(c.data).is_equal_to(s1.state)
asserts.assert_that(c.data).is_equal_to({'herpa': '2'})
