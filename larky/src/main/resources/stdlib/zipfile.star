def BadZipFile(Exception):
    """

        Raised when writing a zipfile, the zipfile requires ZIP64 extensions
        and those extensions are disabled.
    
    """
def _strip_extra(extra, xids):
    """
     Remove Extra Fields with specified IDs.

    """
def _check_zipfile(fp):
    """
     file has correct magic number
    """
def is_zipfile(filename):
    """
    Quickly see if a file is a ZIP file by checking the magic number.

        The filename argument may be a file or file-like object too.
    
    """
def _EndRecData64(fpin, offset, endrec):
    """

        Read the ZIP64 end-of-archive records and use that to update endrec
    
    """
def _EndRecData(fpin):
    """
    Return data from the "End of Central Directory" record, or None.

        The data is a list of the nine items in the ZIP "End of central dir"
        record followed by a tenth item, the file seek offset of this record.
    """
def ZipInfo (object):
    """
    Class with attributes describing each file in the ZIP archive.
    """
    def __init__(self, filename="NoName", date_time=(1980,1,1,0,0,0)):
        """
         Original file name in archive
        """
    def __repr__(self):
        """
        '<%s filename=%r'
        """
    def FileHeader(self, zip64=None):
        """
        Return the per-file header as a bytes object.
        """
    def _encodeFilenameFlags(self):
        """
        'ascii'
        """
    def _decodeExtra(self):
        """
         Try to decode the extra field.

        """
    def from_file(cls, filename, arcname=None, *, strict_timestamps=True):
        """
        Construct an appropriate ZipInfo for a file on the filesystem.

                filename should be the path to a file or directory on the filesystem.

                arcname is the name which it will have within the archive (by default,
                this will be the same as filename, but without a drive letter and with
                leading path separators removed).
        
        """
    def is_dir(self):
        """
        Return True if this archive member is a directory.
        """
def _gen_crc(crc):
    """
     ZIP supports a password-based form of encryption. Even though known
     plaintext attacks have been found against it, it is still useful
     to be able to get data out of such a file.

     Usage:
         zd = _ZipDecrypter(mypwd)
         plain_bytes = zd(cypher_bytes)


    """
def _ZipDecrypter(pwd):
    """
    Compute the CRC32 primitive on one byte.
    """
    def update_keys(c):
        """
        Decrypt a bytes object.
        """
def LZMACompressor:
    """
    'id'
    """
    def compress(self, data):
        """
        b''
        """
    def decompress(self, data):
        """
        b''
        """
def _check_compression(compression):
    """
    Compression requires the (missing) zlib module
    """
def _get_compressor(compress_type, compresslevel=None):
    """
     compresslevel is ignored for ZIP_LZMA

    """
def _get_decompressor(compress_type):
    """
    compression type %d (%s)
    """
def _SharedFile:
    """
    Can't reposition in the ZIP file while 
    there is an open writing handle on it. 
    Close the writing handle before trying to read.
    """
    def read(self, n=-1):
        """
        Can't read from the ZIP file while there 
        is an open writing handle on it. 
        Close the writing handle before trying to read.
        """
    def close(self):
        """
         Provide the tell method for unseekable stream

        """
def _Tellable:
    """
    File-like object for reading an archive member.
           Is returned by ZipFile.open().
    
    """
2021-03-02 20:46:03,569 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, fileobj, mode, zipinfo, pwd=None,
                 close_fileobj=False):
        """
        b''
        """
    def _init_decrypter(self):
        """
         The first 12 bytes in the cypher stream is an encryption header
          used to strengthen the algorithm. The first 11 bytes are
          completely random, while the 12th contains the MSB of the CRC,
          or the MSB of the file time depending on the header type
          and is used to check the correctness of the password.

        """
    def __repr__(self):
        """
        '<%s.%s'
        """
    def readline(self, limit=-1):
        """
        Read and return a line from the stream.

                If limit is specified, at most limit bytes will be read.
        
        """
    def peek(self, n=1):
        """
        Returns buffered bytes without advancing the position.
        """
    def readable(self):
        """
        Read and return up to n bytes.
                If the argument is omitted, None, or negative, data is read and returned until EOF is reached.
        
        """
    def _update_crc(self, newdata):
        """
         Update the CRC using the given data.

        """
    def read1(self, n):
        """
        Read up to n bytes with at most one read() system call.
        """
    def _read1(self, n):
        """
         Read up to n compressed bytes with at most one read() system call,
         decrypt and decompress them.

        """
    def _read2(self, n):
        """
        b''
        """
    def close(self):
        """
        underlying stream is not seekable
        """
    def tell(self):
        """
        underlying stream is not seekable
        """
def _ZipWriteFile(io.BufferedIOBase):
    """
    'I/O operation on closed file.'
    """
    def close(self):
        """
         Flush any data from the compressor, and update header info

        """
def ZipFile:
    """
     Class with methods to open, read, write, close, list zip files.

        z = ZipFile(file, mode="r", compression=ZIP_STORED, allowZip64=True,
                    compresslevel=None)

        file: Either the path to the file, or a file-like object.
              If it is a path, the file will be opened and closed by ZipFile.
        mode: The mode can be either read 'r', write 'w', exclusive create 'x',
              or append 'a'.
        compression: ZIP_STORED (no compression), ZIP_DEFLATED (requires zlib),
                     ZIP_BZIP2 (requires bz2) or ZIP_LZMA (requires lzma).
        allowZip64: if True ZipFile will create files with ZIP64 extensions when
                    needed, otherwise it will raise an exception when this would
                    be necessary.
        compresslevel: None (default for the given compression type) or an integer
                       specifying the level to pass to the compressor.
                       When using ZIP_STORED or ZIP_LZMA this keyword has no effect.
                       When using ZIP_DEFLATED integers 0 through 9 are accepted.
                       When using ZIP_BZIP2 integers 1 through 9 are accepted.

    
    """
2021-03-02 20:46:03,579 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, file, mode="r", compression=ZIP_STORED, allowZip64=True,
                 compresslevel=None, *, strict_timestamps=True):
        """
        Open the ZIP file with mode read 'r', write 'w', exclusive create 'x',
                or append 'a'.
        """
    def __enter__(self):
        """
        '<%s.%s'
        """
    def _RealGetContents(self):
        """
        Read in the table of contents for the ZIP file.
        """
    def namelist(self):
        """
        Return a list of file names in the archive.
        """
    def infolist(self):
        """
        Return a list of class ZipInfo instances for files in the
                archive.
        """
    def printdir(self, file=None):
        """
        Print a table of contents for the zip file.
        """
    def testzip(self):
        """
        Read all the files and check the CRC.
        """
    def getinfo(self, name):
        """
        Return the instance of ZipInfo given 'name'.
        """
    def setpassword(self, pwd):
        """
        Set default password for encrypted files.
        """
    def comment(self):
        """
        The comment text associated with the ZIP file.
        """
    def comment(self, comment):
        """
        comment: expected bytes, got %s
        """
    def read(self, name, pwd=None):
        """
        Return file bytes for name.
        """
    def open(self, name, mode="r", pwd=None, *, force_zip64=False):
        """
        Return file-like object for 'name'.

                name is a string for the file name within the ZIP file, or a ZipInfo
                object.

                mode should be 'r' to read a file already in the ZIP file, or 'w' to
                write to a file newly added to the archive.

                pwd is the password to decrypt files (only used for reading).

                When writing, if the file size is not known in advance but may exceed
                2 GiB, pass force_zip64 to use the ZIP64 format, which can handle large
                files.  If the size is known in advance, it is best to pass a ZipInfo
                instance for name, with zinfo.file_size set.
        
        """
    def _open_to_write(self, zinfo, force_zip64=False):
        """
        force_zip64 is True, but allowZip64 was False when opening 
        the ZIP file.

        """
    def extract(self, member, path=None, pwd=None):
        """
        Extract a member from the archive to the current working directory,
                   using its full name. Its file information is extracted as accurately
                   as possible. `member' may be a filename or a ZipInfo object. You can
                   specify a different directory using `path'.
        
        """
    def extractall(self, path=None, members=None, pwd=None):
        """
        Extract all members from the archive to the current working
                   directory. `path' specifies a different directory to extract to.
                   `members' is optional and must be a subset of the list returned
                   by namelist().
        
        """
    def _sanitize_windows_name(cls, arcname, pathsep):
        """
        Replace bad characters and remove trailing dots from parts.
        """
    def _extract_member(self, member, targetpath, pwd):
        """
        Extract the ZipInfo object 'member' to a physical
                   file on the path targetpath.
        
        """
    def _writecheck(self, zinfo):
        """
        Check for errors before writing a file to the archive.
        """
2021-03-02 20:46:03,592 : INFO : tokenize_signature : --> do i ever get here?
    def write(self, filename, arcname=None,
              compress_type=None, compresslevel=None):
        """
        Put the bytes from filename into the archive under the name
                arcname.
        """
2021-03-02 20:46:03,594 : INFO : tokenize_signature : --> do i ever get here?
    def writestr(self, zinfo_or_arcname, data,
                 compress_type=None, compresslevel=None):
        """
        Write a file into the archive.  The contents is 'data', which
                may be either a 'str' or a 'bytes' instance; if it is a 'str',
                it is encoded as UTF-8 first.
                'zinfo_or_arcname' is either a ZipInfo instance or
                the name of the file in the archive.
        """
    def __del__(self):
        """
        Call the "close()" method in case the user forgot.
        """
    def close(self):
        """
        Close the file, and for mode 'w', 'x' and 'a' write the ending
                records.
        """
    def _write_end_record(self):
        """
         write central directory
        """
    def _fpclose(self, fp):
        """
        Class to create ZIP archives with Python library files and packages.
        """
2021-03-02 20:46:03,598 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, file, mode="r", compression=ZIP_STORED,
                 allowZip64=True, optimize=-1):
        """

        """
    def _get_codename(self, pathname, basename):
        """
        Return (filename, archivename) for the path.

                Given a module name path, return the correct file path and
                archive name, compiling if necessary.  For example, given
                /python/lib/string, return (/python/lib/string.pyc, string).
        
        """
        def _compile(file, optimize=-1):
            """
            Compiling
            """
def _parents(path):
    """

        Given a path with elements separated by
        posixpath.sep, generate all parents of that path.

        >>> list(_parents('b/d'))
        ['b']
        >>> list(_parents('/b/d/'))
        ['/b']
        >>> list(_parents('b/d/f/'))
        ['b/d', 'b']
        >>> list(_parents('b'))
        []
        >>> list(_parents(''))
        []
    
    """
def _ancestry(path):
    """

        Given a path with elements separated by
        posixpath.sep, generate all elements of that path

        >>> list(_ancestry('b/d'))
        ['b/d', 'b']
        >>> list(_ancestry('/b/d/'))
        ['/b/d', '/b']
        >>> list(_ancestry('b/d/f/'))
        ['b/d/f', 'b/d', 'b']
        >>> list(_ancestry('b'))
        ['b']
        >>> list(_ancestry(''))
        []
    
    """
def _difference(minuend, subtrahend):
    """

        Return items in minuend not in subtrahend, retaining order
        with O(1) lookup.
    
    """
def CompleteDirs(ZipFile):
    """

        A ZipFile subclass that ensures that implied directories
        are always included in the namelist.
    
    """
    def _implied_dirs(names):
        """

                If the name represents a directory, return that name
                as a directory (with the trailing slash).
        
        """
    def make(cls, source):
        """

                Given a source (filename or zipfile), return an
                appropriate CompleteDirs subclass.
        
        """
def FastLookup(CompleteDirs):
    """

        ZipFile subclass to ensure implicit
        dirs exist and are resolved rapidly.
    
    """
    def namelist(self):
        """

            A pathlib-compatible interface for zip files.

            Consider a zip file with this structure::

                .
                ├── a.txt
                └── b
                    ├── c.txt
                    └── d
                        └── e.txt

            >>> data = io.BytesIO()
            >>> zf = ZipFile(data, 'w')
            >>> zf.writestr('a.txt', 'content of a')
            >>> zf.writestr('b/c.txt', 'content of c')
            >>> zf.writestr('b/d/e.txt', 'content of e')
            >>> zf.filename = 'abcde.zip'

            Path accepts the zipfile object itself or a filename

            >>> root = Path(zf)

            From there, several path operations are available.

            Directory iteration (including the zip file itself):

            >>> a, b = root.iterdir()
            >>> a
            Path('abcde.zip', 'a.txt')
            >>> b
            Path('abcde.zip', 'b/')

            name property:

            >>> b.name
            'b'

            join with divide operator:

            >>> c = b / 'c.txt'
            >>> c
            Path('abcde.zip', 'b/c.txt')
            >>> c.name
            'c.txt'

            Read text:

            >>> c.read_text()
            'content of c'

            existence:

            >>> c.exists()
            True
            >>> (b / 'missing.txt').exists()
            False

            Coercion to string:

            >>> str(c)
            'abcde.zip/b/c.txt'
    
        """
    def __init__(self, root, at=""):
        """
        /
        """
    def read_text(self, *args, **kwargs):
        """
        /
        """
    def _next(self, at):
        """
        /
        """
    def is_file(self):
        """
        Can't listdir a file
        """
    def __str__(self):
        """
        '/'
        """
def main(args=None):
    """
    'A simple command-line interface for zipfile module.'
    """
        def addToZip(zf, path, zippath):
            """
             else: ignore


            """
