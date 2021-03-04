def _ignore_error(exception):
    """
    'errno'
    """
def _is_wildcard_pattern(pat):
    """
     Whether this pattern needs actual matching using fnmatch, or can
     be looked up directly as a file.

    """
def _Flavour(object):
    """
    A flavour implements a particular (platform-specific) set of path
        semantics.
    """
    def __init__(self):
        """
        ''
        """
    def join_parsed_parts(self, drv, root, parts, drv2, root2, parts2):
        """

                Join the two paths represented by the respective
                (drive, root, parts) tuples.  Return a new (drive, root, parts) tuple.
        
        """
def _WindowsFlavour(_Flavour):
    """
     Reference for Windows paths can be found at
     http://msdn.microsoft.com/en-us/library/aa365247%28v=vs.85%29.aspx


    """
    def splitroot(self, part, sep=sep):
        """
         XXX extended paths should also disable the collapsing of ".
         components (according to MSDN docs).

        """
    def casefold(self, s):
        """
         End of the path after the first one not found
        """
    def _split_extended_path(self, s, ext_prefix=ext_namespace_prefix):
        """
        ''
        """
    def _ext_to_normal(self, s):
        """
         Turn back an extended path into a normal DOS-like path

        """
    def is_reserved(self, parts):
        """
         NOTE: the rules for reserved names seem somewhat complicated
         (e.g. r"..\NUL" is reserved but not r"foo\NUL").
         We err on the side of caution and return True for paths which are
         not considered reserved by Windows.

        """
    def make_uri(self, path):
        """
         Under Windows, file URIs use the UTF-8 encoding.

        """
    def gethomedir(self, username):
        """
        'USERPROFILE'
        """
def _PosixFlavour(_Flavour):
    """
    '/'
    """
    def splitroot(self, part, sep=sep):
        """
         According to POSIX path resolution:
         http://pubs.opengroup.org/onlinepubs/009695399/basedefs/xbd_chap04.html#tag_04_11
         "A pathname that begins with two successive slashes may be
         interpreted in an implementation-defined manner, although more
         than two leading slashes shall be treated as a single slash".

        """
    def casefold(self, s):
        """
        ''
        """
    def is_reserved(self, parts):
        """
         We represent the path using the local filesystem encoding,
         for portability to other applications.

        """
    def gethomedir(self, username):
        """
        'HOME'
        """
def _Accessor:
    """
    An accessor implements a particular (system-specific or not) way of
        accessing paths on the filesystem.
    """
def _NormalAccessor(_Accessor):
    """
    lchmod
    """
        def lchmod(self, pathobj, mode):
            """
            lchmod() not available on this system
            """
        def link_to(self, target):
            """
            os.link() not available on this system
            """
            def symlink(a, b, target_is_directory):
                """
                symlink() not available on this system
                """
        def symlink(a, b, target_is_directory):
            """
             Helper for resolve()

            """
def _make_selector(pattern_parts, flavour):
    """
    '**'
    """
def _Selector:
    """
    A selector matches a specific glob pattern part against the children
        of a given path.
    """
    def __init__(self, child_parts, flavour):
        """
        Iterate over all child paths of `parent_path` matched by this
                selector.  This can contain parent_path itself.
        """
def _TerminatingSelector:
    """
     "entry.is_dir()" can raise PermissionError
     in some cases (see bpo-38894), which is not
     among the errors ignored by _ignore_error()

    """
def _RecursiveWildcardSelector(_Selector):
    """

     Public API



    """
def _PathParents(Sequence):
    """
    This object provides sequence-like access to the logical ancestors
        of a path.  Don't try to construct it yourself.
    """
    def __init__(self, path):
        """
         We don't store the instance to avoid reference cycles

        """
    def __len__(self):
        """
        <{}.parents>
        """
def PurePath(object):
    """
    Base class for manipulating paths without I/O.

        PurePath represents a filesystem path and offers operations which
        don't imply any actual filesystem I/O.  Depending on your system,
        instantiating a PurePath will return either a PurePosixPath or a
        PureWindowsPath object.  You can also instantiate either of these classes
        directly, regardless of your system.
    
    """
    def __new__(cls, *args):
        """
        Construct a PurePath from one or several strings and or existing
                PurePath objects.  The strings and path objects are combined so as
                to yield a canonicalized path, which is incorporated into the
                new PurePath object.
        
        """
    def __reduce__(self):
        """
         Using the parts tuple helps share interned path parts
         when pickling related paths.

        """
    def _parse_args(cls, args):
        """
         This is useful when you don't want to create an instance, just
         canonicalize some constructor arguments.

        """
    def _from_parts(cls, args, init=True):
        """
         We need to call _parse_args on the instance, so as to get the
         right flavour.

        """
    def _from_parsed_parts(cls, drv, root, parts, init=True):
        """
         Overridden in concrete Path

        """
    def _make_child(self, args):
        """
        Return the string representation of the path, suitable for
                passing to system calls.
        """
    def __fspath__(self):
        """
        Return the string representation of the path with forward (/)
                slashes.
        """
    def __bytes__(self):
        """
        Return the bytes representation of the path.  This is only
                recommended to use under Unix.
        """
    def __repr__(self):
        """
        {}({!r})
        """
    def as_uri(self):
        """
        Return the path as a 'file' URI.
        """
    def _cparts(self):
        """
         Cached casefolded parts, for hashing and comparison

        """
    def __eq__(self, other):
        """
        '_drv'
        """
    def anchor(self):
        """
        The concatenation of the drive and root, or ''.
        """
    def name(self):
        """
        The final path component, if any.
        """
    def suffix(self):
        """

                The final component's last suffix, if any.

                This includes the leading period. For example: '.txt'
        
        """
    def suffixes(self):
        """

                A list of the final component's suffixes, if any.

                These include the leading periods. For example: ['.tar', '.gz']
        
        """
    def stem(self):
        """
        The final path component, minus its last suffix.
        """
    def with_name(self, name):
        """
        Return a new path with the file name changed.
        """
    def with_suffix(self, suffix):
        """
        Return a new path with the file suffix changed.  If the path
                has no suffix, add given suffix.  If the given suffix is an empty
                string, remove the suffix from the path.
        
        """
    def relative_to(self, *other):
        """
        Return the relative path to another path identified by the passed
                arguments.  If the operation is not possible (because this is not
                a subpath of the other path), raise ValueError.
        
        """
    def parts(self):
        """
        An object providing sequence-like access to the
                components in the filesystem path.
        """
    def joinpath(self, *args):
        """
        Combine this path with one or several arguments, and return a
                new path representing either a subpath (if all arguments are relative
                paths) or a totally different path (if one of the arguments is
                anchored).
        
        """
    def __truediv__(self, key):
        """
        The logical parent of the path.
        """
    def parents(self):
        """
        A sequence of this path's logical parents.
        """
    def is_absolute(self):
        """
        True if the path is absolute (has both a root and, if applicable,
                a drive).
        """
    def is_reserved(self):
        """
        Return True if the path contains one of the special names reserved
                by the system, if any.
        """
    def match(self, path_pattern):
        """

                Return True if this path matches the given pattern.
        
        """
def PurePosixPath(PurePath):
    """
    PurePath subclass for non-Windows systems.

        On a POSIX system, instantiating a PurePath should return this object.
        However, you can also instantiate it directly on any system.
    
    """
def PureWindowsPath(PurePath):
    """
    PurePath subclass for Windows systems.

        On a Windows system, instantiating a PurePath should return this object.
        However, you can also instantiate it directly on any system.
    
    """
def Path(PurePath):
    """
    PurePath subclass that can make system calls.

        Path represents a filesystem path but unlike PurePath, also offers
        methods to do system calls on path objects. Depending on your system,
        instantiating a Path will return either a PosixPath or a WindowsPath
        object. You can also instantiate a PosixPath or WindowsPath directly,
        but cannot instantiate a WindowsPath on a POSIX system or vice versa.
    
    """
    def __new__(cls, *args, **kwargs):
        """
        'nt'
        """
2021-03-02 20:54:29,183 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:29,183 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:29,183 : INFO : tokenize_signature : --> do i ever get here?
    def _init(self,
              # Private non-constructor arguments
              template=None,
              ):
        """
         This is an optimization used for dir walking.  `part` must be
         a single part relative to this path.

        """
    def __enter__(self):
        """
        I/O operation on closed path
        """
    def _opener(self, name, flags, mode=0o666):
        """
         A stub for the opener argument to built-in open()

        """
    def _raw_open(self, flags, mode=0o777):
        """

                Open the file pointed by this path and return a file descriptor,
                as os.open() does.
        
        """
    def cwd(cls):
        """
        Return a new path pointing to the current working directory
                (as returned by os.getcwd()).
        
        """
    def home(cls):
        """
        Return a new path pointing to the user's home directory (as
                returned by os.path.expanduser('~')).
        
        """
    def samefile(self, other_path):
        """
        Return whether other_path is the same or not as this file
                (as returned by os.path.samefile()).
        
        """
    def iterdir(self):
        """
        Iterate over the files in this directory.  Does not yield any
                result for the special paths '.' and '..'.
        
        """
    def glob(self, pattern):
        """
        Iterate over this subtree and yield all existing files (of any
                kind, including directories) matching the given relative pattern.
        
        """
    def rglob(self, pattern):
        """
        Recursively yield all existing files (of any kind, including
                directories) matching the given relative pattern, anywhere in
                this subtree.
        
        """
    def absolute(self):
        """
        Return an absolute version of this path.  This function works
                even if the path doesn't point to anything.

                No normalization is done, i.e. all '.' and '..' will be kept along.
                Use resolve() to get the canonical path to a file.
        
        """
    def resolve(self, strict=False):
        """

                Make the path absolute, resolving all symlinks on the way and also
                normalizing it (for example turning slashes into backslashes under
                Windows).
        
        """
    def stat(self):
        """

                Return the result of the stat() system call on this path, like
                os.stat() does.
        
        """
    def owner(self):
        """

                Return the login name of the file owner.
        
        """
    def group(self):
        """

                Return the group name of the file gid.
        
        """
2021-03-02 20:54:29,187 : INFO : tokenize_signature : --> do i ever get here?
    def open(self, mode='r', buffering=-1, encoding=None,
             errors=None, newline=None):
        """

                Open the file pointed by this path and return a file object, as
                the built-in open() function does.
        
        """
    def read_bytes(self):
        """

                Open the file in bytes mode, read it, and close the file.
        
        """
    def read_text(self, encoding=None, errors=None):
        """

                Open the file in text mode, read it, and close the file.
        
        """
    def write_bytes(self, data):
        """

                Open the file in bytes mode, write to it, and close the file.
        
        """
    def write_text(self, data, encoding=None, errors=None):
        """

                Open the file in text mode, write to it, and close the file.
        
        """
    def touch(self, mode=0o666, exist_ok=True):
        """

                Create this file with the given access mode, if it doesn't exist.
        
        """
    def mkdir(self, mode=0o777, parents=False, exist_ok=False):
        """

                Create a new directory at this given path.
        
        """
    def chmod(self, mode):
        """

                Change the permissions of the path, like os.chmod().
        
        """
    def lchmod(self, mode):
        """

                Like chmod(), except if the path points to a symlink, the symlink's
                permissions are changed, rather than its target's.
        
        """
    def unlink(self, missing_ok=False):
        """

                Remove this file or link.
                If the path is a directory, use rmdir() instead.
        
        """
    def rmdir(self):
        """

                Remove this directory.  The directory must be empty.
        
        """
    def lstat(self):
        """

                Like stat(), except if the path points to a symlink, the symlink's
                status information is returned, rather than its target's.
        
        """
    def link_to(self, target):
        """

                Create a hard link pointing to a path named target.
        
        """
    def rename(self, target):
        """

                Rename this path to the given path,
                and return a new Path instance pointing to the given path.
        
        """
    def replace(self, target):
        """

                Rename this path to the given path, clobbering the existing
                destination if it exists, and return a new Path instance
                pointing to the given path.
        
        """
    def symlink_to(self, target, target_is_directory=False):
        """

                Make this path a symlink pointing to the given path.
                Note the order of arguments (self, target) is the reverse of os.symlink's.
        
        """
    def exists(self):
        """

                Whether this path exists.
        
        """
    def is_dir(self):
        """

                Whether this path is a directory.
        
        """
    def is_file(self):
        """

                Whether this path is a regular file (also True for symlinks pointing
                to regular files).
        
        """
    def is_mount(self):
        """

                Check if this path is a POSIX mount point
        
        """
    def is_symlink(self):
        """

                Whether this path is a symbolic link.
        
        """
    def is_block_device(self):
        """

                Whether this path is a block device.
        
        """
    def is_char_device(self):
        """

                Whether this path is a character device.
        
        """
    def is_fifo(self):
        """

                Whether this path is a FIFO.
        
        """
    def is_socket(self):
        """

                Whether this path is a socket.
        
        """
    def expanduser(self):
        """
         Return a new path with expanded ~ and ~user constructs
                (as returned by os.path.expanduser)
        
        """
def PosixPath(Path, PurePosixPath):
    """
    Path subclass for non-Windows systems.

        On a POSIX system, instantiating a Path should return this object.
    
    """
def WindowsPath(Path, PureWindowsPath):
    """
    Path subclass for Windows systems.

        On a Windows system, instantiating a Path should return this object.
    
    """
    def owner(self):
        """
        Path.owner() is unsupported on this system
        """
    def group(self):
        """
        Path.group() is unsupported on this system
        """
    def is_mount(self):
        """
        Path.is_mount() is unsupported on this system
        """
