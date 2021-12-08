"""
Emulates python's re module but using Google's re2. More on the syntax and
 what is allowed and what is not here:

  https://github.com/google/re2/wiki/Syntax

Java's standard regular expression package, java.util.regex, and many other
widely used regular expression packages such as PCRE, Perl and Python use a
backtracking implementation strategy: when a pattern presents two alternatives
such as a|b, the engine will try to match subpattern a first, and if that yields
no match, it will reset the input stream and try to match b instead.

If such choices are deeply nested, this strategy requires an exponential number
of passes over the input data before it can detect whether the input matches. If
the input is large, it is easy to construct a pattern whose running time would
exceed the lifetime of the universe. This creates a security risk when accepting
regular expression patterns from untrusted sources, such as users of a web
application.

In contrast, the RE2 algorithm explores all matches simultaneously in a single
pass over the input data by using a nondeterministic finite automaton.

There are certain features of PCRE or Perl regular expressions that cannot be
implemented in linear time, for example, backreferences, but the vast majority
of regular expressions patterns in practice avoid such features.

A good portion of `findall` and `finditer` code was ported from:
pfalcon's pycopy-lib located at:
   https://github.com/pfalcon/pycopy-lib/tree/master/re-pcre
"""
load("@stdlib//larky", "larky")
load("@stdlib//types", "types")
load("@stdlib//enum", "enum")
load("@stdlib//re2j", _re2j="re2j")


_WHILE_LOOP_EMULATION_ITERATION = 1000

__ = -1  # Alias for the invalid class
RegexFlags = enum.enumify_iterable(iterable=[
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

    def span(group=0):
        if group == 0:
            return matchobj.start(group), matchobj.end(group)

        for idx, items in enumerate(matchobj.groupdict().items(), start=1):
            groupname, groupvalue = items
            if groupname == group:
                return matchobj.start(idx), matchobj.end(idx)

    def __str__():
        if matchobj == None:
            return None
        return '<re.Match object; span=%s, match=%r; groups=%r>' % (
            span(), group(), groups()
        )

    return larky.struct(
        group=group,
        groups=groups,
        find=matchobj.find,
        search=matchobj.search,
        pattern=matchobj.pattern,
        start=matchobj.start,
        end=matchobj.end,
        span=span,
        group_count=matchobj.group_count,
        groupdict=matchobj.groupdict,
        matches=matchobj.matches,
        looking_at=matchobj.looking_at,
        replace_first=matchobj.replace_first,
        replace_all=matchobj.replace_all,
        append_tail=matchobj.append_tail,
        append_replacement=matchobj.append_replacement,
        quote_replacement=matchobj.quote_replacement,
        __str__=__str__,
    )


def _pattern__init__(patternobj):

    def matcher(string):
        return _matcher__init__(patternobj.matcher(string))

    def match(string, pos=0, endpos=-1):
        m = matcher(string)
        if not m.looking_at(pos, endpos):
            return None
        return m

    def fullmatch(string, pos=0, endpos=-1):
        m = matcher(string)
        if not m.matches():
            return None
        return m

    def search(string, pos=0, endpos=-1):
        m = matcher(string)
        if not m.search(pos, endpos):
            return None
        return m

    def sub(repl, string, count=0):
        new_string, _number = subn(repl, string, count)
        return new_string

    def subn(repl, string, count=0):
        return _native_subn(repl, string, count)

    def _native_subn(repl, string, count=0):
        _matcher = matcher(string)
        res = []
        cnt_rpl = 0
        # TODO: this can sometimes limit results
        # based only number of matches less than or equal
        # to _WHILE_LOOP_EMULATION_ITERATION which might
        # yield incomplete results.
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

    def _larky_subn(repl, s, count=0):
        res = []
        pos = 0
        cnt_rpl = 0
        finish = len(s)
        m = matcher(s)

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

    # def split(string, maxsplit=0):
    #     return patternobj.split(string, maxsplit)

    def findall(s, pos=0, endpos=-1):
        if endpos != -1:
            s = s[:endpos]

        res = []
        finish = len(s)
        m = matcher(s)

        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if pos > finish:
                break
            if not m.find(pos):
                break

            #print("---> ", m.group(), ":::", m.group_count())
            num = m.group_count()
            if num == 0:
                res.append(m.group())
            elif num == 1:
                res.append(m.group(num))
            else:
                res.append(tuple([m.group(_i+1) for _i in range(num)]))

            #print(res)
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

    def finditer(string, pos=0, endpos=-1):
        # no generator/yield in starlark
        if endpos != -1:
            string = string[:endpos]

        res = []
        finish = len(string)
        m = matcher(string)

        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if pos > finish:
                break
            if not m.find(pos):
                break
            # copy matcher + set it to the position of the match
            clone = matcher(string)
            clone.find(pos)

            # return the matched object
            res.append(clone)
            beg, end = m.start(), m.end()
            pos = end
            if beg == end:
                # Have progress on empty matches
                pos += 1
        return res


    return larky.struct(
        search=search,
        match=match,
        fullmatch=fullmatch,
        sub=sub,
        subn=subn,
        findall=findall,
        finditer=finditer,
        matcher=matcher,
        split=patternobj.split,
        patternobj=patternobj,
        pattern=str(patternobj)
    )
# --------------------------------------------------------------------
# public interface


def _match(pattern, string, flags=0):
    """Try to apply the pattern at the start of the string, returning
    a Match object, or None if no match was found."""
    _rx_pattern = _compile(pattern, flags)
    return _rx_pattern.match(string)


def _fullmatch(pattern, string, flags=0):
    """Try to apply the pattern to all of the string, returning
    a Match object, or None if no match was found."""
    _rx_pattern = _compile(pattern, flags)
    return _rx_pattern.fullmatch(string)


def _search(pattern, string, flags=0):
    """Scan through string looking for a match to the pattern, returning
    a Match object, or None if no match was found."""
    _rx_pattern = _compile(pattern, flags)
    return _rx_pattern.search(string)


def _sub(pattern, repl, string, count=0, flags=0):
    """Return the string obtained by replacing the leftmost
    non-overlapping occurrences of the pattern in string by the
    replacement repl.  repl can be either a string or a callable;
    if a string, backslash escapes in it are processed.  If it is
    a callable, it's passed the Match object and must return
    a replacement string to be used."""
    _rx_pattern = _compile(pattern, flags)
    return _rx_pattern.sub(repl, string, count)
    # new_string, _number = _subn(pattern, repl, string, count, flags)
    # return new_string


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
    _rx_pattern = _compile(pattern, flags)
    return _rx_pattern.subn(repl, string, count)
    #return _native_subn(pattern, string, repl, count, flags)


def _split(pattern, string, maxsplit=0, flags=0):
    """Split the source string by the occurrences of the pattern,
    returning a list containing the resulting substrings.  If
    capturing parentheses are used in pattern, then the text of all
    groups in the pattern are also returned as part of the resulting
    list.  If maxsplit is nonzero, at most maxsplit splits occur,
    and the remainder of the string is returned as the final element
    of the list."""
    _rx_pattern = _compile(pattern, flags)
    return _rx_pattern.split(string, maxsplit)


def _findall(pattern, string, flags=0):
    """Return a list of all non-overlapping matches in the string.
    If one or more capturing groups are present in the pattern, return
    a list of groups; this will be a list of tuples if the pattern
    has more than one group.
    Empty matches are included in the result."""
    _rx_pattern = _compile(pattern, flags)
    return _rx_pattern.findall(string)


def _finditer(pattern, string, flags=0):
    """Return an iterator over all non-overlapping matches in the
    string.  For each match, the iterator returns a Match object.
    Empty matches are included in the result."""
    _rx_pattern = _compile(pattern, flags)
    return _rx_pattern.finditer(string)


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
