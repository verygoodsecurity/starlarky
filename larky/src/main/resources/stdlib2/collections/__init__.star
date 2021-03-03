def __getattr__(name):
    """
     For backwards compatibility, continue to make the collections ABCs
     through Python 3.6 available through the collections module.
     Note, no new collections ABCs were added in Python 3.7

    """
def _OrderedDictKeysView(_collections_abc.KeysView):
    """
    'prev'
    """
def OrderedDict(dict):
    """
    'Dictionary that remembers insertion order'
    """
    def __init__(self, other=(), /, **kwds):
        """
        '''Initialize an ordered dictionary.  The signature is the same as
                regular dictionaries.  Keyword argument order is preserved.
                '''
        """
2021-03-02 20:54:28,720 : INFO : tokenize_signature : --> do i ever get here?
    def __setitem__(self, key, value,
                    dict_setitem=dict.__setitem__, proxy=_proxy, Link=_Link):
        """
        'od.__setitem__(i, y) <==> od[i]=y'
        """
    def __delitem__(self, key, dict_delitem=dict.__delitem__):
        """
        'od.__delitem__(y) <==> del od[y]'
        """
    def __iter__(self):
        """
        'od.__iter__() <==> iter(od)'
        """
    def __reversed__(self):
        """
        'od.__reversed__() <==> reversed(od)'
        """
    def clear(self):
        """
        'od.clear() -> None.  Remove all items from od.'
        """
    def popitem(self, last=True):
        """
        '''Remove and return a (key, value) pair from the dictionary.

                Pairs are returned in LIFO order if last is true or FIFO order if false.
                '''
        """
    def move_to_end(self, key, last=True):
        """
        '''Move an existing element to the end (or beginning if last is false).

                Raise KeyError if the element does not exist.
                '''
        """
    def __sizeof__(self):
        """
         number of links including root
        """
    def keys(self):
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
    def pop(self, key, default=__marker):
        """
        '''od.pop(k[,d]) -> v, remove specified key and return the corresponding
                value.  If key is not found, d is returned if given, otherwise KeyError
                is raised.

                '''
        """
    def setdefault(self, key, default=None):
        """
        '''Insert key with a value of default if key is not in the dictionary.

                Return the value for key if key is in the dictionary, else default.
                '''
        """
    def __repr__(self):
        """
        'od.__repr__() <==> repr(od)'
        """
    def __reduce__(self):
        """
        'Return state information for pickling'
        """
    def copy(self):
        """
        'od.copy() -> a shallow copy of od'
        """
    def fromkeys(cls, iterable, value=None):
        """
        '''Create a new ordered dictionary with keys from iterable and values set to value.
                '''
        """
    def __eq__(self, other):
        """
        '''od.__eq__(y) <==> od==y.  Comparison to another OD is order-sensitive
                while comparison to a regular mapping is order-insensitive.

                '''
        """
def namedtuple(typename, field_names, *, rename=False, defaults=None, module=None):
    """
    Returns a new subclass of tuple with named fields.

        >>> Point = namedtuple('Point', ['x', 'y'])
        >>> Point.__doc__                   # docstring for the new class
        'Point(x, y)'
        >>> p = Point(11, y=22)             # instantiate with positional args or keywords
        >>> p[0] + p[1]                     # indexable like a plain tuple
        33
        >>> x, y = p                        # unpack like a regular tuple
        >>> x, y
        (11, 22)
        >>> p.x + p.y                       # fields also accessible by name
        33
        >>> d = p._asdict()                 # convert to a dictionary
        >>> d['x']
        11
        >>> Point(**d)                      # convert from a dictionary
        Point(x=11, y=22)
        >>> p._replace(x=100)               # _replace() is like str.replace() but targets named fields
        Point(x=100, y=22)

    
    """
    def _make(cls, iterable):
        """
        f'Expected {num_fields} arguments, got {len(result)}'
        """
    def _replace(self, /, **kwds):
        """
        f'Got unexpected field names: {list(kwds)!r}'
        """
    def __repr__(self):
        """
        'Return a nicely formatted representation string'
        """
    def _asdict(self):
        """
        'Return a new dict which maps field names to their values.'
        """
    def __getnewargs__(self):
        """
        'Return self as a plain tuple.  Used by copy and pickle.'
        """
def _count_elements(mapping, iterable):
    """
    'Tally elements from the iterable.'
    """
def Counter(dict):
    """
    '''Dict subclass for counting hashable items.  Sometimes called a bag
        or multiset.  Elements are stored as dictionary keys and their counts
        are stored as dictionary values.

        >>> c = Counter('abcdeabcdabcaba')  # count elements from a string

        >>> c.most_common(3)                # three most common elements
        [('a', 5), ('b', 4), ('c', 3)]
        >>> sorted(c)                       # list all unique elements
        ['a', 'b', 'c', 'd', 'e']
        >>> ''.join(sorted(c.elements()))   # list elements with repetitions
        'aaaaabbbbcccdde'
        >>> sum(c.values())                 # total of all counts
        15

        >>> c['a']                          # count of letter 'a'
        5
        >>> for elem in 'shazam':           # update counts from an iterable
        ...     c[elem] += 1                # by adding 1 to each element's count
        >>> c['a']                          # now there are seven 'a'
        7
        >>> del c['b']                      # remove all 'b'
        >>> c['b']                          # now there are zero 'b'
        0

        >>> d = Counter('simsalabim')       # make another counter
        >>> c.update(d)                     # add in the second counter
        >>> c['a']                          # now there are nine 'a'
        9

        >>> c.clear()                       # empty the counter
        >>> c
        Counter()

        Note:  If a count is set to zero or reduced to zero, it will remain
        in the counter until the entry is deleted or the counter is cleared:

        >>> c = Counter('aaabbc')
        >>> c['b'] -= 2                     # reduce the count of 'b' by two
        >>> c.most_common()                 # 'b' is still in, but its count is zero
        [('a', 3), ('c', 1), ('b', 0)]

        '''
    """
    def __init__(self, iterable=None, /, **kwds):
        """
        '''Create a new, empty Counter object.  And if given, count elements
                from an input iterable.  Or, initialize the count from another mapping
                of elements to their counts.

                >>> c = Counter()                           # a new, empty counter
                >>> c = Counter('gallahad')                 # a new counter from an iterable
                >>> c = Counter({'a': 4, 'b': 2})           # a new counter from a mapping
                >>> c = Counter(a=4, b=2)                   # a new counter from keyword args

                '''
        """
    def __missing__(self, key):
        """
        'The count of elements not in the Counter is zero.'
        """
    def most_common(self, n=None):
        """
        '''List the n most common elements and their counts from the most
                common to the least.  If n is None, then list all element counts.

                >>> Counter('abracadabra').most_common(3)
                [('a', 5), ('b', 2), ('r', 2)]

                '''
        """
    def elements(self):
        """
        '''Iterator over elements repeating each as many times as its count.

                >>> c = Counter('ABCABC')
                >>> sorted(c.elements())
                ['A', 'A', 'B', 'B', 'C', 'C']

                # Knuth's example for prime factors of 1836:  2**2 * 3**3 * 17**1
                >>> prime_factors = Counter({2: 2, 3: 3, 17: 1})
                >>> product = 1
                >>> for factor in prime_factors.elements():     # loop over factors
                ...     product *= factor                       # and multiply them
                >>> product
                1836

                Note, if an element's count has been set to zero or is a negative
                number, elements() will ignore it.

                '''
        """
    def fromkeys(cls, iterable, v=None):
        """
         There is no equivalent method for counters because the semantics
         would be ambiguous in cases such as Counter.fromkeys('aaabbc', v=2).
         Initializing counters to zero values isn't necessary because zero
         is already the default value for counter lookups.  Initializing
         to one is easily accomplished with Counter(set(iterable)).  For
         more exotic cases, create a dictionary first using a dictionary
         comprehension or dict.fromkeys().

        """
    def update(self, iterable=None, /, **kwds):
        """
        '''Like dict.update() but add counts instead of replacing them.

                Source can be an iterable, a dictionary, or another Counter instance.

                >>> c = Counter('which')
                >>> c.update('witch')           # add elements from another iterable
                >>> d = Counter('watch')
                >>> c.update(d)                 # add elements from another counter
                >>> c['h']                      # four 'h' in which, witch, and watch
                4

                '''
        """
    def subtract(self, iterable=None, /, **kwds):
        """
        '''Like dict.update() but subtracts counts instead of replacing them.
                Counts can be reduced below zero.  Both the inputs and outputs are
                allowed to contain zero and negative counts.

                Source can be an iterable, a dictionary, or another Counter instance.

                >>> c = Counter('which')
                >>> c.subtract('witch')             # subtract elements from another iterable
                >>> c.subtract(Counter('watch'))    # subtract elements from another counter
                >>> c['h']                          # 2 in which, minus 1 in witch, minus 1 in watch
                0
                >>> c['w']                          # 1 in which, minus 1 in witch, minus 1 in watch
                -1

                '''
        """
    def copy(self):
        """
        'Return a shallow copy.'
        """
    def __reduce__(self):
        """
        'Like dict.__delitem__() but does not raise KeyError for missing values.'
        """
    def __repr__(self):
        """
        '%s()'
        """
    def __add__(self, other):
        """
        '''Add counts from two counters.

                >>> Counter('abbb') + Counter('bcc')
                Counter({'b': 4, 'c': 2, 'a': 1})

                '''
        """
    def __sub__(self, other):
        """
        ''' Subtract count, but keep only results with positive counts.

                >>> Counter('abbbc') - Counter('bccd')
                Counter({'b': 2, 'a': 1})

                '''
        """
    def __or__(self, other):
        """
        '''Union is the maximum of value in either of the input counters.

                >>> Counter('abbb') | Counter('bcc')
                Counter({'b': 3, 'c': 2, 'a': 1})

                '''
        """
    def __and__(self, other):
        """
        ''' Intersection is the minimum of corresponding counts.

                >>> Counter('abbb') & Counter('bcc')
                Counter({'b': 1})

                '''
        """
    def __pos__(self):
        """
        'Adds an empty counter, effectively stripping negative and zero counts'
        """
    def __neg__(self):
        """
        '''Subtracts from an empty counter.  Strips positive and zero counts,
                and flips the sign on negative counts.

                '''
        """
    def _keep_positive(self):
        """
        '''Internal method to strip elements with a negative or zero count'''
        """
    def __iadd__(self, other):
        """
        '''Inplace add from another counter, keeping only positive counts.

                >>> c = Counter('abbb')
                >>> c += Counter('bcc')
                >>> c
                Counter({'b': 4, 'c': 2, 'a': 1})

                '''
        """
    def __isub__(self, other):
        """
        '''Inplace subtract counter, but keep only results with positive counts.

                >>> c = Counter('abbbc')
                >>> c -= Counter('bccd')
                >>> c
                Counter({'b': 2, 'a': 1})

                '''
        """
    def __ior__(self, other):
        """
        '''Inplace union is the maximum of value from either counter.

                >>> c = Counter('abbb')
                >>> c |= Counter('bcc')
                >>> c
                Counter({'b': 3, 'c': 2, 'a': 1})

                '''
        """
    def __iand__(self, other):
        """
        '''Inplace intersection is the minimum of corresponding counts.

                >>> c = Counter('abbb')
                >>> c &= Counter('bcc')
                >>> c
                Counter({'b': 1})

                '''
        """
def ChainMap(_collections_abc.MutableMapping):
    """
    ''' A ChainMap groups multiple dicts (or other mappings) together
        to create a single, updateable view.

        The underlying mappings are stored in a list.  That list is public and can
        be accessed or updated using the *maps* attribute.  There is no other
        state.

        Lookups search the underlying mappings successively until a key is found.
        In contrast, writes, updates, and deletions only operate on the first
        mapping.

        '''
    """
    def __init__(self, *maps):
        """
        '''Initialize a ChainMap by setting *maps* to the given mappings.
                If no mappings are provided, a single empty dictionary is used.

                '''
        """
    def __missing__(self, key):
        """
         can't use 'key in mapping' with defaultdict
        """
    def get(self, key, default=None):
        """
         reuses stored hash values if possible
        """
    def __iter__(self):
        """
         reuses stored hash values if possible
        """
    def __contains__(self, key):
        """
        f'{self.__class__.__name__}({", ".join(map(repr, self.maps))})'
        """
    def fromkeys(cls, iterable, *args):
        """
        'Create a ChainMap with a single dict created from the iterable.'
        """
    def copy(self):
        """
        'New ChainMap or subclass with a new copy of maps[0] and refs to maps[1:]'
        """
    def new_child(self, m=None):                # like Django's Context.push()
        """
         like Django's Context.push()
        """
    def parents(self):                          # like Django's Context.pop()
        """
         like Django's Context.pop()
        """
    def __setitem__(self, key, value):
        """
        'Key not found in the first mapping: {!r}'
        """
    def popitem(self):
        """
        'Remove and return an item pair from maps[0]. Raise KeyError is maps[0] is empty.'
        """
    def pop(self, key, *args):
        """
        'Remove *key* from maps[0] and return its value. Raise KeyError if *key* not in maps[0].'
        """
    def clear(self):
        """
        'Clear maps[0], leaving maps[1:] intact.'
        """
def UserDict(_collections_abc.MutableMapping):
    """
     Start by filling-out the abstract methods

    """
    def __init__(*args, **kwargs):
        """
        descriptor '__init__' of 'UserDict' object 
        needs an argument
        """
    def __len__(self): return len(self.data)
        """
        __missing__
        """
    def __setitem__(self, key, item): self.data[key] = item
        """
         Modify __contains__ to work correctly when __missing__ is present

        """
    def __contains__(self, key):
        """
         Now, add the methods in dicts but not in MutableMapping

        """
    def __repr__(self): return repr(self.data)
        """
         Create a copy and avoid triggering descriptors

        """
    def copy(self):
        """

         UserList



        """
def UserList(_collections_abc.MutableSequence):
    """
    A more or less complete user-defined wrapper around list objects.
    """
    def __init__(self, initlist=None):
        """
         XXX should this accept an arbitrary sequence?

        """
    def __repr__(self): return repr(self.data)
        """
         Create a copy and avoid triggering descriptors

        """
    def append(self, item): self.data.append(item)
        """

         UserString



        """
def UserString(_collections_abc.Sequence):
    """
     the following methods are defined in alphabetical order:

    """
    def capitalize(self): return self.__class__(self.data.capitalize())
        """
        'utf-8'
        """
    def endswith(self, suffix, start=0, end=_sys.maxsize):
