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