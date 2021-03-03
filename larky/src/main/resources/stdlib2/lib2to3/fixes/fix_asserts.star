def FixAsserts(BaseFix):
    """

                  power< any+ trailer< '.' meth=(%s)> any* >
              
    """
    def transform(self, node, results):
        """
        meth
        """
