def type_repr(type_num):
    """
     printing tokens is possible but not as useful
     from .pgen2 import token // token.__dict__.items():

    """
def Base(object):
    """

        Abstract base class for Node and Leaf.

        This provides some default functionality and boilerplate using the
        template pattern.

        A node may be a subnode of at most one parent.
    
    """
    def __new__(cls, *args, **kwds):
        """
        Constructor that prevents Base from being instantiated.
        """
    def __eq__(self, other):
        """

                Compare two nodes for equality.

                This calls the method _eq().
        
        """
    def _eq(self, other):
        """

                Compare two nodes for equality.

                This is called by __eq__ and __ne__.  It is only called if the two nodes
                have the same type.  This must be implemented by the concrete subclass.
                Nodes should be considered equal if they have the same structure,
                ignoring the prefix string and other context information.
        
        """
    def clone(self):
        """

                Return a cloned (deep) copy of self.

                This must be implemented by the concrete subclass.
        
        """
    def post_order(self):
        """

                Return a post-order iterator for the tree.

                This must be implemented by the concrete subclass.
        
        """
    def pre_order(self):
        """

                Return a pre-order iterator for the tree.

                This must be implemented by the concrete subclass.
        
        """
    def replace(self, new):
        """
        Replace this node with a new one in the parent.
        """
    def get_lineno(self):
        """
        Return the line number which generated the invocant node.
        """
    def changed(self):
        """

                Remove the node from the tree. Returns the position of the node in its
                parent's children before it was removed.
        
        """
    def next_sibling(self):
        """

                The node immediately following the invocant in their parent's children
                list. If the invocant does not have a next sibling, it is None
        
        """
    def prev_sibling(self):
        """

                The node immediately preceding the invocant in their parent's children
                list. If the invocant does not have a previous sibling, it is None.
        
        """
    def leaves(self):
        """

                Return the string immediately following the invocant node. This is
                effectively equivalent to node.next_sibling.prefix
        
        """
        def __str__(self):
            """
            ascii
            """
def Node(Base):
    """
    Concrete implementation for interior nodes.
    """
2021-03-02 20:54:15,867 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:15,867 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:15,867 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,type, children,
                 context=None,
                 prefix=None,
                 fixers_applied=None):
        """

                Initializer.

                Takes a type constant (a symbol number >= 256), a sequence of
                child nodes, and an optional context keyword argument.

                As a side effect, the parent pointers of the children are updated.
        
        """
    def __repr__(self):
        """
        Return a canonical string representation.
        """
    def __unicode__(self):
        """

                Return a pretty string representation.

                This reproduces the input source exactly.
        
        """
    def _eq(self, other):
        """
        Compare two nodes for equality.
        """
    def clone(self):
        """
        Return a cloned (deep) copy of self.
        """
    def post_order(self):
        """
        Return a post-order iterator for the tree.
        """
    def pre_order(self):
        """
        Return a pre-order iterator for the tree.
        """
    def prefix(self):
        """

                The whitespace and comments preceding this node in the input.
        
        """
    def prefix(self, prefix):
        """

                Equivalent to 'node.children[i] = child'. This method also sets the
                child's parent attribute appropriately.
        
        """
    def insert_child(self, i, child):
        """

                Equivalent to 'node.children.insert(i, child)'. This method also sets
                the child's parent attribute appropriately.
        
        """
    def append_child(self, child):
        """

                Equivalent to 'node.children.append(child)'. This method also sets the
                child's parent attribute appropriately.
        
        """
def Leaf(Base):
    """
    Concrete implementation for leaf nodes.
    """
2021-03-02 20:54:15,870 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:15,870 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:15,870 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, type, value,
                 context=None,
                 prefix=None,
                 fixers_applied=[]):
        """

                Initializer.

                Takes a type constant (a token number < 256), a string value, and an
                optional context keyword argument.
        
        """
    def __repr__(self):
        """
        Return a canonical string representation.
        """
    def __unicode__(self):
        """

                Return a pretty string representation.

                This reproduces the input source exactly.
        
        """
    def _eq(self, other):
        """
        Compare two nodes for equality.
        """
    def clone(self):
        """
        Return a cloned (deep) copy of self.
        """
    def leaves(self):
        """
        Return a post-order iterator for the tree.
        """
    def pre_order(self):
        """
        Return a pre-order iterator for the tree.
        """
    def prefix(self):
        """

                The whitespace and comments preceding this token in the input.
        
        """
    def prefix(self, prefix):
        """

            Convert raw node information to a Node or Leaf instance.

            This is passed to the parser driver which calls it whenever a reduction of a
            grammar rule produces a new complete node, so that the tree is build
            strictly bottom-up.
    
        """
def BasePattern(object):
    """

        A pattern is a tree matching pattern.

        It looks for a specific node type (token or symbol), and
        optionally for a specific content.

        This is an abstract base class.  There are three concrete
        subclasses:

        - LeafPattern matches a single leaf node;
        - NodePattern matches a single node (usually non-leaf);
        - WildcardPattern matches a sequence of nodes of variable length.
    
    """
    def __new__(cls, *args, **kwds):
        """
        Constructor that prevents BasePattern from being instantiated.
        """
    def __repr__(self):
        """
        %s(%s)
        """
    def optimize(self):
        """

                A subclass can define this as a hook for optimizations.

                Returns either self or another node with the same effect.
        
        """
    def match(self, node, results=None):
        """

                Does this pattern exactly match a node?

                Returns True if it matches, False if not.

                If results is not None, it must be a dict which will be
                updated with the nodes matching named subpatterns.

                Default implementation for non-wildcard patterns.
        
        """
    def match_seq(self, nodes, results=None):
        """

                Does this pattern exactly match a sequence of nodes?

                Default implementation for non-wildcard patterns.
        
        """
    def generate_matches(self, nodes):
        """

                Generator yielding all matches for this pattern.

                Default implementation for non-wildcard patterns.
        
        """
def LeafPattern(BasePattern):
    """

            Initializer.  Takes optional type, content, and name.

            The type, if given must be a token type (< 256).  If not given,
            this matches any *leaf* node; the content may still be required.

            The content, if given, must be a string.

            If a name is given, the matching node is stored in the results
            dict under that key.
        
    """
    def match(self, node, results=None):
        """
        Override match() to insist on a leaf node.
        """
    def _submatch(self, node, results=None):
        """

                Match the pattern's content to the node's children.

                This assumes the node type matches and self.content is not None.

                Returns True if it matches, False if not.

                If results is not None, it must be a dict which will be
                updated with the nodes matching named subpatterns.

                When returning False, the results dict may still be updated.
        
        """
def NodePattern(BasePattern):
    """

            Initializer.  Takes optional type, content, and name.

            The type, if given, must be a symbol type (>= 256).  If the
            type is None this matches *any* single node (leaf or not),
            except if content is not None, in which it only matches
            non-leaf nodes that also match the content pattern.

            The content, if not None, must be a sequence of Patterns that
            must match the node's children exactly.  If the content is
            given, the type must not be None.

            If a name is given, the matching node is stored in the results
            dict under that key.
        
    """
    def _submatch(self, node, results=None):
        """

                Match the pattern's content to the node's children.

                This assumes the node type matches and self.content is not None.

                Returns True if it matches, False if not.

                If results is not None, it must be a dict which will be
                updated with the nodes matching named subpatterns.

                When returning False, the results dict may still be updated.
        
        """
def WildcardPattern(BasePattern):
    """

        A wildcard pattern can match zero or more nodes.

        This has all the flexibility needed to implement patterns like:

        .*      .+      .?      .{m,n}
        (a b c | d e | f)
        (...)*  (...)+  (...)?  (...){m,n}

        except it always uses non-greedy matching.
    
    """
    def __init__(self, content=None, min=0, max=HUGE, name=None):
        """

                Initializer.

                Args:
                    content: optional sequence of subsequences of patterns;
                             if absent, matches one node;
                             if present, each subsequence is an alternative [*]
                    min: optional minimum number of times to match, default 0
                    max: optional maximum number of times to match, default HUGE
                    name: optional name assigned to this match

                [*] Thus, if content is [[a, b, c], [d, e], [f, g, h]] this is
                    equivalent to (a b c | d e | f g h); if content is None,
                    this is equivalent to '.' in regular expression terms.
                    The min and max parameters work as follows:
                        min=0, max=maxint: .*
                        min=1, max=maxint: .+
                        min=0, max=1: .?
                        min=1, max=1: .
                    If content is not None, replace the dot with the parenthesized
                    list of alternatives, e.g. (a b c | d e | f g h)*
        
        """
    def optimize(self):
        """
        Optimize certain stacked wildcard patterns.
        """
    def match(self, node, results=None):
        """
        Does this pattern exactly match a node?
        """
    def match_seq(self, nodes, results=None):
        """
        Does this pattern exactly match a sequence of nodes?
        """
    def generate_matches(self, nodes):
        """

                Generator yielding matches for a sequence of nodes.

                Args:
                    nodes: sequence of nodes

                Yields:
                    (count, results) tuples where:
                    count: the match comprises nodes[:count];
                    results: dict containing named submatches.
        
        """
    def _iterative_matches(self, nodes):
        """
        Helper to iteratively yield the matches.
        """
    def _bare_name_matches(self, nodes):
        """
        Special optimized matcher for bare_name.
        """
    def _recursive_matches(self, nodes, count):
        """
        Helper to recursively yield the matches.
        """
def NegatedPattern(BasePattern):
    """

            Initializer.

            The argument is either a pattern or None.  If it is None, this
            only matches an empty sequence (effectively '$' in regex
            lingo).  If it is not None, this matches whenever the argument
            pattern doesn't have any matches.
        
    """
    def match(self, node):
        """
         We never match a node in its entirety

        """
    def match_seq(self, nodes):
        """
         We only match an empty sequence of nodes in its entirety

        """
    def generate_matches(self, nodes):
        """
         Return a match if there is an empty sequence

        """
def generate_matches(patterns, nodes):
    """

        Generator yielding matches for a sequence of patterns and nodes.

        Args:
            patterns: a sequence of patterns
            nodes: a sequence of nodes

        Yields:
            (count, results) tuples where:
            count: the entire sequence of patterns matches nodes[:count];
            results: dict containing named submatches.
        
    """
