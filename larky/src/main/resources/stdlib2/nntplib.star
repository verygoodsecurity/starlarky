def NNTPError(Exception):
    """
    Base class for all nntplib exceptions
    """
    def __init__(self, *args):
        """
        'No response given'
        """
def NNTPReplyError(NNTPError):
    """
    Unexpected [123]xx reply
    """
def NNTPTemporaryError(NNTPError):
    """
    4xx errors
    """
def NNTPPermanentError(NNTPError):
    """
    5xx errors
    """
def NNTPProtocolError(NNTPError):
    """
    Response does not begin with [1-5]
    """
def NNTPDataError(NNTPError):
    """
    Error in response data
    """
def decode_header(header_str):
    """
    Takes a unicode string representing a munged header value
        and decodes it as a (possibly non-ASCII) readable value.
    """
def _parse_overview_fmt(lines):
    """
    Parse a list of string representing the response to LIST OVERVIEW.FMT
        and return a list of header/metadata names.
        Raises NNTPDataError if the response is not compliant
        (cf. RFC 3977, section 8.4).
    """
def _parse_overview(lines, fmt, data_process_func=None):
    """
    Parse the response to an OVER or XOVER command according to the
        overview format `fmt`.
    """
def _parse_datetime(date_str, time_str=None):
    """
    Parse a pair of (date, time) strings, and return a datetime object.
        If only the date is given, it is assumed to be date and time
        concatenated together (e.g. response to the DATE command).
    
    """
def _unparse_datetime(dt, legacy=False):
    """
    Format a date or datetime object as a pair of (date, time) strings
        in the format required by the NEWNEWS and NEWGROUPS commands.  If a
        date object is passed, the time is assumed to be midnight (00h00).

        The returned representation depends on the legacy flag:
        * if legacy is False (the default):
          date has the YYYYMMDD format and time the HHMMSS format
        * if legacy is True:
          date has the YYMMDD format and time the HHMMSS format.
        RFC 3977 compliant servers should understand both formats; therefore,
        legacy is only needed when talking to old servers.
    
    """
    def _encrypt_on(sock, context, hostname):
        """
        Wrap a socket in SSL/TLS. Arguments:
                - sock: Socket to wrap
                - context: SSL context to use for the encrypted connection
                Returns:
                - sock: New, encrypted socket.
        
        """
def _NNTPBase:
    """
     UTF-8 is the character set for all NNTP commands and responses: they
     are automatically encoded (when sending) and decoded (and receiving)
     by this class.
     However, some multi-line data blocks can contain arbitrary bytes (for
     example, latin-1 or utf-16 data in the body of a message). Commands
     taking (POST, IHAVE) or returning (HEAD, BODY, ARTICLE) raw message
     data will therefore only accept and produce bytes objects.
     Furthermore, since there could be non-compliant servers out there,
     we use 'surrogateescape' as the error handler for fault tolerance
     and easy round-tripping. This could be useful for some applications
     (e.g. NNTP gateways).


    """
2021-03-02 20:46:39,852 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, file, host,
                 readermode=None, timeout=_GLOBAL_DEFAULT_TIMEOUT):
        """
        Initialize an instance.  Arguments:
                - file: file-like object (open for read/write in binary mode)
                - host: hostname of the server
                - readermode: if true, send 'mode reader' command after
                              connecting.
                - timeout: timeout (in seconds) used for socket connections

                readermode is sometimes necessary if you are connecting to an
                NNTP server on the local machine and intend to call
                reader-specific commands, such as `group'.  If you get
                unexpected NNTPPermanentErrors, you might need to set
                readermode.
        
        """
    def __enter__(self):
        """
        file
        """
    def getwelcome(self):
        """
        Get the welcome message from the server
                (this is read and squirreled away by __init__()).
                If the response code is 200, posting is allowed;
                if it 201, posting is not allowed.
        """
    def getcapabilities(self):
        """
        Get the server capabilities, as read by __init__().
                If the CAPABILITIES command is not supported, an empty dict is
                returned.
        """
    def set_debuglevel(self, level):
        """
        Set the debugging level.  Argument 'level' means:
                0: no debugging output (default)
                1: print commands and responses but not body text etc.
                2: also print raw lines read and sent before stripping CR/LF
        """
    def _putline(self, line):
        """
        Internal: send one line to the server, appending CRLF.
                The `line` must be a bytes-like object.
        """
    def _putcmd(self, line):
        """
        Internal: send one command to the server (through _putline()).
                The `line` must be a unicode string.
        """
    def _getline(self, strip_crlf=True):
        """
        Internal: return one line from the server, stripping _CRLF.
                Raise EOFError if the connection is closed.
                Returns a bytes object.
        """
    def _getresp(self):
        """
        Internal: get a response from the server.
                Raise various errors if the response indicates an error.
                Returns a unicode string.
        """
    def _getlongresp(self, file=None):
        """
        Internal: get a response plus following text from the server.
                Raise various errors if the response indicates an error.

                Returns a (response, lines) tuple where `response` is a unicode
                string and `lines` is a list of bytes objects.
                If `file` is a file-like object, it must be open in binary mode.
        
        """
    def _shortcmd(self, line):
        """
        Internal: send a command and get the response.
                Same return value as _getresp().
        """
    def _longcmd(self, line, file=None):
        """
        Internal: send a command and get the response plus following text.
                Same return value as _getlongresp().
        """
    def _longcmdstring(self, line, file=None):
        """
        Internal: send a command and get the response plus following text.
                Same as _longcmd() and _getlongresp(), except that the returned `lines`
                are unicode strings rather than bytes objects.
        
        """
    def _getoverviewfmt(self):
        """
        Internal: get the overview format. Queries the server if not
                already done, else returns the cached value.
        """
    def _grouplist(self, lines):
        """
         Parse lines into "group last first flag

        """
    def capabilities(self):
        """
        Process a CAPABILITIES command.  Not supported by all servers.
                Return:
                - resp: server response if successful
                - caps: a dictionary mapping capability names to lists of tokens
                (for example {'VERSION': ['2'], 'OVER': [], LIST: ['ACTIVE', 'HEADERS'] })
        
        """
    def newgroups(self, date, *, file=None):
        """
        Process a NEWGROUPS command.  Arguments:
                - date: a date or datetime object
                Return:
                - resp: server response if successful
                - list: list of newsgroup names
        
        """
    def newnews(self, group, date, *, file=None):
        """
        Process a NEWNEWS command.  Arguments:
                - group: group name or '*'
                - date: a date or datetime object
                Return:
                - resp: server response if successful
                - list: list of message ids
        
        """
    def list(self, group_pattern=None, *, file=None):
        """
        Process a LIST or LIST ACTIVE command. Arguments:
                - group_pattern: a pattern indicating which groups to query
                - file: Filename string or file object to store the result in
                Returns:
                - resp: server response if successful
                - list: list of (group, last, first, flag) (strings)
        
        """
    def _getdescriptions(self, group_pattern, return_all):
        """
        '^(?P<group>[^ \t]+)[ \t]+(.*)$'
        """
    def description(self, group):
        """
        Get a description for a single group.  If more than one
                group matches ('group' is a pattern), return the first.  If no
                group matches, return an empty string.

                This elides the response code from the server, since it can
                only be '215' or '285' (for xgtitle) anyway.  If the response
                code is needed, use the 'descriptions' method.

                NOTE: This neither checks for a wildcard in 'group' nor does
                it check whether the group actually exists.
        """
    def descriptions(self, group_pattern):
        """
        Get descriptions for a range of groups.
        """
    def group(self, name):
        """
        Process a GROUP command.  Argument:
                - group: the group name
                Returns:
                - resp: server response if successful
                - count: number of articles
                - first: first article number
                - last: last article number
                - name: the group name
        
        """
    def help(self, *, file=None):
        """
        Process a HELP command. Argument:
                - file: Filename string or file object to store the result in
                Returns:
                - resp: server response if successful
                - list: list of strings returned by the server in response to the
                        HELP command
        
        """
    def _statparse(self, resp):
        """
        Internal: parse the response line of a STAT, NEXT, LAST,
                ARTICLE, HEAD or BODY command.
        """
    def _statcmd(self, line):
        """
        Internal: process a STAT, NEXT or LAST command.
        """
    def stat(self, message_spec=None):
        """
        Process a STAT command.  Argument:
                - message_spec: article number or message id (if not specified,
                  the current article is selected)
                Returns:
                - resp: server response if successful
                - art_num: the article number
                - message_id: the message id
        
        """
    def next(self):
        """
        Process a NEXT command.  No arguments.  Return as for STAT.
        """
    def last(self):
        """
        Process a LAST command.  No arguments.  Return as for STAT.
        """
    def _artcmd(self, line, file=None):
        """
        Internal: process a HEAD, BODY or ARTICLE command.
        """
    def head(self, message_spec=None, *, file=None):
        """
        Process a HEAD command.  Argument:
                - message_spec: article number or message id
                - file: filename string or file object to store the headers in
                Returns:
                - resp: server response if successful
                - ArticleInfo: (article number, message id, list of header lines)
        
        """
    def body(self, message_spec=None, *, file=None):
        """
        Process a BODY command.  Argument:
                - message_spec: article number or message id
                - file: filename string or file object to store the body in
                Returns:
                - resp: server response if successful
                - ArticleInfo: (article number, message id, list of body lines)
        
        """
    def article(self, message_spec=None, *, file=None):
        """
        Process an ARTICLE command.  Argument:
                - message_spec: article number or message id
                - file: filename string or file object to store the article in
                Returns:
                - resp: server response if successful
                - ArticleInfo: (article number, message id, list of article lines)
        
        """
    def slave(self):
        """
        Process a SLAVE command.  Returns:
                - resp: server response if successful
        
        """
    def xhdr(self, hdr, str, *, file=None):
        """
        Process an XHDR command (optional server extension).  Arguments:
                - hdr: the header type (e.g. 'subject')
                - str: an article nr, a message id, or a range nr1-nr2
                - file: Filename string or file object to store the result in
                Returns:
                - resp: server response if successful
                - list: list of (nr, value) strings
        
        """
        def remove_number(line):
            """
            Process an XOVER command (optional server extension) Arguments:
                    - start: start of range
                    - end: end of range
                    - file: Filename string or file object to store the result in
                    Returns:
                    - resp: server response if successful
                    - list: list of dicts containing the response fields
        
            """
    def over(self, message_spec, *, file=None):
        """
        Process an OVER command.  If the command isn't supported, fall
                back to XOVER. Arguments:
                - message_spec:
                    - either a message id, indicating the article to fetch
                      information about
                    - or a (start, end) tuple, indicating a range of article numbers;
                      if end is None, information up to the newest message will be
                      retrieved
                    - or None, indicating the current article number must be used
                - file: Filename string or file object to store the result in
                Returns:
                - resp: server response if successful
                - list: list of dicts containing the response fields

                NOTE: the "message id" form isn't supported by XOVER
        
        """
    def xgtitle(self, group, *, file=None):
        """
        Process an XGTITLE command (optional server extension) Arguments:
                - group: group name wildcard (i.e. news.*)
                Returns:
                - resp: server response if successful
                - list: list of (name,title) strings
        """
    def xpath(self, id):
        """
        Process an XPATH command (optional server extension) Arguments:
                - id: Message id of article
                Returns:
                resp: server response if successful
                path: directory path to article
        
        """
    def date(self):
        """
        Process the DATE command.
                Returns:
                - resp: server response if successful
                - date: datetime object
        
        """
    def _post(self, command, f):
        """
         Raises a specific exception if posting is not allowed

        """
    def post(self, data):
        """
        Process a POST command.  Arguments:
                - data: bytes object, iterable or file containing the article
                Returns:
                - resp: server response if successful
        """
    def ihave(self, message_id, data):
        """
        Process an IHAVE command.  Arguments:
                - message_id: message-id of the article
                - data: file containing the article
                Returns:
                - resp: server response if successful
                Note that if the server refuses the article an exception is raised.
        """
    def _close(self):
        """
        Process a QUIT command and close the socket.  Returns:
                - resp: server response if successful
        """
    def login(self, user=None, password=None, usenetrc=True):
        """
        Already logged in.
        """
    def _setreadermode(self):
        """
        'mode reader'
        """
        def starttls(self, context=None):
            """
            Process a STARTTLS command. Arguments:
                        - context: SSL context to use for the encrypted connection
            
            """
def NNTP(_NNTPBase):
    """
    Initialize an instance.  Arguments:
            - host: hostname to connect to
            - port: port to connect to (default the standard NNTP port)
            - user: username to authenticate with
            - password: password to use with username
            - readermode: if true, send 'mode reader' command after
                          connecting.
            - usenetrc: allow loading username and password from ~/.netrc file
                        if not specified explicitly
            - timeout: timeout (in seconds) used for socket connections

            readermode is sometimes necessary if you are connecting to an
            NNTP server on the local machine and intend to call
            reader-specific commands, such as `group'.  If you get
            unexpected NNTPPermanentErrors, you might need to set
            readermode.
        
    """
    def _close(self):
        """
        This works identically to NNTP.__init__, except for the change
                    in default port and the `ssl_context` argument for SSL connections.
            
        """
        def _close(self):
            """
            NNTP_SSL
            """
    def cut(s, lim):
        """
        ...
        """
