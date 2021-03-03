def install_egg_info(Command):
    """
    Install an .egg-info file for the package
    """
    def initialize_options(self):
        """
        'install_lib'
        """
    def run(self):
        """
        Removing 
        """
    def get_outputs(self):
        """
         The following routines are taken from setuptools' pkg_resources module and
         can be replaced by importing them from pkg_resources once it is included
         in the stdlib.


        """
def safe_name(name):
    """
    Convert an arbitrary string to a standard distribution name

        Any runs of non-alphanumeric/. characters are replaced with a single '-'.
    
    """
def safe_version(version):
    """
    Convert an arbitrary string to a standard version string

        Spaces become dots, and all other non-alphanumeric characters become
        dashes, with runs of multiple dashes condensed to a single dash.
    
    """
def to_filename(name):
    """
    Convert a project or version name to its filename-escaped form

        Any '-' characters are currently replaced with '_'.
    
    """
