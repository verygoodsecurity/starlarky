def ParenMatch:
    """
    Highlight matching openers and closers, (), [], and {}.

        There are three supported styles of paren matching.  When a right
        paren (opener) is typed:

        opener -- highlight the matching left paren (closer);
        parens -- highlight the left and right parens (opener and closer);
        expression -- highlight the entire expression from opener to closer.
        (For back compatibility, 'default' is a synonym for 'opener').

        Flash-delay is the maximum milliseconds the highlighting remains.
        Any cursor movement (key press or click) before that removes the
        highlight.  If flash-delay is 0, there is no maximum.

        TODO:
        - Augment bell() with mismatch warning in status window.
        - Highlight when cursor is moved to the right of a closer.
          This might be too expensive to check.
    
    """
    def __init__(self, editwin):
        """
         Bind the check-restore event to the function restore_event,
         so that we can then use activate_restore (which calls event_add)
         and deactivate_restore (which calls event_delete).

        """
    def reload(cls):
        """
        'extensions'
        """
    def activate_restore(self):
        """
        Activate mechanism to restore text from highlighting.
        """
    def deactivate_restore(self):
        """
        Remove restore event bindings.
        """
    def flash_paren_event(self, event):
        """
        Handle editor 'show surrounding parens' event (menu or shortcut).
        """
    def paren_closed_event(self, event):
        """
        Handle user input of closer.
        """
    def finish_paren_event(self, indices):
        """
         self.create_tag(indices)

        """
    def restore_event(self, event=None):
        """
        Remove effect of doing match.
        """
    def handle_restore_timer(self, timer_count):
        """
         any one of the create_tag_XXX methods can be used depending on
         the style


        """
    def create_tag_opener(self, indices):
        """
        Highlight the single paren that matches
        """
    def create_tag_parens(self, indices):
        """
        Highlight the left and right parens
        """
    def create_tag_expression(self, indices):
        """
        Highlight the entire expression
        """
    def set_timeout_none(self):
        """
        Highlight will remain until user input turns it off
                or the insert has moved
        """
2021-03-02 20:54:26,286 : INFO : tokenize_signature : --> do i ever get here?
        def callme(callme, self=self, c=self.counter,
                   index=self.text.index("insert")):
            """
            insert
            """
    def set_timeout_last(self):
        """
        The last highlight created will be removed after FLASH_DELAY millisecs
        """
