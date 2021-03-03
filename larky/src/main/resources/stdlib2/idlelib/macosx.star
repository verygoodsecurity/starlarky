def _init_tk_type():
    """

        Initializes OS X Tk variant values for
        isAquaTk(), isCarbonTk(), isCocoaTk(), and isXQuartz().
    
    """
def isAquaTk():
    """

        Returns True if IDLE is using a native OS X Tk (Cocoa or Carbon).
    
    """
def isCarbonTk():
    """

        Returns True if IDLE is using a Carbon Aqua Tk (instead of the
        newer Cocoa Aqua Tk).
    
    """
def isCocoaTk():
    """

        Returns True if IDLE is using a Cocoa Aqua Tk.
    
    """
def isXQuartz():
    """

        Returns True if IDLE is using an OS X X11 Tk.
    
    """
def tkVersionWarning(root):
    """

        Returns a string warning message if the Tk version in use appears to
        be one known to cause problems with IDLE.
        1. Apple Cocoa-based Tk 8.5.7 shipped with Mac OS X 10.6 is unusable.
        2. Apple Cocoa-based Tk 8.5.9 in OS X 10.7 and 10.8 is better but
            can still crash unexpectedly.
    
    """
def readSystemPreferences():
    """

        Fetch the macOS system preferences.
    
    """
def preferTabsPreferenceWarning():
    """

        Warn if "Prefer tabs when opening documents" is set to "Always".
    
    """
def addOpenEventSupport(root, flist):
    """

        This ensures that the application will respond to open AppleEvents, which
        makes is feasible to use IDLE as the default application for python files.
    
    """
    def doOpenFile(*args):
        """
         The command below is a hook in aquatk that is called whenever the app
         receives a file open event. The callback can have multiple arguments,
         one for every file that should be opened.

        """
def hideTkConsole(root):
    """
    'console'
    """
def overrideRootMenu(root, flist):
    """

        Replace the Tk root menu by something that is more appropriate for
        IDLE with an Aqua Tk.
    
    """
    def postwindowsmenu(menu=menu):
        """
        'end'
        """
    def about_dialog(event=None):
        """
        Handle Help 'About IDLE' event.
        """
    def config_dialog(event=None):
        """
        Handle Options 'Configure IDLE' event.
        """
    def help_dialog(event=None):
        """
        Handle Help 'IDLE Help' event.
        """
def fixb2context(root):
    """
    '''Removed bad AquaTk Button-2 (right) and Paste bindings.

        They prevent context menu access and seem to be gone in AquaTk8.6.
        See issue #24801.
        '''
    """
def setupApp(root, flist):
    """

        Perform initial OS X customizations if needed.
        Called from pyshell.main() after initial calls to Tk()

        There are currently three major versions of Tk in use on OS X:
            1. Aqua Cocoa Tk (native default since OS X 10.6)
            2. Aqua Carbon Tk (original native, 32-bit only, deprecated)
            3. X11 (supported by some third-party distributors, deprecated)
        There are various differences among the three that affect IDLE
        behavior, primarily with menus, mouse key events, and accelerators.
        Some one-time customizations are performed here.
        Others are dynamically tested throughout idlelib by calls to the
        isAquaTk(), isCarbonTk(), isCocoaTk(), isXQuartz() functions which
        are initialized here as well.
    
    """
