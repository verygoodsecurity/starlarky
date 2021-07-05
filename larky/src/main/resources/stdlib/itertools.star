"""
Copy of https://docs.python.org/3/library/itertools.html
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")


def chain(*iterables):
    """
    Make an iterator that returns elements from the first iterable until
    it is exhausted, then proceeds to the next iterable, until all of
    the iterables are exhausted. Used for treating consecutive sequences
    as a single sequence.

    :param iterables:
    :return:
    """
    # chain('ABC', 'DEF') --> A B C D E F
    k = []
    for it in iterables:
        # if types.is_string(it):
        #     it = it.elems()
        for element in it:
            k.append(element)
    return k


def from_iterable(iterables):
    """
    Alternate constructor for chain(). Gets chained inputs from a single
     iterable argument that is evaluated lazily.

    :param iterables:
    :return:
    """
    k = []
    # from_iterable(['ABC', 'DEF']) --> A B C D E F
    for it in iterables:
        for element in it:
            # if types.is_string(element):
            #     element = element.elems()
            k.append(element)
    return k


itertools = larky.struct(
    chain=chain,
    from_iterable=from_iterable,
)