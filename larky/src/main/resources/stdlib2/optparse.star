def _repr(self):
    """
    <%s at 0x%x: %s>
    """
    def gettext(message):
        """

            Raised if an Option instance is created with invalid or
            inconsistent arguments.
    
        """
    def __init__(self, msg, option):
        """
        option %s: %s
        """
def OptionConflictError (OptionError):
    """

        Raised if conflicting options are added to an OptionParser.
    
    """
def OptionValueError (OptParseError):
    """

        Raised if an invalid option value is encountered on the command
        line.
    
    """
def BadOptionError (OptParseError):
    """

        Raised if an invalid option is seen on the command line.
    
    """
    def __init__(self, opt_str):
        """
        no such option: %s
        """
def AmbiguousOptionError (BadOptionError):
    """

        Raised if an ambiguous option is seen on the command line.
    
    """
    def __init__(self, opt_str, possibilities):
        """
        ambiguous option: %s (%s?)
        """
def HelpFormatter:
    """

        Abstract base class for formatting option help.  OptionParser
        instances should use one of the HelpFormatter subclasses for
        formatting help; by default IndentedHelpFormatter is used.

        Instance attributes:
          parser : OptionParser
            the controlling OptionParser instance
          indent_increment : int
            the number of columns to indent per nesting level
          max_help_position : int
            the maximum starting column for option help text
          help_position : int
            the calculated starting column for option help text;
            initially the same as the maximum
          width : int
            total number of columns for output (pass None to constructor for
            this value to be taken from the $COLUMNS environment variable)
          level : int
            current indentation level
          current_indent : int
            current indentation level (in columns)
          help_width : int
            number of columns available for option help text (calculated)
          default_tag : str
            text to replace with each option's default value, "%default"
            by default.  Set to false value to disable default value expansion.
          option_strings : { Option : str }
            maps Option instances to the snippet of help text explaining
            the syntax of that option, e.g. "-h, --help" or
            "-fFILE, --file=FILE"
          _short_opt_fmt : str
            format string controlling how short options with values are
            printed in help text.  Must be either "%s%s" ("-fFILE") or
            "%s %s" ("-f FILE"), because those are the two syntaxes that
            Optik supports.
          _long_opt_fmt : str
            similar but for long options; must be either "%s %s" ("--file FILE")
            or "%s=%s" ("--file=FILE").
    
    """
2021-03-02 20:53:39,439 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,440 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,440 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,440 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 indent_increment,
                 max_help_position,
                 width,
                 short_first):
        """
        'COLUMNS'
        """
    def set_parser(self, parser):
        """

        """
    def set_long_opt_delimiter(self, delim):
        """
        =
        """
    def indent(self):
        """
        Indent decreased below 0.
        """
    def format_usage(self, usage):
        """
        subclasses must implement
        """
    def format_heading(self, heading):
        """
        subclasses must implement
        """
    def _format_text(self, text):
        """

                Format a paragraph of free-form text for inclusion in the
                help output at the current indentation level.
        
        """
    def format_description(self, description):
        """
        \n
        """
    def format_epilog(self, epilog):
        """
        \n
        """
    def expand_default(self, option):
        """
         The help for each option consists of two parts:
           * the opt strings and metavars
             eg. ("-x", or "-fFILENAME, --file=FILENAME")
           * the user-supplied help string
             eg. ("turn on expert mode", "read data from FILENAME")

         If possible, we write both of these on the same line:
           -x      turn on expert mode

         But if the opt string list is too long, we put the help
         string on a second line, indented to the same column it would
         start in if it fit on the first line.
           -fFILENAME, --file=FILENAME
                   read data from FILENAME

        """
    def store_option_strings(self, parser):
        """
        Return a comma-separated list of option strings & metavariables.
        """
def IndentedHelpFormatter (HelpFormatter):
    """
    Format help with indented section bodies.
    
    """
2021-03-02 20:53:39,445 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,445 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,445 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,445 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 indent_increment=2,
                 max_help_position=24,
                 width=None,
                 short_first=1):
        """
        Usage: %s\n
        """
    def format_heading(self, heading):
        """
        %*s%s:\n
        """
def TitledHelpFormatter (HelpFormatter):
    """
    Format help with underlined section headers.
    
    """
2021-03-02 20:53:39,446 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,446 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,446 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,446 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 indent_increment=0,
                 max_help_position=24,
                 width=None,
                 short_first=0):
        """
        %s  %s\n
        """
    def format_heading(self, heading):
        """
        %s\n%s\n
        """
def _parse_num(val, type):
    """
    0x
    """
def _parse_int(val):
    """
    int
    """
def check_builtin(option, opt, value):
    """
    option %s: invalid %s value: %r
    """
def check_choice(option, opt, value):
    """
    , 
    """
def Option:
    """

        Instance attributes:
          _short_opts : [string]
          _long_opts : [string]

          action : string
          type : string
          dest : string
          default : any
          nargs : int
          const : any
          choices : [string]
          callback : function
          callback_args : (any*)
          callback_kwargs : { string : any }
          help : string
          metavar : string
    
    """
    def __init__(self, *opts, **attrs):
        """
         Set _short_opts, _long_opts attrs from 'opts' tuple.
         Have to be set now, in case no option strings are supplied.

        """
    def _check_opt_strings(self, opts):
        """
         Filter out None because early versions of Optik had exactly
         one short option and one long option, either of which
         could be None.

        """
    def _set_opt_strings(self, opts):
        """
        invalid option string %r: 
        must be at least two characters long
        """
    def _set_attrs(self, attrs):
        """
        'default'
        """
    def _check_action(self):
        """
        store
        """
    def _check_type(self):
        """
         The "choices" attribute implies "choice" type.

        """
    def _check_choice(self):
        """
        choice
        """
    def _check_dest(self):
        """
         No destination given, and we need one for this action.  The
         self.type check is for callbacks that take a value.

        """
    def _check_const(self):
        """
        'const' must not be supplied for action %r
        """
    def _check_nargs(self):
        """
        'nargs' must not be supplied for action %r
        """
    def _check_callback(self):
        """
        callback
        """
    def __str__(self):
        """
        /
        """
    def takes_value(self):
        """
         -- Processing methods --------------------------------------------


        """
    def check_value(self, opt, value):
        """
         First, convert the value(s) to the right type.  Howl if any
         value(s) are bogus.

        """
    def take_action(self, action, dest, opt, value, values, parser):
        """
        store
        """
def Values:
    """

            Update the option values from an arbitrary dictionary, but only
            use keys from dict that already have a corresponding attribute
            in self.  Any keys in dict without a corresponding attribute
            are silently ignored.
        
    """
    def _update_loose(self, dict):
        """

                Update the option values from an arbitrary dictionary,
                using all keys from the dictionary regardless of whether
                they have a corresponding attribute in self or not.
        
        """
    def _update(self, dict, mode):
        """
        careful
        """
    def read_module(self, modname, mode="careful"):
        """
        careful
        """
    def ensure_value(self, attr, value):
        """

            Abstract base class.

            Class attributes:
              standard_option_list : [Option]
                list of standard options that will be accepted by all instances
                of this parser class (intended to be overridden by subclasses).

            Instance attributes:
              option_list : [Option]
                the list of Option objects contained by this OptionContainer
              _short_opt : { string : Option }
                dictionary mapping short option strings, eg. "-f" or "-X",
                to the Option instances that implement them.  If an Option
                has multiple short option strings, it will appear in this
                dictionary multiple times. [1]
              _long_opt : { string : Option }
                dictionary mapping long option strings, eg. "--file" or
                "--exclude", to the Option instances that implement them.
                Again, a given Option can occur multiple times in this
                dictionary. [1]
              defaults : { string : any }
                dictionary mapping option destination names to default
                values for each destination [1]

            [1] These mappings are common to (shared by) all components of the
                controlling OptionParser, where they are initially created.

    
        """
    def __init__(self, option_class, conflict_handler, description):
        """
         Initialize the option list and related data structures.
         This method must be provided by subclasses, and it must
         initialize at least the following instance attributes:
         option_list, _short_opt, _long_opt, defaults.

        """
    def _create_option_mappings(self):
        """
         For use by OptionParser constructor -- create the main
         option mappings used by this OptionParser and all
         OptionGroups that it owns.

        """
    def _share_option_mappings(self, parser):
        """
         For use by OptionGroup constructor -- use shared option
         mappings from the OptionParser that owns this OptionGroup.

        """
    def set_conflict_handler(self, handler):
        """
        error
        """
    def set_description(self, description):
        """
        see OptionParser.destroy().
        """
    def _check_conflict(self, option):
        """
        error
        """
    def add_option(self, *args, **kwargs):
        """
        add_option(Option)
                   add_option(opt_str, ..., kwarg=val, ...)
        
        """
    def add_options(self, option_list):
        """
         -- Option query/removal methods ----------------------------------


        """
    def get_option(self, opt_str):
        """
        no such option %r
        """
    def format_option_help(self, formatter):
        """

        """
    def format_description(self, formatter):
        """
        \n
        """
def OptionGroup (OptionContainer):
    """
    see OptionParser.destroy().
    """
    def format_help(self, formatter):
        """

            Class attributes:
              standard_option_list : [Option]
                list of standard options that will be accepted by all instances
                of this parser class (intended to be overridden by subclasses).

            Instance attributes:
              usage : string
                a usage string for your program.  Before it is displayed
                to the user, "%prog" will be expanded to the name of
                your program (self.prog or os.path.basename(sys.argv[0])).
              prog : string
                the name of the current program (to override
                os.path.basename(sys.argv[0])).
              description : string
                A paragraph of text giving a brief overview of your program.
                optparse reformats this paragraph to fit the current terminal
                width and prints it when the user requests help (after usage,
                but before the list of options).
              epilog : string
                paragraph of help text to print after option help

              option_groups : [OptionGroup]
                list of option groups in this parser (option groups are
                irrelevant for parsing the command-line, but very useful
                for generating help)

              allow_interspersed_args : bool = true
                if true, positional arguments may be interspersed with options.
                Assuming -a and -b each take a single argument, the command-line
                  -ablah foo bar -bboo baz
                will be interpreted the same as
                  -ablah -bboo -- foo bar baz
                If this flag were false, that command line would be interpreted as
                  -ablah -- foo bar -bboo baz
                -- ie. we stop processing options as soon as we see the first
                non-option argument.  (This is the tradition followed by
                Python's getopt module, Perl's Getopt::Std, and other argument-
                parsing libraries, but it is generally annoying to users.)

              process_default_values : bool = true
                if true, option default values are processed similarly to option
                values from the command line: that is, they are passed to the
                type-checking function for the option's type (as long as the
                default value is a string).  (This really only matters if you
                have defined custom types; see SF bug #955889.)  Set it to false
                to restore the behaviour of Optik 1.4.1 and earlier.

              rargs : [string]
                the argument list currently being parsed.  Only set when
                parse_args() is active, and continually trimmed down as
                we consume arguments.  Mainly there for the benefit of
                callback options.
              largs : [string]
                the list of leftover arguments that we have skipped while
                parsing options.  If allow_interspersed_args is false, this
                list is always empty.
              values : Values
                the set of option values currently being accumulated.  Only
                set when parse_args() is active.  Also mainly for callbacks.

            Because of the 'rargs', 'largs', and 'values' attributes,
            OptionParser is not thread-safe.  If, for some perverse reason, you
            need to parse command-line arguments simultaneously in different
            threads, use different OptionParser instances.

    
        """
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:39,462 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 usage=None,
                 option_list=None,
                 option_class=Option,
                 version=None,
                 conflict_handler="error",
                 description=None,
                 formatter=None,
                 add_help_option=True,
                 prog=None,
                 epilog=None):
        """
         Populate the option list; initial sources are the
         standard_option_list class attribute, the 'option_list'
         argument, and (if applicable) the _add_version_option() and
         _add_help_option() methods.

        """
    def destroy(self):
        """

                Declare that you are done with this OptionParser.  This cleans up
                reference cycles so the OptionParser (and all objects referenced by
                it) can be garbage-collected promptly.  After calling destroy(), the
                OptionParser is unusable.
        
        """
    def _create_option_list(self):
        """
        -h
        """
    def _add_version_option(self):
        """
        --version
        """
    def _populate_option_list(self, option_list, add_help=True):
        """
         These are set in parse_args() for the convenience of callbacks.

        """
    def set_usage(self, usage):
        """
        %prog [options]
        """
    def enable_interspersed_args(self):
        """
        Set parsing to not stop on the first non-option, allowing
                interspersing switches with command arguments. This is the
                default behavior. See also disable_interspersed_args() and the
                class documentation description of the attribute
                allow_interspersed_args.
        """
    def disable_interspersed_args(self):
        """
        Set parsing to stop on the first non-option. Use this if
                you have a command processor which runs another command that
                has options of its own and you want to make sure these options
                don't get confused.
        
        """
    def set_process_default_values(self, process):
        """
         Old, pre-Optik 1.5 behaviour.

        """
    def add_option_group(self, *args, **kwargs):
        """
         XXX lots of overlap with OptionContainer.add_option()

        """
    def get_option_group(self, opt_str):
        """
         -- Option-parsing methods ----------------------------------------


        """
    def _get_args(self, args):
        """
         don't modify caller's list
        """
    def parse_args(self, args=None, values=None):
        """

                parse_args(args : [string] = sys.argv[1:],
                           values : Values = None)
                -> (values : Values, args : [string])

                Parse the command-line options found in 'args' (default:
                sys.argv[1:]).  Any errors result in a call to 'error()', which
                by default prints the usage message to stderr and calls
                sys.exit() with an error message.  On success returns a pair
                (values, args) where 'values' is a Values instance (with all
                your option values) and 'args' is the list of arguments left
                over after parsing options.
        
        """
    def check_values(self, values, args):
        """

                check_values(values : Values, args : [string])
                -> (values : Values, args : [string])

                Check that the supplied option values and leftover arguments are
                valid.  Returns the option values and leftover arguments
                (possibly adjusted, possibly completely new -- whatever you
                like).  Default implementation just returns the passed-in
                values; subclasses may override as desired.
        
        """
    def _process_args(self, largs, rargs, values):
        """
        _process_args(largs : [string],
                                 rargs : [string],
                                 values : Values)

                Process command-line arguments and populate 'values', consuming
                options and arguments from 'rargs'.  If 'allow_interspersed_args' is
                false, stop at the first non-option argument.  If true, accumulate any
                interspersed non-option arguments in 'largs'.
        
        """
    def _match_long_opt(self, opt):
        """
        _match_long_opt(opt : string) -> string

                Determine which long option string 'opt' matches, ie. which one
                it is an unambiguous abbreviation for.  Raises BadOptionError if
                'opt' doesn't unambiguously match any long option string.
        
        """
    def _process_long_opt(self, rargs, values):
        """
         Value explicitly attached to arg?  Pretend it's the next
         argument.

        """
    def _process_short_opts(self, rargs, values):
        """
        -
        """
    def get_prog_name(self):
        """
        %prog
        """
    def get_description(self):
        """
        error(msg : string)

                Print a usage message incorporating 'msg' to stderr and exit.
                If you override this in a subclass, it should not return -- it
                should either exit or raise an exception.
        
        """
    def get_usage(self):
        """

        """
    def print_usage(self, file=None):
        """
        print_usage(file : file = stdout)

                Print the usage message for the current program (self.usage) to
                'file' (default stdout).  Any occurrence of the string "%prog" in
                self.usage is replaced with the name of the current program
                (basename of sys.argv[0]).  Does nothing if self.usage is empty
                or not defined.
        
        """
    def get_version(self):
        """

        """
    def print_version(self, file=None):
        """
        print_version(file : file = stdout)

                Print the version message for this program (self.version) to
                'file' (default stdout).  As with print_usage(), any occurrence
                of "%prog" in self.version is replaced by the current program's
                name.  Does nothing if self.version is empty or undefined.
        
        """
    def format_option_help(self, formatter=None):
        """
        Options
        """
    def format_epilog(self, formatter):
        """
        \n
        """
    def print_help(self, file=None):
        """
        print_help(file : file = stdout)

                Print an extended help message, listing all options and any
                help text provided with them, to 'file' (default stdout).
        
        """
def _match_abbrev(s, wordmap):
    """
    _match_abbrev(s : string, wordmap : {string : Option}) -> string

        Return the string key in 'wordmap' for which 's' is an unambiguous
        abbreviation.  If 's' is found to be ambiguous or doesn't match any of
        'words', raise BadOptionError.
    
    """
