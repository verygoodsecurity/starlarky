def _python_exit():
    """
     Break a reference cycle with the exception 'exc'

    """
def _worker(executor_reference, work_queue, initializer, initargs):
    """
    'Exception in initializer:'
    """
def BrokenThreadPool(_base.BrokenExecutor):
    """

        Raised when a worker thread in a ThreadPoolExecutor failed initializing.
    
    """
def ThreadPoolExecutor(_base.Executor):
    """
     Used to assign unique thread names when thread_name_prefix is not supplied.

    """
2021-03-02 20:53:53,871 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, max_workers=None, thread_name_prefix='',
                 initializer=None, initargs=()):
        """
        Initializes a new ThreadPoolExecutor instance.

                Args:
                    max_workers: The maximum number of threads that can be used to
                        execute the given calls.
                    thread_name_prefix: An optional name prefix to give our threads.
                    initializer: A callable used to initialize worker threads.
                    initargs: A tuple of arguments to pass to the initializer.
        
        """
    def submit(*args, **kwargs):
        """
        descriptor 'submit' of 'ThreadPoolExecutor' object 
        needs an argument
        """
    def _adjust_thread_count(self):
        """
         if idle threads are available, don't spin new threads

        """
        def weakref_cb(_, q=self._work_queue):
            """
            '%s_%d'
            """
    def _initializer_failed(self):
        """
        'A thread initializer failed, the thread pool '
        'is not usable anymore'
        """
    def shutdown(self, wait=True):
