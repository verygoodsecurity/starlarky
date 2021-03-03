def install(Command):
    """
    install everything from build directory
    """
    def initialize_options(self):
        """
        Initializes options.
        """
    def finalize_options(self):
        """
        Finalizes options.
        """
    def dump_dirs(self, msg):
        """
        Dumps the list of user options.
        """
    def finalize_unix(self):
        """
        Finalizes options for posix platforms.
        """
    def finalize_other(self):
        """
        Finalizes options for non-posix platforms
        """
    def select_scheme(self, name):
        """
        Sets the install directories by applying the install schemes.
        """
    def _expand_attrs(self, attrs):
        """
        'posix'
        """
    def expand_basedirs(self):
        """
        Calls `os.path.expanduser` on install_base, install_platbase and
                root.
        """
    def expand_dirs(self):
        """
        Calls `os.path.expanduser` on install dirs.
        """
    def convert_paths(self, *names):
        """
        Call `convert_path` over `names`.
        """
    def handle_extra_path(self):
        """
        Set `path_file` and `extra_dirs` using `extra_path`.
        """
    def change_roots(self, *names):
        """
        Change the install directories pointed by name using root.
        """
    def create_home_path(self):
        """
        Create directories under ~.
        """
    def run(self):
        """
        Runs the command.
        """
    def create_path_file(self):
        """
        Creates the .pth file
        """
    def get_outputs(self):
        """
        Assembles the outputs of all the sub-commands.
        """
    def get_inputs(self):
        """
        Returns the inputs of all the sub-commands
        """
    def has_lib(self):
        """
        Returns true if the current distribution has any Python
                modules to install.
        """
    def has_headers(self):
        """
        Returns true if the current distribution has any headers to
                install.
        """
    def has_scripts(self):
        """
        Returns true if the current distribution has any scripts to.
                install.
        """
    def has_data(self):
        """
        Returns true if the current distribution has any data to.
                install.
        """
