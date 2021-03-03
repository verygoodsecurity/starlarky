def FixXrange(fixer_base.BaseFix):
    """

                  power<
                     (name='range'|name='xrange') trailer< '(' args=any ')' >
                  rest=any* >
              
    """
    def start_tree(self, tree, filename):
        """
        name
        """
    def transform_xrange(self, node, results):
        """
        name
        """
    def transform_range(self, node, results):
        """
        range
        """
    def in_special_context(self, node):
        """
        node
        """
