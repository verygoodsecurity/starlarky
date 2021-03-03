def Error(Exception): pass
    """
     unexpected [123]xx reply
    """
def error_temp(Error): pass           # 4xx errors
    """
     4xx errors
    """
def error_perm(Error): pass           # 5xx errors
    """
     5xx errors
    """
def error_proto(Error): pass          # response does not begin with [1-5]
    """
     response does not begin with [1-5]
    """
def FTP:
    """
    '''An FTP client class.

        To create a connection, call the class using these arguments:
                host, user, passwd, acct, timeout

        The first four arguments are all strings, and have default value ''.
        timeout must be numeric and defaults to None if not passed,
        meaning that no timeout will be set on any ftp socket(s)
        If a timeout is passed, then this is now the default timeout for all ftp
        socket operations for this instance.

        Then use self.connect() with optional host and port argument.

        To download a file, use ftp.retrlines('RETR ' + filename),
        or ftp.retrbinary() with slightly different arguments.
        To upload a file, use ftp.storlines() or ftp.storbinary(),
        which have an open file as argument (see their definitions
        below for details).
        The download/upload functions first issue appropriate TYPE
        and PORT or PASV commands.
        '''
    """
2021-03-02 20:53:39,066 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, host='', user='', passwd='', acct='',
                 timeout=_GLOBAL_DEFAULT_TIMEOUT, source_address=None):
        """
         Context management protocol: try to quit() if active

        """
    def __exit__(self, *args):
        """
        ''
        """
    def getwelcome(self):
        """
        '''Get the welcome message from the server.
                (this is read and squirreled away by connect())'''
        """
    def set_debuglevel(self, level):
        """
        '''Set the debugging level.
                The required argument level means:
                0: no debugging output (default)
                1: print commands and responses but not body text etc.
                2: also print raw lines read and sent before stripping CR/LF'''
        """
    def set_pasv(self, val):
        """
        '''Use passive or active mode for data transfers.
                With a false argument, use the normal PORT mode,
                With a true argument, use the PASV command.'''
        """
    def sanitize(self, s):
        """
        'pass '
        """
    def putline(self, line):
        """
        '\r'
        """
    def putcmd(self, line):
        """
        '*cmd*'
        """
    def getline(self):
        """
        got more than %d bytes
        """
    def getmultiline(self):
        """
        '-'
        """
    def getresp(self):
        """
        '*resp*'
        """
    def voidresp(self):
        """
        Expect a response beginning with '2'.
        """
    def abort(self):
        """
        '''Abort a file transfer.  Uses out-of-band data.
                This does not follow the procedure from the RFC to send Telnet
                IP and Synch; that doesn't seem to work with the servers I've
                tried.  Instead, just send the ABOR command as OOB data.'''
        """
    def sendcmd(self, cmd):
        """
        '''Send a command and return the response.'''
        """
    def voidcmd(self, cmd):
        """
        Send a command and expect a response beginning with '2'.
        """
    def sendport(self, host, port):
        """
        '''Send a PORT command with the current host and the given
                port number.
                '''
        """
    def sendeprt(self, host, port):
        """
        '''Send an EPRT command with the current host and the given port number.'''
        """
    def makeport(self):
        """
        '''Create a new socket and send a PORT command for it.'''
        """
    def makepasv(self):
        """
        'PASV'
        """
    def ntransfercmd(self, cmd, rest=None):
        """
        Initiate a transfer over the data connection.

                If the transfer is active, send a port command and the
                transfer command, and accept the connection.  If the server is
                passive, send a pasv command, connect to it, and start the
                transfer command.  Either way, return the socket for the
                connection and the expected size of the transfer.  The
                expected size may be None if it could not be determined.

                Optional `rest' argument can be a string that is sent as the
                argument to a REST command.  This is essentially a server
                marker used to tell the server to skip over any data up to the
                given marker.
        
        """
    def transfercmd(self, cmd, rest=None):
        """
        Like ntransfercmd() but returns only the socket.
        """
    def login(self, user = '', passwd = '', acct = ''):
        """
        '''Login, default anonymous.'''
        """
    def retrbinary(self, cmd, callback, blocksize=8192, rest=None):
        """
        Retrieve data in binary mode.  A new port is created for you.

                Args:
                  cmd: A RETR command.
                  callback: A single parameter callable to be called on each
                            block of data read.
                  blocksize: The maximum number of bytes to read from the
                             socket at one time.  [default: 8192]
                  rest: Passed to transfercmd().  [default: None]

                Returns:
                  The response code.
        
        """
    def retrlines(self, cmd, callback = None):
        """
        Retrieve data in line mode.  A new port is created for you.

                Args:
                  cmd: A RETR, LIST, or NLST command.
                  callback: An optional single parameter callable that is called
                            for each line with the trailing CRLF stripped.
                            [default: print_line()]

                Returns:
                  The response code.
        
        """
    def storbinary(self, cmd, fp, blocksize=8192, callback=None, rest=None):
        """
        Store a file in binary mode.  A new port is created for you.

                Args:
                  cmd: A STOR command.
                  fp: A file-like object with a read(num_bytes) method.
                  blocksize: The maximum data size to read from fp and send over
                             the connection at once.  [default: 8192]
                  callback: An optional single parameter callable that is called on
                            each block of data after it is sent.  [default: None]
                  rest: Passed to transfercmd().  [default: None]

                Returns:
                  The response code.
        
        """
    def storlines(self, cmd, fp, callback=None):
        """
        Store a file in line mode.  A new port is created for you.

                Args:
                  cmd: A STOR command.
                  fp: A file-like object with a readline() method.
                  callback: An optional single parameter callable that is called on
                            each line after it is sent.  [default: None]

                Returns:
                  The response code.
        
        """
    def acct(self, password):
        """
        '''Send new account name.'''
        """
    def nlst(self, *args):
        """
        '''Return a list of files in a given directory (default the current).'''
        """
    def dir(self, *args):
        """
        '''List a directory in long form.
                By default list current directory to stdout.
                Optional last argument is callback function; all
                non-empty arguments before it are concatenated to the
                LIST command.  (This *should* only be used for a pathname.)'''
        """
    def mlsd(self, path="", facts=[]):
        """
        '''List a directory in a standardized format by using MLSD
                command (RFC-3659). If path is omitted the current directory
                is assumed. "facts" is a list of strings representing the type
                of information desired (e.g. ["type", "size", "perm"]).

                Return a generator object yielding a tuple of two elements
                for every file found in path.
                First element is the file name, the second one is a dictionary
                including a variable number of "facts" depending on the server
                and whether "facts" argument has been provided.
                '''
        """
    def rename(self, fromname, toname):
        """
        '''Rename a file.'''
        """
    def delete(self, filename):
        """
        '''Delete a file.'''
        """
    def cwd(self, dirname):
        """
        '''Change to a directory.'''
        """
    def size(self, filename):
        """
        '''Retrieve the size of a file.'''
        """
    def mkd(self, dirname):
        """
        '''Make a directory, return its full pathname.'''
        """
    def rmd(self, dirname):
        """
        '''Remove a directory.'''
        """
    def pwd(self):
        """
        '''Return current working directory.'''
        """
    def quit(self):
        """
        '''Quit, and close the connection.'''
        """
    def close(self):
        """
        '''Close the connection without assuming anything about it.'''
        """
    def FTP_TLS(FTP):
    """
    '''A FTP subclass which adds TLS support to FTP as described
            in RFC-4217.

            Connect as usual to port 21 implicitly securing the FTP control
            connection before authenticating.

            Securing the data connection requires user to explicitly ask
            for it by calling prot_p() method.

            Usage example:
            >>> from ftplib import FTP_TLS
            >>> ftps = FTP_TLS('ftp.python.org')
            >>> ftps.login()  # login anonymously previously securing control channel
            '230 Guest login ok, access restrictions apply.'
            >>> ftps.prot_p()  # switch to secure data connection
            '200 Protection level set to P'
            >>> ftps.retrlines('LIST')  # list directory content securely
            total 9
            drwxr-xr-x   8 root     wheel        1024 Jan  3  1994 .
            drwxr-xr-x   8 root     wheel        1024 Jan  3  1994 ..
            drwxr-xr-x   2 root     wheel        1024 Jan  3  1994 bin
            drwxr-xr-x   2 root     wheel        1024 Jan  3  1994 etc
            d-wxrwxr-x   2 ftp      wheel        1024 Sep  5 13:43 incoming
            drwxr-xr-x   2 root     wheel        1024 Nov 17  1993 lib
            drwxr-xr-x   6 1094     wheel        1024 Sep 13 19:07 pub
            drwxr-xr-x   3 root     wheel        1024 Jan  3  1994 usr
            -rw-r--r--   1 root     root          312 Aug  1  1994 welcome.msg
            '226 Transfer complete.'
            >>> ftps.quit()
            '221 Goodbye.'
            >>>
            '''
    """
2021-03-02 20:53:39,080 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,080 : INFO : tokenize_signature : --> do i ever get here?
        def __init__(self, host='', user='', passwd='', acct='', keyfile=None,
                     certfile=None, context=None,
                     timeout=_GLOBAL_DEFAULT_TIMEOUT, source_address=None):
            """
            context and keyfile arguments are mutually 
            exclusive
            """
        def login(self, user='', passwd='', acct='', secure=True):
            """
            '''Set up secure control connection by using TLS/SSL.'''
            """
        def ccc(self):
            """
            '''Switch back to a clear-text control connection.'''
            """
        def prot_p(self):
            """
            '''Set up secure data connection.'''
            """
        def prot_c(self):
            """
            '''Set up clear text data connection.'''
            """
        def ntransfercmd(self, cmd, rest=None):
            """
             overridden as we can't pass MSG_OOB flag to sendall()

            """
def parse150(resp):
    """
    '''Parse the '150' response for a RETR request.
        Returns the expected transfer size or None; size is not guaranteed to
        be present in the 150 message.
        '''
    """
def parse227(resp):
    """
    '''Parse the '227' response for a PASV request.
        Raises error_proto if it does not contain '(h1,h2,h3,h4,p1,p2)'
        Return ('host.addr.as.numbers', port#) tuple.'''
    """
def parse229(resp, peer):
    """
    '''Parse the '229' response for an EPSV request.
        Raises error_proto if it does not contain '(|||port|)'
        Return ('host.addr.as.numbers', port#) tuple.'''
    """
def parse257(resp):
    """
    '''Parse the '257' response for a MKD or PWD request.
        This is a response to a MKD or PWD request: a directory name.
        Returns the directoryname in the 257 reply.'''
    """
def print_line(line):
    """
    '''Default retrlines callback to print a line.'''
    """
def ftpcp(source, sourcename, target, targetname = '', type = 'I'):
    """
    '''Copy file from one FTP-instance to another.'''
    """
def test():
    """
    '''Test program.
        Usage: ftp [-d] [-r[file]] host [-l[dir]] [-d[dir]] [-p] [file] ...

        -d dir
        -l list
        -p password
        '''
    """
