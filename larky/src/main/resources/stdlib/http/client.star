r"""HTTP/1.1 client library

<intro stuff goes here>
<other stuff, too>

HTTPConnection goes through a number of "states", which define when a client
may legally make another request or fetch the response for a particular
request. This diagram details these state transitions:

    (null)
      |
      | HTTPConnection()
      v
    Idle
      |
      | putrequest()
      v
    Request-started
      |
      | ( putheader() )*  endheaders()
      v
    Request-sent
      |\_____________________________
      |                              | getresponse() raises
      | response = getresponse()     | ConnectionError
      v                              v
    Unread-response                Idle
    [Response-headers-read]
      |\____________________
      |                     |
      | response.read()     | putrequest()
      v                     v
    Idle                  Req-started-unread-response
                     ______/|
                   /        |
   response.read() |        | ( putheader() )*  endheaders()
                   v        v
       Request-started    Req-sent-unread-response
                            |
                            | response.read()
                            v
                          Request-sent

This diagram presents the following rules:
  -- a second request may not be started until {response-headers-read}
  -- a response [object] cannot be retrieved until {request-sent}
  -- there is no differentiation between an unread response body and a
     partially read response body

Note: this enforcement is applied by the HTTPConnection class. The
      HTTPResponse class does not enforce this state machine, which
      implies sophisticated clients may accelerate the request/response
      pipeline. Caution should be taken, though: accelerating the states
      beyond the above pattern may imply knowledge of the server's
      connection-close behavior for certain requests. For example, it
      is impossible to tell whether the server will close the connection
      UNTIL the response headers have been read; this means that further
      requests cannot be placed into the pipeline until it is known that
      the server will NOT be closing the connection.

Logical State                  __state            __response
-------------                  -------            ----------
Idle                           _CS_IDLE           None
Request-started                _CS_REQ_STARTED    None
Request-sent                   _CS_REQ_SENT       None
Unread-response                _CS_IDLE           <response_class>
Req-started-unread-response    _CS_REQ_STARTED    <response_class>
Req-sent-unread-response       _CS_REQ_SENT       <response_class>
"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io", io="io")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//re", re="re")
load("@stdlib//types", types="types")
load("@vendor//option/result", Error="Error")


# HTTPMessage, parse_headers(), and the HTTP status code constants are
# intentionally omitted for simplicity
__all__ = ["HTTPResponse", "HTTPConnection",
           "HTTPException", "NotConnected", "UnknownProtocol",
           "UnknownTransferEncoding", "UnimplementedFileMode",
           "IncompleteRead", "InvalidURL", "ImproperConnectionState",
           "CannotSendRequest", "CannotSendHeader", "ResponseNotReady",
           "BadStatusLine", "LineTooLong", "RemoteDisconnected", "error",
           "responses"]


HTTP_PORT = 80
HTTPS_PORT = 443

_UNKNOWN = 'UNKNOWN'

# connection states
_CS_IDLE = 'Idle'
_CS_REQ_STARTED = 'Request-started'
_CS_REQ_SENT = 'Request-sent'

# # hack to maintain backwards compatibility
# globals().update(http.HTTPStatus.__members__)

# another hack to maintain backwards compatibility
# Mapping status codes to official W3C names
# responses = {v: v.phrase for v in http.HTTPStatus.__members__.values()}

# maximal line length when calling readline().
_MAXLINE = 65536
_MAXHEADERS = 100

# Header name/value ABNF (http://tools.ietf.org/html/rfc7230#section-3.2)
#
# VCHAR          = %x21-7E
# obs-text       = %x80-FF
# header-field   = field-name ":" OWS field-value OWS
# field-name     = token
# field-value    = *( field-content / obs-fold )
# field-content  = field-vchar [ 1*( SP / HTAB ) field-vchar ]
# field-vchar    = VCHAR / obs-text
#
# obs-fold       = CRLF 1*( SP / HTAB )
#                ; obsolete line folding
#                ; see Section 3.2.4

# token          = 1*tchar
#
# tchar          = "!" / "#" / "$" / "%" / "&" / "'" / "*"
#                / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
#                / DIGIT / ALPHA
#                ; any VCHAR, except delimiters
#
# VCHAR defined in http://tools.ietf.org/html/rfc5234#appendix-B.1

# the patterns for both name and value are more lenient than RFC
# definitions to allow for backwards compatibility
_is_legal_header_name = re.compile(rb'[^:\s][^:\r\n]*').fullmatch
_is_illegal_header_value = re.compile(rb'\n(?![ \t])|\r(?![ \t\n])').search

# These characters are not allowed within HTTP URL paths.
#  See https://tools.ietf.org/html/rfc3986#section-3.3 and the
#  https://tools.ietf.org/html/rfc3986#appendix-A pchar definition.
# Prevents CVE-2019-9740.  Includes control characters such as \r\n.
# We don't restrict chars above \x7f as putrequest() limits us to ASCII.
_contains_disallowed_url_pchar_re = re.compile('[\x00-\x20\x7f]')
# Arguably only these _should_ allowed:
#  _is_allowed_url_pchars_re = re.compile(r"^[/!$&'()*+,;=:@%a-zA-Z0-9._~-]+$")
# We are more lenient for assumed real world compatibility purposes.

# These characters are not allowed within HTTP method names
# to prevent http header injection.
_contains_disallowed_method_pchar_re = re.compile('[\x00-\x1f]')

# We always set the Content-Length header for these methods because some
# servers will otherwise respond with a 411
_METHODS_EXPECTING_BODY = ('PATCH', 'POST', 'PUT',)


def FakeSocket(text, fileclass=io.BytesIO, host=None, port=None):
    self = larky.mutablestruct(__name__='FakeSocket', __class__=FakeSocket)

    def __init__(text, fileclass):
        if types.is_string(text):
            text = codecs.encode(text, encoding="ascii")
        self.text = text
        self.fileclass = fileclass
        self.data = b''
        self.file_closed = False
        self.host = host
        self.port = port

        return self
    self = __init__(text, fileclass)

    def sendall(data):
        # self.data += bytes(data)
        olddata = self.data
        if not (types.is_bytes(olddata)):
            fail("assert types.is_bytes(olddata) failed!")
        self.data += data
    self.sendall = sendall

    def makefile(mode, bufsize=None):
        if mode != "r" and mode != "rb":
            fail("UnimplementedFileMode")
        return self.fileclass(self.text)
    self.makefile = makefile
    return self


def HTTPResponse(sock=None, debuglevel=0, method=None, url=None, **kwargs):
    self = io.BytesIO()
    self.__name__ = 'HTTPResponse'
    self.__class__ = HTTPResponse

    # See RFC 2616 sec 19.6 and RFC 1945 sec 6 for details.

    # The bytes from the socket object are iso-8859-1 strings.
    # See RFC 2616 sec 2.2 which notes an exception for MIME-encoded
    # text following RFC 2047.  The basic status line parsing only
    # accepts iso-8859-1.

    def __init__(sock, debuglevel, method, url, **kwargs):
        # If the response includes a content-length header, we need to
        # make sure that the client doesn't read more than the
        # specified number of bytes.  If it does, it will block until
        # the server times out and closes the connection.  This will
        # happen if a self.fp.read() is done (without a size) whether
        # self.fp is buffered or not.  So, no self.fp.read() by
        # clients unless they know what they are doing.
        self.fp = (sock or FakeSocket(kwargs.pop('data', b''))).makefile("rb")
        self.debuglevel = debuglevel
        self._method = method
        self.headers = kwargs.pop('headers', None)
        self.msg = self.headers

        # from the Status-Line of the response
        self.version = _UNKNOWN  # HTTP-Version
        self.status = kwargs.pop('status', _UNKNOWN)  # Status-Code
        self.reason = _UNKNOWN  # Reason-Phrase

        self.chunked = _UNKNOWN  # is "chunked" being used?
        self.chunk_left = _UNKNOWN  # bytes left to read in current chunk
        self.length = _UNKNOWN  # number of bytes left in response
        self.will_close = _UNKNOWN  # conn will close at end of response
        self.kwargs = kwargs
        return self
    self = __init__(sock, debuglevel, method, url, **kwargs)

    def begin():
        if self.headers != None:
            # we've already started reading the response
            return
    self.begin = begin

    def _check_close():
        conn = self.headers.get("connection")
        if self.version == 11:
            # An HTTP/1.1 proxy is assumed to stay open unless
            # explicitly closed.
            if conn and "close" in conn.lower():
                return True
            return False

        # Some HTTP/1.0 implementations have support for persistent
        # connections, using rules different than HTTP/1.1.

        # For older HTTP, Keep-Alive indicates persistent connection.
        if self.headers.get("keep-alive"):
            return False

        # At least Akamai returns a "Connection: Keep-Alive" header,
        # which was supposed to be sent by the client.
        if conn and "keep-alive" in conn.lower():
            return False

        # Proxy-Connection is a netscape hack.
        pconn = self.headers.get("proxy-connection")
        if pconn and "keep-alive" in pconn.lower():
            return False

        # otherwise, assume it will close
        return True
    self._check_close = _check_close

    def _close_conn():
        fp = self.fp
        self.fp = None
        fp.close()
    self._close_conn = _close_conn

    super_close = self.close
    def close():
        super_close()
        if self.fp:
            self._close_conn()
    self.close = close

    # These implementations are for the benefit of io.BufferedReader.

    # XXX This class should probably be revised to act more like
    # the "raw stream" that BufferedReader expects.

    def flush():
        pass
    self.flush = flush

    def readable():
        """Always returns True"""
        return True
    self.readable = readable

    # End of "raw stream" methods

    def isclosed():
        """True if the connection is closed."""
        # NOTE: it is possible that we will not ever call self.close(). This
        #       case occurs when will_close is TRUE, length is None, and we
        #       read up to the last byte, but NOT past it.
        #
        # IMPLIES: if will_close is FALSE, then self.close() will ALWAYS be
        #          called, meaning self.isclosed() is meaningful.
        return self.fp == None
    self.isclosed = isclosed

    def read(amt=-1):
        if self.fp == None:
            return b""

        if self._method == "HEAD":
            self._close_conn()
            return b""

        if self.chunked:
            return self._read_chunked(amt)

        if amt != None:
            if self.length != None and amt > self.length:
                # clip the read to the "end of response"
                amt = self.length
            s = self.fp.read(amt)
            if not s and amt:
                # Ideally, we would raise IncompleteRead if the content-length
                # wasn't satisfied, but it might break compatibility.
                self._close_conn()
            elif self.length != None:
                self.length -= len(s)
                if not self.length:
                    self._close_conn()
            return s
        else:
            # Amount is not given (unbounded read) so we must check self.length
            if self.length == None:
                s = self.fp.read()
            self._close_conn()  # we read everything
            return s
    self.read = read

    def readinto(b):
        """Read up to len(b) bytes into bytearray b and return the number
        of bytes read.
        """

        if self.fp == None:
            return 0

        if self._method == "HEAD":
            self._close_conn()
            return 0

        # we do not use _safe_read() here because this may be a .will_close
        # connection, and the user is reading more bytes than will be provided
        # (for example, reading in 1k chunks)
        n = self.fp.readinto(b)
        if not n and b:
            # Ideally, we would raise IncompleteRead if the content-length
            # wasn't satisfied, but it might break compatibility.
            self._close_conn()
        elif self.length != None:
            self.length -= n
            if not self.length:
                self._close_conn()
        return n
    self.readinto = readinto

    def read1(n=-1):
        """Read with at most one underlying system call.  If at least one
        byte is buffered, return that instead.
        """
        if self.fp == None or self._method == "HEAD":
            return b""
        if self.length != None and (n < 0 or n > self.length):
            n = self.length
        result = self.fp.read1(n)
        if not result and n:
            self._close_conn()
        elif self.length != None:
            self.length -= len(result)
        return result
    self.read1 = read1

    def peek(n=-1):
        # Having this enables IOBase.readline() to read more than one
        # byte at a time
        if self.fp == None or self._method == "HEAD":
            return b""
        return self.fp.peek(n)
    self.peek = peek

    def readline(limit=-1):
        if self.fp == None or self._method == "HEAD":
            return b""
        if self.length != None and (limit < 0 or limit > self.length):
            limit = self.length
        result = self.fp.readline(limit)
        if not result and limit:
            self._close_conn()
        elif self.length != None:
            self.length -= len(result)
        return result
    self.readline = readline

    def fileno():
        pass
    self.fileno = fileno

    def getheader(name, default=None):
        """Returns the value of the header matching *name*.

        If there are multiple matching headers, the values are
        combined into a single string separated by commas and spaces.

        If no matching header is found, returns *default* or None if
        the *default* is not specified.

        If the headers are unknown, raises http.client.ResponseNotReady.

        """
        if self.headers == None:
            fail("ResponseNotReady")
        headers = self.headers.get_all(name) or default
        if types.is_string(headers) or not hasattr(headers, "__iter__"):
            return headers
        else:
            return ", ".join(headers)
    self.getheader = getheader

    def getheaders():
        """Return list of (header, value) tuples."""
        if self.headers == None:
            fail("ResponseNotReady")
        return list(self.headers.items())
    self.getheaders = getheaders

    # We override IOBase.__iter__ so that it doesn't check for closed-ness

    def __iter__():
        return self
    self.__iter__ = __iter__

    # For compatibility with old-style urllib responses.

    def info():
        """Returns an instance of the class mimetools.Message containing
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

        """
        return self.headers
    self.info = info

    def geturl():
        """Return the real URL of the page.

        In some cases, the HTTP server redirects a client to another
        URL. The urlopen() function handles this transparently, but in
        some cases the caller needs to know which URL the client was
        redirected to. The geturl() method can be used to get at this
        redirected URL.

        """
        return self.url
    self.geturl = geturl

    def getcode():
        """Return the HTTP status code that was sent with the response,
        or None if the URL is not an HTTP URL.

        """
        return self.status
    self.getcode = getcode
    return self

