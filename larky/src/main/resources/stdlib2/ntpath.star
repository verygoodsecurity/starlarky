def _get_bothseps(path):
    """
    b'\\/'
    """
def normcase(s):
    """
    Normalize case of pathname.

        Makes all characters lowercase and all slashes into backslashes.
    """
def isabs(s):
    """
    Test whether a path is absolute
    """
def join(path, *paths):
    """
    b'\\'
    """
def splitdrive(p):
    """
    Split a pathname into drive/UNC sharepoint and relative path specifiers.
        Returns a 2-tuple (drive_or_unc, path); either part may be empty.

        If you assign
            result = splitdrive(p)
        It is always true that:
            result[0] + result[1] == p

        If the path contained a drive letter, drive_or_unc will contain everything
        up to and including the colon.  e.g. splitdrive("c:/dir") returns ("c:", "/dir")

        If the path contained a UNC path, the drive_or_unc will contain the host name
        and share up to but not including the fourth directory separator character.
        e.g. splitdrive("//host/computer/dir") returns ("//host/computer", "/dir")

        Paths cannot contain both a drive letter and a UNC path.

    
    """
def split(p):
    """
    Split a pathname.

        Return tuple (head, tail) where tail is everything after the final slash.
        Either part may be empty.
    """
def splitext(p):
    """
    b'\\'
    """
def basename(p):
    """
    Returns the final component of a pathname
    """
def dirname(p):
    """
    Returns the directory component of a pathname
    """
def islink(path):
    """
    Test whether a path is a symbolic link.
        This will always return false for Windows prior to 6.0.
    
    """
def lexists(path):
    """
    Test whether a path exists.  Returns True for broken symbolic links
    """
def ismount(path):
    """
    Test whether a path is a mount point (a drive root, the root of a
        share, or a mounted volume)
    """
def expanduser(path):
    """
    Expand ~ and ~user constructs.

        If user or $HOME is unknown, do nothing.
    """
def expandvars(path):
    """
    Expand shell variables of the forms $var, ${var} and %var%.

        Unknown variables are left unchanged.
    """
def normpath(path):
    """
    Normalize path, eliminating double slashes, etc.
    """
def _abspath_fallback(path):
    """
    Return the absolute version of a path as a fallback function in case
        `nt._getfullpathname` is not available or raises OSError. See bpo-31047 for
        more.

    
    """
    def abspath(path):
        """
        Return the absolute version of a path.
        """
    def _readlink_deep(path):
        """
         These error codes indicate that we should stop reading links and
         return the path we currently have.
         1: ERROR_INVALID_FUNCTION
         2: ERROR_FILE_NOT_FOUND
         3: ERROR_DIRECTORY_NOT_FOUND
         5: ERROR_ACCESS_DENIED
         21: ERROR_NOT_READY (implies drive with no media)
         32: ERROR_SHARING_VIOLATION (probably an NTFS paging file)
         50: ERROR_NOT_SUPPORTED (implies no support for reparse points)
         67: ERROR_BAD_NET_NAME (implies remote server unavailable)
         87: ERROR_INVALID_PARAMETER
         4390: ERROR_NOT_A_REPARSE_POINT
         4392: ERROR_INVALID_REPARSE_DATA
         4393: ERROR_REPARSE_TAG_INVALID

        """
    def _getfinalpathname_nonstrict(path):
        """
         These error codes indicate that we should stop resolving the path
         and return the value we currently have.
         1: ERROR_INVALID_FUNCTION
         2: ERROR_FILE_NOT_FOUND
         3: ERROR_DIRECTORY_NOT_FOUND
         5: ERROR_ACCESS_DENIED
         21: ERROR_NOT_READY (implies drive with no media)
         32: ERROR_SHARING_VIOLATION (probably an NTFS paging file)
         50: ERROR_NOT_SUPPORTED
         67: ERROR_BAD_NET_NAME (implies remote server unavailable)
         87: ERROR_INVALID_PARAMETER
         123: ERROR_INVALID_NAME
         1920: ERROR_CANT_ACCESS_FILE
         1921: ERROR_CANT_RESOLVE_FILENAME (implies unfollowable symlink)

        """
    def realpath(path):
        """
        b'\\\\?\\'
        """
def relpath(path, start=None):
    """
    Return a relative version of a path
    """
def commonpath(paths):
    """
    Given a sequence of path names, returns the longest common sub-path.
    """
