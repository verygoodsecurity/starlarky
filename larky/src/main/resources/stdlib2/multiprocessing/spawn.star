def set_executable(exe):
    """





    """
def is_forking(argv):
    """
    '''
        Return whether commandline indicates we are forking
        '''
    """
def freeze_support():
    """
    '''
        Run code for process object if this in not the main process
        '''
    """
def get_command_line(**kwds):
    """
    '''
        Returns prefix of command line used for spawning a child process
        '''
    """
def spawn_main(pipe_handle, parent_pid=None, tracker_fd=None):
    """
    '''
        Run code specified by data received over pipe
        '''
    """
def _main(fd, parent_sentinel):
    """
    'rb'
    """
def _check_not_importing_main():
    """
    '_inheriting'
    """
def get_preparation_data(name):
    """
    '''
        Return info about parent needed by child to unpickle process object
        '''
    """
def prepare(data):
    """
    '''
        Try to get current process ready to unpickle process object
        '''
    """
def _fixup_main_from_name(mod_name):
    """
     __main__.py files for packages, directories, zip archives, etc, run
     their "main only" code unconditionally, so we don't even try to
     populate anything in __main__, nor do we make any changes to
     __main__ attributes

    """
def _fixup_main_from_path(main_path):
    """
     If this process was forked, __main__ may already be populated

    """
def import_main_path(main_path):
    """
    '''
        Set sys.modules['__main__'] to module at main_path
        '''
    """
