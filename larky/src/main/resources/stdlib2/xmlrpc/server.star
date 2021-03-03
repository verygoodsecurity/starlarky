def resolve_dotted_attribute(obj, attr, allow_dotted_names=True):
    """
    resolve_dotted_attribute(a, 'b.c.d') => a.b.c.d

        Resolves a dotted attribute name to an object.  Raises
        an AttributeError if any attribute in the chain starts with a '_'.

        If the optional allow_dotted_names argument is false, dots are not
        supported and this function operates similar to getattr(obj, attr).
    
    """
def list_public_methods(obj):
    """
    Returns a list of attribute strings, found in the specified
        object, which represent callable attributes
    """
def SimpleXMLRPCDispatcher:
    """
    Mix-in class that dispatches XML-RPC requests.

        This class is used to register XML-RPC method handlers
        and then to dispatch them. This class doesn't need to be
        instanced directly when used by SimpleXMLRPCServer but it
        can be instanced when used by the MultiPathXMLRPCServer
    
    """
2021-03-02 20:54:02,299 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, allow_none=False, encoding=None,
                 use_builtin_types=False):
        """
        'utf-8'
        """
    def register_instance(self, instance, allow_dotted_names=False):
        """
        Registers an instance to respond to XML-RPC requests.

                Only one instance can be installed at a time.

                If the registered instance has a _dispatch method then that
                method will be called with the name of the XML-RPC method and
                its parameters as a tuple
                e.g. instance._dispatch('add',(2,3))

                If the registered instance does not have a _dispatch method
                then the instance will be searched to find a matching method
                and, if found, will be called. Methods beginning with an '_'
                are considered private and will not be called by
                SimpleXMLRPCServer.

                If a registered function matches an XML-RPC request, then it
                will be called instead of the registered instance.

                If the optional allow_dotted_names argument is true and the
                instance does not have a _dispatch method, method names
                containing dots are supported and resolved, as long as none of
                the name segments start with an '_'.

                    *** SECURITY WARNING: ***

                    Enabling the allow_dotted_names options allows intruders
                    to access your module's global variables and may allow
                    intruders to execute arbitrary code on your machine.  Only
                    use this option on a secure, closed network.

        
        """
    def register_function(self, function=None, name=None):
        """
        Registers a function to respond to XML-RPC requests.

                The optional name argument can be used to set a Unicode name
                for the function.
        
        """
    def register_introspection_functions(self):
        """
        Registers the XML-RPC introspection methods in the system
                namespace.

                see http://xmlrpc.usefulinc.com/doc/reserved.html
        
        """
    def register_multicall_functions(self):
        """
        Registers the XML-RPC multicall method in the system
                namespace.

                see http://www.xmlrpc.com/discuss/msgReader$1208
        """
    def _marshaled_dispatch(self, data, dispatch_method = None, path = None):
        """
        Dispatches an XML-RPC method from marshalled (XML) data.

                XML-RPC methods are dispatched from the marshalled (XML) data
                using the _dispatch method and the result is returned as
                marshalled data. For backwards compatibility, a dispatch
                function can be provided as an argument (see comment in
                SimpleXMLRPCRequestHandler.do_POST) but overriding the
                existing method through subclassing is the preferred means
                of changing method dispatch behavior.
        
        """
    def system_listMethods(self):
        """
        system.listMethods() => ['add', 'subtract', 'multiple']

                Returns a list of the methods supported by the server.
        """
    def system_methodSignature(self, method_name):
        """
        system.methodSignature('add') => [double, int, int]

                Returns a list describing the signature of the method. In the
                above example, the add method takes two integers as arguments
                and returns a double result.

                This server does NOT support system.methodSignature.
        """
    def system_methodHelp(self, method_name):
        """
        system.methodHelp('add') => "Adds two integers together"

                Returns a string containing documentation for the specified method.
        """
    def system_multicall(self, call_list):
        """
        system.multicall([{'methodName': 'add', 'params': [2, 2]}, ...]) => \
        [[4], ...]

                Allows the caller to package multiple XML-RPC calls into a single
                request.

                See http://www.xmlrpc.com/discuss/msgReader$1208
        
        """
    def _dispatch(self, method, params):
        """
        Dispatches the XML-RPC method.

                XML-RPC calls are forwarded to a registered function that
                matches the called XML-RPC method name. If no such function
                exists then the call is forwarded to the registered instance,
                if available.

                If the registered instance has a _dispatch method then that
                method will be called with the name of the XML-RPC method and
                its parameters as a tuple
                e.g. instance._dispatch('add',(2,3))

                If the registered instance does not have a _dispatch method
                then the instance will be searched to find a matching method
                and, if found, will be called.

                Methods beginning with an '_' are considered private and will
                not be called.
        
        """
def SimpleXMLRPCRequestHandler(BaseHTTPRequestHandler):
    """
    Simple XML-RPC request handler class.

        Handles all HTTP POST requests and attempts to decode them as
        XML-RPC requests.
    
    """
    def accept_encodings(self):
        """
        Accept-Encoding
        """
    def is_rpc_path_valid(self):
        """
         If .rpc_paths is empty, just assume all paths are legal

        """
    def do_POST(self):
        """
        Handles the HTTP POST request.

                Attempts to interpret all HTTP POST requests as XML-RPC calls,
                which are forwarded to the server's _dispatch method for handling.
        
        """
    def decode_request_content(self, data):
        """
        support gzip encoding of request

        """
    def report_404 (self):
        """
         Report a 404 error

        """
    def log_request(self, code='-', size='-'):
        """
        Selectively log an accepted request.
        """
2021-03-02 20:54:02,307 : INFO : tokenize_signature : --> do i ever get here?
def SimpleXMLRPCServer(socketserver.TCPServer,
                         SimpleXMLRPCDispatcher):
    """
    Simple XML-RPC server.

        Simple XML-RPC server that allows functions and a single instance
        to be installed to handle requests. The default implementation
        attempts to dispatch XML-RPC calls to the functions or instance
        installed in the server. Override the _dispatch method inherited
        from SimpleXMLRPCDispatcher to change this behavior.
    
    """
2021-03-02 20:54:02,307 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:02,308 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, addr, requestHandler=SimpleXMLRPCRequestHandler,
                 logRequests=True, allow_none=False, encoding=None,
                 bind_and_activate=True, use_builtin_types=False):
        """
        Multipath XML-RPC Server
            This specialization of SimpleXMLRPCServer allows the user to create
            multiple Dispatcher instances and assign them to different
            HTTP request paths.  This makes it possible to run two or more
            'virtual XML-RPC servers' at the same port.
            Make sure that the requestHandler accepts the paths in question.
    
        """
2021-03-02 20:54:02,308 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:02,308 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, addr, requestHandler=SimpleXMLRPCRequestHandler,
                 logRequests=True, allow_none=False, encoding=None,
                 bind_and_activate=True, use_builtin_types=False):
        """
        'utf-8'
        """
    def add_dispatcher(self, path, dispatcher):
        """
         report low level exception back to server
         (each dispatcher should have handled their own
         exceptions)

        """
def CGIXMLRPCRequestHandler(SimpleXMLRPCDispatcher):
    """
    Simple handler for XML-RPC data passed through CGI.
    """
    def __init__(self, allow_none=False, encoding=None, use_builtin_types=False):
        """
        Handle a single XML-RPC request
        """
    def handle_get(self):
        """
        Handle a single HTTP GET request.

                Default implementation indicates an error because
                XML-RPC uses the POST method.
        
        """
    def handle_request(self, request_text=None):
        """
        Handle a single XML-RPC request passed through a CGI post method.

                If no XML data is given then it is read from stdin. The resulting
                XML-RPC response is printed to stdout along with the correct HTTP
                headers.
        
        """
def ServerHTMLDoc(pydoc.HTMLDoc):
    """
    Class used to generate pydoc HTML document for a server
    """
    def markup(self, text, escape=None, funcs={}, classes={}, methods={}):
        """
        Mark up some plain text, given a context of symbols to look for.
                Each context dictionary maps object names to anchor names.
        """
2021-03-02 20:54:02,312 : INFO : tokenize_signature : --> do i ever get here?
    def docroutine(self, object, name, mod=None,
                   funcs={}, classes={}, methods={}, cl=None):
        """
        Produce HTML documentation for a function or method object.
        """
    def docserver(self, server_name, package_documentation, methods):
        """
        Produce HTML documentation for an XML-RPC server.
        """
def XMLRPCDocGenerator:
    """
    Generates documentation for an XML-RPC server.

        This class is designed as mix-in and should not
        be constructed directly.
    
    """
    def __init__(self):
        """
         setup variables used for HTML documentation

        """
    def set_server_title(self, server_title):
        """
        Set the HTML title of the generated server documentation
        """
    def set_server_name(self, server_name):
        """
        Set the name of the generated HTML server documentation
        """
    def set_server_documentation(self, server_documentation):
        """
        Set the documentation string for the entire server.
        """
    def generate_html_documentation(self):
        """
        generate_html_documentation() => html documentation for the server

                Generates HTML documentation for the server using introspection for
                installed functions and instances that do not implement the
                _dispatch method. Alternatively, instances can choose to implement
                the _get_method_argstring(method_name) method to provide the
                argument string used in the documentation and the
                _methodHelp(method_name) method to provide the help text used
                in the documentation.
        """
def DocXMLRPCRequestHandler(SimpleXMLRPCRequestHandler):
    """
    XML-RPC and documentation request handler class.

        Handles all HTTP POST requests and attempts to decode them as
        XML-RPC requests.

        Handles all HTTP GET requests and interprets them as requests
        for documentation.
    
    """
    def do_GET(self):
        """
        Handles the HTTP GET request.

                Interpret all HTTP GET requests as requests for server
                documentation.
        
        """
2021-03-02 20:54:02,315 : INFO : tokenize_signature : --> do i ever get here?
def DocXMLRPCServer(  SimpleXMLRPCServer,
                        XMLRPCDocGenerator):
    """
    XML-RPC and HTML documentation server.

        Adds the ability to serve server documentation to the capabilities
        of SimpleXMLRPCServer.
    
    """
2021-03-02 20:54:02,315 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:02,315 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, addr, requestHandler=DocXMLRPCRequestHandler,
                 logRequests=True, allow_none=False, encoding=None,
                 bind_and_activate=True, use_builtin_types=False):
        """
        Handler for XML-RPC data and documentation requests passed through
            CGI
        """
    def handle_get(self):
        """
        Handles the HTTP GET request.

                Interpret all HTTP GET requests as requests for server
                documentation.
        
        """
    def __init__(self):
        """
        '__main__'
        """
    def ExampleService:
    """
    '42'
    """
        def currentTime:
    """
    localhost
    """
