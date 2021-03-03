def FileList:
    """
     N.B. this import overridden in PyShellFileList.

    """
    def __init__(self, root):
        """
         For EditorWindow.getrawvar (shared Tcl variables)
        """
    def open(self, filename, action=None):
        """
         This can happen when bad filename is passed on command line:

        """
    def gotofileline(self, filename, lineno=None):
        """
        cancel
        """
    def unregister_maybe_terminate(self, edit):
        """
        Don't know this EditorWindow object.  (close)
        """
    def filename_changed_edit(self, edit):
        """
        Don't know this EditorWindow object.  (rename)
        """
    def canonize(self, filename):
        """
         TODO check and convert to htest
        """
