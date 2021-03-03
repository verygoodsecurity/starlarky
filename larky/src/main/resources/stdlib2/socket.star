def _intenum_converter(value, enum_klass):
    """
    Convert a numeric family value to an IntEnum member.

        If it's not a known member, return the numeric value itself.
    
    """
def _GiveupOnSendfile(Exception): pass
    """
    A subclass of _socket.socket adding the makefile() method.
    """
    def __init__(self, family=-1, type=-1, proto=-1, fileno=None):
        """
         For user code address family and type values are IntEnum members, but
         for the underlying _socket.socket they're just integers. The
         constructor of _socket.socket converts the given argument to an
         integer automatically.

        """
    def __enter__(self):
        """
        Wrap __repr__() to reveal the real class name and socket
                address(es).
        
        """
    def __getstate__(self):
        """
        f"cannot pickle {self.__class__.__name__!r} object
        """
    def dup(self):
        """
        dup() -> socket object

                Duplicate the socket. Return a new socket object connected to the same
                system resource. The new socket is non-inheritable.
        
        """
    def accept(self):
        """
        accept() -> (socket object, address info)

                Wait for an incoming connection.  Return a new socket
                representing the connection, and the address of the client.
                For IP sockets, the address info is a pair (hostaddr, port).
        
        """
2021-03-02 20:54:28,071 : INFO : tokenize_signature : --> do i ever get here?
    def makefile(self, mode="r", buffering=None, *,
                 encoding=None, errors=None, newline=None):
        """
        makefile(...) -> an I/O stream connected to the socket

                The arguments are as for io.open() after the filename, except the only
                supported mode values are 'r' (default), 'w' and 'b'.
        
        """
        def _sendfile_use_sendfile(self, file, offset=0, count=None):
            """
             not a regular file
            """
        def _sendfile_use_sendfile(self, file, offset=0, count=None):
            """
            os.sendfile() not available on this platform
            """
    def _sendfile_use_send(self, file, offset=0, count=None):
        """
        non-blocking sockets are not supported
        """
    def _check_sendfile_params(self, file, offset, count):
        """
        'b'
        """
    def sendfile(self, file, offset=0, count=None):
        """
        sendfile(file[, offset[, count]]) -> sent

                Send a file until EOF is reached by using high-performance
                os.sendfile() and return the total number of bytes which
                were sent.
                *file* must be a regular file object opened in binary mode.
                If os.sendfile() is not available (e.g. Windows) or file is
                not a regular file socket.send() will be used instead.
                *offset* tells from where to start reading the file.
                If specified, *count* is the total number of bytes to transmit
                as opposed to sending the file until EOF is reached.
                File position is updated on return or also in case of error in
                which case file.tell() can be used to figure out the number of
                bytes which were sent.
                The socket must be of SOCK_STREAM type.
                Non-blocking sockets are not supported.
        
        """
    def _decref_socketios(self):
        """
         This function should not reference any globals. See issue #808164.

        """
    def close(self):
        """
         This function should not reference any globals. See issue #808164.

        """
    def detach(self):
        """
        detach() -> file descriptor

                Close the socket object without closing the underlying file descriptor.
                The object cannot be used after this call, but the file descriptor
                can be reused for other purposes.  The file descriptor is returned.
        
        """
    def family(self):
        """
        Read-only access to the address family for this socket.
        
        """
    def type(self):
        """
        Read-only access to the socket type.
        
        """
        def get_inheritable(self):
            """
            Get the inheritable flag of the socket
            """
def fromfd(fd, family, type, proto=0):
    """
     fromfd(fd, family, type[, proto]) -> socket object

        Create a socket object from a duplicate of the given file
        descriptor.  The remaining arguments are the same as for socket().
    
    """
    def fromshare(info):
        """
         fromshare(info) -> socket object

                Create a socket object from the bytes object returned by
                socket.share(pid).
        
        """
    def socketpair(family=None, type=SOCK_STREAM, proto=0):
        """
        socketpair([family[, type[, proto]]]) -> (socket object, socket object)

                Create a pair of socket objects from the sockets returned by the platform
                socketpair() function.
                The arguments are the same as for socket() except the default family is
                AF_UNIX if defined on the platform; otherwise, the default is AF_INET.
        
        """
    def socketpair(family=AF_INET, type=SOCK_STREAM, proto=0):
        """
        Only AF_INET and AF_INET6 socket address families 
        are supported
        """
def SocketIO(io.RawIOBase):
    """
    Raw I/O implementation for stream sockets.

        This class supports the makefile() method on sockets.  It provides
        the raw I/O interface on top of a socket object.
    
    """
    def __init__(self, sock, mode):
        """
        r
        """
    def readinto(self, b):
        """
        Read up to len(b) bytes into the writable buffer *b* and return
                the number of bytes read.  If the socket is non-blocking and no bytes
                are available, None is returned.

                If *b* is non-empty, a 0 return value indicates that the connection
                was shutdown at the other end.
        
        """
    def write(self, b):
        """
        Write the given bytes or bytearray object *b* to the socket
                and return the number of bytes written.  This can be less than
                len(b) if not all data could be written.  If the socket is
                non-blocking and no bytes could be written None is returned.
        
        """
    def readable(self):
        """
        True if the SocketIO is open for reading.
        
        """
    def writable(self):
        """
        True if the SocketIO is open for writing.
        
        """
    def seekable(self):
        """
        True if the SocketIO is open for seeking.
        
        """
    def fileno(self):
        """
        Return the file descriptor of the underlying socket.
        
        """
    def name(self):
        """
        Close the SocketIO object.  This doesn't close the underlying
                socket, except if all references to it have disappeared.
        
        """
def getfqdn(name=''):
    """
    Get fully qualified domain name from name.

        An empty argument is interpreted as meaning the local host.

        First the hostname returned by gethostbyaddr() is checked, then
        possibly existing aliases. In case no FQDN is available, hostname
        from gethostname() is returned.
    
    """
2021-03-02 20:54:28,081 : INFO : tokenize_signature : --> do i ever get here?
def create_connection(address, timeout=_GLOBAL_DEFAULT_TIMEOUT,
                      source_address=None):
    """
    Connect to *address* and return the socket object.

        Convenience function.  Connect to *address* (a 2-tuple ``(host,
        port)``) and return the socket object.  Passing the optional
        *timeout* parameter will set the timeout on the socket instance
        before attempting to connect.  If no *timeout* is supplied, the
        global default timeout setting returned by :func:`getdefaulttimeout`
        is used.  If *source_address* is set it must be a tuple of (host, port)
        for the socket to bind as a source address before making the connection.
        A host of '' or port 0 tells the OS to use the default.
    
    """
def has_dualstack_ipv6():
    """
    Return True if the platform supports creating a SOCK_STREAM socket
        which can handle both AF_INET and AF_INET6 (IPv4 / IPv6) connections.
    
    """
2021-03-02 20:54:28,082 : INFO : tokenize_signature : --> do i ever get here?
def create_server(address, *, family=AF_INET, backlog=None, reuse_port=False,
                  dualstack_ipv6=False):
    """
    Convenience function which creates a SOCK_STREAM type socket
        bound to *address* (a 2-tuple (host, port)) and return the socket
        object.

        *family* should be either AF_INET or AF_INET6.
        *backlog* is the queue size passed to socket.listen().
        *reuse_port* dictates whether to use the SO_REUSEPORT socket option.
        *dualstack_ipv6*: if true and the platform supports it, it will
        create an AF_INET6 socket able to accept both IPv4 or IPv6
        connections. When false it will explicitly disable this option on
        platforms that enable it by default (e.g. Linux).

        >>> with create_server(('', 8000)) as server:
        ...     while True:
        ...         conn, addr = server.accept()
        ...         # handle new connection
    
    """
def getaddrinfo(host, port, family=0, type=0, proto=0, flags=0):
    """
    Resolve host and port into list of address info entries.

        Translate the host/port argument into a sequence of 5-tuples that contain
        all the necessary arguments for creating a socket connected to that service.
        host is a domain name, a string representation of an IPv4/v6 address or
        None. port is a string service name such as 'http', a numeric port number or
        None. By passing None as the value of host and port, you can pass NULL to
        the underlying C API.

        The family, type and proto arguments can be optionally specified in order to
        narrow the list of addresses returned. Passing zero as a value for each of
        these arguments selects the full range of results.
    
    """
