# String methods under the default semantics. Larky sets -python_string_bounds and
# -python_unicode_strings, which change the cases below to CPython's results; PythonStringsTest
# covers those.

# start/end follow Starlark's indexing conventions: they are clamped to the string, and the method
# works on the (possibly empty) slice S[start:end]. The spec says startswith "reports whether the
# string S[start:end] has the specified prefix", so "abc".startswith("", 4) is True, and the
# empty string is found at the clamped start. starlark-go gives the same results. (CPython gives
# -1, False and 0 whenever start > end.)
assert_eq("abcabc".find("", 7), 6)
assert_eq("abcabc".rfind("", 7), 6)
assert_eq("abc".index("", 4), 3)
assert_eq("abc".rindex("", 4), 3)
assert_eq("abc".count("", 4), 1)
assert_eq("abc".count("", 2, 1), 1)
assert_eq("abc".find("", 2, 1), 2)
assert_eq("abc".startswith("", 4), True)
assert_eq("abc".endswith("", 4), True)
assert_eq("abc".startswith(("x", ""), 5, 10), True)
assert_eq("abc".find("c", 4), -1)
assert_eq("abc".count("c", 4), 0)
assert_eq("abc".startswith("c", 4), False)

# Within range, both agree.
assert_eq("abc".find("", 3), 3)
assert_eq("abc".count("", 3), 1)
assert_eq("abc".startswith("", 3), True)
assert_eq("abc".find("", -10), 0)
assert_eq("abc".count(""), 4)
assert_eq("abc".find("c", -1), 2)
---
"abc".startswith(("a", 1), 5) ### at index 1 of sub, got element of type int, want string
---
"abc".find("", 1 << 40) ### got 1099511627776 for start, want value in signed 32-bit range
