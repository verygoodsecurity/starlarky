def MinNode(object):
    """
    This class serves as an intermediate representation of the
        pattern tree during the conversion to sets of leaf-to-root
        subpatterns
    """
    def __init__(self, type=None, name=None):
        """
        ' '
        """
    def leaf_to_root(self):
        """
        Internal method. Returns a characteristic path of the
                pattern tree. This method must be run for all leaves until the
                linear subpatterns are merged into a single
        """
    def get_linear_subpattern(self):
        """
        Drives the leaf_to_root method. The reason that
                leaf_to_root must be run multiple times is because we need to
                reject 'group' matches; for example the alternative form
                (a | b c) creates a group [b c] that needs to be matched. Since
                matching multiple linear patterns overcomes the automaton's
                capabilities, leaf_to_root merges each group into a single
                choice based on 'characteristic'ity,

                i.e. (a|b c) -> (a|b) if b more characteristic than c

                Returns: The most 'characteristic'(as defined by
                  get_characteristic_subpattern) path for the compiled pattern
                  tree.
        
        """
    def leaves(self):
        """
        Generator that returns the leaves of the tree
        """
def reduce_tree(node, parent=None):
    """

        Internal function. Reduces a compiled pattern tree to an
        intermediate representation suitable for feeding the
        automaton. This also trims off any optional pattern elements(like
        [a], a*).
    
    """
def get_characteristic_subpattern(subpatterns):
    """
    Picks the most characteristic from a list of linear patterns
        Current order used is:
        names > common_names > common_chars
    
    """
def rec_test(sequence, test_func):
    """
    Tests test_func on all items of sequence and items of included
        sub-iterables
    """
