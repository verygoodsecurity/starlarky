def openpty():
    """
    openpty() -> (master_fd, slave_fd)
        Open a pty master/slave pair, using os.openpty() if possible.
    """
def master_open():
    """
    master_open() -> (master_fd, slave_name)
        Open a pty master and return the fd, and the filename of the slave end.
        Deprecated, use openpty() instead.
    """
def _open_terminal():
    """
    Open pty master and return (master_fd, tty_name).
    """
def slave_open(tty_name):
    """
    slave_open(tty_name) -> slave_fd
        Open the pty slave and acquire the controlling terminal, returning
        opened filedescriptor.
        Deprecated, use openpty() instead.
    """
def fork():
    """
    fork() -> (pid, master_fd)
        Fork and make the child a session leader with a controlling terminal.
    """
def _writen(fd, data):
    """
    Write all the data to a descriptor.
    """
def _read(fd):
    """
    Default read function.
    """
def _copy(master_fd, master_read=_read, stdin_read=_read):
    """
    Parent copy loop.
        Copies
                pty master -> standard output   (master_read)
                standard input -> pty master    (stdin_read)
    """
def spawn(argv, master_read=_read, stdin_read=_read):
    """
    Create a spawned process.
    """
