# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Unit tests for sets.star."""

load("@stdlib//builtins", "builtins")
load("@stdlib//larky", "larky")
load("@stdlib//sets", "sets")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


assert_true = asserts.assert_true
assert_false = asserts.assert_false
struct = larky.struct


def _is_equal_test():
    """Unit tests for sets.is_equal."""

    # Note that if this test fails, the results for the other `sets` tests will
    # be inconclusive because they use `asserts.new_set_equals`, which in turn
    # calls `sets.is_equal`.

    assert_true(sets.is_equal(sets.make(), sets.make()))
    assert_false(sets.is_equal(sets.make(), sets.make([1])))
    assert_false(sets.is_equal(sets.make([1]), sets.make()))
    assert_true(sets.is_equal(sets.make([1]), sets.make([1])))
    assert_false(sets.is_equal(sets.make([1]), sets.make([1, 2])))
    assert_false(sets.is_equal(sets.make([1]), sets.make([2])))
    assert_false(sets.is_equal(sets.make([1]), sets.make([1, 2])))

    # If passing a list, verify that duplicate elements are ignored.
    assert_true(sets.is_equal(sets.make([1, 1]), sets.make([1])))


def _is_subset_test():
    """Unit tests for sets.is_subset."""

    assert_true(sets.is_subset(sets.make(), sets.make()))
    assert_true(sets.is_subset(sets.make(), sets.make([1])))
    assert_false(sets.is_subset(sets.make([1]), sets.make()))
    assert_true(sets.is_subset(sets.make([1]), sets.make([1])))
    assert_true(sets.is_subset(sets.make([1]), sets.make([1, 2])))
    assert_false(sets.is_subset(sets.make([1]), sets.make([2])))

    # If passing a list, verify that duplicate elements are ignored.
    assert_true(sets.is_subset(sets.make([1, 1]), sets.make([1, 2])))


def _disjoint_test():
    """Unit tests for sets.disjoint."""

    assert_true(sets.disjoint(sets.make(), sets.make()))
    assert_true(sets.disjoint(sets.make(), sets.make([1])))
    assert_true(sets.disjoint(sets.make([1]), sets.make()))
    assert_false(sets.disjoint(sets.make([1]), sets.make([1])))
    assert_false(sets.disjoint(sets.make([1]), sets.make([1, 2])))
    assert_true(sets.disjoint(sets.make([1]), sets.make([2])))

    # If passing a list, verify that duplicate elements are ignored.
    assert_false(sets.disjoint(sets.make([1, 1]), sets.make([1, 2])))


def _intersection_test():
    """Unit tests for sets.intersection."""

    asserts.eq(sets.make(), sets.intersection(sets.make(), sets.make()))
    asserts.eq(sets.make(), sets.intersection(sets.make(), sets.make([1])))
    asserts.eq(sets.make(), sets.intersection(sets.make([1]), sets.make()))
    asserts.eq(sets.make([1]), sets.intersection(sets.make([1]), sets.make([1])))
    asserts.eq(sets.make([1]), sets.intersection(sets.make([1]), sets.make([1, 2])))
    asserts.eq(sets.make(), sets.intersection(sets.make([1]), sets.make([2])))

    # If passing a list, verify that duplicate elements are ignored.
    asserts.eq(sets.make([1]), sets.intersection(sets.make([1, 1]), sets.make([1, 2])))


def _union_test():
    """Unit tests for sets.union."""

    asserts.eq(sets.make(), sets.union())
    asserts.eq(sets.make([1]), sets.union(sets.make([1])))
    asserts.eq(sets.make(), sets.union(sets.make(), sets.make()))
    asserts.eq(sets.make([1]), sets.union(sets.make(), sets.make([1])))
    asserts.eq(sets.make([1]), sets.union(sets.make([1]), sets.make()))
    asserts.eq(sets.make([1]), sets.union(sets.make([1]), sets.make([1])))
    asserts.eq(sets.make([1, 2]), sets.union(sets.make([1]), sets.make([1, 2])))
    asserts.eq(sets.make([1, 2]), sets.union(sets.make([1]), sets.make([2])))

    # If passing a list, verify that duplicate elements are ignored.
    asserts.eq(sets.make([1, 2]), sets.union(sets.make([1, 1]), sets.make([1, 2])))


def _difference_test():
    """Unit tests for sets.difference."""

    asserts.eq(sets.make(), sets.difference(sets.make(), sets.make()))
    asserts.eq(sets.make(), sets.difference(sets.make(), sets.make([1])))
    asserts.eq(sets.make([1]), sets.difference(sets.make([1]), sets.make()))
    asserts.eq(sets.make(), sets.difference(sets.make([1]), sets.make([1])))
    asserts.eq(sets.make(), sets.difference(sets.make([1]), sets.make([1, 2])))
    asserts.eq(sets.make([1]), sets.difference(sets.make([1]), sets.make([2])))

    # If passing a list, verify that duplicate elements are ignored.
    asserts.eq(sets.make([2]), sets.difference(sets.make([1, 2]), sets.make([1, 1])))


def _to_list_test():
    """Unit tests for sets.to_list."""

    asserts.eq([], sets.to_list(sets.make()))
    asserts.eq([1], sets.to_list(sets.make([1, 1, 1])))
    asserts.eq([1, 2, 3], sets.to_list(sets.make([1, 2, 3])))


def _make_test():
    """Unit tests for sets.make."""

    asserts.eq({}, sets.make()._values)
    asserts.eq({x: None for x in [1, 2, 3]}, sets.make([1, 1, 2, 2, 3, 3])._values)


def _copy_test():
    """Unit tests for sets.copy."""

    asserts.eq(sets.copy(sets.make()), sets.make())
    asserts.eq(sets.copy(sets.make([1, 2, 3])), sets.make([1, 2, 3]))

    # Ensure mutating the copy does not mutate the original
    original = sets.make([1, 2, 3])
    copy = sets.copy(original)
    copy._values[5] = None
    assert_false(sets.is_equal(original, copy))


def _insert_test():
    """Unit tests for sets.insert."""

    asserts.eq(sets.make([1, 2, 3]), sets.insert(sets.make([1, 2]), 3))

    # Ensure mutating the inserted set does mutate the original set.
    original = sets.make([1, 2, 3])
    after_insert = sets.insert(original, 4)
    (asserts.assert_that(
        original
    ).described_as("Insert creates a new set which is an O(n) operation, insert should be O(1).")
        .is_equal_to(after_insert))


def _contains_test():
    """Unit tests for sets.contains."""

    assert_false(sets.contains(sets.make(), 1))
    assert_true(sets.contains(sets.make([1]), 1))
    assert_true(sets.contains(sets.make([1, 2]), 1))
    assert_false(sets.contains(sets.make([2, 3]), 1))


def _length_test():
    """Unit test for sets.length."""

    asserts.eq(0, sets.length(sets.make()))
    asserts.eq(1, sets.length(sets.make([1])))
    asserts.eq(2, sets.length(sets.make([1, 2])))


def _remove_test():
    """Unit test for sets.remove."""

    asserts.eq(sets.make([1, 2]), sets.remove(sets.make([1, 2, 3]), 3))

    # Ensure mutating the inserted set does mutate the original set.
    original = sets.make([1, 2, 3])
    after_removal = sets.remove(original, 3)
    asserts.eq(original, after_removal)


def _repr_str_test():
    """Unit test for sets.repr and sets.str."""

    asserts.eq("[]", sets.repr(sets.make()))
    asserts.eq("[1]", sets.repr(sets.make([1])))
    asserts.eq("[1, 2]", sets.repr(sets.make([1, 2])))

    asserts.eq("[]", sets.str(sets.make()))
    asserts.eq("[1]", sets.str(sets.make([1])))
    asserts.eq("[1, 2]", sets.str(sets.make([1, 2])))


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_is_equal_test))
    _suite.addTest(unittest.FunctionTestCase(_is_subset_test))
    _suite.addTest(unittest.FunctionTestCase(_disjoint_test))
    _suite.addTest(unittest.FunctionTestCase(_intersection_test))
    _suite.addTest(unittest.FunctionTestCase(_union_test))
    _suite.addTest(unittest.FunctionTestCase(_difference_test))
    _suite.addTest(unittest.FunctionTestCase(_to_list_test))
    _suite.addTest(unittest.FunctionTestCase(_make_test))
    _suite.addTest(unittest.FunctionTestCase(_copy_test))
    _suite.addTest(unittest.FunctionTestCase(_insert_test))
    _suite.addTest(unittest.FunctionTestCase(_contains_test))
    _suite.addTest(unittest.FunctionTestCase(_length_test))
    _suite.addTest(unittest.FunctionTestCase(_remove_test))
    _suite.addTest(unittest.FunctionTestCase(_repr_str_test))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
