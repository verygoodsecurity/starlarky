def _register(abstract_cls, *classes):
    """
    Legacy abstract base class for import finders.

        It may be subclassed for compatibility with legacy third party
        reimplementations of the import system.  Otherwise, finder
        implementations should derive from the more specific MetaPathFinder
        or PathEntryFinder ABCs.

        Deprecated since Python 3.3
    
    """
    def find_module(self, fullname, path=None):
        """
        An abstract method that should find a module.
                The fullname is a str and the optional path is a str or None.
                Returns a Loader object or None.
        
        """
def MetaPathFinder(Finder):
    """
    Abstract base class for import finders on sys.meta_path.
    """
    def find_module(self, fullname, path):
        """
        Return a loader for the module.

                If no module is found, return None.  The fullname is a str and
                the path is a list of strings or None.

                This method is deprecated since Python 3.4 in favor of
                finder.find_spec(). If find_spec() exists then backwards-compatible
                functionality is provided for this method.

        
        """
    def invalidate_caches(self):
        """
        An optional method for clearing the finder's cache, if any.
                This method is used by importlib.invalidate_caches().
        
        """
def PathEntryFinder(Finder):
    """
    Abstract base class for path entry finders used by PathFinder.
    """
    def find_loader(self, fullname):
        """
        Return (loader, namespace portion) for the path entry.

                The fullname is a str.  The namespace portion is a sequence of
                path entries contributing to part of a namespace package. The
                sequence may be empty.  If loader is not None, the portion will
                be ignored.

                The portion will be discarded if another path entry finder
                locates the module as a normal module or package.

                This method is deprecated since Python 3.4 in favor of
                finder.find_spec(). If find_spec() is provided than backwards-compatible
                functionality is provided.
        
        """
    def invalidate_caches(self):
        """
        An optional method for clearing the finder's cache, if any.
                This method is used by PathFinder.invalidate_caches().
        
        """
def Loader(metadef=abc.ABCMeta):
    """
    Abstract base class for import loaders.
    """
    def create_module(self, spec):
        """
        Return a module to initialize and into which to load.

                This method should raise ImportError if anything prevents it
                from creating a new module.  It may return None to indicate
                that the spec should create the new module.
        
        """
    def load_module(self, fullname):
        """
        Return the loaded module.

                The module must be added to sys.modules and have import-related
                attributes set properly.  The fullname is a str.

                ImportError is raised on failure.

                This method is deprecated in favor of loader.exec_module(). If
                exec_module() exists then it is used to provide a backwards-compatible
                functionality for this method.

        
        """
    def module_repr(self, module):
        """
        Return a module's repr.

                Used by the module type when the method does not raise
                NotImplementedError.

                This method is deprecated.

        
        """
def ResourceLoader(Loader):
    """
    Abstract base class for loaders which can return data from their
        back-end storage.

        This ABC represents one of the optional protocols specified by PEP 302.

    
    """
    def get_data(self, path):
        """
        Abstract method which when implemented should return the bytes for
                the specified path.  The path must be a str.
        """
def InspectLoader(Loader):
    """
    Abstract base class for loaders which support inspection about the
        modules they can load.

        This ABC represents one of the optional protocols specified by PEP 302.

    
    """
    def is_package(self, fullname):
        """
        Optional method which when implemented should return whether the
                module is a package.  The fullname is a str.  Returns a bool.

                Raises ImportError if the module cannot be found.
        
        """
    def get_code(self, fullname):
        """
        Method which returns the code object for the module.

                The fullname is a str.  Returns a types.CodeType if possible, else
                returns None if a code object does not make sense
                (e.g. built-in module). Raises ImportError if the module cannot be
                found.
        
        """
    def get_source(self, fullname):
        """
        Abstract method which should return the source code for the
                module.  The fullname is a str.  Returns a str.

                Raises ImportError if the module cannot be found.
        
        """
    def source_to_code(data, path='<string>'):
        """
        Compile 'data' into a code object.

                The 'data' argument can be anything that compile() can handle. The'path'
                argument should be where the data was retrieved (when applicable).
        """
def ExecutionLoader(InspectLoader):
    """
    Abstract base class for loaders that wish to support the execution of
        modules as scripts.

        This ABC represents one of the optional protocols specified in PEP 302.

    
    """
    def get_filename(self, fullname):
        """
        Abstract method which should return the value that __file__ is to be
                set to.

                Raises ImportError if the module cannot be found.
        
        """
    def get_code(self, fullname):
        """
        Method to return the code object for fullname.

                Should return None if not applicable (e.g. built-in module).
                Raise ImportError if the module cannot be found.
        
        """
def FileLoader(_bootstrap_external.FileLoader, ResourceLoader, ExecutionLoader):
    """
    Abstract base class partially implementing the ResourceLoader and
        ExecutionLoader ABCs.
    """
def SourceLoader(_bootstrap_external.SourceLoader, ResourceLoader, ExecutionLoader):
    """
    Abstract base class for loading source code (and optionally any
        corresponding bytecode).

        To support loading from source code, the abstractmethods inherited from
        ResourceLoader and ExecutionLoader need to be implemented. To also support
        loading from bytecode, the optional methods specified directly by this ABC
        is required.

        Inherited abstractmethods not implemented in this ABC:

            * ResourceLoader.get_data
            * ExecutionLoader.get_filename

    
    """
    def path_mtime(self, path):
        """
        Return the (int) modification time for the path (str).
        """
    def path_stats(self, path):
        """
        Return a metadata dict for the source pointed to by the path (str).
                Possible keys:
                - 'mtime' (mandatory) is the numeric timestamp of last source
                  code modification;
                - 'size' (optional) is the size in bytes of the source code.
        
        """
    def set_data(self, path, data):
        """
        Write the bytes to the path (if possible).

                Accepts a str path and data as bytes.

                Any needed intermediary directories are to be created. If for some
                reason the file cannot be written because of permissions, fail
                silently.
        
        """
def ResourceReader(metadef=abc.ABCMeta):
    """
    Abstract base class to provide resource-reading support.

        Loaders that support resource reading are expected to implement
        the ``get_resource_reader(fullname)`` method and have it either return None
        or an object compatible with this ABC.
    
    """
    def open_resource(self, resource):
        """
        Return an opened, file-like object for binary reading.

                The 'resource' argument is expected to represent only a file name
                and thus not contain any subdirectory components.

                If the resource cannot be found, FileNotFoundError is raised.
        
        """
    def resource_path(self, resource):
        """
        Return the file system path to the specified resource.

                The 'resource' argument is expected to represent only a file name
                and thus not contain any subdirectory components.

                If the resource does not exist on the file system, raise
                FileNotFoundError.
        
        """
    def is_resource(self, name):
        """
        Return True if the named 'name' is consider a resource.
        """
    def contents(self):
        """
        Return an iterable of strings over the contents of the package.
        """
