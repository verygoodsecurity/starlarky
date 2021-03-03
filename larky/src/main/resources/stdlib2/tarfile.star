def stn(s, length, encoding, errors):
    """
    Convert a string to a null-terminated bytes object.
    
    """
def nts(s, encoding, errors):
    """
    Convert a null-terminated bytes object to a string.
    
    """
def nti(s):
    """
    Convert a number field to a python number.
    
    """
def itn(n, digits=8, format=DEFAULT_FORMAT):
    """
    Convert a python number to a number field.
    
    """
def calc_chksums(buf):
    """
    Calculate the checksum for a member's header by summing up all
           characters except for the chksum field which is treated as if
           it was filled with spaces. According to the GNU tar sources,
           some tars (Sun and NeXT) calculate chksum with signed char,
           which will be different if there are chars in the buffer with
           the high bit set. So we calculate two checksums, unsigned and
           signed.
    
    """
def copyfileobj(src, dst, length=None, exception=OSError, bufsize=None):
    """
    Copy length bytes from fileobj src to fileobj dst.
           If length is None, copy the entire content.
    
    """
def _safe_print(s):
    """
    'encoding'
    """
def TarError(Exception):
    """
    Base exception.
    """
def ExtractError(TarError):
    """
    General exception for extract errors.
    """
def ReadError(TarError):
    """
    Exception for unreadable tar archives.
    """
def CompressionError(TarError):
    """
    Exception for unavailable compression methods.
    """
def StreamError(TarError):
    """
    Exception for unsupported operations on stream-like TarFiles.
    """
def HeaderError(TarError):
    """
    Base exception for header errors.
    """
def EmptyHeaderError(HeaderError):
    """
    Exception for empty headers.
    """
def TruncatedHeaderError(HeaderError):
    """
    Exception for truncated headers.
    """
def EOFHeaderError(HeaderError):
    """
    Exception for end of file headers.
    """
def InvalidHeaderError(HeaderError):
    """
    Exception for invalid headers.
    """
def SubsequentHeaderError(HeaderError):
    """
    Exception for missing and invalid extended headers.
    """
def _LowLevelFile:
    """
    Low-level file object. Supports reading and writing.
           It is used instead of a regular file object for streaming
           access.
    
    """
    def __init__(self, name, mode):
        """
        r
        """
    def close(self):
        """
        Class that serves as an adapter between TarFile and
               a stream-like object.  The stream-like object only
               needs to have a read() or write() method and is accessed
               blockwise.  Use of gzip or bzip2 compression is possible.
               A stream-like object could be for example: sys.stdin,
               sys.stdout, a socket, a tape device etc.

               _Stream is intended to be used only internally.
    
        """
    def __init__(self, name, mode, comptype, fileobj, bufsize):
        """
        Construct a _Stream object.
        
        """
    def __del__(self):
        """
        closed
        """
    def _init_write_gz(self):
        """
        Initialize for writing with gzip compression.
        
        """
    def write(self, s):
        """
        Write string s to the stream.
        
        """
    def __write(self, s):
        """
        Write string s to the stream if a whole new block
                   is ready to be written.
        
        """
    def close(self):
        """
        Close the _Stream object. No operation should be
                   done on it afterwards.
        
        """
    def _init_read_gz(self):
        """
        Initialize for reading a gzip compressed fileobj.
        
        """
    def tell(self):
        """
        Return the stream's file pointer position.
        
        """
    def seek(self, pos=0):
        """
        Set the stream's file pointer to pos. Negative seeking
                   is forbidden.
        
        """
    def read(self, size):
        """
        Return the next size number of bytes from the stream.
        """
    def _read(self, size):
        """
        Return size bytes from the stream.
        
        """
    def __read(self, size):
        """
        Return size bytes from stream. If internal buffer is empty,
                   read another block from the stream.
        
        """
def _StreamProxy(object):
    """
    Small proxy class that enables transparent compression
           detection for the Stream interface (mode 'r|*').
    
    """
    def __init__(self, fileobj):
        """
        b"\x1f\x8b\x08
        """
    def close(self):
        """
         class StreamProxy

        ------------------------
         Extraction file object
        ------------------------

        """
def _FileInFile(object):
    """
    A thin wrapper around an existing file object that
           provides a part of its data as an individual file
           object.
    
    """
    def __init__(self, fileobj, offset, size, blockinfo=None):
        """
        name
        """
    def flush(self):
        """
        Return the current file position.
        
        """
    def seek(self, position, whence=io.SEEK_SET):
        """
        Seek to a position in the file.
        
        """
    def read(self, size=None):
        """
        Read data from the file.
        
        """
    def readinto(self, b):
        """
        class _FileInFile


        """
def ExFileObject(io.BufferedReader):
    """
    class ExFileObject

    ------------------
     Exported Classes
    ------------------

    """
def TarInfo(object):
    """
    Informational class which holds the details about an
           archive member given by a tar header block.
           TarInfo objects are returned by TarFile.getmember(),
           TarFile.getmembers() and TarFile.gettarinfo() and are
           usually created internally.
    
    """
    def __init__(self, name=""):
        """
        Construct a TarInfo object. name is the optional name
                   of the member.
        
        """
    def path(self):
        """
        'In pax headers, "name" is called "path".'
        """
    def path(self, name):
        """
        'In pax headers, "linkname" is called "linkpath".'
        """
    def linkpath(self, linkname):
        """
        <%s %r at %#x>
        """
    def get_info(self):
        """
        Return the TarInfo's attributes as a dictionary.
        
        """
    def tobuf(self, format=DEFAULT_FORMAT, encoding=ENCODING, errors="surrogateescape"):
        """
        Return a tar header as a string of 512 byte blocks.
        
        """
    def create_ustar_header(self, info, encoding, errors):
        """
        Return the object as a ustar header block.
        
        """
    def create_gnu_header(self, info, encoding, errors):
        """
        Return the object as a GNU header block sequence.
        
        """
    def create_pax_header(self, info, encoding):
        """
        Return the object as a ustar header block. If it cannot be
                   represented this way, prepend a pax extended header sequence
                   with supplement information.
        
        """
    def create_pax_global_header(cls, pax_headers):
        """
        Return the object as a pax global header block sequence.
        
        """
    def _posix_split_name(self, name, encoding, errors):
        """
        Split a name longer than 100 chars into a prefix
                   and a name part.
        
        """
    def _create_header(info, format, encoding, errors):
        """
        Return a header block. info is a dictionary with file
                   information, format must be one of the *_FORMAT constants.
        
        """
    def _create_payload(payload):
        """
        Return the string payload filled with zero bytes
                   up to the next 512 byte border.
        
        """
    def _create_gnu_long_header(cls, name, type, encoding, errors):
        """
        Return a GNUTYPE_LONGNAME or GNUTYPE_LONGLINK sequence
                   for name.
        
        """
    def _create_pax_generic_header(cls, pax_headers, type, encoding):
        """
        Return a POSIX.1-2008 extended or global header sequence
                   that contains a list of keyword, value pairs. The values
                   must be strings.
        
        """
    def frombuf(cls, buf, encoding, errors):
        """
        Construct a TarInfo object from a 512 byte bytes object.
        
        """
    def fromtarfile(cls, tarfile):
        """
        Return the next TarInfo object from TarFile object
                   tarfile.
        
        """
    def _proc_member(self, tarfile):
        """
        Choose the right processing method depending on
                   the type and call it.
        
        """
    def _proc_builtin(self, tarfile):
        """
        Process a builtin type or an unknown type which
                   will be treated as a regular file.
        
        """
    def _proc_gnulong(self, tarfile):
        """
        Process the blocks that hold a GNU longname
                   or longlink member.
        
        """
    def _proc_sparse(self, tarfile):
        """
        Process a GNU sparse header plus extra headers.
        
        """
    def _proc_pax(self, tarfile):
        """
        Process an extended or global header as described in
                   POSIX.1-2008.
        
        """
    def _proc_gnusparse_00(self, next, pax_headers, buf):
        """
        Process a GNU tar extended sparse header, version 0.0.
        
        """
    def _proc_gnusparse_01(self, next, pax_headers):
        """
        Process a GNU tar extended sparse header, version 0.1.
        
        """
    def _proc_gnusparse_10(self, next, pax_headers, tarfile):
        """
        Process a GNU tar extended sparse header, version 1.0.
        
        """
    def _apply_pax_info(self, pax_headers, encoding, errors):
        """
        Replace fields with supplemental information from a previous
                   pax extended or global header.
        
        """
    def _decode_pax_field(self, value, encoding, fallback_encoding, fallback_errors):
        """
        Decode a single field from a pax record.
        
        """
    def _block(self, count):
        """
        Round up a byte count by BLOCKSIZE and return it,
                   e.g. _block(834) => 1024.
        
        """
    def isreg(self):
        """
        'Return True if the Tarinfo object is a regular file.'
        """
    def isfile(self):
        """
        'Return True if the Tarinfo object is a regular file.'
        """
    def isdir(self):
        """
        'Return True if it is a directory.'
        """
    def issym(self):
        """
        'Return True if it is a symbolic link.'
        """
    def islnk(self):
        """
        'Return True if it is a hard link.'
        """
    def ischr(self):
        """
        'Return True if it is a character device.'
        """
    def isblk(self):
        """
        'Return True if it is a block device.'
        """
    def isfifo(self):
        """
        'Return True if it is a FIFO.'
        """
    def issparse(self):
        """
        'Return True if it is one of character device, block device or FIFO.'
        """
def TarFile(object):
    """
    The TarFile Class provides an interface to tar archives.
    
    """
2021-03-02 20:54:00,659 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:00,659 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:00,659 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, name=None, mode="r", fileobj=None, format=None,
            tarinfo=None, dereference=None, ignore_zeros=None, encoding=None,
            errors="surrogateescape", pax_headers=None, debug=None,
            errorlevel=None, copybufsize=None):
        """
        Open an (uncompressed) tar archive `name'. `mode' is either 'r' to
                   read from an existing archive, 'a' to append data to an existing
                   file or 'w' to create a new file overwriting an existing one. `mode'
                   defaults to 'r'.
                   If `fileobj' is given, it is used for reading or writing data. If it
                   can be determined, `mode' is overridden by `fileobj's mode.
                   `fileobj' is not closed, when TarFile is closed.
        
        """
    def open(cls, name=None, mode="r", fileobj=None, bufsize=RECORDSIZE, **kwargs):
        """
        Open a tar archive for reading, writing or appending. Return
                   an appropriate TarFile class.

                   mode:
                   'r' or 'r:*' open for reading with transparent compression
                   'r:'         open for reading exclusively uncompressed
                   'r:gz'       open for reading with gzip compression
                   'r:bz2'      open for reading with bzip2 compression
                   'r:xz'       open for reading with lzma compression
                   'a' or 'a:'  open for appending, creating the file if necessary
                   'w' or 'w:'  open for writing without compression
                   'w:gz'       open for writing with gzip compression
                   'w:bz2'      open for writing with bzip2 compression
                   'w:xz'       open for writing with lzma compression

                   'x' or 'x:'  create a tarfile exclusively without compression, raise
                                an exception if the file is already created
                   'x:gz'       create a gzip compressed tarfile, raise an exception
                                if the file is already created
                   'x:bz2'      create a bzip2 compressed tarfile, raise an exception
                                if the file is already created
                   'x:xz'       create an lzma compressed tarfile, raise an exception
                                if the file is already created

                   'r|*'        open a stream of tar blocks with transparent compression
                   'r|'         open an uncompressed stream of tar blocks for reading
                   'r|gz'       open a gzip compressed stream of tar blocks
                   'r|bz2'      open a bzip2 compressed stream of tar blocks
                   'r|xz'       open an lzma compressed stream of tar blocks
                   'w|'         open an uncompressed stream for writing
                   'w|gz'       open a gzip compressed stream for writing
                   'w|bz2'      open a bzip2 compressed stream for writing
                   'w|xz'       open an lzma compressed stream for writing
        
        """
            def not_compressed(comptype):
                """
                'taropen'
                """
    def taropen(cls, name, mode="r", fileobj=None, **kwargs):
        """
        Open uncompressed tar archive name for reading or writing.
        
        """
    def gzopen(cls, name, mode="r", fileobj=None, compresslevel=9, **kwargs):
        """
        Open gzip compressed tar archive name for reading or writing.
                   Appending is not allowed.
        
        """
    def bz2open(cls, name, mode="r", fileobj=None, compresslevel=9, **kwargs):
        """
        Open bzip2 compressed tar archive name for reading or writing.
                   Appending is not allowed.
        
        """
    def xzopen(cls, name, mode="r", fileobj=None, preset=None, **kwargs):
        """
        Open lzma compressed tar archive name for reading or writing.
                   Appending is not allowed.
        
        """
    def close(self):
        """
        Close the TarFile. In write-mode, two finishing zero blocks are
                   appended to the archive.
        
        """
    def getmember(self, name):
        """
        Return a TarInfo object for member `name'. If `name' can not be
                   found in the archive, KeyError is raised. If a member occurs more
                   than once in the archive, its last occurrence is assumed to be the
                   most up-to-date version.
        
        """
    def getmembers(self):
        """
        Return the members of the archive as a list of TarInfo objects. The
                   list has the same order as the members in the archive.
        
        """
    def getnames(self):
        """
        Return the members of the archive as a list of their names. It has
                   the same order as the list returned by getmembers().
        
        """
    def gettarinfo(self, name=None, arcname=None, fileobj=None):
        """
        Create a TarInfo object from the result of os.stat or equivalent
                   on an existing file. The file is either named by `name', or
                   specified as a file object `fileobj' with a file descriptor. If
                   given, `arcname' specifies an alternative name for the file in the
                   archive, otherwise, the name is taken from the 'name' attribute of
                   'fileobj', or the 'name' argument. The name should be a text
                   string.
        
        """
    def list(self, verbose=True, *, members=None):
        """
        Print a table of contents to sys.stdout. If `verbose' is False, only
                   the names of the members are printed. If it is True, an `ls -l'-like
                   output is produced. `members' is optional and must be a subset of the
                   list returned by getmembers().
        
        """
    def add(self, name, arcname=None, recursive=True, *, filter=None):
        """
        Add the file `name' to the archive. `name' may be any type of file
                   (directory, fifo, symbolic link, etc.). If given, `arcname'
                   specifies an alternative name for the file in the archive.
                   Directories are added recursively by default. This can be avoided by
                   setting `recursive' to False. `filter' is a function
                   that expects a TarInfo object argument and returns the changed
                   TarInfo object, if it returns None the TarInfo object will be
                   excluded from the archive.
        
        """
    def addfile(self, tarinfo, fileobj=None):
        """
        Add the TarInfo object `tarinfo' to the archive. If `fileobj' is
                   given, it should be a binary file, and tarinfo.size bytes are read
                   from it and added to the archive. You can create TarInfo objects
                   directly, or by using gettarinfo().
        
        """
    def extractall(self, path=".", members=None, *, numeric_owner=False):
        """
        Extract all members from the archive to the current working
                   directory and set owner, modification time and permissions on
                   directories afterwards. `path' specifies a different directory
                   to extract to. `members' is optional and must be a subset of the
                   list returned by getmembers(). If `numeric_owner` is True, only
                   the numbers for user/group names are used and not the names.
        
        """
    def extract(self, member, path="", set_attrs=True, *, numeric_owner=False):
        """
        Extract a member from the archive to the current working directory,
                   using its full name. Its file information is extracted as accurately
                   as possible. `member' may be a filename or a TarInfo object. You can
                   specify a different directory using `path'. File attributes (owner,
                   mtime, mode) are set unless `set_attrs' is False. If `numeric_owner`
                   is True, only the numbers for user/group names are used and not
                   the names.
        
        """
    def extractfile(self, member):
        """
        Extract a member from the archive as a file object. `member' may be
                   a filename or a TarInfo object. If `member' is a regular file or a
                   link, an io.BufferedReader object is returned. Otherwise, None is
                   returned.
        
        """
2021-03-02 20:54:00,674 : INFO : tokenize_signature : --> do i ever get here?
    def _extract_member(self, tarinfo, targetpath, set_attrs=True,
                        numeric_owner=False):
        """
        Extract the TarInfo object tarinfo to a physical
                   file called targetpath.
        
        """
    def makedir(self, tarinfo, targetpath):
        """
        Make a directory called targetpath.
        
        """
    def makefile(self, tarinfo, targetpath):
        """
        Make a file called targetpath.
        
        """
    def makeunknown(self, tarinfo, targetpath):
        """
        Make a file from a TarInfo object with an unknown type
                   at targetpath.
        
        """
    def makefifo(self, tarinfo, targetpath):
        """
        Make a fifo called targetpath.
        
        """
    def makedev(self, tarinfo, targetpath):
        """
        Make a character or block device called targetpath.
        
        """
    def makelink(self, tarinfo, targetpath):
        """
        Make a (symbolic) link called targetpath. If it cannot be created
                  (platform limitation), we try to make a copy of the referenced file
                  instead of a link.
        
        """
    def chown(self, tarinfo, targetpath, numeric_owner):
        """
        Set owner of targetpath according to tarinfo. If numeric_owner
                   is True, use .gid/.uid instead of .gname/.uname. If numeric_owner
                   is False, fall back to .gid/.uid when the search based on name
                   fails.
        
        """
    def chmod(self, tarinfo, targetpath):
        """
        Set file permissions of targetpath according to tarinfo.
        
        """
    def utime(self, tarinfo, targetpath):
        """
        Set modification time of targetpath according to tarinfo.
        
        """
    def next(self):
        """
        Return the next member of the archive as a TarInfo object, when
                   TarFile is opened for reading. Return None if there is no more
                   available.
        
        """
    def _getmember(self, name, tarinfo=None, normalize=False):
        """
        Find an archive member by name from bottom to top.
                   If tarinfo is given, it is used as the starting point.
        
        """
    def _load(self):
        """
        Read through the entire archive file and look for readable
                   members.
        
        """
    def _check(self, mode=None):
        """
        Check if TarFile is still open, and if the operation's mode
                   corresponds to TarFile's mode.
        
        """
    def _find_link_target(self, tarinfo):
        """
        Find the target member of a symlink or hardlink member in the
                   archive.
        
        """
    def __iter__(self):
        """
        Provide an iterator object.
        
        """
    def _dbg(self, level, msg):
        """
        Write debugging output to sys.stderr.
        
        """
    def __enter__(self):
        """
         An exception occurred. We must not call close() because
         it would try to write end-of-archive blocks and padding.

        """
def is_tarfile(name):
    """
    Return True if name points to a tar archive that we
           are able to handle, else return False.
    
    """
def main():
    """
    'A simple command-line interface for tarfile module.'
    """
