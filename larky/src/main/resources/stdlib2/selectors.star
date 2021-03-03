def _fileobj_to_fd(fileobj):
    """
    Return a file descriptor from a file object.

        Parameters:
        fileobj -- file object or file descriptor

        Returns:
        corresponding file descriptor

        Raises:
        ValueError if the object is invalid
    
    """
def _SelectorMapping(Mapping):
    """
    Mapping of file objects to selector keys.
    """
    def __init__(self, selector):
        """
        {!r} is not registered
        """
    def __iter__(self):
        """
        Selector abstract base class.

            A selector supports registering file objects to be monitored for specific
            I/O events.

            A file object is a file descriptor or any object with a `fileno()` method.
            An arbitrary object can be attached to the file object, which can be used
            for example to store context information, a callback, etc.

            A selector can use various implementations (select(), poll(), epoll()...)
            depending on the platform. The default `Selector` class uses the most
            efficient implementation on the current platform.
    
        """
    def register(self, fileobj, events, data=None):
        """
        Register a file object.

                Parameters:
                fileobj -- file object or file descriptor
                events  -- events to monitor (bitwise mask of EVENT_READ|EVENT_WRITE)
                data    -- attached data

                Returns:
                SelectorKey instance

                Raises:
                ValueError if events is invalid
                KeyError if fileobj is already registered
                OSError if fileobj is closed or otherwise is unacceptable to
                        the underlying system call (if a system call is made)

                Note:
                OSError may or may not be raised
        
        """
    def unregister(self, fileobj):
        """
        Unregister a file object.

                Parameters:
                fileobj -- file object or file descriptor

                Returns:
                SelectorKey instance

                Raises:
                KeyError if fileobj is not registered

                Note:
                If fileobj is registered but has since been closed this does
                *not* raise OSError (even if the wrapped syscall does)
        
        """
    def modify(self, fileobj, events, data=None):
        """
        Change a registered file object monitored events or attached data.

                Parameters:
                fileobj -- file object or file descriptor
                events  -- events to monitor (bitwise mask of EVENT_READ|EVENT_WRITE)
                data    -- attached data

                Returns:
                SelectorKey instance

                Raises:
                Anything that unregister() or register() raises
        
        """
    def select(self, timeout=None):
        """
        Perform the actual selection, until some monitored file objects are
                ready or a timeout expires.

                Parameters:
                timeout -- if timeout > 0, this specifies the maximum wait time, in
                           seconds
                           if timeout <= 0, the select() call won't block, and will
                           report the currently ready file objects
                           if timeout is None, select() will block until a monitored
                           file object becomes ready

                Returns:
                list of (key, events) for ready file objects
                `events` is a bitwise mask of EVENT_READ|EVENT_WRITE
        
        """
    def close(self):
        """
        Close the selector.

                This must be called to make sure that any underlying resource is freed.
        
        """
    def get_key(self, fileobj):
        """
        Return the key associated to a registered file object.

                Returns:
                SelectorKey for this file object
        
        """
    def get_map(self):
        """
        Return a mapping of file objects to selector keys.
        """
    def __enter__(self):
        """
        Base selector implementation.
        """
    def __init__(self):
        """
         this maps file descriptors to keys

        """
    def _fileobj_lookup(self, fileobj):
        """
        Return a file descriptor from a file object.

                This wraps _fileobj_to_fd() to do an exhaustive search in case
                the object is invalid but we still have it in our map.  This
                is used by unregister() so we can unregister an object that
                was previously registered even if it is closed.  It is also
                used by _SelectorMapping.
        
        """
    def register(self, fileobj, events, data=None):
        """
        Invalid events: {!r}
        """
    def unregister(self, fileobj):
        """
        {!r} is not registered
        """
    def modify(self, fileobj, events, data=None):
        """
        {!r} is not registered
        """
    def close(self):
        """
        Return the key associated to a given file descriptor.

                Parameters:
                fd -- file descriptor

                Returns:
                corresponding key, or None if not found
        
        """
def SelectSelector(_BaseSelectorImpl):
    """
    Select-based selector.
    """
    def __init__(self):
        """
        'win32'
        """
        def _select(self, r, w, _, timeout=None):
            """
            Base class shared between poll, epoll and devpoll selectors.
            """
    def __init__(self):
        """
         This can happen if the FD was closed since it
         was registered.

        """
    def modify(self, fileobj, events, data=None):
        """
        f"{fileobj!r} is not registered
        """
    def select(self, timeout=None):
        """
         This is shared between poll() and epoll().
         epoll() has a different signature and handling of timeout parameter.

        """
    def PollSelector(_PollLikeSelector):
    """
    Poll-based selector.
    """
    def EpollSelector(_PollLikeSelector):
    """
    Epoll-based selector.
    """
        def fileno(self):
            """
             epoll_wait() has a resolution of 1 millisecond, round away
             from zero to wait *at least* timeout seconds.

            """
        def close(self):
            """
            'devpoll'
            """
    def DevpollSelector(_PollLikeSelector):
    """
    Solaris /dev/poll selector.
    """
        def fileno(self):
            """
            'kqueue'
            """
    def KqueueSelector(_BaseSelectorImpl):
    """
    Kqueue-based selector.
    """
        def __init__(self):
            """
             This can happen if the FD was closed since it
             was registered.

            """
        def select(self, timeout=None):
            """
             Choose the best implementation, roughly:
                epoll|kqueue|devpoll > poll > select.
             select() also can't accept a FD > FD_SETSIZE (usually around 1024)

            """
