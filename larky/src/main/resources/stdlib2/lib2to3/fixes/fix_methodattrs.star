def FixMethodattrs(fixer_base.BaseFix):
    """

        power< any+ trailer< '.' attr=('im_func' | 'im_self' | 'im_class') > any* >
    
    """
    def transform(self, node, results):
        """
        attr
        """
