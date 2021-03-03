def LZMAFile(_compression.BaseStream):
    """
    A file object providing transparent LZMA (de)compression.

        An LZMAFile can act as a wrapper for an existing file object, or
        refer directly to a named file on disk.

        Note that LZMAFile provides a *binary* file interface - data read
        is returned as bytes, and data to be written must be given as bytes.
    
    """
2021-03-02 20:46:36,186 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, filename=None, mode="r", *,
                 format=None, check=-1, preset=None, filters=None):
        """
        Open an LZMA-compressed file in binary mode.

                filename can be either an actual file name (given as a str,
                bytes, or PathLike object), in which case the named file is
                opened, or it can be an existing file object to read from or
                write to.

                mode can be "r" for reading (default), "w" for (over)writing,
                "x" for creating exclusively, or "a" for appending. These can
                equivalently be given as "rb", "wb", "xb" and "ab" respectively.

                format specifies the container format to use for the file.
                If mode is "r", this defaults to FORMAT_AUTO. Otherwise, the
                default is FORMAT_XZ.

                check specifies the integrity check to use. This argument can
                only be used when opening a file for writing. For FORMAT_XZ,
                the default is CHECK_CRC64. FORMAT_ALONE and FORMAT_RAW do not
                support integrity checks - for these formats, check must be
                omitted, or be CHECK_NONE.

                When opening a file for reading, the *preset* argument is not
                meaningful, and should be omitted. The *filters* argument should
                also be omitted, except when format is FORMAT_RAW (in which case
                it is required).

                When opening a file for writing, the settings used by the
                compressor can be specified either as a preset compression
                level (with the *preset* argument), or in detail as a custom
                filter chain (with the *filters* argument). For FORMAT_XZ and
                FORMAT_ALONE, the default is to use the PRESET_DEFAULT preset
                level. For FORMAT_RAW, the caller must always specify a filter
                chain; the raw compressor does not support preset compression
                levels.

                preset (if provided) should be an integer in the range 0-9,
                optionally OR-ed with the constant PRESET_EXTREME.

                filters (if provided) should be a sequence of dicts. Each dict
                should have an entry for "id" indicating ID of the filter, plus
                additional entries for options to the filter.
        
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
    def peek(self, size=-1):
        """
        Return buffered data without advancing the file position.

                Always returns at least one byte of data, unless at EOF.
                The exact number of bytes returned is unspecified.
        
        """
    def read(self, size=-1):
        """
        Read up to size uncompressed bytes from the file.

                If size is negative or omitted, read until EOF is reached.
                Returns b"" if the file is already at EOF.
        
        """
    def read1(self, size=-1):
        """
        Read up to size uncompressed bytes, while trying to avoid
                making multiple reads from the underlying stream. Reads up to a
                buffer's worth of data if size is negative.

                Returns b"" if the file is at EOF.
        
        """
    def readline(self, size=-1):
        """
        Read a line of uncompressed bytes from the file.

                The terminating newline (if present) is retained. If size is
                non-negative, no more than size bytes will be read (in which
                case the line may be incomplete). Returns b'' if already at EOF.
        
        """
    def write(self, data):
        """
        Write a bytes object to the file.

                Returns the number of uncompressed bytes written, which is
                always len(data). Note that due to buffering, the file on disk
                may not reflect the data written until close() is called.
        
        """
    def seek(self, offset, whence=io.SEEK_SET):
        """
        Change the file position.

                The new position is specified by offset, relative to the
                position indicated by whence. Possible values for whence are:

                    0: start of stream (default): offset must not be negative
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
2021-03-02 20:46:36,190 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:36,190 : INFO : tokenize_signature : --> do i ever get here?
def open(filename, mode="rb", *,
         format=None, check=-1, preset=None, filters=None,
         encoding=None, errors=None, newline=None):
    """
    Open an LZMA-compressed file in binary or text mode.

        filename can be either an actual file name (given as a str, bytes,
        or PathLike object), in which case the named file is opened, or it
        can be an existing file object to read from or write to.

        The mode argument can be "r", "rb" (default), "w", "wb", "x", "xb",
        "a", or "ab" for binary mode, or "rt", "wt", "xt", or "at" for text
        mode.

        The format, check, preset and filters arguments specify the
        compression settings, as for LZMACompressor, LZMADecompressor and
        LZMAFile.

        For binary mode, this function is equivalent to the LZMAFile
        constructor: LZMAFile(filename, mode, ...). In this case, the
        encoding, errors and newline arguments must not be provided.

        For text mode, an LZMAFile object is created, and wrapped in an
        io.TextIOWrapper instance with the specified encoding, error
        handling behavior, and line ending(s).

    
    """
def compress(data, format=FORMAT_XZ, check=-1, preset=None, filters=None):
    """
    Compress a block of data.

        Refer to LZMACompressor's docstring for a description of the
        optional arguments *format*, *check*, *preset* and *filters*.

        For incremental compression, use an LZMACompressor instead.
    
    """
def decompress(data, format=FORMAT_AUTO, memlimit=None, filters=None):
    """
    Decompress a block of data.

        Refer to LZMADecompressor's docstring for a description of the
        optional arguments *format*, *check* and *filters*.

        For incremental decompression, use an LZMADecompressor instead.
    
    """
