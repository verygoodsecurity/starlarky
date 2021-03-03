def AddPackagePath(packagename, path):
    """
     This ReplacePackage mechanism allows modulefinder to work around
     situations in which a package injects itself under the name
     of another package into sys.modules at runtime by calling
     ReplacePackage("real_package_name", "faked_package_name")
     before running ModuleFinder.


    """
def _find_module(name, path=None):
    """
    An importlib reimplementation of imp.find_module (for our purposes).
    """
def Module:
    """
     The set of global names that are assigned to in the module.
     This includes those names imported through starimports of
     Python modules.

    """
    def __repr__(self):
        """
        Module(%r
        """
def ModuleFinder:
    """
     Used in debugging only
    """
    def msg(self, level, str, *args):
        """
   
        """
    def msgin(self, *args):
        """
        run_script
        """
    def load_file(self, pathname):
        """
        rb
        """
    def import_hook(self, name, caller=None, fromlist=None, level=-1):
        """
        import_hook
        """
    def determine_parent(self, caller, level=-1):
        """
        determine_parent
        """
    def find_head_package(self, parent, name):
        """
        find_head_package
        """
    def load_tail(self, q, tail):
        """
        load_tail
        """
    def ensure_fromlist(self, m, fromlist, recursive=0):
        """
        ensure_fromlist
        """
    def find_all_submodules(self, m):
        """
         'suffixes' used to be a list hardcoded to [".py", ".pyc"].
         But we must also collect Python extension modules - although
         we cannot separate normal dlls from Python extensions.

        """
    def import_module(self, partname, fqname, parent):
        """
        import_module
        """
    def load_module(self, fqname, fp, pathname, file_info):
        """
        load_module
        """
    def _add_badmodule(self, name, caller):
        """
        -
        """
    def _safe_import_hook(self, name, caller, fromlist, level=-1):
        """
         wrapper for self.import_hook() that won't raise ImportError

        """
    def scan_opcodes(self, co):
        """
         Scan the code, and yield 'interesting' opcode combinations

        """
    def scan_code(self, co, m):
        """
        store
        """
    def load_package(self, fqname, pathname):
        """
        load_package
        """
    def add_module(self, fqname):
        """
         assert path is not None

        """
    def report(self):
        """
        Print a report to stdout, listing the found modules with their
                paths, as well as modules that are missing, or seem to be missing.
        
        """
    def any_missing(self):
        """
        Return a list of modules that appear to be missing. Use
                any_missing_maybe() if you want to know which modules are
                certain to be missing, and which *may* be missing.
        
        """
    def any_missing_maybe(self):
        """
        Return two lists, one with modules that are certainly missing
                and one with modules that *may* be missing. The latter names could
                either be submodules *or* just global names in the package.

                The reason it can't always be determined is that it's impossible to
                tell which names are imported when "from module import *" is done
                with an extension module, short of actually importing it.
        
        """
    def replace_paths_in_code(self, co):
        """
        co_filename %r changed to %r
        """
def test():
    """
     Parse command line

    """
