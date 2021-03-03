def Address:
    """
    ''
    """
    def display_name(self):
        """
        The addr_spec (username@domain) portion of the address, quoted
                according to RFC 5322 rules, but with no Content Transfer Encoding.
        
        """
    def __repr__(self):
        """
        {}(display_name={!r}, username={!r}, domain={!r})
        """
    def __str__(self):
        """
        ''
        """
    def __eq__(self, other):
        """
        Create an object representing an address group.

                An address group consists of a display_name followed by colon and a
                list of addresses (see Address) terminated by a semi-colon.  The Group
                is created by specifying a display_name and a possibly empty list of
                Address objects.  A Group can also be used to represent a single
                address that is not in a group, which is convenient when manipulating
                lists that are a combination of Groups and individual Addresses.  In
                this case the display_name should be set to None.  In particular, the
                string representation of a Group whose display_name is None is the same
                as the Address object, if there is one and only one Address object in
                the addresses list.

        
        """
    def display_name(self):
        """
        {}(display_name={!r}, addresses={!r}
        """
    def __str__(self):
        """
        , 
        """
    def __eq__(self, other):
        """
         Header Classes #


        """
def BaseHeader(str):
    """
    Base class for message headers.

        Implements generic behavior and provides tools for subclasses.

        A subclass must define a classmethod named 'parse' that takes an unfolded
        value string and a dictionary as its arguments.  The dictionary will
        contain one key, 'defects', initialized to an empty list.  After the call
        the dictionary must contain two additional keys: parse_tree, set to the
        parse tree obtained from parsing the header, and 'decoded', set to the
        string value of the idealized representation of the data from the value.
        (That is, encoded words are decoded, and values that have canonical
        representations are so represented.)

        The defects key is intended to collect parsing defects, which the message
        parser will subsequently dispose of as appropriate.  The parser should not,
        insofar as practical, raise any errors.  Defects should be added to the
        list instead.  The standard header parsers register defects for RFC
        compliance issues, for obsolete RFC syntax, and for unrecoverable parsing
        errors.

        The parse method may add additional keys to the dictionary.  In this case
        the subclass must define an 'init' method, which will be passed the
        dictionary as its keyword arguments.  The method should use (usually by
        setting them as the value of similarly named attributes) and remove all the
        extra keys added by its parse method, and then use super to call its parent
        class with the remaining arguments and keywords.

        The subclass should also make sure that a 'max_count' attribute is defined
        that is either None or 1. XXX: need to better define this API.

    
    """
    def __new__(cls, name, value):
        """
        'defects'
        """
    def init(self, name, *, parse_tree, defects):
        """
        Fold header according to policy.

                The parsed representation of the header is folded according to
                RFC5322 rules, as modified by the policy.  If the parse tree
                contains surrogateescaped bytes, the bytes are CTE encoded using
                the charset 'unknown-8bit".

                Any non-ASCII characters in the parse tree are CTE encoded using
                charset utf-8. XXX: make this a policy setting.

                The returned value is an ASCII-only string possibly containing linesep
                characters, and ending with a linesep character.  The string includes
                the header name and the ': ' separator.

        
        """
def _reconstruct_header(cls_name, bases, value):
    """
    'parse_tree'
    """
def UniqueUnstructuredHeader(UnstructuredHeader):
    """
    Header whose value consists of a single timestamp.

        Provides an additional attribute, datetime, which is either an aware
        datetime using a timezone, or a naive datetime if the timezone
        in the input string is -0000.  Also accepts a datetime as input.
        The 'value' attribute is the normalized form of the timestamp,
        which means it is the output of format_datetime on the datetime.
    
    """
    def parse(cls, value, kwds):
        """
        'defects'
        """
    def init(self, *args, **kw):
        """
        'datetime'
        """
    def datetime(self):
        """
        'this should not happen'
        """
    def parse(cls, value, kwds):
        """
         We are translating here from the RFC language (address/mailbox)
         to our API language (group/address).

        """
    def init(self, *args, **kw):
        """
        'groups'
        """
    def groups(self):
        """
        value of single address header {} is not 
        a single address
        """
def UniqueSingleAddressHeader(SingleAddressHeader):
    """
    'parse_tree'
    """
    def init(self, *args, **kw):
        """
        'version'
        """
    def major(self):
        """
         Mixin that handles the params dict.  Must be subclassed and
         a property value_parser for the specific header provided.


        """
    def parse(cls, value, kwds):
        """
        'parse_tree'
        """
    def init(self, *args, **kw):
        """
        'params'
        """
    def params(self):
        """
        '/'
        """
def ContentDispositionHeader(ParameterizedMIMEHeader):
    """
    'parse_tree'
    """
    def init(self, *args, **kw):
        """
        'parse_tree'
        """
def HeaderRegistry:
    """
    A header_factory and header registry.
    """
2021-03-02 20:54:41,885 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, base_class=BaseHeader, default_class=UnstructuredHeader,
                       use_default_map=True):
        """
        Create a header_factory that works with the Policy API.

                base_class is the class that will be the last class in the created
                header class's __bases__ list.  default_class is the class that will be
                used if "name" (see __call__) does not appear in the registry.
                use_default_map controls whether or not the default mapping of names to
                specialized classes is copied in to the registry when the factory is
                created.  The default is True.

        
        """
    def map_to_type(self, name, cls):
        """
        Register cls as the specialized class for handling "name" headers.

        
        """
    def __getitem__(self, name):
        """
        '_'
        """
    def __call__(self, name, value):
        """
        Create a header instance for header 'name' from 'value'.

                Creates a header instance by creating a specialized class for parsing
                and representing the specified header by combining the factory
                base_class with a specialized class from the registry or the
                default_class, and passing the name and value to the constructed
                class's constructor.

        
        """
