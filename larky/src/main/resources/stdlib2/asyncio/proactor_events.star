def _set_socket_extra(transport, sock):
    """
    'socket'
    """
2021-03-02 20:54:32,230 : INFO : tokenize_signature : --> do i ever get here?
def _ProactorBasePipeTransport(transports._FlowControlMixin,
                                 transports.BaseTransport):
    """
    Base class for pipe and socket transports.
    """
2021-03-02 20:54:32,231 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, loop, sock, protocol, waiter=None,
                 extra=None, server=None):
        """
         None or bytearray.
        """
    def __repr__(self):
        """
        'closed'
        """
    def _set_extra(self, sock):
        """
        'pipe'
        """
    def set_protocol(self, protocol):
        """
        f"unclosed transport {self!r}
        """
    def _fatal_error(self, exc, message='Fatal error on pipe transport'):
        """
        %r: %s
        """
    def _force_close(self, exc):
        """
         XXX If there is a pending overlapped read on the other
         end then it may fail with ERROR_NETNAME_DELETED if we
         just close our end.  First calling shutdown() seems to
         cure it, but maybe using DisconnectEx() would be better.

        """
    def get_write_buffer_size(self):
        """
        Transport for read pipes.
        """
2021-03-02 20:54:32,235 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, loop, sock, protocol, waiter=None,
                 extra=None, server=None):
        """
         bpo-33694: Don't cancel self._read_fut because cancelling an
         overlapped WSASend() loss silently data with the current proactor
         implementation.

         If CancelIoEx() fails with ERROR_NOT_FOUND, it means that WSASend()
         completed (even if HasOverlappedIoCompleted() returns 0), but
         Overlapped.cancel() currently silently ignores the ERROR_NOT_FOUND
         error. Once the overlapped is ignored, the IOCP loop will ignores the
         completion I/O event and so not read the result of the overlapped
         WSARecv().


        """
    def resume_reading(self):
        """
         Call the protocol methode after calling _loop_reading(),
         since the protocol can decide to pause reading again.

        """
    def _eof_received(self):
        """
        %r received EOF
        """
    def _data_received(self, data):
        """
         Don't call any protocol method while reading is paused.
         The protocol will be called on resume_reading().

        """
    def _loop_reading(self, fut=None):
        """
         deliver data later in "finally" clause

        """
2021-03-02 20:54:32,239 : INFO : tokenize_signature : --> do i ever get here?
def _ProactorBaseWritePipeTransport(_ProactorBasePipeTransport,
                                      transports.WriteTransport):
    """
    Transport for write pipes.
    """
    def __init__(self, *args, **kw):
        """
        f"data argument must be a bytes-like object, 
        f"not {type(data).__name__}
        """
    def _loop_writing(self, f=None, data=None):
        """
         XXX most likely self._force_close() has been called, and
         it has set self._write_fut to None.

        """
    def can_write_eof(self):
        """
        Empty waiter is already set
        """
    def _reset_empty_waiter(self):
        """
         the transport has been closed

        """
def _ProactorDatagramTransport(_ProactorBasePipeTransport):
    """
     We don't need to call _protocol.connection_made() since our base
     constructor does it for us.

    """
    def _set_extra(self, sock):
        """
        'data argument must be bytes-like object (%r)'
        """
    def _loop_writing(self, fut=None):
        """
         We are in a _loop_writing() done callback, get the result

        """
    def _loop_reading(self, fut=None):
        """
         since close() has been called we ignore any read data

        """
2021-03-02 20:54:32,246 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:32,246 : INFO : tokenize_signature : --> do i ever get here?
def _ProactorDuplexPipeTransport(_ProactorReadPipeTransport,
                                   _ProactorBaseWritePipeTransport,
                                   transports.Transport):
    """
    Transport for duplex pipes.
    """
    def can_write_eof(self):
        """
        Transport for connected sockets.
        """
2021-03-02 20:54:32,247 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, loop, sock, protocol, waiter=None,
                 extra=None, server=None):
        """
        'Using proactor: %s'
        """
2021-03-02 20:54:32,248 : INFO : tokenize_signature : --> do i ever get here?
    def _make_socket_transport(self, sock, protocol, waiter=None,
                               extra=None, server=None):
        """
         We want connection_lost() to be called when other end closes

        """
    def close(self):
        """
        Cannot close a running event loop
        """
    async def sock_recv(self, sock, n):
            """
            not a regular file
            """
    async def _sendfile_native(self, transp, file, offset, count):
            """
             A self-socket, really. :-)

            """
    def _loop_self_reading(self, f=None):
        """
         may raise
        """
    def _write_to_self(self):
        """
        b'\0'
        """
2021-03-02 20:54:32,252 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:32,252 : INFO : tokenize_signature : --> do i ever get here?
    def _start_serving(self, protocol_factory, sock,
                       sslcontext=None, server=None, backlog=100,
                       ssl_handshake_timeout=None):
        """
        %r got a new connection from %r: %r
        """
    def _process_events(self, event_list):
        """
         Events are processed in the IocpProactor._poll() method

        """
    def _stop_accept_futures(self):
