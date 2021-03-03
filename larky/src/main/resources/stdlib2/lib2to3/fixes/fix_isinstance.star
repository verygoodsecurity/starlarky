def FixIsinstance(fixer_base.BaseFix):
    """

        power<
            'isinstance'
            trailer< '(' arglist< any ',' atom< '('
                args=testlist_gexp< any+ >
            ')' > > ')' >
        >
    
    """
    def transform(self, node, results):
        """
        args
        """
