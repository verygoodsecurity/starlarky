def TransportSocket:
    """
    A socket-like wrapper for exposing real transport sockets.

        These objects can be safely returned by APIs like
        `transport.get_extra_info('socket')`.  All potentially disruptive
        operations (like "socket.close()") are banned.
    
    """
    def __init__(self, sock: socket.socket):
        """
        f"Using {what} on sockets returned from get_extra_info('socket') 
        f"will be prohibited in asyncio 3.9. Please report your use case 
        f"to bugs.python.org.
        """
    def family(self):
        """
        f"<asyncio.TransportSocket fd={self.fileno()}, 
        f"family={self.family!s}, type={self.type!s}, 
        f"proto={self.proto}

        """
    def __getstate__(self):
        """
        Cannot serialize asyncio.TransportSocket object
        """
    def fileno(self):
        """
         asyncio doesn't currently provide a high-level transport API
         to shutdown the connection.

        """
    def getsockopt(self, *args, **kwargs):
        """
        'accept() method'
        """
    def connect(self, *args, **kwargs):
        """
        'connect() method'
        """
    def connect_ex(self, *args, **kwargs):
        """
        'connect_ex() method'
        """
    def bind(self, *args, **kwargs):
        """
        'bind() method'
        """
    def ioctl(self, *args, **kwargs):
        """
        'ioctl() method'
        """
    def listen(self, *args, **kwargs):
        """
        'listen() method'
        """
    def makefile(self):
        """
        'makefile() method'
        """
    def sendfile(self, *args, **kwargs):
        """
        'sendfile() method'
        """
    def close(self):
        """
        'close() method'
        """
    def detach(self):
        """
        'detach() method'
        """
    def sendmsg_afalg(self, *args, **kwargs):
        """
        'sendmsg_afalg() method'
        """
    def sendmsg(self, *args, **kwargs):
        """
        'sendmsg() method'
        """
    def sendto(self, *args, **kwargs):
        """
        'sendto() method'
        """
    def send(self, *args, **kwargs):
        """
        'send() method'
        """
    def sendall(self, *args, **kwargs):
        """
        'sendall() method'
        """
    def set_inheritable(self, *args, **kwargs):
        """
        'set_inheritable() method'
        """
    def share(self, process_id):
        """
        'share() method'
        """
    def recv_into(self, *args, **kwargs):
        """
        'recv_into() method'
        """
    def recvfrom_into(self, *args, **kwargs):
        """
        'recvfrom_into() method'
        """
    def recvmsg_into(self, *args, **kwargs):
        """
        'recvmsg_into() method'
        """
    def recvmsg(self, *args, **kwargs):
        """
        'recvmsg() method'
        """
    def recvfrom(self, *args, **kwargs):
        """
        'recvfrom() method'
        """
    def recv(self, *args, **kwargs):
        """
        'recv() method'
        """
    def settimeout(self, value):
        """
        'settimeout(): only 0 timeout is allowed on transport sockets'
        """
    def gettimeout(self):
        """
        'setblocking(): transport sockets cannot be blocking'
        """
    def __enter__(self):
        """
        'context manager protocol'
        """
    def __exit__(self, *err):
        """
        'context manager protocol'
        """
