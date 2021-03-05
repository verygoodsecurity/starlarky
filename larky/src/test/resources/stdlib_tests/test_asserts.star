"""Unit testing support.

This exports `asserts` which contains the assertions used within tests.

This is modeled after assertpy (https://github.com/assertpy/assertpy)
"""

load("@stdlib/asserts",  "asserts")


print(asserts)

v = asserts.assert_that(1)
print(v)
print(v.described_as('foo'), " and its type: ", type(v.described_as))
print("is v.described_as() of type 'Partial'?",
      type(v.described_as) == "Partial")

asserts.assert_that('foo').is_length(3)
asserts.assert_that(['a', 'b']).is_length(2)
asserts.assert_that((1, 2, 3)).is_length(3)
asserts.assert_that({'a': 1, 'b': 2}).is_length(2)

asserts.assert_that(1).is_equal_to(1)

asserts.assert_that(1).is_not_equal_to(2)

asserts.assert_that(1 == 1).is_true()

asserts.assert_that(1 != 1).is_false()

asserts.assert_that(None).is_none()

asserts.assert_that(1).is_not_none()

asserts.assert_that({}).is_instance_of(dict)

asserts.assert_(1 == 1)
asserts.assert_true(1 == 1)
asserts.assert_false(1 == 2)

#asserts.assert_that({'a': 1, 'b': 2}).is_length(3)