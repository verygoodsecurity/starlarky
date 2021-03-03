def idle_formatwarning(message, category, filename, lineno, line=None):
    """
    Format warnings the IDLE way.
    """
2021-03-02 20:54:17,955 : INFO : tokenize_signature : --> do i ever get here?
def idle_showwarning_subproc(
        message, category, filename, lineno, file=None, line=None):
    """
    Show Idle-format warning after replacing warnings.showwarning.

        The only difference is the formatter called.
    
    """
def capture_warnings(capture):
    """
    Replace warning.showwarning with idle_showwarning_subproc, or reverse.
    """
def handle_tk_events(tcl=tcl):
    """
    Process any tk events that are ready to be dispatched if tkinter
        has been imported, a tcl interpreter has been created and tk has been
        loaded.
    """
def main(del_exitfunc=False):
    """
    Start the Python execution server in a subprocess

        In the Python subprocess, RPCServer is instantiated with handlerclass
        MyHandler, which inherits register/unregister methods from RPCHandler via
        the mix-in class SocketIO.

        When the RPCServer 'server' is instantiated, the TCPServer initialization
        creates an instance of run.MyHandler and calls its handle() method.
        handle() instantiates a run.Executive object, passing it a reference to the
        MyHandler object.  That reference is saved as attribute rpchandler of the
        Executive instance.  The Executive methods have access to the reference and
        can pass it on to entities that they command
        (e.g. debugger_r.Debugger.start_debugger()).  The latter, in turn, can
        call MyHandler(SocketIO) register/unregister methods via the reference to
        register and unregister themselves.

    
    """
def manage_socket(address):
    """
    IDLE Subprocess: OSError: 
    """
def show_socket_error(err, address):
    """
    Display socket error from manage_socket.
    """
def print_exception():
    """
    \nThe above exception was the direct cause 
    of the following exception:\n
    """
def cleanup_traceback(tb, exclude):
    """
    Remove excluded traces from beginning/end of tb; get cached lines
    """
def flush_stdout():
    """
    XXX How to do this now?
    """
def exit():
    """
    Exit subprocess, possibly after first clearing exit functions.

        If config-main.cfg/.def 'General' 'delete-exitfunc' is True, then any
        functions registered with atexit will be removed before exiting.
        (VPython support)

    
    """
def fix_scaling(root):
    """
    Scale fonts on HiDPI displays.
    """
def fixdoc(fun, text):
    """
    '\n\n'
    """
def install_recursionlimit_wrappers():
    """
    Install wrappers to always add 30 to the recursion limit.
    """
    def setrecursionlimit(*args, **kwargs):
        """
         mimic the original sys.setrecursionlimit()'s input handling

        """
    def getrecursionlimit():
        """
        f"""\
                    This IDLE wrapper subtracts {RECURSIONLIMIT_DELTA} to compensate
                    for the {RECURSIONLIMIT_DELTA} IDLE adds when setting the limit.
        """
def uninstall_recursionlimit_wrappers():
    """
    Uninstall the recursion limit wrappers from the sys module.

        IDLE only uses this for tests. Users can import run and call
        this to remove the wrapping.
    
    """
def MyRPCServer(rpc.RPCServer):
    """
    Override RPCServer method for IDLE

            Interrupt the MainThread and exit server if link is dropped.

        
    """
def StdioFile(io.TextIOBase):
    """
    'utf-8'
    """
    def encoding(self):
        """
        '<%s>'
        """
    def isatty(self):
        """
        write to closed file
        """
def StdInputFile(StdioFile):
    """
    ''
    """
    def readable(self):
        """
        read from closed file
        """
    def readline(self, size=-1):
        """
        read from closed file
        """
    def close(self):
        """
        Override base method
        """
    def exithook(self):
        """
        override SocketIO method - wait for MainThread to shut us down
        """
    def EOFhook(self):
        """
        Override SocketIO method - terminate wait on callback and exit thread
        """
    def decode_interrupthook(self):
        """
        interrupt awakened thread
        """
def Executive(object):
    """
     SystemExit called with an argument.
    """
    def interrupt_the_server(self):
        """
        Unregister the Idb Adapter.  Link objects and Idb then subject to GC
        """
    def get_the_calltip(self, name):
        """
        __name__
        """
