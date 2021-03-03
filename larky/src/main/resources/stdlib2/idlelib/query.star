def Query(Toplevel):
    """
    Base class for getting verified answer from a user.

        For this base class, accept any non-blank string.
    
    """
2021-03-02 20:54:19,369 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, parent, title, message, *, text0='', used_names={},
                 _htest=False, _utest=False):
        """
        Create modal popup, return when destroyed.

                Additional subclass init must be done before this unless
                _utest=True is passed to suppress wait_window().

                title - string, title of popup dialog
                message - string, informational message to display
                text0 - initial value for entry
                used_names - names already in use
                _htest - bool, change box location when running htest
                _utest - bool, leave window hidden and not modal
        
        """
    def create_widgets(self, ok_text='OK'):  # Do not replace.
        """
         Do not replace.
        """
    def create_extra(self): pass  # Override to add widgets.
        """
         Override to add widgets.
        """
    def showerror(self, message, widget=None):
        """
        self.bell(displayof=self)

        """
    def entry_ok(self):  # Example: usually replace.
        """
         Example: usually replace.
        """
    def ok(self, event=None):  # Do not replace.
        """
         Do not replace.
        """
    def cancel(self, event=None):  # Do not replace.
        """
         Do not replace.
        """
    def destroy(self):
        """
        Get a name for a config file section name.
        """
2021-03-02 20:54:19,373 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, parent, title, message, used_names,
                 *, _htest=False, _utest=False):
        """
        Return sensible ConfigParser section name or None.
        """
def ModuleName(Query):
    """
    Get a module name for Open Module menu entry.
    """
2021-03-02 20:54:19,373 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, parent, title, message, text0,
                 *, _htest=False, _utest=False):
        """
        Return entered module name as file path or None.
        """
def Goto(Query):
    """
    Get a positive line number for editor Go To Line.
    """
    def entry_ok(self):
        """
        'not a base 10 integer.'
        """
def HelpSource(Query):
    """
    Get menu name and help source for Help menu.
    """
2021-03-02 20:54:19,375 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, parent, title, *, menuitem='', filepath='',
                 used_names={}, _htest=False, _utest=False):
        """
        Get menu entry and url/local file for Additional Help.

                User enters a name for the Help resource and a web url or file
                name. The user can browse for the file.
        
        """
    def create_extra(self):
        """
        Add path widjets to rows 10-12.
        """
    def askfilename(self, filetypes, initdir, initfile):  # htest #
        """
         htest #
        """
    def browse_file(self):
        """
        HTML Files
        """
    def path_ok(self):
        """
        Simple validity check for menu file path
        """
    def entry_ok(self):
        """
        Return apparently valid (name, path) or None
        """
def CustomRun(Query):
    """
    Get settings for custom run of module.

        1. Command line arguments to extend sys.argv.
        2. Whether to restart Shell or not.
    
    """
2021-03-02 20:54:19,378 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, parent, title, *, cli_args=[],
                 _htest=False, _utest=False):
        """
        cli_args is a list of strings.

                The list is assigned to the default Entry StringVar.
                The strings are displayed joined by ' ' for display.
        
        """
    def create_extra(self):
        """
        Add run mode on rows 10-12.
        """
    def cli_args_ok(self):
        """
        Validity check and parsing for command line arguments.
        """
    def entry_ok(self):
        """
        Return apparently valid (cli_args, restart) or None.
        """
