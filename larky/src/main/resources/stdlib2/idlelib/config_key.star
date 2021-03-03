def translate_key(key, modifiers):
    """
    Translate from keycap symbol to the Tkinter keysym.
    """
def GetKeysDialog(Toplevel):
    """
     Dialog title for invalid key sequence

    """
2021-03-02 20:54:20,311 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, parent, title, action, current_key_sequences,
                 *, _htest=False, _utest=False):
        """

                parent - parent of this dialog
                title - string which is the title of the popup dialog
                action - string, the name of the virtual event these keys will be
                         mapped to
                current_key_sequences - list, a list of all key sequence lists
                         currently mapped to virtual events, for overlap checking
                _htest - bool, change box location when running htest
                _utest - bool, do not wait when running unittest
        
        """
    def showerror(self, *args, **kwargs):
        """
         Make testing easier.  Replace in #30751.

        """
    def create_widgets(self):
        """
        'sunken'
        """
    def set_modifiers_for_platform(self):
        """
        Determine list of names of key modifiers for this platform.

                The names are used to build Tk bindings -- it doesn't matter if the
                keyboard has these keys; it matters if Tk understands them.  The
                order is also important: key binding equality depends on it, so
                config-keys.def must use the same ordering.
        
        """
    def toggle_level(self):
        """
        Toggle between basic and advanced keys.
        """
    def final_key_selected(self, event=None):
        """
        Handler for clicking on key in basic settings list.
        """
    def build_key_string(self):
        """
        Create formatted string of modifiers plus the key.
        """
    def get_modifiers(self):
        """
        Return ordered list of modifiers that have been selected.
        """
    def clear_key_seq(self):
        """
        Clear modifiers and keys selection.
        """
    def ok(self, event=None):
        """
        No key specified.
        """
    def cancel(self, event=None):
        """
        ''
        """
    def keys_ok(self, keys):
        """
        Validity check on user's 'basic' keybinding selection.

                Doesn't check the string produced by the advanced dialog because
                'modifiers' isn't set.
        
        """
    def bind_ok(self, keys):
        """
        Return True if Tcl accepts the new keys else show message.
        """
