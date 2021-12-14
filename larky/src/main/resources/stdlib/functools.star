load("@stdlib//larky", larky="larky")


partial = larky.partial


def reduce(function, sequence, initial=larky.SENTINEL):
    it = list(sequence)
    if initial == larky.SENTINEL:
        value = it.pop(0)
    else:
        value = initial

    for element in it:
        value = function(value, element)

    return value


################################################################################
### cmp_to_key() function converter
################################################################################

def cmp_to_key(mycmp):
    """Convert a cmp= function into a key= function"""
    def K(obj):
        def __lt__(other):
            return mycmp(obj, other.obj) < 0
        def __gt__(other):
            return mycmp(obj, other.obj) > 0
        def __eq__(other):
            return mycmp(obj, other.obj) == 0
        def __le__(other):
            return mycmp(obj, other.obj) <= 0
        def __ge__(other):
            return mycmp(obj, other.obj) >= 0
        __hash__ = None

        return larky.struct(
            __name__='K',
            __class__=K,
            __hash__=__hash__,
            __lt__=__lt__,
            __gt__=__gt__,
            __eq__=__eq__,
            __le__=__le__,
            __ge__=__ge__,
            obj=obj,
        )
    return K
