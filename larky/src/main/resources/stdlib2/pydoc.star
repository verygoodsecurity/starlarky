def pathdirs():
    """
    Convert sys.path into a list of absolute, existing, unique paths.
    """
def getdoc(object):
    """
    Get the doc string or comments for an object.
    """
def splitdoc(doc):
    """
    Split a doc string into a synopsis line (if any) and the rest.
    """
def classname(object, modname):
    """
    Get a class name and qualify it with a module name if necessary.
    """
def isdata(object):
    """
    Check if an object is of a type that probably means it's data.
    """
def replace(text, *pairs):
    """
    Do a series of global replacements on a string.
    """
def cram(text, maxlen):
    """
    Omit part of a string if needed to make it fit in a maximum length.
    """
def stripid(text):
    """
    Remove the hexadecimal id from a Python object representation.
    """
def _is_bound_method(fn):
    """

        Returns True if fn is a bound method, regardless of whether
        fn was implemented in Python or in C.
    
    """
def allmethods(cl):
    """
     all your base are belong to us
    """
def _split_list(s, predicate):
    """
    Split sequence s via predicate, and return pair ([true], [false]).

        The return value is a 2-tuple of lists,
            ([x for x in s if predicate(x)],
             [x for x in s if not predicate(x)])
    
    """
def visiblename(name, all=None, obj=None):
    """
    Decide whether to show documentation on a variable.
    """
def classify_class_attrs(object):
    """
    Wrap inspect.classify_class_attrs, with fixup for data descriptors.
    """
def sort_attributes(attrs, object):
    """
    'Sort the attrs list in-place by _fields and then alphabetically by name'
    """
def ispackage(path):
    """
    Guess whether a path refers to a package directory.
    """
def source_synopsis(file):
    """
    '#'
    """
def synopsis(filename, cache={}):
    """
    Get the one-line summary out of a module file.
    """
def ErrorDuringImport(Exception):
    """
    Errors that occurred while trying to import something to document it.
    """
    def __init__(self, filename, exc_info):
        """
        'problem in %s - %s: %s'
        """
def importfile(path):
    """
    Import a Python source file or compiled file given its path.
    """
def safeimport(path, forceload=0, cache={}):
    """
    Import a module; handle errors; return None if the module isn't found.

        If the module *is* found but an exception occurs, it's wrapped in an
        ErrorDuringImport exception and reraised.  Unlike __import__, if a
        package path is specified, the module at the end of the path is returned,
        not the package at the beginning.  If the optional 'forceload' argument
        is 1, we reload the module from disk (unless it's a dynamic extension).
    """
def Doc:
    """
    PYTHONDOCS
    """
    def document(self, object, name=None, *args):
        """
        Generate documentation for an object.
        """
    def fail(self, object, name=None, *args):
        """
        Raise an exception for unimplemented types.
        """
    def getdocloc(self, object, basedir=sysconfig.get_path('stdlib')):
        """
        Return the location of module docs or None
        """
def HTMLRepr(Repr):
    """
    Class for safely making an HTML representation of a Python object.
    """
    def __init__(self):
        """
        '&'
        """
    def repr(self, object):
        """
        '__name__'
        """
    def repr_string(self, x, level):
        """
        '\\'
        """
    def repr_instance(self, x, level):
        """
        '<%s instance>'
        """
def HTMLDoc(Doc):
    """
    Formatter class for HTML documentation.
    """
    def page(self, title, contents):
        """
        Format an HTML page.
        """
    def heading(self, title, fgcol, bgcol, extras=''):
        """
        Format a page heading.
        """
2021-03-02 20:53:32,432 : INFO : tokenize_signature : --> do i ever get here?
    def section(self, title, fgcol, bgcol, contents, width=6,
                prelude='', marginalia=None, gap='&nbsp;'):
        """
        Format a section with a heading.
        """
    def bigsection(self, title, *args):
        """
        Format a section with a big heading.
        """
    def preformat(self, text):
        """
        Format literal preformatted text.
        """
    def multicolumn(self, list, format, cols=4):
        """
        Format a list of items into a multi-column list.
        """
    def grey(self, text): return '<font color="#909090">%s</font>' % text
        """
        '<font color="#909090">%s</font>'
        """
    def namelink(self, name, *dicts):
        """
        Make a link for an identifier, given name-to-URL mappings.
        """
    def classlink(self, object, modname):
        """
        Make a link for a class.
        """
    def modulelink(self, object):
        """
        Make a link for a module.
        """
    def modpkglink(self, modpkginfo):
        """
        Make a link for a module or package to display in an index.
        """
    def filelink(self, url, path):
        """
        Make a link to source file.
        """
    def markup(self, text, escape=None, funcs={}, classes={}, methods={}):
        """
        Mark up some plain text, given a context of symbols to look for.
                Each context dictionary maps object names to anchor names.
        """
    def formattree(self, tree, modname, parent=None):
        """
        Produce HTML for a class tree as given by inspect.getclasstree().
        """
    def docmodule(self, object, name=None, mod=None, *ignored):
        """
        Produce HTML documentation for a module object.
        """
2021-03-02 20:53:32,443 : INFO : tokenize_signature : --> do i ever get here?
    def docclass(self, object, name=None, mod=None, funcs={}, classes={},
                 *ignored):
        """
        Produce HTML documentation for a class object.
        """
        def HorizontalRule:
    """
    '<hr>\n'
    """
        def spill(msg, attrs, predicate):
            """
             Some descriptors may meet a failure in their __get__.
             (bug #1785)

            """
        def spilldescriptors(msg, attrs, predicate):
            """
            __doc__
            """
    def formatvalue(self, object):
        """
        Format an argument default value as text.
        """
2021-03-02 20:53:32,447 : INFO : tokenize_signature : --> do i ever get here?
    def docroutine(self, object, name=None, mod=None,
                   funcs={}, classes={}, methods={}, cl=None):
        """
        Produce HTML documentation for a function or method object.
        """
    def docdata(self, object, name=None, mod=None, cl=None):
        """
        Produce html documentation for a data descriptor.
        """
    def docother(self, object, name=None, mod=None, *ignored):
        """
        Produce HTML documentation for a data object.
        """
    def index(self, dir, shadowed=None):
        """
        Generate an HTML index for a directory of modules.
        """
def TextRepr(Repr):
    """
    Class for safely making a text representation of a Python object.
    """
    def __init__(self):
        """
        '__name__'
        """
    def repr_string(self, x, level):
        """
        '\\'
        """
    def repr_instance(self, x, level):
        """
        '<%s instance>'
        """
def TextDoc(Doc):
    """
    Formatter class for text documentation.
    """
    def bold(self, text):
        """
        Format a string in bold by overstriking.
        """
    def indent(self, text, prefix='    '):
        """
        Indent text by prepending a given prefix to each line.
        """
    def section(self, title, contents):
        """
        Format a section with a given heading.
        """
    def formattree(self, tree, modname, parent=None, prefix=''):
        """
        Render in text a class tree as returned by inspect.getclasstree().
        """
    def docmodule(self, object, name=None, mod=None):
        """
        Produce text documentation for a given module object.
        """
    def docclass(self, object, name=None, mod=None, *ignored):
        """
        Produce text documentation for a given class object.
        """
        def makename(c, m=object.__module__):
            """
            'class '
            """
        def HorizontalRule:
    """
    '-'
    """
        def spill(msg, attrs, predicate):
            """
             Some descriptors may meet a failure in their __get__.
             (bug #1785)

            """
        def spilldescriptors(msg, attrs, predicate):
            """
            '\n'
            """
    def formatvalue(self, object):
        """
        Format an argument default value as text.
        """
    def docroutine(self, object, name=None, mod=None, cl=None):
        """
        Produce text documentation for a function or method object.
        """
    def docdata(self, object, name=None, mod=None, cl=None):
        """
        Produce text documentation for a data descriptor.
        """
    def docother(self, object, name=None, mod=None, parent=None, maxlen=None, doc=None):
        """
        Produce text documentation for a data object.
        """
def _PlainTextDoc(TextDoc):
    """
    Subclass of TextDoc which overrides string styling
    """
    def bold(self, text):
        """
         --------------------------------------------------------- user interfaces


        """
def pager(text):
    """
    The first time this is called, determine what kind of pager to use.
    """
def getpager():
    """
    Decide what method to use for paging through text.
    """
def plain(text):
    """
    Remove boldface formatting from text.
    """
def pipepager(text, cmd):
    """
    Page through text by feeding it to another program.
    """
def tempfilepager(text, cmd):
    """
    Page through text by invoking a program on a temporary file.
    """
def _escape_stdout(text):
    """
     Escape non-encodable characters to avoid encoding errors later

    """
def ttypager(text):
    """
    Page through text on a text terminal.
    """
def plainpager(text):
    """
    Simply print unformatted text.  This is the ultimate fallback.
    """
def describe(thing):
    """
    Produce a short description of the given thing.
    """
def locate(path, forceload=0):
    """
    Locate an object by name or dotted path, importing as necessary.
    """
def resolve(thing, forceload=0):
    """
    Given an object or a path to an object, get the object and its name.
    """
2021-03-02 20:53:32,469 : INFO : tokenize_signature : --> do i ever get here?
def render_doc(thing, title='Python Library Documentation: %s', forceload=0,
        renderer=None):
    """
    Render text documentation, given an object or a path to an object.
    """
2021-03-02 20:53:32,469 : INFO : tokenize_signature : --> do i ever get here?
def doc(thing, title='Python Library Documentation: %s', forceload=0,
        output=None):
    """
    Display text documentation, given an object or a path to an object.
    """
def writedoc(thing, forceload=0):
    """
    Write HTML documentation to a file in the current directory.
    """
def writedocs(dir, pkgpath='', done=None):
    """
    Write out HTML documentation for all modules in a directory tree.
    """
def Helper:
    """
     These dictionaries map a topic name to either an alias, or a tuple
     (label, seealso-items).  The "label" is the label of the corresponding
     section in the .rst file under Doc/ and an index into the dictionary
     in pydoc_data/topics.py.

     CAUTION: if you change one of these dictionaries, be sure to adapt the
              list of needed labels in Doc/tools/extensions/pyspecific.py and
              regenerate the pydoc_data/topics.py file by running
                  make pydoc-topics
              in Doc/ and copying the output file into the Lib/ directory.


    """
    def __init__(self, input=None, output=None):
        """
        '?'
        """
    def __call__(self, request=_GoInteractive):
        """
        '''
        You are now leaving help and returning to the Python interpreter.
        If you want to ask for help on a particular object directly from the
        interpreter, you can type "help(object)".  Executing "help('string')"
        has the same effect as typing a particular string at the help> prompt.
        '''
        """
    def interact(self):
        """
        '\n'
        """
    def getline(self, prompt):
        """
        Read one line, using input() when appropriate.
        """
    def help(self, request):
        """
        ''
        """
    def intro(self):
        """
        '''
        Welcome to Python {0}'s help utility!

        If this is your first time using Python, you should definitely check out
        the tutorial on the Internet at https://docs.python.org/{0}/tutorial/.

        Enter the name of any module, keyword, or topic to get help on writing
        Python programs and using Python modules.  To quit this help utility and
        return to the interpreter, just type "quit".

        To get a list of available modules, keywords, symbols, or topics, type
        "modules", "keywords", "symbols", or "topics".  Each module also comes
        with a one-line summary of what it does; to list the modules whose name
        or summary contain a given string such as "spam", type "modules spam".
        '''
        """
    def list(self, items, columns=4, width=80):
        """
        ' '
        """
    def listkeywords(self):
        """
        '''
        Here is a list of the Python keywords.  Enter any keyword to get more help.

        '''
        """
    def listsymbols(self):
        """
        '''
        Here is a list of the punctuation symbols which Python assigns special meaning
        to. Enter any symbol to get more help.

        '''
        """
    def listtopics(self):
        """
        '''
        Here is a list of available topics.  Enter any topic name to get more help.

        '''
        """
    def showtopic(self, topic, more_xrefs=''):
        """
        '''
        Sorry, topic and keyword documentation is not available because the
        module "pydoc_data.topics" could not be found.
        '''
        """
    def _gettopic(self, topic, more_xrefs=''):
        """
        Return unbuffered tuple of (topic, xrefs).

                If an error occurs here, the exception is caught and displayed by
                the url handler.

                This function duplicates the showtopic method but returns its
                result directly so it can be formatted for display in an html page.
        
        """
    def showsymbol(self, symbol):
        """
        ' '
        """
    def listmodules(self, key=''):
        """
        '''
        Here is a list of modules whose name or summary contains '{}'.
        If there are any, enter a module name to get more help.

        '''
        """
            def callback(path, modname, desc, modules=modules):
                """
                '.__init__'
                """
            def onerror(modname):
                """
                '''
                Enter any module name to get more help.  Or, type "modules spam" to search
                for modules whose name or summary contain the string "spam".
                '''
                """
def ModuleScanner:
    """
    An interruptible scanner that searches module synopses.
    """
    def run(self, callback, key=None, completer=None, onerror=None):
        """
        '__main__'
        """
def apropos(key):
    """
    Print all the one-line module summaries that contain a substring.
    """
    def callback(path, modname, desc):
        """
        '.__init__'
        """
    def onerror(modname):
        """
        'ignore'
        """
def _start_server(urlhandler, hostname, port):
    """
    Start an HTTP server thread on a specific port.

        Start an HTML/text server thread, so HTML or text documents can be
        browsed dynamically and interactively with a Web browser.  Example use:

            >>> import time
            >>> import pydoc

            Define a URL handler.  To determine what the client is asking
            for, check the URL and content_type.

            Then get or generate some text or HTML code and return it.

            >>> def my_url_handler(url, content_type):
            ...     text = 'the URL sent was: (%s, %s)' % (url, content_type)
            ...     return text

            Start server thread on port 0.
            If you use port 0, the server will pick a random port number.
            You can then use serverthread.port to get the port number.

            >>> port = 0
            >>> serverthread = pydoc._start_server(my_url_handler, port)

            Check that the server is really started.  If it is, open browser
            and get first page.  Use serverthread.url as the starting page.

            >>> if serverthread.serving:
            ...    import webbrowser

            The next two lines are commented out so a browser doesn't open if
            doctest is run on this module.

            #...    webbrowser.open(serverthread.url)
            #True

            Let the server do its thing. We just need to monitor its status.
            Use time.sleep so the loop doesn't hog the CPU.

            >>> starttime = time.monotonic()
            >>> timeout = 1                    #seconds

            This is a short timeout for testing purposes.

            >>> while serverthread.serving:
            ...     time.sleep(.01)
            ...     if serverthread.serving and time.monotonic() - starttime > timeout:
            ...          serverthread.stop()
            ...          break

            Print any errors that may have occurred.

            >>> print(serverthread.error)
            None
   
    """
    def DocHandler(http.server.BaseHTTPRequestHandler):
    """
    Process a request from an HTML browser.

                The URL received is in self.path.
                Get an HTML page from self.urlhandler and send it.
            
    """
        def log_message(self, *args):
            """
             Don't log messages.

            """
    def DocServer(http.server.HTTPServer):
    """
    Start the server.
    """
        def ready(self, server):
            """
            'http://%s:%d/'
            """
        def stop(self):
            """
            Stop the server and this thread nicely
            """
def _url_handler(url, content_type="text/html"):
    """
    The pydoc url handler for use with the pydoc server.

        If the content_type is 'text/css', the _pydoc.css style
        sheet is read and returned if it exits.

        If the content_type is 'text/html', then the result of
        get_html_page(url) is returned.
    
    """
    def _HTMLDoc(HTMLDoc):
    """
    Format an HTML page.
    """
        def filelink(self, url, path):
            """
            '<a href="getfile?key=%s">%s</a>'
            """
    def html_navbar():
        """
        %s [%s, %s]
        """
    def html_index():
        """
        Module Index page.
        """
        def bltinlink(name):
            """
            '<a href="%s.html">%s</a>'
            """
    def html_search(key):
        """
        Search results page.
        """
        def callback(path, modname, desc):
            """
            '.__init__'
            """
            def onerror(modname):
                """
                 format page

                """
        def bltinlink(name):
            """
            '<a href="%s.html">%s</a>'
            """
    def html_getfile(path):
        """
        Get and display a source file listing safely.
        """
    def html_topics():
        """
        Index of topic texts available.
        """
        def bltinlink(name):
            """
            '<a href="topic?key=%s">%s</a>'
            """
    def html_keywords():
        """
        Index of keywords.
        """
        def bltinlink(name):
            """
            '<a href="topic?key=%s">%s</a>'
            """
    def html_topicpage(topic):
        """
        Topic or keyword help page.
        """
            def bltinlink(name):
                """
                '<a href="topic?key=%s">%s</a>'
                """
    def html_getobj(url):
        """
        'None'
        """
    def html_error(url, exc):
        """
        '<big><big><strong>Error</strong></big></big>'
        """
    def get_html_page(url):
        """
        Generate an HTML page for url.
        """
def browse(port=0, *, open_browser=True, hostname='localhost'):
    """
    Start the enhanced pydoc Web server and open a Web browser.

        Use port '0' to start the server on an arbitrary port.
        Set open_browser to False to suppress opening a browser.
    
    """
def ispath(x):
    """
    Ensures current directory is on returned path, and argv0 directory is not

        Exception: argv0 dir is left alone if it's also pydoc's directory.

        Returns a new path entry list, or None if no adjustment is needed.
    
    """
def _adjust_cli_sys_path():
    """
    Ensures current directory is on sys.path, and __main__ directory is not.

        Exception: __main__ dir is left alone if it's also pydoc's directory.
    
    """
def cli():
    """
    Command-line interface (looks at sys.argv to decide what to do).
    """
    def BadUsage(Exception): pass
    """
    'bk:n:p:w'
    """
