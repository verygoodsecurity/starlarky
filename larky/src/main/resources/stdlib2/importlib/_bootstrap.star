def _wrap(new, old):
    """
    Simple substitute for functools.update_wrapper.
    """
def _new_module(name):
    """
     Module-level locking ########################################################

     A dict mapping module names to weakrefs of _ModuleLock instances
     Dictionary protected by the global import lock

    """
def _DeadlockError(RuntimeError):
    """
    A recursive lock implementation which is able to detect deadlocks
        (e.g. thread 1 trying to take locks A then B, and thread 2 trying to
        take locks B then A).
    
    """
    def __init__(self, name):
        """
         Deadlock avoidance for concurrent circular imports.

        """
    def acquire(self):
        """

                Acquire the module lock.  If a potential deadlock is detected,
                a _DeadlockError is raised.
                Otherwise, the lock is always acquired and True is returned.
        
        """
    def release(self):
        """
        'cannot release un-acquired lock'
        """
    def __repr__(self):
        """
        '_ModuleLock({!r}) at {}'
        """
def _DummyModuleLock:
    """
    A simple _ModuleLock equivalent for Python builds without
        multi-threading support.
    """
    def __init__(self, name):
        """
        'cannot release un-acquired lock'
        """
    def __repr__(self):
        """
        '_DummyModuleLock({!r}) at {}'
        """
def _ModuleLockManager:
    """
     The following two functions are for consumption by Python/import.c.


    """
def _get_module_lock(name):
    """
    Get or create the module lock for a given module name.

        Acquire/release internally the global import lock to protect
        _module_locks.
    """
            def cb(ref, name=name):
                """
                 bpo-31070: Check if another thread created a new lock
                 after the previous lock was destroyed
                 but before the weakref callback was called.

                """
def _lock_unlock_module(name):
    """
    Acquires then releases the module lock for a given module name.

        This is used to ensure a module is completely initialized, in the
        event it is being imported by another thread.
    
    """
def _call_with_frames_removed(f, *args, **kwds):
    """
    remove_importlib_frames in import.c will always remove sequences
        of importlib frames that end with a call to this function

        Use it instead of a normal call in places where including the importlib
        frames introduces unwanted noise into the traceback (e.g. when executing
        module code)
    
    """
def _verbose_message(message, *args, verbosity=1):
    """
    Print the message to stderr if -v/PYTHONVERBOSE is turned on.
    """
def _requires_builtin(fxn):
    """
    Decorator to verify the named module is built-in.
    """
    def _requires_builtin_wrapper(self, fullname):
        """
        '{!r} is not a built-in module'
        """
def _requires_frozen(fxn):
    """
    Decorator to verify the named module is frozen.
    """
    def _requires_frozen_wrapper(self, fullname):
        """
        '{!r} is not a frozen module'
        """
def _load_module_shim(self, fullname):
    """
    Load the specified module into sys.modules and return it.

        This method is deprecated.  Use loader.exec_module instead.

    
    """
def _module_repr(module):
    """
     The implementation of ModuleType.__repr__().

    """
def ModuleSpec:
    """
    The specification for a module, used for loading.

        A module's spec is the source for information about the module.  For
        data associated with the module, including source, use the spec's
        loader.

        `name` is the absolute name of the module.  `loader` is the loader
        to use when loading the module.  `parent` is the name of the
        package the module is in.  The parent is derived from the name.

        `is_package` determines if the module is considered a package or
        not.  On modules this is reflected by the `__path__` attribute.

        `origin` is the specific location used by the loader from which to
        load the module, if that information is available.  When filename is
        set, origin will match.

        `has_location` indicates that a spec's "origin" reflects a location.
        When this is True, `__file__` attribute of the module is set.

        `cached` is the location of the cached bytecode file, if any.  It
        corresponds to the `__cached__` attribute.

        `submodule_search_locations` is the sequence of path entries to
        search when importing submodules.  If set, is_package should be
        True--and False otherwise.

        Packages are simply modules that (may) have submodules.  If a spec
        has a non-None value in `submodule_search_locations`, the import
        system will consider modules loaded from the spec as packages.

        Only finders (see importlib.abc.MetaPathFinder and
        importlib.abc.PathEntryFinder) should modify ModuleSpec instances.

    
    """
2021-03-02 20:54:01,220 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, name, loader, *, origin=None, loader_state=None,
                 is_package=None):
        """
         file-location attributes

        """
    def __repr__(self):
        """
        'name={!r}'
        """
    def __eq__(self, other):
        """
        The name of the module's parent.
        """
    def has_location(self):
        """
        Return a module spec based on various loader methods.
        """
def _spec_from_module(module, loader=None, origin=None):
    """
     This function is meant for use in _setup().

    """
def _init_module_attrs(spec, module, *, override=False):
    """
     The passed-in module may be not support attribute assignment,
     in which case we simply don't set the attributes.
     __name__

    """
def module_from_spec(spec):
    """
    Create a module based on the provided spec.
    """
def _module_repr_from_spec(spec):
    """
    Return the repr to use for the module.
    """
def _exec(spec, module):
    """
    Execute the spec's specified module in an existing module's namespace.
    """
def _load_backward_compatible(spec):
    """
     (issue19713) Once BuiltinImporter and ExtensionFileLoader
     have exec_module() implemented, we can add a deprecation
     warning here.

    """
def _load_unlocked(spec):
    """
     A helper for direct use by the import system.

    """
def _load(spec):
    """
    Return a new module object, loaded by the spec's loader.

        The module is not added to its parent.

        If a module is already in sys.modules, that existing module gets
        clobbered.

    
    """
def BuiltinImporter:
    """
    Meta path import for built-in modules.

        All methods are either class or static methods to avoid the need to
        instantiate the class.

    
    """
    def module_repr(module):
        """
        Return repr for the module.

                The method is deprecated.  The import machinery does the job itself.

        
        """
    def find_spec(cls, fullname, path=None, target=None):
        """
        'built-in'
        """
    def find_module(cls, fullname, path=None):
        """
        Find the built-in module.

                If 'path' is ever specified then the search is considered a failure.

                This method is deprecated.  Use find_spec() instead.

        
        """
    def create_module(self, spec):
        """
        Create a built-in module
        """
    def exec_module(self, module):
        """
        Exec a built-in module
        """
    def get_code(cls, fullname):
        """
        Return None as built-in modules do not have code objects.
        """
    def get_source(cls, fullname):
        """
        Return None as built-in modules do not have source code.
        """
    def is_package(cls, fullname):
        """
        Return False as built-in modules are never packages.
        """
def FrozenImporter:
    """
    Meta path import for frozen modules.

        All methods are either class or static methods to avoid the need to
        instantiate the class.

    
    """
    def module_repr(m):
        """
        Return repr for the module.

                The method is deprecated.  The import machinery does the job itself.

        
        """
    def find_spec(cls, fullname, path=None, target=None):
        """
        Find a frozen module.

                This method is deprecated.  Use find_spec() instead.

        
        """
    def create_module(cls, spec):
        """
        Use default semantics for module creation.
        """
    def exec_module(module):
        """
        '{!r} is not a frozen module'
        """
    def load_module(cls, fullname):
        """
        Load a frozen module.

                This method is deprecated.  Use exec_module() instead.

        
        """
    def get_code(cls, fullname):
        """
        Return the code object for the frozen module.
        """
    def get_source(cls, fullname):
        """
        Return None as frozen modules do not have source code.
        """
    def is_package(cls, fullname):
        """
        Return True if the frozen module is a package.
        """
def _ImportLockContext:
    """
    Context manager for the import lock.
    """
    def __enter__(self):
        """
        Acquire the import lock.
        """
    def __exit__(self, exc_type, exc_value, exc_traceback):
        """
        Release the import lock regardless of any raised exceptions.
        """
def _resolve_name(name, package, level):
    """
    Resolve a relative module name to an absolute one.
    """
def _find_spec_legacy(finder, name, path):
    """
     This would be a good place for a DeprecationWarning if
     we ended up going that route.

    """
def _find_spec(name, path, target=None):
    """
    Find a module's spec.
    """
def _sanity_check(name, package, level):
    """
    Verify arguments are "sane".
    """
def _find_and_load_unlocked(name, import_):
    """
    '.'
    """
def _find_and_load(name, import_):
    """
    Find and load the module.
    """
def _gcd_import(name, package=None, level=0):
    """
    Import and return the module based on its name, the package the call is
        being made from, and the level adjustment.

        This function represents the greatest common denominator of functionality
        between import_module and __import__. This includes setting __package__ if
        the loader did not.

    
    """
def _handle_fromlist(module, fromlist, import_, *, recursive=False):
    """
    Figure out what __import__ should return.

        The import_ parameter is a callable which takes the name of module to
        import. It is required to decouple the function from assuming importlib's
        import implementation is desired.

    
    """
def _calc___package__(globals):
    """
    Calculate what __package__ should be.

        __package__ is not guaranteed to be defined or could be set to None
        to represent that its proper value is unknown.

    
    """
def __import__(name, globals=None, locals=None, fromlist=(), level=0):
    """
    Import a module.

        The 'globals' argument is used to infer where the import is occurring from
        to handle relative imports. The 'locals' argument is ignored. The
        'fromlist' argument specifies what should exist as attributes on the module
        being imported (e.g. ``from module import <fromlist>``).  The 'level'
        argument represents the package location to import from in a relative
        import (e.g. ``from ..pkg import mod`` would have a 'level' of 2).

    
    """
def _builtin_from_name(name):
    """
    'no built-in module named '
    """
def _setup(sys_module, _imp_module):
    """
    Setup importlib by importing needed built-in modules and injecting them
        into the global namespace.

        As sys is needed for sys.modules access and _imp is needed to load built-in
        modules, those two modules must be explicitly passed in.

    
    """
def _install(sys_module, _imp_module):
    """
    Install importers for builtin and frozen modules
    """
def _install_external_importers():
    """
    Install importers that require external filesystem access
    """
