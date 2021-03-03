def ParseError(Exception):
    """
    Exception to signal the parser is stuck.
    """
    def __init__(self, msg, type, value, context):
        """
        %s: type=%r, value=%r, context=%r
        """
    def __reduce__(self):
        """
        Parser engine.

            The proper usage sequence is:

            p = Parser(grammar, [converter])  # create instance
            p.setup([start])                  # prepare for parsing
            <for each input token>:
                if p.addtoken(...):           # parse a token; may raise ParseError
                    break
            root = p.rootnode                 # root of abstract syntax tree

            A Parser instance may be reused by calling setup() repeatedly.

            A Parser instance contains state pertaining to the current token
            sequence, and should not be used concurrently by different threads
            to parse separate token sequences.

            See driver.py for how to get input tokens by tokenizing a file or
            string.

            Parsing is complete when addtoken() returns True; the root of the
            abstract syntax tree can then be retrieved from the rootnode
            instance variable.  When a syntax error occurs, addtoken() raises
            the ParseError exception.  There is no error recovery; the parser
            cannot be used after a syntax error was reported (but it can be
            reinitialized by calling setup()).

    
        """
    def __init__(self, grammar, convert=None):
        """
        Constructor.

                The grammar argument is a grammar.Grammar instance; see the
                grammar module for more information.

                The parser is not ready yet for parsing; you must call the
                setup() method to get it started.

                The optional convert argument is a function mapping concrete
                syntax tree nodes to abstract syntax tree nodes.  If not
                given, no conversion is done and the syntax tree produced is
                the concrete syntax tree.  If given, it must be a function of
                two arguments, the first being the grammar (a grammar.Grammar
                instance), and the second being the concrete syntax tree node
                to be converted.  The syntax tree is converted from the bottom
                up.

                A concrete syntax tree node is a (type, value, context, nodes)
                tuple, where type is the node type (a token or symbol number),
                value is None for symbols and a string for tokens, context is
                None or an opaque value used for error reporting (typically a
                (lineno, offset) pair), and nodes is a list of children for
                symbols, and None for tokens.

                An abstract syntax tree node may be anything; this is entirely
                up to the converter function.

        
        """
    def setup(self, start=None):
        """
        Prepare for parsing.

                This *must* be called before starting to parse.

                The optional argument is an alternative start symbol; it
                defaults to the grammar's start symbol.

                You can use a Parser instance to parse any number of programs;
                each time you call setup() the parser is reset to an initial
                state determined by the (implicit or explicit) start symbol.

        
        """
    def addtoken(self, type, value, context):
        """
        Add a token; return True iff this is the end of the program.
        """
    def classify(self, type, value, context):
        """
        Turn a token into a label.  (Internal)
        """
    def shift(self, type, value, newstate, context):
        """
        Shift a token.  (Internal)
        """
    def push(self, type, newdfa, newstate, context):
        """
        Push a nonterminal.  (Internal)
        """
    def pop(self):
        """
        Pop a nonterminal.  (Internal)
        """
