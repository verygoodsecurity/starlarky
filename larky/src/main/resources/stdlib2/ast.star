2021-03-02 20:53:55,947 : INFO : tokenize_signature : --> do i ever get here?
def parse(source, filename='<unknown>', mode='exec', *,
          type_comments=False, feature_version=None):
    """

        Parse the source into an AST node.
        Equivalent to compile(source, filename, mode, PyCF_ONLY_AST).
        Pass type_comments=True to get back type comments where the syntax allows.
    
    """
def literal_eval(node_or_string):
    """

        Safely evaluate an expression node or a string containing a Python
        expression.  The string or node provided may only consist of the following
        Python literal structures: strings, bytes, numbers, tuples, lists, dicts,
        sets, booleans, and None.
    
    """
    def _raise_malformed_node(node):
        """
        f'malformed node or string: {node!r}'
        """
    def _convert_num(node):
        """

            Return a formatted dump of the tree in node.  This is mainly useful for
            debugging purposes.  If annotate_fields is true (by default),
            the returned string will show the names and the values for fields.
            If annotate_fields is false, the result string will be more compact by
            omitting unambiguous field names.  Attributes such as line
            numbers and column offsets are not dumped by default.  If this is wanted,
            include_attributes can be set to true.
    
        """
    def _format(node):
        """
        '%s=%s'
        """
def copy_location(new_node, old_node):
    """

        Copy source location (`lineno`, `col_offset`, `end_lineno`, and `end_col_offset`
        attributes) from *old_node* to *new_node* if possible, and return *new_node*.
    
    """
def fix_missing_locations(node):
    """

        When you compile a node tree with compile(), the compiler expects lineno and
        col_offset attributes for every node that supports them.  This is rather
        tedious to fill in for generated nodes, so this helper adds these attributes
        recursively where not already set, by setting them to the values of the
        parent node.  It works recursively starting at *node*.
    
    """
    def _fix(node, lineno, col_offset, end_lineno, end_col_offset):
        """
        'lineno'
        """
def increment_lineno(node, n=1):
    """

        Increment the line number and end line number of each node in the tree
        starting at *node* by *n*. This is useful to "move code" to a different
        location in a file.
    
    """
def iter_fields(node):
    """

        Yield a tuple of ``(fieldname, value)`` for each field in ``node._fields``
        that is present on *node*.
    
    """
def iter_child_nodes(node):
    """

        Yield all direct child nodes of *node*, that is, all fields that are nodes
        and all items of fields that are lists of nodes.
    
    """
def get_docstring(node, clean=True):
    """

        Return the docstring for the given node or None if no docstring can
        be found.  If the node provided does not have docstrings a TypeError
        will be raised.

        If *clean* is `True`, all tabs are expanded to spaces and any whitespace
        that can be uniformly removed from the second line onwards is removed.
    
    """
def _splitlines_no_ff(source):
    """
    Split a string into lines ignoring form feed and other chars.

        This mimics how the Python parser splits source code.
    
    """
def _pad_whitespace(source):
    """
    Replace all chars except '\f\t' in a line with spaces.
    """
def get_source_segment(source, node, *, padded=False):
    """
    Get source code segment of the *source* that generated *node*.

        If some location information (`lineno`, `end_lineno`, `col_offset`,
        or `end_col_offset`) is missing, return None.

        If *padded* is `True`, the first line of a multi-line statement will
        be padded with spaces to match its original position.
    
    """
def walk(node):
    """

        Recursively yield all descendant nodes in the tree starting at *node*
        (including *node* itself), in no specified order.  This is useful if you
        only want to modify nodes in place and don't care about the context.
    
    """
def NodeVisitor(object):
    """

        A node visitor base class that walks the abstract syntax tree and calls a
        visitor function for every node found.  This function may return a value
        which is forwarded by the `visit` method.

        This class is meant to be subclassed, with the subclass adding visitor
        methods.

        Per default the visitor functions for the nodes are ``'visit_'`` +
        class name of the node.  So a `TryFinally` node visit function would
        be `visit_TryFinally`.  This behavior can be changed by overriding
        the `visit` method.  If no visitor function exists for a node
        (return value `None`) the `generic_visit` visitor is used instead.

        Don't use the `NodeVisitor` if you want to apply changes to nodes during
        traversing.  For this a special visitor exists (`NodeTransformer`) that
        allows modifications.
    
    """
    def visit(self, node):
        """
        Visit a node.
        """
    def generic_visit(self, node):
        """
        Called if no explicit visitor function exists for a node.
        """
    def visit_Constant(self, node):
        """
        'visit_'
        """
def NodeTransformer(NodeVisitor):
    """

        A :class:`NodeVisitor` subclass that walks the abstract syntax tree and
        allows modification of nodes.

        The `NodeTransformer` will walk the AST and use the return value of the
        visitor methods to replace or remove the old node.  If the return value of
        the visitor method is ``None``, the node will be removed from its location,
        otherwise it is replaced with the return value.  The return value may be the
        original node in which case no replacement takes place.

        Here is an example transformer that rewrites all occurrences of name lookups
        (``foo``) to ``data['foo']``::

           class RewriteName(NodeTransformer):

               def visit_Name(self, node):
                   return Subscript(
                       value=Name(id='data', ctx=Load()),
                       slice=Index(value=Str(s=node.id)),
                       ctx=node.ctx
                   )

        Keep in mind that if the node you're operating on has child nodes you must
        either transform the child nodes yourself or call the :meth:`generic_visit`
        method for the node first.

        For nodes that were part of a collection of statements (that applies to all
        statement nodes), the visitor may also return a list of nodes rather than
        just a single node.

        Usually you use the transformer like this::

           node = YourTransformer().visit(node)
    
    """
    def generic_visit(self, node):
        """
         The following code is for backward compatibility.
         It will be removed in future.


        """
def _getter(self):
    """
     arbitrary keyword arguments are accepted

    """
def Num(Constant, metadef=_ABC):
    """
    'n'
    """
def Str(Constant, metadef=_ABC):
    """
    's'
    """
def Bytes(Constant, metadef=_ABC):
    """
    's'
    """
def NameConstant(Constant, metadef=_ABC):
    """
    'NameConstant'
    """
