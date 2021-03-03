def abstractmethod(funcobj):
    """
    A decorator indicating abstract methods.

        Requires that the metaclass is ABCMeta or derived from it.  A
        class that has a metaclass derived from ABCMeta cannot be
        instantiated unless all of its abstract methods are overridden.
        The abstract methods can be called using any of the normal
        'super' call mechanisms.  abstractmethod() may be used to declare
        abstract methods for properties and descriptors.

        Usage:

            class C(metaclass=ABCMeta):
                @abstractmethod
                def my_abstract_method(self, ...):
                    ...
    
    """
def abstractdefmethod(defmethod):
    """
    A decorator indicating abstract classmethods.

        Deprecated, use 'classmethod' with 'abstractmethod' instead.
    
    """
    def __init__(self, callable):
        """
        A decorator indicating abstract staticmethods.

            Deprecated, use 'staticmethod' with 'abstractmethod' instead.
    
        """
    def __init__(self, callable):
        """
        A decorator indicating abstract properties.

            Deprecated, use 'property' with 'abstractmethod' instead.
    
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
        def __new__(mcls, name, bases, namespace, **kwargs):
            """
            Register a virtual subclass of an ABC.

                        Returns the subclass, to allow usage as a class decorator.
            
            """
        def __instancecheck__(cls, instance):
            """
            Override for isinstance(instance, cls).
            """
        def __subclasscheck__(cls, subclass):
            """
            Override for issubclass(subclass, cls).
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
def ABC(metadef=ABCMeta):
    """
    Helper class that provides a standard way to create an ABC using
        inheritance.
    
    """
