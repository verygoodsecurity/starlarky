def build_bits():
    """
    Return bits for platform.
    """
def AboutDialog(Toplevel):
    """
    Modal about dialog for idle

    
    """
    def __init__(self, parent, title=None, *, _htest=False, _utest=False):
        """
        Create popup, do not return until tk widget destroyed.

                parent - parent of this dialog
                title - string which is title of popup dialog
                _htest - bool, change box location when running htest
                _utest - bool, don't wait_window when running unittest
        
        """
    def create_widgets(self):
        """
        'Close'
        """
    def show_py_license(self):
        """
        Handle License button event.
        """
    def show_py_copyright(self):
        """
        Handle Copyright button event.
        """
    def show_py_credits(self):
        """
        Handle Python Credits button event.
        """
    def show_idle_credits(self):
        """
        Handle Idle Credits button event.
        """
    def show_readme(self):
        """
        Handle Readme button event.
        """
    def show_idle_news(self):
        """
        Handle News button event.
        """
    def display_printer_text(self, title, printer):
        """
        Create textview for built-in constants.

                Built-in constants have type _sitebuiltins._Printer.  The
                text is extracted from the built-in and then sent to a text
                viewer with self as the parent and title as the title of
                the popup.
        
        """
    def display_file_text(self, title, filename, encoding=None):
        """
        Create textview for filename.

                The filename needs to be in the current directory.  The path
                is sent to a text viewer with self as the parent, title as
                the title of the popup, and the file encoding.
        
        """
    def ok(self, event=None):
        """
        Dismiss help_about dialog.
        """
