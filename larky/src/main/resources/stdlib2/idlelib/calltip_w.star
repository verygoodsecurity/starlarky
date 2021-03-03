def CalltipWindow(TooltipBase):
    """
    A call-tip widget for tkinter text widgets.
    """
    def __init__(self, text_widget):
        """
        Create a call-tip; shown by showtip().

                text_widget: a Text widget with code for which call-tips are desired
        
        """
    def get_position(self):
        """
        Choose the position of the call-tip.
        """
    def position_window(self):
        """
        Reposition the window if needed.
        """
    def showtip(self, text, parenleft, parenright):
        """
        Show the call-tip, bind events which will close it and reposition it.

                text: the text to display in the call-tip
                parenleft: index of the opening parenthesis in the text widget
                parenright: index of the closing parenthesis in the text widget,
                            or the end of the line if there is no closing parenthesis
        
        """
    def showcontents(self):
        """
        Create the call-tip widget.
        """
    def checkhide_event(self, event=None):
        """
        Handle CHECK_HIDE_EVENT: call hidetip or reschedule.
        """
    def hide_event(self, event):
        """
        Handle HIDE_EVENT by calling hidetip.
        """
    def hidetip(self):
        """
        Hide the call-tip.
        """
    def _bind_events(self):
        """
        Bind event handlers.
        """
    def _unbind_events(self):
        """
        Unbind event handlers.
        """
def _calltip_window(parent):  # htest #
    """
     htest #
    """
    def calltip_show(event):
        """
        (s='Hello world')
        """
    def calltip_hide(event):
        """
        <<calltip-show>>
        """
