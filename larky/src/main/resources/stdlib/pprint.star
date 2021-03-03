2021-03-02 20:53:45,750 : INFO : tokenize_signature : --> do i ever get here?
def pprint(object, stream=None, indent=1, width=80, depth=None, *,
           compact=False, sort_dicts=True):
    """
    Pretty-print a Python object to a stream [default is sys.stdout].
    """
2021-03-02 20:53:45,750 : INFO : tokenize_signature : --> do i ever get here?
def pformat(object, indent=1, width=80, depth=None, *,
            compact=False, sort_dicts=True):
    """
    Format a Python object into a pretty-printed representation.
    """
def pp(object, *args, sort_dicts=False, **kwargs):
    """
    Pretty-print a Python object
    """
def saferepr(object):
    """
    Version of repr() which can handle recursive data structures.
    """
def isreadable(object):
    """
    Determine if saferepr(object) is readable by eval().
    """
def isrecursive(object):
    """
    Determine if object requires a recursive representation.
    """
def _safe_key:
    """
    Helper function for key functions when sorting unorderable objects.

        The wrapped-object will fallback to a Py2.x style comparison for
        unorderable types (sorting first comparing the type name and then by
        the obj ids).  Does not work recursively, so dict.items() must have
        _safe_key applied to both the key and the value.

    
    """
    def __init__(self, obj):
        """
        Helper function for comparing 2-tuples
        """
def PrettyPrinter:
    """
    Handle pretty printing operations onto a stream using a set of
            configured parameters.

            indent
                Number of spaces to indent for each level of nesting.

            width
                Attempted maximum number of columns in the output.

            depth
                The maximum depth to print out nested structures.

            stream
                The desired output stream.  If omitted (or false), the standard
                output stream available at construction will be used.

            compact
                If true, several items will be combined in one line.

            sort_dicts
                If true, dict keys are sorted.

        
    """
    def pprint(self, object):
        """
        \n
        """
    def pformat(self, object):
        """
        '{'
        """
    def _pprint_ordered_dict(self, object, stream, indent, allowance, context, level):
        """
        '('
        """
    def _pprint_list(self, object, stream, indent, allowance, context, level):
        """
        '['
        """
    def _pprint_tuple(self, object, stream, indent, allowance, context, level):
        """
        '('
        """
    def _pprint_set(self, object, stream, indent, allowance, context, level):
        """
        '{'
        """
    def _pprint_str(self, object, stream, indent, allowance, context, level):
        """
         A list of alternating (non-space, space) strings

        """
    def _pprint_bytes(self, object, stream, indent, allowance, context, level):
        """
        '('
        """
    def _pprint_bytearray(self, object, stream, indent, allowance, context, level):
        """
        'bytearray('
        """
    def _pprint_mappingproxy(self, object, stream, indent, allowance, context, level):
        """
        'mappingproxy('
        """
2021-03-02 20:53:45,760 : INFO : tokenize_signature : --> do i ever get here?
    def _format_dict_items(self, items, stream, indent, allowance, context,
                           level):
        """
        ',\n'
        """
    def _format_items(self, items, stream, indent, allowance, context, level):
        """
        ' '
        """
    def _repr(self, object, context, level):
        """
        Format object for a specific context, returning a string
                and flags indicating whether the representation is 'readable'
                and whether the object represents a recursive construct.
        
        """
    def _pprint_default_dict(self, object, stream, indent, allowance, context, level):
        """
        '%s(%s,\n%s'
        """
    def _pprint_counter(self, object, stream, indent, allowance, context, level):
        """
        '({'
        """
    def _pprint_chain_map(self, object, stream, indent, allowance, context, level):
        """
        '('
        """
    def _pprint_deque(self, object, stream, indent, allowance, context, level):
        """
        '('
        """
    def _pprint_user_dict(self, object, stream, indent, allowance, context, level):
        """
         Return triple (repr_string, isreadable, isrecursive).


        """
def _safe_repr(object, context, maxlevels, level, sort_dicts):
    """
    __repr__
    """
def _recursion(object):
    """
    <Recursion on %s with id=%s>

    """
def _perfcheck(object=None):
    """
    string
    """
def _wrap_bytes_repr(object, width, allowance):
    """
    b''
    """
