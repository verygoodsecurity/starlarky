def TLSVersion(_IntEnum):
    """
    Content types (record layer)

        See RFC 8446, section B.1
    
    """
def _TLSAlertType(_IntEnum):
    """
    Alert types for TLSContentType.ALERT messages

        See RFC 8466, section B.2
    
    """
def _TLSMessageType(_IntEnum):
    """
    Message types (handshake protocol)

        See RFC 8446, section B.3
    
    """
def _dnsname_match(dn, hostname):
    """
    Matching according to RFC 6125, section 6.4.3

        - Hostnames are compared lower case.
        - For IDNA, both dn and hostname must be encoded as IDN A-label (ACE).
        - Partial wildcards like 'www*.example.org', multiple wildcards, sole
          wildcard or wildcards in labels other then the left-most label are not
          supported and a CertificateError is raised.
        - A wildcard must match at least one character.
    
    """
def _inet_paton(ipname):
    """
    Try to convert an IP address to packed binary form

        Supports IPv4 addresses on all platforms and IPv6 on platforms with IPv6
        support.
    
    """
def _ipaddress_match(cert_ipaddress, host_ip):
    """
    Exact matching of IP addresses.

        RFC 6125 explicitly doesn't define an algorithm for this
        (section 1.7.2 - "Out of Scope").
    
    """
def match_hostname(cert, hostname):
    """
    Verify that *cert* (in decoded format as returned by
        SSLSocket.getpeercert()) matches the *hostname*.  RFC 2818 and RFC 6125
        rules are followed.

        The function matches IP addresses rather than dNSNames if hostname is a
        valid ipaddress string. IPv4 addresses are supported on all platforms.
        IPv6 addresses are supported on platforms with IPv6 support (AF_INET6
        and inet_pton).

        CertificateError is raised on failure. On success, the function
        returns nothing.
    
    """
def get_default_verify_paths():
    """
    Return paths to default cafile and capath.
    
    """
def _ASN1Object(namedtuple("_ASN1Object", "nid shortname longname oid")):
    """
    ASN.1 object identifier lookup
    
    """
    def __new__(cls, oid):
        """
        Create _ASN1Object from OpenSSL numeric ID
        
        """
    def fromname(cls, name):
        """
        Create _ASN1Object from short name, long name or OID
        
        """
def Purpose(_ASN1Object, _Enum):
    """
    SSLContext purpose flags with X509v3 Extended Key Usage objects
    
    """
def SSLContext(_SSLContext):
    """
    An SSLContext holds various SSL-related configuration options and
        data, such as certificates and possibly a private key.
    """
    def __new__(cls, protocol=PROTOCOL_TLS, *args, **kwargs):
        """
        'idna'
        """
2021-03-02 20:54:27,861 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:27,861 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:27,861 : INFO : tokenize_signature : --> do i ever get here?
    def wrap_socket(self, sock, server_side=False,
                    do_handshake_on_connect=True,
                    suppress_ragged_eofs=True,
                    server_hostname=None, session=None):
        """
         SSLSocket class handles server_hostname encoding before it calls
         ctx._wrap_socket()

        """
2021-03-02 20:54:27,862 : INFO : tokenize_signature : --> do i ever get here?
    def wrap_bio(self, incoming, outgoing, server_side=False,
                 server_hostname=None, session=None):
        """
         Need to encode server_hostname here because _wrap_bio() can only
         handle ASCII str.

        """
    def set_npn_protocols(self, npn_protocols):
        """
        'ascii'
        """
    def set_servername_callback(self, server_name_callback):
        """
        not a callable object
        """
            def shim_cb(sslobj, servername, sslctx):
                """
                'ascii'
                """
    def _load_windows_store_certs(self, storename, purpose):
        """
         CA certs are never PKCS#7 encoded

        """
    def load_default_certs(self, purpose=Purpose.SERVER_AUTH):
        """
        win32
        """
        def minimum_version(self):
            """
            'HOSTFLAG_NEVER_CHECK_SUBJECT'
            """
        def hostname_checks_common_name(self):
            """
            TLS message callback

                    The message callback provides a debugging hook to analyze TLS
                    connections. The callback is called for any TLS protocol message
                    (header, handshake, alert, and more), but not for application data.
                    Due to technical  limitations, the callback can't be used to filter
                    traffic or to abort a connection. Any exception raised in the
                    callback is delayed until the handshake, read, or write operation
                    has been performed.

                    def msg_cb(conn, direction, version, content_type, msg_type, data):
                        pass

                    conn
                        :class:`SSLSocket` or :class:`SSLObject` instance
                    direction
                        ``read`` or ``write``
                    version
                        :class:`TLSVersion` enum member or int for unknown version. For a
                        frame header, it's the header version.
                    content_type
                        :class:`_TLSContentType` enum member or int for unsupported
                        content type.
                    msg_type
                        Either a :class:`_TLSContentType` enum number for a header
                        message, a :class:`_TLSAlertType` enum member for an alert
                        message, a :class:`_TLSMessageType` enum member for other
                        messages, or int for unsupported message types.
                    data
                        Raw, decrypted message content as bytes
        
            """
    def _msg_callback(self, callback):
        """
        '__call__'
        """
        def inner(conn, direction, version, content_type, msg_type, data):
            """
            Create a SSLContext object with default settings.

                NOTE: The protocol and settings may change anytime without prior
                      deprecation. The values represent a fair balance between maximum
                      compatibility and security.
    
            """
2021-03-02 20:54:27,869 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:27,869 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:27,869 : INFO : tokenize_signature : --> do i ever get here?
def _create_unverified_context(protocol=PROTOCOL_TLS, *, cert_reqs=CERT_NONE,
                           check_hostname=False, purpose=Purpose.SERVER_AUTH,
                           certfile=None, keyfile=None,
                           cafile=None, capath=None, cadata=None):
    """
    Create a SSLContext object for Python stdlib modules

        All Python stdlib modules shall use this function to create SSLContext
        objects in order to keep common settings in one place. The configuration
        is less restrict than create_default_context()'s to increase backward
        compatibility.
    
    """
def SSLObject:
    """
    This class implements an interface on top of a low-level SSL object as
        implemented by OpenSSL. This object captures the state of an SSL connection
        but does not provide any network IO itself. IO needs to be performed
        through separate "BIO" objects which are OpenSSL's IO abstraction layer.

        This class does not have a public constructor. Instances are returned by
        ``SSLContext.wrap_bio``. This class is typically used by framework authors
        that want to implement asynchronous IO for SSL through memory buffers.

        When compared to ``SSLSocket``, this object lacks the following features:

         * Any form of network IO, including methods such as ``recv`` and ``send``.
         * The ``do_handshake_on_connect`` and ``suppress_ragged_eofs`` machinery.
    
    """
    def __init__(self, *args, **kwargs):
        """
        f"{self.__class__.__name__} does not have a public 
        f"constructor. Instances are returned by SSLContext.wrap_bio().

        """
2021-03-02 20:54:27,870 : INFO : tokenize_signature : --> do i ever get here?
    def _create(cls, incoming, outgoing, server_side=False,
                 server_hostname=None, session=None, context=None):
        """
        The SSLContext that is currently in use.
        """
    def context(self, ctx):
        """
        The SSLSession for client socket.
        """
    def session(self, session):
        """
        Was the client session reused during handshake
        """
    def server_side(self):
        """
        Whether this is a server-side socket.
        """
    def server_hostname(self):
        """
        The currently set server hostname (for SNI), or ``None`` if no
                server hostname is set.
        """
    def read(self, len=1024, buffer=None):
        """
        Read up to 'len' bytes from the SSL object and return them.

                If 'buffer' is provided, read into this buffer and return the number of
                bytes read.
        
        """
    def write(self, data):
        """
        Write 'data' to the SSL object and return the number of bytes
                written.

                The 'data' argument must support the buffer interface.
        
        """
    def getpeercert(self, binary_form=False):
        """
        Returns a formatted version of the data in the certificate provided
                by the other end of the SSL channel.

                Return None if no certificate was provided, {} if a certificate was
                provided, but not validated.
        
        """
    def selected_npn_protocol(self):
        """
        Return the currently selected NPN protocol as a string, or ``None``
                if a next protocol was not negotiated or if NPN is not supported by one
                of the peers.
        """
    def selected_alpn_protocol(self):
        """
        Return the currently selected ALPN protocol as a string, or ``None``
                if a next protocol was not negotiated or if ALPN is not supported by one
                of the peers.
        """
    def cipher(self):
        """
        Return the currently selected cipher as a 3-tuple ``(name,
                ssl_version, secret_bits)``.
        """
    def shared_ciphers(self):
        """
        Return a list of ciphers shared by the client during the handshake or
                None if this is not a valid server connection.
        
        """
    def compression(self):
        """
        Return the current compression algorithm in use, or ``None`` if
                compression was not negotiated or not supported by one of the peers.
        """
    def pending(self):
        """
        Return the number of bytes that can be read immediately.
        """
    def do_handshake(self):
        """
        Start the SSL/TLS handshake.
        """
    def unwrap(self):
        """
        Start the SSL shutdown handshake.
        """
    def get_channel_binding(self, cb_type="tls-unique"):
        """
        Get channel binding data for current connection.  Raise ValueError
                if the requested `cb_type` is not supported.  Return bytes of the data
                or None if the data is not available (e.g. before the handshake).
        """
    def version(self):
        """
        Return a string identifying the protocol version used by the
                current SSL channel. 
        """
    def verify_client_post_handshake(self):
        """
        Copy docstring from SSLObject to SSLSocket
        """
def SSLSocket(socket):
    """
    This class implements a subtype of socket.socket that wraps
        the underlying OS socket in an SSL context when necessary, and
        provides read and write methods over that channel. 
    """
    def __init__(self, *args, **kwargs):
        """
        f"{self.__class__.__name__} does not have a public 
        f"constructor. Instances are returned by 
        f"SSLContext.wrap_socket().

        """
2021-03-02 20:54:27,874 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:27,874 : INFO : tokenize_signature : --> do i ever get here?
    def _create(cls, sock, server_side=False, do_handshake_on_connect=True,
                suppress_ragged_eofs=True, server_hostname=None,
                context=None, session=None):
        """
        only stream sockets are supported
        """
    def context(self):
        """
        Can't dup() %s instances
        """
    def _checkClosed(self, msg=None):
        """
         raise an exception here if you wish to check for spurious closes

        """
    def _check_connected(self):
        """
         getpeername() will raise ENOTCONN if the socket is really
         not connected; note that we can be connected even without
         _connected being set, e.g. if connect() first returned
         EAGAIN.

        """
    def read(self, len=1024, buffer=None):
        """
        Read up to LEN bytes and return them.
                Return zero-length string on EOF.
        """
    def write(self, data):
        """
        Write DATA to the underlying SSL channel.  Returns
                number of bytes of DATA actually transmitted.
        """
    def getpeercert(self, binary_form=False):
        """
        non-zero flags not allowed in calls to send() on %s
        """
    def sendto(self, data, flags_or_addr, addr=None):
        """
        sendto not allowed on instances of %s
        """
    def sendmsg(self, *args, **kwargs):
        """
         Ensure programs don't send data unencrypted if they try to
         use this method.

        """
    def sendall(self, data, flags=0):
        """
        non-zero flags not allowed in calls to sendall() on %s
        """
    def sendfile(self, file, offset=0, count=None):
        """
        Send a file, possibly by using os.sendfile() if this is a
                clear-text socket.  Return the total number of bytes sent.
        
        """
    def recv(self, buflen=1024, flags=0):
        """
        non-zero flags not allowed in calls to recv() on %s
        """
    def recv_into(self, buffer, nbytes=None, flags=0):
        """
        non-zero flags not allowed in calls to recv_into() on %s
        """
    def recvfrom(self, buflen=1024, flags=0):
        """
        recvfrom not allowed on instances of %s
        """
    def recvfrom_into(self, buffer, nbytes=None, flags=0):
        """
        recvfrom_into not allowed on instances of %s
        """
    def recvmsg(self, *args, **kwargs):
        """
        recvmsg not allowed on instances of %s
        """
    def recvmsg_into(self, *args, **kwargs):
        """
        recvmsg_into not allowed on instances of 
        %s
        """
    def pending(self):
        """
        No SSL wrapper around 
        """
    def verify_client_post_handshake(self):
        """
        No SSL wrapper around 
        """
    def _real_close(self):
        """
        can't connect in server-side mode
        """
    def connect(self, addr):
        """
        Connects to remote ADDR, and then wraps the connection in
                an SSL channel.
        """
    def connect_ex(self, addr):
        """
        Connects to remote ADDR, and then wraps the connection in
                an SSL channel.
        """
    def accept(self):
        """
        Accepts a new connection from a remote client, and returns
                a tuple containing that new connection wrapped with a server-side
                SSL channel, and the address of the remote client.
        """
    def get_channel_binding(self, cb_type="tls-unique"):
        """
        {0} channel binding type not implemented
        """
    def version(self):
        """
         Python does not support forward declaration of types.

        """
2021-03-02 20:54:27,884 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:27,885 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:27,885 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:27,885 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:27,885 : INFO : tokenize_signature : --> do i ever get here?
def wrap_socket(sock, keyfile=None, certfile=None,
                server_side=False, cert_reqs=CERT_NONE,
                ssl_version=PROTOCOL_TLS, ca_certs=None,
                do_handshake_on_connect=True,
                suppress_ragged_eofs=True,
                ciphers=None):
    """
    certfile must be specified for server-side 
    operations
    """
def cert_time_to_seconds(cert_time):
    """
    Return the time in seconds since the Epoch, given the timestring
        representing the "notBefore" or "notAfter" date from a certificate
        in ``"%b %d %H:%M:%S %Y %Z"`` strptime format (C locale).

        "notBefore" or "notAfter" dates must use UTC (RFC 5280).

        Month is one of: Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec
        UTC should be specified as GMT (see ASN1_TIME_print())
    
    """
def DER_cert_to_PEM_cert(der_cert_bytes):
    """
    Takes a certificate in binary DER format and returns the
        PEM version of it as a string.
    """
def PEM_cert_to_DER_cert(pem_cert_string):
    """
    Takes a certificate in ASCII PEM format and returns the
        DER-encoded version of it as a byte sequence
    """
def get_server_certificate(addr, ssl_version=PROTOCOL_TLS, ca_certs=None):
    """
    Retrieve the certificate from the server at the specified address,
        and return it as a PEM-encoded string.
        If 'ca_certs' is specified, validate the server cert against it.
        If 'ssl_version' is specified, use it in the connection attempt.
    """
def get_protocol_name(protocol_code):
    """
    '<unknown>'
    """
