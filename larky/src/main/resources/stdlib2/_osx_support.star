def _find_executable(executable, path=None):
    """
    Tries to find 'executable' in the directories listed in 'path'.

        A string listing directories separated by 'os.pathsep'; defaults to
        os.environ['PATH'].  Returns the complete filename or None if not found.
    
    """
def _read_output(commandstring):
    """
    Output from successful command execution or None
    """
def _find_build_tool(toolname):
    """
    Find a build tool on current path or using xcrun
    """
def _get_system_version():
    """
    Return the OS X system version as a string
    """
def _remove_original_values(_config_vars):
    """
    Remove original unmodified values for testing
    """
def _save_modified_value(_config_vars, cv, newvalue):
    """
    Save modified and original unmodified value of configuration var
    """
def _supports_universal_builds():
    """
    Returns True if universal builds are supported on this system
    """
def _find_appropriate_compiler(_config_vars):
    """
    Find appropriate C compiler for extension module builds
    """
def _remove_universal_flags(_config_vars):
    """
    Remove all universal build arguments from config vars
    """
def _remove_unsupported_archs(_config_vars):
    """
    Remove any unsupported archs from config vars
    """
def _override_all_archs(_config_vars):
    """
    Allow override of all archs with ARCHFLAGS env var
    """
def _check_for_unavailable_sdk(_config_vars):
    """
    Remove references to any SDKs not available
    """
def compiler_fixup(compiler_so, cc_args):
    """

        This function will strip '-isysroot PATH' and '-arch ARCH' from the
        compile flags if the user has specified one them in extra_compile_flags.

        This is needed because '-arch ARCH' adds another architecture to the
        build, without a way to remove an architecture. Furthermore GCC will
        barf if multiple '-isysroot' arguments are present.
    
    """
def customize_config_vars(_config_vars):
    """
    Customize Python build configuration variables.

        Called internally from sysconfig with a mutable mapping
        containing name/value pairs parsed from the configured
        makefile used to build this interpreter.  Returns
        the mapping updated as needed to reflect the environment
        in which the interpreter is running; in the case of
        a Python from a binary installer, the installed
        environment may be very different from the build
        environment, i.e. different OS levels, different
        built tools, different available CPU architectures.

        This customization is performed whenever
        distutils.sysconfig.get_config_vars() is first
        called.  It may be used in environments where no
        compilers are present, i.e. when installing pure
        Python dists.  Customization of compiler paths
        and detection of unavailable archs is deferred
        until the first extension module build is
        requested (in distutils.sysconfig.customize_compiler).

        Currently called from distutils.sysconfig
    
    """
def customize_compiler(_config_vars):
    """
    Customize compiler path and configuration variables.

        This customization is performed when the first
        extension module build is requested
        in distutils.sysconfig.customize_compiler).
    
    """
def get_platform_osx(_config_vars, osname, release, machine):
    """
    Filter values for get_platform()
    """
