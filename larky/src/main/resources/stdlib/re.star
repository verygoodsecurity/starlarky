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
load("@stdlib//larky", larky="larky")
load("@stdlib//types", "types")
load("@stdlib//enum", "enum")
load("@stdlib//re2j", _re2j="re2j")


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



def _group_text(string, spans, index, default=None):
    start = spans[2 * index]
    if start < 0:
        return default
    return string[start:spans[2 * index + 1]]


def _expand_template(template, string, spans):
    """Expands a template from py_template(); unmatched groups expand to ''."""
    empty = ""
    if len(template) == 1 and not types.is_int(template[0]):
        return template[0]
    return empty.join([
        _group_text(string, spans, item, empty) if types.is_int(item) else item
        for item in template
    ])


def _Match(pattern, string, pos, endpos, spans):
    """A Python re.Match object for the flat group spans returned by the py_* methods."""
    rx = pattern.patternobj
    ngroups = rx.py_groups

    def _index(group):
        if group == None:  # accepted for compatibility with earlier Larky
            return 0
        return rx.py_group_index(group)

    def group(*args):
        if len(args) == 0:
            return _group_text(string, spans, 0)
        if len(args) == 1:
            return _group_text(string, spans, _index(args[0]))
        return tuple([_group_text(string, spans, _index(g)) for g in args])

    def groups(default=None):
        return tuple([
            _group_text(string, spans, i, default)
            for i in range(1, ngroups + 1)
        ])

    def groupdict(default=None):
        return {
            name: _group_text(string, spans, index, default)
            for name, index in rx.py_groupindex().items()
        }

    def span(group=0):
        index = _index(group)
        return (spans[2 * index], spans[2 * index + 1])

    def start(group=0):
        return span(group)[0]

    def end(group=0):
        return span(group)[1]

    def expand(template):
        return _expand_template(rx.py_template(template), string, spans)

    def __repr__():
        return "<re.Match object; span=%r, match=%r>" % (span(), group())

    return larky.struct(
        __name__="Match",
        group=group,
        groups=groups,
        groupdict=groupdict,
        start=start,
        end=end,
        span=span,
        expand=expand,
        group_count=lambda: ngroups,
        pos=pos,
        endpos=endpos,
        re=pattern,
        string=string,
        regs=tuple([(spans[2 * i], spans[2 * i + 1]) for i in range(ngroups + 1)]),
        __repr__=__repr__,
        __str__=__repr__,
    )


def _pattern__init__(patternobj, flags):
    rx = patternobj

    def _match_obj(string, pos, endpos, spans):
        if spans == None:
            return None
        if endpos == None or endpos > len(string):
            endpos = len(string)
        return _Match(self, rx.py_text(string), pos, endpos, spans)

    def match(string, pos=0, endpos=None):
        return _match_obj(string, pos, endpos, rx.py_match(string, pos, endpos))

    def fullmatch(string, pos=0, endpos=None):
        return _match_obj(string, pos, endpos, rx.py_fullmatch(string, pos, endpos))

    def search(string, pos=0, endpos=None):
        return _match_obj(string, pos, endpos, rx.py_search(string, pos, endpos))

    def _all_spans(string, pos, endpos):
        # Successive matches as in CPython 3.7+: the search continues where the last match
        # ended, and an empty match is not allowed right where the previous match ended empty.
        res = []
        must_advance = False
        for _while_ in larky.while_true():
            spans = rx.py_search(string, pos, endpos, must_advance)
            if spans == None:
                break
            res.append(spans)
            must_advance = spans[0] == spans[1]
            pos = spans[1]
        return res

    def findall(string, pos=0, endpos=None):
        ngroups = rx.py_groups
        text = rx.py_text(string)
        res = []
        for spans in _all_spans(string, pos, endpos):
            if ngroups == 0:
                res.append(_group_text(text, spans, 0))
            elif ngroups == 1:
                res.append(_group_text(text, spans, 1, ""))
            else:
                res.append(tuple([
                    _group_text(text, spans, i, "")
                    for i in range(1, ngroups + 1)
                ]))
        return res

    def finditer(string, pos=0, endpos=None):
        # no generator/yield in starlark
        return [
            _match_obj(string, pos, endpos, spans)
            for spans in _all_spans(string, pos, endpos)
        ]

    def sub(repl, string, count=0):
        new_string, _number = subn(repl, string, count)
        return new_string

    def subn(repl, string, count=0):
        template = None
        text = rx.py_text(string)
        if not types.is_callable(repl):
            template = rx.py_template(repl)
        res = []
        n = 0
        pos = 0
        last = 0
        must_advance = False
        for _while_ in larky.while_true():
            if count > 0 and n >= count:
                break
            spans = rx.py_search(string, pos, None, must_advance)
            if spans == None:
                break
            res.append(text[last:spans[0]])
            if template == None:
                res.append(repl(_match_obj(string, 0, None, spans)))
            else:
                res.append(_expand_template(template, text, spans))
            n += 1
            last = spans[1]
            pos = spans[1]
            must_advance = spans[0] == spans[1]
        res.append(text[last:])
        return "".join(res), n

    def split(string, maxsplit=0):
        spans = rx.py_split(string, maxsplit)
        text = rx.py_text(string)
        return [
            text[spans[2 * i]:spans[2 * i + 1]] if spans[2 * i] >= 0 else None
            for i in range(len(spans) // 2)
        ]

    self = larky.struct(
        __name__="Pattern",
        search=search,
        match=match,
        fullmatch=fullmatch,
        sub=sub,
        subn=subn,
        findall=findall,
        finditer=finditer,
        split=split,
        matcher=patternobj.matcher,
        patternobj=patternobj,
        pattern=patternobj.pattern(),
        flags=flags,
        groups=patternobj.py_groups,
        groupindex=patternobj.py_groupindex(),
        __repr__=lambda: "re.compile(%r)" % patternobj.pattern(),
    )
    return self
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


def _subn(pattern, repl, string, count=0, flags=0):
    """Return a 2-tuple containing (new_string, number).
    new_string is the string obtained by replacing the leftmost
    non-overlapping occurrences of the pattern in the source
    string by the replacement repl.  number is the number of
    substitutions that were made. repl can be either a string or a
    callable; if a string, backslash escapes in it are processed.
    If it is a callable, it's passed the Match object and must
    return a replacement string to be used."""
    _rx_pattern = _compile(pattern, flags)
    return _rx_pattern.subn(repl, string, count)


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
    if not types.is_string(pattern) and hasattr(pattern, "patternobj"):
        # already compiled
        if flags != 0:
            fail("ValueError: cannot process flags argument with a compiled pattern")
        return pattern
    return _pattern__init__(_re2j.Pattern.py_compile(pattern, flags), flags)


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
