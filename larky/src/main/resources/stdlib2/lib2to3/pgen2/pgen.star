def PgenGrammar(grammar.Grammar):
    """
     Initialize lookahead
    """
    def make_grammar(self):
        """
        assert ilabel not in first # XXX failed on <> ... !=

        """
    def make_label(self, c, label):
        """
         XXX Maybe this should be a method on a subclass of converter?

        """
    def addfirstsets(self):
        """
        print name, self.first[name].keys()


        """
    def calcfirst(self, name):
        """
         dummy to detect left recursion
        """
    def parse(self):
        """
         MSTART: (NEWLINE | RULE)* ENDMARKER

        """
    def make_dfa(self, start, finish):
        """
         To turn an NFA into a DFA, we define the states of the DFA
         to correspond to *sets* of states of the NFA.  Then do some
         state reduction.  Let's represent sets as dicts with 1 for
         values.

        """
        def closure(state):
            """
             NB states grows while we're iterating
            """
    def dump_nfa(self, name, start, finish):
        """
        Dump of NFA for
        """
    def dump_dfa(self, name, dfa):
        """
        Dump of DFA for
        """
    def simplify_dfa(self, dfa):
        """
         This is not theoretically optimal, but works well enough.
         Algorithm: repeatedly look for two states that have the same
         set of arcs (same labels pointing to the same nodes) and
         unify them, until things stop changing.

         dfa is a list of DFAState instances

        """
    def parse_rhs(self):
        """
         RHS: ALT ('|' ALT)*

        """
    def parse_alt(self):
        """
         ALT: ITEM+

        """
    def parse_item(self):
        """
         ITEM: '[' RHS ']' | ATOM ['+' | '*']

        """
    def parse_atom(self):
        """
         ATOM: '(' RHS ')' | NAME | STRING

        """
    def expect(self, type, value=None):
        """
        expected %s/%s, got %s/%s
        """
    def gettoken(self):
        """
        print token.tok_name[self.type], repr(self.value)


        """
    def raise_error(self, msg, *args):
        """
 
        """
def NFAState(object):
    """
     list of (label, NFAState) pairs
    """
    def addarc(self, next, label=None):
        """
         map from label to DFAState
        """
    def addarc(self, next, label):
        """
         Equality test -- ignore the nfaset instance variable

        """
def generate_grammar(filename="Grammar.txt"):
