def HyperParser:
    """
    To initialize, analyze the surroundings of the given index.
    """
        def index2line(index):
            """
            .0
            """
    def set_index(self, index):
        """
        Set the index to which the functions relate.

                The index must be in the same statement.
        
        """
    def is_in_string(self):
        """
        Is the index given to the HyperParser in a string?
        """
    def is_in_code(self):
        """
        Is the index given to the HyperParser in normal code?
        """
    def get_surrounding_brackets(self, openers='([{', mustclose=False):
        """
        Return bracket indexes or None.

                If the index given to the HyperParser is surrounded by a
                bracket defined in openers (or at least has one before it),
                return the indices of the opening bracket and the closing
                bracket (or the end of line, whichever comes first).

                If it is not surrounded by brackets, or the end of line comes
                before the closing bracket and mustclose is True, returns None.
        
        """
    def _eat_identifier(cls, str, limit, pos):
        """
        Given a string and pos, return the number of chars in the
                identifier which ends at pos, or 0 if there is no such one.

                This ignores non-identifier eywords are not identifiers.
        
        """
    def get_expression(self):
        """
        Return a string with the Python expression which ends at the
                given index, which is empty if there is no real one.
        
        """
