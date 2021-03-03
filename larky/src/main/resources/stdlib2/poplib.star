def error_proto(Exception): pass
    """
     Standard Port

    """
def POP3:
    """
    This class supports both the minimal and optional command sets.
        Arguments can be strings or integers (where appropriate)
        (e.g.: retr(1) and retr('1') both work equally well.

        Minimal Command Set:
                USER name               user(name)
                PASS string             pass_(string)
                STAT                    stat()
                LIST [msg]              list(msg = None)
                RETR msg                retr(msg)
                DELE msg                dele(msg)
                NOOP                    noop()
                RSET                    rset()
                QUIT                    quit()

        Optional Commands (some servers support these):
                RPOP name               rpop(name)
                APOP name digest        apop(name, digest)
                TOP msg n               top(msg, n)
                UIDL [msg]              uidl(msg = None)
                CAPA                    capa()
                STLS                    stls()
                UTF8                    utf8()

        Raises one exception: 'error_proto'.

        Instantiate with:
                POP3(hostname, port=110)

        NB:     the POP protocol locks the mailbox from user
                authorization until QUIT, so be sure to get in, suck
                the messages, and quit, each time you access the
                mailbox.

                POP is a line-based protocol, which means large mail
                messages consume lots of python cycles reading them
                line-by-line.

                If it's available on your mail server, use IMAP4
                instead, it doesn't suffer from the two problems
                above.
    
    """
2021-03-02 20:53:47,619 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, host, port=POP3_PORT,
                 timeout=socket._GLOBAL_DEFAULT_TIMEOUT):
        """
        poplib.connect
        """
    def _create_socket(self, timeout):
        """
        '*put*'
        """
    def _putcmd(self, line):
        """
        '*cmd*'
        """
    def _getline(self):
        """
        'line too long'
        """
    def _getresp(self):
        """
        '*resp*'
        """
    def _getlongresp(self):
        """
        b'.'
        """
    def _shortcmd(self, line):
        """
         Internal: send a command and get the response plus following text


        """
    def _longcmd(self, line):
        """
         These can be useful:


        """
    def getwelcome(self):
        """
         Here are all the POP commands:


        """
    def user(self, user):
        """
        Send user name, return response

                (should indicate password required).
        
        """
    def pass_(self, pswd):
        """
        Send password, return response

                (response includes message count, mailbox size).

                NB: mailbox is locked by server from here to 'quit()'
        
        """
    def stat(self):
        """
        Get mailbox status.

                Result is tuple of 2 ints (message count, mailbox size)
        
        """
    def list(self, which=None):
        """
        Request listing, return result.

                Result without a message number argument is in form
                ['response', ['mesg_num octets', ...], octets].

                Result when a message number argument is given is a
                single response: the "scan listing" for that message.
        
        """
    def retr(self, which):
        """
        Retrieve whole message number 'which'.

                Result is in form ['response', ['line', ...], octets].
        
        """
    def dele(self, which):
        """
        Delete message number 'which'.

                Result is 'response'.
        
        """
    def noop(self):
        """
        Does nothing.

                One supposes the response indicates the server is alive.
        
        """
    def rset(self):
        """
        Unmark all messages marked for deletion.
        """
    def quit(self):
        """
        Signoff: commit changes on server, unlock mailbox, close connection.
        """
    def close(self):
        """
        Close the connection without assuming anything about it.
        """
    def rpop(self, user):
        """
        Not sure what this does.
        """
    def apop(self, user, password):
        """
        Authorisation

                - only possible if server has supplied a timestamp in initial greeting.

                Args:
                        user     - mailbox user;
                        password - mailbox password.

                NB: mailbox is locked by server from here to 'quit()'
        
        """
    def top(self, which, howmuch):
        """
        Retrieve message header of message number 'which'
                and first 'howmuch' lines of message body.

                Result is in form ['response', ['line', ...], octets].
        
        """
    def uidl(self, which=None):
        """
        Return message digest (unique id) list.

                If 'which', result contains unique id for that message
                in the form 'response mesgnum uid', otherwise result is
                the list ['response', ['mesgnum uid', ...], octets]
        
        """
    def utf8(self):
        """
        Try to enter UTF-8 mode (see RFC 6856). Returns server response.
        
        """
    def capa(self):
        """
        Return server capabilities (RFC 2449) as a dictionary
                >>> c=poplib.POP3('localhost')
                >>> c.capa()
                {'IMPLEMENTATION': ['Cyrus', 'POP3', 'server', 'v2.2.12'],
                 'TOP': [], 'LOGIN-DELAY': ['0'], 'AUTH-RESP-CODE': [],
                 'EXPIRE': ['NEVER'], 'USER': [], 'STLS': [], 'PIPELINING': [],
                 'UIDL': [], 'RESP-CODES': []}
                >>>

                Really, according to RFC 2449, the cyrus folks should avoid
                having the implementation split into multiple arguments...
        
        """
        def _parsecap(line):
            """
            'ascii'
            """
    def stls(self, context=None):
        """
        Start a TLS session on the active connection as specified in RFC 2595.

                        context - a ssl.SSLContext
        
        """
    def POP3_SSL(POP3):
    """
    POP3 client class over SSL connection

            Instantiate with: POP3_SSL(hostname, port=995, keyfile=None, certfile=None,
                                       context=None)

                   hostname - the hostname of the pop3 over ssl server
                   port - port number
                   keyfile - PEM formatted file that contains your private key
                   certfile - PEM formatted certificate chain file
                   context - a ssl.SSLContext

            See the methods of the parent class POP3 for more documentation.
        
    """
2021-03-02 20:53:47,627 : INFO : tokenize_signature : --> do i ever get here?
        def __init__(self, host, port=POP3_SSL_PORT, keyfile=None, certfile=None,
                     timeout=socket._GLOBAL_DEFAULT_TIMEOUT, context=None):
            """
            context and keyfile arguments are mutually 
            exclusive
            """
        def _create_socket(self, timeout):
            """
            The method unconditionally raises an exception since the
                        STLS command doesn't make any sense on an already established
                        SSL/TLS session.
            
            """
