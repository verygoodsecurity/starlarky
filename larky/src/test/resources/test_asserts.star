"""Unit testing support.

This exports `asserts` which contains the assertions used within tests.

This is modeled after assertpy (https://github.com/assertpy/assertpy)
"""

load("testlib/asserts",  "asserts")


print(asserts)

v = asserts.assert_that(1)
print(v)
print(v.described_as('foo'))
