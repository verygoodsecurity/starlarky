def Queue(object):
    """
     Can raise ImportError (see issues #3770 and #23400)

    """
    def __getstate__(self):
        """
        'Queue._after_fork()'
        """
    def put(self, obj, block=True, timeout=None):
        """
        f"Queue {self!r} is closed
        """
    def get(self, block=True, timeout=None):
        """
        f"Queue {self!r} is closed
        """
    def qsize(self):
        """
         Raises NotImplementedError on Mac OSX because of broken sem_getvalue()

        """
    def empty(self):
        """
        'Queue.join_thread()'
        """
    def cancel_join_thread(self):
        """
        'Queue.cancel_join_thread()'
        """
    def _start_thread(self):
        """
        'Queue._start_thread()'
        """
    def _finalize_join(twr):
        """
        'joining queue thread'
        """
    def _finalize_close(buffer, notempty):
        """
        'telling queue thread to quit'
        """
2021-03-02 20:46:51,741 : INFO : tokenize_signature : --> do i ever get here?
    def _feed(buffer, notempty, send_bytes, writelock, close, ignore_epipe,
              onerror, queue_sem):
        """
        'starting thread to feed data to pipe'
        """
    def _on_queue_feeder_error(e, obj):
        """

                Private API hook called when feeding data in the background thread
                raises an exception.  For overriding by concurrent.futures.
        
        """
def JoinableQueue(Queue):
    """
    f"Queue {self!r} is closed
    """
    def task_done(self):
        """
        'task_done() called too many times'
        """
    def join(self):
        """

         Simplified Queue type -- really just a locked pipe



        """
def SimpleQueue(object):
    """
    'win32'
    """
    def empty(self):
        """
         unserialize the data after having released the lock

        """
    def put(self, obj):
        """
         serialize the data before acquiring the lock

        """
