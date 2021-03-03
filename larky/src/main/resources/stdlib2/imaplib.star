def IMAP4:
    """
    r"""IMAP4 client class.

        Instantiate with: IMAP4([host[, port]])

                host - host's name (default: localhost);
                port - port number (default: standard IMAP4 port).

        All IMAP4rev1 commands are supported by methods of the same
        name (in lower-case).

        All arguments to commands are converted to strings, except for
        AUTHENTICATE, and the last argument to APPEND which is passed as
        an IMAP4 literal.  If necessary (the string contains any
        non-printing characters or white-space and isn't enclosed with
        either parentheses or double quotes) each string is quoted.
        However, the 'password' argument to the LOGIN command is always
        quoted.  If you want to avoid having an argument string quoted
        (eg: the 'flags' argument to STORE) then enclose the string in
        parentheses (eg: "(\Deleted)").

        Each command returns a tuple: (type, [data, ...]) where 'type'
        is usually 'OK' or 'NO', and 'data' is either the text from the
        tagged response, or untagged results from command. Each 'data'
        is either a string, or a tuple. If a tuple, then the first part
        is the header of the response, and the second part contains
        the data (ie: 'literal' value).

        Errors raise the exception class <instance>.error("<reason>").
        IMAP4 server errors raise <instance>.abort("<reason>"),
        which is a sub-class of 'error'. Mailbox status changes
        from READ-WRITE to READ-ONLY raise the exception class
        <instance>.readonly("<reason>"), which is a sub-class of 'abort'.

        "error" exceptions imply a program error.
        "abort" exceptions imply the connection should be reset, and
                the command re-tried.
        "readonly" exceptions imply the command should be re-tried.

        Note: to use this module, you must read the RFCs pertaining to the
        IMAP4 protocol, as the semantics of the arguments to each IMAP4
        command are left to the invoker, not to mention the results. Also,
        most IMAP servers implement a sub-set of the commands available here.
    
    """
    def error(Exception): pass    # Logical errors - debug required
    """
     Logical errors - debug required
    """
    def abort(error): pass        # Service errors - close and retry
    """
     Service errors - close and retry
    """
    def readonly(abort): pass     # Mailbox status changed to READ-ONLY
    """
     Mailbox status changed to READ-ONLY
    """
    def __init__(self, host='', port=IMAP4_PORT):
        """
        'LOGOUT'
        """
    def _mode_ascii(self):
        """
        'ascii'
        """
    def _mode_utf8(self):
        """
        'utf-8'
        """
    def _connect(self):
        """
         Create unique tag for this session,
         and compile tagged response matcher.


        """
    def __getattr__(self, attr):
        """
               Allow UPPERCASE variants of IMAP4 command methods.

        """
    def __enter__(self):
        """
        LOGOUT
        """
    def _create_socket(self):
        """
         Default value of IMAP4.host is '', but socket.getaddrinfo()
         (which is used by socket.create_connection()) expects None
         as a default value for host.

        """
    def open(self, host = '', port = IMAP4_PORT):
        """
        Setup connection to remote server on "host:port"
                    (default: localhost:standard IMAP4 port).
                This connection will be used by the routines:
                    read, readline, send, shutdown.
        
        """
    def read(self, size):
        """
        Read 'size' bytes from remote.
        """
    def readline(self):
        """
        Read line from remote.
        """
    def send(self, data):
        """
        Send data to remote.
        """
    def shutdown(self):
        """
        Close I/O established in "open".
        """
    def socket(self):
        """
        Return socket instance used to connect to IMAP4 server.

                socket = <instance>.socket()
        
        """
    def recent(self):
        """
        Return most recent 'RECENT' responses if any exist,
                else prompt server for an update using the 'NOOP' command.

                (typ, [data]) = <instance>.recent()

                'data' is None if no new messages,
                else list of RECENT responses, most recent last.
        
        """
    def response(self, code):
        """
        Return data for response 'code' if received, or None.

                Old value for response 'code' is cleared.

                (code, [data]) = <instance>.response(code)
        
        """
    def append(self, mailbox, flags, date_time, message):
        """
        Append message to named mailbox.

                (typ, [data]) = <instance>.append(mailbox, flags, date_time, message)

                        All args except `message' can be None.
        
        """
    def authenticate(self, mechanism, authobject):
        """
        Authenticate command - requires response processing.

                'mechanism' specifies which authentication mechanism is to
                be used - it must appear in <instance>.capabilities in the
                form AUTH=<mechanism>.

                'authobject' must be a callable object:

                        data = authobject(response)

                It will be called to process server continuation responses; the
                response argument it is passed will be a bytes.  It should return bytes
                data that will be base64 encoded and sent to the server.  It should
                return None if the client abort response '*' should be sent instead.
        
        """
    def capability(self):
        """
        (typ, [data]) = <instance>.capability()
                Fetch capabilities list from server.
        """
    def check(self):
        """
        Checkpoint mailbox on server.

                (typ, [data]) = <instance>.check()
        
        """
    def close(self):
        """
        Close currently selected mailbox.

                Deleted messages are removed from writable mailbox.
                This is the recommended command before 'LOGOUT'.

                (typ, [data]) = <instance>.close()
        
        """
    def copy(self, message_set, new_mailbox):
        """
        Copy 'message_set' messages onto end of 'new_mailbox'.

                (typ, [data]) = <instance>.copy(message_set, new_mailbox)
        
        """
    def create(self, mailbox):
        """
        Create new mailbox.

                (typ, [data]) = <instance>.create(mailbox)
        
        """
    def delete(self, mailbox):
        """
        Delete old mailbox.

                (typ, [data]) = <instance>.delete(mailbox)
        
        """
    def deleteacl(self, mailbox, who):
        """
        Delete the ACLs (remove any rights) set for who on mailbox.

                (typ, [data]) = <instance>.deleteacl(mailbox, who)
        
        """
    def enable(self, capability):
        """
        Send an RFC5161 enable string to the server.

                (typ, [data]) = <intance>.enable(capability)
        
        """
    def expunge(self):
        """
        Permanently remove deleted items from selected mailbox.

                Generates 'EXPUNGE' response for each deleted message.

                (typ, [data]) = <instance>.expunge()

                'data' is list of 'EXPUNGE'd message numbers in order received.
        
        """
    def fetch(self, message_set, message_parts):
        """
        Fetch (parts of) messages.

                (typ, [data, ...]) = <instance>.fetch(message_set, message_parts)

                'message_parts' should be a string of selected parts
                enclosed in parentheses, eg: "(UID BODY[TEXT])".

                'data' are tuples of message part envelope and data.
        
        """
    def getacl(self, mailbox):
        """
        Get the ACLs for a mailbox.

                (typ, [data]) = <instance>.getacl(mailbox)
        
        """
    def getannotation(self, mailbox, entry, attribute):
        """
        (typ, [data]) = <instance>.getannotation(mailbox, entry, attribute)
                Retrieve ANNOTATIONs.
        """
    def getquota(self, root):
        """
        Get the quota root's resource usage and limits.

                Part of the IMAP4 QUOTA extension defined in rfc2087.

                (typ, [data]) = <instance>.getquota(root)
        
        """
    def getquotaroot(self, mailbox):
        """
        Get the list of quota roots for the named mailbox.

                (typ, [[QUOTAROOT responses...], [QUOTA responses]]) = <instance>.getquotaroot(mailbox)
        
        """
    def list(self, directory='""', pattern='*'):
        """
        List mailbox names in directory matching pattern.

                (typ, [data]) = <instance>.list(directory='""', pattern='*')

                'data' is list of LIST responses.
        
        """
    def login(self, user, password):
        """
        Identify client using plaintext password.

                (typ, [data]) = <instance>.login(user, password)

                NB: 'password' will be quoted.
        
        """
    def login_cram_md5(self, user, password):
        """
         Force use of CRAM-MD5 authentication.

                (typ, [data]) = <instance>.login_cram_md5(user, password)
        
        """
    def _CRAM_MD5_AUTH(self, challenge):
        """
         Authobject to use with CRAM-MD5 authentication. 
        """
    def logout(self):
        """
        Shutdown connection to server.

                (typ, [data]) = <instance>.logout()

                Returns server 'BYE' response.
        
        """
    def lsub(self, directory='""', pattern='*'):
        """
        List 'subscribed' mailbox names in directory matching pattern.

                (typ, [data, ...]) = <instance>.lsub(directory='""', pattern='*')

                'data' are tuples of message part envelope and data.
        
        """
    def myrights(self, mailbox):
        """
        Show my ACLs for a mailbox (i.e. the rights that I have on mailbox).

                (typ, [data]) = <instance>.myrights(mailbox)
        
        """
    def namespace(self):
        """
         Returns IMAP namespaces ala rfc2342

                (typ, [data, ...]) = <instance>.namespace()
        
        """
    def noop(self):
        """
        Send NOOP command.

                (typ, [data]) = <instance>.noop()
        
        """
    def partial(self, message_num, message_part, start, length):
        """
        Fetch truncated part of a message.

                (typ, [data, ...]) = <instance>.partial(message_num, message_part, start, length)

                'data' is tuple of message part envelope and data.
        
        """
    def proxyauth(self, user):
        """
        Assume authentication as "user".

                Allows an authorised administrator to proxy into any user's
                mailbox.

                (typ, [data]) = <instance>.proxyauth(user)
        
        """
    def rename(self, oldmailbox, newmailbox):
        """
        Rename old mailbox name to new.

                (typ, [data]) = <instance>.rename(oldmailbox, newmailbox)
        
        """
    def search(self, charset, *criteria):
        """
        Search mailbox for matching messages.

                (typ, [data]) = <instance>.search(charset, criterion, ...)

                'data' is space separated list of matching message numbers.
                If UTF8 is enabled, charset MUST be None.
        
        """
    def select(self, mailbox='INBOX', readonly=False):
        """
        Select a mailbox.

                Flush all untagged responses.

                (typ, [data]) = <instance>.select(mailbox='INBOX', readonly=False)

                'data' is count of messages in mailbox ('EXISTS' response).

                Mandated responses are ('FLAGS', 'EXISTS', 'RECENT', 'UIDVALIDITY'), so
                other responses should be obtained via <instance>.response('FLAGS') etc.
        
        """
    def setacl(self, mailbox, who, what):
        """
        Set a mailbox acl.

                (typ, [data]) = <instance>.setacl(mailbox, who, what)
        
        """
    def setannotation(self, *args):
        """
        (typ, [data]) = <instance>.setannotation(mailbox[, entry, attribute]+)
                Set ANNOTATIONs.
        """
    def setquota(self, root, limits):
        """
        Set the quota root's resource limits.

                (typ, [data]) = <instance>.setquota(root, limits)
        
        """
    def sort(self, sort_criteria, charset, *search_criteria):
        """
        IMAP4rev1 extension SORT command.

                (typ, [data]) = <instance>.sort(sort_criteria, charset, search_criteria, ...)
        
        """
    def starttls(self, ssl_context=None):
        """
        'STARTTLS'
        """
    def status(self, mailbox, names):
        """
        Request named status conditions for mailbox.

                (typ, [data]) = <instance>.status(mailbox, names)
        
        """
    def store(self, message_set, command, flags):
        """
        Alters flag dispositions for messages in mailbox.

                (typ, [data]) = <instance>.store(message_set, command, flags)
        
        """
    def subscribe(self, mailbox):
        """
        Subscribe to new mailbox.

                (typ, [data]) = <instance>.subscribe(mailbox)
        
        """
    def thread(self, threading_algorithm, charset, *search_criteria):
        """
        IMAPrev1 extension THREAD command.

                (type, [data]) = <instance>.thread(threading_algorithm, charset, search_criteria, ...)
        
        """
    def uid(self, command, *args):
        """
        Execute "command arg ..." with messages identified by UID,
                        rather than message number.

                (typ, [data]) = <instance>.uid(command, arg1, arg2, ...)

                Returns response appropriate to 'command'.
        
        """
    def unsubscribe(self, mailbox):
        """
        Unsubscribe from old mailbox.

                (typ, [data]) = <instance>.unsubscribe(mailbox)
        
        """
    def xatom(self, name, *args):
        """
        Allow simple extension commands
                        notified by server in CAPABILITY response.

                Assumes command is legal in current state.

                (typ, [data]) = <instance>.xatom(name, arg, ...)

                Returns response appropriate to extension command `name'.
        
        """
    def _append_untagged(self, typ, dat):
        """
        b''
        """
    def _check_bye(self):
        """
        'BYE'
        """
    def _command(self, name, *args):
        """
        command %s illegal in state %s, 
        only allowed in states %s
        """
    def _command_complete(self, name, tag):
        """
        'LOGOUT'
        """
    def _get_capabilities(self):
        """
        'no CAPABILITY response from server'
        """
    def _get_response(self):
        """
         Read response and store.

         Returns None for continuation responses,
         otherwise first response line received.


        """
    def _get_tagged_response(self, tag, expect_bye=False):
        """
        'BYE'
        """
    def _get_line(self):
        """
        'socket error: EOF'
        """
    def _match(self, cre, s):
        """
         Run compiled regular expression match method on 's'.
         Save result, return success.


        """
    def _new_tag(self):
        """
        '\\'
        """
    def _simple_command(self, name, *args):
        """
        'NO'
        """
        def _mesg(self, s, secs=None):
            """
            '%M:%S'
            """
        def _dump_ur(self, dict):
            """
             Dump untagged responses (in `dict').

            """
        def _log(self, line):
            """
             Keep log of last `_cmd_log_len' interactions for debugging.

            """
        def print_log(self):
            """
            'last %d IMAP4 interactions:'
            """
    def IMAP4_SSL(IMAP4):
    """
    IMAP4 client class over SSL connection

            Instantiate with: IMAP4_SSL([host[, port[, keyfile[, certfile[, ssl_context]]]]])

                    host - host's name (default: localhost);
                    port - port number (default: standard IMAP4 SSL port);
                    keyfile - PEM formatted file that contains your private key (default: None);
                    certfile - PEM formatted certificate chain file (default: None);
                    ssl_context - a SSLContext object that contains your certificate chain
                                  and private key (default: None)
                    Note: if ssl_context is provided, then parameters keyfile or
                    certfile should not be set otherwise ValueError is raised.

            for more documentation see the docstring of the parent class IMAP4.
        
    """
2021-03-02 20:53:55,130 : INFO : tokenize_signature : --> do i ever get here?
        def __init__(self, host='', port=IMAP4_SSL_PORT, keyfile=None,
                     certfile=None, ssl_context=None):
            """
            ssl_context and keyfile arguments are mutually 
            exclusive
            """
        def _create_socket(self):
            """
            ''
            """
def IMAP4_stream(IMAP4):
    """
    IMAP4 client class over a stream

        Instantiate with: IMAP4_stream(command)

                "command" - a string that can be passed to subprocess.Popen()

        for more documentation see the docstring of the parent class IMAP4.
    
    """
    def __init__(self, command):
        """
        Setup a stream connection.
                This connection will be used by the routines:
                    read, readline, send, shutdown.
        
        """
    def read(self, size):
        """
        Read 'size' bytes from remote.
        """
    def readline(self):
        """
        Read line from remote.
        """
    def send(self, data):
        """
        Send data to remote.
        """
    def shutdown(self):
        """
        Close I/O established in "open".
        """
def _Authenticator:
    """
    Private class to provide en/decoding
                for base64-based authentication conversation.
    
    """
    def __init__(self, mechinst):
        """
         Callable object to provide/process data
        """
    def process(self, data):
        """
        b'*' Abort conversation
        """
    def encode(self, inp):
        """

          Invoke binascii.b2a_base64 iteratively with
          short even length buffers, strip the trailing
          line feed from the result and append.  "Even
          means a number that factors to both 6 and 8,
          so when it gets to the end of the 8-bit input
          there's no partial 6-bit output.


        """
    def decode(self, inp):
        """
        b''
        """
def Internaldate2tuple(resp):
    """
    Parse an IMAP4 INTERNALDATE string.

        Return corresponding local time.  The return value is a
        time.struct_time tuple or None if the string has wrong format.
    
    """
def Int2AP(num):
    """
    Convert integer to A-P string representation.
    """
def ParseFlags(resp):
    """
    Convert IMAP4 flags response to python tuple.
    """
def Time2Internaldate(date_time):
    """
    Convert date_time to IMAP4 INTERNALDATE representation.

        Return string in form: '"DD-Mmm-YYYY HH:MM:SS +HHMM"'.  The
        date_time argument can be a number (int or float) representing
        seconds since epoch (as returned by time.time()), a 9-tuple
        representing local time, an instance of time.struct_time (as
        returned by time.localtime()), an aware datetime instance or a
        double-quoted string.  In the last case, it is assumed to already
        be in the correct format.
    
    """
    def run(cmd, args):
        """
        '%s %s'
        """
