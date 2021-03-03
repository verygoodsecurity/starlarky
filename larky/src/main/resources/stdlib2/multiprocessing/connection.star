def _init_timeout(timeout=CONNECTION_TIMEOUT):
    """





    """
def arbitrary_address(family):
    """
    '''
        Return an arbitrary free address for the given family
        '''
    """
def _validate_family(family):
    """
    '''
        Checks if the family is valid for the current environment.
        '''
    """
def address_type(address):
    """
    '''
        Return the types of the address

        This can be 'AF_INET', 'AF_UNIX', or 'AF_PIPE'
        '''
    """
def _ConnectionBase:
    """
    invalid handle
    """
    def __del__(self):
        """
        handle is closed
        """
    def _check_readable(self):
        """
        connection is write-only
        """
    def _check_writable(self):
        """
        connection is read-only
        """
    def _bad_message_length(self):
        """
        bad message length
        """
    def closed(self):
        """
        True if the connection is closed
        """
    def readable(self):
        """
        True if the connection is readable
        """
    def writable(self):
        """
        True if the connection is writable
        """
    def fileno(self):
        """
        File descriptor or handle of the connection
        """
    def close(self):
        """
        Close the connection
        """
    def send_bytes(self, buf, offset=0, size=None):
        """
        Send the bytes data from a bytes-like object
        """
    def send(self, obj):
        """
        Send a (picklable) object
        """
    def recv_bytes(self, maxlength=None):
        """

                Receive bytes data as a bytes object.
        
        """
    def recv_bytes_into(self, buf, offset=0):
        """

                Receive bytes data into a writeable bytes-like object.
                Return the number of bytes read.
        
        """
    def recv(self):
        """
        Receive a (picklable) object
        """
    def poll(self, timeout=0.0):
        """
        Whether there is any input available to be read
        """
    def __enter__(self):
        """

                Connection class based on a Windows named pipe.
                Overlapped I/O is used, so the handles must have been created
                with FILE_FLAG_OVERLAPPED.
        
        """
        def _close(self, _CloseHandle=_winapi.CloseHandle):
            """
            shouldn't get here; expected KeyboardInterrupt
            """
        def _poll(self, timeout):
            """

                Connection class based on an arbitrary file descriptor (Unix only), or
                a socket handle (Windows).
    
            """
        def _close(self, _close=_multiprocessing.closesocket):
            """
            got end of file during message
            """
    def _send_bytes(self, buf):
        """
        !i
        """
    def _recv_bytes(self, maxsize=None):
        """
        !i
        """
    def _poll(self, timeout):
        """

         Public functions



        """
def Listener(object):
    """
    '''
        Returns a listener object.

        This is a wrapper for a bound socket which is 'listening' for
        connections, or for a Windows named pipe.
        '''
    """
    def __init__(self, address=None, family=None, backlog=1, authkey=None):
        """
        'AF_PIPE'
        """
    def accept(self):
        """
        '''
                Accept a connection on the bound socket or named pipe of `self`.

                Returns a `Connection` object.
                '''
        """
    def close(self):
        """
        '''
                Close the bound socket or named pipe of `self`.
                '''
        """
    def address(self):
        """
        '''
            Returns a connection to the address of a `Listener`
            '''
        """
    def Pipe(duplex=True):
        """
        '''
                Returns pair of connection objects at either end of a pipe
                '''
        """
    def Pipe(duplex=True):
        """
        '''
                Returns pair of connection objects at either end of a pipe
                '''
        """
def SocketListener(object):
    """
    '''
        Representation of a socket which is bound to an address and listening
        '''
    """
    def __init__(self, address, family, backlog=1):
        """
         SO_REUSEADDR has different semantics on Windows (issue #2550).

        """
    def accept(self):
        """
        '''
            Return a connection object connected to the socket given by `address`
            '''
        """
    def PipeListener(object):
    """
    '''
            Representation of a named pipe
            '''
    """
        def __init__(self, address, backlog=None):
            """
            'listener created with address=%r'
            """
        def _new_handle(self, first=False):
            """
             ERROR_NO_DATA can occur if a client has already connected,
             written data and then disconnected -- see Issue 14725.

            """
        def _finalize_pipe_listener(queue, address):
            """
            'closing listener with address=%r'
            """
    def PipeClient(address):
        """
        '''
                Return a connection object connected to the pipe given by `address`
                '''
        """
def deliver_challenge(connection, authkey):
    """
    Authkey must be bytes, not {0!s}
    """
def answer_challenge(connection, authkey):
    """
    Authkey must be bytes, not {0!s}
    """
def ConnectionWrapper(object):
    """
    'fileno'
    """
    def send(self, obj):
        """
        'utf-8'
        """
def _xml_loads(s):
    """
    'utf-8'
    """
def XmlListener(Listener):
    """

     Wait



    """
    def _exhaustive_wait(handles, timeout):
        """
         Return ALL handles which are currently signalled.  (Only
         returning the first signalled might create starvation issues.)

        """
    def wait(object_list, timeout=None):
        """
        '''
                Wait till an object in object_list is ready/readable.

                Returns list of those objects in object_list which are ready/readable.
                '''
        """
    def wait(object_list, timeout=None):
        """
        '''
                Wait till an object in object_list is ready/readable.

                Returns list of those objects in object_list which are ready/readable.
                '''
        """
    def reduce_connection(conn):
