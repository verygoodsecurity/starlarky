2021-03-02 20:54:30,522 : INFO : tokenize_signature : --> do i ever get here?
async def open_connection(host=None, port=None, *,
                          loop=None, limit=_DEFAULT_LIMIT, **kwds):
        """
        A wrapper for create_connection() returning a (reader, writer) pair.

            The reader returned is a StreamReader instance; the writer is a
            StreamWriter instance.

            The arguments are all the usual arguments to create_connection()
            except protocol_factory; most common are positional host and port,
            with various optional keyword arguments following.

            Additional optional keyword arguments are loop (to set the event loop
            instance to use) and limit (to set the buffer limit passed to the
            StreamReader).

            (If you want to customize the StreamReader and/or
            StreamReaderProtocol classes, just copy the code -- there's
            really nothing special here except some convenience.)
    
        """
2021-03-02 20:54:30,523 : INFO : tokenize_signature : --> do i ever get here?
async def start_server(client_connected_cb, host=None, port=None, *,
                       loop=None, limit=_DEFAULT_LIMIT, **kwds):
        """
        Start a socket server, call back for each client connected.

            The first parameter, `client_connected_cb`, takes two parameters:
            client_reader, client_writer.  client_reader is a StreamReader
            object, while client_writer is a StreamWriter object.  This
            parameter can either be a plain callback function or a coroutine;
            if it is a coroutine, it will be automatically converted into a
            Task.

            The rest of the arguments are all the usual arguments to
            loop.create_server() except protocol_factory; most common are
            positional host and port, with various optional keyword arguments
            following.  The return value is the same as loop.create_server().

            Additional optional keyword arguments are loop (to set the event loop
            instance to use) and limit (to set the buffer limit passed to the
            StreamReader).

            The return value is the same as loop.create_server(), i.e. a
            Server object which can be used to stop the service.
    
        """
    def factory():
        """
        'AF_UNIX'
        """
2021-03-02 20:54:30,524 : INFO : tokenize_signature : --> do i ever get here?
    async def open_unix_connection(path=None, *,
                                   loop=None, limit=_DEFAULT_LIMIT, **kwds):
            """
            Similar to `open_connection` but works with UNIX Domain Sockets.
            """
2021-03-02 20:54:30,525 : INFO : tokenize_signature : --> do i ever get here?
    async def start_unix_server(client_connected_cb, path=None, *,
                                loop=None, limit=_DEFAULT_LIMIT, **kwds):
            """
            Similar to `start_server` but works with UNIX Domain Sockets.
            """
        def factory():
            """
            Reusable flow control logic for StreamWriter.drain().

                This implements the protocol methods pause_writing(),
                resume_writing() and connection_lost().  If the subclass overrides
                these it must call the super methods.

                StreamWriter.drain() must wait for _drain_helper() coroutine.
    
            """
    def __init__(self, loop=None):
        """
        %r pauses writing
        """
    def resume_writing(self):
        """
        %r resumes writing
        """
    def connection_lost(self, exc):
        """
         Wake up the writer if currently paused.

        """
    async def _drain_helper(self):
            """
            'Connection lost'
            """
    def _get_close_waiter(self, stream):
        """
        Helper class to adapt between Protocol and StreamReader.

            (This is a helper class instead of making StreamReader itself a
            Protocol subclass, because the StreamReader has other potential
            uses, and to prevent the user of the StreamReader to accidentally
            call inappropriate methods of the protocol.)
    
        """
    def __init__(self, stream_reader, client_connected_cb=None, loop=None):
        """
         This is a stream created by the `create_server()` function.
         Keep a strong reference to the reader until a connection
         is established.

        """
    def _stream_reader(self):
        """
        'message'
        """
    def connection_lost(self, exc):
        """
         Prevent a warning in SSLProtocol.eof_received:
         "returning true from eof_received()
         has no effect when using ssl

        """
    def _get_close_waiter(self, stream):
        """
         Prevent reports about unhandled exceptions.
         Better than self._closed._log_traceback = False hack

        """
def StreamWriter:
    """
    Wraps a Transport.

        This exposes write(), writelines(), [can_]write_eof(),
        get_extra_info() and close().  It adds drain() which returns an
        optional Future on which you can wait for flow control.  It also
        adds a transport property which references the Transport
        directly.
    
    """
    def __init__(self, transport, protocol, reader, loop):
        """
         drain() expects that the reader has an exception() method

        """
    def __repr__(self):
        """
        f'transport={self._transport!r}'
        """
    def transport(self):
        """
        Flush the write buffer.

                The intended use is to write

                  w.write(data)
                  await w.drain()
        
        """
def StreamReader:
    """
     The line length limit is  a security feature;
     it also doubles as half the buffer limit.


    """
    def __repr__(self):
        """
        'StreamReader'
        """
    def exception(self):
        """
        Wakeup read*() functions waiting for data or EOF.
        """
    def set_transport(self, transport):
        """
        'Transport already set'
        """
    def _maybe_resume_transport(self):
        """
        Return True if the buffer is empty and 'feed_eof' was called.
        """
    def feed_data(self, data):
        """
        'feed_data after feed_eof'
        """
    async def _wait_for_data(self, func_name):
            """
            Wait until feed_data() or feed_eof() is called.

                    If stream was paused, automatically resume it.
        
            """
    async def readline(self):
            """
            Read chunk of data from the stream until newline (b'\n') is found.

                    On success, return chunk that ends with newline. If only partial
                    line can be read due to EOF, return incomplete line without
                    terminating newline. When EOF was reached while no bytes read, empty
                    bytes object is returned.

                    If limit is reached, ValueError will be raised. In that case, if
                    newline was found, complete line including newline will be removed
                    from internal buffer. Else, internal buffer will be cleared. Limit is
                    compared against part of the line without newline.

                    If stream was paused, this function will automatically resume it if
                    needed.
        
            """
    async def readuntil(self, separator=b'\n'):
            """
            Read data from the stream until ``separator`` is found.

                    On success, the data and separator will be removed from the
                    internal buffer (consumed). Returned data will include the
                    separator at the end.

                    Configured stream limit is used to check result. Limit sets the
                    maximal length of data that can be returned, not counting the
                    separator.

                    If an EOF occurs and the complete separator is still not found,
                    an IncompleteReadError exception will be raised, and the internal
                    buffer will be reset.  The IncompleteReadError.partial attribute
                    may contain the separator partially.

                    If the data cannot be read because of over limit, a
                    LimitOverrunError exception  will be raised, and the data
                    will be left in the internal buffer, so it can be read again.
        
            """
    async def read(self, n=-1):
            """
            Read up to `n` bytes from the stream.

                    If n is not provided, or set to -1, read until EOF and return all read
                    bytes. If the EOF was received and the internal buffer is empty, return
                    an empty bytes object.

                    If n is zero, return empty bytes object immediately.

                    If n is positive, this function try to read `n` bytes, and may return
                    less or equal bytes than requested, but at least one byte. If EOF was
                    received before any byte is read, this function returns empty byte
                    object.

                    Returned value is not limited with limit, configured at stream
                    creation.

                    If stream was paused, this function will automatically resume it if
                    needed.
        
            """
    async def readexactly(self, n):
            """
            Read exactly `n` bytes.

                    Raise an IncompleteReadError if EOF is reached before `n` bytes can be
                    read. The IncompleteReadError.partial attribute of the exception will
                    contain the partial read bytes.

                    if n is zero, return empty bytes object.

                    Returned value is not limited with limit, configured at stream
                    creation.

                    If stream was paused, this function will automatically resume it if
                    needed.
        
            """
    def __aiter__(self):
        """
        b''
        """
