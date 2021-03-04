def BZ2File(_compression.BaseStream):
    """
    A file object providing transparent bzip2 (de)compression.

        A BZ2File can act as a wrapper for an existing file object, or refer
        directly to a named file on disk.

        Note that BZ2File provides a *binary* file interface - data read is
        returned as bytes, and data to be written should be given as bytes.
    
    """
    def __init__(self, filename, mode="r", buffering=_sentinel, compresslevel=9):
        """
        Open a bzip2-compressed file.

                If filename is a str, bytes, or PathLike object, it gives the
                name of the file to be opened. Otherwise, it should be a file
                object, which will be used to read or write the compressed data.

                mode can be 'r' for reading (default), 'w' for (over)writing,
                'x' for creating exclusively, or 'a' for appending. These can
                equivalently be given as 'rb', 'wb', 'xb', and 'ab'.

                buffering is ignored since Python 3.0. Its use is deprecated.

                If mode is 'w', 'x' or 'a', compresslevel can be a number between 1
                and 9 specifying the level of compression: 1 produces the least
                compression, and 9 (default) produces the most compression.

                If mode is 'r', the input file may be the concatenation of
                multiple compressed streams.
        
        """
    def close(self):
        """
        Flush and close the file.

                May be called more than once without error. Once the file is
                closed, any other operation on it will raise a ValueError.
        
        """
    def closed(self):
        """
        True if this file is closed.
        """
    def fileno(self):
        """
        Return the file descriptor for the underlying file.
        """
    def seekable(self):
        """
        Return whether the file supports seeking.
        """
    def readable(self):
        """
        Return whether the file was opened for reading.
        """
    def writable(self):
        """
        Return whether the file was opened for writing.
        """
    def peek(self, n=0):
        """
        Return buffered data without advancing the file position.

                Always returns at least one byte of data, unless at EOF.
                The exact number of bytes returned is unspecified.
        
        """
    def read(self, size=-1):
        """
        Read up to size uncompressed bytes from the file.

                If size is negative or omitted, read until EOF is reached.
                Returns b'' if the file is already at EOF.
        
        """
    def read1(self, size=-1):
        """
        Read up to size uncompressed bytes, while trying to avoid
                making multiple reads from the underlying stream. Reads up to a
                buffer's worth of data if size is negative.

                Returns b'' if the file is at EOF.
        
        """
    def readinto(self, b):
        """
        Read bytes into b.

                Returns the number of bytes read (0 for EOF).
        
        """
    def readline(self, size=-1):
        """
        Read a line of uncompressed bytes from the file.

                The terminating newline (if present) is retained. If size is
                non-negative, no more than size bytes will be read (in which
                case the line may be incomplete). Returns b'' if already at EOF.
        
        """
    def readlines(self, size=-1):
        """
        Read a list of lines of uncompressed bytes from the file.

                size can be specified to control the number of lines read: no
                further lines will be read once the total size of the lines read
                so far equals or exceeds size.
        
        """
    def write(self, data):
        """
        Write a byte string to the file.

                Returns the number of uncompressed bytes written, which is
                always len(data). Note that due to buffering, the file on disk
                may not reflect the data written until close() is called.
        
        """
    def writelines(self, seq):
        """
        Write a sequence of byte strings to the file.

                Returns the number of uncompressed bytes written.
                seq can be any iterable yielding byte strings.

                Line separators are not added between the written byte strings.
        
        """
    def seek(self, offset, whence=io.SEEK_SET):
        """
        Change the file position.

                The new position is specified by offset, relative to the
                position indicated by whence. Values for whence are:

                    0: start of stream (default); offset must not be negative
                    1: current stream position
                    2: end of stream; offset must not be positive

                Returns the new file position.

                Note that seeking is emulated, so depending on the parameters,
                this operation may be extremely slow.
        
        """
    def tell(self):
        """
        Return the current file position.
        """
2021-03-02 20:46:43,635 : INFO : tokenize_signature : --> do i ever get here?
def open(filename, mode="rb", compresslevel=9,
         encoding=None, errors=None, newline=None):
    """
    Open a bzip2-compressed file in binary or text mode.

        The filename argument can be an actual filename (a str, bytes, or
        PathLike object), or an existing file object to read from or write
        to.

        The mode argument can be "r", "rb", "w", "wb", "x", "xb", "a" or
        "ab" for binary mode, or "rt", "wt", "xt" or "at" for text mode.
        The default mode is "rb", and the default compresslevel is 9.

        For binary mode, this function is equivalent to the BZ2File
        constructor: BZ2File(filename, mode, compresslevel). In this case,
        the encoding, errors and newline arguments must not be provided.

        For text mode, a BZ2File object is created, and wrapped in an
        io.TextIOWrapper instance with the specified encoding, error
        handling behavior, and line ending(s).

    
    """
def compress(data, compresslevel=9):
    """
    Compress a block of data.

        compresslevel, if given, must be a number between 1 and 9.

        For incremental compression, use a BZ2Compressor object instead.
    
    """
def decompress(data):
    """
    Decompress a block of data.

        For incremental decompression, use a BZ2Decompressor object instead.
    
    """
