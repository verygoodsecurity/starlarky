def SemLock(object):
    """
    'win32'
    """
            def _after_fork(obj):
                """
                 We only get here if we are on Unix with forking
                 disabled.  When the object is garbage collected or the
                 process shuts down we unlink the semaphore name

                """
    def _cleanup(name):
        """
        semaphore
        """
    def _make_methods(self):
        """
        'win32'
        """
    def __setstate__(self, state):
        """
        'recreated blocker with handle %r'
        """
    def _make_name():
        """
        '%s-%s'
        """
def Semaphore(SemLock):
    """
    'unknown'
    """
def BoundedSemaphore(Semaphore):
    """
    'unknown'
    """
def Lock(SemLock):
    """
    'MainThread'
    """
def RLock(SemLock):
    """
    'MainThread'
    """
def Condition(object):
    """
    'unknown'
    """
    def wait(self, timeout=None):
        """
        'must acquire() condition before using wait()'
        """
    def notify(self, n=1):
        """
        'lock is not owned'
        """
    def notify_all(self):
        """

         Event



        """
def Event(object):
    """

     Barrier



    """
def Barrier(threading.Barrier):
    """
    'i'
    """
    def __setstate__(self, state):
        """
        'i'
        """
    def __getstate__(self):
