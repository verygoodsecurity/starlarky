def initlog(*allargs):
    """
    Write a log message, if there is a log file.

        Even though this function is called initlog(), you should always
        use log(); log is a variable that is set either to initlog
        (initially), to dolog (once the log file has been opened), or to
        nolog (when logging is disabled).

        The first argument is a format string; the remaining arguments (if
        any) are arguments to the % operator, so e.g.
            log("%s: %s", "a", "b")
        will write "a: b" to the log file, followed by a newline.

        If the global logfp is not None, it should be a file object to
        which log data is written.

        If the global logfp is None, the global logfile may be a string
        giving a filename to open, in append mode.  This file should be
        world writable!!!  If the file can't be opened, logging is
        silently disabled (since there is no safe place where we could
        send an error message).

    
    """
def dolog(fmt, *args):
    """
    Write a log message to the log file.  See initlog() for docs.
    """
def nolog(*allargs):
    """
    Dummy function, assigned to log when logging is disabled.
    """
def closelog():
    """
    Close the log file.
    """
def parse(fp=None, environ=os.environ, keep_blank_values=0, strict_parsing=0):
    """
    Parse a query in the environment or from a file (default stdin)

            Arguments, all optional:

            fp              : file pointer; default: sys.stdin.buffer

            environ         : environment dictionary; default: os.environ

            keep_blank_values: flag indicating whether blank values in
                percent-encoded forms should be treated as blank strings.
                A true value indicates that blanks should be retained as
                blank strings.  The default false value indicates that
                blank values are to be ignored and treated as if they were
                not included.

            strict_parsing: flag indicating what to do with parsing errors.
                If false (the default), errors are silently ignored.
                If true, errors raise a ValueError exception.
    
    """
def parse_multipart(fp, pdict, encoding="utf-8", errors="replace"):
    """
    Parse multipart input.

        Arguments:
        fp   : input file
        pdict: dictionary containing other parameters of content-type header
        encoding, errors: request encoding and error handler, passed to
            FieldStorage

        Returns a dictionary just like parse_qs(): keys are the field names, each
        value is a list of values for that field. For non-file fields, the value
        is a list of strings.
    
    """
def _parseparam(s):
    """
    ';'
    """
def parse_header(line):
    """
    Parse a Content-type like header.

        Return the main content-type and a dictionary of options.

    
    """
def MiniFieldStorage:
    """
    Like FieldStorage, for use when no file uploads are possible.
    """
    def __init__(self, name, value):
        """
        Constructor from field name and value.
        """
    def __repr__(self):
        """
        Return printable representation.
        """
def FieldStorage:
    """
    Store a sequence of fields, reading multipart/form-data.

        This class provides naming, typing, files stored on disk, and
        more.  At the top level, it is accessible like a dictionary, whose
        keys are the field names.  (Note: None can occur as a field name.)
        The items are either a Python list (if there's multiple values) or
        another FieldStorage or MiniFieldStorage object.  If it's a single
        object, it has the following attributes:

        name: the field name, if specified; otherwise None

        filename: the filename, if specified; otherwise None; this is the
            client side filename, *not* the file name on which it is
            stored (that's a temporary file you don't deal with)

        value: the value as a *string*; for file uploads, this
            transparently reads the file every time you request the value
            and returns *bytes*

        file: the file(-like) object from which you can read the data *as
            bytes* ; None if the data is stored a simple string

        type: the content-type, or None if not specified

        type_options: dictionary of options specified on the content-type
            line

        disposition: content-disposition, or None if not specified

        disposition_options: dictionary of corresponding options

        headers: a dictionary(-like) object (sometimes email.message.Message or a
            subclass thereof) containing *all* headers

        The class is subclassable, mostly for the purpose of overriding
        the make_file() method, which is called internally to come up with
        a file open for reading and writing.  This makes it possible to
        override the default choice of storing all files in a temporary
        directory and unlinking them as soon as they have been opened.

    
    """
2021-03-02 20:46:47,058 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:47,058 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:47,058 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, fp=None, headers=None, outerboundary=b'',
                 environ=os.environ, keep_blank_values=0, strict_parsing=0,
                 limit=None, encoding='utf-8', errors='replace',
                 max_num_fields=None):
        """
        Constructor.  Read multipart/* until last part.

                Arguments, all optional:

                fp              : file pointer; default: sys.stdin.buffer
                    (not used when the request method is GET)
                    Can be :
                    1. a TextIOWrapper object
                    2. an object whose read() and readline() methods return bytes

                headers         : header dictionary-like object; default:
                    taken from environ as per CGI spec

                outerboundary   : terminating multipart boundary
                    (for internal use only)

                environ         : environment dictionary; default: os.environ

                keep_blank_values: flag indicating whether blank values in
                    percent-encoded forms should be treated as blank strings.
                    A true value indicates that blanks should be retained as
                    blank strings.  The default false value indicates that
                    blank values are to be ignored and treated as if they were
                    not included.

                strict_parsing: flag indicating what to do with parsing errors.
                    If false (the default), errors are silently ignored.
                    If true, errors raise a ValueError exception.

                limit : used internally to read parts of multipart/form-data forms,
                    to exit from the reading loop when reached. It is the difference
                    between the form content-length and the number of bytes already
                    read

                encoding, errors : the encoding and error handler used to decode the
                    binary stream to strings. Must be the same as the charset defined
                    for the page sending the form (content-type : meta http-equiv or
                    header)

                max_num_fields: int. If set, then __init__ throws a ValueError
                    if there are more than n fields read by parse_qsl().

        
        """
    def __del__(self):
        """
        Return a printable representation.
        """
    def __iter__(self):
        """
        'value'
        """
    def __getitem__(self, key):
        """
        Dictionary style indexing.
        """
    def getvalue(self, key, default=None):
        """
        Dictionary style get() method, including 'value' lookup.
        """
    def getfirst(self, key, default=None):
        """
         Return the first value received.
        """
    def getlist(self, key):
        """
         Return list of received values.
        """
    def keys(self):
        """
        Dictionary style keys() method.
        """
    def __contains__(self, key):
        """
        Dictionary style __contains__ method.
        """
    def __len__(self):
        """
        Dictionary style len(x) support.
        """
    def __bool__(self):
        """
        Cannot be converted to bool.
        """
    def read_urlencoded(self):
        """
        Internal: read data in query string format.
        """
    def read_multi(self, environ, keep_blank_values, strict_parsing):
        """
        Internal: read a part that is itself multipart.
        """
    def read_single(self):
        """
        Internal: read an atomic part.
        """
    def read_binary(self):
        """
        Internal: read binary data.
        """
    def read_lines(self):
        """
        Internal: read lines until EOF or outerboundary.
        """
    def __write(self, line):
        """
        line is always bytes, not string
        """
    def read_lines_to_eof(self):
        """
        Internal: read lines until EOF.
        """
    def read_lines_to_outerboundary(self):
        """
        Internal: read lines until outerboundary.
                Data is read as bytes: boundaries and line ends must be converted
                to bytes for comparisons.
        
        """
    def skip_lines(self):
        """
        Internal: skip lines until outer boundary if defined.
        """
    def make_file(self):
        """
        Overridable: return a readable & writable file.

                The file will be used as follows:
                - data is written to it
                - seek(0)
                - data is read from it

                The file is opened in binary mode for files, in text mode
                for other fields

                This version opens a temporary file for reading and writing,
                and immediately deletes (unlinks) it.  The trick (on Unix!) is
                that the file can still be used, but it can't be opened by
                another process, and it will automatically be deleted when it
                is closed or when the current process terminates.

                If you want a more permanent file, you derive a class which
                overrides this method.  If you want a visible temporary file
                that is nevertheless automatically deleted when the script
                terminates, try defining a __del__ method in a derived class
                which unlinks the temporary files you have created.

        
        """
def test(environ=os.environ):
    """
    Robust test CGI script, usable as main program.

        Write minimal HTTP headers and dump all information provided to
        the script in HTML form.

    
    """
        def f():
            """
            testing print_exception() -- <I>italics?</I>
            """
        def g(f=f):
            """
            <H3>What follows is a test, not an actual exception:</H3>
            """
def print_exception(type=None, value=None, tb=None, limit=None):
    """
    <H3>Traceback (most recent call last):</H3>
    """
def print_environ(environ=os.environ):
    """
    Dump the shell environment as HTML.
    """
def print_form(form):
    """
    Dump the contents of a form as HTML.
    """
def print_directory():
    """
    Dump the current directory as HTML.
    """
def print_arguments():
    """
    <H3>Command Line Arguments:</H3>
    """
def print_environ_usage():
    """
    Dump a list of environment variables used by CGI as HTML.
    """
def valid_boundary(s):
    """
    b"^[ -~]{0,200}[!-~]$
    """
