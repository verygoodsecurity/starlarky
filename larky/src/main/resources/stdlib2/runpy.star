def _TempModule(object):
    """
    Temporarily replace a module in sys.modules with an empty namespace
    """
    def __init__(self, mod_name):
        """
        Already preserving saved value
        """
    def __exit__(self, *args):
        """
         TODO: Replace these helpers with importlib._bootstrap_external functions.

        """
2021-03-02 20:54:43,158 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:43,158 : INFO : tokenize_signature : --> do i ever get here?
def _run_code(code, run_globals, init_globals=None,
              mod_name=None, mod_spec=None,
              pkg_name=None, script_name=None):
    """
    Helper to run code in nominated namespace
    """
2021-03-02 20:54:43,159 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:43,159 : INFO : tokenize_signature : --> do i ever get here?
def _run_module_code(code, init_globals=None,
                    mod_name=None, mod_spec=None,
                    pkg_name=None, script_name=None):
    """
    Helper to run code in new namespace with sys modified
    """
def _get_module_details(mod_name, error=ImportError):
    """
    .
    """
def _Error(Exception):
    """
    Error that _run_module_as_main() should report without a traceback
    """
def _run_module_as_main(mod_name, alter_argv=True):
    """
    Runs the designated module in the __main__ namespace

           Note that the executed module will have full access to the
           __main__ namespace. If this is not desirable, the run_module()
           function should be used to run the module code in a fresh namespace.

           At the very least, these variables in __main__ will be overwritten:
               __name__
               __file__
               __cached__
               __loader__
               __package__
    
    """
2021-03-02 20:54:43,162 : INFO : tokenize_signature : --> do i ever get here?
def run_module(mod_name, init_globals=None,
               run_name=None, alter_sys=False):
    """
    Execute a module's code without importing it

           Returns the resulting top level namespace dictionary
    
    """
def _get_main_module_details(error=ImportError):
    """
     Helper that gives a nicer error message when attempting to
     execute a zipfile or directory by invoking __main__.py
     Also moves the standard __main__ out of the way so that the
     preexisting __loader__ entry doesn't cause issues

    """
def _get_code_from_file(run_name, fname):
    """
     Check for a compiled file first

    """
def run_path(path_name, init_globals=None, run_name=None):
    """
    Execute code located at the specified filesystem location

           Returns the resulting top level namespace dictionary

           The file path may refer directly to a Python script (i.e.
           one that could be directly executed with execfile) or else
           it may refer to a zipfile or directory containing a top
           level __main__.py script.
    
    """
