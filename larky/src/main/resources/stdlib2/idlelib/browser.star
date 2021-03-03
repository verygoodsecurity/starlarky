def transform_children(child_dict, modname=None):
    """
    Transform a child dictionary to an ordered sequence of objects.

        The dictionary maps names to pyclbr information objects.
        Filter out imported objects.
        Augment class names with bases.
        The insertion order of the dictionary is assumed to have been in line
        number order, so sorting is not necessary.

        The current tree only calls this once per child_dict as it saves
        TreeItems once created.  A future tree and tests might violate this,
        so a check prevents multiple in-place augmentations.
    
    """
def ModuleBrowser:
    """
    Browse module classes and functions in IDLE.
    
    """
    def __init__(self, master, path, *, _htest=False, _utest=False):
        """
        Create a window for browsing a module's structure.

                Args:
                    master: parent for widgets.
                    path: full path of file to browse.
                    _htest - bool; change box location when running htest.
                    -utest - bool; suppress contents when running unittest.

                Global variables:
                    file_open: Function used for opening a file.

                Instance variables:
                    name: Module name.
                    file: Full path and module with .py extension.  Used in
                        creating ModuleBrowserTreeItem as the rootnode for
                        the tree and subsequently in the children.
        
        """
    def close(self, event=None):
        """
        Dismiss the window and the tree nodes.
        """
    def init(self):
        """
        Create browser tkinter widgets, including the tree.
        """
    def settitle(self):
        """
        Set the window title.
        """
    def rootnode(self):
        """
        Return a ModuleBrowserTreeItem as the root of the tree.
        """
def ModuleBrowserTreeItem(TreeItem):
    """
    Browser tree for Python module.

        Uses TreeItem as the basis for the structure of the tree.
        Used by both browsers.
    
    """
    def __init__(self, file):
        """
        Create a TreeItem for the file.

                Args:
                    file: Full path and module name.
        
        """
    def GetText(self):
        """
        Return the module name as the text string to display.
        """
    def GetIconName(self):
        """
        Return the name of the icon to display.
        """
    def GetSubList(self):
        """
        Return ChildBrowserTreeItems for children.
        """
    def OnDoubleClick(self):
        """
        Open a module in an editor window when double clicked.
        """
    def IsExpandable(self):
        """
        Return True if Python (.py) file.
        """
    def listchildren(self):
        """
        Return sequenced classes and functions in the module.
        """
def ChildBrowserTreeItem(TreeItem):
    """
    Browser tree for child nodes within the module.

        Uses TreeItem as the basis for the structure of the tree.
    
    """
    def __init__(self, obj):
        """
        Create a TreeItem for a pyclbr class/function object.
        """
    def GetText(self):
        """
        Return the name of the function/class to display.
        """
    def GetIconName(self):
        """
        Return the name of the icon to display.
        """
    def IsExpandable(self):
        """
        Return True if self.obj has nested objects.
        """
    def GetSubList(self):
        """
        Return ChildBrowserTreeItems for children.
        """
    def OnDoubleClick(self):
        """
        Open module with file_open and position to lineno.
        """
def _module_browser(parent): # htest #
    """
     htest #
    """
        def Nested_in_func(TreeNode):
    """
    __main__
    """
