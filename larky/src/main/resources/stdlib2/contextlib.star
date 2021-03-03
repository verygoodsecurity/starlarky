def AbstractContextManager(abc.ABC):
    """
    An abstract base class for context managers.
    """
    def __enter__(self):
        """
        Return `self` upon entering the runtime context.
        """
    def __exit__(self, exc_type, exc_value, traceback):
        """
        Raise any exception triggered within the runtime context.
        """
    def __subclasshook__(cls, C):
        """
        __enter__
        """
def AbstractAsyncContextManager(abc.ABC):
    """
    An abstract base class for asynchronous context managers.
    """
    async def __aenter__(self):
            """
            Return `self` upon entering the runtime context.
            """
    async def __aexit__(self, exc_type, exc_value, traceback):
            """
            Raise any exception triggered within the runtime context.
            """
    def __subclasshook__(cls, C):
        """
        __aenter__
        """
def ContextDecorator(object):
    """
    A base class or mixin that enables context managers to work as decorators.
    """
    def _recreate_cm(self):
        """
        Return a recreated instance of self.

                Allows an otherwise one-shot context manager like
                _GeneratorContextManager to support use as
                a decorator via implicit recreation.

                This is a private interface just for _GeneratorContextManager.
                See issue #11647 for details.
        
        """
    def __call__(self, func):
        """
        Shared functionality for @contextmanager and @asynccontextmanager.
        """
    def __init__(self, func, args, kwds):
        """
         Issue 19330: ensure context manager instances have good docstrings

        """
2021-03-02 20:53:56,918 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,918 : INFO : tokenize_signature : --> do i ever get here?
def _GeneratorContextManager(_GeneratorContextManagerBase,
                               AbstractContextManager,
                               ContextDecorator):
    """
    Helper for @contextmanager decorator.
    """
    def _recreate_cm(self):
        """
         _GCM instances are one-shot context managers, so the
         CM must be recreated each time a decorated function is
         called

        """
    def __enter__(self):
        """
         do not keep args and kwds alive unnecessarily
         they are only needed for recreation, which is not possible anymore

        """
    def __exit__(self, type, value, traceback):
        """
        generator didn't stop
        """
2021-03-02 20:53:56,919 : INFO : tokenize_signature : --> do i ever get here?
def _AsyncGeneratorContextManager(_GeneratorContextManagerBase,
                                    AbstractAsyncContextManager):
    """
    Helper for @asynccontextmanager.
    """
    async def __aenter__(self):
            """
            generator didn't yield
            """
    async def __aexit__(self, typ, value, traceback):
            """
            generator didn't stop
            """
def contextmanager(func):
    """
    @contextmanager decorator.

        Typical usage:

            @contextmanager
            def some_generator(<arguments>):
                <setup>
                try:
                    yield <value>
                finally:
                    <cleanup>

        This makes this:

            with some_generator(<arguments>) as <variable>:
                <body>

        equivalent to this:

            <setup>
            try:
                <variable> = <value>
                <body>
            finally:
                <cleanup>
    
    """
    def helper(*args, **kwds):
        """
        @asynccontextmanager decorator.

            Typical usage:

                @asynccontextmanager
                async def some_async_generator(<arguments>):
                    <setup>
                    try:
                        yield <value>
                    finally:
                        <cleanup>

            This makes this:

                async with some_async_generator(<arguments>) as <variable>:
                    <body>

            equivalent to this:

                <setup>
                try:
                    <variable> = <value>
                    <body>
                finally:
                    <cleanup>
    
        """
    def helper(*args, **kwds):
        """
        Context to automatically close something at the end of a block.

            Code like this:

                with closing(<module>.open(<arguments>)) as f:
                    <block>

            is equivalent to this:

                f = <module>.open(<arguments>)
                try:
                    <block>
                finally:
                    f.close()

    
        """
    def __init__(self, thing):
        """
         We use a list of old targets to make this CM re-entrant

        """
    def __enter__(self):
        """
        Context manager for temporarily redirecting stdout to another file.

                # How to send help() to stderr
                with redirect_stdout(sys.stderr):
                    help(dir)

                # How to write help() to a file
                with open('help.txt', 'w') as f:
                    with redirect_stdout(f):
                        help(pow)
    
        """
def redirect_stderr(_RedirectStream):
    """
    Context manager for temporarily redirecting stderr to another file.
    """
def suppress(AbstractContextManager):
    """
    Context manager to suppress specified exceptions

        After the exception is suppressed, execution proceeds with the next
        statement following the with statement.

             with suppress(FileNotFoundError):
                 os.remove(somefile)
             # Execution still resumes here if the file was already removed
    
    """
    def __init__(self, *exceptions):
        """
         Unlike isinstance and issubclass, CPython exception handling
         currently only looks at the concrete type hierarchy (ignoring
         the instance and subclass checking hooks). While Guido considers
         that a bug rather than a feature, it's a fairly hard one to fix
         due to various internal implementation details. suppress provides
         the simpler issubclass based semantics, rather than trying to
         exactly reproduce the limitations of the CPython interpreter.

         See http://bugs.python.org/issue12029 for more details

        """
def _BaseExitStack:
    """
    A base class for ExitStack and AsyncExitStack.
    """
    def _create_exit_wrapper(cm, cm_exit):
        """
        Preserve the context stack by transferring it to a new instance.
        """
    def push(self, exit):
        """
        Registers a callback with the standard __exit__ method signature.

                Can suppress exceptions the same way __exit__ method can.
                Also accepts any object with an __exit__ method (registering a call
                to the method instead of the object itself).
        
        """
    def enter_context(self, cm):
        """
        Enters the supplied context manager.

                If successful, also pushes its __exit__ method as a callback and
                returns the result of the __enter__ method.
        
        """
    def callback(*args, **kwds):
        """
        Registers an arbitrary callback and arguments.

                Cannot suppress exceptions.
        
        """
    def _push_cm_exit(self, cm, cm_exit):
        """
        Helper to correctly register callbacks to __exit__ methods.
        """
    def _push_exit_callback(self, callback, is_sync=True):
        """
         Inspired by discussions on http://bugs.python.org/issue13585

        """
def ExitStack(_BaseExitStack, AbstractContextManager):
    """
    Context manager for dynamic management of a stack of exit callbacks.

        For example:
            with ExitStack() as stack:
                files = [stack.enter_context(open(fname)) for fname in filenames]
                # All opened files will automatically be closed at the end of
                # the with statement, even if attempts to open files later
                # in the list raise an exception.
    
    """
    def __enter__(self):
        """
         We manipulate the exception state so it behaves as though
         we were actually nesting multiple with statements

        """
        def _fix_exception_context(new_exc, old_exc):
            """
             Context may not be correct, so find the end of the chain

            """
    def close(self):
        """
        Immediately unwind the context stack.
        """
def AsyncExitStack(_BaseExitStack, AbstractAsyncContextManager):
    """
    Async context manager for dynamic management of a stack of exit
        callbacks.

        For example:
            async with AsyncExitStack() as stack:
                connections = [await stack.enter_async_context(get_connection())
                    for i in range(5)]
                # All opened connections will automatically be released at the
                # end of the async with statement, even if attempts to open a
                # connection later in the list raise an exception.
    
    """
    def _create_async_exit_wrapper(cm, cm_exit):
        """
        Enters the supplied async context manager.

                If successful, also pushes its __aexit__ method as a callback and
                returns the result of the __aenter__ method.
        
        """
    def push_async_exit(self, exit):
        """
        Registers a coroutine function with the standard __aexit__ method
                signature.

                Can suppress exceptions the same way __aexit__ method can.
                Also accepts any object with an __aexit__ method (registering a call
                to the method instead of the object itself).
        
        """
    def push_async_callback(*args, **kwds):
        """
        Registers an arbitrary coroutine function and arguments.

                Cannot suppress exceptions.
        
        """
    async def aclose(self):
            """
            Immediately unwind the context stack.
            """
    def _push_async_cm_exit(self, cm, cm_exit):
        """
        Helper to correctly register coroutine function to __aexit__
                method.
        """
    async def __aenter__(self):
            """
             We manipulate the exception state so it behaves as though
             we were actually nesting multiple with statements

            """
        def _fix_exception_context(new_exc, old_exc):
            """
             Context may not be correct, so find the end of the chain

            """
def nullcontext(AbstractContextManager):
    """
    Context manager that does no additional processing.

        Used as a stand-in for a normal context manager, when a particular
        block of code is only sometimes used with a normal context manager:

        cm = optional_cm if condition else nullcontext()
        with cm:
            # Perform operation, using optional_cm if condition is True
    
    """
    def __init__(self, enter_result=None):
