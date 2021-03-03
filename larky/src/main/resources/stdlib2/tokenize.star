def TokenInfo(collections.namedtuple('TokenInfo', 'type string start end line')):
    """
    '%d (%s)'
    """
    def exact_type(self):
        """
        '('
        """
def any(*choices): return group(*choices) + '*'
    """
    '*'
    """
def maybe(*choices): return group(*choices) + '?'
    """
    '?'
    """
def _all_string_prefixes():
    """
     The valid string prefixes. Only contain the lower case versions,
      and don't contain any permutations (include 'fr', but not
      'rf'). The various permutations will be generated.

    """
def _compile(expr):
    """
     Note that since _all_string_prefixes includes the empty string,
      StringPrefix can be the empty string (making it optional).

    """
def TokenError(Exception): pass
    """
    start ({},{}) precedes previous end ({},{})

    """
    def untokenize(self, iterable):
        """

        """
    def compat(self, token, iterable):
        """
        ' '
        """
def untokenize(iterable):
    """
    Transform tokens back into Python source code.
        It returns a bytes object, encoded using the ENCODING
        token, which is the first token sequence output by tokenize.

        Each element returned by the iterable must be a token sequence
        with at least two elements, a token number and token value.  If
        only two tokens are passed, the resulting output is poor.

        Round-trip invariant for full input:
            Untokenized source will match input source exactly

        Round-trip invariant for limited input:
            # Output bytes will tokenize back to the input
            t1 = [tok[:2] for tok in tokenize(f.readline)]
            newcode = untokenize(t1)
            readline = BytesIO(newcode).readline
            t2 = [tok[:2] for tok in tokenize(readline)]
            assert t1 == t2
    
    """
def _get_normal_name(orig_enc):
    """
    Imitates get_normal_name in tokenizer.c.
    """
def detect_encoding(readline):
    """

        The detect_encoding() function is used to detect the encoding that should
        be used to decode a Python source file.  It requires one argument, readline,
        in the same way as the tokenize() generator.

        It will call readline a maximum of twice, and return the encoding used
        (as a string) and a list of any lines (left as bytes) it has read in.

        It detects the encoding from the presence of a utf-8 bom or an encoding
        cookie as specified in pep-0263.  If both a bom and a cookie are present,
        but disagree, a SyntaxError will be raised.  If the encoding cookie is an
        invalid charset, raise a SyntaxError.  Note that if a utf-8 bom is found,
        'utf-8-sig' is returned.

        If no encoding is specified, then the default of 'utf-8' will be returned.
    
    """
    def read_or_stop():
        """
        b''
        """
    def find_cookie(line):
        """
         Decode as UTF-8. Either the line is an encoding declaration,
         in which case it should be pure ASCII, or it must be UTF-8
         per default encoding.

        """
def open(filename):
    """
    Open a file in read only mode using the encoding detected by
        detect_encoding().
    
    """
def tokenize(readline):
    """

        The tokenize() generator requires one argument, readline, which
        must be a callable object which provides the same interface as the
        readline() method of built-in file objects.  Each call to the function
        should return one line of input as bytes.  Alternatively, readline
        can be a callable function terminating with StopIteration:
            readline = open(myfile, 'rb').__next__  # Example of alternate readline

        The generator produces 5-tuples with these members: the token type; the
        token string; a 2-tuple (srow, scol) of ints specifying the row and
        column where the token begins in the source; a 2-tuple (erow, ecol) of
        ints specifying the row and column where the token ends in the source;
        and the line on which the token was found.  The line passed is the
        physical line.

        The first token sequence will always be an ENCODING token
        which tells you which encoding was used to decode the bytes stream.
    
    """
def _tokenize(readline, encoding):
    """
    '0123456789'
    """
def generate_tokens(readline):
    """
    Tokenize a source reading Python code as unicode strings.

        This has the same API as tokenize(), except that it expects the *readline*
        callable to return str objects instead of bytes.
    
    """
def main():
    """
     Helper error handling routines

    """
    def error(message, filename=None, location=None):
        """
        %s:%d:%d: error: %s
        """
