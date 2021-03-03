def FixZip(fixer_base.ConditionalFix):
    """

        power< 'zip' args=trailer< '(' [any] ')' > [trailers=trailer*]
        >
    
    """
    def transform(self, node, results):
        """
        'args'
        """
