load("@stdlib//uuid", uuid="uuid")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


# this is the only test for uuid4 in CPython
# https://github.com/python/cpython/blob/main/Lib/test/test_uuid.py

# TODO: port more tests for the UUID class itself..

def test_uuid4():
    equal = asserts.eq

    # Make sure uuid4() generates UUIDs that are actually version 4.
    for u in [uuid.uuid4() for i in range(10)]:
        equal(u.variant, uuid.RFC_4122)
        equal(u.version, 4)

    # Make sure the generated UUIDs are actually unique.
    uuids = {}
    for u in [uuid.uuid4() for i in range(1000)]:
        uuids[u] = 1
    equal(len(uuids.keys()), 1000)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_uuid4))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())