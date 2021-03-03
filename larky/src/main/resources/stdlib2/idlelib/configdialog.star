def ConfigDialog(Toplevel):
    """
    Config dialog for IDLE.
    
    """
    def __init__(self, parent, title='', *, _htest=False, _utest=False):
        """
        Show the tabbed dialog for user configuration.

                Args:
                    parent - parent of this dialog
                    title - string which is the title of this popup dialog
                    _htest - bool, change box location when running htest
                    _utest - bool, don't wait_window when running unittest

                Note: Focus set on font page fontlist.

                Methods:
                    create_widgets
                    cancel: Bound to DELETE_WINDOW protocol.
        
        """
    def create_widgets(self):
        """
        Create and place widgets for tabbed dialog.

                Widgets Bound to self:
                    note: Notebook
                    highpage: HighPage
                    fontpage: FontPage
                    keyspage: KeysPage
                    genpage: GenPage
                    extpage: self.create_page_extensions

                Methods:
                    create_action_buttons
                    load_configs: Load pages except for extensions.
                    activate_config_changes: Tell editors to reload.
        
        """
    def create_action_buttons(self):
        """
        Return frame of action buttons for dialog.

                Methods:
                    ok
                    apply
                    cancel
                    help

                Widget Structure:
                    outer: Frame
                        buttons: Frame
                            (no assignment): Button (ok)
                            (no assignment): Button (apply)
                            (no assignment): Button (cancel)
                            (no assignment): Button (help)
                        (no assignment): Frame
        
        """
    def ok(self):
        """
        Apply config changes, then dismiss dialog.

                Methods:
                    apply
                    destroy: inherited
        
        """
    def apply(self):
        """
        Apply config changes and leave dialog open.

                Methods:
                    deactivate_current_config
                    save_all_changed_extensions
                    activate_config_changes
        
        """
    def cancel(self):
        """
        Dismiss config dialog.

                Methods:
                    destroy: inherited
        
        """
    def destroy(self):
        """
        '1.0'
        """
    def help(self):
        """
        Create textview for config dialog help.

                Attributes accessed:
                    note
                Methods:
                    view_text: Method from textview module.
        
        """
    def deactivate_current_config(self):
        """
        Remove current key bindings.
                Iterate over window instances defined in parent and remove
                the keybindings.
        
        """
    def activate_config_changes(self):
        """
        Apply configuration changes to current windows.

                Dynamically update the current parent window instances
                with some of the configuration changes.
        
        """
    def create_page_extensions(self):
        """
        Part of the config dialog used for configuring IDLE extensions.

                This code is generic - it works for any and all IDLE extensions.

                IDLE extensions save their configuration options using idleConf.
                This code reads the current configuration using idleConf, supplies a
                GUI interface to change the configuration values, and saves the
                changes using idleConf.

                Not all changes take effect immediately - some may require restarting IDLE.
                This depends on each extension's implementation.

                All values are treated as text, and it is up to the user to supply
                reasonable values. The only exception to this are the 'enable*' options,
                which are boolean, and can be toggled with a True/False button.

                Methods:
                    load_extensions:
                    extension_selected: Handle selection from list.
                    create_extension_frame: Hold widgets for one extension.
                    set_extension_value: Set in userCfg['extensions'].
                    save_all_changed_extensions: Call extension page Save().
        
        """
    def load_extensions(self):
        """
        Fill self.extensions with data from the default and user configs.
        """
    def extension_selected(self, event):
        """
        Handle selection of an extension from the list.
        """
    def create_extension_frame(self, ext_name):
        """
        Create a frame holding the widgets to configure one extension
        """
    def set_extension_value(self, section, opt):
        """
        Return True if the configuration was added or changed.

                If the value is the same as the default, then remove it
                from user config file.
        
        """
    def save_all_changed_extensions(self):
        """
        Save configuration changes to the user config file.

                Attributes accessed:
                    extensions

                Methods:
                    set_extension_value
        
        """
def FontPage(Frame):
    """
    Return frame of widgets for Font/Tabs tab.

            Fonts: Enable users to provisionally change font face, size, or
            boldness and to see the consequence of proposed choices.  Each
            action set 3 options in changes structuree and changes the
            corresponding aspect of the font sample on this page and
            highlight sample on highlight page.

            Function load_font_cfg initializes font vars and widgets from
            idleConf entries and tk.

            Fontlist: mouse button 1 click or up or down key invoke
            on_fontlist_select(), which sets var font_name.

            Sizelist: clicking the menubutton opens the dropdown menu. A
            mouse button 1 click or return key sets var font_size.

            Bold_toggle: clicking the box toggles var font_bold.

            Changing any of the font vars invokes var_changed_font, which
            adds all 3 font options to changes and calls set_samples.
            Set_samples applies a new font constructed from the font vars to
            font_sample and to highlight_sample on the highlight page.

            Tabs: Enable users to change spaces entered for indent tabs.
            Changing indent_scale value with the mouse sets Var space_num,
            which invokes the default callback to add an entry to
            changes.  Load_tab_cfg initializes space_num to default.

            Widgets for FontPage(Frame):  (*) widgets bound to self
                frame_font: LabelFrame
                    frame_font_name: Frame
                        font_name_title: Label
                        (*)fontlist: ListBox - font_name
                        scroll_font: Scrollbar
                    frame_font_param: Frame
                        font_size_title: Label
                        (*)sizelist: DynOptionMenu - font_size
                        (*)bold_toggle: Checkbutton - font_bold
                frame_sample: LabelFrame
                    (*)font_sample: Label
                frame_indent: LabelFrame
                        indent_title: Label
                        (*)indent_scale: Scale - space_num
        
    """
    def load_font_cfg(self):
        """
        Load current configuration settings for the font options.

                Retrieve current font with idleConf.GetFont and font families
                from tk. Setup fontlist and set font_name.  Setup sizelist,
                which sets font_size.  Set font_bold.  Call set_samples.
        
        """
    def var_changed_font(self, *params):
        """
        Store changes to font attributes.

                When one font attribute changes, save them all, as they are
                not independent from each other. In particular, when we are
                overriding the default font, we need to write out everything.
        
        """
    def on_fontlist_select(self, event):
        """
        Handle selecting a font from the list.

                Event can result from either mouse click or Up or Down key.
                Set font_name and example displays to selection.
        
        """
    def set_samples(self, event=None):
        """
        Update update both screen samples with the font settings.

                Called on font initialization and change events.
                Accesses font_name, font_size, and font_bold Variables.
                Updates font_sample and highlight page highlight_sample.
        
        """
    def load_tab_cfg(self):
        """
        Load current configuration settings for the tab options.

                Attributes updated:
                    space_num: Set to value from idleConf.
        
        """
    def var_changed_space_num(self, *params):
        """
        Store change to indentation size.
        """
def HighPage(Frame):
    """
    Return frame of widgets for Highlighting tab.

            Enable users to provisionally change foreground and background
            colors applied to textual tags.  Color mappings are stored in
            complete listings called themes.  Built-in themes in
            idlelib/config-highlight.def are fixed as far as the dialog is
            concerned. Any theme can be used as the base for a new custom
            theme, stored in .idlerc/config-highlight.cfg.

            Function load_theme_cfg() initializes tk variables and theme
            lists and calls paint_theme_sample() and set_highlight_target()
            for the current theme.  Radiobuttons builtin_theme_on and
            custom_theme_on toggle var theme_source, which controls if the
            current set of colors are from a builtin or custom theme.
            DynOptionMenus builtinlist and customlist contain lists of the
            builtin and custom themes, respectively, and the current item
            from each list is stored in vars builtin_name and custom_name.

            Function paint_theme_sample() applies the colors from the theme
            to the tags in text widget highlight_sample and then invokes
            set_color_sample().  Function set_highlight_target() sets the state
            of the radiobuttons fg_on and bg_on based on the tag and it also
            invokes set_color_sample().

            Function set_color_sample() sets the background color for the frame
            holding the color selector.  This provides a larger visual of the
            color for the current tag and plane (foreground/background).

            Note: set_color_sample() is called from many places and is often
            called more than once when a change is made.  It is invoked when
            foreground or background is selected (radiobuttons), from
            paint_theme_sample() (theme is changed or load_cfg is called), and
            from set_highlight_target() (target tag is changed or load_cfg called).

            Button delete_custom invokes delete_custom() to delete
            a custom theme from idleConf.userCfg['highlight'] and changes.
            Button save_custom invokes save_as_new_theme() which calls
            get_new_theme_name() and create_new() to save a custom theme
            and its colors to idleConf.userCfg['highlight'].

            Radiobuttons fg_on and bg_on toggle var fg_bg_toggle to control
            if the current selected color for a tag is for the foreground or
            background.

            DynOptionMenu targetlist contains a readable description of the
            tags applied to Python source within IDLE.  Selecting one of the
            tags from this list populates highlight_target, which has a callback
            function set_highlight_target().

            Text widget highlight_sample displays a block of text (which is
            mock Python code) in which is embedded the defined tags and reflects
            the color attributes of the current theme and changes for those tags.
            Mouse button 1 allows for selection of a tag and updates
            highlight_target with that tag value.

            Note: The font in highlight_sample is set through the config in
            the fonts tab.

            In other words, a tag can be selected either from targetlist or
            by clicking on the sample text within highlight_sample.  The
            plane (foreground/background) is selected via the radiobutton.
            Together, these two (tag and plane) control what color is
            shown in set_color_sample() for the current theme.  Button set_color
            invokes get_color() which displays a ColorChooser to change the
            color for the selected tag/plane.  If a new color is picked,
            it will be saved to changes and the highlight_sample and
            frame background will be updated.

            Tk Variables:
                color: Color of selected target.
                builtin_name: Menu variable for built-in theme.
                custom_name: Menu variable for custom theme.
                fg_bg_toggle: Toggle for foreground/background color.
                    Note: this has no callback.
                theme_source: Selector for built-in or custom theme.
                highlight_target: Menu variable for the highlight tag target.

            Instance Data Attributes:
                theme_elements: Dictionary of tags for text highlighting.
                    The key is the display name and the value is a tuple of
                    (tag name, display sort order).

            Methods [attachment]:
                load_theme_cfg: Load current highlight colors.
                get_color: Invoke colorchooser [button_set_color].
                set_color_sample_binding: Call set_color_sample [fg_bg_toggle].
                set_highlight_target: set fg_bg_toggle, set_color_sample().
                set_color_sample: Set frame background to target.
                on_new_color_set: Set new color and add option.
                paint_theme_sample: Recolor sample.
                get_new_theme_name: Get from popup.
                create_new: Combine theme with changes and save.
                save_as_new_theme: Save [button_save_custom].
                set_theme_type: Command for [theme_source].
                delete_custom: Activate default [button_delete_custom].
                save_new: Save to userCfg['theme'] (is function).

            Widgets of highlights page frame:  (*) widgets bound to self
                frame_custom: LabelFrame
                    (*)highlight_sample: Text
                    (*)frame_color_set: Frame
                        (*)button_set_color: Button
                        (*)targetlist: DynOptionMenu - highlight_target
                    frame_fg_bg_toggle: Frame
                        (*)fg_on: Radiobutton - fg_bg_toggle
                        (*)bg_on: Radiobutton - fg_bg_toggle
                    (*)button_save_custom: Button
                frame_theme: LabelFrame
                    theme_type_title: Label
                    (*)builtin_theme_on: Radiobutton - theme_source
                    (*)custom_theme_on: Radiobutton - theme_source
                    (*)builtinlist: DynOptionMenu - builtin_name
                    (*)customlist: DynOptionMenu - custom_name
                    (*)button_delete_custom: Button
                    (*)theme_message: Label
        
    """
            def tem(event, elem=element):
                """
                 event.widget.winfo_top_level().highlight_target.set(elem)

                """
    def load_theme_cfg(self):
        """
        Load current configuration settings for the theme options.

                Based on the theme_source toggle, the theme is set as
                either builtin or custom and the initial widget values
                reflect the current settings from idleConf.

                Attributes updated:
                    theme_source: Set from idleConf.
                    builtinlist: List of default themes from idleConf.
                    customlist: List of custom themes from idleConf.
                    custom_theme_on: Disabled if there are no custom themes.
                    custom_theme: Message with additional information.
                    targetlist: Create menu from self.theme_elements.

                Methods:
                    set_theme_type
                    paint_theme_sample
                    set_highlight_target
        
        """
    def var_changed_builtin_name(self, *params):
        """
        Process new builtin theme selection.

                Add the changed theme's name to the changed_items and recreate
                the sample with the values from the selected theme.
        
        """
    def var_changed_custom_name(self, *params):
        """
        Process new custom theme selection.

                If a new custom theme is selected, add the name to the
                changed_items and apply the theme to the sample.
        
        """
    def var_changed_theme_source(self, *params):
        """
        Process toggle between builtin and custom theme.

                Update the default toggle value and apply the newly
                selected theme type.
        
        """
    def var_changed_color(self, *params):
        """
        Process change to color choice.
        """
    def var_changed_highlight_target(self, *params):
        """
        Process selection of new target tag for highlighting.
        """
    def set_theme_type(self):
        """
        Set available screen options based on builtin or custom theme.

                Attributes accessed:
                    theme_source

                Attributes updated:
                    builtinlist
                    customlist
                    button_delete_custom
                    custom_theme_on

                Called from:
                    handler for builtin_theme_on and custom_theme_on
                    delete_custom
                    create_new
                    load_theme_cfg
        
        """
    def get_color(self):
        """
        Handle button to select a new color for the target tag.

                If a new color is selected while using a builtin theme, a
                name must be supplied to create a custom theme.

                Attributes accessed:
                    highlight_target
                    frame_color_set
                    theme_source

                Attributes updated:
                    color

                Methods:
                    get_new_theme_name
                    create_new
        
        """
    def on_new_color_set(self):
        """
        Display sample of new color selection on the dialog.
        """
    def get_new_theme_name(self, message):
        """
        Return name of new theme from query popup.
        """
    def save_as_new_theme(self):
        """
        Prompt for new theme name and create the theme.

                Methods:
                    get_new_theme_name
                    create_new
        
        """
    def create_new(self, new_theme_name):
        """
        Create a new custom theme with the given name.

                Create the new theme based on the previously active theme
                with the current changes applied.  Once it is saved, then
                activate the new theme.

                Attributes accessed:
                    builtin_name
                    custom_name

                Attributes updated:
                    customlist
                    theme_source

                Method:
                    save_new
                    set_theme_type
        
        """
    def set_highlight_target(self):
        """
        Set fg/bg toggle and color based on highlight tag target.

                Instance variables accessed:
                    highlight_target

                Attributes updated:
                    fg_on
                    bg_on
                    fg_bg_toggle

                Methods:
                    set_color_sample

                Called from:
                    var_changed_highlight_target
                    load_theme_cfg
        
        """
    def set_color_sample_binding(self, *args):
        """
        Change color sample based on foreground/background toggle.

                Methods:
                    set_color_sample
        
        """
    def set_color_sample(self):
        """
        Set the color of the frame background to reflect the selected target.

                Instance variables accessed:
                    theme_elements
                    highlight_target
                    fg_bg_toggle
                    highlight_sample

                Attributes updated:
                    frame_color_set
        
        """
    def paint_theme_sample(self):
        """
        Apply the theme colors to each element tag in the sample text.

                Instance attributes accessed:
                    theme_elements
                    theme_source
                    builtin_name
                    custom_name

                Attributes updated:
                    highlight_sample: Set the tag elements to the theme.

                Methods:
                    set_color_sample

                Called from:
                    var_changed_builtin_name
                    var_changed_custom_name
                    load_theme_cfg
        
        """
    def save_new(self, theme_name, theme):
        """
        Save a newly created theme to idleConf.

                theme_name - string, the name of the new theme
                theme - dictionary containing the new theme
        
        """
    def askyesno(self, *args, **kwargs):
        """
         Make testing easier.  Could change implementation.

        """
    def delete_custom(self):
        """
        Handle event to delete custom theme.

                The current theme is deactivated and the default theme is
                activated.  The custom theme is permanently removed from
                the config file.

                Attributes accessed:
                    custom_name

                Attributes updated:
                    custom_theme_on
                    customlist
                    theme_source
                    builtin_name

                Methods:
                    deactivate_current_config
                    save_all_changed_extensions
                    activate_config_changes
                    set_theme_type
        
        """
def KeysPage(Frame):
    """
    Return frame of widgets for Keys tab.

            Enable users to provisionally change both individual and sets of
            keybindings (shortcut keys). Except for features implemented as
            extensions, keybindings are stored in complete sets called
            keysets. Built-in keysets in idlelib/config-keys.def are fixed
            as far as the dialog is concerned. Any keyset can be used as the
            base for a new custom keyset, stored in .idlerc/config-keys.cfg.

            Function load_key_cfg() initializes tk variables and keyset
            lists and calls load_keys_list for the current keyset.
            Radiobuttons builtin_keyset_on and custom_keyset_on toggle var
            keyset_source, which controls if the current set of keybindings
            are from a builtin or custom keyset. DynOptionMenus builtinlist
            and customlist contain lists of the builtin and custom keysets,
            respectively, and the current item from each list is stored in
            vars builtin_name and custom_name.

            Button delete_custom_keys invokes delete_custom_keys() to delete
            a custom keyset from idleConf.userCfg['keys'] and changes.  Button
            save_custom_keys invokes save_as_new_key_set() which calls
            get_new_keys_name() and create_new_key_set() to save a custom keyset
            and its keybindings to idleConf.userCfg['keys'].

            Listbox bindingslist contains all of the keybindings for the
            selected keyset.  The keybindings are loaded in load_keys_list()
            and are pairs of (event, [keys]) where keys can be a list
            of one or more key combinations to bind to the same event.
            Mouse button 1 click invokes on_bindingslist_select(), which
            allows button_new_keys to be clicked.

            So, an item is selected in listbindings, which activates
            button_new_keys, and clicking button_new_keys calls function
            get_new_keys().  Function get_new_keys() gets the key mappings from the
            current keyset for the binding event item that was selected.  The
            function then displays another dialog, GetKeysDialog, with the
            selected binding event and current keys and allows new key sequences
            to be entered for that binding event.  If the keys aren't
            changed, nothing happens.  If the keys are changed and the keyset
            is a builtin, function get_new_keys_name() will be called
            for input of a custom keyset name.  If no name is given, then the
            change to the keybinding will abort and no updates will be made.  If
            a custom name is entered in the prompt or if the current keyset was
            already custom (and thus didn't require a prompt), then
            idleConf.userCfg['keys'] is updated in function create_new_key_set()
            with the change to the event binding.  The item listing in bindingslist
            is updated with the new keys.  Var keybinding is also set which invokes
            the callback function, var_changed_keybinding, to add the change to
            the 'keys' or 'extensions' changes tracker based on the binding type.

            Tk Variables:
                keybinding: Action/key bindings.

            Methods:
                load_keys_list: Reload active set.
                create_new_key_set: Combine active keyset and changes.
                set_keys_type: Command for keyset_source.
                save_new_key_set: Save to idleConf.userCfg['keys'] (is function).
                deactivate_current_config: Remove keys bindings in editors.

            Widgets for KeysPage(frame):  (*) widgets bound to self
                frame_key_sets: LabelFrame
                    frames[0]: Frame
                        (*)builtin_keyset_on: Radiobutton - var keyset_source
                        (*)custom_keyset_on: Radiobutton - var keyset_source
                        (*)builtinlist: DynOptionMenu - var builtin_name,
                                func keybinding_selected
                        (*)customlist: DynOptionMenu - var custom_name,
                                func keybinding_selected
                        (*)keys_message: Label
                    frames[1]: Frame
                        (*)button_delete_custom_keys: Button - delete_custom_keys
                        (*)button_save_custom_keys: Button -  save_as_new_key_set
                frame_custom: LabelFrame
                    frame_target: Frame
                        target_title: Label
                        scroll_target_y: Scrollbar
                        scroll_target_x: Scrollbar
                        (*)bindingslist: ListBox - on_bindingslist_select
                        (*)button_new_keys: Button - get_new_keys & ..._name
        
    """
    def load_key_cfg(self):
        """
        Load current configuration settings for the keybinding options.
        """
    def var_changed_builtin_name(self, *params):
        """
        Process selection of builtin key set.
        """
    def var_changed_custom_name(self, *params):
        """
        Process selection of custom key set.
        """
    def var_changed_keyset_source(self, *params):
        """
        Process toggle between builtin key set and custom key set.
        """
    def var_changed_keybinding(self, *params):
        """
        Store change to a keybinding.
        """
    def set_keys_type(self):
        """
        Set available screen options based on builtin or custom key set.
        """
    def get_new_keys(self):
        """
        Handle event to change key binding for selected line.

                A selection of a key/binding in the list of current
                bindings pops up a dialog to enter a new binding.  If
                the current key set is builtin and a binding has
                changed, then a name for a custom key set needs to be
                entered for the change to be applied.
        
        """
    def get_new_keys_name(self, message):
        """
        Return new key set name from query popup.
        """
    def save_as_new_key_set(self):
        """
        Prompt for name of new key set and save changes using that name.
        """
    def on_bindingslist_select(self, event):
        """
        Activate button to assign new keys to selected action.
        """
    def create_new_key_set(self, new_key_set_name):
        """
        Create a new custom key set with the given name.

                Copy the bindings/keys from the previously active keyset
                to the new keyset and activate the new custom keyset.
        
        """
    def load_keys_list(self, keyset_name):
        """
        Reload the list of action/key binding pairs for the active key set.

                An action/key binding can be selected to change the key binding.
        
        """
    def save_new_key_set(keyset_name, keyset):
        """
        Save a newly created core key set.

                Add keyset to idleConf.userCfg['keys'], not to disk.
                If the keyset doesn't exist, it is created.  The
                binding/keys are taken from the keyset argument.

                keyset_name - string, the name of the new key set
                keyset - dictionary containing the new keybindings
        
        """
    def askyesno(self, *args, **kwargs):
        """
         Make testing easier.  Could change implementation.

        """
    def delete_custom_keys(self):
        """
        Handle event to delete a custom key set.

                Applying the delete deactivates the current configuration and
                reverts to the default.  The custom key set is permanently
                deleted from the config file.
        
        """
def GenPage(Frame):
    """
    r'[0-9]*'
    """
        def is_digits_or_empty(s):
            """
            Return 's is blank or contains only digits'
            """
    def create_page_general(self):
        """
        Return frame of widgets for General tab.

                Enable users to provisionally change general options. Function
                load_general_cfg initializes tk variables and helplist using
                idleConf.  Radiobuttons startup_shell_on and startup_editor_on
                set var startup_edit. Radiobuttons save_ask_on and save_auto_on
                set var autosave. Entry boxes win_width_int and win_height_int
                set var win_width and win_height.  Setting var_name invokes the
                default callback that adds option to changes.

                Helplist: load_general_cfg loads list user_helplist with
                name, position pairs and copies names to listbox helplist.
                Clicking a name invokes help_source selected. Clicking
                button_helplist_name invokes helplist_item_name, which also
                changes user_helplist.  These functions all call
                set_add_delete_state. All but load call update_help_changes to
                rewrite changes['main']['HelpFiles'].

                Widgets for GenPage(Frame):  (*) widgets bound to self
                    frame_window: LabelFrame
                        frame_run: Frame
                            startup_title: Label
                            (*)startup_editor_on: Radiobutton - startup_edit
                            (*)startup_shell_on: Radiobutton - startup_edit
                        frame_win_size: Frame
                            win_size_title: Label
                            win_width_title: Label
                            (*)win_width_int: Entry - win_width
                            win_height_title: Label
                            (*)win_height_int: Entry - win_height
                        frame_cursor_blink: Frame
                            cursor_blink_title: Label
                            (*)cursor_blink_bool: Checkbutton - cursor_blink
                        frame_autocomplete: Frame
                            auto_wait_title: Label
                            (*)auto_wait_int: Entry - autocomplete_wait
                        frame_paren1: Frame
                            paren_style_title: Label
                            (*)paren_style_type: OptionMenu - paren_style
                        frame_paren2: Frame
                            paren_time_title: Label
                            (*)paren_flash_time: Entry - flash_delay
                            (*)bell_on: Checkbutton - paren_bell
                    frame_editor: LabelFrame
                        frame_save: Frame
                            run_save_title: Label
                            (*)save_ask_on: Radiobutton - autosave
                            (*)save_auto_on: Radiobutton - autosave
                        frame_format: Frame
                            format_width_title: Label
                            (*)format_width_int: Entry - format_width
                        frame_line_numbers_default: Frame
                            line_numbers_default_title: Label
                            (*)line_numbers_default_bool: Checkbutton - line_numbers_default
                        frame_context: Frame
                            context_title: Label
                            (*)context_int: Entry - context_lines
                    frame_shell: LabelFrame
                        frame_auto_squeeze_min_lines: Frame
                            auto_squeeze_min_lines_title: Label
                            (*)auto_squeeze_min_lines_int: Entry - auto_squeeze_min_lines
                    frame_help: LabelFrame
                        frame_helplist: Frame
                            frame_helplist_buttons: Frame
                                (*)button_helplist_edit
                                (*)button_helplist_add
                                (*)button_helplist_remove
                            (*)helplist: ListBox
                            scroll_helplist: Scrollbar
        
        """
    def load_general_cfg(self):
        """
        Load current configuration settings for the general options.
        """
    def help_source_selected(self, event):
        """
        Handle event for selecting additional help.
        """
    def set_add_delete_state(self):
        """
        Toggle the state for the help list buttons based on list entries.
        """
    def helplist_item_add(self):
        """
        Handle add button for the help list.

                Query for name and location of new help sources and add
                them to the list.
        
        """
    def helplist_item_edit(self):
        """
        Handle edit button for the help list.

                Query with existing help source information and update
                config if the values are changed.
        
        """
    def helplist_item_remove(self):
        """
        Handle remove button for the help list.

                Delete the help list item from config.
        
        """
    def update_help_changes(self):
        """
        Clear and rebuild the HelpFiles section in changes
        """
def VarTrace:
    """
    Maintain Tk variables trace state.
    """
    def __init__(self):
        """
        Store Tk variables and callbacks.

                untraced: List of tuples (var, callback)
                    that do not have the callback attached
                    to the Tk var.
                traced: List of tuples (var, callback) where
                    that callback has been attached to the var.
        
        """
    def clear(self):
        """
        Clear lists (for tests).
        """
    def add(self, var, callback):
        """
        Add (var, callback) tuple to untraced list.

                Args:
                    var: Tk variable instance.
                    callback: Either function name to be used as a callback
                        or a tuple with IdleConf config-type, section, and
                        option names used in the default callback.

                Return:
                    Tk variable instance.
        
        """
    def make_callback(var, config):
        """
        Return default callback function to add values to changes instance.
        """
        def default_callback(*params):
            """
            Add config values to changes instance.
            """
    def attach(self):
        """
        Attach callback to all vars that are not traced.
        """
    def detach(self):
        """
        Remove callback from traced vars.
        """
def is_int(s):
    """
    Return 's is blank or represents an int'
    """
def VerticalScrolledFrame(Frame):
    """
    A pure Tkinter vertically scrollable frame.

        * Use the 'interior' attribute to place widgets inside the scrollable frame
        * Construct and pack/place/grid normally
        * This frame only allows vertical scrolling
    
    """
    def __init__(self, parent, *args, **kw):
        """
         Create a canvas object and a vertical scrollbar for scrolling it.

        """
        def _configure_interior(event):
            """
             Update the scrollbars to match the size of the inner frame.

            """
        def _configure_canvas(event):
            """
             Update the inner frame's width to fill the canvas.

            """
