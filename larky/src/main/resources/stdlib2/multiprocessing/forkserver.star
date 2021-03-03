def ForkServer(object):
    """
    '__main__'
    """
    def _stop(self):
        """
         Method used by unit tests to stop the server

        """
    def _stop_unlocked(self):
        """
         close the "alive" file descriptor asks the server to stop

        """
    def set_forkserver_preload(self, modules_names):
        """
        '''Set list of module names to try to load in forkserver process.'''
        """
    def get_inherited_fds(self):
        """
        '''Return list of fds inherited from parent process.

                This returns None if the current process was not started by fork
                server.
                '''
        """
    def connect_to_new_process(self, fds):
        """
        '''Request forkserver to create a child process.

                Returns a pair of fds (status_r, data_w).  The calling process can read
                the child process's pid and (eventually) its returncode from status_r.
                The calling process should write to data_w the pickled preparation and
                process data.
                '''
        """
    def ensure_running(self):
        """
        '''Make sure that a fork server is running.

                This can be called from any process.  Note that usually a child
                process will just reuse the forkserver started by its parent, so
                ensure_running() will do nothing.
                '''
        """
def main(listener_fd, alive_r, preload, main_path=None, sys_path=None):
    """
    '''Run forkserver.'''
    """
    def sigchld_handler(*_unused):
        """
         Dummy signal handler, doesn't do anything

        """
def _serve_one(child_r, fds, unused_fds, handlers):
    """
     close unnecessary stuff and reset signal handlers

    """
def read_signed(fd):
    """
    b''
    """
def write_signed(fd, n):
    """
    'should not get here'
    """
