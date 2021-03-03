def _OverlappedFuture(futures.Future):
    """
    Subclass of Future which represents an overlapped operation.

        Cancelling it will immediately cancel the overlapped operation.
    
    """
    def __init__(self, ov, *, loop=None):
        """
        'pending'
        """
    def _cancel_overlapped(self):
        """
        'message'
        """
    def cancel(self):
        """
        Subclass of Future which represents a wait handle.
        """
    def __init__(self, ov, handle, wait_handle, *, loop=None):
        """
         Keep a reference to the Overlapped object to keep it alive until the
         wait is unregistered

        """
    def _poll(self):
        """
         non-blocking wait: use a timeout of 0 millisecond

        """
    def _repr_info(self):
        """
        f'handle={self._handle:#x}'
        """
    def _unregister_wait_cb(self, fut):
        """
         The wait was unregistered: it's not safe to destroy the Overlapped
         object

        """
    def _unregister_wait(self):
        """
        'message'
        """
    def cancel(self):
        """
        Subclass of Future which represents a wait for the cancellation of a
            _WaitHandleFuture using an event.
    
        """
    def __init__(self, ov, event, wait_handle, *, loop=None):
        """
        _WaitCancelFuture must not be cancelled
        """
    def set_result(self, result):
        """
         If the wait was cancelled, the wait may never be signalled, so
         it's required to unregister it. Otherwise, IocpProactor.close() will
         wait forever for an event which will never come.

         If the IocpProactor already received the event, it's safe to call
         _unregister() because we kept a reference to the Overlapped object
         which is used as a unique key.

        """
    def _unregister_wait(self):
        """
        'message'
        """
def PipeServer(object):
    """
    Class representing a pipe server.

        This is much like a bound, listening socket.
    
    """
    def __init__(self, address):
        """
         initialize the pipe attribute before calling _server_pipe_handle()
         because this function can raise an exception and the destructor calls
         the close() method

        """
    def _get_unconnected_pipe(self):
        """
         Create new instance and return previous one.  This ensures
         that (until the server is closed) there is always at least
         one pipe handle for address.  Therefore if a client attempt
         to connect it will not fail with FileNotFoundError.

        """
    def _server_pipe_handle(self, first):
        """
         Return a wrapper for a new pipe handle.

        """
    def closed(self):
        """
         Close all instances which have not been connected to by a client.

        """
def _WindowsSelectorEventLoop(selector_events.BaseSelectorEventLoop):
    """
    Windows version of selector event loop.
    """
def ProactorEventLoop(proactor_events.BaseProactorEventLoop):
    """
    Windows version of proactor event loop using IOCP.
    """
    def __init__(self, proactor=None):
        """
         self_reading_future was just cancelled so it will never be signalled
         Unregister it otherwise IocpProactor.close will wait for it forever

        """
    async def create_pipe_connection(self, protocol_factory, address):
            """
            'addr'
            """
    async def start_serving_pipe(self, protocol_factory, address):
            """
             A client connected before the server was closed:
             drop the client (close the pipe) and exit

            """
2021-03-02 20:54:34,880 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:34,880 : INFO : tokenize_signature : --> do i ever get here?
    async def _make_subprocess_transport(self, protocol, args, shell,
                                         stdin, stdout, stderr, bufsize,
                                         extra=None, **kwargs):
            """
            Proactor implementation using IOCP.
            """
    def __init__(self, concurrency=0xffffffff):
        """
        'IocpProactor is closed'
        """
    def __repr__(self):
        """
        'overlapped#=%s'
        """
    def set_loop(self, loop):
        """
        b''
        """
        def finish_recv(trans, key, ov):
            """
            b''
            """
        def finish_recv(trans, key, ov):
            """
            b''
            """
        def finish_recv(trans, key, ov):
            """
             Use SO_UPDATE_ACCEPT_CONTEXT so getsockname() etc work.

            """
        async def accept_coro(future, conn):
                """
                 Coroutine closing the accept socket if the future is cancelled

                """
    def connect(self, conn, address):
        """
         WSAConnect will complete immediately for UDP sockets so we don't
         need to register any IOCP operation

        """
        def finish_connect(trans, key, ov):
            """
             Use SO_UPDATE_CONNECT_CONTEXT so getsockname() etc work.

            """
    def sendfile(self, sock, file, offset, count):
        """
         ConnectNamePipe() failed with ERROR_PIPE_CONNECTED which means
         that the pipe is connected. There is no need to wait for the
         completion of the connection.

        """
        def finish_accept_pipe(trans, key, ov):
            """
             Unfortunately there is no way to do an overlapped connect to
             a pipe.  Call CreateFile() in a loop until it doesn't fail with
             ERROR_PIPE_BUSY.

            """
    def wait_for_handle(self, handle, timeout=None):
        """
        Wait for a handle.

                Return a Future object. The result of the future is True if the wait
                completed, or False if the wait did not complete (on timeout).
        
        """
    def _wait_cancel(self, event, done_callback):
        """
         add_done_callback() cannot be used because the wait may only complete
         in IocpProactor.close(), while the event loop is not running.

        """
    def _wait_for_handle(self, handle, timeout, _is_cancel):
        """
         RegisterWaitForSingleObject() has a resolution of 1 millisecond,
         round away from zero to wait *at least* timeout seconds.

        """
        def finish_wait_for_handle(trans, key, ov):
            """
             Note that this second wait means that we should only use
             this with handles types where a successful wait has no
             effect.  So events or processes are all right, but locks
             or semaphores are not.  Also note if the handle is
             signalled and then quickly reset, then we may return
             False even though we have not timed out.

            """
    def _register_with_iocp(self, obj):
        """
         To get notifications of finished ops on this objects sent to the
         completion port, were must register the handle.

        """
    def _register(self, ov, obj, callback):
        """
         Return a future which will be set with the result of the
         operation when it completes.  The future's value is actually
         the value returned by callback().

        """
    def _unregister(self, ov):
        """
        Unregister an overlapped object.

                Call this method when its future has been cancelled. The event can
                already be signalled (pending in the proactor event queue). It is also
                safe if the event is never signalled (because it was cancelled).
        
        """
    def _get_accept_socket(self, family):
        """
        negative timeout
        """
    def _stop_serving(self, obj):
        """
         obj is a socket or pipe handle.  It will be closed in
         BaseProactorEventLoop._stop_serving() which will make any
         pending operations fail quickly.

        """
    def close(self):
        """
         already closed

        """
    def __del__(self):
