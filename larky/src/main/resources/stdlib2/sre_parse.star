def Verbose(Exception):
    """
     keeps track of state for parsing

    """
    def __init__(self):
        """
         group 0
        """
    def groups(self):
        """
        too many groups
        """
    def closegroup(self, gid, p):
        """
        'cannot refer to an open group'
        """
def SubPattern:
    """
     a subpattern, in intermediate form

    """
    def __init__(self, state, data=None):
        """
  
        """
    def __repr__(self):
        """
         determine the width (min, max) for this subpattern

        """
def Tokenizer:
    """
    'latin1'
    """
    def __next(self):
        """
        \\
        """
    def match(self, char):
        """
        ''
        """
    def getuntil(self, terminator, name):
        """
        ''
        """
    def pos(self):
        """
        ''
        """
    def tell(self):
        """
        ''
        """
    def seek(self, index):
        """
         handle escape code inside character class

        """
def _escape(source, escape, state):
    """
     handle escape code in expression

    """
def _uniq(items):
    """
     parse an alternation: a|b|c


    """
def _parse(source, state, verbose, nested, first=False):
    """
     parse a simple pattern

    """
def _parse_flags(source, state, char):
    """
    -
    """
def fix_flags(src, flags):
    """
     Check and fix flags according to the type of pattern (str or bytes)

    """
def parse(str, flags=0, state=None):
    """
     parse 're' pattern into list of (opcode, argument) tuples


    """
def parse_template(source, state):
    """
     parse 're' replacement string into list of literals and
     group references

    """
    def addgroup(index, pos):
        """
        invalid group reference %d
        """
def expand_template(template, match):
    """
    invalid group reference %d
    """
