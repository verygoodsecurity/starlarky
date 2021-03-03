def replace(text):
    """
    Create or reuse a singleton ReplaceDialog instance.

        The singleton dialog saves user entries and preferences
        across instances.

        Args:
            text: Text widget containing the text to be searched.
    
    """
def ReplaceDialog(SearchDialogBase):
    """
    Dialog for finding and replacing a pattern in text.
    """
    def __init__(self, root, engine):
        """
        Create search dialog for finding and replacing text.

                Uses SearchDialogBase as the basis for the GUI and a
                searchengine instance to prepare the search.

                Attributes:
                    replvar: StringVar containing 'Replace with:' value.
                    replent: Entry widget for replvar.  Created in
                        create_entries().
                    ok: Boolean used in searchengine.search_text to indicate
                        whether the search includes the selection.
        
        """
    def open(self, text):
        """
        Make dialog visible on top of others and ready to use.

                Also, highlight the currently selected text and set the
                search to include the current selection (self.ok).

                Args:
                    text: Text widget being searched.
        
        """
    def create_entries(self):
        """
        Create base and additional label and text entry widgets.
        """
    def create_command_buttons(self):
        """
        Create base and additional command buttons.

                The additional buttons are for Find, Replace,
                Replace+Find, and Replace All.
        
        """
    def find_it(self, event=None):
        """
        Handle the Find button.
        """
    def replace_it(self, event=None):
        """
        Handle the Replace button.

                If the find is successful, then perform replace.
        
        """
    def default_command(self, event=None):
        """
        Handle the Replace+Find button as the default command.

                First performs a replace and then, if the replace was
                successful, a find next.
        
        """
    def _replace_expand(self, m, repl):
        """
        Expand replacement text if regular expression.
        """
    def replace_all(self, event=None):
        """
        Handle the Replace All button.

                Search text for occurrences of the Find value and replace
                each of them.  The 'wrap around' value controls the start
                point for searching.  If wrap isn't set, then the searching
                starts at the first occurrence after the current selection;
                if wrap is set, the replacement starts at the first line.
                The replacement is always done top-to-bottom in the text.
        
        """
    def do_find(self, ok=False):
        """
        Search for and highlight next occurrence of pattern in text.

                No text replacement is done with this option.
        
        """
    def do_replace(self):
        """
        Replace search pattern in text with replacement value.
        """
    def show_hit(self, first, last):
        """
        Highlight text between first and last indices.

                Text is highlighted via the 'hit' tag and the marked
                section is brought into view.

                The colors from the 'hit' tag aren't currently shown
                when the text is displayed.  This is due to the 'sel'
                tag being added first, so the colors in the 'sel'
                config are seen instead of the colors for 'hit'.
        
        """
    def close(self, event=None):
        """
        Close the dialog and remove hit tags.
        """
def _replace_dialog(parent):  # htest #
    """
     htest #
    """
    def undo_block_start():
        """
        'gray'
        """
    def show_replace():
        """
        1.0
        """
