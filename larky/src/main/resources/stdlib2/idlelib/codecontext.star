def get_spaces_firstword(codeline, c=re.compile(r"^(\s*)(\w*)")):
    """
    Extract the beginning whitespace and first word from codeline.
    """
def get_line_info(codeline):
    """
    Return tuple of (line indent value, codeline, block start keyword).

        The indentation of empty lines (or comment lines) is INFINITY.
        If the line does not start a block, the keyword value is False.
    
    """
def CodeContext:
    """
    Display block context above the edit window.
    """
    def __init__(self, editwin):
        """
        Initialize settings for context block.

                editwin is the Editor window for the context block.
                self.text is the editor window text widget.

                self.context displays the code context text above the editor text.
                  Initially None, it is toggled via <<toggle-code-context>>.
                self.topvisible is the number of the top text line displayed.
                self.info is a list of (line number, indent level, line text,
                  block keyword) tuples for the block structure above topvisible.
                  self.info[0] is initialized with a 'dummy' line which
                  starts the toplevel 'block' of the module.

                self.t1 and self.t2 are two timer events on the editor text widget to
                  monitor for changes to the context text or editor font.
        
        """
    def _reset(self):
        """

        """
    def reload(cls):
        """
        Load class variables from config.
        """
    def __del__(self):
        """
        Cancel scheduled events.
        """
    def toggle_code_context_event(self, event=None):
        """
        Toggle code context display.

                If self.context doesn't exist, create it to match the size of the editor
                window text (toggle on).  If it does exist, destroy it (toggle off).
                Return 'break' to complete the processing of the binding.
        
        """
    def get_context(self, new_topvisible, stopline=1, stopindent=0):
        """
        Return a list of block line tuples and the 'last' indent.

                The tuple fields are (linenum, indent, text, opener).
                The list represents header lines from new_topvisible back to
                stopline with successively shorter indents > stopindent.
                The list is returned ordered by line number.
                Last indent returned is the smallest indent observed.
        
        """
    def update_code_context(self):
        """
        Update context information and lines visible in the context pane.

                No update is done if the text hasn't been scrolled.  If the text
                was scrolled, the lines that should be shown in the context will
                be retrieved and the context area will be updated with the code,
                up to the number of maxlines.
        
        """
    def jumptoline(self, event=None):
        """
         Show clicked context line at top of editor.

                If a selection was made, don't jump; allow copying.
                If no visible context, show the top line of the file.
        
        """
    def timer_event(self):
        """
        Event on editor text widget triggered every UPDATEINTERVAL ms.
        """
    def update_font(self):
        """
        'main'
        """
    def update_highlight_colors(self):
        """
        'context'
        """
