load("@stdlib/larky", "larky")
load("@stdlib/c99math", _c99math = "c99math")

math =  larky.struct(
  pi = _c99math.PI,
  pow = _c99math.pow,
  sqrt = _c99math.sqrt,
)


