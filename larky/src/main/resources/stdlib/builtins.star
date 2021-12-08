# try to mimic: https://docs.python.org/3/library/builtins.html

load("@stdlib/larky", "larky")
load("@stdlib/types", "types")
load("@stdlib/codecs", "codecs")


def _bytes(s, encoding='utf-8', errors='strict'):
    """While bytes literals and representations are based on ASCII text, bytes
    objects actually behave like immutable sequences of integers, with each
    value in the sequence restricted such that 0 <= x < 256 (attempts to
    violate this restriction will trigger ValueError). This is done
    deliberately to emphasise that while many binary formats include ASCII
    based elements and can be usefully manipulated with some text-oriented
    algorithms, this is not generally the case for arbitrary binary data
    (blindly applying text processing algorithms to binary data formats that
    are not ASCII compatible will usually lead to data corruption).

    In addition to the literal forms, bytes objects can be created in a number
    of other ways:

    - A zero-filled bytes object of a specified length: bytes(10)
    - From an iterable of integers: bytes(range(20))
    - Copying existing binary data via the buffer protocol: bytes(obj)

    Since bytes objects are sequences of integers (akin to a tuple), for a bytes
    object b, b[0] will be an integer, while b[0:1] will be a bytes object of
    length 1. (This contrasts with text strings, where both indexing and slicing
    will produce a string of length 1)

    The representation of bytes objects uses the literal format (b'...') since
    it is often more useful than e.g. bytes([46, 46, 46]). You can always
    convert a bytes object into a list of integers using list(b).

    :param s:
    :return:
    """
    if hasattr(s, '__bytes__'):
        return s.__bytes__()

    # utf-8 encoding by default
    return bytes(s, encoding, errors)
    # if types.is_iterable(s):
    #     return bytearray(list(s))
    # else:
    #     fail("TypeError: cannot convert '%s' object to bytes" % type(s))


def _bytearray(source, encoding='utf-8', errors='strict'):
    """Return a new array of bytes. The bytearray class is a mutable sequence
    of integers in the range 0 <= x < 256. It has most of the usual methods of
    mutable sequences, described in Mutable Sequence Types, as well as most
    methods that the bytes type has, see Bytes and Bytearray Operations.

    The optional source parameter can be used to initialize the array in a few
    different ways:

        - If it is a string, you must also give the encoding
        (and optionally, errors) parameters; bytearray() then converts the
        string to bytes using str.encode().
        - If it is an integer, the array will have that size and will be
        initialized with null bytes.
        - If it is an object conforming to the buffer interface, a read-only
        buffer of the object will be used to initialize the bytes array.
        - If it is an iterable, it must be an iterable of integers in the
        range 0 <= x < 256, which are used as the initial contents of the
        array.

    Without an argument, an array of size 0 is created.
    """
    if hasattr(source, '__bytes__'):
        return bytearray(source.__bytes__())
    return bytearray(source, encoding, errors)


def _sum(nums,*args,**kwargs):
    if(kwargs and args):
      msg = "TypeError: sum() expects at most 2 arguments (" + str(len(args)+len(kwargs)+1) + " given)"
      fail(msg)
    elif len(args) > 1:
      msg = "TypeError: sum() expects at most 2 arguments (" + str(len(args)+1) + " given)"
      fail(msg)
    else:
      if 'start' in kwargs:
        s = kwargs['start']
      elif args:
        s = args[0]
      else:
        s = 0
      for num in nums:
        s += num
      return s


def map(func, iterable):
    return [func(x) for x in iterable]


def callable(obj):
    if types.is_function(obj):
        return True
    if hasattr(obj, "__call__"):
        return True
    return False

# TODO: should we move this to starlark?
# list of functions from: https://docs.python.org/3/library/functions.html
builtins = larky.struct(
    bytes=_bytes,
    b=_bytes,
    bytearray=_bytearray,
    sum=_sum,
    abs=abs,
    pow=pow,
    iter=iter,
    next=next,
    map=map,
    isinstance=types.is_instance,
    repr=repr,
    reversed=reversed,
    callable=callable,
    # Sentintel
    NotImplemented=larky.SENTINEL,
    # Errors..
    StopIteration=StopIteration,
    IndexError=IndexError,

)
