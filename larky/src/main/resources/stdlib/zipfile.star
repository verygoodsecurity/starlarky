load("@stdlib//io", io="io")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION")
load("@stdlib//struct", struct="struct")
load("@stdlib//zlib", zlib="zlib")

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


def ZipInfo (filename):
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
        return self.filename.encode('utf-8'), self.flag_bits | _MASK_UTF_FILENAME
    self._encodeFilenameFlags = _encodeFilenameFlags

    def _decodeExtra():
        # Try to decode the extra field.
        extra = self.extra
        unpack = struct.unpack
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
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
            if len(extra) < 4:
                break
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

    self.__init__ = __init__(filename)
    return self

"""
ATTENTION MOTHERFUCKER
THIS IS THE DEMARCATION LINE
BELOW HERE IS TO BE PRESERVED UNTIL THE ABOVE IS WORKING
"""

def ZipFile(file, compression=ZIP_STORED, allowZip64=False):
    self = larky.mutablestruct(__name__="ZipFile", __class__=ZipFile)
    fp = None       # Set here since __del__ checks it, but it may not be needed since it's the output of open()?

    def namelist():
        """Return a list of file names in the archive."""
        l = []
        for data in self.filelist:
            l.append(data.filename)
        return l

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

        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
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

            if total >= size_cd:
                break

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

    def __init__(self, file, mode="a", compression=ZIP_STORED, allowZip64=False):
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
        self.comment = ''

        # skip check if we were passed a file-like object, can't open from fs in Larky.
        self._filePassed = 1
        self.fp = file
        self.filename = None # Can't use `getattr()` on the bytestream
        self.metadata_encoding = None

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

    __init__(self, file, mode="a", compression=compression, allowZip64=allowZip64)
    return self
