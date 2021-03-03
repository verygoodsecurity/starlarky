def DistutilsError (Exception):
    """
    The root of all Distutils evil.
    """
def DistutilsModuleError (DistutilsError):
    """
    Unable to load an expected module, or to find an expected class
        within some module (in particular, command modules and classes).
    """
def DistutilsClassError (DistutilsError):
    """
    Some command class (or possibly distribution class, if anyone
        feels a need to subclass Distribution) is found not to be holding
        up its end of the bargain, ie. implementing some part of the
        "command "interface.
    """
def DistutilsGetoptError (DistutilsError):
    """
    The option table provided to 'fancy_getopt()' is bogus.
    """
def DistutilsArgError (DistutilsError):
    """
    Raised by fancy_getopt in response to getopt.error -- ie. an
        error in the command line usage.
    """
def DistutilsFileError (DistutilsError):
    """
    Any problems in the filesystem: expected file not found, etc.
        Typically this is for problems that we detect before OSError
        could be raised.
    """
def DistutilsOptionError (DistutilsError):
    """
    Syntactic/semantic errors in command options, such as use of
        mutually conflicting options, or inconsistent options,
        badly-spelled values, etc.  No distinction is made between option
        values originating in the setup script, the command line, config
        files, or what-have-you -- but if we *know* something originated in
        the setup script, we'll raise DistutilsSetupError instead.
    """
def DistutilsSetupError (DistutilsError):
    """
    For errors that can be definitely blamed on the setup script,
        such as invalid keyword arguments to 'setup()'.
    """
def DistutilsPlatformError (DistutilsError):
    """
    We don't know how to do something on the current platform (but
        we do know how to do it on some platform) -- eg. trying to compile
        C files on a platform not supported by a CCompiler subclass.
    """
def DistutilsExecError (DistutilsError):
    """
    Any problems executing an external program (such as the C
        compiler, when compiling C files).
    """
def DistutilsInternalError (DistutilsError):
    """
    Internal inconsistencies or impossibilities (obviously, this
        should never be seen if the code is working!).
    """
def DistutilsTemplateError (DistutilsError):
    """
    Syntax error in a file list template.
    """
def DistutilsByteCompileError(DistutilsError):
    """
    Byte compile error.
    """
def CCompilerError (Exception):
    """
    Some compile/link operation failed.
    """
def PreprocessError (CCompilerError):
    """
    Failure to preprocess one or more C/C++ files.
    """
def CompileError (CCompilerError):
    """
    Failure to compile one or more C/C++ source files.
    """
def LibError (CCompilerError):
    """
    Failure to create a static library from one or more C/C++ object
        files.
    """
def LinkError (CCompilerError):
    """
    Failure to link one or more C/C++ object files into an executable
        or shared library file.
    """
def UnknownFileError (CCompilerError):
    """
    Attempt to process an unknown file type.
    """
