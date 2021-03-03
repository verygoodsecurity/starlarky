def listicons(icondir=ICONDIR):
    """
    Utility to display the available icons.
    """
def wheel_event(event, widget=None):
    """
    Handle scrollwheel event.

        For wheel up, event.delta = 120*n on Windows, -1*n on darwin,
        where n can be > 1 if one scrolls fast.  Flicking the wheel
        generates up to maybe 20 events with n up to 10 or more 1.
        Macs use wheel down (delta = 1*n) to scroll up, so positive
        delta means to scroll up on both systems.

        X-11 sends Control-Button-4,5 events instead.

        The widget parameter is needed so browser label bindings can pass
        the underlying canvas.

        This function depends on widget.yview to not be overridden by
        a subclass.
    
    """
def TreeNode:
    """
    'collapsed'
    """
    def destroy(self):
        """
        .gif
        """
    def select(self, event=None):
        """
        'expanded'
        """
    def expand(self, event=None):
        """
        'expanded'
        """
    def collapse(self, event=None):
        """
        'collapsed'
        """
    def view(self):
        """
        'scrollregion'
        """
    def lastvisiblechild(self):
        """
        'expanded'
        """
    def update(self):
        """
        'cursor'
        """
    def draw(self, x, y):
        """
         XXX This hard-codes too many geometry constants!

        """
    def drawicon(self):
        """
        openfolder
        """
    def drawtext(self):
        """
        nw
        """
    def select_or_edit(self, event=None):
        """
        'text'
        """
    def edit_finish(self, event=None):
        """
        'text'
        """
    def edit_cancel(self, event=None):
        """
        Abstract class representing tree items.

            Methods should typically be overridden, otherwise a default action
            is used.

    
        """
    def __init__(self):
        """
        Constructor.  Do whatever you need to do.
        """
    def GetText(self):
        """
        Return text string to display.
        """
    def GetLabelText(self):
        """
        Return label text string to display in front of text (if any).
        """
    def _IsExpandable(self):
        """
        Do not override!  Called by TreeNode.
        """
    def IsExpandable(self):
        """
        Return whether there are subitems.
        """
    def _GetSubList(self):
        """
        Do not override!  Called by TreeNode.
        """
    def IsEditable(self):
        """
        Return whether the item's text may be edited.
        """
    def SetText(self, text):
        """
        Change the item's text (if it is editable).
        """
    def GetIconName(self):
        """
        Return name of icon to be displayed normally.
        """
    def GetSelectedIconName(self):
        """
        Return name of icon to be displayed when selected.
        """
    def GetSubList(self):
        """
        Return list of items forming sublist.
        """
    def OnDoubleClick(self):
        """
        Called on a double-click on the item.
        """
def FileTreeItem(TreeItem):
    """
    Example TreeItem subclass -- browse the file system.
    """
    def __init__(self, path):
        """

        """
    def SetText(self, text):
        """
        python XXX wish there was a "file" icon
        """
    def IsExpandable(self):
        """
         A canvas widget with scroll bars and some useful bindings


        """
def ScrolledCanvas:
    """
    'yscrollincrement'
    """
    def page_up(self, event):
        """
        page
        """
    def page_down(self, event):
        """
        page
        """
    def unit_up(self, event):
        """
        unit
        """
    def unit_down(self, event):
        """
        unit
        """
    def zoom_height(self, event):
        """
        break
        """
def _tree_widget(parent):  # htest #
    """
     htest #
    """
