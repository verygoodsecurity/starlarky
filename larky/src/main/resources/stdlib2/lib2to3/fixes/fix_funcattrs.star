def FixFuncattrs(fixer_base.BaseFix):
    """

        power< any+ trailer< '.' attr=('func_closure' | 'func_doc' | 'func_globals'
                                      | 'func_name' | 'func_defaults' | 'func_code'
                                      | 'func_dict') > any* >
    
    """
    def transform(self, node, results):
        """
        attr
        """
