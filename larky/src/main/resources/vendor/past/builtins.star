load("@stdlib//larky", larky="larky")
load("@stdlib//struct", struct="struct")
load("@stdlib//operator", operator="operator")



def cmp(a, b):
    _a = 1 if bool(operator.ge(a, b)) else 0
    _b = 1 if bool(operator.lt(a, b)) else 0
    return _a - _b


builtins = larky.struct(
    __name__='builtins',
    cmp=cmp
)