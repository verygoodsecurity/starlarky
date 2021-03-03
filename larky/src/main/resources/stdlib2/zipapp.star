def ZipAppError(ValueError):
    """
    Write a shebang line.
    """
def _copy_archive(archive, new_archive, interpreter=None):
    """
    Copy an application archive, modifying the shebang line.
    """
2021-03-02 20:46:40,787 : INFO : tokenize_signature : --> do i ever get here?
def create_archive(source, target=None, interpreter=None, main=None,
                   filter=None, compressed=False):
    """
    Create an application archive from SOURCE.

        The SOURCE can be the name of a directory, or a filename or a file-like
        object referring to an existing archive.

        The content of SOURCE is packed into an application archive in TARGET,
        which can be a filename or a file-like object.  If SOURCE is a directory,
        TARGET can be omitted and will default to the name of SOURCE with .pyz
        appended.

        The created application archive will have a shebang line specifying
        that it should run with INTERPRETER (there will be no shebang line if
        INTERPRETER is None), and a __main__.py which runs MAIN (if MAIN is
        not specified, an existing __main__.py will be used).  It is an error
        to specify MAIN for anything other than a directory source with no
        __main__.py, and it is an error to omit MAIN if the directory has no
        __main__.py.
    
    """
def get_interpreter(archive):
    """
    'rb'
    """
def main(args=None):
    """
    Run the zipapp command line interface.

        The ARGS parameter lets you specify the argument list directly.
        Omitting ARGS (or setting it to None) works as for argparse, using
        sys.argv[1:] as the argument list.
    
    """
