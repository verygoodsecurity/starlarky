def showwarning(message, category, filename, lineno, file=None, line=None):
    """
    Hook to write a warning to a file; replace if you like.
    """
def formatwarning(message, category, filename, lineno, line=None):
    """
    Function to format a warning the standard way.
    """
def _showwarnmsg_impl(msg):
    """
     sys.stderr is None when run with pythonw.exe:
     warnings get lost

    """
def _formatwarnmsg_impl(msg):
    """
    f"{msg.filename}:{msg.lineno}: {category}: {msg.message}\n
    """
def _showwarnmsg(msg):
    """
    Hook to write a warning to a file; replace if you like.
    """
def _formatwarnmsg(msg):
    """
    Function to format a warning the standard way.
    """
2021-03-02 20:46:55,900 : INFO : tokenize_signature : --> do i ever get here?
def filterwarnings(action, message="", category=Warning, module="", lineno=0,
                   append=False):
    """
    Insert an entry into the list of warnings filters (at the front).

        'action' -- one of "error", "ignore", "always", "default", "module",
                    or "once"
        'message' -- a regex that the warning message must match
        'category' -- a class that the warning must be a subclass of
        'module' -- a regex that the module name must match
        'lineno' -- an integer line number, 0 matches all warnings
        'append' -- if true, append to the list of filters
    
    """
def simplefilter(action, category=Warning, lineno=0, append=False):
    """
    Insert a simple entry into the list of warnings filters (at the front).

        A simple filter matches all modules and messages.
        'action' -- one of "error", "ignore", "always", "default", "module",
                    or "once"
        'category' -- a class that the warning must be a subclass of
        'lineno' -- an integer line number, 0 matches all warnings
        'append' -- if true, append to the list of filters
    
    """
def _add_filter(*item, append):
    """
     Remove possible duplicate filters, so new one will be placed
     in correct place. If append=True and duplicate exists, do nothing.

    """
def resetwarnings():
    """
    Clear the list of warning filters, so that no filters are active.
    """
def _OptionError(Exception):
    """
    Exception used by option processing helpers.
    """
def _processoptions(args):
    """
    Invalid -W option ignored:
    """
def _setoption(arg):
    """
    ':'
    """
def _getaction(action):
    """
    default
    """
def _getcategory(category):
    """
    '.'
    """
def _is_internal_frame(frame):
    """
    Signal whether the frame is an internal CPython implementation detail.
    """
def _next_external_frame(frame):
    """
    Find the next frame that doesn't involve CPython internals.
    """
def warn(message, category=None, stacklevel=1, source=None):
    """
    Issue a warning, or maybe ignore it or raise an exception.
    """
2021-03-02 20:46:55,906 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:55,906 : INFO : tokenize_signature : --> do i ever get here?
def warn_explicit(message, category, filename, lineno,
                  module=None, registry=None, module_globals=None,
                  source=None):
    """
    <unknown>
    """
def WarningMessage(object):
    """
    message
    """
2021-03-02 20:46:55,908 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, message, category, filename, lineno, file=None,
                 line=None, source=None):
        """
        {message : %r, category : %r, filename : %r, lineno : %s, 
        line : %r}
        """
def catch_warnings(object):
    """
    A context manager that copies and restores the warnings filter upon
        exiting the context.

        The 'record' argument specifies whether warnings should be captured by a
        custom implementation of warnings.showwarning() and be appended to a list
        returned by the context manager. Otherwise None is returned by the context
        manager. The objects appended to the list are arguments whose attributes
        mirror the arguments to showwarning().

        The 'module' argument is to specify an alternative module to the module
        named 'warnings' and imported under that name. This argument is only useful
        when testing the warnings module itself.

    
    """
    def __init__(self, *, record=False, module=None):
        """
        Specify whether to record warnings and if an alternative module
                should be used other than sys.modules['warnings'].

                For compatibility with Python 3.0, please consider all arguments to be
                keyword-only.

        
        """
    def __repr__(self):
        """
        record=True
        """
    def __enter__(self):
        """
        Cannot enter %r twice
        """
    def __exit__(self, *exc_info):
        """
        Cannot exit %r without entering first
        """
def _warn_unawaited_coroutine(coro):
    """
    f"coroutine '{coro.__qualname__}' was never awaited\n

    """
        def extract():
            """
            Coroutine created at (most recent call last)\n
            """
    def _filters_mutated():
        """
         Module initialization

        """
