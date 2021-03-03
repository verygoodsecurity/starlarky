def invalidate_caches():
    """
    Call the invalidate_caches() method on all meta path finders stored in
        sys.meta_path (where implemented).
    """
def find_loader(name, path=None):
    """
    Return the loader for the specified module.

        This is a backward-compatible wrapper around find_spec().

        This function is deprecated in favor of importlib.util.find_spec().

    
    """
def import_module(name, package=None):
    """
    Import a module.

        The 'package' argument is required when performing a relative import. It
        specifies the package to use as the anchor point from which to resolve the
        relative import to an absolute import.

    
    """
def reload(module):
    """
    Reload the module and return it.

        The module must have been successfully imported before.

    
    """
