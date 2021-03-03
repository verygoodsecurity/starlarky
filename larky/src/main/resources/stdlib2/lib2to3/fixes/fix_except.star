def find_excepts(nodes):
    """
    'except'
    """
def FixExcept(fixer_base.BaseFix):
    """

        try_stmt< 'try' ':' (simple_stmt | suite)
                      cleanup=(except_clause ':' (simple_stmt | suite))+
                      tail=(['except' ':' (simple_stmt | suite)]
                            ['else' ':' (simple_stmt | suite)]
                            ['finally' ':' (simple_stmt | suite)]) >
    
    """
    def transform(self, node, results):
        """
        tail
        """
