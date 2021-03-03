def _ContextManager:
    """
    Context manager.

        This enables the following idiom for acquiring and releasing a
        lock around a block:

            with (yield from lock):
                <block>

        while failing loudly when accidentally using:

            with lock:
                <block>

        Deprecated, use 'async with' statement:
            async with lock:
                <block>
    
    """
    def __init__(self, lock):
        """
         We have no use for the "as ..."  clause in the with
         statement for locks.

        """
    def __exit__(self, *args):
        """
         Crudely prevent reuse.
        """
def _ContextManagerMixin:
    """
    '"yield from" should be used as context manager expression'
    """
    def __exit__(self, *args):
        """
         This must exist because __enter__ exists, even though that
         always raises; that's how the with-statement works.

        """
    def __iter__(self):
        """
         This is not a coroutine.  It is meant to enable the idiom:

             with (yield from lock):
                 <block>

         as an alternative to:

             yield from lock.acquire()
             try:
                 <block>
             finally:
                 lock.release()
         Deprecated, use 'async with' statement:
             async with lock:
                 <block>

        """
    async def __acquire_ctx(self):
            """
            'with await lock' is deprecated 
            use 'async with lock' instead
            """
    async def __aenter__(self):
            """
             We have no use for the "as ..."  clause in the with
             statement for locks.

            """
    async def __aexit__(self, exc_type, exc, tb):
            """
            Primitive lock objects.

                A primitive lock is a synchronization primitive that is not owned
                by a particular coroutine when locked.  A primitive lock is in one
                of two states, 'locked' or 'unlocked'.

                It is created in the unlocked state.  It has two basic methods,
                acquire() and release().  When the state is unlocked, acquire()
                changes the state to locked and returns immediately.  When the
                state is locked, acquire() blocks until a call to release() in
                another coroutine changes it to unlocked, then the acquire() call
                resets it to locked and returns.  The release() method should only
                be called in the locked state; it changes the state to unlocked
                and returns immediately.  If an attempt is made to release an
                unlocked lock, a RuntimeError will be raised.

                When more than one coroutine is blocked in acquire() waiting for
                the state to turn to unlocked, only one coroutine proceeds when a
                release() call resets the state to unlocked; first coroutine which
                is blocked in acquire() is being processed.

                acquire() is a coroutine and should be called with 'await'.

                Locks also support the asynchronous context management protocol.
                'async with lock' statement should be used.

                Usage:

                    lock = Lock()
                    ...
                    await lock.acquire()
                    try:
                        ...
                    finally:
                        lock.release()

                Context manager usage:

                    lock = Lock()
                    ...
                    async with lock:
                         ...

                Lock objects can be tested for locking state:

                    if not lock.locked():
                       await lock.acquire()
                    else:
                       # lock is acquired
                       ...

    
            """
    def __init__(self, *, loop=None):
        """
        The loop argument is deprecated since Python 3.8, 
        and scheduled for removal in Python 3.10.
        """
    def __repr__(self):
        """
        'locked'
        """
    def locked(self):
        """
        Return True if lock is acquired.
        """
    async def acquire(self):
            """
            Acquire a lock.

                    This method blocks until the lock is unlocked, then sets it to
                    locked and returns True.
        
            """
    def release(self):
        """
        Release a lock.

                When the lock is locked, reset it to unlocked, and return.
                If any other coroutines are blocked waiting for the lock to become
                unlocked, allow exactly one of them to proceed.

                When invoked on an unlocked lock, a RuntimeError is raised.

                There is no return value.
        
        """
    def _wake_up_first(self):
        """
        Wake up the first waiter if it isn't done.
        """
def Event:
    """
    Asynchronous equivalent to threading.Event.

        Class implementing event objects. An event manages a flag that can be set
        to true with the set() method and reset to false with the clear() method.
        The wait() method blocks until the flag is true. The flag is initially
        false.
    
    """
    def __init__(self, *, loop=None):
        """
        The loop argument is deprecated since Python 3.8, 
        and scheduled for removal in Python 3.10.
        """
    def __repr__(self):
        """
        'set'
        """
    def is_set(self):
        """
        Return True if and only if the internal flag is true.
        """
    def set(self):
        """
        Set the internal flag to true. All coroutines waiting for it to
                become true are awakened. Coroutine that call wait() once the flag is
                true will not block at all.
        
        """
    def clear(self):
        """
        Reset the internal flag to false. Subsequently, coroutines calling
                wait() will block until set() is called to set the internal flag
                to true again.
        """
    async def wait(self):
            """
            Block until the internal flag is true.

                    If the internal flag is true on entry, return True
                    immediately.  Otherwise, block until another coroutine calls
                    set() to set the flag to true, then return True.
        
            """
def Condition(_ContextManagerMixin):
    """
    Asynchronous equivalent to threading.Condition.

        This class implements condition variable objects. A condition variable
        allows one or more coroutines to wait until they are notified by another
        coroutine.

        A new Lock object is created and used as the underlying lock.
    
    """
    def __init__(self, lock=None, *, loop=None):
        """
        The loop argument is deprecated since Python 3.8, 
        and scheduled for removal in Python 3.10.
        """
    def __repr__(self):
        """
        'locked'
        """
    async def wait(self):
            """
            Wait until notified.

                    If the calling coroutine has not acquired the lock when this
                    method is called, a RuntimeError is raised.

                    This method releases the underlying lock, and then blocks
                    until it is awakened by a notify() or notify_all() call for
                    the same condition variable in another coroutine.  Once
                    awakened, it re-acquires the lock and returns True.
        
            """
    async def wait_for(self, predicate):
            """
            Wait until a predicate becomes true.

                    The predicate should be a callable which result will be
                    interpreted as a boolean value.  The final predicate value is
                    the return value.
        
            """
    def notify(self, n=1):
        """
        By default, wake up one coroutine waiting on this condition, if any.
                If the calling coroutine has not acquired the lock when this method
                is called, a RuntimeError is raised.

                This method wakes up at most n of the coroutines waiting for the
                condition variable; it is a no-op if no coroutines are waiting.

                Note: an awakened coroutine does not actually return from its
                wait() call until it can reacquire the lock. Since notify() does
                not release the lock, its caller should.
        
        """
    def notify_all(self):
        """
        Wake up all threads waiting on this condition. This method acts
                like notify(), but wakes up all waiting threads instead of one. If the
                calling thread has not acquired the lock when this method is called,
                a RuntimeError is raised.
        
        """
def Semaphore(_ContextManagerMixin):
    """
    A Semaphore implementation.

        A semaphore manages an internal counter which is decremented by each
        acquire() call and incremented by each release() call. The counter
        can never go below zero; when acquire() finds that it is zero, it blocks,
        waiting until some other thread calls release().

        Semaphores also support the context management protocol.

        The optional argument gives the initial value for the internal
        counter; it defaults to 1. If the value given is less than 0,
        ValueError is raised.
    
    """
    def __init__(self, value=1, *, loop=None):
        """
        Semaphore initial value must be >= 0
        """
    def __repr__(self):
        """
        'locked'
        """
    def _wake_up_next(self):
        """
        Returns True if semaphore can not be acquired immediately.
        """
    async def acquire(self):
            """
            Acquire a semaphore.

                    If the internal counter is larger than zero on entry,
                    decrement it by one and return True immediately.  If it is
                    zero on entry, block, waiting until some other coroutine has
                    called release() to make it larger than 0, and then return
                    True.
        
            """
    def release(self):
        """
        Release a semaphore, incrementing the internal counter by one.
                When it was zero on entry and another coroutine is waiting for it to
                become larger than zero again, wake up that coroutine.
        
        """
def BoundedSemaphore(Semaphore):
    """
    A bounded semaphore implementation.

        This raises ValueError in release() if it would increase the value
        above the initial value.
    
    """
    def __init__(self, value=1, *, loop=None):
        """
        The loop argument is deprecated since Python 3.8, 
        and scheduled for removal in Python 3.10.
        """
    def release(self):
        """
        'BoundedSemaphore released too many times'
        """
