def splitUp(pred):
    """
    Parse a single version comparison.

        Return (comparison string, StrictVersion)
    
    """
def VersionPredicate:
    """
    Parse and test package version predicates.

        >>> v = VersionPredicate('pyepat.abc (>1.0, <3333.3a1, !=1555.1b3)')

        The `name` attribute provides the full dotted name that is given::

        >>> v.name
        'pyepat.abc'

        The str() of a `VersionPredicate` provides a normalized
        human-readable version of the expression::

        >>> print(v)
        pyepat.abc (> 1.0, < 3333.3a1, != 1555.1b3)

        The `satisfied_by()` method can be used to determine with a given
        version number is included in the set described by the version
        restrictions::

        >>> v.satisfied_by('1.1')
        True
        >>> v.satisfied_by('1.4')
        True
        >>> v.satisfied_by('1.0')
        False
        >>> v.satisfied_by('4444.4')
        False
        >>> v.satisfied_by('1555.1b3')
        False

        `VersionPredicate` is flexible in accepting extra whitespace::

        >>> v = VersionPredicate(' pat( ==  0.1  )  ')
        >>> v.name
        'pat'
        >>> v.satisfied_by('0.1')
        True
        >>> v.satisfied_by('0.2')
        False

        If any version numbers passed in do not conform to the
        restrictions of `StrictVersion`, a `ValueError` is raised::

        >>> v = VersionPredicate('p1.p2.p3.p4(>=1.0, <=1.3a1, !=1.2zb3)')
        Traceback (most recent call last):
          ...
        ValueError: invalid version number '1.2zb3'

        It the module or package name given does not conform to what's
        allowed as a legal module or package name, `ValueError` is
        raised::

        >>> v = VersionPredicate('foo-bar')
        Traceback (most recent call last):
          ...
        ValueError: expected parenthesized list: '-bar'

        >>> v = VersionPredicate('foo bar (12.21)')
        Traceback (most recent call last):
          ...
        ValueError: expected parenthesized list: 'bar (12.21)'

    
    """
    def __init__(self, versionPredicateStr):
        """
        Parse a version predicate string.
        
        """
    def __str__(self):
        """
 
        """
    def satisfied_by(self, version):
        """
        True if version is compatible with all the predicates in self.
                The parameter version must be acceptable to the StrictVersion
                constructor.  It may be either a string or StrictVersion.
        
        """
def split_provision(value):
    """
    Return the name and optional version number of a provision.

        The version number, if given, will be returned as a `StrictVersion`
        instance, otherwise it will be `None`.

        >>> split_provision('mypkg')
        ('mypkg', None)
        >>> split_provision(' mypkg( 1.2 ) ')
        ('mypkg', StrictVersion ('1.2'))
    
    """
