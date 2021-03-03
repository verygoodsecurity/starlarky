def grep(text, io=None, flist=None):
    """
    Open the Find in Files dialog.

        Module-level function to access the singleton GrepDialog
        instance and open the dialog.  If text is selected, it is
        used as the search phrase; otherwise, the previous entry
        is used.

        Args:
            text: Text widget that contains the selected text for
                  default search phrase.
            io: iomenu.IOBinding instance with default path to search.
            flist: filelist.FileList instance for OutputWindow parent.
    
    """
def walk_error(msg):
    """
    Handle os.walk error.
    """
def findfiles(folder, pattern, recursive):
    """
    Generate file names in dir that match pattern.

        Args:
            folder: Root directory to search.
            pattern: File pattern to match.
            recursive: True to include subdirectories.
    
    """
def GrepDialog(SearchDialogBase):
    """
    Dialog for searching multiple files.
    """
    def __init__(self, root, engine, flist):
        """
        Create search dialog for searching for a phrase in the file system.

                Uses SearchDialogBase as the basis for the GUI and a
                searchengine instance to prepare the search.

                Attributes:
                    flist: filelist.Filelist instance for OutputWindow parent.
                    globvar: String value of Entry widget for path to search.
                    globent: Entry widget for globvar.  Created in
                        create_entries().
                    recvar: Boolean value of Checkbutton widget for
                        traversing through subdirectories.
        
        """
    def open(self, text, searchphrase, io=None):
        """
        Make dialog visible on top of others and ready to use.

                Extend the SearchDialogBase open() to set the initial value
                for globvar.

                Args:
                    text: Multicall object containing the text information.
                    searchphrase: String phrase to search.
                    io: iomenu.IOBinding instance containing file path.
        
        """
    def create_entries(self):
        """
        Create base entry widgets and add widget for search path.
        """
    def create_other_buttons(self):
        """
        Add check button to recurse down subdirectories.
        """
    def create_command_buttons(self):
        """
        Create base command buttons and add button for Search Files.
        """
    def default_command(self, event=None):
        """
        Grep for search pattern in file path. The default command is bound
                to <Return>.

                If entry values are populated, set OutputWindow as stdout
                and perform search.  The search dialog is closed automatically
                when the search begins.
        
        """
    def grep_it(self, prog, path):
        """
        Search for prog within the lines of the files in path.

                For the each file in the path directory, open the file and
                search each line for the matching pattern.  If the pattern is
                found,  write the file and line information to stdout (which
                is an OutputWindow).

                Args:
                    prog: The compiled, cooked search pattern.
                    path: String containing the search path.
        
        """
def _grep_dialog(parent):  # htest #
    """
     htest #
    """
    def show_grep_dialog():
        """
        1.0
        """
