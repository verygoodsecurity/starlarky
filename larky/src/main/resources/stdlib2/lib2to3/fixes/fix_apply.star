def FixApply(fixer_base.BaseFix):
    """

        power< 'apply'
            trailer<
                '('
                arglist<
                    (not argument<NAME '=' any>) func=any ','
                    (not argument<NAME '=' any>) args=any [','
                    (not argument<NAME '=' any>) kwds=any] [',']
                >
                ')'
            >
        >
    
    """
    def transform(self, node, results):
        """
        func
        """
