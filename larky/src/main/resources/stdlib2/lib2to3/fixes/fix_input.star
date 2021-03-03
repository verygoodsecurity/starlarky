def FixInput(fixer_base.BaseFix):
    """

                  power< 'input' args=trailer< '(' [any] ')' > >
              
    """
    def transform(self, node, results):
        """
         If we're already wrapped in an eval() call, we're done.

        """
