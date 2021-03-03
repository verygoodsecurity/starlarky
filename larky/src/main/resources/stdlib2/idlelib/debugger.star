def Idb(bdb.Bdb):
    """
     An instance of Debugger or proxy of remote.
    """
    def user_line(self, frame):
        """
         When closing debugger window with [x] in 3.x
        """
    def user_exception(self, frame, info):
        """
        'rpc.py'
        """
    def __frame2message(self, frame):
        """
        %s:%s
        """
def Debugger:
    """
     If passed, a proxy of remote instance.
    """
    def run(self, *args):
        """
         Deal with the scenario where we've already got a program running
         in the debugger and we want to start another. If that is the case,
         our second 'run' was invoked from an event dispatched not from
         the main event loop, but from the nested event loop in 'interaction'
         below. So our stack looks something like this:
               outer main event loop
                 run()
                   <running program with traces>
                     callback to debugger's interaction()
                       nested event loop
                         run() for second command

         This kind of nesting of event loops causes all kinds of problems
         (see e.g. issue #24455) especially when dealing with running as a
         subprocess, where there's all kinds of extra stuff happening in
         there - insert a traceback.print_stack() to check it out.

         By this point, we've already called restart_subprocess() in
         ScriptBinding. However, we also need to unwind the stack back to
         that outer event loop.  To accomplish this, we:
           - return immediately from the nested run()
           - abort_loop ensures the nested event loop will terminate
           - the debugger's interaction routine completes normally
           - the restart_subprocess() will have taken care of stopping
             the running program, which will also let the outer run complete

         That leaves us back at the outer main event loop, at which point our
         after event can fire, and we'll come back to this routine with a
         clean stack.

        """
    def close(self, event=None):
        """
         Clean up pyshell if user clicked debugger control close widget.
         (Causes a harmless extra cycle through close_debugger() if user
         toggled debugger from pyshell Debug menu)

        """
    def make_gui(self):
        """
        Debug Control
        """
    def interaction(self, message, frame, info=None):
        """


        """
    def sync_source_line(self):
        """
        <>
        """
    def __frame2fileline(self, frame):
        """
        'set'
        """
    def show_stack(self):
        """
        'height'
        """
    def show_source(self):
        """
         lineno is stackitem[1]
        """
    def show_locals(self):
        """
        Locals
        """
    def show_globals(self):
        """
        Globals
        """
    def show_variables(self, force=0):
        """
        Load PyShellEditorWindow breakpoints into subprocess debugger
        """
def StackViewer(ScrolledList):
    """
     At least on with the stock AquaTk version on OSX 10.4 you'll
     get a shaking GUI that eventually kills IDLE if the width
     argument is specified.

    """
    def load_stack(self, stack, index=None):
        """
        __name__
        """
    def popup_event(self, event):
        """
        override base method
        """
    def fill_menu(self):
        """
        override base method
        """
    def on_select(self, index):
        """
        override base method
        """
    def on_double(self, index):
        """
        override base method
        """
    def goto_source_line(self):
        """
        active
        """
    def show_stack_frame(self):
        """
        active
        """
    def show_source(self, index):
        """
         XXX 20 == observed height of Entry widget
        """
    def load_dict(self, dict, force=0, rpc_client=None):
        """
        None
        """
    def close(self):
        """
        __main__
        """
