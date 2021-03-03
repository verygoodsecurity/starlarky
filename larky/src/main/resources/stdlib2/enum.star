def _is_descriptor(obj):
    """
    Returns True if obj is a descriptor, False otherwise.
    """
def _is_dunder(name):
    """
    Returns True if a __dunder__ name, False otherwise.
    """
def _is_sunder(name):
    """
    Returns True if a _sunder_ name, False otherwise.
    """
def _make_class_unpicklable(cls):
    """
    Make the given class un-picklable.
    """
    def _break_on_call_reduce(self, proto):
        """
        '%r cannot be pickled'
        """
def auto:
    """

        Instances are replaced with an appropriate value in Enum class suites.
    
    """
def _EnumDict(dict):
    """
    Track enum member order and ensure member names are not reused.

        EnumMeta will use the names found in self._member_names as the
        enumeration member names.

    
    """
    def __init__(self):
        """
        Changes anything not dundered or not a descriptor.

                If an enum member name is used twice, an error is raised; duplicate
                values are not checked for.

                Single underscore (sunder) names are reserved.

        
        """
def EnumMeta(type):
    """
    Metaclass for Enum
    """
    def __prepare__(metacls, cls, bases):
        """
         create the namespace dict

        """
    def __new__(metacls, cls, bases, classdict):
        """
         an Enum class is final once enumeration items have been defined; it
         cannot be mixed with other types (int, float, etc.) if it has an
         inherited __new__ unless a new __new__ is defined (or the resulting
         class will fail).

         remove any keys listed in _ignore_

        """
    def __bool__(self):
        """

                classes/types should always be True.
        
        """
    def __call__(cls, value, names=None, *, module=None, qualname=None, type=None, start=1):
        """
        Either returns an existing member, or creates a new enum class.

                This method is used both when an enum class is given a value to match
                to an enumeration member (i.e. Color(3)) and for the functional API
                (i.e. Color = Enum('Color', names='RED GREEN BLUE')).

                When used for the functional API:

                `value` will be the name of the new class.

                `names` should be either a string of white-space/comma delimited names
                (values will start at `start`), or an iterator/mapping of name, value pairs.

                `module` should be set to the module this class is being created in;
                if it is not set, an attempt to find that module will be made, but if
                it fails the class will not be picklable.

                `qualname` should be set to the actual location this class can be found
                at in its module; by default it is set to the global scope.  If this is
                not correct, unpickling will fail in some circumstances.

                `type`, if set, will be mixed in as the first base class.

        
        """
    def __contains__(cls, member):
        """
        unsupported operand type(s) for 'in': '%s' and '%s'
        """
    def __delattr__(cls, attr):
        """
         nicer error message when someone tries to delete an attribute
         (see issue19025).

        """
    def __dir__(self):
        """
        '__class__'
        """
    def __getattr__(cls, name):
        """
        Return the enum member matching `name`

                We use __getattr__ instead of descriptors or inserting into the enum
                class' __dict__ in order to support `name` and `value` being both
                properties for enum members (which live in the class' __dict__) and
                enum members themselves.

        
        """
    def __getitem__(cls, name):
        """
        Returns a mapping of member name->value.

                This mapping lists all enum members, including aliases. Note that this
                is a read-only view of the internal mapping.

        
        """
    def __repr__(cls):
        """
        <enum %r>
        """
    def __reversed__(cls):
        """
        Block attempts to reassign Enum members.

                A simple assignment to the class namespace only changes one of the
                several possible ways to get an Enum member from the Enum class,
                resulting in an inconsistent Enumeration.

        
        """
    def _create_(cls, class_name, names, *, module=None, qualname=None, type=None, start=1):
        """
        Convenience method to create a new Enum class.

                `names` can be:

                * A string containing member names, separated either with spaces or
                  commas.  Values are incremented by 1 from `start`.
                * An iterable of member names.  Values are incremented by 1 from `start`.
                * An iterable of (member name, value) pairs.
                * A mapping of member name -> value pairs.

        
        """
    def _convert_(cls, name, module, filter, source=None):
        """

                Create a new Enum subclass that replaces a collection of global constants
        
        """
    def _convert(cls, *args, **kwargs):
        """
        _convert is deprecated and will be removed in 3.9, use 
        _convert_ instead.
        """
    def _get_mixins_(bases):
        """
        Returns the type for creating enum members, and the first inherited
                enum class.

                bases: the tuple of bases that was given to __new__

        
        """
        def _find_data_type(bases):
            """
            '__new__'
            """
    def _find_new_(classdict, member_type, first_enum):
        """
        Returns the __new__ to be used for creating the enum members.

                classdict: the class dictionary given to __new__
                member_type: the data type whose __new__ will be used by default
                first_enum: enumeration to check for an overriding __new__

        
        """
def Enum(metadef=EnumMeta):
    """
    Generic enumeration.

        Derive from this class to define new enumerations.

    
    """
    def __new__(cls, value):
        """
         all enum instances are actually created during class construction
         without calling this method; this method is called by the metaclass'
         __call__ (i.e. Color(3) ), and by pickle

        """
    def _generate_next_value_(name, start, count, last_values):
        """
        %r is not a valid %s
        """
    def __repr__(self):
        """
        <%s.%s: %r>
        """
    def __str__(self):
        """
        %s.%s
        """
    def __dir__(self):
        """
        '_'
        """
    def __format__(self, format_spec):
        """
         mixed-in Enums should use the mixed-in type's __format__, otherwise
         we can get strange results with the Enum name showing up instead of
         the value

         pure Enum branch

        """
    def __hash__(self):
        """
         DynamicClassAttribute is used to provide access to the `name` and
         `value` properties of enum members while keeping some measure of
         protection from modification, while still allowing for an enumeration
         to have members named `name` and `value`.  This works because enumeration
         members are not set directly on the enum class -- __getattr__ is
         used to look them up.


        """
    def name(self):
        """
        The name of the Enum member.
        """
    def value(self):
        """
        The value of the Enum member.
        """
def IntEnum(int, Enum):
    """
    Enum where members are also (and must be) ints
    """
def _reduce_ex_by_name(self, proto):
    """
    Support for flags
    """
    def _generate_next_value_(name, start, count, last_values):
        """

                Generate the next value when not given.

                name: the name of the member
                start: the initial start value or None
                count: the number of existing members
                last_value: the last value assigned or None
        
        """
    def _missing_(cls, value):
        """

                Create a composite member iff value contains only members.
        
        """
    def __contains__(self, other):
        """
        unsupported operand type(s) for 'in': '%s' and '%s'
        """
    def __repr__(self):
        """
        '<%s.%s: %r>'
        """
    def __str__(self):
        """
        '%s.%s'
        """
    def __bool__(self):
        """
        Support for integer-based Flags
        """
    def _missing_(cls, value):
        """
        %r is not a valid %s
        """
    def _create_pseudo_member_(cls, value):
        """
         get unaccounted for bits

        """
    def __or__(self, other):
        """
        returns index of highest bit, or -1 if value is zero or negative
        """
def unique(enumeration):
    """
    Class decorator for enumerations ensuring unique member values.
    """
def _decompose(flag, value):
    """
    Extract all members from the value.
    """
def _power_of_two(value):
