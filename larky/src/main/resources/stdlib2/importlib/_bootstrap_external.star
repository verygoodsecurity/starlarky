def _make_relax_case():
    """
    'PYTHONCASEOK'
    """
        def _relax_case():
            """
            True if filenames must be checked case-insensitively.
            """
        def _relax_case():
            """
            True if filenames must be checked case-insensitively.
            """
def _pack_uint32(x):
    """
    Convert a 32-bit integer to little-endian.
    """
def _unpack_uint32(data):
    """
    Convert 4 bytes in little-endian to an integer.
    """
def _unpack_uint16(data):
    """
    Convert 2 bytes in little-endian to an integer.
    """
def _path_join(*path_parts):
    """
    Replacement for os.path.join().
    """
def _path_split(path):
    """
    Replacement for os.path.split().
    """
def _path_stat(path):
    """
    Stat the path.

        Made a separate function to make it easier to override in experiments
        (e.g. cache stat results).

    
    """
def _path_is_mode_type(path, mode):
    """
    Test whether the path is the specified mode type.
    """
def _path_isfile(path):
    """
    Replacement for os.path.isfile.
    """
def _path_isdir(path):
    """
    Replacement for os.path.isdir.
    """
def _path_isabs(path):
    """
    Replacement for os.path.isabs.

        Considers a Windows drive-relative path (no drive, but starts with slash) to
        still be "absolute".
    
    """
def _write_atomic(path, data, mode=0o666):
    """
    Best-effort function to write data to a path atomically.
        Be prepared to handle a FileExistsError if concurrent writing of the
        temporary file is attempted.
    """
def cache_from_source(path, debug_override=None, *, optimization=None):
    """
    Given the path to a .py file, return the path to its .pyc file.

        The .py file does not need to exist; this simply returns the path to the
        .pyc file calculated as if the .py file were imported.

        The 'optimization' parameter controls the presumed optimization level of
        the bytecode file. If 'optimization' is not None, the string representation
        of the argument is taken and verified to be alphanumeric (else ValueError
        is raised).

        The debug_override parameter is deprecated. If debug_override is not None,
        a True value is the same as setting 'optimization' to the empty string
        while a False value is equivalent to setting 'optimization' to '1'.

        If sys.implementation.cache_tag is None then NotImplementedError is raised.

    
    """
def source_from_cache(path):
    """
    Given the path to a .pyc. file, return the path to its .py file.

        The .pyc file does not need to exist; this simply returns the path to
        the .py file calculated to correspond to the .pyc file.  If path does
        not conform to PEP 3147/488 format, ValueError will be raised. If
        sys.implementation.cache_tag is None then NotImplementedError is raised.

    
    """
def _get_sourcefile(bytecode_path):
    """
    Convert a bytecode file path to a source path (if possible).

        This function exists purely for backwards-compatibility for
        PyImport_ExecCodeModuleWithFilenames() in the C API.

    
    """
def _get_cached(filename):
    """
    Calculate the mode permissions for a bytecode file.
    """
def _check_name(method):
    """
    Decorator to verify that the module being requested matches the one the
        loader can handle.

        The first argument (self) must define _name which the second argument is
        compared against. If the comparison fails then ImportError is raised.

    
    """
    def _check_name_wrapper(self, name=None, *args, **kwargs):
        """
        'loader for %s cannot handle %s'
        """
        def _wrap(new, old):
            """
            '__module__'
            """
def _find_module_shim(self, fullname):
    """
    Try to find a loader for the specified module by delegating to
        self.find_loader().

        This method is deprecated in favor of finder.find_spec().

    
    """
def _classify_pyc(data, name, exc_details):
    """
    Perform basic validity checking of a pyc header and return the flags field,
        which determines how the pyc should be further validated against the source.

        *data* is the contents of the pyc file. (Only the first 16 bytes are
        required, though.)

        *name* is the name of the module being imported. It is used for logging.

        *exc_details* is a dictionary passed to ImportError if it raised for
        improved debugging.

        ImportError is raised when the magic number is incorrect or when the flags
        field is invalid. EOFError is raised when the data is found to be truncated.

    
    """
2021-03-02 20:54:01,598 : INFO : tokenize_signature : --> do i ever get here?
def _validate_timestamp_pyc(data, source_mtime, source_size, name,
                            exc_details):
    """
    Validate a pyc against the source last-modified time.

        *data* is the contents of the pyc file. (Only the first 16 bytes are
        required.)

        *source_mtime* is the last modified timestamp of the source file.

        *source_size* is None or the size of the source file in bytes.

        *name* is the name of the module being imported. It is used for logging.

        *exc_details* is a dictionary passed to ImportError if it raised for
        improved debugging.

        An ImportError is raised if the bytecode is stale.

    
    """
def _validate_hash_pyc(data, source_hash, name, exc_details):
    """
    Validate a hash-based pyc by checking the real source hash against the one in
        the pyc header.

        *data* is the contents of the pyc file. (Only the first 16 bytes are
        required.)

        *source_hash* is the importlib.util.source_hash() of the source file.

        *name* is the name of the module being imported. It is used for logging.

        *exc_details* is a dictionary passed to ImportError if it raised for
        improved debugging.

        An ImportError is raised if the bytecode is stale.

    
    """
def _compile_bytecode(data, name=None, bytecode_path=None, source_path=None):
    """
    Compile bytecode as found in a pyc.
    """
def _code_to_timestamp_pyc(code, mtime=0, source_size=0):
    """
    Produce the data for a timestamp-based pyc.
    """
def _code_to_hash_pyc(code, source_hash, checked=True):
    """
    Produce the data for a hash-based pyc.
    """
def decode_source(source_bytes):
    """
    Decode bytes representing source code and return the string.

        Universal newline support is used in the decoding.
    
    """
2021-03-02 20:54:01,600 : INFO : tokenize_signature : --> do i ever get here?
def spec_from_file_location(name, location=None, *, loader=None,
                            submodule_search_locations=_POPULATE):
    """
    Return a module spec based on a file location.

        To indicate that the module is a package, set
        submodule_search_locations to a list of directory paths.  An
        empty list is sufficient, though its not otherwise useful to the
        import system.

        The loader must take a spec as its only __init__() arg.

    
    """
def WindowsRegistryFinder:
    """
    Meta path finder for modules declared in the Windows registry.
    """
    def _open_registry(cls, key):
        """
        '%d.%d'
        """
    def find_spec(cls, fullname, path=None, target=None):
        """
        Find module named in the registry.

                This method is deprecated.  Use exec_module() instead.

        
        """
def _LoaderBasics:
    """
    Base class of common code needed by both SourceLoader and
        SourcelessFileLoader.
    """
    def is_package(self, fullname):
        """
        Concrete implementation of InspectLoader.is_package by checking if
                the path returned by get_filename has a filename of '__init__.py'.
        """
    def create_module(self, spec):
        """
        Use default semantics for module creation.
        """
    def exec_module(self, module):
        """
        Execute the module.
        """
    def load_module(self, fullname):
        """
        This module is deprecated.
        """
def SourceLoader(_LoaderBasics):
    """
    Optional method that returns the modification time (an int) for the
            specified path (a str).

            Raises OSError when the path cannot be handled.
        
    """
    def path_stats(self, path):
        """
        Optional method returning a metadata dict for the specified
                path (a str).

                Possible keys:
                - 'mtime' (mandatory) is the numeric timestamp of last source
                  code modification;
                - 'size' (optional) is the size in bytes of the source code.

                Implementing this method allows the loader to read bytecode files.
                Raises OSError when the path cannot be handled.
        
        """
    def _cache_bytecode(self, source_path, cache_path, data):
        """
        Optional method which writes data (bytes) to a file path (a str).

                Implementing this method allows for the writing of bytecode files.

                The source path is needed in order to correctly transfer permissions
        
        """
    def set_data(self, path, data):
        """
        Optional method which writes data (bytes) to a file path (a str).

                Implementing this method allows for the writing of bytecode files.
        
        """
    def get_source(self, fullname):
        """
        Concrete implementation of InspectLoader.get_source.
        """
    def source_to_code(self, data, path, *, _optimize=-1):
        """
        Return the code object compiled from source.

                The 'data' argument can be any object type that compile() supports.
        
        """
    def get_code(self, fullname):
        """
        Concrete implementation of InspectLoader.get_code.

                Reading of bytecode requires path_stats to be implemented. To write
                bytecode, set_data must also be implemented.

        
        """
def FileLoader:
    """
    Base file loader class which implements the loader protocol methods that
        require file system usage.
    """
    def __init__(self, fullname, path):
        """
        Cache the module name and the path to the file found by the
                finder.
        """
    def __eq__(self, other):
        """
        Load a module from a file.

                This method is deprecated.  Use exec_module() instead.

        
        """
    def get_filename(self, fullname):
        """
        Return the path to the source file as found by the finder.
        """
    def get_data(self, path):
        """
        Return the data from path as raw bytes.
        """
    def get_resource_reader(self, module):
        """
        'r'
        """
    def resource_path(self, resource):
        """
        Concrete implementation of SourceLoader using the file system.
        """
    def path_stats(self, path):
        """
        Return the metadata for the path.
        """
    def _cache_bytecode(self, source_path, bytecode_path, data):
        """
         Adapt between the two APIs

        """
    def set_data(self, path, data, *, _mode=0o666):
        """
        Write bytes data to a file.
        """
def SourcelessFileLoader(FileLoader, _LoaderBasics):
    """
    Loader which handles sourceless file imports.
    """
    def get_code(self, fullname):
        """
         Call _classify_pyc to do basic validation of the pyc but ignore the
         result. There's no source to check against.

        """
    def get_source(self, fullname):
        """
        Return None as there is no source code.
        """
def ExtensionFileLoader(FileLoader, _LoaderBasics):
    """
    Loader for extension modules.

        The constructor is designed to work with FileFinder.

    
    """
    def __init__(self, name, path):
        """
        Create an unitialized extension module
        """
    def exec_module(self, module):
        """
        Initialize an extension module
        """
    def is_package(self, fullname):
        """
        Return True if the extension module is a package.
        """
    def get_code(self, fullname):
        """
        Return None as an extension module cannot create a code object.
        """
    def get_source(self, fullname):
        """
        Return None as extension modules have no source code.
        """
    def get_filename(self, fullname):
        """
        Return the path to the source file as found by the finder.
        """
def _NamespacePath:
    """
    Represents a namespace package's path.  It uses the module name
        to find its parent module, and from there it looks up the parent's
        __path__.  When this changes, the module's own path is recomputed,
        using path_finder.  For top-level modules, the parent module's path
        is sys.path.
    """
    def __init__(self, name, path, path_finder):
        """
        Returns a tuple of (parent-module-name, parent-path-attr-name)
        """
    def _get_parent_path(self):
        """
         If the parent's path has changed, recalculate _path

        """
    def __iter__(self):
        """
        '_NamespacePath({!r})'
        """
    def __contains__(self, item):
        """
         We use this exclusively in module_from_spec() for backward-compatibility.

        """
def _NamespaceLoader:
    """
    Return repr for the module.

            The method is deprecated.  The import machinery does the job itself.

        
    """
    def is_package(self, fullname):
        """
        ''
        """
    def get_code(self, fullname):
        """
        ''
        """
    def create_module(self, spec):
        """
        Use default semantics for module creation.
        """
    def exec_module(self, module):
        """
        Load a namespace module.

                This method is deprecated.  Use exec_module() instead.

        
        """
def PathFinder:
    """
    Meta path finder for sys.path and package __path__ attributes.
    """
    def invalidate_caches(cls):
        """
        Call the invalidate_caches() method on all path entry finders
                stored in sys.path_importer_caches (where implemented).
        """
    def _path_hooks(cls, path):
        """
        Search sys.path_hooks for a finder for 'path'.
        """
    def _path_importer_cache(cls, path):
        """
        Get the finder for the path entry from sys.path_importer_cache.

                If the path entry is not in the cache, find the appropriate finder
                and cache it. If no finder is available, store None.

        
        """
    def _legacy_get_spec(cls, fullname, finder):
        """
         This would be a good place for a DeprecationWarning if
         we ended up going that route.

        """
    def _get_spec(cls, fullname, path, target=None):
        """
        Find the loader or namespace_path for this module/package name.
        """
    def find_spec(cls, fullname, path=None, target=None):
        """
        Try to find a spec for 'fullname' on sys.path or 'path'.

                The search is based on sys.path_hooks and sys.path_importer_cache.
        
        """
    def find_module(cls, fullname, path=None):
        """
        find the module on sys.path or 'path' based on sys.path_hooks and
                sys.path_importer_cache.

                This method is deprecated.  Use find_spec() instead.

        
        """
    def find_distributions(cls, *args, **kwargs):
        """

                Find distributions.

                Return an iterable of all Distribution instances capable of
                loading the metadata for packages matching ``context.name``
                (or all names if ``None`` indicated) along the paths in the list
                of directories ``context.path``.
        
        """
def FileFinder:
    """
    File-based finder.

        Interactions with the file system are cached for performance, being
        refreshed when the directory the finder is handling has been modified.

    
    """
    def __init__(self, path, *loader_details):
        """
        Initialize with the path to search on and a variable number of
                2-tuples containing the loader and the file suffixes the loader
                recognizes.
        """
    def invalidate_caches(self):
        """
        Invalidate the directory mtime.
        """
    def find_loader(self, fullname):
        """
        Try to find a loader for the specified module, or the namespace
                package portions. Returns (loader, list-of-portions).

                This method is deprecated.  Use find_spec() instead.

        
        """
    def _get_spec(self, loader_class, fullname, path, smsl, target):
        """
        Try to find a spec for the specified module.

                Returns the matching spec, or None if not found.
        
        """
    def _fill_cache(self):
        """
        Fill the cache of potential modules and packages for this directory.
        """
    def path_hook(cls, *loader_details):
        """
        A class method which returns a closure to use on sys.path_hook
                which will return an instance using the specified loaders and the path
                called on the closure.

                If the path called on the closure is not a directory, ImportError is
                raised.

        
        """
        def path_hook_for_FileFinder(path):
            """
            Path hook for importlib.machinery.FileFinder.
            """
    def __repr__(self):
        """
        'FileFinder({!r})'
        """
def _fix_up_module(ns, name, pathname, cpathname=None):
    """
     This function is used by PyImport_ExecCodeModuleObject().

    """
def _get_supported_file_loaders():
    """
    Returns a list of file-based module loaders.

        Each item is a tuple (loader, suffixes).
    
    """
def _setup(_bootstrap_module):
    """
    Setup the path-based importers for importlib by importing needed
        built-in modules and injecting them into the global namespace.

        Other components are extracted from the core bootstrap module.

    
    """
def _install(_bootstrap_module):
    """
    Install the path-based import components.
    """
