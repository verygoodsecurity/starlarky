def get_msvcr():
    """
    Include the appropriate MSVC runtime library if Python was built
        with MSVC 7.0 or later.
    
    """
def CygwinCCompiler(UnixCCompiler):
    """
     Handles the Cygwin port of the GNU C compiler to Windows.
    
    """
    def __init__(self, verbose=0, dry_run=0, force=0):
        """
        Python's GCC status: %s (details: %s)
        """
    def _compile(self, obj, src, ext, cc_args, extra_postargs, pp_opts):
        """
        Compiles the source by spawning GCC and windres if needed.
        """
2021-03-02 20:46:29,754 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,754 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,754 : INFO : tokenize_signature : --> do i ever get here?
    def link(self, target_desc, objects, output_filename, output_dir=None,
             libraries=None, library_dirs=None, runtime_library_dirs=None,
             export_symbols=None, debug=0, extra_preargs=None,
             extra_postargs=None, build_temp=None, target_lang=None):
        """
        Link the objects.
        """
    def object_filenames(self, source_filenames, strip_dir=0, output_dir=''):
        """
        Adds supports for rc and res files.
        """
def Mingw32CCompiler(CygwinCCompiler):
    """
     Handles the Mingw32 port of the GNU C compiler to Windows.
    
    """
    def __init__(self, verbose=0, dry_run=0, force=0):
        """
         ld_version >= "2.13" support -shared so use it instead of
         -mdll -static

        """
def check_config_h():
    """
    Check if the current Python installation appears amenable to building
        extensions with GCC.

        Returns a tuple (status, details), where 'status' is one of the following
        constants:

        - CONFIG_H_OK: all is well, go ahead and compile
        - CONFIG_H_NOTOK: doesn't look good
        - CONFIG_H_UNCERTAIN: not sure -- unable to read pyconfig.h

        'details' is a human-readable string explaining the situation.

        Note there are two ways to conclude "OK": either 'sys.version' contains
        the string "GCC" (implying that this Python was built with GCC), or the
        installed "pyconfig.h" contains the string "__GNUC__".
    
    """
def _find_exe_version(cmd):
    """
    Find the version of an executable by running `cmd` in the shell.

        If the command is not found, or the output does not match
        `RE_VERSION`, returns None.
    
    """
def get_versions():
    """
     Try to find out the versions of gcc, ld and dllwrap.

        If not possible it returns None for it.
    
    """
def is_cygwingcc():
    """
    '''Try to determine if the gcc that would be used is from cygwin.'''
    """
