def Extension:
    """
    Just a collection of attributes that describes an extension
        module and everything needed to build it (hopefully in a portable
        way, but there are hooks that let you be as unportable as you need).

        Instance attributes:
          name : string
            the full name of the extension, including any packages -- ie.
            *not* a filename or pathname, but Python dotted name
          sources : [string]
            list of source filenames, relative to the distribution root
            (where the setup script lives), in Unix form (slash-separated)
            for portability.  Source files may be C, C++, SWIG (.i),
            platform-specific resource files, or whatever else is recognized
            by the "build_ext" command as source for a Python extension.
          include_dirs : [string]
            list of directories to search for C/C++ header files (in Unix
            form for portability)
          define_macros : [(name : string, value : string|None)]
            list of macros to define; each macro is defined using a 2-tuple,
            where 'value' is either the string to define it to or None to
            define it without a particular value (equivalent of "#define
            FOO" in source or -DFOO on Unix C compiler command line)
          undef_macros : [string]
            list of macros to undefine explicitly
          library_dirs : [string]
            list of directories to search for C/C++ libraries at link time
          libraries : [string]
            list of library names (not filenames or paths) to link against
          runtime_library_dirs : [string]
            list of directories to search for C/C++ libraries at run time
            (for shared extensions, this is when the extension is loaded)
          extra_objects : [string]
            list of extra files to link with (eg. object files not implied
            by 'sources', static library that must be explicitly specified,
            binary resource files, etc.)
          extra_compile_args : [string]
            any extra platform- and compiler-specific information to use
            when compiling the source files in 'sources'.  For platforms and
            compilers where "command line" makes sense, this is typically a
            list of command-line arguments, but for other platforms it could
            be anything.
          extra_link_args : [string]
            any extra platform- and compiler-specific information to use
            when linking object files together to create the extension (or
            to create a new static Python interpreter).  Similar
            interpretation as for 'extra_compile_args'.
          export_symbols : [string]
            list of symbols to be exported from a shared extension.  Not
            used on all platforms, and not generally necessary for Python
            extensions, which typically export exactly one symbol: "init" +
            extension_name.
          swig_opts : [string]
            any extra options to pass to SWIG if a source file has the .i
            extension.
          depends : [string]
            list of files that the extension depends on
          language : string
            extension language (i.e. "c", "c++", "objc"). Will be detected
            from the source extensions if not provided.
          optional : boolean
            specifies that a build failure in the extension should not abort the
            build process, but simply not install the failing extension.
    
    """
2021-03-02 20:46:29,933 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,933 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,934 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,934 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,934 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,934 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,934 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,934 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,934 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,934 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,935 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,935 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,935 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,935 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,935 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:29,935 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, name, sources,
                  include_dirs=None,
                  define_macros=None,
                  undef_macros=None,
                  library_dirs=None,
                  libraries=None,
                  runtime_library_dirs=None,
                  extra_objects=None,
                  extra_compile_args=None,
                  extra_link_args=None,
                  export_symbols=None,
                  swig_opts = None,
                  depends=None,
                  language=None,
                  optional=None,
                  **kw                      # To catch unknown keywords
                 ):
        """
        'name' must be a string
        """
    def __repr__(self):
        """
        '<%s.%s(%r) at %#x>'
        """
def read_setup_file(filename):
    """
    Reads a Setup file and returns Extension instances.
    """
