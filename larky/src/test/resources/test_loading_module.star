# load("//testlib/builtinz", "setz") # works, but root is not defined.
# load("/testlib/builtinz", "setz")  # does not work
# load("./testlib/builtinz", "setz") # works
load("@stdlib//json", "json")
load("@stdlib//hashlib", "hashlib")
load("@vendor//pycryptodome", "pycryptodome")
load("testlib/builtinz", "setz", "collections")


def _assert_false(
        condition,
        msg="Expected condition to be false, but was true."):
    """Asserts that the given `condition` is false.
    Args:
      condition: A value that will be evaluated in a Boolean context.
      msg: An optional message that will be printed that describes the failure.
          If omitted, a default will be used.
    """
    if condition:
        fail(msg)


def _assert_true(
        condition,
        msg="Expected condition to be true, but was false."):
    """Asserts that the given `condition` is true.
    Args:
      condition: A value that will be evaluated in a Boolean context.
      msg: An optional message that will be printed that describes the failure.
          If omitted, a default will be used.
    """
    if not condition:
        fail(msg)


def assert_not_none(item):
    return _assert_true(item != None, "item %s is None!" % item)


def assert_eq(a, b):
    return _assert_true(a == b, "item %s does not equal %s" % (a, b))


# # request = struct(
# #     max_content_length = native.request.max_content_length,
# # )
# request.max_content_length == 15
#
# requestobj = request.from_proto('string')
# requestobj.headers()
#

assert_not_none(collections)
assert_not_none(setz.make())
_hash = hashlib.md5(bytes("foo", encoding='utf-8'))
assert_eq(_hash.digest_size, 16)
assert_eq(_hash.block_size, 64)
assert_eq(_hash.oid, "1.2.840.113549.2.5")
assert_eq(_hash.hexdigest(), 'acbd18db4cc2f85cedef654fccc4a4d8')

c1 = json.dumps({"one": 1, "two": 2})
d1 = json.dumps("😹💍")
c = json.decode(c1)
d = json.decode(d1)
assert_eq(json.loads('{"one": 1, "two": 2}'), c)
assert_eq(json.loads('"\\ud83d\\ude39\\ud83d\\udc8d"'), d)
