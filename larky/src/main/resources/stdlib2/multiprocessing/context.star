def ProcessError(Exception):
    """

     Base type for contexts. Bound methods of an instance of this type are included in __all__ of __init__.py



    """
def BaseContext(object):
    """
    '''Returns the number of CPUs in the system'''
    """
    def Manager(self):
        """
        '''Returns a manager associated with a running server process

                The managers methods such as `Lock()`, `Condition()` and `Queue()`
                can be used to create shared objects.
                '''
        """
    def Pipe(self, duplex=True):
        """
        '''Returns two connection object connected by a pipe'''
        """
    def Lock(self):
        """
        '''Returns a non-recursive lock object'''
        """
    def RLock(self):
        """
        '''Returns a recursive lock object'''
        """
    def Condition(self, lock=None):
        """
        '''Returns a condition object'''
        """
    def Semaphore(self, value=1):
        """
        '''Returns a semaphore object'''
        """
    def BoundedSemaphore(self, value=1):
        """
        '''Returns a bounded semaphore object'''
        """
    def Event(self):
        """
        '''Returns an event object'''
        """
    def Barrier(self, parties, action=None, timeout=None):
        """
        '''Returns a barrier object'''
        """
    def Queue(self, maxsize=0):
        """
        '''Returns a queue object'''
        """
    def JoinableQueue(self, maxsize=0):
        """
        '''Returns a queue object'''
        """
    def SimpleQueue(self):
        """
        '''Returns a queue object'''
        """
2021-03-02 20:46:53,531 : INFO : tokenize_signature : --> do i ever get here?
    def Pool(self, processes=None, initializer=None, initargs=(),
             maxtasksperchild=None):
        """
        '''Returns a process pool object'''
        """
    def RawValue(self, typecode_or_type, *args):
        """
        '''Returns a shared object'''
        """
    def RawArray(self, typecode_or_type, size_or_initializer):
        """
        '''Returns a shared array'''
        """
    def Value(self, typecode_or_type, *args, lock=True):
        """
        '''Returns a synchronized shared object'''
        """
    def Array(self, typecode_or_type, size_or_initializer, *, lock=True):
        """
        '''Returns a synchronized shared array'''
        """
    def freeze_support(self):
        """
        '''Check whether this is a fake forked process in a frozen executable.
                If so then run code specified by commandline and exit.
                '''
        """
    def get_logger(self):
        """
        '''Return package logger -- if it does not already exist then
                it is created.
                '''
        """
    def log_to_stderr(self, level=None):
        """
        '''Turn on logging and add a handler which prints to stderr'''
        """
    def allow_connection_pickling(self):
        """
        '''Install support for sending connections and sockets
                between processes
                '''
        """
    def set_executable(self, executable):
        """
        '''Sets the path to a python.exe or pythonw.exe binary used to run
                child processes instead of sys.executable when using the 'spawn'
                start method.  Useful for people embedding Python.
                '''
        """
    def set_forkserver_preload(self, module_names):
        """
        '''Set list of module names to try to load in forkserver process.
                This is really just a hint.
                '''
        """
    def get_context(self, method=None):
        """
        'cannot find context for %r'
        """
    def get_start_method(self, allow_none=False):
        """
        'cannot set start method of concrete context'
        """
    def reducer(self):
        """
        '''Controls how objects will be reduced to a form that can be
                shared with other processes.'''
        """
    def reducer(self, reduction):
        """
        'reduction'
        """
    def _check_available(self):
        """

         Type of default context -- underlying context can be set at most once



        """
def Process(process.BaseProcess):
    """
    'context has already been set'
    """
    def get_start_method(self, allow_none=False):
        """
        'win32'
        """
    def ForkProcess(process.BaseProcess):
    """
    'fork'
    """
        def _Popen(process_obj):
            """
            'spawn'
            """
        def _Popen(process_obj):
            """
            'forkserver'
            """
        def _Popen(process_obj):
            """
            'fork'
            """
    def SpawnContext(BaseContext):
    """
    'spawn'
    """
    def ForkServerContext(BaseContext):
    """
    'forkserver'
    """
        def _check_available(self):
            """
            'forkserver start method not available'
            """
    def SpawnProcess(process.BaseProcess):
    """
    'spawn'
    """
        def _Popen(process_obj):
            """
            'spawn'
            """
def _force_start_method(method):
    """

     Check that the current thread is spawning a child process



    """
def get_spawning_popen():
    """
    'spawning_popen'
    """
def set_spawning_popen(popen):
    """
    '%s objects should only be shared between processes'
    ' through inheritance'
    """
