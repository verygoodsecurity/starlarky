def EmailPolicy(Policy):
    """
    +
        PROVISIONAL

        The API extensions enabled by this policy are currently provisional.
        Refer to the documentation for details.

        This policy adds new header parsing and folding algorithms.  Instead of
        simple strings, headers are custom objects with custom attributes
        depending on the type of the field.  The folding algorithm fully
        implements RFCs 2047 and 5322.

        In addition to the settable attributes listed above that apply to
        all Policies, this policy adds the following additional attributes:

        utf8                -- if False (the default) message headers will be
                               serialized as ASCII, using encoded words to encode
                               any non-ASCII characters in the source strings.  If
                               True, the message headers will be serialized using
                               utf8 and will not contain encoded words (see RFC
                               6532 for more on this serialization format).

        refold_source       -- if the value for a header in the Message object
                               came from the parsing of some source, this attribute
                               indicates whether or not a generator should refold
                               that value when transforming the message back into
                               stream form.  The possible values are:

                               none  -- all source values use original folding
                               long  -- source values that have any line that is
                                        longer than max_line_length will be
                                        refolded
                               all  -- all values are refolded.

                               The default is 'long'.

        header_factory      -- a callable that takes two arguments, 'name' and
                               'value', where 'name' is a header field name and
                               'value' is an unfolded header field value, and
                               returns a string-like object that represents that
                               header.  A default header_factory is provided that
                               understands some of the RFC5322 header field types.
                               (Currently address fields and date fields have
                               special treatment, while all other fields are
                               treated as unstructured.  This list will be
                               completed before the extension is marked stable.)

        content_manager     -- an object with at least two methods: get_content
                               and set_content.  When the get_content or
                               set_content method of a Message object is called,
                               it calls the corresponding method of this object,
                               passing it the message object as its first argument,
                               and any arguments or keywords that were passed to
                               it as additional arguments.  The default
                               content_manager is
                               :data:`~email.contentmanager.raw_data_manager`.

    
    """
    def __init__(self, **kw):
        """
         Ensure that each new instance gets a unique header factory
         (as opposed to clones, which share the factory).

        """
    def header_max_count(self, name):
        """
        +
                The implementation for this class returns the max_count attribute from
                the specialized header class that would be used to construct a header
                of type 'name'.
        
        """
    def header_source_parse(self, sourcelines):
        """
        +
                The name is parsed as everything up to the ':' and returned unmodified.
                The value is determined by stripping leading whitespace off the
                remainder of the first line, joining all subsequent lines together, and
                stripping any trailing carriage return or linefeed characters.  (This
                is the same as Compat32).

        
        """
    def header_store_parse(self, name, value):
        """
        +
                The name is returned unchanged.  If the input value has a 'name'
                attribute and it matches the name ignoring case, the value is returned
                unchanged.  Otherwise the name and value are passed to header_factory
                method, and the resulting custom header object is returned as the
                value.  In this case a ValueError is raised if the input value contains
                CR or LF characters.

        
        """
    def header_fetch_parse(self, name, value):
        """
        +
                If the value has a 'name' attribute, it is returned to unmodified.
                Otherwise the name and the value with any linesep characters removed
                are passed to the header_factory method, and the resulting custom
                header object is returned.  Any surrogateescaped bytes get turned
                into the unicode unknown-character glyph.

        
        """
    def fold(self, name, value):
        """
        +
                Header folding is controlled by the refold_source policy setting.  A
                value is considered to be a 'source value' if and only if it does not
                have a 'name' attribute (having a 'name' attribute means it is a header
                object of some sort).  If a source value needs to be refolded according
                to the policy, it is converted into a custom header object by passing
                the name and the value with any linesep characters removed to the
                header_factory method.  Folding of a custom header object is done by
                calling its fold method with the current policy.

                Source values are split into lines using splitlines.  If the value is
                not to be refolded, the lines are rejoined using the linesep from the
                policy and returned.  The exception is lines containing non-ascii
                binary data.  In that case the value is refolded regardless of the
                refold_source setting, which causes the binary data to be CTE encoded
                using the unknown-8bit charset.

        
        """
    def fold_binary(self, name, value):
        """
        +
                The same as fold if cte_type is 7bit, except that the returned value is
                bytes.

                If cte_type is 8bit, non-ASCII binary data is converted back into
                bytes.  Headers with binary data are not refolded, regardless of the
                refold_header setting, since there is no way to know whether the binary
                data consists of single byte characters or multibyte characters.

                If utf8 is true, headers are encoded to utf8, otherwise to ascii with
                non-ASCII unicode rendered as encoded words.

        
        """
    def _fold(self, name, value, refold_binary=False):
        """
        'name'
        """
