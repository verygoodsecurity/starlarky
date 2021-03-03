def URLError(OSError):
    """
     URLError is a sub-type of OSError, but it doesn't share any of
     the implementation.  need to override __init__ and __str__.
     It sets self.args for compatibility with other OSError
     subclasses, but args doesn't have the typical format with errno in
     slot 0 and strerror in slot 1.  This may be better than nothing.

    """
    def __init__(self, reason, filename=None):
        """
        '<urlopen error %s>'
        """
def HTTPError(URLError, urllib.response.addinfourl):
    """
    Raised when HTTP error occurs, but also acts like non-error return
    """
    def __init__(self, url, code, msg, hdrs, fp):
        """
         The addinfourl classes depend on fp being a valid file
         object.  In some cases, the HTTPError may not have a valid
         file object.  If this happens, the simplest workaround is to
         not initialize the base classes.

        """
    def __str__(self):
        """
        'HTTP Error %s: %s'
        """
    def __repr__(self):
        """
        '<HTTPError %s: %r>'
        """
    def reason(self):
        """
        Exception raised when downloaded size does not match content-length.
        """
    def __init__(self, message, content):
