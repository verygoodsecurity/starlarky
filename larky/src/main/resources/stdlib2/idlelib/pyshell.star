2021-03-02 20:54:17,262 : INFO : tokenize_signature : --> do i ever get here?
def idle_showwarning(
        message, category, filename, lineno, file=None, line=None):
    """
    Show Idle-format warning (after replacing warnings.showwarning).

        The differences are the formatter called, the file=None replacement,
        which can be None, the capture of the consequence AttributeError,
        and the output of a hard-coded prompt.
    
    """
def capture_warnings(capture):
    """
    Replace warning.showwarning with idle_showwarning, or reverse.
    """
2021-03-02 20:54:17,263 : INFO : tokenize_signature : --> do i ever get here?
def extended_linecache_checkcache(filename=None,
                                  orig_checkcache=linecache.checkcache):
    """
    Extend linecache.checkcache to preserve the <pyshell#...> entries

        Rather than repeating the linecache code, patch it to save the
        <pyshell#...> entries, call the original linecache.checkcache()
        (skipping them), and then restore the saved entries.

        orig_checkcache is bound at definition time to the original
        method, allowing it to be patched.
    
    """
def PyShellEditorWindow(EditorWindow):
    """
    Regular text edit window in IDLE, supports breakpoints
    """
    def __init__(self, *args):
        """
        <<set-breakpoint-here>>
        """
2021-03-02 20:54:17,265 : INFO : tokenize_signature : --> do i ever get here?
        def filename_changed_hook(old_hook=self.io.filename_change_hook,
                                  self=self):
            """
            Cut
            """
    def color_breakpoint_text(self, color=True):
        """
        Turn colorizing of breakpoint text on or off
        """
    def set_breakpoint(self, lineno):
        """
        BREAK
        """
    def set_breakpoint_here(self, event=None):
        """
        insert
        """
    def clear_breakpoint_here(self, event=None):
        """
        insert
        """
    def clear_file_breaks(self):
        """
        BREAK
        """
    def store_file_breaks(self):
        """
        Save breakpoints when file is saved
        """
    def restore_file_breaks(self):
        """
         this enables setting "BREAK" tags to be visible
        """
    def update_breakpoints(self):
        """
        Retrieves all the breakpoints in the current window
        """
    def ranges_to_linenumbers(self, ranges):
        """
         XXX 13 Dec 2002 KBK Not used currently
            def saved_change_hook(self):
                "Extend base method - clear breaks if module is modified
                if not self.get_saved():
                    self.clear_file_breaks()
                EditorWindow.saved_change_hook(self)


        """
    def _close(self):
        """
        Extend base method - clear breaks when module is closed
        """
def PyShellFileList(FileList):
    """
    Extend base class: IDLE supports a shell and breakpoints
    """
    def open_shell(self, event=None):
        """
        Extend base class: colorizer for the shell window itself
        """
    def __init__(self):
        """
        TODO
        """
    def LoadTagDefs(self):
        """
        stdin
        """
    def removecolors(self):
        """
         Don't remove shell color tags before "iomark

        """
def ModifiedUndoDelegator(UndoDelegator):
    """
    Extend base class: forbid insert/delete before the I/O mark
    """
    def insert(self, index, chars, tags=None):
        """
        <
        """
    def delete(self, index1, index2=None):
        """
        <
        """
def MyRPCClient(rpc.RPCClient):
    """
    Override the base class - just re-raise EOFError
    """
def restart_line(width, filename):  # See bpo-38141.
    """
     See bpo-38141.
    """
def ModifiedInterpreter(InteractiveInterpreter):
    """
    '__main__'
    """
    def spawn_subprocess(self):
        """
        Socket should have been assigned a port number.
        """
    def start_subprocess(self):
        """
         GUI makes several attempts to acquire socket, listens for connection

        """
    def restart_subprocess(self, with_cwd=False, filename=''):
        """
         close only the subprocess debugger

        """
    def __request_interrupt(self):
        """
        exec
        """
    def interrupt_subprocess(self):
        """
         no socket
        """
    def terminate_subprocess(self):
        """
        Make sure subprocess is terminated
        """
    def transfer_path(self, with_cwd=False):
        """
         Issue 13506
        """
    def poll_subprocess(self):
        """
         lost connection or subprocess terminated itself, restart
         [the KBI is from rpc.SocketIO.handle_EOF()]

        """
    def setdebugger(self, debugger):
        """
        Initiate the remote stack viewer from a separate thread.

                This method is called from the subprocess, and by returning from this
                method we allow the subprocess to unblock.  After a bit the shell
                requests the subprocess to open the remote stack viewer which returns a
                static object looking at the last exception.  It is queried through
                the RPC mechanism.

        
        """
    def remote_stack_viewer(self):
        """
        exec
        """
    def execsource(self, source):
        """
        Like runsource() but assumes complete exec source
        """
    def execfile(self, filename, source=None):
        """
        Execute an existing file
        """
    def runsource(self, source):
        """
        Extend base class method: Stuff the source in the line cache first
        """
    def stuffsource(self, source):
        """
        Stuff source in the filename cache
        """
    def prepend_syspath(self, filename):
        """
        Prepend sys.path with file's directory if not already included
        """
    def showsyntaxerror(self, filename=None):
        """
        Override Interactive Interpreter method: Use Colorizing

                Color the offending position instead of printing it and pointing at it
                with a caret.

        
        """
    def showtraceback(self):
        """
        Extend base class method to reset output properly
        """
    def checklinecache(self):
        """
        <>
        """
    def runcommand(self, code):
        """
        Run the code without invoking the debugger
        """
    def runcode(self, code):
        """
        Override base class method
        """
    def write(self, s):
        """
        Override base class method
        """
    def display_port_binding_error(self):
        """
        Port Binding Error
        """
    def display_no_subprocess_error(self):
        """
        Subprocess Connection Error
        """
    def display_executing_dialog(self):
        """
        Already executing
        """
def PyShell(OutputWindow):
    """
    Python 
    """
    def __init__(self, flist=None):
        """
        shell
        """
    def get_standard_extension_names(self):
        """
        Don't debug now
        """
    def set_debugger_indicator(self):
        """
        <<toggle-debugger>>
        """
    def toggle_jit_stack_viewer(self, event=None):
        """
         All we need is the variable
        """
    def close_debugger(self):
        """
        [DEBUG OFF]\n
        """
    def open_debugger(self):
        """
        [DEBUG ON]\n
        """
    def beginexecuting(self):
        """
        Helper for ModifiedInterpreter
        """
    def endexecuting(self):
        """
        Helper for ModifiedInterpreter
        """
    def close(self):
        """
        Extend EditorWindow.close()
        """
    def _close(self):
        """
        Extend EditorWindow._close(), shut down debugger and execution server
        """
    def ispythonsource(self, filename):
        """
        Override EditorWindow method: never remove the colorizer
        """
    def short_title(self):
        """
        'Type "help", "copyright", "credits" or "license()" for more information.'
        """
    def begin(self):
        """
        iomark
        """
    def stop_readline(self):
        """
         no nested mainloop to exit.
        """
    def readline(self):
        """
         nested mainloop()
        """
    def isatty(self):
        """
        sel.first
        """
    def eof_callback(self, event):
        """
         Let the default binding (delete next char) take over
        """
    def linefeed_callback(self, event):
        """
         Insert a linefeed without entering anything (still autoindented)

        """
    def enter_callback(self, event):
        """
         Let the default binding (insert '\n') take over
        """
    def recall(self, s, event):
        """
         remove leading and trailing empty or whitespace lines

        """
    def runit(self):
        """
        iomark
        """
    def open_stack_viewer(self, event=None):
        """
        No stack trace
        """
    def view_restart_mark(self, event=None):
        """
        iomark
        """
    def restart_shell(self, event=None):
        """
        Callback for Run/Restart Shell Cntl-F6
        """
    def showprompt(self):
        """
        insert
        """
    def show_warning(self, msg):
        """
        '\n'
        """
    def resetoutput(self):
        """
        iomark
        """
    def write(self, s, tags=()):
        """
        iomark
        """
    def rmenu_check_cut(self):
        """
        'sel.first'
        """
    def rmenu_check_paste(self):
        """
        'insert'
        """
def fix_x11_paste(root):
    """
    Make paste replace selection on x11.  See issue #5124.
    """
def main():
    """
     bool value
    """
