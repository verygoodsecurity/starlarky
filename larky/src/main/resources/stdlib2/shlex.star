def shlex:
    """
    A lexical analyzer class for simple shell-like syntaxes.
    """
2021-03-02 20:53:50,572 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, instream=None, infile=None, posix=False,
                 punctuation_chars=False):
        """
        ''
        """
    def punctuation_chars(self):
        """
        Push a token onto the stack popped by the get_token method
        """
    def push_source(self, newstream, newfile=None):
        """
        Push an input source onto the lexer's input source stack.
        """
    def pop_source(self):
        """
        Pop the input source stack.
        """
    def get_token(self):
        """
        Get a token from the input stream (or from stack if it's nonempty)
        """
    def read_token(self):
        """
        ' '
        """
    def sourcehook(self, newfile):
        """
        Hook called on a filename to be sourced.
        """
    def error_leader(self, infile=None, lineno=None):
        """
        Emit a C-compiler-like, Emacs-friendly error-message leader.
        """
    def __iter__(self):
        """
        Split the string *s* using shell-like syntax.
        """
def join(split_command):
    """
    Return a shell-escaped string from *split_command*.
    """
def quote(s):
    """
    Return a shell-escaped version of the string *s*.
    """
def _print_tokens(lexer):
    """
    Token: 
    """
