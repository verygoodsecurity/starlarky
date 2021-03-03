def get_all_fix_names(fixer_pkg, remove_prefix=True):
    """
    Return a sorted list of all available fix names in the given package.
    """
def _EveryNode(Exception):
    """
     Accepts a pytree Pattern Node and returns a set
            of the pattern types which will match first. 
    """
def _get_headnode_dict(fixer_list):
    """
     Accepts a list of fixers and returns a dictionary
            of head node type --> fixer list.  
    """
def get_fixers_from_package(pkg_name):
    """

        Return the fully qualified names for fixers in the package pkg_name.
    
    """
def _identity(obj):
    """
    from
    """
def FixerError(Exception):
    """
    A fixer could not be loaded.
    """
def RefactoringTool(object):
    """
    print_function
    """
    def __init__(self, fixer_names, options=None, explicit=None):
        """
        Initializer.

                Args:
                    fixer_names: a list of fixers to import
                    options: a dict with configuration.
                    explicit: a list of fixers to run even if they are explicit.
        
        """
    def get_fixers(self):
        """
        Inspects the options to load the requested patterns and handlers.

                Returns:
                  (pre_order, post_order), where pre_order is the list of fixers that
                  want a pre-order AST traversal, and post_order is the list that want
                  post-order traversal.
        
        """
    def log_error(self, msg, *args, **kwds):
        """
        Called when an error occurs.
        """
    def log_message(self, msg, *args):
        """
        Hook to log a message.
        """
    def log_debug(self, msg, *args):
        """
        Called with the old version, new version, and filename of a
                refactored file.
        """
    def refactor(self, items, write=False, doctests_only=False):
        """
        Refactor a list of files and directories.
        """
    def refactor_dir(self, dir_name, write=False, doctests_only=False):
        """
        Descends down a directory and refactor every Python file found.

                Python files are assumed to have a .py extension.

                Files and subdirectories starting with '.' are skipped.
        
        """
    def _read_python_source(self, filename):
        """

                Do our best to decode a Python source file correctly.
        
        """
    def refactor_file(self, filename, write=False, doctests_only=False):
        """
        Refactors a file.
        """
    def refactor_string(self, data, name):
        """
        Refactor a given input string.

                Args:
                    data: a string holding the code to be refactored.
                    name: a human-readable name for use in error/log messages.

                Returns:
                    An AST corresponding to the refactored input stream; None if
                    there were errors during the parse.
        
        """
    def refactor_stdin(self, doctests_only=False):
        """
        Refactoring doctests in stdin
        """
    def refactor_tree(self, tree, name):
        """
        Refactors a parse tree (modifying the tree in place).

                For compatible patterns the bottom matcher module is
                used. Otherwise the tree is traversed node-to-node for
                matches.

                Args:
                    tree: a pytree.Node instance representing the root of the tree
                          to be refactored.
                    name: a human-readable name for this tree.

                Returns:
                    True if the tree was modified, False otherwise.
        
        """
    def traverse_by(self, fixers, traversal):
        """
        Traverse an AST, applying a set of fixers to each node.

                This is a helper method for refactor_tree().

                Args:
                    fixers: a list of fixer instances.
                    traversal: a generator that yields AST nodes.

                Returns:
                    None
        
        """
2021-03-02 20:54:12,441 : INFO : tokenize_signature : --> do i ever get here?
    def processed_file(self, new_text, filename, old_text=None, write=False,
                       encoding=None):
        """

                Called when a file has been refactored and there may be changes.
        
        """
    def write_file(self, new_text, filename, old_text, encoding=None):
        """
        Writes a string to a file.

                It first shows a unified diff between the old text and the new text, and
                then rewrites the file; the latter is only done if the write option is
                set.
        
        """
    def refactor_docstring(self, input, filename):
        """
        Refactors a docstring, looking for doctests.

                This returns a modified version of the input string.  It looks
                for doctests, which start with a ">>>" prompt, and may be
                continued with "..." prompts, as long as the "..." is indented
                the same as the ">>>".

                (Unfortunately we can't use the doctest module's parser,
                since, like most parsers, it is not geared towards preserving
                the original source.)
        
        """
    def refactor_doctest(self, block, lineno, indent, filename):
        """
        Refactors one doctest.

                A doctest is given as a block of lines, the first of which starts
                with ">>>" (possibly indented), while the remaining lines start
                with "..." (identically indented).

        
        """
    def summarize(self):
        """
        were
        """
    def parse_block(self, block, lineno, indent):
        """
        Parses a block into a tree.

                This is necessary to get correct line number / offset information
                in the parser diagnostics and embedded into the parse tree.
        
        """
    def wrap_toks(self, block, lineno, indent):
        """
        Wraps a tokenize stream to systematically modify start/end.
        """
    def gen_lines(self, block, indent):
        """
        Generates lines as expected by tokenize from a list of lines.

                This strips the first len(indent + self.PS1) characters off each line.
        
        """
def MultiprocessingUnsupported(Exception):
    """
    already doing multiple processes
    """
    def _child(self):
