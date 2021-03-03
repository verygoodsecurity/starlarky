def FixNext(fixer_base.BaseFix):
    """

        power< base=any+ trailer< '.' attr='next' > trailer< '(' ')' > >
        |
        power< head=any+ trailer< '.' attr='next' > not trailer< '(' ')' > >
        |
        classdef< 'class' any+ ':'
                  suite< any*
                         funcdef< 'def'
                                  name='next'
                                  parameters< '(' NAME ')' > any+ >
                         any* > >
        |
        global=global_stmt< 'global' any* 'next' any* >
    
    """
    def start_tree(self, tree, filename):
        """
        'next'
        """
    def transform(self, node, results):
        """
        base
        """
def is_assign_target(node):
