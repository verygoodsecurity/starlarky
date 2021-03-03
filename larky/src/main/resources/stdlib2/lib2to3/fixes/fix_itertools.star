def FixItertools(fixer_base.BaseFix):
    """
    ('imap'|'ifilter'|'izip'|'izip_longest'|'ifilterfalse')
    """
    def transform(self, node, results):
        """
        'func'
        """
