def Driver(object):
    """
    Parse a series of tokens and return the syntax tree.
    """
    def parse_stream_raw(self, stream, debug=False):
        """
        Parse a stream and return the syntax tree.
        """
    def parse_stream(self, stream, debug=False):
        """
        Parse a stream and return the syntax tree.
        """
    def parse_file(self, filename, encoding=None, debug=False):
        """
        Parse a file and return the syntax tree.
        """
    def parse_string(self, text, debug=False):
        """
        Parse a string and return the syntax tree.
        """
def _generate_pickle_name(gt):
    """
    .txt
    """
2021-03-02 20:54:14,898 : INFO : tokenize_signature : --> do i ever get here?
def load_grammar(gt="Grammar.txt", gp=None,
                 save=True, force=False, logger=None):
    """
    Load the grammar (maybe from a pickle).
    """
def _newer(a, b):
    """
    Inquire whether file a was written since file b.
    """
def load_packaged_grammar(package, grammar_source):
    """
    Normally, loads a pickled grammar by doing
            pkgutil.get_data(package, pickled_grammar)
        where *pickled_grammar* is computed from *grammar_source* by adding the
        Python version and using a ``.pickle`` extension.

        However, if *grammar_source* is an extant file, load_grammar(grammar_source)
        is called instead. This facilitates using a packaged grammar file when needed
        but preserves load_grammar's automatic regeneration behavior when possible.

    
    """
def main(*args):
    """
    Main program, when run as a script: produce grammar pickle files.

        Calls load_grammar for each argument, a path to a grammar text file.
    
    """
