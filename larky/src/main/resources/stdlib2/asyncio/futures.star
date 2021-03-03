def Future:
    """
    This class is *almost* compatible with concurrent.futures.Future.

        Differences:

        - This class is not thread-safe.

        - result() and exception() do not take a timeout argument and
          raise an exception when the future isn't done yet.

        - Callbacks registered with add_done_callback() are always called
          via the event loop's call_soon().

        - This class is not compatible with the wait() and as_completed()
          methods in the concurrent.futures package.

        (In Python 3.4 or later we may be able to unify the implementations.)
    
    """
    def __init__(self, *, loop=None):
        """
        Initialize the future.

                The optional event_loop argument allows explicitly setting the event
                loop object used by the future. If it's not provided, the future uses
                the default event loop.
        
        """
    def __repr__(self):
        """
        '<{} {}>'
        """
    def __del__(self):
        """
         set_exception() was not called, or result() or exception()
         has consumed the exception

        """
    def _log_traceback(self):
        """
        '_log_traceback can only be set to False'
        """
    def get_loop(self):
        """
        Return the event loop the Future is bound to.
        """
    def cancel(self):
        """
        Cancel the future and schedule callbacks.

                If the future is already done or cancelled, return False.  Otherwise,
                change the future's state to cancelled, schedule the callbacks and
                return True.
        
        """
    def __schedule_callbacks(self):
        """
        Internal: Ask the event loop to call all callbacks.

                The callbacks are scheduled to be called as soon as possible. Also
                clears the callback list.
        
        """
    def cancelled(self):
        """
        Return True if the future was cancelled.
        """
    def done(self):
        """
        Return True if the future is done.

                Done means either that a result / exception are available, or that the
                future was cancelled.
        
        """
    def result(self):
        """
        Return the result this future represents.

                If the future has been cancelled, raises CancelledError.  If the
                future's result isn't yet available, raises InvalidStateError.  If
                the future is done and has an exception set, this exception is raised.
        
        """
    def exception(self):
        """
        Return the exception that was set on this future.

                The exception (or None if no exception was set) is returned only if
                the future is done.  If the future has been cancelled, raises
                CancelledError.  If the future isn't done yet, raises
                InvalidStateError.
        
        """
    def add_done_callback(self, fn, *, context=None):
        """
        Add a callback to be run when the future becomes done.

                The callback is called with a single argument - the future object. If
                the future is already done when this is called, the callback is
                scheduled with call_soon.
        
        """
    def remove_done_callback(self, fn):
        """
        Remove all instances of a callback from the "call when done" list.

                Returns the number of callbacks removed.
        
        """
    def set_result(self, result):
        """
        Mark the future done and set its result.

                If the future is already done when this method is called, raises
                InvalidStateError.
        
        """
    def set_exception(self, exception):
        """
        Mark the future done and set an exception.

                If the future is already done when this method is called, raises
                InvalidStateError.
        
        """
    def __await__(self):
        """
         This tells Task to wait for completion.
        """
def _get_loop(fut):
    """
     Tries to call Future.get_loop() if it's available.
     Otherwise fallbacks to using the old '_loop' property.

    """
def _set_result_unless_cancelled(fut, result):
    """
    Helper setting the result only if the future was not cancelled.
    """
def _convert_future_exc(exc):
    """
    Copy state from a future to a concurrent.futures.Future.
    """
def _copy_future_state(source, dest):
    """
    Internal helper to copy state from another Future.

        The other Future may be a concurrent.futures.Future.
    
    """
def _chain_future(source, destination):
    """
    Chain two futures so that when one completes, so does the other.

        The result (or exception) of source will be copied to destination.
        If destination is cancelled, source gets cancelled too.
        Compatible with both asyncio.Future and concurrent.futures.Future.
    
    """
    def _set_state(future, other):
        """
        Wrap concurrent.futures.Future object.
        """
