def BaseRotatingHandler(logging.FileHandler):
    """

        Base class for handlers that rotate log files at a certain point.
        Not meant to be instantiated directly.  Instead, use RotatingFileHandler
        or TimedRotatingFileHandler.
    
    """
    def __init__(self, filename, mode, encoding=None, delay=False):
        """

                Use the specified filename for streamed logging
        
        """
    def emit(self, record):
        """

                Emit a record.

                Output the record to the file, catering for rollover as described
                in doRollover().
        
        """
    def rotation_filename(self, default_name):
        """

                Modify the filename of a log file when rotating.

                This is provided so that a custom filename can be provided.

                The default implementation calls the 'namer' attribute of the
                handler, if it's callable, passing the default name to
                it. If the attribute isn't callable (the default is None), the name
                is returned unchanged.

                :param default_name: The default name for the log file.
        
        """
    def rotate(self, source, dest):
        """

                When rotating, rotate the current log.

                The default implementation calls the 'rotator' attribute of the
                handler, if it's callable, passing the source and dest arguments to
                it. If the attribute isn't callable (the default is None), the source
                is simply renamed to the destination.

                :param source: The source filename. This is normally the base
                               filename, e.g. 'test.log'
                :param dest:   The destination filename. This is normally
                               what the source is rotated to, e.g. 'test.log.1'.
        
        """
def RotatingFileHandler(BaseRotatingHandler):
    """

        Handler for logging to a set of files, which switches from one file
        to the next when the current file reaches a certain size.
    
    """
    def __init__(self, filename, mode='a', maxBytes=0, backupCount=0, encoding=None, delay=False):
        """

                Open the specified file and use it as the stream for logging.

                By default, the file grows indefinitely. You can specify particular
                values of maxBytes and backupCount to allow the file to rollover at
                a predetermined size.

                Rollover occurs whenever the current log file is nearly maxBytes in
                length. If backupCount is >= 1, the system will successively create
                new files with the same pathname as the base file, but with extensions
                ".1", ".2" etc. appended to it. For example, with a backupCount of 5
                and a base file name of "app.log", you would get "app.log",
                "app.log.1", "app.log.2", ... through to "app.log.5". The file being
                written to is always "app.log" - when it gets filled up, it is closed
                and renamed to "app.log.1", and if files "app.log.1", "app.log.2" etc.
                exist, then they are renamed to "app.log.2", "app.log.3" etc.
                respectively.

                If maxBytes is zero, rollover never occurs.
        
        """
    def doRollover(self):
        """

                Do a rollover, as described in __init__().
        
        """
    def shouldRollover(self, record):
        """

                Determine if rollover should occur.

                Basically, see if the supplied record would cause the file to exceed
                the size limit we have.
        
        """
def TimedRotatingFileHandler(BaseRotatingHandler):
    """

        Handler for logging to a file, rotating the log file at certain timed
        intervals.

        If backupCount is > 0, when rollover is done, no more than backupCount
        files are kept - the oldest ones are deleted.
    
    """
    def __init__(self, filename, when='h', interval=1, backupCount=0, encoding=None, delay=False, utc=False, atTime=None):
        """
        'a'
        """
    def computeRollover(self, currentTime):
        """

                Work out the rollover time based on the specified time.
        
        """
    def shouldRollover(self, record):
        """

                Determine if rollover should occur.

                record is not used, as we are just comparing times, but it is needed so
                the method signatures are the same
        
        """
    def getFilesToDelete(self):
        """

                Determine the files to delete when rolling over.

                More specific than the earlier method, which just used glob.glob().
        
        """
    def doRollover(self):
        """

                do a rollover; in this case, a date/time stamp is appended to the filename
                when the rollover happens.  However, you want the file to be named for the
                start of the interval, not the current time.  If there is a backup count,
                then we have to get a list of matching filenames, sort them and remove
                the one with the oldest suffix.
        
        """
def WatchedFileHandler(logging.FileHandler):
    """

        A handler for logging to a file, which watches the file
        to see if it has changed while in use. This can happen because of
        usage of programs such as newsyslog and logrotate which perform
        log file rotation. This handler, intended for use under Unix,
        watches the file to see if it has changed since the last emit.
        (A file has changed if its device or inode have changed.)
        If it has changed, the old file stream is closed, and the file
        opened to get a new stream.

        This handler is not appropriate for use under Windows, because
        under Windows open files cannot be moved or renamed - logging
        opens the files with exclusive locks - and so there is no need
        for such a handler. Furthermore, ST_INO is not supported under
        Windows; stat always returns zero for this value.

        This handler is based on a suggestion and patch by Chad J.
        Schroeder.
    
    """
    def __init__(self, filename, mode='a', encoding=None, delay=False):
        """

                Reopen log file if needed.

                Checks if the underlying file has changed, and if it
                has, close the old stream and reopen the file to get the
                current stream.
        
        """
    def emit(self, record):
        """

                Emit a record.

                If underlying file has changed, reopen the file before emitting the
                record to it.
        
        """
def SocketHandler(logging.Handler):
    """

        A handler class which writes logging records, in pickle format, to
        a streaming socket. The socket is kept open across logging calls.
        If the peer resets it, an attempt is made to reconnect on the next call.
        The pickle which is sent is that of the LogRecord's attribute dictionary
        (__dict__), so that the receiver does not need to have the logging module
        installed in order to process the logging event.

        To unpickle the record at the receiving end into a LogRecord, use the
        makeLogRecord function.
    
    """
    def __init__(self, host, port):
        """

                Initializes the handler with a specific host address and port.

                When the attribute *closeOnError* is set to True - if a socket error
                occurs, the socket is silently closed and then reopened on the next
                logging call.
        
        """
    def makeSocket(self, timeout=1):
        """

                A factory method which allows subclasses to define the precise
                type of socket they want.
        
        """
    def createSocket(self):
        """

                Try to create a socket, using an exponential backoff with
                a max retry time. Thanks to Robert Olson for the original patch
                (SF #815911) which has been slightly refactored.
        
        """
    def send(self, s):
        """

                Send a pickled string to the socket.

                This function allows for partial sends which can happen when the
                network is busy.
        
        """
    def makePickle(self, record):
        """

                Pickles the record in binary format with a length prefix, and
                returns it ready for transmission across the socket.
        
        """
    def handleError(self, record):
        """

                Handle an error during logging.

                An error has occurred during logging. Most likely cause -
                connection lost. Close the socket so that we can retry on the
                next event.
        
        """
    def emit(self, record):
        """

                Emit a record.

                Pickles the record and writes it to the socket in binary format.
                If there is an error with the socket, silently drop the packet.
                If there was a problem with the socket, re-establishes the
                socket.
        
        """
    def close(self):
        """

                Closes the socket.
        
        """
def DatagramHandler(SocketHandler):
    """

        A handler class which writes logging records, in pickle format, to
        a datagram socket.  The pickle which is sent is that of the LogRecord's
        attribute dictionary (__dict__), so that the receiver does not need to
        have the logging module installed in order to process the logging event.

        To unpickle the record at the receiving end into a LogRecord, use the
        makeLogRecord function.

    
    """
    def __init__(self, host, port):
        """

                Initializes the handler with a specific host address and port.
        
        """
    def makeSocket(self):
        """

                The factory method of SocketHandler is here overridden to create
                a UDP socket (SOCK_DGRAM).
        
        """
    def send(self, s):
        """

                Send a pickled string to a socket.

                This function no longer allows for partial sends which can happen
                when the network is busy - UDP does not guarantee delivery and
                can deliver packets out of sequence.
        
        """
def SysLogHandler(logging.Handler):
    """

        A handler class which sends formatted logging records to a syslog
        server. Based on Sam Rushing's syslog module:
        http://www.nightmare.com/squirl/python-ext/misc/syslog.py
        Contributed by Nicolas Untz (after which minor refactoring changes
        have been made).
    
    """
2021-03-02 20:54:35,822 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, address=('localhost', SYSLOG_UDP_PORT),
                 facility=LOG_USER, socktype=None):
        """

                Initialize a handler.

                If address is specified as a string, a UNIX socket is used. To log to a
                local syslogd, "SysLogHandler(address="/dev/log")" can be used.
                If facility is not specified, LOG_USER is used. If socktype is
                specified as socket.SOCK_DGRAM or socket.SOCK_STREAM, that specific
                socket type will be used. For Unix sockets, you can also specify a
                socktype of None, in which case socket.SOCK_DGRAM will be used, falling
                back to socket.SOCK_STREAM.
        
        """
    def _connect_unixsocket(self, address):
        """
         it worked, so set self.socktype to the used type

        """
    def encodePriority(self, facility, priority):
        """

                Encode the facility and priority. You can pass in strings or
                integers - if strings are passed, the facility_names and
                priority_names mapping dictionaries are used to convert them to
                integers.
        
        """
    def close(self):
        """

                Closes the socket.
        
        """
    def mapPriority(self, levelName):
        """

                Map a logging level name to a key in the priority_names map.
                This is useful in two scenarios: when custom levels are being
                used, and in the case where you can't do a straightforward
                mapping by lowercasing the logging level name because of locale-
                specific issues (see SF #1524081).
        
        """
    def emit(self, record):
        """

                Emit a record.

                The record is formatted, and then sent to the syslog server. If
                exception information is present, it is NOT sent to the server.
        
        """
def SMTPHandler(logging.Handler):
    """

        A handler class which sends an SMTP email for each logging event.
    
    """
2021-03-02 20:54:35,825 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, mailhost, fromaddr, toaddrs, subject,
                 credentials=None, secure=None, timeout=5.0):
        """

                Initialize the handler.

                Initialize the instance with the from and to addresses and subject
                line of the email. To specify a non-standard SMTP port, use the
                (host, port) tuple format for the mailhost argument. To specify
                authentication credentials, supply a (username, password) tuple
                for the credentials argument. To specify the use of a secure
                protocol (TLS), pass in a tuple for the secure argument. This will
                only be used when authentication credentials are supplied. The tuple
                will be either an empty tuple, or a single-value tuple with the name
                of a keyfile, or a 2-value tuple with the names of the keyfile and
                certificate file. (This tuple is passed to the `starttls` method).
                A timeout in seconds can be specified for the SMTP connection (the
                default is one second).
        
        """
    def getSubject(self, record):
        """

                Determine the subject for the email.

                If you want to specify a subject line which is record-dependent,
                override this method.
        
        """
    def emit(self, record):
        """

                Emit a record.

                Format the record and send it to the specified addressees.
        
        """
def NTEventLogHandler(logging.Handler):
    """

        A handler class which sends events to the NT Event Log. Adds a
        registry entry for the specified application name. If no dllname is
        provided, win32service.pyd (which contains some basic message
        placeholders) is used. Note that use of these placeholders will make
        your event logs big, as the entire message source is held in the log.
        If you want slimmer logs, you have to pass in the name of your own DLL
        which contains the message definitions you want to use in the event log.
    
    """
    def __init__(self, appname, dllname=None, logtype="Application"):
        """
        r'win32service.pyd'
        """
    def getMessageID(self, record):
        """

                Return the message ID for the event record. If you are using your
                own messages, you could do this by having the msg passed to the
                logger being an ID rather than a formatting string. Then, in here,
                you could use a dictionary lookup to get the message ID. This
                version returns 1, which is the base message ID in win32service.pyd.
        
        """
    def getEventCategory(self, record):
        """

                Return the event category for the record.

                Override this if you want to specify your own categories. This version
                returns 0.
        
        """
    def getEventType(self, record):
        """

                Return the event type for the record.

                Override this if you want to specify your own types. This version does
                a mapping using the handler's typemap attribute, which is set up in
                __init__() to a dictionary which contains mappings for DEBUG, INFO,
                WARNING, ERROR and CRITICAL. If you are using your own levels you will
                either need to override this method or place a suitable dictionary in
                the handler's typemap attribute.
        
        """
    def emit(self, record):
        """

                Emit a record.

                Determine the message ID, event category and event type. Then
                log the message in the NT event log.
        
        """
    def close(self):
        """

                Clean up this handler.

                You can remove the application name from the registry as a
                source of event log entries. However, if you do this, you will
                not be able to see the events as you intended in the Event Log
                Viewer - it needs to be able to access the registry to get the
                DLL name.
        
        """
def HTTPHandler(logging.Handler):
    """

        A class which sends records to a Web server, using either GET or
        POST semantics.
    
    """
2021-03-02 20:54:35,829 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, host, url, method="GET", secure=False, credentials=None,
                 context=None):
        """

                Initialize the instance with the host, the request URL, and the method
                ("GET" or "POST")
        
        """
    def mapLogRecord(self, record):
        """

                Default implementation of mapping the log record into a dict
                that is sent as the CGI data. Overwrite in your class.
                Contributed by Franz Glasner.
        
        """
    def emit(self, record):
        """

                Emit a record.

                Send the record to the Web server as a percent-encoded dictionary
        
        """
def BufferingHandler(logging.Handler):
    """

      A handler class which buffers logging records in memory. Whenever each
      record is added to the buffer, a check is made to see if the buffer should
      be flushed. If it should, then flush() is expected to do what's needed.
    
    """
    def __init__(self, capacity):
        """

                Initialize the handler with the buffer size.
        
        """
    def shouldFlush(self, record):
        """

                Should the handler flush its buffer?

                Returns true if the buffer is up to capacity. This method can be
                overridden to implement custom flushing strategies.
        
        """
    def emit(self, record):
        """

                Emit a record.

                Append the record. If shouldFlush() tells us to, call flush() to process
                the buffer.
        
        """
    def flush(self):
        """

                Override to implement custom flushing behaviour.

                This version just zaps the buffer to empty.
        
        """
    def close(self):
        """

                Close the handler.

                This version just flushes and chains to the parent class' close().
        
        """
def MemoryHandler(BufferingHandler):
    """

        A handler class which buffers logging records in memory, periodically
        flushing them to a target handler. Flushing occurs whenever the buffer
        is full, or when an event of a certain severity or greater is seen.
    
    """
2021-03-02 20:54:35,832 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, capacity, flushLevel=logging.ERROR, target=None,
                 flushOnClose=True):
        """

                Initialize the handler with the buffer size, the level at which
                flushing should occur and an optional target.

                Note that without a target being set either here or via setTarget(),
                a MemoryHandler is no use to anyone!

                The ``flushOnClose`` argument is ``True`` for backward compatibility
                reasons - the old behaviour is that when the handler is closed, the
                buffer is flushed, even if the flush level hasn't been exceeded nor the
                capacity exceeded. To prevent this, set ``flushOnClose`` to ``False``.
        
        """
    def shouldFlush(self, record):
        """

                Check for buffer full or a record at the flushLevel or higher.
        
        """
    def setTarget(self, target):
        """

                Set the target handler for this handler.
        
        """
    def flush(self):
        """

                For a MemoryHandler, flushing means just sending the buffered
                records to the target, if there is one. Override if you want
                different behaviour.

                The record buffer is also cleared by this operation.
        
        """
    def close(self):
        """

                Flush, if appropriately configured, set the target to None and lose the
                buffer.
        
        """
def QueueHandler(logging.Handler):
    """

        This handler sends events to a queue. Typically, it would be used together
        with a multiprocessing Queue to centralise logging to file in one process
        (in a multi-process application), so as to avoid file write contention
        between processes.

        This code is new in Python 3.2, but this class can be copy pasted into
        user code for use with earlier Python versions.
    
    """
    def __init__(self, queue):
        """

                Initialise an instance, using the passed queue.
        
        """
    def enqueue(self, record):
        """

                Enqueue a record.

                The base implementation uses put_nowait. You may want to override
                this method if you want to use blocking, timeouts or custom queue
                implementations.
        
        """
    def prepare(self, record):
        """

                Prepares a record for queuing. The object returned by this method is
                enqueued.

                The base implementation formats the record to merge the message
                and arguments, and removes unpickleable items from the record
                in-place.

                You might want to override this method if you want to convert
                the record to a dict or JSON string, or send a modified copy
                of the record while leaving the original intact.
        
        """
    def emit(self, record):
        """

                Emit a record.

                Writes the LogRecord to the queue, preparing it for pickling first.
        
        """
def QueueListener(object):
    """

        This class implements an internal threaded listener which watches for
        LogRecords being added to a queue, removes them and passes them to a
        list of handlers for processing.
    
    """
    def __init__(self, queue, *handlers, respect_handler_level=False):
        """

                Initialise an instance with the specified queue and
                handlers.
        
        """
    def dequeue(self, block):
        """

                Dequeue a record and return it, optionally blocking.

                The base implementation uses get. You may want to override this method
                if you want to use timeouts or work with custom queue implementations.
        
        """
    def start(self):
        """

                Start the listener.

                This starts up a background thread to monitor the queue for
                LogRecords to process.
        
        """
    def prepare(self, record):
        """

                Prepare a record for handling.

                This method just returns the passed-in record. You may want to
                override this method if you need to do any custom marshalling or
                manipulation of the record before passing it to the handlers.
        
        """
    def handle(self, record):
        """

                Handle a record.

                This just loops through the handlers offering them the record
                to handle.
        
        """
    def _monitor(self):
        """

                Monitor the queue for records, and ask the handler
                to deal with them.

                This method runs on a separate, internal thread.
                The thread will terminate if it sees a sentinel object in the queue.
        
        """
    def enqueue_sentinel(self):
        """

                This is used to enqueue the sentinel record.

                The base implementation uses put_nowait. You may want to override this
                method if you want to use timeouts or work with custom queue
                implementations.
        
        """
    def stop(self):
        """

                Stop the listener.

                This asks the thread to terminate, and then waits for it to do so.
                Note that if you don't call this before your application exits, there
                may be some records still left on the queue, which won't be processed.
        
        """
