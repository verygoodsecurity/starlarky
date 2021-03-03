def FixXreadlines(fixer_base.BaseFix):
    """

        power< call=any+ trailer< '.' 'xreadlines' > trailer< '(' ')' > >
        |
        power< any+ trailer< '.' no_call='xreadlines' > >
    
    """
    def transform(self, node, results):
        """
        no_call
        """
