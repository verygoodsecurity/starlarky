# str.encode(encoding="utf-8", errors="strict"), as in Python. Expected values are CPython's.

# utf-8 is the default
assert_eq("abc".encode(), b"abc")
assert_eq("".encode(), b"")
assert_eq(type("abc".encode()), "bytes")
assert_eq("h\u00e9".encode(), b"h\xc3\xa9")
assert_eq("\u20ac".encode("utf-8"), b"\xe2\x82\xac")
assert_eq("\U0001f600".encode("utf-8"), b"\xf0\x9f\x98\x80")
assert_eq("h\u00e9".encode("UTF8"), b"h\xc3\xa9")
assert_eq("h\u00e9".encode("utf_8"), b"h\xc3\xa9")

# positional and keyword forms
assert_eq("h\u00e9".encode("latin-1"), b"h\xe9")
assert_eq("h\u00e9".encode(encoding="latin-1"), b"h\xe9")
assert_eq("h\u00e9".encode(encoding="ascii", errors="ignore"), b"h")
assert_eq("h\u00e9".encode("ascii", "ignore"), b"h")
assert_eq("h\u00e9".encode("ascii", errors="replace"), b"h?")
assert_eq("h\u00e9".encode(errors="strict"), b"h\xc3\xa9")

# latin-1 and its aliases
assert_eq("\u00e9\u00ff".encode("iso-8859-1"), b"\xe9\xff")
assert_eq("\u00e9".encode("ISO8859-1"), b"\xe9")
assert_eq("\u00e9".encode("latin1"), b"\xe9")
assert_eq("\u00e9".encode("l1"), b"\xe9")

# ascii
assert_eq("abc".encode("ascii"), b"abc")
assert_eq("abc".encode("US-ASCII"), b"abc")

# errors modes
assert_eq("a\u00e9b".encode("ascii", "ignore"), b"ab")
assert_eq("a\u00e9\u20acb".encode("ascii", "replace"), b"a??b")
assert_eq("a\u00e9\u20ac\U0001f600b".encode("ascii", "backslashreplace"), b"a\\xe9\\u20ac\\U0001f600b")
assert_eq("a\u00e9\u20acb".encode("ascii", "xmlcharrefreplace"), b"a&#233;&#8364;b")
assert_eq("a\u00e9b".encode("ascii", "namereplace"), b"a\\N{LATIN SMALL LETTER E WITH ACUTE}b")
assert_eq("a\u20acb".encode("latin-1", "ignore"), b"ab")
assert_eq("a\u20acb".encode("latin-1", "replace"), b"a?b")
assert_eq("a\u20acb".encode("latin-1", "backslashreplace"), b"a\\u20acb")
assert_eq("a\u20acb".encode("latin-1", "xmlcharrefreplace"), b"a&#8364;b")
# the handler is consulted only when a character cannot be encoded
assert_eq("abc".encode("ascii", "no-such-handler"), b"abc")
assert_eq("abc".encode("utf-8", "no-such-handler"), b"abc")

# a supplementary character is one character, replaced once
assert_eq("\U0001f600".encode("ascii", "replace"), b"?")
# a run of unencodable characters is replaced per character
assert_eq("\u20ac\u20ac".encode("ascii", "replace"), b"??")

# the result is bytes, so it can be decoded again
assert_eq("h\u00e9\u20ac".encode().decode(), "h\u00e9\u20ac")
assert_eq("h\u00e9".encode("latin-1").decode("iso-8859-1"), "h\u00e9")
assert_eq(len("h\u00e9\u20ac".encode()), 6)

---
"h\u00e9".encode("ascii") ### 'ascii' codec can't encode character '\\xe9' in position 1: ordinal not in range\(128\)
---
"a\u20acb".encode("latin-1") ### 'latin-1' codec can't encode character '\\u20ac' in position 1: ordinal not in range\(256\)
---
"a\u20ac\u20acb".encode("ascii") ### 'ascii' codec can't encode characters in position 1-2: ordinal not in range\(128\)
---
"a\U0001f600".encode("ascii") ### 'ascii' codec can't encode character '\\U0001f600' in position 1: ordinal not in range\(128\)
---
"h\u00e9".encode("ascii", "strict") ### 'ascii' codec can't encode character '\\xe9' in position 1: ordinal not in range\(128\)
---
"h\u00e9".encode(errors="strict", encoding="ascii") ### 'ascii' codec can't encode character '\\xe9' in position 1: ordinal not in range\(128\)
---
"h\u00e9".encode("ascii", "bogus") ### unknown error handler name 'bogus'
---
"abc".encode("no-such-codec") ### unknown encoding: no-such-codec
---
"abc".encode(1) ### got value of type 'int', want 'string'
---
"abc".encode("utf-8", None) ### got value of type 'NoneType', want 'string'
---
"abc".encode("utf-8", "strict", "extra") ### accepts no more than 2 positional arguments but got 3
---
"abc".encode(encodin="utf-8") ### unexpected keyword argument 'encodin'
