def JSONDecodeError(ValueError):
    """
    Subclass of ValueError with the following additional properties:

        msg: The unformatted error message
        doc: The JSON document being parsed
        pos: The start index of doc where parsing failed
        lineno: The line corresponding to pos
        colno: The column corresponding to pos

    
    """
    def __init__(self, msg, doc, pos):
        """
        '\n'
        """
    def __reduce__(self):
        """
        '-Infinity'
        """
def _decode_uXXXX(s, pos):
    """
    'xX'
    """
2021-03-02 20:53:48,748 : INFO : tokenize_signature : --> do i ever get here?
def py_scanstring(s, end, strict=True,
        _b=BACKSLASH, _m=STRINGCHUNK.match):
    """
    Scan the string s for a JSON string. End is the index of the
        character in s after the quote that started the JSON string.
        Unescapes all valid JSON string escape sequences and raises ValueError
        on attempt to decode an invalid string. If strict is False then literal
        control characters are allowed in the string.

        Returns a tuple of the decoded string and the index of the character in s
        after the end quote.
    """
2021-03-02 20:53:48,750 : INFO : tokenize_signature : --> do i ever get here?
def JSONObject(s_and_end, strict, scan_once, object_hook, object_pairs_hook,
               memo=None, _w=WHITESPACE.match, _ws=WHITESPACE_STR):
    """
     Backwards compatibility

    """
def JSONArray(s_and_end, scan_once, _w=WHITESPACE.match, _ws=WHITESPACE_STR):
    """
     Look-ahead for trivial empty array

    """
def JSONDecoder(object):
    """
    Simple JSON <http://json.org> decoder

        Performs the following translations in decoding by default:

        +---------------+-------------------+
        | JSON          | Python            |
        +===============+===================+
        | object        | dict              |
        +---------------+-------------------+
        | array         | list              |
        +---------------+-------------------+
        | string        | str               |
        +---------------+-------------------+
        | number (int)  | int               |
        +---------------+-------------------+
        | number (real) | float             |
        +---------------+-------------------+
        | true          | True              |
        +---------------+-------------------+
        | false         | False             |
        +---------------+-------------------+
        | null          | None              |
        +---------------+-------------------+

        It also understands ``NaN``, ``Infinity``, and ``-Infinity`` as
        their corresponding ``float`` values, which is outside the JSON spec.

    
    """
2021-03-02 20:53:48,753 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:48,753 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, *, object_hook=None, parse_float=None,
            parse_int=None, parse_constant=None, strict=True,
            object_pairs_hook=None):
        """
        ``object_hook``, if specified, will be called with the result
                of every JSON object decoded and its return value will be used in
                place of the given ``dict``.  This can be used to provide custom
                deserializations (e.g. to support JSON-RPC class hinting).

                ``object_pairs_hook``, if specified will be called with the result of
                every JSON object decoded with an ordered list of pairs.  The return
                value of ``object_pairs_hook`` will be used instead of the ``dict``.
                This feature can be used to implement custom decoders.
                If ``object_hook`` is also defined, the ``object_pairs_hook`` takes
                priority.

                ``parse_float``, if specified, will be called with the string
                of every JSON float to be decoded. By default this is equivalent to
                float(num_str). This can be used to use another datatype or parser
                for JSON floats (e.g. decimal.Decimal).

                ``parse_int``, if specified, will be called with the string
                of every JSON int to be decoded. By default this is equivalent to
                int(num_str). This can be used to use another datatype or parser
                for JSON integers (e.g. float).

                ``parse_constant``, if specified, will be called with one of the
                following strings: -Infinity, Infinity, NaN.
                This can be used to raise an exception if invalid JSON numbers
                are encountered.

                If ``strict`` is false (true is the default), then control
                characters will be allowed inside strings.  Control characters in
                this context are those with character codes in the 0-31 range,
                including ``'\\t'`` (tab), ``'\\n'``, ``'\\r'`` and ``'\\0'``.
        
        """
    def decode(self, s, _w=WHITESPACE.match):
        """
        Return the Python representation of ``s`` (a ``str`` instance
                containing a JSON document).

        
        """
    def raw_decode(self, s, idx=0):
        """
        Decode a JSON document from ``s`` (a ``str`` beginning with
                a JSON document) and return a 2-tuple of the Python
                representation and the index in ``s`` where the document ended.

                This can be used to decode a JSON document from a string that may
                have extraneous data at the end.

        
        """
