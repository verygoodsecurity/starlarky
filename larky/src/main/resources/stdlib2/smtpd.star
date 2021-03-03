def Devnull:
    """
    '\n'
    """
def usage(code, msg=''):
    """
    decode_data and enable_SMTPUTF8 cannot
     be set to True at the same time
    """
    def _set_post_data_state(self):
        """
        Reset state variables to their post-DATA state.
        """
    def _set_rset_state(self):
        """
        Reset all state variables except the greeting.
        """
    def __server(self):
        """
        Access to __server attribute on SMTPChannel is deprecated, 
        use 'smtp_server' instead
        """
    def __server(self, value):
        """
        Setting __server attribute on SMTPChannel is deprecated, 
        set 'smtp_server' instead
        """
    def __line(self):
        """
        Access to __line attribute on SMTPChannel is deprecated, 
        use 'received_lines' instead
        """
    def __line(self, value):
        """
        Setting __line attribute on SMTPChannel is deprecated, 
        set 'received_lines' instead
        """
    def __state(self):
        """
        Access to __state attribute on SMTPChannel is deprecated, 
        use 'smtp_state' instead
        """
    def __state(self, value):
        """
        Setting __state attribute on SMTPChannel is deprecated, 
        set 'smtp_state' instead
        """
    def __greeting(self):
        """
        Access to __greeting attribute on SMTPChannel is deprecated, 
        use 'seen_greeting' instead
        """
    def __greeting(self, value):
        """
        Setting __greeting attribute on SMTPChannel is deprecated, 
        set 'seen_greeting' instead
        """
    def __mailfrom(self):
        """
        Access to __mailfrom attribute on SMTPChannel is deprecated, 
        use 'mailfrom' instead
        """
    def __mailfrom(self, value):
        """
        Setting __mailfrom attribute on SMTPChannel is deprecated, 
        set 'mailfrom' instead
        """
    def __rcpttos(self):
        """
        Access to __rcpttos attribute on SMTPChannel is deprecated, 
        use 'rcpttos' instead
        """
    def __rcpttos(self, value):
        """
        Setting __rcpttos attribute on SMTPChannel is deprecated, 
        set 'rcpttos' instead
        """
    def __data(self):
        """
        Access to __data attribute on SMTPChannel is deprecated, 
        use 'received_data' instead
        """
    def __data(self, value):
        """
        Setting __data attribute on SMTPChannel is deprecated, 
        set 'received_data' instead
        """
    def __fqdn(self):
        """
        Access to __fqdn attribute on SMTPChannel is deprecated, 
        use 'fqdn' instead
        """
    def __fqdn(self, value):
        """
        Setting __fqdn attribute on SMTPChannel is deprecated, 
        set 'fqdn' instead
        """
    def __peer(self):
        """
        Access to __peer attribute on SMTPChannel is deprecated, 
        use 'peer' instead
        """
    def __peer(self, value):
        """
        Setting __peer attribute on SMTPChannel is deprecated, 
        set 'peer' instead
        """
    def __conn(self):
        """
        Access to __conn attribute on SMTPChannel is deprecated, 
        use 'conn' instead
        """
    def __conn(self, value):
        """
        Setting __conn attribute on SMTPChannel is deprecated, 
        set 'conn' instead
        """
    def __addr(self):
        """
        Access to __addr attribute on SMTPChannel is deprecated, 
        use 'addr' instead
        """
    def __addr(self, value):
        """
        Setting __addr attribute on SMTPChannel is deprecated, 
        set 'addr' instead
        """
    def push(self, msg):
        """
        '\r\n'
        """
    def collect_incoming_data(self, data):
        """
        'utf-8'
        """
    def found_terminator(self):
        """
        'Data:'
        """
    def smtp_HELO(self, arg):
        """
        '501 Syntax: HELO hostname'
        """
    def smtp_EHLO(self, arg):
        """
        '501 Syntax: EHLO hostname'
        """
    def smtp_NOOP(self, arg):
        """
        '501 Syntax: NOOP'
        """
    def smtp_QUIT(self, arg):
        """
         args is ignored

        """
    def _strip_command_keyword(self, keyword, arg):
        """
        ''
        """
    def _getaddr(self, arg):
        """
        ''
        """
    def _getparams(self, params):
        """
         Return params as dictionary. Return None if not all parameters
         appear to be syntactically valid according to RFC 1869.

        """
    def smtp_HELP(self, arg):
        """
        ' [SP <mail-parameters>]'
        """
    def smtp_VRFY(self, arg):
        """
        '252 Cannot VRFY user, but will accept message '
        'and attempt delivery'
        """
    def smtp_MAIL(self, arg):
        """
        '503 Error: send HELO first'
        """
    def smtp_RCPT(self, arg):
        """
        '503 Error: send HELO first'
        """
    def smtp_RSET(self, arg):
        """
        '501 Syntax: RSET'
        """
    def smtp_DATA(self, arg):
        """
        '503 Error: send HELO first'
        """
    def smtp_EXPN(self, arg):
        """
        '502 EXPN not implemented'
        """
def SMTPServer(asyncore.dispatcher):
    """
     SMTPChannel class to use for managing client connections

    """
2021-03-02 20:54:36,418 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:36,418 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, localaddr, remoteaddr,
                 data_size_limit=DATA_SIZE_DEFAULT, map=None,
                 enable_SMTPUTF8=False, decode_data=False):
        """
        decode_data and enable_SMTPUTF8 cannot
         be set to True at the same time
        """
    def handle_accepted(self, conn, addr):
        """
        'Incoming connection from %s'
        """
    def process_message(self, peer, mailfrom, rcpttos, data, **kwargs):
        """
        Override this abstract method to handle messages from the client.

                peer is a tuple containing (ipaddr, port) of the client that made the
                socket connection to our smtp port.

                mailfrom is the raw address the client claims the message is coming
                from.

                rcpttos is a list of raw addresses the client wishes to deliver the
                message to.

                data is a string containing the entire full text of the message,
                headers (if supplied) and all.  It has been `de-transparencied'
                according to RFC 821, Section 4.5.2.  In other words, a line
                containing a `.' followed by other text has had the leading dot
                removed.

                kwargs is a dictionary containing additional information.  It is
                empty if decode_data=True was given as init parameter, otherwise
                it will contain the following keys:
                    'mail_options': list of parameters to the mail command.  All
                                    elements are uppercase strings.  Example:
                                    ['BODY=8BITMIME', 'SMTPUTF8'].
                    'rcpt_options': same, for the rcpt command.

                This function should return None for a normal `250 Ok' response;
                otherwise, it should return the desired response string in RFC 821
                format.

        
        """
def DebuggingServer(SMTPServer):
    """
     headers first

    """
    def process_message(self, peer, mailfrom, rcpttos, data, **kwargs):
        """
        '---------- MESSAGE FOLLOWS ----------'
        """
def PureProxy(SMTPServer):
    """
    'enable_SMTPUTF8'
    """
    def process_message(self, peer, mailfrom, rcpttos, data):
        """
        '\n'
        """
    def _deliver(self, mailfrom, rcpttos, data):
        """
        'got SMTPRecipientsRefused'
        """
def MailmanProxy(PureProxy):
    """
    'enable_SMTPUTF8'
    """
    def process_message(self, peer, mailfrom, rcpttos, data):
        """
         If the message is to a Mailman mailing list, then we'll invoke the
         Mailman script directly, without going through the real smtpd.
         Otherwise we'll forward it to the local proxy for disposition.

        """
def Options:
    """
    'PureProxy'
    """
def parseargs():
    """
    'nVhc:s:du'
    """
