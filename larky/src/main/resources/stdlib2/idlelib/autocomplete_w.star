def AutoCompleteWindow:
    """
     The widget (Text) on which we place the AutoCompleteWindow

    """
    def _change_start(self, newstart):
        """
        %s+%dc
        """
    def _binary_search(self, s):
        """
        Find the first index in self.completions where completions[i] is
                greater or equal to s, or the last index if there is no such.
        
        """
    def _complete_string(self, s):
        """
        Assuming that s is the prefix of a string in self.completions,
                return the longest string which is a prefix of all the strings which
                s is a prefix of them. If s is not a prefix of a string, return s.
        
        """
    def _selection_changed(self):
        """
        Call when the selection of the Listbox has changed.

                Updates the Listbox display and calls _change_start.
        
        """
    def show_window(self, comp_lists, index, complete, mode, userWantsWin):
        """
        Show the autocomplete list, bind events.

                If complete is True, complete the text, and if there is exactly
                one matching completion, don't open a list.
        
        """
    def winconfig_event(self, event):
        """
         Avoid running on recursive <Configure> callback invocations.

        """
    def _hide_event_check(self):
        """
         See issue 734176, when user click on menu, acw.focus_get()
         will get KeyError.

        """
    def hide_event(self, event):
        """
         Hide autocomplete list if it exists and does not have focus or
         mouse click on widget / text area.

        """
    def listselect_event(self, event):
        """
         Put the selected completion in the text, and close the list

        """
    def keypress_event(self, event):
        """
        mc_state
        """
    def keyrelease_event(self, event):
        """
        insert
        """
    def is_active(self):
        """
         The selection doesn't change.


        """
    def hide_window(self):
        """
         unbind events

        """
