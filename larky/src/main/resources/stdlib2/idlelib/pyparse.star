def ParseMap(dict):
    """
    r"""Dict subclass that maps anything not in dict to 'x'.

        This is designed to be used with str.translate in study1.
        Anything not specifically mapped otherwise becomes 'x'.
        Example: replace everything except whitespace with 'x'.

        >>> keepwhite = ParseMap((ord(c), ord(c)) for c in ' \t\n\r')
        >>> "a + b\tc\nd".translate(keepwhite)
        'x x x\tx\nx'
    
    """
    def __missing__(self, key):
        """
         ord('x')
        """
def Parser:
    """
    '\n'
    """
    def find_good_parse_start(self, is_char_in_string):
        """

                Return index of a good place to begin parsing, as close to the
                end of the string as possible.  This will be the start of some
                popular stmt like "if" or "def".  Return None if none found:
                the caller should pass more prior context then, if possible, or
                if not (the entire program text up until the point of interest
                has already been tried) pass 0 to set_lo().

                This will be reliable iff given a reliable is_char_in_string()
                function, meaning that when it says "no", it's absolutely
                guaranteed that the char is not in a string.
        
        """
    def set_lo(self, lo):
        """
         Throw away the start of the string.

                Intended to be called with the result of find_good_parse_start().
        
        """
    def _study1(self):
        """
        Find the line numbers of non-continuation lines.

                As quickly as humanly possible <wink>, find the line numbers (0-
                based) of the non-continuation lines.
                Creates self.{goodlines, continuation}.
        
        """
    def get_continuation_type(self):
        """

                study1 was sufficient to determine the continuation status,
                but doing more requires looking at every character.  study2
                does this for the last interesting statement in the block.
                Creates:
                    self.stmt_start, stmt_end
                        slice indices of last interesting stmt
                    self.stmt_bracketing
                        the bracketing structure of the last interesting stmt; for
                        example, for the statement "say(boo) or die",
                        stmt_bracketing will be ((0, 0), (0, 1), (2, 0), (2, 1),
                        (4, 0)). Strings and comments are treated as brackets, for
                        the matter.
                    self.lastch
                        last interesting character before optional trailing comment
                    self.lastopenbracketpos
                        if continuation is C_BRACKET, index of last open bracket
        
        """
    def compute_bracket_indent(self):
        """
        Return number of spaces the next line should be indented.

                Line continuation must be C_BRACKET.
        
        """
    def get_num_lines_in_stmt(self):
        """
        Return number of physical lines in last stmt.

                The statement doesn't have to be an interesting statement.  This is
                intended to be called when continuation is C_BACKSLASH.
        
        """
    def compute_backslash_indent(self):
        """
        Return number of spaces the next line should be indented.

                Line continuation must be C_BACKSLASH.  Also assume that the new
                line is the first one following the initial line of the stmt.
        
        """
    def get_base_indent_string(self):
        """
        Return the leading whitespace on the initial line of the last
                interesting stmt.
        
        """
    def is_block_opener(self):
        """
        Return True if the last interesting statement opens a block.
        """
    def is_block_closer(self):
        """
        Return True if the last interesting statement closes a block.
        """
    def get_last_stmt_bracketing(self):
        """
        Return bracketing structure of the last interesting statement.

                The returned tuple is in the format defined in _study2().
        
        """
