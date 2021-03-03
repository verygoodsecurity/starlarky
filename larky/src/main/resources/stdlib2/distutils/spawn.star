def spawn(cmd, search_path=1, verbose=0, dry_run=0):
    """
    Run another program, specified as a command list 'cmd', in a new process.

        'cmd' is just the argument list for the new process, ie.
        cmd[0] is the program to run and cmd[1:] are the rest of its arguments.
        There is no way to run a program with a name different from that of its
        executable.

        If 'search_path' is true (the default), the system's executable
        search path will be used to find the program; otherwise, cmd[0]
        must be the exact path to the executable.  If 'dry_run' is true,
        the command will not actually be run.

        Raise DistutilsExecError if running the program fails in any way; just
        return on success.
    
    """
def _nt_quote_args(args):
    """
    Quote command-line arguments for DOS/Windows conventions.

        Just wraps every argument which contains blanks in double quotes, and
        returns a new argument list.
    
    """
def _spawn_nt(cmd, search_path=1, verbose=0, dry_run=0):
    """
     either we find one or it stays the same

    """
def _spawn_posix(cmd, search_path=1, verbose=0, dry_run=0):
    """
    ' '
    """
def find_executable(executable, path=None):
    """
    Tries to find 'executable' in the directories listed in 'path'.

        A string listing directories separated by 'os.pathsep'; defaults to
        os.environ['PATH'].  Returns the complete filename or None if not found.
    
    """
