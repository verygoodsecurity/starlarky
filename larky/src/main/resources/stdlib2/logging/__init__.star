def getLevelName(level):
    """

        Return the textual representation of logging level 'level'.

        If the level is one of the predefined levels (CRITICAL, ERROR, WARNING,
        INFO, DEBUG) then you get the corresponding string. If you have
        associated levels with names using addLevelName then the name you have
        associated with 'level' is returned.

        If a numeric value corresponding to one of the defined levels is passed
        in, the corresponding string representation is returned.

        Otherwise, the string "Level %s" % level is returned.
    
    """
def addLevelName(level, levelName):
    """

        Associate 'levelName' with 'level'.

        This is used when converting levels to text during message formatting.
    
    """
    def currentframe():
        """
        Return the frame object for the caller's stack frame.
        """
def _checkLevel(level):
    """
    Unknown level: %r
    """
def _acquireLock():
    """

        Acquire the module-level lock for serializing access to shared data.

        This should be released with _releaseLock().
    
    """
def _releaseLock():
    """

        Release the module-level lock acquired by calling _acquireLock().
    
    """
    def _register_at_fork_reinit_lock(instance):
        """
         no-op when os.register_at_fork does not exist.
        """
    def _register_at_fork_reinit_lock(instance):
        """
         _acquireLock() was called in the parent before forking.

        """
def LogRecord(object):
    """

        A LogRecord instance represents an event being logged.

        LogRecord instances are created every time something is logged. They
        contain all the information pertinent to the event being logged. The
        main information passed in is in msg and args, which are combined
        using str(msg) % args to create the message field of the record. The
        record also includes information such as when the record was created,
        the source line where the logging call was made, and any exception
        information to be logged.
    
    """
2021-03-02 20:54:36,021 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, name, level, pathname, lineno,
                 msg, args, exc_info, func=None, sinfo=None, **kwargs):
        """

                Initialize a logging record with interesting information.
        
        """
    def __repr__(self):
        """
        '<LogRecord: %s, %s, %s, %s, "%s">'
        """
    def getMessage(self):
        """

                Return the message for this LogRecord.

                Return the message for this LogRecord after merging any user-supplied
                arguments with the message.
        
        """
def setLogRecordFactory(factory):
    """

        Set the factory to be used when instantiating a log record.

        :param factory: A callable which will be called to instantiate
        a log record.
    
    """
def getLogRecordFactory():
    """

        Return the factory to be used when instantiating a log record.
    
    """
def makeLogRecord(dict):
    """

        Make a LogRecord whose attributes are defined by the specified dictionary,
        This function is useful for converting a logging event received over
        a socket connection (which is sent as a dictionary) into a LogRecord
        instance.
    
    """
def PercentStyle(object):
    """
    '%(message)s'
    """
    def __init__(self, fmt):
        """
        Validate the input format, ensure it matches the correct style
        """
    def _format(self, record):
        """
        'Formatting field not found in record: %s'
        """
def StrFormatStyle(PercentStyle):
    """
    '{message}'
    """
    def _format(self, record):
        """
        Validate the input format, ensure it is the correct string formatting style
        """
def StringTemplateStyle(PercentStyle):
    """
    '${message}'
    """
    def __init__(self, fmt):
        """
        '$asctime'
        """
    def validate(self):
        """
        'named'
        """
    def _format(self, record):
        """
        %(levelname)s:%(name)s:%(message)s
        """
def Formatter(object):
    """

        Formatter instances are used to convert a LogRecord to text.

        Formatters need to know how a LogRecord is constructed. They are
        responsible for converting a LogRecord to (usually) a string which can
        be interpreted by either a human or an external system. The base Formatter
        allows a formatting string to be specified. If none is supplied, the
        the style-dependent default value, "%(message)s", "{message}", or
        "${message}", is used.

        The Formatter can be initialized with a format string which makes use of
        knowledge of the LogRecord attributes - e.g. the default value mentioned
        above makes use of the fact that the user's message and arguments are pre-
        formatted into a LogRecord's message attribute. Currently, the useful
        attributes in a LogRecord are described by:

        %(name)s            Name of the logger (logging channel)
        %(levelno)s         Numeric logging level for the message (DEBUG, INFO,
                            WARNING, ERROR, CRITICAL)
        %(levelname)s       Text logging level for the message ("DEBUG", "INFO",
                            "WARNING", "ERROR", "CRITICAL")
        %(pathname)s        Full pathname of the source file where the logging
                            call was issued (if available)
        %(filename)s        Filename portion of pathname
        %(module)s          Module (name portion of filename)
        %(lineno)d          Source line number where the logging call was issued
                            (if available)
        %(funcName)s        Function name
        %(created)f         Time when the LogRecord was created (time.time()
                            return value)
        %(asctime)s         Textual time when the LogRecord was created
        %(msecs)d           Millisecond portion of the creation time
        %(relativeCreated)d Time in milliseconds when the LogRecord was created,
                            relative to the time the logging module was loaded
                            (typically at application startup time)
        %(thread)d          Thread ID (if available)
        %(threadName)s      Thread name (if available)
        %(process)d         Process ID (if available)
        %(message)s         The result of record.getMessage(), computed just as
                            the record is emitted
    
    """
    def __init__(self, fmt=None, datefmt=None, style='%', validate=True):
        """

                Initialize the formatter with specified format strings.

                Initialize the formatter either with the specified format string, or a
                default as described above. Allow for specialized date formatting with
                the optional datefmt argument. If datefmt is omitted, you get an
                ISO8601-like (or RFC 3339-like) format.

                Use a style parameter of '%', '{' or '$' to specify that you want to
                use one of %-formatting, :meth:`str.format` (``{}``) formatting or
                :class:`string.Template` formatting in your format string.

                .. versionchanged:: 3.2
                   Added the ``style`` parameter.
        
        """
    def formatTime(self, record, datefmt=None):
        """

                Return the creation time of the specified LogRecord as formatted text.

                This method should be called from format() by a formatter which
                wants to make use of a formatted time. This method can be overridden
                in formatters to provide for any specific requirement, but the
                basic behaviour is as follows: if datefmt (a string) is specified,
                it is used with time.strftime() to format the creation time of the
                record. Otherwise, an ISO8601-like (or RFC 3339-like) format is used.
                The resulting string is returned. This function uses a user-configurable
                function to convert the creation time to a tuple. By default,
                time.localtime() is used; to change this for a particular formatter
                instance, set the 'converter' attribute to a function with the same
                signature as time.localtime() or time.gmtime(). To change it for all
                formatters, for example if you want all logging times to be shown in GMT,
                set the 'converter' attribute in the Formatter class.
        
        """
    def formatException(self, ei):
        """

                Format and return the specified exception information as a string.

                This default implementation just uses
                traceback.print_exception()
        
        """
    def usesTime(self):
        """

                Check if the format uses the creation time of the record.
        
        """
    def formatMessage(self, record):
        """

                This method is provided as an extension point for specialized
                formatting of stack information.

                The input data is a string as returned from a call to
                :func:`traceback.print_stack`, but with the last trailing newline
                removed.

                The base implementation just returns the value passed in.
        
        """
    def format(self, record):
        """

                Format the specified record as text.

                The record's attribute dictionary is used as the operand to a
                string formatting operation which yields the returned string.
                Before formatting the dictionary, a couple of preparatory steps
                are carried out. The message attribute of the record is computed
                using LogRecord.getMessage(). If the formatting string uses the
                time (as determined by a call to usesTime(), formatTime() is
                called to format the event time. If there is exception information,
                it is formatted using formatException() and appended to the message.
        
        """
def BufferingFormatter(object):
    """

        A formatter suitable for formatting a number of records.
    
    """
    def __init__(self, linefmt=None):
        """

                Optionally specify a formatter which will be used to format each
                individual record.
        
        """
    def formatHeader(self, records):
        """

                Return the header string for the specified records.
        
        """
    def formatFooter(self, records):
        """

                Return the footer string for the specified records.
        
        """
    def format(self, records):
        """

                Format the specified records and return the result as a string.
        
        """
def Filter(object):
    """

        Filter instances are used to perform arbitrary filtering of LogRecords.

        Loggers and Handlers can optionally use Filter instances to filter
        records as desired. The base filter class only allows events which are
        below a certain point in the logger hierarchy. For example, a filter
        initialized with "A.B" will allow events logged by loggers "A.B",
        "A.B.C", "A.B.C.D", "A.B.D" etc. but not "A.BB", "B.A.B" etc. If
        initialized with the empty string, all events are passed.
    
    """
    def __init__(self, name=''):
        """

                Initialize a filter.

                Initialize with the name of the logger which, together with its
                children, will have its events allowed through the filter. If no
                name is specified, allow every event.
        
        """
    def filter(self, record):
        """

                Determine if the specified record is to be logged.

                Is the specified record to be logged? Returns 0 for no, nonzero for
                yes. If deemed appropriate, the record may be modified in-place.
        
        """
def Filterer(object):
    """

        A base class for loggers and handlers which allows them to share
        common code.
    
    """
    def __init__(self):
        """

                Initialize the list of filters to be an empty list.
        
        """
    def addFilter(self, filter):
        """

                Add the specified filter to this handler.
        
        """
    def removeFilter(self, filter):
        """

                Remove the specified filter from this handler.
        
        """
    def filter(self, record):
        """

                Determine if a record is loggable by consulting all the filters.

                The default is to allow the record to be logged; any filter can veto
                this and the record is then dropped. Returns a zero value if a record
                is to be dropped, else non-zero.

                .. versionchanged:: 3.2

                   Allow filters to be just callables.
        
        """
def _removeHandlerRef(wr):
    """

        Remove a handler reference from the internal cleanup list.
    
    """
def _addHandlerRef(handler):
    """

        Add a handler to the internal cleanup list using a weak reference.
    
    """
def Handler(Filterer):
    """

        Handler instances dispatch logging events to specific destinations.

        The base handler class. Acts as a placeholder which defines the Handler
        interface. Handlers can optionally use Formatter instances to format
        records as desired. By default, no formatter is specified; in this case,
        the 'raw' message as determined by record.message is logged.
    
    """
    def __init__(self, level=NOTSET):
        """

                Initializes the instance - basically setting the formatter to None
                and the filter list to empty.
        
        """
    def get_name(self):
        """

                Acquire a thread lock for serializing access to the underlying I/O.
        
        """
    def acquire(self):
        """

                Acquire the I/O thread lock.
        
        """
    def release(self):
        """

                Release the I/O thread lock.
        
        """
    def setLevel(self, level):
        """

                Set the logging level of this handler.  level must be an int or a str.
        
        """
    def format(self, record):
        """

                Format the specified record.

                If a formatter is set, use it. Otherwise, use the default formatter
                for the module.
        
        """
    def emit(self, record):
        """

                Do whatever it takes to actually log the specified logging record.

                This version is intended to be implemented by subclasses and so
                raises a NotImplementedError.
        
        """
    def handle(self, record):
        """

                Conditionally emit the specified logging record.

                Emission depends on filters which may have been added to the handler.
                Wrap the actual emission of the record with acquisition/release of
                the I/O thread lock. Returns whether the filter passed the record for
                emission.
        
        """
    def setFormatter(self, fmt):
        """

                Set the formatter for this handler.
        
        """
    def flush(self):
        """

                Ensure all logging output has been flushed.

                This version does nothing and is intended to be implemented by
                subclasses.
        
        """
    def close(self):
        """

                Tidy up any resources used by the handler.

                This version removes the handler from an internal map of handlers,
                _handlers, which is used for handler lookup by name. Subclasses
                should ensure that this gets called from overridden close()
                methods.
        
        """
    def handleError(self, record):
        """

                Handle errors which occur during an emit() call.

                This method should be called from handlers when an exception is
                encountered during an emit() call. If raiseExceptions is false,
                exceptions get silently ignored. This is what is mostly wanted
                for a logging system - most users will not care about errors in
                the logging system, they are more interested in application errors.
                You could, however, replace this with a custom handler if you wish.
                The record which was being processed is passed in to this method.
        
        """
    def __repr__(self):
        """
        '<%s (%s)>'
        """
def StreamHandler(Handler):
    """

        A handler class which writes logging records, appropriately formatted,
        to a stream. Note that this class does not close the stream, as
        sys.stdout or sys.stderr may be used.
    
    """
    def __init__(self, stream=None):
        """

                Initialize the handler.

                If stream is not specified, sys.stderr is used.
        
        """
    def flush(self):
        """

                Flushes the stream.
        
        """
    def emit(self, record):
        """

                Emit a record.

                If a formatter is specified, it is used to format the record.
                The record is then written to the stream with a trailing newline.  If
                exception information is present, it is formatted using
                traceback.print_exception and appended to the stream.  If the stream
                has an 'encoding' attribute, it is used to determine how to do the
                output to the stream.
        
        """
    def setStream(self, stream):
        """

                Sets the StreamHandler's stream to the specified value,
                if it is different.

                Returns the old stream, if the stream was changed, or None
                if it wasn't.
        
        """
    def __repr__(self):
        """
        'name'
        """
def FileHandler(StreamHandler):
    """

        A handler class which writes formatted logging records to disk files.
    
    """
    def __init__(self, filename, mode='a', encoding=None, delay=False):
        """

                Open the specified file and use it as the stream for logging.
        
        """
    def close(self):
        """

                Closes the stream.
        
        """
    def _open(self):
        """

                Open the current base file with the (original) mode and encoding.
                Return the resulting stream.
        
        """
    def emit(self, record):
        """

                Emit a record.

                If the stream was not opened because 'delay' was specified in the
                constructor, open it before calling the superclass's emit.
        
        """
    def __repr__(self):
        """
        '<%s %s (%s)>'
        """
def _StderrHandler(StreamHandler):
    """

        This class is like a StreamHandler using sys.stderr, but always uses
        whatever sys.stderr is currently set to rather than the value of
        sys.stderr at handler construction time.
    
    """
    def __init__(self, level=NOTSET):
        """

                Initialize the handler.
        
        """
    def stream(self):
        """
        ---------------------------------------------------------------------------
           Manager classes and functions
        ---------------------------------------------------------------------------


        """
    def __init__(self, alogger):
        """

                Initialize with the specified logger being a child of this placeholder.
        
        """
    def append(self, alogger):
        """

                Add the specified logger as a child of this placeholder.
        
        """
def setLoggerClass(klass):
    """

        Set the class to be used when instantiating a logger. The class should
        define __init__() such that only a name argument is required, and the
        __init__() should call Logger.__init__()
    
    """
def getLoggerClass():
    """

        Return the class to be used when instantiating a logger.
    
    """
def Manager(object):
    """

        There is [under normal circumstances] just one Manager instance, which
        holds the hierarchy of loggers.
    
    """
    def __init__(self, rootnode):
        """

                Initialize the manager with the root node of the logger hierarchy.
        
        """
    def getLogger(self, name):
        """

                Get a logger with the specified name (channel name), creating it
                if it doesn't yet exist. This name is a dot-separated hierarchical
                name, such as "a", "a.b", "a.b.c" or similar.

                If a PlaceHolder existed for the specified name [i.e. the logger
                didn't exist but a child of it did], replace it with the created
                logger and fix up the parent/child references which pointed to the
                placeholder to now point to the logger.
        
        """
    def setLoggerClass(self, klass):
        """

                Set the class to be used when instantiating a logger with this Manager.
        
        """
    def setLogRecordFactory(self, factory):
        """

                Set the factory to be used when instantiating a log record with this
                Manager.
        
        """
    def _fixupParents(self, alogger):
        """

                Ensure that there are either loggers or placeholders all the way
                from the specified logger to the root of the logger hierarchy.
        
        """
    def _fixupChildren(self, ph, alogger):
        """

                Ensure that children of the placeholder ph are connected to the
                specified logger.
        
        """
    def _clear_cache(self):
        """

                Clear the cache for all loggers in loggerDict
                Called when level changes are made
        
        """
def Logger(Filterer):
    """

        Instances of the Logger class represent a single logging channel. A
        "logging channel" indicates an area of an application. Exactly how an
        "area" is defined is up to the application developer. Since an
        application can have any number of areas, logging channels are identified
        by a unique string. Application areas can be nested (e.g. an area
        of "input processing" might include sub-areas "read CSV files", "read
        XLS files" and "read Gnumeric files"). To cater for this natural nesting,
        channel names are organized into a namespace hierarchy where levels are
        separated by periods, much like the Java or Python package namespace. So
        in the instance given above, channel names might be "input" for the upper
        level, and "input.csv", "input.xls" and "input.gnu" for the sub-levels.
        There is no arbitrary limit to the depth of nesting.
    
    """
    def __init__(self, name, level=NOTSET):
        """

                Initialize the logger with a name and an optional level.
        
        """
    def setLevel(self, level):
        """

                Set the logging level of this logger.  level must be an int or a str.
        
        """
    def debug(self, msg, *args, **kwargs):
        """

                Log 'msg % args' with severity 'DEBUG'.

                To pass exception information, use the keyword argument exc_info with
                a true value, e.g.

                logger.debug("Houston, we have a %s", "thorny problem", exc_info=1)
        
        """
    def info(self, msg, *args, **kwargs):
        """

                Log 'msg % args' with severity 'INFO'.

                To pass exception information, use the keyword argument exc_info with
                a true value, e.g.

                logger.info("Houston, we have a %s", "interesting problem", exc_info=1)
        
        """
    def warning(self, msg, *args, **kwargs):
        """

                Log 'msg % args' with severity 'WARNING'.

                To pass exception information, use the keyword argument exc_info with
                a true value, e.g.

                logger.warning("Houston, we have a %s", "bit of a problem", exc_info=1)
        
        """
    def warn(self, msg, *args, **kwargs):
        """
        The 'warn' method is deprecated, 
        use 'warning' instead
        """
    def error(self, msg, *args, **kwargs):
        """

                Log 'msg % args' with severity 'ERROR'.

                To pass exception information, use the keyword argument exc_info with
                a true value, e.g.

                logger.error("Houston, we have a %s", "major problem", exc_info=1)
        
        """
    def exception(self, msg, *args, exc_info=True, **kwargs):
        """

                Convenience method for logging an ERROR with exception information.
        
        """
    def critical(self, msg, *args, **kwargs):
        """

                Log 'msg % args' with severity 'CRITICAL'.

                To pass exception information, use the keyword argument exc_info with
                a true value, e.g.

                logger.critical("Houston, we have a %s", "major disaster", exc_info=1)
        
        """
    def log(self, level, msg, *args, **kwargs):
        """

                Log 'msg % args' with the integer severity 'level'.

                To pass exception information, use the keyword argument exc_info with
                a true value, e.g.

                logger.log(level, "We have a %s", "mysterious problem", exc_info=1)
        
        """
    def findCaller(self, stack_info=False, stacklevel=1):
        """

                Find the stack frame of the caller so that we can note the source
                file name, line number and function name.
        
        """
2021-03-02 20:54:36,044 : INFO : tokenize_signature : --> do i ever get here?
    def makeRecord(self, name, level, fn, lno, msg, args, exc_info,
                   func=None, extra=None, sinfo=None):
        """

                A factory method which can be overridden in subclasses to create
                specialized LogRecords.
        
        """
2021-03-02 20:54:36,045 : INFO : tokenize_signature : --> do i ever get here?
    def _log(self, level, msg, args, exc_info=None, extra=None, stack_info=False,
             stacklevel=1):
        """

                Low-level logging routine which creates a LogRecord and then calls
                all the handlers of this logger to handle the record.
        
        """
    def handle(self, record):
        """

                Call the handlers for the specified record.

                This method is used for unpickled records received from a socket, as
                well as those created locally. Logger-level filtering is applied.
        
        """
    def addHandler(self, hdlr):
        """

                Add the specified handler to this logger.
        
        """
    def removeHandler(self, hdlr):
        """

                Remove the specified handler from this logger.
        
        """
    def hasHandlers(self):
        """

                See if this logger has any handlers configured.

                Loop through all handlers for this logger and its parents in the
                logger hierarchy. Return True if a handler was found, else False.
                Stop searching up the hierarchy whenever a logger with the "propagate"
                attribute set to zero is found - that will be the last logger which
                is checked for the existence of handlers.
        
        """
    def callHandlers(self, record):
        """

                Pass a record to all relevant handlers.

                Loop through all handlers for this logger and its parents in the
                logger hierarchy. If no handler was found, output a one-off error
                message to sys.stderr. Stop searching up the hierarchy whenever a
                logger with the "propagate" attribute set to zero is found - that
                will be the last logger whose handlers are called.
        
        """
    def getEffectiveLevel(self):
        """

                Get the effective level for this logger.

                Loop through this logger and its parents in the logger hierarchy,
                looking for a non-zero logging level. Return the first one found.
        
        """
    def isEnabledFor(self, level):
        """

                Is this logger enabled for level 'level'?
        
        """
    def getChild(self, suffix):
        """

                Get a logger which is a descendant to this one.

                This is a convenience method, such that

                logging.getLogger('abc').getChild('def.ghi')

                is the same as

                logging.getLogger('abc.def.ghi')

                It's useful, for example, when the parent logger is named using
                __name__ rather than a literal string.
        
        """
    def __repr__(self):
        """
        '<%s %s (%s)>'
        """
    def __reduce__(self):
        """
         In general, only the root logger will not be accessible via its name.
         However, the root logger's class has its own __reduce__ method.

        """
def RootLogger(Logger):
    """

        A root logger is not that different to any other logger, except that
        it must have a logging level and there is only one instance of it in
        the hierarchy.
    
    """
    def __init__(self, level):
        """

                Initialize the logger with the name "root".
        
        """
    def __reduce__(self):
        """

            An adapter for loggers which makes it easier to specify contextual
            information in logging output.
    
        """
    def __init__(self, logger, extra):
        """

                Initialize the adapter with a logger and a dict-like object which
                provides contextual information. This constructor signature allows
                easy stacking of LoggerAdapters, if so desired.

                You can effectively pass keyword arguments as shown in the
                following example:

                adapter = LoggerAdapter(someLogger, dict(p1=v1, p2="v2"))
        
        """
    def process(self, msg, kwargs):
        """

                Process the logging message and keyword arguments passed in to
                a logging call to insert contextual information. You can either
                manipulate the message itself, the keyword args or both. Return
                the message and kwargs modified (or not) to suit your needs.

                Normally, you'll only need to override this one method in a
                LoggerAdapter subclass for your specific needs.
        
        """
    def debug(self, msg, *args, **kwargs):
        """

                Delegate a debug call to the underlying logger.
        
        """
    def info(self, msg, *args, **kwargs):
        """

                Delegate an info call to the underlying logger.
        
        """
    def warning(self, msg, *args, **kwargs):
        """

                Delegate a warning call to the underlying logger.
        
        """
    def warn(self, msg, *args, **kwargs):
        """
        The 'warn' method is deprecated, 
        use 'warning' instead
        """
    def error(self, msg, *args, **kwargs):
        """

                Delegate an error call to the underlying logger.
        
        """
    def exception(self, msg, *args, exc_info=True, **kwargs):
        """

                Delegate an exception call to the underlying logger.
        
        """
    def critical(self, msg, *args, **kwargs):
        """

                Delegate a critical call to the underlying logger.
        
        """
    def log(self, level, msg, *args, **kwargs):
        """

                Delegate a log call to the underlying logger, after adding
                contextual information from this adapter instance.
        
        """
    def isEnabledFor(self, level):
        """

                Is this logger enabled for level 'level'?
        
        """
    def setLevel(self, level):
        """

                Set the specified level on the underlying logger.
        
        """
    def getEffectiveLevel(self):
        """

                Get the effective level for the underlying logger.
        
        """
    def hasHandlers(self):
        """

                See if the underlying logger has any handlers.
        
        """
    def _log(self, level, msg, args, exc_info=None, extra=None, stack_info=False):
        """

                Low-level log implementation, proxied to allow nested logger adapters.
        
        """
    def manager(self):
        """
        '<%s %s (%s)>'
        """
def basicConfig(**kwargs):
    """

        Do basic configuration for the logging system.

        This function does nothing if the root logger already has handlers
        configured, unless the keyword argument *force* is set to ``True``.
        It is a convenience method intended for use by simple scripts
        to do one-shot configuration of the logging package.

        The default behaviour is to create a StreamHandler which writes to
        sys.stderr, set a formatter using the BASIC_FORMAT format string, and
        add the handler to the root logger.

        A number of optional keyword arguments may be specified, which can alter
        the default behaviour.

        filename  Specifies that a FileHandler be created, using the specified
                  filename, rather than a StreamHandler.
        filemode  Specifies the mode to open the file, if filename is specified
                  (if filemode is unspecified, it defaults to 'a').
        format    Use the specified format string for the handler.
        datefmt   Use the specified date/time format.
        style     If a format string is specified, use this to specify the
                  type of format string (possible values '%', '{', '$', for
                  %-formatting, :meth:`str.format` and :class:`string.Template`
                  - defaults to '%').
        level     Set the root logger level to the specified level.
        stream    Use the specified stream to initialize the StreamHandler. Note
                  that this argument is incompatible with 'filename' - if both
                  are present, 'stream' is ignored.
        handlers  If specified, this should be an iterable of already created
                  handlers, which will be added to the root handler. Any handler
                  in the list which does not have a formatter assigned will be
                  assigned the formatter created in this function.
        force     If this keyword  is specified as true, any existing handlers
                  attached to the root logger are removed and closed, before
                  carrying out the configuration as specified by the other
                  arguments.
        Note that you could specify a stream created using open(filename, mode)
        rather than passing the filename and mode in. However, it should be
        remembered that StreamHandler does not close its stream (since it may be
        using sys.stdout or sys.stderr), whereas FileHandler closes its stream
        when the handler is closed.

        .. versionchanged:: 3.8
           Added the ``force`` parameter.

        .. versionchanged:: 3.2
           Added the ``style`` parameter.

        .. versionchanged:: 3.3
           Added the ``handlers`` parameter. A ``ValueError`` is now thrown for
           incompatible arguments (e.g. ``handlers`` specified together with
           ``filename``/``filemode``, or ``filename``/``filemode`` specified
           together with ``stream``, or ``handlers`` specified together with
           ``stream``.
    
    """
def getLogger(name=None):
    """

        Return a logger with the specified name, creating it if necessary.

        If no name is specified, return the root logger.
    
    """
def critical(msg, *args, **kwargs):
    """

        Log a message with severity 'CRITICAL' on the root logger. If the logger
        has no handlers, call basicConfig() to add a console handler with a
        pre-defined format.
    
    """
def error(msg, *args, **kwargs):
    """

        Log a message with severity 'ERROR' on the root logger. If the logger has
        no handlers, call basicConfig() to add a console handler with a pre-defined
        format.
    
    """
def exception(msg, *args, exc_info=True, **kwargs):
    """

        Log a message with severity 'ERROR' on the root logger, with exception
        information. If the logger has no handlers, basicConfig() is called to add
        a console handler with a pre-defined format.
    
    """
def warning(msg, *args, **kwargs):
    """

        Log a message with severity 'WARNING' on the root logger. If the logger has
        no handlers, call basicConfig() to add a console handler with a pre-defined
        format.
    
    """
def warn(msg, *args, **kwargs):
    """
    The 'warn' function is deprecated, 
    use 'warning' instead
    """
def info(msg, *args, **kwargs):
    """

        Log a message with severity 'INFO' on the root logger. If the logger has
        no handlers, call basicConfig() to add a console handler with a pre-defined
        format.
    
    """
def debug(msg, *args, **kwargs):
    """

        Log a message with severity 'DEBUG' on the root logger. If the logger has
        no handlers, call basicConfig() to add a console handler with a pre-defined
        format.
    
    """
def log(level, msg, *args, **kwargs):
    """

        Log 'msg % args' with the integer severity 'level' on the root logger. If
        the logger has no handlers, call basicConfig() to add a console handler
        with a pre-defined format.
    
    """
def disable(level=CRITICAL):
    """

        Disable all logging calls of severity 'level' and below.
    
    """
def shutdown(handlerList=_handlerList):
    """

        Perform any cleanup actions in the logging system (e.g. flushing
        buffers).

        Should be called at application exit.
    
    """
def NullHandler(Handler):
    """

        This handler does nothing. It's intended to be used to avoid the
        "No handlers could be found for logger XXX" one-off warning. This is
        important for library code, which may contain code to log events. If a user
        of the library does not configure logging, the one-off warning might be
        produced; to avoid this, the library developer simply needs to instantiate
        a NullHandler and add it to the top-level logger of the library module or
        package.
    
    """
    def handle(self, record):
        """
        Stub.
        """
    def emit(self, record):
        """
        Stub.
        """
    def createLock(self):
        """
         Warnings integration


        """
def _showwarning(message, category, filename, lineno, file=None, line=None):
    """

        Implementation of showwarnings which redirects to logging, which will first
        check to see if the file parameter is None. If a file is specified, it will
        delegate to the original warnings implementation of showwarning. Otherwise,
        it will call warnings.formatwarning and will log the resulting string to a
        warnings logger named "py.warnings" with level logging.WARNING.
    
    """
def captureWarnings(capture):
    """

        If capture is true, redirect all warnings to the logging package.
        If capture is False, ensure that warnings are not redirected to logging
        but to their original destinations.
    
    """
