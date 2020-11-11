load("@stdlib/c99math", _c99math = "c99math")

math = struct(
  pi = _c99math.PI,
  pow = _c99math.pow,
  sqrt = _c99math.sqrt,
)


