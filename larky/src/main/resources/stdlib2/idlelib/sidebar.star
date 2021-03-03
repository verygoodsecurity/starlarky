def get_end_linenumber(text):
    """
    Utility to get the last line's number in a Tk text widget.
    """
def get_widget_padding(widget):
    """
    Get the total padding of a Tk widget, including its border.
    """
def BaseSideBar:
    """

        The base class for extensions which require a sidebar.
    
    """
    def __init__(self, editwin):
        """
        'yscrollcommand'
        """
    def update_font(self):
        """
        Update the sidebar text font, usually after config changes.
        """
    def _update_font(self, font):
        """
        'font'
        """
    def update_colors(self):
        """
        Update the sidebar text colors, usually after config changes.
        """
    def _update_colors(self, foreground, background):
        """
        Redirect vertical scrolling to the main editor text widget.

                The scroll bar is also updated.
        
        """
    def redirect_focusin_event(self, event):
        """
        Redirect focus-in events to the main editor text widget.
        """
    def redirect_mousebutton_event(self, event, event_name):
        """
        Redirect mouse button events to the main editor text widget.
        """
    def redirect_mousewheel_event(self, event):
        """
        Redirect mouse wheel events to the editwin text widget.
        """
def EndLineDelegator(Delegator):
    """
    Generate callbacks with the current end line number after
           insert or delete operations
    """
    def __init__(self, changed_callback):
        """

                changed_callback - Callable, will be called after insert
                                   or delete operations with the current
                                   end line number.
        
        """
    def insert(self, index, chars, tags=None):
        """
        Line numbers support for editor windows.
        """
    def __init__(self, editwin):
        """
        'width'
        """
    def bind_events(self):
        """
         Ensure focus is always redirected to the main editor text widget.

        """
        def bind_mouse_event(event_name, target_event_name):
            """
            f'<Button-{button}>'
            """
        def b1_mousedown_handler(event):
            """
             select the entire line

            """
        def b1_mouseup_handler(event):
            """
             On mouse up, we're no longer dragging.  Set the shared persistent
             variables to None to represent this.

            """
        def drag_update_selection_and_insert_mark(y_coord):
            """
            Helper function for drag and selection event handlers.
            """
        def b1_drag_handler(event, *args):
            """
            '<B1-Motion>'
            """
        def selection_handler(event):
            """
             This logic is only needed while dragging.

            """
    def update_colors(self):
        """
        Update the sidebar text colors, usually after config changes.
        """
    def update_sidebar_text(self, end):
        """

                Perform the following action:
                Each line sidebar_text contains the linenumber for that line
                Synchronize with editwin.text so that both sidebar_text and
                editwin.text contain the same number of lines
        """
def _linenumbers_drag_scrolling(parent):  # htest #
    """
     htest #
    """
