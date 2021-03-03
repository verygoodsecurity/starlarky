def clean(Command):
    """
    clean up temporary files from 'build' command
    """
    def initialize_options(self):
        """
        'build'
        """
    def run(self):
        """
         remove the build/temp.<plat> directory (unless it's already
         gone)

        """
