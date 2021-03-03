def _load_tile(master):
    """
    'TILE_LIBRARY'
    """
def _format_optvalue(value, script=False):
    """
    Internal function.
    """
def _format_optdict(optdict, script=False, ignore=None):
    """
    Formats optdict to a tuple to pass it to tk.call.

        E.g. (script=False):
          {'foreground': 'blue', 'padding': [1, 2, 3, 4]} returns:
          ('-foreground', 'blue', '-padding', '1 2 3 4')
    """
def _mapdict_values(items):
    """
     each value in mapdict is expected to be a sequence, where each item
     is another sequence containing a state (or several) and a value
     E.g. (script=False):
       [('active', 'selected', 'grey'), ('focus', [1, 2, 3, 4])]
       returns:
       ['active selected', 'grey', 'focus', [1, 2, 3, 4]]

    """
def _format_mapdict(mapdict, script=False):
    """
    Formats mapdict to pass it to tk.call.

        E.g. (script=False):
          {'expand': [('active', 'selected', 'grey'), ('focus', [1, 2, 3, 4])]}

          returns:

          ('-expand', '{active selected} grey focus {1, 2, 3, 4}')
    """
def _format_elemcreate(etype, script=False, *args, **kw):
    """
    Formats args and kw according to the given element factory etype.
    """
def _format_layoutlist(layout, indent=0, indent_size=2):
    """
    Formats a layout list so we can pass the result to ttk::style
        layout and ttk::style settings. Note that the layout doesn't have to
        be a list necessarily.

        E.g.:
          [("Menubutton.background", None),
           ("Menubutton.button", {"children":
               [("Menubutton.focus", {"children":
                   [("Menubutton.padding", {"children":
                    [("Menubutton.label", {"side": "left", "expand": 1})]
                   })]
               })]
           }),
           ("Menubutton.indicator", {"side": "right"})
          ]

          returns:

          Menubutton.background
          Menubutton.button -children {
            Menubutton.focus -children {
              Menubutton.padding -children {
                Menubutton.label -side left -expand 1
              }
            }
          }
          Menubutton.indicator -side right
    """
def _script_from_settings(settings):
    """
    Returns an appropriate script, based on settings, according to
        theme_settings definition to be used by theme_settings and
        theme_create.
    """
def _list_from_statespec(stuple):
    """
    Construct a list from the given statespec tuple according to the
        accepted statespec accepted by _format_mapdict.
    """
def _list_from_layouttuple(tk, ltuple):
    """
    Construct a list from the tuple returned by ttk::layout, this is
        somewhat the reverse of _format_layoutlist.
    """
def _val_or_dict(tk, options, *args):
    """
    Format options then call Tk command with args and options and return
        the appropriate result.

        If no option is specified, a dict is returned. If an option is
        specified with the None value, the value for that option is returned.
        Otherwise, the function just sets the passed options and the caller
        shouldn't be expecting a return value anyway.
    """
def _convert_stringval(value):
    """
    Converts a value to, hopefully, a more appropriate Python object.
    """
def _to_number(x):
    """
    '.'
    """
def _tclobj_to_py(val):
    """
    Return value converted from Tcl object to Python object.
    """
def tclobjs_to_py(adict):
    """
    Returns adict with its values converted from Tcl objects to Python
        objects.
    """
def setup_master(master=None):
    """
    If master is not None, itself is returned. If master is None,
        the default master is returned if there is one, otherwise a new
        master is created and returned.

        If it is not allowed to use the default root and master is None,
        RuntimeError is raised.
    """
def Style(object):
    """
    Manipulate style database.
    """
    def __init__(self, master=None):
        """
        '_tile_loaded'
        """
    def configure(self, style, query_opt=None, **kw):
        """
        Query or sets the default value of the specified option(s) in
                style.

                Each key in kw is an option and each value is either a string or
                a sequence identifying the value for that option.
        """
    def map(self, style, query_opt=None, **kw):
        """
        Query or sets dynamic values of the specified option(s) in
                style.

                Each key in kw is an option and each value should be a list or a
                tuple (usually) containing statespecs grouped in tuples, or list,
                or something else of your preference. A statespec is compound of
                one or more states and then a value.
        """
    def lookup(self, style, option, state=None, default=None):
        """
        Returns the value specified for option in style.

                If state is specified it is expected to be a sequence of one
                or more states. If the default argument is set, it is used as
                a fallback value in case no specification for option is found.
        """
    def layout(self, style, layoutspec=None):
        """
        Define the widget layout for given style. If layoutspec is
                omitted, return the layout specification for given style.

                layoutspec is expected to be a list or an object different than
                None that evaluates to False if you want to "turn off" that style.
                If it is a list (or tuple, or something else), each item should be
                a tuple where the first item is the layout name and the second item
                should have the format described below:

                LAYOUTS

                    A layout can contain the value None, if takes no options, or
                    a dict of options specifying how to arrange the element.
                    The layout mechanism uses a simplified version of the pack
                    geometry manager: given an initial cavity, each element is
                    allocated a parcel. Valid options/values are:

                        side: whichside
                            Specifies which side of the cavity to place the
                            element; one of top, right, bottom or left. If
                            omitted, the element occupies the entire cavity.

                        sticky: nswe
                            Specifies where the element is placed inside its
                            allocated parcel.

                        children: [sublayout... ]
                            Specifies a list of elements to place inside the
                            element. Each element is a tuple (or other sequence)
                            where the first item is the layout name, and the other
                            is a LAYOUT.
        """
    def element_create(self, elementname, etype, *args, **kw):
        """
        Create a new element in the current theme of given etype.
        """
    def element_names(self):
        """
        Returns the list of elements defined in the current theme.
        """
    def element_options(self, elementname):
        """
        Return the list of elementname's options.
        """
    def theme_create(self, themename, parent=None, settings=None):
        """
        Creates a new theme.

                It is an error if themename already exists. If parent is
                specified, the new theme will inherit styles, elements and
                layouts from the specified parent theme. If settings are present,
                they are expected to have the same syntax used for theme_settings.
        """
    def theme_settings(self, themename, settings):
        """
        Temporarily sets the current theme to themename, apply specified
                settings and then restore the previous theme.

                Each key in settings is a style and each value may contain the
                keys 'configure', 'map', 'layout' and 'element create' and they
                are expected to have the same format as specified by the methods
                configure, map, layout and element_create respectively.
        """
    def theme_names(self):
        """
        Returns a list of all known themes.
        """
    def theme_use(self, themename=None):
        """
        If themename is None, returns the theme in use, otherwise, set
                the current theme to themename, refreshes all widgets and emits
                a <<ThemeChanged>> event.
        """
def Widget(tkinter.Widget):
    """
    Base class for Tk themed widgets.
    """
    def __init__(self, master, widgetname, kw=None):
        """
        Constructs a Ttk Widget with the parent master.

                STANDARD OPTIONS

                    class, cursor, takefocus, style

                SCROLLABLE WIDGET OPTIONS

                    xscrollcommand, yscrollcommand

                LABEL WIDGET OPTIONS

                    text, textvariable, underline, image, compound, width

                WIDGET STATES

                    active, disabled, focus, pressed, selected, background,
                    readonly, alternate, invalid
        
        """
    def identify(self, x, y):
        """
        Returns the name of the element at position x, y, or the empty
                string if the point does not lie within any element.

                x and y are pixel coordinates relative to the widget.
        """
    def instate(self, statespec, callback=None, *args, **kw):
        """
        Test the widget's state.

                If callback is not specified, returns True if the widget state
                matches statespec and False otherwise. If callback is specified,
                then it will be invoked with *args, **kw if the widget state
                matches statespec. statespec is expected to be a sequence.
        """
    def state(self, statespec=None):
        """
        Modify or inquire widget state.

                Widget state is returned if statespec is None, otherwise it is
                set according to the statespec flags and then a new state spec
                is returned indicating which flags were changed. statespec is
                expected to be a sequence.
        """
def Button(Widget):
    """
    Ttk Button widget, displays a textual label and/or image, and
        evaluates a command when pressed.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Button widget with the parent master.

                STANDARD OPTIONS

                    class, compound, cursor, image, state, style, takefocus,
                    text, textvariable, underline, width

                WIDGET-SPECIFIC OPTIONS

                    command, default, width
        
        """
    def invoke(self):
        """
        Invokes the command associated with the button.
        """
def Checkbutton(Widget):
    """
    Ttk Checkbutton widget which is either in on- or off-state.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Checkbutton widget with the parent master.

                STANDARD OPTIONS

                    class, compound, cursor, image, state, style, takefocus,
                    text, textvariable, underline, width

                WIDGET-SPECIFIC OPTIONS

                    command, offvalue, onvalue, variable
        
        """
    def invoke(self):
        """
        Toggles between the selected and deselected states and
                invokes the associated command. If the widget is currently
                selected, sets the option variable to the offvalue option
                and deselects the widget; otherwise, sets the option variable
                to the option onvalue.

                Returns the result of the associated command.
        """
def Entry(Widget, tkinter.Entry):
    """
    Ttk Entry widget displays a one-line text string and allows that
        string to be edited by the user.
    """
    def __init__(self, master=None, widget=None, **kw):
        """
        Constructs a Ttk Entry widget with the parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus, xscrollcommand

                WIDGET-SPECIFIC OPTIONS

                    exportselection, invalidcommand, justify, show, state,
                    textvariable, validate, validatecommand, width

                VALIDATION MODES

                    none, key, focus, focusin, focusout, all
        
        """
    def bbox(self, index):
        """
        Return a tuple of (x, y, width, height) which describes the
                bounding box of the character given by index.
        """
    def identify(self, x, y):
        """
        Returns the name of the element at position x, y, or the
                empty string if the coordinates are outside the window.
        """
    def validate(self):
        """
        Force revalidation, independent of the conditions specified
                by the validate option. Returns False if validation fails, True
                if it succeeds. Sets or clears the invalid state accordingly.
        """
def Combobox(Entry):
    """
    Ttk Combobox widget combines a text field with a pop-down list of
        values.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Combobox widget with the parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus

                WIDGET-SPECIFIC OPTIONS

                    exportselection, justify, height, postcommand, state,
                    textvariable, values, width
        
        """
    def current(self, newindex=None):
        """
        If newindex is supplied, sets the combobox value to the
                element at position newindex in the list of values. Otherwise,
                returns the index of the current value in the list of values
                or -1 if the current value does not appear in the list.
        """
    def set(self, value):
        """
        Sets the value of the combobox to value.
        """
def Frame(Widget):
    """
    Ttk Frame widget is a container, used to group other widgets
        together.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Frame with parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus

                WIDGET-SPECIFIC OPTIONS

                    borderwidth, relief, padding, width, height
        
        """
def Label(Widget):
    """
    Ttk Label widget displays a textual label and/or image.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Label with parent master.

                STANDARD OPTIONS

                    class, compound, cursor, image, style, takefocus, text,
                    textvariable, underline, width

                WIDGET-SPECIFIC OPTIONS

                    anchor, background, font, foreground, justify, padding,
                    relief, text, wraplength
        
        """
def Labelframe(Widget):
    """
    Ttk Labelframe widget is a container used to group other widgets
        together. It has an optional label, which may be a plain text string
        or another widget.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Labelframe with parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus

                WIDGET-SPECIFIC OPTIONS
                    labelanchor, text, underline, padding, labelwidget, width,
                    height
        
        """
def Menubutton(Widget):
    """
    Ttk Menubutton widget displays a textual label and/or image, and
        displays a menu when pressed.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Menubutton with parent master.

                STANDARD OPTIONS

                    class, compound, cursor, image, state, style, takefocus,
                    text, textvariable, underline, width

                WIDGET-SPECIFIC OPTIONS

                    direction, menu
        
        """
def Notebook(Widget):
    """
    Ttk Notebook widget manages a collection of windows and displays
        a single one at a time. Each child window is associated with a tab,
        which the user may select to change the currently-displayed window.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Notebook with parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus

                WIDGET-SPECIFIC OPTIONS

                    height, padding, width

                TAB OPTIONS

                    state, sticky, padding, text, image, compound, underline

                TAB IDENTIFIERS (tab_id)

                    The tab_id argument found in several methods may take any of
                    the following forms:

                        * An integer between zero and the number of tabs
                        * The name of a child window
                        * A positional specification of the form "@x,y", which
                          defines the tab
                        * The string "current", which identifies the
                          currently-selected tab
                        * The string "end", which returns the number of tabs (only
                          valid for method index)
        
        """
    def add(self, child, **kw):
        """
        Adds a new tab to the notebook.

                If window is currently managed by the notebook but hidden, it is
                restored to its previous position.
        """
    def forget(self, tab_id):
        """
        Removes the tab specified by tab_id, unmaps and unmanages the
                associated window.
        """
    def hide(self, tab_id):
        """
        Hides the tab specified by tab_id.

                The tab will not be displayed, but the associated window remains
                managed by the notebook and its configuration remembered. Hidden
                tabs may be restored with the add command.
        """
    def identify(self, x, y):
        """
        Returns the name of the tab element at position x, y, or the
                empty string if none.
        """
    def index(self, tab_id):
        """
        Returns the numeric index of the tab specified by tab_id, or
                the total number of tabs if tab_id is the string "end".
        """
    def insert(self, pos, child, **kw):
        """
        Inserts a pane at the specified position.

                pos is either the string end, an integer index, or the name of
                a managed child. If child is already managed by the notebook,
                moves it to the specified position.
        """
    def select(self, tab_id=None):
        """
        Selects the specified tab.

                The associated child window will be displayed, and the
                previously-selected window (if different) is unmapped. If tab_id
                is omitted, returns the widget name of the currently selected
                pane.
        """
    def tab(self, tab_id, option=None, **kw):
        """
        Query or modify the options of the specific tab_id.

                If kw is not given, returns a dict of the tab option values. If option
                is specified, returns the value of that option. Otherwise, sets the
                options to the corresponding values.
        """
    def tabs(self):
        """
        Returns a list of windows managed by the notebook.
        """
    def enable_traversal(self):
        """
        Enable keyboard traversal for a toplevel window containing
                this notebook.

                This will extend the bindings for the toplevel window containing
                this notebook as follows:

                    Control-Tab: selects the tab following the currently selected
                                 one

                    Shift-Control-Tab: selects the tab preceding the currently
                                       selected one

                    Alt-K: where K is the mnemonic (underlined) character of any
                           tab, will select that tab.

                Multiple notebooks in a single toplevel may be enabled for
                traversal, including nested notebooks. However, notebook traversal
                only works properly if all panes are direct children of the
                notebook.
        """
def Panedwindow(Widget, tkinter.PanedWindow):
    """
    Ttk Panedwindow widget displays a number of subwindows, stacked
        either vertically or horizontally.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Panedwindow with parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus

                WIDGET-SPECIFIC OPTIONS

                    orient, width, height

                PANE OPTIONS

                    weight
        
        """
    def insert(self, pos, child, **kw):
        """
        Inserts a pane at the specified positions.

                pos is either the string end, and integer index, or the name
                of a child. If child is already managed by the paned window,
                moves it to the specified position.
        """
    def pane(self, pane, option=None, **kw):
        """
        Query or modify the options of the specified pane.

                pane is either an integer index or the name of a managed subwindow.
                If kw is not given, returns a dict of the pane option values. If
                option is specified then the value for that option is returned.
                Otherwise, sets the options to the corresponding values.
        """
    def sashpos(self, index, newpos=None):
        """
        If newpos is specified, sets the position of sash number index.

                May adjust the positions of adjacent sashes to ensure that
                positions are monotonically increasing. Sash positions are further
                constrained to be between 0 and the total size of the widget.

                Returns the new position of sash number index.
        """
def Progressbar(Widget):
    """
    Ttk Progressbar widget shows the status of a long-running
        operation. They can operate in two modes: determinate mode shows the
        amount completed relative to the total amount of work to be done, and
        indeterminate mode provides an animated display to let the user know
        that something is happening.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Progressbar with parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus

                WIDGET-SPECIFIC OPTIONS

                    orient, length, mode, maximum, value, variable, phase
        
        """
    def start(self, interval=None):
        """
        Begin autoincrement mode: schedules a recurring timer event
                that calls method step every interval milliseconds.

                interval defaults to 50 milliseconds (20 steps/second) if omitted.
        """
    def step(self, amount=None):
        """
        Increments the value option by amount.

                amount defaults to 1.0 if omitted.
        """
    def stop(self):
        """
        Stop autoincrement mode: cancels any recurring timer event
                initiated by start.
        """
def Radiobutton(Widget):
    """
    Ttk Radiobutton widgets are used in groups to show or change a
        set of mutually-exclusive options.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Radiobutton with parent master.

                STANDARD OPTIONS

                    class, compound, cursor, image, state, style, takefocus,
                    text, textvariable, underline, width

                WIDGET-SPECIFIC OPTIONS

                    command, value, variable
        
        """
    def invoke(self):
        """
        Sets the option variable to the option value, selects the
                widget, and invokes the associated command.

                Returns the result of the command, or an empty string if
                no command is specified.
        """
def Scale(Widget, tkinter.Scale):
    """
    Ttk Scale widget is typically used to control the numeric value of
        a linked variable that varies uniformly over some range.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Scale with parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus

                WIDGET-SPECIFIC OPTIONS

                    command, from, length, orient, to, value, variable
        
        """
    def configure(self, cnf=None, **kw):
        """
        Modify or query scale options.

                Setting a value for any of the "from", "from_" or "to" options
                generates a <<RangeChanged>> event.
        """
    def get(self, x=None, y=None):
        """
        Get the current value of the value option, or the value
                corresponding to the coordinates x, y if they are specified.

                x and y are pixel coordinates relative to the scale widget
                origin.
        """
def Scrollbar(Widget, tkinter.Scrollbar):
    """
    Ttk Scrollbar controls the viewport of a scrollable widget.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Scrollbar with parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus

                WIDGET-SPECIFIC OPTIONS

                    command, orient
        
        """
def Separator(Widget):
    """
    Ttk Separator widget displays a horizontal or vertical separator
        bar.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Separator with parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus

                WIDGET-SPECIFIC OPTIONS

                    orient
        
        """
def Sizegrip(Widget):
    """
    Ttk Sizegrip allows the user to resize the containing toplevel
        window by pressing and dragging the grip.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Sizegrip with parent master.

                STANDARD OPTIONS

                    class, cursor, state, style, takefocus
        
        """
def Spinbox(Entry):
    """
    Ttk Spinbox is an Entry with increment and decrement arrows

        It is commonly used for number entry or to select from a list of
        string values.
    
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Spinbox widget with the parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus, validate,
                    validatecommand, xscrollcommand, invalidcommand

                WIDGET-SPECIFIC OPTIONS

                    to, from_, increment, values, wrap, format, command
        
        """
    def set(self, value):
        """
        Sets the value of the Spinbox to value.
        """
def Treeview(Widget, tkinter.XView, tkinter.YView):
    """
    Ttk Treeview widget displays a hierarchical collection of items.

        Each item has a textual label, an optional image, and an optional list
        of data values. The data values are displayed in successive columns
        after the tree label.
    """
    def __init__(self, master=None, **kw):
        """
        Construct a Ttk Treeview with parent master.

                STANDARD OPTIONS

                    class, cursor, style, takefocus, xscrollcommand,
                    yscrollcommand

                WIDGET-SPECIFIC OPTIONS

                    columns, displaycolumns, height, padding, selectmode, show

                ITEM OPTIONS

                    text, image, values, open, tags

                TAG OPTIONS

                    foreground, background, font, image
        
        """
    def bbox(self, item, column=None):
        """
        Returns the bounding box (relative to the treeview widget's
                window) of the specified item in the form x y width height.

                If column is specified, returns the bounding box of that cell.
                If the item is not visible (i.e., if it is a descendant of a
                closed item or is scrolled offscreen), returns an empty string.
        """
    def get_children(self, item=None):
        """
        Returns a tuple of children belonging to item.

                If item is not specified, returns root children.
        """
    def set_children(self, item, *newchildren):
        """
        Replaces item's child with newchildren.

                Children present in item that are not present in newchildren
                are detached from tree. No items in newchildren may be an
                ancestor of item.
        """
    def column(self, column, option=None, **kw):
        """
        Query or modify the options for the specified column.

                If kw is not given, returns a dict of the column option values. If
                option is specified then the value for that option is returned.
                Otherwise, sets the options to the corresponding values.
        """
    def delete(self, *items):
        """
        Delete all specified items and all their descendants. The root
                item may not be deleted.
        """
    def detach(self, *items):
        """
        Unlinks all of the specified items from the tree.

                The items and all of their descendants are still present, and may
                be reinserted at another point in the tree, but will not be
                displayed. The root item may not be detached.
        """
    def exists(self, item):
        """
        Returns True if the specified item is present in the tree,
                False otherwise.
        """
    def focus(self, item=None):
        """
        If item is specified, sets the focus item to item. Otherwise,
                returns the current focus item, or '' if there is none.
        """
    def heading(self, column, option=None, **kw):
        """
        Query or modify the heading options for the specified column.

                If kw is not given, returns a dict of the heading option values. If
                option is specified then the value for that option is returned.
                Otherwise, sets the options to the corresponding values.

                Valid options/values are:
                    text: text
                        The text to display in the column heading
                    image: image_name
                        Specifies an image to display to the right of the column
                        heading
                    anchor: anchor
                        Specifies how the heading text should be aligned. One of
                        the standard Tk anchor values
                    command: callback
                        A callback to be invoked when the heading label is
                        pressed.

                To configure the tree column heading, call this with column = "#0" 
        """
    def identify(self, component, x, y):
        """
        Returns a description of the specified component under the
                point given by x and y, or the empty string if no such component
                is present at that position.
        """
    def identify_row(self, y):
        """
        Returns the item ID of the item at position y.
        """
    def identify_column(self, x):
        """
        Returns the data column identifier of the cell at position x.

                The tree column has ID #0.
        """
    def identify_region(self, x, y):
        """
        Returns one of:

                heading: Tree heading area.
                separator: Space between two columns headings;
                tree: The tree area.
                cell: A data cell.

                * Availability: Tk 8.6
        """
    def identify_element(self, x, y):
        """
        Returns the element at position x, y.

                * Availability: Tk 8.6
        """
    def index(self, item):
        """
        Returns the integer index of item within its parent's list
                of children.
        """
    def insert(self, parent, index, iid=None, **kw):
        """
        Creates a new item and return the item identifier of the newly
                created item.

                parent is the item ID of the parent item, or the empty string
                to create a new top-level item. index is an integer, or the value
                end, specifying where in the list of parent's children to insert
                the new item. If index is less than or equal to zero, the new node
                is inserted at the beginning, if index is greater than or equal to
                the current number of children, it is inserted at the end. If iid
                is specified, it is used as the item identifier, iid must not
                already exist in the tree. Otherwise, a new unique identifier
                is generated.
        """
    def item(self, item, option=None, **kw):
        """
        Query or modify the options for the specified item.

                If no options are given, a dict with options/values for the item
                is returned. If option is specified then the value for that option
                is returned. Otherwise, sets the options to the corresponding
                values as given by kw.
        """
    def move(self, item, parent, index):
        """
        Moves item to position index in parent's list of children.

                It is illegal to move an item under one of its descendants. If
                index is less than or equal to zero, item is moved to the
                beginning, if greater than or equal to the number of children,
                it is moved to the end. If item was detached it is reattached.
        """
    def next(self, item):
        """
        Returns the identifier of item's next sibling, or '' if item
                is the last child of its parent.
        """
    def parent(self, item):
        """
        Returns the ID of the parent of item, or '' if item is at the
                top level of the hierarchy.
        """
    def prev(self, item):
        """
        Returns the identifier of item's previous sibling, or '' if
                item is the first child of its parent.
        """
    def see(self, item):
        """
        Ensure that item is visible.

                Sets all of item's ancestors open option to True, and scrolls
                the widget if necessary so that item is within the visible
                portion of the tree.
        """
    def selection(self):
        """
        Returns the tuple of selected items.
        """
    def _selection(self, selop, items):
        """
        selection
        """
    def selection_set(self, *items):
        """
        The specified items becomes the new selection.
        """
    def selection_add(self, *items):
        """
        Add all of the specified items to the selection.
        """
    def selection_remove(self, *items):
        """
        Remove all of the specified items from the selection.
        """
    def selection_toggle(self, *items):
        """
        Toggle the selection state of each specified item.
        """
    def set(self, item, column=None, value=None):
        """
        Query or set the value of given item.

                With one argument, return a dictionary of column/value pairs
                for the specified item. With two arguments, return the current
                value of the specified column. With three arguments, set the
                value of given column in given item to the specified value.
        """
    def tag_bind(self, tagname, sequence=None, callback=None):
        """
        Bind a callback for the given event sequence to the tag tagname.
                When an event is delivered to an item, the callbacks for each
                of the item's tags option are called.
        """
    def tag_configure(self, tagname, option=None, **kw):
        """
        Query or modify the options for the specified tagname.

                If kw is not given, returns a dict of the option settings for tagname.
                If option is specified, returns the value for that option for the
                specified tagname. Otherwise, sets the options to the corresponding
                values for the given tagname.
        """
    def tag_has(self, tagname, item=None):
        """
        If item is specified, returns 1 or 0 depending on whether the
                specified item has the given tagname. Otherwise, returns a list of
                all items which have the specified tag.

                * Availability: Tk 8.6
        """
def LabeledScale(Frame):
    """
    A Ttk Scale widget with a Ttk Label widget indicating its
        current value.

        The Ttk Scale can be accessed through instance.scale, and Ttk Label
        can be accessed through instance.label
    """
    def __init__(self, master=None, variable=None, from_=0, to=10, **kw):
        """
        Construct a horizontal LabeledScale with parent master, a
                variable to be associated with the Ttk Scale widget and its range.
                If variable is not specified, a tkinter.IntVar is created.

                WIDGET-SPECIFIC OPTIONS

                    compound: 'top' or 'bottom'
                        Specifies how to display the label relative to the scale.
                        Defaults to 'top'.
        
        """
    def destroy(self):
        """
        Destroy this widget and possibly its associated variable.
        """
    def _adjust(self, *args):
        """
        Adjust the label position according to the scale.
        """
        def adjust_label():
            """
             "force" scale redraw
            """
    def value(self):
        """
        Return current scale value.
        """
    def value(self, val):
        """
        Set new scale value.
        """
def OptionMenu(Menubutton):
    """
    Themed OptionMenu, based after tkinter's OptionMenu, which allows
        the user to select a value from a menu.
    """
    def __init__(self, master, variable, default=None, *values, **kwargs):
        """
        Construct a themed OptionMenu widget with master as the parent,
                the resource textvariable set to variable, the initially selected
                value specified by the default parameter, the menu values given by
                *values and additional keywords.

                WIDGET-SPECIFIC OPTIONS

                    style: stylename
                        Menubutton style.
                    direction: 'above', 'below', 'left', 'right', or 'flush'
                        Menubutton direction.
                    command: callback
                        A callback that will be invoked after selecting an item.
        
        """
    def __getitem__(self, item):
        """
        'menu'
        """
    def set_menu(self, default=None, *values):
        """
        Build a new menu of radiobuttons with *values and optionally
                a default value.
        """
    def destroy(self):
        """
        Destroy this widget and its associated variable.
        """
