def QueueEmpty(Exception):
    """
    Raised when Queue.get_nowait() is called on an empty Queue.
    """
def QueueFull(Exception):
    """
    Raised when the Queue.put_nowait() method is called on a full Queue.
    """
def Queue:
    """
    A queue, useful for coordinating producer and consumer coroutines.

        If maxsize is less than or equal to zero, the queue size is infinite. If it
        is an integer greater than 0, then "await put()" will block when the
        queue reaches maxsize, until an item is removed by get().

        Unlike the standard library Queue, you can reliably know this Queue's size
        with qsize(), since your single-threaded asyncio application won't be
        interrupted between calling qsize() and doing an operation on the Queue.
    
    """
    def __init__(self, maxsize=0, *, loop=None):
        """
        The loop argument is deprecated since Python 3.8, 
        and scheduled for removal in Python 3.10.
        """
    def _init(self, maxsize):
        """
         End of the overridable methods.


        """
    def _wakeup_next(self, waiters):
        """
         Wake up the next waiter (if any) that isn't cancelled.

        """
    def __repr__(self):
        """
        f'<{type(self).__name__} at {id(self):#x} {self._format()}>'
        """
    def __str__(self):
        """
        f'<{type(self).__name__} {self._format()}>'
        """
    def _format(self):
        """
        f'maxsize={self._maxsize!r}'
        """
    def qsize(self):
        """
        Number of items in the queue.
        """
    def maxsize(self):
        """
        Number of items allowed in the queue.
        """
    def empty(self):
        """
        Return True if the queue is empty, False otherwise.
        """
    def full(self):
        """
        Return True if there are maxsize items in the queue.

                Note: if the Queue was initialized with maxsize=0 (the default),
                then full() is never True.
        
        """
    async def put(self, item):
            """
            Put an item into the queue.

                    Put an item into the queue. If the queue is full, wait until a free
                    slot is available before adding item.
        
            """
    def put_nowait(self, item):
        """
        Put an item into the queue without blocking.

                If no free slot is immediately available, raise QueueFull.
        
        """
    async def get(self):
            """
            Remove and return an item from the queue.

                    If queue is empty, wait until an item is available.
        
            """
    def get_nowait(self):
        """
        Remove and return an item from the queue.

                Return an item if one is immediately available, else raise QueueEmpty.
        
        """
    def task_done(self):
        """
        Indicate that a formerly enqueued task is complete.

                Used by queue consumers. For each get() used to fetch a task,
                a subsequent call to task_done() tells the queue that the processing
                on the task is complete.

                If a join() is currently blocking, it will resume when all items have
                been processed (meaning that a task_done() call was received for every
                item that had been put() into the queue).

                Raises ValueError if called more times than there were items placed in
                the queue.
        
        """
    async def join(self):
            """
            Block until all items in the queue have been gotten and processed.

                    The count of unfinished tasks goes up whenever an item is added to the
                    queue. The count goes down whenever a consumer calls task_done() to
                    indicate that the item was retrieved and all work on it is complete.
                    When the count of unfinished tasks drops to zero, join() unblocks.
        
            """
def PriorityQueue(Queue):
    """
    A subclass of Queue; retrieves entries in priority order (lowest first).

        Entries are typically tuples of the form: (priority number, data).
    
    """
    def _init(self, maxsize):
        """
        A subclass of Queue that retrieves most recently added entries first.
        """
    def _init(self, maxsize):
