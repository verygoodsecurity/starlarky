def FileDialog:
    """
    Standard file selection dialog -- no checks on selected file.

        Usage:

            d = FileDialog(master)
            fname = d.go(dir_or_file, pattern, default, key)
            if fname is None: ...canceled...
            else: ...open file...

        All arguments to go() are optional.

        The 'key' argument specifies a key in the global dictionary
        'dialogstates', which keeps track of the values for the directory
        and pattern arguments, overriding the values passed in (it does
        not keep track of the default argument!).  If no key is specified,
        the dialog keeps no memory of previous state.  Note that memory is
        kept even when the dialog is canceled.  (All this emulates the
        behavior of the Macintosh file selection dialogs.)

    
    """
    def __init__(self, master, title=None):
        """
        '<Return>'
        """
    def go(self, dir_or_file=os.curdir, pattern="*", default="", key=None):
        """
         window needs to be visible for the grab
        """
    def quit(self, how=None):
        """
         Exit mainloop()
        """
    def dirs_double_event(self, event):
        """
        'active'
        """
    def files_double_event(self, event):
        """
        'active'
        """
    def ok_event(self, event):
        """
        ''
        """
    def get_filter(self):
        """
        *
        """
    def get_selection(self):
        """
        *
        """
    def set_selection(self, file):
        """
        File selection dialog which checks that the file exists.
        """
    def ok_command(self):
        """
        File selection dialog which checks that the file may be created.
        """
    def ok_command(self):
        """
        Overwrite Existing File Question
        """
def _Dialog(commondialog.Dialog):
    """
     make sure "filetypes" is a tuple

    """
    def _fixresult(self, widget, result):
        """
         keep directory and filename until next time
         convert Tcl path objects to strings

        """
def Open(_Dialog):
    """
    Ask for a filename to open
    """
    def _fixresult(self, widget, result):
        """
         multiple results:

        """
def SaveAs(_Dialog):
    """
    Ask for a filename to save as
    """
def Directory(commondialog.Dialog):
    """
    Ask for a directory
    """
    def _fixresult(self, widget, result):
        """
         convert Tcl path objects to strings

        """
def askopenfilename(**options):
    """
    Ask for a filename to open
    """
def asksaveasfilename(**options):
    """
    Ask for a filename to save as
    """
def askopenfilenames(**options):
    """
    Ask for multiple filenames to open

        Returns a list of filenames or empty list if
        cancel button selected
    
    """
def askopenfile(mode = "r", **options):
    """
    Ask for a filename to open, and returned the opened file
    """
def askopenfiles(mode = "r", **options):
    """
    Ask for multiple filenames and return the open file
        objects

        returns a list of open file objects or an empty list if
        cancel selected
    
    """
def asksaveasfile(mode = "w", **options):
    """
    Ask for a filename to save as, and returned the opened file
    """
def askdirectory (**options):
    """
    Ask for a directory, and return the file name
    """
def test():
    """
    Simple test program.
    """
