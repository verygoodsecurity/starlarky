def _type_check(arg, msg, is_argument=True):
    """
    Check that the argument is a type, and return it (internal helper).

        As a special case, accept None and return type(None) instead. Also wrap strings
        into ForwardRef instances. Consider several corner cases, for example plain
        special forms like Union are not valid, while Union[int, str] is OK, etc.
        The msg argument is a human-readable error message, e.g::

            "Union[arg, ...]: arg should be a type."

        We append the repr() of the actual value (truncated to 100 chars).
    
    """
def _type_repr(obj):
    """
    Return the repr() of an object, special-casing types (internal helper).

        If obj is a type, we return a shorter version than the default
        type.__repr__, based on the module and qualified name, which is
        typically enough to uniquely identify a type.  For everything
        else, we fall back on repr(obj).
    
    """
def _collect_type_vars(types):
    """
    Collect all type variable contained in types in order of
        first appearance (lexicographic order). For example::

            _collect_type_vars((T, List[S, T])) == (T, S)
    
    """
def _subs_tvars(tp, tvars, subs):
    """
    Substitute type variables 'tvars' with substitutions 'subs'.
        These two must have the same length.
    
    """
def _check_generic(cls, parameters):
    """
    Check correct count for parameters of a generic cls (internal helper).
        This gives a nice error message in case of count mismatch.
    
    """
def _remove_dups_flatten(parameters):
    """
    An internal helper for Union creation and substitution: flatten Unions
        among parameters, then remove duplicates.
    
    """
def _tp_cache(func):
    """
    Internal wrapper caching __getitem__ of generic types with a fallback to
        original function for non-hashable arguments.
    
    """
    def inner(*args, **kwds):
        """
         All real errors (not unhashable args) are raised below.
        """
def _eval_type(t, globalns, localns):
    """
    Evaluate all forward reverences in the given type t.
        For use of globalns and localns see the docstring for get_type_hints().
    
    """
def _Final:
    """
    Mixin to prohibit subclassing
    """
    def __init_subclass__(self, /, *args, **kwds):
        """
        '_root'
        """
def _Immutable:
    """
    Mixin to indicate that object should not be copied.
    """
    def __copy__(self):
        """
        Internal indicator of special typing constructs.
            See _doc instance attribute for specific docs.
    
        """
    def __new__(cls, *args, **kwds):
        """
        Constructor.

                This only exists to give a better error message in case
                someone tries to subclass a special typing object (not a good idea).
        
        """
    def __init__(self, name, doc):
        """
        'typing.'
        """
    def __reduce__(self):
        """
        f"Cannot instantiate {self!r}
        """
    def __instancecheck__(self, obj):
        """
        f"{self} cannot be used with isinstance()
        """
    def __subclasscheck__(self, cls):
        """
        f"{self} cannot be used with issubclass()
        """
    def __getitem__(self, parameters):
        """
        'ClassVar'
        """
def ForwardRef(_Final, _root=True):
    """
    Internal wrapper to hold a forward reference.
    """
    def __init__(self, arg, is_argument=True):
        """
        f"Forward reference must be a string -- got {arg!r}
        """
    def _evaluate(self, globalns, localns):
        """
        Forward references must evaluate to types.
        """
    def __eq__(self, other):
        """
        f'ForwardRef({self.__forward_arg__!r})'
        """
def TypeVar(_Final, _Immutable, _root=True):
    """
    Type variable.

        Usage::

          T = TypeVar('T')  # Can be anything
          A = TypeVar('A', str, bytes)  # Must be str or bytes

        Type variables exist primarily for the benefit of static type
        checkers.  They serve as the parameters for generic types as well
        as for generic function definitions.  See class Generic for more
        information on generic types.  Generic functions work as follows:

          def repeat(x: T, n: int) -> List[T]:
              '''Return a list containing n references to x.'''
              return [x]*n

          def longest(x: A, y: A) -> A:
              '''Return the longest of two strings.'''
              return x if len(x) >= len(y) else y

        The latter example's signature is essentially the overloading
        of (str, str) -> str and (bytes, bytes) -> bytes.  Also note
        that if the arguments are instances of some subclass of str,
        the return type is still plain str.

        At runtime, isinstance(x, T) and issubclass(C, T) will raise TypeError.

        Type variables defined with covariant=True or contravariant=True
        can be used to declare covariant or contravariant generic types.
        See PEP 484 for more details. By default generic types are invariant
        in all type variables.

        Type variables can be introspected. e.g.:

          T.__name__ == 'T'
          T.__constraints__ == ()
          T.__covariant__ == False
          T.__contravariant__ = False
          A.__constraints__ == (str, bytes)

        Note that only type variables defined in global scope can be pickled.
    
    """
2021-03-02 20:54:03,049 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, name, *constraints, bound=None,
                 covariant=False, contravariant=False):
        """
        Bivariant types are not supported.
        """
    def __repr__(self):
        """
        '+'
        """
    def __reduce__(self):
        """
         Special typing constructs Union, Optional, Generic, Callable and Tuple
         use three special attributes for internal bookkeeping of generic types:
         * __parameters__ is a tuple of unique free type parameters of a generic
           type, for example, Dict[T, T].__parameters__ == (T,);
         * __origin__ keeps a reference to a type that was subscripted,
           e.g., Union[T, int].__origin__ == Union, or the non-generic version of
           the type.
         * __args__ is a tuple of all arguments used in subscripting,
           e.g., Dict[T, int].__args__ == (T, int).


         Mapping from non-generic type names that have a generic alias in typing
         but with a different name.

        """
def _is_dunder(attr):
    """
    '__'
    """
def _GenericAlias(_Final, _root=True):
    """
    The central part of internal API.

        This represents a generic version of type 'origin' with type arguments 'params'.
        There are two kind of these aliases: user defined and special. The special ones
        are wrappers around builtin collections and ABCs in collections.abc. These must
        have 'name' always set. If 'inst' is False, then the alias can't be instantiated,
        this is used by e.g. typing.List and typing.Dict.
    
    """
    def __init__(self, origin, params, *, inst=True, special=False, name=None):
        """
         This is not documented.
        """
    def __getitem__(self, params):
        """
         Can't subscript Generic[...] or Protocol[...].

        """
    def copy_with(self, params):
        """
         We don't copy self._special.

        """
    def __repr__(self):
        """
        'Callable'
        """
    def __eq__(self, other):
        """
        f"Type {self._name} cannot be instantiated; 
        f"use {self._name.lower()}() instead
        """
    def __mro_entries__(self, bases):
        """
         generic version of an ABC or built-in class
        """
    def __getattr__(self, attr):
        """
         We are careful for copy and pickle.
         Also for simplicity we just don't relay all dunder names

        """
    def __setattr__(self, attr, val):
        """
        '_name'
        """
    def __instancecheck__(self, obj):
        """
        Subscripted generics cannot be used with
         class and instance checks
        """
    def __reduce__(self):
        """
        Same as _GenericAlias above but for variadic aliases. Currently,
            this is used only by special internal aliases: Tuple and Callable.
    
        """
    def __getitem__(self, params):
        """
        'Callable'
        """
    def __getitem_inner__(self, params):
        """
        Tuple[t, ...]: t must be a type.
        """
def Generic:
    """
    Abstract base class for generic types.

        A generic type is typically declared by inheriting from
        this class parameterized with one or more type variables.
        For example, a generic mapping type might be defined as::

          class Mapping(Generic[KT, VT]):
              def __getitem__(self, key: KT) -> VT:
                  ...
              # Etc.

        This class can then be used as follows::

          def lookup_name(mapping: Mapping[KT, VT], key: KT, default: VT) -> VT:
              try:
                  return mapping[key]
              except KeyError:
                  return default
    
    """
    def __new__(cls, *args, **kwds):
        """
        f"Type {cls.__name__} cannot be instantiated; 
        it can be used only as a base class
        """
    def __class_getitem__(cls, params):
        """
        f"Parameter list to {cls.__qualname__}[...] cannot be empty
        """
    def __init_subclass__(cls, *args, **kwargs):
        """
        '__orig_bases__'
        """
def _TypingEmpty:
    """
    Internal placeholder for () or []. Used by TupleMeta and CallableMeta
        to allow empty list/tuple in specific places, without allowing them
        to sneak in where prohibited.
    
    """
def _TypingEllipsis:
    """
    Internal placeholder for ... (ellipsis).
    """
def _get_protocol_attrs(cls):
    """
    Collect protocol members from a protocol class objects.

        This includes names actually defined in the class dictionary, as well
        as names that appear in annotations. Special names (above) are skipped.
    
    """
def _is_callable_members_only(cls):
    """
     PEP 544 prohibits using issubclass() with protocols that have non-method members.

    """
def _no_init(self, *args, **kwargs):
    """
    'Protocols cannot be instantiated'
    """
def _allow_reckless_class_cheks():
    """
    Allow instnance and class checks for special stdlib modules.

        The abc and functools modules indiscriminately call isinstance() and
        issubclass() on the whole MRO of a user class, which may contain protocols.
    
    """
def _ProtocolMeta(ABCMeta):
    """
     This metaclass is really unfortunate and exists only because of
     the lack of __instancehook__.

    """
    def __instancecheck__(cls, instance):
        """
         We need this method for situations where attributes are
         assigned in __init__.

        """
def Protocol(Generic, metadef=_ProtocolMeta):
    """
    Base class for protocol classes.

        Protocol classes are defined as::

            class Proto(Protocol):
                def meth(self) -> int:
                    ...

        Such classes are primarily used with static type checkers that recognize
        structural subtyping (static duck-typing), for example::

            class C:
                def meth(self) -> int:
                    return 0

            def func(x: Proto) -> int:
                return x.meth()

            func(C())  # Passes static type check

        See PEP 544 for details. Protocol classes decorated with
        @typing.runtime_checkable act as simple-minded runtime protocols that check
        only the presence of given attributes, ignoring their type signatures.
        Protocol classes can be generic, they are defined as::

            class GenProto(Protocol[T]):
                def meth(self) -> T:
                    ...
    
    """
    def __init_subclass__(cls, *args, **kwargs):
        """
         Determine if this is a protocol or a concrete subclass.

        """
        def _proto_hook(other):
            """
            '_is_protocol'
            """
def runtime_checkable(cls):
    """
    Mark a protocol class as a runtime protocol.

        Such protocol can be used with isinstance() and issubclass().
        Raise TypeError if applied to a non-protocol class.
        This allows a simple-minded structural check very similar to
        one trick ponies in collections.abc such as Iterable.
        For example::

            @runtime_checkable
            class Closable(Protocol):
                def close(self): ...

            assert isinstance(open('/some/file'), Closable)

        Warning: this will check only the presence of the required methods,
        not their type signatures!
    
    """
def cast(typ, val):
    """
    Cast a value to a type.

        This returns the value unchanged.  To the type checker this
        signals that the return value has the designated type, but at
        runtime we intentionally don't check anything (we want this
        to be as fast as possible).
    
    """
def _get_defaults(func):
    """
    Internal helper to extract the default arguments, by name.
    """
def get_type_hints(obj, globalns=None, localns=None):
    """
    Return type hints for an object.

        This is often the same as obj.__annotations__, but it handles
        forward references encoded as string literals, and if necessary
        adds Optional[t] if a default value equal to None is set.

        The argument may be a module, class, method, or function. The annotations
        are returned as a dictionary. For classes, annotations include also
        inherited members.

        TypeError is raised if the argument is not of a type that can contain
        annotations, and an empty dictionary is returned if no annotations are
        present.

        BEWARE -- the behavior of globalns and localns is counterintuitive
        (unless you are familiar with how eval() and exec() work).  The
        search order is locals first, then globals.

        - If no dict arguments are passed, an attempt is made to use the
          globals from obj (or the respective module's globals for classes),
          and these are also used as the locals.  If the object does not appear
          to have globals, an empty dictionary is used.

        - If one dict argument is passed, it is used for both globals and
          locals.

        - If two dict arguments are passed, they specify globals and
          locals, respectively.
    
    """
def get_origin(tp):
    """
    Get the unsubscripted version of a type.

        This supports generic types, Callable, Tuple, Union, Literal, Final and ClassVar.
        Return None for unsupported types. Examples::

            get_origin(Literal[42]) is Literal
            get_origin(int) is None
            get_origin(ClassVar[int]) is ClassVar
            get_origin(Generic) is Generic
            get_origin(Generic[T]) is Generic
            get_origin(Union[T, int]) is Union
            get_origin(List[Tuple[T, T]][int]) == list
    
    """
def get_args(tp):
    """
    Get type arguments with all substitutions performed.

        For unions, basic simplifications used by Union constructor are performed.
        Examples::
            get_args(Dict[str, int]) == (str, int)
            get_args(int) == ()
            get_args(Union[int, Union[T, int], str][int]) == (int, str)
            get_args(Union[int, Tuple[T, int]][str]) == (int, Tuple[str, int])
            get_args(Callable[[], T][int]) == ([], int)
    
    """
def no_type_check(arg):
    """
    Decorator to indicate that annotations are not type hints.

        The argument must be a class or function; if it is a class, it
        applies recursively to all methods and classes defined in that class
        (but not to methods defined in its superclasses or subclasses).

        This mutates the function(s) or class(es) in place.
    
    """
def no_type_check_decorator(decorator):
    """
    Decorator to give another decorator the @no_type_check effect.

        This wraps the decorator with something that wraps the decorated
        function in @no_type_check.
    
    """
    def wrapped_decorator(*args, **kwds):
        """
        Helper for @overload to raise when called.
        """
def overload(func):
    """
    Decorator for overloaded functions/methods.

        In a stub file, place two or more stub definitions for the same
        function in a row, each decorated with @overload.  For example:

          @overload
          def utf8(value: None) -> None: ...
          @overload
          def utf8(value: bytes) -> bytes: ...
          @overload
          def utf8(value: str) -> bytes: ...

        In a non-stub file (i.e. a regular .py file), do the same but
        follow it with an implementation.  The implementation should *not*
        be decorated with @overload.  For example:

          @overload
          def utf8(value: None) -> None: ...
          @overload
          def utf8(value: bytes) -> bytes: ...
          @overload
          def utf8(value: str) -> bytes: ...
          def utf8(value):
              # implementation goes here
    
    """
def final(f):
    """
    A decorator to indicate final methods and final classes.

        Use this decorator to indicate to type checkers that the decorated
        method cannot be overridden, and decorated class cannot be subclassed.
        For example:

          class Base:
              @final
              def done(self) -> None:
                  ...
          class Sub(Base):
              def done(self) -> None:  # Error reported by type checker
                    ...

          @final
          class Leaf:
              ...
          class Other(Leaf):  # Error reported by type checker
              ...

        There is no runtime checking of these properties.
    
    """
def _alias(origin, params, inst=True):
    """
     Not generic.
    """
def SupportsInt(Protocol):
    """
    An ABC with one abstract method __int__.
    """
    def __int__(self) -> int:
        """
        An ABC with one abstract method __float__.
        """
    def __float__(self) -> float:
        """
        An ABC with one abstract method __complex__.
        """
    def __complex__(self) -> complex:
        """
        An ABC with one abstract method __bytes__.
        """
    def __bytes__(self) -> bytes:
        """
        An ABC with one abstract method __index__.
        """
    def __index__(self) -> int:
        """
        An ABC with one abstract method __abs__ that is covariant in its return type.
        """
    def __abs__(self) -> T_co:
        """
        An ABC with one abstract method __round__ that is covariant in its return type.
        """
    def __round__(self, ndigits: int = 0) -> T_co:
        """
        NamedTuple('Name', [(f0, t0), (f1, t1), ...]); each t must be a type
        """
def NamedTupleMeta(type):
    """
    '_root'
    """
def NamedTuple(metadef=NamedTupleMeta):
    """
    Typed version of namedtuple.

        Usage in Python versions >= 3.6::

            class Employee(NamedTuple):
                name: str
                id: int

        This is equivalent to::

            Employee = collections.namedtuple('Employee', ['name', 'id'])

        The resulting class has an extra __annotations__ attribute, giving a
        dict that maps field names to types.  (The field names are also in
        the _fields attribute, which is part of the namedtuple API.)
        Alternative equivalent keyword syntax is also accepted::

            Employee = NamedTuple('Employee', name=str, id=int)

        In Python versions <= 3.5 use::

            Employee = NamedTuple('Employee', [('name', str), ('id', int)])
    
    """
    def __new__(*args, **kwargs):
        """
        'NamedTuple.__new__(): not enough arguments'
        """
def _dict_new(cls, /, *args, **kwargs):
    """
    TypedDict takes either a dict or keyword arguments,
     but not both
    """
def _check_fails(cls, other):
    """
     Typed dicts are only for static structural subtyping.

    """
def _TypedDictMeta(type):
    """
    Create new typed dict class object.

            This method is called directly when TypedDict is subclassed,
            or via _typeddict_new when TypedDict is instantiated. This way
            TypedDict supports all three syntax forms described in its docstring.
            Subclasses and instances of TypedDict return actual dictionaries
            via _dict_new.
        
    """
def TypedDict(dict, metadef=_TypedDictMeta):
    """
    A simple typed namespace. At runtime it is equivalent to a plain dict.

        TypedDict creates a dictionary type that expects all of its
        instances to have a certain set of keys, where each key is
        associated with a value of a consistent type. This expectation
        is not checked at runtime but is only enforced by type checkers.
        Usage::

            class Point2D(TypedDict):
                x: int
                y: int
                label: str

            a: Point2D = {'x': 1, 'y': 2, 'label': 'good'}  # OK
            b: Point2D = {'z': 3, 'label': 'bad'}           # Fails type check

            assert Point2D(x=1, y=2, label='first') == dict(x=1, y=2, label='first')

        The type info can be accessed via Point2D.__annotations__. TypedDict
        supports two additional equivalent forms::

            Point2D = TypedDict('Point2D', x=int, y=int, label=str)
            Point2D = TypedDict('Point2D', {'x': int, 'y': int, 'label': str})

        By default, all keys must be present in a TypedDict. It is possible
        to override this by specifying totality.
        Usage::

            class point2D(TypedDict, total=False):
                x: int
                y: int

        This means that a point2D TypedDict can have any of the keys omitted.A type
        checker is only expected to support a literal False or True as the value of
        the total argument. True is the default, and makes all items defined in the
        class body be required.

        The class syntax is only supported in Python 3.6+, while two other
        syntax forms work for Python 2.7 and 3.2+
    
    """
def NewType(name, tp):
    """
    NewType creates simple unique types with almost zero
        runtime overhead. NewType(name, tp) is considered a subtype of tp
        by static type checkers. At runtime, NewType(name, tp) returns
        a dummy function that simply returns its argument. Usage::

            UserId = NewType('UserId', int)

            def name_by_id(user_id: UserId) -> str:
                ...

            UserId('user')          # Fails type check

            name_by_id(42)          # Fails type check
            name_by_id(UserId(42))  # OK

            num = UserId(5) + 1     # type: int
    
    """
    def new_type(x):
        """
         Python-version-specific alias (Python 2: unicode; Python 3: str)

        """
def IO(Generic[AnyStr]):
    """
    Generic base class for TextIO and BinaryIO.

        This is an abstract, generic version of the return of open().

        NOTE: This does not distinguish between the different possible
        classes (text vs. binary, read vs. write vs. read/write,
        append-only, unbuffered).  The TextIO and BinaryIO subclasses
        below capture the distinctions between text vs. binary, which is
        pervasive in the interface; however we currently do not offer a
        way to track the other distinctions in the type system.
    
    """
    def mode(self) -> str:
        """
        'IO[AnyStr]'
        """
    def __exit__(self, type, value, traceback) -> None:
        """
        Typed version of the return of open() in binary mode.
        """
    def write(self, s: Union[bytes, bytearray]) -> int:
        """
        'BinaryIO'
        """
def TextIO(IO[str]):
    """
    Typed version of the return of open() in text mode.
    """
    def buffer(self) -> BinaryIO:
        """
        'TextIO'
        """
def io:
    """
    Wrapper namespace for IO generic classes.
    """
def re:
    """
    Wrapper namespace for re type aliases.
    """
