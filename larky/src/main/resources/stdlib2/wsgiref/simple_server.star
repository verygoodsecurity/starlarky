def ServerHandler(SimpleHandler):
    """
    ' '
    """
def WSGIServer(HTTPServer):
    """
    BaseHTTPServer that implements the Python WSGI protocol
    """
    def server_bind(self):
        """
        Override server_bind to store the server name.
        """
    def setup_environ(self):
        """
         Set up base environment

        """
    def get_app(self):
        """
        WSGIServer/
        """
    def get_environ(self):
        """
        'SERVER_PROTOCOL'
        """
    def get_stderr(self):
        """
        Handle a single HTTP request
        """
def demo_app(environ,start_response):
    """
    Hello world!
    """
2021-03-02 20:53:47,132 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:47,132 : INFO : tokenize_signature : --> do i ever get here?
def make_server(
    host, port, app, server_class=WSGIServer, handler_class=WSGIRequestHandler
):
    """
    Create a new WSGI server listening on `host` and `port` for `app`
    """
