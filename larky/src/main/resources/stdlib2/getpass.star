def GetPassWarning(UserWarning): pass
    """
    'Password: '
    """
def win_getpass(prompt='Password: ', stream=None):
    """
    Prompt for password with echo off, using Windows getch().
    """
def fallback_getpass(prompt='Password: ', stream=None):
    """
    Can not control echo on the terminal.
    """
def _raw_input(prompt="", stream=None, input=None):
    """
     This doesn't save the string in the GNU readline history.

    """
def getuser():
    """
    Get the username from the environment or password database.

        First try various environment variables, then the password
        database.  This works on Windows as long as USERNAME is set.

    
    """
