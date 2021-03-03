def bdist_wininst(Command):
    """
    create an executable installer for MS Windows
    """
    def __init__(self, *args, **kw):
        """
        bdist_wininst command is deprecated since Python 3.8, 
        use bdist_wheel (wheel packages) instead
        """
    def initialize_options(self):
        """
        'bdist'
        """
    def run(self):
        """
        win32
        """
    def get_inidata(self):
        """
         Return data describing the installation.

        """
        def escape(s):
            """
            \n
            """
    def create_exe(self, arcname, fullname, bitmap=None):
        """
        creating %s
        """
    def get_installer_filename(self, fullname):
        """
         Factored out to allow overriding in subclasses

        """
    def get_exe_bytes(self):
        """
         If a target-version other than the current version has been
         specified, then using the MSVC version from *this* build is no good.
         Without actually finding and executing the target version and parsing
         its sys.version, we just hard-code our knowledge of old versions.
         NOTE: Possible alternative is to allow "--target-version" to
         specify a Python executable rather than a simple version string.
         We can then execute this program to obtain any info we need, such
         as the real sys.version string for the build.

        """
