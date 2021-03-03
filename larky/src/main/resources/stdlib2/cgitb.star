def reset():
    """
    Return a string that resets the CGI and browser to a known state.
    """
def small(text):
    """
    '<small>'
    """
def strong(text):
    """
    '<strong>'
    """
def grey(text):
    """
    '<font color="#909090">'
    """
def lookup(name, frame, locals):
    """
    Find the value for a given name in the given environment.
    """
def scanvars(reader, frame, locals):
    """
    Scan one logical line of Python and look up values of variables used.
    """
def html(einfo, context=5):
    """
    Return a nice HTML document describing a given traceback.
    """
        def reader(lnum=[lnum]):
            """
            '<tr><td bgcolor="#d8bbff">%s%s %s</td></tr>'
            """
def text(einfo, context=5):
    """
    Return a plain text document describing a given traceback.
    """
        def reader(lnum=[lnum]):
            """
            ' %s %s'
            """
def Hook:
    """
    A hook to replace sys.excepthook that shows tracebacks in HTML.
    """
2021-03-02 20:46:44,331 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, display=1, logdir=None, context=5, file=None,
                 format="html"):
        """
         send tracebacks to browser if true
        """
    def __call__(self, etype, evalue, etb):
        """
        html
        """
def enable(display=1, logdir=None, context=5, format="html"):
    """
    Install an exception handler that formats tracebacks as HTML.

        The optional argument 'display' can be set to 0 to suppress sending the
        traceback to the browser, and 'logdir' can be set to a directory to cause
        tracebacks to be written to files there.
    """
