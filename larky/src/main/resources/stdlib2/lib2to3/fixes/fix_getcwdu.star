def FixGetcwdu(fixer_base.BaseFix):
    """

                  power< 'os' trailer< dot='.' name='getcwdu' > any* >
              
    """
    def transform(self, node, results):
        """
        name
        """
