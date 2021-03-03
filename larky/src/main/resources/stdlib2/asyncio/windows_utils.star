def pipe(*, duplex=False, overlapped=(True, True), bufsize=BUFSIZE):
    """
    Like os.pipe() but with overlapped support and using handles not fds.
    """
def PipeHandle:
    """
    Wrapper for an overlapped pipe handle which is vaguely file-object like.

        The IOCP event loop can use these instead of socket objects.
    
    """
    def __init__(self, handle):
        """
        f'handle={self._handle!r}'
        """
    def handle(self):
        """
        I/O operation on closed pipe
        """
    def close(self, *, CloseHandle=_winapi.CloseHandle):
        """
        f"unclosed {self!r}
        """
    def __enter__(self):
        """
         Replacement for subprocess.Popen using overlapped pipe handles



        """
def Popen(subprocess.Popen):
    """
    Replacement for subprocess.Popen using overlapped pipe handles.

        The stdin, stdout, stderr are None or instances of PipeHandle.
    
    """
    def __init__(self, args, stdin=None, stdout=None, stderr=None, **kwds):
        """
        'universal_newlines'
        """
