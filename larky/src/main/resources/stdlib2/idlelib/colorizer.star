def any(name, alternates):
    """
    Return a named group pattern matching list of alternates.
    """
def make_pat():
    """
    r"\b
    """
def color_config(text):
    """
    Set color options of Text widget.

        If ColorDelegator is used, this should be called first.
    
    """
def ColorDelegator(Delegator):
    """
    Delegator for syntax highlighting (text coloring).

        Instance variables:
            delegate: Delegator below this one in the stack, meaning the
                    one this one delegates to.

            Used to track state:
            after_id: Identifier for scheduled after event, which is a
                    timer for colorizing the text.
            allow_colorizing: Boolean toggle for applying colorizing.
            colorizing: Boolean flag when colorizing is in process.
            stop_colorizing: Boolean flag to end an active colorizing
                    process.
    
    """
    def __init__(self):
        """
        Initialize variables that track colorizing state.
        """
    def setdelegate(self, delegate):
        """
        Set the delegate for this instance.

                A delegate is an instance of a Delegator class and each
                delegate points to the next delegator in the stack.  This
                allows multiple delegators to be chained together for a
                widget.  The bottom delegate for a colorizer is a Text
                widget.

                If there is a delegate, also start the colorizing process.
        
        """
    def config_colors(self):
        """
        Configure text widget tags with colors from tagdefs.
        """
    def LoadTagDefs(self):
        """
        Create dictionary of tag names to text colors.
        """
    def insert(self, index, chars, tags=None):
        """
        Insert chars into widget at index and mark for colorizing.
        """
    def delete(self, index1, index2=None):
        """
        Delete chars between indexes and mark for colorizing.
        """
    def notify_range(self, index1, index2=None):
        """
        Mark text changes for processing and restart colorizing, if active.
        """
    def close(self):
        """
        cancel scheduled recolorizer
        """
    def toggle_colorize_event(self, event=None):
        """
        Toggle colorizing on and off.

                When toggling off, if colorizing is scheduled or is in
                process, it will be cancelled and/or stopped.

                When toggling on, colorizing will be scheduled.
        
        """
    def recolorize(self):
        """
        Timer event (every 1ms) to colorize text.

                Colorizing is only attempted when the text widget exists,
                when colorizing is toggled on, and when the colorizing
                process is not already running.

                After colorizing is complete, some cleanup is done to
                make sure that all the text has been colorized.
        
        """
    def recolorize_main(self):
        """
        Evaluate text and apply colorizing tags.
        """
    def removecolors(self):
        """
        Remove all colorizing tags.
        """
def _color_delegator(parent):  # htest #
    """
     htest #
    """
