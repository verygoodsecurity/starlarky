def InvalidConfigType(Exception): pass
    """

        A ConfigParser specialised for idle configuration file handling
    
    """
    def __init__(self, cfgFile, cfgDefaults=None):
        """

                cfgFile - string, fully specified configuration file name
        
        """
    def Get(self, section, option, type=None, default=None, raw=False):
        """

                Get an option value for given section/option or return default.
                If type is specified, return as type.
        
        """
    def GetOptionList(self, section):
        """
        Return a list of options for given section, else [].
        """
    def Load(self):
        """
        Load the configuration file from disk.
        """
def IdleUserConfParser(IdleConfParser):
    """

        IdleConfigParser specialised for user configuration handling.
    
    """
    def SetOption(self, section, option, value):
        """
        Return True if option is added or changed to value, else False.

                Add section if required.  False means option already had value.
        
        """
    def RemoveOption(self, section, option):
        """
        Return True if option is removed from section, else False.

                False if either section does not exist or did not have option.
        
        """
    def AddSection(self, section):
        """
        If section doesn't exist, add it.
        """
    def RemoveEmptySections(self):
        """
        Remove any sections that have no options.
        """
    def IsEmpty(self):
        """
        Return True if no sections after removing empty sections.
        """
    def Save(self):
        """
        Update user configuration file.

                If self not empty after removing empty sections, write the file
                to disk. Otherwise, remove the file from disk if it exists.
        
        """
def IdleConf:
    """
    Hold config parsers for all idle config files in singleton instance.

        Default config files, self.defaultCfg --
            for config_type in self.config_types:
                (idle install dir)/config-{config-type}.def

        User config files, self.userCfg --
            for config_type in self.config_types:
            (user home dir)/.idlerc/config-{config-type}.cfg
    
    """
    def __init__(self, _utest=False):
        """
        'main'
        """
    def CreateConfigHandlers(self):
        """
        Populate default and user config parser dictionaries.
        """
    def GetUserCfgDir(self):
        """
        Return a filesystem directory for storing user config files.

                Creates it if required.
        
        """
2021-03-02 20:54:18,797 : INFO : tokenize_signature : --> do i ever get here?
    def GetOption(self, configType, section, option, default=None, type=None,
                  warn_on_default=True, raw=False):
        """
        Return a value for configType section option, or default.

                If type is not None, return a value of that type.  Also pass raw
                to the config parser.  First try to return a valid value
                (including type) from a user configuration. If that fails, try
                the default configuration. If that fails, return default, with a
                default of None.

                Warn if either user or default configurations have an invalid value.
                Warn if default is returned and warn_on_default is True.
        
        """
    def SetOption(self, configType, section, option, value):
        """
        Set section option to value in user config file.
        """
    def GetSectionList(self, configSet, configType):
        """
        Return sections for configSet configType configuration.

                configSet must be either 'user' or 'default'
                configType must be in self.config_types.
        
        """
    def GetHighlight(self, theme, element):
        """
        Return dict of theme element highlight colors.

                The keys are 'foreground' and 'background'.  The values are
                tkinter color strings for configuring backgrounds and tags.
        
        """
    def GetThemeDict(self, type, themeName):
        """
        Return {option:value} dict for elements in themeName.

                type - string, 'default' or 'user' theme type
                themeName - string, theme name
                Values are loaded over ultimate fallback defaults to guarantee
                that all theme elements are present in a newly created theme.
        
        """
    def CurrentTheme(self):
        """
        Return the name of the currently active text color theme.
        """
    def CurrentKeys(self):
        """
        Return the name of the currently active key set.
        """
    def current_colors_and_keys(self, section):
        """
        Return the currently active name for Theme or Keys section.

                idlelib.config-main.def ('default') includes these sections

                [Theme]
                default= 1
                name= IDLE Classic
                name2=

                [Keys]
                default= 1
                name=
                name2=

                Item 'name2', is used for built-in ('default') themes and keys
                added after 2015 Oct 1 and 2016 July 1.  This kludge is needed
                because setting 'name' to a builtin not defined in older IDLEs
                to display multiple error messages or quit.
                See https://bugs.python.org/issue25313.
                When default = True, 'name2' takes precedence over 'name',
                while older IDLEs will just use name.  When default = False,
                'name2' may still be set, but it is ignored.
        
        """
    def default_keys():
        """
        'win'
        """
2021-03-02 20:54:18,801 : INFO : tokenize_signature : --> do i ever get here?
    def GetExtensions(self, active_only=True,
                      editor_only=False, shell_only=False):
        """
        Return extensions in default and user config-extensions files.

                If active_only True, only return active (enabled) extensions
                and optionally only editor or shell extensions.
                If active_only False, return all extensions.
        
        """
    def RemoveKeyBindNames(self, extnNameList):
        """
        Return extnNameList with keybinding section names removed.
        """
    def GetExtnNameForEvent(self, virtualEvent):
        """
        Return the name of the extension binding virtualEvent, or None.

                virtualEvent - string, name of the virtual event to test for,
                               without the enclosing '<< >>'
        
        """
    def GetExtensionKeys(self, extensionName):
        """
        Return dict: {configurable extensionName event : active keybinding}.

                Events come from default config extension_cfgBindings section.
                Keybindings come from GetCurrentKeySet() active key dict,
                where previously used bindings are disabled.
        
        """
    def __GetRawExtensionKeys(self,extensionName):
        """
        Return dict {configurable extensionName event : keybinding list}.

                Events come from default config extension_cfgBindings section.
                Keybindings list come from the splitting of GetOption, which
                tries user config before default config.
        
        """
    def GetExtensionBindings(self, extensionName):
        """
        Return dict {extensionName event : active or defined keybinding}.

                Augment self.GetExtensionKeys(extensionName) with mapping of non-
                configurable events (from default config) to GetOption splits,
                as in self.__GetRawExtensionKeys.
        
        """
    def GetKeyBinding(self, keySetName, eventStr):
        """
        Return the keybinding list for keySetName eventStr.

                keySetName - name of key binding set (config-keys section).
                eventStr - virtual event, including brackets, as in '<<event>>'.
        
        """
    def GetCurrentKeySet(self):
        """
        Return CurrentKeys with 'darwin' modifications.
        """
    def GetKeySet(self, keySetName):
        """
        Return event-key dict for keySetName core plus active extensions.

                If a binding defined in an extension is already in use, the
                extension binding is disabled by being set to ''
        
        """
    def IsCoreBinding(self, virtualEvent):
        """
        Return True if the virtual event is one of the core idle key events.

                virtualEvent - string, name of the virtual event to test for,
                               without the enclosing '<< >>'
        
        """
    def GetCoreKeys(self, keySetName=None):
        """
        Return dict of core virtual-key keybindings for keySetName.

                The default keySetName None corresponds to the keyBindings base
                dict. If keySetName is not None, bindings from the config
                file(s) are loaded _over_ these defaults, so if there is a
                problem getting any core binding there will be an 'ultimate last
                resort fallback' to the CUA-ish bindings defined here.
        
        """
    def GetExtraHelpSourceList(self, configSet):
        """
        Return list of extra help sources from a given configSet.

                Valid configSets are 'user' or 'default'.  Return a list of tuples of
                the form (menu_item , path_to_help_file , option), or return the empty
                list.  'option' is the sequence number of the help resource.  'option'
                values determine the position of the menu items on the Help menu,
                therefore the returned list must be sorted by 'option'.

        
        """
    def GetAllExtraHelpSourcesList(self):
        """
        Return a list of the details of all additional help sources.

                Tuples in the list are those of GetExtraHelpSourceList.
        
        """
    def GetFont(self, root, configType, section):
        """
        Retrieve a font from configuration (font, font-size, font-bold)
                Intercept the special value 'TkFixedFont' and substitute
                the actual font, factoring in some tweaks if needed for
                appearance sakes.

                The 'root' parameter can normally be any valid Tkinter widget.

                Return a tuple (family, size, weight) suitable for passing
                to tkinter.Font
        
        """
    def LoadCfgFiles(self):
        """
        Load all configuration files.
        """
    def SaveUserCfgFiles(self):
        """
        Write all loaded user configuration files to disk.
        """
def _warn(msg, *key):
    """
    Manage a user's proposed configuration option changes.

        Names used across multiple methods:
            page -- one of the 4 top-level dicts representing a
                    .idlerc/config-x.cfg file.
            config_type -- name of a page.
            section -- a section within a page/file.
            option -- name of an option within a section.
            value -- value for the option.

        Methods
            add_option: Add option and value to changes.
            save_option: Save option and value to config parser.
            save_all: Save all the changes to the config parser and file.
            delete_section: If section exists,
                            delete from changes, userCfg, and file.
            clear: Clear all changes by clearing each page.
    
    """
    def __init__(self):
        """
        Create a page for each configuration file
        """
    def add_option(self, config_type, section, item, value):
        """
        Add item/value pair for config_type and section.
        """
    def save_option(config_type, section, item, value):
        """
        Return True if the configuration value was added or changed.

                Helper for save_all.
        
        """
    def save_all(self):
        """
        Save configuration changes to the user config file.

                Clear self in preparation for additional changes.
                Return changed for testing.
        
        """
    def delete_section(self, config_type, section):
        """
        Delete a section from self, userCfg, and file.

                Used to delete custom themes and keysets.
        
        """
    def clear(self):
        """
        Clear all 4 pages.

                Called in save_all after saving to idleConf.
                XXX Mark window *title* when there are changes; unmark here.
        
        """
def _dump():  # htest # (not really, but ignore in coverage)
    """
     htest # (not really, but ignore in coverage)
    """
    def sprint(obj):
        """
        'utf-8'
        """
    def dumpCfg(cfg):
        """
        '\n'
        """
