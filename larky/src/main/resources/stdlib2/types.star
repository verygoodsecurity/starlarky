def _f(): pass
    """
     Same as FunctionType
    """
def _cell_factory():
    """
     Prevent ResourceWarning
    """
async def _ag():
        """
         Same as BuiltinFunctionType
        """
def new_class(name, bases=(), kwds=None, exec_body=None):
    """
    Create a class object dynamically using the appropriate metaclass.
    """
def resolve_bases(bases):
    """
    Resolve MRO entries dynamically as specified by PEP 560.
    """
def prepare_class(name, bases=(), kwds=None):
    """
    Call the __prepare__ method of the appropriate metaclass.

        Returns (metaclass, namespace, kwds) as a 3-tuple

        *metaclass* is the appropriate metaclass
        *namespace* is the prepared class namespace
        *kwds* is an updated copy of the passed in kwds argument with any
        'metaclass' entry removed. If no kwds argument is passed in, this will
        be an empty dict.
    
    """
def _calculate_meta(meta, bases):
    """
    Calculate the most derived metaclass.
    """
def DynamicClassAttribute:
    """
    Route attribute access on a class to __getattr__.

        This is a descriptor, used to define attributes that act differently when
        accessed through an instance and through a class.  Instance access remains
        normal, but access to an attribute through a class will be routed to the
        class's __getattr__ method; this is done by raising AttributeError.

        This allows one to have properties active on an instance, and have virtual
        attributes on the class with the same name (see Enum for an example).

    
    """
    def __init__(self, fget=None, fset=None, fdel=None, doc=None):
        """
         next two lines make DynamicClassAttribute act the same as property

        """
    def __get__(self, instance, ownerclass=None):
        """
        unreadable attribute
        """
    def __set__(self, instance, value):
        """
        can't set attribute
        """
    def __delete__(self, instance):
        """
        can't delete attribute
        """
    def getter(self, fget):
        """
         TODO: Implement this in C.

        """
    def __init__(self, gen):
        """
        '__name__'
        """
    def send(self, val):
        """
        Convert regular generator function to a coroutine.
        """
    def wrapped(*args, **kwargs):
        """
         'coro' is a native coroutine object or an iterable coroutine

        """
