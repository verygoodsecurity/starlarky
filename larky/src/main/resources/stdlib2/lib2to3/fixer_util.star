def KeywordArg(keyword, value):
    """
    =
    """
def LParen():
    """
    (
    """
def RParen():
    """
    )
    """
def Assign(target, source):
    """
    Build an assignment statement
    """
def Name(name, prefix=None):
    """
    Return a NAME leaf
    """
def Attr(obj, attr):
    """
    A node tuple for obj.attr
    """
def Comma():
    """
    A comma leaf
    """
def Dot():
    """
    A period (.) leaf
    """
def ArgList(args, lparen=LParen(), rparen=RParen()):
    """
    A parenthesised argument list, used by Call()
    """
def Call(func_name, args=None, prefix=None):
    """
    A function call
    """
def Newline():
    """
    A newline literal
    """
def BlankLine():
    """
    A blank line
    """
def Number(n, prefix=None):
    """
    A numeric or string subscript
    """
def String(string, prefix=None):
    """
    A string leaf
    """
def ListComp(xp, fp, it, test=None):
    """
    A list comprehension of the form [xp for fp in it if test].

        If test is None, the "if test" part is omitted.
    
    """
def FromImport(package_name, name_leafs):
    """
     Return an import statement in the form:
            from package import name_leafs
    """
def ImportAndCall(node, results, names):
    """
    Returns an import statement and calls a method
        of the module:

        import module
        module.name()
    """
def is_tuple(node):
    """
    Does the node represent a tuple literal?
    """
def is_list(node):
    """
    Does the node represent a list literal?
    """
def parenthesize(node):
    """
    sorted
    """
def attr_chain(obj, attr):
    """
    Follow an attribute chain.

        If you have a chain of objects where a.foo -> b, b.foo-> c, etc,
        use this to iterate over all objects in the chain. Iteration is
        terminated by getattr(x, attr) is None.

        Args:
            obj: the starting object
            attr: the name of the chaining attribute

        Yields:
            Each successive object in the chain.
    
    """
def in_special_context(node):
    """
     Returns true if node is in an environment where all that is required
            of it is being iterable (ie, it doesn't matter if it returns a list
            or an iterator).
            See test_map_nochange in test_fixers.py for some examples and tests.
        
    """
def is_probably_builtin(node):
    """

        Check that something isn't an attribute or function name etc.
    
    """
def find_indentation(node):
    """
    Find the indentation of *node*.
    """
def make_suite(node):
    """
    Find the top level namespace.
    """
def does_tree_import(package, name, node):
    """
     Returns true if name is imported from package at the
            top level of the tree which node belongs to.
            To cover the case of an import like 'import foo', use
            None for the package and 'foo' for the name. 
    """
def is_import(node):
    """
    Returns true if the node is an import statement.
    """
def touch_import(package, name, node):
    """
     Works like `does_tree_import` but adds an import statement
            if it was not imported. 
    """
    def is_import_stmt(node):
        """
         figure out where to insert the new import.  First try to find
         the first import and then skip to the last one.

        """
def find_binding(name, node, package=None):
    """
     Returns the node which binds variable name, otherwise None.
            If optional argument package is supplied, only imports will
            be returned.
            See test cases for examples.
    """
def _find(name, node):
    """
     Will reuturn node if node will import name, or node
            will import * from package.  None is returned otherwise.
            See test cases for examples. 
    """
