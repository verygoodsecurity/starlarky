def SimpleDialog:
    """
    ''
    """
    def _set_transient(self, master, relx=0.5, rely=0.3):
        """
         Remain invisible while we figure out the geometry
        """
    def go(self):
        """
        '''Class to open dialogs.

            This class is intended as a base class for custom dialogs
            '''
        """
    def __init__(self, parent, title = None):
        """
        '''Initialize a dialog.

                Arguments:

                    parent -- a parent window (the application window)

                    title -- the dialog title
                '''
        """
    def destroy(self):
        """
        '''Destroy the window'''
        """
    def body(self, master):
        """
        '''create dialog body.

                return widget that should have initial focus.
                This method should be overridden, and is called
                by the __init__ method.
                '''
        """
    def buttonbox(self):
        """
        '''add standard button box.

                override if you do not want the standard buttons
                '''
        """
    def ok(self, event=None):
        """
         put focus back
        """
    def cancel(self, event=None):
        """
         put focus back to the parent window

        """
    def validate(self):
        """
        '''validate the data

                This method is called automatically to validate the data before the
                dialog is destroyed. By default, it always validates OK.
                '''
        """
    def apply(self):
        """
        '''process the data

                This method is called automatically to process the data, *after*
                the dialog is destroyed. By default, it does nothing.
                '''
        """
def _QueryDialog(Dialog):
    """
    entry
    """
    def validate(self):
        """
        Illegal value
        """
def _QueryInteger(_QueryDialog):
    """
    Not an integer.
    """
    def getresult(self):
        """
        '''get an integer from the user

            Arguments:

                title -- the dialog title
                prompt -- the label text
                **kw -- see SimpleDialog class

            Return value is an integer
            '''
        """
def _QueryFloat(_QueryDialog):
    """
    Not a floating point value.
    """
    def getresult(self):
        """
        '''get a float from the user

            Arguments:

                title -- the dialog title
                prompt -- the label text
                **kw -- see SimpleDialog class

            Return value is a float
            '''
        """
def _QueryString(_QueryDialog):
    """
    show
    """
    def body(self, master):
        """
        '''get a string from the user

            Arguments:

                title -- the dialog title
                prompt -- the label text
                **kw -- see SimpleDialog class

            Return value is a string
            '''
        """
    def test():
        """
        This is a test dialog.  
        Would this have been an actual dialog, 
        the buttons below would have been glowing 
        in soft pink light.\n
        Do you believe this?
        """
