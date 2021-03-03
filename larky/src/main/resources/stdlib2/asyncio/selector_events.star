def _test_selector_event(selector, fd, event):
    """
     Test if the selector is monitoring 'event' events
     for the file descriptor 'fd'.

    """
def _check_ssl_socket(sock):
    """
    Socket cannot be of type SSLSocket
    """
def BaseSelectorEventLoop(base_events.BaseEventLoop):
    """
    Selector event loop.

        See events.EventLoop for API specification.
    
    """
    def __init__(self, selector=None):
        """
        'Using selector: %s'
        """
2021-03-02 20:54:30,927 : INFO : tokenize_signature : --> do i ever get here?
    def _make_socket_transport(self, sock, protocol, waiter=None, *,
                               extra=None, server=None):
        """
        Cannot close a running event loop
        """
    def _close_self_pipe(self):
        """
         A self-socket, really. :-)

        """
    def _process_self_data(self, data):
        """
         This may be called from a different thread, possibly after
         _close_self_pipe() has been called or even while it is
         running.  Guard for self._csock being None or closed.  When
         a socket is closed, send() raises OSError (with errno set to
         EBADF, but let's not rely on the exact error code).

        """
2021-03-02 20:54:30,930 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:30,930 : INFO : tokenize_signature : --> do i ever get here?
    def _start_serving(self, protocol_factory, sock,
                       sslcontext=None, server=None, backlog=100,
                       ssl_handshake_timeout=constants.SSL_HANDSHAKE_TIMEOUT):
        """
         This method is only called once for each event loop tick where the
         listening socket has triggered an EVENT_READ. There may be multiple
         connections waiting for an .accept() so it is called in a loop.
         See https://bugs.python.org/issue27906 for more details.

        """
2021-03-02 20:54:30,931 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:30,931 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:30,931 : INFO : tokenize_signature : --> do i ever get here?
    async def _accept_connection2(
            self, protocol_factory, conn, extra,
            sslcontext=None, server=None,
            ssl_handshake_timeout=constants.SSL_HANDSHAKE_TIMEOUT):
            """
             It's now up to the protocol to handle the connection.


            """
    def _ensure_fd_no_transport(self, fd):
        """
         This code matches selectors._fileobj_to_fd function.

        """
    def _add_reader(self, fd, callback, *args):
        """
        Remove a writer callback.
        """
    def add_reader(self, fd, callback, *args):
        """
        Add a reader callback.
        """
    def remove_reader(self, fd):
        """
        Remove a reader callback.
        """
    def add_writer(self, fd, callback, *args):
        """
        Add a writer callback..
        """
    def remove_writer(self, fd):
        """
        Remove a writer callback.
        """
    async def sock_recv(self, sock, n):
            """
            Receive data from the socket.

                    The return value is a bytes object representing the data received.
                    The maximum amount of data to be received at once is specified by
                    nbytes.
        
            """
    def _sock_read_done(self, fd, fut):
        """
         _sock_recv() can add itself as an I/O callback if the operation can't
         be done immediately. Don't use it directly, call sock_recv().

        """
    async def sock_recv_into(self, sock, buf):
            """
            Receive data from the socket.

                    The received data is written into *buf* (a writable buffer).
                    The return value is the number of bytes written.
        
            """
    def _sock_recv_into(self, fut, sock, buf):
        """
         _sock_recv_into() can add itself as an I/O callback if the operation
         can't be done immediately. Don't use it directly, call
         sock_recv_into().

        """
    async def sock_sendall(self, sock, data):
            """
            Send data to the socket.

                    The socket must be connected to a remote socket. This method continues
                    to send data from data until either all data has been sent or an
                    error occurs. None is returned on success. On error, an exception is
                    raised, and there is no way to determine how much data, if any, was
                    successfully processed by the receiving end of the connection.
        
            """
    def _sock_sendall(self, fut, sock, view, pos):
        """
         Future cancellation can be scheduled on previous loop iteration

        """
    async def sock_connect(self, sock, address):
            """
            Connect to a remote socket at address.

                    This method is a coroutine.
        
            """
    def _sock_connect(self, fut, sock, address):
        """
         Issue #23618: When the C function connect() fails with EINTR, the
         connection runs in background. We have to wait until the socket
         becomes writable to be notified when the connection succeed or
         fails.

        """
    def _sock_write_done(self, fd, fut):
        """
         Jump to any except clause below.

        """
    async def sock_accept(self, sock):
            """
            Accept a connection.

                    The socket must be bound to an address and listening for connections.
                    The return value is a pair (conn, address) where conn is a new socket
                    object usable to send and receive data on the connection, and address
                    is the address bound to the socket on the other end of the connection.
        
            """
    def _sock_accept(self, fut, registered, sock):
        """
         Buffer size passed to recv().
        """
    def __init__(self, loop, sock, protocol, extra=None, server=None):
        """
        'socket'
        """
    def __repr__(self):
        """
        'closed'
        """
    def abort(self):
        """
        f"unclosed transport {self!r}
        """
    def _fatal_error(self, exc, message='Fatal error on transport'):
        """
         Should be called from exception handler only.

        """
    def _force_close(self, exc):
        """
         Disable the Nagle algorithm -- small writes will be
         sent without waiting for the TCP ACK.  This generally
         decreases the latency (in some cases significantly.)

        """
    def set_protocol(self, protocol):
        """
        %r pauses reading
        """
    def resume_reading(self):
        """
        %r resumes reading
        """
    def _read_ready(self):
        """
        'get_buffer() returned an empty buffer'
        """
    def _read_ready__data_received(self):
        """
        'Fatal read error on socket transport'
        """
    def _read_ready__on_eof(self):
        """
        %r received EOF
        """
    def write(self, data):
        """
        f'data argument must be a bytes-like object, '
        f'not {type(data).__name__!r}'
        """
    def _write_ready(self):
        """
        'Data should not be empty'
        """
    def write_eof(self):
        """
        Connection is closed by peer
        """
    def _make_empty_waiter(self):
        """
        Empty waiter is already set
        """
    def _reset_empty_waiter(self):
        """
         only start reading when connection_made() has been called

        """
    def get_write_buffer_size(self):
        """
        'Fatal read error on datagram transport'
        """
    def sendto(self, data, addr=None):
        """
        f'data argument must be a bytes-like object, '
        f'not {type(data).__name__!r}'
        """
    def _sendto_ready(self):
        """
        'peername'
        """
