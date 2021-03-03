def show_compilers():
    """
    build everything needed to install
    """
    def initialize_options(self):
        """
        'build'
        """
    def finalize_options(self):
        """
         plat-name only supported for windows (other platforms are
         supported via ./configure flags, if at all).  Avoid misleading
         other platforms.

        """
    def run(self):
        """
         Run all relevant sub-commands.  This will be some subset of:
          - build_py      - pure Python modules
          - build_clib    - standalone C libraries
          - build_ext     - Python extensions
          - build_scripts - (Python) scripts

        """
    def has_pure_modules(self):
        """
        'build_py'
        """
