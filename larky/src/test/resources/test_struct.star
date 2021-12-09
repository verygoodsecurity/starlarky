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



def _test_structure_callable():

    def __call__(*args, **kwargs):
        return "in call! %s %s" % (args, kwargs)

    cls = larky.mutablestruct(
        frombytes=lambda x: 'frombytes ' + str(x),
        __call__=__call__
    )

    asserts.assert_that(cls.frombytes(1)).is_equal_to("frombytes 1")
    asserts.assert_that(cls(1, 2, foo='cls')).is_equal_to('in call! (1, 2) {"foo": "cls"}')

    cls2 = larky.struct(
        frombytes=lambda x: 'cls2-frombytes ' + str(x),
        __call__=__call__
    )

    asserts.assert_that(cls2.frombytes(1)).is_equal_to("cls2-frombytes 1")
    asserts.assert_that(cls2(1, 2, foo='cls2')).is_equal_to('in call! (1, 2) {"foo": "cls2"}')

    cls3 = larky.struct(
        frombytes=lambda x: 'cls3-frombytes ' + str(x),
    )
    asserts.assert_that(cls3.frombytes(1)).is_equal_to("cls3-frombytes 1")
    asserts.assert_fails(lambda: cls3(1, 2, foo='cls3'), ".*'ImmutableStruct' object is not callable.*")

    cls4 = larky.struct(
        __name__="AnonClass",
        frombytes=lambda x: 'cls4-frombytes ' + str(x),
    )
    asserts.assert_fails(lambda: cls4(1, 2, foo='cls4'), ".*'AnonClass' object is not callable.*")

    cls5 = larky.struct(
        __name__="__Call__NOT_CALLABLE",
        frombytes=lambda x: 'cls5-frombytes ' + str(x),
        __call__=1,
    )
    asserts.assert_fails(
        lambda: cls5(1, 2, foo='cls5'),
        r"'__Call__NOT_CALLABLE' object is not callable.*" +
        r"__call__ is defined but is not callable")


_test_structure_callable()