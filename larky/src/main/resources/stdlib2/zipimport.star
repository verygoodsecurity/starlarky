def ZipImportError(ImportError):
    """
     _read_directory() cache

    """
def zipimporter:
    """
    zipimporter(archivepath) -> zipimporter object

        Create a new zipimporter instance. 'archivepath' must be a path to
        a zipfile, or to a specific path inside a zipfile. For example, it can be
        '/tmp/myimport.zip', or '/tmp/myimport.zip/mydirectory', if mydirectory is a
        valid directory inside the archive.

        'ZipImportError is raised if 'archivepath' doesn't point to a valid Zip
        archive.

        The 'archive' attribute of zipimporter objects contains the name of the
        zipfile targeted.
    
    """
    def __init__(self, path):
        """
        'archive path is empty'
        """
    def find_loader(self, fullname, path=None):
        """
        find_loader(fullname, path=None) -> self, str or None.

                Search for a module specified by 'fullname'. 'fullname' must be the
                fully qualified (dotted) module name. It returns the zipimporter
                instance itself if the module was found, a string containing the
                full path name if it's possibly a portion of a namespace package,
                or None otherwise. The optional 'path' argument is ignored -- it's
                there for compatibility with the importer protocol.
        
        """
    def find_module(self, fullname, path=None):
        """
        find_module(fullname, path=None) -> self or None.

                Search for a module specified by 'fullname'. 'fullname' must be the
                fully qualified (dotted) module name. It returns the zipimporter
                instance itself if the module was found, or None if it wasn't.
                The optional 'path' argument is ignored -- it's there for compatibility
                with the importer protocol.
        
        """
    def get_code(self, fullname):
        """
        get_code(fullname) -> code object.

                Return the code object for the specified module. Raise ZipImportError
                if the module couldn't be found.
        
        """
    def get_data(self, pathname):
        """
        get_data(pathname) -> string with file data.

                Return the data associated with 'pathname'. Raise OSError if
                the file wasn't found.
        
        """
    def get_filename(self, fullname):
        """
        get_filename(fullname) -> filename string.

                Return the filename for the specified module.
        
        """
    def get_source(self, fullname):
        """
        get_source(fullname) -> source string.

                Return the source code for the specified module. Raise ZipImportError
                if the module couldn't be found, return None if the archive does
                contain the module, but has no source for it.
        
        """
    def is_package(self, fullname):
        """
        is_package(fullname) -> bool.

                Return True if the module specified by fullname is a package.
                Raise ZipImportError if the module couldn't be found.
        
        """
    def load_module(self, fullname):
        """
        load_module(fullname) -> module.

                Load the module specified by 'fullname'. 'fullname' must be the
                fully qualified (dotted) module name. It returns the imported
                module, or raises ZipImportError if it wasn't found.
        
        """
    def get_resource_reader(self, fullname):
        """
        Return the ResourceReader for a package in a zip file.

                If 'fullname' is a package within the zip file, return the
                'ResourceReader' object for the package.  Otherwise return None.
        
        """
    def __repr__(self):
        """
        f'<zipimporter object "{self.archive}{path_sep}{self.prefix}">'
        """
def _get_module_path(self, fullname):
    """
    '.'
    """
def _is_dir(self, path):
    """
     See if this is a "directory". If so, it's eligible to be part
     of a namespace package. We test by seeing if the name, with an
     appended path separator, exists.

    """
def _get_module_info(self, fullname):
    """
     implementation

     _read_directory(archive) -> files dict (new reference)

     Given a path to a Zip archive, build a dict, mapping file names
     (local to the archive, using SEP as a separator) to toc entries.

     A toc_entry is a tuple:

     (__file__,        # value to use for __file__, available for all files,
                       # encoded to the filesystem encoding
      compress,        # compression kind; 0 for uncompressed
      data_size,       # size of compressed data on disk
      file_size,       # size of decompressed data
      file_offset,     # offset of file header from start of archive
      time,            # mod time of file (in dos format)
      date,            # mod data of file (in dos format)
      crc,             # crc checksum of the data
     )

     Directories can be recognized by the trailing path_sep in the name,
     data_size and file_offset are 0.

    """
def _read_directory(archive):
    """
    f"can't open Zip file: {archive!r}
    """
def _get_decompress_func():
    """
     Someone has a zlib.py[co] in their Zip file
     let's avoid a stack overflow.

    """
def _get_data(archive, toc_entry):
    """
    'negative data size'
    """
def _eq_mtime(t1, t2):
    """
     dostime only stores even seconds, so be lenient

    """
def _unmarshal_code(self, pathname, fullpath, fullname, data):
    """
    'name'
    """
def _normalize_line_endings(source):
    """
    b'\r\n'
    """
def _compile_source(pathname, source):
    """
    'exec'
    """
def _parse_dostime(d, t):
    """
     bits 9..15: year

    """
def _get_mtime_and_size_of_source(self, path):
    """
     strip 'c' or 'o' from *.py[co]

    """
def _get_pyc_source(self, path):
    """
     strip 'c' or 'o' from *.py[co]

    """
def _get_module_code(self, fullname):
    """
    'trying {}{}{}'
    """
def _ZipImportResourceReader:
    """
    Private class used to support ZipImport.get_resource_reader().

        This class is allowed to reference all the innards and private parts of
        the zipimporter.
    
    """
    def __init__(self, zipimporter, fullname):
        """
        '.'
        """
    def resource_path(self, resource):
        """
         All resources are in the zip file, so there is no path to the file.
         Raising FileNotFoundError tells the higher level API to extract the
         binary data and create a temporary file.

        """
    def is_resource(self, name):
        """
         Maybe we could do better, but if we can get the data, it's a
         resource.  Otherwise it isn't.

        """
    def contents(self):
        """
         This is a bit convoluted, because fullname will be a module path,
         but _files is a list of file names relative to the top of the
         archive's namespace.  We want to compare file paths to find all the
         names of things inside the module represented by fullname.  So we
         turn the module path of fullname into a file path relative to the
         top of the archive, and then we iterate through _files looking for
         names inside that "directory".

        """
