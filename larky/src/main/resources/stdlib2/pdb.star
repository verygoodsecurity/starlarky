def Restart(Exception):
    """
    Causes a debugger to be restarted for the debugged python program.
    """
def find_function(funcname, filename):
    """
    r'def\s+%s\s*[(]'
    """
def getsourcelines(obj):
    """
     must be a module frame: do not try to cut a block out of it

    """
def lasti2lineno(code, lasti):
    """
    String that doesn't quote its repr.
    """
    def __repr__(self):
        """
         Interaction prompt line will separate file and call info from code
         text using value of line_prefix string.  A newline and arrow may
         be to your liking.  You can set it once pdb is imported using the
         command "pdb.line_prefix = '\n% '".
         line_prefix = ': '    # Use this to get the old situation back

        """
def Pdb(bdb.Bdb, cmd.Cmd):
    """
    'tab'
    """
    def sigint_handler(self, signum, frame):
        """
        \nProgram interrupted. (Use 'cont' to resume).
        """
    def reset(self):
        """
         when setting up post-mortem debugging with a traceback, save all
         the original line numbers to be displayed along the current line
         numbers (which can be different, e.g. due to finally clauses)

        """
    def execRcLines(self):
        """
         local copy because of recursion

        """
    def user_call(self, frame, argument_list):
        """
        This method is called when there is the remote possibility
                that we ever need to stop in this function.
        """
    def user_line(self, frame):
        """
        This function is called when we stop or break at this line.
        """
    def bp_commands(self, frame):
        """
        Call every command that was set for the current active breakpoint
                (if there is one).

                Returns True if the normal interaction function must be called,
                False otherwise.
        """
    def user_return(self, frame, return_value):
        """
        This function is called when a return trap is set here.
        """
    def user_exception(self, frame, exc_info):
        """
        This function is called if an exception occurs,
                but only if we are to stop at or just below this level.
        """
    def _cmdloop(self):
        """
         keyboard interrupts allow for an easy way to cancel
         the current command, so allow them during interactive input

        """
    def preloop(self):
        """
         check for identity first; this prevents custom __eq__ to
         be called at every loop, and also prevents instances whose
         fields are changed to be displayed

        """
    def interaction(self, frame, traceback):
        """
         Restore the previous signal handler at the Pdb prompt.

        """
    def displayhook(self, obj):
        """
        Custom displayhook for the exec in default(), which prevents
                assignment of the _ variable in the builtins.
        
        """
    def default(self, line):
        """
        '!'
        """
    def precmd(self, line):
        """
        Handle alias expansion and ';;' separator.
        """
    def onecmd(self, line):
        """
        Interpret the argument as though it had been typed in response
                to the prompt.

                Checks whether this line is typed at the normal prompt or in
                a breakpoint command list definition.
        
        """
    def handle_command_def(self, line):
        """
        Handles one command line during command list definition.
        """
    def message(self, msg):
        """
        '***'
        """
    def _complete_location(self, text, line, begidx, endidx):
        """
         Complete a file/module/function location for break/tbreak/clear.

        """
    def _complete_bpnumber(self, text, line, begidx, endidx):
        """
         Complete a breakpoint number.  (This would be more helpful if we could
         display additional info along with the completions, such as file/line
         of the breakpoint.)

        """
    def _complete_expression(self, text, line, begidx, endidx):
        """
         Complete an arbitrary expression.

        """
    def do_commands(self, arg):
        """
        commands [bpnumber]
                (com) ...
                (com) end
                (Pdb)

                Specify a list of commands for breakpoint number bpnumber.
                The commands themselves are entered on the following lines.
                Type a line containing just 'end' to terminate the commands.
                The commands are executed when the breakpoint is hit.

                To remove all commands from a breakpoint, type commands and
                follow it immediately with end; that is, give no commands.

                With no bpnumber argument, commands refers to the last
                breakpoint set.

                You can use breakpoint commands to start your program up
                again.  Simply use the continue command, or step, or any other
                command that resumes execution.

                Specifying any command resuming execution (currently continue,
                step, next, return, jump, quit and their abbreviations)
                terminates the command list (as if that command was
                immediately followed by end).  This is because any time you
                resume execution (even with a simple next or step), you may
                encounter another breakpoint -- which could have its own
                command list, leading to ambiguities about which list to
                execute.

                If you use the 'silent' command in the command list, the usual
                message about stopping at a breakpoint is not printed.  This
                may be desirable for breakpoints that are to print a specific
                message and then continue.  If none of the other commands
                print anything, you will see no sign that the breakpoint was
                reached.
        
        """
    def do_break(self, arg, temporary = 0):
        """
        b(reak) [ ([filename:]lineno | function) [, condition] ]
                Without argument, list all breaks.

                With a line number argument, set a break at this line in the
                current file.  With a function name, set a break at the first
                executable line of that function.  If a second argument is
                present, it is a string specifying an expression which must
                evaluate to true before the breakpoint is honored.

                The line number may be prefixed with a filename and a colon,
                to specify a breakpoint in another file (probably one that
                hasn't been loaded yet).  The file is searched for on
                sys.path; the .py suffix may be omitted.
        
        """
    def defaultFile(self):
        """
        Produce a reasonable default.
        """
    def do_tbreak(self, arg):
        """
        tbreak [ ([filename:]lineno | function) [, condition] ]
                Same arguments as break, but sets a temporary breakpoint: it
                is automatically deleted when first hit.
        
        """
    def lineinfo(self, identifier):
        """
         Input is identifier, may be in single quotes

        """
    def checkline(self, filename, lineno):
        """
        Check whether specified line seems to be executable.

                Return `lineno` if it is, 0 if not (e.g. a docstring, comment, blank
                line or EOF). Warning: testing is not comprehensive.
        
        """
    def do_enable(self, arg):
        """
        enable bpnumber [bpnumber ...]
                Enables the breakpoints given as a space separated list of
                breakpoint numbers.
        
        """
    def do_disable(self, arg):
        """
        disable bpnumber [bpnumber ...]
                Disables the breakpoints given as a space separated list of
                breakpoint numbers.  Disabling a breakpoint means it cannot
                cause the program to stop execution, but unlike clearing a
                breakpoint, it remains in the list of breakpoints and can be
                (re-)enabled.
        
        """
    def do_condition(self, arg):
        """
        condition bpnumber [condition]
                Set a new condition for the breakpoint, an expression which
                must evaluate to true before the breakpoint is honored.  If
                condition is absent, any existing condition is removed; i.e.,
                the breakpoint is made unconditional.
        
        """
    def do_ignore(self, arg):
        """
        ignore bpnumber [count]
                Set the ignore count for the given breakpoint number.  If
                count is omitted, the ignore count is set to 0.  A breakpoint
                becomes active when the ignore count is zero.  When non-zero,
                the count is decremented each time the breakpoint is reached
                and the breakpoint is not disabled and any associated
                condition evaluates to true.
        
        """
    def do_clear(self, arg):
        """
        cl(ear) filename:lineno\ncl(ear) [bpnumber [bpnumber...]]
                With a space separated list of breakpoint numbers, clear
                those breakpoints.  Without argument, clear all breaks (but
                first ask confirmation).  With a filename:lineno argument,
                clear all breaks at that line in that file.
        
        """
    def do_where(self, arg):
        """
        w(here)
                Print a stack trace, with the most recent frame at the bottom.
                An arrow indicates the "current frame", which determines the
                context of most commands.  'bt' is an alias for this command.
        
        """
    def _select_frame(self, number):
        """
        u(p) [count]
                Move the current frame count (default one) levels up in the
                stack trace (to an older frame).
        
        """
    def do_down(self, arg):
        """
        d(own) [count]
                Move the current frame count (default one) levels down in the
                stack trace (to a newer frame).
        
        """
    def do_until(self, arg):
        """
        unt(il) [lineno]
                Without argument, continue execution until the line with a
                number greater than the current one is reached.  With a line
                number, continue execution until a line with a number greater
                or equal to that is reached.  In both cases, also stop when
                the current frame returns.
        
        """
    def do_step(self, arg):
        """
        s(tep)
                Execute the current line, stop at the first possible occasion
                (either in a function that is called or in the current
                function).
        
        """
    def do_next(self, arg):
        """
        n(ext)
                Continue execution until the next line in the current function
                is reached or it returns.
        
        """
    def do_run(self, arg):
        """
        run [args...]
                Restart the debugged python program. If a string is supplied
                it is split with "shlex", and the result is used as the new
                sys.argv.  History, breakpoints, actions and debugger options
                are preserved.  "restart" is an alias for "run".
        
        """
    def do_return(self, arg):
        """
        r(eturn)
                Continue execution until the current function returns.
        
        """
    def do_continue(self, arg):
        """
        c(ont(inue))
                Continue execution, only stop when a breakpoint is encountered.
        
        """
    def do_jump(self, arg):
        """
        j(ump) lineno
                Set the next line that will be executed.  Only available in
                the bottom-most frame.  This lets you jump back and execute
                code again, or jump forward to skip code that you don't want
                to run.

                It should be noted that not all jumps are allowed -- for
                instance it is not possible to jump into the middle of a
                for loop or out of a finally clause.
        
        """
    def do_debug(self, arg):
        """
        debug code
                Enter a recursive debugger that steps through the code
                argument (which is an arbitrary expression or statement to be
                executed in the current environment).
        
        """
    def do_quit(self, arg):
        """
        q(uit)\nexit
                Quit from the debugger. The program being executed is aborted.
        
        """
    def do_EOF(self, arg):
        """
        EOF
                Handles the receipt of EOF as a command.
        
        """
    def do_args(self, arg):
        """
        a(rgs)
                Print the argument list of the current function.
        
        """
    def do_retval(self, arg):
        """
        retval
                Print the return value for the last return of a function.
        
        """
    def _getval(self, arg):
        """
        '** raised %s **'
        """
    def do_p(self, arg):
        """
        p expression
                Print the value of the expression.
        
        """
    def do_pp(self, arg):
        """
        pp expression
                Pretty-print the value of the expression.
        
        """
    def do_list(self, arg):
        """
        l(ist) [first [,last] | .]

                List source code for the current file.  Without arguments,
                list 11 lines around the current line or continue the previous
                listing.  With . as argument, list 11 lines around the current
                line.  With one argument, list 11 lines starting at that line.
                With two arguments, list the given range; if the second
                argument is less than the first, it is a count.

                The current line in the current frame is indicated by "->".
                If an exception is being debugged, the line where the
                exception was originally raised or propagated is indicated by
                ">>", if it differs from the current line.
        
        """
    def do_longlist(self, arg):
        """
        longlist | ll
                List the whole source code for the current function or frame.
        
        """
    def do_source(self, arg):
        """
        source expression
                Try to get source code for the given object and display it.
        
        """
    def _print_lines(self, lines, start, breaks=(), frame=None):
        """
        Print a range of lines.
        """
    def do_whatis(self, arg):
        """
        whatis arg
                Print the type of the argument.
        
        """
    def do_display(self, arg):
        """
        display [expression]

                Display the value of the expression if it changed, each time execution
                stops in the current frame.

                Without expression, list all display expressions for the current frame.
        
        """
    def do_undisplay(self, arg):
        """
        undisplay [expression]

                Do not display the expression any more in the current frame.

                Without expression, clear all display expressions for the current frame.
        
        """
    def complete_undisplay(self, text, line, begidx, endidx):
        """
        interact

                Start an interactive interpreter whose global namespace
                contains all the (global and local) names found in the current scope.
        
        """
    def do_alias(self, arg):
        """
        alias [name [command [parameter parameter ...] ]]
                Create an alias called 'name' that executes 'command'.  The
                command must *not* be enclosed in quotes.  Replaceable
                parameters can be indicated by %1, %2, and so on, while %* is
                replaced by all the parameters.  If no command is given, the
                current alias for name is shown. If no name is given, all
                aliases are listed.

                Aliases may be nested and can contain anything that can be
                legally typed at the pdb prompt.  Note!  You *can* override
                internal pdb commands with aliases!  Those internal commands
                are then hidden until the alias is removed.  Aliasing is
                recursively applied to the first word of the command line; all
                other words in the line are left alone.

                As an example, here are two useful aliases (especially when
                placed in the .pdbrc file):

                # Print instance variables (usage "pi classInst")
                alias pi for k in %1.__dict__.keys(): print("%1.",k,"=",%1.__dict__[k])
                # Print instance variables in self
                alias ps pi self
        
        """
    def do_unalias(self, arg):
        """
        unalias name
                Delete the specified alias.
        
        """
    def complete_unalias(self, text, line, begidx, endidx):
        """
         List of all the commands making the program resume execution.

        """
    def print_stack_trace(self):
        """
        '> '
        """
    def do_help(self, arg):
        """
        h(elp)
                Without argument, print the list of available commands.
                With a command name as argument, print help about that command.
                "help pdb" shows the full pdb documentation.
                "help exec" gives help on the ! command.
        
        """
    def help_exec(self):
        """
        (!) statement
                Execute the (one-line) statement in the context of the current
                stack frame.  The exclamation point can be omitted unless the
                first word of the statement resembles a debugger command.  To
                assign to a global variable you must always prefix the command
                with a 'global' command, e.g.:
                (Pdb) global list_options; list_options = ['-l']
                (Pdb)
        
        """
    def help_pdb(self):
        """
         other helper functions


        """
    def lookupmodule(self, filename):
        """
        Helper function for break/clear parsing -- may be overridden.

                lookupmodule() translates (possibly incomplete) file or module name
                into an absolute file name.
        
        """
    def _runmodule(self, module_name):
        """
        __name__
        """
    def _runscript(self, filename):
        """
         The script has to run in __main__ namespace (or imports from
         __main__ will break).

         So we clear up the __main__ and set several special variables
         (this gets rid of pdb's globals and cleans old variables on restarts).

        """
def run(statement, globals=None, locals=None):
    """
     B/W compatibility

    """
def runcall(*args, **kwds):
    """
     Post-Mortem interface


    """
def post_mortem(t=None):
    """
     handling the default

    """
def pm():
    """
     Main program for testing


    """
def test():
    """
     print help

    """
def help():
    """
    \
    usage: pdb.py [-c command] ... [-m module | pyfile] [arg] ...

    Debug the Python program given by pyfile. Alternatively,
    an executable module or package to debug can be specified using
    the -m switch.

    Initial commands are read from .pdbrc files in your home directory
    and in the current directory, if they exist.  Commands supplied with
    -c are executed after commands from .pdbrc files.

    To let the script run until an exception occurs, use "-c continue".
    To let the script run up to a given line X in the debugged file, use
    "-c 'until X'".
    """
def main():
    """
    'mhc:'
    """
