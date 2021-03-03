def FixExec(fixer_base.BaseFix):
    """

        exec_stmt< 'exec' a=any 'in' b=any [',' c=any] >
        |
        exec_stmt< 'exec' (not atom<'(' [any] ')'>) a=any >
    
    """
    def transform(self, node, results):
        """
        a
        """
