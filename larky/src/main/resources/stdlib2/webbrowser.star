def Error(Exception):
    """
     Dictionary of available browser controllers
    """
def register(name, klass, instance=None, *, preferred=False):
    """
    Register a browser connector.
    """
def get(using=None):
    """
    Return a browser launcher instance appropriate for the environment.
    """
def open(url, new=0, autoraise=True):
    """
    Display url using the default browser.

        If possible, open url in a location determined by new.
        - 0: the same browser window (the default).
        - 1: a new browser window.
        - 2: a new browser page ("tab").
        If possible, autoraise raises the window (the default) or not.
    
    """
def open_new(url):
    """
    Open url in a new window of the default browser.

        If not possible, then open url in the only browser window.
    
    """
def open_new_tab(url):
    """
    Open url in a new page ("tab") of the default browser.

        If not possible, then the behavior becomes equivalent to open_new().
    
    """
def _synthesize(browser, *, preferred=False):
    """
    Attempt to synthesize a controller based on existing controllers.

        This is useful to create a controller when a user specifies a path to
        an entry in the BROWSER environment variable -- we can copy a general
        controller to operate using a specific installation of the desired
        browser in this way.

        If we can't create a controller in this way, or if there is no
        executable for the requested browser, return [None, None].

    
    """
def BaseBrowser(object):
    """
    Parent class for all browsers. Do not use directly.
    """
    def __init__(self, name=""):
        """
        Class for all browsers started with a command
               and without remote functionality.
        """
    def __init__(self, name):
        """
        %s
        """
    def open(self, url, new=0, autoraise=True):
        """
        webbrowser.open
        """
def BackgroundBrowser(GenericBrowser):
    """
    Class for all browsers which are to be started in the
           background.
    """
    def open(self, url, new=0, autoraise=True):
        """
        %s
        """
def UnixBrowser(BaseBrowser):
    """
    Parent class for all Unix browsers with remote functionality.
    """
    def _invoke(self, args, remote, autoraise, url=None):
        """
         use autoraise argument only for remote invocation

        """
    def open(self, url, new=0, autoraise=True):
        """
        webbrowser.open
        """
def Mozilla(UnixBrowser):
    """
    Launcher class for Mozilla browsers.
    """
def Netscape(UnixBrowser):
    """
    Launcher class for Netscape browser.
    """
def Galeon(UnixBrowser):
    """
    Launcher class for Galeon/Epiphany browsers.
    """
def Chrome(UnixBrowser):
    """
    Launcher class for Google Chrome browser.
    """
def Opera(UnixBrowser):
    """
    Launcher class for Opera browser.
    """
def Elinks(UnixBrowser):
    """
    Launcher class for Elinks browsers.
    """
def Konqueror(BaseBrowser):
    """
    Controller for the KDE File Manager (kfm, or Konqueror).

        See the output of ``kfmclient --commands``
        for more information on the Konqueror remote-control interface.
    
    """
    def open(self, url, new=0, autoraise=True):
        """
        webbrowser.open
        """
def Grail(BaseBrowser):
    """
     There should be a way to maintain a connection to Grail, but the
     Grail remote control protocol doesn't really allow that at this
     point.  It probably never will!

    """
    def _find_grail_rc(self):
        """
        .grail-unix
        """
    def _remote(self, action):
        """
        webbrowser.open
        """
def register_X_browsers():
    """
     use xdg-open if around

    """
def register_standard_browsers():
    """
    'darwin'
    """
    def WindowsDefault(BaseBrowser):
    """
    webbrowser.open
    """
    def MacOSX(BaseBrowser):
    """
    Launcher class for Aqua browsers on Mac OS X

            Optionally specify a browser name on instantiation.  Note that this
            will not work for Aqua browsers if the user has moved the application
            package after installation.

            If no browser is specified, the default browser, as specified in the
            Internet System Preferences panel, will be used.
        
    """
        def __init__(self, name):
            """
            webbrowser.open
            """
    def MacOSXOSAScript(BaseBrowser):
    """
    'default'
    """
def main():
    """
    Usage: %s [-n | -t] url
        -n: open new window
        -t: open new tab
    """
