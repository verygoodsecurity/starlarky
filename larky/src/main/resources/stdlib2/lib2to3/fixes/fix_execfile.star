def FixExecfile(fixer_base.BaseFix):
    """

        power< 'execfile' trailer< '(' arglist< filename=any [',' globals=any [',' locals=any ] ] > ')' > >
        |
        power< 'execfile' trailer< '(' filename=any ')' > >
    
    """
    def transform(self, node, results):
        """
        filename
        """
