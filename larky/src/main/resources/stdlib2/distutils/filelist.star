def FileList:
    """
    A list of files built by on exploring the filesystem and filtered by
        applying various patterns to what we find there.

        Instance attributes:
          dir
            directory from which files will be taken -- only used if
            'allfiles' not supplied to constructor
          files
            list of filenames currently being built/filtered/manipulated
          allfiles
            complete list of files under consideration (ie. without any
            filtering applied)
    
    """
    def __init__(self, warn=None, debug_print=None):
        """
         ignore argument to FileList, but keep them for backwards
         compatibility

        """
    def set_allfiles(self, allfiles):
        """
        Print 'msg' to stdout if the global DEBUG (taken from the
                DISTUTILS_DEBUG environment variable) flag is true.
        
        """
    def append(self, item):
        """
         Not a strict lexical sort!

        """
    def remove_duplicates(self):
        """
         Assumes list has been sorted!

        """
    def _parse_template_line(self, line):
        """
        'include'
        """
    def process_template_line(self, line):
        """
         Parse the line: split it up, make sure the right number of words
         is there, and return the relevant words.  'action' is always
         defined: it's the first word of the line.  Which of the other
         three are defined depends on the action; it'll be either
         patterns, (dir and patterns), or (dir_pattern).

        """
    def include_pattern(self, pattern, anchor=1, prefix=None, is_regex=0):
        """
        Select strings (presumably filenames) from 'self.files' that
                match 'pattern', a Unix-style wildcard (glob) pattern.  Patterns
                are not quite the same as implemented by the 'fnmatch' module: '*'
                and '?'  match non-special characters, where "special" is platform-
                dependent: slash on Unix; colon, slash, and backslash on
                DOS/Windows; and colon on Mac OS.

                If 'anchor' is true (the default), then the pattern match is more
                stringent: "*.py" will match "foo.py" but not "foo/bar.py".  If
                'anchor' is false, both of these will match.

                If 'prefix' is supplied, then only filenames starting with 'prefix'
                (itself a pattern) and ending with 'pattern', with anything in between
                them, will match.  'anchor' is ignored in this case.

                If 'is_regex' is true, 'anchor' and 'prefix' are ignored, and
                'pattern' is assumed to be either a string containing a regex or a
                regex object -- no translation is done, the regex is just compiled
                and used as-is.

                Selected strings will be added to self.files.

                Return True if files are found, False otherwise.
        
        """
2021-03-02 20:46:27,108 : INFO : tokenize_signature : --> do i ever get here?
    def exclude_pattern (self, pattern,
                         anchor=1, prefix=None, is_regex=0):
        """
        Remove strings (presumably filenames) from 'files' that match
                'pattern'.  Other parameters are the same as for
                'include_pattern()', above.
                The list 'self.files' is modified in place.
                Return True if files are found, False otherwise.
        
        """
def _find_all_simple(path):
    """

        Find all files under 'path'
    
    """
def findall(dir=os.curdir):
    """

        Find all files under 'dir' and return the list of full filenames.
        Unless dir is '.', return full filenames with dir prepended.
    
    """
def glob_to_re(pattern):
    """
    Translate a shell-like glob pattern to a regular expression; return
        a string containing the regex.  Differs from 'fnmatch.translate()' in
        that '*' does not match "special characters" (which are
        platform-specific).
    
    """
def translate_pattern(pattern, anchor=1, prefix=None, is_regex=0):
    """
    Translate a shell-like wildcard pattern to a compiled regular
        expression.  Return the compiled regex.  If 'is_regex' true,
        then 'pattern' is directly compiled to a regex (if it's a string)
        or just returned as-is (assumes it's a regex object).
    
    """
