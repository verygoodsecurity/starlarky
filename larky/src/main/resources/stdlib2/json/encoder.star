def py_encode_basestring(s):
    """
    Return a JSON representation of a Python string

    
    """
    def replace(match):
        """
        '"'
        """
def py_encode_basestring_ascii(s):
    """
    Return an ASCII-only JSON representation of a Python string

    
    """
    def replace(match):
        """
        '\\u{0:04x}'
        """
def JSONEncoder(object):
    """
    Extensible JSON <http://json.org> encoder for Python data structures.

        Supports the following objects and types by default:

        +-------------------+---------------+
        | Python            | JSON          |
        +===================+===============+
        | dict              | object        |
        +-------------------+---------------+
        | list, tuple       | array         |
        +-------------------+---------------+
        | str               | string        |
        +-------------------+---------------+
        | int, float        | number        |
        +-------------------+---------------+
        | True              | true          |
        +-------------------+---------------+
        | False             | false         |
        +-------------------+---------------+
        | None              | null          |
        +-------------------+---------------+

        To extend this to recognize other objects, subclass and implement a
        ``.default()`` method with another method that returns a serializable
        object for ``o`` if possible, otherwise it should call the superclass
        implementation (to raise ``TypeError``).

    
    """
2021-03-02 20:53:49,271 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,272 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, *, skipkeys=False, ensure_ascii=True,
            check_circular=True, allow_nan=True, sort_keys=False,
            indent=None, separators=None, default=None):
        """
        Constructor for JSONEncoder, with sensible defaults.

                If skipkeys is false, then it is a TypeError to attempt
                encoding of keys that are not str, int, float or None.  If
                skipkeys is True, such items are simply skipped.

                If ensure_ascii is true, the output is guaranteed to be str
                objects with all incoming non-ASCII characters escaped.  If
                ensure_ascii is false, the output can contain non-ASCII characters.

                If check_circular is true, then lists, dicts, and custom encoded
                objects will be checked for circular references during encoding to
                prevent an infinite recursion (which would cause an OverflowError).
                Otherwise, no such check takes place.

                If allow_nan is true, then NaN, Infinity, and -Infinity will be
                encoded as such.  This behavior is not JSON specification compliant,
                but is consistent with most JavaScript based encoders and decoders.
                Otherwise, it will be a ValueError to encode such floats.

                If sort_keys is true, then the output of dictionaries will be
                sorted by key; this is useful for regression tests to ensure
                that JSON serializations can be compared on a day-to-day basis.

                If indent is a non-negative integer, then JSON array
                elements and object members will be pretty-printed with that
                indent level.  An indent level of 0 will only insert newlines.
                None is the most compact representation.

                If specified, separators should be an (item_separator, key_separator)
                tuple.  The default is (', ', ': ') if *indent* is ``None`` and
                (',', ': ') otherwise.  To get the most compact JSON representation,
                you should specify (',', ':') to eliminate whitespace.

                If specified, default is a function that gets called for objects
                that can't otherwise be serialized.  It should return a JSON encodable
                version of the object or raise a ``TypeError``.

        
        """
    def default(self, o):
        """
        Implement this method in a subclass such that it returns
                a serializable object for ``o``, or calls the base implementation
                (to raise a ``TypeError``).

                For example, to support arbitrary iterators, you could
                implement default like this::

                    def default(self, o):
                        try:
                            iterable = iter(o)
                        except TypeError:
                            pass
                        else:
                            return list(iterable)
                        # Let the base class default method raise the TypeError
                        return JSONEncoder.default(self, o)

        
        """
    def encode(self, o):
        """
        Return a JSON string representation of a Python data structure.

                >>> from json.encoder import JSONEncoder
                >>> JSONEncoder().encode({"foo": ["bar", "baz"]})
                '{"foo": ["bar", "baz"]}'

        
        """
    def iterencode(self, o, _one_shot=False):
        """
        Encode the given object and yield each string
                representation as available.

                For example::

                    for chunk in JSONEncoder().iterencode(bigobject):
                        mysocket.write(chunk)

        
        """
2021-03-02 20:53:49,273 : INFO : tokenize_signature : --> do i ever get here?
        def floatstr(o, allow_nan=self.allow_nan,
                _repr=float.__repr__, _inf=INFINITY, _neginf=-INFINITY):
            """
             Check for specials.  Note that this type of test is processor
             and/or platform-specific, so do tests which don't depend on the
             internals.


            """
2021-03-02 20:53:49,274 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,274 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,274 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,275 : INFO : tokenize_signature : --> do i ever get here?
def _make_iterencode(markers, _default, _encoder, _indent, _floatstr,
        _key_separator, _item_separator, _sort_keys, _skipkeys, _one_shot,
        ## HACK: hand-optimized bytecode; turn globals into locals
        ValueError=ValueError,
        dict=dict,
        float=float,
        id=id,
        int=int,
        isinstance=isinstance,
        list=list,
        str=str,
        tuple=tuple,
        _intstr=int.__repr__,
    ):
    """
    ' '
    """
    def _iterencode_list(lst, _current_indent_level):
        """
        '[]'
        """
    def _iterencode_dict(dct, _current_indent_level):
        """
        '{}'
        """
    def _iterencode(o, _current_indent_level):
        """
        'null'
        """
