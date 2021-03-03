def Error(Exception):
    """
    Base class for ConfigParser exceptions.
    """
    def __init__(self, msg=''):
        """
        Raised when no section matches a requested option.
        """
    def __init__(self, section):
        """
        'No section: %r'
        """
def DuplicateSectionError(Error):
    """
    Raised when a section is repeated in an input source.

        Possible repetitions that raise this exception are: multiple creation
        using the API or in strict parsers when a section is found more than once
        in a single input file, string or dictionary.
    
    """
    def __init__(self, section, source=None, lineno=None):
        """
         already exists
        """
def DuplicateOptionError(Error):
    """
    Raised by strict parsers when an option is repeated in an input source.

        Current implementation raises this exception only when an option is found
        more than once in a single file, string or dictionary.
    
    """
    def __init__(self, section, option, source=None, lineno=None):
        """
         in section 
        """
def NoOptionError(Error):
    """
    A requested option was not found.
    """
    def __init__(self, option, section):
        """
        No option %r in section: %r
        """
def InterpolationError(Error):
    """
    Base class for interpolation-related exceptions.
    """
    def __init__(self, option, section, msg):
        """
        A string substitution required a setting which was not available.
        """
    def __init__(self, option, section, rawval, reference):
        """
        Bad value substitution: option {!r} in section {!r} contains 
        an interpolation key {!r} which is not a valid option name. 
        Raw value: {!r}
        """
def InterpolationSyntaxError(InterpolationError):
    """
    Raised when the source text contains invalid syntax.

        Current implementation raises this exception when the source text into
        which substitutions are made does not conform to the required syntax.
    
    """
def InterpolationDepthError(InterpolationError):
    """
    Raised when substitutions are nested too deeply.
    """
    def __init__(self, option, section, rawval):
        """
        Recursion limit exceeded in value substitution: option {!r} 
        in section {!r} contains an interpolation key which 
        cannot be substituted in {} steps. Raw value: {!r}

        """
def ParsingError(Error):
    """
    Raised when a configuration file does not follow legal syntax.
    """
    def __init__(self, source=None, filename=None):
        """
         Exactly one of `source'/`filename' arguments has to be given.
         `filename' kept for compatibility.

        """
    def filename(self):
        """
        Deprecated, use `source'.
        """
    def filename(self, value):
        """
        Deprecated, user `source'.
        """
    def append(self, lineno, line):
        """
        '\n\t[line %2d]: %s'
        """
def MissingSectionHeaderError(ParsingError):
    """
    Raised when a key-value pair is found before any section header.
    """
    def __init__(self, filename, lineno, line):
        """
        'File contains no section headers.\nfile: %r, line: %d\n%r'
        """
def Interpolation:
    """
    Dummy interpolation that passes the value through with no changes.
    """
    def before_get(self, parser, section, option, value, defaults):
        """
        Interpolation as implemented in the classic ConfigParser.

            The option values can contain format strings which refer to other values in
            the same section, or values in the special default section.

            For example:

                something: %(dir)s/whatever

            would resolve the "%(dir)s" to the value of dir.  All reference
            expansions are done late, on demand. If a user needs to use a bare % in
            a configuration file, she can escape it by writing %%. Other % usage
            is considered a user error and raises `InterpolationSyntaxError'.
        """
    def before_get(self, parser, section, option, value, defaults):
        """
        ''
        """
    def before_set(self, parser, section, option, value):
        """
        '%%'
        """
2021-03-02 20:53:49,981 : INFO : tokenize_signature : --> do i ever get here?
    def _interpolate_some(self, parser, option, accum, rest, section, map,
                          depth):
        """
        %
        """
def ExtendedInterpolation(Interpolation):
    """
    Advanced variant of interpolation, supports the syntax used by
        `zc.buildout'. Enables interpolation between sections.
    """
    def before_get(self, parser, section, option, value, defaults):
        """
        ''
        """
    def before_set(self, parser, section, option, value):
        """
        '$$'
        """
2021-03-02 20:53:49,983 : INFO : tokenize_signature : --> do i ever get here?
    def _interpolate_some(self, parser, option, accum, rest, section, map,
                          depth):
        """
        $
        """
def LegacyInterpolation(Interpolation):
    """
    Deprecated interpolation used in old versions of ConfigParser.
        Use BasicInterpolation or ExtendedInterpolation instead.
    """
    def before_get(self, parser, section, option, value, vars):
        """
         Loop through this until it's done
        """
    def before_set(self, parser, section, option, value):
        """
        %%(%s)s
        """
def RawConfigParser(MutableMapping):
    """
    ConfigParser that does not do interpolation.
    """
2021-03-02 20:53:49,986 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,986 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,986 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,986 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:49,986 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, defaults=None, dict_type=_default_dict,
                 allow_no_value=False, *, delimiters=('=', ':'),
                 comment_prefixes=('#', ';'), inline_comment_prefixes=None,
                 strict=True, empty_lines_in_values=True,
                 default_section=DEFAULTSECT,
                 interpolation=_UNSET, converters=_UNSET):
        """
        '='
        """
    def defaults(self):
        """
        Return a list of section names, excluding [DEFAULT]
        """
    def add_section(self, section):
        """
        Create a new section in the configuration.

                Raise DuplicateSectionError if a section by the specified name
                already exists. Raise ValueError if name is DEFAULT.
        
        """
    def has_section(self, section):
        """
        Indicate whether the named section is present in the configuration.

                The DEFAULT section is not acknowledged.
        
        """
    def options(self, section):
        """
        Return a list of option names for the given section name.
        """
    def read(self, filenames, encoding=None):
        """
        Read and parse a filename or an iterable of filenames.

                Files that cannot be opened are silently ignored; this is
                designed so that you can specify an iterable of potential
                configuration file locations (e.g. current directory, user's
                home directory, systemwide directory), and all existing
                configuration files in the iterable will be read.  A single
                filename may also be given.

                Return list of successfully read files.
        
        """
    def read_file(self, f, source=None):
        """
        Like read() but the argument must be a file-like object.

                The `f' argument must be iterable, returning one line at a time.
                Optional second argument is the `source' specifying the name of the
                file being read. If not given, it is taken from f.name. If `f' has no
                `name' attribute, `<???>' is used.
        
        """
    def read_string(self, string, source='<string>'):
        """
        Read configuration from a given string.
        """
    def read_dict(self, dictionary, source='<dict>'):
        """
        Read configuration from a dictionary.

                Keys are section names, values are dictionaries with keys and values
                that should be present in the section. If the used dictionary type
                preserves order, sections and their keys will be added in order.

                All types held in the dictionary are converted to strings during
                reading, including section names, option names and keys.

                Optional second argument is the `source' specifying the name of the
                dictionary being read.
        
        """
    def readfp(self, fp, filename=None):
        """
        Deprecated, use read_file instead.
        """
    def get(self, section, option, *, raw=False, vars=None, fallback=_UNSET):
        """
        Get an option value for a given section.

                If `vars' is provided, it must be a dictionary. The option is looked up
                in `vars' (if provided), `section', and in `DEFAULTSECT' in that order.
                If the key is not found and `fallback' is provided, it is used as
                a fallback value. `None' can be provided as a `fallback' value.

                If interpolation is enabled and the optional argument `raw' is False,
                all interpolations are expanded in the return values.

                Arguments `raw', `vars', and `fallback' are keyword only.

                The section DEFAULT is special.
        
        """
    def _get(self, section, conv, option, **kwargs):
        """
         getint, getfloat and getboolean provided directly for backwards compat

        """
2021-03-02 20:53:49,992 : INFO : tokenize_signature : --> do i ever get here?
    def getint(self, section, option, *, raw=False, vars=None,
               fallback=_UNSET, **kwargs):
        """
        Return a list of (name, value) tuples for each option in a section.

                All % interpolations are expanded in the return values, based on the
                defaults passed into the constructor, unless the optional argument
                `raw' is true.  Additional substitutions may be provided using the
                `vars' argument, which must be a dictionary whose contents overrides
                any pre-existing defaults.

                The section DEFAULT is special.
        
        """
    def popitem(self):
        """
        Remove a section from the parser and return it as
                a (section_name, section_proxy) tuple. If no section is present, raise
                KeyError.

                The section DEFAULT is never returned because it cannot be removed.
        
        """
    def optionxform(self, optionstr):
        """
        Check for the existence of a given option in a given section.
                If the specified `section' is None or an empty string, DEFAULT is
                assumed. If the specified `section' does not exist, returns False.
        """
    def set(self, section, option, value=None):
        """
        Set an option.
        """
    def write(self, fp, space_around_delimiters=True):
        """
        Write an .ini-format representation of the configuration state.

                If `space_around_delimiters' is True (the default), delimiters
                between keys and values are surrounded by spaces.
        
        """
    def _write_section(self, fp, section_name, section_items, delimiter):
        """
        Write a single section to the specified `fp'.
        """
    def remove_option(self, section, option):
        """
        Remove an option.
        """
    def remove_section(self, section):
        """
        Remove a file section.
        """
    def __getitem__(self, key):
        """
         To conform with the mapping protocol, overwrites existing values in
         the section.

        """
    def __delitem__(self, key):
        """
        Cannot remove the default section.
        """
    def __contains__(self, key):
        """
         the default section
        """
    def __iter__(self):
        """
         XXX does it break when underlying container state changed?

        """
    def _read(self, fp, fpname):
        """
        Parse a sectioned configuration file.

                Each section in a configuration file contains a header, indicated by
                a name in square brackets (`[]'), plus key/value options, indicated by
                `name' and `value' delimited with a specific substring (`=' or `:' by
                default).

                Values can span multiple lines, as long as they are indented deeper
                than the first line of the value. Depending on the parser's mode, blank
                lines may be treated as parts of multiline values or ignored.

                Configuration files may include comments, prefixed by specific
                characters (`#' and `;' by default). Comments may appear on their own
                in an otherwise empty line or may be entered in lines holding values or
                section names.
        
        """
    def _join_multiline_values(self):
        """
        '\n'
        """
    def _read_defaults(self, defaults):
        """
        Read the defaults passed in the initializer.
                Note: values can be non-string.
        """
    def _handle_error(self, exc, fpname, lineno, line):
        """
        Create a sequence of lookups with 'vars' taking priority over
                the 'section' which takes priority over the DEFAULTSECT.

        
        """
    def _convert_to_boolean(self, value):
        """
        Return a boolean value translating from other types if necessary.
        
        """
    def _validate_value_types(self, *, section="", option="", value=""):
        """
        Raises a TypeError for non-string values.

                The only legal non-string value if we allow valueless
                options is None, so we need to check if the value is a
                string if:
                - we do not allow valueless options, or
                - we allow valueless options but the value is not None

                For compatibility reasons this method is not used in classic set()
                for RawConfigParsers. It is invoked in every case for mapping protocol
                access and in ConfigParser.set().
        
        """
    def converters(self):
        """
        ConfigParser implementing interpolation.
        """
    def set(self, section, option, value=None):
        """
        Set an option.  Extends RawConfigParser.set by validating type and
                interpolation syntax on the value.
        """
    def add_section(self, section):
        """
        Create a new section in the configuration.  Extends
                RawConfigParser.add_section by validating if the section name is
                a string.
        """
    def _read_defaults(self, defaults):
        """
        Reads the defaults passed in the initializer, implicitly converting
                values to strings like the rest of the API.

                Does not perform interpolation for backwards compatibility.
        
        """
def SafeConfigParser(ConfigParser):
    """
    ConfigParser alias for backwards compatibility purposes.
    """
    def __init__(self, *args, **kwargs):
        """
        The SafeConfigParser class has been renamed to ConfigParser 
        in Python 3.2. This alias will be removed in future versions.
         Use ConfigParser directly instead.
        """
def SectionProxy(MutableMapping):
    """
    A proxy for a single section from a parser.
    """
    def __init__(self, parser, name):
        """
        Creates a view on a section of the specified `name` in `parser`.
        """
    def __repr__(self):
        """
        '<Section: {}>'
        """
    def __getitem__(self, key):
        """
         The parser object of the proxy is read-only.

        """
    def name(self):
        """
         The name of the section on a proxy is read-only.

        """
2021-03-02 20:53:50,007 : INFO : tokenize_signature : --> do i ever get here?
    def get(self, option, fallback=None, *, raw=False, vars=None,
            _impl=None, **kwargs):
        """
        Get an option value.

                Unless `fallback` is provided, `None` will be returned if the option
                is not found.

        
        """
def ConverterMapping(MutableMapping):
    """
    Enables reuse of get*() methods between the parser and section proxies.

        If a parser class implements a getter directly, the value for the given
        key will be ``None``. The presence of the converter name here enables
        section proxies to find and use the implementation on the parser class.
    
    """
    def __init__(self, parser):
        """
        'name'
        """
    def __getitem__(self, key):
        """
        'get'
        """
    def __delitem__(self, key):
        """
        'get'
        """
    def __iter__(self):
