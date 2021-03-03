def print_list(extracted_list, file=None):
    """
    Print the list of tuples as returned by extract_tb() or
        extract_stack() as a formatted stack trace to the given file.
    """
def format_list(extracted_list):
    """
    Format a list of tuples or FrameSummary objects for printing.

        Given a list of tuples or FrameSummary objects as returned by
        extract_tb() or extract_stack(), return a list of strings ready
        for printing.

        Each string in the resulting list corresponds to the item with the
        same index in the argument list.  Each string ends in a newline;
        the strings may contain internal newlines as well, for those items
        whose source text line is not None.
    
    """
def print_tb(tb, limit=None, file=None):
    """
    Print up to 'limit' stack trace entries from the traceback 'tb'.

        If 'limit' is omitted or None, all entries are printed.  If 'file'
        is omitted or None, the output goes to sys.stderr; otherwise
        'file' should be an open file or file-like object with a write()
        method.
    
    """
def format_tb(tb, limit=None):
    """
    A shorthand for 'format_list(extract_tb(tb, limit))'.
    """
def extract_tb(tb, limit=None):
    """

        Return a StackSummary object representing a list of
        pre-processed entries from traceback.

        This is useful for alternate formatting of stack traces.  If
        'limit' is omitted or None, all entries are extracted.  A
        pre-processed stack trace entry is a FrameSummary object
        containing attributes filename, lineno, name, and line
        representing the information that is usually printed for a stack
        trace.  The line is a string with leading and trailing
        whitespace stripped; if the source is not available it is None.
    
    """
def print_exception(etype, value, tb, limit=None, file=None, chain=True):
    """
    Print exception up to 'limit' stack trace entries from 'tb' to 'file'.

        This differs from print_tb() in the following ways: (1) if
        traceback is not None, it prints a header "Traceback (most recent
        call last):"; (2) it prints the exception type and value after the
        stack trace; (3) if type is SyntaxError and value has the
        appropriate format, it prints the line where the syntax error
        occurred with a caret on the next line indicating the approximate
        position of the error.
    
    """
def format_exception(etype, value, tb, limit=None, chain=True):
    """
    Format a stack trace and the exception information.

        The arguments have the same meaning as the corresponding arguments
        to print_exception().  The return value is a list of strings, each
        ending in a newline and some containing internal newlines.  When
        these lines are concatenated and printed, exactly the same text is
        printed as does print_exception().
    
    """
def format_exception_only(etype, value):
    """
    Format the exception part of a traceback.

        The arguments are the exception type and value such as given by
        sys.last_type and sys.last_value. The return value is a list of
        strings, each ending in a newline.

        Normally, the list contains a single string; however, for
        SyntaxError exceptions, it contains several lines that (when
        printed) display detailed information about where the syntax
        error occurred.

        The message indicating which exception occurred is always the last
        string in the list.

    
    """
def _format_final_exc_line(etype, value):
    """
    %s\n
    """
def _some_str(value):
    """
    '<unprintable %s object>'
    """
def print_exc(limit=None, file=None, chain=True):
    """
    Shorthand for 'print_exception(*sys.exc_info(), limit, file)'.
    """
def format_exc(limit=None, chain=True):
    """
    Like print_exc() but return a string.
    """
def print_last(limit=None, file=None, chain=True):
    """
    This is a shorthand for 'print_exception(sys.last_type,
        sys.last_value, sys.last_traceback, limit, file)'.
    """
def print_stack(f=None, limit=None, file=None):
    """
    Print a stack trace from its invocation point.

        The optional 'f' argument can be used to specify an alternate
        stack frame at which to start. The optional 'limit' and 'file'
        arguments have the same meaning as for print_exception().
    
    """
def format_stack(f=None, limit=None):
    """
    Shorthand for 'format_list(extract_stack(f, limit))'.
    """
def extract_stack(f=None, limit=None):
    """
    Extract the raw traceback from the current stack frame.

        The return value has the same format as for extract_tb().  The
        optional 'f' and 'limit' arguments have the same meaning as for
        print_stack().  Each item in the list is a quadruple (filename,
        line number, function name, text), and the entries are in order
        from oldest to newest stack frame.
    
    """
def clear_frames(tb):
    """
    Clear all references to local variables in the frames of a traceback.
    """
def FrameSummary:
    """
    A single frame from a traceback.

        - :attr:`filename` The filename for the frame.
        - :attr:`lineno` The line within filename for the frame that was
          active when the frame was captured.
        - :attr:`name` The name of the function or method that was executing
          when the frame was captured.
        - :attr:`line` The text from the linecache module for the
          of code that was running when the frame was captured.
        - :attr:`locals` Either None if locals were not supplied, or a dict
          mapping the name to the repr() of the variable.
    
    """
2021-03-02 20:46:55,544 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, filename, lineno, name, *, lookup_line=True,
            locals=None, line=None):
        """
        Construct a FrameSummary.

                :param lookup_line: If True, `linecache` is consulted for the source
                    code line. Otherwise, the line will be looked up when first needed.
                :param locals: If supplied the frame locals, which will be captured as
                    object representations.
                :param line: If provided, use this instead of looking up the line in
                    the linecache.
        
        """
    def __eq__(self, other):
        """
        <FrameSummary file {filename}, line {lineno} in {name}>
        """
    def __len__(self):
        """
        Walk a stack yielding the frame and line number for each frame.

            This will follow f.f_back from the given frame. If no frame is given, the
            current stack is used. Usually used with StackSummary.extract.
    
        """
def walk_tb(tb):
    """
    Walk a traceback yielding the frame and line number for each frame.

        This will follow tb.tb_next (and thus is in the opposite order to
        walk_stack). Usually used with StackSummary.extract.
    
    """
def StackSummary(list):
    """
    A stack of frames.
    """
2021-03-02 20:46:55,546 : INFO : tokenize_signature : --> do i ever get here?
    def extract(klass, frame_gen, *, limit=None, lookup_lines=True,
            capture_locals=False):
        """
        Create a StackSummary from a traceback or stack object.

                :param frame_gen: A generator that yields (frame, lineno) tuples to
                    include in the stack.
                :param limit: None to include all frames or the number of frames to
                    include.
                :param lookup_lines: If True, lookup lines for each frame immediately,
                    otherwise lookup is deferred until the frame is rendered.
                :param capture_locals: If True, the local variables from each frame will
                    be captured as object representations into the FrameSummary.
        
        """
    def from_list(klass, a_list):
        """

                Create a StackSummary object from a supplied list of
                FrameSummary objects or old-style list of tuples.
        
        """
    def format(self):
        """
        Format the stack ready for printing.

                Returns a list of strings ready for printing.  Each string in the
                resulting list corresponds to a single frame from the stack.
                Each string ends in a newline; the strings may contain internal
                newlines as well, for those items with source text lines.

                For long sequences of the same frame and line, the first few
                repetitions are shown, followed by a summary line stating the exact
                number of further repetitions.
        
        """
def TracebackException:
    """
    An exception ready for rendering.

        The traceback module captures enough attributes from the original exception
        to this intermediary form to ensure that no references are held, while
        still being able to fully print or format it.

        Use `from_exception` to create TracebackException instances from exception
        objects, or the constructor to create TracebackException instances from
        individual components.

        - :attr:`__cause__` A TracebackException of the original *__cause__*.
        - :attr:`__context__` A TracebackException of the original *__context__*.
        - :attr:`__suppress_context__` The *__suppress_context__* value from the
          original exception.
        - :attr:`stack` A `StackSummary` representing the traceback.
        - :attr:`exc_type` The class of the original traceback.
        - :attr:`filename` For syntax errors - the filename where the error
          occurred.
        - :attr:`lineno` For syntax errors - the linenumber where the error
          occurred.
        - :attr:`text` For syntax errors - the text where the error
          occurred.
        - :attr:`offset` For syntax errors - the offset into the text where the
          error occurred.
        - :attr:`msg` For syntax errors - the compiler error message.
    
    """
2021-03-02 20:46:55,548 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, exc_type, exc_value, exc_traceback, *, limit=None,
            lookup_lines=True, capture_locals=False, _seen=None):
        """
         NB: we need to accept exc_traceback, exc_value, exc_traceback to
         permit backwards compat with the existing API, otherwise we
         need stub thunk objects just to glue it together.
         Handle loops in __cause__ or __context__.

        """
    def from_exception(cls, exc, *args, **kwargs):
        """
        Create a TracebackException from an exception.
        """
    def _load_lines(self):
        """
        Private API. force all lines in the stack to be loaded.
        """
    def __eq__(self, other):
        """
        Format the exception part of the traceback.

                The return value is a generator of strings, each ending in a newline.

                Normally, the generator emits a single string; however, for
                SyntaxError exceptions, it emits several lines that (when
                printed) display detailed information about where the syntax
                error occurred.

                The message indicating which exception occurred is always the last
                string in the output.
        
        """
    def format(self, *, chain=True):
        """
        Format the exception.

                If chain is not *True*, *__cause__* and *__context__* will not be formatted.

                The return value is a generator of strings, each ending in a newline and
                some containing internal newlines. `print_exception` is a wrapper around
                this method which just prints the lines to a file.

                The message indicating which exception occurred is always the last
                string in the output.
        
        """
