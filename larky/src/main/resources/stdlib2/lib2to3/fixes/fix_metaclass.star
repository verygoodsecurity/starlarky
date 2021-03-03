def has_metaclass(parent):
    """
     we have to check the cls_node without changing it.
            There are two possibilities:
              1)  clsdef => suite => simple_stmt => expr_stmt => Leaf('__meta')
              2)  clsdef => simple_stmt => expr_stmt => Leaf('__meta')
    
    """
def fixup_parse_tree(cls_node):
    """
     one-line classes don't get a suite in the parse tree so we add
            one to normalize the tree
    
    """
def fixup_simple_stmt(parent, i, stmt_node):
    """
     if there is a semi-colon all the parts count as part of the same
            simple_stmt.  We just want the __metaclass__ part so we move
            everything after the semi-colon into its own simple_stmt node
    
    """
def remove_trailing_newline(node):
    """
     find the suite node (Mmm, sweet nodes)

    """
def fixup_indent(suite):
    """
     If an INDENT is followed by a thing with a prefix then nuke the prefix
            Otherwise we get in trouble when removing __metaclass__ at suite start
    
    """
def FixMetadef(fixer_base.BaseFix):
    """

        classdef<any*>
    
    """
    def transform(self, node, results):
        """
         find metaclasses, keep the last one

        """
