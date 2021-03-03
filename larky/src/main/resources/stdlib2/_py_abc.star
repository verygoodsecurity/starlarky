def get_cache_token():
    """
    Returns the current ABC cache token.

        The token is an opaque object (supporting equality testing) identifying the
        current version of the ABC cache for virtual subclasses. The token changes
        with every call to ``register()`` on any ABC.
    
    """
def ABCMeta(type):
    """
    Metaclass for defining Abstract Base Classes (ABCs).

        Use this metaclass to create an ABC.  An ABC can be subclassed
        directly, and then acts as a mix-in class.  You can also register
        unrelated concrete classes (even built-in classes) and unrelated
        ABCs as 'virtual subclasses' -- these and their descendants will
        be considered subclasses of the registering ABC by the built-in
        issubclass() function, but the registering ABC won't show up in
        their MRO (Method Resolution Order) nor will method
        implementations defined by the registering ABC be callable (not
        even via super()).
    
    """
    def __new__(mcls, name, bases, namespace, /, **kwargs):
        """
         Compute set of abstract method names

        """
    def register(cls, subclass):
        """
        Register a virtual subclass of an ABC.

                Returns the subclass, to allow usage as a class decorator.
        
        """
    def _dump_registry(cls, file=None):
        """
        Debug helper to print the ABC registry.
        """
    def _abc_registry_clear(cls):
        """
        Clear the registry (for debugging or testing).
        """
    def _abc_caches_clear(cls):
        """
        Clear the caches (for debugging or testing).
        """
    def __instancecheck__(cls, instance):
        """
        Override for isinstance(instance, cls).
        """
    def __subclasscheck__(cls, subclass):
        """
        Override for issubclass(subclass, cls).
        """
