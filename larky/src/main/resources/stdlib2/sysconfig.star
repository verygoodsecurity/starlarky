def _safe_realpath(path):
    """
     sys.executable can be empty if argv[0] has been changed and Python is
     unable to retrieve the real program name

    """
def _is_python_source_dir(d):
    """
    Setup
    """
    def _fix_pcbuild(d):
        """
        PCbuild
        """
def is_python_build(check_home=False):
    """
    'posix_prefix'
    """
def _subst_vars(s, local_vars):
    """
    '{%s}'
    """
def _extend_dict(target_dict, other_dict):
    """
    'posix'
    """
def _get_default_scheme():
    """
    'posix'
    """
def _getuserbase():
    """
    PYTHONUSERBASE
    """
    def joinuser(*args):
        """
        nt
        """
def _parse_makefile(filename, vars=None):
    """
    Parse a Makefile-style file.

        A dictionary containing name/value pairs is returned.  If an
        optional dictionary is passed in as the second argument, it is
        used instead of a new dictionary.
    
    """
def get_makefile_filename():
    """
    Return the path of the Makefile.
    """
def _get_sysconfigdata_name():
    """
    '_PYTHON_SYSCONFIGDATA_NAME'
    """
def _generate_posix_vars():
    """
    Generate the Python module containing build-time variables.
    """
def _init_posix(vars):
    """
    Initialize the module as appropriate for POSIX systems.
    """
def _init_non_posix(vars):
    """
    Initialize the module as appropriate for NT
    """
def parse_config_h(fp, vars=None):
    """
    Parse a config.h-style file.

        A dictionary containing name/value pairs is returned.  If an
        optional dictionary is passed in as the second argument, it is
        used instead of a new dictionary.
    
    """
def get_config_h_filename():
    """
    Return the path of pyconfig.h.
    """
def get_scheme_names():
    """
    Return a tuple containing the schemes names.
    """
def get_path_names():
    """
    Return a tuple containing the paths names.
    """
def get_paths(scheme=_get_default_scheme(), vars=None, expand=True):
    """
    Return a mapping containing an install scheme.

        ``scheme`` is the install scheme name. If not provided, it will
        return the default scheme for the current platform.
    
    """
def get_path(name, scheme=_get_default_scheme(), vars=None, expand=True):
    """
    Return a path corresponding to the scheme.

        ``scheme`` is the install scheme name.
    
    """
def get_config_vars(*args):
    """
    With no arguments, return a dictionary of all configuration
        variables relevant for the current platform.

        On Unix, this means every variable defined in Python's installed Makefile;
        On Windows it's a much smaller set.

        With arguments, return a list of values that result from looking up
        each argument in the configuration variable dictionary.
    
    """
def get_config_var(name):
    """
    Return the value of a single variable using the dictionary returned by
        'get_config_vars()'.

        Equivalent to get_config_vars().get(name)
    
    """
def get_platform():
    """
    Return a string that identifies the current platform.

        This is used mainly to distinguish platform-specific build directories and
        platform-specific built distributions.  Typically includes the OS name and
        version and the architecture (as supplied by 'os.uname()'), although the
        exact information included depends on the OS; on Linux, the kernel version
        isn't particularly important.

        Examples of returned values:
           linux-i586
           linux-alpha (?)
           solaris-2.6-sun4u

        Windows will return one of:
           win-amd64 (64bit Windows on AMD64 (aka x86_64, Intel64, EM64T, etc)
           win32 (all others - specifically, sys.platform is returned)

        For other non-POSIX platforms, currently just returns 'sys.platform'.

    
    """
def get_python_version():
    """
    '%s: '
    """
def _main():
    """
    Display all information sysconfig detains.
    """
