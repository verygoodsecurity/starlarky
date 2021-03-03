def show_formats():
    """
    Print all possible values for the 'formats' option (used by
        the "--help-formats" command-line option).
    
    """
def sdist(Command):
    """
    create a source distribution (tarball, zip file, etc.)
    """
    def checking_metadata(self):
        """
        Callable used for the check sub-command.

                Placed here so user_options can view it
        """
    def initialize_options(self):
        """
         'template' and 'manifest' are, respectively, the names of
         the manifest template and manifest file.

        """
    def finalize_options(self):
        """
        MANIFEST
        """
    def run(self):
        """
         'filelist' contains the list of files that will make up the
         manifest

        """
    def check_metadata(self):
        """
        Deprecated API.
        """
    def get_file_list(self):
        """
        Figure out the list of files to include in the source
                distribution, and put it in 'self.filelist'.  This might involve
                reading the manifest template (and writing the manifest), or just
                reading the manifest, or just using the default file set -- it all
                depends on the user's options.
        
        """
    def add_defaults(self):
        """
        Add all the default files to self.filelist:
                  - README or README.txt
                  - setup.py
                  - test/test*.py
                  - all pure Python modules mentioned in setup script
                  - all files pointed by package_data (build_py)
                  - all files defined in data_files.
                  - all files defined as scripts.
                  - all C sources listed as part of extensions or C libraries
                    in the setup script (doesn't catch C headers!)
                Warns if (README or README.txt) or setup.py are missing; everything
                else is optional.
        
        """
    def _cs_path_exists(fspath):
        """

                Case-sensitive path existence check

                >>> sdist._cs_path_exists(__file__)
                True
                >>> sdist._cs_path_exists(__file__.upper())
                False
        
        """
    def _add_defaults_standards(self):
        """
        standard file not found: should have one of 
        """
    def _add_defaults_optional(self):
        """
        'test/test*.py'
        """
    def _add_defaults_python(self):
        """
         build_py is used to get:
          - python modules
          - files defined in package_data

        """
    def _add_defaults_data_files(self):
        """
         getting distribution.data_files

        """
    def _add_defaults_ext(self):
        """
        'build_ext'
        """
    def _add_defaults_c_libs(self):
        """
        'build_clib'
        """
    def _add_defaults_scripts(self):
        """
        'build_scripts'
        """
    def read_template(self):
        """
        Read and parse manifest template file named by self.template.

                (usually "MANIFEST.in") The parsing and processing is done by
                'self.filelist', which updates itself accordingly.
        
        """
    def prune_file_list(self):
        """
        Prune off branches that might slip into the file list as created
                by 'read_template()', but really don't belong there:
                  * the build tree (typically "build")
                  * the release tree itself (only an issue if we ran "sdist"
                    previously with --keep-temp, or it aborted)
                  * any RCS, CVS, .svn, .hg, .git, .bzr, _darcs directories
        
        """
    def write_manifest(self):
        """
        Write the file list in 'self.filelist' (presumably as filled in
                by 'add_defaults()' and 'read_template()') to the manifest file
                named by 'self.manifest'.
        
        """
    def _manifest_is_not_generated(self):
        """
         check for special comment used in 3.1.3 and higher

        """
    def read_manifest(self):
        """
        Read the manifest file (named by 'self.manifest') and use it to
                fill in 'self.filelist', the list of files to include in the source
                distribution.
        
        """
    def make_release_tree(self, base_dir, files):
        """
        Create the directory tree that will become the source
                distribution archive.  All directories implied by the filenames in
                'files' are created under 'base_dir', and then we hard link or copy
                (if hard linking is unavailable) those files into place.
                Essentially, this duplicates the developer's source tree, but in a
                directory named after the distribution, containing only the files
                to be distributed.
        
        """
    def make_distribution(self):
        """
        Create the source distribution(s).  First, we create the release
                tree with 'make_release_tree()'; then, we create all required
                archive files (according to 'self.formats') from the release tree.
                Finally, we clean up by blowing away the release tree (unless
                'self.keep_temp' is true).  The list of archive files created is
                stored so it can be retrieved later by 'get_archive_files()'.
        
        """
    def get_archive_files(self):
        """
        Return the list of archive files created when the command
                was run, or None if the command hasn't run yet.
        
        """
