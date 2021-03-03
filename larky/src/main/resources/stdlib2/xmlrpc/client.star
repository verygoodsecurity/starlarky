def escape(s):
    """
    &
    """
def Error(Exception):
    """
    Base class for client errors.
    """
def ProtocolError(Error):
    """
    Indicates an HTTP protocol error.
    """
    def __init__(self, url, errcode, errmsg, headers):
        """
        <%s for %s: %s %s>
        """
def ResponseError(Error):
    """
    Indicates a broken response package.
    """
def Fault(Error):
    """
    Indicates an XML-RPC fault package.
    """
    def __init__(self, faultCode, faultString, **extra):
        """
        <%s %s: %r>
        """
    def _iso8601_format(value):
        """
        %Y%m%dT%H:%M:%S
        """
    def _iso8601_format(value):
        """
        %4Y%m%dT%H:%M:%S
        """
    def _iso8601_format(value):
        """
        %Y%m%dT%H:%M:%S
        """
def _strftime(value):
    """
    %04d%02d%02dT%02d:%02d:%02d
    """
def DateTime:
    """
    DateTime wrapper for an ISO 8601 string or time tuple or
        localtime integer value to generate 'dateTime.iso8601' XML-RPC
        value.
    
    """
    def __init__(self, value=0):
        """
        timetuple
        """
    def __lt__(self, other):
        """
        %Y%m%dT%H:%M:%S
        """
    def __str__(self):
        """
        <%s %r at %#x>
        """
    def decode(self, data):
        """
        <value><dateTime.iso8601>
        """
def _datetime(data):
    """
     decode xml element contents into a DateTime structure.

    """
def _datetime_type(data):
    """
    %Y%m%dT%H:%M:%S
    """
def Binary:
    """
    Wrapper for binary data.
    """
    def __init__(self, data=None):
        """
        b
        """
    def __str__(self):
        """
        latin-1
        """
    def __eq__(self, other):
        """
        <value><base64>\n
        """
def _binary(data):
    """
     decode xml element contents into a Binary structure

    """
def ExpatParser:
    """
     fast expat parser for Python 2.0 and later.

    """
    def __init__(self, target):
        """
         get rid of circular references
        """
def Marshaller:
    """
    Generate an XML-RPC params chunk from a Python data structure.

        Create a Marshaller instance for each set of parameters, and use
        the "dumps" method to convert your data (represented as a tuple)
        to an XML-RPC params chunk.  To write a fault response, pass a
        Fault instance instead.  You may prefer to use the "dumps" module
        function for this purpose.
    
    """
    def __init__(self, encoding=None, allow_none=False):
        """
         fault instance

        """
    def __dump(self, value, write):
        """
         check if this object can be marshalled as a structure

        """
    def dump_nil (self, value, write):
        """
        cannot marshal None unless allow_none is enabled
        """
    def dump_bool(self, value, write):
        """
        <value><boolean>
        """
    def dump_long(self, value, write):
        """
        int exceeds XML-RPC limits
        """
    def dump_double(self, value, write):
        """
        <value><double>
        """
    def dump_unicode(self, value, write, escape=escape):
        """
        <value><string>
        """
    def dump_bytes(self, value, write):
        """
        <value><base64>\n
        """
    def dump_array(self, value, write):
        """
        cannot marshal recursive sequences
        """
    def dump_struct(self, value, write, escape=escape):
        """
        cannot marshal recursive dictionaries
        """
    def dump_datetime(self, value, write):
        """
        <value><dateTime.iso8601>
        """
    def dump_instance(self, value, write):
        """
         check for special wrappers

        """
def Unmarshaller:
    """
    Unmarshal an XML-RPC response, based on incoming XML event
        messages (start, data, end).  Call close() to get the resulting
        data structure.

        Note that this reader is fairly tolerant, and gladly accepts bogus
        XML-RPC data without complaining (but not bogus XML).
    
    """
    def __init__(self, use_datetime=False, use_builtin_types=False):
        """
        utf-8
        """
    def close(self):
        """
         return response tuple and target method

        """
    def getmethodname(self):
        """

         event handlers


        """
    def xml(self, encoding, standalone):
        """
         FIXME: assert standalone == 1 ???


        """
    def start(self, tag, attrs):
        """
         prepare to handle this element

        """
    def data(self, text):
        """
         call the appropriate end tag handler

        """
    def end_dispatch(self, tag, data):
        """
         dispatch data

        """
    def end_nil (self, data):
        """
        nil
        """
    def end_boolean(self, data):
        """
        0
        """
    def end_int(self, data):
        """
        i1
        """
    def end_double(self, data):
        """
        double
        """
    def end_bigdecimal(self, data):
        """
        bigdecimal
        """
    def end_string(self, data):
        """
        string
        """
    def end_array(self, data):
        """
         map arrays to Python lists

        """
    def end_struct(self, data):
        """
         map structs to Python dictionaries

        """
    def end_base64(self, data):
        """
        ascii
        """
    def end_dateTime(self, data):
        """
        dateTime.iso8601
        """
    def end_value(self, data):
        """
         if we stumble upon a value element with no internal
         elements, treat it as a string element

        """
    def end_params(self, data):
        """
        params
        """
    def end_fault(self, data):
        """
        fault
        """
    def end_methodName(self, data):
        """
        methodName no params
        """
def _MultiCallMethod:
    """
     some lesser magic to store calls made to a MultiCall object
     for batch execution

    """
    def __init__(self, call_list, name):
        """
        %s.%s
        """
    def __call__(self, *args):
        """
        Iterates over the results of a multicall. Exceptions are
            raised in response to xmlrpc faults.
        """
    def __init__(self, results):
        """
        'faultCode'
        """
def MultiCall:
    """
    server -> an object used to boxcar method calls

        server should be a ServerProxy object.

        Methods can be added to the MultiCall using normal
        method call syntax e.g.:

        multicall = MultiCall(server_proxy)
        multicall.add(2,3)
        multicall.get_address("Guido")

        To execute the multicall, call the MultiCall object e.g.:

        add_result, address = multicall()
    
    """
    def __init__(self, server):
        """
        <%s at %#x>
        """
    def __getattr__(self, name):
        """
        'methodName'
        """
def getparser(use_datetime=False, use_builtin_types=False):
    """
    getparser() -> parser, unmarshaller

        Create an instance of the fastest available parser, and attach it
        to an unmarshalling object.  Return both objects.
    
    """
2021-03-02 20:54:02,508 : INFO : tokenize_signature : --> do i ever get here?
def dumps(params, methodname=None, methodresponse=None, encoding=None,
          allow_none=False):
    """
    data [,options] -> marshalled data

        Convert an argument tuple or a Fault instance to an XML-RPC
        request (or response, if the methodresponse option is used).

        In addition to the data object, the following options can be given
        as keyword arguments:

            methodname: the method name for a methodCall packet

            methodresponse: true to create a methodResponse packet.
            If this option is used with a tuple, the tuple must be
            a singleton (i.e. it can contain only one element).

            encoding: the packet encoding (default is UTF-8)

        All byte strings in the data structure are assumed to use the
        packet encoding.  Unicode strings are automatically converted,
        where necessary.
    
    """
def loads(data, use_datetime=False, use_builtin_types=False):
    """
    data -> unmarshalled data, method name

        Convert an XML-RPC packet to unmarshalled data plus a method
        name (None if not present).

        If the XML-RPC packet represents a fault condition, this function
        raises a Fault exception.
    
    """
def gzip_encode(data):
    """
    data -> gzip encoded data

        Encode data using the gzip content encoding as described in RFC 1952
    
    """
def gzip_decode(data, max_decode=20971520):
    """
    gzip encoded data -> unencoded data

        Decode data using the gzip content encoding as described in RFC 1952
    
    """
def GzipDecodedResponse(gzip.GzipFile if gzip else object):
    """
    a file-like object to decode a response encoded with the gzip
        method, as described in RFC 1952.
    
    """
    def __init__(self, response):
        """
        response doesn't support tell() and read(), required by
        GzipFile

        """
    def close(self):
        """
         --------------------------------------------------------------------
         request dispatcher


        """
def _Method:
    """
     some magic to bind an XML-RPC method to an RPC server.
     supports "nested" methods (e.g. examples.getStateName)

    """
    def __init__(self, send, name):
        """
        %s.%s
        """
    def __call__(self, *args):
        """

         Standard transport class for XML-RPC over HTTP.
         <p>
         You can create custom transports by subclassing this method, and
         overriding selected methods.


        """
def Transport:
    """
    Handles an HTTP transaction to an XML-RPC server.
    """
2021-03-02 20:54:02,511 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, use_datetime=False, use_builtin_types=False,
                 *, headers=()):
        """

         Send a complete request, and parse the response.
         Retry request if a cached connection has disconnected.

         @param host Target host.
         @param handler Target PRC handler.
         @param request_body XML-RPC request body.
         @param verbose Debugging flag.
         @return Parsed response.


        """
    def request(self, host, handler, request_body, verbose=False):
        """
        retry request once if cached connection has gone cold

        """
    def single_request(self, host, handler, request_body, verbose=False):
        """
         issue XML-RPC request

        """
    def getparser(self):
        """
         get parser and unmarshaller

        """
    def get_host_info(self, host):
        """
        utf-8
        """
    def make_connection(self, host):
        """
        return an existing connection if possible.  This allows
        HTTP/1.1 keep-alive.

        """
    def close(self):
        """

         Send HTTP request.

         @param host Host descriptor (URL or (URL, x509 info) tuple).
         @param handler Target RPC handler (a path relative to host)
         @param request_body The XML-RPC request body
         @param debug Enable debugging if debug is true.
         @return An HTTPConnection.


        """
    def send_request(self, host, handler, request_body, debug):
        """
        POST
        """
    def send_headers(self, connection, headers):
        """

         Send request body.
         This function provides a useful hook for subclassing

         @param connection httpConnection.
         @param request_body XML-RPC request body.


        """
    def send_content(self, connection, request_body):
        """
        optionally encode the request

        """
    def parse_response(self, response):
        """
         read response data from httpresponse, and parse it
         Check for new http response object, otherwise it is a file object.

        """
def SafeTransport(Transport):
    """
    Handles an HTTPS transaction to an XML-RPC server.
    """
2021-03-02 20:54:02,516 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, use_datetime=False, use_builtin_types=False,
                 *, headers=(), context=None):
        """
         FIXME: mostly untested


        """
    def make_connection(self, host):
        """
        HTTPSConnection
        """
def ServerProxy:
    """
    uri [,options] -> a logical connection to an XML-RPC server

        uri is the connection point on the server, given as
        scheme://host/target.

        The standard implementation always supports the "http" scheme.  If
        SSL socket support is available (Python 2.0), it also supports
        "https".

        If the target part and the slash preceding it are both omitted,
        "/RPC2" is assumed.

        The following options can be given as keyword arguments:

            transport: a transport factory
            encoding: the request encoding (default is UTF-8)

        All 8-bit strings passed to the server proxy are assumed to use
        the given encoding.
    
    """
2021-03-02 20:54:02,517 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:02,517 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, uri, transport=None, encoding=None, verbose=False,
                 allow_none=False, use_datetime=False, use_builtin_types=False,
                 *, headers=(), context=None):
        """
         establish a "logical" server connection

         get the url

        """
    def __close(self):
        """
         call a method on the remote server


        """
    def __repr__(self):
        """
        <%s for %s%s>
        """
    def __getattr__(self, name):
        """
         magic method dispatcher

        """
    def __call__(self, attr):
        """
        A workaround to get special attributes on the ServerProxy
                   without interfering with the magic __getattr__
        
        """
    def __enter__(self):
        """
         compatibility


        """
