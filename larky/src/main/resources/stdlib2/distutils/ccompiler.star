def CCompiler:
    """
    Abstract base class to define the interface that must be implemented
        by real compiler classes.  Also has some utility methods used by
        several compiler classes.

        The basic idea behind a compiler abstraction class is that each
        instance can be used for all the compile/link steps in building a
        single project.  Thus, attributes common to all of those compile and
        link steps -- include directories, macros to define, libraries to link
        against, etc. -- are attributes of the compiler instance.  To allow for
        variability in how individual files are treated, most of those
        attributes may be varied on a per-compilation or per-link basis.
    
    """
    def __init__(self, verbose=0, dry_run=0, force=0):
        """
         'output_dir': a common output directory for object, library,
         shared object, and shared library files

        """
    def set_executables(self, **kwargs):
        """
        Define the executables (and options for them) that will be run
                to perform the various stages of compilation.  The exact set of
                executables that may be specified here depends on the compiler
                class (via the 'executables' class attribute), but most will have:
                  compiler      the C/C++ compiler
                  linker_so     linker used to create shared objects and libraries
                  linker_exe    linker used to create binary executables
                  archiver      static library creator

                On platforms with a command-line (Unix, DOS/Windows), each of these
                is a string that will be split into executable name and (optional)
                list of arguments.  (Splitting the string is done similarly to how
                Unix shells operate: words are delimited by spaces, but quotes and
                backslashes can override this.  See
                'distutils.util.split_quoted()'.)
        
        """
    def set_executable(self, key, value):
        """
        Ensures that every element of 'definitions' is a valid macro
                definition, ie. either (name,value) 2-tuple or a (name,) tuple.  Do
                nothing if all definitions are OK, raise TypeError otherwise.
        
        """
    def define_macro(self, name, value=None):
        """
        Define a preprocessor macro for all compilations driven by this
                compiler object.  The optional parameter 'value' should be a
                string; if it is not supplied, then the macro will be defined
                without an explicit value and the exact outcome depends on the
                compiler used (XXX true? does ANSI say anything about this?)
        
        """
    def undefine_macro(self, name):
        """
        Undefine a preprocessor macro for all compilations driven by
                this compiler object.  If the same macro is defined by
                'define_macro()' and undefined by 'undefine_macro()' the last call
                takes precedence (including multiple redefinitions or
                undefinitions).  If the macro is redefined/undefined on a
                per-compilation basis (ie. in the call to 'compile()'), then that
                takes precedence.
        
        """
    def add_include_dir(self, dir):
        """
        Add 'dir' to the list of directories that will be searched for
                header files.  The compiler is instructed to search directories in
                the order in which they are supplied by successive calls to
                'add_include_dir()'.
        
        """
    def set_include_dirs(self, dirs):
        """
        Set the list of directories that will be searched to 'dirs' (a
                list of strings).  Overrides any preceding calls to
                'add_include_dir()'; subsequence calls to 'add_include_dir()' add
                to the list passed to 'set_include_dirs()'.  This does not affect
                any list of standard include directories that the compiler may
                search by default.
        
        """
    def add_library(self, libname):
        """
        Add 'libname' to the list of libraries that will be included in
                all links driven by this compiler object.  Note that 'libname'
                should *not* be the name of a file containing a library, but the
                name of the library itself: the actual filename will be inferred by
                the linker, the compiler, or the compiler class (depending on the
                platform).

                The linker will be instructed to link against libraries in the
                order they were supplied to 'add_library()' and/or
                'set_libraries()'.  It is perfectly valid to duplicate library
                names; the linker will be instructed to link against libraries as
                many times as they are mentioned.
        
        """
    def set_libraries(self, libnames):
        """
        Set the list of libraries to be included in all links driven by
                this compiler object to 'libnames' (a list of strings).  This does
                not affect any standard system libraries that the linker may
                include by default.
        
        """
    def add_library_dir(self, dir):
        """
        Add 'dir' to the list of directories that will be searched for
                libraries specified to 'add_library()' and 'set_libraries()'.  The
                linker will be instructed to search for libraries in the order they
                are supplied to 'add_library_dir()' and/or 'set_library_dirs()'.
        
        """
    def set_library_dirs(self, dirs):
        """
        Set the list of library search directories to 'dirs' (a list of
                strings).  This does not affect any standard library search path
                that the linker may search by default.
        
        """
    def add_runtime_library_dir(self, dir):
        """
        Add 'dir' to the list of directories that will be searched for
                shared libraries at runtime.
        
        """
    def set_runtime_library_dirs(self, dirs):
        """
        Set the list of directories to search for shared libraries at
                runtime to 'dirs' (a list of strings).  This does not affect any
                standard search path that the runtime linker may search by
                default.
        
        """
    def add_link_object(self, object):
        """
        Add 'object' to the list of object files (or analogues, such as
                explicitly named library files or the output of "resource
                compilers") to be included in every link driven by this compiler
                object.
        
        """
    def set_link_objects(self, objects):
        """
        Set the list of object files (or analogues) to be included in
                every link to 'objects'.  This does not affect any standard object
                files that the linker may include by default (such as system
                libraries).
        
        """
2021-03-02 20:46:27,318 : INFO : tokenize_signature : --> do i ever get here?
    def _setup_compile(self, outdir, macros, incdirs, sources, depends,
                       extra):
        """
        Process arguments and decide which source files to compile.
        """
    def _get_cc_args(self, pp_opts, debug, before):
        """
         works for unixccompiler, cygwinccompiler

        """
    def _fix_compile_args(self, output_dir, macros, include_dirs):
        """
        Typecheck and fix-up some of the arguments to the 'compile()'
                method, and return fixed-up values.  Specifically: if 'output_dir'
                is None, replaces it with 'self.output_dir'; ensures that 'macros'
                is a list, and augments it with 'self.macros'; ensures that
                'include_dirs' is a list, and augments it with 'self.include_dirs'.
                Guarantees that the returned values are of the correct type,
                i.e. for 'output_dir' either string or None, and for 'macros' and
                'include_dirs' either list or None.
        
        """
    def _prep_compile(self, sources, output_dir, depends=None):
        """
        Decide which souce files must be recompiled.

                Determine the list of object files corresponding to 'sources',
                and figure out which ones really need to be recompiled.
                Return a list of all object files and a dictionary telling
                which source files can be skipped.
        
        """
    def _fix_object_args(self, objects, output_dir):
        """
        Typecheck and fix up some arguments supplied to various methods.
                Specifically: ensure that 'objects' is a list; if output_dir is
                None, replace with self.output_dir.  Return fixed versions of
                'objects' and 'output_dir'.
        
        """
    def _fix_lib_args(self, libraries, library_dirs, runtime_library_dirs):
        """
        Typecheck and fix up some of the arguments supplied to the
                'link_*' methods.  Specifically: ensure that all arguments are
                lists, and augment them with their permanent versions
                (eg. 'self.libraries' augments 'libraries').  Return a tuple with
                fixed versions of all arguments.
        
        """
    def _need_link(self, objects, output_file):
        """
        Return true if we need to relink the files listed in 'objects'
                to recreate 'output_file'.
        
        """
    def detect_language(self, sources):
        """
        Detect the language of a given file, or list of files. Uses
                language_map, and language_order to do the job.
        
        """
2021-03-02 20:46:27,324 : INFO : tokenize_signature : --> do i ever get here?
    def preprocess(self, source, output_file=None, macros=None,
                   include_dirs=None, extra_preargs=None, extra_postargs=None):
        """
        Preprocess a single C/C++ source file, named in 'source'.
                Output will be written to file named 'output_file', or stdout if
                'output_file' not supplied.  'macros' is a list of macro
                definitions as for 'compile()', which will augment the macros set
                with 'define_macro()' and 'undefine_macro()'.  'include_dirs' is a
                list of directory names that will be added to the default list.

                Raises PreprocessError on failure.
        
        """
2021-03-02 20:46:27,325 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,325 : INFO : tokenize_signature : --> do i ever get here?
    def compile(self, sources, output_dir=None, macros=None,
                include_dirs=None, debug=0, extra_preargs=None,
                extra_postargs=None, depends=None):
        """
        Compile one or more source files.

                'sources' must be a list of filenames, most likely C/C++
                files, but in reality anything that can be handled by a
                particular compiler and compiler class (eg. MSVCCompiler can
                handle resource files in 'sources').  Return a list of object
                filenames, one per source filename in 'sources'.  Depending on
                the implementation, not all source files will necessarily be
                compiled, but all corresponding object filenames will be
                returned.

                If 'output_dir' is given, object files will be put under it, while
                retaining their original path component.  That is, "foo/bar.c"
                normally compiles to "foo/bar.o" (for a Unix implementation); if
                'output_dir' is "build", then it would compile to
                "build/foo/bar.o".

                'macros', if given, must be a list of macro definitions.  A macro
                definition is either a (name, value) 2-tuple or a (name,) 1-tuple.
                The former defines a macro; if the value is None, the macro is
                defined without an explicit value.  The 1-tuple case undefines a
                macro.  Later definitions/redefinitions/ undefinitions take
                precedence.

                'include_dirs', if given, must be a list of strings, the
                directories to add to the default include file search path for this
                compilation only.

                'debug' is a boolean; if true, the compiler will be instructed to
                output debug symbols in (or alongside) the object file(s).

                'extra_preargs' and 'extra_postargs' are implementation- dependent.
                On platforms that have the notion of a command-line (e.g. Unix,
                DOS/Windows), they are most likely lists of strings: extra
                command-line arguments to prepend/append to the compiler command
                line.  On other platforms, consult the implementation class
                documentation.  In any event, they are intended as an escape hatch
                for those occasions when the abstract compiler framework doesn't
                cut the mustard.

                'depends', if given, is a list of filenames that all targets
                depend on.  If a source file is older than any file in
                depends, then the source file will be recompiled.  This
                supports dependency tracking, but only at a coarse
                granularity.

                Raises CompileError on failure.
        
        """
    def _compile(self, obj, src, ext, cc_args, extra_postargs, pp_opts):
        """
        Compile 'src' to product 'obj'.
        """
2021-03-02 20:46:27,327 : INFO : tokenize_signature : --> do i ever get here?
    def create_static_lib(self, objects, output_libname, output_dir=None,
                          debug=0, target_lang=None):
        """
        Link a bunch of stuff together to create a static library file.
                The "bunch of stuff" consists of the list of object files supplied
                as 'objects', the extra object files supplied to
                'add_link_object()' and/or 'set_link_objects()', the libraries
                supplied to 'add_library()' and/or 'set_libraries()', and the
                libraries supplied as 'libraries' (if any).

                'output_libname' should be a library name, not a filename; the
                filename will be inferred from the library name.  'output_dir' is
                the directory where the library file will be put.

                'debug' is a boolean; if true, debugging information will be
                included in the library (note that on most platforms, it is the
                compile step where this matters: the 'debug' flag is included here
                just for consistency).

                'target_lang' is the target language for which the given objects
                are being compiled. This allows specific linkage time treatment of
                certain languages.

                Raises LibError on failure.
        
        """
2021-03-02 20:46:27,328 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,328 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,328 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,328 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,328 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,328 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,328 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,329 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,329 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,329 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,329 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,329 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,329 : INFO : tokenize_signature : --> do i ever get here?
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
        Link a bunch of stuff together to create an executable or
                shared library file.

                The "bunch of stuff" consists of the list of object files supplied
                as 'objects'.  'output_filename' should be a filename.  If
                'output_dir' is supplied, 'output_filename' is relative to it
                (i.e. 'output_filename' can provide directory components if
                needed).

                'libraries' is a list of libraries to link against.  These are
                library names, not filenames, since they're translated into
                filenames in a platform-specific way (eg. "foo" becomes "libfoo.a"
                on Unix and "foo.lib" on DOS/Windows).  However, they can include a
                directory component, which means the linker will look in that
                specific directory rather than searching all the normal locations.

                'library_dirs', if supplied, should be a list of directories to
                search for libraries that were specified as bare library names
                (ie. no directory component).  These are on top of the system
                default and those supplied to 'add_library_dir()' and/or
                'set_library_dirs()'.  'runtime_library_dirs' is a list of
                directories that will be embedded into the shared library and used
                to search for other shared libraries that *it* depends on at
                run-time.  (This may only be relevant on Unix.)

                'export_symbols' is a list of symbols that the shared library will
                export.  (This appears to be relevant only on Windows.)

                'debug' is as for 'compile()' and 'create_static_lib()', with the
                slight distinction that it actually matters on most platforms (as
                opposed to 'create_static_lib()', which includes a 'debug' flag
                mostly for form's sake).

                'extra_preargs' and 'extra_postargs' are as for 'compile()' (except
                of course that they supply command-line arguments for the
                particular linker being used).

                'target_lang' is the target language for which the given objects
                are being compiled. This allows specific linkage time treatment of
                certain languages.

                Raises LinkError on failure.
        
        """
2021-03-02 20:46:27,330 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,330 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,330 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,330 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,330 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,330 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,330 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,331 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,331 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,331 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,331 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,331 : INFO : tokenize_signature : --> do i ever get here?
    def link_shared_lib(self,
                        objects,
                        output_libname,
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
        'shared'
        """
2021-03-02 20:46:27,331 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,332 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,332 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,332 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,332 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,332 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,332 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,332 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,332 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,332 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,333 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:27,333 : INFO : tokenize_signature : --> do i ever get here?
    def link_shared_object(self,
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
         -- Miscellaneous methods -----------------------------------------
         These are all used by the 'gen_lib_options() function; there is
         no appropriate default implementation so subclasses should
         implement all of these.


        """
    def library_dir_option(self, dir):
        """
        Return the compiler option to add 'dir' to the list of
                directories searched for libraries.
        
        """
    def runtime_library_dir_option(self, dir):
        """
        Return the compiler option to add 'dir' to the list of
                directories searched for runtime libraries.
        
        """
    def library_option(self, lib):
        """
        Return the compiler option to add 'lib' to the list of libraries
                linked into the shared library or executable.
        
        """
2021-03-02 20:46:27,335 : INFO : tokenize_signature : --> do i ever get here?
    def has_function(self, funcname, includes=None, include_dirs=None,
                     libraries=None, library_dirs=None):
        """
        Return a boolean indicating whether funcname is supported on
                the current platform.  The optional arguments can be used to
                augment the compilation environment.
        
        """
    def find_library_file (self, dirs, lib, debug=0):
        """
        Search the specified list of directories for a static or shared
                library file 'lib' and return the full path to that file.  If
                'debug' true, look for a debugging version (if that makes sense on
                the current platform).  Return None if 'lib' wasn't found in any of
                the specified directories.
        
        """
    def object_filenames(self, source_filenames, strip_dir=0, output_dir=''):
        """
        ''
        """
    def shared_object_filename(self, basename, strip_dir=0, output_dir=''):
        """
        ''
        """
2021-03-02 20:46:27,339 : INFO : tokenize_signature : --> do i ever get here?
    def library_filename(self, libname, lib_type='static',     # or 'shared'
                         strip_dir=0, output_dir=''):
        """
        static
        """
    def announce(self, msg, level=1):
        """
        warning: %s\n
        """
    def execute(self, func, args, msg=None, level=1):
        """
         Map a sys.platform/os.name ('posix', 'nt') to the default compiler
         type for that platform. Keys are interpreted as re match
         patterns. Order is important; platform mappings are preferred over
         OS names.

        """
def get_default_compiler(osname=None, platform=None):
    """
    Determine the default compiler to use for the given platform.

           osname should be one of the standard Python OS names (i.e. the
           ones returned by os.name) and platform the common value
           returned by sys.platform for the platform in question.

           The default values are os.name and sys.platform in case the
           parameters are not given.
    
    """
def show_compilers():
    """
    Print list of available compilers (used by the "--help-compiler"
        options to "build", "build_ext", "build_clib").
    
    """
def new_compiler(plat=None, compiler=None, verbose=0, dry_run=0, force=0):
    """
    Generate an instance of some CCompiler subclass for the supplied
        platform/compiler combination.  'plat' defaults to 'os.name'
        (eg. 'posix', 'nt'), and 'compiler' defaults to the default compiler
        for that platform.  Currently only 'posix' and 'nt' are supported, and
        the default compilers are "traditional Unix interface" (UnixCCompiler
        class) and Visual C++ (MSVCCompiler class).  Note that it's perfectly
        possible to ask for a Unix compiler object under Windows, and a
        Microsoft compiler object under Unix -- if you supply a value for
        'compiler', 'plat' is ignored.
    
    """
def gen_preprocess_options(macros, include_dirs):
    """
    Generate C pre-processor options (-D, -U, -I) as used by at least
        two types of compilers: the typical Unix compiler and Visual C++.
        'macros' is the usual thing, a list of 1- or 2-tuples, where (name,)
        means undefine (-U) macro 'name', and (name,value) means define (-D)
        macro 'name' to 'value'.  'include_dirs' is just a list of directory
        names to be added to the header file search path (-I).  Returns a list
        of command-line options suitable for either Unix compilers or Visual
        C++.
    
    """
def gen_lib_options (compiler, library_dirs, runtime_library_dirs, libraries):
    """
    Generate linker options for searching library directories and
        linking with specific libraries.  'libraries' and 'library_dirs' are,
        respectively, lists of library names (not filenames!) and search
        directories.  Returns a list of command-line options suitable for use
        with some compiler (depending on the two format strings passed in).
    
    """
