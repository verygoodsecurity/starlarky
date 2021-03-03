def exists(path):
    """
    Test whether a path exists.  Returns False for broken symbolic links
    """
def isfile(path):
    """
    Test whether a path is a regular file
    """
def isdir(s):
    """
    Return true if the pathname refers to an existing directory.
    """
def getsize(filename):
    """
    Return the size of a file, reported by os.stat().
    """
def getmtime(filename):
    """
    Return the last modification time of a file, reported by os.stat().
    """
def getatime(filename):
    """
    Return the last access time of a file, reported by os.stat().
    """
def getctime(filename):
    """
    Return the metadata change time of a file, reported by os.stat().
    """
def commonprefix(m):
    """
    Given a list of pathnames, returns the longest common leading component
    """
def samestat(s1, s2):
    """
    Test whether two stat buffers reference the same file
    """
def samefile(f1, f2):
    """
    Test whether two pathnames reference the same actual file or directory

        This is determined by the device number and i-node number and
        raises an exception if an os.stat() call on either pathname fails.
    
    """
def sameopenfile(fp1, fp2):
    """
    Test whether two open file objects reference the same file
    """
def _splitext(p, sep, altsep, extsep):
    """
    Split the extension from a pathname.

        Extension is everything from the last dot to the end, ignoring
        leading dots.  Returns "(root, ext)"; ext may be empty.
    """
def _check_arg_types(funcname, *args):
    """
    f'{funcname}() argument must be str, bytes, or '
    f'os.PathLike object, not {s.__class__.__name__!r}'
    """
