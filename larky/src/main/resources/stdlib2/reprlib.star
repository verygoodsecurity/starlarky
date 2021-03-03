def recursive_repr(fillvalue='...'):
    """
    'Decorator to make a repr function return fillvalue for a recursive call'
    """
    def decorating_function(user_function):
        """
         Can't use functools.wraps() here because of bootstrap issues

        """
def Repr:
    """
    ' '
    """
    def _repr_iterable(self, x, level, left, right, maxiter, trail=''):
        """
        '...'
        """
    def repr_tuple(self, x, level):
        """
        '('
        """
    def repr_list(self, x, level):
        """
        '['
        """
    def repr_array(self, x, level):
        """
        array('%s')
        """
    def repr_set(self, x, level):
        """
        'set()'
        """
    def repr_frozenset(self, x, level):
        """
        'frozenset()'
        """
    def repr_deque(self, x, level):
        """
        'deque(['
        """
    def repr_dict(self, x, level):
        """
        '{}'
        """
    def repr_str(self, x, level):
        """
        '...'
        """
    def repr_int(self, x, level):
        """
         XXX Hope this isn't too slow...
        """
    def repr_instance(self, x, level):
        """
         Bugs in x.__repr__() can cause arbitrary
         exceptions -- then make up something

        """
def _possibly_sorted(x):
    """
     Since not all sequences of items can be sorted and comparison
     functions may raise arbitrary exceptions, return an unsorted
     sequence in that case.

    """
