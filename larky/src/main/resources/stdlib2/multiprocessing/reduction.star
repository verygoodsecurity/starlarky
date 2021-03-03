def ForkingPickler(pickle.Pickler):
    """
    '''Pickler subclass used by multiprocessing.'''
    """
    def __init__(self, *args):
        """
        '''Register a reduce function for a type.'''
        """
    def dumps(cls, obj, protocol=None):
        """
        '''Replacement for pickle.dump() using ForkingPickler.'''
        """
2021-03-02 20:46:52,467 : INFO : tokenize_signature : --> do i ever get here?
    def duplicate(handle, target_process=None, inheritable=False,
                  *, source_process=None):
        """
        '''Duplicate a handle.  (target_process is a handle not a pid!)'''
        """
    def steal_handle(source_pid, handle):
        """
        '''Steal a handle from process identified by source_pid.'''
        """
    def send_handle(conn, handle, destination_pid):
        """
        '''Send a handle over a local connection.'''
        """
    def recv_handle(conn):
        """
        '''Receive a handle over a local connection.'''
        """
    def DupHandle(object):
    """
    '''Picklable wrapper for a handle.'''
    """
        def __init__(self, handle, access, pid=None):
            """
             We just duplicate the handle in the current process and
             let the receiving process steal the handle.

            """
        def detach(self):
            """
            '''Get the handle.  This should only be called once.'''
            """
    def sendfds(sock, fds):
        """
        '''Send an array of fds over an AF_UNIX socket.'''
        """
    def recvfds(sock, size):
        """
        '''Receive an array of fds over an AF_UNIX socket.'''
        """
    def send_handle(conn, handle, destination_pid):
        """
        '''Send a handle over a local connection.'''
        """
    def recv_handle(conn):
        """
        '''Receive a handle over a local connection.'''
        """
    def DupFd(fd):
        """
        '''Return a wrapper for an fd.'''
        """
def _reduce_method(m):
    """

     Make sockets picklable



    """
    def _reduce_socket(s):
        """
        '''Abstract base class for use in implementing a Reduction class
            suitable for use in replacing the standard reduction mechanism
            used in multiprocessing.'''
        """
    def __init__(self, *args):
