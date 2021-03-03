def FixItertoolsImports(fixer_base.BaseFix):
    """

                  import_from< 'from' 'itertools' 'import' imports=any >
              
    """
    def transform(self, node, results):
        """
        'imports'
        """
