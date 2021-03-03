2021-03-02 20:46:57,423 : INFO : tokenize_signature : --> do i ever get here?
def urlopen(url, data=None, timeout=socket._GLOBAL_DEFAULT_TIMEOUT,
            *, cafile=None, capath=None, cadefault=False, context=None):
    """
    '''Open the URL url, which can be either a string or a Request object.

        *data* must be an object specifying additional data to be sent to
        the server, or None if no such data is needed.  See Request for
        details.

        urllib.request module uses HTTP/1.1 and includes a "Connection:close"
        header in its HTTP requests.

        The optional *timeout* parameter specifies a timeout in seconds for
        blocking operations like the connection attempt (if not specified, the
        global default timeout setting will be used). This only works for HTTP,
        HTTPS and FTP connections.

        If *context* is specified, it must be a ssl.SSLContext instance describing
        the various SSL options. See HTTPSConnection for more details.

        The optional *cafile* and *capath* parameters specify a set of trusted CA
        certificates for HTTPS requests. cafile should point to a single file
        containing a bundle of CA certificates, whereas capath should point to a
        directory of hashed certificate files. More information can be found in
        ssl.SSLContext.load_verify_locations().

        The *cadefault* parameter is ignored.

        This function always returns an object which can work as a context
        manager and has methods such as

        * geturl() - return the URL of the resource retrieved, commonly used to
          determine if a redirect was followed

        * info() - return the meta-information of the page, such as headers, in the
          form of an email.message_from_string() instance (see Quick Reference to
          HTTP Headers)

        * getcode() - return the HTTP status code of the response.  Raises URLError
          on errors.

        For HTTP and HTTPS URLs, this function returns a http.client.HTTPResponse
        object slightly modified. In addition to the three new methods above, the
        msg attribute contains the same information as the reason attribute ---
        the reason phrase returned by the server --- instead of the response
        headers as it is specified in the documentation for HTTPResponse.

        For FTP, file, and data URLs and requests explicitly handled by legacy
        URLopener and FancyURLopener classes, this function returns a
        urllib.response.addinfourl object.

        Note that None may be returned if no handler handles the request (though
        the default installed global OpenerDirector uses UnknownHandler to ensure
        this never happens).

        In addition, if proxy settings are detected (for example, when a *_proxy
        environment variable like http_proxy is set), ProxyHandler is default
        installed and makes sure the requests are handled through the proxy.

        '''
    """
def install_opener(opener):
    """

        Retrieve a URL into a temporary location on disk.

        Requires a URL argument. If a filename is passed, it is used as
        the temporary file location. The reporthook argument should be
        a callable that accepts a block number, a read size, and the
        total file size of the URL target. The data argument should be
        valid URL encoded data.

        If a filename is passed and the URL points to a local resource,
        the result is a copy from local file to new file.

        Returns a tuple containing the path to the newly created
        data file as well as the resulting HTTPMessage object.
    
    """
def urlcleanup():
    """
    Clean up temporary files from urlretrieve calls.
    """
def request_host(request):
    """
    Return request-host, as defined by RFC 2965.

        Variation from RFC: returned value is lowercased, for convenient
        comparison.

    
    """
def Request:
    """
    '{}#{}'
    """
    def full_url(self, url):
        """
         unwrap('<URL:type://host/path>') --> 'type://host/path'

        """
    def full_url(self):
        """
        ''
        """
    def data(self):
        """
         issue 16464
         if we change data we need to remove content-length header
         (cause it's most probably calculated for previous value)

        """
    def data(self):
        """
        unknown url type: %r
        """
    def get_method(self):
        """
        Return a string indicating the HTTP request method.
        """
    def get_full_url(self):
        """
        'https'
        """
    def has_proxy(self):
        """
         useful for something like authentication

        """
    def add_unredirected_header(self, key, val):
        """
         will not be added to a redirected request

        """
    def has_header(self, header_name):
        """
        Python-urllib/%s
        """
    def add_handler(self, handler):
        """
        add_parent
        """
    def close(self):
        """
         Only exists for backwards compatibility.

        """
    def _call_chain(self, chain, kind, meth_name, *args):
        """
         Handlers raise an exception if no one else should try to handle
         the request, or return None if they can't but another handler
         could.  Otherwise, they return the response.

        """
    def open(self, fullurl, data=None, timeout=socket._GLOBAL_DEFAULT_TIMEOUT):
        """
         accept a URL or a Request object

        """
    def _open(self, req, data=None):
        """
        'default'
        """
    def error(self, proto, *args):
        """
        'http'
        """
def build_opener(*handlers):
    """
    Create an opener object from a list of handlers.

        The opener will use several default handlers, including support
        for HTTP, FTP and when applicable HTTPS.

        If any of the handlers passed as arguments are subclasses of the
        default handlers, the default handlers will not be used.
    
    """
def BaseHandler:
    """
     Only exists for backwards compatibility

    """
    def __lt__(self, other):
        """
        handler_order
        """
def HTTPErrorProcessor(BaseHandler):
    """
    Process HTTP error responses.
    """
    def http_response(self, request, response):
        """
         According to RFC 2616, "2xx" code indicates that the client's
         request was successfully received, understood, and accepted.

        """
def HTTPDefaultErrorHandler(BaseHandler):
    """
     maximum number of redirections to any single URL
     this is needed because of the state that cookies introduce

    """
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        """
        Return a Request or None in response to a redirect.

                This is called by the http_error_30x methods when a
                redirection response is received.  If a redirection should
                take place, return a new Request to allow http_error_30x to
                perform the redirect.  Otherwise, raise HTTPError if no-one
                else should try to handle this url.  Return None if you can't
                but another Handler might.
        
        """
    def http_error_302(self, req, fp, code, msg, headers):
        """
         Some servers (incorrectly) return multiple Location headers
         (so probably same goes for URI).  Use first header.

        """
def _parse_proxy(proxy):
    """
    Return (scheme, user, password, host/port) given a URL or an authority.

        If a URL is supplied, it must have an authority (host:port) component.
        According to RFC 3986, having an authority component means the URL must
        have two slashes after the scheme.
    
    """
def ProxyHandler(BaseHandler):
    """
     Proxies must be in front

    """
    def __init__(self, proxies=None):
        """
        'keys'
        """
    def proxy_open(self, req, proxy, type):
        """
        '%s:%s'
        """
def HTTPPasswordMgr:
    """
     uri could be a single URI or a sequence

    """
    def find_user_password(self, realm, authuri):
        """
        Accept authority or URI and extract only the authority and path.
        """
    def is_suburi(self, base, test):
        """
        Check if test is below base in a URI tree

                Both args must be URIs in reduced form.
        
        """
def HTTPPasswordMgrWithDefaultRealm(HTTPPasswordMgr):
    """
     Add a default for prior auth requests

    """
    def update_authenticated(self, uri, is_authenticated=False):
        """
         uri could be a single URI or a sequence

        """
    def is_authenticated(self, authuri):
        """
         XXX this allows for multiple auth-schemes, but will stupidly pick
         the last one with a realm specified.

         allow for double- and single-quoted realm values
         (single quotes are a violation of the RFC, but appear in the wild)

        """
    def __init__(self, password_mgr=None):
        """
         parse WWW-Authenticate header: accept multiple challenges per header

        """
    def http_error_auth_reqed(self, authreq, host, req, headers):
        """
         host may be an authority (without userinfo) or a URL with an
         authority

        """
    def retry_http_basic_auth(self, host, req, realm):
        """
        %s:%s
        """
    def http_request(self, req):
        """
        'is_authenticated'
        """
    def http_response(self, req, response):
        """
        'is_authenticated'
        """
def HTTPBasicAuthHandler(AbstractBasicAuthHandler, BaseHandler):
    """
    'Authorization'
    """
    def http_error_401(self, req, fp, code, msg, headers):
        """
        'www-authenticate'
        """
def ProxyBasicAuthHandler(AbstractBasicAuthHandler, BaseHandler):
    """
    'Proxy-authorization'
    """
    def http_error_407(self, req, fp, code, msg, headers):
        """
         http_error_auth_reqed requires that there is no userinfo component in
         authority.  Assume there isn't one, since urllib.request does not (and
         should not, RFC 3986 s. 3.2.1) support requests for URLs containing
         userinfo.

        """
def AbstractDigestAuthHandler:
    """
     Digest authentication is specified in RFC 2617.

     XXX The client does not inspect the Authentication-Info header
     in a successful response.

     XXX It should be possible to test this implementation against
     a mock server that just generates a static set of challenges.

     XXX qop="auth-int" supports is shaky


    """
    def __init__(self, passwd=None):
        """
         Don't fail endlessly - if we failed once, we'll probably
         fail a second time. Hm. Unless the Password Manager is
         prompting for the information. Crap. This isn't great
         but it's better than the current 'repeat until recursion
         depth exceeded' approach <wink>

        """
    def retry_http_digest_auth(self, req, auth):
        """
        ' '
        """
    def get_cnonce(self, nonce):
        """
         The cnonce-value is an opaque
         quoted string value provided by the client and used by both client
         and server to avoid chosen plaintext attacks, to provide mutual
         authentication, and to provide some message integrity protection.
         This isn't a fabulous effort, but it's probably Good Enough.

        """
    def get_authorization(self, req, chal):
        """
        'realm'
        """
    def get_algorithm_impls(self, algorithm):
        """
         lambdas assume digest modules are imported at the top level

        """
    def get_entity_digest(self, data, chal):
        """
         XXX not implemented yet

        """
def HTTPDigestAuthHandler(BaseHandler, AbstractDigestAuthHandler):
    """
    An authentication protocol defined by RFC 2069

        Digest authentication improves on basic authentication because it
        does not transmit passwords in the clear.
    
    """
    def http_error_401(self, req, fp, code, msg, headers):
        """
        'www-authenticate'
        """
def ProxyDigestAuthHandler(BaseHandler, AbstractDigestAuthHandler):
    """
    'Proxy-Authorization'
    """
    def http_error_407(self, req, fp, code, msg, headers):
        """
        'proxy-authenticate'
        """
def AbstractHTTPHandler(BaseHandler):
    """
    'no host given'
    """
    def do_open(self, http_class, req, **http_conn_args):
        """
        Return an HTTPResponse object for the request, using http_class.

                http_class must implement the HTTPConnection API from http.client.
        
        """
def HTTPHandler(AbstractHTTPHandler):
    """
    'HTTPSConnection'
    """
    def HTTPSHandler(AbstractHTTPHandler):
    """
    'HTTPSHandler'
    """
def HTTPCookieProcessor(BaseHandler):
    """
    'unknown url type: %s'
    """
def parse_keqv_list(l):
    """
    Parse list of key=value strings where keys are not duplicated.
    """
def parse_http_list(s):
    """
    Parse lists as described by RFC 2068 Section 2.

        In particular, parse comma-separated lists where the elements of
        the list may include quoted-strings.  A quoted-string could
        contain a comma.  A non-quoted string could have quotes in the
        middle.  Neither commas nor quotes count if they are escaped.
        Only double-quotes count, not single-quotes.
    
    """
def FileHandler(BaseHandler):
    """
     Use local file or FTP depending on form of URL

    """
    def file_open(self, req):
        """
        '//'
        """
    def get_names(self):
        """
        'localhost'
        """
    def open_local_file(self, req):
        """
        'Content-type: %s\nContent-length: %d\nLast-modified: %s\n'
        """
def _safe_gethostbyname(host):
    """
    'ftp error: no host given'
    """
    def connect_ftp(self, user, passwd, host, port, dirs, timeout):
        """
         XXX would be nice to have pluggable cache strategies
         XXX this stuff is definitely not thread safe

        """
    def __init__(self):
        """
        '/'
        """
    def check_cache(self):
        """
         first check for old ones

        """
    def clear_cache(self):
        """
         data URLs as specified in RFC 2397.

         ignores POSTed data

         syntax:
         dataurl   := "data:" [ mediatype ] [ ";base64" ] "," data
         mediatype := [ type "/" subtype ] *( ";" parameter )
         data      := *urlchar
         parameter := attribute "=" value

        """
    def url2pathname(pathname):
        """
        OS-specific conversion from a relative URL of the 'file' scheme
                to a file system path; not recommended for general use.
        """
    def pathname2url(pathname):
        """
        OS-specific conversion from a file system path to a relative URL
                of the 'file' scheme; not recommended for general use.
        """
def URLopener:
    """
    Class to open URLs.
        This is a class rather than just a subroutine because we may need
        more than one set of global protocol-specific options.
        Note -- this is a base class for those who don't want the
        automatic handling of errors type 302 (relocated) and 401
        (authorization needed).
    """
    def __init__(self, proxies=None, **x509):
        """
        %(class)s style of invoking requests is deprecated. Use newer urlopen functions/methods
        """
    def __del__(self):
        """
         This code sometimes runs when the rest of this module
         has already been deleted, so it can't use any globals
         or import anything.

        """
    def addheader(self, *args):
        """
        Add a header to be used by the HTTP interface only
                e.g. u.addheader('Accept', 'sound/basic')
        """
    def open(self, fullurl, data=None):
        """
        Use URLopener().open(file) instead of open(file, 'r').
        """
    def open_unknown(self, fullurl, data=None):
        """
        Overridable interface to open unknown URL type.
        """
    def open_unknown_proxy(self, proxy, fullurl, data=None):
        """
        Overridable interface to open unknown URL type.
        """
    def retrieve(self, url, filename=None, reporthook=None, data=None):
        """
        retrieve(url) returns (filename, headers) for a local object
                or (tempfilename, headers) for a remote object.
        """
    def _open_generic_http(self, connection_factory, url, data):
        """
        Make an HTTP connection using connection_class.

                This is an internal method that should be called from
                open_http() or open_https().

                Arguments:
                - connection_factory should take a host name and return an
                  HTTPConnection instance.
                - url is the url to retrieval or a host, relative-path pair.
                - data is payload for a POST request or None.
        
        """
    def open_http(self, url, data=None):
        """
        Use HTTP protocol.
        """
    def http_error(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Handle http errors.

                Derived class can override this, or provide specific handlers
                named http_error_DDD where DDD is the 3-digit error code.
        """
    def http_error_default(self, url, fp, errcode, errmsg, headers):
        """
        Default error handler: close the connection and raise OSError.
        """
        def _https_connection(self, host):
            """
            Use HTTPS protocol.
            """
    def open_file(self, url):
        """
        Use local file or FTP depending on form of URL.
        """
    def open_local_file(self, url):
        """
        Use local file.
        """
    def open_ftp(self, url):
        """
        Use FTP protocol.
        """
    def open_data(self, url, data=None):
        """
        Use "data" URL.
        """
def FancyURLopener(URLopener):
    """
    Derived class with handlers for errors we can handle (perhaps).
    """
    def __init__(self, *args, **kwargs):
        """
        Default error handling -- don't raise an exception.
        """
    def http_error_302(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Error 302 -- relocated (temporarily).
        """
    def redirect_internal(self, url, fp, errcode, errmsg, headers, data):
        """
        'location'
        """
    def http_error_301(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Error 301 -- also relocated (permanently).
        """
    def http_error_303(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Error 303 -- also relocated (essentially identical to 302).
        """
    def http_error_307(self, url, fp, errcode, errmsg, headers, data=None):
        """
        Error 307 -- relocated, but turn POST into error.
        """
2021-03-02 20:46:57,482 : INFO : tokenize_signature : --> do i ever get here?
    def http_error_401(self, url, fp, errcode, errmsg, headers, data=None,
            retry=False):
        """
        Error 401 -- authentication required.
                This function supports Basic authentication only.
        """
2021-03-02 20:46:57,483 : INFO : tokenize_signature : --> do i ever get here?
    def http_error_407(self, url, fp, errcode, errmsg, headers, data=None,
            retry=False):
        """
        Error 407 -- proxy authentication required.
                This function supports Basic authentication only.
        """
    def retry_proxy_http_basic_auth(self, url, realm, data=None):
        """
        'http://'
        """
    def retry_proxy_https_basic_auth(self, url, realm, data=None):
        """
        'https://'
        """
    def retry_http_basic_auth(self, url, realm, data=None):
        """
        '@'
        """
    def retry_https_basic_auth(self, url, realm, data=None):
        """
        '@'
        """
    def get_user_passwd(self, host, realm, clear_cache=0):
        """
        '@'
        """
    def prompt_user_passwd(self, host, realm):
        """
        Override this in a GUI environment!
        """
def localhost():
    """
    Return the IP address of the magic hostname 'localhost'.
    """
def thishost():
    """
    Return the IP addresses of the current host.
    """
def ftperrors():
    """
    Return the set of errors raised by the FTP class.
    """
def noheaders():
    """
    Return an empty email Message object.
    """
def ftpwrapper:
    """
    Class used by open_ftp() for cache of open FTP connections.
    """
2021-03-02 20:46:57,490 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, user, passwd, host, port, dirs, timeout=None,
                 persistent=True):
        """
        '/'
        """
    def retrfile(self, file, type):
        """
        'd'
        """
    def endtransfer(self):
        """
         Proxy handling

        """
def getproxies_environment():
    """
    Return a dictionary of scheme -> proxy server URL mappings.

        Scan the environment for variables named <scheme>_proxy;
        this seems to be the standard convention.  If you need a
        different way, you can pass a proxies dictionary to the
        [Fancy]URLopener constructor.

    
    """
def proxy_bypass_environment(host, proxies=None):
    """
    Test if proxies should not be used for a particular host.

        Checks the proxy dict for the value of no_proxy, which should
        be a list of comma separated DNS suffixes, or '*' for all hosts.

    
    """
def _proxy_bypass_macosx_sysconf(host, proxy_settings):
    """

        Return True iff this host shouldn't be accessed using a proxy

        This function uses the MacOSX framework SystemConfiguration
        to fetch the proxy information.

        proxy_settings come from _scproxy._get_proxy_settings or get mocked ie:
        { 'exclude_simple': bool,
          'exceptions': ['foo.bar', '*.bar.com', '127.0.0.1', '10.1', '10.0/16']
        }
    
    """
    def ip2num(ipAddr):
        """
        '.'
        """
    def proxy_bypass_macosx_sysconf(host):
        """
        Return a dictionary of scheme -> proxy server URL mappings.

                This function uses the MacOSX framework SystemConfiguration
                to fetch the proxy information.
        
        """
    def proxy_bypass(host):
        """
        Return True, if host should be bypassed.

                Checks proxy settings gathered from the environment, if specified,
                or from the MacOSX framework SystemConfiguration.

        
        """
    def getproxies():
        """
        'nt'
        """
    def getproxies_registry():
        """
        Return a dictionary of scheme -> proxy server URL mappings.

                Win32 uses the registry to store proxies.

        
        """
    def getproxies():
        """
        Return a dictionary of scheme -> proxy server URL mappings.

                Returns settings gathered from the environment, if specified,
                or the registry.

        
        """
    def proxy_bypass_registry(host):
        """
         Std modules, so should be around - but you never know!

        """
    def proxy_bypass(host):
        """
        Return True, if host should be bypassed.

                Checks proxy settings gathered from the environment, if specified,
                or the registry.

        
        """
