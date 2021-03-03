def nametofont(name):
    """
    Given the name of a tk named font, returns a Font representation.
    
    """
def Font:
    """
    Represents a named font.

        Constructor options are:

        font -- font specifier (name, system font, or (family, size, style)-tuple)
        name -- name to use for this font configuration (defaults to a unique name)
        exists -- does a named font by this name already exist?
           Creates a new named font if False, points to the existing font if True.
           Raises _tkinter.TclError if the assertion is false.

           the following are ignored if font is specified:

        family -- font 'family', e.g. Courier, Times, Helvetica
        size -- font size in points
        weight -- font thickness: NORMAL, BOLD
        slant -- font slant: ROMAN, ITALIC
        underline -- font underlining: false (0), true (1)
        overstrike -- font strikeout: false (0), true (1)

    
    """
    def _set(self, kw):
        """
        -
        """
    def _get(self, args):
        """
        -
        """
    def _mkdict(self, args):
        """
        'tk'
        """
    def __str__(self):
        """
        font
        """
    def copy(self):
        """
        Return a distinct copy of the current font
        """
    def actual(self, option=None, displayof=None):
        """
        Return actual font attributes
        """
    def cget(self, option):
        """
        Get font attribute
        """
    def config(self, **options):
        """
        Modify font attributes
        """
    def measure(self, text, displayof=None):
        """
        Return text width
        """
    def metrics(self, *options, **kw):
        """
        Return font metrics.

                For best performance, create a dummy widget
                using this font before calling this method.
        """
def families(root=None, displayof=None):
    """
    Get font families (as a tuple)
    """
def names(root=None):
    """
    Get names of defined fonts (as a tuple)
    """
