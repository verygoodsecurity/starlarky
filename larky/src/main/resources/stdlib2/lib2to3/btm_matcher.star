def BMNode(object):
    """
    Class for a node of the Aho-Corasick automaton used in matching
    """
    def __init__(self):
        """
        ''
        """
def BottomMatcher(object):
    """
    The main matcher class. After instantiating the patterns should
        be added using the add_fixer method
    """
    def __init__(self):
        """
        RefactoringTool
        """
    def add_fixer(self, fixer):
        """
        Reduces a fixer's pattern tree to a linear path and adds it
                to the matcher(a common Aho-Corasick automaton). The fixer is
                appended on the matching states and called when they are
                reached
        """
    def add(self, pattern, start):
        """
        Recursively adds a linear pattern to the AC automaton
        """
    def run(self, leaves):
        """
        The main interface with the bottom matcher. The tree is
                traversed from the bottom using the constructed
                automaton. Nodes are only checked once as the tree is
                retraversed. When the automaton fails, we give it one more
                shot(in case the above tree matches as a whole with the
                rejected leaf), then we break for the next leaf. There is the
                special case of multiple arguments(see code comments) where we
                recheck the nodes

                Args:
                   The leaves of the AST tree to be matched

                Returns:
                   A dictionary of node matches with fixers as the keys
        
        """
    def print_ac(self):
        """
        Prints a graphviz diagram of the BM automaton(for debugging)
        """
        def print_node(node):
            """
            %d -> %d [label=%s] //%s
            """
def type_repr(type_num):
    """
     printing tokens is possible but not as useful
     from .pgen2 import token // token.__dict__.items():

    """
