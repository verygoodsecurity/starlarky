def start_new_thread(function, args, kwargs={}):
    """
    Dummy implementation of _thread.start_new_thread().

        Compatibility is maintained by making sure that ``args`` is a
        tuple and ``kwargs`` is a dictionary.  If an exception is raised
        and it is SystemExit (which can be done by _thread.exit()) it is
        caught and nothing is done; all other exceptions are printed out
        by using traceback.print_exc().

        If the executed function calls interrupt_main the KeyboardInterrupt will be
        raised when the function returns.

    
    """
def exit():
    """
    Dummy implementation of _thread.exit().
    """
def get_ident():
    """
    Dummy implementation of _thread.get_ident().

        Since this module should only be used when _threadmodule is not
        available, it is safe to assume that the current process is the
        only thread.  Thus a constant can be safely returned.
    
    """
def allocate_lock():
    """
    Dummy implementation of _thread.allocate_lock().
    """
def stack_size(size=None):
    """
    Dummy implementation of _thread.stack_size().
    """
def _set_sentinel():
    """
    Dummy implementation of _thread._set_sentinel().
    """
def LockType(object):
    """
    Class implementing dummy implementation of _thread.LockType.

        Compatibility is maintained by maintaining self.locked_status
        which is a boolean that stores the state of the lock.  Pickling of
        the lock, though, should not be done since if the _thread module is
        then used with an unpickled ``lock()`` from here problems could
        occur from this class not having atomic methods.

    
    """
    def __init__(self):
        """
        Dummy implementation of acquire().

                For blocking calls, self.locked_status is automatically set to
                True and returned appropriately based on value of
                ``waitflag``.  If it is non-blocking, then the value is
                actually checked and not set if it is already acquired.  This
                is all done so that threading.Condition's assert statements
                aren't triggered and throw a little fit.

        
        """
    def __exit__(self, typ, val, tb):
        """
        Release the dummy lock.
        """
    def locked(self):
        """
        <%s %s.%s object at %s>
        """
def RLock(LockType):
    """
    Dummy implementation of threading._RLock.

        Re-entrant lock can be aquired multiple times and needs to be released
        just as many times. This dummy implemention does not check wheter the
        current thread actually owns the lock, but does accounting on the call
        counts.
    
    """
    def __init__(self):
        """
        Aquire the lock, can be called multiple times in succession.
        
        """
    def release(self):
        """
        Release needs to be called once for every call to acquire().
        
        """
def interrupt_main():
    """
    Set _interrupt flag to True to have start_new_thread raise
        KeyboardInterrupt upon exiting.
    """
