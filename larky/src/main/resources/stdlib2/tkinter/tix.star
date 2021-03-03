def tixCommand:
    """
    The tix commands provide access to miscellaneous  elements
        of  Tix's  internal state and the Tix application context.
        Most of the information manipulated by these  commands pertains
        to  the  application  as a whole, or to a screen or
        display, rather than to a particular window.

        This is a mixin class, assumed to be mixed to Tkinter.Tk
        that supports the self.tk.call method.
    
    """
    def tix_addbitmapdir(self, directory):
        """
        Tix maintains a list of directories under which
                the  tix_getimage  and tix_getbitmap commands will
                search for image files. The standard bitmap  directory
                is $TIX_LIBRARY/bitmaps. The addbitmapdir command
                adds directory into this list. By  using  this
                command, the  image  files  of an applications can
                also be located using the tix_getimage or tix_getbitmap
                command.
        
        """
    def tix_cget(self, option):
        """
        Returns  the  current  value  of the configuration
                option given by option. Option may be  any  of  the
                options described in the CONFIGURATION OPTIONS section.
        
        """
    def tix_configure(self, cnf=None, **kw):
        """
        Query or modify the configuration options of the Tix application
                context. If no option is specified, returns a dictionary all of the
                available options.  If option is specified with no value, then the
                command returns a list describing the one named option (this list
                will be identical to the corresponding sublist of the value
                returned if no option is specified).  If one or more option-value
                pairs are specified, then the command modifies the given option(s)
                to have the given value(s); in this case the command returns an
                empty string. Option may be any of the configuration options.
        
        """
    def tix_filedialog(self, dlgclass=None):
        """
        Returns the file selection dialog that may be shared among
                different calls from this application.  This command will create a
                file selection dialog widget when it is called the first time. This
                dialog will be returned by all subsequent calls to tix_filedialog.
                An optional dlgclass parameter can be passed to specified what type
                of file selection dialog widget is desired. Possible options are
                tix FileSelectDialog or tixExFileSelectDialog.
        
        """
    def tix_getbitmap(self, name):
        """
        Locates a bitmap file of the name name.xpm or name in one of the
                bitmap directories (see the tix_addbitmapdir command above).  By
                using tix_getbitmap, you can avoid hard coding the pathnames of the
                bitmap files in your application. When successful, it returns the
                complete pathname of the bitmap file, prefixed with the character
                '@'.  The returned value can be used to configure the -bitmap
                option of the TK and Tix widgets.
        
        """
    def tix_getimage(self, name):
        """
        Locates an image file of the name name.xpm, name.xbm or name.ppm
                in one of the bitmap directories (see the addbitmapdir command
                above). If more than one file with the same name (but different
                extensions) exist, then the image type is chosen according to the
                depth of the X display: xbm images are chosen on monochrome
                displays and color images are chosen on color displays. By using
                tix_ getimage, you can avoid hard coding the pathnames of the
                image files in your application. When successful, this command
                returns the name of the newly created image, which can be used to
                configure the -image option of the Tk and Tix widgets.
        
        """
    def tix_option_get(self, name):
        """
        Gets  the options  maintained  by  the  Tix
                scheme mechanism. Available options include:

                    active_bg       active_fg      bg
                    bold_font       dark1_bg       dark1_fg
                    dark2_bg        dark2_fg       disabled_fg
                    fg              fixed_font     font
                    inactive_bg     inactive_fg    input1_bg
                    input2_bg       italic_font    light1_bg
                    light1_fg       light2_bg      light2_fg
                    menu_font       output1_bg     output2_bg
                    select_bg       select_fg      selector
            
        """
    def tix_resetoptions(self, newScheme, newFontSet, newScmPrio=None):
        """
        Resets the scheme and fontset of the Tix application to
                newScheme and newFontSet, respectively.  This affects only those
                widgets created after this call. Therefore, it is best to call the
                resetoptions command before the creation of any widgets in a Tix
                application.

                The optional parameter newScmPrio can be given to reset the
                priority level of the Tk options set by the Tix schemes.

                Because of the way Tk handles the X option database, after Tix has
                been has imported and inited, it is not possible to reset the color
                schemes and font sets using the tix config command.  Instead, the
                tix_resetoptions command must be used.
        
        """
def Tk(tkinter.Tk, tixCommand):
    """
    Toplevel widget of Tix which represents mostly the main window
        of an application. It has an associated Tcl interpreter.
    """
    def __init__(self, screenName=None, baseName=None, className='Tix'):
        """
        'TIX_LIBRARY'
        """
    def destroy(self):
        """
         For safety, remove the delete_window binding before destroy

        """
def Form:
    """
    The Tix Form geometry manager

        Widgets can be arranged by specifying attachments to other widgets.
        See Tix documentation for complete details
    """
    def config(self, cnf={}, **kw):
        """
        'tixForm'
        """
    def __setitem__(self, key, value):
        """
        'tixForm'
        """
    def forget(self):
        """
        'tixForm'
        """
    def grid(self, xsize=0, ysize=0):
        """
        'tixForm'
        """
    def info(self, option=None):
        """
        'tixForm'
        """
    def slaves(self):
        """
        'tixForm'
        """
def TixWidget(tkinter.Widget):
    """
    A TixWidget class is used to package all (or most) Tix widgets.

        Widget initialization is extended in two ways:
           1) It is possible to give a list of options which must be part of
           the creation command (so called Tix 'static' options). These cannot be
           given as a 'config' command later.
           2) It is possible to give the name of an existing TK widget. These are
           child widgets created automatically by a Tix mega-widget. The Tk call
           to create these widgets is therefore bypassed in TixWidget.__init__

        Both options are for use by subclasses only.
    
    """
2021-03-02 20:53:36,882 : INFO : tokenize_signature : --> do i ever get here?
    def __init__ (self, master=None, widgetName=None,
                static_options=None, cnf={}, kw={}):
        """
         Merge keywords and dictionary arguments

        """
    def __getattr__(self, name):
        """
        Set a variable without calling its action routine
        """
    def subwidget(self, name):
        """
        Return the named subwidget (which must have been created by
                the sub-class).
        """
    def subwidgets_all(self):
        """
        Return all subwidgets.
        """
    def _subwidget_name(self,name):
        """
        Get a subwidget name (returns a String, not a Widget !)
        """
    def _subwidget_names(self):
        """
        Return the name of all subwidgets.
        """
    def config_all(self, option, value):
        """
        Set configuration options for all subwidgets (and self).
        """
    def image_create(self, imgtype, cnf={}, master=None, **kw):
        """
        'Too early to create image'
        """
    def image_delete(self, imgname):
        """
        'image'
        """
def TixSubWidget(TixWidget):
    """
    Subwidget class.

        This is used to mirror child widgets automatically created
        by Tix/Tk as part of a mega-widget in Python (which is not informed
        of this)
    """
2021-03-02 20:53:36,885 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, master, name,
               destroy_physically=1, check_intermediate=1):
        """
        '.'
        """
    def destroy(self):
        """
         For some widgets e.g., a NoteBook, when we call destructors,
         we must be careful not to destroy the frame widget since this
         also destroys the parent NoteBook thus leading to an exception
         in Tkinter when it finally calls Tcl to destroy the NoteBook

        """
def DisplayStyle:
    """
    DisplayStyle - handle configuration options shared by
        (multiple) Display Items
    """
    def __init__(self, itemtype, cnf={}, *, master=None, **kw):
        """
        'refwindow'
        """
    def __str__(self):
        """
        '-'
        """
    def delete(self):
        """
        'delete'
        """
    def __setitem__(self,key,value):
        """
        'configure'
        """
    def config(self, cnf={}, **kw):
        """
        'configure'
        """
    def __getitem__(self,key):
        """
        'cget'
        """
def Balloon(TixWidget):
    """
    Balloon help widget.

        Subwidget       Class
        ---------       -----
        label           Label
        message         Message
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
         static seem to be -installcolormap -initwait -statusbar -cursor

        """
    def bind_widget(self, widget, cnf={}, **kw):
        """
        Bind balloon widget to another.
                One balloon widget may be bound to several widgets at the same time
        """
    def unbind_widget(self, widget):
        """
        'unbind'
        """
def ButtonBox(TixWidget):
    """
    ButtonBox - A container for pushbuttons.
        Subwidgets are the buttons added with the add method.
    
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        'tixButtonBox'
        """
    def add(self, name, cnf={}, **kw):
        """
        Add a button with given name to box.
        """
    def invoke(self, name):
        """
        'invoke'
        """
def ComboBox(TixWidget):
    """
    ComboBox - an Entry field with a dropdown menu. The user can select a
        choice by either typing in the entry subwidget or selecting from the
        listbox subwidget.

        Subwidget       Class
        ---------       -----
        entry       Entry
        arrow       Button
        slistbox    ScrolledListBox
        tick        Button
        cross       Button : present if created with the fancy option
    """
    def __init__ (self, master=None, cnf={}, **kw):
        """
        'tixComboBox'
        """
    def add_history(self, str):
        """
        'addhistory'
        """
    def append_history(self, str):
        """
        'appendhistory'
        """
    def insert(self, index, str):
        """
        'insert'
        """
    def pick(self, index):
        """
        'pick'
        """
def Control(TixWidget):
    """
    Control - An entry field with value change arrows.  The user can
        adjust the value by pressing the two arrow buttons or by entering
        the value directly into the entry. The new value will be checked
        against the user-defined upper and lower limits.

        Subwidget       Class
        ---------       -----
        incr       Button
        decr       Button
        entry       Entry
        label       Label
    """
    def __init__ (self, master=None, cnf={}, **kw):
        """
        'tixControl'
        """
    def decrement(self):
        """
        'decr'
        """
    def increment(self):
        """
        'incr'
        """
    def invoke(self):
        """
        'invoke'
        """
    def update(self):
        """
        'update'
        """
def DirList(TixWidget):
    """
    DirList - displays a list view of a directory, its previous
        directories and its sub-directories. The user can choose one of
        the directories displayed in the list or change to another directory.

        Subwidget       Class
        ---------       -----
        hlist       HList
        hsb              Scrollbar
        vsb              Scrollbar
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixDirList'
        """
    def chdir(self, dir):
        """
        'chdir'
        """
def DirTree(TixWidget):
    """
    DirTree - Directory Listing in a hierarchical view.
        Displays a tree view of a directory, its previous directories and its
        sub-directories. The user can choose one of the directories displayed
        in the list or change to another directory.

        Subwidget       Class
        ---------       -----
        hlist           HList
        hsb             Scrollbar
        vsb             Scrollbar
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixDirTree'
        """
    def chdir(self, dir):
        """
        'chdir'
        """
def DirSelectBox(TixWidget):
    """
    DirSelectBox - Motif style file select box.
        It is generally used for
        the user to choose a file. FileSelectBox stores the files mostly
        recently selected into a ComboBox widget so that they can be quickly
        selected again.

        Subwidget       Class
        ---------       -----
        selection       ComboBox
        filter          ComboBox
        dirlist         ScrolledListBox
        filelist        ScrolledListBox
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixDirSelectBox'
        """
def ExFileSelectBox(TixWidget):
    """
    ExFileSelectBox - MS Windows style file select box.
        It provides a convenient method for the user to select files.

        Subwidget       Class
        ---------       -----
        cancel       Button
        ok              Button
        hidden       Checkbutton
        types       ComboBox
        dir              ComboBox
        file       ComboBox
        dirlist       ScrolledListBox
        filelist       ScrolledListBox
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixExFileSelectBox'
        """
    def filter(self):
        """
        'filter'
        """
    def invoke(self):
        """
        'invoke'
        """
def DirSelectDialog(TixWidget):
    """
    The DirSelectDialog widget presents the directories in the file
        system in a dialog window. The user can use this dialog window to
        navigate through the file system to select the desired directory.

        Subwidgets       Class
        ----------       -----
        dirbox       DirSelectDialog
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixDirSelectDialog'
        """
    def popup(self):
        """
        'popup'
        """
    def popdown(self):
        """
        'popdown'
        """
def ExFileSelectDialog(TixWidget):
    """
    ExFileSelectDialog - MS Windows style file select dialog.
        It provides a convenient method for the user to select files.

        Subwidgets       Class
        ----------       -----
        fsbox       ExFileSelectBox
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixExFileSelectDialog'
        """
    def popup(self):
        """
        'popup'
        """
    def popdown(self):
        """
        'popdown'
        """
def FileSelectBox(TixWidget):
    """
    ExFileSelectBox - Motif style file select box.
        It is generally used for
        the user to choose a file. FileSelectBox stores the files mostly
        recently selected into a ComboBox widget so that they can be quickly
        selected again.

        Subwidget       Class
        ---------       -----
        selection       ComboBox
        filter          ComboBox
        dirlist         ScrolledListBox
        filelist        ScrolledListBox
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixFileSelectBox'
        """
    def apply_filter(self):              # name of subwidget is same as command
        """
         name of subwidget is same as command
        """
    def invoke(self):
        """
        'invoke'
        """
def FileSelectDialog(TixWidget):
    """
    FileSelectDialog - Motif style file select dialog.

        Subwidgets       Class
        ----------       -----
        btns       StdButtonBox
        fsbox       FileSelectBox
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixFileSelectDialog'
        """
    def popup(self):
        """
        'popup'
        """
    def popdown(self):
        """
        'popdown'
        """
def FileEntry(TixWidget):
    """
    FileEntry - Entry field with button that invokes a FileSelectDialog.
        The user can type in the filename manually. Alternatively, the user can
        press the button widget that sits next to the entry, which will bring
        up a file selection dialog.

        Subwidgets       Class
        ----------       -----
        button       Button
        entry       Entry
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixFileEntry'
        """
    def invoke(self):
        """
        'invoke'
        """
    def file_dialog(self):
        """
         FIXME: return python object

        """
def HList(TixWidget, XView, YView):
    """
    HList - Hierarchy display  widget can be used to display any data
        that have a hierarchical structure, for example, file system directory
        trees. The list entries are indented and connected by branch lines
        according to their places in the hierarchy.

        Subwidgets - None
    """
    def __init__ (self,master=None,cnf={}, **kw):
        """
        'tixHList'
        """
    def add(self, entry, cnf={}, **kw):
        """
        'add'
        """
    def add_child(self, parent=None, cnf={}, **kw):
        """
        ''
        """
    def anchor_set(self, entry):
        """
        'anchor'
        """
    def anchor_clear(self):
        """
        'anchor'
        """
    def column_width(self, col=0, width=None, chars=None):
        """
        'column'
        """
    def delete_all(self):
        """
        'delete'
        """
    def delete_entry(self, entry):
        """
        'delete'
        """
    def delete_offsprings(self, entry):
        """
        'delete'
        """
    def delete_siblings(self, entry):
        """
        'delete'
        """
    def dragsite_set(self, index):
        """
        'dragsite'
        """
    def dragsite_clear(self):
        """
        'dragsite'
        """
    def dropsite_set(self, index):
        """
        'dropsite'
        """
    def dropsite_clear(self):
        """
        'dropsite'
        """
    def header_create(self, col, cnf={}, **kw):
        """
        'header'
        """
    def header_configure(self, col, cnf={}, **kw):
        """
        'header'
        """
    def header_cget(self,  col, opt):
        """
        'header'
        """
    def header_exists(self,  col):
        """
         A workaround to Tix library bug (issue #25464).
         The documented command is "exists", but only erroneous "exist" is
         accepted.

        """
    def header_delete(self, col):
        """
        'header'
        """
    def header_size(self, col):
        """
        'header'
        """
    def hide_entry(self, entry):
        """
        'hide'
        """
    def indicator_create(self, entry, cnf={}, **kw):
        """
        'indicator'
        """
    def indicator_configure(self, entry, cnf={}, **kw):
        """
        'indicator'
        """
    def indicator_cget(self,  entry, opt):
        """
        'indicator'
        """
    def indicator_exists(self,  entry):
        """
        'indicator'
        """
    def indicator_delete(self, entry):
        """
        'indicator'
        """
    def indicator_size(self, entry):
        """
        'indicator'
        """
    def info_anchor(self):
        """
        'info'
        """
    def info_bbox(self, entry):
        """
        'info'
        """
    def info_children(self, entry=None):
        """
        'info'
        """
    def info_data(self, entry):
        """
        'info'
        """
    def info_dragsite(self):
        """
        'info'
        """
    def info_dropsite(self):
        """
        'info'
        """
    def info_exists(self, entry):
        """
        'info'
        """
    def info_hidden(self, entry):
        """
        'info'
        """
    def info_next(self, entry):
        """
        'info'
        """
    def info_parent(self, entry):
        """
        'info'
        """
    def info_prev(self, entry):
        """
        'info'
        """
    def info_selection(self):
        """
        'info'
        """
    def item_cget(self, entry, col, opt):
        """
        'item'
        """
    def item_configure(self, entry, col, cnf={}, **kw):
        """
        'item'
        """
    def item_create(self, entry, col, cnf={}, **kw):
        """
        'item'
        """
    def item_exists(self, entry, col):
        """
        'item'
        """
    def item_delete(self, entry, col):
        """
        'item'
        """
    def entrycget(self, entry, opt):
        """
        'entrycget'
        """
    def entryconfigure(self, entry, cnf={}, **kw):
        """
        'entryconfigure'
        """
    def nearest(self, y):
        """
        'nearest'
        """
    def see(self, entry):
        """
        'see'
        """
    def selection_clear(self, cnf={}, **kw):
        """
        'selection'
        """
    def selection_includes(self, entry):
        """
        'selection'
        """
    def selection_set(self, first, last=None):
        """
        'selection'
        """
    def show_entry(self, entry):
        """
        'show'
        """
def InputOnly(TixWidget):
    """
    InputOnly - Invisible widget. Unix only.

        Subwidgets - None
    """
    def __init__ (self,master=None,cnf={}, **kw):
        """
        'tixInputOnly'
        """
def LabelEntry(TixWidget):
    """
    LabelEntry - Entry field with label. Packages an entry widget
        and a label into one mega widget. It can be used to simplify the creation
        of ``entry-form'' type of interface.

        Subwidgets       Class
        ----------       -----
        label       Label
        entry       Entry
    """
    def __init__ (self,master=None,cnf={}, **kw):
        """
        'tixLabelEntry'
        """
def LabelFrame(TixWidget):
    """
    LabelFrame - Labelled Frame container. Packages a frame widget
        and a label into one mega widget. To create widgets inside a
        LabelFrame widget, one creates the new widgets relative to the
        frame subwidget and manage them inside the frame subwidget.

        Subwidgets       Class
        ----------       -----
        label       Label
        frame       Frame
    """
    def __init__ (self,master=None,cnf={}, **kw):
        """
        'tixLabelFrame'
        """
def ListNoteBook(TixWidget):
    """
    A ListNoteBook widget is very similar to the TixNoteBook widget:
        it can be used to display many windows in a limited space using a
        notebook metaphor. The notebook is divided into a stack of pages
        (windows). At one time only one of these pages can be shown.
        The user can navigate through these pages by
        choosing the name of the desired page in the hlist subwidget.
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixListNoteBook'
        """
    def add(self, name, cnf={}, **kw):
        """
        'add'
        """
    def page(self, name):
        """
         Can't call subwidgets_all directly because we don't want .nbframe

        """
    def raise_page(self, name):              # raise is a python keyword
        """
         raise is a python keyword
        """
def Meter(TixWidget):
    """
    The Meter widget can be used to show the progress of a background
        job which may take a long time to execute.
    
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        'tixMeter'
        """
def NoteBook(TixWidget):
    """
    NoteBook - Multi-page container widget (tabbed notebook metaphor).

        Subwidgets       Class
        ----------       -----
        nbframe       NoteBookFrame
        <pages>       page widgets added dynamically with the add method
    """
    def __init__ (self,master=None,cnf={}, **kw):
        """
        'tixNoteBook'
        """
    def add(self, name, cnf={}, **kw):
        """
        'add'
        """
    def delete(self, name):
        """
        'delete'
        """
    def page(self, name):
        """
         Can't call subwidgets_all directly because we don't want .nbframe

        """
    def raise_page(self, name):              # raise is a python keyword
        """
         raise is a python keyword
        """
    def raised(self):
        """
        'raised'
        """
def NoteBookFrame(TixWidget):
    """
     FIXME: This is dangerous to expose to be called on its own.

    """
def OptionMenu(TixWidget):
    """
    OptionMenu - creates a menu button of options.

        Subwidget       Class
        ---------       -----
        menubutton      Menubutton
        menu            Menu
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixOptionMenu'
        """
    def add_command(self, name, cnf={}, **kw):
        """
        'add'
        """
    def add_separator(self, name, cnf={}, **kw):
        """
        'add'
        """
    def delete(self, name):
        """
        'delete'
        """
    def disable(self, name):
        """
        'disable'
        """
    def enable(self, name):
        """
        'enable'
        """
def PanedWindow(TixWidget):
    """
    PanedWindow - Multi-pane container widget
        allows the user to interactively manipulate the sizes of several
        panes. The panes can be arranged either vertically or horizontally.The
        user changes the sizes of the panes by dragging the resize handle
        between two panes.

        Subwidgets       Class
        ----------       -----
        <panes>       g/p widgets added dynamically with the add method.
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixPanedWindow'
        """
    def add(self, name, cnf={}, **kw):
        """
        'add'
        """
    def delete(self, name):
        """
        'delete'
        """
    def forget(self, name):
        """
        'forget'
        """
    def panecget(self,  entry, opt):
        """
        'panecget'
        """
    def paneconfigure(self, entry, cnf={}, **kw):
        """
        'paneconfigure'
        """
    def panes(self):
        """
        'panes'
        """
def PopupMenu(TixWidget):
    """
    PopupMenu widget can be used as a replacement of the tk_popup command.
        The advantage of the Tix PopupMenu widget is it requires less application
        code to manipulate.


        Subwidgets       Class
        ----------       -----
        menubutton       Menubutton
        menu       Menu
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixPopupMenu'
        """
    def bind_widget(self, widget):
        """
        'bind'
        """
    def unbind_widget(self, widget):
        """
        'unbind'
        """
    def post_widget(self, widget, x, y):
        """
        'post'
        """
def ResizeHandle(TixWidget):
    """
    Internal widget to draw resize handles on Scrolled widgets.
    """
    def __init__(self, master, cnf={}, **kw):
        """
         There seems to be a Tix bug rejecting the configure method
         Let's try making the flags -static

        """
    def attach_widget(self, widget):
        """
        'attachwidget'
        """
    def detach_widget(self, widget):
        """
        'detachwidget'
        """
    def hide(self, widget):
        """
        'hide'
        """
    def show(self, widget):
        """
        'show'
        """
def ScrolledHList(TixWidget):
    """
    ScrolledHList - HList with automatic scrollbars.
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixScrolledHList'
        """
def ScrolledListBox(TixWidget):
    """
    ScrolledListBox - Listbox with automatic scrollbars.
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixScrolledListBox'
        """
def ScrolledText(TixWidget):
    """
    ScrolledText - Text with automatic scrollbars.
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixScrolledText'
        """
def ScrolledTList(TixWidget):
    """
    ScrolledTList - TList with automatic scrollbars.
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixScrolledTList'
        """
def ScrolledWindow(TixWidget):
    """
    ScrolledWindow - Window with automatic scrollbars.
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixScrolledWindow'
        """
def Select(TixWidget):
    """
    Select - Container of button subwidgets. It can be used to provide
        radio-box or check-box style of selection options for the user.

        Subwidgets are buttons added dynamically using the add method.
    """
    def __init__(self, master, cnf={}, **kw):
        """
        'tixSelect'
        """
    def add(self, name, cnf={}, **kw):
        """
        'add'
        """
    def invoke(self, name):
        """
        'invoke'
        """
def Shell(TixWidget):
    """
    Toplevel window.

        Subwidgets - None
    """
    def __init__ (self,master=None,cnf={}, **kw):
        """
        'tixShell'
        """
def DialogShell(TixWidget):
    """
    Toplevel window, with popup popdown and center methods.
        It tells the window manager that it is a dialog window and should be
        treated specially. The exact treatment depends on the treatment of
        the window manager.

        Subwidgets - None
    """
    def __init__ (self,master=None,cnf={}, **kw):
        """
        'tixDialogShell'
        """
    def popdown(self):
        """
        'popdown'
        """
    def popup(self):
        """
        'popup'
        """
    def center(self):
        """
        'center'
        """
def StdButtonBox(TixWidget):
    """
    StdButtonBox - Standard Button Box (OK, Apply, Cancel and Help) 
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        'tixStdButtonBox'
        """
    def invoke(self, name):
        """
        'invoke'
        """
def TList(TixWidget, XView, YView):
    """
    TList - Hierarchy display widget which can be
        used to display data in a tabular format. The list entries of a TList
        widget are similar to the entries in the Tk listbox widget. The main
        differences are (1) the TList widget can display the list entries in a
        two dimensional format and (2) you can use graphical images as well as
        multiple colors and fonts for the list entries.

        Subwidgets - None
    """
    def __init__ (self,master=None,cnf={}, **kw):
        """
        'tixTList'
        """
    def active_set(self, index):
        """
        'active'
        """
    def active_clear(self):
        """
        'active'
        """
    def anchor_set(self, index):
        """
        'anchor'
        """
    def anchor_clear(self):
        """
        'anchor'
        """
    def delete(self, from_, to=None):
        """
        'delete'
        """
    def dragsite_set(self, index):
        """
        'dragsite'
        """
    def dragsite_clear(self):
        """
        'dragsite'
        """
    def dropsite_set(self, index):
        """
        'dropsite'
        """
    def dropsite_clear(self):
        """
        'dropsite'
        """
    def insert(self, index, cnf={}, **kw):
        """
        'insert'
        """
    def info_active(self):
        """
        'info'
        """
    def info_anchor(self):
        """
        'info'
        """
    def info_down(self, index):
        """
        'info'
        """
    def info_left(self, index):
        """
        'info'
        """
    def info_right(self, index):
        """
        'info'
        """
    def info_selection(self):
        """
        'info'
        """
    def info_size(self):
        """
        'info'
        """
    def info_up(self, index):
        """
        'info'
        """
    def nearest(self, x, y):
        """
        'nearest'
        """
    def see(self, index):
        """
        'see'
        """
    def selection_clear(self, cnf={}, **kw):
        """
        'selection'
        """
    def selection_includes(self, index):
        """
        'selection'
        """
    def selection_set(self, first, last=None):
        """
        'selection'
        """
def Tree(TixWidget):
    """
    Tree - The tixTree widget can be used to display hierarchical
        data in a tree form. The user can adjust
        the view of the tree by opening or closing parts of the tree.
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        'tixTree'
        """
    def autosetmode(self):
        """
        '''This command calls the setmode method for all the entries in this
             Tree widget: if an entry has no child entries, its mode is set to
             none. Otherwise, if the entry has any hidden child entries, its mode is
             set to open; otherwise its mode is set to close.'''
        """
    def close(self, entrypath):
        """
        '''Close the entry given by entryPath if its mode is close.'''
        """
    def getmode(self, entrypath):
        """
        '''Returns the current mode of the entry given by entryPath.'''
        """
    def open(self, entrypath):
        """
        '''Open the entry given by entryPath if its mode is open.'''
        """
    def setmode(self, entrypath, mode='none'):
        """
        '''This command is used to indicate whether the entry given by
             entryPath has children entries and whether the children are visible. mode
             must be one of open, close or none. If mode is set to open, a (+)
             indicator is drawn next the entry. If mode is set to close, a (-)
             indicator is drawn next the entry. If mode is set to none, no
             indicators will be drawn for this entry. The default mode is none. The
             open mode indicates the entry has hidden children and this entry can be
             opened by the user. The close mode indicates that all the children of the
             entry are now visible and the entry can be closed by the user.'''
        """
def CheckList(TixWidget):
    """
    The CheckList widget
        displays a list of items to be selected by the user. CheckList acts
        similarly to the Tk checkbutton or radiobutton widgets, except it is
        capable of handling many more items than checkbuttons or radiobuttons.
    
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        'tixCheckList'
        """
    def autosetmode(self):
        """
        '''This command calls the setmode method for all the entries in this
             Tree widget: if an entry has no child entries, its mode is set to
             none. Otherwise, if the entry has any hidden child entries, its mode is
             set to open; otherwise its mode is set to close.'''
        """
    def close(self, entrypath):
        """
        '''Close the entry given by entryPath if its mode is close.'''
        """
    def getmode(self, entrypath):
        """
        '''Returns the current mode of the entry given by entryPath.'''
        """
    def open(self, entrypath):
        """
        '''Open the entry given by entryPath if its mode is open.'''
        """
    def getselection(self, mode='on'):
        """
        '''Returns a list of items whose status matches status. If status is
             not specified, the list of items in the "on" status will be returned.
             Mode can be on, off, default'''
        """
    def getstatus(self, entrypath):
        """
        '''Returns the current status of entryPath.'''
        """
    def setstatus(self, entrypath, mode='on'):
        """
        '''Sets the status of entryPath to be status. A bitmap will be
             displayed next to the entry its status is on, off or default.'''
        """
def _dummyButton(Button, TixSubWidget):
    """
    'listbox'
    """
def _dummyHList(HList, TixSubWidget):
    """
    'hlist'
    """
def _dummyTList(TList, TixSubWidget):
    """
    'fancy'
    """
def _dummyDirList(DirList, TixSubWidget):
    """
    'hlist'
    """
def _dummyDirSelectBox(DirSelectBox, TixSubWidget):
    """
    'dirlist'
    """
def _dummyExFileSelectBox(ExFileSelectBox, TixSubWidget):
    """
    'cancel'
    """
def _dummyFileSelectBox(FileSelectBox, TixSubWidget):
    """
    'dirlist'
    """
def _dummyFileComboBox(ComboBox, TixSubWidget):
    """
    'dircbx'
    """
def _dummyStdButtonBox(StdButtonBox, TixSubWidget):
    """
    'ok'
    """
def _dummyNoteBookFrame(NoteBookFrame, TixSubWidget):
    """

     Utility Routines ###


    mike Should tixDestroy be exposed as a wrapper? - but not for widgets.


    """
def OptionName(widget):
    """
    '''Returns the qualified path name for the widget. Normally used to set
        default options for subwidgets. See tixwidgets.py'''
    """
def FileTypeList(dict):
    """
    ''
    """
def CObjView(TixWidget):
    """
    This file implements the Canvas Object View widget. This is a base
        class of IconView. It implements automatic placement/adjustment of the
        scrollbars according to the canvas objects inside the canvas subwidget.
        The scrollbars are adjusted so that the canvas is just large enough
        to see all the objects.
    
    """
def Grid(TixWidget, XView, YView):
    """
    '''The Tix Grid command creates a new window  and makes it into a
        tixGrid widget. Additional options, may be specified on the command
        line or in the option database to configure aspects such as its cursor
        and relief.

        A Grid widget displays its contents in a two dimensional grid of cells.
        Each cell may contain one Tix display item, which may be in text,
        graphics or other formats. See the DisplayStyle class for more information
        about Tix display items. Individual cells, or groups of cells, can be
        formatted with a wide range of attributes, such as its color, relief and
        border.

        Subwidgets - None'''
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        'tixGrid'
        """
    def anchor_clear(self):
        """
        Removes the selection anchor.
        """
    def anchor_get(self):
        """
        Get the (x,y) coordinate of the current anchor cell
        """
    def anchor_set(self, x, y):
        """
        Set the selection anchor to the cell at (x, y).
        """
    def delete_row(self, from_, to=None):
        """
        Delete rows between from_ and to inclusive.
                If to is not provided,  delete only row at from_
        """
    def delete_column(self, from_, to=None):
        """
        Delete columns between from_ and to inclusive.
                If to is not provided,  delete only column at from_
        """
    def edit_apply(self):
        """
        If any cell is being edited, de-highlight the cell  and  applies
                the changes.
        """
    def edit_set(self, x, y):
        """
        Highlights  the  cell  at  (x, y) for editing, if the -editnotify
                command returns True for this cell.
        """
    def entrycget(self, x, y, option):
        """
        Get the option value for cell at (x,y)
        """
    def entryconfigure(self, x, y, cnf=None, **kw):
        """
        'entryconfigure'
        """
    def info_exists(self, x, y):
        """
        Return True if display item exists at (x,y)
        """
    def info_bbox(self, x, y):
        """
         This seems to always return '', at least for 'text' displayitems

        """
    def move_column(self, from_, to, offset):
        """
        Moves the range of columns from position FROM through TO by
                the distance indicated by OFFSET. For example, move_column(2, 4, 1)
                moves the columns 2,3,4 to columns 3,4,5.
        """
    def move_row(self, from_, to, offset):
        """
        Moves the range of rows from position FROM through TO by
                the distance indicated by OFFSET.
                For example, move_row(2, 4, 1) moves the rows 2,3,4 to rows 3,4,5.
        """
    def nearest(self, x, y):
        """
        Return coordinate of cell nearest pixel coordinate (x,y)
        """
    def set(self, x, y, itemtype=None, **kw):
        """
        '-itemtype'
        """
    def size_column(self, index, **kw):
        """
        Queries or sets the size of the column given by
                INDEX.  INDEX may be any non-negative
                integer that gives the position of a given column.
                INDEX can also be the string "default"; in this case, this command
                queries or sets the default size of all columns.
                When no option-value pair is given, this command returns a tuple
                containing the current size setting of the given column.  When
                option-value pairs are given, the corresponding options of the
                size setting of the given column are changed. Options may be one
                of the following:
                      pad0 pixels
                             Specifies the paddings to the left of a column.
                      pad1 pixels
                             Specifies the paddings to the right of a column.
                      size val
                             Specifies the width of a column.  Val may be:
                             "auto" -- the width of the column is set to the
                             width of the widest cell in the column;
                             a valid Tk screen distance unit;
                             or a real number following by the word chars
                             (e.g. 3.4chars) that sets the width of the column to the
                             given number of characters.
        """
    def size_row(self, index, **kw):
        """
        Queries or sets the size of the row given by
                INDEX. INDEX may be any non-negative
                integer that gives the position of a given row .
                INDEX can also be the string "default"; in this case, this command
                queries or sets the default size of all rows.
                When no option-value pair is given, this command returns a list con-
                taining the current size setting of the given row . When option-value
                pairs are given, the corresponding options of the size setting of the
                given row are changed. Options may be one of the following:
                      pad0 pixels
                             Specifies the paddings to the top of a row.
                      pad1 pixels
                             Specifies the paddings to the bottom of a row.
                      size val
                             Specifies the height of a row.  Val may be:
                             "auto" -- the height of the row is set to the
                             height of the highest cell in the row;
                             a valid Tk screen distance unit;
                             or a real number following by the word chars
                             (e.g. 3.4chars) that sets the height of the row to the
                             given number of characters.
        """
    def unset(self, x, y):
        """
        Clears the cell at (x, y) by removing its display item.
        """
def ScrolledGrid(Grid):
    """
    '''Scrolled Grid widgets'''
    """
    def __init__(self, master=None, cnf={}, **kw):
        """
        'tixScrolledGrid'
        """
