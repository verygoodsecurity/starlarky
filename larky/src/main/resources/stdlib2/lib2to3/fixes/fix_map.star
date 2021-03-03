def FixMap(fixer_base.ConditionalFix):
    """

        map_none=power<
            'map'
            trailer< '(' arglist< 'None' ',' arg=any [','] > ')' >
            [extra_trailers=trailer*]
        >
        |
        map_lambda=power<
            'map'
            trailer<
                '('
                arglist<
                    lambdef< 'lambda'
                             (fp=NAME | vfpdef< '(' fp=NAME ')'> ) ':' xp=any
                    >
                    ','
                    it=any
                >
                ')'
            >
            [extra_trailers=trailer*]
        >
        |
        power<
            'map' args=trailer< '(' [any] ')' >
            [extra_trailers=trailer*]
        >
    
    """
    def transform(self, node, results):
        """
        'extra_trailers'
        """
