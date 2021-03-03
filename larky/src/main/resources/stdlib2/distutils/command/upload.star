def upload(PyPIRCCommand):
    """
    upload binary package to PyPI
    """
    def initialize_options(self):
        """
        ''
        """
    def finalize_options(self):
        """
        Must use --sign for --identity to have meaning

        """
    def run(self):
        """
        Must create and upload files in one command 
        (e.g. setup.py sdist upload)
        """
    def upload_file(self, command, pyversion, filename):
        """
         Makes sure the repository URL is compliant

        """
