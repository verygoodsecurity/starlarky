def get(root):
    """
    '''Return the singleton SearchEngine instance for the process.

        The single SearchEngine saves settings between dialog instances.
        If there is not a SearchEngine already, make one.
        '''
    """
def SearchEngine:
    """
    Handles searching a text widget for Find, Replace, and Grep.
    """
    def __init__(self, root):
        """
        '''Initialize Variables that save search state.

                The dialogs bind these to the UI elements present in the dialogs.
                '''
        """
    def getpat(self):
        """
         Higher level access methods


        """
    def setcookedpat(self, pat):
        """
        Set pattern after escaping if re.
        """
    def getcookedpat(self):
        """
         if True, see setcookedpat
        """
    def getprog(self):
        """
        Return compiled cooked search pattern.
        """
    def report_error(self, pat, msg, col=-1):
        """
         Derived class could override this with something fancier

        """
    def search_text(self, text, prog=None, ok=0):
        """
        '''Return (lineno, matchobj) or None for forward/backward search.

                This function calls the right function with the right arguments.
                It directly return the result of that call.

                Text is a text widget. Prog is a precompiled pattern.
                The ok parameter is a bit complicated as it has two effects.

                If there is a selection, the search begin at either end,
                depending on the direction setting and ok, with ok meaning that
                the search starts with the selection. Otherwise, search begins
                at the insert mark.

                To aid progress, the search functions do not return an empty
                match at the starting position unless ok is True.
                '''
        """
    def search_forward(self, text, prog, line, col, wrap, ok=0):
        """
        %d.0
        """
    def search_backward(self, text, prog, line, col, wrap, ok=0):
        """
        %d.0
        """
def search_reverse(prog, chars, col):
    """
    '''Search backwards and return an re match object or None.

        This is done by searching forwards until there is no match.
        Prog: compiled re object with a search method returning a match.
        Chars: line of text, without \\n.
        Col: stop index for the search; the limit for match.end().
        '''
    """
def get_selection(text):
    """
    '''Return tuple of 'line.col' indexes from selection or insert mark.
        '''
    """
def get_line_col(index):
    """
    '''Return (line, col) tuple of ints from 'line.col' string.'''
    """
