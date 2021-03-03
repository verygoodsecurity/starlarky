def ScriptBinding:
    """
     Provide instance variables referenced by debugger
     XXX This should be done differently

    """
    def check_module_event(self, event):
        """
        'break'
        """
    def tabnanny(self, filename):
        """
         XXX: tabnanny should work on binary files as well

        """
    def checksyntax(self, filename):
        """
        'rb'
        """
    def run_module_event(self, event):
        """
         Tk-Cocoa in MacOSX is broken until at least
         Tk 8.5.9, and without this rather
         crude workaround IDLE would hang when a user
         tries to run a module using the keyboard shortcut
         (the menu item works fine).

        """
    def run_custom_event(self, event):
        """
        Run the module after setting up the environment.

                First check the syntax.  Next get customization.  If OK, make
                sure the shell is active and then transfer the arguments, set
                the run environment's working directory to the directory of the
                module being executed and also add that directory to its
                sys.path if not already included.
        
        """
    def getfilename(self):
        """
        Get source filename.  If not saved, offer to save (or create) file

                The debugger requires a source file.  Make sure there is one, and that
                the current version of the source buffer has been saved.  If the user
                declines to save or cancels the Save As dialog, return None.

                If the user has configured IDLE for Autosave, the file will be
                silently saved if it already exists and is dirty.

        
        """
    def ask_save_dialog(self):
        """
        Source Must Be Saved\n
        """
    def errorbox(self, title, message):
        """
         XXX This should really be a function of EditorWindow...

        """
