def glob(pathname, *, recursive=False):
    """
    Return a list of paths matching a pathname pattern.

        The pattern may contain simple shell-style wildcards a la
        fnmatch. However, unlike fnmatch, filenames starting with a
        dot are special cases that are not matched by '*' and '?'
        patterns.

        If recursive is true, the pattern '**' will match any files and
        zero or more directories and subdirectories.
    
    """
def iglob(pathname, *, recursive=False):
    """
    Return an iterator which yields the paths matching a pathname pattern.

        The pattern may contain simple shell-style wildcards a la
        fnmatch. However, unlike fnmatch, filenames starting with a
        dot are special cases that are not matched by '*' and '?'
        patterns.

        If recursive is true, the pattern '**' will match any files and
        zero or more directories and subdirectories.
    
    """
def _iglob(pathname, recursive, dironly):
    """
     Patterns ending with a slash should match only directories

    """
def _glob1(dirname, pattern, dironly):
    """
     `os.path.split()` returns an empty basename for paths ending with a
     directory separator.  'q*x/' should match only directories.

    """
def glob0(dirname, pattern):
    """
     This helper function recursively yields relative pathnames inside a literal
     directory.


    """
def _glob2(dirname, pattern, dironly):
    """
     If dironly is false, yields all file names inside a directory.
     If dironly is true, yields only directory names.

    """
def _iterdir(dirname, dironly):
    """
    'ASCII'
    """
def _rlistdir(dirname, dironly):
    """
    '([*?[])'
    """
def has_magic(s):
    """
    '.'
    """
def _isrecursive(pattern):
    """
    b'**'
    """
def escape(pathname):
    """
    Escape all special characters.
    
    """
