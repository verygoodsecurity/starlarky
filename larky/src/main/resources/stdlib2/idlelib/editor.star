def _sphinx_version():
    """
    Format sys.version_info to produce the Sphinx version string used to install the chm docs
    """
def EditorWindow(object):
    """
     for file names
    """
    def __init__(self, flist=None, filename=None, key=None, root=None):
        """
         Delay import: runscript imports pyshell imports EditorWindow.

        """
    def handle_winconfig(self, event=None):
        """
        'border'
        """
    def new_callback(self, event):
        """
        break
        """
    def home_callback(self, event):
        """
        Home
        """
    def set_status_bar(self):
        """
        'grey75'
        """
    def set_line_and_column(self, event=None):
        """
        '.'
        """
    def createmenubar(self):
        """
         Insert the application menu

        """
    def postwindowsmenu(self):
        """
         Only called when Window menu exists

        """
    def update_menu_label(self, menu, index, label):
        """
        Update label for menu item at index.
        """
    def update_menu_state(self, menu, index, state):
        """
        Update state for menu item at index.
        """
    def handle_yview(self, event, *args):
        """
        Handle scrollbar.
        """
    def right_menu_event(self, event):
        """
        f'@{event.x},{event.y}'
        """
    def make_rmenu(self):
        """
        'sel.first'
        """
    def rmenu_check_paste(self):
        """
        'tk::GetSelection'
        """
    def about_dialog(self, event=None):
        """
        Handle Help 'About IDLE' event.
        """
    def config_dialog(self, event=None):
        """
        Handle Options 'Configure IDLE' event.
        """
    def help_dialog(self, event=None):
        """
        Handle Help 'IDLE Help' event.
        """
    def python_docs(self, event=None):
        """
        'win'
        """
    def cut(self,event):
        """
        <<Cut>>
        """
    def copy(self,event):
        """
        sel
        """
    def paste(self,event):
        """
        <<Paste>>
        """
    def select_all(self, event=None):
        """
        sel
        """
    def remove_selection(self, event=None):
        """
        sel
        """
    def move_at_edge_if_selection(self, edge_index):
        """
        Cursor move begins at start or end of selection

                When a left/right cursor key is pressed create and return to Tkinter a
                function which causes a cursor move from the associated edge of the
                selection.

        
        """
        def move_at_edge(event):
            """
             no shift(==1) or control(==4) pressed
            """
    def del_word_left(self, event):
        """
        '<Meta-Delete>'
        """
    def del_word_right(self, event):
        """
        '<Meta-d>'
        """
    def find_event(self, event):
        """
        break
        """
    def find_again_event(self, event):
        """
        break
        """
    def find_selection_event(self, event):
        """
        break
        """
    def find_in_files_event(self, event):
        """
        break
        """
    def replace_event(self, event):
        """
        break
        """
    def goto_line_event(self, event):
        """
        Go To Line
        """
    def open_module(self):
        """
        Get module name from user and open it.

                Return module path or None for calls by open_module_browser
                when latter is not invoked in named editor window.
        
        """
    def open_module_event(self, event):
        """
        break
        """
    def open_module_browser(self, event=None):
        """
        'PyShellEditorWindow'

        """
    def open_path_browser(self, event=None):
        """
        break
        """
    def open_turtle_demo(self, event = None):
        """
        '-c'
        """
    def gotoline(self, lineno):
        """
        insert
        """
    def ispythonsource(self, filename):
        """
        .py
        """
    def close_hook(self):
        """
         can add more colorizers here...

        """
    def _rmcolorizer(self):
        """
        Update the color theme
        """
    def colorize_syntax_error(self, text, pos):
        """
        ERROR
        """
    def update_cursor_blink(self):
        """
        Update the cursor blink configuration.
        """
    def ResetFont(self):
        """
        Update the text widgets' font if it is changed
        """
    def RemoveKeybindings(self):
        """
        Remove the keybindings before they are changed.
        """
    def ApplyKeybindings(self):
        """
        Update the keybindings after they are changed
        """
    def set_notabs_indentwidth(self):
        """
        Update the indentwidth if changed and not using tabs in this window
        """
    def reset_help_menu_entries(self):
        """
        Update the additional help entries on the Help menu
        """
    def __extra_help_callback(self, helpfile):
        """
        Create a callback with the helpfile value frozen at definition time
        """
        def display_extra_help(helpfile=helpfile):
            """
            'www'
            """
    def update_recent_files_list(self, new_file=None):
        """
        Load and update the recent files list and menus
        """
    def __recent_file_callback(self, file_name):
        """
         - 
        """
    def get_saved(self):
        """
        untitled
        """
    def long_title(self):
        """

        """
    def center_insert_event(self, event):
        """
        break
        """
    def center(self, mark="insert"):
        """
        @0,0
        """
    def getlineno(self, mark="insert"):
        """
        Return (width, height, x, y)
        """
    def close_event(self, event):
        """
        break
        """
    def maybesave(self):
        """
        'normal'
        """
    def close(self):
        """
        cancel
        """
    def _close(self):
        """
         unless override: unregister from flist, terminate if last window

        """
    def load_extensions(self):
        """
        close
        """
    def load_standard_extensions(self):
        """
        Failed to load extension
        """
    def get_standard_extension_names(self):
        """
         Map built-in config-extension section names to file names.
        'ZzDummy'
        """
    def load_extension(self, name):
        """
        '.'
        """
    def apply_bindings(self, keydefs=None):
        """
        Add appropriate entries to the menus and submenus

                Menus that are absent or None in self.menudict are ignored.
        
        """
                    def command(text=text, eventname=eventname):
                        """
                         create a Tkinter variable object with self.text as master:

                        """
    def is_char_in_string(self, text_index):
        """
         Return true iff colorizer hasn't (re)gotten this far
         yet, or the character is tagged as being in a string

        """
    def get_selection_indices(self):
        """
        sel.first
        """
    def get_tk_tabwidth(self):
        """
        'tabs'
        """
    def set_tk_tabwidth(self, newtabwidth):
        """
         Set text widget tab width

        """
    def set_indentation_params(self, is_py_src, guess=True):
        """
        insert
        """
    def smart_indent_event(self, event):
        """
         if intraline selection:
             delete it
         elif multiline selection:
             do indent-region
         else:
             indent one level

        """
    def newline_and_indent_event(self, event):
        """
        Insert a newline and indentation after Enter keypress event.

                Properly position the cursor on the new line based on information
                from the current line.  This takes into account if the current line
                is a shell prompt, is empty, has selected text, contains a block
                opener, contains a block closer, is a continuation line, or
                is inside a string.
        
        """
    def _build_char_in_string_func(self, startindex):
        """
        +%dc
        """
    def _make_blanks(self, n):
        """
        '\t'
        """
    def reindent_to(self, column):
        """
        insert linestart
        """
    def guess_indent(self):
        """
        Show
        """
def index2line(index):
    """
    r'[ \t]*'
    """
def get_line_indent(line, tabwidth):
    """
    Return a line's indentation as (# chars, effective # of spaces).

        The effective # of spaces is the length after properly "expanding"
        the tabs into spaces, as done by str.expandtabs(tabwidth).
    
    """
def IndentSearcher(object):
    """
     .run() chews over the Text widget, looking for a block opener
     and the stmt following it.  Returns a pair,
         (line containing block opener, line containing stmt)
     Either or both may be None.


    """
    def __init__(self, text, tabwidth):
        """

        """
2021-03-02 20:54:25,016 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:25,016 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:25,016 : INFO : tokenize_signature : --> do i ever get here?
    def tokeneater(self, type, token, start, end, line,
                   INDENT=tokenize.INDENT,
                   NAME=tokenize.NAME,
                   OPENERS=('class', 'def', 'for', 'if', 'try', 'while')):
        """
         since we cut off the tokenizer early, we can trigger
         spurious errors

        """
def prepstr(s):
    """
     Helper to extract the underscore from a string, e.g.
     prepstr("Co_py") returns (2, "Copy").

    """
def get_accelerator(keydefs, eventname):
    """
     issue10940: temporary workaround to prevent hang with OS X Cocoa Tk 8.5
     if not keylist:

    """
def fixwordbreaks(root):
    """
     On Windows, tcl/tk breaks 'words' only on spaces, as in Command Prompt.
     We want Motif style everywhere. See #21474, msg218992 and followup.

    """
def _editor_window(parent):  # htest #
    """
     htest #
    """
