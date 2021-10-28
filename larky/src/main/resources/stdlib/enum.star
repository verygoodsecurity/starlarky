load("@stdlib//larky", larky="larky")
load("@stdlib//types", "types")


# TODO: we need to move this to a vendor utility. See issue:
#   https://github.com/verygoodsecurity/starlarky/issues/129
def _split(string, delimiter=" "):
    """
    Python's split function implementation

    :param string: a string
    :return: a list after breaking string on delimiter match
    """
    result_list = []

    if not string:
        return [string]
    start = 0
    index = 0
    for index, char in enumerate(string.elems()):
        if char == delimiter:
            result_list.append(string[start:index])
            start = index + 1
    if start == 0:
        return [string]
    result_list.append(string[start:index + 1])

    return result_list


def _enumify_iterable(iterable, enum_dict, numerator=None):
    """A hacky function to turn an iterable into a dict with whose keys are the
    members of the iterable, and value is the index.

    If the key is a tuple, it will iterate over the keys and assign the same
    enumerated position.

    A numerator is a callable that takes the enumerated position and returns
    the expected number in order. For example, numerator=lambda x: x << 2 will
    map to 1, 2, 4, 8, 16 instead of 1, 2, 3, 4, 5

    .. python::

        __ = -1  # Alias for the invalid class
        RegexFlags = _enumify_iterable(iterable=[
            ("I", "IGNORECASE"),
            ("S", "DOTALL"),
            ("M", "MULTILINE"),
            ("U", "UNICODE"),
            "LONGEST_MATCH",
            ("A", "ASCII"),
            "DEBUG",
            ("L", "LOCALE"),
            ("X", "VERBOSE"),
            ("T", "TEMPLATE"),
        ], enum_dict={'__': __}, numerator=lambda x: 1 << x)

        assert RegexFlags["I"] == 1
        assert RegexFlags["DOTALL"] == RegexFlags["S"] == 2
        assert RegexFlags["MULTILINE"] == RegexFlags["M"] == 4

    """
    for i, t in enumerate(iterable):
        _i = i
        if numerator and types.is_callable(numerator):
            _i = numerator(i)
        if types.is_tuple(t):
            for t_elem in t:
                enum_dict[t_elem] = _i
        else:
            enum_dict[t] = _i
    return enum_dict


# https://stackoverflow.com/a/1695250
def enum2(*sequential, **named):
    """
    Use like so:

    .. python::

       _enum = enum.enum2('_TRY', '_EXCEPT', '_ELSE', '_FINALLY', '_BUILD')

    :param sequential: a list or tuple that we will use as enum values
    :param named: name/value mapping
    :return: enum with `reversed_mapping` for output formatting
    """
    enums = dict(zip(sequential, range(len(sequential))), **named)
    reverse = {value: key for key, value in enums.items()}
    enums['_value2member_map_'] = reverse
    return larky.struct(__class__='Enum', **enums)


def _generate_next_value_(name, start, count, last_values):
    """
    Generate the next value when not given.
    name: the name of the member
    start: the initial start value or None
    count: the number of existing members
    last_value: the last value assigned or None
    """
    for last_value in reversed(last_values):
        if types.is_int(last_value):
            return last_value + 1

    return start


# try to emulate python's functional API
# https://docs.python.org/3/library/enum.html#functional-api
def Enum(class_name, names, module=None, qualname=None, type=None, start=1):
    """
    Create a new *IMMUTABLE* Enum class.

    `names` can be:
    * A string containing member names, separated either with spaces or
      commas.  Values are incremented by 1 from `start`.
    * An iterable of member names.  Values are incremented by 1 from `start`.
    * An iterable of (member name, value) pairs.
    * A mapping of member name -> value pairs.
    """

    def __init__(class_name, names, module, qualname, type, start):
        # https://github.com/python/cpython/blob/5a8ddcc4524dca3880d7fc2818814ffae1cfb8a2/Lib/enum.py#L451
        # special processing needed for names?
        clsdict = {}
        if types.is_string(names):
            names = _split(names.replace(',', ' '))
        if types.is_iterable(names) and names and types.is_string(names[0]):
            original_names, names = names, []
            last_values = []
            for count, name in enumerate(original_names):
                value = _generate_next_value_(name, start, count, last_values[:])
                last_values.append(value)
                names.append((name, value))

        _value2member_map_ = {}
        # Here, names is either an iterable of (name, value) or a mapping.
        for item in names:
            if types.is_string(item):
                member_name, member_value = item, names[item]
            else:
                member_name, member_value = item
            _value2member_map_[member_value] = member_name
            clsdict[member_name] = member_value

        return larky.struct(
            __class__ = class_name,
            __members__=clsdict,
            _value2member_map_=_value2member_map_,
            **clsdict
        )  # Immutable.

    self = __init__(class_name, names, module, qualname, type, start)
    return self


enum = larky.struct(
    enumify_iterable=_enumify_iterable,
    enum2=enum2,
    Enum=Enum
)
