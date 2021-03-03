def format_date_time(timestamp):
    """
    %s, %02d %3s %4d %02d:%02d:%02d GMT
    """
def _needs_transcode(k):
    """
    'HTTP_'
    """
def read_environ():
    """
    Read environment, fixing HTTP variables
    """
def BaseHandler:
    """
    Manage the invocation of a WSGI application
    """
    def run(self, application):
        """
        Invoke the application
        """
    def setup_environ(self):
        """
        Set up the environment for one request
        """
    def finish_response(self):
        """
        Send any iterable data, then close self and the iterable

                Subclasses intended for use in asynchronous servers will
                want to redefine this method, such that it sets up callbacks
                in the event loop to iterate over the data, and to call
                'self.close()' once the response is finished.
        
        """
    def get_scheme(self):
        """
        Return the URL scheme being used
        """
    def set_content_length(self):
        """
        Compute Content-Length or switch to chunked encoding if possible
        """
    def cleanup_headers(self):
        """
        Make any necessary header changes or defaults

                Subclasses can extend this to add other defaults.
        
        """
    def start_response(self, status, headers,exc_info=None):
        """
        'start_response()' callable as specified by PEP 3333
        """
    def _convert_string_type(self, value, title):
        """
        Convert/check value type.
        """
    def send_preamble(self):
        """
        Transmit version/status/date/server, via self._write()
        """
    def write(self, data):
        """
        'write()' callable as specified by PEP 3333
        """
    def sendfile(self):
        """
        Platform-specific file transmission

                Override this method in subclasses to support platform-specific
                file transmission.  It is only called if the application's
                return iterable ('self.result') is an instance of
                'self.wsgi_file_wrapper'.

                This method should return a true value if it was able to actually
                transmit the wrapped file-like object using a platform-specific
                approach.  It should return a false value if normal iteration
                should be used instead.  An exception can be raised to indicate
                that transmission was attempted, but failed.

                NOTE: this method should call 'self.send_headers()' if
                'self.headers_sent' is false and it is going to attempt direct
                transmission of the file.
        
        """
    def finish_content(self):
        """
        Ensure headers and content have both been sent
        """
    def close(self):
        """
        Close the iterable (if needed) and reset all instance vars

                Subclasses may want to also drop the client connection.
        
        """
    def send_headers(self):
        """
        Transmit headers to the client, via self._write()
        """
    def result_is_file(self):
        """
        True if 'self.result' is an instance of 'self.wsgi_file_wrapper'
        """
    def client_is_modern(self):
        """
        True if client can accept status and headers
        """
    def log_exception(self,exc_info):
        """
        Log the 'exc_info' tuple in the server log

                Subclasses may override to retarget the output or change its format.
        
        """
    def handle_error(self):
        """
        Log current error, and send error output to client if possible
        """
    def error_output(self, environ, start_response):
        """
        WSGI mini-app to create error output

                By default, this just uses the 'error_status', 'error_headers',
                and 'error_body' attributes to generate an output page.  It can
                be overridden in a subclass to dynamically generate diagnostics,
                choose an appropriate message for the user's preferred language, etc.

                Note, however, that it's not recommended from a security perspective to
                spit out diagnostics to any old user; ideally, you should have to do
                something special to enable diagnostic output, which is why we don't
                include any here!
        
        """
    def _write(self,data):
        """
        Override in subclass to buffer data for send to client

                It's okay if this method actually transmits the data; BaseHandler
                just separates write and flush operations for greater efficiency
                when the underlying system actually has such a distinction.
        
        """
    def _flush(self):
        """
        Override in subclass to force sending of recent '_write()' calls

                It's okay if this method is a no-op (i.e., if '_write()' actually
                sends the data.
        
        """
    def get_stdin(self):
        """
        Override in subclass to return suitable 'wsgi.input'
        """
    def get_stderr(self):
        """
        Override in subclass to return suitable 'wsgi.errors'
        """
    def add_cgi_vars(self):
        """
        Override in subclass to insert CGI variables in 'self.environ'
        """
def SimpleHandler(BaseHandler):
    """
    Handler that's just initialized with streams, environment, etc.

        This handler subclass is intended for synchronous HTTP/1.0 origin servers,
        and handles sending the entire response output, given the correct inputs.

        Usage::

            handler = SimpleHandler(
                inp,out,err,env, multithread=False, multiprocess=True
            )
            handler.run(app)
    """
2021-03-02 20:53:46,616 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:46,617 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,stdin,stdout,stderr,environ,
        multithread=True, multiprocess=False
    ):
        """
        SimpleHandler.stdout.write() should not do partial writes
        """
    def _flush(self):
        """
        CGI-like systems using input/output/error streams and environ mapping

            Usage::

                handler = BaseCGIHandler(inp,out,err,env)
                handler.run(app)

            This handler class is useful for gateway protocols like ReadyExec and
            FastCGI, that have usable input/output/error streams and an environment
            mapping.  It's also the base class for CGIHandler, which just uses
            sys.stdin, os.environ, and so on.

            The constructor also takes keyword arguments 'multithread' and
            'multiprocess' (defaulting to 'True' and 'False' respectively) to control
            the configuration sent to the application.  It sets 'origin_server' to
            False (to enable CGI-like output), and assumes that 'wsgi.run_once' is
            False.
    
        """
def CGIHandler(BaseCGIHandler):
    """
    CGI-based invocation via sys.stdin/stdout/stderr and os.environ

        Usage::

            CGIHandler().run(app)

        The difference between this class and BaseCGIHandler is that it always
        uses 'wsgi.run_once' of 'True', 'wsgi.multithread' of 'False', and
        'wsgi.multiprocess' of 'True'.  It does not take any initialization
        parameters, but always uses 'sys.stdin', 'os.environ', and friends.

        If you need to override any of these parameters, use BaseCGIHandler
        instead.
    
    """
    def __init__(self):
        """
        CGI-based invocation with workaround for IIS path bug

            This handler should be used in preference to CGIHandler when deploying on
            Microsoft IIS without having set the config allowPathInfo option (IIS>=7)
            or metabase allowPathInfoForScriptMappings (IIS<7).
    
        """
    def __init__(self):
        """
        'PATH_INFO'
        """
