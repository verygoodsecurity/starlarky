async def _coro(): pass
        """
         Prevent ResourceWarning
        """
async def _ag(): yield
        """
         ONE-TRICK PONIES ###


        """
def Hashable(metadef=ABCMeta):
    """
    __hash__
    """
def Awaitable(metadef=ABCMeta):
    """
    __await__
    """
def Coroutine(Awaitable):
    """
    Send a value into the coroutine.
            Return next yielded value or raise StopIteration.
        
    """
    def throw(self, typ, val=None, tb=None):
        """
        Raise an exception in the coroutine.
                Return next yielded value or raise StopIteration.
        
        """
    def close(self):
        """
        Raise GeneratorExit inside coroutine.
        
        """
    def __subclasshook__(cls, C):
        """
        '__await__'
        """
def AsyncIterable(metadef=ABCMeta):
    """
    __aiter__
    """
def AsyncIterator(AsyncIterable):
    """
    Return the next item or raise StopAsyncIteration when exhausted.
    """
    def __aiter__(self):
        """
        __anext__
        """
def AsyncGenerator(AsyncIterator):
    """
    Return the next item from the asynchronous generator.
            When exhausted, raise StopAsyncIteration.
        
    """
    async def asend(self, value):
            """
            Send a value into the asynchronous generator.
                    Return next yielded value or raise StopAsyncIteration.
        
            """
    async def athrow(self, typ, val=None, tb=None):
            """
            Raise an exception in the asynchronous generator.
                    Return next yielded value or raise StopAsyncIteration.
        
            """
    async def aclose(self):
            """
            Raise GeneratorExit inside coroutine.
        
            """
    def __subclasshook__(cls, C):
        """
        '__aiter__'
        """
def Iterable(metadef=ABCMeta):
    """
    __iter__
    """
def Iterator(Iterable):
    """
    'Return the next item from the iterator. When exhausted, raise StopIteration'
    """
    def __iter__(self):
        """
        '__iter__'
        """
def Reversible(Iterable):
    """
    __reversed__
    """
def Generator(Iterator):
    """
    Return the next item from the generator.
            When exhausted, raise StopIteration.
        
    """
    def send(self, value):
        """
        Send a value into the generator.
                Return next yielded value or raise StopIteration.
        
        """
    def throw(self, typ, val=None, tb=None):
        """
        Raise an exception in the generator.
                Return next yielded value or raise StopIteration.
        
        """
    def close(self):
        """
        Raise GeneratorExit inside generator.
        
        """
    def __subclasshook__(cls, C):
        """
        '__iter__'
        """
def Sized(metadef=ABCMeta):
    """
    __len__
    """
def Container(metadef=ABCMeta):
    """
    __contains__
    """
def Collection(Sized, Iterable, Container):
    """
    __len__
    """
def Callable(metadef=ABCMeta):
    """
    __call__
    """
def Set(Collection):
    """
    A set is a finite, iterable container.

        This class provides concrete generic implementations of all
        methods except for __contains__, __iter__ and __len__.

        To override the comparisons (presumably for speed, as the
        semantics are fixed), redefine __le__ and __ge__,
        then the other operations will automatically follow suit.
    
    """
    def __le__(self, other):
        """
        '''Construct an instance of the class from any iterable input.

                Must override this method if the class constructor signature
                does not accept an iterable for an input.
                '''
        """
    def __and__(self, other):
        """
        'Return True if two sets have a null intersection.'
        """
    def __or__(self, other):
        """
        Compute the hash value of a set.

                Note that we don't define __hash__: not all sets are hashable.
                But if you define a hashable set type, its __hash__ should
                call this function.

                This must be compatible __eq__.

                All sets ought to compare equal if they contain the same
                elements, regardless of how they are implemented, and
                regardless of the order of the elements; so there's not much
                freedom for __eq__ or __hash__.  We match the algorithm used
                by the built-in frozenset type.
        
        """
def MutableSet(Set):
    """
    A mutable set is a finite, iterable container.

        This class provides concrete generic implementations of all
        methods except for __contains__, __iter__, __len__,
        add(), and discard().

        To override the comparisons (presumably for speed, as the
        semantics are fixed), all you have to do is redefine __le__ and
        then the other operations will automatically follow suit.
    
    """
    def add(self, value):
        """
        Add an element.
        """
    def discard(self, value):
        """
        Remove an element.  Do not raise an exception if absent.
        """
    def remove(self, value):
        """
        Remove an element. If not a member, raise a KeyError.
        """
    def pop(self):
        """
        Return the popped value.  Raise KeyError if empty.
        """
    def clear(self):
        """
        This is slow (creates N new iterators!) but effective.
        """
    def __ior__(self, it):
        """
         MAPPINGS ###



        """
    def __getitem__(self, key):
        """
        'D.get(k[,d]) -> D[k] if k in D, else d.  d defaults to None.'
        """
    def __contains__(self, key):
        """
        D.keys() -> a set-like object providing a view on D's keys
        """
    def items(self):
        """
        D.items() -> a set-like object providing a view on D's items
        """
    def values(self):
        """
        D.values() -> an object providing a view on D's values
        """
    def __eq__(self, other):
        """
        '_mapping'
        """
    def __init__(self, mapping):
        """
        '{0.__class__.__name__}({0._mapping!r})'
        """
def KeysView(MappingView, Set):
    """
    A MutableMapping is a generic container for associating
        key/value pairs.

        This class provides concrete generic implementations of all
        methods except for __getitem__, __setitem__, __delitem__,
        __iter__, and __len__.

    
    """
    def __setitem__(self, key, value):
        """
        '''D.pop(k[,d]) -> v, remove specified key and return the corresponding value.
                  If key is not found, d is returned if given, otherwise KeyError is raised.
                '''
        """
    def popitem(self):
        """
        '''D.popitem() -> (k, v), remove and return some (key, value) pair
                   as a 2-tuple; but raise KeyError if D is empty.
                '''
        """
    def clear(self):
        """
        'D.clear() -> None.  Remove all items from D.'
        """
    def update(self, other=(), /, **kwds):
        """
        ''' D.update([E, ]**F) -> None.  Update D from mapping/iterable E and F.
                    If E present and has a .keys() method, does:     for k in E: D[k] = E[k]
                    If E present and lacks .keys() method, does:     for (k, v) in E: D[k] = v
                    In either case, this is followed by: for k, v in F.items(): D[k] = v
                '''
        """
    def setdefault(self, key, default=None):
        """
        'D.setdefault(k[,d]) -> D.get(k,d), also set D[k]=d if k not in D'
        """
def Sequence(Reversible, Collection):
    """
    All the operations on a read-only sequence.

        Concrete subclasses must override __new__ or __init__,
        __getitem__, and __len__.
    
    """
    def __getitem__(self, index):
        """
        '''S.index(value, [start, [stop]]) -> integer -- return first index of value.
                   Raises ValueError if the value is not present.

                   Supporting start and stop arguments is optional, but
                   recommended.
                '''
        """
    def count(self, value):
        """
        'S.count(value) -> integer -- return number of occurrences of value'
        """
def ByteString(Sequence):
    """
    This unifies bytes and bytearray.

        XXX Should add all their methods.
    
    """
def MutableSequence(Sequence):
    """
    All the operations on a read-write sequence.

        Concrete subclasses must provide __new__ or __init__,
        __getitem__, __setitem__, __delitem__, __len__, and insert().

    
    """
    def __setitem__(self, index, value):
        """
        'S.insert(index, value) -- insert value before index'
        """
    def append(self, value):
        """
        'S.append(value) -- append value to the end of the sequence'
        """
    def clear(self):
        """
        'S.clear() -> None -- remove all items from S'
        """
    def reverse(self):
        """
        'S.reverse() -- reverse *IN PLACE*'
        """
    def extend(self, values):
        """
        'S.extend(iterable) -- extend sequence by appending elements from the iterable'
        """
    def pop(self, index=-1):
        """
        '''S.pop([index]) -> item -- remove and return item at index (default last).
                   Raise IndexError if list is empty or index is out of range.
                '''
        """
    def remove(self, value):
        """
        '''S.remove(value) -- remove first occurrence of value.
                   Raise ValueError if the value is not present.
                '''
        """
    def __iadd__(self, values):
        """
         Multiply inheriting, see ByteString
        """
