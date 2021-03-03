2021-03-02 20:46:26,163 : INFO : tokenize_signature : --> do i ever get here?
def open(file, mode="r", buffering=-1, encoding=None, errors=None,
         newline=None, closefd=True, opener=None):
    """
    r"""Open file and return a stream.  Raise OSError upon failure.

        file is either a text or byte string giving the name (and the path
        if the file isn't in the current working directory) of the file to
        be opened or an integer file descriptor of the file to be
        wrapped. (If a file descriptor is given, it is closed when the
        returned I/O object is closed, unless closefd is set to False.)

        mode is an optional string that specifies the mode in which the file is
        opened. It defaults to 'r' which means open for reading in text mode. Other
        common values are 'w' for writing (truncating the file if it already
        exists), 'x' for exclusive creation of a new file, and 'a' for appending
        (which on some Unix systems, means that all writes append to the end of the
        file regardless of the current seek position). In text mode, if encoding is
        not specified the encoding used is platform dependent. (For reading and
        writing raw bytes use binary mode and leave encoding unspecified.) The
        available modes are:

        ========= ===============================================================
        Character Meaning
        --------- ---------------------------------------------------------------
        'r'       open for reading (default)
        'w'       open for writing, truncating the file first
        'x'       create a new file and open it for writing
        'a'       open for writing, appending to the end of the file if it exists
        'b'       binary mode
        't'       text mode (default)
        '+'       open a disk file for updating (reading and writing)
        'U'       universal newline mode (deprecated)
        ========= ===============================================================

        The default mode is 'rt' (open for reading text). For binary random
        access, the mode 'w+b' opens and truncates the file to 0 bytes, while
        'r+b' opens the file without truncation. The 'x' mode implies 'w' and
        raises an `FileExistsError` if the file already exists.

        Python distinguishes between files opened in binary and text modes,
        even when the underlying operating system doesn't. Files opened in
        binary mode (appending 'b' to the mode argument) return contents as
        bytes objects without any decoding. In text mode (the default, or when
        't' is appended to the mode argument), the contents of the file are
        returned as strings, the bytes having been first decoded using a
        platform-dependent encoding or using the specified encoding if given.

        'U' mode is deprecated and will raise an exception in future versions
        of Python.  It has no effect in Python 3.  Use newline to control
        universal newlines mode.

        buffering is an optional integer used to set the buffering policy.
        Pass 0 to switch buffering off (only allowed in binary mode), 1 to select
        line buffering (only usable in text mode), and an integer > 1 to indicate
        the size of a fixed-size chunk buffer.  When no buffering argument is
        given, the default buffering policy works as follows:

        * Binary files are buffered in fixed-size chunks; the size of the buffer
          is chosen using a heuristic trying to determine the underlying device's
          "block size" and falling back on `io.DEFAULT_BUFFER_SIZE`.
          On many systems, the buffer will typically be 4096 or 8192 bytes long.

        * "Interactive" text files (files for which isatty() returns True)
          use line buffering.  Other text files use the policy described above
          for binary files.

        encoding is the str name of the encoding used to decode or encode the
        file. This should only be used in text mode. The default encoding is
        platform dependent, but any encoding supported by Python can be
        passed.  See the codecs module for the list of supported encodings.

        errors is an optional string that specifies how encoding errors are to
        be handled---this argument should not be used in binary mode. Pass
        'strict' to raise a ValueError exception if there is an encoding error
        (the default of None has the same effect), or pass 'ignore' to ignore
        errors. (Note that ignoring encoding errors can lead to data loss.)
        See the documentation for codecs.register for a list of the permitted
        encoding error strings.

        newline is a string controlling how universal newlines works (it only
        applies to text mode). It can be None, '', '\n', '\r', and '\r\n'.  It works
        as follows:

        * On input, if newline is None, universal newlines mode is
          enabled. Lines in the input can end in '\n', '\r', or '\r\n', and
          these are translated into '\n' before being returned to the
          caller. If it is '', universal newline mode is enabled, but line
          endings are returned to the caller untranslated. If it has any of
          the other legal values, input lines are only terminated by the given
          string, and the line ending is returned to the caller untranslated.

        * On output, if newline is None, any '\n' characters written are
          translated to the system default line separator, os.linesep. If
          newline is '', no translation takes place. If newline is any of the
          other legal values, any '\n' characters written are translated to
          the given string.

        closedfd is a bool. If closefd is False, the underlying file descriptor will
        be kept open when the file is closed. This does not work when a file name is
        given and must be True in that case.

        The newly created file is non-inheritable.

        A custom opener can be used by passing a callable as *opener*. The
        underlying file descriptor for the file object is then obtained by calling
        *opener* with (*file*, *flags*). *opener* must return an open file
        descriptor (passing os.open as *opener* results in functionality similar to
        passing None).

        open() returns a file object whose type depends on the mode, and
        through which the standard file operations such as reading and writing
        are performed. When open() is used to open a file in a text mode ('w',
        'r', 'wt', 'rt', etc.), it returns a TextIOWrapper. When used to open
        a file in a binary mode, the returned class varies: in read binary
        mode, it returns a BufferedReader; in write binary and append binary
        modes, it returns a BufferedWriter, and in read/write mode, it returns
        a BufferedRandom.

        It is also possible to use a string or bytearray as a file for both
        reading and writing. For strings StringIO can be used like a file
        opened in a text mode, and for bytes a BytesIO can be used like a file
        opened in a binary mode.
    
    """
def _open_code_with_warning(path):
    """
    Opens the provided file with mode ``'rb'``. This function
        should be used when the intent is to treat the contents as
        executable code.

        ``path`` should be an absolute path.

        When supported by the runtime, this function can be hooked
        in order to allow embedders more control over code files.
        This functionality is not supported on the current runtime.
    
    """
def DocDescriptor:
    """
    Helper for builtins.open.__doc__
    
    """
    def __get__(self, obj, typ=None):
        """
        open(file, mode='r', buffering=-1, encoding=None, 
        errors=None, newline=None, closefd=True)\n\n
        """
def OpenWrapper:
    """
    Wrapper for builtins.open

        Trick so that open won't become a bound method when stored
        as a class variable (as dbm.dumb does).

        See initstdio() in Python/pylifecycle.c.
    
    """
    def __new__(cls, *args, **kwargs):
        """
         In normal operation, both `UnsupportedOperation`s should be bound to the
         same object.

        """
    def UnsupportedOperation(OSError, ValueError):
    """
    The abstract base class for all I/O classes, acting on streams of
        bytes. There is no public constructor.

        This class provides dummy implementations for many methods that
        derived classes can override selectively; the default implementations
        represent a file that cannot be read, written or seeked.

        Even though IOBase does not declare read or write because
        their signatures will vary, implementations and clients should
        consider those methods part of the interface. Also, implementations
        may raise UnsupportedOperation when operations they do not support are
        called.

        The basic type used for binary data read from or written to a file is
        bytes. Other bytes-like objects are accepted as method arguments too.
        Text I/O classes work with str data.

        Note that calling any method (even inquiries) on a closed stream is
        undefined. Implementations may raise OSError in this case.

        IOBase (and its subclasses) support the iterator protocol, meaning
        that an IOBase object can be iterated over yielding the lines in a
        stream.

        IOBase also supports the :keyword:`with` statement. In this example,
        fp is closed after the suite of the with statement is complete:

        with open('spam.txt', 'r') as fp:
            fp.write('Spam and eggs!')
    
    """
    def _unsupported(self, name):
        """
        Internal: raise an OSError exception for unsupported operations.
        """
    def seek(self, pos, whence=0):
        """
        Change stream position.

                Change the stream position to byte offset pos. Argument pos is
                interpreted relative to the position indicated by whence.  Values
                for whence are ints:

                * 0 -- start of stream (the default); offset should be zero or positive
                * 1 -- current stream position; offset may be negative
                * 2 -- end of stream; offset is usually negative
                Some operating systems / file systems could provide additional values.

                Return an int indicating the new absolute position.
        
        """
    def tell(self):
        """
        Return an int indicating the current stream position.
        """
    def truncate(self, pos=None):
        """
        Truncate file to size bytes.

                Size defaults to the current IO position as reported by tell().  Return
                the new size.
        
        """
    def flush(self):
        """
        Flush write buffers, if applicable.

                This is not implemented for read-only and non-blocking streams.
        
        """
    def close(self):
        """
        Flush and close the IO object.

                This method has no effect if the file is already closed.
        
        """
    def __del__(self):
        """
        Destructor.  Calls close().
        """
    def seekable(self):
        """
        Return a bool indicating whether object supports random access.

                If False, seek(), tell() and truncate() will raise OSError.
                This method may need to do a test seek().
        
        """
    def _checkSeekable(self, msg=None):
        """
        Internal: raise UnsupportedOperation if file is not seekable
        
        """
    def readable(self):
        """
        Return a bool indicating whether object was opened for reading.

                If False, read() will raise OSError.
        
        """
    def _checkReadable(self, msg=None):
        """
        Internal: raise UnsupportedOperation if file is not readable
        
        """
    def writable(self):
        """
        Return a bool indicating whether object was opened for writing.

                If False, write() and truncate() will raise OSError.
        
        """
    def _checkWritable(self, msg=None):
        """
        Internal: raise UnsupportedOperation if file is not writable
        
        """
    def closed(self):
        """
        closed: bool.  True iff the file has been closed.

                For backwards compatibility, this is a property, not a predicate.
        
        """
    def _checkClosed(self, msg=None):
        """
        Internal: raise a ValueError if file is closed
        
        """
    def __enter__(self):  # That's a forward reference
        """
         That's a forward reference
        """
    def __exit__(self, *args):
        """
        Context management protocol.  Calls close()
        """
    def fileno(self):
        """
        Returns underlying file descriptor (an int) if one exists.

                An OSError is raised if the IO object does not use a file descriptor.
        
        """
    def isatty(self):
        """
        Return a bool indicating whether this is an 'interactive' stream.

                Return False if it can't be determined.
        
        """
    def readline(self, size=-1):
        """
        r"""Read and return a line of bytes from the stream.

                If size is specified, at most size bytes will be read.
                Size should be an int.

                The line terminator is always b'\n' for binary files; for text
                files, the newlines argument to open can be used to select the line
                terminator(s) recognized.
        
        """
            def nreadahead():
                """
                b"\n
                """
            def nreadahead():
                """
                f"{size!r} is not an integer
                """
    def __iter__(self):
        """
        Return a list of lines from the stream.

                hint can be specified to control the number of lines read: no more
                lines will be read if the total size (in bytes/characters) of all
                lines so far exceeds hint.
        
        """
    def writelines(self, lines):
        """
        Write a list of lines to the stream.

                Line separators are not added, so it is usual for each of the lines
                provided to have a line separator at the end.
        
        """
def RawIOBase(IOBase):
    """
    Base class for raw binary I/O.
    """
    def read(self, size=-1):
        """
        Read and return up to size bytes, where size is an int.

                Returns an empty bytes object on EOF, or None if the object is
                set not to block and has no data to read.
        
        """
    def readall(self):
        """
        Read until EOF, using multiple read() call.
        """
    def readinto(self, b):
        """
        Read bytes into a pre-allocated bytes-like object b.

                Returns an int representing the number of bytes read (0 for EOF), or
                None if the object is set not to block and has no data to read.
        
        """
    def write(self, b):
        """
        Write the given buffer to the IO stream.

                Returns the number of bytes written, which may be less than the
                length of b in bytes.
        
        """
def BufferedIOBase(IOBase):
    """
    Base class for buffered IO objects.

        The main difference with RawIOBase is that the read() method
        supports omitting the size argument, and does not have a default
        implementation that defers to readinto().

        In addition, read(), readinto() and write() may raise
        BlockingIOError if the underlying raw stream is in non-blocking
        mode and not ready; unlike their raw counterparts, they will never
        return None.

        A typical implementation should not inherit from a RawIOBase
        implementation, but wrap one.
    
    """
    def read(self, size=-1):
        """
        Read and return up to size bytes, where size is an int.

                If the argument is omitted, None, or negative, reads and
                returns all data until EOF.

                If the argument is positive, and the underlying raw stream is
                not 'interactive', multiple raw reads may be issued to satisfy
                the byte count (unless EOF is reached first).  But for
                interactive raw streams (XXX and for pipes?), at most one raw
                read will be issued, and a short result does not imply that
                EOF is imminent.

                Returns an empty bytes array on EOF.

                Raises BlockingIOError if the underlying raw stream has no
                data at the moment.
        
        """
    def read1(self, size=-1):
        """
        Read up to size bytes with at most one read() system call,
                where size is an int.
        
        """
    def readinto(self, b):
        """
        Read bytes into a pre-allocated bytes-like object b.

                Like read(), this may issue multiple reads to the underlying raw
                stream, unless the latter is 'interactive'.

                Returns an int representing the number of bytes read (0 for EOF).

                Raises BlockingIOError if the underlying raw stream has no
                data at the moment.
        
        """
    def readinto1(self, b):
        """
        Read bytes into buffer *b*, using at most one system call

                Returns an int representing the number of bytes read (0 for EOF).

                Raises BlockingIOError if the underlying raw stream has no
                data at the moment.
        
        """
    def _readinto(self, b, read1):
        """
        'B'
        """
    def write(self, b):
        """
        Write the given bytes buffer to the IO stream.

                Return the number of bytes written, which is always the length of b
                in bytes.

                Raises BlockingIOError if the buffer is full and the
                underlying raw stream cannot accept more data at the moment.
        
        """
    def detach(self):
        """

                Separate the underlying raw stream from the buffer and return it.

                After the raw stream has been detached, the buffer is in an unusable
                state.
        
        """
def _BufferedIOMixin(BufferedIOBase):
    """
    A mixin implementation of BufferedIOBase with an underlying raw stream.

        This passes most requests on to the underlying raw stream.  It
        does *not* provide implementations of read(), readinto() or
        write().
    
    """
    def __init__(self, raw):
        """
         Positioning ###


        """
    def seek(self, pos, whence=0):
        """
        seek() returned an invalid position
        """
    def tell(self):
        """
        tell() returned an invalid position
        """
    def truncate(self, pos=None):
        """
         Flush the stream.  We're mixing buffered I/O with lower-level I/O,
         and a flush may be necessary to synch both views of the current
         file state.

        """
    def flush(self):
        """
        flush on closed file
        """
    def close(self):
        """
         may raise BlockingIOError or BrokenPipeError etc

        """
    def detach(self):
        """
        raw stream already detached
        """
    def seekable(self):
        """
        f"cannot pickle {self.__class__.__name__!r} object
        """
    def __repr__(self):
        """
        <{}.{}>
        """
    def fileno(self):
        """
        Buffered I/O implementation using an in-memory bytes buffer.
        """
    def __init__(self, initial_bytes=None):
        """
        __getstate__ on closed file
        """
    def getvalue(self):
        """
        Return the bytes value (contents) of the buffer
        
        """
    def getbuffer(self):
        """
        Return a readable and writable view of the buffer.
        
        """
    def close(self):
        """
        read from closed file
        """
    def read1(self, size=-1):
        """
        This is the same as read.
        
        """
    def write(self, b):
        """
        write to closed file
        """
    def seek(self, pos, whence=0):
        """
        seek on closed file
        """
    def tell(self):
        """
        tell on closed file
        """
    def truncate(self, pos=None):
        """
        truncate on closed file
        """
    def readable(self):
        """
        I/O operation on closed file.
        """
    def writable(self):
        """
        I/O operation on closed file.
        """
    def seekable(self):
        """
        I/O operation on closed file.
        """
def BufferedReader(_BufferedIOMixin):
    """
    BufferedReader(raw[, buffer_size])

        A buffer for a readable, sequential BaseRawIO object.

        The constructor creates a BufferedReader for the given readable raw
        stream and buffer_size. If buffer_size is omitted, DEFAULT_BUFFER_SIZE
        is used.
    
    """
    def __init__(self, raw, buffer_size=DEFAULT_BUFFER_SIZE):
        """
        Create a new buffered reader using the given readable raw IO object.
        
        """
    def readable(self):
        """
        b
        """
    def read(self, size=None):
        """
        Read size bytes.

                Returns exactly size bytes of data unless the underlying raw IO
                stream reaches EOF or if the call would block in non-blocking
                mode. If size is negative, read until EOF or until read() would
                block.
        
        """
    def _read_unlocked(self, n=None):
        """
        b
        """
    def peek(self, size=0):
        """
        Returns buffered bytes without advancing the position.

                The argument indicates a desired minimal number of bytes; we
                do at most one raw read to satisfy it.  We never return more
                than self.buffer_size.
        
        """
    def _peek_unlocked(self, n=0):
        """
        Reads up to size bytes, with at most one read() system call.
        """
    def _readinto(self, buf, read1):
        """
        Read data into *buf* with at most one system call.
        """
    def tell(self):
        """
        invalid whence value
        """
def BufferedWriter(_BufferedIOMixin):
    """
    A buffer for a writeable sequential RawIO object.

        The constructor creates a BufferedWriter for the given writeable raw
        stream. If the buffer_size is not given, it defaults to
        DEFAULT_BUFFER_SIZE.
    
    """
    def __init__(self, raw, buffer_size=DEFAULT_BUFFER_SIZE):
        """
        '"raw" argument must be writable.'
        """
    def writable(self):
        """
        can't write str to binary stream
        """
    def truncate(self, pos=None):
        """
        flush on closed file
        """
    def tell(self):
        """
        invalid whence value
        """
    def close(self):
        """
         We have to release the lock and call self.flush() (which will
         probably just re-take the lock) in case flush has been overridden in
         a subclass or the user set self.flush to something. This is the same
         behavior as the C implementation.

        """
def BufferedRWPair(BufferedIOBase):
    """
    A buffered reader and writer object together.

        A buffered reader object and buffered writer object put together to
        form a sequential IO object that can read and write. This is typically
        used with a socket or two-way pipe.

        reader and writer are RawIOBase objects that are readable and
        writeable respectively. If the buffer_size is omitted it defaults to
        DEFAULT_BUFFER_SIZE.
    
    """
    def __init__(self, reader, writer, buffer_size=DEFAULT_BUFFER_SIZE):
        """
        Constructor.

                The arguments are two RawIO instances.
        
        """
    def read(self, size=-1):
        """
        A buffered interface to random access streams.

            The constructor creates a reader and writer for a seekable stream,
            raw, given in the first argument. If the buffer_size is omitted it
            defaults to DEFAULT_BUFFER_SIZE.
    
        """
    def __init__(self, raw, buffer_size=DEFAULT_BUFFER_SIZE):
        """
        invalid whence value
        """
    def tell(self):
        """
         Use seek to flush the read buffer.

        """
    def read(self, size=None):
        """
         Undo readahead

        """
def FileIO(RawIOBase):
    """
    'r'
    """
    def __del__(self):
        """
        'unclosed file %r'
        """
    def __getstate__(self):
        """
        f"cannot pickle {self.__class__.__name__!r} object
        """
    def __repr__(self):
        """
        '%s.%s'
        """
    def _checkReadable(self):
        """
        'File not open for reading'
        """
    def _checkWritable(self, msg=None):
        """
        'File not open for writing'
        """
    def read(self, size=None):
        """
        Read at most size bytes, returned as bytes.

                Only makes one system call, so less data may be returned than requested
                In non-blocking mode, returns None if no data is available.
                Return an empty bytes object at EOF.
        
        """
    def readall(self):
        """
        Read all data from the file, returned as bytes.

                In non-blocking mode, returns as much as is immediately available,
                or None if no data is available.  Return an empty bytes object at EOF.
        
        """
    def readinto(self, b):
        """
        Same as RawIOBase.readinto().
        """
    def write(self, b):
        """
        Write bytes b to file, return number written.

                Only makes one system call, so not all of the data may be written.
                The number of bytes actually written is returned.  In non-blocking mode,
                returns None if the write would block.
        
        """
    def seek(self, pos, whence=SEEK_SET):
        """
        Move to new file position.

                Argument offset is a byte count.  Optional argument whence defaults to
                SEEK_SET or 0 (offset from start of file, offset should be >= 0); other values
                are SEEK_CUR or 1 (move relative to current position, positive or negative),
                and SEEK_END or 2 (move relative to end of file, usually negative, although
                many platforms allow seeking beyond the end of a file).

                Note that not all file objects are seekable.
        
        """
    def tell(self):
        """
        tell() -> int.  Current file position.

                Can raise OSError for non seekable files.
        """
    def truncate(self, size=None):
        """
        Truncate the file to at most size bytes.

                Size defaults to the current file position, as returned by tell().
                The current file position is changed to the value of size.
        
        """
    def close(self):
        """
        Close the file.

                A closed file cannot be used for further I/O operations.  close() may be
                called more than once without error.
        
        """
    def seekable(self):
        """
        True if file supports random-access.
        """
    def readable(self):
        """
        True if file was opened in a read mode.
        """
    def writable(self):
        """
        True if file was opened in a write mode.
        """
    def fileno(self):
        """
        Return the underlying file descriptor (an integer).
        """
    def isatty(self):
        """
        True if the file is connected to a TTY device.
        """
    def closefd(self):
        """
        True if the file descriptor will be closed by close().
        """
    def mode(self):
        """
        String giving the file mode
        """
def TextIOBase(IOBase):
    """
    Base class for text I/O.

        This class provides a character and line based interface to stream
        I/O. There is no public constructor.
    
    """
    def read(self, size=-1):
        """
        Read at most size characters from stream, where size is an int.

                Read from underlying buffer until we have size characters or we hit EOF.
                If size is negative or omitted, read until EOF.

                Returns a string.
        
        """
    def write(self, s):
        """
        Write string s to stream and returning an int.
        """
    def truncate(self, pos=None):
        """
        Truncate size to pos, where pos is an int.
        """
    def readline(self):
        """
        Read until newline or EOF.

                Returns an empty string if EOF is hit immediately.
        
        """
    def detach(self):
        """

                Separate the underlying buffer from the TextIOBase and return it.

                After the underlying buffer has been detached, the TextIO is in an
                unusable state.
        
        """
    def encoding(self):
        """
        Subclasses should override.
        """
    def newlines(self):
        """
        Line endings translated so far.

                Only line endings translated during reading are considered.

                Subclasses should override.
        
        """
    def errors(self):
        """
        Error setting of the decoder or encoder.

                Subclasses should override.
        """
def IncrementalNewlineDecoder(codecs.IncrementalDecoder):
    """
    r"""Codec used when reading a file in universal newlines mode.  It wraps
        another incremental decoder, translating \r\n and \r into \n.  It also
        records the types of newlines encountered.  When used with
        translate=False, it ensures that the newline sequence is returned in
        one piece.
    
    """
    def __init__(self, decoder, translate, errors='strict'):
        """
         decode input (with the eventual \r from a previous pass)

        """
    def getstate(self):
        """
        b
        """
    def setstate(self, state):
        """
        \n
        """
def TextIOWrapper(TextIOBase):
    """
    r"""Character and line based layer over a BufferedIOBase object, buffer.

        encoding gives the name of the encoding that the stream will be
        decoded or encoded with. It defaults to locale.getpreferredencoding(False).

        errors determines the strictness of encoding and decoding (see the
        codecs.register) and defaults to "strict".

        newline can be None, '', '\n', '\r', or '\r\n'.  It controls the
        handling of line endings. If it is None, universal newlines is
        enabled.  With this enabled, on input, the lines endings '\n', '\r',
        or '\r\n' are translated to '\n' before being returned to the
        caller. Conversely, on output, '\n' is translated to the system
        default line separator, os.linesep. If newline is any other of its
        legal values, that newline becomes the newline when the file is read
        and it is returned untranslated. On output, '\n' is converted to the
        newline.

        If line_buffering is True, a call to flush is implied when a call to
        write contains a newline character.
    
    """
2021-03-02 20:46:26,201 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, buffer, encoding=None, errors=None, newline=None,
                 line_buffering=False, write_through=False):
        """
         Importing locale may fail if Python is being built

        """
    def _check_newline(self, newline):
        """
        illegal newline type: %r
        """
2021-03-02 20:46:26,203 : INFO : tokenize_signature : --> do i ever get here?
    def _configure(self, encoding=None, errors=None, newline=None,
                   line_buffering=False, write_through=False):
        """
        ''
        """
    def __repr__(self):
        """
        <{}.{}
        """
    def encoding(self):
        """
        Reconfigure the text stream with new parameters.

                This also flushes the stream.
        
        """
    def seekable(self):
        """
        I/O operation on closed file.
        """
    def readable(self):
        """
        'Write data, where s is a str'
        """
    def _get_encoder(self):
        """
         The following three methods implement an ADT for _decoded_chars.
         Text returned from the decoder is buffered here until the client
         requests it by calling our read() or readline() method.

        """
    def _set_decoded_chars(self, chars):
        """
        Set the _decoded_chars buffer.
        """
    def _get_decoded_chars(self, n=None):
        """
        Advance into the _decoded_chars buffer.
        """
    def _rewind_decoded_chars(self, n):
        """
        Rewind the _decoded_chars buffer.
        """
    def _read_chunk(self):
        """

                Read and decode the next chunk of data from the BufferedReader.
        
        """
2021-03-02 20:46:26,209 : INFO : tokenize_signature : --> do i ever get here?
    def _pack_cookie(self, position, dec_flags=0,
                           bytes_to_feed=0, need_eof=0, chars_to_skip=0):
        """
         The meaning of a tell() cookie is: seek to position, set the
         decoder flags to dec_flags, read bytes_to_feed bytes, feed them
         into the decoder with need_eof as the EOF flag, then skip
         chars_to_skip characters of the decoded result.  For most simple
         decoders, tell() will often just give a byte offset in the file.

        """
    def _unpack_cookie(self, bigint):
        """
        underlying stream is not seekable
        """
    def truncate(self, pos=None):
        """
        buffer is already detached
        """
    def seek(self, cookie, whence=0):
        """
        Reset the encoder (merely useful for proper BOM handling)
        """
    def read(self, size=None):
        """
        f"{size!r} is not an integer
        """
    def __next__(self):
        """
        read from closed file
        """
    def newlines(self):
        """
        Text I/O implementation using an in-memory buffer.

            The initial_value argument sets the value of object.  The newline
            argument is like the one of TextIOWrapper's constructor.
    
        """
    def __init__(self, initial_value="", newline="\n"):
        """
        utf-8
        """
    def getvalue(self):
        """
         TextIOWrapper tells the encoding in its repr. In StringIO,
         that's an implementation detail.

        """
    def errors(self):
        """
         This doesn't make sense on StringIO.

        """
