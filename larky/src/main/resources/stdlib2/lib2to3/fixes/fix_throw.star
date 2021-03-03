def FixThrow(fixer_base.BaseFix):
    """

        power< any trailer< '.' 'throw' >
               trailer< '(' args=arglist< exc=any ',' val=any [',' tb=any] > ')' >
        >
        |
        power< any trailer< '.' 'throw' > trailer< '(' exc=any ')' > >
    
    """
    def transform(self, node, results):
        """
        exc
        """
