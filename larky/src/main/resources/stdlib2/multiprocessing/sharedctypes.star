def _new_value(type_):
    """
    '''
        Returns a ctypes object allocated from shared memory
        '''
    """
def RawArray(typecode_or_type, size_or_initializer):
    """
    '''
        Returns a ctypes array allocated from shared memory
        '''
    """
def Value(typecode_or_type, *args, lock=True, ctx=None):
    """
    '''
        Return a synchronization wrapper for a Value
        '''
    """
def Array(typecode_or_type, size_or_initializer, *, lock=True, ctx=None):
    """
    '''
        Return a synchronization wrapper for a RawArray
        '''
    """
def copy(obj):
    """
    'object already synchronized'
    """
def reduce_ctype(obj):
    """

     Function to create properties



    """
def make_property(name):
    """
    '''
    def get%s(self):
        self.acquire()
        try:
            return self._obj.%s
        finally:
            self.release()
    def set%s(self, value):
        self.acquire()
        try:
            self._obj.%s = value
        finally:
            self.release()
    %s = property(get%s, set%s)
    '''
    """
def SynchronizedBase(object):
    """
    '<%s wrapper for %s>'
    """
def Synchronized(SynchronizedBase):
    """
    'value'
    """
def SynchronizedArray(SynchronizedBase):
    """
    'value'
    """
