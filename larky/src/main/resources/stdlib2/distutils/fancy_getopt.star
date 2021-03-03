def FancyGetopt:
    """
    Wrapper around the standard 'getopt()' module that provides some
        handy extra functionality:
          * short and long options are tied together
          * options have help strings, and help text can be assembled
            from them
          * options set attributes of a passed-in object
          * boolean options can have "negative aliases" -- eg. if
            --quiet is the "negative alias" of --verbose, then "--quiet"
            on the command line sets 'verbose' to false
    
    """
    def __init__(self, option_table=None):
        """
         The option table is (currently) a list of tuples.  The
         tuples may have 3 or four values:
           (long_option, short_option, help_string [, repeatable])
         if an option takes an argument, its long_option should have '='
         appended; short_option should just be a single character, no ':'
         in any case.  If a long_option doesn't have a corresponding
         short_option, short_option should be None.  All option tuples
         must have long options.

        """
    def _build_index(self):
        """
        option conflict: already an option '%s'
        """
    def has_option(self, long_option):
        """
        Return true if the option table for this parser has an
                option with long name 'long_option'.
        """
    def get_attr_name(self, long_option):
        """
        Translate long option name 'long_option' to the form it
                has as an attribute of some object: ie., translate hyphens
                to underscores.
        """
    def _check_alias_dict(self, aliases, what):
        """
        invalid %s '%s': 
        option '%s' not defined
        """
    def set_aliases(self, alias):
        """
        Set the aliases for this option parser.
        """
    def set_negative_aliases(self, negative_alias):
        """
        Set the negative aliases for this option parser.
                'negative_alias' should be a dictionary mapping option names to
                option names, both the key and value must already be defined
                in the option table.
        """
    def _grok_option_table(self):
        """
        Populate the various data structures that keep tabs on the
                option table.  Called by 'getopt()' before it can do anything
                worthwhile.
        
        """
    def getopt(self, args=None, object=None):
        """
        Parse command-line options in args. Store as attributes on object.

                If 'args' is None or not supplied, uses 'sys.argv[1:]'.  If
                'object' is None or not supplied, creates a new OptionDummy
                object, stores option values there, and returns a tuple (args,
                object).  If 'object' is supplied, it is modified in place and
                'getopt()' just returns 'args'; in both cases, the returned
                'args' is a modified copy of the passed-in 'args' list, which
                is left untouched.
        
        """
    def get_option_order(self):
        """
        Returns the list of (option, value) tuples processed by the
                previous run of 'getopt()'.  Raises RuntimeError if
                'getopt()' hasn't been called yet.
        
        """
    def generate_help(self, header=None):
        """
        Generate help text (a list of strings, one per suggested line of
                output) from the option table for this FancyGetopt object.
        
        """
    def print_help(self, header=None, file=None):
        """
        \n
        """
def fancy_getopt(options, negative_opt, object, args):
    """
    ' '
    """
def wrap_text(text, width):
    """
    wrap_text(text : string, width : int) -> [string]

        Split 'text' into multiple lines of no more than 'width' characters
        each, and return the list of strings that results.
    
    """
def translate_longopt(opt):
    """
    Convert a long option name to a valid Python identifier by
        changing "-" to "_".
    
    """
def OptionDummy:
    """
    Dummy class just used as a place to hold command-line option
        values as instance attributes.
    """
    def __init__(self, options=[]):
        """
        Create a new OptionDummy instance.  The attributes listed in
                'options' will be initialized to None.
        """
