def WidgetRedirector:
    """
    Support for redirecting arbitrary widget subcommands.

        Some Tk operations don't normally pass through tkinter.  For example, if a
        character is inserted into a Text widget by pressing a key, a default Tk
        binding to the widget's 'insert' operation is activated, and the Tk library
        processes the insert without calling back into tkinter.

        Although a binding to <Key> could be made via tkinter, what we really want
        to do is to hook the Tk 'insert' operation itself.  For one thing, we want
        a text.insert call in idle code to have the same effect as a key press.

        When a widget is instantiated, a Tcl command is created whose name is the
        same as the pathname widget._w.  This command is used to invoke the various
        widget operations, e.g. insert (for a Text widget). We are going to hook
        this command and provide a facility ('register') to intercept the widget
        operation.  We will also intercept method calls on the tkinter class
        instance that represents the tk widget.

        In IDLE, WidgetRedirector is used in Percolator to intercept Text
        commands.  The function being registered provides access to the top
        of a Percolator chain.  At the bottom of the chain is a call to the
        original Tk widget operation.
    
    """
    def __init__(self, widget):
        """
        '''Initialize attributes and setup redirection.

                _operations: dict mapping operation name to new function.
                widget: the widget whose tcl command is to be intercepted.
                tk: widget.tk, a convenience attribute, probably not needed.
                orig: new name of the original tcl command.

                Since renaming to orig fails with TclError when orig already
                exists, only one WidgetDirector can exist for a given widget.
                '''
        """
    def __repr__(self):
        """
        %s(%s<%s>)
        """
    def close(self):
        """
        Unregister operations and revert redirection created by .__init__.
        """
    def register(self, operation, function):
        """
        '''Return OriginalCommand(operation) after registering function.

                Registration adds an operation: function pair to ._operations.
                It also adds a widget function attribute that masks the tkinter
                class instance method.  Method masking operates independently
                from command dispatch.

                If a second function is registered for the same operation, the
                first function is replaced in both places.
                '''
        """
    def unregister(self, operation):
        """
        '''Return the function for the operation, or None.

                Deleting the instance attribute unmasks the class attribute.
                '''
        """
    def dispatch(self, operation, *args):
        """
        '''Callback from Tcl which runs when the widget is referenced.

                If an operation has been registered in self._operations, apply the
                associated function to the args passed into Tcl. Otherwise, pass the
                operation through to Tk via the original Tcl function.

                Note that if a registered function is called, the operation is not
                passed through to Tk.  Apply the function returned by self.register()
                to *args to accomplish that.  For an example, see colorizer.py.

                '''
        """
def OriginalCommand:
    """
    '''Callable for original tk command that has been redirected.

        Returned by .register; can be used in the function registered.
        redir = WidgetRedirector(text)
        def my_insert(*args):
            print("insert", args)
            original_insert(*args)
        original_insert = redir.register("insert", my_insert)
        '''
    """
    def __init__(self, redir, operation):
        """
        '''Create .tk_call and .orig_and_operation for .__call__ method.

                .redir and .operation store the input args for __repr__.
                .tk and .orig copy attributes of .redir (probably not needed).
                '''
        """
    def __repr__(self):
        """
        %s(%r, %r)
        """
    def __call__(self, *args):
        """
         htest #
        """
    def my_insert(*args):
        """
        insert
        """
