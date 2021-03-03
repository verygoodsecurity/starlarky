def wrap_frame(frame):
    """
    replace info[2], a traceback instance, by its ID
    """
def GUIProxy:
    """
     calls rpc.SocketIO.remotecall() via run.MyHandler instance
     pass frame and traceback object IDs instead of the objects themselves

    """
def IdbAdapter:
    """
    ----------called by an IdbProxy----------


    """
    def set_step(self):
        """
        ----------called by a FrameProxy----------


        """
    def frame_attr(self, fid, name):
        """
        ----------called by a CodeProxy----------


        """
    def code_name(self, cid):
        """
        ----------called by a DictProxy----------


        """
    def dict_keys(self, did):
        """
        dict_keys not public or pickleable
        """
    def dict_keys_list(self, did):
        """
         can't pickle module 'builtins'
        """
def start_debugger(rpchandler, gui_adap_oid):
    """
    Start the debugger and its RPC link in the Python subprocess

        Start the subprocess side of the split debugger and set up that side of the
        RPC link by instantiating the GUIProxy, Idb debugger, and IdbAdapter
        objects and linking them together.  Register the IdbAdapter with the
        RPCServer to handle RPC requests from the split debugger GUI via the
        IdbProxy.

    
    """
def FrameProxy:
    """
    idb_adapter
    """
    def __getattr__(self, name):
        """
        _
        """
    def _get_f_code(self):
        """
        frame_code
        """
    def _get_f_globals(self):
        """
        frame_globals
        """
    def _get_f_locals(self):
        """
        frame_locals
        """
    def _get_dict_proxy(self, did):
        """
        co_name
        """
def DictProxy:
    """
        def keys(self):
            return self._conn.remotecall(self._oid, "dict_keys", (self._did,), {})

     'temporary' until dict_keys is a pickleable built-in type

    """
    def keys(self):
        """
        dict_keys_list
        """
    def __getitem__(self, key):
        """
        dict_item
        """
    def __getattr__(self, name):
        """
        print("*** Failed DictProxy.__getattr__:", name)

        """
def GUIAdapter:
    """
    print("*** Interaction: (%s, %s, %s)" % (message, fid, modified_info))

    """
def IdbProxy:
    """
    print("*** IdbProxy.call %s %s %s" % (methodname, args, kwargs))

    """
    def run(self, cmd, locals):
        """
         Ignores locals on purpose!

        """
    def get_stack(self, frame, tbid):
        """
         passing frame and traceback IDs, not the objects themselves

        """
    def set_continue(self):
        """
        set_continue
        """
    def set_step(self):
        """
        set_step
        """
    def set_next(self, frame):
        """
        set_next
        """
    def set_return(self, frame):
        """
        set_return
        """
    def set_quit(self):
        """
        set_quit
        """
    def set_break(self, filename, lineno):
        """
        set_break
        """
    def clear_break(self, filename, lineno):
        """
        clear_break
        """
    def clear_all_file_breaks(self, filename):
        """
        clear_all_file_breaks
        """
def start_remote_debugger(rpcclt, pyshell):
    """
    Start the subprocess debugger, initialize the debugger GUI and RPC link

        Request the RPCServer start the Python subprocess debugger and link.  Set
        up the Idle side of the split debugger by instantiating the IdbProxy,
        debugger GUI, and debugger GUIAdapter objects and linking them together.

        Register the GUIAdapter with the RPCClient to handle debugger GUI
        interaction requests coming from the subprocess debugger via the GUIProxy.

        The IdbAdapter will pass execution and environment requests coming from the
        Idle debugger GUI to the subprocess debugger via the IdbProxy.

    
    """
def close_remote_debugger(rpcclt):
    """
    Shut down subprocess debugger and Idle side of debugger RPC link

        Request that the RPCServer shut down the subprocess debugger and link.
        Unregister the GUIAdapter, which will cause a GC on the Idle process
        debugger and RPC link objects.  (The second reference to the debugger GUI
        is deleted in pyshell.close_remote_debugger().)

    
    """
def close_subprocess_debugger(rpcclt):
    """
    exec
    """
def restart_subprocess_debugger(rpcclt):
    """
    exec
    """
