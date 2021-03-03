def _IterationGuard:
    """
     This context manager registers itself in the current iterators of the
     weak container, such as to delay all removals until the context manager
     exits.
     This technique should be relatively thread-safe (since sets are).


    """
    def __init__(self, weakcontainer):
        """
         Don't create cycles

        """
    def __enter__(self):
        """
         A list of keys to be removed

        """
    def _commit_removals(self):
        """
         Caveat: the iterator will keep a strong reference to
         `item` until it is resumed or closed.

        """
    def __len__(self):
        """
        '__dict__'
        """
    def add(self, item):
        """
        'pop from empty WeakSet'
        """
    def remove(self, item):
