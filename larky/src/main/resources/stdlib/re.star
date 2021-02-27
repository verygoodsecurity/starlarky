"""
"""
load("@stdlib/larky", "larky")
load("@stdlib/types", "types")
load("@stdlib/re2j", _re2j="re2j")


def _enumify_iterable(iterable, enum_dict, numerator=None):
    """A hacky function to turn an iterable into a dict with whose keys are the
    members of the iterable, and value is the index.

    If the key is a tuple, it will iterate over the keys and assign the same
    enumerated position.

    A numerator is a callable that takes the enumerated position and returns
    the expected number in order. For example, numerator=lambda x: x << 2 will
    map to 1, 2, 4, 8, 16 instead of 1, 2, 3, 4, 5


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
            m.append(matchobj.group(i + 1))
        return tuple(m)



    return larky.struct(
        group=group,
        groups=groups,
        find=matchobj.find,
        pattern=matchobj.pattern,
        start=matchobj.start,
        end=matchobj.end,
        group_count=matchobj.group_count,
        matches=matchobj.matches,
        looking_at=matchobj.looking_at,
        replace_first=matchobj.replace_first,
        replace_all=matchobj.replace_all,
        append_tail=matchobj.append_tail,
        append_replacement=matchobj.append_replacement,
        quote_replacement=matchobj.quote_replacement
    )


def _pattern__init__(patternobj):

    def search(string, flags=0):
        return _search(patternobj.pattern(), string, flags)

    def match(string, flags=0):
        return _match(patternobj.pattern(), string, flags)

    def matcher(string):
        return _matcher__init__(patternobj.matcher(string))

    def fullmatch(string, flags=0):
        return _fullmatch(patternobj.pattern(), string, flags)

    def sub(repl, string, count=0, flags=0):
        return _sub(patternobj.pattern(), repl, string, count, flags)

    def subn(repl, string, count=0, flags=0):
        return _subn(patternobj.pattern(), repl, string, count, flags)

    def split(string, maxsplit=0, flags=0):
        return _split(patternobj.pattern(), string, maxsplit, flags)

    def findall(string, flags=0):
        return _findall(patternobj.pattern(), string, flags)

    def finditer(string, flags=0):
        return _finditer(patternobj.pattern(), string, flags)

    return larky.struct(
        search=search,
        match=match,
        fullmatch=fullmatch,
        sub=sub,
        subn=subn,
        findall=findall,
        finditer=finditer,
        matcher=matcher,
        split=split,
        patternobj=patternobj
    )
# --------------------------------------------------------------------
# public interface


def _match(pattern, string, flags=0):
    """Try to apply the pattern at the start of the string, returning
    a Match object, or None if no match was found."""
    _matcher = _compile(pattern, flags).matcher(string)
    if not _matcher.looking_at():
        return None
    return _matcher


def _fullmatch(pattern, string, flags=0):
    """Try to apply the pattern to all of the string, returning
    a Match object, or None if no match was found."""
    _matcher = _compile(pattern, flags).matcher(string)
    if not _matcher.matches():
        return None
    return _matcher


def _search(pattern, string, flags=0):
    """Scan through string looking for a match to the pattern, returning
    a Match object, or None if no match was found."""
    _matcher = _compile(pattern, flags).matcher(string)
    if not _matcher.find():
        return None
    return _matcher


def _sub(pattern, repl, string, count=0, flags=0):
    """Return the string obtained by replacing the leftmost
    non-overlapping occurrences of the pattern in string by the
    replacement repl.  repl can be either a string or a callable;
    if a string, backslash escapes in it are processed.  If it is
    a callable, it's passed the Match object and must return
    a replacement string to be used."""
    new_string, _number = _subn(pattern, repl, string, count, flags)
    return new_string


def _subn(pattern, repl, string, count=0, flags=0):
    """Return a 2-tuple containing (new_string, number).
    new_string is the string obtained by replacing the leftmost
    non-overlapping occurrences of the pattern in the source
    string by the replacement repl.  number is the number of
    substitutions that were made. repl can be either a string or a
    callable; if a string, backslash escapes in it are processed.
    If it is a callable, it's passed the Match object and must
    return a replacement string to be used."""
    # print("replacing:", string, "matching:", pattern, "with:", repl)
    return _native_subn(pattern, string, repl, count, flags)


_WHILE_LOOP_EMULATION_ITERATION = 50


def _native_subn(pattern, string, repl, count=0, flags=0):
    _matcher = _compile(pattern, flags).matcher(string)
    res = []
    cnt_rpl = 0

    for _i in range(_WHILE_LOOP_EMULATION_ITERATION):
        if not _matcher.find():
            break
        _repl = repl
        if types.is_callable(repl):
            _repl = repl(_matcher)
        _matcher.append_replacement(res, _repl)
        cnt_rpl += 1
        if count != 0:
            count -= 1
            if count == 0:
                break
    return _matcher.append_tail("".join(res)), cnt_rpl


def _larky_subn(pattern, s, repl, count=0, flags=0):
    res = []
    pos = 0
    cnt_rpl = 0
    finish = len(s)
    m = _compile(pattern, flags).matcher(s)

    for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
        if pos > finish:
            break

        if not m.find():
            res.append(s[pos:])
            break
        beg, end = m.start(), m.end()
        res.append(s[pos:beg])
        if types.is_callable(repl):
            res.append(repl(m))
        elif "\\" in repl:
            res.append(m.quote_replacement(repl))
        else:
            res.append(repl)
        cnt_rpl += 1

        pos = end
        if beg == end:
            # Have progress on empty matches
            res.append(s[pos:pos + 1])
            pos += 1

        if count != 0:
            count -= 1
            if count == 0:
                res.append(s[pos:])
                break

    return ''.join(res), cnt_rpl


def _split(pattern, string, maxsplit=0, flags=0):
    """Split the source string by the occurrences of the pattern,
    returning a list containing the resulting substrings.  If
    capturing parentheses are used in pattern, then the text of all
    groups in the pattern are also returned as part of the resulting
    list.  If maxsplit is nonzero, at most maxsplit splits occur,
    and the remainder of the string is returned as the final element
    of the list."""
    return _compile(pattern, flags).patternobj.split(string, maxsplit)


def _findall(pattern, s, flags=0):
    """Return a list of all non-overlapping matches in the string.
    If one or more capturing groups are present in the pattern, return
    a list of groups; this will be a list of tuples if the pattern
    has more than one group.
    Empty matches are included in the result."""
    res = []
    m = _compile(pattern, flags).matcher(s)

    pos = 0
    finish = len(s)
    for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
        if pos > finish:
            break
        if not m.find(pos):
            break

        print("---> ", m.group(), ":::", m.group_count())
        num = m.group_count()
        if num == 0:
            res.append(m.group())
        elif num == 1:
            res.append(m.group(num))
        else:
            res.append(tuple([m.group(_i+1) for _i in range(num)]))

        print(res)
        beg, end = m.start(), m.end()
        pos = end
        if beg == end:
            # Have progress on empty matches
            pos += 1

    for i in range(len(res)):
        x = res[i]
        if types.is_tuple(x):
            res[i] = tuple(["%s" % x1 for x1 in x])
        else:
            res[i] = "%s" % x
    return res


def _finditer(pattern, string, flags=0):
    """Return an iterator over all non-overlapping matches in the
    string.  For each match, the iterator returns a Match object.
    Empty matches are included in the result."""
    pass
#
#     def finditer(self, s, pos=0, endpos=-1):
#         if endpos != -1:
#             s = s[:endpos]
#         res = []
#         finish = len(s)
#         while pos <= finish:
#             m = self.search(s, pos)
#             if not m:
#                 break
#             yield m
#             beg, end = m.span(0)
#             pos = end
#             if beg == end:
#                 # Have progress on empty matches
#                 pos += 1


def _compile(pattern, flags=0):
    "Compile a regular expression pattern, returning a Pattern object."
    pattern = _re2j.Pattern.compile(pattern, flags)
    return _pattern__init__(pattern)


def _purge():
    "Clear the regular expression caches"
    pass


def _template(pattern, flags=0):
    "Compile a template pattern, returning a Pattern object"
    # return _compile(pattern, flags|T)
    pass


# SPECIAL_CHARS
# closing ')', '}' and ']'
# '-' (a range in character set)
# '&', '~', (extended character set operations)
# '#' (comment) and WHITESPACE (ignored) in verbose mode
# _special_chars_map = {i: '\\' + chr(i) for i in bytes('()[]{}?*+-|^$\\.&~# \t\n\r')}

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
    # return pattern.translate(_special_chars_map)


re = larky.struct(
    compile=_compile,
    search=_search,
    match=_match,
    fullmatch=_fullmatch,
    split=_split,
    findall=_findall,
    finditer=_finditer,
    sub=_sub,
    subn=_subn,
    escape=_escape,
    purge=_purge,
    template=_template,
    **RegexFlags
)
