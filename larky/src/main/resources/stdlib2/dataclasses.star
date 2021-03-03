def FrozenInstanceError(AttributeError): pass
    """
     A sentinel object for default values to signal that a default
     factory will be used.  This is given a nice repr() which will appear
     in the function signature of dataclasses' constructors.

    """
    def __repr__(self):
        """
        '<factory>'
        """
def _MISSING_TYPE:
    """
     Since most per-field metadata will be unused, create an empty
     read-only proxy that can be shared among all fields.

    """
def _FIELD_BASE:
    """
    '_FIELD'
    """
def _InitVarMeta(type):
    """
    'type'
    """
    def __init__(self, type):
        """
         typing objects, e.g. List[int]

        """
def Field:
    """
    'name'
    """
2021-03-02 20:53:57,924 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, default, default_factory, init, repr, hash, compare,
                 metadata):
        """
        'Field('
        f'name={self.name!r},'
        f'type={self.type!r},'
        f'default={self.default!r},'
        f'default_factory={self.default_factory!r},'
        f'init={self.init!r},'
        f'repr={self.repr!r},'
        f'hash={self.hash!r},'
        f'compare={self.compare!r},'
        f'metadata={self.metadata!r},'
        f'_field_type={self._field_type}'
        ')'
        """
    def __set_name__(self, owner, name):
        """
        '__set_name__'
        """
def _DatadefParams:
    """
    'init'
    """
    def __init__(self, init, repr, eq, order, unsafe_hash, frozen):
        """
        '_DataclassParams('
        f'init={self.init!r},'
        f'repr={self.repr!r},'
        f'eq={self.eq!r},'
        f'order={self.order!r},'
        f'unsafe_hash={self.unsafe_hash!r},'
        f'frozen={self.frozen!r}'
        ')'
        """
2021-03-02 20:53:57,925 : INFO : tokenize_signature : --> do i ever get here?
def field(*, default=MISSING, default_factory=MISSING, init=True, repr=True,
          hash=None, compare=True, metadata=None):
    """
    Return an object to identify dataclass fields.

        default is the default value of the field.  default_factory is a
        0-argument function called to initialize a field's value.  If init
        is True, the field will be a parameter to the class's __init__()
        function.  If repr is True, the field will be included in the
        object's repr().  If hash is True, the field will be included in
        the object's hash().  If compare is True, the field will be used
        in comparison functions.  metadata, if specified, must be a
        mapping which is stored but not otherwise examined by dataclass.

        It is an error to specify both default and default_factory.
    
    """
def _tuple_str(obj_name, fields):
    """
     Return a string representing each field of obj_name as a tuple
     member.  So, if fields is ['x', 'y'] and obj_name is "self",
     return "(self.x,self.y)".

     Special case for the 0-tuple.

    """
def _recursive_repr(user_function):
    """
     Decorator to make a repr function return "..." for a recursive
     call.

    """
    def wrapper(self):
        """
        '...'
        """
2021-03-02 20:53:57,927 : INFO : tokenize_signature : --> do i ever get here?
def _create_fn(name, args, body, *, globals=None, locals=None,
               return_type=MISSING):
    """
     Note that we mutate locals when exec() is called.  Caller
     beware!  The only callers are internal to this module, so no
     worries about external callers.

    """
def _field_assign(frozen, name, value, self_name):
    """
     If we're a frozen class, then assign to our fields in __init__
     via object.__setattr__.  Otherwise, just use a simple
     assignment.

     self_name is what "self" is called in this function: don't
     hard-code "self", since that might be a field name.

    """
def _field_init(f, frozen, globals, self_name):
    """
     Return the text of the line in the body of __init__ that will
     initialize this field.


    """
def _init_param(f):
    """
     Return the __init__ parameter string for this field.  For
     example, the equivalent of 'x:int=3' (except instead of 'int',
     reference a variable set to int, and instead of '3', reference a
     variable set to 3).

    """
def _init_fn(fields, frozen, has_post_init, self_name, globals):
    """
     fields contains both real fields and InitVar pseudo-fields.

     Make sure we don't have fields without defaults following fields
     with defaults.  This actually would be caught when exec-ing the
     function source code, but catching it here gives a better error
     message, and future-proofs us in case we build up the function
     using ast.

    """
def _repr_fn(fields, globals):
    """
    '__repr__'
    """
def _frozen_get_del_attr(cls, fields, globals):
    """
    'cls'
    """
def _cmp_fn(name, op, self_tuple, other_tuple, globals):
    """
     Create a comparison function.  If the fields in the object are
     named 'x' and 'y', then self_tuple is the string
     '(self.x,self.y)' and other_tuple is the string
     '(other.x,other.y)'.


    """
def _hash_fn(fields, globals):
    """
    'self'
    """
def _is_classvar(a_type, typing):
    """
     This test uses a typing internal class, but it's the best way to
     test if this is a ClassVar.

    """
def _is_initvar(a_type, dataclasses):
    """
     The module we're checking against is the module we're
     currently in (dataclasses.py).

    """
def _is_type(annotation, cls, a_module, a_type, is_type_predicate):
    """
     Given a type annotation string, does it refer to a_type in
     a_module?  For example, when checking that annotation denotes a
     ClassVar, then a_module is typing, and a_type is
     typing.ClassVar.

     It's possible to look up a_module given a_type, but it involves
     looking in sys.modules (again!), and seems like a waste since
     the caller already knows a_module.

     - annotation is a string type annotation
     - cls is the class that this annotation was found in
     - a_module is the module we want to match
     - a_type is the type in that module we want to match
     - is_type_predicate is a function called with (obj, a_module)
       that determines if obj is of the desired type.

     Since this test does not do a local namespace lookup (and
     instead only a module (global) lookup), there are some things it
     gets wrong.

     With string annotations, cv0 will be detected as a ClassVar:
       CV = ClassVar
       @dataclass
       class C0:
         cv0: CV

     But in this example cv1 will not be detected as a ClassVar:
       @dataclass
       class C1:
         CV = ClassVar
         cv1: CV

     In C1, the code in this function (_is_type) will look up "CV" in
     the module and not find it, so it will not consider cv1 as a
     ClassVar.  This is a fairly obscure corner case, and the best
     way to fix it would be to eval() the string "CV" with the
     correct global and local namespaces.  However that would involve
     a eval() penalty for every single field of every dataclass
     that's defined.  It was judged not worth it.


    """
def _get_field(cls, a_name, a_type):
    """
     Return a Field object for this field name and type.  ClassVars
     and InitVars are also returned, but marked as such (see
     f._field_type).

     If the default value isn't derived from Field, then it's only a
     normal default value.  Convert it to a Field().

    """
def _set_new_attribute(cls, name, value):
    """
     Never overwrites an existing attribute.  Returns True if the
     attribute already exists.

    """
def _hash_set_none(cls, fields, globals):
    """
     Raise an exception.

    """
def _process_class(cls, init, repr, eq, order, unsafe_hash, frozen):
    """
     Now that dicts retain insertion order, there's no reason to use
     an ordered dict.  I am leveraging that ordering here, because
     derived class fields overwrite base class fields, but the order
     is defined by the base class, which is found first.

    """
2021-03-02 20:53:57,939 : INFO : tokenize_signature : --> do i ever get here?
def dataclass(cls=None, /, *, init=True, repr=True, eq=True, order=False,
              unsafe_hash=False, frozen=False):
    """
    Returns the same class as was passed in, with dunder methods
        added based on the fields defined in the class.

        Examines PEP 526 __annotations__ to determine fields.

        If init is true, an __init__() method is added to the class. If
        repr is true, a __repr__() method is added. If order is true, rich
        comparison dunder methods are added. If unsafe_hash is true, a
        __hash__() method function is added. If frozen is true, fields may
        not be assigned to after instance creation.
    
    """
    def wrap(cls):
        """
         See if we're being called as @dataclass or @dataclass().

        """
def fields(class_or_instance):
    """
    Return a tuple describing the fields of this dataclass.

        Accepts a dataclass or an instance of one. Tuple elements are of
        type Field.
    
    """
def _is_dataclass_instance(obj):
    """
    Returns True if obj is an instance of a dataclass.
    """
def is_dataclass(obj):
    """
    Returns True if obj is a dataclass or an instance of a
        dataclass.
    """
def asdict(obj, *, dict_factory=dict):
    """
    Return the fields of a dataclass instance as a new dictionary mapping
        field names to field values.

        Example usage:

          @dataclass
          class C:
              x: int
              y: int

          c = C(1, 2)
          assert asdict(c) == {'x': 1, 'y': 2}

        If given, 'dict_factory' will be used instead of built-in dict.
        The function applies recursively to field values that are
        dataclass instances. This will also look into built-in containers:
        tuples, lists, and dicts.
    
    """
def _asdict_inner(obj, dict_factory):
    """
    '_fields'
    """
def astuple(obj, *, tuple_factory=tuple):
    """
    Return the fields of a dataclass instance as a new tuple of field values.

        Example usage::

          @dataclass
          class C:
              x: int
              y: int

        c = C(1, 2)
        assert astuple(c) == (1, 2)

        If given, 'tuple_factory' will be used instead of built-in tuple.
        The function applies recursively to field values that are
        dataclass instances. This will also look into built-in containers:
        tuples, lists, and dicts.
    
    """
def _astuple_inner(obj, tuple_factory):
    """
    '_fields'
    """
2021-03-02 20:53:57,942 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:57,942 : INFO : tokenize_signature : --> do i ever get here?
def make_dataclass(cls_name, fields, *, bases=(), namespace=None, init=True,
                   repr=True, eq=True, order=False, unsafe_hash=False,
                   frozen=False):
    """
    Return a new dynamically created dataclass.

        The dataclass name will be 'cls_name'.  'fields' is an iterable
        of either (name), (name, type) or (name, type, Field) objects. If type is
        omitted, use the string 'typing.Any'.  Field objects are created by
        the equivalent of calling 'field(name, type [, Field-info])'.

          C = make_dataclass('C', ['x', ('y', int), ('z', int, field(init=False))], bases=(Base,))

        is equivalent to:

          @dataclass
          class C(Base):
              x: 'typing.Any'
              y: int
              z: int = field(init=False)

        For the bases and namespace parameters, see the builtin type() function.

        The parameters init, repr, eq, order, unsafe_hash, and frozen are passed to
        dataclass().
    
    """
def replace(*args, **changes):
    """
    Return a new object replacing specified fields with new values.

        This is especially useful for frozen classes.  Example usage:

          @dataclass(frozen=True)
          class C:
              x: int
              y: int

          c = C(1, 2)
          c1 = replace(c, x=3)
          assert c1.x == 3 and c1.y == 2
      
    """
