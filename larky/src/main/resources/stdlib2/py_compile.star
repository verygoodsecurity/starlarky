def PyCompileError(Exception):
    """
    Exception raised when an error occurs while attempting to
        compile the file.

        To raise this exception, use

            raise PyCompileError(exc_type,exc_value,file[,msg])

        where

            exc_type:   exception type to be used in error message
                        type name can be accesses as class variable
                        'exc_type_name'

            exc_value:  exception value to be used in error message
                        can be accesses as class variable 'exc_value'

            file:       name of file being compiled to be used in error message
                        can be accesses as class variable 'file'

            msg:        string message to be written as error message
                        If no value is given, a default exception message will be
                        given, consistent with 'standard' py_compile output.
                        message (or default) can be accesses as class variable
                        'msg'

    
    """
    def __init__(self, exc_type, exc_value, file, msg=''):
        """
        ''
        """
    def __str__(self):
        """
        'SOURCE_DATE_EPOCH'
        """
2021-03-02 20:53:52,479 : INFO : tokenize_signature : --> do i ever get here?
def compile(file, cfile=None, dfile=None, doraise=False, optimize=-1,
            invalidation_mode=None, quiet=0):
    """
    Byte-compile one Python source file to Python bytecode.

        :param file: The source file name.
        :param cfile: The target byte compiled file name.  When not given, this
            defaults to the PEP 3147/PEP 488 location.
        :param dfile: Purported file name, i.e. the file name that shows up in
            error messages.  Defaults to the source file name.
        :param doraise: Flag indicating whether or not an exception should be
            raised when a compile error is found.  If an exception occurs and this
            flag is set to False, a string indicating the nature of the exception
            will be printed, and the function will return to the caller. If an
            exception occurs and this flag is set to True, a PyCompileError
            exception will be raised.
        :param optimize: The optimization level for the compiler.  Valid values
            are -1, 0, 1 and 2.  A value of -1 means to use the optimization
            level of the current interpreter, as given by -O command line options.
        :param invalidation_mode:
        :param quiet: Return full output with False or 0, errors only with 1,
            and no output with 2.

        :return: Path to the resulting byte compiled file.

        Note that it isn't necessary to byte-compile Python modules for
        execution efficiency -- Python itself byte-compiles a module when
        it is loaded, and if it can, writes out the bytecode to the
        corresponding .pyc file.

        However, if a Python installation is shared between users, it is a
        good idea to byte-compile all modules upon installation, since
        other users may not be able to write in the source directories,
        and thus they won't be able to write the .pyc file, and then
        they would be byte-compiling every module each time it is loaded.
        This can slow down program start-up considerably.

        See compileall.py for a script/module that uses this module to
        byte-compile all installed files (or all files in selected
        directories).

        Do note that FileExistsError is raised if cfile ends up pointing at a
        non-regular file or symlink. Because the compilation uses a file renaming,
        the resulting file would be regular and thus not the same type of file as
        it was previously.
    
    """
def main(args=None):
    """
    Compile several source files.

        The files named in 'args' (or on the command line, if 'args' is
        not specified) are compiled and the resulting bytecode is cached
        in the normal manner.  This function does not search a directory
        structure to locate source files; it only compiles files named
        explicitly.  If '-' is the only parameter in args, the list of
        files is taken from standard input.

    
    """
