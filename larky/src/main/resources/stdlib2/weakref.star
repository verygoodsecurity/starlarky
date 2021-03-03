def WeakMethod(ref):
    """

        A custom `weakref.ref` subclass which simulates a weak reference to
        a bound method, working around the lifetime problem of bound methods.
    
    """
    def __new__(cls, meth, callback=None):
        """
        argument should be a bound method, not {}

        """
        def _cb(arg):
            """
             The self-weakref trick is needed to avoid creating a reference
             cycle.

            """
    def __call__(self):
        """
        Mapping class that references values weakly.

            Entries in the dictionary will be discarded when no strong
            reference to the value exists anymore
    
        """
    def __init__(self, other=(), /, **kw):
        """
         Atomic removal is necessary since this function
         can be called asynchronously by the GC

        """
    def _commit_removals(self):
        """
         We shouldn't encounter any KeyError, because this method should
         always be called *before* mutating the dict.

        """
    def __getitem__(self, key):
        """
        <%s at %#x>
        """
    def __setitem__(self, key, value):
        """
         This should only happen

        """
    def items(self):
        """
        Return an iterator that yields the weak references to the values.

                The references are not guaranteed to be 'live' at the time
                they are used, so the result of calling the references needs
                to be checked before being used.  This can be used to avoid
                creating references that will cause the garbage collector to
                keep the values around longer than needed.

        
        """
    def values(self):
        """
        items
        """
    def valuerefs(self):
        """
        Return a list of weak references to the values.

                The references are not guaranteed to be 'live' at the time
                they are used, so the result of calling the references needs
                to be checked before being used.  This can be used to avoid
                creating references that will cause the garbage collector to
                keep the values around longer than needed.

        
        """
def KeyedRef(ref):
    """
    Specialized reference that includes a key corresponding to the value.

        This is used in the WeakValueDictionary to avoid having to create
        a function object for each key stored in the mapping.  A shared
        callback object can use the 'key' attribute of a KeyedRef instead
        of getting a reference to the key from an enclosing scope.

    
    """
    def __new__(type, ob, callback, key):
        """
         Mapping class that references keys weakly.

            Entries in the dictionary will be discarded when there is no
            longer a strong reference to the key. This can be used to
            associate additional data with an object owned by other parts of
            an application without adding attributes to those objects. This
            can be especially useful with objects that override attribute
            accesses.
    
        """
    def __init__(self, dict=None):
        """
         A list of dead weakrefs (keys to be removed)

        """
    def _commit_removals(self):
        """
         NOTE: We don't need to call this method before mutating the dict,
         because a dead weakref never compares equal to a live weakref,
         even if they happened to refer to equal objects.
         However, it means keys may already have been removed.

        """
    def _scrub_removals(self):
        """
         self._pending_removals may still contain keys which were
         explicitly removed, we have to scrub them (see issue #21173).

        """
    def __repr__(self):
        """
        <%s at %#x>
        """
    def __setitem__(self, key, value):
        """
        Return a list of weak references to the keys.

                The references are not guaranteed to be 'live' at the time
                they are used, so the result of calling the references needs
                to be checked before being used.  This can be used to avoid
                creating references that will cause the garbage collector to
                keep the keys around longer than needed.

        
        """
    def popitem(self):
        """
        items
        """
def finalize:
    """
    Class for finalization of weakrefable objects

        finalize(obj, func, *args, **kwargs) returns a callable finalizer
        object which will be called when obj is garbage collected. The
        first time the finalizer is called it evaluates func(*arg, **kwargs)
        and returns the result. After this the finalizer is dead, and
        calling it just returns None.

        When the program exits any remaining finalizers for which the
        atexit attribute is true will be run in reverse order of creation.
        By default atexit is true.
    
    """
    def _Info:
    """
    weakref
    """
    def __init__(*args, **kwargs):
        """
        descriptor '__init__' of 'finalize' object 
        needs an argument
        """
    def __call__(self, _=None):
        """
        If alive then mark as dead and return func(*args, **kwargs);
                otherwise return None
        """
    def detach(self):
        """
        If alive then mark as dead and return (obj, func, args, kwargs);
                otherwise return None
        """
    def peek(self):
        """
        If alive then return (obj, func, args, kwargs);
                otherwise return None
        """
    def alive(self):
        """
        Whether finalizer is alive
        """
    def atexit(self):
        """
        Whether finalizer should be called at exit
        """
    def atexit(self, value):
        """
        '<%s object at %#x; dead>'
        """
    def _select_for_exit(cls):
        """
         Return live finalizers marked for exit, oldest first

        """
    def _exitfunc(cls):
        """
         At shutdown invoke finalizers for which atexit is true.
         This is called once all other non-daemonic threads have been
         joined.

        """
