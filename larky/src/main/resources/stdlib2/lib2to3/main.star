def diff_texts(a, b, filename):
    """
    Return a unified diff of two strings.
    """
def StdoutRefactoringTool(refactor.MultiprocessRefactoringTool):
    """

        A refactoring tool that can avoid overwriting its input files.
        Prints output to stdout.

        Output files can optionally be written to a different directory and or
        have an extra file suffix appended to their name for use in situations
        where you do not want to replace the input files.
    
    """
2021-03-02 20:54:13,468 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, fixers, options, explicit, nobackups, show_diffs,
                 input_base_dir='', output_dir='', append_suffix=''):
        """

                Args:
                    fixers: A list of fixers to import.
                    options: A dict with RefactoringTool configuration.
                    explicit: A list of fixers to run even if they are explicit.
                    nobackups: If true no backup '.bak' files will be created for those
                        files that are being refactored.
                    show_diffs: Should diffs of the refactoring be printed to stdout?
                    input_base_dir: The base directory for all input files.  This class
                        will strip this path prefix off of filenames before substituting
                        it with output_dir.  Only meaningful if output_dir is supplied.
                        All files processed by refactor() must start with this path.
                    output_dir: If supplied, all converted files will be written into
                        this directory tree instead of input_base_dir.
                    append_suffix: If supplied, all files output by this tool will have
                        this appended to their filename.  Useful for changing .py to
                        .py3 for example by passing append_suffix='3'.
        
        """
    def log_error(self, msg, *args, **kwargs):
        """
        'filename %s does not start with the '
        'input_base_dir %s'
        """
    def print_output(self, old, new, filename, equal):
        """
        No changes to %s
        """
def warn(msg):
    """
    WARNING: %s
    """
def main(fixer_pkg, args=None):
    """
    Main program.

        Args:
            fixer_pkg: the name of a package where the fixers are located.
            args: optional; a list of command line arguments. If omitted,
                  sys.argv[1:] is used.

        Returns a suggested exit status (0, 1, 2).
    
    """
