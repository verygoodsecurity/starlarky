def _strerror(err):
    """
    Unknown error %s
    """
def ExitNow(Exception):
    """
     accepting sockets should not be writable

    """
def poll2(timeout=0.0, map=None):
    """
     Use the poll() support added to the select module in Python 2.0

    """
def loop(timeout=30.0, use_poll=False, map=None, count=None):
    """
    'poll'
    """
def dispatcher:
    """
    'warning'
    """
    def __init__(self, sock=None, map=None):
        """
         Set to nonblocking just to make sure for cases where we
         get a socket from a blocking source.

        """
    def __repr__(self):
        """
        .
        """
    def add_channel(self, map=None):
        """
        self.log_info('adding channel %s' % self)

        """
    def del_channel(self, map=None):
        """
        self.log_info('closing channel %d:%s' % (fd, self))

        """
    def create_socket(self, family=socket.AF_INET, type=socket.SOCK_STREAM):
        """
         try to re-use a server port if possible

        """
    def readable(self):
        """
         ==================================================
         socket object methods.
         ==================================================


        """
    def listen(self, num):
        """
        'nt'
        """
    def bind(self, addr):
        """
        'nt'
        """
    def accept(self):
        """
         XXX can return either an address pair or None

        """
    def send(self, data):
        """
         a closed connection is indicated by signaling
         a read condition, and having recv() return 0.

        """
    def close(self):
        """
         log and log_info may be overridden to provide more sophisticated
         logging and warning methods. In general, log is for 'hit' logging
         and 'log_info' is for informational, warning and error logging.


        """
    def log(self, message):
        """
        'log: %s\n'
        """
    def log_info(self, message, type='info'):
        """
        '%s: %s'
        """
    def handle_read_event(self):
        """
         accepting sockets are never connected, they "spawn" new
         sockets that are connected

        """
    def handle_connect_event(self):
        """
         Accepting sockets shouldn't get a write event.
         We will pretend it didn't happen.

        """
    def handle_expt_event(self):
        """
         handle_expt_event() is called if there might be an error on the
         socket, or if there is OOB data
         check for the error condition first

        """
    def handle_error(self):
        """
         sometimes a user repr method will crash.

        """
    def handle_expt(self):
        """
        'unhandled incoming priority event'
        """
    def handle_read(self):
        """
        'unhandled read event'
        """
    def handle_write(self):
        """
        'unhandled write event'
        """
    def handle_connect(self):
        """
        'unhandled connect event'
        """
    def handle_accept(self):
        """
        'unhandled accepted event'
        """
    def handle_close(self):
        """
        'unhandled close event'
        """
def dispatcher_with_send(dispatcher):
    """
    b''
    """
    def initiate_send(self):
        """
        'sending %s'
        """
def compact_traceback():
    """
     Must have a traceback
    """
def close_all(map=None, ignore_all=False):
    """
     Asynchronous File I/O:

     After a little research (reading man pages on various unixen, and
     digging through the linux kernel), I've determined that select()
     isn't meant for doing asynchronous file i/o.
     Heartening, though - reading linux/mm/filemap.c shows that linux
     supports asynchronous read-ahead.  So _MOST_ of the time, the data
     will be sitting in memory for us already when we go to read it.

     What other OS's (besides NT) support async file i/o?  [VMS?]

     Regardless, this is useful for pipes, and stdin/stdout...


    """
    def file_wrapper:
    """
     Here we override just enough to make a file
     look like a socket for the purposes of asyncore.
     The passed fd is automatically os.dup()'d


    """
        def __init__(self, fd):
            """
            unclosed file %r
            """
        def recv(self, *args):
            """
            Only asyncore specific behaviour 
            implemented.
            """
        def close(self):
            """
             set it to non-blocking mode

            """
        def set_file(self, fd):
