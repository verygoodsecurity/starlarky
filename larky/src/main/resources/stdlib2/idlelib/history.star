def History:
    """
    ''' Implement Idle Shell history mechanism.

        store - Store source statement (called from pyshell.resetoutput).
        fetch - Fetch stored statement matching prefix already entered.
        history_next - Bound to <<history-next>> event (default Alt-N).
        history_prev - Bound to <<history-prev>> event (default Alt-P).
        '''
    """
    def __init__(self, text):
        """
        '''Initialize data attributes and bind event methods.

                .text - Idle wrapper of tk Text widget, with .bell().
                .history - source statements, possibly with multiple lines.
                .prefix - source already entered at prompt; filters history list.
                .pointer - index into history.
                .cyclic - wrap around history list (or not).
                '''
        """
    def history_next(self, event):
        """
        Fetch later statement; start with ealiest if cyclic.
        """
    def history_prev(self, event):
        """
        Fetch earlier statement; start with most recent.
        """
    def fetch(self, reverse):
        """
        '''Fetch statement and replace current line in text widget.

                Set prefix and pointer as needed for successive fetches.
                Reset them to None, None when returning to the start line.
                Sound bell when return to start line or cannot leave a line
                because cyclic is False.
                '''
        """
    def store(self, source):
        """
        Store Shell input statement into history list.
        """
