def _exists(fn):
    """
    Look at the type of all args and divine their implied return type.
    """
def _sanitize_params(prefix, suffix, dir):
    """
    Common parameter processing for most APIs in this module.
    """
def _RandomNameSequence:
    """
    An instance of _RandomNameSequence generates an endless
        sequence of unpredictable strings which can safely be incorporated
        into file names.  Each string is eight characters long.  Multiple
        threads can safely use the same instance at the same time.

        _RandomNameSequence is an iterator.
    """
    def rng(self):
        """
        '_rng_pid'
        """
    def __iter__(self):
        """
        ''
        """
def _candidate_tempdir_list():
    """
    Generate a list of candidate temporary directories which
        _get_default_tempdir will try.
    """
def _get_default_tempdir():
    """
    Calculate the default directory to use for temporary files.
        This routine should be called exactly once.

        We determine whether or not a candidate temp dir is usable by
        trying to create and write to a file in that directory.  If this
        is successful, the test file is deleted.  To prevent denial of
        service, the name of the test file must be randomized.
    """
def _get_candidate_names():
    """
    Common setup sequence for all user-callable interfaces.
    """
def _mkstemp_inner(dir, pre, suf, flags, output_type):
    """
    Code common to mkstemp, TemporaryFile, and NamedTemporaryFile.
    """
def gettempprefix():
    """
    The default prefix for temporary directories.
    """
def gettempprefixb():
    """
    The default prefix for temporary directories as bytes.
    """
def gettempdir():
    """
    Accessor for tempfile.tempdir.
    """
def gettempdirb():
    """
    A bytes version of tempfile.gettempdir().
    """
def mkstemp(suffix=None, prefix=None, dir=None, text=False):
    """
    User-callable function to create and return a unique temporary
        file.  The return value is a pair (fd, name) where fd is the
        file descriptor returned by os.open, and name is the filename.

        If 'suffix' is not None, the file name will end with that suffix,
        otherwise there will be no suffix.

        If 'prefix' is not None, the file name will begin with that prefix,
        otherwise a default prefix is used.

        If 'dir' is not None, the file will be created in that directory,
        otherwise a default directory is used.

        If 'text' is specified and true, the file is opened in text
        mode.  Else (the default) the file is opened in binary mode.  On
        some operating systems, this makes no difference.

        If any of 'suffix', 'prefix' and 'dir' are not None, they must be the
        same type.  If they are bytes, the returned name will be bytes; str
        otherwise.

        The file is readable and writable only by the creating user ID.
        If the operating system uses permission bits to indicate whether a
        file is executable, the file is executable by no one. The file
        descriptor is not inherited by children of this process.

        Caller is responsible for deleting the file when done with it.
    
    """
def mkdtemp(suffix=None, prefix=None, dir=None):
    """
    User-callable function to create and return a unique temporary
        directory.  The return value is the pathname of the directory.

        Arguments are as for mkstemp, except that the 'text' argument is
        not accepted.

        The directory is readable, writable, and searchable only by the
        creating user.

        Caller is responsible for deleting the directory when done with it.
    
    """
def mktemp(suffix="", prefix=template, dir=None):
    """
    User-callable function to return a unique temporary file name.  The
        file is not created.

        Arguments are similar to mkstemp, except that the 'text' argument is
        not accepted, and suffix=None, prefix=None and bytes file names are not
        supported.

        THIS FUNCTION IS UNSAFE AND SHOULD NOT BE USED.  The file name may
        refer to a file that did not exist at some point, but by the time
        you get around to creating it, someone else may have beaten you to
        the punch.
    
    """
def _TemporaryFileCloser:
    """
    A separate object allowing proper closing of a temporary file's
        underlying file object, without adding a __del__ method to the
        temporary file.
    """
    def __init__(self, file, name, delete=True):
        """
         NT provides delete-on-close as a primitive, so we don't need
         the wrapper to do anything special.  We still use it so that
         file.name is useful (i.e. not "(fdopen)") with NamedTemporaryFile.

        """
        def close(self, unlink=_os.unlink):
            """
             Need to ensure the file is deleted on __del__

            """
        def __del__(self):
            """
            Temporary file wrapper

                This class provides a wrapper around files opened for
                temporary use.  In particular, it seeks to automatically
                remove the file when it is no longer needed.
    
            """
    def __init__(self, file, name, delete=True):
        """
         Attribute lookups are delegated to the underlying file
         and cached for non-numeric results
         (i.e. methods are cached, closed and friends are not)

        """
            def func_wrapper(*args, **kwargs):
                """
                 Avoid closing the file as long as the wrapper is alive,
                 see issue #18879.

                """
    def __enter__(self):
        """
         Need to trap __exit__ as well to ensure the file gets
         deleted when used in a with statement

        """
    def __exit__(self, exc, value, tb):
        """

                Close the temporary file, possibly deleting it.
        
        """
    def __iter__(self):
        """
         Don't return iter(self.file), but yield from it to avoid closing
         file as long as it's being used as iterator (see issue #23700).  We
         can't use 'yield from' here because iter(file) returns the file
         object itself, which has a close method, and thus the file would get
         closed when the generator is finalized, due to PEP380 semantics.

        """
2021-03-02 20:46:04,000 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:04,001 : INFO : tokenize_signature : --> do i ever get here?
def NamedTemporaryFile(mode='w+b', buffering=-1, encoding=None,
                       newline=None, suffix=None, prefix=None,
                       dir=None, delete=True, *, errors=None):
    """
    Create and return a temporary file.
        Arguments:
        'prefix', 'suffix', 'dir' -- as for mkstemp.
        'mode' -- the mode argument to io.open (default "w+b").
        'buffering' -- the buffer size argument to io.open (default -1).
        'encoding' -- the encoding argument to io.open (default None)
        'newline' -- the newline argument to io.open (default None)
        'delete' -- whether the file is deleted on close (default True).
        'errors' -- the errors argument to io.open (default None)
        The file is created as mkstemp() would do it.

        Returns an object with a file-like interface; the name of the file
        is accessible as its 'name' attribute.  The file will be automatically
        deleted when it is closed unless the 'delete' argument is set to False.
    
    """
2021-03-02 20:46:04,002 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:04,002 : INFO : tokenize_signature : --> do i ever get here?
    def TemporaryFile(mode='w+b', buffering=-1, encoding=None,
                      newline=None, suffix=None, prefix=None,
                      dir=None, *, errors=None):
        """
        Create and return a temporary file.
                Arguments:
                'prefix', 'suffix', 'dir' -- as for mkstemp.
                'mode' -- the mode argument to io.open (default "w+b").
                'buffering' -- the buffer size argument to io.open (default -1).
                'encoding' -- the encoding argument to io.open (default None)
                'newline' -- the newline argument to io.open (default None)
                'errors' -- the errors argument to io.open (default None)
                The file is created as mkstemp() would do it.

                Returns an object with a file-like interface.  The file has no
                name, and will cease to exist when it is closed.
        
        """
def SpooledTemporaryFile:
    """
    Temporary file wrapper, specialized to switch from BytesIO
        or StringIO to a real file when it exceeds a certain size or
        when a fileno is needed.
    
    """
2021-03-02 20:46:04,003 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:04,003 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, max_size=0, mode='w+b', buffering=-1,
                 encoding=None, newline=None,
                 suffix=None, prefix=None, dir=None, *, errors=None):
        """
        'b'
        """
    def _check(self, file):
        """
        'buffer'
        """
    def __enter__(self):
        """
        Cannot enter context with closed file
        """
    def __exit__(self, exc, value, tb):
        """
         file protocol

        """
    def __iter__(self):
        """
        'mode'
        """
    def name(self):
        """
        Create and return a temporary directory.  This has the same
            behavior as mkdtemp but can be used as a context manager.  For
            example:

                with TemporaryDirectory() as tmpdir:
                    ...

            Upon exiting the context, the directory and everything contained
            in it are removed.
    
        """
    def __init__(self, suffix=None, prefix=None, dir=None):
        """
        Implicitly cleaning up {!r}
        """
    def _rmtree(cls, name):
        """
         PermissionError is raised on FreeBSD for directories

        """
    def _cleanup(cls, name, warn_message):
        """
        <{} {!r}>
        """
    def __enter__(self):
