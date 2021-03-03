def FormatParagraph:
    """
    Format a paragraph, comment block, or selection to a max width.

        Does basic, standard text formatting, and also understands Python
        comment blocks. Thus, for editing Python source code, this
        extension is really only suitable for reformatting these comment
        blocks or triple-quoted strings.

        Known problems with comment reformatting:
        * If there is a selection marked, and the first line of the
          selection is not complete, the block will probably not be detected
          as comments, and will have the normal "text formatting" rules
          applied.
        * If a comment block has leading whitespace that mixes tabs and
          spaces, they will not be considered part of the same block.
        * Fancy comments, like this bulleted list, aren't handled :-)
    
    """
    def __init__(self, editwin):
        """
        'extensions'
        """
    def close(self):
        """
        Formats paragraph to a max width specified in idleConf.

                If text is selected, format_paragraph_event will start breaking lines
                at the max width, starting from the beginning selection.

                If no text is selected, format_paragraph_event uses the current
                cursor location to determine the paragraph (lines of text surrounded
                by blank lines) and formats it.

                The length limit parameter is for testing with a known value.
        
        """
def find_paragraph(text, mark):
    """
    Returns the start/stop indices enclosing the paragraph that mark is in.

        Also returns the comment format string, if any, and paragraph of text
        between the start/stop indices.
    
    """
def reformat_paragraph(data, limit):
    """
    Return data reformatted to specified width (limit).
    """
def reformat_comment(data, limit, comment_header):
    """
    Return data reformatted to specified width with comment header.
    """
def is_all_white(line):
    """
    Return True if line is empty or all whitespace.
    """
def get_indent(line):
    """
    Return the initial space or tab indent of line.
    """
def get_comment_header(line):
    """
    Return string with leading whitespace and '#' from line or ''.

        A null return indicates that the line is not a comment line. A non-
        null return, such as '    #', will be used to find the other lines of
        a comment block with the same  indent.
    
    """
def get_line_indent(line, tabwidth):
    """
    Return a line's indentation as (# chars, effective # of spaces).

        The effective # of spaces is the length after properly "expanding"
        the tabs into spaces, as done by str.expandtabs(tabwidth).
    
    """
def FormatRegion:
    """
    Format selected text (region).
    """
    def __init__(self, editwin):
        """
        Return line information about the selected text region.

                If text is selected, the first and last indices will be
                for the selection.  If there is no text selected, the
                indices will be the current cursor location.

                Return a tuple containing (first index, last index,
                    string representation of text, list of text lines).
        
        """
    def set_region(self, head, tail, chars, lines):
        """
        Replace the text between the given indices.

                Args:
                    head: Starting index of text to replace.
                    tail: Ending index of text to replace.
                    chars: Expected to be string of current text
                        between head and tail.
                    lines: List of new lines to insert between head
                        and tail.
        
        """
    def indent_region_event(self, event=None):
        """
        Indent region by indentwidth spaces.
        """
    def dedent_region_event(self, event=None):
        """
        Dedent region by indentwidth spaces.
        """
    def comment_region_event(self, event=None):
        """
        Comment out each line in region.

                ## is appended to the beginning of each line to comment it out.
        
        """
    def uncomment_region_event(self, event=None):
        """
        Uncomment each line in region.

                Remove ## or # in the first positions of a line.  If the comment
                is not in the beginning position, this command will have no effect.
        
        """
    def tabify_region_event(self, event=None):
        """
        Convert leading spaces to tabs for each line in selected region.
        """
    def untabify_region_event(self, event=None):
        """
        Expand tabs to spaces for each line in region.
        """
    def _asktabwidth(self):
        """
        Return value for tab width.
        """
def Indents:
    """
    Change future indents.
    """
    def __init__(self, editwin):
        """
        Toggle tabs
        """
    def change_indentwidth_event(self, event):
        """
        Indent width
        """
def Rstrip:  # 'Strip Trailing Whitespace" on "Format" menu.
    """
     'Strip Trailing Whitespace" on "Format" menu.
    """
    def __init__(self, editwin):
        """
        'end'
        """
