def FixExitfunc(fixer_base.BaseFix):
    """

                  (
                      sys_import=import_name<'import'
                          ('sys'
                          |
                          dotted_as_names< (any ',')* 'sys' (',' any)* >
                          )
                      >
                  |
                      expr_stmt<
                          power< 'sys' trailer< '.' 'exitfunc' > >
                      '=' func=any >
                  )
              
    """
    def __init__(self, *args):
        """
         First, find the sys import. We'll just hope it's global scope.

        """
