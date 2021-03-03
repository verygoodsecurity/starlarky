def read_keys(base, key):
    """
    Return list of registry keys.
    """
def read_values(base, key):
    """
    Return dict of registry keys and values.

        All names are converted to lowercase.
    
    """
def convert_mbcs(s):
    """
    decode
    """
def MacroExpander:
    """
    $(%s)
    """
    def load_macros(self, version):
        """
        r"Software\Microsoft\VisualStudio\%0.1f
        """
    def sub(self, s):
        """
        Return the version of MSVC that was used to build Python.

            For Python 2.3 and up, the version number is included in
            sys.version.  For earlier versions, assume the compiler is MSVC 6.
    
        """
def get_build_architecture():
    """
    Return the processor architecture.

        Possible results are "Intel" or "AMD64".
    
    """
def normalize_and_reduce_paths(paths):
    """
    Return a list of normalized paths with duplicates removed.

        The current order of paths is maintained.
    
    """
def MSVCCompiler(CCompiler) :
    """
    Concrete class that implements an interface to Microsoft Visual C++,
           as defined by the CCompiler abstract class.
    """
    def __init__(self, verbose=0, dry_run=0, force=0):
        """
        Intel
        """
    def initialize(self):
        """
        DISTUTILS_USE_SDK
        """
2021-03-02 20:46:30,661 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,661 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,661 : INFO : tokenize_signature : --> do i ever get here?
    def object_filenames(self,
                         source_filenames,
                         strip_dir=0,
                         output_dir=''):
        """
         Copied from ccompiler.py, extended to return .res as 'object'-file
         for .rc input file

        """
2021-03-02 20:46:30,663 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,663 : INFO : tokenize_signature : --> do i ever get here?
    def compile(self, sources,
                output_dir=None, macros=None, include_dirs=None, debug=0,
                extra_preargs=None, extra_postargs=None, depends=None):
        """
        '/c'
        """
2021-03-02 20:46:30,665 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,665 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,665 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,665 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,665 : INFO : tokenize_signature : --> do i ever get here?
    def create_static_lib(self,
                          objects,
                          output_libname,
                          output_dir=None,
                          debug=0,
                          target_lang=None):
        """
        '/OUT:'
        """
2021-03-02 20:46:30,666 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,666 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,666 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,666 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,666 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,666 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,666 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,667 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,667 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,667 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,667 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,667 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:30,667 : INFO : tokenize_signature : --> do i ever get here?
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
    def library_dir_option(self, dir):
        """
        /LIBPATH:
        """
    def runtime_library_dir_option(self, dir):
        """
        don't know how to set runtime library search path for MSVC++
        """
    def library_option(self, lib):
        """
         Prefer a debugging library if found (and requested), but deal
         with it if we don't have one.

        """
    def find_exe(self, exe):
        """
        Return path to an MSVC executable program.

                Tries to find the program in several places: first, one of the
                MSVC program search paths from the registry; next, the directories
                in the PATH environment variable.  If any of those work, return an
                absolute path that is known to exist.  If none of them work, just
                return the original program name, 'exe'.
        
        """
    def get_msvc_paths(self, path, platform='x86'):
        """
        Get a list of devstudio directories (include, lib or path).

                Return a list of strings.  The list will be empty if unable to
                access the registry or appropriate registry keys not found.
        
        """
    def set_path_env_var(self, name):
        """
        Set environment variable 'name' to an MSVC path type value.

                This is equivalent to a SET command prior to execution of spawned
                commands.
        
        """
