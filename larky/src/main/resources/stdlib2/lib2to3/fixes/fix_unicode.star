def FixUnicode(fixer_base.BaseFix):
    """
    STRING | 'unicode' | 'unichr'
    """
    def start_tree(self, tree, filename):
        """
        'unicode_literals'
        """
    def transform(self, node, results):
        """
        '\'"'
        """
