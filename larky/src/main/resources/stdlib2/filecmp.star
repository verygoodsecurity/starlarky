def clear_cache():
    """
    Clear the filecmp cache.
    """
def cmp(f1, f2, shallow=True):
    """
    Compare two files.

        Arguments:

        f1 -- First file name

        f2 -- Second file name

        shallow -- Just check stat signature (do not read the files).
                   defaults to True.

        Return value:

        True if the files are the same, False otherwise.

        This function uses a cache for past comparisons and the results,
        with cache entries invalidated if their stat information
        changes.  The cache may be cleared by calling clear_cache().

    
    """
def _sig(st):
    """
    'rb'
    """
def dircmp:
    """
    A class that manages the comparison of 2 directories.

        dircmp(a, b, ignore=None, hide=None)
          A and B are directories.
          IGNORE is a list of names to ignore,
            defaults to DEFAULT_IGNORES.
          HIDE is a list of names to hide,
            defaults to [os.curdir, os.pardir].

        High level usage:
          x = dircmp(dir1, dir2)
          x.report() -> prints a report on the differences between dir1 and dir2
           or
          x.report_partial_closure() -> prints report on differences between dir1
                and dir2, and reports on common immediate subdirectories.
          x.report_full_closure() -> like report_partial_closure,
                but fully recursive.

        Attributes:
         left_list, right_list: The files in dir1 and dir2,
            filtered by hide and ignore.
         common: a list of names in both dir1 and dir2.
         left_only, right_only: names only in dir1, dir2.
         common_dirs: subdirectories in both dir1 and dir2.
         common_files: files in both dir1 and dir2.
         common_funny: names in both dir1 and dir2 where the type differs between
            dir1 and dir2, or the name is not stat-able.
         same_files: list of identical files.
         diff_files: list of filenames which differ.
         funny_files: list of files which could not be compared.
         subdirs: a dictionary of dircmp objects, keyed by names in common_dirs.
     
    """
    def __init__(self, a, b, ignore=None, hide=None): # Initialize
        """
         Initialize
        """
    def phase0(self): # Compare everything except common subdirectories
        """
         Compare everything except common subdirectories
        """
    def phase1(self): # Compute common names
        """
         Compute common names
        """
    def phase2(self): # Distinguish files, directories, funnies
        """
         Distinguish files, directories, funnies
        """
    def phase3(self): # Find out differences between common files
        """
         Find out differences between common files
        """
    def phase4(self): # Find out differences between common subdirectories
        """
         Find out differences between common subdirectories
        """
    def phase4_closure(self): # Recursively call phase4() on subdirectories
        """
         Recursively call phase4() on subdirectories
        """
    def report(self): # Print a report on the differences between a and b
        """
         Print a report on the differences between a and b
        """
    def report_partial_closure(self): # Print reports on self and on subdirs
        """
         Print reports on self and on subdirs
        """
    def report_full_closure(self): # Report on self and subdirs recursively
        """
         Report on self and subdirs recursively
        """
    def __getattr__(self, attr):
        """
        Compare common files in two directories.

            a, b -- directory names
            common -- list of file names found in both directories
            shallow -- if true, do comparison based solely on stat() information

            Returns a tuple of three lists:
              files that compare equal
              files that are different
              filenames that aren't regular files.

    
        """
def _cmp(a, b, sh, abs=abs, cmp=cmp):
    """
     Return a copy with items that occur in skip removed.


    """
def _filter(flist, skip):
    """
     Demonstration and testing.


    """
def demo():
    """
    'r'
    """
