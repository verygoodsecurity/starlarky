def FixDict(fixer_base.BaseFix):
    """

        power< head=any+
             trailer< '.' method=('keys'|'items'|'values'|
                                  'iterkeys'|'iteritems'|'itervalues'|
                                  'viewkeys'|'viewitems'|'viewvalues') >
             parens=trailer< '(' ')' >
             tail=any*
        >
    
    """
    def transform(self, node, results):
        """
        head
        """
    def in_special_context(self, node, isiter):
        """
        node
        """
