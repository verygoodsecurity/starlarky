def FixHasKey(fixer_base.BaseFix):
    """

        anchor=power<
            before=any+
            trailer< '.' 'has_key' >
            trailer<
                '('
                ( not(arglist | argument<any '=' any>) arg=any
                | arglist<(not argument<any '=' any>) arg=any ','>
                )
                ')'
            >
            after=any*
        >
        |
        negation=not_test<
            'not'
            anchor=power<
                before=any+
                trailer< '.' 'has_key' >
                trailer<
                    '('
                    ( not(arglist | argument<any '=' any>) arg=any
                    | arglist<(not argument<any '=' any>) arg=any ','>
                    )
                    ')'
                >
            >
        >
    
    """
    def transform(self, node, results):
        """
         Don't transform a node matching the first alternative of the
         pattern when its parent matches the second alternative

        """
