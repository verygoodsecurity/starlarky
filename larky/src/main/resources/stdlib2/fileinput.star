def input(files=None, inplace=False, backup="", *, mode="r", openhook=None):
    """
    Return an instance of the FileInput class, which can be iterated.

        The parameters are passed to the constructor of the FileInput class.
        The returned instance, in addition to being an iterator,
        keeps global state for the functions of this module,.
    
    """
def close():
    """
    Close the sequence.
    """
def nextfile():
    """

        Close the current file so that the next iteration will read the first
        line from the next file (if any); lines not read from the file will
        not count towards the cumulative line count. The filename is not
        changed until after the first line of the next file has been read.
        Before the first line has been read, this function has no effect;
        it cannot be used to skip the first file. After the last line of the
        last file has been read, this function has no effect.
    
    """
def filename():
    """

        Return the name of the file currently being read.
        Before the first line has been read, returns None.
    
    """
def lineno():
    """

        Return the cumulative line number of the line that has just been read.
        Before the first line has been read, returns 0. After the last line
        of the last file has been read, returns the line number of that line.
    
    """
def filelineno():
    """

        Return the line number in the current file. Before the first line
        has been read, returns 0. After the last line of the last file has
        been read, returns the line number of that line within the file.
    
    """
def fileno():
    """

        Return the file number of the current file. When no file is currently
        opened, returns -1.
    
    """
def isfirstline():
    """

        Returns true the line just read is the first line of its file,
        otherwise returns false.
    
    """
def isstdin():
    """

        Returns true if the last line was read from sys.stdin,
        otherwise returns false.
    
    """
def FileInput:
    """
    FileInput([files[, inplace[, backup]]], *, mode=None, openhook=None)

        Class FileInput is the implementation of the module; its methods
        filename(), lineno(), fileline(), isfirstline(), isstdin(), fileno(),
        nextfile() and close() correspond to the functions of the same name
        in the module.
        In addition it has a readline() method which returns the next
        input line, and a __getitem__() method which implements the
        sequence behavior. The sequence must be accessed in strictly
        sequential order; random access and readline() cannot be mixed.
    
    """
2021-03-02 20:46:59,508 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, files=None, inplace=False, backup="", *,
                 mode="r", openhook=None):
        """
        '-'
        """
    def __del__(self):
        """
         repeat with next file


        """
    def __getitem__(self, i):
        """
        Support for indexing FileInput objects is deprecated. 
        Use iterator protocol instead.
        """
    def nextfile(self):
        """
         restore FileInput._readline
        """
    def readline(self):
        """
         repeat with next file


        """
    def _readline(self):
        """
        'b'
        """
    def filename(self):
        """
        '.gz'
        """
def hook_encoded(encoding, errors=None):
    """
    ib:
    """
