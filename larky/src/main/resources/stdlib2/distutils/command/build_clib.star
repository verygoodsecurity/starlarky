def show_compilers():
    """
    build C/C++ libraries used by Python extensions
    """
    def initialize_options(self):
        """
         List of libraries to build

        """
    def finalize_options(self):
        """
         This might be confusing: both build-clib and build-temp default
         to build-temp as defined by the "build" command.  This is because
         I think that C libraries are really just temporary build
         by-products, at least from the point of view of building Python
         extensions -- but I want to keep my options open.

        """
    def run(self):
        """
         Yech -- this is cut 'n pasted from build_ext.py!

        """
    def check_library_list(self, libraries):
        """
        Ensure that the list of libraries is valid.

                `library` is presumably provided as a command option 'libraries'.
                This method checks that it is a list of 2-tuples, where the tuples
                are (library_name, build_info_dict).

                Raise DistutilsSetupError if the structure is invalid anywhere;
                just returns otherwise.
        
        """
    def get_library_names(self):
        """
         Assume the library list is valid -- 'check_library_list()' is
         called from 'finalize_options()', so it should be!

        """
    def get_source_files(self):
        """
        'sources'
        """
    def build_libraries(self, libraries):
        """
        'sources'
        """
