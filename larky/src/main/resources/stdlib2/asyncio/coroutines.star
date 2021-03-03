def _is_debug_mode():
    """
     If you set _DEBUG to true, @coroutine will wrap the resulting
     generator objects in a CoroWrapper instance (defined below).  That
     instance will log a message when the generator is never iterated
     over, which may happen when you forget to use "await" or "yield from
     with a coroutine call.
     Note that the value of the _DEBUG flag is taken
     when the decorator is used, so to be of any use it must be set
     before you define your coroutines.  A downside of using this feature
     is that tracebacks show entries for the CoroWrapper.__next__ method
     when _DEBUG is true.

    """
def CoroWrapper:
    """
     Wrapper for coroutine object in _DEBUG mode.


    """
    def __init__(self, gen, func=None):
        """
         Used to unwrap @coroutine decorator
        """
    def __repr__(self):
        """
        f', created at {frame[0]}:{frame[1]}'
        """
    def __iter__(self):
        """
         Be careful accessing self.gen.frame -- self.gen might not exist.

        """
def coroutine(func):
    """
    Decorator to mark coroutines.

        If the coroutine is not yielded from before it is destroyed,
        an error message is logged.
    
    """
        def coro(*args, **kw):
            """
             If 'res' is an awaitable, run it.

            """
        def wrapper(*args, **kwds):
            """
             Python < 3.5 does not implement __qualname__
             on generator objects, so we set it manually.
             We use getattr as some callables (such as
             functools.partial may lack __qualname__).

            """
def iscoroutinefunction(func):
    """
    Return True if func is a decorated coroutine function.
    """
def iscoroutine(obj):
    """
    Return True if obj is a coroutine object.
    """
def _format_coroutine(coro):
    """
     Coroutines compiled with Cython sometimes don't have
     proper __qualname__ or __name__.  While that is a bug
     in Cython, asyncio shouldn't crash with an AttributeError
     in its __repr__ functions.

    """
    def is_running(coro):
        """
        'cr_code'
        """
