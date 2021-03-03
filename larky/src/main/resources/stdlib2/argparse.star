def _AttributeHolder(object):
    """
    Abstract base class that provides __repr__.

        The __repr__ method returns a string in the format::
            ClassName(attr=name, attr=name, ...)
        The attributes are determined either by a class-level attribute,
        '_kwarg_names', or by inspecting the instance __dict__.
    
    """
    def __repr__(self):
        """
        '%s=%r'
        """
    def _get_kwargs(self):
        """
         The copy module is used only in the 'append' and 'append_const'
         actions, and it is needed only when the default value isn't a list.
         Delay its import for speeding up the common case.

        """
def HelpFormatter(object):
    """
    Formatter for generating usage messages and argument help strings.

        Only the name of this class is considered a public API. All the methods
        provided by the class are considered an implementation detail.
    
    """
2021-03-02 20:53:56,133 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,133 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,133 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,133 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 prog,
                 indent_increment=2,
                 max_help_position=24,
                 width=None):
        """
         default setting for width

        """
    def _indent(self):
        """
        'Indent decreased below 0.'
        """
    def _Section(object):
    """
     format the indented section

    """
    def _add_item(self, func, args):
        """
         ========================
         Message building methods
         ========================

        """
    def start_section(self, heading):
        """
         find all invocations

        """
    def add_arguments(self, actions):
        """
         =======================
         Help-formatting methods
         =======================

        """
    def format_help(self):
        """
        '\n\n'
        """
    def _join_parts(self, part_strings):
        """
        ''
        """
    def _format_usage(self, usage, actions, groups, prefix):
        """
        'usage: '
        """
                def get_lines(parts, indent, prefix=None):
                    """
                    ' '
                    """
    def _format_actions_usage(self, actions, groups):
        """
         find group indices and identify actions in groups

        """
    def _format_text(self, text):
        """
        '%(prog)'
        """
    def _format_action(self, action):
        """
         determine the required width and the entry label

        """
    def _format_action_invocation(self, action):
        """
         if the Optional doesn't take a value, format is:
            -s, --long

        """
    def _metavar_formatter(self, action, default_metavar):
        """
        '{%s}'
        """
        def format(tuple_size):
            """
            '%s'
            """
    def _expand_help(self, action):
        """
        '__name__'
        """
    def _iter_indented_subactions(self, action):
        """
        ' '
        """
    def _fill_text(self, text, width, indent):
        """
        ' '
        """
    def _get_help_string(self, action):
        """
        Help message formatter which retains any formatting in descriptions.

            Only the name of this class is considered a public API. All the methods
            provided by the class are considered an implementation detail.
    
        """
    def _fill_text(self, text, width, indent):
        """
        ''
        """
def RawTextHelpFormatter(RawDescriptionHelpFormatter):
    """
    Help message formatter which retains formatting of all help text.

        Only the name of this class is considered a public API. All the methods
        provided by the class are considered an implementation detail.
    
    """
    def _split_lines(self, text, width):
        """
        Help message formatter which adds default values to argument help.

            Only the name of this class is considered a public API. All the methods
            provided by the class are considered an implementation detail.
    
        """
    def _get_help_string(self, action):
        """
        '%(default)'
        """
def MetavarTypeHelpFormatter(HelpFormatter):
    """
    Help message formatter which uses the argument 'type' as the default
        metavar value (instead of the argument 'dest')

        Only the name of this class is considered a public API. All the methods
        provided by the class are considered an implementation detail.
    
    """
    def _get_default_metavar_for_optional(self, action):
        """
         =====================
         Options and Arguments
         =====================


        """
def _get_action_name(argument):
    """
    '/'
    """
def ArgumentError(Exception):
    """
    An error from creating or using an argument (optional or positional).

        The string value of this exception is the message, augmented with
        information about the argument that caused it.
    
    """
    def __init__(self, argument, message):
        """
        '%(message)s'
        """
def ArgumentTypeError(Exception):
    """
    An error from trying to convert a command line string to a type.
    """
def Action(_AttributeHolder):
    """
    Information about how to convert command line strings to Python objects.

        Action objects are used by an ArgumentParser to represent the information
        needed to parse a single argument from one or more strings from the
        command line. The keyword arguments to the Action constructor are also
        all attributes of Action instances.

        Keyword Arguments:

            - option_strings -- A list of command-line option strings which
                should be associated with this action.

            - dest -- The name of the attribute to hold the created object(s)

            - nargs -- The number of command-line arguments that should be
                consumed. By default, one argument will be consumed and a single
                value will be produced.  Other values include:
                    - N (an integer) consumes N arguments (and produces a list)
                    - '?' consumes zero or one arguments
                    - '*' consumes zero or more arguments (and produces a list)
                    - '+' consumes one or more arguments (and produces a list)
                Note that the difference between the default and nargs=1 is that
                with the default, a single value will be produced, while with
                nargs=1, a list containing a single value will be produced.

            - const -- The value to be produced if the option is specified and the
                option uses an action that takes no values.

            - default -- The value to be produced if the option is not specified.

            - type -- A callable that accepts a single string argument, and
                returns the converted value.  The standard Python types str, int,
                float, and complex are useful examples of such callables.  If None,
                str is used.

            - choices -- A container of values that should be allowed. If not None,
                after a command-line argument has been converted to the appropriate
                type, an exception will be raised if it is not a member of this
                collection.

            - required -- True if the action must always be specified at the
                command line. This is only meaningful for optional command-line
                arguments.

            - help -- The help string describing the argument.

            - metavar -- The name to be used for the option's argument with the
                help string. If None, the 'dest' value will be used as the name.
    
    """
2021-03-02 20:53:56,148 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,149 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,149 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,149 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,149 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,149 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,149 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,149 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,149 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,149 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 option_strings,
                 dest,
                 nargs=None,
                 const=None,
                 default=None,
                 type=None,
                 choices=None,
                 required=False,
                 help=None,
                 metavar=None):
        """
        'option_strings'
        """
    def __call__(self, parser, namespace, values, option_string=None):
        """
        '.__call__() not defined'
        """
def _StoreAction(Action):
    """
    'nargs for store actions must be != 0; if you '
    'have nothing to store, actions such as store '
    'true or store const may be more appropriate'
    """
    def __call__(self, parser, namespace, values, option_string=None):
        """
        'nargs for append actions must be != 0; if arg '
        'strings are not supplying the value to append, '
        'the append const action may be more appropriate'
        """
    def __call__(self, parser, namespace, values, option_string=None):
        """
        show program's version number and exit
        """
    def __call__(self, parser, namespace, values, option_string=None):
        """
        ' (%s)'
        """
2021-03-02 20:53:56,155 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,155 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,155 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,155 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,155 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,155 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,155 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 option_strings,
                 prog,
                 parser_class,
                 dest=SUPPRESS,
                 required=False,
                 help=None,
                 metavar=None):
        """
         set prog from the existing prefix

        """
    def _get_subactions(self):
        """
         set the parser name if requested

        """
def _ExtendAction(_AppendAction):
    """
     ==============
     Type classes
     ==============


    """
def FileType(object):
    """
    Factory for creating file object types

        Instances of FileType are typically passed as type= arguments to the
        ArgumentParser add_argument() method.

        Keyword Arguments:
            - mode -- A string indicating how the file is to be opened. Accepts the
                same values as the builtin open() function.
            - bufsize -- The file's desired buffer size. Accepts the same values as
                the builtin open() function.
            - encoding -- The file's encoding. Accepts the same values as the
                builtin open() function.
            - errors -- A string indicating how encoding and decoding errors are to
                be handled. Accepts the same value as the builtin open() function.
    
    """
    def __init__(self, mode='r', bufsize=-1, encoding=None, errors=None):
        """
         the special argument "-" means sys.std{in,out}

        """
    def __repr__(self):
        """
        'encoding'
        """
def Namespace(_AttributeHolder):
    """
    Simple object for storing attributes.

        Implements equality by attribute names and values, and provides a simple
        string representation.
    
    """
    def __init__(self, **kwargs):
        """
         set up registries

        """
    def register(self, registry_name, value, object):
        """
         ==================================
         Namespace default accessor methods
         ==================================

        """
    def set_defaults(self, **kwargs):
        """
         if these defaults match any existing arguments, replace
         the previous default on the object with the new one

        """
    def get_default(self, dest):
        """
         =======================
         Adding argument actions
         =======================

        """
    def add_argument(self, *args, **kwargs):
        """

                add_argument(dest, ..., name=value, ...)
                add_argument(option_string, option_string, ..., name=value, ...)
        
        """
    def add_argument_group(self, *args, **kwargs):
        """
         resolve any conflicts

        """
    def _remove_action(self, action):
        """
         collect groups by titles

        """
    def _get_positional_kwargs(self, dest, **kwargs):
        """
         make sure required is not specified

        """
    def _get_optional_kwargs(self, *args, **kwargs):
        """
         determine short and long option strings

        """
    def _pop_action_class(self, kwargs, default=None):
        """
        'action'
        """
    def _get_handler(self):
        """
         determine function from conflict handler string

        """
    def _check_conflict(self, action):
        """
         find all options that conflict with this option

        """
    def _handle_conflict_error(self, action, conflicting_actions):
        """
        'conflicting option string: %s'
        """
    def _handle_conflict_resolve(self, action, conflicting_actions):
        """
         remove all conflicting options

        """
def _ArgumentGroup(_ActionsContainer):
    """
     add any missing keyword arguments by checking the container

    """
    def _add_action(self, action):
        """
        'mutually exclusive arguments must be optional'
        """
    def _remove_action(self, action):
        """
        Object for parsing command line strings into Python objects.

            Keyword Arguments:
                - prog -- The name of the program (default: sys.argv[0])
                - usage -- A usage message (default: auto-generated from arguments)
                - description -- A description of what the program does
                - epilog -- Text following the argument descriptions
                - parents -- Parsers whose arguments should be copied into this one
                - formatter_class -- HelpFormatter class for printing help messages
                - prefix_chars -- Characters that prefix optional arguments
                - fromfile_prefix_chars -- Characters that prefix files containing
                    additional arguments
                - argument_default -- The default value for all arguments
                - conflict_handler -- String indicating how to handle conflicts
                - add_help -- Add a -h/-help option
                - allow_abbrev -- Allow long options to be abbreviated unambiguously
    
        """
2021-03-02 20:53:56,168 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,168 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,168 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,168 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,169 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,169 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,169 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,169 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,169 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,169 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,169 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:56,169 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 prog=None,
                 usage=None,
                 description=None,
                 epilog=None,
                 parents=[],
                 formatter_class=HelpFormatter,
                 prefix_chars='-',
                 fromfile_prefix_chars=None,
                 argument_default=None,
                 conflict_handler='error',
                 add_help=True,
                 allow_abbrev=True):
        """
         default setting for prog

        """
        def identity(string):
            """
            'type'
            """
    def _get_kwargs(self):
        """
        'prog'
        """
    def add_subparsers(self, **kwargs):
        """
        'cannot have multiple subparser arguments'
        """
    def _add_action(self, action):
        """
         =====================================
         Command line argument parsing methods
         =====================================

        """
    def parse_args(self, args=None, namespace=None):
        """
        'unrecognized arguments: %s'
        """
    def parse_known_args(self, args=None, namespace=None):
        """
         args default to the system args

        """
    def _parse_known_args(self, arg_strings, namespace):
        """
         replace arg strings that are file references

        """
        def take_action(action, argument_strings, option_string=None):
            """
             error if this argument is not allowed with other previously
             seen arguments, assuming that actions that use the default
             value don't really count as "present

            """
        def consume_optional(start_index):
            """
             get the optional identified at this index

            """
        def consume_positionals(start_index):
            """
             match as many Positionals as possible

            """
    def _read_args_from_files(self, arg_strings):
        """
         expand arguments referencing files

        """
    def convert_arg_line_to_args(self, arg_line):
        """
         match the pattern for this action to the arg strings

        """
    def _match_arguments_partial(self, actions, arg_strings_pattern):
        """
         progressively shorten the actions list by slicing off the
         final actions until we find a match

        """
    def _parse_optional(self, arg_string):
        """
         if it's an empty string, it was meant to be a positional

        """
    def _get_option_tuples(self, option_string):
        """
         option strings starting with two prefix characters are only
         split at the '='

        """
    def _get_nargs_pattern(self, action):
        """
         in all examples below, we have to allow for '--' args
         which are represented as '-' in the pattern

        """
    def parse_intermixed_args(self, args=None, namespace=None):
        """
        'unrecognized arguments: %s'
        """
    def parse_known_intermixed_args(self, args=None, namespace=None):
        """
         returns a namespace and list of extras

         positional can be freely intermixed with optionals.  optionals are
         first parsed with all positional arguments deactivated.  The 'extras'
         are then parsed.  If the parser definition is incompatible with the
         intermixed assumptions (e.g. use of REMAINDER, subparsers) a
         TypeError is raised.

         positionals are 'deactivated' by setting nargs and default to
         SUPPRESS.  This blocks the addition of that positional to the
         namespace


        """
    def _get_values(self, action, arg_strings):
        """
         for everything but PARSER, REMAINDER args, strip out first '--'

        """
    def _get_value(self, action, arg_string):
        """
        'type'
        """
    def _check_value(self, action, value):
        """
         converted value must be one of the choices (if specified)

        """
    def format_usage(self):
        """
         usage

        """
    def _get_formatter(self):
        """
         =====================
         Help-printing methods
         =====================

        """
    def print_usage(self, file=None):
        """
         ===============
         Exiting methods
         ===============

        """
    def exit(self, status=0, message=None):
        """
        error(message: string)

                Prints a usage message incorporating the message to stderr and
                exits.

                If you override this in a subclass, it should not return -- it
                should either exit or raise an exception.
        
        """
