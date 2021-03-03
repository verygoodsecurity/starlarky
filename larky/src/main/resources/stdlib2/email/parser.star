def Parser:
    """
    Parser of RFC 2822 and MIME email messages.

            Creates an in-memory object tree representing the email message, which
            can then be manipulated and turned over to a Generator to return the
            textual representation of the message.

            The string must be formatted as a block of RFC 2822 headers and header
            continuation lines, optionally preceded by a `Unix-from' header.  The
            header block is terminated either by the end of the string or by a
            blank line.

            _class is the class to instantiate for new message objects when they
            must be created.  This class must have a constructor that can take
            zero arguments.  Default is Message.Message.

            The policy keyword specifies a policy object that controls a number of
            aspects of the parser's operation.  The default policy maintains
            backward compatibility.

        
    """
    def parse(self, fp, headersonly=False):
        """
        Create a message structure from the data in a file.

                Reads all the data from the file and returns the root of the message
                structure.  Optional headersonly is a flag specifying whether to stop
                parsing after reading the headers or not.  The default is False,
                meaning it parses the entire contents of the file.
        
        """
    def parsestr(self, text, headersonly=False):
        """
        Create a message structure from a string.

                Returns the root of the message structure.  Optional headersonly is a
                flag specifying whether to stop parsing after reading the headers or
                not.  The default is False, meaning it parses the entire contents of
                the file.
        
        """
def HeaderParser(Parser):
    """
    Parser of binary RFC 2822 and MIME email messages.

            Creates an in-memory object tree representing the email message, which
            can then be manipulated and turned over to a Generator to return the
            textual representation of the message.

            The input must be formatted as a block of RFC 2822 headers and header
            continuation lines, optionally preceded by a `Unix-from' header.  The
            header block is terminated either by the end of the input or by a
            blank line.

            _class is the class to instantiate for new message objects when they
            must be created.  This class must have a constructor that can take
            zero arguments.  Default is Message.Message.
        
    """
    def parse(self, fp, headersonly=False):
        """
        Create a message structure from the data in a binary file.

                Reads all the data from the file and returns the root of the message
                structure.  Optional headersonly is a flag specifying whether to stop
                parsing after reading the headers or not.  The default is False,
                meaning it parses the entire contents of the file.
        
        """
    def parsebytes(self, text, headersonly=False):
        """
        Create a message structure from a byte string.

                Returns the root of the message structure.  Optional headersonly is a
                flag specifying whether to stop parsing after reading the headers or
                not.  The default is False, meaning it parses the entire contents of
                the file.
        
        """
def BytesHeaderParser(BytesParser):
