def _get_package(package) -> ModuleType:
    """
    Take a package name or module object and return the module.

        If a name, the module is imported.  If the passed or imported module
        object is not a package, raise an exception.
    
    """
def _normalize_path(path) -> str:
    """
    Normalize a path by ensuring it is a string.

        If the resulting string contains path separators, an exception is raised.
    
    """
2021-03-02 20:54:01,789 : INFO : tokenize_signature : --> do i ever get here?
def _get_resource_reader(
        package: ModuleType) -> Optional[resources_abc.ResourceReader]:
    """
     Return the package's loader if it's a ResourceReader.  We can't use
     a issubclass() check here because apparently abc.'s __subclasscheck__()
     hook wants to create a weak reference to the object, but
     zipimport.zipimporter does not support weak references, resulting in a
     TypeError.  That seems terrible.

    """
def _check_location(package):
    """
    f'Package has no location {package!r}'
    """
def open_binary(package: Package, resource: Resource) -> BinaryIO:
    """
    Return a file-like object opened for binary reading of the resource.
    """
2021-03-02 20:54:01,790 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:01,790 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:01,790 : INFO : tokenize_signature : --> do i ever get here?
def open_text(package: Package,
              resource: Resource,
              encoding: str = 'utf-8',
              errors: str = 'strict') -> TextIO:
    """
    Return a file-like object opened for text reading of the resource.
    """
def read_binary(package: Package, resource: Resource) -> bytes:
    """
    Return the binary contents of the resource.
    """
2021-03-02 20:54:01,792 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:01,792 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:01,792 : INFO : tokenize_signature : --> do i ever get here?
def read_text(package: Package,
              resource: Resource,
              encoding: str = 'utf-8',
              errors: str = 'strict') -> str:
    """
    Return the decoded string of the resource.

        The decoding-related arguments have the same semantics as those of
        bytes.decode().
    
    """
def path(package: Package, resource: Resource) -> Iterator[Path]:
    """
    A context manager providing a file path object to the resource.

        If the resource does not already exist on its own on the file system,
        a temporary file will be created. If the file was created, the file
        will be deleted upon exiting the context manager (no exception is
        raised if the file was deleted prior to the context manager
        exiting).
    
    """
def is_resource(package: Package, name: str) -> bool:
    """
    True if 'name' is a resource inside 'package'.

        Directories are *not* resources.
    
    """
def contents(package: Package) -> Iterable[str]:
    """
    Return an iterable of entries in 'package'.

        Note that not all entries are resources.  Specifically, directories are
        not considered resources.  Use `is_resource()` on each entry returned here
        to check if it is a resource or not.
    
    """
