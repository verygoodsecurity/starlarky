def Version:
    """
    Abstract base class for version numbering classes.  Just provides
        constructor (__init__) and reproducer (__repr__), because those
        seem to be the same for all version numbering classes; and route
        rich comparisons to _cmp.
    
    """
    def __init__ (self, vstring=None):
        """
        %s ('%s')
        """
    def __eq__(self, other):
        """
         Interface for version-number classes -- must be implemented
         by the following classes (the concrete ones -- Version should
         be treated as an abstract class).
            __init__ (string) - create and take same action as 'parse'
                                (string parameter is optional)
            parse (string)    - convert a string representation to whatever
                                internal representation is appropriate for
                                this style of version numbering
            __str__ (self)    - convert back to a string; should be very similar
                                (if not identical to) the string supplied to parse
            __repr__ (self)   - generate Python code to recreate
                                the instance
            _cmp (self, other) - compare two version numbers ('other' may
                                be an unparsed version string, or another
                                instance of your version class)



        """
def StrictVersion (Version):
    """
    Version numbering for anal retentives and software idealists.
        Implements the standard interface for version number classes as
        described above.  A version number consists of two or three
        dot-separated numeric components, with an optional "pre-release" tag
        on the end.  The pre-release tag consists of the letter 'a' or 'b'
        followed by a number.  If the numeric components of two version
        numbers are equal, then one with a pre-release tag will always
        be deemed earlier (lesser) than one without.

        The following are valid version numbers (shown in the order that
        would be obtained by sorting according to the supplied cmp function):

            0.4       0.4.0  (these two are equivalent)
            0.4.1
            0.5a1
            0.5b3
            0.5
            0.9.6
            1.0
            1.0.4a3
            1.0.4b1
            1.0.4

        The following are examples of invalid version numbers:

            1
            2.7.2.2
            1.3.a4
            1.3pl1
            1.3c4

        The rationale for this version numbering system will be explained
        in the distutils documentation.
    
    """
    def parse (self, vstring):
        """
        invalid version number '%s'
        """
    def __str__ (self):
        """
        '.'
        """
    def _cmp (self, other):
        """
         numeric versions don't match
         prerelease stuff doesn't matter

        """
def LooseVersion (Version):
    """
    Version numbering for anarchists and software realists.
        Implements the standard interface for version number classes as
        described above.  A version number consists of a series of numbers,
        separated by either periods or strings of letters.  When comparing
        version numbers, the numeric components will be compared
        numerically, and the alphabetic components lexically.  The following
        are all valid version numbers, in no particular order:

            1.5.1
            1.5.2b2
            161
            3.10a
            8.02
            3.4j
            1996.07.12
            3.2.pl0
            3.1.1.6
            2g6
            11g
            0.960923
            2.2beta29
            1.13++
            5.5.kw
            2.0b1pl0

        In fact, there is no such thing as an invalid version number under
        this scheme; the rules for comparison are simple and predictable,
        but may not always give the results you want (for some definition
        of "want").
    
    """
    def __init__ (self, vstring=None):
        """
         I've given up on thinking I can reconstruct the version string
         from the parsed tuple -- so I just store the string here for
         use by __str__

        """
    def __str__ (self):
        """
        LooseVersion ('%s')
        """
    def _cmp (self, other):
        """
         end class LooseVersion

        """
