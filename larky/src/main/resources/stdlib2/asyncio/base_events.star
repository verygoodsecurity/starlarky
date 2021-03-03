def _format_handle(handle):
    """
    '__self__'
    """
def _format_pipe(fd):
    """
    '<pipe>'
    """
def _set_reuseport(sock):
    """
    'SO_REUSEPORT'
    """
def _ipaddr_info(host, port, family, type, proto, flowinfo=0, scopeid=0):
    """
     Try to skip getaddrinfo if "host" is already an IP. Users might have
     handled name resolution in their own code and pass in resolved IPs.

    """
def _interleave_addrinfos(addrinfos, first_address_family_count=1):
    """
    Interleave list of addrinfo tuples by family.
    """
def _run_until_complete_cb(fut):
    """
     Issue #22429: run_forever() already finished, no need to
     stop it.

    """
    def _set_nodelay(sock):
        """
        transport should be _FlowControlMixin instance
        """
    async def drain(self):
            """
            Connection closed by peer
            """
    def connection_made(self, transport):
        """
        Invalid state: 
        connection should have been established already.
        """
    def connection_lost(self, exc):
        """
         Never happens if peer disconnects after sending the whole content
         Thus disconnection is always an exception from user perspective

        """
    def pause_writing(self):
        """
        Invalid state: reading should be paused
        """
    def eof_received(self):
        """
        Invalid state: reading should be paused
        """
    async def restore(self):
            """
             Cancel the future.
             Basically it has no effect because protocol is switched back,
             no code should wait for it anymore.

            """
def Server(events.AbstractServer):
    """
    f'<{self.__class__.__name__} sockets={self.sockets!r}>'
    """
    def _attach(self):
        """
         Skip one loop iteration so that all 'loop.add_reader'
         go through.

        """
    async def serve_forever(self):
            """
            f'server {self!r} is already being awaited on serve_forever()'
            """
    async def wait_closed(self):
            """
             Identifier of the thread running the event loop, or None if the
             event loop is not running

            """
    def __repr__(self):
        """
        f'<{self.__class__.__name__} running={self.is_running()} '
        f'closed={self.is_closed()} debug={self.get_debug()}>'

        """
    def create_future(self):
        """
        Create a Future object attached to the loop.
        """
    def create_task(self, coro, *, name=None):
        """
        Schedule a coroutine object.

                Return a task object.
        
        """
    def set_task_factory(self, factory):
        """
        Set a task factory that will be used by loop.create_task().

                If factory is None the default task factory will be set.

                If factory is a callable, it should have a signature matching
                '(loop, coro)', where 'loop' will be a reference to the active
                event loop, 'coro' will be a coroutine object.  The callable
                must return a Future.
        
        """
    def get_task_factory(self):
        """
        Return a task factory, or None if the default one is in use.
        """
2021-03-02 20:54:31,688 : INFO : tokenize_signature : --> do i ever get here?
    def _make_socket_transport(self, sock, protocol, waiter=None, *,
                               extra=None, server=None):
        """
        Create socket transport.
        """
2021-03-02 20:54:31,689 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,689 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,689 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,689 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,689 : INFO : tokenize_signature : --> do i ever get here?
    def _make_ssl_transport(
            self, rawsock, protocol, sslcontext, waiter=None,
            *, server_side=False, server_hostname=None,
            extra=None, server=None,
            ssl_handshake_timeout=None,
            call_connection_made=True):
        """
        Create SSL transport.
        """
2021-03-02 20:54:31,689 : INFO : tokenize_signature : --> do i ever get here?
    def _make_datagram_transport(self, sock, protocol,
                                 address=None, waiter=None, extra=None):
        """
        Create datagram transport.
        """
2021-03-02 20:54:31,689 : INFO : tokenize_signature : --> do i ever get here?
    def _make_read_pipe_transport(self, pipe, protocol, waiter=None,
                                  extra=None):
        """
        Create read pipe transport.
        """
2021-03-02 20:54:31,689 : INFO : tokenize_signature : --> do i ever get here?
    def _make_write_pipe_transport(self, pipe, protocol, waiter=None,
                                   extra=None):
        """
        Create write pipe transport.
        """
2021-03-02 20:54:31,690 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,690 : INFO : tokenize_signature : --> do i ever get here?
    async def _make_subprocess_transport(self, protocol, args, shell,
                                         stdin, stdout, stderr, bufsize,
                                         extra=None, **kwargs):
            """
            Create subprocess transport.
            """
    def _write_to_self(self):
        """
        Write a byte to self-pipe, to wake up the event loop.

                This may be called from a different thread.

                The subclass is responsible for implementing the self-pipe.
        
        """
    def _process_events(self, event_list):
        """
        Process selector events.
        """
    def _check_closed(self):
        """
        'Event loop is closed'
        """
    def _asyncgen_finalizer_hook(self, agen):
        """
        f"asynchronous generator {agen!r} was scheduled after 
        f"loop.shutdown_asyncgens() call
        """
    async def shutdown_asyncgens(self):
            """
            Shutdown all active asynchronous generators.
            """
    def _check_running(self):
        """
        'This event loop is already running'
        """
    def run_forever(self):
        """
        Run until stop() is called.
        """
    def run_until_complete(self, future):
        """
        Run until the Future is done.

                If the argument is a coroutine, it is wrapped in a Task.

                WARNING: It would be disastrous to call run_until_complete()
                with the same coroutine twice -- it would wrap it in two
                different Tasks and that can't be good.

                Return the Future's result, or raise its exception.
        
        """
    def stop(self):
        """
        Stop running the event loop.

                Every callback already scheduled will still run.  This simply informs
                run_forever to stop looping after a complete iteration.
        
        """
    def close(self):
        """
        Close the event loop.

                This clears the queues and shuts down the executor,
                but does not wait for the executor to finish.

                The event loop must not be running.
        
        """
    def is_closed(self):
        """
        Returns True if the event loop was closed.
        """
    def __del__(self, _warn=warnings.warn):
        """
        f"unclosed event loop {self!r}
        """
    def is_running(self):
        """
        Returns True if the event loop is running.
        """
    def time(self):
        """
        Return the time according to the event loop's clock.

                This is a float expressed in seconds since an epoch, but the
                epoch, precision, accuracy and drift are unspecified and may
                differ per event loop.
        
        """
    def call_later(self, delay, callback, *args, context=None):
        """
        Arrange for a callback to be called at a given time.

                Return a Handle: an opaque object with a cancel() method that
                can be used to cancel the call.

                The delay can be an int or float, expressed in seconds.  It is
                always relative to the current time.

                Each callback will be called exactly once.  If two callbacks
                are scheduled for exactly the same time, it undefined which
                will be called first.

                Any positional arguments after the callback will be passed to
                the callback when it is called.
        
        """
    def call_at(self, when, callback, *args, context=None):
        """
        Like call_later(), but uses an absolute time.

                Absolute time corresponds to the event loop's time() method.
        
        """
    def call_soon(self, callback, *args, context=None):
        """
        Arrange for a callback to be called as soon as possible.

                This operates as a FIFO queue: callbacks are called in the
                order in which they are registered.  Each callback will be
                called exactly once.

                Any positional arguments after the callback will be passed to
                the callback when it is called.
        
        """
    def _check_callback(self, callback, method):
        """
        f"coroutines cannot be used with {method}()
        """
    def _call_soon(self, callback, args, context):
        """
        Check that the current thread is the thread running the event loop.

                Non-thread-safe methods of this class make this assumption and will
                likely behave incorrectly when the assumption is violated.

                Should only be called when (self._debug == True).  The caller is
                responsible for checking this condition for performance reasons.
        
        """
    def call_soon_threadsafe(self, callback, *args, context=None):
        """
        Like call_soon(), but thread-safe.
        """
    def run_in_executor(self, executor, func, *args):
        """
        'run_in_executor'
        """
    def set_default_executor(self, executor):
        """
        'Using the default executor that is not an instance of '
        'ThreadPoolExecutor is deprecated and will be prohibited '
        'in Python 3.9'
        """
    def _getaddrinfo_debug(self, host, port, family, type, proto, flags):
        """
        f"{host}:{port!r}
        """
2021-03-02 20:54:31,697 : INFO : tokenize_signature : --> do i ever get here?
    async def getaddrinfo(self, host, port, *,
                          family=0, type=0, proto=0, flags=0):
            """
            the socket must be non-blocking
            """
    async def _sock_sendfile_native(self, sock, file, offset, count):
            """
             NB: sendfile syscall is not supported for SSL sockets and
             non-mmap files even if sendfile is supported by OS

            """
    async def _sock_sendfile_fallback(self, sock, file, offset, count):
            """
             EOF
            """
    def _check_sendfile_params(self, sock, file, offset, count):
        """
        'b'
        """
    async def _connect_sock(self, exceptions, addr_info, local_addr_infos=None):
            """
            Create, bind and connect one socket.
            """
2021-03-02 20:54:31,700 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,701 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,701 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,701 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,701 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,701 : INFO : tokenize_signature : --> do i ever get here?
    async def create_connection(
            self, protocol_factory, host=None, port=None,
            *, ssl=None, family=0,
            proto=0, flags=0, sock=None,
            local_addr=None, server_hostname=None,
            ssl_handshake_timeout=None,
            happy_eyeballs_delay=None, interleave=None):
            """
            Connect to a TCP server.

                    Create a streaming transport connection to a given Internet host and
                    port: socket family AF_INET or socket.AF_INET6 depending on host (or
                    family if specified), socket type SOCK_STREAM. protocol_factory must be
                    a callable returning a protocol instance.

                    This method is a coroutine which will try to establish the connection
                    in the background.  When successful, the coroutine returns a
                    (transport, protocol) pair.
        
            """
2021-03-02 20:54:31,703 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,703 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,704 : INFO : tokenize_signature : --> do i ever get here?
    async def _create_connection_transport(
            self, sock, protocol_factory, ssl,
            server_hostname, server_side=False,
            ssl_handshake_timeout=None):
            """
            Send a file to transport.

                    Return the total number of bytes which were sent.

                    The method uses high-performance os.sendfile if available.

                    file must be a regular file object opened in binary mode.

                    offset tells from where to start reading the file. If specified,
                    count is the total number of bytes to transmit as opposed to
                    sending the file until EOF is reached. File position is updated on
                    return or also in case of error in which case file.tell()
                    can be used to figure out the number of bytes
                    which were sent.

                    fallback set to True makes asyncio to manually read and send
                    the file when the platform does not support the sendfile syscall
                    (e.g. Windows or SSL socket on Unix).

                    Raise SendfileNotAvailableError if the system does not support
                    sendfile syscall and fallback is False.
        
            """
    async def _sendfile_native(self, transp, file, offset, count):
            """
            sendfile syscall is not supported
            """
    async def _sendfile_fallback(self, transp, file, offset, count):
            """
             EOF
            """
2021-03-02 20:54:31,706 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,706 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,706 : INFO : tokenize_signature : --> do i ever get here?
    async def start_tls(self, transport, protocol, sslcontext, *,
                        server_side=False,
                        server_hostname=None,
                        ssl_handshake_timeout=None):
            """
            Upgrade transport to TLS.

                    Return a new transport that *protocol* should start using
                    immediately.
        
            """
2021-03-02 20:54:31,707 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,707 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,707 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,707 : INFO : tokenize_signature : --> do i ever get here?
    async def create_datagram_endpoint(self, protocol_factory,
                                       local_addr=None, remote_addr=None, *,
                                       family=0, proto=0, flags=0,
                                       reuse_address=_unset, reuse_port=None,
                                       allow_broadcast=None, sock=None):
            """
            Create datagram connection.
            """
2021-03-02 20:54:31,711 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,711 : INFO : tokenize_signature : --> do i ever get here?
    async def _ensure_resolved(self, address, *,
                               family=0, type=socket.SOCK_STREAM,
                               proto=0, flags=0, loop):
            """
             "host" is already a resolved IP.

            """
    async def _create_server_getaddrinfo(self, host, port, family, flags):
            """
            f'getaddrinfo({host!r}) returned empty list'
            """
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,712 : INFO : tokenize_signature : --> do i ever get here?
    async def create_server(
            self, protocol_factory, host=None, port=None,
            *,
            family=socket.AF_UNSPEC,
            flags=socket.AI_PASSIVE,
            sock=None,
            backlog=100,
            ssl=None,
            reuse_address=None,
            reuse_port=None,
            ssl_handshake_timeout=None,
            start_serving=True):
            """
            Create a TCP server.

                    The host parameter can be a string, in that case the TCP server is
                    bound to host and port.

                    The host parameter can also be a sequence of strings and in that case
                    the TCP server is bound to all hosts of the sequence. If a host
                    appears multiple times (possibly indirectly e.g. when hostnames
                    resolve to the same IP address), the server is only bound once to that
                    host.

                    Return a Server object which can be used to stop the service.

                    This method is a coroutine.
        
            """
2021-03-02 20:54:31,714 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,715 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,715 : INFO : tokenize_signature : --> do i ever get here?
    async def connect_accepted_socket(
            self, protocol_factory, sock,
            *, ssl=None,
            ssl_handshake_timeout=None):
            """
            Handle an accepted connection.

                    This is used by servers that accept connections outside of
                    asyncio but that use asyncio to handle connections.

                    This method is a coroutine.  When completed, the coroutine
                    returns a (transport, protocol) pair.
        
            """
    async def connect_read_pipe(self, protocol_factory, pipe):
            """
            'Read pipe %r connected: (%r, %r)'
            """
    async def connect_write_pipe(self, protocol_factory, pipe):
            """
            'Write pipe %r connected: (%r, %r)'
            """
    def _log_subprocess(self, msg, stdin, stdout, stderr):
        """
        f'stdin={_format_pipe(stdin)}'
        """
2021-03-02 20:54:31,716 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,716 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,716 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,717 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,717 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,717 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,717 : INFO : tokenize_signature : --> do i ever get here?
    async def subprocess_shell(self, protocol_factory, cmd, *,
                               stdin=subprocess.PIPE,
                               stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE,
                               universal_newlines=False,
                               shell=True, bufsize=0,
                               encoding=None, errors=None, text=None,
                               **kwargs):
            """
            cmd must be a string
            """
2021-03-02 20:54:31,718 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,718 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,718 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,718 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,718 : INFO : tokenize_signature : --> do i ever get here?
    async def subprocess_exec(self, protocol_factory, program, *args,
                              stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, universal_newlines=False,
                              shell=False, bufsize=0,
                              encoding=None, errors=None, text=None,
                              **kwargs):
            """
            universal_newlines must be False
            """
    def get_exception_handler(self):
        """
        Return an exception handler, or None if the default one is in use.
        
        """
    def set_exception_handler(self, handler):
        """
        Set handler as the new event loop exception handler.

                If handler is None, the default exception handler will
                be set.

                If handler is a callable object, it should have a
                signature matching '(loop, context)', where 'loop'
                will be a reference to the active event loop, 'context'
                will be a dict object (see `call_exception_handler()`
                documentation for details about context).
        
        """
    def default_exception_handler(self, context):
        """
        Default exception handler.

                This is called when an exception occurs and no exception
                handler is set, and can be called by a custom exception
                handler that wants to defer to the default behavior.

                This default handler logs the error message and other
                context-dependent information.  In debug mode, a truncated
                stack trace is also appended showing where the given object
                (e.g. a handle or future or task) was created, if any.

                The context parameter has the same meaning as in
                `call_exception_handler()`.
        
        """
    def call_exception_handler(self, context):
        """
        Call the current event loop's exception handler.

                The context argument is a dict containing the following keys:

                - 'message': Error message;
                - 'exception' (optional): Exception object;
                - 'future' (optional): Future instance;
                - 'task' (optional): Task instance;
                - 'handle' (optional): Handle instance;
                - 'protocol' (optional): Protocol instance;
                - 'transport' (optional): Transport instance;
                - 'socket' (optional): Socket instance;
                - 'asyncgen' (optional): Asynchronous generator that caused
                                         the exception.

                New keys maybe introduced in the future.

                Note: do not overload this method in an event loop subclass.
                For custom exception handling, use the
                `set_exception_handler()` method.
        
        """
    def _add_callback(self, handle):
        """
        Add a Handle to _scheduled (TimerHandle) or _ready.
        """
    def _add_callback_signalsafe(self, handle):
        """
        Like _add_callback() but called from a signal handler.
        """
    def _timer_handle_cancelled(self, handle):
        """
        Notification that a TimerHandle has been cancelled.
        """
    def _run_once(self):
        """
        Run one full iteration of the event loop.

                This calls all currently ready callbacks, polls for I/O,
                schedules the resulting callbacks, and finally schedules
                'call_later' callbacks.
        
        """
    def _set_coroutine_origin_tracking(self, enabled):
