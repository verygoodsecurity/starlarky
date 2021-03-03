def WSGIWarning(Warning):
    """

        Raised in response to WSGI-spec-related warnings
    
    """
def assert_(cond, *args):
    """
    {0} must be of type str (got {1})
    """
def validator(application):
    """

        When applied between a WSGI server and a WSGI application, this
        middleware will check for WSGI compliancy on a number of levels.
        This middleware does not modify the request or response in any
        way, but will raise an AssertionError if anything seems off
        (except for a failure to close the application iterator, which
        will be printed to stderr -- there's no way to raise an exception
        at that point).
    
    """
    def lint_app(*args, **kw):
        """
        Two arguments required
        """
        def start_response_wrapper(*args, **kw):
            """
            Invalid number of arguments: %s
            """
def InputWrapper:
    """
    input.close() must not be called
    """
def ErrorWrapper:
    """
    errors.close() must not be called
    """
def WriteWrapper:
    """
     We want to make sure __iter__ is called

    """
def IteratorWrapper:
    """
    Iterator read after closed
    """
    def close(self):
        """
        'close'
        """
    def __del__(self):
        """
        Iterator garbage collected without being closed
        """
def check_environ(environ):
    """
    Environment is not of the right type: %r (environment: %r)

    """
def check_input(wsgi_input):
    """
    'read'
    """
def check_errors(wsgi_errors):
    """
    'flush'
    """
def check_status(status):
    """
    Status
    """
def check_headers(headers):
    """
    Headers (%r) must be of type list: %r

    """
def check_content_type(status, headers):
    """
    Status
    """
def check_exc_info(exc_info):
    """
    exc_info (%r) is not a tuple: %r
    """
def check_iterator(iterator):
    """
     Technically a bytestring is legal, which is why it's a really bad
     idea, because it may cause the response to be returned
     character-by-character

    """
