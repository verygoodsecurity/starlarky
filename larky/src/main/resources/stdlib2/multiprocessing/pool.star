def mapstar(args):
    """

     Hack to embed stringification of remote traceback in local traceback



    """
def RemoteTraceback(Exception):
    """
    ''
    """
    def __reduce__(self):
        """

         Code run by worker processes



        """
def MaybeEncodingError(Exception):
    """
    Wraps possible unpickleable errors, so they can be
        safely sent through the socket.
    """
    def __init__(self, exc, value):
        """
        Error sending result: '%s'. Reason: '%s'
        """
    def __repr__(self):
        """
        <%s: %s>
        """
2021-03-02 20:46:54,601 : INFO : tokenize_signature : --> do i ever get here?
def worker(inqueue, outqueue, initializer=None, initargs=(), maxtasks=None,
           wrap_exception=False):
    """
    Maxtasks {!r} is not valid
    """
def _helper_reraises_exception(ex):
    """
    'Pickle-able helper function for use by _guarded_task_generation.'
    """
def _PoolCache(dict):
    """

        Class that implements a cache for the Pool class that will notify
        the pool management threads every time the cache is emptied. The
        notification is done by the use of a queue that is provided when
        instantiating the cache.
    
    """
    def __init__(self, /, *args, notifier=None, **kwds):
        """
         Notify that the cache is empty. This is important because the
         pool keeps maintaining workers until the cache gets drained. This
         eliminates a race condition in which a task is finished after the
         the pool's _handle_workers method has enter another iteration of the
         loop. In this situation, the only event that can wake up the pool
         is the cache to be emptied (no more tasks available).

        """
def Pool(object):
    """
    '''
        Class which supports an async version of applying functions to arguments.
        '''
    """
    def Process(ctx, *args, **kwds):
        """
         Attributes initialized early to make sure that they exist in
         __del__() if __init__() raises an exception

        """
    def __del__(self, _warn=warnings.warn, RUN=RUN):
        """
        f"unclosed running multiprocessing pool {self!r}
        """
    def __repr__(self):
        """
        f'<{cls.__module__}.{cls.__qualname__} '
        f'state={self._state} '
        f'pool_size={len(self._pool)}>'
        """
    def _get_sentinels(self):
        """
        sentinel
        """
    def _join_exited_workers(pool):
        """
        Cleanup after any worker processes which have exited due to reaching
                their specified lifetime.  Returns True if any workers were cleaned up.
        
        """
    def _repopulate_pool(self):
        """
        Bring the number of pool processes up to the specified number,
                for use after reaping workers which have exited.
        
        """
2021-03-02 20:46:54,608 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:54,608 : INFO : tokenize_signature : --> do i ever get here?
    def _maintain_pool(ctx, Process, processes, pool, inqueue, outqueue,
                       initializer, initargs, maxtasksperchild,
                       wrap_exception):
        """
        Clean up any exited workers and start replacements for them.
        
        """
    def _setup_queues(self):
        """
        Pool not running
        """
    def apply(self, func, args=(), kwds={}):
        """
        '''
                Equivalent of `func(*args, **kwds)`.
                Pool must be running.
                '''
        """
    def map(self, func, iterable, chunksize=None):
        """
        '''
                Apply `func` to each element in `iterable`, collecting the results
                in a list that is returned.
                '''
        """
    def starmap(self, func, iterable, chunksize=None):
        """
        '''
                Like `map()` method but the elements of the `iterable` are expected to
                be iterables as well and will be unpacked as arguments. Hence
                `func` and (a, b) becomes func(a, b).
                '''
        """
2021-03-02 20:46:54,609 : INFO : tokenize_signature : --> do i ever get here?
    def starmap_async(self, func, iterable, chunksize=None, callback=None,
            error_callback=None):
        """
        '''
                Asynchronous version of `starmap()` method.
                '''
        """
    def _guarded_task_generation(self, result_job, func, iterable):
        """
        '''Provides a generator of tasks for imap and imap_unordered with
                appropriate handling for iterables which throw exceptions during
                iteration.'''
        """
    def imap(self, func, iterable, chunksize=1):
        """
        '''
                Equivalent of `map()` -- can be MUCH slower than `Pool.map()`.
                '''
        """
    def imap_unordered(self, func, iterable, chunksize=1):
        """
        '''
                Like `imap()` method but ordering of results is arbitrary.
                '''
        """
2021-03-02 20:46:54,611 : INFO : tokenize_signature : --> do i ever get here?
    def apply_async(self, func, args=(), kwds={}, callback=None,
            error_callback=None):
        """
        '''
                Asynchronous version of `apply()` method.
                '''
        """
2021-03-02 20:46:54,611 : INFO : tokenize_signature : --> do i ever get here?
    def map_async(self, func, iterable, chunksize=None, callback=None,
            error_callback=None):
        """
        '''
                Asynchronous version of `map()` method.
                '''
        """
2021-03-02 20:46:54,612 : INFO : tokenize_signature : --> do i ever get here?
    def _map_async(self, func, iterable, mapper, chunksize=None, callback=None,
            error_callback=None):
        """
        '''
                Helper function to implement map, starmap and their async counterparts.
                '''
        """
    def _wait_for_updates(sentinels, change_notifier, timeout=None):
        """
         Keep maintaining workers until the cache gets drained, unless the pool
         is terminated.

        """
    def _handle_tasks(taskqueue, put, outqueue, pool, cache):
        """
         iterating taskseq cannot fail

        """
    def _handle_results(outqueue, get, cache):
        """
        'result handler got EOFError/OSError -- exiting'
        """
    def _get_tasks(func, it, size):
        """
        'pool objects cannot be passed between processes or pickled'

        """
    def close(self):
        """
        'closing pool'
        """
    def terminate(self):
        """
        'terminating pool'
        """
    def join(self):
        """
        'joining pool'
        """
    def _help_stuff_finish(inqueue, task_handler, size):
        """
         task_handler may be blocked trying to put items on inqueue

        """
2021-03-02 20:46:54,617 : INFO : tokenize_signature : --> do i ever get here?
    def _terminate_pool(cls, taskqueue, inqueue, outqueue, pool, change_notifier,
                        worker_handler, task_handler, result_handler, cache):
        """
         this is guaranteed to only be called once

        """
    def __enter__(self):
        """

         Class whose instances are returned by `Pool.apply_async()`



        """
def ApplyResult(object):
    """
    {0!r} not ready
    """
    def wait(self, timeout=None):
        """
         create alias -- see #17805
        """
def MapResult(ApplyResult):
    """
     only store first exception

    """
def IMapIterator(object):
    """
     XXX
    """
    def _set(self, i, obj):
        """

         Class whose instances are returned by `Pool.imap_unordered()`



        """
def IMapUnorderedIterator(IMapIterator):
    """





    """
def ThreadPool(Pool):
    """
     drain inqueue, and put sentinels at its head to make workers finish

    """
    def _wait_for_updates(self, sentinels, change_notifier, timeout):
