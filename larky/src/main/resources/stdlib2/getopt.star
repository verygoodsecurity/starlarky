    def _(s): return s
        """
        ''
        """
    def __init__(self, msg, opt=''):
        """
         backward compatibility
        """
def getopt(args, shortopts, longopts = []):
    """
    getopt(args, options[, long_options]) -> opts, args

        Parses command line options and parameter list.  args is the
        argument list to be parsed, without the leading reference to the
        running program.  Typically, this means "sys.argv[1:]".  shortopts
        is the string of option letters that the script wants to
        recognize, with options that require an argument followed by a
        colon (i.e., the same format that Unix getopt() uses).  If
        specified, longopts is a list of strings with the names of the
        long options which should be supported.  The leading '--'
        characters should not be included in the option name.  Options
        which require an argument should be followed by an equal sign
        ('=').

        The return value consists of two elements: the first is a list of
        (option, value) pairs; the second is the list of program arguments
        left after the option list was stripped (this is a trailing slice
        of the first argument).  Each option-and-value pair returned has
        the option as its first element, prefixed with a hyphen (e.g.,
        '-x'), and the option argument as its second element, or an empty
        string if the option has no argument.  The options occur in the
        list in the same order in which they were found, thus allowing
        multiple occurrences.  Long and short options may be mixed.

    
    """
def gnu_getopt(args, shortopts, longopts = []):
    """
    getopt(args, options[, long_options]) -> opts, args

        This function works like getopt(), except that GNU style scanning
        mode is used by default. This means that option and non-option
        arguments may be intermixed. The getopt() function stops
        processing options as soon as a non-option argument is
        encountered.

        If the first character of the option string is `+', or if the
        environment variable POSIXLY_CORRECT is set, then option
        processing stops as soon as a non-option argument is encountered.

    
    """
def do_longs(opts, opt, longopts, args):
    """
    '='
    """
def long_has_args(opt, longopts):
    """
    'option --%s not recognized'
    """
def do_shorts(opts, optstring, shortopts, args):
    """
    ''
    """
def short_has_arg(opt, shortopts):
    """
    ':'
    """
