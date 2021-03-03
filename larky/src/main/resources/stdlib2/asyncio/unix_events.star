def _sighandler_noop(signum, frame):
    """
    Dummy signal handler.
    """
def _UnixSelectorEventLoop(selector_events.BaseSelectorEventLoop):
    """
    Unix event loop.

        Adds signal handling and UNIX Domain Socket support to SelectorEventLoop.
    
    """
    def __init__(self, selector=None):
        """
        f"Closing the loop {self!r} 
        f"on interpreter shutdown 
        f"stage, skipping signal handlers removal
        """
    def _process_self_data(self, data):
        """
         ignore null bytes written by _write_to_self()

        """
    def add_signal_handler(self, sig, callback, *args):
        """
        Add a handler for a signal.  UNIX only.

                Raise ValueError if the signal number is invalid or uncatchable.
                Raise RuntimeError if there is a problem setting up the handler.
        
        """
    def _handle_signal(self, sig):
        """
        Internal helper that is the actual signal handler.
        """
    def remove_signal_handler(self, sig):
        """
        Remove a handler for a signal.  UNIX only.

                Return True if a signal handler was removed, False if not.
        
        """
    def _check_signal(self, sig):
        """
        Internal helper to validate a signal.

                Raise ValueError if the signal number is invalid or uncatchable.
                Raise RuntimeError if there is a problem setting up the handler.
        
        """
2021-03-02 20:54:35,409 : INFO : tokenize_signature : --> do i ever get here?
    def _make_read_pipe_transport(self, pipe, protocol, waiter=None,
                                  extra=None):
        """
         Check early.
         Raising exception before process creation
         prevents subprocess execution if the watcher
         is not ready to handle it.

        """
    def _child_watcher_callback(self, pid, returncode, transp):
        """
        'you have to pass server_hostname when using ssl'
        """
2021-03-02 20:54:35,412 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:35,412 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:35,412 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:35,412 : INFO : tokenize_signature : --> do i ever get here?
    async def create_unix_server(
            self, protocol_factory, path=None, *,
            sock=None, backlog=100, ssl=None,
            ssl_handshake_timeout=None,
            start_serving=True):
            """
            'ssl argument must be an SSLContext or None'
            """
    async def _sock_sendfile_native(self, sock, file, offset, count):
            """
            os.sendfile() is not available
            """
2021-03-02 20:54:35,414 : INFO : tokenize_signature : --> do i ever get here?
    def _sock_sendfile_native_impl(self, fut, registered_fd, sock, fileno,
                                   offset, count, blocksize, total_sent):
        """
         Remove the callback early.  It should be rare that the
         selector says the fd is ready but the call still returns
         EAGAIN, and I am willing to take a hit in that case in
         order to simplify the common case.

        """
    def _sock_sendfile_update_filepos(self, fileno, offset, total_sent):
        """
         max bytes we read in one event loop iteration
        """
    def __init__(self, loop, pipe, protocol, waiter=None, extra=None):
        """
        'pipe'
        """
    def __repr__(self):
        """
        'closed'
        """
    def _read_ready(self):
        """
        'Fatal read error on pipe transport'
        """
    def pause_reading(self):
        """
        %r pauses reading
        """
    def resume_reading(self):
        """
        %r resumes reading
        """
    def set_protocol(self, protocol):
        """
        f"unclosed transport {self!r}
        """
    def _fatal_error(self, exc, message='Fatal error on pipe transport'):
        """
         should be called by exception handler only

        """
    def _close(self, exc):
        """
        'pipe'
        """
    def __repr__(self):
        """
        'closed'
        """
    def get_write_buffer_size(self):
        """
         Pipe was closed by peer.

        """
    def write(self, data):
        """
        'pipe closed by peer or '
        'os.write(pipe, data) raised exception.'
        """
    def _write_ready(self):
        """
        'Data should not be empty'
        """
    def can_write_eof(self):
        """
         write_eof is all what we needed to close the write pipe

        """
    def __del__(self, _warn=warnings.warn):
        """
        f"unclosed transport {self!r}
        """
    def abort(self):
        """
        'Fatal error on pipe transport'
        """
    def _close(self, exc=None):
        """
         Use a socket pair for stdin, since not all platforms
         support selecting read events on the write end of a
         socket (which we use in order to detect closing of the
         other end).  Notably this is needed on AIX, and works
         just fine on other platforms.

        """
def AbstractChildWatcher:
    """
    Abstract base class for monitoring child processes.

        Objects derived from this class monitor a collection of subprocesses and
        report their termination or interruption by a signal.

        New callbacks are registered with .add_child_handler(). Starting a new
        process must be done within a 'with' block to allow the watcher to suspend
        its activity until the new process if fully registered (this is needed to
        prevent a race condition in some implementations).

        Example:
            with watcher:
                proc = subprocess.Popen("sleep 1")
                watcher.add_child_handler(proc.pid, callback)

        Notes:
            Implementations of this class must be thread-safe.

            Since child watcher objects may catch the SIGCHLD signal and call
            waitpid(-1), there should be only one active object per process.
    
    """
    def add_child_handler(self, pid, callback, *args):
        """
        Register a new child handler.

                Arrange for callback(pid, returncode, *args) to be called when
                process 'pid' terminates. Specifying another callback for the same
                process replaces the previous handler.

                Note: callback() must be thread-safe.
        
        """
    def remove_child_handler(self, pid):
        """
        Removes the handler for process 'pid'.

                The function returns True if the handler was successfully removed,
                False if there was nothing to remove.
        """
    def attach_loop(self, loop):
        """
        Attach the watcher to an event loop.

                If the watcher was previously attached to an event loop, then it is
                first detached before attaching to the new loop.

                Note: loop may be None.
        
        """
    def close(self):
        """
        Close the watcher.

                This must be called to make sure that any underlying resource is freed.
        
        """
    def is_active(self):
        """
        Return ``True`` if the watcher is active and is used by the event loop.

                Return True if the watcher is installed and ready to handle process exit
                notifications.

        
        """
    def __enter__(self):
        """
        Enter the watcher's context and allow starting new processes

                This function must return self
        """
    def __exit__(self, a, b, c):
        """
        Exit the watcher's context
        """
def _compute_returncode(status):
    """
     The child process died because of a signal.

    """
def BaseChildWatcher(AbstractChildWatcher):
    """
    'A loop is being detached '
    'from a child watcher with pending handlers'
    """
    def _sig_chld(self):
        """
         self._loop should always be available here
         as '_sig_chld' is added as a signal handler
         in 'attach_loop'

        """
def SafeChildWatcher(BaseChildWatcher):
    """
    'Safe' child watcher implementation.

        This implementation avoids disrupting other code spawning processes by
        polling explicitly each process in the SIGCHLD handler instead of calling
        os.waitpid(-1).

        This is a safe solution but it has a significant overhead when handling a
        big number of children (O(n) each time SIGCHLD is raised)
    
    """
    def close(self):
        """
         Prevent a race condition in case the child is already terminated.

        """
    def remove_child_handler(self, pid):
        """
         The child process is already reaped
         (may happen if waitpid() is called elsewhere).

        """
def FastChildWatcher(BaseChildWatcher):
    """
    'Fast' child watcher implementation.

        This implementation reaps every terminated processes by calling
        os.waitpid(-1) directly, possibly breaking other code spawning processes
        and waiting for their termination.

        There is no noticeable overhead when handling a big number of children
        (O(1) each time a child terminates).
    
    """
    def __init__(self):
        """
        Caught subprocesses termination from unknown pids: %s
        """
    def add_child_handler(self, pid, callback, *args):
        """
        Must use the context manager
        """
    def remove_child_handler(self, pid):
        """
         Because of signal coalescing, we must keep calling waitpid() as
         long as we're able to reap a child.

        """
def MultiLoopChildWatcher(AbstractChildWatcher):
    """
    A watcher that doesn't require running loop in the main thread.

        This implementation registers a SIGCHLD signal handler on
        instantiation (which may conflict with other code that
        install own handler for this signal).

        The solution is safe but it has a significant overhead when
        handling a big number of processes (*O(n)* each time a
        SIGCHLD is received).
    
    """
    def __init__(self):
        """
        SIGCHLD handler was changed by outside code
        """
    def __enter__(self):
        """
         Prevent a race condition in case the child is already terminated.

        """
    def remove_child_handler(self, pid):
        """
         Don't save the loop but initialize itself if called first time
         The reason to do it here is that attach_loop() is called from
         unix policy only for the main thread.
         Main thread is required for subscription on SIGCHLD signal

        """
    def _do_waitpid_all(self):
        """
         The child process is already reaped
         (may happen if waitpid() is called elsewhere).

        """
    def _sig_chld(self, signum, frame):
        """
        'Unknown exception in SIGCHLD handler'
        """
def ThreadedChildWatcher(AbstractChildWatcher):
    """
    Threaded child watcher implementation.

        The watcher uses a thread per process
        for waiting for the process finish.

        It doesn't require subscription on POSIX signal
        but a thread creation is not free.

        The watcher has O(1) complexity, its performance doesn't depend
        on amount of spawn processes.
    
    """
    def __init__(self):
        """
        Internal: Join all non-daemon threads
        """
    def __enter__(self):
        """
        f"{self.__class__} has registered but not finished child processes
        """
    def add_child_handler(self, pid, callback, *args):
        """
        f"waitpid-{next(self._pid_counter)}
        """
    def remove_child_handler(self, pid):
        """
         asyncio never calls remove_child_handler() !!!
         The method is no-op but is implemented because
         abstract base classe requires it

        """
    def attach_loop(self, loop):
        """
         The child process is already reaped
         (may happen if waitpid() is called elsewhere).

        """
def _UnixDefaultEventLoopPolicy(events.BaseDefaultEventLoopPolicy):
    """
    UNIX event loop policy with a watcher for child processes.
    """
    def __init__(self):
        """
         pragma: no branch
        """
    def set_event_loop(self, loop):
        """
        Set the event loop.

                As a side effect, if a child watcher was set before, then calling
                .set_event_loop() from the main thread will call .attach_loop(loop) on
                the child watcher.
        
        """
    def get_child_watcher(self):
        """
        Get the watcher for child processes.

                If not yet set, a ThreadedChildWatcher object is automatically created.
        
        """
    def set_child_watcher(self, watcher):
        """
        Set the watcher for child processes.
        """
