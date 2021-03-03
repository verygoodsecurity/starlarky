def _create_transport_context(server_side, server_hostname):
    """
    'Server side SSL needs a valid SSLContext'
    """
def _SSLPipe(object):
    """
    An SSL "Pipe".

        An SSL pipe allows you to communicate with an SSL/TLS protocol instance
        through memory buffers. It can be used to implement a security layer for an
        existing connection where you don't have access to the connection's file
        descriptor, or for some reason you don't want to use it.

        An SSL pipe can be in "wrapped" and "unwrapped" mode. In unwrapped mode,
        data is passed through untransformed. In wrapped mode, application level
        data is encrypted to SSL record level data and vice versa. The SSL record
        level is the lowest level in the SSL protocol suite and is what travels
        as-is over the wire.

        An SslPipe initially is in "unwrapped" mode. To start SSL, call
        do_handshake(). To shutdown SSL again, call unwrap().
    
    """
    def __init__(self, context, server_side, server_hostname=None):
        """

                The *context* argument specifies the ssl.SSLContext to use.

                The *server_side* argument indicates whether this is a server side or
                client side transport.

                The optional *server_hostname* argument can be used to specify the
                hostname you are connecting to. You may only specify this parameter if
                the _ssl module supports Server Name Indication (SNI).
        
        """
    def context(self):
        """
        The SSL context passed to the constructor.
        """
    def ssl_object(self):
        """
        The internal ssl.SSLObject instance.

                Return None if the pipe is not wrapped.
        
        """
    def need_ssldata(self):
        """
        Whether more record level data is needed to complete a handshake
                that is currently in progress.
        """
    def wrapped(self):
        """

                Whether a security layer is currently in effect.

                Return False during handshake.
        
        """
    def do_handshake(self, callback=None):
        """
        Start the SSL handshake.

                Return a list of ssldata. A ssldata element is a list of buffers

                The optional *callback* argument can be used to install a callback that
                will be called when the handshake is complete. The callback will be
                called with None if successful, else an exception instance.
        
        """
    def shutdown(self, callback=None):
        """
        Start the SSL shutdown sequence.

                Return a list of ssldata. A ssldata element is a list of buffers

                The optional *callback* argument can be used to install a callback that
                will be called when the shutdown is complete. The callback will be
                called without arguments.
        
        """
    def feed_eof(self):
        """
        Send a potentially "ragged" EOF.

                This method will raise an SSL_ERROR_EOF exception if the EOF is
                unexpected.
        
        """
    def feed_ssldata(self, data, only_handshake=False):
        """
        Feed SSL record level data into the pipe.

                The data must be a bytes instance. It is OK to send an empty bytes
                instance. This can be used to get ssldata for a handshake initiated by
                this endpoint.

                Return a (ssldata, appdata) tuple. The ssldata element is a list of
                buffers containing SSL data that needs to be sent to the remote SSL.

                The appdata element is a list of buffers containing plaintext data that
                needs to be forwarded to the application. The appdata list may contain
                an empty buffer indicating an SSL "close_notify" alert. This alert must
                be acknowledged by calling shutdown().
        
        """
    def feed_appdata(self, data, offset=0):
        """
        Feed plaintext data into the pipe.

                Return an (ssldata, offset) tuple. The ssldata element is a list of
                buffers containing record level data that needs to be sent to the
                remote SSL instance. The offset is the number of plaintext bytes that
                were processed, which may be less than the length of data.

                NOTE: In case of short writes, this call MUST be retried with the SAME
                buffer passed into the *data* argument (i.e. the id() must be the
                same). This is an OpenSSL requirement. A further particularity is that
                a short write will always have offset == 0, because the _ssl module
                does not enable partial writes. And even though the offset is zero,
                there will still be encrypted data in ssldata.
        
        """
2021-03-02 20:54:33,310 : INFO : tokenize_signature : --> do i ever get here?
def _SSLProtocolTransport(transports._FlowControlMixin,
                            transports.Transport):
    """
     SSLProtocol instance

    """
    def get_extra_info(self, name, default=None):
        """
        Get optional transport information.
        """
    def set_protocol(self, protocol):
        """
        Close the transport.

                Buffered data will be flushed asynchronously.  No more data
                will be received.  After all buffered data is flushed, the
                protocol's connection_lost() method will (eventually) called
                with None as its argument.
        
        """
    def __del__(self, _warn=warnings.warn):
        """
        f"unclosed transport {self!r}
        """
    def is_reading(self):
        """
        'SSL transport has not been initialized yet'
        """
    def pause_reading(self):
        """
        Pause the receiving end.

                No data will be passed to the protocol's data_received()
                method until resume_reading() is called.
        
        """
    def resume_reading(self):
        """
        Resume the receiving end.

                Data received will once again be passed to the protocol's
                data_received() method.
        
        """
    def set_write_buffer_limits(self, high=None, low=None):
        """
        Set the high- and low-water limits for write flow control.

                These two values control when to call the protocol's
                pause_writing() and resume_writing() methods.  If specified,
                the low-water limit must be less than or equal to the
                high-water limit.  Neither value can be negative.

                The defaults are implementation-specific.  If only the
                high-water limit is given, the low-water limit defaults to an
                implementation-specific value less than or equal to the
                high-water limit.  Setting high to zero forces low to zero as
                well, and causes pause_writing() to be called whenever the
                buffer becomes non-empty.  Setting low to zero causes
                resume_writing() to be called only once the buffer is empty.
                Use of zero for either limit is generally sub-optimal as it
                reduces opportunities for doing I/O and computation
                concurrently.
        
        """
    def get_write_buffer_size(self):
        """
        Return the current size of the write buffer.
        """
    def _protocol_paused(self):
        """
         Required for sendfile fallback pause_writing/resume_writing logic

        """
    def write(self, data):
        """
        Write some data bytes to the transport.

                This does not block; it buffers the data and arranges for it
                to be sent out asynchronously.
        
        """
    def can_write_eof(self):
        """
        Return True if this transport supports write_eof(), False if not.
        """
    def abort(self):
        """
        Close the transport immediately.

                Buffered data will be lost.  No more data will be received.
                The protocol's connection_lost() method will (eventually) be
                called with None as its argument.
        
        """
def SSLProtocol(protocols.Protocol):
    """
    SSL protocol.

        Implementation of SSL on top of a socket using incoming and outgoing
        buffers which are ssl.MemoryBIO objects.
    
    """
2021-03-02 20:54:33,312 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:33,312 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:33,312 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, loop, app_protocol, sslcontext, waiter,
                 server_side=False, server_hostname=None,
                 call_connection_made=True,
                 ssl_handshake_timeout=None):
        """
        'stdlib ssl module not available'
        """
    def _set_app_protocol(self, app_protocol):
        """
        Called when the low-level connection is made.

                Start the SSL handshake.
        
        """
    def connection_lost(self, exc):
        """
        Called when the low-level connection is lost or closed.

                The argument is an exception object or None (the latter
                meaning a regular EOF is received or the connection was
                aborted or closed).
        
        """
    def pause_writing(self):
        """
        Called when the low-level transport's buffer goes over
                the high-water mark.
        
        """
    def resume_writing(self):
        """
        Called when the low-level transport's buffer drains below
                the low-water mark.
        
        """
    def data_received(self, data):
        """
        Called when some SSL data is received.

                The argument is a bytes object.
        
        """
    def eof_received(self):
        """
        Called when the other end of the low-level stream
                is half-closed.

                If this returns a false value (including None), the transport
                will close itself.  If it returns a true value, closing the
                transport is up to the protocol.
        
        """
    def _get_extra_info(self, name, default=None):
        """
        b''
        """
    def _write_appdata(self, data):
        """
        %r starts SSL handshake
        """
    def _check_handshake_timeout(self):
        """
        f"SSL handshake is taking longer than 
        f"{self._ssl_handshake_timeout} seconds: 
        f"aborting the connection

        """
    def _on_handshake_complete(self, handshake_exc):
        """
        'SSL handshake failed on verifying the certificate'
        """
    def _process_write_backlog(self):
        """
         Try to make progress on the write backlog.

        """
    def _fatal_error(self, exc, message='Fatal error on transport'):
        """
        %r: %s
        """
    def _finalize(self):
