def BaseFix(object):
    """
    Optional base class for fixers.

        The subclass name must be FixFooBar where FooBar is the result of
        removing underscores and capitalizing the words of the fix name.
        For example, the class name for a fixer named 'has_key' should be
        FixHasKey.
    
    """
    def __init__(self, options, log):
        """
        Initializer.  Subclass may override.

                Args:
                    options: a dict containing the options passed to RefactoringTool
                    that could be used to customize the fixer through the command line.
                    log: a list to append warnings and other messages to.
        
        """
    def compile_pattern(self):
        """
        Compiles self.PATTERN into self.pattern.

                Subclass may override if it doesn't want to use
                self.{pattern,PATTERN} in .match().
        
        """
    def set_filename(self, filename):
        """
        Set the filename.

                The main refactoring tool should call this.
        
        """
    def match(self, node):
        """
        Returns match for a given parse tree node.

                Should return a true or false object (not necessarily a bool).
                It may return a non-empty dict of matching sub-nodes as
                returned by a matching pattern.

                Subclass may override.
        
        """
    def transform(self, node, results):
        """
        Returns the transformation for a given parse tree node.

                Args:
                  node: the root of the parse tree that matched the fixer.
                  results: a dict mapping symbolic names to part of the match.

                Returns:
                  None, or a node that is a modified copy of the
                  argument node.  The node argument may also be modified in-place to
                  effect the same change.

                Subclass *must* override.
        
        """
    def new_name(self, template="xxx_todo_changeme"):
        """
        Return a string suitable for use as an identifier

                The new name is guaranteed not to conflict with other identifiers.
        
        """
    def log_message(self, message):
        """
         In file %s ###
        """
    def cannot_convert(self, node, reason=None):
        """
        Warn the user that a given chunk of code is not valid Python 3,
                but that it cannot be converted automatically.

                First argument is the top-level node for the code in question.
                Optional second argument is why it can't be converted.
        
        """
    def warning(self, node, reason):
        """
        Used for warning the user about possible uncertainty in the
                translation.

                First argument is the top-level node for the code in question.
                Optional second argument is why it can't be converted.
        
        """
    def start_tree(self, tree, filename):
        """
        Some fixers need to maintain tree-wide state.
                This method is called once, at the start of tree fix-up.

                tree - the root node of the tree to be processed.
                filename - the name of the file the tree came from.
        
        """
    def finish_tree(self, tree, filename):
        """
        Some fixers need to maintain tree-wide state.
                This method is called once, at the conclusion of tree fix-up.

                tree - the root node of the tree to be processed.
                filename - the name of the file the tree came from.
        
        """
def ConditionalFix(BaseFix):
    """
     Base class for fixers which not execute if an import is found. 
    """
    def start_tree(self, *args):
        """
        .
        """
