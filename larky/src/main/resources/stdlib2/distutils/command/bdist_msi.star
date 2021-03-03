def PyDialog(Dialog):
    """
    Dialog class with a fixed layout: controls at the top, then a ruler,
        then a list of buttons: back, next, cancel. Optionally a bitmap at the
        left.
    """
    def __init__(self, *args, **kw):
        """
        Dialog(database, name, x, y, w, h, attributes, title, first,
                default, cancel, bitmap=true)
        """
    def title(self, title):
        """
        Set the title text of the dialog at the top.
        """
    def back(self, title, next, name = "Back", active = 1):
        """
        Add a back button with a given title, the tab-next button,
                its name in the Control table, possibly initially disabled.

                Return the button, so that events can be associated
        """
    def cancel(self, title, next, name = "Cancel", active = 1):
        """
        Add a cancel button with a given title, the tab-next button,
                its name in the Control table, possibly initially disabled.

                Return the button, so that events can be associated
        """
    def next(self, title, next, name = "Next", active = 1):
        """
        Add a Next button with a given title, the tab-next button,
                its name in the Control table, possibly initially disabled.

                Return the button, so that events can be associated
        """
    def xbutton(self, name, title, next, xpos):
        """
        Add a button with a given title, the tab-next button,
                its name in the Control table, giving its x position; the
                y-position is aligned with the other buttons.

                Return the button, so that events can be associated
        """
def bdist_msi(Command):
    """
    create a Microsoft Installer (.msi) binary distribution
    """
    def initialize_options(self):
        """
        'bdist'
        """
    def run(self):
        """
        'build'
        """
    def add_files(self):
        """
        distfiles
        """
    def add_find_python(self):
        """
        Adds code to the installer to compute the location of Python.

                Properties PYTHON.MACHINE.X.Y and PYTHON.USER.X.Y will be set from the
                registry for each version of Python.

                Properties TARGETDIRX.Y will be set from PYTHON.USER.X.Y if defined,
                else from PYTHON.MACHINE.X.Y.

                Properties PYTHONX.Y will be set to TARGETDIRX.Y\\python.exe
        """
    def add_scripts(self):
        """
        install_script.
        """
    def add_ui(self):
        """
        [ProductName] Setup
        """
    def get_installer_filename(self, fullname):
        """
         Factored out to allow overriding in subclasses

        """
