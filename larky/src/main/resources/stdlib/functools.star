def reduce(function, sequence, initial=None):
    it = list(sequence)
    value = initial
    for element in it:
        value = function(value, element)
    return value