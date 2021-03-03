def initscr():
    """
     we call setupterm() here because it raises an error
     instead of calling exit() in error cases.

    """
def start_color():
    """
    'COLORS'
    """
def wrapper(*args, **kwds):
    """
    Wrapper function that initializes curses and calls another function,
        restoring normal keyboard/screen behavior on error.
        The callable object 'func' is then passed the main window 'stdscr'
        as its first argument, followed by any other arguments passed to
        wrapper().
    
    """
