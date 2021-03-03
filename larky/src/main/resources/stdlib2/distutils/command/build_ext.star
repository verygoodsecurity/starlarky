def show_compilers ():
    """
    build C/C++ extensions (compile/link to build directory)
    """
    def initialize_options(self):
        """
        'build'
        """
    def run(self):
        """
         'self.extensions', as supplied by setup.py, is a list of
         Extension instances.  See the documentation for Extension (in
         distutils.extension) for details.

         For backwards compatibility with Distutils 0.8.2 and earlier, we
         also allow the 'extensions' list to be a list of tuples:
            (ext_name, build_info)
         where build_info is a dictionary containing everything that
         Extension instances do except the name, with a few things being
         differently named.  We convert these 2-tuples to Extension
         instances as needed.


        """
    def check_extensions_list(self, extensions):
        """
        Ensure that the list of extensions (presumably provided as a
                command option 'extensions') is valid, i.e. it is a list of
                Extension objects.  We also support the old-style list of 2-tuples,
                where the tuples are (ext_name, build_info), which are converted to
                Extension instances here.

                Raise DistutilsSetupError if the structure is invalid anywhere;
                just returns otherwise.
        
        """
    def get_source_files(self):
        """
         Wouldn't it be neat if we knew the names of header files too...

        """
    def get_outputs(self):
        """
         Sanity check the 'extensions' list -- can't assume this is being
         done in the same run as a 'build_extensions()' call (in fact, we
         can probably assume that it *isn't*!).

        """
    def build_extensions(self):
        """
         First, sanity-check the 'extensions' list

        """
    def _build_extensions_parallel(self):
        """
         may return None
        """
    def _build_extensions_serial(self):
        """
        'building extension "%s" failed: %s'
        """
    def build_extension(self, ext):
        """
        in 'ext_modules' option (extension '%s'), 
        'sources' must be present and must be 
        a list of source filenames
        """
    def swig_sources(self, sources, extension):
        """
        Walk the list of source files in 'sources', looking for SWIG
                interface (.i) files.  Run SWIG on all that are found, and
                return a modified 'sources' list with SWIG source files replaced
                by the generated C (or C++) files.
        
        """
    def find_swig(self):
        """
        Return the name of the SWIG executable.  On Unix, this is
                just "swig" -- it should be in the PATH.  Tries a bit harder on
                Windows.
        
        """
    def get_ext_fullpath(self, ext_name):
        """
        Returns the path of the filename for a given extension.

                The file is located in `build_lib` or directly in the package
                (inplace option).
        
        """
    def get_ext_fullname(self, ext_name):
        """
        Returns the fullname of a given extension name.

                Adds the `package.` prefix
        """
    def get_ext_filename(self, ext_name):
        """
        r"""Convert the name of an extension (eg. "foo.bar") into the name
                of the file from which it will be loaded (eg. "foo/bar.so", or
                "foo\bar.pyd").
        
        """
    def get_export_symbols(self, ext):
        """
        Return the list of symbols that a shared extension has to
                export.  This either uses 'ext.export_symbols' or, if it's not
                provided, "PyInit_" + module_name.  Only relevant on Windows, where
                the .pyd file (DLL) must export the module "PyInit_" function.
        
        """
    def get_libraries(self, ext):
        """
        Return the list of libraries to link against when building a
                shared extension.  On most platforms, this is just 'ext.libraries';
                on Windows, we add the Python library (eg. python20.dll).
        
        """
