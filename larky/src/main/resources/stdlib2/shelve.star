def _ClosedDict(collections.abc.MutableMapping):
    """
    'Marker for a closed dict.  Access attempts raise a ValueError.'
    """
    def closed(self, *args):
        """
        'invalid operation on closed shelf'
        """
    def __repr__(self):
        """
        '<Closed Dictionary>'
        """
def Shelf(collections.abc.MutableMapping):
    """
    Base class for shelf implementations.

        This is initialized with a dictionary-like object.
        See the module's __doc__ string for an overview of the interface.
    
    """
2021-03-02 20:53:58,119 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, dict, protocol=None, writeback=False,
                 keyencoding="utf-8"):
        """
         Catch errors that may happen when close is called from __del__
         because CPython is in interpreter shutdown.

        """
    def __del__(self):
        """
        'writeback'
        """
    def sync(self):
        """
        'sync'
        """
def BsdDbShelf(Shelf):
    """
    Shelf implementation using the "BSD" db interface.

        This adds methods first(), next(), previous(), last() and
        set_location() that have no counterpart in [g]dbm databases.

        The actual database must be opened using one of the "bsddb"
        modules "open" routines (i.e. bsddb.hashopen, bsddb.btopen or
        bsddb.rnopen) and passed to the constructor.

        See the module's __doc__ string for an overview of the interface.
    
    """
2021-03-02 20:53:58,122 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, dict, protocol=None, writeback=False,
                 keyencoding="utf-8"):
        """
        Shelf implementation using the "dbm" generic dbm interface.

            This is initialized with the filename for the dbm database.
            See the module's __doc__ string for an overview of the interface.
    
        """
    def __init__(self, filename, flag='c', protocol=None, writeback=False):
        """
        'c'
        """
