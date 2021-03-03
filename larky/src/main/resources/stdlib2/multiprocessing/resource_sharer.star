    def DupSocket(object):
    """
    '''Picklable wrapper for a socket.'''
    """
        def __init__(self, sock):
            """
            '''Get the socket.  This should only be called once.'''
            """
    def DupFd(object):
    """
    '''Wrapper for fd which can be used at any time.'''
    """
        def __init__(self, fd):
            """
            '''Get the fd.  This should only be called once.'''
            """
def _ResourceSharer(object):
    """
    '''Manager for resources using background thread.'''
    """
    def __init__(self):
        """
        '''Register resource, returning an identifier.'''
        """
    def get_connection(ident):
        """
        '''Return connection from which to receive identified resource.'''
        """
    def stop(self, timeout=None):
        """
        '''Stop the background thread and clear registered resources.'''
        """
    def _afterfork(self):
        """
         If self._lock was locked at the time of the fork, it may be broken
         -- see issue 6721.  Replace it without letting it be gc'ed.

        """
    def _start(self):
        """
        Already have Listener
        """
    def _serve(self):
        """
        'pthread_sigmask'
        """
