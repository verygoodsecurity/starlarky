def pickle(ob_type, pickle_function, constructor_ob=None):
    """
    reduction functions must be callable
    """
def constructor(object):
    """
    constructors must be callable
    """
    def pickle_complex(c):
        """
         Support for pickling new-style objects


        """
def _reconstructor(cls, base, state):
    """
     Python code for object.__reduce_ex__ for protocols 0 and 1


    """
def __newobj__(cls, *args):
    """
    Used by pickle protocol 4, instead of __newobj__ to allow classes with
        keyword-only arguments to be pickled correctly.
    
    """
def _slotnames(cls):
    """
    Return a list of slot names for a given class.

        This needs to find slots defined by the class and its bases, so we
        can't simply return the __slots__ attribute.  We must walk down
        the Method Resolution Order and concatenate the __slots__ of each
        class found there.  (This assumes classes don't modify their
        __slots__ attribute to misrepresent their slots after the class is
        defined.)
    
    """
def add_extension(module, name, code):
    """
    Register an extension code.
    """
def remove_extension(module, name, code):
    """
    Unregister an extension code.  For testing only.
    """
def clear_extension_cache():
    """
     Standard extension code assignments

     Reserved ranges

     First  Last Count  Purpose
         1   127   127  Reserved for Python standard library
       128   191    64  Reserved for Zope
       192   239    48  Reserved for 3rd parties
       240   255    16  Reserved for private use (will never be assigned)
       256   Inf   Inf  Reserved for future assignment

     Extension codes are assigned by the Python Software Foundation.

    """
