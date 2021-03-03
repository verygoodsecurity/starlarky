def heappush(heap, item):
    """
    Push item onto heap, maintaining the heap invariant.
    """
def heappop(heap):
    """
    Pop the smallest item off the heap, maintaining the heap invariant.
    """
def heapreplace(heap, item):
    """
    Pop and return the current smallest value, and add the new item.

        This is more efficient than heappop() followed by heappush(), and can be
        more appropriate when using a fixed-size heap.  Note that the value
        returned may be larger than item!  That constrains reasonable uses of
        this routine unless written as part of a conditional replacement:

            if item > heap[0]:
                item = heapreplace(heap, item)
    
    """
def heappushpop(heap, item):
    """
    Fast version of a heappush followed by a heappop.
    """
def heapify(x):
    """
    Transform list into a heap, in-place, in O(len(x)) time.
    """
def _heappop_max(heap):
    """
    Maxheap version of a heappop.
    """
def _heapreplace_max(heap, item):
    """
    Maxheap version of a heappop followed by a heappush.
    """
def _heapify_max(x):
    """
    Transform list into a maxheap, in-place, in O(len(x)) time.
    """
def _siftdown(heap, startpos, pos):
    """
     Follow the path to the root, moving parents down until finding a place
     newitem fits.

    """
def _siftup(heap, pos):
    """
     Bubble up the smaller child until hitting a leaf.

    """
def _siftdown_max(heap, startpos, pos):
    """
    'Maxheap variant of _siftdown'
    """
def _siftup_max(heap, pos):
    """
    'Maxheap variant of _siftup'
    """
def merge(*iterables, key=None, reverse=False):
    """
    '''Merge multiple sorted inputs into a single sorted output.

        Similar to sorted(itertools.chain(*iterables)) but returns a generator,
        does not pull the data into memory all at once, and assumes that each of
        the input streams is already sorted (smallest to largest).

        >>> list(merge([1,3,5,7], [0,2,4,8], [5,10,15,20], [], [25]))
        [0, 1, 2, 3, 4, 5, 5, 7, 8, 10, 15, 20, 25]

        If *key* is not None, applies a key function to each element to determine
        its sort order.

        >>> list(merge(['dog', 'horse'], ['cat', 'fish', 'kangaroo'], key=len))
        ['dog', 'cat', 'fish', 'horse', 'kangaroo']

        '''
    """
def nsmallest(n, iterable, key=None):
    """
    Find the n smallest elements in a dataset.

        Equivalent to:  sorted(iterable, key=key)[:n]
    
    """
def nlargest(n, iterable, key=None):
    """
    Find the n largest elements in a dataset.

        Equivalent to:  sorted(iterable, key=key, reverse=True)[:n]
    
    """
