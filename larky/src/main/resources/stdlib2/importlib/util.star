def source_hash(source_bytes):
    """
    Return the hash of *source_bytes* as used in hash-based pyc files.
    """
def resolve_name(name, package):
    """
    Resolve a relative module name to an absolute one.
    """
def _find_spec_from_path(name, path=None):
    """
    Return the spec for the specified module.

        First, sys.modules is checked to see if the module was already imported. If
        so, then sys.modules[name].__spec__ is returned. If that happens to be
        set to None, then ValueError is raised. If the module is not in
        sys.modules, then sys.meta_path is searched for a suitable spec with the
        value of 'path' given to the finders. None is returned if no spec could
        be found.

        Dotted names do not have their parent packages implicitly imported. You will
        most likely need to explicitly import all parent packages in the proper
        order for a submodule to get the correct spec.

    
    """
def find_spec(name, package=None):
    """
    Return the spec for the specified module.

        First, sys.modules is checked to see if the module was already imported. If
        so, then sys.modules[name].__spec__ is returned. If that happens to be
        set to None, then ValueError is raised. If the module is not in
        sys.modules, then sys.meta_path is searched for a suitable spec with the
        value of 'path' given to the finders. None is returned if no spec could
        be found.

        If the name is for submodule (contains a dot), the parent module is
        automatically imported.

        The name and package arguments work the same as importlib.import_module().
        In other words, relative module names (with leading dots) work.

    
    """
def _module_to_load(name):
    """
     This must be done before open() is called as the 'io' module
     implicitly imports 'locale' and would otherwise trigger an
     infinite loop.

    """
def set_package(fxn):
    """
    Set __package__ on the returned module.

        This function is deprecated.

    
    """
    def set_package_wrapper(*args, **kwargs):
        """
        'The import system now takes care of this automatically.'
        """
def set_loader(fxn):
    """
    Set __loader__ on the returned module.

        This function is deprecated.

    
    """
    def set_loader_wrapper(self, *args, **kwargs):
        """
        'The import system now takes care of this automatically.'
        """
def module_for_loader(fxn):
    """
    Decorator to handle selecting the proper module for loaders.

        The decorated function is passed the module to use instead of the module
        name. The module passed in to the function is either from sys.modules if
        it already exists or is a new module. If the module is new, then __name__
        is set the first argument to the method, __loader__ is set to self, and
        __package__ is set accordingly (if self.is_package() is defined) will be set
        before it is passed to the decorated function (if self.is_package() does
        not work for the module it will be set post-load).

        If an exception is raised and the decorator created the module it is
        subsequently removed from sys.modules.

        The decorator assumes that the decorated function takes the module name as
        the second argument.

    
    """
    def module_for_loader_wrapper(self, fullname, *args, **kwargs):
        """
        '.'
        """
def _LazyModule(types.ModuleType):
    """
    A subclass of the module type which triggers loading upon attribute access.
    """
    def __getattribute__(self, attr):
        """
        Trigger the load of the module and return the attribute.
        """
    def __delattr__(self, attr):
        """
        Trigger the load and then perform the deletion.
        """
def LazyLoader(abc.Loader):
    """
    A loader that creates a module which defers loading until attribute access.
    """
    def __check_eager_loader(loader):
        """
        'exec_module'
        """
    def factory(cls, loader):
        """
        Construct a callable which returns the eager loader made lazy.
        """
    def __init__(self, loader):
        """
        Make the module load lazily.
        """
