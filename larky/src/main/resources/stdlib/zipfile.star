load("@stdlib//builtins", builtins="builtins")
load("@stdlib//base64", base64="base64")
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//bz2", bz2="bz2")
load("@stdlib//io", io="io")
load("@stdlib//larky", larky="larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION")
load("@stdlib//struct", struct="struct")
load("@stdlib//zlib", zlib="zlib")

lzma = None

crc32 = zlib.crc32

ZIP64_LIMIT = (1 << 31) - 1
ZIP_FILECOUNT_LIMIT = (1 << 16) - 1
ZIP_MAX_COMMENT = (1 << 16) - 1

# constants for Zip file compression methods
ZIP_STORED = 0
ZIP_DEFLATED = 8
ZIP_BZIP2 = 12
ZIP_LZMA = 14
# Other ZIP compression methods not supported

DEFAULT_VERSION = 20
ZIP64_VERSION = 45
BZIP2_VERSION = 46
LZMA_VERSION = 63
# we recognize (but not necessarily support) all features up to that version
MAX_EXTRACT_VERSION = 63

# Below are some formats and associated data for reading/writing headers using
# the struct module.  The names and structures of headers/records are those used
# in the PKWARE description of the ZIP file format:
#     http://www.pkware.com/documents/casestudies/APPNOTE.TXT
# (URL valid as of January 2008)

# The "end of central directory" structure, magic number, size, and indices
# (section V.I in the format document)
structEndArchive = b"<4s4H2LH"
stringEndArchive = b"PK\005\006"
sizeEndCentDir = struct.calcsize(structEndArchive)

_ECD_SIGNATURE = 0
_ECD_DISK_NUMBER = 1
_ECD_DISK_START = 2
_ECD_ENTRIES_THIS_DISK = 3
_ECD_ENTRIES_TOTAL = 4
_ECD_SIZE = 5
_ECD_OFFSET = 6
_ECD_COMMENT_SIZE = 7
# These last two indices are not part of the structure as defined in the
# spec, but they are used internally by this module as a convenience
_ECD_COMMENT = 8
_ECD_LOCATION = 9

# The "central directory" structure, magic number, size, and indices
# of entries in the structure (section V.F in the format document)
structCentralDir = "<4s4B4HL2L5H2L"
stringCentralDir = b"PK\001\002"
sizeCentralDir = struct.calcsize(structCentralDir)

# indexes of entries in the central directory structure
_CD_SIGNATURE = 0
_CD_CREATE_VERSION = 1
_CD_CREATE_SYSTEM = 2
_CD_EXTRACT_VERSION = 3
_CD_EXTRACT_SYSTEM = 4
_CD_FLAG_BITS = 5
_CD_COMPRESS_TYPE = 6
_CD_TIME = 7
_CD_DATE = 8
_CD_CRC = 9
_CD_COMPRESSED_SIZE = 10
_CD_UNCOMPRESSED_SIZE = 11
_CD_FILENAME_LENGTH = 12
_CD_EXTRA_FIELD_LENGTH = 13
_CD_COMMENT_LENGTH = 14
_CD_DISK_NUMBER_START = 15
_CD_INTERNAL_FILE_ATTRIBUTES = 16
_CD_EXTERNAL_FILE_ATTRIBUTES = 17
_CD_LOCAL_HEADER_OFFSET = 18

# General purpose bit flags
# Zip Appnote: 4.4.4 general purpose bit flag: (2 bytes)
_MASK_ENCRYPTED = 1 << 0
# Bits 1 and 2 have different meanings depending on the compression used.
_MASK_COMPRESS_OPTION_1 = 1 << 1
# _MASK_COMPRESS_OPTION_2 = 1 << 2
# _MASK_USE_DATA_DESCRIPTOR: If set, crc-32, compressed size and uncompressed
# size are zero in the local header and the real values are written in the data
# descriptor immediately following the compressed data.
_MASK_USE_DATA_DESCRIPTOR = 1 << 3
# Bit 4: Reserved for use with compression method 8, for enhanced deflating.
# _MASK_RESERVED_BIT_4 = 1 << 4
_MASK_COMPRESSED_PATCH = 1 << 5
_MASK_STRONG_ENCRYPTION = 1 << 6
# _MASK_UNUSED_BIT_7 = 1 << 7
# _MASK_UNUSED_BIT_8 = 1 << 8
# _MASK_UNUSED_BIT_9 = 1 << 9
# _MASK_UNUSED_BIT_10 = 1 << 10
_MASK_UTF_FILENAME = 1 << 11
# Bit 12: Reserved by PKWARE for enhanced compression.
# _MASK_RESERVED_BIT_12 = 1 << 12
# _MASK_ENCRYPTED_CENTRAL_DIR = 1 << 13
# Bit 14, 15: Reserved by PKWARE
# _MASK_RESERVED_BIT_14 = 1 << 14
# _MASK_RESERVED_BIT_15 = 1 << 15

# The "local file header" structure, magic number, size, and indices
# (section V.A in the format document)
structFileHeader = "<4s2B4HL2L2H"
stringFileHeader = b"PK\003\004"
sizeFileHeader = struct.calcsize(structFileHeader)

_FH_SIGNATURE = 0
_FH_EXTRACT_VERSION = 1
_FH_EXTRACT_SYSTEM = 2
_FH_GENERAL_PURPOSE_FLAG_BITS = 3
_FH_COMPRESSION_METHOD = 4
_FH_LAST_MOD_TIME = 5
_FH_LAST_MOD_DATE = 6
_FH_CRC = 7
_FH_COMPRESSED_SIZE = 8
_FH_UNCOMPRESSED_SIZE = 9
_FH_FILENAME_LENGTH = 10
_FH_EXTRA_FIELD_LENGTH = 11

# The "Zip64 end of central directory locator" structure, magic number, and size
structEndArchive64Locator = "<4sLQL"
stringEndArchive64Locator = b"PK\x06\x07"
sizeEndCentDir64Locator = struct.calcsize(structEndArchive64Locator)

# The "Zip64 end of central directory" record, magic number, size, and indices
# (section V.G in the format document)
structEndArchive64 = "<4sQ2H2L4Q"
stringEndArchive64 = b"PK\x06\x06"
sizeEndCentDir64 = struct.calcsize(structEndArchive64)

_CD64_SIGNATURE = 0
_CD64_DIRECTORY_RECSIZE = 1
_CD64_CREATE_VERSION = 2
_CD64_EXTRACT_VERSION = 3
_CD64_DISK_NUMBER = 4
_CD64_DISK_NUMBER_START = 5
_CD64_NUMBER_ENTRIES_THIS_DISK = 6
_CD64_NUMBER_ENTRIES_TOTAL = 7
_CD64_DIRECTORY_SIZE = 8
_CD64_OFFSET_START_CENTDIR = 9

_DD_SIGNATURE = 0x08074b50

compressor_names = {
    0: 'store',
    1: 'shrink',
    2: 'reduce',
    3: 'reduce',
    4: 'reduce',
    5: 'reduce',
    6: 'implode',
    7: 'tokenize',
    8: 'deflate',
    9: 'deflate64',
    10: 'implode',
    12: 'bzip2',
    14: 'lzma',
    18: 'terse',
    19: 'lz77',
    97: 'wavpack',
    98: 'ppmd',
}

def _check_compression(compression):
    if compression == ZIP_STORED:
        pass
    elif compression == ZIP_DEFLATED:
        if not zlib:
            fail("RuntimeError: Compression requires the (missing) zlib module")
    elif compression == ZIP_BZIP2:
        if not bz2:
            fail("RuntimeError: Compression requires the (missing) bz2 module")
    elif compression == ZIP_LZMA:
        if not lzma:
            fail("RuntimeError: Compression requires the (missing) lzma module")
    else:
        fail("NotImplementedError: That compression method is not supported")


def _get_compressor(compress_type, compresslevel=None):
    if compress_type == ZIP_DEFLATED:
        if compresslevel != None:
            return zlib.compressobj(compresslevel, zlib.DEFLATED, -15)
        return zlib.compressobj(zlib.Z_DEFAULT_COMPRESSION, zlib.DEFLATED, -15)
    elif compress_type == ZIP_BZIP2:
        if compresslevel != None:
            return bz2.BZ2Compressor(compresslevel)
        return bz2.BZ2Compressor()
    # compresslevel is ignored for ZIP_LZMA
    elif compress_type == ZIP_LZMA:
        return LZMACompressor()
    else:
        return None


def _get_decompressor(compress_type):
    _check_compression(compress_type)
    if compress_type == ZIP_STORED:
        return None
    elif compress_type == ZIP_DEFLATED:
        return zlib.decompressobj(-15)
    elif compress_type == ZIP_BZIP2:
        return bz2.BZ2Decompressor()
    elif compress_type == ZIP_LZMA:
        return LZMADecompressor()
    else:
        descr = compressor_names.get(compress_type)
        if descr:
            fail("NotImplementedError: compression type %d (%s)" % (compress_type, descr))
        else:
            fail("NotImplementedError: compression type %d" % (compress_type,))

def _strip_extra(extra, xids):
    # Remove Extra Fields with specified IDs.
    modified = False
    buffer = []
    start = 0
    i = 0
    iteration_limit_reached = False
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if i + 4 > len(extra):
            break
        xid, xlen = struct.unpack(b"<HH", extra[i : i + 4])
        j = i + 4 + xlen
        if xid in xids:
            if i != start:
                buffer.append(extra[start : i])
            start = j
            modified = True
        i = j

        # Check if this is the last iteration
        if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
            iteration_limit_reached = True

    # If we reached the iteration limit and still have data, fail
    if iteration_limit_reached and i + 4 <= len(extra):
        fail("Iteration limit exceeded: too many extra field entries, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)

    if not modified:
        return extra
    return b''.join(buffer)

def filemode(mode):
    file_types = {
        0o140000: "d",
        0o120000: "l",
        0o100000: "-",
        0o060000: "b",
        0o020000: "c",
        0o010000: "p",
        0o040000: "s",
    }
    permissions = "rwxrwxrwx"

    file_type = file_types.get(mode & 0o170000, "?")
    permission_bits = mode & 0o777
    permission_chars = []
    for index in range(len(permissions) - 1, -1, -1):
        char = permissions[index]
        if permission_bits & (1 << (len(permissions) - 1 - index)):
            permission_chars.append(char)
        else:
            permission_chars.append("-")
    permission_string = "".join(reversed(permission_chars))
    return file_type + permission_string

_crctable = None
def _gen_crc(crc):
    for j in range(8):
        if crc & 1:
            crc = (crc >> 1) ^ 0xEDB88320
        else:
            crc >>= 1
    return crc

_crctable = None

def LZMACompressor():
    self = larky.mutablestruct(__name__="LZMACompressor", __class__=LZMACompressor)
    def __init__():
        self._comp = None
        return self
    self.__init__ = __init__
    """
    def _init():
        props = lzma._encode_filter_properties({'id': lzma.FILTER_LZMA1})
        self._comp = lzma.LZMACompressor(lzma.FORMAT_RAW, filters=[
            lzma._decode_filter_properties(lzma.FILTER_LZMA1, props)
        ])
        return struct.pack('<BBH', 9, 4, len(props)) + props
    self._init = _init

    def compress(data):
        if self._comp == None:
            return self._init() + self._comp.compress(data)
        return self._comp.compress(data)
    self.compress = compress

    def flush():
        if self._comp == None:
            return self._init() + self._comp.flush()
        return self._comp.flush()
    self.flush = flush
    """
    return __init__()


def LZMADecompressor():
    self = larky.mutablestruct(__name__="LZMADecompressor", __class__=LZMADecompressor)
    def __init__():
        self._decomp = None
        self._unconsumed = b''
        self.eof = False
        return self
    self.__init__ = __init__
    """
    def decompress(data):
        if self._decomp == None:
            self._unconsumed += data
            if len(self._unconsumed) <= 4:
                return b''
            psize, _ = struct.unpack('<H', self._unconsumed[2:4])
            if len(self._unconsumed) <= 4 + psize:
                return b''

            self._decomp = lzma.LZMADecompressor(lzma.FORMAT_RAW, filters=[
                lzma._decode_filter_properties(lzma.FILTER_LZMA1,
                                               self._unconsumed[4:4 + psize])
            ])
            data = self._unconsumed[4 + psize:]
            # del self._unconsumed
            self._unconsumed = None

        result = self._decomp.decompress(data)
        self.eof = self._decomp.eof
        return result
    self.decompress = decompress
    """
    return __init__()

def _ZipDecrypter(pwd):
    key0 = 305419896
    key1 = 591751049
    key2 = 878082192

    if _crctable == None:
        _crctable = [_gen_crc(i) for i in range(256)]
    crctable = _crctable

    def crc32(ch, crc):
        """Compute the CRC32 primitive on one byte."""
        return (crc >> 8) ^ crctable[(crc ^ ch) & 0xFF]

    def update_keys(c):
        key0 = crc32(c, key0)
        key1 = (key1 + (key0 & 0xFF)) & 0xFFFFFFFF
        key1 = (key1 * 134775813 + 1) & 0xFFFFFFFF
        key2 = crc32(key1 >> 24, key2)

    for p in pwd:
        update_keys(p)

    def decrypter(data):
        """Decrypt a bytes object."""
        result = bytearray()
        append = result.append
        for c in data:
            k = key2 | 2
            c ^= ((k * (k^1)) >> 8) & 0xFF
            update_keys(c)
            append(c)
        return bytes(result)

    return decrypter

def _SharedFile(file, pos, _close, writing):
    self = larky.mutablestruct(__name__="_SharedFile", __class__=_SharedFile)
    def tell():
        return self._pos
    self.tell = tell

    def seek(offset, whence=0):
        if self._writing():
            fail("ValueError: Can't reposition in the ZIP file while ",
                             "there is an open writing handle on it. ",
                             "Close the writing handle before trying to read.")
        self._file.seek(offset, whence)
        self._pos = self._file.tell()
        return self._pos
    self.seek = seek

    def read(n=-1):
        if self._writing():
             fail("ValueError: Can't read from the ZIP file while there",
                             "is an open writing handle on it.",
                             "Close the writing handle before trying to read.")
        self._file.seek(self._pos)
        data = self._file.read(n)
        self._pos = self._file.tell()
        return data
    self.read = read

    def close():
        fileobj = self._file
        self._file = None
        self._close(fileobj)
    self.close = close

    def __init__(file, pos, close, writing):
        self._file = file
        self._pos = pos
        self._close = close
        self._writing = writing
        self.seekable = file.seekable
        return self
    self.__init__ = __init__

    return self.__init__(file, pos, _close, writing)

def ZipExtFile(fileobj, mode, zipinfo, pwd=None, close_fileobj=False):
    """File-like object for reading an archive member.
       Is returned by ZipFile.open().
    """
    self = larky.mutablestruct(__name__="ZipExtFile", __class__=ZipExtFile)
    # Max size supported by decompressor.
    self.MAX_N = 1 << 31 - 1

    # Read from compressed files in 4k blocks.
    self.MIN_READ_SIZE = 4096

    # Chunk size to read during seek
    self.MAX_SEEK_READ = 1 << 24

    def _init_decrypter():
        self._decrypter = _ZipDecrypter(self._pwd)
        # The first 12 bytes in the cypher stream is an encryption header
        #  used to strengthen the algorithm. The first 11 bytes are
        #  completely random, while the 12th contains the MSB of the CRC,
        #  or the MSB of the file time depending on the header type
        #  and is used to check the correctness of the password.
        header = self._fileobj.read(12)
        self._compress_left -= 12
        return self._decrypter(header)[11]
    self._init_decrypter = _init_decrypter

    def __repr__(self):
        result = ['<%s.%s' % (self.__class__.__module__,
                              self.__class__.__qualname__)]
        if not self.closed:
            result.append(' name=%r mode=%r' % (self.name, self.mode))
            if self._compress_type != ZIP_STORED:
                result.append(' compress_type=%s' %
                              compressor_names.get(self._compress_type,
                                                   self._compress_type))
        else:
            result.append(' [closed]')
        result.append('>')
        return ''.join(result)
    self.__repr__ = __repr__

    def readline(self, limit=-1):
        """Read and return a line from the stream.

        If limit is specified, at most limit bytes will be read.
        """

        if limit < 0:
            # Shortcut common case - newline found in buffer.
            i = self._readbuffer.find(b'\n', self._offset) + 1
            if i > 0:
                line = self._readbuffer[self._offset: i]
                self._offset = i
                return line

        return io.BufferedIOBase.readline(self, limit)
    self.readline = readline

    def peek(n=1):
        """Returns buffered bytes without advancing the position."""
        if n > len(self._readbuffer) - self._offset:
            chunk = self.read(n)
            if len(chunk) > self._offset:
                self._readbuffer = chunk + self._readbuffer[self._offset:]
                self._offset = 0
            else:
                self._offset -= len(chunk)

        # Return up to 512 bytes to reduce allocation overhead for tight loops.
        return self._readbuffer[self._offset: self._offset + 512]
    self.peek = peek

    def readable():
        if self.closed:
            fail("ValueError: I/O operation on closed file.")
        return True
    self.readable = readable

    def read(n=-1):
        """Read and return up to n bytes.
        If the argument is omitted, None, or negative, data is read and returned until EOF is reached.
        """
        if self.closed:
            fail("ValueError: read from closed file.")
        if n == None or n < 0:
            buf = self._readbuffer[self._offset:]
            self._readbuffer = b''
            self._offset = 0
            iteration_limit_reached = False
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                buf += self._read1(self.MAX_N)
                if not self._eof:
                    break

                # Check if this is the last iteration
                if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                    iteration_limit_reached = True

            # If we reached the iteration limit and still not EOF, fail
            if iteration_limit_reached and not self._eof:
                fail("Iteration limit exceeded: file too large to read, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)

            return buf

        end = n + self._offset
        if end < len(self._readbuffer):
            buf = self._readbuffer[self._offset:end]
            self._offset = end
            return buf

        n = end - len(self._readbuffer)
        buf = self._readbuffer[self._offset:]
        self._readbuffer = b''
        self._offset = 0
        iteration_limit_reached = False
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            data = self._read1(n)
            if n < len(data):
                self._readbuffer = data
                self._offset = n
                buf += data[:n]
                break
            buf += data
            n -= len(data)
            if n > 0 and not self._eof:
                # Check if this is the last iteration
                if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                    iteration_limit_reached = True
                continue
            else:
                break

        # If we reached the iteration limit and still have data to read, fail
        if iteration_limit_reached and n > 0 and not self._eof:
            fail("Iteration limit exceeded: file read operation too complex, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)

        return buf
    self.read = read

    def _update_crc(newdata):
        # Update the CRC using the given data.
        if self._expected_crc == None:
            # No need to compute the CRC if we don't have a reference value
            return
        self._running_crc = crc32(newdata, self._running_crc)
        # Check the CRC if we're at the end of the file
        if self._eof and self._running_crc != self._expected_crc:
            fail("BadZipFile: Bad CRC-32 for file %r" % self.name)
    self._update_crc = _update_crc

    def read1(n):
        """Read up to n bytes with at most one read() system call."""

        if n == None or n < 0:
            buf = self._readbuffer[self._offset:]
            self._readbuffer = b''
            self._offset = 0
            iteration_limit_reached = False
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                    iteration_limit_reached = True
                data = self._read1(self.MAX_N)
                if data:
                    buf += data
                    break
                if self._eof:
                    break
            if iteration_limit_reached and not self._eof:
                fail("Iteration limit exceeded: read1 operation too complex, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)
            return buf

        end = n + self._offset
        if end < len(self._readbuffer):
            buf = self._readbuffer[self._offset:end]
            self._offset = end
            return buf

        n = end - len(self._readbuffer)
        buf = self._readbuffer[self._offset:]
        self._readbuffer = b''
        self._offset = 0
        if n > 0:
            iteration_limit_reached = False
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                    iteration_limit_reached = True
                data = self._read1(n)
                if n < len(data):
                    self._readbuffer = data
                    self._offset = n
                    buf += data[:n]
                    break
                if data:
                    buf += data
                    break
                if self._eof:
                    break
            if iteration_limit_reached and not self._eof:
                fail("Iteration limit exceeded: read1 operation too complex, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)
        return buf
    self.read1 = read1

    def _read1(n):
        # Read up to n compressed bytes with at most one read() system call,
        # decrypt and decompress them.
        if self._eof or n <= 0:
            return b''

        # Read from file.
        if self._compress_type == ZIP_DEFLATED:
            ## Handle unconsumed data.
            data = self._decompressor.unconsumed_tail
            if n > len(data):
                data += self._read2(n - len(data))
        else:
            data = self._read2(n)

        if self._compress_type == ZIP_STORED:
            self._eof = self._compress_left <= 0
        elif self._compress_type == ZIP_DEFLATED:
            n = max(n, self.MIN_READ_SIZE)
            data = self._decompressor.decompress(data, n)
            self._eof = (self._decompressor.eof or
                         self._compress_left <= 0 and
                         not self._decompressor.unconsumed_tail)
            if self._eof:
                data += self._decompressor.flush()
        else:
            data = self._decompressor.decompress(data)
            self._eof = self._decompressor.eof or self._compress_left <= 0

        data = data[:self._left]
        self._left -= len(data)
        if self._left <= 0:
            self._eof = True
        self._update_crc(data)
        return data
    self._read1 = _read1

    def _read2(n):
        if self._compress_left <= 0:
            return b''

        n = max(n, self.MIN_READ_SIZE)
        n = min(n, self._compress_left)

        data = self._fileobj.read(n)
        self._compress_left -= len(data)
        if not data:
            # raise EOFError
            fail("EOFError")
        if self._decrypter != None:
            data = self._decrypter(data)
        return data
    self._read2 = _read2

    def close():
        if self._close_fileobj:
            self._fileobj.close()
        """
        finally:
            super().close()
        """
        super().close()
    self.close = close

    def seekable():
        if self.closed:
            fail("ValueError: I/O operation on closed file.")
        return self._seekable
    self.seekable = seekable

    def seek(offset, whence=0):
        if self.closed:
            fail("ValueError: seek on closed file.")
        if not self._seekable:
            fail("UnsupportedOperation: underlying stream is not seekable")
        curr_pos = self.tell()
        if whence == 0: # Seek from start of file
            new_pos = offset
        elif whence == 1: # Seek from current position
            new_pos = curr_pos + offset
        elif whence == 2: # Seek from EOF
            new_pos = self._orig_file_size + offset
        else:
            fail("ValueError: whence must be os.SEEK_SET (0), ",
                             "os.SEEK_CUR (1), or os.SEEK_END (2)")

        if new_pos > self._orig_file_size:
            new_pos = self._orig_file_size

        if new_pos < 0:
            new_pos = 0

        read_offset = new_pos - curr_pos
        buff_offset = read_offset + self._offset

        if buff_offset >= 0 and buff_offset < len(self._readbuffer):
            # Just move the _offset index if the new position is in the _readbuffer
            self._offset = buff_offset
            read_offset = 0
        elif read_offset < 0:
            # Position is before the current position. Reset the ZipExtFile
            self._fileobj.seek(self._orig_compress_start)
            self._running_crc = self._orig_start_crc
            self._compress_left = self._orig_compress_size
            self._left = self._orig_file_size
            self._readbuffer = b''
            self._offset = 0
            self._decompressor = _get_decompressor(self._compress_type)
            self._eof = False
            read_offset = new_pos
            if self._decrypter != None:
                self._init_decrypter()

        iteration_limit_reached = False
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                iteration_limit_reached = True
            if read_offset <= 0:
                break
            read_len = min(self.MAX_SEEK_READ, read_offset)
            self.read(read_len)
            read_offset -= read_len
        if iteration_limit_reached and read_offset > 0:
            fail("Iteration limit exceeded: seek operation too complex, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)


        return self.tell()
    self.seek = seek

    def tell():
        if self.closed:
            fail("ValueError: tell on closed file.")
        if not self._seekable:
            fail("UnsupportedOperation: underlying stream is not seekable")
        filepos = self._orig_file_size - self._left - len(self._readbuffer) + self._offset
        return filepos
    self.tell = tell

    def __init__(fileobj, mode, zipinfo, pwd=None,
                    close_fileobj=False):
        self._fileobj = fileobj
        self._pwd = pwd
        self._close_fileobj = close_fileobj

        self._compress_type = zipinfo.compress_type
        self._compress_left = zipinfo.compress_size
        self._left = zipinfo.file_size

        self._decompressor = _get_decompressor(self._compress_type)

        self._eof = False
        self._readbuffer = b''
        self._offset = 0

        self.newlines = None

        self.mode = mode
        self.name = zipinfo.filename
        self.closed = False

        if hasattr(zipinfo, 'CRC'):
            self._expected_crc = zipinfo.CRC
            self._running_crc = crc32(b'')
        else:
            self._expected_crc = None

        self._seekable = False

        if fileobj.seekable():
            self._orig_compress_start = fileobj.tell()
            self._orig_compress_size = zipinfo.compress_size
            self._orig_file_size = zipinfo.file_size
            self._orig_start_crc = self._running_crc
            self._seekable = True

        self._decrypter = None
        if pwd:
            if zipinfo.flag_bits & 0x8:
                # compare against the file type from extended local headers
                check_byte = (zipinfo._raw_time >> 8) & 0xff
            else:
                # compare against the CRC otherwise
                check_byte = (zipinfo.CRC >> 24) & 0xff
            h = self._init_decrypter()
            if h != check_byte:
                fail("RuntimeError: Bad password for file %r" % zipinfo.orig_filename)
        return self
    self.__init__ = __init__
    return self.__init__(fileobj, mode, zipinfo, pwd, close_fileobj)


def _ZipWriteFile(zf, zinfo, zip64):
    self = larky.mutablestruct(__name__="_ZipWriteFile", __class__=_ZipWriteFile)

    def writable():
        return True
    self.writable = writable

    def write(data):
        if self.closed:
            fail('ValueError: I/O operation on closed file.')

        # Accept any data that supports the buffer protocol
        if builtins.isinstance(data, (bytes, bytearray)):
            nbytes = len(data)
        else:
            if hasattr(data, 'tobytes'):
                data = data.tobytes()
            nbytes = len(data)
        self._file_size += nbytes
        self._crc = crc32(data, self._crc)
        if self._compressor:
            # This just returns something blank
            # And a Length of 0
            # But it's not a bug, python does it too
            # WTF is this doing?
            data = self._compressor.compress(data)
            self._compress_size += len(data)
        self._fileobj.write(data)
        return nbytes

    self.write = write

    def close():
        if self.closed:
            return
        else:
            # super().close()
            # Flush any data from the compressor, and update header info
            if self._compressor:
                buf = self._compressor.flush()
                self._compress_size += len(buf)
                self._fileobj.write(buf)
                self._zinfo.compress_size = self._compress_size
            else:
                self._zinfo.compress_size = self._file_size
            self._zinfo.CRC = self._crc
            self._zinfo.file_size = self._file_size

            # Write updated header info
            if self._zinfo.flag_bits & 0x08:
                # Write CRC and file sizes after the file data
                fmt = '<LLQQ' if self._zip64 else '<LLLL'
                self._fileobj.write(struct.pack(fmt, _DD_SIGNATURE, self._zinfo.CRC,
                                                self._zinfo.compress_size, self._zinfo.file_size))
                self._zipfile.start_dir = self._fileobj.tell()
            else:
                if not self._zip64:
                    if self._file_size > ZIP64_LIMIT:
                        fail('RuntimeError: File size unexpectedly exceeded ZIP64 limit')
                    if self._compress_size > ZIP64_LIMIT:
                        fail('RuntimeError: Compressed size unexpectedly exceeded ZIP64 limit')
                # Seek backwards and write file header (which will now include
                # correct CRC and file sizes)

                # Preserve current position in file
                self._zipfile.start_dir = self._fileobj.tell()
                self._fileobj.seek(self._zinfo.header_offset)
                self._fileobj.write(self._zinfo.FileHeader(self._zip64))
                self._fileobj.seek(self._zipfile.start_dir)

            # Successfully written: Add file to our caches
            self._zipfile.filelist.append(self._zinfo)
            self._zipfile.NameToInfo[self._zinfo.filename] = self._zinfo
        self._zipfile._writing = False

    self.close = close

    def __init__(zf, zinfo, zip64):
        self._zinfo = zinfo
        self._zip64 = zip64
        self._zipfile = zf
        self._compressor = _get_compressor(zinfo.compress_type,
                                           zinfo._compresslevel)
        self._file_size = 0
        self._compress_size = 0
        self._crc = 0
        self.closed = False
        self._fileobj = self._zipfile.fp
        return self

    self.__init__ = __init__(zf, zinfo, zip64)
    return self

def _get_compressor(compress_type, compresslevel=None):
    if compress_type == ZIP_DEFLATED:
        if compresslevel != None:
            return zlib.compressobj(compresslevel, zlib.DEFLATED, -15)
        return zlib.compressobj(zlib.Z_DEFAULT_COMPRESSION, zlib.DEFLATED, -15)
    elif compress_type == ZIP_BZIP2:
        if compresslevel != None:
            return bz2.BZ2Compressor(compresslevel)
        return bz2.BZ2Compressor()
    # compresslevel is ignored for ZIP_LZMA
    elif compress_type == ZIP_LZMA:
        return LZMACompressor()
    else:
        return None

def _get_decompressor(compress_type):
    _check_compression(compress_type)
    if compress_type == ZIP_STORED:
        return None
    elif compress_type == ZIP_DEFLATED:
        return zlib.decompressobj(-15)
    elif compress_type == ZIP_BZIP2:
        return bz2.BZ2Decompressor()
    elif compress_type == ZIP_LZMA:
        return LZMADecompressor()
    else:
        descr = compressor_names.get(compress_type)
        if descr:
            fail("NotImplementedError: compression type %d (%s)" % (compress_type, descr))
        else:
            fail("NotImplementedError: compression type %d" % (compress_type,))

def ZipInfo(filename="NoName", date_time=(1980,1,1,0,0,0)):
    self = larky.mutablestruct(__name__="ZipInfo", __class__=ZipInfo)
    """Class with attributes describing each file in the ZIP archive."""

    __slots__ = (
        'orig_filename',
        'filename',
        'date_time',
        'compress_type',
        '_compresslevel',
        'comment',
        'extra',
        'create_system',
        'create_version',
        'extract_version',
        'reserved',
        'flag_bits',
        'volume',
        'internal_attr',
        'external_attr',
        'header_offset',
        'CRC',
        'compress_size',
        'file_size',
        '_raw_time',
    )

    def __repr__():
        result = ['<%s filename=%r' % (self.__name__, self.filename)]
        if self.compress_type != ZIP_STORED:
            result.append(' compress_type=%s' %
                          compressor_names.get(self.compress_type,
                                               self.compress_type))
        hi = self.external_attr >> 16
        lo = self.external_attr & 0xFFFF
        if hi:
            result.append(' filemode=%r' % filemode(hi))
        if lo:
            result.append(' external_attr=%#x' % lo)
        isdir = False
        if not isdir or self.file_size:
            result.append(' file_size=%r' % self.file_size)
        if ((not isdir or self.compress_size) and
                (self.compress_type != ZIP_STORED or
                 self.file_size != self.compress_size)):
            result.append(' compress_size=%r' % self.compress_size)
        result.append('>')
        return ''.join(result)
    self.__repr__ = __repr__

    def FileHeader(zip64=None):
        """Return the per-file header as a bytes object."""
        dt = self.date_time
        dosdate = (dt[0] - 1980) << 9 | dt[1] << 5 | dt[2]
        dostime = dt[3] << 11 | dt[4] << 5 | (dt[5] // 2)
        if self.flag_bits & _MASK_USE_DATA_DESCRIPTOR:
            # Set these to zero because we write them after the file data
            CRC = 0
            compress_size = 0
            file_size = 0
        else:
            CRC = self.CRC
            compress_size = self.compress_size
            file_size = self.file_size

        extra = self.extra

        min_version = 0
        if zip64 == None:
            zip64 = file_size > ZIP64_LIMIT or compress_size > ZIP64_LIMIT
        if zip64:
            fmt = '<HHQQ'
            extra = extra + struct.pack(fmt,
                                        1, struct.calcsize(fmt)-4, file_size, compress_size)
        if file_size > ZIP64_LIMIT or compress_size > ZIP64_LIMIT:
            if not zip64:
                fail("LargeZipFile: Filesize would require ZIP64 extensions")
            # File is larger than what fits into a 4 byte integer,
            # fall back to the ZIP64 extension
            file_size = 0xffffffff
            compress_size = 0xffffffff
            min_version = ZIP64_VERSION


        if self.compress_type == ZIP_BZIP2:
            min_version = max(BZIP2_VERSION, min_version)
        elif self.compress_type == ZIP_LZMA:
            min_version = max(LZMA_VERSION, min_version)

        self.extract_version = max(min_version, self.extract_version)
        self.create_version = max(min_version, self.create_version)
        filename, flag_bits = self._encodeFilenameFlags()
        header = struct.pack(structFileHeader, stringFileHeader,
                             self.extract_version, self.reserved, flag_bits,
                             self.compress_type, dostime, dosdate, CRC,
                             compress_size, file_size,
                             len(filename), len(extra))
        return header + filename + extra
    self.FileHeader = FileHeader

    def _encodeFilenameFlags():
        """
        try:
            return self.filename.encode('ascii'), self.flag_bits
        except UnicodeEncodeError:
            return self.filename.encode('utf-8'), self.flag_bits | _MASK_UTF_FILENAME
        """
        return bytes(self.filename, 'utf-8'), self.flag_bits | _MASK_UTF_FILENAME
    self._encodeFilenameFlags = _encodeFilenameFlags

    def _decodeExtra():
        # Try to decode the extra field.
        extra = self.extra
        unpack = struct.unpack
        iteration_limit_reached = False
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                iteration_limit_reached = True
            if len(extra) < 4:
                break
            tp, ln = unpack('<HH', extra[:4])
            if ln+4 > len(extra):
                fail("BadZipFile: Corrupt extra field %04x (size=%d)" % (tp, ln))
            if tp == 0x0001:
                data = extra[4:ln+4]
                # ZIP64 extension (large files and/or large archives)
                if self.file_size in (0xFFFFFFFFFFFFFFFF, 0xFFFFFFFF):
                    field = "File size"
                    (self.file_size,) = unpack('<Q', data[:8])
                    data = data[8:]
                if self.compress_size == 0xFFFFFFFF:
                    field = "Compress size"
                    (self.compress_size,) = unpack('<Q', data[:8])
                    data = data[8:]
                if self.header_offset == 0xFFFFFFFF:
                    field = "Header offset"
                    (self.header_offset,) = unpack('<Q', data[:8])

            extra = extra[ln+4:]
        if iteration_limit_reached and len(extra) >= 4:
            fail("Iteration limit exceeded: extra field decoding too complex, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)

    self._decodeExtra = _decodeExtra

    def is_dir():
        """Return True if this archive member is a directory."""
        return self.filename[-1] == '/'
    self.is_dir = is_dir

    def __init__(filename="NoName", date_time=(1980,1,1,0,0,0)):
        self.orig_filename = filename   # Original file name in archive

        # Terminate the file name at the first null byte.  Null bytes in file
        # names are used as tricks by viruses in archives.
        # null_byte = filename.find(chr(0))
        null_byte = -1
        if null_byte >= 0:
            filename = filename[0:null_byte]
        # This is used to ensure paths in generated ZIP files always use
        # forward slashes as the directory separator, as required by the
        # ZIP format specification.
        # filename.replace("\\", "/")

        self.filename = filename        # Normalized file name
        self.date_time = date_time      # year, month, day, hour, min, sec

        if date_time[0] < 1980:
            fail('ValueError: ZIP does not support timestamps before 1980')

        # Standard values:
        self.compress_type = ZIP_STORED # Type of compression for the file
        self._compresslevel = None      # Level for the compressor
        self.comment = b""              # Comment for each file
        self.extra = b""                # ZIP extra data
        """
        We're not gonna sys.platform on Larky and we certainly aren't on Windows. 
        if sys.platform == 'win32':
            self.create_system = 0          # System which created ZIP archive
        else:
            # Assume everything else is unix-y
            self.create_system = 3          # System which created ZIP archive
        """

        self.create_system = 3
        self.create_version = DEFAULT_VERSION  # Version which created ZIP archive
        self.extract_version = DEFAULT_VERSION # Version needed to extract archive
        self.reserved = 0               # Must be zero
        self.flag_bits = 0              # ZIP flag bits
        self.volume = 0                 # Volume number of file header
        self.internal_attr = 0          # Internal attributes
        self.external_attr = 0          # External file attributes
        self.compress_size = 0          # Size of the compressed file
        self.file_size = 0              # Size of the uncompressed file
        # Other attributes are set by class ZipFile:
        # header_offset         Byte offset to the file header
        # CRC                   CRC-32 of the uncompressed file
        return self

    self.__init__ = __init__(filename, date_time)
    return self

def ZipFile(file, mode="r", compression=ZIP_STORED, allowZip64=False):
    self = larky.mutablestruct(__name__="ZipFile", __class__=ZipFile)
    fp = None       # Set here since __del__ checks it, but it may not be needed since it's the output of open()?

    def namelist():
        """Return a list of file names in the archive."""
        l = []
        for data in self.filelist:
            l.append(data.filename)
        return l
    self.namelist = namelist

    def _EndRecData64(fpin, offset, endrec):
        """
        Read the ZIP64 end-of-archive records and use that to update endrec
        """
        filesize = fpin.tell()

        if (filesize - offset) < sizeEndCentDir64Locator:
            # If the condition fails, the file is not large enough to contain a ZIP64
            # end-of-archive record, so just return the end record we were given.
            return endrec
        else:
            fpin.seek(offset - sizeEndCentDir64Locator, 2)
        data = fpin.read(sizeEndCentDir64Locator)
        if len(data) != sizeEndCentDir64Locator:
            return endrec
        sig, diskno, reloff, disks = struct.unpack(structEndArchive64Locator, data)
        if sig != stringEndArchive64Locator:
            return endrec

        if diskno != 0 or disks > 1:
            fail("BadZipfile: zipfiles that span multiple disks are not supported")

        # Assume no 'zip64 extensible data'
        fpin.seek(offset - sizeEndCentDir64Locator - sizeEndCentDir64, 2)
        data = fpin.read(sizeEndCentDir64)
        if len(data) != sizeEndCentDir64:
            return endrec
        sig, sz, create_version, read_version, disk_num, disk_dir, \
        dircount, dircount2, dirsize, diroffset = struct.unpack(structEndArchive64, data)
        if sig != stringEndArchive64:
            return endrec

        # Update the original endrec using data from the ZIP64 record
        endrec[_ECD_SIGNATURE] = sig
        endrec[_ECD_DISK_NUMBER] = disk_num
        endrec[_ECD_DISK_START] = disk_dir
        endrec[_ECD_ENTRIES_THIS_DISK] = dircount
        endrec[_ECD_ENTRIES_TOTAL] = dircount2
        endrec[_ECD_SIZE] = dirsize
        endrec[_ECD_OFFSET] = diroffset
        return endrec

    def _EndRecData(fpin):
        fpin.seek(0, 2)
        filesize = fpin.tell()
        fpin.seek(-sizeEndCentDir, 2)
        data = fpin.read()
        if len(data) == sizeEndCentDir and data[0:4] == stringEndArchive and data[-2:] == b"\000\000":
            # signature is correct, unpack data.
            endrec = list(struct.unpack(structEndArchive, data))

            # Append blank comment and record start offset
            endrec.append(b"")
            endrec.append(filesize - sizeEndCentDir)
            # Try to read the Zip64 end of central directory structure.
            return _EndRecData64(fpin, -sizeEndCentDir, endrec)

        # Either this is not a ZIP file, or it is a ZIP file with an archive comment.
        # Search the end of the file for the "end of central directory" record signature.
        # The comment is the last item in the ZIP file and may be up to 64K long. It is
        # assumed the end of the "end of central directory" magic number does not appear
        # anywhere in the comment.
        maxCommentStart = max(filesize - (1 << 16) - sizeEndCentDir, 0)
        fpin.seek(maxCommentStart, 0)
        data = fpin.read()
        start = data.rfind(stringEndArchive)
        if start >= 0:
            # found the magic number; attempt to unpack and interpret
            recData = data[start:start+sizeEndCentDir]
            if len(recData) != sizeEndCentDir:
                # Zip file is corrupted.
                return None
            endrec = list(struct.unpack(structEndArchive, recData))
            commentSize = endrec[_ECD_COMMENT_SIZE] #as claimed by the zip file
            comment = data[start+sizeEndCentDir:start+sizeEndCentDir+commentSize]
            endrec.append(comment)
            endrec.append(maxCommentStart + start)

            # Try to read the "Zip64 end of central directory" structure
            return _EndRecData64(fpin, maxCommentStart + start - filesize,
                                 endrec)

        # Unable to find a valid end of central directory structure
        return None

    def _RealGetContents():
        fp = self.fp
        endrec = _EndRecData(fp)
        if not endrec:
            fail("BadZipFile: File is not a zip file")

        size_cd = endrec[_ECD_SIZE]             # bytes in central directory
        offset_cd = endrec[_ECD_OFFSET]         # offset of central directory
        self._comment = endrec[_ECD_COMMENT]    # archive comment

        # "concat" is zero, unless zip was concatenated to another file
        concat = endrec[_ECD_LOCATION] - size_cd - offset_cd
        if endrec[_ECD_SIGNATURE] == stringEndArchive64:
            # If Zip64 extension structures are present, account for them
            concat -= (sizeEndCentDir64 + sizeEndCentDir64Locator)
        self.start_dir = offset_cd + concat
        fp.seek(self.start_dir, 0)
        data = fp.read(size_cd)
        fp = io.BytesIO(data)
        total = 0
        iteration_limit_reached = False

        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                iteration_limit_reached = True
            if total >= size_cd:
                break
            centdir = fp.read(sizeCentralDir)
            if len(centdir) != sizeCentralDir:
                fail("BadZipFile: Truncated central directory")
            centdir = struct.unpack(structCentralDir, centdir)
            if centdir[_CD_SIGNATURE] != stringCentralDir:
                fail("BadZipFile: Bad magic number for central directory")

            filename = fp.read(centdir[_CD_FILENAME_LENGTH])
            flags = centdir[_CD_FLAG_BITS]
            if flags & 0x800:
                # UTF-8 file names extension
                filename = filename.decode('utf-8')
            else:
                # Historical ZIP filename encoding
                if not self.metadata_encoding:
                    metadata = "cp437"
                else:
                    metadata = self.metadata_encoding
                filename = filename.decode(metadata or 'cp437')
            x = ZipInfo(filename)
            x.extra = fp.read(centdir[_CD_EXTRA_FIELD_LENGTH])
            x.comment = fp.read(centdir[_CD_COMMENT_LENGTH])
            x.header_offset = centdir[_CD_LOCAL_HEADER_OFFSET]
            (x.create_version, x.create_system, x.extract_version, x.reserved,
             x.flag_bits, x.compress_type, t, d,
             x.CRC, x.compress_size, x.file_size) = centdir[1:12]
            if x.extract_version > MAX_EXTRACT_VERSION:
                fail("NotImplementedError: zip file version %.1f" %
                     (x.extract_version / 10))
            x.volume, x.internal_attr, x.external_attr = centdir[15:18]
            # Convert date/time code to (year, month, day, hour, min, sec)
            x._raw_time = t
            x.date_time = ( (d>>9)+1980, (d>>5)&0xF, d&0x1F,
                            t>>11, (t>>5)&0x3F, (t&0x1F) * 2 )

            x._decodeExtra()
            x.header_offset = x.header_offset + concat
            self.filelist.append(x)
            self.NameToInfo[x.filename] = x

            # update total bytes read from central directory
            total = (total + sizeCentralDir + centdir[_CD_FILENAME_LENGTH]
                     + centdir[_CD_EXTRA_FIELD_LENGTH]
                     + centdir[_CD_COMMENT_LENGTH])
        if iteration_limit_reached and total < size_cd:
            fail("Iteration limit exceeded: central directory parsing too complex, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)

    self._RealGetContents = _RealGetContents

    def _GetContents():
        """Read the directory, making sure we close the file if the format is bad. "" "
        try:
            self._RealGetContents()
        except BadZipfile:
            if not self._filePassed:
                self.fp.close()
                self.fp = None
            raise
        """
        self._RealGetContents()
    self._GetContents = _GetContents

    def infolist():
        """Return a list of class ZipInfo instances for files in the
        archive."""
        return self.filelist
    self.infolist = infolist

    def printdir(file=None):
        """Print a table of contents for the zip file."""
        print("%-46s %19s %12s" % ("File Name", "Modified    ", "Size"),
              file=file)
        for zinfo in self.filelist:
            date = "%d-%02d-%02d %02d:%02d:%02d" % zinfo.date_time[:6]
            print("%-46s %s %12d" % (zinfo.filename, date, zinfo.file_size),
                  file=file)
    self.printdir = printdir

    def testzip():
        """Read all the files and check the CRC."""
        chunk_size = builtins.pow(2, 20)
        """
        for zinfo in self.filelist:
            try:
                # Read by chunks, to avoid an OverflowError or a
                # MemoryError with very large embedded files.
                with self.open(zinfo.filename, "r") as f:
                    while f.read(chunk_size):     # Check CRC-32
                        pass
            except BadZipFile:
                return zinfo.filename
        """
    self.testzip = testzip

    def close():
        """Close the file, and for mode 'w', 'x' and 'a' write the ending
        records."""
        if self.fp == None:
            return

        if self._writing:
            fail("ValueError: Can't close the ZIP file while there is " +
                             "an open writing handle on it. " +
                             "Close the writing handle before closing the zip.")
        if self.mode in ('w', 'x', 'a') and self._didModify:  # write ending records
            if self._seekable:
                self.fp.seek(self.start_dir)
            self._write_end_record()

        fp = self.fp
        self.fp = None
        self._fpclose(fp)
    self.close = close

    def _write_end_record():
        for zinfo in self.filelist:         # write central directory
            dt = zinfo.date_time
            dosdate = (dt[0] - 1980) << 9 | dt[1] << 5 | dt[2]
            dostime = dt[3] << 11 | dt[4] << 5 | (dt[5] // 2)
            extra = []
            if zinfo.file_size > ZIP64_LIMIT \
                    or zinfo.compress_size > ZIP64_LIMIT:
                extra.append(zinfo.file_size)
                extra.append(zinfo.compress_size)
                file_size = 0xffffffff
                compress_size = 0xffffffff
            else:
                file_size = zinfo.file_size
                compress_size = zinfo.compress_size

            if zinfo.header_offset > ZIP64_LIMIT:
                extra.append(zinfo.header_offset)
                header_offset = 0xffffffff
            else:
                header_offset = zinfo.header_offset

            extra_data = zinfo.extra
            min_version = 0
            if extra:
                # Append a ZIP64 field to the extra's
                extra_data = _strip_extra(extra_data, (1,))
                extra_data = struct.pack(
                    '<HH' + 'Q'*len(extra),
                    1, 8*len(extra), *extra) + extra_data

                min_version = ZIP64_VERSION

            if zinfo.compress_type == ZIP_BZIP2:
                min_version = max(BZIP2_VERSION, min_version)
            elif zinfo.compress_type == ZIP_LZMA:
                min_version = max(LZMA_VERSION, min_version)

            extract_version = max(min_version, zinfo.extract_version)
            create_version = max(min_version, zinfo.create_version)
            filename, flag_bits = zinfo._encodeFilenameFlags()
            centdir = struct.pack(structCentralDir,
                                  stringCentralDir, create_version,
                                  zinfo.create_system, extract_version, zinfo.reserved,
                                  flag_bits, zinfo.compress_type, dostime, dosdate,
                                  zinfo.CRC, compress_size, file_size,
                                  len(filename), len(extra_data), len(zinfo.comment),
                                  0, zinfo.internal_attr, zinfo.external_attr,
                                  header_offset)
            self.fp.write(centdir)
            self.fp.write(filename)
            self.fp.write(extra_data)
            self.fp.write(zinfo.comment)

        pos2 = self.fp.tell()
        # Write end-of-zip-archive record
        centDirCount = len(self.filelist)
        centDirSize = pos2 - self.start_dir
        centDirOffset = self.start_dir
        requires_zip64 = None
        if centDirCount > ZIP_FILECOUNT_LIMIT:
            requires_zip64 = "Files count"
        elif centDirOffset > ZIP64_LIMIT:
            requires_zip64 = "Central directory offset"
        elif centDirSize > ZIP64_LIMIT:
            requires_zip64 = "Central directory size"
        if requires_zip64:
            # Need to write the ZIP64 end-of-archive records
            if not self._allowZip64:
                fail("LargeZipFile: "+ requires_zip64 +
                                   " would require ZIP64 extensions")
            zip64endrec = struct.pack(
                structEndArchive64, stringEndArchive64,
                44, 45, 45, 0, 0, centDirCount, centDirCount,
                centDirSize, centDirOffset)
            self.fp.write(zip64endrec)

            zip64locrec = struct.pack(
                structEndArchive64Locator,
                stringEndArchive64Locator, 0, pos2, 1)
            self.fp.write(zip64locrec)
            centDirCount = min(centDirCount, 0xFFFF)
            centDirSize = min(centDirSize, 0xFFFFFFFF)
            centDirOffset = min(centDirOffset, 0xFFFFFFFF)

        endrec = struct.pack(structEndArchive, stringEndArchive,
                             0, 0, centDirCount, centDirCount,
                             centDirSize, centDirOffset, len(self._comment))
        self.fp.write(endrec)
        self.fp.write(self._comment)
        if self.mode == "a":
            self.fp.truncate()
        self.fp.flush()

    self._write_end_record = _write_end_record

    def getinfo(name):
        """Return the instance of ZipInfo given 'name'."""
        info = self.NameToInfo.get(name)
        if info == None:
            fail('There is no item named %r in the archive' % name)
        return info
    self.getinfo = getinfo

    def setpassword(pwd):
        """Set default password for encrypted files."""
        if pwd and not builtins.isinstance(pwd, bytes):
            fail("TypeError: pwd: expected bytes, got %s" % type(pwd).__name__)
        if pwd:
            self.pwd = pwd
        else:
            self.pwd = None
    self.setpassword = setpassword

    def read(name, pwd=None):
        fp = self.open(name, "r", pwd)
        return fp.read()
    self.read = read

    def _writecheck(zinfo):
        """Check for errors before writing a file to the archive."""
        if zinfo.filename in self.NameToInfo:
            print('Warning: Duplicate name: %r' % zinfo.filename)
        if self.mode not in ('w', 'x', 'a'):
            fail("ValueError: write() requires mode 'w', 'x', or 'a'")
        if not self.fp:
            fail("ValueError: Attempt to write ZIP archive that was already closed")
        _check_compression(zinfo.compress_type)
        if not self._allowZip64:
            requires_zip64 = None
            if len(self.filelist) >= ZIP_FILECOUNT_LIMIT:
                requires_zip64 = "Files count"
            elif zinfo.file_size > ZIP64_LIMIT:
                requires_zip64 = "Filesize"
            elif zinfo.header_offset > ZIP64_LIMIT:
                requires_zip64 = "Zipfile size"
            if requires_zip64:
                fail("LargeZipFile: " + requires_zip64 +
                                   " would require ZIP64 extensions")
    self._writecheck = _writecheck

    def open(name, mode="r", pwd=None, *, force_zip64=False):
        if mode not in ("r", "w"):
            fail('ValueError: open() requires mode "r" or "w"')
        if pwd and not builtins.isinstance(pwd, bytes):
            fail('TypeError: pwd: expected bytes, got %s' % (type(pwd)))
        if pwd and (mode == "w"):
            fail('ValueError: pwd is only supported for reading files')
        if not self.fp:
            fail('ValueError: Attempt to use ZIP archive that was already closed')

        if builtins.isinstance(name, ZipInfo):
            # 'name' is already an info object
            zinfo = name
        elif mode == 'w':
            zinfo = ZipInfo(name)
            zinfo.compress_type = self.compression
            zinfo._compresslevel = self.compresslevel
        else:
            zinfo = self.getinfo(name)
        if mode == "w":
            return self._open_to_write(zinfo, force_zip64=force_zip64)
        if self._writing:
            message = "ValueError: Can't read from the ZIP file where there is an open", \
            "writing handle on it. Close the writing handle before trying to read."
            fail(message)
        # Open for reading:
        self._fileRefCnt += 1
        zef_file = _SharedFile(self.fp, zinfo.header_offset, self._fpclose, lambda: self._writing)

        # Skip the file header:
        fheader = zef_file.read(sizeFileHeader)
        if len(fheader) != sizeFileHeader:
            fail("BadZipFile: Truncated file header")
        fheader = struct.unpack(structFileHeader, fheader)
        if fheader[_FH_SIGNATURE] != stringFileHeader:
            fail("BadZipFile: Bad magic number for file header")

        fname = zef_file.read(fheader[_FH_FILENAME_LENGTH])
        if fheader[_FH_EXTRA_FIELD_LENGTH]:
            zef_file.read(fheader[_FH_EXTRA_FIELD_LENGTH])

        if zinfo.flag_bits & 0x20:
            # Zip 2.7: compressed patched data
            fail("NotImplementedError: compressed patched data (flag bit 5)")

        if zinfo.flag_bits & 0x40:
            # strong encryption
            fail("NotImplementedError: strong encryption (flag bit 6)")

        if fheader[_FH_GENERAL_PURPOSE_FLAG_BITS] & 0x800:
            # UTF-8 filename
            fname_str = fname.decode("utf-8")
        else:
            fname_str = fname.decode("cp437")

        if fname_str != zinfo.orig_filename:
            fail('BadZipFile: File name in directory %r and header %r differ.'
                % (zinfo.orig_filename, fname))

        # check for encrypted flag & handle password
        is_encrypted = zinfo.flag_bits & 0x1
        if is_encrypted:
            if not pwd:
                pwd = self.pwd
            if not pwd:
                fail("RuntimeError: File %r is encrypted, password",
                                   "required for extraction" % name)
        else:
            pwd = None
        return ZipExtFile(zef_file, mode, zinfo, pwd, True)

    self.open = open

    def _fpclose(fp):
        if self._fileRefCnt <= 0:
            fail()
        self._fileRefCnt -= 1
        if not self._fileRefCnt and not self._filePassed:
            fp.close()
    self._fpclose = _fpclose

    def _open_to_write(zinfo, force_zip64=False):
        if force_zip64 and not self._allowZip64:
            fail("ValueError: force_zip64 is True, but allowZip64 was False when opening the ZIP file.")
        if self._writing:
            fail("ValueError: Can't write to the ZIP file while there is another write handle open on it. " +
                    "Close the first handle before opening another.")

        # Size and CRC are overwritten with correct data after processing the file
        zinfo.compress_size = 0
        zinfo.CRC = 0

        zinfo.flag_bits = 0x00
        if zinfo.compress_type == ZIP_LZMA:
            # Compressed data includes an end-of-stream (EOS) marker
            zinfo.flag_bits |= 0x02
        if not self._seekable:
            zinfo.flag_bits |= 0x08

        if not zinfo.external_attr:
            zinfo.external_attr = 0o600 << 16  # permissions: ?rw-------

        # Compressed size can be larger than uncompressed size
        zip64 = self._allowZip64 and \
                (force_zip64 or zinfo.file_size * 1.05 > ZIP64_LIMIT)

        if self._seekable:
            self.fp.seek(self.start_dir)
        zinfo.header_offset = self.fp.tell()

        self._writecheck(zinfo)
        self._didModify = True

        self.fp.write(zinfo.FileHeader(zip64))

        self._writing = True
        return _ZipWriteFile(self, zinfo, zip64)
    self._open_to_write = _open_to_write

    def writestr(zinfo_or_arcname, data,
                 compress_type=None, compresslevel=None):
        """Write a file into the archive.  The contents is 'data', which
        may be either a 'str' or a 'bytes' instance; if it is a 'str',
        it is encoded as UTF-8 first.
        'zinfo_or_arcname' is either a ZipInfo instance or
        the name of the file in the archive."""
        if builtins.isinstance(data, str):
            data = bytes(data, "utf-8")
        if not builtins.isinstance(zinfo_or_arcname, ZipInfo):
            zinfo = ZipInfo(filename=zinfo_or_arcname,
                            date_time=(1970, 1, 1, 0, 0, 0))
            zinfo.compress_type = self.compression
            zinfo._compresslevel = self.compresslevel
            if zinfo.filename[-1] == '/':
                zinfo.external_attr = 0o40775 << 16   # drwxrwxr-x
                zinfo.external_attr |= 0x10           # MS-DOS directory flag
            else:
                zinfo.external_attr = 0o600 << 16     # ?rw-------
        else:
            zinfo = zinfo_or_arcname
        if not self.fp:
            fail("ValueError: Attempt to write to ZIP archive that was already closed")
        if self._writing:
            fail("ValueError: Can't write to ZIP archive while an open writing handle exists.")

        if compress_type != None:
            zinfo.compress_type = compress_type

        if compresslevel != None:
            zinfo._compresslevel = compresslevel

        zinfo.file_size = len(data)            # Uncompressed size
        dest = self.open(zinfo, mode='w')
        dest.write(data)
        dest.close()
    self.writestr = writestr

    def __init__(file, mode="a", compression=ZIP_STORED, allowZip64=False):
        if mode not in ("r", "w", "a"):
            fail('ZipFile() requires mode "r", "w" or "a".')
        if compression == ZIP_STORED:
            pass
        elif compression == ZIP_DEFLATED:
            if not zlib:
                fail("Compression requires the missing zlib module")
        else:
            fail("That compression method is not supported")

        self._allowZip64 = allowZip64
        self._didModify = False
        self.debug = 0  # Level of printing: 0 through 3
        self.NameToInfo = {}    # Find file info given name
        self.filelist = []      # List of ZipInfo instances for archive
        self.compression = compression  # Method of compression
        key = mode.replace('b', '')[0]
        self.mode = key
        self.pwd = None
        self._comment = ''

        # skip check if we were passed a file-like object, can't open from fs in Larky.
        self._filePassed = 1
        self.fp = file
        self.filename = None # Can't use `getattr()` on the bytestream
        self.metadata_encoding = None
        self._fileRefCnt = 1
        self._seekable = True
        self._writing = False

        if mode == 'r':
            self._GetContents()
        elif mode in ('w', 'x'):
            self._didModify = True
            self.start_dir = 0
            self.seekable = False

        elif mode == 'a':
            self._RealGetContents()
        else:
            fail("Mode must be 'r', 'w', 'x', or 'a'")
        return self

    return __init__(file, mode, compression=compression, allowZip64=allowZip64)
