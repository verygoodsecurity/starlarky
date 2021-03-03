def BCPPCompiler(CCompiler) :
    """
    Concrete class that implements an interface to the Borland C/C++
        compiler, as defined by the CCompiler abstract class.
    
    """
2021-03-02 20:46:35,813 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,813 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,813 : INFO : tokenize_signature : --> do i ever get here?
    def __init__ (self,
                  verbose=0,
                  dry_run=0,
                  force=0):
        """
         These executables are assumed to all be in the path.
         Borland doesn't seem to use any special registry settings to
         indicate their installation locations.


        """
2021-03-02 20:46:35,814 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,814 : INFO : tokenize_signature : --> do i ever get here?
    def compile(self, sources,
                output_dir=None, macros=None, include_dirs=None, debug=0,
                extra_preargs=None, extra_postargs=None, depends=None):
        """
        '-c'
        """
2021-03-02 20:46:35,816 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,816 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
    def create_static_lib (self,
                           objects,
                           output_libname,
                           output_dir=None,
                           debug=0,
                           target_lang=None):
        """
        '/u'
        """
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,817 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,818 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,818 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,818 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,818 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,818 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,818 : INFO : tokenize_signature : --> do i ever get here?
    def link (self,
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
         XXX this ignores 'build_temp'!  should follow the lead of
         msvccompiler.py


        """
    def find_library_file (self, dirs, lib, debug=0):
        """
         List of effective library names to try, in order of preference:
         xxx_bcpp.lib is better than xxx.lib
         and xxx_d.lib is better than xxx.lib if debug is set

         The "_bcpp" suffix is to handle a Python installation for people
         with multiple compilers (primarily Distutils hackers, I suspect
         ;-).  The idea is they'd have one static library for each
         compiler they care about, since (almost?) every Windows compiler
         seems to have a different format for static libraries.

        """
2021-03-02 20:46:35,821 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,821 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,821 : INFO : tokenize_signature : --> do i ever get here?
    def object_filenames (self,
                          source_filenames,
                          strip_dir=0,
                          output_dir=''):
        """
        ''
        """
2021-03-02 20:46:35,822 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,822 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,822 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,822 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,822 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:35,822 : INFO : tokenize_signature : --> do i ever get here?
    def preprocess (self,
                    source,
                    output_file=None,
                    macros=None,
                    include_dirs=None,
                    extra_preargs=None,
                    extra_postargs=None):
        """
        'cpp32.exe'
        """
