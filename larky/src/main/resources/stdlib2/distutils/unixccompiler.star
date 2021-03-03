def UnixCCompiler(CCompiler):
    """
    'unix'
    """
2021-03-02 20:46:26,930 : INFO : tokenize_signature : --> do i ever get here?
    def preprocess(self, source, output_file=None, macros=None,
                   include_dirs=None, extra_preargs=None, extra_postargs=None):
        """
        '-o'
        """
    def _compile(self, obj, src, ext, cc_args, extra_postargs, pp_opts):
        """
        'darwin'
        """
2021-03-02 20:46:26,932 : INFO : tokenize_signature : --> do i ever get here?
    def create_static_lib(self, objects, output_libname,
                          output_dir=None, debug=0, target_lang=None):
        """
         Not many Unices required ranlib anymore -- SunOS 4.x is, I
         think the only major Unix that does.  Maybe we need some
         platform intelligence here to skip ranlib if it's not
         needed -- or maybe Python's configure script took care of
         it for us, hence the check for leading colon.

        """
2021-03-02 20:46:26,933 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,933 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,933 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:26,933 : INFO : tokenize_signature : --> do i ever get here?
    def link(self, target_desc, objects,
             output_filename, output_dir=None, libraries=None,
             library_dirs=None, runtime_library_dirs=None,
             export_symbols=None, debug=0, extra_preargs=None,
             extra_postargs=None, build_temp=None, target_lang=None):
        """
        'output_dir' must be a string or None
        """
    def library_dir_option(self, dir):
        """
        -L
        """
    def _is_gcc(self, compiler_name):
        """
        gcc
        """
    def runtime_library_dir_option(self, dir):
        """
         XXX Hackish, at the very least.  See Python bug #445902:
         http://sourceforge.net/tracker/index.php
           ?func=detail&aid=445902&group_id=5470&atid=105470
         Linkers on different platforms need different options to
         specify that directories need to be added to the list of
         directories searched for dependencies when a dynamic library
         is sought.  GCC on GNU systems (Linux, FreeBSD, ...) has to
         be told to pass the -R option through to the linker, whereas
         other compilers and gcc on other systems just know this.
         Other compilers may need something slightly different.  At
         this time, there's no way to determine this information from
         the configuration data stored in the Python installation, so
         we use this hack.

        """
    def library_option(self, lib):
        """
        -l
        """
    def find_library_file(self, dirs, lib, debug=0):
        """
        'shared'
        """
