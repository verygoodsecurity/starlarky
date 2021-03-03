def bdist_rpm(Command):
    """
    create an RPM distribution
    """
    def initialize_options(self):
        """
        'bdist'
        """
    def finalize_package_data(self):
        """
        'group'
        """
    def run(self):
        """
        before _get_package_data():
        """
    def _dist_path(self, path):
        """
        Generate the text of an RPM spec file and return it as a
                list of strings (one per line).
        
        """
    def _format_changelog(self, changelog):
        """
        Format the changelog correctly and convert it to a list of strings
        
        """
