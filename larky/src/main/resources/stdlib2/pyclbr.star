def _Object:
    """
    Information about Python class or function.
    """
    def __init__(self, module, name, file, lineno, parent):
        """
        Information about a Python function, including methods.
        """
    def __init__(self, module, name, file, lineno, parent=None):
        """
        Information about a Python class.
        """
    def __init__(self, module, name, super, file, lineno, parent=None):
        """
        Return a Function after nesting within ob.
        """
def _nest_class(ob, class_name, lineno, super=None):
    """
    Return a Class after nesting within ob.
    """
def readmodule(module, path=None):
    """
    Return Class objects for the top-level classes in module.

        This is the original interface, before Functions were added.
    
    """
def readmodule_ex(module, path=None):
    """
    Return a dictionary with all functions and classes in module.

        Search for module in PATH + sys.path.
        If possible, include imported superclasses.
        Do this by reading source, without importing (and executing) it.
    
    """
def _readmodule(module, path, inpackage=None):
    """
    Do the hard work for readmodule[_ex].

        If inpackage is given, it must be the dotted name of the package in
        which we are searching for a submodule, and then PATH must be the
        package search path; otherwise, we are searching for a top-level
        module, and path is combined with sys.path.
    
    """
def _create_tree(fullmodule, path, fname, source, tree, inpackage):
    """
    Return the tree for a particular module.

        fullmodule (full module name), inpackage+module, becomes o.module.
        path is passed to recursive calls of _readmodule.
        fname becomes o.file.
        source is tokenized.  Imports cause recursive calls to _readmodule.
        tree is {} or {'__path__': <submodule search locations>}.
        inpackage, None or string, is passed to recursive calls of _readmodule.

        The effect of recursive calls is mutation of global _modules.
    
    """
def _getnamelist(g):
    """
    Return list of (dotted-name, as-name or None) tuples for token source g.

        An as-name is the name that follows 'as' in an as clause.
    
    """
def _getname(g):
    """
    Return (dotted-name or None, next-token) tuple for token source g.
    """
def _main():
    """
    Print module output (default this file) for quick visual check.
    """
