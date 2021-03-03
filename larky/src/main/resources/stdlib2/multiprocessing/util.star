def sub_debug(msg, *args):
    """
    '''
        Returns logger used by multiprocessing
        '''
    """
def log_to_stderr(level=None):
    """
    '''
        Turn on logging and add a handler which prints to stderr
        '''
    """
def _platform_supports_abstract_sockets():
    """
    linux
    """
def is_abstract_socket_namespace(address):
    """
    \0
    """
def _remove_temp_dir(rmtree, tempdir):
    """
     current_process() can be None if the finalizer is called
     late during Python finalization

    """
def get_temp_dir():
    """
     get name of a temp directory which will be automatically cleaned up

    """
def _run_after_forkers():
    """
    'after forker raised exception %s'
    """
def register_after_fork(obj, func):
    """

     Finalization using weakrefs



    """
def Finalize(object):
    """
    '''
        Class which supports object finalization using weakrefs
        '''
    """
    def __init__(self, obj, callback, args=(), kwargs=None, exitpriority=None):
        """
        Exitpriority ({0!r}) must be None or int, not {1!s}
        """
2021-03-02 20:46:52,641 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:52,641 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:52,641 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:52,641 : INFO : tokenize_signature : --> do i ever get here?
    def __call__(self, wr=None,
                 # Need to bind these locally because the globals can have
                 # been cleared at shutdown
                 _finalizer_registry=_finalizer_registry,
                 sub_debug=sub_debug, getpid=os.getpid):
        """
        '''
                Run the callback unless it has already been called or cancelled
                '''
        """
    def cancel(self):
        """
        '''
                Cancel finalization of the object
                '''
        """
    def still_active(self):
        """
        '''
                Return whether this finalizer is still waiting to invoke callback
                '''
        """
    def __repr__(self):
        """
        '<%s object, dead>'
        """
def _run_finalizers(minpriority=None):
    """
    '''
        Run all finalizers whose exit priority is not None and at least minpriority

        Finalizers with highest priority are called first; finalizers with
        the same priority will be called in reverse order of creation.
        '''
    """
def is_exiting():
    """
    '''
        Returns true if the process is shutting down
        '''
    """
2021-03-02 20:46:52,643 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:52,643 : INFO : tokenize_signature : --> do i ever get here?
def _exit_function(info=info, debug=debug, _run_finalizers=_run_finalizers,
                   active_children=process.active_children,
                   current_process=process.current_process):
    """
     We hold on to references to functions in the arglist due to the
     situation described below, where this function is called after this
     module's globals are destroyed.


    """
def ForkAwareThreadLock(object):
    """

     Close fds except those specified



    """
def close_all_fds_except(fds):
    """
    'fd too large'
    """
def _close_stdin():
    """

     Flush standard streams, if any



    """
def _flush_std_streams():
    """

     Start a program with only specified fds kept open



    """
def spawnv_passfds(path, args, passfds):
    """
    Close each file descriptor given as an argument
    """
def _cleanup_tests():
    """
    Cleanup multiprocessing resources when multiprocessing tests
        completed.
    """
