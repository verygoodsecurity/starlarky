def addbase(tempfile._TemporaryFileWrapper):
    """
    Base class for addinfo and addclosehook. Is a good idea for garbage collection.
    """
    def __init__(self, fp):
        """
        '<urllib response>'
        """
    def __repr__(self):
        """
        '<%s at %r whose fp = %r>'
        """
    def __enter__(self):
        """
        I/O operation on closed file
        """
    def __exit__(self, type, value, traceback):
        """
        Class to add a close hook to an open file.
        """
    def __init__(self, fp, closehook, *hookargs):
        """
        class to add an info() method to an open file.
        """
    def __init__(self, fp, headers):
        """
        class to add info() and geturl() methods to an open file.
        """
    def __init__(self, fp, headers, url, code=None):
