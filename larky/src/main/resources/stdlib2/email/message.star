def _splitparam(param):
    """
     Split header parameters.  BAW: this may be too simple.  It isn't
     strictly RFC 2045 (section 5.1) compliant, but it catches most headers
     found in the wild.  We may eventually need a full fledged parser.
     RDM: we might have a Header here; for now just stringify it.

    """
def _formatparam(param, value=None, quote=True):
    """
    Convenience function to format and return a key=value pair.

        This will quote the value if needed or if quote is true.  If value is a
        three tuple (charset, language, value), it will be encoded according
        to RFC2231 rules.  If it contains non-ascii characters it will likewise
        be encoded according to RFC2231 rules, using the utf-8 charset and
        a null language.
    
    """
def _parseparam(s):
    """
     RDM This might be a Header, so for now stringify it.

    """
def _unquotevalue(value):
    """
     This is different than utils.collapse_rfc2231_value() because it doesn't
     try to convert the value to a unicode.  Message.get_param() and
     Message.get_params() are both currently defined to return the tuple in
     the face of RFC 2231 parameters.

    """
def Message:
    """
    Basic message object.

        A message object is defined as something that has a bunch of RFC 2822
        headers and a payload.  It may optionally have an envelope header
        (a.k.a. Unix-From or From_ header).  If the message is a container (i.e. a
        multipart or a message/rfc822), then the payload is a list of Message
        objects, otherwise it is a string.

        Message objects implement part of the `mapping' interface, which assumes
        there is exactly one occurrence of the header per message.  Some headers
        do in fact appear multiple times (e.g. Received) and for those headers,
        you must use the explicit API to set or get all the headers.  Not all of
        the mapping methods are implemented.
    
    """
    def __init__(self, policy=compat32):
        """
         Defaults for multipart messages

        """
    def __str__(self):
        """
        Return the entire formatted message as a string.
        
        """
    def as_string(self, unixfrom=False, maxheaderlen=0, policy=None):
        """
        Return the entire formatted message as a string.

                Optional 'unixfrom', when true, means include the Unix From_ envelope
                header.  For backward compatibility reasons, if maxheaderlen is
                not specified it defaults to 0, so you must override it explicitly
                if you want a different maxheaderlen.  'policy' is passed to the
                Generator instance used to serialize the mesasge; if it is not
                specified the policy associated with the message instance is used.

                If the message object contains binary data that is not encoded
                according to RFC standards, the non-compliant data will be replaced by
                unicode "unknown character" code points.
        
        """
    def __bytes__(self):
        """
        Return the entire formatted message as a bytes object.
        
        """
    def as_bytes(self, unixfrom=False, policy=None):
        """
        Return the entire formatted message as a bytes object.

                Optional 'unixfrom', when true, means include the Unix From_ envelope
                header.  'policy' is passed to the BytesGenerator instance used to
                serialize the message; if not specified the policy associated with
                the message instance is used.
        
        """
    def is_multipart(self):
        """
        Return True if the message consists of multiple parts.
        """
    def set_unixfrom(self, unixfrom):
        """

         Payload manipulation.


        """
    def attach(self, payload):
        """
        Add the given payload to the current payload.

                The current payload will always be a list of objects after this method
                is called.  If you want to set the payload to a scalar object, use
                set_payload() instead.
        
        """
    def get_payload(self, i=None, decode=False):
        """
        Return a reference to the payload.

                The payload will either be a list object or a string.  If you mutate
                the list object, you modify the message's payload in place.  Optional
                i returns that index into the payload.

                Optional decode is a flag indicating whether the payload should be
                decoded or not, according to the Content-Transfer-Encoding header
                (default is False).

                When True and the message is not a multipart, the payload will be
                decoded if this header's value is `quoted-printable' or `base64'.  If
                some other encoding is used, or the header is missing, or if the
                payload has bogus data (i.e. bogus base64 or uuencoded data), the
                payload is returned as-is.

                If the message is a multipart and the decode flag is True, then None
                is returned.
        
        """
    def set_payload(self, payload, charset=None):
        """
        Set the payload to the given value.

                Optional charset sets the message's default character set.  See
                set_charset() for details.
        
        """
    def set_charset(self, charset):
        """
        Set the charset of the payload to a given character set.

                charset can be a Charset instance, a string naming a character set, or
                None.  If it is a string it will be converted to a Charset instance.
                If charset is None, the charset parameter will be removed from the
                Content-Type field.  Anything else will generate a TypeError.

                The message will be assumed to be of type text/* encoded with
                charset.input_charset.  It will be converted to charset.output_charset
                and encoded properly, if needed, when generating the plain text
                representation of the message.  MIME headers (MIME-Version,
                Content-Type, Content-Transfer-Encoding) will be added as needed.
        
        """
    def get_charset(self):
        """
        Return the Charset instance associated with the message's payload.
        
        """
    def __len__(self):
        """
        Return the total number of headers, including duplicates.
        """
    def __getitem__(self, name):
        """
        Get a header value.

                Return None if the header is missing instead of raising an exception.

                Note that if the header appeared multiple times, exactly which
                occurrence gets returned is undefined.  Use get_all() to get all
                the values matching a header field name.
        
        """
    def __setitem__(self, name, val):
        """
        Set the value of a header.

                Note: this does not overwrite an existing header with the same field
                name.  Use __delitem__() first to delete any existing headers.
        
        """
    def __delitem__(self, name):
        """
        Delete all occurrences of a header, if present.

                Does not raise an exception if the header is missing.
        
        """
    def __contains__(self, name):
        """
        Return a list of all the message's header field names.

                These will be sorted in the order they appeared in the original
                message, or were added to the message, and may contain duplicates.
                Any fields deleted and re-inserted are always appended to the header
                list.
        
        """
    def values(self):
        """
        Return a list of all the message's header values.

                These will be sorted in the order they appeared in the original
                message, or were added to the message, and may contain duplicates.
                Any fields deleted and re-inserted are always appended to the header
                list.
        
        """
    def items(self):
        """
        Get all the message's header fields and values.

                These will be sorted in the order they appeared in the original
                message, or were added to the message, and may contain duplicates.
                Any fields deleted and re-inserted are always appended to the header
                list.
        
        """
    def get(self, name, failobj=None):
        """
        Get a header value.

                Like __getitem__() but return failobj instead of None when the field
                is missing.
        
        """
    def set_raw(self, name, value):
        """
        Store name and value in the model without modification.

                This is an "internal" API, intended only for use by a parser.
        
        """
    def raw_items(self):
        """
        Return the (name, value) header pairs without modification.

                This is an "internal" API, intended only for use by a generator.
        
        """
    def get_all(self, name, failobj=None):
        """
        Return a list of all the values for the named field.

                These will be sorted in the order they appeared in the original
                message, and may contain duplicates.  Any fields deleted and
                re-inserted are always appended to the header list.

                If no such fields exist, failobj is returned (defaults to None).
        
        """
    def add_header(self, _name, _value, **_params):
        """
        Extended header setting.

                name is the header field to add.  keyword arguments can be used to set
                additional parameters for the header field, with underscores converted
                to dashes.  Normally the parameter will be added as key="value" unless
                value is None, in which case only the key will be added.  If a
                parameter value contains non-ASCII characters it can be specified as a
                three-tuple of (charset, language, value), in which case it will be
                encoded according to RFC2231 rules.  Otherwise it will be encoded using
                the utf-8 charset and a language of ''.

                Examples:

                msg.add_header('content-disposition', 'attachment', filename='bud.gif')
                msg.add_header('content-disposition', 'attachment',
                               filename=('utf-8', '', Fußballer.ppt'))
                msg.add_header('content-disposition', 'attachment',
                               filename='Fußballer.ppt'))
        
        """
    def replace_header(self, _name, _value):
        """
        Replace a header.

                Replace the first matching header found in the message, retaining
                header order and case.  If no matching header was found, a KeyError is
                raised.
        
        """
    def get_content_type(self):
        """
        Return the message's content type.

                The returned string is coerced to lower case of the form
                `maintype/subtype'.  If there was no Content-Type header in the
                message, the default type as given by get_default_type() will be
                returned.  Since according to RFC 2045, messages always have a default
                type this will always return a value.

                RFC 2045 defines a message's default type to be text/plain unless it
                appears inside a multipart/digest container, in which case it would be
                message/rfc822.
        
        """
    def get_content_maintype(self):
        """
        Return the message's main content type.

                This is the `maintype' part of the string returned by
                get_content_type().
        
        """
    def get_content_subtype(self):
        """
        Returns the message's sub-content type.

                This is the `subtype' part of the string returned by
                get_content_type().
        
        """
    def get_default_type(self):
        """
        Return the `default' content type.

                Most messages have a default content type of text/plain, except for
                messages that are subparts of multipart/digest containers.  Such
                subparts have a default content type of message/rfc822.
        
        """
    def set_default_type(self, ctype):
        """
        Set the `default' content type.

                ctype should be either "text/plain" or "message/rfc822", although this
                is not enforced.  The default content type is not stored in the
                Content-Type header.
        
        """
    def _get_params_preserve(self, failobj, header):
        """
         Like get_params() but preserves the quoting of values.  BAW:
         should this be part of the public interface?

        """
    def get_params(self, failobj=None, header='content-type', unquote=True):
        """
        Return the message's Content-Type parameters, as a list.

                The elements of the returned list are 2-tuples of key/value pairs, as
                split on the `=' sign.  The left hand side of the `=' is the key,
                while the right hand side is the value.  If there is no `=' sign in
                the parameter the value is the empty string.  The value is as
                described in the get_param() method.

                Optional failobj is the object to return if there is no Content-Type
                header.  Optional header is the header to search instead of
                Content-Type.  If unquote is True, the value is unquoted.
        
        """
2021-03-02 20:54:38,194 : INFO : tokenize_signature : --> do i ever get here?
    def get_param(self, param, failobj=None, header='content-type',
                  unquote=True):
        """
        Return the parameter value if found in the Content-Type header.

                Optional failobj is the object to return if there is no Content-Type
                header, or the Content-Type header has no such parameter.  Optional
                header is the header to search instead of Content-Type.

                Parameter keys are always compared case insensitively.  The return
                value can either be a string, or a 3-tuple if the parameter was RFC
                2231 encoded.  When it's a 3-tuple, the elements of the value are of
                the form (CHARSET, LANGUAGE, VALUE).  Note that both CHARSET and
                LANGUAGE can be None, in which case you should consider VALUE to be
                encoded in the us-ascii charset.  You can usually ignore LANGUAGE.
                The parameter value (either the returned string, or the VALUE item in
                the 3-tuple) is always unquoted, unless unquote is set to False.

                If your application doesn't care whether the parameter was RFC 2231
                encoded, it can turn the return value into a string as follows:

                    rawparam = msg.get_param('foo')
                    param = email.utils.collapse_rfc2231_value(rawparam)

        
        """
2021-03-02 20:54:38,195 : INFO : tokenize_signature : --> do i ever get here?
    def set_param(self, param, value, header='Content-Type', requote=True,
                  charset=None, language='', replace=False):
        """
        Set a parameter in the Content-Type header.

                If the parameter already exists in the header, its value will be
                replaced with the new value.

                If header is Content-Type and has not yet been defined for this
                message, it will be set to "text/plain" and the new parameter and
                value will be appended as per RFC 2045.

                An alternate header can be specified in the header argument, and all
                parameters will be quoted as necessary unless requote is False.

                If charset is specified, the parameter will be encoded according to RFC
                2231.  Optional language specifies the RFC 2231 language, defaulting
                to the empty string.  Both charset and language should be strings.
        
        """
    def del_param(self, param, header='content-type', requote=True):
        """
        Remove the given parameter completely from the Content-Type header.

                The header will be re-written in place without the parameter or its
                value. All values will be quoted as necessary unless requote is
                False.  Optional header specifies an alternative to the Content-Type
                header.
        
        """
    def set_type(self, type, header='Content-Type', requote=True):
        """
        Set the main type and subtype for the Content-Type header.

                type must be a string in the form "maintype/subtype", otherwise a
                ValueError is raised.

                This method replaces the Content-Type header, keeping all the
                parameters in place.  If requote is False, this leaves the existing
                header's quoting as is.  Otherwise, the parameters will be quoted (the
                default).

                An alternative header can be specified in the header argument.  When
                the Content-Type header is set, we'll always also add a MIME-Version
                header.
        
        """
    def get_filename(self, failobj=None):
        """
        Return the filename associated with the payload if present.

                The filename is extracted from the Content-Disposition header's
                `filename' parameter, and it is unquoted.  If that header is missing
                the `filename' parameter, this method falls back to looking for the
                `name' parameter.
        
        """
    def get_boundary(self, failobj=None):
        """
        Return the boundary associated with the payload if present.

                The boundary is extracted from the Content-Type header's `boundary'
                parameter, and it is unquoted.
        
        """
    def set_boundary(self, boundary):
        """
        Set the boundary parameter in Content-Type to 'boundary'.

                This is subtly different than deleting the Content-Type header and
                adding a new one with a new boundary parameter via add_header().  The
                main difference is that using the set_boundary() method preserves the
                order of the Content-Type header in the original message.

                HeaderParseError is raised if the message has no Content-Type header.
        
        """
    def get_content_charset(self, failobj=None):
        """
        Return the charset parameter of the Content-Type header.

                The returned string is always coerced to lower case.  If there is no
                Content-Type header, or if that header has no charset parameter,
                failobj is returned.
        
        """
    def get_charsets(self, failobj=None):
        """
        Return a list containing the charset(s) used in this message.

                The returned list of items describes the Content-Type headers'
                charset parameter for this message and all the subparts in its
                payload.

                Each item will either be a string (the value of the charset parameter
                in the Content-Type header of that part) or the value of the
                'failobj' parameter (defaults to None), if the part does not have a
                main MIME type of "text", or the charset is not defined.

                The list will contain one string for each part of the message, plus
                one for the container message (i.e. self), so that a non-multipart
                message will still return a list of length 1.
        
        """
    def get_content_disposition(self):
        """
        Return the message's content-disposition if it exists, or None.

                The return values can be either 'inline', 'attachment' or None
                according to the rfc2183.
        
        """
def MIMEPart(Message):
    """
    Return the entire formatted message as a string.

            Optional 'unixfrom', when true, means include the Unix From_ envelope
            header.  maxheaderlen is retained for backward compatibility with the
            base Message class, but defaults to None, meaning that the policy value
            for max_line_length controls the header maximum length.  'policy' is
            passed to the Generator instance used to serialize the mesasge; if it
            is not specified the policy associated with the message instance is
            used.
        
    """
    def __str__(self):
        """
        'content-disposition'
        """
    def _find_body(self, part, preferencelist):
        """
        '/'
        """
    def get_body(self, preferencelist=('related', 'html', 'plain')):
        """
        Return best candidate mime part for display as 'body' of message.

                Do a depth first search, starting with self, looking for the first part
                matching each of the items in preferencelist, and return the part
                corresponding to the first item that has a match, or None if no items
                have a match.  If 'related' is not included in preferencelist, consider
                the root part of any multipart/related encountered as a candidate
                match.  Ignore parts with 'Content-Disposition: attachment'.
        
        """
    def iter_attachments(self):
        """
        Return an iterator over the non-main parts of a multipart.

                Skip the first of each occurrence of text/plain, text/html,
                multipart/related, or multipart/alternative in the multipart (unless
                they have a 'Content-Disposition: attachment' header) and include all
                remaining subparts in the returned iterator.  When applied to a
                multipart/related, return all parts except the root part.  Return an
                empty iterator when applied to a multipart/alternative or a
                non-multipart.
        
        """
    def iter_parts(self):
        """
        Return an iterator over all immediate subparts of a multipart.

                Return an empty iterator for a non-multipart.
        
        """
    def get_content(self, *args, content_manager=None, **kw):
        """
        'multipart'
        """
    def make_related(self, boundary=None):
        """
        'related'
        """
    def make_alternative(self, boundary=None):
        """
        'alternative'
        """
    def make_mixed(self, boundary=None):
        """
        'mixed'
        """
    def _add_multipart(self, _subtype, *args, _disp=None, **kw):
        """
        'multipart'
        """
    def add_related(self, *args, **kw):
        """
        'related'
        """
    def add_alternative(self, *args, **kw):
        """
        'alternative'
        """
    def add_attachment(self, *args, **kw):
        """
        'mixed'
        """
    def clear(self):
        """
        'content-'
        """
def EmailMessage(MIMEPart):
    """
    'MIME-Version'
    """
