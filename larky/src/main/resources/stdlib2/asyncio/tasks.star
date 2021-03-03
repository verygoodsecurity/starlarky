def current_task(loop=None):
    """
    Return a currently executed task.
    """
def all_tasks(loop=None):
    """
    Return a set of all tasks for the loop.
    """
def _all_tasks_compat(loop=None):
    """
     Different from "all_task()" by returning *all* Tasks, including
     the completed ones.  Used to implement deprecated "Tasks.all_task()
     method.

    """
def _set_task_name(task, name):
    """
     Inherit Python Task implementation
    """
    def current_task(cls, loop=None):
        """
        Return the currently running task in an event loop or None.

                By default the current task for the current event loop is returned.

                None is returned when called not in the context of a Task.
        
        """
    def all_tasks(cls, loop=None):
        """
        Return a set of all tasks for an event loop.

                By default all tasks for the current event loop are returned.
        
        """
    def __init__(self, coro, *, loop=None, name=None):
        """
         raise after Future.__init__(), attrs are required for __del__
         prevent logging for pending task in __del__

        """
    def __del__(self):
        """
        'task'
        """
    def _repr_info(self):
        """
        'Task does not support set_result operation'
        """
    def set_exception(self, exception):
        """
        'Task does not support set_exception operation'
        """
    def get_stack(self, *, limit=None):
        """
        Return the list of stack frames for this task's coroutine.

                If the coroutine is not done, this returns the stack where it is
                suspended.  If the coroutine has completed successfully or was
                cancelled, this returns an empty list.  If the coroutine was
                terminated by an exception, this returns the list of traceback
                frames.

                The frames are always ordered from oldest to newest.

                The optional limit gives the maximum number of frames to
                return; by default all available frames are returned.  Its
                meaning differs depending on whether a stack or a traceback is
                returned: the newest frames of a stack are returned, but the
                oldest frames of a traceback are returned.  (This matches the
                behavior of the traceback module.)

                For reasons beyond our control, only one stack frame is
                returned for a suspended coroutine.
        
        """
    def print_stack(self, *, limit=None, file=None):
        """
        Print the stack or traceback for this task's coroutine.

                This produces output similar to that of the traceback module,
                for the frames retrieved by get_stack().  The limit argument
                is passed to get_stack().  The file argument is an I/O stream
                to which the output is written; by default output is written
                to sys.stderr.
        
        """
    def cancel(self):
        """
        Request that this task cancel itself.

                This arranges for a CancelledError to be thrown into the
                wrapped coroutine on the next cycle through the event loop.
                The coroutine then has a chance to clean up or even deny
                the request using try/except/finally.

                Unlike Future.cancel, this does not guarantee that the
                task will be cancelled: the exception might be caught and
                acted upon, delaying cancellation of the task or preventing
                cancellation completely.  The task may also return a value or
                raise a different exception.

                Immediately after this method is called, Task.cancelled() will
                not return True (unless the task was already cancelled).  A
                task will be marked as cancelled when the wrapped coroutine
                terminates with a CancelledError exception (even if cancel()
                was not called).
        
        """
    def __step(self, exc=None):
        """
        f'_step(): already done: {self!r}, {exc!r}'
        """
    def __wakeup(self, future):
        """
         This may also be a cancellation.

        """
def create_task(coro, *, name=None):
    """
    Schedule the execution of a coroutine object in a spawn task.

        Return a Task object.
    
    """
async def wait(fs, *, loop=None, timeout=None, return_when=ALL_COMPLETED):
        """
        Wait for the Futures and coroutines given by fs to complete.

            The sequence futures must not be empty.

            Coroutines will be wrapped in Tasks.

            Returns two sets of Future: (done, pending).

            Usage:

                done, pending = await asyncio.wait(fs)

            Note: This does not raise TimeoutError! Futures that aren't done
            when the timeout occurs are returned in the second set.
    
        """
def _release_waiter(waiter, *args):
    """
    Wait for the single Future or coroutine to complete, with timeout.

        Coroutine will be wrapped in Task.

        Returns result of the Future or coroutine.  When a timeout occurs,
        it cancels the task and raises TimeoutError.  To avoid the task
        cancellation, wrap it in shield().

        If the wait is cancelled, the task is also cancelled.

        This function is a coroutine.
    
    """
async def _wait(fs, timeout, return_when, loop):
        """
        Internal helper for wait().

            The fs argument must be a collection of Futures.
    
        """
    def _on_completion(f):
        """
        Cancel the *fut* future or task and wait until it completes.
        """
def as_completed(fs, *, loop=None, timeout=None):
    """
    Return an iterator whose values are coroutines.

        When waiting for the yielded coroutines you'll get the results (or
        exceptions!) of the original Futures (or coroutines), in the order
        in which and as soon as they complete.

        This differs from PEP 3148; the proper way to use this is:

            for f in as_completed(fs):
                result = await f  # The 'await' may raise.
                # Use result.

        If a timeout is specified, the 'await' will raise
        TimeoutError when the timeout occurs before all Futures are done.

        Note: The futures 'f' are not necessarily members of fs.
    
    """
    def _on_timeout():
        """
         Queue a dummy value for _wait_for_one().
        """
    def _on_completion(f):
        """
         _on_timeout() was here first.
        """
    async def _wait_for_one():
            """
             Dummy value from _on_timeout().

            """
def __sleep0():
    """
    Skip one event loop run cycle.

        This is a private helper for 'asyncio.sleep()', used
        when the 'delay' is set to 0.  It uses a bare 'yield'
        expression (which Task.__step knows how to handle)
        instead of creating a Future object.
    
    """
async def sleep(delay, result=None, *, loop=None):
        """
        Coroutine that completes after a given time (in seconds).
        """
def ensure_future(coro_or_future, *, loop=None):
    """
    Wrap a coroutine or an awaitable in a future.

        If the argument is a Future, it is returned directly.
    
    """
def _wrap_awaitable(awaitable):
    """
    Helper for asyncio.ensure_future().

        Wraps awaitable (an object with __await__) into a coroutine
        that will later be wrapped in a Task by ensure_future().
    
    """
def _GatheringFuture(futures.Future):
    """
    Helper for gather().

        This overrides cancel() to cancel all the children and act more
        like Task.cancel(), which doesn't immediately mark itself as
        cancelled.
    
    """
    def __init__(self, children, *, loop=None):
        """
         If any child tasks were actually cancelled, we should
         propagate the cancellation request regardless of
         *return_exceptions* argument.  See issue 32684.

        """
def gather(*coros_or_futures, loop=None, return_exceptions=False):
    """
    Return a future aggregating results from the given coroutines/futures.

        Coroutines will be wrapped in a future and scheduled in the event
        loop. They will not necessarily be scheduled in the same order as
        passed in.

        All futures must share the same event loop.  If all the tasks are
        done successfully, the returned future's result is the list of
        results (in the order of the original sequence, not necessarily
        the order of results arrival).  If *return_exceptions* is True,
        exceptions in the tasks are treated the same as successful
        results, and gathered in the result list; otherwise, the first
        raised exception will be immediately propagated to the returned
        future.

        Cancellation: if the outer Future is cancelled, all children (that
        have not completed yet) are also cancelled.  If any child is
        cancelled, this is treated as if it raised CancelledError --
        the outer Future is *not* cancelled in this case.  (This is to
        prevent the cancellation of one child to cause other children to
        be cancelled.)

        If *return_exceptions* is False, cancelling gather() after it
        has been marked done won't cancel any submitted awaitables.
        For instance, gather can be marked done after propagating an
        exception to the caller, therefore, calling ``gather.cancel()``
        after catching an exception (raised by one of the awaitables) from
        gather won't cancel any other awaitables.
    
    """
    def _done_callback(fut):
        """
         Mark exception retrieved.

        """
def shield(arg, *, loop=None):
    """
    Wait for a future, shielding it from cancellation.

        The statement

            res = await shield(something())

        is exactly equivalent to the statement

            res = await something()

        *except* that if the coroutine containing it is cancelled, the
        task running in something() is not cancelled.  From the POV of
        something(), the cancellation did not happen.  But its caller is
        still cancelled, so the yield-from expression still raises
        CancelledError.  Note: If something() is cancelled by other means
        this will still cancel shield().

        If you want to completely ignore cancellation (not recommended)
        you can combine shield() with a try/except clause, as follows:

            try:
                res = await shield(something())
            except CancelledError:
                res = None
    
    """
    def _inner_done_callback(inner):
        """
         Mark inner's result as retrieved.

        """
    def _outer_done_callback(outer):
        """
        Submit a coroutine object to a given event loop.

            Return a concurrent.futures.Future to access the result.
    
        """
    def callback():
        """
         WeakSet containing all alive tasks.

        """
def _register_task(task):
    """
    Register a new task in asyncio as executed by loop.
    """
def _enter_task(loop, task):
    """
    f"Cannot enter into task {task!r} while another 
    f"task {current_task!r} is being executed.
    """
def _leave_task(loop, task):
    """
    f"Leaving task {task!r} does not match 
    f"the current task {current_task!r}.
    """
def _unregister_task(task):
    """
    Unregister a task.
    """
