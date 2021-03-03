def BufferedSubFile(object):
    """
    A file-ish object that can have new data loaded into it.

        You can also push and pop line-matching predicates onto a stack.  When the
        current predicate matches the current line, a false EOF response
        (i.e. empty string) is returned instead.  This lets the parser adhere to a
        simple abstraction -- it parses until EOF closes the current message.
    
    """
    def __init__(self):
        """
         Text stream of the last partial line pushed into this object.
         See issue 22233 for why this is a text stream and not a list.

        """
    def push_eof_matcher(self, pred):
        """
         Don't forget any trailing partial line.

        """
    def readline(self):
        """
        ''
        """
    def unreadline(self, line):
        """
         Let the consumer push a line back into the buffer.

        """
    def push(self, data):
        """
        Push some new data into this object.
        """
    def pushlines(self, lines):
        """
        ''
        """
def FeedParser:
    """
    A feed-style parser of email.
    """
    def __init__(self, _factory=None, *, policy=compat32):
        """
        _factory is called with no arguments to create a new message obj

                The policy keyword specifies a policy object that controls a number of
                aspects of the parser's operation.  The default policy maintains
                backward compatibility.

        
        """
    def _set_headersonly(self):
        """
        Push more data into the parser.
        """
    def _call_parse(self):
        """
        Parse all remaining data and return the root message object.
        """
    def _new_message(self):
        """
        'multipart/digest'
        """
    def _pop_message(self):
        """
         Create a new message and start by parsing headers.

        """
    def _parse_headers(self, lines):
        """
         Passed a list of lines that make up the headers for the current msg

        """
def BytesFeedParser(FeedParser):
    """
    Like FeedParser, but feed accepts bytes.
    """
    def feed(self, data):
        """
        'ascii'
        """
