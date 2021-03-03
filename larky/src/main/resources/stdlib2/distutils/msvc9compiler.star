def Reg:
    """
    Helper class to read values from the registry
    
    """
    def get_value(cls, path, key):
        """
        Return list of registry keys.
        """
    def read_values(cls, base, key):
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
        VCInstallDir
        """
    def sub(self, s):
        """
        Return the version of MSVC that was used to build Python.

            For Python 2.3 and up, the version number is included in
            sys.version.  For earlier versions, assume the compiler is MSVC 6.
    
        """
def normalize_and_reduce_paths(paths):
    """
    Return a list of normalized paths with duplicates removed.

        The current order of paths is maintained.
    
    """
def removeDuplicates(variable):
    """
    Remove duplicate values of an environment variable.
    
    """
def find_vcvarsall(version):
    """
    Find the vcvarsall.bat file

        At first it tries to find the productdir of VS 2008 in the registry. If
        that fails it falls back to the VS90COMNTOOLS env var.
    
    """
def query_vcvarsall(version, arch="x86"):
    """
    Launch vcvarsall.bat and read the settings from its environment
    
    """
def MSVCCompiler(CCompiler) :
    """
    Concrete class that implements an interface to Microsoft Visual C++,
           as defined by the CCompiler abstract class.
    """
    def __init__(self, verbose=0, dry_run=0, force=0):
        """
        r"Software\Microsoft\VisualStudio
        """
    def initialize(self, plat_name=None):
        """
         multi-init means we would need to check platform same each time...

        """
2021-03-02 20:46:27,563 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,564 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,564 : INFO : tokenize_signature : --> do i ever get here?
    def object_filenames(self,
                         source_filenames,
                         strip_dir=0,
                         output_dir=''):
        """
         Copied from ccompiler.py, extended to return .res as 'object'-file
         for .rc input file

        """
2021-03-02 20:46:27,565 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,565 : INFO : tokenize_signature : --> do i ever get here?
    def compile(self, sources,
                output_dir=None, macros=None, include_dirs=None, debug=0,
                extra_preargs=None, extra_postargs=None, depends=None):
        """
        '/c'
        """
2021-03-02 20:46:27,567 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,567 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,567 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,567 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,567 : INFO : tokenize_signature : --> do i ever get here?
    def create_static_lib(self,
                          objects,
                          output_libname,
                          output_dir=None,
                          debug=0,
                          target_lang=None):
        """
        '/OUT:'
        """
2021-03-02 20:46:27,567 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,567 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,567 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,567 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,568 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,568 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,568 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,568 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,568 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,568 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,568 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,568 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,568 : INFO : tokenize_signature : --> do i ever get here?
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
    def manifest_setup_ldargs(self, output_filename, build_temp, ld_args):
        """
         If we need a manifest at all, an embedded manifest is recommended.
         See MSDN article titled
         "How to: Embed a Manifest Inside a C/C++ Application
         (currently at http://msdn2.microsoft.com/en-us/library/ms235591(VS.80).aspx)
         Ask the linker to generate the manifest in the temp dir, so
         we can check it, and possibly embed it, later.

        """
    def manifest_get_embed_info(self, target_desc, ld_args):
        """
         If a manifest should be embedded, return a tuple of
         (manifest_filename, resource_id).  Returns None if no manifest
         should be embedded.  See http://bugs.python.org/issue7833 for why
         we want to avoid any manifest for extension modules if we can)

        """
    def _remove_visual_c_ref(self, manifest_file):
        """
         Remove references to the Visual C runtime, so they will
         fall through to the Visual C dependency of Python.exe.
         This way, when installed for a restricted user (e.g.
         runtimes are not in WinSxS folder, but in Python's own
         folder), the runtimes do not need to be in every folder
         with .pyd's.
         Returns either the filename of the modified manifest or
         None if no manifest should be embedded.

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
