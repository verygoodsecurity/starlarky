def build_py (Command):
    """
    \"build\" pure Python modules (copy to build directory)
    """
    def initialize_options(self):
        """
        'build'
        """
    def run(self):
        """
         XXX copy_file by default preserves atime and mtime.  IMHO this is
         the right thing to do, but perhaps it should be an option -- in
         particular, a site administrator might want installed files to
         reflect the time of installation rather than the last
         modification time before the installed release.

         XXX copy_file by default preserves mode, which appears to be the
         wrong thing to do: if a file is read-only in the working
         directory, we want it to be installed read/write so that the next
         installation of the same module distribution can overwrite it
         without problems.  (This might be a Unix-specific issue.)  Thus
         we turn off 'preserve_mode' when copying to the build directory,
         since the build directory is supposed to be exactly what the
         installation will look like (ie. we preserve mode when
         installing).

         Two options control which modules will be installed: 'packages'
         and 'py_modules'.  The former lets us work with whole packages, not
         specifying individual modules at all; the latter is for
         specifying modules one-at-a-time.


        """
    def get_data_files(self):
        """
        Generate list of '(package,src_dir,build_dir,filenames)' tuples
        """
    def find_data_files(self, package, src_dir):
        """
        Return filenames for package's data files in 'src_dir'
        """
    def build_package_data(self):
        """
        Copy data files into build directory
        """
    def get_package_dir(self, package):
        """
        Return the directory, relative to the top of the source
                   distribution, where package 'package' should be found
                   (at least according to the 'package_dir' option, if any).
        """
    def check_package(self, package, package_dir):
        """
         Empty dir name means current directory, which we can probably
         assume exists.  Also, os.path.exists and isdir don't know about
         my "empty string means current dir" convention, so we have to
         circumvent them.

        """
    def check_module(self, module, module_file):
        """
        file %s (for module %s) not found
        """
    def find_package_modules(self, package, package_dir):
        """
        *.py
        """
    def find_modules(self):
        """
        Finds individually-specified Python modules, ie. those listed by
                module name in 'self.py_modules'.  Returns a list of tuples (package,
                module_base, filename): 'package' is a tuple of the path through
                package-space to the module; 'module_base' is the bare (no
                packages, no dots) module name, and 'filename' is the path to the
                ".py" file (relative to the distribution root) that implements the
                module.
        
        """
    def find_all_modules(self):
        """
        Compute the list of all modules that will be built, whether
                they are specified one-module-at-a-time ('self.py_modules') or
                by whole packages ('self.packages').  Return a list of tuples
                (package, module, module_file), just like 'find_modules()' and
                'find_package_modules()' do.
        """
    def get_source_files(self):
        """
        .py
        """
    def get_outputs(self, include_bytecode=1):
        """
        '.'
        """
    def build_module(self, module, module_file, package):
        """
        '.'
        """
    def build_modules(self):
        """
         Now "build" the module -- ie. copy the source file to
         self.build_lib (the build directory for Python source).
         (Actually, it gets copied to the directory for this package
         under self.build_lib.)

        """
    def build_packages(self):
        """
         Get list of (package, module, module_file) tuples based on
         scanning the package directory.  'package' is only included
         in the tuple so that 'find_modules()' and
         'find_package_tuples()' have a consistent interface; it's
         ignored here (apart from a sanity check).  Also, 'module' is
         the *unqualified* module name (ie. no dots, no package -- we
         already know its package!), and 'module_file' is the path to
         the .py file, relative to the current directory
         (ie. including 'package_dir').

        """
    def byte_compile(self, files):
        """
        'byte-compiling is disabled, skipping.'
        """
def build_py_2to3(build_py, Mixin2to3):
    """
     Base class code

    """
    def build_module(self, module, module_file, package):
        """
         file was copied

        """
