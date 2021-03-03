def _Database(collections.abc.MutableMapping):
    """
     The on-disk directory and data files can remain in mutually
     inconsistent states for an arbitrarily long time (see comments
     at the end of __setitem__).  This is only repaired when _commit()
     gets called.  One place _commit() gets called is from __del__(),
     and if that occurs at program shutdown time, module globals may
     already have gotten rebound to None.  Since it's crucial that
     _commit() finish successfully, we can't ignore shutdown races
     here, and _commit() must not reference any globals.

    """
    def __init__(self, filebasename, mode, flag='c'):
        """
        'r'
        """
    def _create(self, flag):
        """
        'n'
        """
    def _update(self, flag):
        """
        'r'
        """
    def _commit(self):
        """
         CAUTION:  It's vital that _commit() succeed, and _commit() can
         be called from __del__().  Therefore we must never reference a
         global in this routine.

        """
    def _verify_open(self):
        """
        'DBM object has already been closed'
        """
    def __getitem__(self, key):
        """
        'utf-8'
        """
    def _addval(self, val):
        """
        'rb+'
        """
    def _setval(self, pos, val):
        """
        'rb+'
        """
    def _addkey(self, key, pos_and_siz_pair):
        """
        'a'
        """
    def __setitem__(self, key, val):
        """
        'The database is opened for reading only'
        """
    def __delitem__(self, key):
        """
        'The database is opened for reading only'
        """
    def keys(self):
        """
        'DBM object has already been closed'
        """
    def items(self):
        """
        'utf-8'
        """
    def iterkeys(self):
        """
        'DBM object has already been closed'
        """
    def __len__(self):
        """
        'DBM object has already been closed'
        """
    def close(self):
        """
        'c'
        """
