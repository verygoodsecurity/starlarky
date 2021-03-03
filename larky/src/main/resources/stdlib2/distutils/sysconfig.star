def _is_python_source_dir(d):
    """
    Setup
    """
    def _fix_pcbuild(d):
        """
        PCbuild
        """
def _python_build():
    """
     Calculate the build qualifier flags if they are defined.  Adding the flags
     to the include and lib directories only makes sense for an installation, not
     an in-source build.

    """
def get_python_version():
    """
    Return a string containing the major and minor Python version,
        leaving off the patchlevel.  Sample return values could be '1.5'
        or '2.2'.
    
    """
def get_python_inc(plat_specific=0, prefix=None):
    """
    Return the directory containing installed Python header files.

        If 'plat_specific' is false (the default), this is the path to the
        non-platform-specific header files, i.e. Python.h and so on;
        otherwise, this is the path to platform-specific header files
        (namely pyconfig.h).

        If 'prefix' is supplied, use it instead of sys.base_prefix or
        sys.base_exec_prefix -- i.e., ignore 'plat_specific'.
    
    """
def get_python_lib(plat_specific=0, standard_lib=0, prefix=None):
    """
    Return the directory containing the Python library (standard or
        site additions).

        If 'plat_specific' is true, return the directory containing
        platform-specific modules, i.e. any module from a non-pure-Python
        module distribution; otherwise, return the platform-shared library
        directory.  If 'standard_lib' is true, return the directory
        containing standard Python library modules; otherwise, return the
        directory for site-specific modules.

        If 'prefix' is supplied, use it instead of sys.base_prefix or
        sys.base_exec_prefix -- i.e., ignore 'plat_specific'.
    
    """
def customize_compiler(compiler):
    """
    Do any platform-specific customization of a CCompiler instance.

        Mainly needed on Unix, so we can plug in the information that
        varies across Unices and is stored in Python's Makefile.
    
    """
def get_config_h_filename():
    """
    Return full pathname of installed pyconfig.h file.
    """
def get_makefile_filename():
    """
    Return full pathname of installed Makefile from the Python build.
    """
def parse_config_h(fp, g=None):
    """
    Parse a config.h-style file.

        A dictionary containing name/value pairs is returned.  If an
        optional dictionary is passed in as the second argument, it is
        used instead of a new dictionary.
    
    """
def parse_makefile(fn, g=None):
    """
    Parse a Makefile-style file.

        A dictionary containing name/value pairs is returned.  If an
        optional dictionary is passed in as the second argument, it is
        used instead of a new dictionary.
    
    """
def expand_makefile_vars(s, vars):
    """
    Expand Makefile-style variables -- "${foo}" or "$(foo)" -- in
        'string' according to 'vars' (a dictionary mapping variable names to
        values).  Variables not present in 'vars' are silently expanded to the
        empty string.  The variable values in 'vars' should not contain further
        variable expansions; if 'vars' is the output of 'parse_makefile()',
        you're fine.  Returns a variable-expanded version of 's'.
    
    """
def _init_posix():
    """
    Initialize the module as appropriate for POSIX systems.
    """
def _init_nt():
    """
    Initialize the module as appropriate for NT
    """
def get_config_vars(*args):
    """
    With no arguments, return a dictionary of all configuration
        variables relevant for the current platform.  Generally this includes
        everything needed to build extensions and install both pure modules and
        extensions.  On Unix, this means every variable defined in Python's
        installed Makefile; on Windows it's a much smaller set.

        With arguments, return a list of values that result from looking up
        each argument in the configuration variable dictionary.
    
    """
def get_config_var(name):
    """
    Return the value of a single variable using the dictionary
        returned by 'get_config_vars()'.  Equivalent to
        get_config_vars().get(name)
    
    """
