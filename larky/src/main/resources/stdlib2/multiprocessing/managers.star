def reduce_array(a):
    """
    'items'
    """
    def rebuild_as_list(obj):
        """

         Type for identifying shared objects



        """
def Token(object):
    """
    '''
        Type to uniquely identify a shared object
        '''
    """
    def __init__(self, typeid, address, id):
        """
        '%s(typeid=%r, address=%r, id=%r)'
        """
def dispatch(c, id, methodname, args=(), kwds={}):
    """
    '''
        Send a message to manager using connection `c` and return response
        '''
    """
def convert_to_error(kind, result):
    """
    '#ERROR'
    """
def RemoteError(Exception):
    """
    '\n'
    """
def all_methods(obj):
    """
    '''
        Return a list of names of methods of `obj`
        '''
    """
def public_methods(obj):
    """
    '''
        Return a list of names of methods of `obj` which do not start with '_'
        '''
    """
def Server(object):
    """
    '''
        Server class which runs in a process controlled by a manager object
        '''
    """
    def __init__(self, registry, address, authkey, serializer):
        """
        Authkey {0!r} is type {1!s}, not bytes
        """
    def serve_forever(self):
        """
        '''
                Run the server forever
                '''
        """
    def accepter(self):
        """
        '''
                Handle a new connection
                '''
        """
    def serve_client(self, conn):
        """
        '''
                Handle requests from the proxies in a particular process/thread
                '''
        """
    def fallback_getvalue(self, conn, ident, obj):
        """
        '__str__'
        """
    def dummy(self, c):
        """
        '''
                Return some info --- useful to spot problems with refcounting
                '''
        """
    def number_of_objects(self, c):
        """
        '''
                Number of shared objects
                '''
        """
    def shutdown(self, c):
        """
        '''
                Shutdown this process
                '''
        """
    def create(*args, **kwds):
        """
        '''
                Create a new shared object and return its id
                '''
        """
    def get_methods(self, c, token):
        """
        '''
                Return the methods of the shared object indicated by token
                '''
        """
    def accept_connection(self, c, name):
        """
        '''
                Spawn a new thread to serve this connection
                '''
        """
    def incref(self, c, ident):
        """
         If no external references exist but an internal (to the
         manager) still does and a new external reference is created
         from it, restore the manager's tracking of it from the
         previously stashed internal ref.

        """
    def decref(self, c, ident):
        """
        'Server DECREF skipping %r'
        """
def State(object):
    """
    'value'
    """
def BaseManager(object):
    """
    '''
        Base class for managers
        '''
    """
2021-03-02 20:46:55,162 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, address=None, authkey=None, serializer='pickle',
                 ctx=None):
        """
         XXX not final address if eg ('', 0)
        """
    def get_server(self):
        """
        '''
                Return server object with serve_forever() method and address attribute
                '''
        """
    def connect(self):
        """
        '''
                Connect manager object to the server process
                '''
        """
    def start(self, initializer=None, initargs=()):
        """
        '''
                Spawn a server process for this manager object
                '''
        """
2021-03-02 20:46:55,165 : INFO : tokenize_signature : --> do i ever get here?
    def _run_server(cls, registry, address, authkey, serializer, writer,
                    initializer=None, initargs=()):
        """
        '''
                Create a server, report its address and run it
                '''
        """
    def _create(self, typeid, /, *args, **kwds):
        """
        '''
                Create a new shared object; return the token and exposed tuple
                '''
        """
    def join(self, timeout=None):
        """
        '''
                Join the manager process (if it has been spawned)
                '''
        """
    def _debug_info(self):
        """
        '''
                Return some info about the servers shared objects and connections
                '''
        """
    def _number_of_objects(self):
        """
        '''
                Return the number of shared objects
                '''
        """
    def __enter__(self):
        """
        Unable to start server
        """
    def __exit__(self, exc_type, exc_val, exc_tb):
        """
        '''
                Shutdown the manager process; will be registered as a finalizer
                '''
        """
    def address(self):
        """
        '''
                Register a typeid with the manager type
                '''
        """
            def temp(self, /, *args, **kwds):
                """
                'requesting creation of a shared %r object'
                """
def ProcessLocalSet(set):
    """

     Definition of BaseProxy



    """
def BaseProxy(object):
    """
    '''
        A base for proxies of shared objects
        '''
    """
2021-03-02 20:46:55,169 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, token, serializer, manager=None,
                 authkey=None, exposed=None, incref=True, manager_owned=False):
        """
         self._tls is used to record the connection used by this
         thread to communicate with the manager at token.address

        """
    def _connect(self):
        """
        'making connection to manager'
        """
    def _callmethod(self, methodname, args=(), kwds={}):
        """
        '''
                Try to call a method of the referent and return a copy of the result
                '''
        """
    def _getvalue(self):
        """
        '''
                Get a copy of the value of the referent
                '''
        """
    def _incref(self):
        """
        'owned_by_manager skipped INCREF of %r'
        """
    def _decref(token, authkey, state, tls, idset, _Client):
        """
         check whether manager is still alive

        """
    def _after_fork(self):
        """
         the proxy may just be for a manager which has shutdown

        """
    def __reduce__(self):
        """
        'authkey'
        """
    def __deepcopy__(self, memo):
        """
        '<%s object, typeid %r at %#x>'
        """
    def __str__(self):
        """
        '''
                Return representation of the referent (or a fall-back if that fails)
                '''
        """
def RebuildProxy(func, token, serializer, kwds):
    """
    '''
        Function used for unpickling proxy objects.
        '''
    """
def MakeProxyType(name, exposed, _cache={}):
    """
    '''
        Return a proxy type whose methods are given by `exposed`
        '''
    """
2021-03-02 20:46:55,175 : INFO : tokenize_signature : --> do i ever get here?
def AutoProxy(token, serializer, manager=None, authkey=None,
              exposed=None, incref=True):
    """
    '''
        Return an auto-proxy for `token`
        '''
    """
def Namespace(object):
    """
    '_'
    """
def Value(object):
    """
    '%s(%r, %r)'
    """
def Array(typecode, sequence, lock=True):
    """

     Proxy types used by SyncManager



    """
def IteratorProxy(BaseProxy):
    """
    '__next__'
    """
    def __iter__(self):
        """
        '__next__'
        """
    def send(self, *args):
        """
        'send'
        """
    def throw(self, *args):
        """
        'throw'
        """
    def close(self, *args):
        """
        'close'
        """
def AcquirerProxy(BaseProxy):
    """
    'acquire'
    """
    def acquire(self, blocking=True, timeout=None):
        """
        'acquire'
        """
    def release(self):
        """
        'release'
        """
    def __enter__(self):
        """
        'acquire'
        """
    def __exit__(self, exc_type, exc_val, exc_tb):
        """
        'release'
        """
def ConditionProxy(AcquirerProxy):
    """
    'acquire'
    """
    def wait(self, timeout=None):
        """
        'wait'
        """
    def notify(self, n=1):
        """
        'notify'
        """
    def notify_all(self):
        """
        'notify_all'
        """
    def wait_for(self, predicate, timeout=None):
        """
        'is_set'
        """
    def is_set(self):
        """
        'is_set'
        """
    def set(self):
        """
        'set'
        """
    def clear(self):
        """
        'clear'
        """
    def wait(self, timeout=None):
        """
        'wait'
        """
def BarrierProxy(BaseProxy):
    """
    '__getattribute__'
    """
    def wait(self, timeout=None):
        """
        'wait'
        """
    def abort(self):
        """
        'abort'
        """
    def reset(self):
        """
        'reset'
        """
    def parties(self):
        """
        '__getattribute__'
        """
    def n_waiting(self):
        """
        '__getattribute__'
        """
    def broken(self):
        """
        '__getattribute__'
        """
def NamespaceProxy(BaseProxy):
    """
    '__getattribute__'
    """
    def __getattr__(self, key):
        """
        '_'
        """
    def __setattr__(self, key, value):
        """
        '_'
        """
    def __delattr__(self, key):
        """
        '_'
        """
def ValueProxy(BaseProxy):
    """
    'get'
    """
    def get(self):
        """
        'get'
        """
    def set(self, value):
        """
        'set'
        """
def ListProxy(BaseListProxy):
    """
    'extend'
    """
    def __imul__(self, value):
        """
        '__imul__'
        """
def PoolProxy(BasePoolProxy):
    """

     Definition of SyncManager



    """
def SyncManager(BaseManager):
    """
    '''
        Subclass of `BaseManager` which supports a number of shared object types.

        The types registered are those intended for the synchronization
        of threads, plus `dict`, `list` and `Namespace`.

        The `multiprocessing.Manager()` function creates started instances of
        this class.
        '''
    """
    def _SharedMemoryTracker:
    """
    Manages one or more shared memory segments.
    """
        def __init__(self, name, segment_names=[]):
            """
            Adds the supplied shared memory block name to tracker.
            """
        def destroy_segment(self, segment_name):
            """
            Calls unlink() on the shared memory block with the supplied name
                        and removes it from the list of blocks being tracked.
            """
        def unlink(self):
            """
            Calls destroy_segment() on all tracked shared memory blocks.
            """
        def __del__(self):
            """
            f"Call {self.__class__.__name__}.__del__ in {getpid()}
            """
        def __getstate__(self):
            """
            'track_segment'
            """
        def __init__(self, *args, **kwargs):
            """
             The address of Linux abstract namespaces can be bytes

            """
        def create(*args, **kwargs):
            """
            Create a new distributed-shared object (not backed by a shared
                        memory block) and return its id to be used in a Proxy Object.
            """
        def shutdown(self, c):
            """
            Call unlink() on all tracked shared memory, terminate the Server.
            """
        def track_segment(self, c, segment_name):
            """
            Adds the supplied shared memory block name to Server's tracker.
            """
        def release_segment(self, c, segment_name):
            """
            Calls unlink() on the shared memory block with the supplied name
                        and removes it from the tracker instance inside the Server.
            """
        def list_segments(self, c):
            """
            Returns a list of names of shared memory blocks that the Server
                        is currently tracking.
            """
    def SharedMemoryManager(BaseManager):
    """
    Like SyncManager but uses SharedMemoryServer instead of Server.

            It provides methods for creating and returning SharedMemory instances
            and for creating a list-like object (ShareableList) backed by shared
            memory.  It also provides methods that create and return Proxy Objects
            that support synchronization across processes (i.e. multi-process-safe
            locks and semaphores).
        
    """
        def __init__(self, *args, **kwargs):
            """
            posix
            """
        def __del__(self):
            """
            f"{self.__class__.__name__}.__del__ by pid {getpid()}
            """
        def get_server(self):
            """
            'Better than monkeypatching for now; merge into Server ultimately'
            """
        def SharedMemory(self, size):
            """
            Returns a new SharedMemory instance with the specified size in
                        bytes, to be tracked by the manager.
            """
        def ShareableList(self, sequence):
            """
            Returns a new ShareableList instance populated with the values
                        from the input sequence, to be tracked by the manager.
            """
