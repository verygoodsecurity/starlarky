def Telnet:
    """
    Telnet interface class.

        An instance of this class represents a connection to a telnet
        server.  The instance is initially not connected; the open()
        method must be used to establish a connection.  Alternatively, the
        host name and optional port number can be passed to the
        constructor, too.

        Don't try to reopen an already connected instance.

        This class has many read_*() methods.  Note that some of them
        raise EOFError when the end of the connection is read, because
        they can return an empty string for other reasons.  See the
        individual doc strings.

        read_until(expected, [timeout])
            Read until the expected string has been seen, or a timeout is
            hit (default is no timeout); may block.

        read_all()
            Read all data until EOF; may block.

        read_some()
            Read at least one byte or EOF; may block.

        read_very_eager()
            Read all data available already queued or on the socket,
            without blocking.

        read_eager()
            Read either data already queued or some data available on the
            socket, without blocking.

        read_lazy()
            Read all data in the raw queue (processing it first), without
            doing any socket I/O.

        read_very_lazy()
            Reads all data in the cooked queue, without doing any socket
            I/O.

        read_sb_data()
            Reads available data between SB ... SE sequence. Don't block.

        set_option_negotiation_callback(callback)
            Each time a telnet option is read on the input flow, this callback
            (if set) is called with the following parameters :
            callback(telnet socket, command, option)
                option will be chr(0) when there is no option.
            No other action is done afterwards by telnetlib.

    
    """
2021-03-02 20:53:49,623 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, host=None, port=0,
                 timeout=socket._GLOBAL_DEFAULT_TIMEOUT):
        """
        Constructor.

                When called without arguments, create an unconnected instance.
                With a hostname argument, it connects the instance; port number
                and timeout are optional.
        
        """
    def open(self, host, port=0, timeout=socket._GLOBAL_DEFAULT_TIMEOUT):
        """
        Connect to a host.

                The optional second argument is the port number, which
                defaults to the standard telnet port (23).

                Don't try to reopen an already connected instance.
        
        """
    def __del__(self):
        """
        Destructor -- close the connection.
        """
    def msg(self, msg, *args):
        """
        Print a debug message, when the debug level is > 0.

                If extra arguments are present, they are substituted in the
                message using the standard string formatting operator.

        
        """
    def set_debuglevel(self, debuglevel):
        """
        Set the debug level.

                The higher it is, the more debug output you get (on sys.stdout).

        
        """
    def close(self):
        """
        Close the connection.
        """
    def get_socket(self):
        """
        Return the socket object used internally.
        """
    def fileno(self):
        """
        Return the fileno() of the socket object used internally.
        """
    def write(self, buffer):
        """
        Write a string to the socket, doubling any IAC characters.

                Can block if the connection is blocked.  May raise
                OSError if the connection is closed.

        
        """
    def read_until(self, match, timeout=None):
        """
        Read until a given string is encountered or until timeout.

                When no match is found, return whatever is available instead,
                possibly the empty string.  Raise EOFError if the connection
                is closed and no cooked data is available.

        
        """
    def read_all(self):
        """
        Read all data until EOF; block until connection closed.
        """
    def read_some(self):
        """
        Read at least one byte of cooked data unless EOF is hit.

                Return b'' if EOF is hit.  Block if no data is immediately
                available.

        
        """
    def read_very_eager(self):
        """
        Read everything that's possible without blocking in I/O (eager).

                Raise EOFError if connection closed and no cooked data
                available.  Return b'' if no cooked data available otherwise.
                Don't block unless in the midst of an IAC sequence.

        
        """
    def read_eager(self):
        """
        Read readily available data.

                Raise EOFError if connection closed and no cooked data
                available.  Return b'' if no cooked data available otherwise.
                Don't block unless in the midst of an IAC sequence.

        
        """
    def read_lazy(self):
        """
        Process and return data that's already in the queues (lazy).

                Raise EOFError if connection closed and no data available.
                Return b'' if no cooked data available otherwise.  Don't block
                unless in the midst of an IAC sequence.

        
        """
    def read_very_lazy(self):
        """
        Return any data available in the cooked queue (very lazy).

                Raise EOFError if connection closed and no data available.
                Return b'' if no cooked data available otherwise.  Don't block.

        
        """
    def read_sb_data(self):
        """
        Return any data available in the SB ... SE queue.

                Return b'' if no SB ... SE available. Should only be called
                after seeing a SB or SE command. When a new SB command is
                found, old unread SB data will be discarded. Don't block.

        
        """
    def set_option_negotiation_callback(self, callback):
        """
        Provide a callback function called after each receipt of a telnet option.
        """
    def process_rawq(self):
        """
        Transfer from raw queue to cooked queue.

                Set self.eof when connection is closed.  Don't block unless in
                the midst of an IAC sequence.

        
        """
    def rawq_getchar(self):
        """
        Get next char from raw queue.

                Block if no data is immediately available.  Raise EOFError
                when connection is closed.

        
        """
    def fill_rawq(self):
        """
        Fill raw queue from exactly one recv() system call.

                Block if no data is immediately available.  Set self.eof when
                connection is closed.

        
        """
    def sock_avail(self):
        """
        Test whether data is available on the socket.
        """
    def interact(self):
        """
        Interaction function, emulates a very dumb telnet client.
        """
    def mt_interact(self):
        """
        Multithreaded version of interact().
        """
    def listener(self):
        """
        Helper for mt_interact() -- this executes in the other thread.
        """
    def expect(self, list, timeout=None):
        """
        Read until one from a list of a regular expressions matches.

                The first argument is a list of regular expressions, either
                compiled (re.Pattern instances) or uncompiled (strings).
                The optional second argument is a timeout, in seconds; default
                is no timeout.

                Return a tuple of three items: the index in the list of the
                first regular expression that matches; the re.Match object
                returned; and the text read up till and including the match.

                If EOF is read and no text was read, raise EOFError.
                Otherwise, when nothing matches, return (-1, None, text) where
                text is the text received so far (may be the empty string if a
                timeout happened).

                If a regular expression ends with a greedy match (e.g. '.*')
                or if more than one expression can match the same input, the
                results are undeterministic, and may depend on the I/O timing.

        
        """
    def __enter__(self):
        """
        Test program for telnetlib.

            Usage: python telnetlib.py [-d] ... [host [port]]

            Default host is localhost; default port is 23.

    
        """
