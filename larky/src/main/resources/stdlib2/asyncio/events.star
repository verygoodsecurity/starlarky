def Handle:
    """
    Object returned by callback registration methods.
    """
    def __init__(self, callback, args, loop, context=None):
        """
        'cancelled'
        """
    def __repr__(self):
        """
        '<{}>'
        """
    def cancel(self):
        """
         Keep a representation in debug mode to keep callback and
         parameters. For example, to log the warning
         "Executing <Handle...> took 2.5 second

        """
    def cancelled(self):
        """
        f'Exception in callback {cb}'
        """
def TimerHandle(Handle):
    """
    Object returned by timed callback registration methods.
    """
    def __init__(self, when, callback, args, loop, context=None):
        """
        f'when={self._when}'
        """
    def __hash__(self):
        """
        Return a scheduled callback time.

                The time is an absolute timestamp, using the same time
                reference as loop.time().
        
        """
def AbstractServer:
    """
    Abstract server returned by create_server().
    """
    def close(self):
        """
        Stop serving.  This leaves existing connections open.
        """
    def get_loop(self):
        """
        Get the event loop the Server object is attached to.
        """
    def is_serving(self):
        """
        Return True if the server is accepting connections.
        """
    async def start_serving(self):
            """
            Start accepting connections.

                    This method is idempotent, so it can be called when
                    the server is already being serving.
        
            """
    async def serve_forever(self):
            """
            Start accepting connections until the coroutine is cancelled.

                    The server is closed when the coroutine is cancelled.
        
            """
    async def wait_closed(self):
            """
            Coroutine to wait until service is closed.
            """
    async def __aenter__(self):
            """
            Abstract event loop.
            """
    def run_forever(self):
        """
        Run the event loop until stop() is called.
        """
    def run_until_complete(self, future):
        """
        Run the event loop until a Future is done.

                Return the Future's result, or raise its exception.
        
        """
    def stop(self):
        """
        Stop the event loop as soon as reasonable.

                Exactly how soon that is may depend on the implementation, but
                no more I/O callbacks should be scheduled.
        
        """
    def is_running(self):
        """
        Return whether the event loop is currently running.
        """
    def is_closed(self):
        """
        Returns True if the event loop was closed.
        """
    def close(self):
        """
        Close the loop.

                The loop should not be running.

                This is idempotent and irreversible.

                No other methods should be called after this one.
        
        """
    async def shutdown_asyncgens(self):
            """
            Shutdown all active asynchronous generators.
            """
    def _timer_handle_cancelled(self, handle):
        """
        Notification that a TimerHandle has been cancelled.
        """
    def call_soon(self, callback, *args):
        """
         Method scheduling a coroutine object: create a task.


        """
    def create_task(self, coro, *, name=None):
        """
         Methods for interacting with threads.


        """
    def call_soon_threadsafe(self, callback, *args):
        """
         Network I/O methods returning Futures.


        """
2021-03-02 20:54:31,494 : INFO : tokenize_signature : --> do i ever get here?
    async def getaddrinfo(self, host, port, *,
                          family=0, type=0, proto=0, flags=0):
            """
            A coroutine which creates a TCP server bound to host and port.

                    The return value is a Server object which can be used to stop
                    the service.

                    If host is an empty string or None all interfaces are assumed
                    and a list of multiple sockets will be returned (most likely
                    one for IPv4 and another one for IPv6). The host parameter can also be
                    a sequence (e.g. list) of hosts to bind to.

                    family can be set to either AF_INET or AF_INET6 to force the
                    socket to use IPv4 or IPv6. If not set it will be determined
                    from host (defaults to AF_UNSPEC).

                    flags is a bitmask for getaddrinfo().

                    sock can optionally be specified in order to use a preexisting
                    socket object.

                    backlog is the maximum number of queued connections passed to
                    listen() (defaults to 100).

                    ssl can be set to an SSLContext to enable SSL over the
                    accepted connections.

                    reuse_address tells the kernel to reuse a local socket in
                    TIME_WAIT state, without waiting for its natural timeout to
                    expire. If not specified will automatically be set to True on
                    UNIX.

                    reuse_port tells the kernel to allow this endpoint to be bound to
                    the same port as other existing endpoints are bound to, so long as
                    they all set this flag when being created. This option is not
                    supported on Windows.

                    ssl_handshake_timeout is the time in seconds that an SSL server
                    will wait for completion of the SSL handshake before aborting the
                    connection. Default is 60s.

                    start_serving set to True (default) causes the created server
                    to start accepting connections immediately.  When set to False,
                    the user should await Server.start_serving() or Server.serve_forever()
                    to make the server to start accepting connections.
        
            """
2021-03-02 20:54:31,495 : INFO : tokenize_signature : --> do i ever get here?
    async def sendfile(self, transport, file, offset=0, count=None,
                       *, fallback=True):
            """
            Send a file through a transport.

                    Return an amount of sent bytes.
        
            """
2021-03-02 20:54:31,496 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,496 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,496 : INFO : tokenize_signature : --> do i ever get here?
    async def start_tls(self, transport, protocol, sslcontext, *,
                        server_side=False,
                        server_hostname=None,
                        ssl_handshake_timeout=None):
            """
            Upgrade a transport to TLS.

                    Return a new transport that *protocol* should start using
                    immediately.
        
            """
2021-03-02 20:54:31,496 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,496 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,497 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,497 : INFO : tokenize_signature : --> do i ever get here?
    async def create_unix_connection(
            self, protocol_factory, path=None, *,
            ssl=None, sock=None,
            server_hostname=None,
            ssl_handshake_timeout=None):
            """
            A coroutine which creates a UNIX Domain Socket server.

                    The return value is a Server object, which can be used to stop
                    the service.

                    path is a str, representing a file systsem path to bind the
                    server socket to.

                    sock can optionally be specified in order to use a preexisting
                    socket object.

                    backlog is the maximum number of queued connections passed to
                    listen() (defaults to 100).

                    ssl can be set to an SSLContext to enable SSL over the
                    accepted connections.

                    ssl_handshake_timeout is the time in seconds that an SSL server
                    will wait for the SSL handshake to complete (defaults to 60s).

                    start_serving set to True (default) causes the created server
                    to start accepting connections immediately.  When set to False,
                    the user should await Server.start_serving() or Server.serve_forever()
                    to make the server to start accepting connections.
        
            """
2021-03-02 20:54:31,497 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,498 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,498 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,498 : INFO : tokenize_signature : --> do i ever get here?
    async def create_datagram_endpoint(self, protocol_factory,
                                       local_addr=None, remote_addr=None, *,
                                       family=0, proto=0, flags=0,
                                       reuse_address=None, reuse_port=None,
                                       allow_broadcast=None, sock=None):
            """
            A coroutine which creates a datagram endpoint.

                    This method will try to establish the endpoint in the background.
                    When successful, the coroutine returns a (transport, protocol) pair.

                    protocol_factory must be a callable returning a protocol instance.

                    socket family AF_INET, socket.AF_INET6 or socket.AF_UNIX depending on
                    host (or family if specified), socket type SOCK_DGRAM.

                    reuse_address tells the kernel to reuse a local socket in
                    TIME_WAIT state, without waiting for its natural timeout to
                    expire. If not specified it will automatically be set to True on
                    UNIX.

                    reuse_port tells the kernel to allow this endpoint to be bound to
                    the same port as other existing endpoints are bound to, so long as
                    they all set this flag when being created. This option is not
                    supported on Windows and some UNIX's. If the
                    :py:data:`~socket.SO_REUSEPORT` constant is not defined then this
                    capability is unsupported.

                    allow_broadcast tells the kernel to allow this endpoint to send
                    messages to the broadcast address.

                    sock can optionally be specified in order to use a preexisting
                    socket object.
        
            """
    async def connect_read_pipe(self, protocol_factory, pipe):
            """
            Register read pipe in event loop. Set the pipe to non-blocking mode.

                    protocol_factory should instantiate object with Protocol interface.
                    pipe is a file-like object.
                    Return pair (transport, protocol), where transport supports the
                    ReadTransport interface.
            """
    async def connect_write_pipe(self, protocol_factory, pipe):
            """
            Register write pipe in event loop.

                    protocol_factory should instantiate object with BaseProtocol interface.
                    Pipe is file-like object already switched to nonblocking.
                    Return pair (transport, protocol), where transport support
                    WriteTransport interface.
            """
2021-03-02 20:54:31,498 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,498 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,498 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:31,498 : INFO : tokenize_signature : --> do i ever get here?
    async def subprocess_shell(self, protocol_factory, cmd, *,
                               stdin=subprocess.PIPE,
                               stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE,
                               **kwargs):
            """
             Ready-based callback registration methods.
             The add_*() methods return None.
             The remove_*() methods return True if something was removed,
             False if there was nothing to delete.


            """
    def add_reader(self, fd, callback, *args):
        """
         Completion based I/O methods returning Futures.


        """
    async def sock_recv(self, sock, nbytes):
            """
             Signal handling.


            """
    def add_signal_handler(self, sig, callback, *args):
        """
         Task factory.


        """
    def set_task_factory(self, factory):
        """
         Error handlers.


        """
    def get_exception_handler(self):
        """
         Debug flag management.


        """
    def get_debug(self):
        """
        Abstract policy for accessing the event loop.
        """
    def get_event_loop(self):
        """
        Get the event loop for the current context.

                Returns an event loop object implementing the BaseEventLoop interface,
                or raises an exception in case no event loop has been set for the
                current context and the current policy does not specify to create one.

                It should never return None.
        """
    def set_event_loop(self, loop):
        """
        Set the event loop for the current context to loop.
        """
    def new_event_loop(self):
        """
        Create and return a new event loop object according to this
                policy's rules. If there's need to set this loop as the event loop for
                the current context, set_event_loop must be called explicitly.
        """
    def get_child_watcher(self):
        """
        Get the watcher for child processes.
        """
    def set_child_watcher(self, watcher):
        """
        Set the watcher for child processes.
        """
def BaseDefaultEventLoopPolicy(AbstractEventLoopPolicy):
    """
    Default policy implementation for accessing the event loop.

        In this policy, each thread has its own event loop.  However, we
        only automatically create an event loop by default for the main
        thread; other threads by default have no event loop.

        Other policies may have different rules (e.g. a single global
        event loop, or automatically creating an event loop per thread, or
        using some other notion of context to which an event loop is
        associated).
    
    """
    def _Local(threading.local):
    """
    Get the event loop for the current context.

            Returns an instance of EventLoop or raises an exception.
        
    """
    def set_event_loop(self, loop):
        """
        Set the event loop.
        """
    def new_event_loop(self):
        """
        Create a new event loop.

                You must call set_event_loop() to make this the current event
                loop.
        
        """
def _RunningLoop(threading.local):
    """
    Return the running event loop.  Raise a RuntimeError if there is none.

        This function is thread-specific.
    
    """
def _get_running_loop():
    """
    Return the running event loop or None.

        This is a low-level function intended to be used by event loops.
        This function is thread-specific.
    
    """
def _set_running_loop(loop):
    """
    Set the running event loop.

        This is a low-level function intended to be used by event loops.
        This function is thread-specific.
    
    """
def _init_event_loop_policy():
    """
     pragma: no branch
    """
def get_event_loop_policy():
    """
    Get the current event loop policy.
    """
def set_event_loop_policy(policy):
    """
    Set the current event loop policy.

        If policy is None, the default policy is restored.
    """
def get_event_loop():
    """
    Return an asyncio event loop.

        When called from a coroutine or a callback (e.g. scheduled with call_soon
        or similar API), this function will always return the running event loop.

        If there is no running event loop set, the function will return
        the result of `get_event_loop_policy().get_event_loop()` call.
    
    """
def set_event_loop(loop):
    """
    Equivalent to calling get_event_loop_policy().set_event_loop(loop).
    """
def new_event_loop():
    """
    Equivalent to calling get_event_loop_policy().new_event_loop().
    """
def get_child_watcher():
    """
    Equivalent to calling get_event_loop_policy().get_child_watcher().
    """
def set_child_watcher(watcher):
    """
    Equivalent to calling
        get_event_loop_policy().set_child_watcher(watcher).
    """
