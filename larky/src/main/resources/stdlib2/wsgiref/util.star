def FileWrapper:
    """
    Wrapper to convert file-like objects to iterables
    """
    def __init__(self, filelike, blksize=8192):
        """
        'close'
        """
    def __getitem__(self,key):
        """
        FileWrapper's __getitem__ method ignores 'key' parameter. 
        Use iterator protocol instead.
        """
    def __iter__(self):
        """
        Return a guess for whether 'wsgi.url_scheme' should be 'http' or 'https'
    
        """
def application_uri(environ):
    """
    Return the application's base URI (no PATH_INFO or QUERY_STRING)
    """
def request_uri(environ, include_query=True):
    """
    Return the full request URI, optionally including the query string
    """
def shift_path_info(environ):
    """
    Shift a name from PATH_INFO to SCRIPT_NAME, returning it

        If there are no remaining path segments in PATH_INFO, return None.
        Note: 'environ' is modified in-place; use a copy if you need to keep
        the original PATH_INFO or SCRIPT_NAME.

        Note: when PATH_INFO is just a '/', this returns '' and appends a trailing
        '/' to SCRIPT_NAME, even though empty path segments are normally ignored,
        and SCRIPT_NAME doesn't normally end in a '/'.  This is intentional
        behavior, to ensure that an application can tell the difference between
        '/x' and '/x/' when traversing to objects.
    
    """
def setup_testing_defaults(environ):
    """
    Update 'environ' with trivial defaults for testing purposes

        This adds various parameters required for WSGI, including HTTP_HOST,
        SERVER_NAME, SERVER_PORT, REQUEST_METHOD, SCRIPT_NAME, PATH_INFO,
        and all of the wsgi.* variables.  It only supplies default values,
        and does not replace any existing settings for these variables.

        This routine is intended to make it easier for unit tests of WSGI
        servers and applications to set up dummy environments.  It should *not*
        be used by actual WSGI servers or applications, since the data is fake!
    
    """
def is_hop_by_hop(header_name):
    """
    Return true if 'header_name' is an HTTP/1.1 "Hop-by-Hop" header
    """
