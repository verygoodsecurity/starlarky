2021-03-02 20:46:37,503 : INFO : tokenize_signature : --> do i ever get here?
def open(filename, mode="rb", compresslevel=_COMPRESS_LEVEL_BEST,
         encoding=None, errors=None, newline=None):
    """
    Open a gzip-compressed file in binary or text mode.

        The filename argument can be an actual filename (a str or bytes object), or
        an existing file object to read from or write to.

        The mode argument can be "r", "rb", "w", "wb", "x", "xb", "a" or "ab" for
        binary mode, or "rt", "wt", "xt" or "at" for text mode. The default mode is
        "rb", and the default compresslevel is 9.

        For binary mode, this function is equivalent to the GzipFile constructor:
        GzipFile(filename, mode, compresslevel). In this case, the encoding, errors
        and newline arguments must not be provided.

        For text mode, a GzipFile object is created, and wrapped in an
        io.TextIOWrapper instance with the specified encoding, error handling
        behavior, and line ending(s).

    
    """
def write32u(output, value):
    """
     The L format writes the bit pattern correctly whether signed
     or unsigned.

    """
def _PaddedFile:
    """
    Minimal read-only file object that prepends a string to the contents
        of an actual file. Shouldn't be used outside of gzip.py, as it lacks
        essential functionality.
    """
    def __init__(self, f, prepend=b''):
        """
        b''
        """
    def seek(self, off):
        """
         Allows fast-forwarding even in unseekable streams
        """
def BadGzipFile(OSError):
    """
    Exception raised in some cases for invalid gzip files.
    """
def GzipFile(_compression.BaseStream):
    """
    The GzipFile class simulates most of the methods of a file object with
        the exception of the truncate() method.

        This class only supports opening files in binary mode. If you need to open a
        compressed file in text mode, use the gzip.open() function.

    
    """
2021-03-02 20:46:37,507 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, filename=None, mode=None,
                 compresslevel=_COMPRESS_LEVEL_BEST, fileobj=None, mtime=None):
        """
        Constructor for the GzipFile class.

                At least one of fileobj and filename must be given a
                non-trivial value.

                The new class instance is based on fileobj, which can be a regular
                file, an io.BytesIO object, or any other object which simulates a file.
                It defaults to None, in which case filename is opened to provide
                a file object.

                When fileobj is not None, the filename argument is only used to be
                included in the gzip file header, which may include the original
                filename of the uncompressed file.  It defaults to the filename of
                fileobj, if discernible; otherwise, it defaults to the empty string,
                and in this case the original filename is not included in the header.

                The mode argument can be any of 'r', 'rb', 'a', 'ab', 'w', 'wb', 'x', or
                'xb' depending on whether the file will be read or written.  The default
                is the mode of fileobj if discernible; otherwise, the default is 'rb'.
                A mode of 'r' is equivalent to one of 'rb', and similarly for 'w' and
                'wb', 'a' and 'ab', and 'x' and 'xb'.

                The compresslevel argument is an integer from 0 to 9 controlling the
                level of compression; 1 is fastest and produces the least compression,
                and 9 is slowest and produces the most compression. 0 is no compression
                at all. The default is 9.

                The mtime argument is an optional numeric timestamp to be written
                to the last modification time field in the stream when compressing.
                If omitted or None, the current time is used.

        
        """
    def filename(self):
        """
        use the name attribute
        """
    def mtime(self):
        """
        Last modification time read from stream, or None
        """
    def __repr__(self):
        """
        '<gzip '
        """
    def _init_write(self, filename):
        """
        b
        """
    def _write_gzip_header(self, compresslevel):
        """
        b'\037\213'
        """
    def write(self,data):
        """
        write() on read-only GzipFile object
        """
    def read(self, size=-1):
        """
        read() on write-only GzipFile object
        """
    def read1(self, size=-1):
        """
        Implements BufferedIOBase.read1()

                Reads up to a buffer's worth of data if size is negative.
        """
    def peek(self, n):
        """
        peek() on write-only GzipFile object
        """
    def closed(self):
        """
         self.size may exceed 2 GiB, or even 4 GiB

        """
    def flush(self,zlib_mode=zlib.Z_SYNC_FLUSH):
        """
         Ensure the compressor's buffer is flushed

        """
    def fileno(self):
        """
        Invoke the underlying file object's fileno() method.

                This will raise AttributeError if the underlying file object
                doesn't support fileno().
        
        """
    def rewind(self):
        """
        '''Return the uncompressed stream file position indicator to the
                beginning of the file'''
        """
    def readable(self):
        """
        'Seek from end not supported'
        """
    def readline(self, size=-1):
        """
         Set flag indicating start of a new member

        """
    def _init_read(self):
        """
        b
        """
    def _read_exact(self, n):
        """
        '''Read exactly *n* bytes from `self._fp`

                This method is required because self._fp may be unbuffered,
                i.e. return short reads.
                '''
        """
    def _read_gzip_header(self):
        """
        b''
        """
    def read(self, size=-1):
        """
         size=0 is special because decompress(max_length=0) is not supported

        """
    def _add_read_data(self, data):
        """
         We've read to the end of the file
         We check the that the computed CRC and size of the
         uncompressed data matches the stored values.  Note that the size
         stored is the true file size mod 2**32.

        """
    def _rewind(self):
        """
        Compress data in one shot and return the compressed string.
            Optional argument is the compression level, in range of 0-9.
    
        """
def decompress(data):
    """
    Decompress a gzip compressed string in one shot.
        Return the decompressed string.
    
    """
def main():
    """
    A simple command line interface for the gzip module: act like gzip, 
    but do not delete the input file.
    """
