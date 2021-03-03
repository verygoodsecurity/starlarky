def FixRawInput(fixer_base.BaseFix):
    """

                  power< name='raw_input' trailer< '(' [any] ')' > any* >
              
    """
    def transform(self, node, results):
        """
        name
        """
