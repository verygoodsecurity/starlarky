def _encode(data, name='data'):
    """
    Call data.encode("latin-1") but show a better error message.
    """
def HTTPMessage(email.message.Message):
    """
     XXX The only usage of this method is in
     http.server.CGIHTTPRequestHandler.  Maybe move the code there so
     that it doesn't need to be part of the public API.  The API has
     never been defined so this could cause backwards compatibility
     issues.


    """
    def getallmatchingheaders(self, name):
        """
        Find all header lines matching a given header name.

                Look through the list of headers and find all lines matching a given
                header name (and their continuation lines).  A list of the lines is
                returned, without interpretation.  If the header does not occur, an
                empty list is returned.  If the header occurs multiple times, all
                occurrences are returned.  Case is not important in the header name.

        
        """
def parse_headers(fp, _class=HTTPMessage):
    """
    Parses only RFC2822 headers from a file pointer.

        email Parser wants to see strings rather than bytes.
        But a TextIOWrapper around self.rfile would buffer too many bytes
        from the stream, bytes which we later need to read as bytes.
        So we read the correct bytes here, as bytes, for email Parser
        to parse.

    
    """
def HTTPResponse(io.BufferedIOBase):
    """
     See RFC 2616 sec 19.6 and RFC 1945 sec 6 for details.

     The bytes from the socket object are iso-8859-1 strings.
     See RFC 2616 sec 2.2 which notes an exception for MIME-encoded
     text following RFC 2047.  The basic status line parsing only
     accepts iso-8859-1.


    """
    def __init__(self, sock, debuglevel=0, method=None, url=None):
        """
         If the response includes a content-length header, we need to
         make sure that the client doesn't read more than the
         specified number of bytes.  If it does, it will block until
         the server times out and closes the connection.  This will
         happen if a self.fp.read() is done (without a size) whether
         self.fp is buffered or not.  So, no self.fp.read() by
         clients unless they know what they are doing.

        """
    def _read_status(self):
        """
        iso-8859-1
        """
    def begin(self):
        """
         we've already started reading the response

        """
    def _check_close(self):
        """
        connection
        """
    def _close_conn(self):
        """
         set "closed" flag
        """
    def flush(self):
        """
        Always returns True
        """
    def isclosed(self):
        """
        True if the connection is closed.
        """
    def read(self, amt=None):
        """
        b
        """
    def readinto(self, b):
        """
        Read up to len(b) bytes into bytearray b and return the number
                of bytes read.
        
        """
    def _read_next_chunk_size(self):
        """
         Read the next chunk size from the file

        """
    def _read_and_discard_trailer(self):
        """
         read and discard trailer up to the CRLF terminator
         note: we shouldn't have any trailers!

        """
    def _get_chunk_left(self):
        """
         return self.chunk_left, reading a new chunk if necessary.
         chunk_left == 0: at the end of the current chunk, need to close it
         chunk_left == None: No current chunk, should read next.
         This function returns non-zero or None if the last chunk has
         been read.

        """
    def _readall_chunked(self):
        """
        b''
        """
    def _readinto_chunked(self, b):
        """
        Read the number of bytes requested.

                This function should be used when <amt> bytes "should" be present for
                reading. If the bytes are truly not available (due to EOF), then the
                IncompleteRead exception can be used to detect the problem.
        
        """
    def _safe_readinto(self, b):
        """
        Same as _safe_read, but for reading into a buffer.
        """
    def read1(self, n=-1):
        """
        Read with at most one underlying system call.  If at least one
                byte is buffered, return that instead.
        
        """
    def peek(self, n=-1):
        """
         Having this enables IOBase.readline() to read more than one
         byte at a time

        """
    def readline(self, limit=-1):
        """
        HEAD
        """
    def _read1_chunked(self, n):
        """
         Strictly speaking, _get_chunk_left() may cause more than one read,
         but that is ok, since that is to satisfy the chunked protocol.

        """
    def _peek_chunked(self, n):
        """
         Strictly speaking, _get_chunk_left() may cause more than one read,
         but that is ok, since that is to satisfy the chunked protocol.

        """
    def fileno(self):
        """
        '''Returns the value of the header matching *name*.

                If there are multiple matching headers, the values are
                combined into a single string separated by commas and spaces.

                If no matching header is found, returns *default* or None if
                the *default* is not specified.

                If the headers are unknown, raises http.client.ResponseNotReady.

                '''
        """
    def getheaders(self):
        """
        Return list of (header, value) tuples.
        """
    def __iter__(self):
        """
         For compatibility with old-style urllib responses.


        """
    def info(self):
        """
        '''Returns an instance of the class mimetools.Message containing
                meta-information associated with the URL.

                When the method is HTTP, these headers are those returned by
                the server at the head of the retrieved HTML page (including
                Content-Length and Content-Type).

                When the method is FTP, a Content-Length header will be
                present if (as is now usual) the server passed back a file
                length in response to the FTP retrieval request. A
                Content-Type header will be present if the MIME type can be
                guessed.

                When the method is local-file, returned headers will include
                a Date representing the file's last-modified time, a
                Content-Length giving file size, and a Content-Type
                containing a guess at the file's type. See also the
                description of the mimetools module.

                '''
        """
    def geturl(self):
        """
        '''Return the real URL of the page.

                In some cases, the HTTP server redirects a client to another
                URL. The urlopen() function handles this transparently, but in
                some cases the caller needs to know which URL the client was
                redirected to. The geturl() method can be used to get at this
                redirected URL.

                '''
        """
    def getcode(self):
        """
        '''Return the HTTP status code that was sent with the response,
                or None if the URL is not an HTTP URL.

                '''
        """
def HTTPConnection:
    """
    'HTTP/1.1'
    """
    def _is_textIO(stream):
        """
        Test whether a file-like object is a text or a binary stream.
        
        """
    def _get_content_length(body, method):
        """
        Get the content-length based on the body.

                If the body is None, we set Content-Length: 0 for methods that expect
                a body (RFC 7230, Section 3.3.2). We also set the Content-Length for
                any method if the body is a str or bytes-like object and not a file.
        
        """
2021-03-02 20:53:51,166 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, host, port=None, timeout=socket._GLOBAL_DEFAULT_TIMEOUT,
                 source_address=None, blocksize=8192):
        """
         This is stored as an instance variable to allow unit
         tests to replace it with a suitable mockup

        """
    def set_tunnel(self, host, port=None, headers=None):
        """
        Set up host and port for HTTP CONNECT tunnelling.

                In a connection that uses HTTP CONNECT tunneling, the host passed to the
                constructor is used as a proxy server that relays all communication to
                the endpoint passed to `set_tunnel`. This done by sending an HTTP
                CONNECT request to the proxy server when the connection is established.

                This method must be called before the HTML connection has been
                established.

                The headers argument should be a mapping of extra HTTP headers to send
                with the CONNECT request.
        
        """
    def _get_hostport(self, host, port):
        """
        ':'
        """
    def set_debuglevel(self, level):
        """
        CONNECT %s:%d HTTP/1.0\r\n
        """
    def connect(self):
        """
        Connect to the host and port specified in __init__.
        """
    def close(self):
        """
        Close the connection to the HTTP server.
        """
    def send(self, data):
        """
        Send `data' to the server.
                ``data`` can be a string object, a bytes object, an array object, a
                file-like object that supports a .read() method, or an iterable object.
        
        """
    def _output(self, s):
        """
        Add a line of output to the current request buffer.

                Assumes that the line does *not* end with \\r\\n.
        
        """
    def _read_readable(self, readable):
        """
        sendIng a read()able
        """
    def _send_output(self, message_body=None, encode_chunked=False):
        """
        Send the currently buffered request and clear the buffer.

                Appends an extra \\r\\n to the buffer.
                A message_body may be specified, to be appended to the request.
        
        """
2021-03-02 20:53:51,174 : INFO : tokenize_signature : --> do i ever get here?
    def putrequest(self, method, url, skip_host=False,
                   skip_accept_encoding=False):
        """
        Send a request to the server.

                `method' specifies an HTTP request method, e.g. 'GET'.
                `url' specifies the object being requested, e.g. '/index.html'.
                `skip_host' if True does not add automatically a 'Host:' header
                `skip_accept_encoding' if True does not add automatically an
                   'Accept-Encoding:' header
        
        """
    def _encode_request(self, request):
        """
         ASCII also helps prevent CVE-2019-9740.

        """
    def _validate_method(self, method):
        """
        Validate a method name for putrequest.
        """
    def _validate_path(self, url):
        """
        Validate a url for putrequest.
        """
    def _validate_host(self, host):
        """
        Validate a host so it doesn't contain control characters.
        """
    def putheader(self, header, *values):
        """
        Send a request header line to the server.

                For example: h.putheader('Accept', 'text/html')
        
        """
    def endheaders(self, message_body=None, *, encode_chunked=False):
        """
        Indicate that the last header line has been sent to the server.

                This method sends the request to the server.  The optional message_body
                argument can be used to pass a message body associated with the
                request.
        
        """
2021-03-02 20:53:51,178 : INFO : tokenize_signature : --> do i ever get here?
    def request(self, method, url, body=None, headers={}, *,
                encode_chunked=False):
        """
        Send a complete request to the server.
        """
    def _send_request(self, method, url, body, headers, encode_chunked):
        """
         Honor explicitly requested Host: and Accept-Encoding: headers.

        """
    def getresponse(self):
        """
        Get the response from the server.

                If the HTTPConnection is in the correct state, returns an
                instance of HTTPResponse or of whatever object is returned by
                the response_class variable.

                If a request has not been sent or if a previous response has
                not be handled, ResponseNotReady is raised.  If the HTTP
                response indicates that the connection should be closed, then
                it will be closed before the response is returned.  When the
                connection is closed, the underlying socket is closed.
        
        """
    def HTTPSConnection(HTTPConnection):
    """
    This class allows communication via SSL.
    """
2021-03-02 20:53:51,181 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,181 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,181 : INFO : tokenize_signature : --> do i ever get here?
        def __init__(self, host, port=None, key_file=None, cert_file=None,
                     timeout=socket._GLOBAL_DEFAULT_TIMEOUT,
                     source_address=None, *, context=None,
                     check_hostname=None, blocksize=8192):
            """
            key_file, cert_file and check_hostname are 
            deprecated, use a custom context instead.
            """
        def connect(self):
            """
            Connect to a host on a given (SSL) port.
            """
def HTTPException(Exception):
    """
     Subclasses that define an __init__ must call Exception.__init__
     or define self.args.  Otherwise, str() will fail.

    """
def NotConnected(HTTPException):
    """
    ', %i more expected'
    """
def ImproperConnectionState(HTTPException):
    """
    got more than %d bytes when reading %s

    """
def RemoteDisconnected(ConnectionResetError, BadStatusLine):
    """

    """
