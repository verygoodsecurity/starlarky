def _get_gid(name):
    """
    Returns a gid, given a group name.
    """
def _get_uid(name):
    """
    Returns an uid, given a user name.
    """
2021-03-02 20:46:27,750 : INFO : tokenize_signature : --> do i ever get here?
def make_tarball(base_name, base_dir, compress="gzip", verbose=0, dry_run=0,
                 owner=None, group=None):
    """
    Create a (possibly compressed) tar file from all the files under
        'base_dir'.

        'compress' must be "gzip" (the default), "bzip2", "xz", "compress", or
        None.  ("compress" will be deprecated in Python 3.2)

        'owner' and 'group' can be used to define an owner and a group for the
        archive that is being built. If not provided, the current owner and group
        will be used.

        The output tar file will be named 'base_dir' +  ".tar", possibly plus
        the appropriate compression extension (".gz", ".bz2", ".xz" or ".Z").

        Returns the output filename.
    
    """
    def _set_uid_gid(tarinfo):
        """
        'w|%s'
        """
def make_zipfile(base_name, base_dir, verbose=0, dry_run=0):
    """
    Create a zip file from all the files under 'base_dir'.

        The output zip file will be named 'base_name' + ".zip".  Uses either the
        "zipfile" Python module (if available) or the InfoZIP "zip" utility
        (if installed and found on the default search path).  If neither tool is
        available, raises DistutilsExecError.  Returns the name of the output zip
        file.
    
    """
def check_archive_formats(formats):
    """
    Returns the first format from the 'format' list that is unknown.

        If all formats are known, returns None
    
    """
2021-03-02 20:46:27,755 : INFO : tokenize_signature : --> do i ever get here?
def make_archive(base_name, format, root_dir=None, base_dir=None, verbose=0,
                 dry_run=0, owner=None, group=None):
    """
    Create an archive file (eg. zip or tar).

        'base_name' is the name of the file to create, minus any format-specific
        extension; 'format' is the archive format: one of "zip", "tar", "gztar",
        "bztar", "xztar", or "ztar".

        'root_dir' is a directory that will be the root directory of the
        archive; ie. we typically chdir into 'root_dir' before creating the
        archive.  'base_dir' is the directory where we start archiving from;
        ie. 'base_dir' will be the common prefix of all files and
        directories in the archive.  'root_dir' and 'base_dir' both default
        to the current directory.  Returns the name of the archive file.

        'owner' and 'group' are used when creating a tar archive. By default,
        uses the current owner and group.
    
    """
