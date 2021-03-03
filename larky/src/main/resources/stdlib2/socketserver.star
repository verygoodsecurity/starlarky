def BaseServer:
    """
    Base class for server classes.

        Methods for the caller:

        - __init__(server_address, RequestHandlerClass)
        - serve_forever(poll_interval=0.5)
        - shutdown()
        - handle_request()  # if you do not use serve_forever()
        - fileno() -> int   # for selector

        Methods that may be overridden:

        - server_bind()
        - server_activate()
        - get_request() -> request, client_address
        - handle_timeout()
        - verify_request(request, client_address)
        - server_close()
        - process_request(request, client_address)
        - shutdown_request(request)
        - close_request(request)
        - service_actions()
        - handle_error()

        Methods for derived classes:

        - finish_request(request, client_address)

        Class variables that may be overridden by derived classes or
        instances:

        - timeout
        - address_family
        - socket_type
        - allow_reuse_address

        Instance variables:

        - RequestHandlerClass
        - socket

    
    """
    def __init__(self, server_address, RequestHandlerClass):
        """
        Constructor.  May be extended, do not override.
        """
    def server_activate(self):
        """
        Called by constructor to activate the server.

                May be overridden.

        
        """
    def serve_forever(self, poll_interval=0.5):
        """
        Handle one request at a time until shutdown.

                Polls for shutdown every poll_interval seconds. Ignores
                self.timeout. If you need to do periodic tasks, do them in
                another thread.
        
        """
    def shutdown(self):
        """
        Stops the serve_forever loop.

                Blocks until the loop has finished. This must be called while
                serve_forever() is running in another thread, or it will
                deadlock.
        
        """
    def service_actions(self):
        """
        Called by the serve_forever() loop.

                May be overridden by a subclass / Mixin to implement any code that
                needs to be run during the loop.
        
        """
    def handle_request(self):
        """
        Handle one request, possibly blocking.

                Respects self.timeout.
        
        """
    def _handle_request_noblock(self):
        """
        Handle one request, without blocking.

                I assume that selector.select() has returned that the socket is
                readable before this function was called, so there should be no risk of
                blocking in get_request().
        
        """
    def handle_timeout(self):
        """
        Called if no new request arrives within self.timeout.

                Overridden by ForkingMixIn.
        
        """
    def verify_request(self, request, client_address):
        """
        Verify the request.  May be overridden.

                Return True if we should proceed with this request.

        
        """
    def process_request(self, request, client_address):
        """
        Call finish_request.

                Overridden by ForkingMixIn and ThreadingMixIn.

        
        """
    def server_close(self):
        """
        Called to clean-up the server.

                May be overridden.

        
        """
    def finish_request(self, request, client_address):
        """
        Finish one request by instantiating RequestHandlerClass.
        """
    def shutdown_request(self, request):
        """
        Called to shutdown and close an individual request.
        """
    def close_request(self, request):
        """
        Called to clean up an individual request.
        """
    def handle_error(self, request, client_address):
        """
        Handle an error gracefully.  May be overridden.

                The default is to print a traceback and continue.

        
        """
    def __enter__(self):
        """
        Base class for various socket-based server classes.

            Defaults to synchronous IP stream (i.e., TCP).

            Methods for the caller:

            - __init__(server_address, RequestHandlerClass, bind_and_activate=True)
            - serve_forever(poll_interval=0.5)
            - shutdown()
            - handle_request()  # if you don't use serve_forever()
            - fileno() -> int   # for selector

            Methods that may be overridden:

            - server_bind()
            - server_activate()
            - get_request() -> request, client_address
            - handle_timeout()
            - verify_request(request, client_address)
            - process_request(request, client_address)
            - shutdown_request(request)
            - close_request(request)
            - handle_error()

            Methods for derived classes:

            - finish_request(request, client_address)

            Class variables that may be overridden by derived classes or
            instances:

            - timeout
            - address_family
            - socket_type
            - request_queue_size (only for stream sockets)
            - allow_reuse_address

            Instance variables:

            - server_address
            - RequestHandlerClass
            - socket

    
        """
    def __init__(self, server_address, RequestHandlerClass, bind_and_activate=True):
        """
        Constructor.  May be extended, do not override.
        """
    def server_bind(self):
        """
        Called by constructor to bind the socket.

                May be overridden.

        
        """
    def server_activate(self):
        """
        Called by constructor to activate the server.

                May be overridden.

        
        """
    def server_close(self):
        """
        Called to clean-up the server.

                May be overridden.

        
        """
    def fileno(self):
        """
        Return socket file number.

                Interface required by selector.

        
        """
    def get_request(self):
        """
        Get the request and client address from the socket.

                May be overridden.

        
        """
    def shutdown_request(self, request):
        """
        Called to shutdown and close an individual request.
        """
    def close_request(self, request):
        """
        Called to clean up an individual request.
        """
def UDPServer(TCPServer):
    """
    UDP server class.
    """
    def get_request(self):
        """
         No need to call listen() for UDP.

        """
    def shutdown_request(self, request):
        """
         No need to shutdown anything.

        """
    def close_request(self, request):
        """
         No need to close anything.

        """
    def ForkingMixIn:
    """
    Mix-in class to handle each request in a new process.
    """
        def collect_children(self, *, blocking=False):
            """
            Internal routine to wait for children that have exited.
            """
        def handle_timeout(self):
            """
            Wait for zombies after self.timeout seconds of inactivity.

                        May be extended, do not override.
            
            """
        def service_actions(self):
            """
            Collect the zombie child processes regularly in the ForkingMixIn.

                        service_actions is called in the BaseServer's serve_forever loop.
            
            """
        def process_request(self, request, client_address):
            """
            Fork a new subprocess to process the request.
            """
        def server_close(self):
            """
            Mix-in class to handle each request in a new thread.
            """
    def process_request_thread(self, request, client_address):
        """
        Same as in BaseServer but as a thread.

                In addition, exception handling is done here.

        
        """
    def process_request(self, request, client_address):
        """
        Start a new thread to process the request.
        """
    def server_close(self):
        """
        fork
        """
    def ForkingUDPServer(ForkingMixIn, UDPServer): pass
    """
    'AF_UNIX'
    """
    def UnixStreamServer(TCPServer):
    """
    Base class for request handler classes.

        This class is instantiated for each request to be handled.  The
        constructor sets the instance variables request, client_address
        and server, and then calls the handle() method.  To implement a
        specific service, all you need to do is to derive a class which
        defines a handle() method.

        The handle() method can find the request as self.request, the
        client address as self.client_address, and the server (in case it
        needs access to per-server information) as self.server.  Since a
        separate instance is created for each request, the handle() method
        can define other arbitrary instance variables.

    
    """
    def __init__(self, request, client_address, server):
        """
         The following two classes make it possible to use the same service
         class for stream or datagram servers.
         Each class sets up these instance variables:
         - rfile: a file object from which receives the request is read
         - wfile: a file object to which the reply is written
         When the handle() method returns, wfile is flushed properly



        """
def StreamRequestHandler(BaseRequestHandler):
    """
    Define self.rfile and self.wfile for stream sockets.
    """
    def setup(self):
        """
        'rb'
        """
    def finish(self):
        """
         A final socket error may have occurred here, such as
         the local error ECONNABORTED.

        """
def _SocketWriter(BufferedIOBase):
    """
    Simple writable BufferedIOBase implementation for a socket

        Does not hold data in a buffer, avoiding any need to call flush().
    """
    def __init__(self, sock):
        """
        Define self.rfile and self.wfile for datagram sockets.
        """
    def setup(self):
