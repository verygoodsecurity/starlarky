def fileConfig(fname, defaults=None, disable_existing_loggers=True):
    """

        Read the logging configuration from a ConfigParser-format file.

        This can be called several times from an application, allowing an end user
        the ability to select from various pre-canned configurations (if the
        developer provides a mechanism to present the choices and load the chosen
        configuration).
    
    """
def _resolve(name):
    """
    Resolve a dotted name to a global object.
    """
def _strip_spaces(alist):
    """
    Create and return formatters
    """
def _install_handlers(cp, formatters):
    """
    Install and return handlers
    """
def _handle_existing_loggers(existing, child_loggers, disable_existing):
    """

        When (re)configuring logging, handle loggers which were in the previous
        configuration but are not in the new configuration. There's no point
        deleting them as other threads may continue to hold references to them;
        and by disabling them, you stop them doing any logging.

        However, don't disable children of named loggers, as that's probably not
        what was intended by the user. Also, allow existing loggers to NOT be
        disabled if disable_existing is false.
    
    """
def _install_loggers(cp, handlers, disable_existing):
    """
    Create and install loggers
    """
def _clearExistingHandlers():
    """
    Clear and close existing handlers
    """
def valid_ident(s):
    """
    'Not a valid Python identifier: %r'
    """
def ConvertingMixin(object):
    """
    For ConvertingXXX's, this mixin class provides common functions
    """
    def convert_with_key(self, key, value, replace=True):
        """
        If the converted value is different, save for next time

        """
    def convert(self, value):
        """
         The ConvertingXXX classes are wrappers around standard Python containers,
         and they serve to convert any suitable values in the container. The
         conversion converts base dicts, lists and tuples to their wrapped
         equivalents, whereas strings which match a conversion format are converted
         appropriately.

         Each wrapper should have a configurator attribute holding the actual
         configurator to use for conversion.


        """
def ConvertingDict(dict, ConvertingMixin):
    """
    A converting dictionary wrapper.
    """
    def __getitem__(self, key):
        """
        A converting list wrapper.
        """
    def __getitem__(self, key):
        """
        A converting tuple wrapper.
        """
    def __getitem__(self, key):
        """
         Can't replace a tuple entry.

        """
def BaseConfigurator(object):
    """

        The configurator base class which defines some useful defaults.
    
    """
    def __init__(self, config):
        """

                Resolve strings to objects using standard import and attribute
                syntax.
        
        """
    def ext_convert(self, value):
        """
        Default converter for the ext:// protocol.
        """
    def cfg_convert(self, value):
        """
        Default converter for the cfg:// protocol.
        """
    def convert(self, value):
        """

                Convert values to an appropriate type. dicts, lists and tuples are
                replaced by their converting alternatives. Strings are checked to
                see if they have a conversion format and are converted if they do.
        
        """
    def configure_custom(self, config):
        """
        Configure an object with a user-supplied factory.
        """
    def as_tuple(self, value):
        """
        Utility function which converts lists to tuples.
        """
def DictConfigurator(BaseConfigurator):
    """

        Configure logging using a dictionary-like object to describe the
        configuration.
    
    """
    def configure(self):
        """
        Do the configuration.
        """
    def configure_formatter(self, config):
        """
        Configure a formatter from a dictionary.
        """
    def configure_filter(self, config):
        """
        Configure a filter from a dictionary.
        """
    def add_filters(self, filterer, filters):
        """
        Add filters to a filterer from a list of names.
        """
    def configure_handler(self, config):
        """
        Configure a handler from a dictionary.
        """
    def add_handlers(self, logger, handlers):
        """
        Add handlers to a logger from a list of names.
        """
    def common_logger_config(self, logger, config, incremental=False):
        """

                Perform configuration which is common to root and non-root loggers.
        
        """
    def configure_logger(self, name, config, incremental=False):
        """
        Configure a non-root logger from a dictionary.
        """
    def configure_root(self, config, incremental=False):
        """
        Configure a root logger from a dictionary.
        """
def dictConfig(config):
    """
    Configure logging using a dictionary.
    """
def listen(port=DEFAULT_LOGGING_CONFIG_PORT, verify=None):
    """

        Start up a socket server on the specified port, and listen for new
        configurations.

        These will be sent as a file suitable for processing by fileConfig().
        Returns a Thread object on which you can call start() to start the server,
        and which you can join() when appropriate. To stop the server, call
        stopListening().

        Use the ``verify`` argument to verify any bytes received across the wire
        from a client. If specified, it should be a callable which receives a
        single argument - the bytes of configuration data received across the
        network - and it should return either ``None``, to indicate that the
        passed in bytes could not be verified and should be discarded, or a
        byte string which is then passed to the configuration machinery as
        normal. Note that you can return transformed bytes, e.g. by decrypting
        the bytes passed in.
    
    """
    def ConfigStreamHandler(StreamRequestHandler):
    """

            Handler for a logging configuration request.

            It expects a completely new logging configuration and uses fileConfig
            to install it.
        
    """
        def handle(self):
            """

                        Handle a request.

                        Each request is expected to be a 4-byte length, packed using
                        struct.pack(">L", n), followed by the config file.
                        Uses fileConfig() to do the grunt work.
            
            """
    def ConfigSocketReceiver(ThreadingTCPServer):
    """

            A simple TCP socket-based logging config receiver.
        
    """
2021-03-02 20:54:35,637 : INFO : tokenize_signature : --> do i ever get here?
        def __init__(self, host='localhost', port=DEFAULT_LOGGING_CONFIG_PORT,
                     handler=None, ready=None, verify=None):
            """

                Stop the listening server which was created with a call to listen().
    
            """
