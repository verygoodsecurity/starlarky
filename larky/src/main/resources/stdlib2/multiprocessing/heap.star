    def Arena(object):
    """

            A shared memory area backed by anonymous memory (Windows).
        
    """
        def __init__(self, size):
            """
            'pym-%d-%s'
            """
        def __getstate__(self):
            """
             Reopen existing mmap

            """
    def Arena(object):
    """

            A shared memory area backed by a temporary file (POSIX).
        
    """
        def __init__(self, size, fd=-1):
            """
             Arena is created anew (if fd != -1, it means we're coming
             from rebuild_arena() below)

            """
        def _choose_dir(self, size):
            """
             Choose a non-storage backed directory if possible,
             to improve performance

            """
    def reduce_arena(a):
        """
        'Arena is unpicklable because '
        'forking was enabled when it was created'
        """
    def rebuild_arena(size, dupfd):
        """

         Class allowing allocation of chunks of memory from arenas



        """
def Heap(object):
    """
     Minimum malloc() alignment

    """
    def __init__(self, size=mmap.PAGESIZE):
        """
         Current arena allocation size

        """
    def _roundup(n, alignment):
        """
         alignment must be a power of 2

        """
    def _new_arena(self, size):
        """
         Create a new arena with at least the given *size*

        """
    def _discard_arena(self, arena):
        """
         Possibly delete the given (unused) arena

        """
    def _malloc(self, size):
        """
         returns a large enough block -- it might be much larger

        """
    def _add_free_block(self, block):
        """
         make block available and try to merge with its neighbours in the arena

        """
    def _absorb(self, block):
        """
         deregister this block so it can be merged with a neighbour

        """
    def _remove_allocated_block(self, block):
        """
         Arena is entirely free, discard it from this process

        """
    def _free_pending_blocks(self):
        """
         Free all the blocks in the pending list - called with the lock held.

        """
    def free(self, block):
        """
         free a block returned by malloc()
         Since free() can be called asynchronously by the GC, it could happen
         that it's called while self._lock is held: in that case,
         self._lock.acquire() would deadlock (issue #12352). To avoid that, a
         trylock is used instead, and if the lock can't be acquired
         immediately, the block is added to a list of blocks to be freed
         synchronously sometimes later from malloc() or free(), by calling
         _free_pending_blocks() (appending and retrieving from a list is not
         strictly thread-safe but under CPython it's atomic thanks to the GIL).

        """
    def malloc(self, size):
        """
         return a block of right size (possibly rounded up)

        """
def BufferWrapper(object):
    """
    Size {0:n} out of range
    """
    def create_memoryview(self):
