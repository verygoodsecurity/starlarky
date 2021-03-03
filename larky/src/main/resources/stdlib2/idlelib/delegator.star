def Delegator:
    """
     Cache is used to only remove added attributes
     when changing the delegate.


    """
    def __getattr__(self, name):
        """
         May raise AttributeError
        """
    def resetcache(self):
        """
        Removes added attributes while leaving original attributes.
        """
    def setdelegate(self, delegate):
        """
        Reset attributes and change delegate.
        """
