def _localimpl:
    """
    A class managing thread-local dicts
    """
    def __init__(self):
        """
         The key used in the Thread objects' attribute dicts.
         We keep it a string for speed but make it unlikely to clash with
         a "real" attribute.

        """
    def get_dict(self):
        """
        Return the dict for the current thread. Raises KeyError if none
                defined.
        """
    def create_dict(self):
        """
        Create a new dict for the current thread, and return it.
        """
        def local_deleted(_, key=key):
            """
             When the localimpl is deleted, remove the thread attribute.

            """
        def thread_deleted(_, idt=idt):
            """
             When the thread is deleted, remove the local dict.
             Note that this is suboptimal if the thread object gets
             caught in a reference loop. We would like to be called
             as soon as the OS-level thread ends instead.

            """
def _patch(self):
    """
    '_local__impl'
    """
def local:
    """
    '_local__impl'
    """
    def __new__(cls, /, *args, **kw):
        """
        Initialization arguments are not supported
        """
    def __getattribute__(self, name):
        """
        '__dict__'
        """
    def __delattr__(self, name):
        """
        '__dict__'
        """
