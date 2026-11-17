"""Tests for the json module's handling of non-JSON types."""
load("@stdlib//collections", "collections")
load("@stdlib//json", "json")
load("@stdlib//larky", "larky")
load("@stdlib//types", "types")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


def _test_rejects_bytes_and_sets():
    # CPython 3: json.dumps(b"a") raises
    # TypeError("Object of type bytes is not JSON serializable"), and likewise
    # for bytearray and set, at any depth.
    asserts.assert_fails(lambda: json.dumps(b"a"),
                         "^Object of type bytes is not JSON serializable$")
    asserts.assert_fails(lambda: json.dumps(bytearray(b"a")),
                         "^Object of type bytearray is not JSON serializable$")
    asserts.assert_fails(lambda: json.dumps(set([1])),
                         "^Object of type set is not JSON serializable$")
    asserts.assert_fails(lambda: json.dumps({"a": set([1])}),
                         "Object of type set is not JSON serializable$")
    asserts.assert_fails(lambda: json.dumps([b"a"]),
                         "Object of type bytes is not JSON serializable$")
    asserts.assert_fails(lambda: json.encode({"k": (1, [b"x"])}),
                         "Object of type bytes is not JSON serializable$")


def _test_encodes_json_types():
    # output format is unchanged
    asserts.assert_that(json.dumps({"a": [1, (2,)], "b": None, "c": True})).is_equal_to(
        '{"a":[1,[2]],"b":null,"c":true}')
    asserts.assert_that(json.dumps("café")).is_equal_to('"café"')
    asserts.assert_that(json.dumps(1.5)).is_equal_to("1.5")


def _point_init(self, x):
    self.x = x


def _items_iter(self):
    return iter([1, 2])


# A class, and a class whose instances are iterable: Python encodes neither.
Point = types.new_class("Point", (), {}, lambda ns: ns.update({"__init__": _point_init}))
Items = types.new_class("Items", (), {}, lambda ns: ns.update({"__iter__": _items_iter}))


def Record(x):
    # A class written in the older Larky style, as a struct with __name__ and __class__.
    self = larky.mutablestruct(__name__="Record", __class__=Record)
    self.x = x
    self.__iter__ = lambda: iter([x])
    return self


def _test_rejects_other_values():
    # CPython: TypeError("Object of type <type(o).__name__> is not JSON serializable") for
    # anything but dict, list, tuple, str, int, float, bool and None, iterable or not.
    for value, name in [
        (range(3), "range"),
        (Point(1), "Point"),
        (Items(), "Items"),
        (Record(1), "Record"),
        (larky.struct(a=1), "ImmutableStruct"),
        (larky.mutablestruct(a=1), "MutableStruct"),
        (iter([1]), "list_iterator"),
        (len, "builtin_function_or_method"),
        (lambda: 1, "function"),
        (Point, "type"),
    ]:
        asserts.assert_fails(lambda: json.dumps(value),
                             "^Object of type %s is not JSON serializable$" % name)
        asserts.assert_fails(lambda: json.encode(value),
                             "^Object of type %s is not JSON serializable$" % name)
        asserts.assert_fails(lambda: json.encode_indent(value),
                             "^Object of type %s is not JSON serializable$" % name)


def _test_nested_errors_name_the_path():
    asserts.assert_fails(lambda: json.dumps([range(2)]),
                         "^at list index 0: Object of type range is not JSON serializable$")
    asserts.assert_fails(lambda: json.dumps({"k": (1, Point(1))}),
                         "^in dict key \"k\": at tuple index 1: " +
                         "Object of type Point is not JSON serializable$")


def _test_encodes_namedtuple_as_array():
    # A namedtuple is a tuple, so Python encodes it as an array.
    P = collections.namedtuple("P", ["x", "y"])
    asserts.assert_that(json.dumps(P(1, 2))).is_equal_to("[1,2]")


def _as_dict(o):
    if hasattr(o, "x"):
        return {"x": o.x}
    return [str(o)]


def _test_default():
    # Python's default=: called for each value that is not JSON serializable, at any depth,
    # and its result is encoded in the value's place.
    asserts.assert_that(json.dumps(Point(1), default=_as_dict)).is_equal_to('{"x":1}')
    asserts.assert_that(json.encode(Point(1), default=_as_dict)).is_equal_to('{"x":1}')
    asserts.assert_that(json.dumps(Record(2), default=_as_dict)).is_equal_to('{"x":2}')
    asserts.assert_that(
        json.dumps({"b": [Point(1), Items()], "a": range(1)}, default=_as_dict)
    ).is_equal_to('{"a":["range(0, 1)"],"b":[{"x":1},["<\'Items\' object>"]]}')
    # bytes and sets go to default too.
    asserts.assert_that(json.dumps([b"a", set([1])], default=lambda o: len(o))).is_equal_to("[1,1]")
    # JSON values never do.
    asserts.assert_that(json.dumps([1, "a", None], default=lambda o: fail("called"))).is_equal_to(
        '[1,"a",null]')
    # default=None is no default.
    asserts.assert_fails(lambda: json.dumps(Point(1), default=None),
                         "^Object of type Point is not JSON serializable$")
    # Whatever default returns is encoded in turn, calling default again if need be.
    asserts.assert_that(
        json.dumps([Point(1)], default=lambda o: range(1) if hasattr(o, "x") else "r")
    ).is_equal_to('["r"]')
    # Its errors propagate.
    asserts.assert_fails(lambda: json.dumps(Point(1), default=lambda o: fail("no")), "no")
    # As in Python, a non-callable default fails only when it is called.
    asserts.assert_that(json.dumps(1, default=1)).is_equal_to("1")
    asserts.assert_fails(lambda: json.dumps(Point(1), default=1), "not callable")
    # A default that returns the value it was given is a circular reference, as in Python;
    # one that returns a new unserializable value each time stops at a depth limit.
    asserts.assert_fails(lambda: json.dumps(Point(1), default=lambda o: o),
                         "^Circular reference detected$")
    asserts.assert_fails(lambda: json.dumps(Point(1), default=lambda o: [o]),
                         "Circular reference detected$")
    asserts.assert_fails(lambda: json.dumps(Point(1), default=lambda o: Point(1)),
                         "^nesting depth limit exceeded$")
    # The same value twice is not a circular reference.
    p = Point(3)
    asserts.assert_that(json.dumps([p, p], default=_as_dict)).is_equal_to('[{"x":3},{"x":3}]')


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_rejects_bytes_and_sets))
    _suite.addTest(unittest.FunctionTestCase(_test_encodes_json_types))
    _suite.addTest(unittest.FunctionTestCase(_test_rejects_other_values))
    _suite.addTest(unittest.FunctionTestCase(_test_nested_errors_name_the_path))
    _suite.addTest(unittest.FunctionTestCase(_test_encodes_namedtuple_as_array))
    _suite.addTest(unittest.FunctionTestCase(_test_default))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
