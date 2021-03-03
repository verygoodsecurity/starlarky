def FixFilter(fixer_base.ConditionalFix):
    """

        filter_lambda=power<
            'filter'
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
            'filter'
            trailer< '(' arglist< none='None' ',' seq=any > ')' >
            [extra_trailers=trailer*]
        >
        |
        power<
            'filter'
            args=trailer< '(' [any] ')' >
            [extra_trailers=trailer*]
        >
    
    """
    def transform(self, node, results):
        """
        'extra_trailers'
        """
