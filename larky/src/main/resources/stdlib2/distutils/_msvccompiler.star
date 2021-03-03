def _find_vc2015():
    """
    r"Software\Microsoft\VisualStudio\SxS\VC7
    """
def _find_vc2017():
    """
    Returns "15, path" based on the result of invoking vswhere.exe
        If no install is found, returns "None, None"

        The version is returned to avoid unnecessarily changing the function
        result. It may be ignored when the path is not None.

        If vswhere.exe is not available, by definition, VS 2017 is not
        installed.
    
    """
def _find_vcvarsall(plat_spec):
    """
     bpo-38597: Removed vcruntime return value

    """
def _get_vc_env(plat_spec):
    """
    DISTUTILS_USE_SDK
    """
def _find_exe(exe, paths=None):
    """
    Return path to an MSVC executable program.

        Tries to find the program in several places: first, one of the
        MSVC program search paths from the registry; next, the directories
        in the PATH environment variable.  If any of those work, return an
        absolute path that is known to exist.  If none of them work, just
        return the original program name, 'exe'.
    
    """
def MSVCCompiler(CCompiler) :
    """
    Concrete class that implements an interface to Microsoft Visual C++,
           as defined by the CCompiler abstract class.
    """
    def __init__(self, verbose=0, dry_run=0, force=0):
        """
         target platform (.plat_name is consistent with 'bdist')

        """
    def initialize(self, plat_name=None):
        """
         multi-init means we would need to check platform same each time...

        """
2021-03-02 20:46:26,746 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,746 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,746 : INFO : tokenize_signature : --> do i ever get here?
    def object_filenames(self,
                         source_filenames,
                         strip_dir=0,
                         output_dir=''):
        """
        ''
        """
        def make_out_path(p):
            """
             XXX: This may produce absurdly long paths. We should check
             the length of the result and trim base until we fit within
             260 characters.

            """
2021-03-02 20:46:26,747 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,747 : INFO : tokenize_signature : --> do i ever get here?
    def compile(self, sources,
                output_dir=None, macros=None, include_dirs=None, debug=0,
                extra_preargs=None, extra_postargs=None, depends=None):
        """
        '/c'
        """
2021-03-02 20:46:26,749 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,749 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,749 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,749 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,749 : INFO : tokenize_signature : --> do i ever get here?
    def create_static_lib(self,
                          objects,
                          output_libname,
                          output_dir=None,
                          debug=0,
                          target_lang=None):
        """
        '/OUT:'
        """
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,750 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,751 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,751 : INFO : tokenize_signature : --> do i ever get here?
    def link(self,
             target_desc,
             objects,
             output_filename,
             output_dir=None,
             libraries=None,
             library_dirs=None,
             runtime_library_dirs=None,
             export_symbols=None,
             debug=0,
             extra_preargs=None,
             extra_postargs=None,
             build_temp=None,
             target_lang=None):
        """
        I don't know what to do with 'runtime_library_dirs': 

        """
    def spawn(self, cmd):
        """
        'path'
        """
    def library_dir_option(self, dir):
        """
        /LIBPATH:
        """
    def runtime_library_dir_option(self, dir):
        """
        don't know how to set runtime library search path for MSVC
        """
    def library_option(self, lib):
        """
         Prefer a debugging library if found (and requested), but deal
         with it if we don't have one.

        """
