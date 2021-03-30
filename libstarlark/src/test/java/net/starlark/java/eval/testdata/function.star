# 2-step function call

x = [1, 2, 3]
y = x.clear
assert_eq(x, [1, 2, 3])
z = y()
assert_eq(z, None)
assert_eq(x, [])
---

x = {1: 2}
y = x.pop
assert_eq(x, {1: 2})
z = y(1)
assert_eq(z, 2)
assert_eq(x, {})
---

x = "hello"
y = x.upper
z = y()
assert_eq(z, "HELLO")
assert_eq(x, "hello")
---

x = "abc"
y = x.index
z = y("b")
assert_eq(z, 1)
assert_eq(x, "abc")
---

y = {}
assert_eq(y.clear == y.clear, False)
assert_eq([].clear == [].clear, False)
assert_eq(type([].clear), "function")
assert_eq(str([].clear), "<built-in method clear of list value>")
assert_eq(str({}.clear), "<built-in method clear of dict value>")
assert_eq(str(len), "<built-in function len>")

---
x = {}.pop
x() ###  missing 1 required positional argument: key
---

# getattr

assert_eq(getattr("abc", "upper")(), "ABC")
assert_eq(getattr({'a': True}, "pop")('a'), True)
assert_eq(getattr({}, "hello", "default"), "default")

y = [1, 2, 3]
x = getattr(y, "clear")
assert_eq(y, [1, 2, 3])
x()
assert_eq(y, [])

---
getattr("", "abc") ### 'string' value has no field or method 'abc'
---
x = getattr("", "pop", "clear")
x() ### 'string' object is not callable
---
# Regression test for a type mismatch crash (b/168743413).
getattr(1, []) ### parameter 'name' got value of type 'list', want 'string'
