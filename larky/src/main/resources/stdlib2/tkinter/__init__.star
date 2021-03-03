def _join(value):
    """
    Internal function.
    """
def _stringify(value):
    """
    Internal function.
    """
def _flatten(seq):
    """
    Internal function.
    """
def _cnfmerge(cnfs):
    """
    Internal function.
    """
def _splitdict(tk, v, cut_minus=True, conv=None):
    """
    Return a properly formatted dict built from Tcl list pairs.

        If cut_minus is True, the supposed '-' prefix will be removed from
        keys. If conv is specified, it is used to convert values.

        Tcl list is expected to contain an even number of elements.
    
    """
def EventType(str, enum.Enum):
    """
    '2'
    """
    def __str__(self):
        """
        Container for the properties of an event.

            Instances of this type are generated if one of the following events occurs:

            KeyPress, KeyRelease - for keyboard events
            ButtonPress, ButtonRelease, Motion, Enter, Leave, MouseWheel - for mouse events
            Visibility, Unmap, Map, Expose, FocusIn, FocusOut, Circulate,
            Colormap, Gravity, Reparent, Property, Destroy, Activate,
            Deactivate - for window events.

            If a callback function for one of these events is registered
            using bind, bind_all, bind_class, or tag_bind, the callback is
            called with an Event as first argument. It will have the
            following attributes (in braces are the event types for which
            the attribute is valid):

                serial - serial number of event
            num - mouse button pressed (ButtonPress, ButtonRelease)
            focus - whether the window has the focus (Enter, Leave)
            height - height of the exposed window (Configure, Expose)
            width - width of the exposed window (Configure, Expose)
            keycode - keycode of the pressed key (KeyPress, KeyRelease)
            state - state of the event as a number (ButtonPress, ButtonRelease,
                                    Enter, KeyPress, KeyRelease,
                                    Leave, Motion)
            state - state as a string (Visibility)
            time - when the event occurred
            x - x-position of the mouse
            y - y-position of the mouse
            x_root - x-position of the mouse on the screen
                     (ButtonPress, ButtonRelease, KeyPress, KeyRelease, Motion)
            y_root - y-position of the mouse on the screen
                     (ButtonPress, ButtonRelease, KeyPress, KeyRelease, Motion)
            char - pressed character (KeyPress, KeyRelease)
            send_event - see X/Windows documentation
            keysym - keysym of the event as a string (KeyPress, KeyRelease)
            keysym_num - keysym of the event as a number (KeyPress, KeyRelease)
            type - type of the event as a number
            widget - widget in which the event occurred
            delta - delta of wheel movement (MouseWheel)
    
        """
    def __repr__(self):
        """
        '??'
        """
def NoDefaultRoot():
    """
    Inhibit setting of default root window.

        Call this function to inhibit that the first instance of
        Tk is used for windows without an explicit parent window.
    
    """
def _tkerror(err):
    """
    Internal function.
    """
def _exit(code=0):
    """
    Internal function. Calling it will raise the exception SystemExit.
    """
def Variable:
    """
    Class to define value holders for e.g. buttons.

        Subclasses StringVar, IntVar, DoubleVar, BooleanVar are specializations
        that constrain the type of the value returned from get().
    """
    def __init__(self, master=None, value=None, name=None):
        """
        Construct a variable

                MASTER can be given as master widget.
                VALUE is an optional value (defaults to "")
                NAME is an optional Tcl name (defaults to PY_VARnum).

                If NAME matches an existing variable and VALUE is omitted
                then the existing value is retained.
        
        """
    def __del__(self):
        """
        Unset the variable in Tcl.
        """
    def __str__(self):
        """
        Return the name of the variable in Tcl.
        """
    def set(self, value):
        """
        Set the variable to VALUE.
        """
    def get(self):
        """
        Return value of variable.
        """
    def _register(self, callback):
        """
        Define a trace callback for the variable.

                Mode is one of "read", "write", "unset", or a list or tuple of
                such strings.
                Callback must be a function which is called when the variable is
                read, written or unset.

                Return the name of the callback.
        
        """
    def trace_remove(self, mode, cbname):
        """
        Delete the trace callback for a variable.

                Mode is one of "read", "write", "unset" or a list or tuple of
                such strings.  Must be same as were specified in trace_add().
                cbname is the name of the callback returned from trace_add().
        
        """
    def trace_info(self):
        """
        Return all trace callback information.
        """
    def trace_variable(self, mode, callback):
        """
        Define a trace callback for the variable.

                MODE is one of "r", "w", "u" for read, write, undefine.
                CALLBACK must be a function which is called when
                the variable is read, written or undefined.

                Return the name of the callback.

                This deprecated method wraps a deprecated Tcl method that will
                likely be removed in the future.  Use trace_add() instead.
        
        """
    def trace_vdelete(self, mode, cbname):
        """
        Delete the trace callback for a variable.

                MODE is one of "r", "w", "u" for read, write, undefine.
                CBNAME is the name of the callback returned from trace_variable or trace.

                This deprecated method wraps a deprecated Tcl method that will
                likely be removed in the future.  Use trace_remove() instead.
        
        """
    def trace_vinfo(self):
        """
        Return all trace callback information.

                This deprecated method wraps a deprecated Tcl method that will
                likely be removed in the future.  Use trace_info() instead.
        
        """
    def __eq__(self, other):
        """
        Comparison for equality (==).

                Note: if the Variable's master matters to behavior
                also compare self._master == other._master
        
        """
def StringVar(Variable):
    """
    Value holder for strings variables.
    """
    def __init__(self, master=None, value=None, name=None):
        """
        Construct a string variable.

                MASTER can be given as master widget.
                VALUE is an optional value (defaults to "")
                NAME is an optional Tcl name (defaults to PY_VARnum).

                If NAME matches an existing variable and VALUE is omitted
                then the existing value is retained.
        
        """
    def get(self):
        """
        Return value of variable as string.
        """
def IntVar(Variable):
    """
    Value holder for integer variables.
    """
    def __init__(self, master=None, value=None, name=None):
        """
        Construct an integer variable.

                MASTER can be given as master widget.
                VALUE is an optional value (defaults to 0)
                NAME is an optional Tcl name (defaults to PY_VARnum).

                If NAME matches an existing variable and VALUE is omitted
                then the existing value is retained.
        
        """
    def get(self):
        """
        Return the value of the variable as an integer.
        """
def DoubleVar(Variable):
    """
    Value holder for float variables.
    """
    def __init__(self, master=None, value=None, name=None):
        """
        Construct a float variable.

                MASTER can be given as master widget.
                VALUE is an optional value (defaults to 0.0)
                NAME is an optional Tcl name (defaults to PY_VARnum).

                If NAME matches an existing variable and VALUE is omitted
                then the existing value is retained.
        
        """
    def get(self):
        """
        Return the value of the variable as a float.
        """
def BooleanVar(Variable):
    """
    Value holder for boolean variables.
    """
    def __init__(self, master=None, value=None, name=None):
        """
        Construct a boolean variable.

                MASTER can be given as master widget.
                VALUE is an optional value (defaults to False)
                NAME is an optional Tcl name (defaults to PY_VARnum).

                If NAME matches an existing variable and VALUE is omitted
                then the existing value is retained.
        
        """
    def set(self, value):
        """
        Set the variable to VALUE.
        """
    def get(self):
        """
        Return the value of the variable as a bool.
        """
def mainloop(n=0):
    """
    Run the main loop of Tcl.
    """
def getboolean(s):
    """
    Convert true and false to integer values 1 and 0.
    """
def Misc:
    """
    Internal class.

        Base class which defines methods common for interior widgets.
    """
    def destroy(self):
        """
        Internal function.

                Delete all Tcl commands created for
                this widget in the Tcl interpreter.
        """
    def deletecommand(self, name):
        """
        Internal function.

                Delete the Tcl command provided in NAME.
        """
    def tk_strictMotif(self, boolean=None):
        """
        Set Tcl internal variable, whether the look and feel
                should adhere to Motif.

                A parameter of 1 means adhere to Motif (e.g. no color
                change if mouse passes over slider).
                Returns the set value.
        """
    def tk_bisque(self):
        """
        Change the color scheme to light brown as used in Tk 3.6 and before.
        """
    def tk_setPalette(self, *args, **kw):
        """
        Set a new color scheme for all widget elements.

                A single color as argument will cause that all colors of Tk
                widget elements are derived from this.
                Alternatively several keyword parameters and its associated
                colors can be given. The following keywords are valid:
                activeBackground, foreground, selectColor,
                activeForeground, highlightBackground, selectBackground,
                background, highlightColor, selectForeground,
                disabledForeground, insertBackground, troughColor.
        """
    def wait_variable(self, name='PY_VAR'):
        """
        Wait until the variable is modified.

                A parameter of type IntVar, StringVar, DoubleVar or
                BooleanVar must be given.
        """
    def wait_window(self, window=None):
        """
        Wait until a WIDGET is destroyed.

                If no parameter is given self is used.
        """
    def wait_visibility(self, window=None):
        """
        Wait until the visibility of a WIDGET changes
                (e.g. it appears).

                If no parameter is given self is used.
        """
    def setvar(self, name='PY_VAR', value='1'):
        """
        Set Tcl variable NAME to VALUE.
        """
    def getvar(self, name='PY_VAR'):
        """
        Return value of Tcl variable NAME.
        """
    def getint(self, s):
        """
        Return a boolean value for Tcl boolean values true and false given as parameter.
        """
    def focus_set(self):
        """
        Direct input focus to this widget.

                If the application currently does not have the focus
                this widget will get the focus if the application gets
                the focus through the window manager.
        """
    def focus_force(self):
        """
        Direct input focus to this widget even if the
                application does not have the focus. Use with
                caution!
        """
    def focus_get(self):
        """
        Return the widget which has currently the focus in the
                application.

                Use focus_displayof to allow working with several
                displays. Return None if application does not have
                the focus.
        """
    def focus_displayof(self):
        """
        Return the widget which has currently the focus on the
                display where this widget is located.

                Return None if the application does not have the focus.
        """
    def focus_lastfor(self):
        """
        Return the widget which would have the focus if top level
                for this widget gets the focus from the window manager.
        """
    def tk_focusFollowsMouse(self):
        """
        The widget under mouse will get automatically focus. Can not
                be disabled easily.
        """
    def tk_focusNext(self):
        """
        Return the next widget in the focus order which follows
                widget which has currently the focus.

                The focus order first goes to the next child, then to
                the children of the child recursively and then to the
                next sibling which is higher in the stacking order.  A
                widget is omitted if it has the takefocus resource set
                to 0.
        """
    def tk_focusPrev(self):
        """
        Return previous widget in the focus order. See tk_focusNext for details.
        """
    def after(self, ms, func=None, *args):
        """
        Call function once after given time.

                MS specifies the time in milliseconds. FUNC gives the
                function which shall be called. Additional parameters
                are given as parameters to the function call.  Return
                identifier to cancel scheduling with after_cancel.
        """
            def callit():
                """
                'after'
                """
    def after_idle(self, func, *args):
        """
        Call FUNC once if the Tcl main loop has no event to
                process.

                Return an identifier to cancel the scheduling with
                after_cancel.
        """
    def after_cancel(self, id):
        """
        Cancel scheduling of function identified with ID.

                Identifier returned by after or after_idle must be
                given as first parameter.
        
        """
    def bell(self, displayof=0):
        """
        Ring a display's bell.
        """
    def clipboard_get(self, **kw):
        """
        Retrieve data from the clipboard on window's display.

                The window keyword defaults to the root window of the Tkinter
                application.

                The type keyword specifies the form in which the data is
                to be returned and should be an atom name such as STRING
                or FILE_NAME.  Type defaults to STRING, except on X11, where the default
                is to try UTF8_STRING and fall back to STRING.

                This command is equivalent to:

                selection_get(CLIPBOARD)
        
        """
    def clipboard_clear(self, **kw):
        """
        Clear the data in the Tk clipboard.

                A widget specified for the optional displayof keyword
                argument specifies the target display.
        """
    def clipboard_append(self, string, **kw):
        """
        Append STRING to the Tk clipboard.

                A widget specified at the optional displayof keyword
                argument specifies the target display. The clipboard
                can be retrieved with selection_get.
        """
    def grab_current(self):
        """
        Return widget which has currently the grab in this application
                or None.
        """
    def grab_release(self):
        """
        Release grab for this widget if currently set.
        """
    def grab_set(self):
        """
        Set grab for this widget.

                A grab directs all events to this and descendant
                widgets in the application.
        """
    def grab_set_global(self):
        """
        Set global grab for this widget.

                A global grab directs all events to this and
                descendant widgets on the display. Use with caution -
                other applications do not get events anymore.
        """
    def grab_status(self):
        """
        Return None, "local" or "global" if this widget has
                no, a local or a global grab.
        """
    def option_add(self, pattern, value, priority = None):
        """
        Set a VALUE (second parameter) for an option
                PATTERN (first parameter).

                An optional third parameter gives the numeric priority
                (defaults to 80).
        """
    def option_clear(self):
        """
        Clear the option database.

                It will be reloaded if option_add is called.
        """
    def option_get(self, name, className):
        """
        Return the value for an option NAME for this widget
                with CLASSNAME.

                Values with higher priority override lower values.
        """
    def option_readfile(self, fileName, priority = None):
        """
        Read file FILENAME into the option database.

                An optional second parameter gives the numeric
                priority.
        """
    def selection_clear(self, **kw):
        """
        Clear the current X selection.
        """
    def selection_get(self, **kw):
        """
        Return the contents of the current X selection.

                A keyword parameter selection specifies the name of
                the selection and defaults to PRIMARY.  A keyword
                parameter displayof specifies a widget on the display
                to use. A keyword parameter type specifies the form of data to be
                fetched, defaulting to STRING except on X11, where UTF8_STRING is tried
                before STRING.
        """
    def selection_handle(self, command, **kw):
        """
        Specify a function COMMAND to call if the X
                selection owned by this widget is queried by another
                application.

                This function must return the contents of the
                selection. The function will be called with the
                arguments OFFSET and LENGTH which allows the chunking
                of very long selections. The following keyword
                parameters can be provided:
                selection - name of the selection (default PRIMARY),
                type - type of the selection (e.g. STRING, FILE_NAME).
        """
    def selection_own(self, **kw):
        """
        Become owner of X selection.

                A keyword parameter selection specifies the name of
                the selection (default PRIMARY).
        """
    def selection_own_get(self, **kw):
        """
        Return owner of X selection.

                The following keyword parameter can
                be provided:
                selection - name of the selection (default PRIMARY),
                type - type of the selection (e.g. STRING, FILE_NAME).
        """
    def send(self, interp, cmd, *args):
        """
        Send Tcl command CMD to different interpreter INTERP to be executed.
        """
    def lower(self, belowThis=None):
        """
        Lower this widget in the stacking order.
        """
    def tkraise(self, aboveThis=None):
        """
        Raise this widget in the stacking order.
        """
    def winfo_atom(self, name, displayof=0):
        """
        Return integer which represents atom NAME.
        """
    def winfo_atomname(self, id, displayof=0):
        """
        Return name of atom with identifier ID.
        """
    def winfo_cells(self):
        """
        Return number of cells in the colormap for this widget.
        """
    def winfo_children(self):
        """
        Return a list of all widgets which are children of this widget.
        """
    def winfo_class(self):
        """
        Return window class name of this widget.
        """
    def winfo_colormapfull(self):
        """
        Return True if at the last color request the colormap was full.
        """
    def winfo_containing(self, rootX, rootY, displayof=0):
        """
        Return the widget which is at the root coordinates ROOTX, ROOTY.
        """
    def winfo_depth(self):
        """
        Return the number of bits per pixel.
        """
    def winfo_exists(self):
        """
        Return true if this widget exists.
        """
    def winfo_fpixels(self, number):
        """
        Return the number of pixels for the given distance NUMBER
                (e.g. "3c") as float.
        """
    def winfo_geometry(self):
        """
        Return geometry string for this widget in the form "widthxheight+X+Y".
        """
    def winfo_height(self):
        """
        Return height of this widget.
        """
    def winfo_id(self):
        """
        Return identifier ID for this widget.
        """
    def winfo_interps(self, displayof=0):
        """
        Return the name of all Tcl interpreters for this display.
        """
    def winfo_ismapped(self):
        """
        Return true if this widget is mapped.
        """
    def winfo_manager(self):
        """
        Return the window manager name for this widget.
        """
    def winfo_name(self):
        """
        Return the name of this widget.
        """
    def winfo_parent(self):
        """
        Return the name of the parent of this widget.
        """
    def winfo_pathname(self, id, displayof=0):
        """
        Return the pathname of the widget given by ID.
        """
    def winfo_pixels(self, number):
        """
        Rounded integer value of winfo_fpixels.
        """
    def winfo_pointerx(self):
        """
        Return the x coordinate of the pointer on the root window.
        """
    def winfo_pointerxy(self):
        """
        Return a tuple of x and y coordinates of the pointer on the root window.
        """
    def winfo_pointery(self):
        """
        Return the y coordinate of the pointer on the root window.
        """
    def winfo_reqheight(self):
        """
        Return requested height of this widget.
        """
    def winfo_reqwidth(self):
        """
        Return requested width of this widget.
        """
    def winfo_rgb(self, color):
        """
        Return tuple of decimal values for red, green, blue for
                COLOR in this widget.
        """
    def winfo_rootx(self):
        """
        Return x coordinate of upper left corner of this widget on the
                root window.
        """
    def winfo_rooty(self):
        """
        Return y coordinate of upper left corner of this widget on the
                root window.
        """
    def winfo_screen(self):
        """
        Return the screen name of this widget.
        """
    def winfo_screencells(self):
        """
        Return the number of the cells in the colormap of the screen
                of this widget.
        """
    def winfo_screendepth(self):
        """
        Return the number of bits per pixel of the root window of the
                screen of this widget.
        """
    def winfo_screenheight(self):
        """
        Return the number of pixels of the height of the screen of this widget
                in pixel.
        """
    def winfo_screenmmheight(self):
        """
        Return the number of pixels of the height of the screen of
                this widget in mm.
        """
    def winfo_screenmmwidth(self):
        """
        Return the number of pixels of the width of the screen of
                this widget in mm.
        """
    def winfo_screenvisual(self):
        """
        Return one of the strings directcolor, grayscale, pseudocolor,
                staticcolor, staticgray, or truecolor for the default
                colormodel of this screen.
        """
    def winfo_screenwidth(self):
        """
        Return the number of pixels of the width of the screen of
                this widget in pixel.
        """
    def winfo_server(self):
        """
        Return information of the X-Server of the screen of this widget in
                the form "XmajorRminor vendor vendorVersion".
        """
    def winfo_toplevel(self):
        """
        Return the toplevel widget of this widget.
        """
    def winfo_viewable(self):
        """
        Return true if the widget and all its higher ancestors are mapped.
        """
    def winfo_visual(self):
        """
        Return one of the strings directcolor, grayscale, pseudocolor,
                staticcolor, staticgray, or truecolor for the
                colormodel of this widget.
        """
    def winfo_visualid(self):
        """
        Return the X identifier for the visual for this widget.
        """
    def winfo_visualsavailable(self, includeids=False):
        """
        Return a list of all visuals available for the screen
                of this widget.

                Each item in the list consists of a visual name (see winfo_visual), a
                depth and if includeids is true is given also the X identifier.
        """
    def __winfo_parseitem(self, t):
        """
        Internal function.
        """
    def __winfo_getint(self, x):
        """
        Internal function.
        """
    def winfo_vrootheight(self):
        """
        Return the height of the virtual root window associated with this
                widget in pixels. If there is no virtual root window return the
                height of the screen.
        """
    def winfo_vrootwidth(self):
        """
        Return the width of the virtual root window associated with this
                widget in pixel. If there is no virtual root window return the
                width of the screen.
        """
    def winfo_vrootx(self):
        """
        Return the x offset of the virtual root relative to the root
                window of the screen of this widget.
        """
    def winfo_vrooty(self):
        """
        Return the y offset of the virtual root relative to the root
                window of the screen of this widget.
        """
    def winfo_width(self):
        """
        Return the width of this widget.
        """
    def winfo_x(self):
        """
        Return the x coordinate of the upper left corner of this widget
                in the parent.
        """
    def winfo_y(self):
        """
        Return the y coordinate of the upper left corner of this widget
                in the parent.
        """
    def update(self):
        """
        Enter event loop until all pending events have been processed by Tcl.
        """
    def update_idletasks(self):
        """
        Enter event loop until all idle callbacks have been called. This
                will update the display of windows but not process events caused by
                the user.
        """
    def bindtags(self, tagList=None):
        """
        Set or get the list of bindtags for this widget.

                With no argument return the list of all bindtags associated with
                this widget. With a list of strings as argument the bindtags are
                set to this list. The bindtags determine in which order events are
                processed (see bind).
        """
    def _bind(self, what, sequence, func, add, needcleanup=1):
        """
        Internal function.
        """
    def bind(self, sequence=None, func=None, add=None):
        """
        Bind to this widget at event SEQUENCE a call to function FUNC.

                SEQUENCE is a string of concatenated event
                patterns. An event pattern is of the form
                <MODIFIER-MODIFIER-TYPE-DETAIL> where MODIFIER is one
                of Control, Mod2, M2, Shift, Mod3, M3, Lock, Mod4, M4,
                Button1, B1, Mod5, M5 Button2, B2, Meta, M, Button3,
                B3, Alt, Button4, B4, Double, Button5, B5 Triple,
                Mod1, M1. TYPE is one of Activate, Enter, Map,
                ButtonPress, Button, Expose, Motion, ButtonRelease
                FocusIn, MouseWheel, Circulate, FocusOut, Property,
                Colormap, Gravity Reparent, Configure, KeyPress, Key,
                Unmap, Deactivate, KeyRelease Visibility, Destroy,
                Leave and DETAIL is the button number for ButtonPress,
                ButtonRelease and DETAIL is the Keysym for KeyPress and
                KeyRelease. Examples are
                <Control-Button-1> for pressing Control and mouse button 1 or
                <Alt-A> for pressing A and the Alt key (KeyPress can be omitted).
                An event pattern can also be a virtual event of the form
                <<AString>> where AString can be arbitrary. This
                event can be generated by event_generate.
                If events are concatenated they must appear shortly
                after each other.

                FUNC will be called if the event sequence occurs with an
                instance of Event as argument. If the return value of FUNC is
                "break" no further bound function is invoked.

                An additional boolean parameter ADD specifies whether FUNC will
                be called additionally to the other bound function or whether
                it will replace the previous function.

                Bind will return an identifier to allow deletion of the bound function with
                unbind without memory leak.

                If FUNC or SEQUENCE is omitted the bound function or list
                of bound events are returned.
        """
    def unbind(self, sequence, funcid=None):
        """
        Unbind for this widget for event SEQUENCE  the
                function identified with FUNCID.
        """
    def bind_all(self, sequence=None, func=None, add=None):
        """
        Bind to all widgets at an event SEQUENCE a call to function FUNC.
                An additional boolean parameter ADD specifies whether FUNC will
                be called additionally to the other bound function or whether
                it will replace the previous function. See bind for the return value.
        """
    def unbind_all(self, sequence):
        """
        Unbind for all widgets for event SEQUENCE all functions.
        """
    def bind_class(self, className, sequence=None, func=None, add=None):
        """
        Bind to widgets with bindtag CLASSNAME at event
                SEQUENCE a call of function FUNC. An additional
                boolean parameter ADD specifies whether FUNC will be
                called additionally to the other bound function or
                whether it will replace the previous function. See bind for
                the return value.
        """
    def unbind_class(self, className, sequence):
        """
        Unbind for all widgets with bindtag CLASSNAME for event SEQUENCE
                all functions.
        """
    def mainloop(self, n=0):
        """
        Call the mainloop of Tk.
        """
    def quit(self):
        """
        Quit the Tcl interpreter. All widgets will be destroyed.
        """
    def _getints(self, string):
        """
        Internal function.
        """
    def _getdoubles(self, string):
        """
        Internal function.
        """
    def _getboolean(self, string):
        """
        Internal function.
        """
    def _displayof(self, displayof):
        """
        Internal function.
        """
    def _windowingsystem(self):
        """
        Internal function.
        """
    def _options(self, cnf, kw = None):
        """
        Internal function.
        """
    def nametowidget(self, name):
        """
        Return the Tkinter instance of a widget identified by
                its Tcl name NAME.
        """
    def _register(self, func, subst=None, needcleanup=1):
        """
        Return a newly created Tcl function. If this
                function is called, the Python function FUNC will
                be executed. An optional function SUBST can
                be given which will be executed before FUNC.
        """
    def _root(self):
        """
        Internal function.
        """
    def _substitute(self, *args):
        """
        Internal function.
        """
        def getint_event(s):
            """
            Tk changed behavior in 8.4.2, returning "??" rather more often.
            """
    def _report_exception(self):
        """
        Internal function.
        """
    def _getconfigure(self, *args):
        """
        Call Tcl configure command and return the result as a dict.
        """
    def _getconfigure1(self, *args):
        """
        Internal function.
        """
    def configure(self, cnf=None, **kw):
        """
        Configure resources of a widget.

                The values for resources are specified as keyword
                arguments. To get an overview about
                the allowed keyword arguments call the method keys.
        
        """
    def cget(self, key):
        """
        Return the resource value for a KEY given as string.
        """
    def __setitem__(self, key, value):
        """
        Return a list of all resource names of this widget.
        """
    def __str__(self):
        """
        Return the window path name of this widget.
        """
    def __repr__(self):
        """
        '<%s.%s object %s>'
        """
    def pack_propagate(self, flag=_noarg_):
        """
        Set or get the status for propagation of geometry information.

                A boolean argument specifies whether the geometry information
                of the slaves will determine the size of this widget. If no argument
                is given the current setting will be returned.
        
        """
    def pack_slaves(self):
        """
        Return a list of all slaves of this widget
                in its packing order.
        """
    def place_slaves(self):
        """
        Return a list of all slaves of this widget
                in its packing order.
        """
    def grid_anchor(self, anchor=None): # new in Tk 8.5
        """
         new in Tk 8.5
        """
    def grid_bbox(self, column=None, row=None, col2=None, row2=None):
        """
        Return a tuple of integer coordinates for the bounding
                box of this widget controlled by the geometry manager grid.

                If COLUMN, ROW is given the bounding box applies from
                the cell with row and column 0 to the specified
                cell. If COL2 and ROW2 are given the bounding box
                starts at that cell.

                The returned integers specify the offset of the upper left
                corner in the master widget and the width and height.
        
        """
    def _gridconvvalue(self, value):
        """
        '.'
        """
    def _grid_configure(self, command, index, cnf, kw):
        """
        Internal function.
        """
    def grid_columnconfigure(self, index, cnf={}, **kw):
        """
        Configure column INDEX of a grid.

                Valid resources are minsize (minimum size of the column),
                weight (how much does additional space propagate to this column)
                and pad (how much space to let additionally).
        """
    def grid_location(self, x, y):
        """
        Return a tuple of column and row which identify the cell
                at which the pixel at position X and Y inside the master
                widget is located.
        """
    def grid_propagate(self, flag=_noarg_):
        """
        Set or get the status for propagation of geometry information.

                A boolean argument specifies whether the geometry information
                of the slaves will determine the size of this widget. If no argument
                is given, the current setting will be returned.
        
        """
    def grid_rowconfigure(self, index, cnf={}, **kw):
        """
        Configure row INDEX of a grid.

                Valid resources are minsize (minimum size of the row),
                weight (how much does additional space propagate to this row)
                and pad (how much space to let additionally).
        """
    def grid_size(self):
        """
        Return a tuple of the number of column and rows in the grid.
        """
    def grid_slaves(self, row=None, column=None):
        """
        Return a list of all slaves of this widget
                in its packing order.
        """
    def event_add(self, virtual, *sequences):
        """
        Bind a virtual event VIRTUAL (of the form <<Name>>)
                to an event SEQUENCE such that the virtual event is triggered
                whenever SEQUENCE occurs.
        """
    def event_delete(self, virtual, *sequences):
        """
        Unbind a virtual event VIRTUAL from SEQUENCE.
        """
    def event_generate(self, sequence, **kw):
        """
        Generate an event SEQUENCE. Additional
                keyword arguments specify parameter of the event
                (e.g. x, y, rootx, rooty).
        """
    def event_info(self, virtual=None):
        """
        Return a list of all virtual events or the information
                about the SEQUENCE bound to the virtual event VIRTUAL.
        """
    def image_names(self):
        """
        Return a list of all existing image names.
        """
    def image_types(self):
        """
        Return a list of all available image types (e.g. photo bitmap).
        """
def CallWrapper:
    """
    Internal class. Stores function to call when some user
        defined Tcl function is called e.g. after an event occurred.
    """
    def __init__(self, func, subst, widget):
        """
        Store FUNC, SUBST and WIDGET as members.
        """
    def __call__(self, *args):
        """
        Apply first function SUBST to arguments, than FUNC.
        """
def XView:
    """
    Mix-in class for querying and changing the horizontal position
        of a widget's window.
    """
    def xview(self, *args):
        """
        Query and change the horizontal position of the view.
        """
    def xview_moveto(self, fraction):
        """
        Adjusts the view in the window so that FRACTION of the
                total width of the canvas is off-screen to the left.
        """
    def xview_scroll(self, number, what):
        """
        Shift the x-view according to NUMBER which is measured in "units"
                or "pages" (WHAT).
        """
def YView:
    """
    Mix-in class for querying and changing the vertical position
        of a widget's window.
    """
    def yview(self, *args):
        """
        Query and change the vertical position of the view.
        """
    def yview_moveto(self, fraction):
        """
        Adjusts the view in the window so that FRACTION of the
                total height of the canvas is off-screen to the top.
        """
    def yview_scroll(self, number, what):
        """
        Shift the y-view according to NUMBER which is measured in
                "units" or "pages" (WHAT).
        """
def Wm:
    """
    Provides functions for the communication with the window manager.
    """
2021-03-02 20:53:36,045 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:36,046 : INFO : tokenize_signature : --> do i ever get here?
    def wm_aspect(self,
              minNumer=None, minDenom=None,
              maxNumer=None, maxDenom=None):
        """
        Instruct the window manager to set the aspect ratio (width/height)
                of this widget to be between MINNUMER/MINDENOM and MAXNUMER/MAXDENOM. Return a tuple
                of the actual values if no argument is given.
        """
    def wm_attributes(self, *args):
        """
        This subcommand returns or sets platform specific attributes

                The first form returns a list of the platform specific flags and
                their values. The second form returns the value for the specific
                option. The third form sets one or more of the values. The values
                are as follows:

                On Windows, -disabled gets or sets whether the window is in a
                disabled state. -toolwindow gets or sets the style of the window
                to toolwindow (as defined in the MSDN). -topmost gets or sets
                whether this is a topmost window (displays above all other
                windows).

                On Macintosh, XXXXX

                On Unix, there are currently no special attribute values.
        
        """
    def wm_client(self, name=None):
        """
        Store NAME in WM_CLIENT_MACHINE property of this widget. Return
                current value.
        """
    def wm_colormapwindows(self, *wlist):
        """
        Store list of window names (WLIST) into WM_COLORMAPWINDOWS property
                of this widget. This list contains windows whose colormaps differ from their
                parents. Return current list of widgets if WLIST is empty.
        """
    def wm_command(self, value=None):
        """
        Store VALUE in WM_COMMAND property. It is the command
                which shall be used to invoke the application. Return current
                command if VALUE is None.
        """
    def wm_deiconify(self):
        """
        Deiconify this widget. If it was never mapped it will not be mapped.
                On Windows it will raise this widget and give it the focus.
        """
    def wm_focusmodel(self, model=None):
        """
        Set focus model to MODEL. "active" means that this widget will claim
                the focus itself, "passive" means that the window manager shall give
                the focus. Return current focus model if MODEL is None.
        """
    def wm_forget(self, window): # new in Tk 8.5
        """
         new in Tk 8.5
        """
    def wm_frame(self):
        """
        Return identifier for decorative frame of this widget if present.
        """
    def wm_geometry(self, newGeometry=None):
        """
        Set geometry to NEWGEOMETRY of the form =widthxheight+x+y. Return
                current value if None is given.
        """
2021-03-02 20:53:36,048 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:36,048 : INFO : tokenize_signature : --> do i ever get here?
    def wm_grid(self,
         baseWidth=None, baseHeight=None,
         widthInc=None, heightInc=None):
        """
        Instruct the window manager that this widget shall only be
                resized on grid boundaries. WIDTHINC and HEIGHTINC are the width and
                height of a grid unit in pixels. BASEWIDTH and BASEHEIGHT are the
                number of grid units requested in Tk_GeometryRequest.
        """
    def wm_group(self, pathName=None):
        """
        Set the group leader widgets for related widgets to PATHNAME. Return
                the group leader of this widget if None is given.
        """
    def wm_iconbitmap(self, bitmap=None, default=None):
        """
        Set bitmap for the iconified widget to BITMAP. Return
                the bitmap if None is given.

                Under Windows, the DEFAULT parameter can be used to set the icon
                for the widget and any descendents that don't have an icon set
                explicitly.  DEFAULT can be the relative path to a .ico file
                (example: root.iconbitmap(default='myicon.ico') ).  See Tk
                documentation for more information.
        """
    def wm_iconify(self):
        """
        Display widget as icon.
        """
    def wm_iconmask(self, bitmap=None):
        """
        Set mask for the icon bitmap of this widget. Return the
                mask if None is given.
        """
    def wm_iconname(self, newName=None):
        """
        Set the name of the icon for this widget. Return the name if
                None is given.
        """
    def wm_iconphoto(self, default=False, *args): # new in Tk 8.5
        """
         new in Tk 8.5
        """
    def wm_iconposition(self, x=None, y=None):
        """
        Set the position of the icon of this widget to X and Y. Return
                a tuple of the current values of X and X if None is given.
        """
    def wm_iconwindow(self, pathName=None):
        """
        Set widget PATHNAME to be displayed instead of icon. Return the current
                value if None is given.
        """
    def wm_manage(self, widget): # new in Tk 8.5
        """
         new in Tk 8.5
        """
    def wm_maxsize(self, width=None, height=None):
        """
        Set max WIDTH and HEIGHT for this widget. If the window is gridded
                the values are given in grid units. Return the current values if None
                is given.
        """
    def wm_minsize(self, width=None, height=None):
        """
        Set min WIDTH and HEIGHT for this widget. If the window is gridded
                the values are given in grid units. Return the current values if None
                is given.
        """
    def wm_overrideredirect(self, boolean=None):
        """
        Instruct the window manager to ignore this widget
                if BOOLEAN is given with 1. Return the current value if None
                is given.
        """
    def wm_positionfrom(self, who=None):
        """
        Instruct the window manager that the position of this widget shall
                be defined by the user if WHO is "user", and by its own policy if WHO is
                "program".
        """
    def wm_protocol(self, name=None, func=None):
        """
        Bind function FUNC to command NAME for this widget.
                Return the function bound to NAME if None is given. NAME could be
                e.g. "WM_SAVE_YOURSELF" or "WM_DELETE_WINDOW".
        """
    def wm_resizable(self, width=None, height=None):
        """
        Instruct the window manager whether this width can be resized
                in WIDTH or HEIGHT. Both values are boolean values.
        """
    def wm_sizefrom(self, who=None):
        """
        Instruct the window manager that the size of this widget shall
                be defined by the user if WHO is "user", and by its own policy if WHO is
                "program".
        """
    def wm_state(self, newstate=None):
        """
        Query or set the state of this widget as one of normal, icon,
                iconic (see wm_iconwindow), withdrawn, or zoomed (Windows only).
        """
    def wm_title(self, string=None):
        """
        Set the title of this widget.
        """
    def wm_transient(self, master=None):
        """
        Instruct the window manager that this widget is transient
                with regard to widget MASTER.
        """
    def wm_withdraw(self):
        """
        Withdraw this widget from the screen such that it is unmapped
                and forgotten by the window manager. Re-draw it with wm_deiconify.
        """
def Tk(Misc, Wm):
    """
    Toplevel widget of Tk which represents mostly the main window
        of an application. It has an associated Tcl interpreter.
    """
2021-03-02 20:53:36,052 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, screenName=None, baseName=None, className='Tk',
                 useTk=1, sync=0, use=None):
        """
        Return a new Toplevel widget on screen SCREENNAME. A new Tcl interpreter will
                be created. BASENAME will be used for the identification of the profile file (see
                readprofile).
                It is constructed from sys.argv[0] without extensions if None is given. CLASSNAME
                is the name of the widget class.
        """
    def loadtk(self):
        """
         Version sanity checks

        """
    def destroy(self):
        """
        Destroy this and all descendants widgets. This will
                end the application of this Tcl interpreter.
        """
    def readprofile(self, baseName, className):
        """
        Internal function. It reads BASENAME.tcl and CLASSNAME.tcl into
                the Tcl Interpreter and calls exec on the contents of BASENAME.py and
                CLASSNAME.py if such a file exists in the home directory.
        """
    def report_callback_exception(self, exc, val, tb):
        """
        Report callback exception on sys.stderr.

                Applications may want to override this internal function, and
                should when sys.stderr is None.
        """
    def __getattr__(self, attr):
        """
        Delegate attribute access to the interpreter object
        """
def Tcl(screenName=None, baseName=None, className='Tk', useTk=0):
    """
    Geometry manager Pack.

        Base class to use the methods pack_* in every widget.
    """
    def pack_configure(self, cnf={}, **kw):
        """
        Pack a widget in the parent widget. Use as options:
                after=widget - pack it after you have packed widget
                anchor=NSEW (or subset) - position widget according to
                                          given direction
                before=widget - pack it before you will pack widget
                expand=bool - expand widget if parent size grows
                fill=NONE or X or Y or BOTH - fill widget if widget grows
                in=master - use master to contain this widget
                in_=master - see 'in' option description
                ipadx=amount - add internal padding in x direction
                ipady=amount - add internal padding in y direction
                padx=amount - add padding in x direction
                pady=amount - add padding in y direction
                side=TOP or BOTTOM or LEFT or RIGHT -  where to add this widget.
        
        """
    def pack_forget(self):
        """
        Unmap this widget and do not use it for the packing order.
        """
    def pack_info(self):
        """
        Return information about the packing options
                for this widget.
        """
def Place:
    """
    Geometry manager Place.

        Base class to use the methods place_* in every widget.
    """
    def place_configure(self, cnf={}, **kw):
        """
        Place a widget in the parent widget. Use as options:
                in=master - master relative to which the widget is placed
                in_=master - see 'in' option description
                x=amount - locate anchor of this widget at position x of master
                y=amount - locate anchor of this widget at position y of master
                relx=amount - locate anchor of this widget between 0.0 and 1.0
                              relative to width of master (1.0 is right edge)
                rely=amount - locate anchor of this widget between 0.0 and 1.0
                              relative to height of master (1.0 is bottom edge)
                anchor=NSEW (or subset) - position anchor according to given direction
                width=amount - width of this widget in pixel
                height=amount - height of this widget in pixel
                relwidth=amount - width of this widget between 0.0 and 1.0
                                  relative to width of master (1.0 is the same width
                                  as the master)
                relheight=amount - height of this widget between 0.0 and 1.0
                                   relative to height of master (1.0 is the same
                                   height as the master)
                bordermode="inside" or "outside" - whether to take border width of
                                                   master widget into account
        
        """
    def place_forget(self):
        """
        Unmap this widget.
        """
    def place_info(self):
        """
        Return information about the placing options
                for this widget.
        """
def Grid:
    """
    Geometry manager Grid.

        Base class to use the methods grid_* in every widget.
    """
    def grid_configure(self, cnf={}, **kw):
        """
        Position a widget in the parent widget in a grid. Use as options:
                column=number - use cell identified with given column (starting with 0)
                columnspan=number - this widget will span several columns
                in=master - use master to contain this widget
                in_=master - see 'in' option description
                ipadx=amount - add internal padding in x direction
                ipady=amount - add internal padding in y direction
                padx=amount - add padding in x direction
                pady=amount - add padding in y direction
                row=number - use cell identified with given row (starting with 0)
                rowspan=number - this widget will span several rows
                sticky=NSEW - if cell is larger on which sides will this
                              widget stick to the cell boundary
        
        """
    def grid_forget(self):
        """
        Unmap this widget.
        """
    def grid_remove(self):
        """
        Unmap this widget but remember the grid options.
        """
    def grid_info(self):
        """
        Return information about the options
                for positioning this widget in a grid.
        """
def BaseWidget(Misc):
    """
    Internal class.
    """
    def _setup(self, master, cnf):
        """
        Internal function. Sets up information about children.
        """
    def __init__(self, master, widgetName, cnf={}, kw={}, extra=()):
        """
        Construct a widget with the parent widget MASTER, a name WIDGETNAME
                and appropriate options.
        """
    def destroy(self):
        """
        Destroy this and all descendants widgets.
        """
    def _do(self, name, args=()):
        """
         XXX Obsolete -- better use self.tk.call directly!

        """
def Widget(BaseWidget, Pack, Place, Grid):
    """
    Internal class.

        Base class for a widget which can be positioned with the geometry managers
        Pack, Place or Grid.
    """
def Toplevel(BaseWidget, Wm):
    """
    Toplevel widget, e.g. for dialogs.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a toplevel widget with the parent MASTER.

                Valid resource names: background, bd, bg, borderwidth, class,
                colormap, container, cursor, height, highlightbackground,
                highlightcolor, highlightthickness, menu, relief, screen, takefocus,
                use, visual, width.
        """
def Button(Widget):
    """
    Button widget.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a button widget with the parent MASTER.

                STANDARD OPTIONS

                    activebackground, activeforeground, anchor,
                    background, bitmap, borderwidth, cursor,
                    disabledforeground, font, foreground
                    highlightbackground, highlightcolor,
                    highlightthickness, image, justify,
                    padx, pady, relief, repeatdelay,
                    repeatinterval, takefocus, text,
                    textvariable, underline, wraplength

                WIDGET-SPECIFIC OPTIONS

                    command, compound, default, height,
                    overrelief, state, width
        
        """
    def flash(self):
        """
        Flash the button.

                This is accomplished by redisplaying
                the button several times, alternating between active and
                normal colors. At the end of the flash the button is left
                in the same normal/active state as when the command was
                invoked. This command is ignored if the button's state is
                disabled.
        
        """
    def invoke(self):
        """
        Invoke the command associated with the button.

                The return value is the return value from the command,
                or an empty string if there is no command associated with
                the button. This command is ignored if the button's state
                is disabled.
        
        """
def Canvas(Widget, XView, YView):
    """
    Canvas widget to display graphical elements like lines or text.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a canvas widget with the parent MASTER.

                Valid resource names: background, bd, bg, borderwidth, closeenough,
                confine, cursor, height, highlightbackground, highlightcolor,
                highlightthickness, insertbackground, insertborderwidth,
                insertofftime, insertontime, insertwidth, offset, relief,
                scrollregion, selectbackground, selectborderwidth, selectforeground,
                state, takefocus, width, xscrollcommand, xscrollincrement,
                yscrollcommand, yscrollincrement.
        """
    def addtag(self, *args):
        """
        Internal function.
        """
    def addtag_above(self, newtag, tagOrId):
        """
        Add tag NEWTAG to all items above TAGORID.
        """
    def addtag_all(self, newtag):
        """
        Add tag NEWTAG to all items.
        """
    def addtag_below(self, newtag, tagOrId):
        """
        Add tag NEWTAG to all items below TAGORID.
        """
    def addtag_closest(self, newtag, x, y, halo=None, start=None):
        """
        Add tag NEWTAG to item which is closest to pixel at X, Y.
                If several match take the top-most.
                All items closer than HALO are considered overlapping (all are
                closests). If START is specified the next below this tag is taken.
        """
    def addtag_enclosed(self, newtag, x1, y1, x2, y2):
        """
        Add tag NEWTAG to all items in the rectangle defined
                by X1,Y1,X2,Y2.
        """
    def addtag_overlapping(self, newtag, x1, y1, x2, y2):
        """
        Add tag NEWTAG to all items which overlap the rectangle
                defined by X1,Y1,X2,Y2.
        """
    def addtag_withtag(self, newtag, tagOrId):
        """
        Add tag NEWTAG to all items with TAGORID.
        """
    def bbox(self, *args):
        """
        Return a tuple of X1,Y1,X2,Y2 coordinates for a rectangle
                which encloses all items with tags specified as arguments.
        """
    def tag_unbind(self, tagOrId, sequence, funcid=None):
        """
        Unbind for all items with TAGORID for event SEQUENCE  the
                function identified with FUNCID.
        """
    def tag_bind(self, tagOrId, sequence=None, func=None, add=None):
        """
        Bind to all items with TAGORID at event SEQUENCE a call to function FUNC.

                An additional boolean parameter ADD specifies whether FUNC will be
                called additionally to the other bound function or whether it will
                replace the previous function. See bind for the return value.
        """
    def canvasx(self, screenx, gridspacing=None):
        """
        Return the canvas x coordinate of pixel position SCREENX rounded
                to nearest multiple of GRIDSPACING units.
        """
    def canvasy(self, screeny, gridspacing=None):
        """
        Return the canvas y coordinate of pixel position SCREENY rounded
                to nearest multiple of GRIDSPACING units.
        """
    def coords(self, *args):
        """
        Return a list of coordinates for the item given in ARGS.
        """
    def _create(self, itemType, args, kw): # Args: (val, val, ..., cnf={})
        """
         Args: (val, val, ..., cnf={})
        """
    def create_arc(self, *args, **kw):
        """
        Create arc shaped region with coordinates x1,y1,x2,y2.
        """
    def create_bitmap(self, *args, **kw):
        """
        Create bitmap with coordinates x1,y1.
        """
    def create_image(self, *args, **kw):
        """
        Create image item with coordinates x1,y1.
        """
    def create_line(self, *args, **kw):
        """
        Create line with coordinates x1,y1,...,xn,yn.
        """
    def create_oval(self, *args, **kw):
        """
        Create oval with coordinates x1,y1,x2,y2.
        """
    def create_polygon(self, *args, **kw):
        """
        Create polygon with coordinates x1,y1,...,xn,yn.
        """
    def create_rectangle(self, *args, **kw):
        """
        Create rectangle with coordinates x1,y1,x2,y2.
        """
    def create_text(self, *args, **kw):
        """
        Create text with coordinates x1,y1.
        """
    def create_window(self, *args, **kw):
        """
        Create window with coordinates x1,y1,x2,y2.
        """
    def dchars(self, *args):
        """
        Delete characters of text items identified by tag or id in ARGS (possibly
                several times) from FIRST to LAST character (including).
        """
    def delete(self, *args):
        """
        Delete items identified by all tag or ids contained in ARGS.
        """
    def dtag(self, *args):
        """
        Delete tag or id given as last arguments in ARGS from items
                identified by first argument in ARGS.
        """
    def find(self, *args):
        """
        Internal function.
        """
    def find_above(self, tagOrId):
        """
        Return items above TAGORID.
        """
    def find_all(self):
        """
        Return all items.
        """
    def find_below(self, tagOrId):
        """
        Return all items below TAGORID.
        """
    def find_closest(self, x, y, halo=None, start=None):
        """
        Return item which is closest to pixel at X, Y.
                If several match take the top-most.
                All items closer than HALO are considered overlapping (all are
                closest). If START is specified the next below this tag is taken.
        """
    def find_enclosed(self, x1, y1, x2, y2):
        """
        Return all items in rectangle defined
                by X1,Y1,X2,Y2.
        """
    def find_overlapping(self, x1, y1, x2, y2):
        """
        Return all items which overlap the rectangle
                defined by X1,Y1,X2,Y2.
        """
    def find_withtag(self, tagOrId):
        """
        Return all items with TAGORID.
        """
    def focus(self, *args):
        """
        Set focus to the first item specified in ARGS.
        """
    def gettags(self, *args):
        """
        Return tags associated with the first item specified in ARGS.
        """
    def icursor(self, *args):
        """
        Set cursor at position POS in the item identified by TAGORID.
                In ARGS TAGORID must be first.
        """
    def index(self, *args):
        """
        Return position of cursor as integer in item specified in ARGS.
        """
    def insert(self, *args):
        """
        Insert TEXT in item TAGORID at position POS. ARGS must
                be TAGORID POS TEXT.
        """
    def itemcget(self, tagOrId, option):
        """
        Return the resource value for an OPTION for item TAGORID.
        """
    def itemconfigure(self, tagOrId, cnf=None, **kw):
        """
        Configure resources of an item TAGORID.

                The values for resources are specified as keyword
                arguments. To get an overview about
                the allowed keyword arguments call the method without arguments.
        
        """
    def tag_lower(self, *args):
        """
        Lower an item TAGORID given in ARGS
                (optional below another item).
        """
    def move(self, *args):
        """
        Move an item TAGORID given in ARGS.
        """
    def moveto(self, tagOrId, x='', y=''):
        """
        Move the items given by TAGORID in the canvas coordinate
                space so that the first coordinate pair of the bottommost
                item with tag TAGORID is located at position (X,Y).
                X and Y may be the empty string, in which case the
                corresponding coordinate will be unchanged. All items matching
                TAGORID remain in the same positions relative to each other.
        """
    def postscript(self, cnf={}, **kw):
        """
        Print the contents of the canvas to a postscript
                file. Valid options: colormap, colormode, file, fontmap,
                height, pageanchor, pageheight, pagewidth, pagex, pagey,
                rotate, width, x, y.
        """
    def tag_raise(self, *args):
        """
        Raise an item TAGORID given in ARGS
                (optional above another item).
        """
    def scale(self, *args):
        """
        Scale item TAGORID with XORIGIN, YORIGIN, XSCALE, YSCALE.
        """
    def scan_mark(self, x, y):
        """
        Remember the current X, Y coordinates.
        """
    def scan_dragto(self, x, y, gain=10):
        """
        Adjust the view of the canvas to GAIN times the
                difference between X and Y and the coordinates given in
                scan_mark.
        """
    def select_adjust(self, tagOrId, index):
        """
        Adjust the end of the selection near the cursor of an item TAGORID to index.
        """
    def select_clear(self):
        """
        Clear the selection if it is in this widget.
        """
    def select_from(self, tagOrId, index):
        """
        Set the fixed end of a selection in item TAGORID to INDEX.
        """
    def select_item(self):
        """
        Return the item which has the selection.
        """
    def select_to(self, tagOrId, index):
        """
        Set the variable end of a selection in item TAGORID to INDEX.
        """
    def type(self, tagOrId):
        """
        Return the type of the item TAGORID.
        """
def Checkbutton(Widget):
    """
    Checkbutton widget which is either in on- or off-state.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a checkbutton widget with the parent MASTER.

                Valid resource names: activebackground, activeforeground, anchor,
                background, bd, bg, bitmap, borderwidth, command, cursor,
                disabledforeground, fg, font, foreground, height,
                highlightbackground, highlightcolor, highlightthickness, image,
                indicatoron, justify, offvalue, onvalue, padx, pady, relief,
                selectcolor, selectimage, state, takefocus, text, textvariable,
                underline, variable, width, wraplength.
        """
    def deselect(self):
        """
        Put the button in off-state.
        """
    def flash(self):
        """
        Flash the button.
        """
    def invoke(self):
        """
        Toggle the button and invoke a command if given as resource.
        """
    def select(self):
        """
        Put the button in on-state.
        """
    def toggle(self):
        """
        Toggle the button.
        """
def Entry(Widget, XView):
    """
    Entry widget which allows displaying simple text.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct an entry widget with the parent MASTER.

                Valid resource names: background, bd, bg, borderwidth, cursor,
                exportselection, fg, font, foreground, highlightbackground,
                highlightcolor, highlightthickness, insertbackground,
                insertborderwidth, insertofftime, insertontime, insertwidth,
                invalidcommand, invcmd, justify, relief, selectbackground,
                selectborderwidth, selectforeground, show, state, takefocus,
                textvariable, validate, validatecommand, vcmd, width,
                xscrollcommand.
        """
    def delete(self, first, last=None):
        """
        Delete text from FIRST to LAST (not included).
        """
    def get(self):
        """
        Return the text.
        """
    def icursor(self, index):
        """
        Insert cursor at INDEX.
        """
    def index(self, index):
        """
        Return position of cursor.
        """
    def insert(self, index, string):
        """
        Insert STRING at INDEX.
        """
    def scan_mark(self, x):
        """
        Remember the current X, Y coordinates.
        """
    def scan_dragto(self, x):
        """
        Adjust the view of the canvas to 10 times the
                difference between X and Y and the coordinates given in
                scan_mark.
        """
    def selection_adjust(self, index):
        """
        Adjust the end of the selection near the cursor to INDEX.
        """
    def selection_clear(self):
        """
        Clear the selection if it is in this widget.
        """
    def selection_from(self, index):
        """
        Set the fixed end of a selection to INDEX.
        """
    def selection_present(self):
        """
        Return True if there are characters selected in the entry, False
                otherwise.
        """
    def selection_range(self, start, end):
        """
        Set the selection from START to END (not included).
        """
    def selection_to(self, index):
        """
        Set the variable end of a selection to INDEX.
        """
def Frame(Widget):
    """
    Frame widget which may contain other widgets and can have a 3D border.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a frame widget with the parent MASTER.

                Valid resource names: background, bd, bg, borderwidth, class,
                colormap, container, cursor, height, highlightbackground,
                highlightcolor, highlightthickness, relief, takefocus, visual, width.
        """
def Label(Widget):
    """
    Label widget which can display text and bitmaps.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a label widget with the parent MASTER.

                STANDARD OPTIONS

                    activebackground, activeforeground, anchor,
                    background, bitmap, borderwidth, cursor,
                    disabledforeground, font, foreground,
                    highlightbackground, highlightcolor,
                    highlightthickness, image, justify,
                    padx, pady, relief, takefocus, text,
                    textvariable, underline, wraplength

                WIDGET-SPECIFIC OPTIONS

                    height, state, width

        
        """
def Listbox(Widget, XView, YView):
    """
    Listbox widget which can display a list of strings.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a listbox widget with the parent MASTER.

                Valid resource names: background, bd, bg, borderwidth, cursor,
                exportselection, fg, font, foreground, height, highlightbackground,
                highlightcolor, highlightthickness, relief, selectbackground,
                selectborderwidth, selectforeground, selectmode, setgrid, takefocus,
                width, xscrollcommand, yscrollcommand, listvariable.
        """
    def activate(self, index):
        """
        Activate item identified by INDEX.
        """
    def bbox(self, index):
        """
        Return a tuple of X1,Y1,X2,Y2 coordinates for a rectangle
                which encloses the item identified by the given index.
        """
    def curselection(self):
        """
        Return the indices of currently selected item.
        """
    def delete(self, first, last=None):
        """
        Delete items from FIRST to LAST (included).
        """
    def get(self, first, last=None):
        """
        Get list of items from FIRST to LAST (included).
        """
    def index(self, index):
        """
        Return index of item identified with INDEX.
        """
    def insert(self, index, *elements):
        """
        Insert ELEMENTS at INDEX.
        """
    def nearest(self, y):
        """
        Get index of item which is nearest to y coordinate Y.
        """
    def scan_mark(self, x, y):
        """
        Remember the current X, Y coordinates.
        """
    def scan_dragto(self, x, y):
        """
        Adjust the view of the listbox to 10 times the
                difference between X and Y and the coordinates given in
                scan_mark.
        """
    def see(self, index):
        """
        Scroll such that INDEX is visible.
        """
    def selection_anchor(self, index):
        """
        Set the fixed end oft the selection to INDEX.
        """
    def selection_clear(self, first, last=None):
        """
        Clear the selection from FIRST to LAST (included).
        """
    def selection_includes(self, index):
        """
        Return True if INDEX is part of the selection.
        """
    def selection_set(self, first, last=None):
        """
        Set the selection from FIRST to LAST (included) without
                changing the currently selected elements.
        """
    def size(self):
        """
        Return the number of elements in the listbox.
        """
    def itemcget(self, index, option):
        """
        Return the resource value for an ITEM and an OPTION.
        """
    def itemconfigure(self, index, cnf=None, **kw):
        """
        Configure resources of an ITEM.

                The values for resources are specified as keyword arguments.
                To get an overview about the allowed keyword arguments
                call the method without arguments.
                Valid resource names: background, bg, foreground, fg,
                selectbackground, selectforeground.
        """
def Menu(Widget):
    """
    Menu widget which allows displaying menu bars, pull-down menus and pop-up menus.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct menu widget with the parent MASTER.

                Valid resource names: activebackground, activeborderwidth,
                activeforeground, background, bd, bg, borderwidth, cursor,
                disabledforeground, fg, font, foreground, postcommand, relief,
                selectcolor, takefocus, tearoff, tearoffcommand, title, type.
        """
    def tk_popup(self, x, y, entry=""):
        """
        Post the menu at position X,Y with entry ENTRY.
        """
    def activate(self, index):
        """
        Activate entry at INDEX.
        """
    def add(self, itemType, cnf={}, **kw):
        """
        Internal function.
        """
    def add_cascade(self, cnf={}, **kw):
        """
        Add hierarchical menu item.
        """
    def add_checkbutton(self, cnf={}, **kw):
        """
        Add checkbutton menu item.
        """
    def add_command(self, cnf={}, **kw):
        """
        Add command menu item.
        """
    def add_radiobutton(self, cnf={}, **kw):
        """
        Addd radio menu item.
        """
    def add_separator(self, cnf={}, **kw):
        """
        Add separator.
        """
    def insert(self, index, itemType, cnf={}, **kw):
        """
        Internal function.
        """
    def insert_cascade(self, index, cnf={}, **kw):
        """
        Add hierarchical menu item at INDEX.
        """
    def insert_checkbutton(self, index, cnf={}, **kw):
        """
        Add checkbutton menu item at INDEX.
        """
    def insert_command(self, index, cnf={}, **kw):
        """
        Add command menu item at INDEX.
        """
    def insert_radiobutton(self, index, cnf={}, **kw):
        """
        Addd radio menu item at INDEX.
        """
    def insert_separator(self, index, cnf={}, **kw):
        """
        Add separator at INDEX.
        """
    def delete(self, index1, index2=None):
        """
        Delete menu items between INDEX1 and INDEX2 (included).
        """
    def entrycget(self, index, option):
        """
        Return the resource value of a menu item for OPTION at INDEX.
        """
    def entryconfigure(self, index, cnf=None, **kw):
        """
        Configure a menu item at INDEX.
        """
    def index(self, index):
        """
        Return the index of a menu item identified by INDEX.
        """
    def invoke(self, index):
        """
        Invoke a menu item identified by INDEX and execute
                the associated command.
        """
    def post(self, x, y):
        """
        Display a menu at position X,Y.
        """
    def type(self, index):
        """
        Return the type of the menu item at INDEX.
        """
    def unpost(self):
        """
        Unmap a menu.
        """
    def xposition(self, index): # new in Tk 8.5
        """
         new in Tk 8.5
        """
    def yposition(self, index):
        """
        Return the y-position of the topmost pixel of the menu item at INDEX.
        """
def Menubutton(Widget):
    """
    Menubutton widget, obsolete since Tk8.0.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        'menubutton'
        """
def Message(Widget):
    """
    Message widget to display multiline text. Obsolete since Label does it too.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        'message'
        """
def Radiobutton(Widget):
    """
    Radiobutton widget which shows only one of several buttons in on-state.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a radiobutton widget with the parent MASTER.

                Valid resource names: activebackground, activeforeground, anchor,
                background, bd, bg, bitmap, borderwidth, command, cursor,
                disabledforeground, fg, font, foreground, height,
                highlightbackground, highlightcolor, highlightthickness, image,
                indicatoron, justify, padx, pady, relief, selectcolor, selectimage,
                state, takefocus, text, textvariable, underline, value, variable,
                width, wraplength.
        """
    def deselect(self):
        """
        Put the button in off-state.
        """
    def flash(self):
        """
        Flash the button.
        """
    def invoke(self):
        """
        Toggle the button and invoke a command if given as resource.
        """
    def select(self):
        """
        Put the button in on-state.
        """
def Scale(Widget):
    """
    Scale widget which can display a numerical scale.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a scale widget with the parent MASTER.

                Valid resource names: activebackground, background, bigincrement, bd,
                bg, borderwidth, command, cursor, digits, fg, font, foreground, from,
                highlightbackground, highlightcolor, highlightthickness, label,
                length, orient, relief, repeatdelay, repeatinterval, resolution,
                showvalue, sliderlength, sliderrelief, state, takefocus,
                tickinterval, to, troughcolor, variable, width.
        """
    def get(self):
        """
        Get the current value as integer or float.
        """
    def set(self, value):
        """
        Set the value to VALUE.
        """
    def coords(self, value=None):
        """
        Return a tuple (X,Y) of the point along the centerline of the
                trough that corresponds to VALUE or the current value if None is
                given.
        """
    def identify(self, x, y):
        """
        Return where the point X,Y lies. Valid return values are "slider",
                "though1" and "though2".
        """
def Scrollbar(Widget):
    """
    Scrollbar widget which displays a slider at a certain position.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a scrollbar widget with the parent MASTER.

                Valid resource names: activebackground, activerelief,
                background, bd, bg, borderwidth, command, cursor,
                elementborderwidth, highlightbackground,
                highlightcolor, highlightthickness, jump, orient,
                relief, repeatdelay, repeatinterval, takefocus,
                troughcolor, width.
        """
    def activate(self, index=None):
        """
        Marks the element indicated by index as active.
                The only index values understood by this method are "arrow1",
                "slider", or "arrow2".  If any other value is specified then no
                element of the scrollbar will be active.  If index is not specified,
                the method returns the name of the element that is currently active,
                or None if no element is active.
        """
    def delta(self, deltax, deltay):
        """
        Return the fractional change of the scrollbar setting if it
                would be moved by DELTAX or DELTAY pixels.
        """
    def fraction(self, x, y):
        """
        Return the fractional value which corresponds to a slider
                position of X,Y.
        """
    def identify(self, x, y):
        """
        Return the element under position X,Y as one of
                "arrow1","slider","arrow2" or "".
        """
    def get(self):
        """
        Return the current fractional values (upper and lower end)
                of the slider position.
        """
    def set(self, first, last):
        """
        Set the fractional values of the slider position (upper and
                lower ends as value between 0 and 1).
        """
def Text(Widget, XView, YView):
    """
    Text widget which can display text in various forms.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a text widget with the parent MASTER.

                STANDARD OPTIONS

                    background, borderwidth, cursor,
                    exportselection, font, foreground,
                    highlightbackground, highlightcolor,
                    highlightthickness, insertbackground,
                    insertborderwidth, insertofftime,
                    insertontime, insertwidth, padx, pady,
                    relief, selectbackground,
                    selectborderwidth, selectforeground,
                    setgrid, takefocus,
                    xscrollcommand, yscrollcommand,

                WIDGET-SPECIFIC OPTIONS

                    autoseparators, height, maxundo,
                    spacing1, spacing2, spacing3,
                    state, tabs, undo, width, wrap,

        
        """
    def bbox(self, index):
        """
        Return a tuple of (x,y,width,height) which gives the bounding
                box of the visible part of the character at the given index.
        """
    def compare(self, index1, op, index2):
        """
        Return whether between index INDEX1 and index INDEX2 the
                relation OP is satisfied. OP is one of <, <=, ==, >=, >, or !=.
        """
    def count(self, index1, index2, *args): # new in Tk 8.5
        """
         new in Tk 8.5
        """
    def debug(self, boolean=None):
        """
        Turn on the internal consistency checks of the B-Tree inside the text
                widget according to BOOLEAN.
        """
    def delete(self, index1, index2=None):
        """
        Delete the characters between INDEX1 and INDEX2 (not included).
        """
    def dlineinfo(self, index):
        """
        Return tuple (x,y,width,height,baseline) giving the bounding box
                and baseline position of the visible part of the line containing
                the character at INDEX.
        """
    def dump(self, index1, index2=None, command=None, **kw):
        """
        Return the contents of the widget between index1 and index2.

                The type of contents returned in filtered based on the keyword
                parameters; if 'all', 'image', 'mark', 'tag', 'text', or 'window' are
                given and true, then the corresponding items are returned. The result
                is a list of triples of the form (key, value, index). If none of the
                keywords are true then 'all' is used by default.

                If the 'command' argument is given, it is called once for each element
                of the list of triples, with the values of each triple serving as the
                arguments to the function. In this case the list is not returned.
        """
            def append_triple(key, value, index, result=result):
                """
                -command
                """
    def edit(self, *args):
        """
        Internal method

                This method controls the undo mechanism and
                the modified flag. The exact behavior of the
                command depends on the option argument that
                follows the edit argument. The following forms
                of the command are currently supported:

                edit_modified, edit_redo, edit_reset, edit_separator
                and edit_undo

        
        """
    def edit_modified(self, arg=None):
        """
        Get or Set the modified flag

                If arg is not specified, returns the modified
                flag of the widget. The insert, delete, edit undo and
                edit redo commands or the user can set or clear the
                modified flag. If boolean is specified, sets the
                modified flag of the widget to arg.
        
        """
    def edit_redo(self):
        """
        Redo the last undone edit

                When the undo option is true, reapplies the last
                undone edits provided no other edits were done since
                then. Generates an error when the redo stack is empty.
                Does nothing when the undo option is false.
        
        """
    def edit_reset(self):
        """
        Clears the undo and redo stacks
        
        """
    def edit_separator(self):
        """
        Inserts a separator (boundary) on the undo stack.

                Does nothing when the undo option is false
        
        """
    def edit_undo(self):
        """
        Undoes the last edit action

                If the undo option is true. An edit action is defined
                as all the insert and delete commands that are recorded
                on the undo stack in between two separators. Generates
                an error when the undo stack is empty. Does nothing
                when the undo option is false
        
        """
    def get(self, index1, index2=None):
        """
        Return the text from INDEX1 to INDEX2 (not included).
        """
    def image_cget(self, index, option):
        """
        Return the value of OPTION of an embedded image at INDEX.
        """
    def image_configure(self, index, cnf=None, **kw):
        """
        Configure an embedded image at INDEX.
        """
    def image_create(self, index, cnf={}, **kw):
        """
        Create an embedded image at INDEX.
        """
    def image_names(self):
        """
        Return all names of embedded images in this widget.
        """
    def index(self, index):
        """
        Return the index in the form line.char for INDEX.
        """
    def insert(self, index, chars, *args):
        """
        Insert CHARS before the characters at INDEX. An additional
                tag can be given in ARGS. Additional CHARS and tags can follow in ARGS.
        """
    def mark_gravity(self, markName, direction=None):
        """
        Change the gravity of a mark MARKNAME to DIRECTION (LEFT or RIGHT).
                Return the current value if None is given for DIRECTION.
        """
    def mark_names(self):
        """
        Return all mark names.
        """
    def mark_set(self, markName, index):
        """
        Set mark MARKNAME before the character at INDEX.
        """
    def mark_unset(self, *markNames):
        """
        Delete all marks in MARKNAMES.
        """
    def mark_next(self, index):
        """
        Return the name of the next mark after INDEX.
        """
    def mark_previous(self, index):
        """
        Return the name of the previous mark before INDEX.
        """
    def peer_create(self, newPathName, cnf={}, **kw): # new in Tk 8.5
        """
         new in Tk 8.5
        """
    def peer_names(self): # new in Tk 8.5
        """
         new in Tk 8.5
        """
    def replace(self, index1, index2, chars, *args): # new in Tk 8.5
        """
         new in Tk 8.5
        """
    def scan_mark(self, x, y):
        """
        Remember the current X, Y coordinates.
        """
    def scan_dragto(self, x, y):
        """
        Adjust the view of the text to 10 times the
                difference between X and Y and the coordinates given in
                scan_mark.
        """
2021-03-02 20:53:36,091 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:36,091 : INFO : tokenize_signature : --> do i ever get here?
    def search(self, pattern, index, stopindex=None,
           forwards=None, backwards=None, exact=None,
           regexp=None, nocase=None, count=None, elide=None):
        """
        Search PATTERN beginning from INDEX until STOPINDEX.
                Return the index of the first character of a match or an
                empty string.
        """
    def see(self, index):
        """
        Scroll such that the character at INDEX is visible.
        """
    def tag_add(self, tagName, index1, *args):
        """
        Add tag TAGNAME to all characters between INDEX1 and index2 in ARGS.
                Additional pairs of indices may follow in ARGS.
        """
    def tag_unbind(self, tagName, sequence, funcid=None):
        """
        Unbind for all characters with TAGNAME for event SEQUENCE  the
                function identified with FUNCID.
        """
    def tag_bind(self, tagName, sequence, func, add=None):
        """
        Bind to all characters with TAGNAME at event SEQUENCE a call to function FUNC.

                An additional boolean parameter ADD specifies whether FUNC will be
                called additionally to the other bound function or whether it will
                replace the previous function. See bind for the return value.
        """
    def tag_cget(self, tagName, option):
        """
        Return the value of OPTION for tag TAGNAME.
        """
    def tag_configure(self, tagName, cnf=None, **kw):
        """
        Configure a tag TAGNAME.
        """
    def tag_delete(self, *tagNames):
        """
        Delete all tags in TAGNAMES.
        """
    def tag_lower(self, tagName, belowThis=None):
        """
        Change the priority of tag TAGNAME such that it is lower
                than the priority of BELOWTHIS.
        """
    def tag_names(self, index=None):
        """
        Return a list of all tag names.
        """
    def tag_nextrange(self, tagName, index1, index2=None):
        """
        Return a list of start and end index for the first sequence of
                characters between INDEX1 and INDEX2 which all have tag TAGNAME.
                The text is searched forward from INDEX1.
        """
    def tag_prevrange(self, tagName, index1, index2=None):
        """
        Return a list of start and end index for the first sequence of
                characters between INDEX1 and INDEX2 which all have tag TAGNAME.
                The text is searched backwards from INDEX1.
        """
    def tag_raise(self, tagName, aboveThis=None):
        """
        Change the priority of tag TAGNAME such that it is higher
                than the priority of ABOVETHIS.
        """
    def tag_ranges(self, tagName):
        """
        Return a list of ranges of text which have tag TAGNAME.
        """
    def tag_remove(self, tagName, index1, index2=None):
        """
        Remove tag TAGNAME from all characters between INDEX1 and INDEX2.
        """
    def window_cget(self, index, option):
        """
        Return the value of OPTION of an embedded window at INDEX.
        """
    def window_configure(self, index, cnf=None, **kw):
        """
        Configure an embedded window at INDEX.
        """
    def window_create(self, index, cnf={}, **kw):
        """
        Create a window at INDEX.
        """
    def window_names(self):
        """
        Return all names of embedded windows in this widget.
        """
    def yview_pickplace(self, *what):
        """
        Obsolete function, use see.
        """
def _setit:
    """
    Internal class. It wraps the command in the widget OptionMenu.
    """
    def __init__(self, var, value, callback=None):
        """
        OptionMenu which allows the user to select a value from a menu.
        """
    def __init__(self, master, variable, value, *values, **kwargs):
        """
        Construct an optionmenu widget with the parent MASTER, with
                the resource textvariable set to VARIABLE, the initially selected
                value VALUE, the other menu values VALUES and an additional
                keyword argument command.
        """
    def __getitem__(self, name):
        """
        'menu'
        """
    def destroy(self):
        """
        Destroy this widget and the associated menu.
        """
def Image:
    """
    Base class for images.
    """
    def __init__(self, imgtype, name=None, cnf={}, master=None, **kw):
        """
        'Too early to create image'
        """
    def __str__(self): return self.name
        """
        'image'
        """
    def __setitem__(self, key, value):
        """
        'configure'
        """
    def __getitem__(self, key):
        """
        'configure'
        """
    def configure(self, **kw):
        """
        Configure the image.
        """
    def height(self):
        """
        Return the height of the image.
        """
    def type(self):
        """
        Return the type of the image, e.g. "photo" or "bitmap".
        """
    def width(self):
        """
        Return the width of the image.
        """
def PhotoImage(Image):
    """
    Widget which can display images in PGM, PPM, GIF, PNG format.
    """
    def __init__(self, name=None, cnf={}, master=None, **kw):
        """
        Create an image with NAME.

                Valid resource names: data, format, file, gamma, height, palette,
                width.
        """
    def blank(self):
        """
        Display a transparent image.
        """
    def cget(self, option):
        """
        Return the value of OPTION.
        """
    def __getitem__(self, key):
        """
        'cget'
        """
    def copy(self):
        """
        Return a new PhotoImage with the same image as this widget.
        """
    def zoom(self, x, y=''):
        """
        Return a new PhotoImage with the same image as this widget
                but zoom it with a factor of x in the X direction and y in the Y
                direction.  If y is not given, the default value is the same as x.
        
        """
    def subsample(self, x, y=''):
        """
        Return a new PhotoImage based on the same image as this widget
                but use only every Xth or Yth pixel.  If y is not given, the
                default value is the same as x.
        
        """
    def get(self, x, y):
        """
        Return the color (red, green, blue) of the pixel at X,Y.
        """
    def put(self, data, to=None):
        """
        Put row formatted colors to image starting from
                position TO, e.g. image.put("{red green} {blue yellow}", to=(4,6))
        """
    def write(self, filename, format=None, from_coords=None):
        """
        Write image to file FILENAME in FORMAT starting from
                position FROM_COORDS.
        """
    def transparency_get(self, x, y):
        """
        Return True if the pixel at x,y is transparent.
        """
    def transparency_set(self, x, y, boolean):
        """
        Set the transparency of the pixel at x,y.
        """
def BitmapImage(Image):
    """
    Widget which can display images in XBM format.
    """
    def __init__(self, name=None, cnf={}, master=None, **kw):
        """
        Create a bitmap with NAME.

                Valid resource names: background, data, file, foreground, maskdata, maskfile.
        """
def image_names():
    """
    'image'
    """
def image_types():
    """
    'image'
    """
def Spinbox(Widget, XView):
    """
    spinbox widget.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a spinbox widget with the parent MASTER.

                STANDARD OPTIONS

                    activebackground, background, borderwidth,
                    cursor, exportselection, font, foreground,
                    highlightbackground, highlightcolor,
                    highlightthickness, insertbackground,
                    insertborderwidth, insertofftime,
                    insertontime, insertwidth, justify, relief,
                    repeatdelay, repeatinterval,
                    selectbackground, selectborderwidth
                    selectforeground, takefocus, textvariable
                    xscrollcommand.

                WIDGET-SPECIFIC OPTIONS

                    buttonbackground, buttoncursor,
                    buttondownrelief, buttonuprelief,
                    command, disabledbackground,
                    disabledforeground, format, from,
                    invalidcommand, increment,
                    readonlybackground, state, to,
                    validate, validatecommand values,
                    width, wrap,
        
        """
    def bbox(self, index):
        """
        Return a tuple of X1,Y1,X2,Y2 coordinates for a
                rectangle which encloses the character given by index.

                The first two elements of the list give the x and y
                coordinates of the upper-left corner of the screen
                area covered by the character (in pixels relative
                to the widget) and the last two elements give the
                width and height of the character, in pixels. The
                bounding box may refer to a region outside the
                visible area of the window.
        
        """
    def delete(self, first, last=None):
        """
        Delete one or more elements of the spinbox.

                First is the index of the first character to delete,
                and last is the index of the character just after
                the last one to delete. If last isn't specified it
                defaults to first+1, i.e. a single character is
                deleted.  This command returns an empty string.
        
        """
    def get(self):
        """
        Returns the spinbox's string
        """
    def icursor(self, index):
        """
        Alter the position of the insertion cursor.

                The insertion cursor will be displayed just before
                the character given by index. Returns an empty string
        
        """
    def identify(self, x, y):
        """
        Returns the name of the widget at position x, y

                Return value is one of: none, buttondown, buttonup, entry
        
        """
    def index(self, index):
        """
        Returns the numerical index corresponding to index
        
        """
    def insert(self, index, s):
        """
        Insert string s at index

                 Returns an empty string.
        
        """
    def invoke(self, element):
        """
        Causes the specified element to be invoked

                The element could be buttondown or buttonup
                triggering the action associated with it.
        
        """
    def scan(self, *args):
        """
        Internal function.
        """
    def scan_mark(self, x):
        """
        Records x and the current view in the spinbox window;

                used in conjunction with later scan dragto commands.
                Typically this command is associated with a mouse button
                press in the widget. It returns an empty string.
        
        """
    def scan_dragto(self, x):
        """
        Compute the difference between the given x argument
                and the x argument to the last scan mark command

                It then adjusts the view left or right by 10 times the
                difference in x-coordinates. This command is typically
                associated with mouse motion events in the widget, to
                produce the effect of dragging the spinbox at high speed
                through the window. The return value is an empty string.
        
        """
    def selection(self, *args):
        """
        Internal function.
        """
    def selection_adjust(self, index):
        """
        Locate the end of the selection nearest to the character
                given by index,

                Then adjust that end of the selection to be at index
                (i.e including but not going beyond index). The other
                end of the selection is made the anchor point for future
                select to commands. If the selection isn't currently in
                the spinbox, then a new selection is created to include
                the characters between index and the most recent selection
                anchor point, inclusive.
        
        """
    def selection_clear(self):
        """
        Clear the selection

                If the selection isn't in this widget then the
                command has no effect.
        
        """
    def selection_element(self, element=None):
        """
        Sets or gets the currently selected element.

                If a spinbutton element is specified, it will be
                displayed depressed.
        
        """
    def selection_from(self, index):
        """
        Set the fixed end of a selection to INDEX.
        """
    def selection_present(self):
        """
        Return True if there are characters selected in the spinbox, False
                otherwise.
        """
    def selection_range(self, start, end):
        """
        Set the selection from START to END (not included).
        """
    def selection_to(self, index):
        """
        Set the variable end of a selection to INDEX.
        """
def LabelFrame(Widget):
    """
    labelframe widget.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a labelframe widget with the parent MASTER.

                STANDARD OPTIONS

                    borderwidth, cursor, font, foreground,
                    highlightbackground, highlightcolor,
                    highlightthickness, padx, pady, relief,
                    takefocus, text

                WIDGET-SPECIFIC OPTIONS

                    background, class, colormap, container,
                    height, labelanchor, labelwidget,
                    visual, width
        
        """
def PanedWindow(Widget):
    """
    panedwindow widget.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        Construct a panedwindow widget with the parent MASTER.

                STANDARD OPTIONS

                    background, borderwidth, cursor, height,
                    orient, relief, width

                WIDGET-SPECIFIC OPTIONS

                    handlepad, handlesize, opaqueresize,
                    sashcursor, sashpad, sashrelief,
                    sashwidth, showhandle,
        
        """
    def add(self, child, **kw):
        """
        Add a child widget to the panedwindow in a new pane.

                The child argument is the name of the child widget
                followed by pairs of arguments that specify how to
                manage the windows. The possible options and values
                are the ones accepted by the paneconfigure method.
        
        """
    def remove(self, child):
        """
        Remove the pane containing child from the panedwindow

                All geometry management options for child will be forgotten.
        
        """
    def identify(self, x, y):
        """
        Identify the panedwindow component at point x, y

                If the point is over a sash or a sash handle, the result
                is a two element list containing the index of the sash or
                handle, and a word indicating whether it is over a sash
                or a handle, such as {0 sash} or {2 handle}. If the point
                is over any other part of the panedwindow, the result is
                an empty list.
        
        """
    def proxy(self, *args):
        """
        Internal function.
        """
    def proxy_coord(self):
        """
        Return the x and y pair of the most recent proxy location
        
        """
    def proxy_forget(self):
        """
        Remove the proxy from the display.
        
        """
    def proxy_place(self, x, y):
        """
        Place the proxy at the given x and y coordinates.
        
        """
    def sash(self, *args):
        """
        Internal function.
        """
    def sash_coord(self, index):
        """
        Return the current x and y pair for the sash given by index.

                Index must be an integer between 0 and 1 less than the
                number of panes in the panedwindow. The coordinates given are
                those of the top left corner of the region containing the sash.
                pathName sash dragto index x y This command computes the
                difference between the given coordinates and the coordinates
                given to the last sash coord command for the given sash. It then
                moves that sash the computed difference. The return value is the
                empty string.
        
        """
    def sash_mark(self, index):
        """
        Records x and y for the sash given by index;

                Used in conjunction with later dragto commands to move the sash.
        
        """
    def sash_place(self, index, x, y):
        """
        Place the sash given by index at the given coordinates
        
        """
    def panecget(self, child, option):
        """
        Query a management option for window.

                Option may be any value allowed by the paneconfigure subcommand
        
        """
    def paneconfigure(self, tagOrId, cnf=None, **kw):
        """
        Query or modify the management options for window.

                If no option is specified, returns a list describing all
                of the available options for pathName.  If option is
                specified with no value, then the command returns a list
                describing the one named option (this list will be identical
                to the corresponding sublist of the value returned if no
                option is specified). If one or more option-value pairs are
                specified, then the command modifies the given widget
                option(s) to have the given value(s); in this case the
                command returns an empty string. The following options
                are supported:

                after window
                    Insert the window after the window specified. window
                    should be the name of a window already managed by pathName.
                before window
                    Insert the window before the window specified. window
                    should be the name of a window already managed by pathName.
                height size
                    Specify a height for the window. The height will be the
                    outer dimension of the window including its border, if
                    any. If size is an empty string, or if -height is not
                    specified, then the height requested internally by the
                    window will be used initially; the height may later be
                    adjusted by the movement of sashes in the panedwindow.
                    Size may be any value accepted by Tk_GetPixels.
                minsize n
                    Specifies that the size of the window cannot be made
                    less than n. This constraint only affects the size of
                    the widget in the paned dimension -- the x dimension
                    for horizontal panedwindows, the y dimension for
                    vertical panedwindows. May be any value accepted by
                    Tk_GetPixels.
                padx n
                    Specifies a non-negative value indicating how much
                    extra space to leave on each side of the window in
                    the X-direction. The value may have any of the forms
                    accepted by Tk_GetPixels.
                pady n
                    Specifies a non-negative value indicating how much
                    extra space to leave on each side of the window in
                    the Y-direction. The value may have any of the forms
                    accepted by Tk_GetPixels.
                sticky style
                    If a window's pane is larger than the requested
                    dimensions of the window, this option may be used
                    to position (or stretch) the window within its pane.
                    Style is a string that contains zero or more of the
                    characters n, s, e or w. The string can optionally
                    contains spaces or commas, but they are ignored. Each
                    letter refers to a side (north, south, east, or west)
                    that the window will "stick" to. If both n and s
                    (or e and w) are specified, the window will be
                    stretched to fill the entire height (or width) of
                    its cavity.
                width size
                    Specify a width for the window. The width will be
                    the outer dimension of the window including its
                    border, if any. If size is an empty string, or
                    if -width is not specified, then the width requested
                    internally by the window will be used initially; the
                    width may later be adjusted by the movement of sashes
                    in the panedwindow. Size may be any value accepted by
                    Tk_GetPixels.

        
        """
    def panes(self):
        """
        Returns an ordered list of the child panes.
        """
def _test():
    """
    This is Tcl/Tk version %s
    """
