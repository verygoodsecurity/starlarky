def FixFuture(fixer_base.BaseFix):
    """
    import_from< 'from' module_name="__future__" 'import' any >
    """
    def transform(self, node, results):
