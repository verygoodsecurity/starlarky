def SearchDialogBase:
    """
    '''Create most of a 3 or 4 row, 3 column search dialog.

        The left and wide middle column contain:
        1 or 2 labeled text entry lines (make_entry, create_entries);
        a row of standard Checkbuttons (make_frame, create_option_buttons),
        each of which corresponds to a search engine Variable;
        a row of dialog-specific Check/Radiobuttons (create_other_buttons).

        The narrow right column contains command buttons
        (make_button, create_command_buttons).
        These are bound to functions that execute the command.

        Except for command buttons, this base class is not limited to items
        common to all three subclasses.  Rather, it is the Find dialog minus
        the "Find Next" command, its execution function, and the
        default_command attribute needed in create_widgets. The other
        dialogs override attributes and methods, the latter to replace and
        add widgets.
        '''
    """
    def __init__(self, root, engine):
        """
        '''Initialize root, engine, and top attributes.

                top (level widget): set in create_widgets() called from open().
                text (Text searched): set in open(), only used in subclasses().
                ent (ry): created in make_entry() called from create_entry().
                row (of grid): 0 in create_widgets(), +1 in make_entry/frame().
                default_command: set in subclasses, used in create_widgets().

                title (of dialog): class attribute, override in subclasses.
                icon (of dialog): ditto, use unclear if cannot minimize dialog.
                '''
        """
    def open(self, text, searchphrase=None):
        """
        Make dialog visible on top of others and ready to use.
        """
    def close(self, event=None):
        """
        Put dialog away for later use.
        """
    def create_widgets(self):
        """
        '''Create basic 3 row x 3 col search (find) dialog.

                Other dialogs override subsidiary create_x methods as needed.
                Replace and Find-in-Files add another entry row.
                '''
        """
    def make_entry(self, label_text, var):
        """
        '''Return (entry, label), .

                entry - gridded labeled Entry for text entry.
                label - Label widget, returned for testing.
                '''
        """
    def create_entries(self):
        """
        Create one or more entry lines with make_entry.
        """
    def make_frame(self,labeltext=None):
        """
        '''Return (frame, label).

                frame - gridded labeled Frame for option or other buttons.
                label - Label widget, returned for testing.
                '''
        """
    def create_option_buttons(self):
        """
        '''Return (filled frame, options) for testing.

                Options is a list of searchengine booleanvar, label pairs.
                A gridded frame from make_frame is filled with a Checkbutton
                for each pair, bound to the var, with the corresponding label.
                '''
        """
    def create_other_buttons(self):
        """
        '''Return (frame, others) for testing.

                Others is a list of value, label pairs.
                A gridded frame from make_frame is filled with radio buttons.
                '''
        """
    def make_button(self, label, command, isdef=0):
        """
        Return command button gridded in command frame.
        """
    def create_command_buttons(self):
        """
        Place buttons in vertical command frame gridded on right.
        """
def _searchbase(SearchDialogBase):  # htest #
    """
     htest #
    """
    def __init__(self, parent):
        """
        '[x+]'
        """
    def default_command(self, dummy): pass
        """
        '__main__'
        """
