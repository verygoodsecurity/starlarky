2021-03-02 20:46:46,137 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:46,137 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:46,137 : INFO : tokenize_signature : --> do i ever get here?
def update_wrapper(wrapper,
                   wrapped,
                   assigned = WRAPPER_ASSIGNMENTS,
                   updated = WRAPPER_UPDATES):
    """
    Update a wrapper function to look like the wrapped function

           wrapper is the function to be updated
           wrapped is the original function
           assigned is a tuple naming the attributes assigned directly
           from the wrapped function to the wrapper function (defaults to
           functools.WRAPPER_ASSIGNMENTS)
           updated is a tuple naming the attributes of the wrapper that
           are updated with the corresponding attribute from the wrapped
           function (defaults to functools.WRAPPER_UPDATES)
    
    """
2021-03-02 20:46:46,137 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:46,137 : INFO : tokenize_signature : --> do i ever get here?
def wraps(wrapped,
          assigned = WRAPPER_ASSIGNMENTS,
          updated = WRAPPER_UPDATES):
    """
    Decorator factory to apply update_wrapper() to a wrapper function

           Returns a decorator that invokes update_wrapper() with the decorated
           function as the wrapper argument and the arguments to wraps() as the
           remaining arguments. Default arguments are as for update_wrapper().
           This is a convenience function to simplify applying partial() to
           update_wrapper().
    
    """
def _gt_from_lt(self, other, NotImplemented=NotImplemented):
    """
    'Return a > b.  Computed by @total_ordering from (not a < b) and (a != b).'
    """
def _le_from_lt(self, other, NotImplemented=NotImplemented):
    """
    'Return a <= b.  Computed by @total_ordering from (a < b) or (a == b).'
    """
def _ge_from_lt(self, other, NotImplemented=NotImplemented):
    """
    'Return a >= b.  Computed by @total_ordering from (not a < b).'
    """
def _ge_from_le(self, other, NotImplemented=NotImplemented):
    """
    'Return a >= b.  Computed by @total_ordering from (not a <= b) or (a == b).'
    """
def _lt_from_le(self, other, NotImplemented=NotImplemented):
    """
    'Return a < b.  Computed by @total_ordering from (a <= b) and (a != b).'
    """
def _gt_from_le(self, other, NotImplemented=NotImplemented):
    """
    'Return a > b.  Computed by @total_ordering from (not a <= b).'
    """
def _lt_from_gt(self, other, NotImplemented=NotImplemented):
    """
    'Return a < b.  Computed by @total_ordering from (not a > b) and (a != b).'
    """
def _ge_from_gt(self, other, NotImplemented=NotImplemented):
    """
    'Return a >= b.  Computed by @total_ordering from (a > b) or (a == b).'
    """
def _le_from_gt(self, other, NotImplemented=NotImplemented):
    """
    'Return a <= b.  Computed by @total_ordering from (not a > b).'
    """
def _le_from_ge(self, other, NotImplemented=NotImplemented):
    """
    'Return a <= b.  Computed by @total_ordering from (not a >= b) or (a == b).'
    """
def _gt_from_ge(self, other, NotImplemented=NotImplemented):
    """
    'Return a > b.  Computed by @total_ordering from (a >= b) and (a != b).'
    """
def _lt_from_ge(self, other, NotImplemented=NotImplemented):
    """
    'Return a < b.  Computed by @total_ordering from (not a >= b).'
    """
def total_ordering(cls):
    """
    Class decorator that fills in missing ordering methods
    """
def cmp_to_key(mycmp):
    """
    Convert a cmp= function into a key= function
    """
    def K(object):
    """
    'obj'
    """
        def __init__(self, obj):
            """

             reduce() sequence to a single item



            """
def reduce(function, sequence, initial=_initial_missing):
    """

        reduce(function, sequence[, initial]) -> value

        Apply a function of two arguments cumulatively to the items of a sequence,
        from left to right, so as to reduce the sequence to a single value.
        For example, reduce(lambda x, y: x+y, [1, 2, 3, 4, 5]) calculates
        ((((1+2)+3)+4)+5).  If initial is present, it is placed before the items
        of the sequence in the calculation, and serves as a default when the
        sequence is empty.
    
    """
def partial:
    """
    New function with partial application of the given arguments
        and keywords.
    
    """
    def __new__(cls, func, /, *args, **keywords):
        """
        the first argument must be callable
        """
    def __call__(self, /, *args, **keywords):
        """
        f"{k}={v!r}
        """
    def __reduce__(self):
        """
        argument to __setstate__ must be a tuple
        """
def partialmethod(object):
    """
    Method descriptor with partial application of the given arguments
        and keywords.

        Supports wrapping existing descriptors and handles non-descriptor
        callables as instance methods.
    
    """
    def __init__(*args, **keywords):
        """
        descriptor '__init__' of partialmethod 
        needs an argument
        """
    def __repr__(self):
        """
        , 
        """
    def _make_unbound_method(self):
        """
        __get__
        """
    def __isabstractmethod__(self):
        """
        __isabstractmethod__
        """
def _unwrap_partial(func):
    """

     LRU Cache function decorator



    """
def _HashedSeq(list):
    """
     This class guarantees that hash() will be called no more than once
            per element.  This is important because the lru_cache() will hash
            the key multiple times on a cache miss.

    
    """
    def __init__(self, tup, hash=hash):
        """
        Make a cache key from optionally typed positional and keyword arguments

            The key is constructed in a way that is flat as possible rather than
            as a nested structure that would take more memory.

            If there is only a single argument and its data type is known to cache
            its hash value, then that argument is returned without a wrapper.  This
            saves space and improves lookup speed.

    
        """
def lru_cache(maxsize=128, typed=False):
    """
    Least-recently-used cache decorator.

        If *maxsize* is set to None, the LRU features are disabled and the cache
        can grow without bound.

        If *typed* is True, arguments of different types will be cached separately.
        For example, f(3.0) and f(3) will be treated as distinct calls with
        distinct results.

        Arguments to the cached function must be hashable.

        View the cache statistics named tuple (hits, misses, maxsize, currsize)
        with f.cache_info().  Clear the cache and statistics with f.cache_clear().
        Access the underlying function with f.__wrapped__.

        See:  http://en.wikipedia.org/wiki/Cache_replacement_policies#Least_recently_used_(LRU)

    
    """
    def decorating_function(user_function):
        """
         Constants shared by all lru cache instances:

        """
        def wrapper(*args, **kwds):
            """
             No caching -- just a statistics update

            """
        def wrapper(*args, **kwds):
            """
             Simple caching without ordering or size limit

            """
        def wrapper(*args, **kwds):
            """
             Size limited caching that tracks accesses by recency

            """
    def cache_info():
        """
        Report cache statistics
        """
    def cache_clear():
        """
        Clear the cache and cache statistics
        """
def _c3_merge(sequences):
    """
    Merges MROs in *sequences* to a single MRO using the C3 algorithm.

        Adapted from http://www.python.org/download/releases/2.3/mro/.

    
    """
def _c3_mro(cls, abcs=None):
    """
    Computes the method resolution order using extended C3 linearization.

        If no *abcs* are given, the algorithm works exactly like the built-in C3
        linearization used for method resolution.

        If given, *abcs* is a list of abstract base classes that should be inserted
        into the resulting MRO. Unrelated ABCs are ignored and don't end up in the
        result. The algorithm inserts ABCs where their functionality is introduced,
        i.e. issubclass(cls, abc) returns True for the class itself but returns
        False for all its direct base classes. Implicit ABCs for a given class
        (either registered or inferred from the presence of a special method like
        __len__) are inserted directly after the last ABC explicitly listed in the
        MRO of said class. If two implicit ABCs end up next to each other in the
        resulting MRO, their ordering depends on the order of types in *abcs*.

    
    """
def _compose_mro(cls, types):
    """
    Calculates the method resolution order for a given class *cls*.

        Includes relevant abstract base classes (with their respective bases) from
        the *types* iterable. Uses a modified C3 linearization algorithm.

    
    """
    def is_related(typ):
        """
        '__mro__'
        """
    def is_strict_base(typ):
        """
         Subclasses of the ABCs in *types* which are also implemented by
         *cls* can be used to stabilize ABC ordering.

        """
def _find_impl(cls, registry):
    """
    Returns the best matching implementation from *registry* for type *cls*.

        Where there is no registered implementation for a specific type, its method
        resolution order is used to find a more generic implementation.

        Note: if *registry* does not contain an implementation for the base
        *object* type, this function may return None.

    
    """
def singledispatch(func):
    """
    Single-dispatch generic function decorator.

        Transforms a function into a generic function, which can have different
        behaviours depending upon the type of its first argument. The decorated
        function acts as the default implementation, and additional
        implementations can be registered using the register() attribute of the
        generic function.
    
    """
    def dispatch(cls):
        """
        generic_func.dispatch(cls) -> <function implementation>

                Runs the dispatch algorithm to return the best available implementation
                for the given *cls* registered on *generic_func*.

        
        """
    def register(cls, func=None):
        """
        generic_func.register(cls, func) -> func

                Registers a new implementation for the given *cls* on a *generic_func*.

        
        """
    def wrapper(*args, **kw):
        """
        f'{funcname} requires at least '
        '1 positional argument'
        """
def singledispatchmethod:
    """
    Single-dispatch generic method descriptor.

        Supports wrapping existing descriptors and handles non-descriptor
        callables as instance methods.
    
    """
    def __init__(self, func):
        """
        __get__
        """
    def register(self, cls, method=None):
        """
        generic_method.register(cls, func) -> func

                Registers a new implementation for the given *cls* on a *generic_method*.
        
        """
    def __get__(self, obj, cls=None):
        """
        '__isabstractmethod__'
        """
def cached_property:
    """
    Cannot assign the same cached_property to two different names 
    f"({self.attrname!r} and {name!r}).

    """
    def __get__(self, instance, owner=None):
        """
        Cannot use cached_property instance without calling __set_name__ on it.
        """
