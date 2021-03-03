def _walk_dir(dir, ddir=None, maxlevels=10, quiet=0):
    """
    'Listing {!r}...'
    """
2021-03-02 20:53:53,513 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:53,513 : INFO : tokenize_signature : --> do i ever get here?
def compile_dir(dir, maxlevels=10, ddir=None, force=False, rx=None,
                quiet=0, legacy=False, optimize=-1, workers=1,
                invalidation_mode=None):
    """
    Byte-compile all modules in the given directory tree.

        Arguments (only dir is required):

        dir:       the directory to byte-compile
        maxlevels: maximum recursion level (default 10)
        ddir:      the directory that will be prepended to the path to the
                   file as it is compiled into each byte-code file.
        force:     if True, force compilation, even if timestamps are up-to-date
        quiet:     full output with False or 0, errors only with 1,
                   no output with 2
        legacy:    if True, produce legacy pyc paths instead of PEP 3147 paths
        optimize:  optimization level or -1 for level of the interpreter
        workers:   maximum number of parallel workers
        invalidation_mode: how the up-to-dateness of the pyc will be checked
    
    """
def _compile_file_tuple(file_and_dfile, **kwargs):
    """
    Needs to be toplevel for ProcessPoolExecutor.
    """
2021-03-02 20:53:53,515 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:53,515 : INFO : tokenize_signature : --> do i ever get here?
def compile_file(fullname, ddir=None, force=False, rx=None, quiet=0,
                 legacy=False, optimize=-1,
                 invalidation_mode=None):
    """
    Byte-compile one file.

        Arguments (only fullname is required):

        fullname:  the file to byte-compile
        ddir:      if given, the directory name compiled in to the
                   byte-code file.
        force:     if True, force compilation, even if timestamps are up-to-date
        quiet:     full output with False or 0, errors only with 1,
                   no output with 2
        legacy:    if True, produce legacy pyc paths instead of PEP 3147 paths
        optimize:  optimization level or -1 for level of the interpreter
        invalidation_mode: how the up-to-dateness of the pyc will be checked
    
    """
2021-03-02 20:53:53,517 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:53,517 : INFO : tokenize_signature : --> do i ever get here?
def compile_path(skip_curdir=1, maxlevels=0, force=False, quiet=0,
                 legacy=False, optimize=-1,
                 invalidation_mode=None):
    """
    Byte-compile all module on sys.path.

        Arguments (all optional):

        skip_curdir: if true, skip current directory (default True)
        maxlevels:   max recursion level (default 0)
        force: as for compile_dir() (default False)
        quiet: as for compile_dir() (default 0)
        legacy: as for compile_dir() (default False)
        optimize: as for compile_dir() (default -1)
        invalidation_mode: as for compiler_dir()
    
    """
def main():
    """
    Script main program.
    """
