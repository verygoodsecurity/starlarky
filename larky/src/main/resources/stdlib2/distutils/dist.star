def _ensure_list(value, fieldname):
    """
     a string containing comma separated values is okay.  It will
     be converted to a list by Distribution.finalize_options().

    """
def Distribution:
    """
    The core of the Distutils.  Most of the work hiding behind 'setup'
        is really done within a Distribution instance, which farms the work out
        to the Distutils commands specified on the command line.

        Setup scripts will almost never instantiate Distribution directly,
        unless the 'setup()' function is totally inadequate to their needs.
        However, it is conceivable that a setup script might wish to subclass
        Distribution for some specialized purpose, and then pass the subclass
        to 'setup()' as the 'distclass' keyword argument.  If so, it is
        necessary to respect the expectations that 'setup' has of Distribution.
        See the code for 'setup()', in core.py, for details.
    
    """
    def __init__(self, attrs=None):
        """
        Construct a new Distribution instance: initialize all the
                attributes of a Distribution, and then use 'attrs' (a dictionary
                mapping attribute names to values) to assign some of those
                attributes their "real" values.  (Any attributes not mentioned in
                'attrs' will be assigned to some null value: 0, None, an empty list
                or dictionary, etc.)  Most importantly, initialize the
                'command_obj' attribute to the empty dictionary; this will be
                filled in with real command objects by 'parse_command_line()'.
        
        """
    def get_option_dict(self, command):
        """
        Get the option dictionary for a given command.  If that
                command's option dictionary hasn't been created yet, then create it
                and return the new dictionary; otherwise, return the existing
                option dictionary.
        
        """
    def dump_option_dicts(self, header=None, commands=None, indent=""):
        """
         dump all command option dicts
        """
    def find_config_files(self):
        """
        Find as many configuration files as should be processed for this
                platform, and return a list of filenames in the order in which they
                should be parsed.  The filenames returned are guaranteed to exist
                (modulo nasty race conditions).

                There are three possible config files: distutils.cfg in the
                Distutils installation directory (ie. where the top-level
                Distutils __inst__.py file lives), a file in the user's home
                directory named .pydistutils.cfg on Unix and pydistutils.cfg
                on Windows/Mac; and setup.cfg in the current directory.

                The file in the user's home directory can be disabled with the
                --no-user-cfg option.
        
        """
    def parse_config_files(self, filenames=None):
        """
         Ignore install directory options if we have a venv

        """
    def parse_command_line(self):
        """
        Parse the setup script's command line, taken from the
                'script_args' instance attribute (which defaults to 'sys.argv[1:]'
                -- see 'setup()' in core.py).  This list is first processed for
                "global options" -- options that set attributes of the Distribution
                instance.  Then, it is alternately scanned for Distutils commands
                and options for that command.  Each new command terminates the
                options for the previous command.  The allowed options for a
                command are determined by the 'user_options' attribute of the
                command class -- thus, we have to be able to load command classes
                in order to parse the command line.  Any error in that 'options'
                attribute raises DistutilsGetoptError; any error on the
                command-line raises DistutilsArgError.  If no Distutils commands
                were found on the command line, raises DistutilsArgError.  Return
                true if command-line was successfully parsed and we should carry
                on with executing commands; false if no errors but we shouldn't
                execute commands (currently, this only happens if user asks for
                help).
        
        """
    def _get_toplevel_options(self):
        """
        Return the non-display options recognized at the top level.

                This includes options that are recognized *only* at the top
                level as well as options recognized for commands.
        
        """
    def _parse_command_opts(self, parser, args):
        """
        Parse the command-line options for a single command.
                'parser' must be a FancyGetopt instance; 'args' must be the list
                of arguments, starting with the current command (whose options
                we are about to parse).  Returns a new version of 'args' with
                the next command at the front of the list; will be the empty
                list if there are no more commands on the command line.  Returns
                None if the user asked for help on this command.
        
        """
    def finalize_options(self):
        """
        Set final values for all the options on the Distribution
                instance, analogous to the .finalize_options() method of Command
                objects.
        
        """
2021-03-02 20:46:35,633 : INFO : tokenize_signature : --> do i ever get here?
    def _show_help(self, parser, global_options=1, display_options=1,
                   commands=[]):
        """
        Show help for the setup script command-line in the form of
                several lists of command-line options.  'parser' should be a
                FancyGetopt instance; do not expect it to be returned in the
                same state, as its option table will be reset to make it
                generate the correct help text.

                If 'global_options' is true, lists the global options:
                --verbose, --dry-run, etc.  If 'display_options' is true, lists
                the "display-only" options: --name, --version, etc.  Finally,
                lists per-command help for every command name or command class
                in 'commands'.
        
        """
    def handle_display_options(self, option_order):
        """
        If there were any non-global "display-only" options
                (--help-commands or the metadata display options) on the command
                line, display the requested info and return true; else return
                false.
        
        """
    def print_command_list(self, commands, header, max_length):
        """
        Print a subset of the list of all commands -- used by
                'print_commands()'.
        
        """
    def print_commands(self):
        """
        Print out a help message listing all available commands with a
                description of each.  The list is divided into "standard commands"
                (listed in distutils.command.__all__) and "extra commands"
                (mentioned in self.cmdclass, but not a standard command).  The
                descriptions come from the command class attribute
                'description'.
        
        """
    def get_command_list(self):
        """
        Get a list of (command, description) tuples.
                The list is divided into "standard commands" (listed in
                distutils.command.__all__) and "extra commands" (mentioned in
                self.cmdclass, but not a standard command).  The descriptions come
                from the command class attribute 'description'.
        
        """
    def get_command_packages(self):
        """
        Return a list of packages from which commands are loaded.
        """
    def get_command_class(self, command):
        """
        Return the class that implements the Distutils command named by
                'command'.  First we check the 'cmdclass' dictionary; if the
                command is mentioned there, we fetch the class object from the
                dictionary and return it.  Otherwise we load the command module
                ("distutils.command." + command) and fetch the command class from
                the module.  The loaded class is also stored in 'cmdclass'
                to speed future calls to 'get_command_class()'.

                Raises DistutilsModuleError if the expected module could not be
                found, or if that module does not define the expected class.
        
        """
    def get_command_obj(self, command, create=1):
        """
        Return the command object for 'command'.  Normally this object
                is cached on a previous call to 'get_command_obj()'; if no command
                object for 'command' is in the cache, then we either create and
                return it (if 'create' is true) or return None.
        
        """
    def _set_command_options(self, command_obj, option_dict=None):
        """
        Set the options for 'command_obj' from 'option_dict'.  Basically
                this means copying elements of a dictionary ('option_dict') to
                attributes of an instance ('command').

                'command_obj' must be a Command instance.  If 'option_dict' is not
                supplied, uses the standard option dictionary for this command
                (from 'self.command_options').
        
        """
    def reinitialize_command(self, command, reinit_subcommands=0):
        """
        Reinitializes a command to the state it was in when first
                returned by 'get_command_obj()': ie., initialized but not yet
                finalized.  This provides the opportunity to sneak option
                values in programmatically, overriding or supplementing
                user-supplied values from the config files and command line.
                You'll have to re-finalize the command object (by calling
                'finalize_options()' or 'ensure_finalized()') before using it for
                real.

                'command' should be a command name (string) or command object.  If
                'reinit_subcommands' is true, also reinitializes the command's
                sub-commands, as declared by the 'sub_commands' class attribute (if
                it has one).  See the "install" command for an example.  Only
                reinitializes the sub-commands that actually matter, ie. those
                whose test predicates return true.

                Returns the reinitialized command object.
        
        """
    def announce(self, msg, level=log.INFO):
        """
        Run each command that was seen on the setup script command line.
                Uses the list of commands found and cache of command objects
                created by 'get_command_obj()'.
        
        """
    def run_command(self, command):
        """
        Do whatever it takes to run a command (including nothing at all,
                if the command has already been run).  Specifically: if we have
                already created and run the command named by 'command', return
                silently without doing anything.  If the command named by 'command'
                doesn't even have a command object yet, create one.  Then invoke
                'run()' on that command object (or an existing one).
        
        """
    def has_pure_modules(self):
        """
         -- Metadata query methods ----------------------------------------

         If you're looking for 'get_name()', 'get_version()', and so forth,
         they are defined in a sneaky way: the constructor binds self.get_XXX
         to self.metadata.get_XXX.  The actual code is in the
         DistributionMetadata class, below.


        """
def DistributionMetadata:
    """
    Dummy class to hold the distribution meta-data: name, version,
        author, and so forth.
    
    """
    def __init__(self, path=None):
        """
         PEP 314

        """
    def read_pkg_file(self, file):
        """
        Reads the metadata values from a file object.
        """
        def _read_field(name):
            """
            'UNKNOWN'
            """
        def _read_list(name):
            """
            'metadata-version'
            """
    def write_pkg_info(self, base_dir):
        """
        Write the PKG-INFO file into the release tree.
        
        """
    def write_pkg_file(self, file):
        """
        Write the PKG-INFO format data to a file object.
        
        """
    def _write_list(self, file, name, values):
        """
        '%s: %s\n'
        """
    def get_name(self):
        """
        UNKNOWN
        """
    def get_version(self):
        """
        0.0.0
        """
    def get_fullname(self):
        """
        %s-%s
        """
    def get_author(self):
        """
        UNKNOWN
        """
    def get_author_email(self):
        """
        UNKNOWN
        """
    def get_maintainer(self):
        """
        UNKNOWN
        """
    def get_maintainer_email(self):
        """
        UNKNOWN
        """
    def get_contact(self):
        """
        UNKNOWN
        """
    def get_contact_email(self):
        """
        UNKNOWN
        """
    def get_url(self):
        """
        UNKNOWN
        """
    def get_license(self):
        """
        UNKNOWN
        """
    def get_description(self):
        """
        UNKNOWN
        """
    def get_long_description(self):
        """
        UNKNOWN
        """
    def get_keywords(self):
        """
        'keywords'
        """
    def get_platforms(self):
        """
        UNKNOWN
        """
    def set_platforms(self, value):
        """
        'platforms'
        """
    def get_classifiers(self):
        """
        'classifiers'
        """
    def get_download_url(self):
        """
        UNKNOWN
        """
    def get_requires(self):
        """
        Convert a 4-tuple 'help_options' list as found in various command
            classes to the 3-tuple form required by FancyGetopt.
    
        """
