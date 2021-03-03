def _path_eq(p1, p2):
    """

     We define a Popen class similar to the one from subprocess, but
     whose constructor takes a process object as its argument.



    """
def Popen(object):
    """
    '''
        Start a subprocess to run the code of a process object
        '''
    """
    def __init__(self, process_obj):
        """
         read end of pipe will be duplicated by the child process
         -- see spawn_main() in spawn.py.

         bpo-33929: Previously, the read end of pipe was "stolen" by the child
         process, but it leaked a handle if the child process had been
         terminated before it could steal the handle from the parent process.

        """
    def duplicate_for_child(self, handle):
