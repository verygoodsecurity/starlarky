def PyPIRCCommand(Command):
    """
    Base command that knows how to handle the .pypirc file
    
    """
    def _get_rc_file(self):
        """
        Returns rc file path.
        """
    def _store_pypirc(self, username, password):
        """
        Creates a default .pypirc file.
        """
    def _read_pypirc(self):
        """
        Reads the .pypirc file.
        """
    def _read_pypi_response(self, response):
        """
        Read and decode a PyPI HTTP response.
        """
    def initialize_options(self):
        """
        Initialize options.
        """
    def finalize_options(self):
        """
        Finalizes options.
        """
