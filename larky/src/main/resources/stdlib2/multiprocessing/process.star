def current_process():
    """
    '''
        Return process object representing the current process
        '''
    """
def active_children():
    """
    '''
        Return list of process objects corresponding to live child processes
        '''
    """
def parent_process():
    """
    '''
        Return process object representing the parent process
        '''
    """
def _cleanup():
    """
     check for processes which have finished

    """
def BaseProcess(object):
    """
    '''
        Process objects represent activity that is run in a separate process

        The class is analogous to `threading.Thread`
        '''
    """
    def _Popen(self):
        """
        'group argument must be None for now'
        """
    def _check_closed(self):
        """
        process object is closed
        """
    def run(self):
        """
        '''
                Method to be run in sub-process; can be overridden in sub-class
                '''
        """
    def start(self):
        """
        '''
                Start child process
                '''
        """
    def terminate(self):
        """
        '''
                Terminate process; sends SIGTERM signal or uses TerminateProcess()
                '''
        """
    def kill(self):
        """
        '''
                Terminate process; sends SIGKILL signal or uses TerminateProcess()
                '''
        """
    def join(self, timeout=None):
        """
        '''
                Wait until child process terminates
                '''
        """
    def is_alive(self):
        """
        '''
                Return whether process is alive
                '''
        """
    def close(self):
        """
        '''
                Close the Process object.

                This method releases resources held by the Process object.  It is
                an error to call this method if the child process is still running.
                '''
        """
    def name(self):
        """
        'name must be a string'
        """
    def daemon(self):
        """
        '''
                Return whether process is a daemon
                '''
        """
    def daemon(self, daemonic):
        """
        '''
                Set whether process is a daemon
                '''
        """
    def authkey(self):
        """
        'authkey'
        """
    def authkey(self, authkey):
        """
        '''
                Set authorization key of process
                '''
        """
    def exitcode(self):
        """
        '''
                Return exit code of process or `None` if it has yet to stop
                '''
        """
    def ident(self):
        """
        '''
                Return identifier (PID) of process or `None` if it has yet to start
                '''
        """
    def sentinel(self):
        """
        '''
                Return a file descriptor (Unix) or handle (Windows) suitable for
                waiting for process termination.
                '''
        """
    def __repr__(self):
        """
        'started'
        """
    def _bootstrap(self, parent_sentinel=None):
        """
         delay finalization of the old process object until after
         _run_after_forkers() is executed

        """
def AuthenticationString(bytes):
    """
    'Pickling an AuthenticationString object is '
    'disallowed for security reasons'

    """
def _ParentProcess(BaseProcess):
    """
    '''
            Wait until parent process terminates
            '''
    """
def _MainProcess(BaseProcess):
    """
    'MainProcess'
    """
    def close(self):
        """

         Give names to some return codes



        """
