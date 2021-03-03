def rectangle(win, uly, ulx, lry, lrx):
    """
    Draw a rectangle with corners at the provided upper-left
        and lower-right coordinates.
    
    """
def Textbox:
    """
    Editing widget using the interior of a window object.
         Supports the following Emacs-like key bindings:

        Ctrl-A      Go to left edge of window.
        Ctrl-B      Cursor left, wrapping to previous line if appropriate.
        Ctrl-D      Delete character under cursor.
        Ctrl-E      Go to right edge (stripspaces off) or end of line (stripspaces on).
        Ctrl-F      Cursor right, wrapping to next line when appropriate.
        Ctrl-G      Terminate, returning the window contents.
        Ctrl-H      Delete character backward.
        Ctrl-J      Terminate if the window is 1 line, otherwise insert newline.
        Ctrl-K      If line is blank, delete it, otherwise clear to end of line.
        Ctrl-L      Refresh screen.
        Ctrl-N      Cursor down; move down one line.
        Ctrl-O      Insert a blank line at cursor location.
        Ctrl-P      Cursor up; move up one line.

        Move operations do nothing if the cursor is at an edge where the movement
        is not possible.  The following synonyms are supported where possible:

        KEY_LEFT = Ctrl-B, KEY_RIGHT = Ctrl-F, KEY_UP = Ctrl-P, KEY_DOWN = Ctrl-N
        KEY_BACKSPACE = Ctrl-h
    
    """
    def __init__(self, win, insert_mode=False):
        """
        Go to the location of the first blank on the given line,
                returning the index of the last non-blank character.
        """
    def _insert_printable_char(self, ch):
        """
         The try-catch ignores the error we trigger from some curses
         versions by trying to write into the lowest-rightmost spot
         in the window.

        """
    def do_command(self, ch):
        """
        Process a single editing command.
        """
    def gather(self):
        """
        Collect and return the contents of the window.
        """
    def edit(self, validate=None):
        """
        Edit in the widget window and collect the results.
        """
    def test_editbox(stdscr):
        """
        Use Ctrl-G to end editing.
        """
