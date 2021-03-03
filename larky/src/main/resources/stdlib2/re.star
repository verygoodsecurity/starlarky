def RegexFlag(enum.IntFlag):
    """
     assume ascii "locale
    """
    def __repr__(self):
        """
        f're.{self._name_}'
        """
def match(pattern, string, flags=0):
    """
    Try to apply the pattern at the start of the string, returning
        a Match object, or None if no match was found.
    """
def fullmatch(pattern, string, flags=0):
    """
    Try to apply the pattern to all of the string, returning
        a Match object, or None if no match was found.
    """
def search(pattern, string, flags=0):
    """
    Scan through string looking for a match to the pattern, returning
        a Match object, or None if no match was found.
    """
def sub(pattern, repl, string, count=0, flags=0):
    """
    Return the string obtained by replacing the leftmost
        non-overlapping occurrences of the pattern in string by the
        replacement repl.  repl can be either a string or a callable;
        if a string, backslash escapes in it are processed.  If it is
        a callable, it's passed the Match object and must return
        a replacement string to be used.
    """
def subn(pattern, repl, string, count=0, flags=0):
    """
    Return a 2-tuple containing (new_string, number).
        new_string is the string obtained by replacing the leftmost
        non-overlapping occurrences of the pattern in the source
        string by the replacement repl.  number is the number of
        substitutions that were made. repl can be either a string or a
        callable; if a string, backslash escapes in it are processed.
        If it is a callable, it's passed the Match object and must
        return a replacement string to be used.
    """
def split(pattern, string, maxsplit=0, flags=0):
    """
    Split the source string by the occurrences of the pattern,
        returning a list containing the resulting substrings.  If
        capturing parentheses are used in pattern, then the text of all
        groups in the pattern are also returned as part of the resulting
        list.  If maxsplit is nonzero, at most maxsplit splits occur,
        and the remainder of the string is returned as the final element
        of the list.
    """
def findall(pattern, string, flags=0):
    """
    Return a list of all non-overlapping matches in the string.

        If one or more capturing groups are present in the pattern, return
        a list of groups; this will be a list of tuples if the pattern
        has more than one group.

        Empty matches are included in the result.
    """
def finditer(pattern, string, flags=0):
    """
    Return an iterator over all non-overlapping matches in the
        string.  For each match, the iterator returns a Match object.

        Empty matches are included in the result.
    """
def compile(pattern, flags=0):
    """
    Compile a regular expression pattern, returning a Pattern object.
    """
def purge():
    """
    Clear the regular expression caches
    """
def template(pattern, flags=0):
    """
    Compile a template pattern, returning a Pattern object
    """
def escape(pattern):
    """

        Escape special characters in a string.
    
    """
def _compile(pattern, flags):
    """
     internal: compile pattern

    """
def _compile_repl(repl, pattern):
    """
     internal: compile replacement pattern

    """
def _expand(pattern, match, template):
    """
     internal: Match.expand implementation hook

    """
def _subx(pattern, template):
    """
     internal: Pattern.sub/subn implementation helper

    """
    def filter(match, template=template):
        """
         register myself for pickling


        """
def _pickle(p):
    """
     --------------------------------------------------------------------
     experimental stuff (see python-dev discussions for details)


    """
    def __init__(self, lexicon, flags=0):
        """
         combine phrases into a compound pattern

        """
    def scan(self, string):
