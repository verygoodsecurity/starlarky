def count_lines_with_wrapping(s, linewidth=80):
    """
    Count the number of lines in a given string.

        Lines are counted as if the string was wrapped so that lines are never over
        linewidth characters long.

        Tabs are considered tabwidth characters long.
    
    """
def ExpandingButton(tk.Button):
    """
    Class for the "squeezed" text buttons used by Squeezer

        These buttons are displayed inside a Tk Text widget in place of text. A
        user can then use the button to replace it with the original text, copy
        the original text to the clipboard or view the original text in a separate
        window.

        Each button is tied to a Squeezer instance, and it knows to update the
        Squeezer instance when it is expanded (and therefore removed).
    
    """
    def __init__(self, s, tags, numoflines, squeezer):
        """
         The base Text widget is needed to change text before iomark.

        """
    def set_is_dangerous(self):
        """
        r'[^\n]+'
        """
    def expand(self, event=None):
        """
        expand event handler

                This inserts the original text in place of the button in the Text
                widget, removes the button and updates the Squeezer instance.

                If the original text is dangerously long, i.e. expanding it could
                cause a performance degradation, ask the user for confirmation.
        
        """
    def copy(self, event=None):
        """
        copy event handler

                Copy the original text to the clipboard.
        
        """
    def view(self, event=None):
        """
        view event handler

                View the original text in a separate text viewer window.
        
        """
    def context_menu_event(self, event):
        """
        insert
        """
def Squeezer:
    """
    Replace long outputs in the shell with a simple button.

        This avoids IDLE's shell slowing down considerably, and even becoming
        completely unresponsive, when very long outputs are written.
    
    """
    def reload(cls):
        """
        Load class variables from config.
        """
    def __init__(self, editwin):
        """
        Initialize settings for Squeezer.

                editwin is the shell's Editor window.
                self.text is the editor window text widget.
                self.base_test is the actual editor window Tk text widget, rather than
                    EditorWindow's wrapper.
                self.expandingbuttons is the list of all buttons representing
                    "squeezed" output.
        
        """
        def mywrite(s, tags=(), write=editwin.write):
            """
             Only auto-squeeze text which has just the "stdout" tag.

            """
    def count_lines(self, s):
        """
        Count the number of lines in a given text.

                Before calculation, the tab width and line length of the text are
                fetched, so that up-to-date values are used.

                Lines are counted as if the string was wrapped so that lines are never
                over linewidth characters long.

                Tabs are considered tabwidth characters long.
        
        """
    def squeeze_current_text_event(self, event):
        """
        squeeze-current-text event handler

                Squeeze the block of text inside which contains the "insert" cursor.

                If the insert cursor is not in a squeezable block of text, give the
                user a small warning and do nothing.
        
        """
