def SMTPException(OSError):
    """
    Base class for all exceptions raised by this module.
    """
def SMTPNotSupportedError(SMTPException):
    """
    The command or option is not supported by the SMTP server.

        This exception is raised when an attempt is made to run a command or a
        command with an option which is not supported by the server.
    
    """
def SMTPServerDisconnected(SMTPException):
    """
    Not connected to any SMTP server.

        This exception is raised when the server unexpectedly disconnects,
        or when an attempt is made to use the SMTP instance before
        connecting it to a server.
    
    """
def SMTPResponseException(SMTPException):
    """
    Base class for all exceptions that include an SMTP error code.

        These exceptions are generated in some instances when the SMTP
        server returns an error code.  The error code is stored in the
        `smtp_code' attribute of the error, and the `smtp_error' attribute
        is set to the error message.
    
    """
    def __init__(self, code, msg):
        """
        Sender address refused.

            In addition to the attributes set by on all SMTPResponseException
            exceptions, this sets `sender' to the string that the SMTP refused.
    
        """
    def __init__(self, code, msg, sender):
        """
        All recipient addresses refused.

            The errors for each recipient are accessible through the attribute
            'recipients', which is a dictionary of exactly the same sort as
            SMTP.sendmail() returns.
    
        """
    def __init__(self, recipients):
        """
        The SMTP server didn't accept the data.
        """
def SMTPConnectError(SMTPResponseException):
    """
    Error during connection establishment.
    """
def SMTPHeloError(SMTPResponseException):
    """
    The server refused our HELO reply.
    """
def SMTPAuthenticationError(SMTPResponseException):
    """
    Authentication error.

        Most probably the server didn't accept the username/password
        combination provided.
    
    """
def quoteaddr(addrstring):
    """
    Quote a subset of the email addresses defined by RFC 821.

        Should be able to handle anything email.utils.parseaddr can handle.
    
    """
def _addr_only(addrstring):
    """
    ''
    """
def quotedata(data):
    """
    Quote data for email.

        Double leading '.', and change Unix newline '\\n', or Mac '\\r' into
        Internet CRLF end-of-line.
    
    """
def _quote_periods(bindata):
    """
    br'(?m)^\.'
    """
def _fix_eols(data):
    """
    r'(?:\r\n|\n|\r(?!\n))'
    """
def SMTP:
    """
    This class manages a connection to an SMTP or ESMTP server.
        SMTP Objects:
            SMTP objects have the following attributes:
                helo_resp
                    This is the message given by the server in response to the
                    most recent HELO command.

                ehlo_resp
                    This is the message given by the server in response to the
                    most recent EHLO command. This is usually multiline.

                does_esmtp
                    This is a True value _after you do an EHLO command_, if the
                    server supports ESMTP.

                esmtp_features
                    This is a dictionary, which, if the server supports ESMTP,
                    will _after you do an EHLO command_, contain the names of the
                    SMTP service extensions this server supports, and their
                    parameters (if any).

                    Note, all extension names are mapped to lower case in the
                    dictionary.

            See each method's docstrings for details.  In general, there is a
            method of the same name to perform each SMTP command.  There is also a
            method called 'sendmail' that will do an entire mail transaction.
        
    """
2021-03-02 20:53:58,486 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:58,486 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, host='', port=0, local_hostname=None,
                 timeout=socket._GLOBAL_DEFAULT_TIMEOUT,
                 source_address=None):
        """
        Initialize a new instance.

                If specified, `host' is the name of the remote host to which to
                connect.  If specified, `port' specifies the port to which to connect.
                By default, smtplib.SMTP_PORT is used.  If a host is specified the
                connect method is called, and if it returns anything other than a
                success code an SMTPConnectError is raised.  If specified,
                `local_hostname` is used as the FQDN of the local host in the HELO/EHLO
                command.  Otherwise, the local hostname is found using
                socket.getfqdn(). The `source_address` parameter takes a 2-tuple (host,
                port) for the socket to bind to as its source address before
                connecting. If the host is '' and port is 0, the OS default behavior
                will be used.

        
        """
    def __enter__(self):
        """
        QUIT
        """
    def set_debuglevel(self, debuglevel):
        """
        Set the debug output level.

                A non-false value results in debug messages for connection and for all
                messages sent to and received from the server.

        
        """
    def _print_debug(self, *args):
        """
         This makes it simpler for SMTP_SSL to use the SMTP connect code
         and just alter the socket connection bit.

        """
    def connect(self, host='localhost', port=0, source_address=None):
        """
        Connect to a host on a given port.

                If the hostname ends with a colon (`:') followed by a number, and
                there is no port specified, that suffix will be stripped off and the
                number interpreted as the port number to use.

                Note: This method is automatically invoked by __init__, if a host is
                specified during instantiation.

        
        """
    def send(self, s):
        """
        Send `s' to the server.
        """
    def putcmd(self, cmd, args=""):
        """
        Send a command to the server.
        """
    def getreply(self):
        """
        Get a reply from the server.

                Returns a tuple consisting of:

                  - server response code (e.g. '250', or such, if all goes well)
                    Note: returns -1 if it can't read response code.

                  - server response string corresponding to response code (multiline
                    responses are converted to a single, multiline string).

                Raises SMTPServerDisconnected if end-of-file is reached.
        
        """
    def docmd(self, cmd, args=""):
        """
        Send a command, and return its response code.
        """
    def helo(self, name=''):
        """
        SMTP 'helo' command.
                Hostname to send for this command defaults to the FQDN of the local
                host.
        
        """
    def ehlo(self, name=''):
        """
         SMTP 'ehlo' command.
                Hostname to send for this command defaults to the FQDN of the local
                host.
        
        """
    def has_extn(self, opt):
        """
        Does the server support a given SMTP service extension?
        """
    def help(self, args=''):
        """
        SMTP 'help' command.
                Returns help text from server.
        """
    def rset(self):
        """
        SMTP 'rset' command -- resets session.
        """
    def _rset(self):
        """
        Internal 'rset' command which ignores any SMTPServerDisconnected error.

                Used internally in the library, since the server disconnected error
                should appear to the application when the *next* command is issued, if
                we are doing an internal "safety" reset.
        
        """
    def noop(self):
        """
        SMTP 'noop' command -- doesn't do anything :>
        """
    def mail(self, sender, options=()):
        """
        SMTP 'mail' command -- begins mail xfer session.

                This method may raise the following exceptions:

                 SMTPNotSupportedError  The options parameter includes 'SMTPUTF8'
                                        but the SMTPUTF8 extension is not supported by
                                        the server.
        
        """
    def rcpt(self, recip, options=()):
        """
        SMTP 'rcpt' command -- indicates 1 recipient for this mail.
        """
    def data(self, msg):
        """
        SMTP 'DATA' command -- sends message data to server.

                Automatically quotes lines beginning with a period per rfc821.
                Raises SMTPDataError if there is an unexpected reply to the
                DATA command; the return value from this method is the final
                response code received when the all data is sent.  If msg
                is a string, lone '\\r' and '\\n' characters are converted to
                '\\r\\n' characters.  If msg is bytes, it is transmitted as is.
        
        """
    def verify(self, address):
        """
        SMTP 'verify' command -- checks for address validity.
        """
    def expn(self, address):
        """
        SMTP 'expn' command -- expands a mailing list.
        """
    def ehlo_or_helo_if_needed(self):
        """
        Call self.ehlo() and/or self.helo() if needed.

                If there has been no previous EHLO or HELO command this session, this
                method tries ESMTP EHLO first.

                This method may raise the following exceptions:

                 SMTPHeloError            The server didn't reply properly to
                                          the helo greeting.
        
        """
    def auth(self, mechanism, authobject, *, initial_response_ok=True):
        """
        Authentication command - requires response processing.

                'mechanism' specifies which authentication mechanism is to
                be used - the valid values are those listed in the 'auth'
                element of 'esmtp_features'.

                'authobject' must be a callable object taking a single argument:

                        data = authobject(challenge)

                It will be called to process the server's challenge response; the
                challenge argument it is passed will be a bytes.  It should return
                an ASCII string that will be base64 encoded and sent to the server.

                Keyword arguments:
                    - initial_response_ok: Allow sending the RFC 4954 initial-response
                      to the AUTH command, if the authentication methods supports it.
        
        """
    def auth_cram_md5(self, challenge=None):
        """
         Authobject to use with CRAM-MD5 authentication. Requires self.user
                and self.password to be set.
        """
    def auth_plain(self, challenge=None):
        """
         Authobject to use with PLAIN authentication. Requires self.user and
                self.password to be set.
        """
    def auth_login(self, challenge=None):
        """
         Authobject to use with LOGIN authentication. Requires self.user and
                self.password to be set.
        """
    def login(self, user, password, *, initial_response_ok=True):
        """
        Log in on an SMTP server that requires authentication.

                The arguments are:
                    - user:         The user name to authenticate with.
                    - password:     The password for the authentication.

                Keyword arguments:
                    - initial_response_ok: Allow sending the RFC 4954 initial-response
                      to the AUTH command, if the authentication methods supports it.

                If there has been no previous EHLO or HELO command this session, this
                method tries ESMTP EHLO first.

                This method will return normally if the authentication was successful.

                This method may raise the following exceptions:

                 SMTPHeloError            The server didn't reply properly to
                                          the helo greeting.
                 SMTPAuthenticationError  The server didn't accept the username/
                                          password combination.
                 SMTPNotSupportedError    The AUTH command is not supported by the
                                          server.
                 SMTPException            No suitable authentication method was
                                          found.
        
        """
    def starttls(self, keyfile=None, certfile=None, context=None):
        """
        Puts the connection to the SMTP server into TLS mode.

                If there has been no previous EHLO or HELO command this session, this
                method tries ESMTP EHLO first.

                If the server supports TLS, this will encrypt the rest of the SMTP
                session. If you provide the keyfile and certfile parameters,
                the identity of the SMTP server and client can be checked. This,
                however, depends on whether the socket module really checks the
                certificates.

                This method may raise the following exceptions:

                 SMTPHeloError            The server didn't reply properly to
                                          the helo greeting.
        
        """
2021-03-02 20:53:58,499 : INFO : tokenize_signature : --> do i ever get here?
    def sendmail(self, from_addr, to_addrs, msg, mail_options=(),
                 rcpt_options=()):
        """
        This command performs an entire mail transaction.

                The arguments are:
                    - from_addr    : The address sending this mail.
                    - to_addrs     : A list of addresses to send this mail to.  A bare
                                     string will be treated as a list with 1 address.
                    - msg          : The message to send.
                    - mail_options : List of ESMTP options (such as 8bitmime) for the
                                     mail command.
                    - rcpt_options : List of ESMTP options (such as DSN commands) for
                                     all the rcpt commands.

                msg may be a string containing characters in the ASCII range, or a byte
                string.  A string is encoded to bytes using the ascii codec, and lone
                \\r and \\n characters are converted to \\r\\n characters.

                If there has been no previous EHLO or HELO command this session, this
                method tries ESMTP EHLO first.  If the server does ESMTP, message size
                and each of the specified options will be passed to it.  If EHLO
                fails, HELO will be tried and ESMTP options suppressed.

                This method will return normally if the mail is accepted for at least
                one recipient.  It returns a dictionary, with one entry for each
                recipient that was refused.  Each entry contains a tuple of the SMTP
                error code and the accompanying error message sent by the server.

                This method may raise the following exceptions:

                 SMTPHeloError          The server didn't reply properly to
                                        the helo greeting.
                 SMTPRecipientsRefused  The server rejected ALL recipients
                                        (no mail was sent).
                 SMTPSenderRefused      The server didn't accept the from_addr.
                 SMTPDataError          The server replied with an unexpected
                                        error code (other than a refusal of
                                        a recipient).
                 SMTPNotSupportedError  The mail_options parameter includes 'SMTPUTF8'
                                        but the SMTPUTF8 extension is not supported by
                                        the server.

                Note: the connection will be open even after an exception is raised.

                Example:

                 >>> import smtplib
                 >>> s=smtplib.SMTP("localhost")
                 >>> tolist=["one@one.org","two@two.org","three@three.org","four@four.org"]
                 >>> msg = '''\\
                 ... From: Me@my.org
                 ... Subject: testin'...
                 ...
                 ... This is a test '''
                 >>> s.sendmail("me@my.org",tolist,msg)
                 { "three@three.org" : ( 550 ,"User unknown" ) }
                 >>> s.quit()

                In the above example, the message was accepted for delivery to three
                of the four addresses, and one was rejected, with the error code
                550.  If all addresses are accepted, then the method will return an
                empty dictionary.

        
        """
2021-03-02 20:53:58,501 : INFO : tokenize_signature : --> do i ever get here?
    def send_message(self, msg, from_addr=None, to_addrs=None,
                     mail_options=(), rcpt_options=()):
        """
        Converts message to a bytestring and passes it to sendmail.

                The arguments are as for sendmail, except that msg is an
                email.message.Message object.  If from_addr is None or to_addrs is
                None, these arguments are taken from the headers of the Message as
                described in RFC 2822 (a ValueError is raised if there is more than
                one set of 'Resent-' headers).  Regardless of the values of from_addr and
                to_addr, any Bcc field (or Resent-Bcc field, when the Message is a
                resent) of the Message object won't be transmitted.  The Message
                object is then serialized using email.generator.BytesGenerator and
                sendmail is called to transmit the message.  If the sender or any of
                the recipient addresses contain non-ASCII and the server advertises the
                SMTPUTF8 capability, the policy is cloned with utf8 set to True for the
                serialization, and SMTPUTF8 and BODY=8BITMIME are asserted on the send.
                If the server does not support SMTPUTF8, an SMTPNotSupported error is
                raised.  Otherwise the generator is called without modifying the
                policy.

        
        """
    def close(self):
        """
        Close the connection to the SMTP server.
        """
    def quit(self):
        """
        Terminate the SMTP session.
        """
    def SMTP_SSL(SMTP):
    """
     This is a subclass derived from SMTP that connects over an SSL
            encrypted socket (to use this class you need a socket module that was
            compiled with SSL support). If host is not specified, '' (the local
            host) is used. If port is omitted, the standard SMTP-over-SSL port
            (465) is used.  local_hostname and source_address have the same meaning
            as they do in the SMTP class.  keyfile and certfile are also optional -
            they can contain a PEM formatted private key and certificate chain file
            for the SSL connection. context also optional, can contain a
            SSLContext, and is an alternative to keyfile and certfile; If it is
            specified both keyfile and certfile must be None.

        
    """
2021-03-02 20:53:58,503 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:58,503 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:58,503 : INFO : tokenize_signature : --> do i ever get here?
        def __init__(self, host='', port=0, local_hostname=None,
                     keyfile=None, certfile=None,
                     timeout=socket._GLOBAL_DEFAULT_TIMEOUT,
                     source_address=None, context=None):
            """
            context and keyfile arguments are mutually 
            exclusive
            """
        def _get_socket(self, host, port, timeout):
            """
            'connect:'
            """
def LMTP(SMTP):
    """
    LMTP - Local Mail Transfer Protocol

        The LMTP protocol, which is very similar to ESMTP, is heavily based
        on the standard SMTP client. It's common to use Unix sockets for
        LMTP, so our connect() method must support that as well as a regular
        host:port server.  local_hostname and source_address have the same
        meaning as they do in the SMTP class.  To specify a Unix socket,
        you must use an absolute path as the host, starting with a '/'.

        Authentication is supported, using the regular SMTP mechanism. When
        using a Unix socket, LMTP generally don't support or require any
        authentication, but your mileage might vary.
    """
2021-03-02 20:53:58,504 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, host='', port=LMTP_PORT, local_hostname=None,
            source_address=None):
        """
        Initialize a new instance.
        """
    def connect(self, host='localhost', port=0, source_address=None):
        """
        Connect to the LMTP daemon, on either a Unix or a TCP socket.
        """
    def prompt(prompt):
        """
        : 
        """
