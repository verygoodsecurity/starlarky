def _get_sep(path):
    """
    b'/'
    """
def normcase(s):
    """
    Normalize case of pathname.  Has no effect under Posix
    """
def isabs(s):
    """
    Test whether a path is absolute
    """
def join(a, *p):
    """
    Join two or more pathname components, inserting '/' as needed.
        If any component is an absolute path, all previous path components
        will be discarded.  An empty last part will result in a path that
        ends with a separator.
    """
def split(p):
    """
    Split a pathname.  Returns tuple "(head, tail)" where "tail" is
        everything after the final slash.  Either part may be empty.
    """
def splitext(p):
    """
    b'/'
    """
def splitdrive(p):
    """
    Split a pathname into drive and path. On Posix, drive is always
        empty.
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
    Test whether a path is a symbolic link
    """
def lexists(path):
    """
    Test whether a path exists.  Returns True for broken symbolic links
    """
def ismount(path):
    """
    Test whether a path is a mount point
    """
def expanduser(path):
    """
    Expand ~ and ~user constructions.  If user or $HOME is unknown,
        do nothing.
    """
def expandvars(path):
    """
    Expand shell variables of form $var and ${var}.  Unknown variables
        are left unchanged.
    """
def normpath(path):
    """
    Normalize path, eliminating double slashes, etc.
    """
def abspath(path):
    """
    Return an absolute path.
    """
def realpath(filename):
    """
    Return the canonical path of the specified filename, eliminating any
    symbolic links encountered in the path.
    """
def _joinrealpath(path, rest, seen):
    """
    b'/'
    """
def relpath(path, start=None):
    """
    Return a relative version of a path
    """
def commonpath(paths):
    """
    Given a sequence of path names, returns the longest common sub-path.
    """
