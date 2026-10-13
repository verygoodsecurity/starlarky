"""Properties and Starlark-defined descriptors on types.new_class classes."""
load("@stdlib//larky", "larky")
load("@stdlib//types", "types")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


def _new_class(name, fields, bases=()):
    return types.new_class(name, bases, {}, lambda ns: ns.update(fields))


def _init(self, value):
    self._value = value


def _set_double(self, value):
    self._value = value * 2


def _test_property_getter_and_class_access():
    prop = larky.property(lambda self: self._value)
    Foo = _new_class("Foo", {"__init__": _init, "name": prop})
    asserts.assert_true(Foo("bob").name == "bob")
    asserts.assert_true(Foo.name == prop)


def _test_property_setter():
    prop = larky.property(lambda self: self._value, _set_double)
    Temp = _new_class("Temp", {"__init__": _init, "x": prop})
    t = Temp(0)
    t.x = 21
    asserts.assert_that(t.x).is_equal_to(42)
    asserts.assert_true(Temp.x == prop)
    # A data descriptor takes precedence even over an existing instance entry.
    t.__dict__["x"] = "shadow"
    asserts.assert_that(t.x).is_equal_to(42)


def _test_property_readonly():
    ReadOnly = _new_class("ReadOnly", {"x": larky.property(lambda self: 7)})
    obj = ReadOnly()
    def assign():
        obj.x = 3
    asserts.assert_fails(assign, ".*AttributeError: can't set attribute.*")
    asserts.assert_that(obj.x).is_equal_to(7)


def _test_property_delete():
    for setter in [None, _set_double]:
        Foo = _new_class("Foo", {"x": larky.property(lambda self: 7, setter)})
        obj = Foo()
        # Starlark has no del statement; use the class machinery's deletion entry point.
        asserts.assert_fails(lambda: obj.__delattr__("x"), ".*AttributeError: can't delete attribute.*")
        asserts.assert_that(obj.x).is_equal_to(7)


def _test_property_inheritance_and_override():
    prop = larky.property(lambda self: self._value, _set_double)
    Base = _new_class("Base", {"__init__": _init, "x": prop})
    Child = _new_class("Child", {}, (Base,))
    obj = Child(5)
    asserts.assert_that(obj.x).is_equal_to(5)
    obj.x = 3
    asserts.assert_that(obj.x).is_equal_to(6)
    asserts.assert_true(Child.x == prop)
    Override = _new_class("Override", {"x": larky.property(lambda self: "override")}, (Base,))
    asserts.assert_that(Override(5).x).is_equal_to("override")
    PlainOverride = _new_class("PlainOverride", {"x": "plain"}, (Base,))
    asserts.assert_that(PlainOverride(5).x).is_equal_to("plain")


def _test_mutablestruct_property_regression():
    obj = larky.mutablestruct(_value=4)
    def getter():
        return obj._value
    def setter(value):
        obj._value = value * 2
    obj.x = larky.property(getter, setter)
    asserts.assert_that(obj.x).is_equal_to(4)
    obj.x = 3
    asserts.assert_that(obj.x).is_equal_to(6)
    obj.readonly = larky.property(getter)
    asserts.assert_that(obj.readonly).is_equal_to(6)
    def assign():
        obj.readonly = 9
    asserts.assert_fails(assign, ".*does not define a setter.*")


def _check_non_data_descriptor(desc):
    Foo = _new_class("Foo", {"__init__": _init, "x": desc})
    obj = Foo(5)
    asserts.assert_that(obj.x).is_equal_to((5, Foo))
    obj.x = "shadow"
    asserts.assert_that(obj.x).is_equal_to("shadow")


def _test_mutablestruct_non_data_descriptor():
    _check_non_data_descriptor(larky.mutablestruct(__get__=lambda obj, objtype: (obj._value, objtype)))


def _test_struct_non_data_descriptor():
    _check_non_data_descriptor(larky.struct(__get__=lambda obj, objtype: (obj._value, objtype)))


def _test_dict_non_data_descriptor():
    _check_non_data_descriptor({"__get__": lambda obj, objtype: (obj._value, objtype)})


def _check_data_descriptor(desc):
    Foo = _new_class("Foo", {"__init__": _init, "x": desc})
    obj = Foo(5)
    asserts.assert_that(obj.x).is_equal_to(5)
    obj.x = 3
    asserts.assert_that(obj.x).is_equal_to(6)
    obj.__dict__["x"] = "shadow"
    asserts.assert_that(obj.x).is_equal_to(6)


def _test_mutablestruct_data_descriptor():
    _check_data_descriptor(larky.mutablestruct(__get__=lambda obj, objtype: obj._value, __set__=_set_double))


def _test_struct_data_descriptor():
    _check_data_descriptor(larky.struct(__get__=lambda obj, objtype: obj._value, __set__=_set_double))


def _test_dict_data_descriptor():
    _check_data_descriptor({"__get__": lambda obj, objtype: obj._value, "__set__": _set_double})


def _test_starlark_descriptor_delete():
    # Preserve the existing deletion calling convention: (obj, attribute_name).
    def delete(obj, name):
        obj.deleted = name
    for desc in [
        larky.mutablestruct(__get__=lambda obj, objtype: 7, __delete__=delete),
        larky.struct(__get__=lambda obj, objtype: 7, __delete__=delete),
        {"__get__": lambda obj, objtype: 7, "__delete__": delete},
    ]:
        Foo = _new_class("Foo", {"x": desc})
        obj = Foo()
        obj.__delattr__("x")
        asserts.assert_that(obj.deleted).is_equal_to("x")


def _descriptor_variants(fields):
    return [larky.mutablestruct(**fields), larky.struct(**fields), dict(fields)]


def _record_delete(obj, name):
    obj.deleted = name


def _test_data_descriptor_without_getter():
    for fields in [{"__set__": _set_double}, {"__delete__": _record_delete}]:
        for desc in _descriptor_variants(fields):
            Foo = _new_class("Foo", {"__init__": _init, "x": desc})
            obj = Foo(5)
            asserts.assert_true(obj.x == desc)
            asserts.assert_true(Foo.x == desc)
            obj.__dict__["x"] = "instance value"
            asserts.assert_that(obj.x).is_equal_to("instance value")


def _test_data_descriptor_without_setter():
    for desc in _descriptor_variants({"__get__": lambda obj, objtype: obj._value, "__delete__": _record_delete}):
        Foo = _new_class("Foo", {"__init__": _init, "x": desc})
        obj = Foo(5)
        obj.__dict__["x"] = "instance value"
        def assign():
            obj.x = 3
        asserts.assert_fails(assign, ".*AttributeError: object has no __set__ method.*")
        asserts.assert_that(obj.__dict__["x"]).is_equal_to("instance value")
        asserts.assert_that(obj.x).is_equal_to(5)
        obj.__delattr__("x")
        asserts.assert_that(obj.deleted).is_equal_to("x")


def _test_data_descriptor_without_deleter():
    for desc in _descriptor_variants({"__get__": lambda obj, objtype: obj._value, "__set__": _set_double}):
        Foo = _new_class("Foo", {"__init__": _init, "x": desc})
        obj = Foo(5)
        obj.__dict__["x"] = "instance value"
        asserts.assert_fails(lambda: obj.__delattr__("x"), ".*AttributeError: object has no __delete__ method.*")
        asserts.assert_that(obj.__dict__["x"]).is_equal_to("instance value")
        obj.x = 3
        asserts.assert_that(obj.x).is_equal_to(6)


def _suite():
    suite = unittest.TestSuite()
    for test in [
        _test_property_getter_and_class_access,
        _test_property_setter,
        _test_property_readonly,
        _test_property_delete,
        _test_property_inheritance_and_override,
        _test_mutablestruct_property_regression,
        _test_mutablestruct_non_data_descriptor,
        _test_struct_non_data_descriptor,
        _test_dict_non_data_descriptor,
        _test_mutablestruct_data_descriptor,
        _test_struct_data_descriptor,
        _test_dict_data_descriptor,
        _test_starlark_descriptor_delete,
        _test_data_descriptor_without_getter,
        _test_data_descriptor_without_setter,
        _test_data_descriptor_without_deleter,
    ]:
        suite.addTest(unittest.FunctionTestCase(test))
    return suite


unittest.TextTestRunner().run(_suite())
