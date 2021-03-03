def BdbQuit(Exception):
    """
    Exception to give up completely.
    """
def Bdb:
    """
    Generic Python debugger base class.

        This class takes care of details of the trace facility;
        a derived class should implement user interaction.
        The standard debugger class (pdb.Pdb) is an example.

        The optional skip argument must be an iterable of glob-style
        module name patterns.  The debugger will not step into frames
        that originate in a module that matches one of these patterns.
        Whether a frame is considered to originate in a certain module
        is determined by the __name__ in the frame globals.
    
    """
    def __init__(self, skip=None):
        """
        Return canonical form of filename.

                For real filenames, the canonical form is a case-normalized (on
                case insensitive filesystems) absolute path.  'Filenames' with
                angle brackets, such as "<stdin>", generated in interactive
                mode, are returned unchanged.
        
        """
    def reset(self):
        """
        Set values of attributes as ready to start debugging.
        """
    def trace_dispatch(self, frame, event, arg):
        """
        Dispatch a trace function for debugged frames based on the event.

                This function is installed as the trace function for debugged
                frames. Its return value is the new trace function, which is
                usually itself. The default implementation decides how to
                dispatch a frame, depending on the type of event (passed in as a
                string) that is about to be executed.

                The event can be one of the following:
                    line: A new line of code is going to be executed.
                    call: A function is about to be called or another code block
                          is entered.
                    return: A function or other code block is about to return.
                    exception: An exception has occurred.
                    c_call: A C function is about to be called.
                    c_return: A C function has returned.
                    c_exception: A C function has raised an exception.

                For the Python events, specialized functions (see the dispatch_*()
                methods) are called.  For the C events, no action is taken.

                The arg parameter depends on the previous event.
        
        """
    def dispatch_line(self, frame):
        """
        Invoke user function and return trace function for line event.

                If the debugger stops on the current line, invoke
                self.user_line(). Raise BdbQuit if self.quitting is set.
                Return self.trace_dispatch to continue tracing in this scope.
        
        """
    def dispatch_call(self, frame, arg):
        """
        Invoke user function and return trace function for call event.

                If the debugger stops on this function call, invoke
                self.user_call(). Raise BbdQuit if self.quitting is set.
                Return self.trace_dispatch to continue tracing in this scope.
        
        """
    def dispatch_return(self, frame, arg):
        """
        Invoke user function and return trace function for return event.

                If the debugger stops on this function return, invoke
                self.user_return(). Raise BdbQuit if self.quitting is set.
                Return self.trace_dispatch to continue tracing in this scope.
        
        """
    def dispatch_exception(self, frame, arg):
        """
        Invoke user function and return trace function for exception event.

                If the debugger stops on this exception, invoke
                self.user_exception(). Raise BdbQuit if self.quitting is set.
                Return self.trace_dispatch to continue tracing in this scope.
        
        """
    def is_skipped_module(self, module_name):
        """
        Return True if module_name matches any skip pattern.
        """
    def stop_here(self, frame):
        """
        Return True if frame is below the starting frame in the stack.
        """
    def break_here(self, frame):
        """
        Return True if there is an effective breakpoint for this line.

                Check for line or function breakpoint and if in effect.
                Delete temporary breakpoints if effective() says to.
        
        """
    def do_clear(self, arg):
        """
        Remove temporary breakpoint.

                Must implement in derived classes or get NotImplementedError.
        
        """
    def break_anywhere(self, frame):
        """
        Return True if there is any breakpoint for frame's filename.
        
        """
    def user_call(self, frame, argument_list):
        """
        Called if we might stop in a function.
        """
    def user_line(self, frame):
        """
        Called when we stop or break at a line.
        """
    def user_return(self, frame, return_value):
        """
        Called when a return trap is set here.
        """
    def user_exception(self, frame, exc_info):
        """
        Called when we stop on an exception.
        """
    def _set_stopinfo(self, stopframe, returnframe, stoplineno=0):
        """
        Set the attributes for stopping.

                If stoplineno is greater than or equal to 0, then stop at line
                greater than or equal to the stopline.  If stoplineno is -1, then
                don't stop at all.
        
        """
    def set_until(self, frame, lineno=None):
        """
        Stop when the line with the lineno greater than the current one is
                reached or when returning from current frame.
        """
    def set_step(self):
        """
        Stop after one line of code.
        """
    def set_next(self, frame):
        """
        Stop on the next line in or below the given frame.
        """
    def set_return(self, frame):
        """
        Stop when returning from the given frame.
        """
    def set_trace(self, frame=None):
        """
        Start debugging from frame.

                If frame is not specified, debugging starts from caller's frame.
        
        """
    def set_continue(self):
        """
        Stop only at breakpoints or when finished.

                If there are no breakpoints, set the system trace function to None.
        
        """
    def set_quit(self):
        """
        Set quitting attribute to True.

                Raises BdbQuit exception in the next call to a dispatch_*() method.
        
        """
2021-03-02 20:46:40,608 : INFO : tokenize_signature : --> do i ever get here?
    def set_break(self, filename, lineno, temporary=False, cond=None,
                  funcname=None):
        """
        Set a new breakpoint for filename:lineno.

                If lineno doesn't exist for the filename, return an error message.
                The filename should be in canonical form.
        
        """
    def _prune_breaks(self, filename, lineno):
        """
        Prune breakpoints for filename:lineno.

                A list of breakpoints is maintained in the Bdb instance and in
                the Breakpoint class.  If a breakpoint in the Bdb instance no
                longer exists in the Breakpoint class, then it's removed from the
                Bdb instance.
        
        """
    def clear_break(self, filename, lineno):
        """
        Delete breakpoints for filename:lineno.

                If no breakpoints were set, return an error message.
        
        """
    def clear_bpbynumber(self, arg):
        """
        Delete a breakpoint by its index in Breakpoint.bpbynumber.

                If arg is invalid, return an error message.
        
        """
    def clear_all_file_breaks(self, filename):
        """
        Delete all breakpoints in filename.

                If none were set, return an error message.
        
        """
    def clear_all_breaks(self):
        """
        Delete all existing breakpoints.

                If none were set, return an error message.
        
        """
    def get_bpbynumber(self, arg):
        """
        Return a breakpoint by its index in Breakpoint.bybpnumber.

                For invalid arg values or if the breakpoint doesn't exist,
                raise a ValueError.
        
        """
    def get_break(self, filename, lineno):
        """
        Return True if there is a breakpoint for filename:lineno.
        """
    def get_breaks(self, filename, lineno):
        """
        Return all breakpoints for filename:lineno.

                If no breakpoints are set, return an empty list.
        
        """
    def get_file_breaks(self, filename):
        """
        Return all lines with breakpoints for filename.

                If no breakpoints are set, return an empty list.
        
        """
    def get_all_breaks(self):
        """
        Return all breakpoints that are set.
        """
    def get_stack(self, f, t):
        """
        Return a list of (frame, lineno) in a stack trace and a size.

                List starts with original calling frame, if there is one.
                Size may be number of frames above or below f.
        
        """
    def format_stack_entry(self, frame_lineno, lprefix=': '):
        """
        Return a string with information about a stack entry.

                The stack entry frame_lineno is a (frame, lineno) tuple.  The
                return string contains the canonical filename, the function name
                or '<lambda>', the input arguments, the return value, and the
                line of code (if it exists).

        
        """
    def run(self, cmd, globals=None, locals=None):
        """
        Debug a statement executed via the exec() function.

                globals defaults to __main__.dict; locals defaults to globals.
        
        """
    def runeval(self, expr, globals=None, locals=None):
        """
        Debug an expression executed via the eval() function.

                globals defaults to __main__.dict; locals defaults to globals.
        
        """
    def runctx(self, cmd, globals, locals):
        """
        For backwards-compatibility.  Defers to run().
        """
    def runcall(*args, **kwds):
        """
        Debug a single function call.

                Return the result of the function call.
        
        """
def set_trace():
    """
    Start debugging with a Bdb instance from the caller's frame.
    """
def Breakpoint:
    """
    Breakpoint class.

        Implements temporary breakpoints, ignore counts, disabling and
        (re)-enabling, and conditionals.

        Breakpoints are indexed by number through bpbynumber and by
        the (file, line) tuple using bplist.  The former points to a
        single instance of class Breakpoint.  The latter points to a
        list of such instances since there may be more than one
        breakpoint per line.

        When creating a breakpoint, its associated filename should be
        in canonical form.  If funcname is defined, a breakpoint hit will be
        counted when the first line of that function is executed.  A
        conditional breakpoint always counts a hit.
    
    """
    def __init__(self, file, line, temporary=False, cond=None, funcname=None):
        """
         Needed if funcname is not None.

        """
    def deleteMe(self):
        """
        Delete the breakpoint from the list associated to a file:line.

                If it is the last breakpoint in that position, it also deletes
                the entry for the file:line.
        
        """
    def enable(self):
        """
        Mark the breakpoint as enabled.
        """
    def disable(self):
        """
        Mark the breakpoint as disabled.
        """
    def bpprint(self, out=None):
        """
        Print the output of bpformat().

                The optional out argument directs where the output is sent
                and defaults to standard output.
        
        """
    def bpformat(self):
        """
        Return a string with information about the breakpoint.

                The information includes the breakpoint number, temporary
                status, file:line position, break condition, number of times to
                ignore, and number of times hit.

        
        """
    def __str__(self):
        """
        Return a condensed description of the breakpoint.
        """
def checkfuncname(b, frame):
    """
    Return True if break should happen here.

        Whether a break should happen depends on the way that b (the breakpoint)
        was set.  If it was set via line number, check if b.line is the same as
        the one in the frame.  If it was set via function name, check if this is
        the right function and if it is on the first executable line.
    
    """
def effective(file, line, frame):
    """
    Determine which breakpoint for this file:line is to be acted upon.

        Called only if we know there is a breakpoint at this location.  Return
        the breakpoint that was triggered and a boolean that indicates if it is
        ok to delete a temporary breakpoint.  Return (None, None) if there is no
        matching breakpoint.
    
    """
def Tdb(Bdb):
    """
    '???'
    """
    def user_line(self, frame):
        """
        '???'
        """
    def user_return(self, frame, retval):
        """
        '+++ return'
        """
    def user_exception(self, frame, exc_stuff):
        """
        '+++ exception'
        """
def foo(n):
    """
    'foo('
    """
def bar(a):
    """
    'bar('
    """
def test():
    """
    'import bdb; bdb.foo(10)'
    """
