def Calltip:
    """
     subprocess and test
    """
    def close(self):
        """
         See __init__ for usage

        """
    def remove_calltip_window(self, event=None):
        """
        The user selected the menu entry or hotkey, open the tip.
        """
    def try_open_calltip_event(self, event):
        """
        Happens when it would be nice to open a calltip, but not really
                necessary, for example after an opening bracket, so function calls
                won't be made.
        
        """
    def refresh_calltip_event(self, event):
        """
        insert
        """
    def fetch_tip(self, expression):
        """
        Return the argument list and docstring of a function or class.

                If there is a Python subprocess, get the calltip there.  Otherwise,
                either this fetch_tip() is running in the subprocess or it was
                called in an IDLE running without the subprocess.

                The subprocess environment is that of the most recently run script.  If
                two unrelated modules are being edited some calltips in the current
                module may be inoperative if the module was not the last to run.

                To find methods, fetch_tip must be fed a fully qualified name.

        
        """
def get_entity(expression):
    """
    Return the object corresponding to expression evaluated
        in a namespace spanning sys.modules and __main.dict__.
    
    """
def get_argspec(ob):
    """
    '''Return a string describing the signature of a callable object, or ''.

        For Python-coded functions and methods, the first line is introspected.
        Delete 'self' parameter for classes (.__init__) and bound methods.
        The next lines are the first lines of the doc string up to the first
        empty line or _MAX_LINES.    For builtins, this typically includes
        the arguments in addition to the return value.
        '''
    """
