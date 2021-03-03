def install_lib(Command):
    """
    install all Python modules (extensions and pure Python)
    """
    def initialize_options(self):
        """
         let the 'install' command dictate our installation directory

        """
    def finalize_options(self):
        """
         Get all the information we need to install pure Python modules
         from the umbrella 'install' command -- build (source) directory,
         install (target) directory, and whether to compile .py files.

        """
    def run(self):
        """
         Make sure we have built everything we need first

        """
    def build(self):
        """
        'build_py'
        """
    def install(self):
        """
        '%s' does not exist -- no Python modules to install
        """
    def byte_compile(self, files):
        """
        'byte-compiling is disabled, skipping.'
        """
    def _mutate_outputs(self, has_any, build_cmd, cmd_option, output_dir):
        """
         Since build_py handles package data installation, the
         list of outputs can contain more than just .py files.
         Make sure we only report bytecode for the .py files.

        """
    def get_outputs(self):
        """
        Return the list of files that would be installed if this command
                were actually run.  Not affected by the "dry-run" flag or whether
                modules have actually been built yet.
        
        """
    def get_inputs(self):
        """
        Get the list of files that are input to this command, ie. the
                files that get installed as they are named in the build tree.
                The files in this list correspond one-to-one to the output
                filenames returned by 'get_outputs()'.
        
        """
