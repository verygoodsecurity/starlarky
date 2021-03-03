def config(Command):
    """
    prepare to build
    """
    def initialize_options(self):
        """
         maximal output for now

        """
    def finalize_options(self):
        """
         Utility methods for actual "config" commands.  The interfaces are
         loosely based on Autoconf macros of similar names.  Sub-classes
         may use these freely.


        """
    def _check_compiler(self):
        """
        Check that 'self.compiler' really is a CCompiler object;
                if not, make it one.
        
        """
    def _gen_temp_sourcefile(self, body, headers, lang):
        """
        _configtest
        """
    def _preprocess(self, body, headers, include_dirs, lang):
        """
        _configtest.i
        """
    def _compile(self, body, headers, include_dirs, lang):
        """
        compiling '%s':
        """
2021-03-02 20:46:31,563 : INFO : tokenize_signature : --> do i ever get here?
    def _link(self, body, headers, include_dirs, libraries, library_dirs,
              lang):
        """
        removing: %s
        """
    def try_cpp(self, body=None, headers=None, include_dirs=None, lang="c"):
        """
        Construct a source file from 'body' (a string containing lines
                of C/C++ code) and 'headers' (a list of header files to include)
                and run it through the preprocessor.  Return true if the
                preprocessor succeeded, false if there were any errors.
                ('body' probably isn't of much use, but what the heck.)
        
        """
2021-03-02 20:46:31,564 : INFO : tokenize_signature : --> do i ever get here?
    def search_cpp(self, pattern, body=None, headers=None, include_dirs=None,
                   lang="c"):
        """
        Construct a source file (just like 'try_cpp()'), run it through
                the preprocessor, and return true if any line of the output matches
                'pattern'.  'pattern' should either be a compiled regex object or a
                string containing a regex.  If both 'body' and 'headers' are None,
                preprocesses an empty file -- which can be useful to determine the
                symbols the preprocessor and compiler set by default.
        
        """
    def try_compile(self, body, headers=None, include_dirs=None, lang="c"):
        """
        Try to compile a source file built from 'body' and 'headers'.
                Return true on success, false otherwise.
        
        """
2021-03-02 20:46:31,565 : INFO : tokenize_signature : --> do i ever get here?
    def try_link(self, body, headers=None, include_dirs=None, libraries=None,
                 library_dirs=None, lang="c"):
        """
        Try to compile and link a source file, built from 'body' and
                'headers', to executable form.  Return true on success, false
                otherwise.
        
        """
2021-03-02 20:46:31,566 : INFO : tokenize_signature : --> do i ever get here?
    def try_run(self, body, headers=None, include_dirs=None, libraries=None,
                library_dirs=None, lang="c"):
        """
        Try to compile, link to an executable, and run a program
                built from 'body' and 'headers'.  Return true on success, false
                otherwise.
        
        """
2021-03-02 20:46:31,566 : INFO : tokenize_signature : --> do i ever get here?
    def check_func(self, func, headers=None, include_dirs=None,
                   libraries=None, library_dirs=None, decl=0, call=0):
        """
        Determine if function 'func' is available by constructing a
                source file that refers to 'func', and compiles and links it.
                If everything succeeds, returns true; otherwise returns false.

                The constructed source file starts out by including the header
                files listed in 'headers'.  If 'decl' is true, it then declares
                'func' (as "int func()"); you probably shouldn't supply 'headers'
                and set 'decl' true in the same call, or you might get errors about
                a conflicting declarations for 'func'.  Finally, the constructed
                'main()' function either references 'func' or (if 'call' is true)
                calls it.  'libraries' and 'library_dirs' are used when
                linking.
        
        """
2021-03-02 20:46:31,567 : INFO : tokenize_signature : --> do i ever get here?
    def check_lib(self, library, library_dirs=None, headers=None,
                  include_dirs=None, other_libraries=[]):
        """
        Determine if 'library' is available to be linked against,
                without actually checking that any particular symbols are provided
                by it.  'headers' will be used in constructing the source file to
                be compiled, but the only effect of this is to check if all the
                header files listed are available.  Any libraries listed in
                'other_libraries' will be included in the link, in case 'library'
                has symbols that depend on other libraries.
        
        """
2021-03-02 20:46:31,567 : INFO : tokenize_signature : --> do i ever get here?
    def check_header(self, header, include_dirs=None, library_dirs=None,
                     lang="c"):
        """
        Determine if the system header file named by 'header_file'
                exists and can be found by the preprocessor; return true if so,
                false otherwise.
        
        """
def dump_file(filename, head=None):
    """
    Dumps a file content into log.info.

        If head is not None, will be dumped before the file content.
    
    """
