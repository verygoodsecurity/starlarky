def Command:
    """
    Abstract base class for defining command classes, the "worker bees"
        of the Distutils.  A useful analogy for command classes is to think of
        them as subroutines with local variables called "options".  The options
        are "declared" in 'initialize_options()' and "defined" (given their
        final values, aka "finalized") in 'finalize_options()', both of which
        must be defined by every command class.  The distinction between the
        two is necessary because option values might come from the outside
        world (command line, config file, ...), and any options dependent on
        other options must be computed *after* these outside influences have
        been processed -- hence 'finalize_options()'.  The "body" of the
        subroutine, where it does all its work based on the values of its
        options, is the 'run()' method, which must also be implemented by every
        command class.
    
    """
    def __init__(self, dist):
        """
        Create and initialize a new Command object.  Most importantly,
                invokes the 'initialize_options()' method, which is the real
                initializer and depends on the actual command being
                instantiated.
        
        """
    def __getattr__(self, attr):
        """
        'dry_run'
        """
    def ensure_finalized(self):
        """
         Subclasses must define:
           initialize_options()
             provide default values for all options; may be customized by
             setup script, by options from config file(s), or by command-line
             options
           finalize_options()
             decide on the final values for all options; this is called
             after all possible intervention from the outside world
             (command-line, option file, etc.) has been processed
           run()
             run the command: do whatever it is we're here to do,
             controlled by the command's various option values


        """
    def initialize_options(self):
        """
        Set default values for all the options that this command
                supports.  Note that these defaults may be overridden by other
                commands, by the setup script, by config files, or by the
                command-line.  Thus, this is not the place to code dependencies
                between options; generally, 'initialize_options()' implementations
                are just a bunch of "self.foo = None" assignments.

                This method must be implemented by all command classes.
        
        """
    def finalize_options(self):
        """
        Set final values for all the options that this command supports.
                This is always called as late as possible, ie.  after any option
                assignments from the command-line or from other commands have been
                done.  Thus, this is the place to code option dependencies: if
                'foo' depends on 'bar', then it is safe to set 'foo' from 'bar' as
                long as 'foo' still has the same value it was assigned in
                'initialize_options()'.

                This method must be implemented by all command classes.
        
        """
    def dump_options(self, header=None, indent=""):
        """
        command options for '%s':
        """
    def run(self):
        """
        A command's raison d'etre: carry out the action it exists to
                perform, controlled by the options initialized in
                'initialize_options()', customized by other commands, the setup
                script, the command-line, and config files, and finalized in
                'finalize_options()'.  All terminal output and filesystem
                interaction should be done by 'run()'.

                This method must be implemented by all command classes.
        
        """
    def announce(self, msg, level=1):
        """
        If the current verbosity level is of greater than or equal to
                'level' print 'msg' to stdout.
        
        """
    def debug_print(self, msg):
        """
        Print 'msg' to stdout if the global DEBUG (taken from the
                DISTUTILS_DEBUG environment variable) flag is true.
        
        """
    def _ensure_stringlike(self, option, what, default=None):
        """
        '%s' must be a %s (got `%s`)

        """
    def ensure_string(self, option, default=None):
        """
        Ensure that 'option' is a string; if not defined, set it to
                'default'.
        
        """
    def ensure_string_list(self, option):
        """
        r"""Ensure that 'option' is a list of strings.  If 'option' is
                currently a string, we split it either on /,\s*/ or /\s+/, so
                "foo bar baz", "foo,bar,baz", and "foo,   bar baz" all become
                ["foo", "bar", "baz"].
        
        """
2021-03-02 20:46:27,958 : INFO : tokenize_signature : --> do i ever get here?
    def _ensure_tested_string(self, option, tester, what, error_fmt,
                              default=None):
        """
        error in '%s' option: 
        """
    def ensure_filename(self, option):
        """
        Ensure that 'option' is the name of an existing file.
        """
    def ensure_dirname(self, option):
        """
        directory name
        """
    def get_command_name(self):
        """
        'command_name'
        """
    def set_undefined_options(self, src_cmd, *option_pairs):
        """
        Set the values of any "undefined" options from corresponding
                option values in some other command object.  "Undefined" here means
                "is None", which is the convention used to indicate that an option
                has not been changed between 'initialize_options()' and
                'finalize_options()'.  Usually called from 'finalize_options()' for
                options that depend on some other command rather than another
                option of the same command.  'src_cmd' is the other command from
                which option values will be taken (a command object will be created
                for it if necessary); the remaining arguments are
                '(src_option,dst_option)' tuples which mean "take the value of
                'src_option' in the 'src_cmd' command object, and copy it to
                'dst_option' in the current command object".
        
        """
    def get_finalized_command(self, command, create=1):
        """
        Wrapper around Distribution's 'get_command_obj()' method: find
                (create if necessary and 'create' is true) the command object for
                'command', call its 'ensure_finalized()' method, and return the
                finalized command object.
        
        """
    def reinitialize_command(self, command, reinit_subcommands=0):
        """
        Run some other command: uses the 'run_command()' method of
                Distribution, which creates and finalizes the command object if
                necessary and then invokes its 'run()' method.
        
        """
    def get_sub_commands(self):
        """
        Determine the sub-commands that are relevant in the current
                distribution (ie., that need to be run).  This is based on the
                'sub_commands' class attribute: each tuple in that list may include
                a method that we call to determine if the subcommand needs to be
                run for the current distribution.  Return a list of command names.
        
        """
    def warn(self, msg):
        """
        warning: %s: %s\n
        """
    def execute(self, func, args, msg=None, level=1):
        """
        Copy a file respecting verbose, dry-run and force flags.  (The
                former two default to whatever is in the Distribution object, and
                the latter defaults to false for commands that don't define it.)
        """
2021-03-02 20:46:27,961 : INFO : tokenize_signature : --> do i ever get here?
    def copy_tree(self, infile, outfile, preserve_mode=1, preserve_times=1,
                   preserve_symlinks=0, level=1):
        """
        Copy an entire directory tree respecting verbose, dry-run,
                and force flags.
        
        """
    def move_file (self, src, dst, level=1):
        """
        Move a file respecting dry-run flag.
        """
    def spawn(self, cmd, search_path=1, level=1):
        """
        Spawn an external command respecting dry-run flag.
        """
2021-03-02 20:46:27,962 : INFO : tokenize_signature : --> do i ever get here?
    def make_archive(self, base_name, format, root_dir=None, base_dir=None,
                     owner=None, group=None):
        """
        Special case of 'execute()' for operations that process one or
                more input files and generate one output file.  Works just like
                'execute()', except the operation is skipped and a different
                message printed if 'outfile' already exists and is newer than all
                files listed in 'infiles'.  If the command defined 'self.force',
                and it is true, then the command is unconditionally run -- does no
                timestamp checks.
        
        """
