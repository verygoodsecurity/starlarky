def AutoHideScrollbar(Scrollbar):
    """
    A scrollbar that is automatically hidden when not needed.

        Only the grid geometry manager is supported.
    
    """
    def set(self, lo, hi):
        """
        f'{self.__class__.__name__} does not support "pack"'
        """
    def place(self, **kwargs):
        """
        f'{self.__class__.__name__} does not support "place"'
        """
def ScrollableTextFrame(Frame):
    """
    Display text with scrollbar(s).
    """
    def __init__(self, master, wrap=NONE, **kwargs):
        """
        Create a frame for Textview.

                master - master widget for this frame
                wrap - type of text wrapping to use ('word', 'char' or 'none')

                All parameters except for 'wrap' are passed to Frame.__init__().

                The Text widget is accessible via the 'text' attribute.

                Note: Changing the wrapping mode of the text widget after
                instantiation is not supported.
        
        """
def ViewFrame(Frame):
    """
    Display TextFrame and Close button.
    """
    def __init__(self, parent, contents, wrap='word'):
        """
        Create a frame for viewing text with a "Close" button.

                parent - parent widget for this frame
                contents - text to display
                wrap - type of text wrapping to use ('word', 'char' or 'none')

                The Text widget is accessible via the 'text' attribute.
        
        """
    def ok(self, event=None):
        """
        Dismiss text viewer dialog.
        """
def ViewWindow(Toplevel):
    """
    A simple text viewer dialog for IDLE.
    """
2021-03-02 20:54:21,416 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, parent, title, contents, modal=True, wrap=WORD,
                 *, _htest=False, _utest=False):
        """
        Show the given text in a scrollable window with a 'close' button.

                If modal is left True, users cannot interact with other windows
                until the textview window is closed.

                parent - parent of this dialog
                title - string which is title of popup dialog
                contents - text to display in dialog
                wrap - type of text wrapping to use ('word', 'char' or 'none')
                _htest - bool; change box location when running htest.
                _utest - bool; don't wait_window when running unittest.
        
        """
    def ok(self, event=None):
        """
        Dismiss text viewer dialog.
        """
def view_text(parent, title, contents, modal=True, wrap='word', _utest=False):
    """
    Create text viewer for given text.

        parent - parent of this dialog
        title - string which is the title of popup dialog
        contents - text to display in this dialog
        wrap - type of text wrapping to use ('word', 'char' or 'none')
        modal - controls if users can interact with other windows while this
                dialog is displayed
        _utest - bool; controls wait_window on unittest
    
    """
2021-03-02 20:54:21,419 : INFO : tokenize_signature : --> do i ever get here?
def view_file(parent, title, filename, encoding, modal=True, wrap='word',
              _utest=False):
    """
    Create text viewer for text in filename.

        Return error message if file cannot be read.  Otherwise calls view_text
        with contents of the file.
    
    """
