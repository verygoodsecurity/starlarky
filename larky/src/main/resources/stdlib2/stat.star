def S_IMODE(mode):
    """
    Return the portion of the file's mode that can be set by
        os.chmod().
    
    """
def S_IFMT(mode):
    """
    Return the portion of the file's mode that describes the
        file type.
    
    """
def S_ISDIR(mode):
    """
    Return True if mode is from a directory.
    """
def S_ISCHR(mode):
    """
    Return True if mode is from a character special device file.
    """
def S_ISBLK(mode):
    """
    Return True if mode is from a block special device file.
    """
def S_ISREG(mode):
    """
    Return True if mode is from a regular file.
    """
def S_ISFIFO(mode):
    """
    Return True if mode is from a FIFO (named pipe).
    """
def S_ISLNK(mode):
    """
    Return True if mode is from a symbolic link.
    """
def S_ISSOCK(mode):
    """
    Return True if mode is from a socket.
    """
def S_ISDOOR(mode):
    """
    Return True if mode is from a door.
    """
def S_ISPORT(mode):
    """
    Return True if mode is from an event port.
    """
def S_ISWHT(mode):
    """
    Return True if mode is from a whiteout.
    """
def filemode(mode):
    """
    Convert a file's mode to a string of the form '-rwxrwxrwx'.
    """
