def _Ignore:
    """
    '<string>'
    """
    def names(self, filename, modulename):
        """
         haven't seen this one before, so see if the module name is
         on the ignore list.

        """
def _modname(path):
    """
    Return a plausible module name for the patch.
    """
def _fullmodname(path):
    """
    Return a plausible module name for the path.
    """
def CoverageResults:
    """
     map (filename, lineno) to count
    """
    def is_ignored_filename(self, filename):
        """
        Return True if the filename does not refer to a file
                we want to have reported.
        
        """
    def update(self, other):
        """
        Merge in the data from another CoverageResults
        """
    def write_results(self, show_missing=True, summary=False, coverdir=None):
        """

                Write the coverage results.

                :param show_missing: Show lines that had no hits.
                :param summary: Include coverage summary per module.
                :param coverdir: If None, the results of each module are placed in its
                                 directory, otherwise it is included in the directory
                                 specified.
        
        """
    def write_results_file(self, path, lines, lnotab, lines_hit, encoding=None):
        """
        Return a coverage results file in path.
        """
def _find_lines_from_code(code, strs):
    """
    Return dict where keys are lines in the line number table.
    """
def _find_lines(code, strs):
    """
    Return lineno dict for all code objects reachable from code.
    """
def _find_strings(filename, encoding=None):
    """
    Return a dict of possible docstring positions.

        The dict maps line numbers to strings.  There is an entry for
        line that contains only a string or a part of a triple-quoted
        string.
    
    """
def _find_executable_linenos(filename):
    """
    Return dict where keys are line numbers in the line number table.
    """
def Trace:
    """

            @param count true iff it should count number of times each
                         line is executed
            @param trace true iff it should print out each line that is
                         being counted
            @param countfuncs true iff it should just output a list of
                         (filename, modulename, funcname,) for functions
                         that were called at least once;  This overrides
                         `count' and `trace'
            @param ignoremods a list of the names of modules to ignore
            @param ignoredirs a list of the names of directories to ignore
                         all of the (recursive) contents of
            @param infile file from which to read stored counts to be
                         added into the results
            @param outfile file in which to write the results
            @param timing true iff timing information be displayed
        
    """
    def run(self, cmd):
        """
        descriptor 'runfunc' of 'Trace' object 
        needs an argument
        """
    def file_module_function_of(self, frame):
        """
         use of gc.get_referrers() was suggested by Michael Hudson
         all functions which refer to this code object

        """
    def globaltrace_trackcallers(self, frame, why, arg):
        """
        Handler for call events.

                Adds information about who called who to the self._callers dict.
        
        """
    def globaltrace_countfuncs(self, frame, why, arg):
        """
        Handler for call events.

                Adds (filename, modulename, funcname) to the self._calledfuncs dict.
        
        """
    def globaltrace_lt(self, frame, why, arg):
        """
        Handler for call events.

                If the code block being entered is to be ignored, returns `None',
                else returns self.localtrace.
        
        """
    def localtrace_trace_and_count(self, frame, why, arg):
        """
        line
        """
    def localtrace_trace(self, frame, why, arg):
        """
        line
        """
    def localtrace_count(self, frame, why, arg):
        """
        line
        """
    def results(self):
        """
        '--version'
        """
    def parse_ignore_dir(s):
        """
        '$prefix'
        """
