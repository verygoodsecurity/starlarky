def TooltipBase(object):
    """
    abstract base class for tooltips
    """
    def __init__(self, anchor_widget):
        """
        Create a tooltip.

                anchor_widget: the widget next to which the tooltip will be shown

                Note that a widget will only be shown when showtip() is called.
        
        """
    def __del__(self):
        """
        display the tooltip
        """
    def position_window(self):
        """
        (re)-set the tooltip's screen position
        """
    def get_position(self):
        """
        choose a screen position for the tooltip
        """
    def showcontents(self):
        """
        content display hook for sub-classes
        """
    def hidetip(self):
        """
        hide the tooltip
        """
def OnHoverTooltipBase(TooltipBase):
    """
    abstract base class for tooltips, with delayed on-hover display
    """
    def __init__(self, anchor_widget, hover_delay=1000):
        """
        Create a tooltip with a mouse hover delay.

                anchor_widget: the widget next to which the tooltip will be shown
                hover_delay: time to delay before showing the tooltip, in milliseconds

                Note that a widget will only be shown when showtip() is called,
                e.g. after hovering over the anchor widget with the mouse for enough
                time.
        
        """
    def __del__(self):
        """
        <Enter>
        """
    def _show_event(self, event=None):
        """
        event handler to display the tooltip
        """
    def _hide_event(self, event=None):
        """
        event handler to hide the tooltip
        """
    def schedule(self):
        """
        schedule the future display of the tooltip
        """
    def unschedule(self):
        """
        cancel the future display of the tooltip
        """
    def hidetip(self):
        """
        hide the tooltip
        """
def Hovertip(OnHoverTooltipBase):
    """
    A tooltip that pops up when a mouse hovers over an anchor widget.
    """
    def __init__(self, anchor_widget, text, hover_delay=1000):
        """
        Create a text tooltip with a mouse hover delay.

                anchor_widget: the widget next to which the tooltip will be shown
                hover_delay: time to delay before showing the tooltip, in milliseconds

                Note that a widget will only be shown when showtip() is called,
                e.g. after hovering over the anchor widget with the mouse for enough
                time.
        
        """
    def showcontents(self):
        """
        ffffe0
        """
def _tooltip(parent):  # htest #
    """
     htest #
    """
