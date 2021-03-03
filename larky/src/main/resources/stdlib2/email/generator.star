def Generator:
    """
    Generates output from a Message object tree.

        This basic generator writes the message to the given file object as plain
        text.
    
    """
2021-03-02 20:54:38,720 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, outfp, mangle_from_=None, maxheaderlen=None, *,
                 policy=None):
        """
        Create the generator for message flattening.

                outfp is the output file-like object for writing the message to.  It
                must have a write() method.

                Optional mangle_from_ is a flag that, when True (the default if policy
                is not set), escapes From_ lines in the body of the message by putting
                a `>' in front of them.

                Optional maxheaderlen specifies the longest length for a non-continued
                header.  When a header line is longer (in characters, with tabs
                expanded to 8 spaces) than maxheaderlen, the header will split as
                defined in the Header class.  Set maxheaderlen to zero to disable
                header wrapping.  The default is 78, as recommended (but not required)
                by RFC 2822.

                The policy keyword specifies a policy object that controls a number of
                aspects of the generator's operation.  If no policy is specified,
                the policy associated with the Message object passed to the
                flatten method is used.

        
        """
    def write(self, s):
        """
         Just delegate to the file object

        """
    def flatten(self, msg, unixfrom=False, linesep=None):
        """
        r"""Print the message object tree rooted at msg to the output file
                specified when the Generator instance was created.

                unixfrom is a flag that forces the printing of a Unix From_ delimiter
                before the first object in the message tree.  If the original message
                has no From_ delimiter, a `standard' one is crafted.  By default, this
                is False to inhibit the printing of any From_ delimiter.

                Note that for subobjects, no From_ line is printed.

                linesep specifies the characters used to indicate a new line in
                the output.  The default value is determined by the policy specified
                when the Generator instance was created or, if none was specified,
                from the policy associated with the msg.

        
        """
    def clone(self, fp):
        """
        Clone this generator with the exact same options.
        """
    def _new_buffer(self):
        """
         BytesGenerator overrides this to return BytesIO.

        """
    def _encode(self, s):
        """
         BytesGenerator overrides this to encode strings to bytes.

        """
    def _write_lines(self, lines):
        """
         We have to transform the line endings.

        """
    def _write(self, msg):
        """
         We can't write the headers yet because of the following scenario:
         say a multipart message includes the boundary string somewhere in
         its body.  We'd have to calculate the new boundary /before/ we write
         the headers so that we can write the correct Content-Type:
         parameter.

         The way we do this, so as to make the _handle_*() methods simpler,
         is to cache any subpart writes into a buffer.  The we write the
         headers and the buffer contents.  That way, subpart handlers can
         Do The Right Thing, and can still modify the Content-Type: header if
         necessary.

        """
    def _dispatch(self, msg):
        """
         Get the Content-Type: for the message, then try to dispatch to
         self._handle_<maintype>_<subtype>().  If there's no handler for the
         full MIME type, then dispatch to self._handle_<maintype>().  If
         that's missing too, then dispatch to self._writeBody().

        """
    def _write_headers(self, msg):
        """
         A blank line always separates headers from body

        """
    def _handle_text(self, msg):
        """
        'string payload expected: %s'
        """
    def _handle_multipart(self, msg):
        """
         The trick here is to write out each part separately, merge them all
         together, and then make sure that the boundary we've chosen isn't
         present in the payload.

        """
    def _handle_multipart_signed(self, msg):
        """
         The contents of signed parts has to stay unmodified in order to keep
         the signature intact per RFC1847 2.1, so we disable header wrapping.
         RDM: This isn't enough to completely preserve the part, but it helps.

        """
    def _handle_message_delivery_status(self, msg):
        """
         We can't just write the headers directly to self's file object
         because this will leave an extra newline between the last header
         block and the boundary.  Sigh.

        """
    def _handle_message(self, msg):
        """
         The payload of a message/rfc822 part should be a multipart sequence
         of length 1.  The zeroth element of the list should be the Message
         object for the subpart.  Extract that object, stringify it, and
         write it out.
         Except, it turns out, when it's a string instead, which happens when
         and only when HeaderParser is used on a message of mime type
         message/rfc822.  Such messages are generated by, for example,
         Groupwise when forwarding unadorned messages.  (Issue 7970.)  So
         in that case we just emit the string body.

        """
    def _make_boundary(cls, text=None):
        """
         Craft a random boundary.  If text is given, ensure that the chosen
         boundary doesn't appear in the text.

        """
    def _compile_re(cls, s, flags):
        """
        Generates a bytes version of a Message object tree.

            Functionally identical to the base Generator except that the output is
            bytes and not string.  When surrogates were used in the input to encode
            bytes, these are decoded back to bytes for output.  If the policy has
            cte_type set to 7bit, then the message is transformed such that the
            non-ASCII bytes are properly content transfer encoded, using the charset
            unknown-8bit.

            The outfp object must accept bytes in its write method.
    
        """
    def write(self, s):
        """
        'ascii'
        """
    def _new_buffer(self):
        """
        'ascii'
        """
    def _write_headers(self, msg):
        """
         This is almost the same as the string version, except for handling
         strings with 8bit bytes.

        """
    def _handle_text(self, msg):
        """
         If the string has surrogates the original source was bytes, so
         just write it back out.

        """
    def _compile_re(cls, s, flags):
        """
        'ascii'
        """
def DecodedGenerator(Generator):
    """
    Generates a text representation of a message.

        Like the Generator base class, except that non-text parts are substituted
        with a format string representing the part.
    
    """
2021-03-02 20:54:38,729 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, outfp, mangle_from_=None, maxheaderlen=None, fmt=None, *,
                 policy=None):
        """
        Like Generator.__init__() except that an additional optional
                argument is allowed.

                Walks through all subparts of a message.  If the subpart is of main
                type `text', then it prints the decoded payload of the subpart.

                Otherwise, fmt is a format string that is used instead of the message
                payload.  fmt is expanded with the following keywords (in
                %(keyword)s format):

                type       : Full MIME type of the non-text part
                maintype   : Main MIME type of the non-text part
                subtype    : Sub-MIME type of the non-text part
                filename   : Filename of the non-text part
                description: Description associated with the non-text part
                encoding   : Content transfer encoding of the non-text part

                The default value for fmt is None, meaning

                [Non-text (%(type)s) part of message omitted, filename %(filename)s]
        
        """
    def _dispatch(self, msg):
        """
        'text'
        """
