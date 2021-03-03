def FixRaise(fixer_base.BaseFix):
    """

        raise_stmt< 'raise' exc=any [',' val=any [',' tb=any]] >
    
    """
    def transform(self, node, results):
        """
        exc
        """
