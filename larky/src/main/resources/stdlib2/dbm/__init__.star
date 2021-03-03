def error(Exception):
    """
    'dbm.gnu'
    """
def open(file, flag='r', mode=0o666):
    """
    Open or create database at path given by *file*.

        Optional argument *flag* can be 'r' (default) for read-only access, 'w'
        for read-write access of an existing database, 'c' for read-write access
        to a new or existing database, and 'n' for read-write access to a new
        database.

        Note: 'r' and 'w' fail if the database doesn't exist; 'c' creates it
        only if it doesn't exist; and 'n' always creates a new database.
    
    """
def whichdb(filename):
    """
    Guess which db package to use to open a db file.

        Return values:

        - None if the database file can't be read;
        - empty string if the file can be read but can't be recognized
        - the name of the dbm submodule (e.g. "ndbm" or "gnu") if recognized.

        Importing the given module may still fail, and opening the
        database using that module may still fail.
    
    """
