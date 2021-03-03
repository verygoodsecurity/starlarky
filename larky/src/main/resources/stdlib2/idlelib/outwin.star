def compile_progs():
    """
    Compile the patterns for matching to file name and line number.
    """
def file_line_helper(line):
    """
    Extract file name and line number from line of text.

        Check if line of text contains one of the file/line patterns.
        If it does and if the file and line are valid, return
        a tuple of the file name and line number.  If it doesn't match
        or if the file or line is invalid, return None.
    
    """
def OutputWindow(EditorWindow):
    """
    An editor window that can serve as an output file.

        Also the future base class for the Python shell window.
        This class has no input facilities.

        Adds binding to open a file at a line to the text widget.
    
    """
    def __init__(self, *args):
        """
        <<goto-file-line>>
        """
    def ispythonsource(self, filename):
        """
        Python source is only part of output: do not colorize.
        """
    def short_title(self):
        """
        Customize EditorWindow title.
        """
    def maybesave(self):
        """
        Customize EditorWindow to not display save file messagebox.
        """
    def write(self, s, tags=(), mark="insert"):
        """
        Write text to text widget.

                The text is inserted at the given index with the provided
                tags.  The text widget is then scrolled to make it visible
                and updated to display it, giving the effect of seeing each
                line as it is added.

                Args:
                    s: Text to insert into text widget.
                    tags: Tuple of tag strings to apply on the insert.
                    mark: Index for the insert.

                Return:
                    Length of text inserted.
        
        """
    def writelines(self, lines):
        """
        Write each item in lines iterable.
        """
    def flush(self):
        """
        No flushing needed as write() directly writes to widget.
        """
    def showerror(self, *args, **kwargs):
        """
        Handle request to open file/line.

                If the selected or previous line in the output window
                contains a file name and line number, then open that file
                name in a new window and position on the line number.

                Otherwise, display an error messagebox.
        
        """
def OnDemandOutputWindow:
    """
     XXX Should use IdlePrefs.ColorPrefs
    stdout
    """
    def __init__(self, flist):
        """
        'sel'
        """
