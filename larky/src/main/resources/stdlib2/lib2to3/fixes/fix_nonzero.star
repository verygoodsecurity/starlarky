def FixNonzero(fixer_base.BaseFix):
    """

        classdef< 'class' any+ ':'
                  suite< any*
                         funcdef< 'def' name='__nonzero__'
                                  parameters< '(' NAME ')' > any+ >
                         any* > >
    
    """
    def transform(self, node, results):
        """
        name
        """
