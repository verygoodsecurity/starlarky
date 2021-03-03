def _run_pip(args, additional_paths=None):
    """
     Add our bundled software to the sys.path so we can import it

    """
def version():
    """

        Returns a string specifying the bundled version of pip.
    
    """
def _disable_pip_configuration_settings():
    """
     We deliberately ignore all pip environment variables
     when invoking pip
     See http://bugs.python.org/issue19734 for details

    """
2021-03-02 20:53:52,658 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:52,658 : INFO : tokenize_signature : --> do i ever get here?
def bootstrap(*, root=None, upgrade=False, user=False,
              altinstall=False, default_pip=False,
              verbosity=0):
    """

        Bootstrap pip into the current Python installation (or the given root
        directory).

        Note that calling this function will alter both sys.path and os.environ.
    
    """
2021-03-02 20:53:52,658 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:52,658 : INFO : tokenize_signature : --> do i ever get here?
def _bootstrap(*, root=None, upgrade=False, user=False,
              altinstall=False, default_pip=False,
              verbosity=0):
    """

        Bootstrap pip into the current Python installation (or the given root
        directory). Returns pip command status code.

        Note that calling this function will alter both sys.path and os.environ.
    
    """
def _uninstall_helper(*, verbosity=0):
    """
    Helper to support a clean default uninstall process on Windows

        Note that calling this function may alter os.environ.
    
    """
def _main(argv=None):
    """
    python -m ensurepip
    """
