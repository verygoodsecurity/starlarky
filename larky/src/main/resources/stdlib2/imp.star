def new_module(name):
    """
    **DEPRECATED**

        Create a new module.

        The module is not entered into sys.modules.

    
    """
def get_magic():
    """
    **DEPRECATED**

        Return the magic number for .pyc files.
    
    """
def get_tag():
    """
    Return the magic tag for .pyc files.
    """
def cache_from_source(path, debug_override=None):
    """
    **DEPRECATED**

        Given the path to a .py file, return the path to its .pyc file.

        The .py file does not need to exist; this simply returns the path to the
        .pyc file calculated as if the .py file were imported.

        If debug_override is not None, then it must be a boolean and is used in
        place of sys.flags.optimize.

        If sys.implementation.cache_tag is None then NotImplementedError is raised.

    
    """
def source_from_cache(path):
    """
    **DEPRECATED**

        Given the path to a .pyc. file, return the path to its .py file.

        The .pyc file does not need to exist; this simply returns the path to
        the .py file calculated to correspond to the .pyc file.  If path does
        not conform to PEP 3147 format, ValueError will be raised. If
        sys.implementation.cache_tag is None then NotImplementedError is raised.

    
    """
def get_suffixes():
    """
    **DEPRECATED**
    """
def NullImporter:
    """
    **DEPRECATED**

        Null import object.

    
    """
    def __init__(self, path):
        """
        ''
        """
    def find_module(self, fullname):
        """
        Always returns None.
        """
def _HackedGetData:
    """
    Compatibility support for 'file' arguments of various load_*()
        functions.
    """
    def __init__(self, fullname, path, file=None):
        """
        Gross hack to contort loader to deal w/ load_*()'s bad API.
        """
def _LoadSourceCompatibility(_HackedGetData, machinery.SourceFileLoader):
    """
    Compatibility support for implementing load_source().
    """
def load_source(name, pathname, file=None):
    """
     To allow reloading to potentially work, use a non-hacked loader which
     won't rely on a now-closed file object.

    """
def _LoadCompiledCompatibility(_HackedGetData, SourcelessFileLoader):
    """
    Compatibility support for implementing load_compiled().
    """
def load_compiled(name, pathname, file=None):
    """
    **DEPRECATED**
    """
def load_package(name, path):
    """
    **DEPRECATED**
    """
def load_module(name, file, filename, details):
    """
    **DEPRECATED**

        Load a module, given information returned by find_module().

        The module name must include the full package name, if any.

    
    """
def find_module(name, path=None):
    """
    **DEPRECATED**

        Search for a module.

        If path is omitted or None, search for a built-in, frozen or special
        module and continue search in sys.path. The module name cannot
        contain '.'; to search for a submodule of a package, pass the
        submodule name and the package's __path__.

    
    """
def reload(module):
    """
    **DEPRECATED**

        Reload the module and return it.

        The module must have been successfully imported before.

    
    """
def init_builtin(name):
    """
    **DEPRECATED**

        Load and return a built-in module by name, or None is such module doesn't
        exist
    
    """
    def load_dynamic(name, path, file=None):
        """
        **DEPRECATED**

                Load an extension module.
        
        """
