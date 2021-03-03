def unpickle_code(ms):
    """
    Return code object from marshal string ms.
    """
def pickle_code(co):
    """
    Return unpickle function and tuple with marshalled co code object.
    """
def dumps(obj, protocol=None):
    """
    Return pickled (or marshalled) string for obj.
    """
def CodePickler(pickle.Pickler):
    """
    '127.0.0.1'
    """
def RPCServer(socketserver.TCPServer):
    """
    Override TCPServer method, no bind() phase for connecting entity
    """
    def server_activate(self):
        """
        Override TCPServer method, connect() instead of listen()

                Due to the reversed connection, self.server_address is actually the
                address of the Idle Client to which we are connecting.

        
        """
    def get_request(self):
        """
        Override TCPServer method, return already connected socket
        """
    def handle_error(self, request, client_address):
        """
        Override TCPServer method

                Error message goes to __stderr__.  No error message if exiting
                normally or socket raised EOF.  Other exceptions not handled in
                server code will cause os._exit.

        
        """
def SocketIO(object):
    """
    override for specific exit action
    """
    def debug(self, *args):
        """
 
        """
    def register(self, oid, object):
        """
        localcall:
        """
    def remotecall(self, oid, methodname, args, kwargs):
        """
        remotecall:asynccall: 
        """
    def remotequeue(self, oid, methodname, args, kwargs):
        """
        remotequeue:asyncqueue: 
        """
    def asynccall(self, oid, methodname, args, kwargs):
        """
        CALL
        """
    def asyncqueue(self, oid, methodname, args, kwargs):
        """
        QUEUE
        """
    def asyncreturn(self, seq):
        """
        asyncreturn:%d:call getresponse(): 
        """
    def decoderesponse(self, response):
        """
        OK
        """
    def decode_interrupthook(self):
        """

        """
    def mainloop(self):
        """
        Listen on socket until I/O not ready or EOF

                pollresponse() will loop looking for seq number None, which
                never comes, and exit on EOFError.

        
        """
    def getresponse(self, myseq, wait):
        """
        OK
        """
    def _proxify(self, obj):
        """
         XXX Check for other types -- not currently needed

        """
    def _getresponse(self, myseq, wait):
        """
        _getresponse:myseq:
        """
    def newseq(self):
        """
        putmessage:%d:
        """
    def pollpacket(self, wait):
        """
        <i
        """
    def _stage1(self):
        """
        -----------------------
        """
    def pollresponse(self, myseq, wait):
        """
        Handle messages received on the socket.

                Some messages received may be asynchronous 'call' or 'queue' requests,
                and some may be responses for other threads.

                'call' requests are passed to self.localcall() with the expectation of
                immediate execution, during which time the socket is not serviced.

                'queue' requests are used for tasks (which may block or hang) to be
                processed in a different thread.  These requests are fed into
                request_queue by self.localcall().  Responses to queued requests are
                taken from response_queue and sent across the link with the associated
                sequence numbers.  Messages in the queues are (sequence_number,
                request/response) tuples and code using this module removing messages
                from the request_queue is responsible for returning the correct
                sequence number in the response_queue.

                pollresponse() will loop until a response message with the myseq
                sequence number is received, and will save other responses in
                self.responses and notify the owning thread.

        
        """
    def handle_EOF(self):
        """
        action taken upon link being closed by peer
        """
    def EOFhook(self):
        """
        Classes using rpc client/server can override to augment EOF action
        """
def RemoteObject(object):
    """
     Token mix-in class

    """
def remoteref(obj):
    """
    S Server
    """
    def __init__(self, sock, addr, svr):
        """
         cgt xxx
        """
    def handle(self):
        """
        handle() method required by socketserver
        """
    def get_remote_proxy(self, oid):
        """
        C Client
        """
    def __init__(self, address, family=socket.AF_INET, type=socket.SOCK_STREAM):
        """
        ****** Connection request from 
        """
    def get_remote_proxy(self, oid):
        """
        '__getattribute__'
        """
    def __getattributes(self):
        """
        __attributes__
        """
    def __getmethods(self):
        """
        __methods__
        """
def _getmethods(obj, methods):
    """
     Helper to get a list of methods from an object
     Adds names to dictionary argument 'methods'

    """
def _getattributes(obj, attributes):
    """
     XXX KBK 09Sep03  We need a proper unit test for this module.  Previously
                      existing test code was removed at Rev 1.27 (r34098).


    """
def displayhook(value):
    """
    Override standard display hook to use non-locale encoding
    """
