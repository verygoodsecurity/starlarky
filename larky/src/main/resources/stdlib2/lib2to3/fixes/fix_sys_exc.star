def FixSysExc(fixer_base.BaseFix):
    """
     This order matches the ordering of sys.exc_info().

    """
    def transform(self, node, results):
        """
        attribute
        """
