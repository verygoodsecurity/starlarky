def IOBinding:
    """
     One instance per editor Window so methods know which to save, close.
     Open returns focus to self.editwin if aborted.
     EditorWindow.open_module, others, belong here.


    """
    def __init__(self, editwin):
        """
        <<open-window-from-file>>
        """
    def close(self):
        """
         Undo command bindings

        """
    def get_saved(self):
        """
         Save in case parent window is closed (ie, during askopenfile()).

        """
    def loadfile(self, filename):
        """
         Wait for the editor window to appear

        """
    def maybesave(self):
        """
        yes
        """
    def save(self, event):
        """
         may be a PyShell
        """
    def save_as(self, event):
        """
        break
        """
    def save_a_copy(self, event):
        """
        break
        """
    def writefile(self, filename):
        """
        wb
        """
    def fixnewlines(self):
        """
        Return text with final \n if needed and os eols.
        """
    def encode(self, chars):
        """
         This is either plain ASCII, or Tk was returning mixed-encoding
         text to us. Don't try to guess further.

        """
    def print_window(self, event):
        """
        Print
        """
    def askopenfile(self):
        """
        open
        """
    def defaultfilename(self, mode="open"):
        """

        """
    def asksavefile(self):
        """
        save
        """
    def updaterecentfileslist(self,filename):
        """
        Update recent file list on all editor windows
        """
def _io_binding(parent):  # htest #
    """
     htest #
    """
    def MyEditWin:
    """
    <Control-o>
    """
        def get_saved(self): return 0
            """
            <<open-window-from-file>>
            """
        def print(self, event):
            """
            <<print-window>>
            """
        def save(self, event):
            """
            <<save-window>>
            """
        def saveas(self, event):
            """
            <<save-window-as-file>>
            """
        def savecopy(self, event):
            """
            <<save-copy-of-window-as-file>>
            """
