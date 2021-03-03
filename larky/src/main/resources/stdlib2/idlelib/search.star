def _setup(text):
    """
    Return the new or existing singleton SearchDialog instance.

        The singleton dialog saves user entries and preferences
        across instances.

        Args:
            text: Text widget containing the text to be searched.
    
    """
def find(text):
    """
    Open the search dialog.

        Module-level function to access the singleton SearchDialog
        instance and open the dialog.  If text is selected, it is
        used as the search phrase; otherwise, the previous entry
        is used.  No search is done with this command.
    
    """
def find_again(text):
    """
    Repeat the search for the last pattern and preferences.

        Module-level function to access the singleton SearchDialog
        instance to search again using the user entries and preferences
        from the last dialog.  If there was no prior search, open the
        search dialog; otherwise, perform the search without showing the
        dialog.
    
    """
def find_selection(text):
    """
    Search for the selected pattern in the text.

        Module-level function to access the singleton SearchDialog
        instance to search using the selected text.  With a text
        selection, perform the search without displaying the dialog.
        Without a selection, use the prior entry as the search phrase
        and don't display the dialog.  If there has been no prior
        search, open the search dialog.
    
    """
def SearchDialog(SearchDialogBase):
    """
    Dialog for finding a pattern in text.
    """
    def create_widgets(self):
        """
        Create the base search dialog and add a button for Find Next.
        """
    def default_command(self, event=None):
        """
        Handle the Find Next button as the default command.
        """
    def find_again(self, text):
        """
        Repeat the last search.

                If no search was previously run, open a new search dialog.  In
                this case, no search is done.

                If a search was previously run, the search dialog won't be
                shown and the options from the previous search (including the
                search pattern) will be used to find the next occurrence
                of the pattern.  Next is relative based on direction.

                Position the window to display the located occurrence in the
                text.

                Return True if the search was successful and False otherwise.
        
        """
    def find_selection(self, text):
        """
        Search for selected text with previous dialog preferences.

                Instead of using the same pattern for searching (as Find
                Again does), this first resets the pattern to the currently
                selected text.  If the selected text isn't changed, then use
                the prior search phrase.
        
        """
def _search_dialog(parent):  # htest #
    """
     htest #
    """
    def show_find():
        """
        'sel'
        """
