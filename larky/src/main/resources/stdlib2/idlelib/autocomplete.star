def AutoComplete:
    """
     not in subprocess or no-gui test
    """
    def reload(cls):
        """
        extensions
        """
    def _make_autocomplete_window(self):  # Makes mocking easier.
        """
         Makes mocking easier.
        """
    def _remove_autocomplete_window(self, event=None):
        """
        (^space) Open completion list, even if a function call is needed.
        """
    def autocomplete_event(self, event):
        """
        (tab) Complete word or open list if multiple options.
        """
    def try_open_completions_event(self, event=None):
        """
        (./) Open completion list after pause with no movement.
        """
    def _delayed_open_completions(self, args):
        """
        Call open_completions if index unchanged.
        """
    def open_completions(self, args):
        """
        Find the completions and create the AutoCompleteWindow.
                Return True if successful (no syntax error or so found).
                If complete is True, then if there's nothing to complete and no
                start of completion, won't open completions and return False.
                If mode is given, will open a completion list only in this mode.
        
        """
    def fetch_completions(self, what, mode):
        """
        Return a pair of lists of completions for something. The first list
                is a sublist of the second. Both are sorted.

                If there is a Python subprocess, get the comp. list there.  Otherwise,
                either fetch_completions() is running in the subprocess itself or it
                was called in an IDLE EditorWindow before any script had been run.

                The subprocess environment is that of the most recently run script.  If
                two unrelated modules are being edited some calltips in the current
                module may be inoperative if the module was not the last to run.
        
        """
    def get_entity(self, name):
        """
        Lookup name in a namespace spanning sys.modules and __main.dict__.
        """
