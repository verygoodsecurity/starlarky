    def _get_build_version():
        """
        Return the version of MSVC that was used to build Python.

                For Python 2.3 and up, the version number is included in
                sys.version.  For earlier versions, assume the compiler is MSVC 6.
        
        """
    def find_msvcrt():
        """
        Return the name of the VC runtime dll
        """
    def find_library(name):
        """
        'c'
        """
    def find_library(name):
        """
        'lib%s.dylib'
        """
    def _findLib_gcc(name):
        """
         Run GCC's linker with the -t (aka --trace) option and examine the
         library name it prints out. The GCC command will fail because we
         haven't supplied a proper program with main(), but that does not
         matter.

        """
        def _get_soname(f):
            """
            /usr/ccs/bin/dump
            """
        def _get_soname(f):
            """
             assuming GNU binutils / ELF

            """
        def _num_version(libname):
            """
             "libxyz.so.MAJOR.MINOR" => [ MAJOR, MINOR ]

            """
        def find_library(name):
            """
            r':-l%s\.\S+ => \S*/(lib%s\.\S+)'
            """
        def _findLib_crle(name, is64):
            """
            '/usr/bin/crle'
            """
        def find_library(name, is64 = False):
            """
            'l'
            """
        def _findLib_ld(name):
            """
             See issue #9998 for why this is needed

            """
        def find_library(name):
            """
             See issue #9998

            """
def test():
    """
    nt
    """
