def SubprocessError(Exception): pass
    """
    Raised when run() is called with check=True and the process
        returns a non-zero exit status.

        Attributes:
          cmd, returncode, stdout, stderr, output
    
    """
    def __init__(self, returncode, cmd, output=None, stderr=None):
        """
        Command '%s' died with %r.
        """
    def stdout(self):
        """
        Alias for output attribute, to match stderr
        """
    def stdout(self, value):
        """
         There's no obvious reason to set this, but allow it anyway so
         .stdout is a transparent alias for .output

        """
def TimeoutExpired(SubprocessError):
    """
    This exception is raised when the timeout expires while waiting for a
        child process.

        Attributes:
            cmd, output, stdout, stderr, timeout
    
    """
    def __init__(self, cmd, timeout, output=None, stderr=None):
        """
        Command '%s' timed out after %s seconds
        """
    def stdout(self):
        """
         There's no obvious reason to set this, but allow it anyway so
         .stdout is a transparent alias for .output

        """
    def STARTUPINFO:
    """
    handle_list
    """
        def copy(self):
            """
            'handle_list'
            """
    def Handle(int):
    """
    already closed
    """
        def __repr__(self):
            """
            %s(%d)
            """
    def _cleanup():
        """
         This lists holds Popen instances for which the underlying process had not
         exited at the time its __del__ method got called: those processes are
         wait()ed for synchronously from _cleanup() when a new Popen object is
         created, to avoid zombie processes.

        """
    def _cleanup():
        """
         This can happen if two threads create a new Popen instance.
         It's harmless that it was already removed, so ignore.

        """
def _optim_args_from_interpreter_flags():
    """
    Return a list of command-line arguments reproducing the current
        optimization settings in sys.flags.
    """
def _args_from_interpreter_flags():
    """
    Return a list of command-line arguments reproducing the current
        settings in sys.flags, sys.warnoptions and sys._xoptions.
    """
def call(*popenargs, timeout=None, **kwargs):
    """
    Run command with arguments.  Wait for command to complete or
        timeout, then return the returncode attribute.

        The arguments are the same as for the Popen constructor.  Example:

        retcode = call(["ls", "-l"])
    
    """
def check_call(*popenargs, **kwargs):
    """
    Run command with arguments.  Wait for command to complete.  If
        the exit code was zero then return, otherwise raise
        CalledProcessError.  The CalledProcessError object will have the
        return code in the returncode attribute.

        The arguments are the same as for the call function.  Example:

        check_call(["ls", "-l"])
    
    """
def check_output(*popenargs, timeout=None, **kwargs):
    """
    r"""Run command with arguments and return its output.

        If the exit code was non-zero it raises a CalledProcessError.  The
        CalledProcessError object will have the return code in the returncode
        attribute and output in the output attribute.

        The arguments are the same as for the Popen constructor.  Example:

        >>> check_output(["ls", "-l", "/dev/null"])
        b'crw-rw-rw- 1 root root 1, 3 Oct 18  2007 /dev/null\n'

        The stdout argument is not allowed as it is used internally.
        To capture standard error in the result, use stderr=STDOUT.

        >>> check_output(["/bin/sh", "-c",
        ...               "ls -l non_existent_file ; exit 0"],
        ...              stderr=STDOUT)
        b'ls: non_existent_file: No such file or directory\n'

        There is an additional optional argument, "input", allowing you to
        pass a string to the subprocess's stdin.  If you use this argument
        you may not also use the Popen constructor's "stdin" argument, as
        it too will be used internally.  Example:

        >>> check_output(["sed", "-e", "s/foo/bar/"],
        ...              input=b"when in the course of fooman events\n")
        b'when in the course of barman events\n'

        By default, all communication is in bytes, and therefore any "input"
        should be bytes, and the return value will be bytes.  If in text mode,
        any "input" should be a string, and the return value will be a string
        decoded according to locale encoding, or by "encoding" if set. Text mode
        is triggered by setting any of text, encoding, errors or universal_newlines.
    
    """
def CompletedProcess(object):
    """
    A process that has finished running.

        This is returned by run().

        Attributes:
          args: The list or str args passed to run().
          returncode: The exit code of the process, negative for signals.
          stdout: The standard output (None if not captured).
          stderr: The standard error (None if not captured).
    
    """
    def __init__(self, args, returncode, stdout=None, stderr=None):
        """
        'args={!r}'
        """
    def check_returncode(self):
        """
        Raise CalledProcessError if the exit code is non-zero.
        """
2021-03-02 20:46:56,093 : INFO : tokenize_signature : --> do i ever get here?
def run(*popenargs,
        input=None, capture_output=False, timeout=None, check=False, **kwargs):
    """
    Run command with arguments and return a CompletedProcess instance.

        The returned instance will have attributes args, returncode, stdout and
        stderr. By default, stdout and stderr are not captured, and those attributes
        will be None. Pass stdout=PIPE and/or stderr=PIPE in order to capture them.

        If check is True and the exit code was non-zero, it raises a
        CalledProcessError. The CalledProcessError object will have the return code
        in the returncode attribute, and output & stderr attributes if those streams
        were captured.

        If timeout is given, and the process takes too long, a TimeoutExpired
        exception will be raised.

        There is an optional argument "input", allowing you to
        pass bytes or a string to the subprocess's stdin.  If you use this argument
        you may not also use the Popen constructor's "stdin" argument, as
        it will be used internally.

        By default, all communication is in bytes, and therefore any "input" should
        be bytes, and the stdout and stderr will be bytes. If in text mode, any
        "input" should be a string, and stdout and stderr will be strings decoded
        according to locale encoding, or by "encoding" if set. Text mode is
        triggered by setting any of text, encoding, errors or universal_newlines.

        The other arguments are the same as for the Popen constructor.
    
    """
def list2cmdline(seq):
    """

        Translate a sequence of arguments into a command line
        string, using the same rules as the MS C runtime:

        1) Arguments are delimited by white space, which is either a
           space or a tab.

        2) A string surrounded by double quotation marks is
           interpreted as a single argument, regardless of white space
           contained within.  A quoted string can be embedded in an
           argument.

        3) A double quotation mark preceded by a backslash is
           interpreted as a literal double quotation mark.

        4) Backslashes are interpreted literally, unless they
           immediately precede a double quotation mark.

        5) If backslashes immediately precede a double quotation mark,
           every pair of backslashes is interpreted as a literal
           backslash.  If the number of backslashes is odd, the last
           backslash escapes the next double quotation mark as
           described in rule 3.
    
    """
def getstatusoutput(cmd):
    """
    Return (exitcode, output) of executing cmd in a shell.

        Execute the string 'cmd' in a shell with 'check_output' and
        return a 2-tuple (status, output). The locale encoding is used
        to decode the output and process newlines.

        A trailing newline is stripped from the output.
        The exit status for the command can be interpreted
        according to the rules for the function 'wait'. Example:

        >>> import subprocess
        >>> subprocess.getstatusoutput('ls /bin/ls')
        (0, '/bin/ls')
        >>> subprocess.getstatusoutput('cat /bin/junk')
        (1, 'cat: /bin/junk: No such file or directory')
        >>> subprocess.getstatusoutput('/bin/junk')
        (127, 'sh: /bin/junk: not found')
        >>> subprocess.getstatusoutput('/bin/kill $$')
        (-15, '')
    
    """
def getoutput(cmd):
    """
    Return output (stdout or stderr) of executing cmd in a shell.

        Like getstatusoutput(), except the exit status is ignored and the return
        value is a string containing the command's output.  Example:

        >>> import subprocess
        >>> subprocess.getoutput('ls /bin/ls')
        '/bin/ls'
    
    """
def _use_posix_spawn():
    """
    Check if posix_spawn() can be used for subprocess.

        subprocess requires a posix_spawn() implementation that properly reports
        errors to the parent process, & sets errno on the following failures:

        * Process attribute actions failed.
        * File actions failed.
        * exec() failed.

        Prefer an implementation which can use vfork() in some cases for best
        performance.
    
    """
def Popen(object):
    """
     Execute a child program in a new process.

        For a complete description of the arguments see the Python documentation.

        Arguments:
          args: A string, or a sequence of program arguments.

          bufsize: supplied as the buffering argument to the open() function when
              creating the stdin/stdout/stderr pipe file objects

          executable: A replacement program to execute.

          stdin, stdout and stderr: These specify the executed programs' standard
              input, standard output and standard error file handles, respectively.

          preexec_fn: (POSIX only) An object to be called in the child process
              just before the child is executed.

          close_fds: Controls closing or inheriting of file descriptors.

          shell: If true, the command will be executed through the shell.

          cwd: Sets the current directory before the child is executed.

          env: Defines the environment variables for the new process.

          text: If true, decode stdin, stdout and stderr using the given encoding
              (if set) or the system default otherwise.

          universal_newlines: Alias of text, provided for backwards compatibility.

          startupinfo and creationflags (Windows only)

          restore_signals (POSIX only)

          start_new_session (POSIX only)

          pass_fds (POSIX only)

          encoding and errors: Text mode encoding and error handling to use for
              file objects stdin, stdout and stderr.

        Attributes:
            stdin, stdout, stderr, pid, returncode
    
    """
2021-03-02 20:46:56,097 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,097 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,097 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,097 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,097 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,097 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, args, bufsize=-1, executable=None,
                 stdin=None, stdout=None, stderr=None,
                 preexec_fn=None, close_fds=True,
                 shell=False, cwd=None, env=None, universal_newlines=None,
                 startupinfo=None, creationflags=0,
                 restore_signals=True, start_new_session=False,
                 pass_fds=(), *, encoding=None, errors=None, text=None):
        """
        Create new Popen instance.
        """
    def universal_newlines(self):
        """
         universal_newlines as retained as an alias of text_mode for API
         compatibility. bpo-31756

        """
    def universal_newlines(self, universal_newlines):
        """
        \r\n
        """
    def __enter__(self):
        """
         Flushing a BufferedWriter may raise an error
        """
    def __del__(self, _maxsize=sys.maxsize, _warn=warnings.warn):
        """
         We didn't get to successfully create a child process.

        """
    def _get_devnull(self):
        """
        '_devnull'
        """
    def _stdin_write(self, input):
        """
         communicate() must ignore broken pipe errors.
        """
    def communicate(self, input=None, timeout=None):
        """
        Interact with process: Send data to stdin and close it.
                Read data from stdout and stderr, until end-of-file is
                reached.  Wait for process to terminate.

                The optional "input" argument should be data to be sent to the
                child process, or None, if no data should be sent to the child.
                communicate() returns a tuple (stdout, stderr).

                By default, all communication is in bytes, and therefore any
                "input" should be bytes, and the (stdout, stderr) will be bytes.
                If in text mode (indicated by self.text_mode), any "input" should
                be a string, and (stdout, stderr) will be strings decoded
                according to locale encoding, or by "encoding" if set. Text mode
                is triggered by setting any of text, encoding, errors or
                universal_newlines.
        
        """
    def poll(self):
        """
        Check if child process has terminated. Set and return returncode
                attribute.
        """
    def _remaining_time(self, endtime):
        """
        Convenience for _communicate when computing timeouts.
        """
2021-03-02 20:46:56,105 : INFO : tokenize_signature : --> do i ever get here?
    def _check_timeout(self, endtime, orig_timeout, stdout_seq, stderr_seq,
                       skip_check_and_raise=False):
        """
        Convenience for checking if a timeout has expired.
        """
    def wait(self, timeout=None):
        """
        Wait for child process to terminate; returns self.returncode.
        """
2021-03-02 20:46:56,106 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,106 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,106 : INFO : tokenize_signature : --> do i ever get here?
    def _close_pipe_fds(self,
                        p2cread, p2cwrite,
                        c2pread, c2pwrite,
                        errread, errwrite):
        """
         self._devnull is not always defined.

        """
        def _get_handles(self, stdin, stdout, stderr):
            """
            Construct and return tuple with IO objects:
                        p2cread, p2cwrite, c2pread, c2pwrite, errread, errwrite
            
            """
        def _make_inheritable(self, handle):
            """
            Return a duplicate of handle, which is inheritable
            """
        def _filter_handle_list(self, handle_list):
            """
            Filter out console handles that can't be used
                        in lpAttributeList["handle_list"] and make sure the list
                        isn't empty. This also removes duplicate handles.
            """
2021-03-02 20:46:56,110 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,110 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,110 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,110 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,110 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,110 : INFO : tokenize_signature : --> do i ever get here?
        def _execute_child(self, args, executable, preexec_fn, close_fds,
                           pass_fds, cwd, env,
                           startupinfo, creationflags, shell,
                           p2cread, p2cwrite,
                           c2pread, c2pwrite,
                           errread, errwrite,
                           unused_restore_signals, unused_start_new_session):
            """
            Execute program (MS Windows version)
            """
2021-03-02 20:46:56,112 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,112 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,112 : INFO : tokenize_signature : --> do i ever get here?
        def _internal_poll(self, _deadstate=None,
                _WaitForSingleObject=_winapi.WaitForSingleObject,
                _WAIT_OBJECT_0=_winapi.WAIT_OBJECT_0,
                _GetExitCodeProcess=_winapi.GetExitCodeProcess):
            """
            Check if child process has terminated.  Returns returncode
                        attribute.

                        This method is called by __del__, so it can only refer to objects
                        in its local scope.

            
            """
        def _wait(self, timeout):
            """
            Internal implementation of wait() on Windows.
            """
        def _readerthread(self, fh, buffer):
            """
             Start reader threads feeding into a list hanging off of this
             object, unless they've already been started.

            """
        def send_signal(self, sig):
            """
            Send a signal to the process.
            """
        def terminate(self):
            """
            Terminates the process.
            """
        def _get_handles(self, stdin, stdout, stderr):
            """
            Construct and return tuple with IO objects:
                        p2cread, p2cwrite, c2pread, c2pwrite, errread, errwrite
            
            """
2021-03-02 20:46:56,116 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,116 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,116 : INFO : tokenize_signature : --> do i ever get here?
        def _posix_spawn(self, args, executable, env, restore_signals,
                         p2cread, p2cwrite,
                         c2pread, c2pwrite,
                         errread, errwrite):
            """
            Execute program using os.posix_spawn().
            """
2021-03-02 20:46:56,117 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,117 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,118 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,118 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,118 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,118 : INFO : tokenize_signature : --> do i ever get here?
        def _execute_child(self, args, executable, preexec_fn, close_fds,
                           pass_fds, cwd, env,
                           startupinfo, creationflags, shell,
                           p2cread, p2cwrite,
                           c2pread, c2pwrite,
                           errread, errwrite,
                           restore_signals, start_new_session):
            """
            Execute program (POSIX version)
            """
2021-03-02 20:46:56,121 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,121 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:46:56,121 : INFO : tokenize_signature : --> do i ever get here?
        def _handle_exitstatus(self, sts, _WIFSIGNALED=os.WIFSIGNALED,
                _WTERMSIG=os.WTERMSIG, _WIFEXITED=os.WIFEXITED,
                _WEXITSTATUS=os.WEXITSTATUS, _WIFSTOPPED=os.WIFSTOPPED,
                _WSTOPSIG=os.WSTOPSIG):
            """
            All callers to this function MUST hold self._waitpid_lock.
            """
2021-03-02 20:46:56,122 : INFO : tokenize_signature : --> do i ever get here?
        def _internal_poll(self, _deadstate=None, _waitpid=os.waitpid,
                _WNOHANG=os.WNOHANG, _ECHILD=errno.ECHILD):
            """
            Check if child process has terminated.  Returns returncode
                        attribute.

                        This method is called by __del__, so it cannot reference anything
                        outside of the local scope (nor can any methods it calls).

            
            """
        def _try_wait(self, wait_flags):
            """
            All callers to this function MUST hold self._waitpid_lock.
            """
        def _wait(self, timeout):
            """
            Internal implementation of wait() on POSIX.
            """
        def _communicate(self, input, endtime, orig_timeout):
            """
             Flush stdio buffer.  This might block, if the user has
             been writing to .stdin in an uncontrolled fashion.

            """
        def _save_input(self, input):
            """
             This method is called from the _communicate_with_*() methods
             so that if we time out while communicating, we can continue
             sending input if we retry.

            """
        def send_signal(self, sig):
            """
            Send a signal to the process.
            """
        def terminate(self):
            """
            Terminate the process with SIGTERM
            
            """
        def kill(self):
            """
            Kill the process with SIGKILL
            
            """
