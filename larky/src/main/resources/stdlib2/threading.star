def setprofile(func):
    """
    Set a profile function for all threads started from the threading module.

        The func will be passed to sys.setprofile() for each thread, before its
        run() method is called.

    
    """
def settrace(func):
    """
    Set a trace function for all threads started from the threading module.

        The func will be passed to sys.settrace() for each thread, before its run()
        method is called.

    
    """
def RLock(*args, **kwargs):
    """
    Factory function that returns a new reentrant lock.

        A reentrant lock must be released by the thread that acquired it. Once a
        thread has acquired a reentrant lock, the same thread may acquire it again
        without blocking; the thread must release it once for each time it has
        acquired it.

    
    """
def _RLock:
    """
    This class implements reentrant lock objects.

        A reentrant lock must be released by the thread that acquired it. Once a
        thread has acquired a reentrant lock, the same thread may acquire it
        again without blocking; the thread must release it once for each time it
        has acquired it.

    
    """
    def __init__(self):
        """
        <%s %s.%s object owner=%r count=%d at %s>
        """
    def acquire(self, blocking=True, timeout=-1):
        """
        Acquire a lock, blocking or non-blocking.

                When invoked without arguments: if this thread already owns the lock,
                increment the recursion level by one, and return immediately. Otherwise,
                if another thread owns the lock, block until the lock is unlocked. Once
                the lock is unlocked (not owned by any thread), then grab ownership, set
                the recursion level to one, and return. If more than one thread is
                blocked waiting until the lock is unlocked, only one at a time will be
                able to grab ownership of the lock. There is no return value in this
                case.

                When invoked with the blocking argument set to true, do the same thing
                as when called without arguments, and return true.

                When invoked with the blocking argument set to false, do not block. If a
                call without an argument would block, return false immediately;
                otherwise, do the same thing as when called without arguments, and
                return true.

                When invoked with the floating-point timeout argument set to a positive
                value, block for at most the number of seconds specified by timeout
                and as long as the lock cannot be acquired.  Return true if the lock has
                been acquired, false if the timeout has elapsed.

        
        """
    def release(self):
        """
        Release a lock, decrementing the recursion level.

                If after the decrement it is zero, reset the lock to unlocked (not owned
                by any thread), and if any other threads are blocked waiting for the
                lock to become unlocked, allow exactly one of them to proceed. If after
                the decrement the recursion level is still nonzero, the lock remains
                locked and owned by the calling thread.

                Only call this method when the calling thread owns the lock. A
                RuntimeError is raised if this method is called when the lock is
                unlocked.

                There is no return value.

        
        """
    def __exit__(self, t, v, tb):
        """
         Internal methods used by condition variables


        """
    def _acquire_restore(self, state):
        """
        cannot release un-acquired lock
        """
    def _is_owned(self):
        """
        Class that implements a condition variable.

            A condition variable allows one or more threads to wait until they are
            notified by another thread.

            If the lock argument is given and not None, it must be a Lock or RLock
            object, and it is used as the underlying lock. Otherwise, a new RLock object
            is created and used as the underlying lock.

    
        """
    def __init__(self, lock=None):
        """
         Export the lock's acquire() and release() methods

        """
    def __enter__(self):
        """
        <Condition(%s, %d)>
        """
    def _release_save(self):
        """
         No state to save
        """
    def _acquire_restore(self, x):
        """
         Ignore saved state
        """
    def _is_owned(self):
        """
         Return True if lock is owned by current_thread.
         This method is called only if _lock doesn't have _is_owned().

        """
    def wait(self, timeout=None):
        """
        Wait until notified or until a timeout occurs.

                If the calling thread has not acquired the lock when this method is
                called, a RuntimeError is raised.

                This method releases the underlying lock, and then blocks until it is
                awakened by a notify() or notify_all() call for the same condition
                variable in another thread, or until the optional timeout occurs. Once
                awakened or timed out, it re-acquires the lock and returns.

                When the timeout argument is present and not None, it should be a
                floating point number specifying a timeout for the operation in seconds
                (or fractions thereof).

                When the underlying lock is an RLock, it is not released using its
                release() method, since this may not actually unlock the lock when it
                was acquired multiple times recursively. Instead, an internal interface
                of the RLock class is used, which really unlocks it even when it has
                been recursively acquired several times. Another internal interface is
                then used to restore the recursion level when the lock is reacquired.

        
        """
    def wait_for(self, predicate, timeout=None):
        """
        Wait until a condition evaluates to True.

                predicate should be a callable which result will be interpreted as a
                boolean value.  A timeout may be provided giving the maximum time to
                wait.

        
        """
    def notify(self, n=1):
        """
        Wake up one or more threads waiting on this condition, if any.

                If the calling thread has not acquired the lock when this method is
                called, a RuntimeError is raised.

                This method wakes up at most n of the threads waiting for the condition
                variable; it is a no-op if no threads are waiting.

        
        """
    def notify_all(self):
        """
        Wake up all threads waiting on this condition.

                If the calling thread has not acquired the lock when this method
                is called, a RuntimeError is raised.

        
        """
def Semaphore:
    """
    This class implements semaphore objects.

        Semaphores manage a counter representing the number of release() calls minus
        the number of acquire() calls, plus an initial value. The acquire() method
        blocks if necessary until it can return without making the counter
        negative. If not given, value defaults to 1.

    
    """
    def __init__(self, value=1):
        """
        semaphore initial value must be >= 0
        """
    def acquire(self, blocking=True, timeout=None):
        """
        Acquire a semaphore, decrementing the internal counter by one.

                When invoked without arguments: if the internal counter is larger than
                zero on entry, decrement it by one and return immediately. If it is zero
                on entry, block, waiting until some other thread has called release() to
                make it larger than zero. This is done with proper interlocking so that
                if multiple acquire() calls are blocked, release() will wake exactly one
                of them up. The implementation may pick one at random, so the order in
                which blocked threads are awakened should not be relied on. There is no
                return value in this case.

                When invoked with blocking set to true, do the same thing as when called
                without arguments, and return true.

                When invoked with blocking set to false, do not block. If a call without
                an argument would block, return false immediately; otherwise, do the
                same thing as when called without arguments, and return true.

                When invoked with a timeout other than None, it will block for at
                most timeout seconds.  If acquire does not complete successfully in
                that interval, return false.  Return true otherwise.

        
        """
    def release(self):
        """
        Release a semaphore, incrementing the internal counter by one.

                When the counter is zero on entry and another thread is waiting for it
                to become larger than zero again, wake up that thread.

        
        """
    def __exit__(self, t, v, tb):
        """
        Implements a bounded semaphore.

            A bounded semaphore checks to make sure its current value doesn't exceed its
            initial value. If it does, ValueError is raised. In most situations
            semaphores are used to guard resources with limited capacity.

            If the semaphore is released too many times it's a sign of a bug. If not
            given, value defaults to 1.

            Like regular semaphores, bounded semaphores manage a counter representing
            the number of release() calls minus the number of acquire() calls, plus an
            initial value. The acquire() method blocks if necessary until it can return
            without making the counter negative. If not given, value defaults to 1.

    
        """
    def __init__(self, value=1):
        """
        Release a semaphore, incrementing the internal counter by one.

                When the counter is zero on entry and another thread is waiting for it
                to become larger than zero again, wake up that thread.

                If the number of releases exceeds the number of acquires,
                raise a ValueError.

        
        """
def Event:
    """
    Class implementing event objects.

        Events manage a flag that can be set to true with the set() method and reset
        to false with the clear() method. The wait() method blocks until the flag is
        true.  The flag is initially false.

    
    """
    def __init__(self):
        """
         private!  called by Thread._reset_internal_locks by _after_fork()

        """
    def is_set(self):
        """
        Return true if and only if the internal flag is true.
        """
    def set(self):
        """
        Set the internal flag to true.

                All threads waiting for it to become true are awakened. Threads
                that call wait() once the flag is true will not block at all.

        
        """
    def clear(self):
        """
        Reset the internal flag to false.

                Subsequently, threads calling wait() will block until set() is called to
                set the internal flag to true again.

        
        """
    def wait(self, timeout=None):
        """
        Block until the internal flag is true.

                If the internal flag is true on entry, return immediately. Otherwise,
                block until another thread calls set() to set the flag to true, or until
                the optional timeout occurs.

                When the timeout argument is present and not None, it should be a
                floating point number specifying a timeout for the operation in seconds
                (or fractions thereof).

                This method returns the internal flag on exit, so it will always return
                True except if a timeout is given and the operation times out.

        
        """
def Barrier:
    """
    Implements a Barrier.

        Useful for synchronizing a fixed number of threads at known synchronization
        points.  Threads block on 'wait()' and are simultaneously awoken once they
        have all made that call.

    
    """
    def __init__(self, parties, action=None, timeout=None):
        """
        Create a barrier, initialised to 'parties' threads.

                'action' is a callable which, when supplied, will be called by one of
                the threads after they have all entered the barrier and just prior to
                releasing them all. If a 'timeout' is provided, it is used as the
                default for all subsequent 'wait()' calls.

        
        """
    def wait(self, timeout=None):
        """
        Wait for the barrier.

                When the specified number of threads have started waiting, they are all
                simultaneously awoken. If an 'action' was provided for the barrier, one
                of the threads will have executed that callback prior to returning.
                Returns an individual index number from 0 to 'parties-1'.

        
        """
    def _enter(self):
        """
         It is draining or resetting, wait until done

        """
    def _release(self):
        """
         enter draining state

        """
    def _wait(self, timeout):
        """
        timed out.  Break the barrier

        """
    def _exit(self):
        """
        resetting or draining

        """
    def reset(self):
        """
        Reset the barrier to the initial state.

                Any threads currently waiting will get the BrokenBarrier exception
                raised.

        
        """
    def abort(self):
        """
        Place the barrier into a 'broken' state.

                Useful in case of error.  Any currently waiting threads and threads
                attempting to 'wait()' will have BrokenBarrierError raised.

        
        """
    def _break(self):
        """
         An internal error was detected.  The barrier is set to
         a broken state all parties awakened.

        """
    def parties(self):
        """
        Return the number of threads required to trip the barrier.
        """
    def n_waiting(self):
        """
        Return the number of threads currently waiting at the barrier.
        """
    def broken(self):
        """
        Return True if the barrier is in a broken state.
        """
def BrokenBarrierError(RuntimeError):
    """
     Helper to generate new thread names

    """
def _newname(template="Thread-%d"):
    """
     Active thread administration

    """
def Thread:
    """
    A class that represents a thread of control.

        This class can be safely subclassed in a limited fashion. There are two ways
        to specify the activity: by passing a callable object to the constructor, or
        by overriding the run() method in a subclass.

    
    """
2021-03-02 20:53:44,236 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, group=None, target=None, name=None,
                 args=(), kwargs=None, *, daemon=None):
        """
        This constructor should always be called with keyword arguments. Arguments are:

                *group* should be None; reserved for future extension when a ThreadGroup
                class is implemented.

                *target* is the callable object to be invoked by the run()
                method. Defaults to None, meaning nothing is called.

                *name* is the thread name. By default, a unique name is constructed of
                the form "Thread-N" where N is a small decimal number.

                *args* is the argument tuple for the target invocation. Defaults to ().

                *kwargs* is a dictionary of keyword arguments for the target
                invocation. Defaults to {}.

                If a subclass overrides the constructor, it must make sure to invoke
                the base class constructor (Thread.__init__()) before doing anything
                else to the thread.

        
        """
    def _reset_internal_locks(self, is_alive):
        """
         private!  Called by _after_fork() to reset our internal locks as
         they may be in an invalid state leading to a deadlock or crash.

        """
    def __repr__(self):
        """
        Thread.__init__() was not called
        """
    def start(self):
        """
        Start the thread's activity.

                It must be called at most once per thread object. It arranges for the
                object's run() method to be invoked in a separate thread of control.

                This method will raise a RuntimeError if called more than once on the
                same thread object.

        
        """
    def run(self):
        """
        Method representing the thread's activity.

                You may override this method in a subclass. The standard run() method
                invokes the callable object passed to the object's constructor as the
                target argument, if any, with sequential and keyword arguments taken
                from the args and kwargs arguments, respectively.

        
        """
    def _bootstrap(self):
        """
         Wrapper around the real bootstrap code that ignores
         exceptions during interpreter cleanup.  Those typically
         happen when a daemon thread wakes up at an unfortunate
         moment, finds the world around it destroyed, and raises some
         random exception *** while trying to report the exception in
         _bootstrap_inner() below ***.  Those random exceptions
         don't help anybody, and they confuse users, so we suppress
         them.  We suppress them only when it appears that the world
         indeed has already been destroyed, so that exceptions in
         _bootstrap_inner() during normal business hours are properly
         reported.  Also, we only suppress them for daemonic threads;
         if a non-daemonic encounters this, something else is wrong.

        """
    def _set_ident(self):
        """

                Set a lock object which will be released by the interpreter when
                the underlying thread state (see pystate.h) gets deleted.
        
        """
    def _bootstrap_inner(self):
        """
         We don't call self._delete() because it also
         grabs _active_limbo_lock.

        """
    def _stop(self):
        """
         After calling ._stop(), .is_alive() returns False and .join() returns
         immediately.  ._tstate_lock must be released before calling ._stop().

         Normal case:  C code at the end of the thread's life
         (release_sentinel in _threadmodule.c) releases ._tstate_lock, and
         that's detected by our ._wait_for_tstate_lock(), called by .join()
         and .is_alive().  Any number of threads _may_ call ._stop()
         simultaneously (for example, if multiple threads are blocked in
         .join() calls), and they're not serialized.  That's harmless -
         they'll just make redundant rebindings of ._is_stopped and
         ._tstate_lock.  Obscure:  we rebind ._tstate_lock last so that the
         "assert self._is_stopped" in ._wait_for_tstate_lock() always works
         (the assert is executed only if ._tstate_lock is None).

         Special case:  _main_thread releases ._tstate_lock via this
         module's _shutdown() function.

        """
    def _delete(self):
        """
        Remove current thread from the dict of currently running threads.
        """
    def join(self, timeout=None):
        """
        Wait until the thread terminates.

                This blocks the calling thread until the thread whose join() method is
                called terminates -- either normally or through an unhandled exception
                or until the optional timeout occurs.

                When the timeout argument is present and not None, it should be a
                floating point number specifying a timeout for the operation in seconds
                (or fractions thereof). As join() always returns None, you must call
                is_alive() after join() to decide whether a timeout happened -- if the
                thread is still alive, the join() call timed out.

                When the timeout argument is not present or None, the operation will
                block until the thread terminates.

                A thread can be join()ed many times.

                join() raises a RuntimeError if an attempt is made to join the current
                thread as that would cause a deadlock. It is also an error to join() a
                thread before it has been started and attempts to do so raises the same
                exception.

        
        """
    def _wait_for_tstate_lock(self, block=True, timeout=-1):
        """
         Issue #18808: wait for the thread state to be gone.
         At the end of the thread's life, after all knowledge of the thread
         is removed from C data structures, C code releases our _tstate_lock.
         This method passes its arguments to _tstate_lock.acquire().
         If the lock is acquired, the C code is done, and self._stop() is
         called.  That sets ._is_stopped to True, and ._tstate_lock to None.

        """
    def name(self):
        """
        A string used for identification purposes only.

                It has no semantics. Multiple threads may be given the same name. The
                initial name is set by the constructor.

        
        """
    def name(self, name):
        """
        Thread.__init__() not called
        """
    def ident(self):
        """
        Thread identifier of this thread or None if it has not been started.

                This is a nonzero integer. See the get_ident() function. Thread
                identifiers may be recycled when a thread exits and another thread is
                created. The identifier is available even after the thread has exited.

        
        """
        def native_id(self):
            """
            Native integral thread ID of this thread, or None if it has not been started.

                        This is a non-negative integer. See the get_native_id() function.
                        This represents the Thread ID as reported by the kernel.

            
            """
    def is_alive(self):
        """
        Return whether the thread is alive.

                This method returns True just before the run() method starts until just
                after the run() method terminates. The module function enumerate()
                returns a list of all alive threads.

        
        """
    def isAlive(self):
        """
        Return whether the thread is alive.

                This method is deprecated, use is_alive() instead.
        
        """
    def daemon(self):
        """
        A boolean value indicating whether this thread is a daemon thread.

                This must be set before start() is called, otherwise RuntimeError is
                raised. Its initial value is inherited from the creating thread; the
                main thread is not a daemon thread and therefore all threads created in
                the main thread default to daemon = False.

                The entire Python program exits when only daemon threads are left.

        
        """
    def daemon(self, daemonic):
        """
        Thread.__init__() not called
        """
    def isDaemon(self):
        """
         Simple Python implementation if _thread._excepthook() is not available

        """
    def ExceptHookArgs(args):
        """

                Handle uncaught Thread.run() exception.
        
        """
def _make_invoke_excepthook():
    """
     Create a local namespace to ensure that variables remain alive
     when _invoke_excepthook() is called, even if it is called late during
     Python shutdown. It is mostly needed for daemon threads.


    """
    def invoke_excepthook(thread):
        """
        Exception in threading.excepthook:
        """
def Timer(Thread):
    """
    Call a function after a specified number of seconds:

                t = Timer(30.0, f, args=None, kwargs=None)
                t.start()
                t.cancel()     # stop the timer's action if it's still waiting

    
    """
    def __init__(self, interval, function, args=None, kwargs=None):
        """
        Stop the timer if it hasn't finished yet.
        """
    def run(self):
        """
         Special thread class to represent the main thread


        """
def _MainThread(Thread):
    """
    MainThread
    """
def _DummyThread(Thread):
    """
    Dummy-%d
    """
    def _stop(self):
        """
        cannot join a dummy thread
        """
def current_thread():
    """
    Return the current Thread object, corresponding to the caller's thread of control.

        If the caller's thread of control was not created through the threading
        module, a dummy thread object with limited functionality is returned.

    
    """
def active_count():
    """
    Return the number of Thread objects currently alive.

        The returned count is equal to the length of the list returned by
        enumerate().

    
    """
def _enumerate():
    """
     Same as enumerate(), but without the lock. Internal use only.

    """
def enumerate():
    """
    Return a list of all Thread objects currently alive.

        The list includes daemonic threads, dummy thread objects created by
        current_thread(), and the main thread. It excludes terminated threads and
        threads that have not yet been started.

    
    """
def _shutdown():
    """

        Wait until the Python thread state of all non-daemon threads get deleted.
    
    """
def main_thread():
    """
    Return the main thread object.

        In normal conditions, the main thread is the thread from which the
        Python interpreter was started.
    
    """
def _after_fork():
    """

        Cleanup threading module state that should not exist after a fork.
    
    """
