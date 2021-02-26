"""
"""
load("@stdlib/larky", "larky")
load("@stdlib/re2j", _re2j = "re2j")


def _enumify_iterable(iterable, enum_dict):
    """A hacky function to turn an iterable into a dict with whose keys are the
    members of the iterable, and value is the index."""
    for i, t in enumerate(iterable):
        enum_dict[t] = i
    return enum_dict


__ = -1 # Alias for the invalid class
RegexFlags = _enumify_iterable(iterable = [
    "A",
    "ASCII",
    "DEBUG",
    "I",
    "IGNORECASE",
    "L",
    "LOCALE",
    "M",
    "MULTILINE",
    "S",
    "DOTALL",
    "X",
    "VERBOSE",
    "U",
    "UNICODE",
    "T",
    "TEMPLATE",
], enum_dict = {'__' : __})


# emulate class object
def _matcher__init__(matchobj):

    def group(*args):
        if len(args) <= 1:
            return matchobj.group(*args)
        else:
            m = []
            for i in args:
                m.append(matchobj.group(i))
            return tuple(m)

    def groups():
        m = []
        for i in range(matchobj.group_count()):
            m.append(matchobj.group(i+1))
        return tuple(m)

    return larky.struct(
        group=group,
        groups=groups
    )

# --------------------------------------------------------------------
# public interface

def _match(pattern, string, flags=0):
    """Try to apply the pattern at the start of the string, returning
    a Match object, or None if no match was found."""
    _matcher = _compile(pattern, flags).matcher(string)
    if not _matcher.looking_at():
        return None
    return _matcher__init__(_matcher)

def _fullmatch(pattern, string, flags=0):
    """Try to apply the pattern to all of the string, returning
    a Match object, or None if no match was found."""
    _matcher = _compile(pattern, flags).matcher(string)
    if not _matcher.matches():
        return None
    return _matcher__init__(_matcher)

def _search(pattern, string, flags=0):
    """Scan through string looking for a match to the pattern, returning
    a Match object, or None if no match was found."""
    _matcher = _compile(pattern, flags).matcher(string)
    if not _matcher.find():
        return None
    return _matcher__init__(_matcher)

def _sub(pattern, repl, string, count=0, flags=0):
    """Return the string obtained by replacing the leftmost
    non-overlapping occurrences of the pattern in string by the
    replacement repl.  repl can be either a string or a callable;
    if a string, backslash escapes in it are processed.  If it is
    a callable, it's passed the Match object and must return
    a replacement string to be used."""
    return _compile(pattern, flags).sub(repl, string, count)

def _subn(pattern, repl, string, count=0, flags=0):
    """Return a 2-tuple containing (new_string, number).
    new_string is the string obtained by replacing the leftmost
    non-overlapping occurrences of the pattern in the source
    string by the replacement repl.  number is the number of
    substitutions that were made. repl can be either a string or a
    callable; if a string, backslash escapes in it are processed.
    If it is a callable, it's passed the Match object and must
    return a replacement string to be used."""
    return _compile(pattern, flags).subn(repl, string, count)

def _split(pattern, string, maxsplit=0, flags=0):
    """Split the source string by the occurrences of the pattern,
    returning a list containing the resulting substrings.  If
    capturing parentheses are used in pattern, then the text of all
    groups in the pattern are also returned as part of the resulting
    list.  If maxsplit is nonzero, at most maxsplit splits occur,
    and the remainder of the string is returned as the final element
    of the list."""
    return _compile(pattern, flags).split(string, maxsplit)

def _findall(pattern, string, flags=0):
    """Return a list of all non-overlapping matches in the string.
    If one or more capturing groups are present in the pattern, return
    a list of groups; this will be a list of tuples if the pattern
    has more than one group.
    Empty matches are included in the result."""
    return _compile(pattern, flags).findall(string)

def _finditer(pattern, string, flags=0):
    """Return an iterator over all non-overlapping matches in the
    string.  For each match, the iterator returns a Match object.
    Empty matches are included in the result."""
    return _compile(pattern, flags).finditer(string)

def _compile(pattern, flags=0):
    "Compile a regular expression pattern, returning a Pattern object."
    pattern = _re2j.Pattern.compile(pattern, flags)
    return pattern

def _purge():
    "Clear the regular expression caches"
    pass

def _template(pattern, flags=0):
    "Compile a template pattern, returning a Pattern object"
    #return _compile(pattern, flags|T)
    pass

# SPECIAL_CHARS
# closing ')', '}' and ']'
# '-' (a range in character set)
# '&', '~', (extended character set operations)
# '#' (comment) and WHITESPACE (ignored) in verbose mode
#_special_chars_map = {i: '\\' + chr(i) for i in bytes('()[]{}?*+-|^$\\.&~# \t\n\r')}

def _escape(pattern):
    """
    Escape special characters in a string.
    """
    res = ""
    for c in pattern.elems():
        if any((
            (('0' <= c) and (c <= '9')),
            (('A' <= c) and (c <= 'Z')),
            (('a' <= c) and (c <= 'z')),
            c == '_',
        )):
            res += c
        else:
            res += "\\" + c
    return res
    #return pattern.translate(_special_chars_map)


re = larky.struct(
    compile = _compile,
    search = _search,
    match = _match,
    fullmatch = _fullmatch,
    split = _split,
    findall = _findall,
    finditer = _finditer,
    sub = _sub,
    subn = _subn,
    escape = _escape,
    purge = _purge,
    template = _template,
)