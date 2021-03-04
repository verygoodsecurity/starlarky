def ParseError(SyntaxError):
    """
    An error when parsing an XML document.

        In addition to its exception value, a ParseError contains
        two extra attributes:
            'code'     - the specific exception code
            'position' - the line and column of the error

    
    """
def iselement(element):
    """
    Return True if *element* appears to be an Element.
    """
def Element:
    """
    An XML element.

        This class is the reference implementation of the Element interface.

        An element's length is its number of subelements.  That means if you
        want to check if an element is truly empty, you should check BOTH
        its length AND its text attribute.

        The element tag, attribute names, and attribute values can be either
        bytes or strings.

        *tag* is the element name.  *attrib* is an optional dictionary containing
        element attributes. *extra* are additional element attributes given as
        keyword arguments.

        Example form:
            <tag attrib>text<child/>...</tag>tail

    
    """
    def __init__(self, tag, attrib={}, **extra):
        """
        attrib must be dict, not %s
        """
    def __repr__(self):
        """
        <%s %r at %#x>
        """
    def makeelement(self, tag, attrib):
        """
        Create a new element with the same type.

                *tag* is a string containing the element name.
                *attrib* is a dictionary containing the element attributes.

                Do not call this method, use the SubElement factory function instead.

        
        """
    def copy(self):
        """
        Return copy of current element.

                This creates a shallow copy. Subelements will be shared with the
                original tree.

        
        """
    def __len__(self):
        """
        The behavior of this method will change in future versions.  
        Use specific 'len(elem)' or 'elem is not None' test instead.
        """
    def __getitem__(self, index):
        """
        Add *subelement* to the end of this element.

                The new element will appear in document order after the last existing
                subelement (or directly after the text, if it's the first subelement),
                but before the end tag for this element.

        
        """
    def extend(self, elements):
        """
        Append subelements from a sequence.

                *elements* is a sequence with zero or more elements.

        
        """
    def insert(self, index, subelement):
        """
        Insert *subelement* at position *index*.
        """
    def _assert_is_element(self, e):
        """
         Need to refer to the actual Python implementation, not the
         shadowing C implementation.

        """
    def remove(self, subelement):
        """
        Remove matching subelement.

                Unlike the find methods, this method compares elements based on
                identity, NOT ON tag value or contents.  To remove subelements by
                other means, the easiest way is to use a list comprehension to
                select what elements to keep, and then use slice assignment to update
                the parent element.

                ValueError is raised if a matching element could not be found.

        
        """
    def getchildren(self):
        """
        (Deprecated) Return all subelements.

                Elements are returned in document order.

        
        """
    def find(self, path, namespaces=None):
        """
        Find first matching element by tag name or path.

                *path* is a string having either an element tag or an XPath,
                *namespaces* is an optional mapping from namespace prefix to full name.

                Return the first matching element, or None if no element was found.

        
        """
    def findtext(self, path, default=None, namespaces=None):
        """
        Find text for first matching element by tag name or path.

                *path* is a string having either an element tag or an XPath,
                *default* is the value to return if the element was not found,
                *namespaces* is an optional mapping from namespace prefix to full name.

                Return text content of first matching element, or default value if
                none was found.  Note that if an element is found having no text
                content, the empty string is returned.

        
        """
    def findall(self, path, namespaces=None):
        """
        Find all matching subelements by tag name or path.

                *path* is a string having either an element tag or an XPath,
                *namespaces* is an optional mapping from namespace prefix to full name.

                Returns list containing all matching elements in document order.

        
        """
    def iterfind(self, path, namespaces=None):
        """
        Find all matching subelements by tag name or path.

                *path* is a string having either an element tag or an XPath,
                *namespaces* is an optional mapping from namespace prefix to full name.

                Return an iterable yielding all matching elements in document order.

        
        """
    def clear(self):
        """
        Reset element.

                This function removes all subelements, clears all attributes, and sets
                the text and tail attributes to None.

        
        """
    def get(self, key, default=None):
        """
        Get element attribute.

                Equivalent to attrib.get, but some implementations may handle this a
                bit more efficiently.  *key* is what attribute to look for, and
                *default* is what to return if the attribute was not found.

                Returns a string containing the attribute value, or the default if
                attribute was not found.

        
        """
    def set(self, key, value):
        """
        Set element attribute.

                Equivalent to attrib[key] = value, but some implementations may handle
                this a bit more efficiently.  *key* is what attribute to set, and
                *value* is the attribute value to set it to.

        
        """
    def keys(self):
        """
        Get list of attribute names.

                Names are returned in an arbitrary order, just like an ordinary
                Python dict.  Equivalent to attrib.keys()

        
        """
    def items(self):
        """
        Get element attributes as a sequence.

                The attributes are returned in arbitrary order.  Equivalent to
                attrib.items().

                Return a list of (name, value) tuples.

        
        """
    def iter(self, tag=None):
        """
        Create tree iterator.

                The iterator loops over the element and all subelements in document
                order, returning all elements with a matching tag.

                If the tree structure is modified during iteration, new or removed
                elements may or may not be included.  To get a stable set, use the
                list() function on the iterator, and loop over the resulting list.

                *tag* is what tags to look for (default is to return all elements)

                Return an iterator containing all the matching elements.

        
        """
    def getiterator(self, tag=None):
        """
        This method will be removed in future versions.  
        Use 'elem.iter()' or 'list(elem.iter())' instead.
        """
    def itertext(self):
        """
        Create text iterator.

                The iterator loops over the element and all subelements in document
                order, returning all inner text.

        
        """
def SubElement(parent, tag, attrib={}, **extra):
    """
    Subelement factory which creates an element instance, and appends it
        to an existing parent.

        The element tag, attribute names, and attribute values can be either
        bytes or Unicode strings.

        *parent* is the parent element, *tag* is the subelements name, *attrib* is
        an optional directory containing element attributes, *extra* are
        additional attributes given as keyword arguments.

    
    """
def Comment(text=None):
    """
    Comment element factory.

        This function creates a special element which the standard serializer
        serializes as an XML comment.

        *text* is a string containing the comment string.

    
    """
def ProcessingInstruction(target, text=None):
    """
    Processing Instruction element factory.

        This function creates a special element which the standard serializer
        serializes as an XML comment.

        *target* is a string containing the processing instruction, *text* is a
        string containing the processing instruction contents, if any.

    
    """
def QName:
    """
    Qualified name wrapper.

        This class can be used to wrap a QName attribute value in order to get
        proper namespace handing on output.

        *text_or_uri* is a string containing the QName value either in the form
        {uri}local, or if the tag argument is given, the URI part of a QName.

        *tag* is an optional argument which if given, will make the first
        argument (text_or_uri) be interpreted as a URI, and this argument (tag)
        be interpreted as a local name.

    
    """
    def __init__(self, text_or_uri, tag=None):
        """
        {%s}%s
        """
    def __str__(self):
        """
        '<%s %r>'
        """
    def __hash__(self):
        """
         --------------------------------------------------------------------



        """
def ElementTree:
    """
    An XML element hierarchy.

        This class also provides support for serialization to and from
        standard XML.

        *element* is an optional root element node,
        *file* is an optional file handle or file name of an XML file whose
        contents will be used to initialize the tree with.

    
    """
    def __init__(self, element=None, file=None):
        """
         assert element is None or iselement(element)

        """
    def getroot(self):
        """
        Return root element of this tree.
        """
    def _setroot(self, element):
        """
        Replace root element of this tree.

                This will discard the current contents of the tree and replace it
                with the given element.  Use with care!

        
        """
    def parse(self, source, parser=None):
        """
        Load external XML document into element tree.

                *source* is a file name or file object, *parser* is an optional parser
                instance that defaults to XMLParser.

                ParseError is raised if the parser fails to parse the document.

                Returns the root element of the given source document.

        
        """
    def iter(self, tag=None):
        """
        Create and return tree iterator for the root element.

                The iterator loops over all elements in this tree, in document order.

                *tag* is a string with the tag name to iterate over
                (default is to return all elements).

        
        """
    def getiterator(self, tag=None):
        """
        This method will be removed in future versions.  
        Use 'tree.iter()' or 'list(tree.iter())' instead.
        """
    def find(self, path, namespaces=None):
        """
        Find first matching element by tag name or path.

                Same as getroot().find(path), which is Element.find()

                *path* is a string having either an element tag or an XPath,
                *namespaces* is an optional mapping from namespace prefix to full name.

                Return the first matching element, or None if no element was found.

        
        """
    def findtext(self, path, default=None, namespaces=None):
        """
        Find first matching element by tag name or path.

                Same as getroot().findtext(path),  which is Element.findtext()

                *path* is a string having either an element tag or an XPath,
                *namespaces* is an optional mapping from namespace prefix to full name.

                Return the first matching element, or None if no element was found.

        
        """
    def findall(self, path, namespaces=None):
        """
        Find all matching subelements by tag name or path.

                Same as getroot().findall(path), which is Element.findall().

                *path* is a string having either an element tag or an XPath,
                *namespaces* is an optional mapping from namespace prefix to full name.

                Return list containing all matching elements in document order.

        
        """
    def iterfind(self, path, namespaces=None):
        """
        Find all matching subelements by tag name or path.

                Same as getroot().iterfind(path), which is element.iterfind()

                *path* is a string having either an element tag or an XPath,
                *namespaces* is an optional mapping from namespace prefix to full name.

                Return an iterable yielding all matching elements in document order.

        
        """
2021-03-02 20:53:43,744 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:43,745 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:43,745 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:43,745 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:43,745 : INFO : tokenize_signature : --> do i ever get here?
    def write(self, file_or_filename,
              encoding=None,
              xml_declaration=None,
              default_namespace=None,
              method=None, *,
              short_empty_elements=True):
        """
        Write element tree to a file as XML.

                Arguments:
                  *file_or_filename* -- file name or a file object opened for writing

                  *encoding* -- the output encoding (default: US-ASCII)

                  *xml_declaration* -- bool indicating if an XML declaration should be
                                       added to the output. If None, an XML declaration
                                       is added if encoding IS NOT either of:
                                       US-ASCII, UTF-8, or Unicode

                  *default_namespace* -- sets the default XML namespace (for "xmlns")

                  *method* -- either "xml" (default), "html, "text", or "c14n"

                  *short_empty_elements* -- controls the formatting of elements
                                            that contain no content. If True (default)
                                            they are emitted as a single self-closed
                                            tag, otherwise they are emitted as a pair
                                            of start/end tags

        
        """
    def write_c14n(self, file):
        """
         lxml.etree compatibility.  use output method instead

        """
def _get_writer(file_or_filename, encoding):
    """
     returns text write method and release all resources after using

    """
def _namespaces(elem, default_namespace=None):
    """
     identify namespaces used in this tree

     maps qnames to *encoded* prefix:local names

    """
    def add_qname(qname):
        """
         calculate serialized qname representation

        """
2021-03-02 20:53:43,749 : INFO : tokenize_signature : --> do i ever get here?
def _serialize_xml(write, elem, qnames, namespaces,
                   short_empty_elements, **kwargs):
    """
    <!--%s-->
    """
def _serialize_html(write, elem, qnames, namespaces, **kwargs):
    """
    <!--%s-->
    """
def _serialize_text(write, elem):
    """
    xml
    """
def register_namespace(prefix, uri):
    """
    Register a namespace prefix.

        The registry is global, and any existing mapping for either the
        given prefix or the namespace URI will be removed.

        *prefix* is the namespace prefix, *uri* is a namespace uri. Tags and
        attributes in this namespace will be serialized with prefix if possible.

        ValueError is raised if prefix is reserved or is invalid.

    
    """
def _raise_serialization_error(text):
    """
    cannot serialize %r (type %s)
    """
def _escape_cdata(text):
    """
     escape character data

    """
def _escape_attrib(text):
    """
     escape attribute value

    """
def _escape_attrib_html(text):
    """
     escape attribute value

    """
2021-03-02 20:53:43,754 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:43,754 : INFO : tokenize_signature : --> do i ever get here?
def tostring(element, encoding=None, method=None, *,
             xml_declaration=None, default_namespace=None,
             short_empty_elements=True):
    """
    Generate string representation of XML element.

        All subelements are included.  If encoding is "unicode", a string
        is returned. Otherwise a bytestring is returned.

        *element* is an Element instance, *encoding* is an optional output
        encoding defaulting to US-ASCII, *method* is an optional output which can
        be one of "xml" (default), "html", "text" or "c14n", *default_namespace*
        sets the default XML namespace (for "xmlns").

        Returns an (optionally) encoded string containing the XML data.

    
    """
def _ListDataStream(io.BufferedIOBase):
    """
    An auxiliary stream accumulating into a list reference.
    """
    def __init__(self, lst):
        """
        Write element tree or element structure to sys.stdout.

            This function should be used for debugging only.

            *elem* is either an ElementTree, or a single Element.  The exact output
            format is implementation dependent.  In this version, it's written as an
            ordinary XML file.

    
        """
def parse(source, parser=None):
    """
    Parse XML document into element tree.

        *source* is a filename or file object containing XML data,
        *parser* is an optional parser instance defaulting to XMLParser.

        Return an ElementTree instance.

    
    """
def iterparse(source, events=None, parser=None):
    """
    Incrementally parse XML document into ElementTree.

        This class also reports what's going on to the user based on the
        *events* it is initialized with.  The supported events are the strings
        "start", "end", "start-ns" and "end-ns" (the "ns" events are used to get
        detailed namespace information).  If *events* is omitted, only
        "end" events are reported.

        *source* is a filename or file object containing XML data, *events* is
        a list of events to report back, *parser* is an optional parser instance.

        Returns an iterator providing (event, elem) pairs.

    
    """
    def iterator():
        """
         load event buffer

        """
    def IterParseIterator(collections.abc.Iterator):
    """
    read
    """
def XMLPullParser:
    """
     The _parser argument is for internal use only and must not be relied
     upon in user code. It will be removed in a future release.
     See http://bugs.python.org/issue17741 for more details.


    """
    def feed(self, data):
        """
        Feed encoded data to parser.
        """
    def _close_and_return_root(self):
        """
         iterparse needs this to set its root attribute properly :(

        """
    def close(self):
        """
        Finish feeding data to parser.

                Unlike XMLParser, does not return the root element. Use
                read_events() to consume elements from XMLPullParser.
        
        """
    def read_events(self):
        """
        Return an iterator over currently available (event, elem) pairs.

                Events are consumed from the internal event queue as they are
                retrieved from the iterator.
        
        """
def XML(text, parser=None):
    """
    Parse XML document from string constant.

        This function can be used to embed "XML Literals" in Python code.

        *text* is a string containing XML data, *parser* is an
        optional parser instance, defaulting to the standard XMLParser.

        Returns an Element instance.

    
    """
def XMLID(text, parser=None):
    """
    Parse XML document from string constant for its IDs.

        *text* is a string containing XML data, *parser* is an
        optional parser instance, defaulting to the standard XMLParser.

        Returns an (Element, dict) tuple, in which the
        dict maps element id:s to elements.

    
    """
def fromstringlist(sequence, parser=None):
    """
    Parse XML document from sequence of string fragments.

        *sequence* is a list of other sequence, *parser* is an optional parser
        instance, defaulting to the standard XMLParser.

        Returns an Element instance.

    
    """
def TreeBuilder:
    """
    Generic element structure builder.

        This builder converts a sequence of start, data, and end method
        calls to a well-formed element structure.

        You can use this class to build an element structure using a custom XML
        parser, or a parser for some other XML-like format.

        *element_factory* is an optional element factory which is called
        to create new Element instances, as necessary.

        *comment_factory* is a factory to create comments to be used instead of
        the standard factory.  If *insert_comments* is false (the default),
        comments will not be inserted into the tree.

        *pi_factory* is a factory to create processing instructions to be used
        instead of the standard factory.  If *insert_pis* is false (the default),
        processing instructions will not be inserted into the tree.
    
    """
2021-03-02 20:53:43,758 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:43,758 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, element_factory=None, *,
                 comment_factory=None, pi_factory=None,
                 insert_comments=False, insert_pis=False):
        """
         data collector
        """
    def close(self):
        """
        Flush builder buffers and return toplevel document Element.
        """
    def _flush(self):
        """

        """
    def data(self, data):
        """
        Add text to current element.
        """
    def start(self, tag, attrs):
        """
        Open new element and return it.

                *tag* is the element name, *attrs* is a dict containing element
                attributes.

        
        """
    def end(self, tag):
        """
        Close and return current Element.

                *tag* is the element name.

        
        """
    def comment(self, text):
        """
        Create a comment using the comment_factory.

                *text* is the text of the comment.
        
        """
    def pi(self, target, text=None):
        """
        Create a processing instruction using the pi_factory.

                *target* is the target name of the processing instruction.
                *text* is the data of the processing instruction, or ''.
        
        """
    def _handle_single(self, factory, insert, *args):
        """
         also see ElementTree and TreeBuilder

        """
def XMLParser:
    """
    Element structure builder for XML source data based on the expat parser.

        *target* is an optional target object which defaults to an instance of the
        standard TreeBuilder class, *encoding* is an optional encoding string
        which if given, overrides the encoding specified in the XML file:
        http://www.iana.org/assignments/character-sets

    
    """
    def __init__(self, *, target=None, encoding=None):
        """
        No module named expat; use SimpleXMLTreeBuilder instead

        """
    def _setevents(self, events_queue, events_to_report):
        """
         Internal API for XMLPullParser
         events_to_report: a list of events to report during parsing (same as
         the *events* of XMLPullParser's constructor.
         events_queue: a list of actual parsing events that will be populated
         by the underlying parser.


        """
2021-03-02 20:53:43,763 : INFO : tokenize_signature : --> do i ever get here?
                def handler(tag, attrib_in, event=event_name, append=append,
                            start=self._start):
                    """
                    end
                    """
2021-03-02 20:53:43,763 : INFO : tokenize_signature : --> do i ever get here?
                def handler(tag, event=event_name, append=append,
                            end=self._end):
                    """
                    start-ns
                    """
2021-03-02 20:53:43,764 : INFO : tokenize_signature : --> do i ever get here?
                    def handler(prefix, uri, event=event_name, append=append,
                                start_ns=self._start_ns):
                        """
                        ''
                        """
2021-03-02 20:53:43,764 : INFO : tokenize_signature : --> do i ever get here?
                    def handler(prefix, event=event_name, append=append,
                                end_ns=self._end_ns):
                        """
                        'comment'
                        """
                def handler(text, event=event_name, append=append, self=self):
                    """
                    'pi'
                    """
2021-03-02 20:53:43,765 : INFO : tokenize_signature : --> do i ever get here?
                def handler(pi_target, data, event=event_name, append=append,
                            self=self):
                    """
                    unknown event %r
                    """
    def _raiseerror(self, value):
        """
         expand qname, and convert name string to ascii, if possible

        """
    def _start_ns(self, prefix, uri):
        """
        ''
        """
    def _end_ns(self, prefix):
        """
        ''
        """
    def _start(self, tag, attr_list):
        """
         Handler for expat's StartElementHandler. Since ordered_attributes
         is set, the attributes are reported as a list of alternating
         attribute name,value.

        """
    def _end(self, tag):
        """
        &
        """
    def feed(self, data):
        """
        Feed encoded data to parser.
        """
    def close(self):
        """
        Finish feeding data to parser and return element structure.
        """
def canonicalize(xml_data=None, *, out=None, from_file=None, **options):
    """
    Convert XML to its C14N 2.0 serialised form.

        If *out* is provided, it must be a file or file-like object that receives
        the serialised canonical XML output (text, not bytes) through its ``.write()``
        method.  To write to a file, open it in text mode with encoding "utf-8".
        If *out* is not provided, this function returns the output as text string.

        Either *xml_data* (an XML string) or *from_file* (a file path or
        file-like object) must be provided as input.

        The configuration options are the same as for the ``C14NWriterTarget``.
    
    """
def C14NWriterTarget:
    """

        Canonicalization writer target for the XMLParser.

        Serialises parse events to XML C14N 2.0.

        The *write* function is used for writing out the resulting data stream
        as text (not bytes).  To write to a file, open it in text mode with encoding
        "utf-8" and pass its ``.write`` method.

        Configuration options:

        - *with_comments*: set to true to include comments
        - *strip_text*: set to true to strip whitespace before and after text content
        - *rewrite_prefixes*: set to true to replace namespace prefixes by "n{number}"
        - *qname_aware_tags*: a set of qname aware tag names in which prefixes
                              should be replaced in text content
        - *qname_aware_attrs*: a set of qname aware attribute names in which prefixes
                               should be replaced in text content
        - *exclude_attrs*: a set of attribute names that should not be serialised
        - *exclude_tags*: a set of tag names that should not be serialised
    
    """
2021-03-02 20:53:43,771 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:43,771 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:43,771 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, write, *,
                 with_comments=False, strip_text=False, rewrite_prefixes=False,
                 qname_aware_tags=None, qname_aware_attrs=None,
                 exclude_attrs=None, exclude_tags=None):
        """
         Stack with globally and newly declared namespaces as (uri, prefix) pairs.

        """
    def _iter_namespaces(self, ns_stack, _reversed=reversed):
        """
         almost no element declares new namespaces
        """
    def _resolve_prefix_name(self, prefixed_name):
        """
        ':'
        """
    def _qname(self, qname, uri=None):
        """
        '}'
        """
    def data(self, data):
        """
        ''
        """
    def start_ns(self, prefix, uri):
        """
         we may have to resolve qnames in text content

        """
    def start(self, tag, attrs):
        """
         Need to parse text first to see if it requires a prefix declaration.

        """
    def _start(self, tag, attrs, new_namespaces, qname_text=None):
        """
         Resolve prefixes in attribute and tag text.

        """
    def end(self, tag):
        """
        f'</{self._qname(tag)[0]}>'
        """
    def comment(self, text):
        """
        '\n'
        """
    def pi(self, target, data):
        """
        '\n'
        """
def _escape_cdata_c14n(text):
    """
     escape character data

    """
def _escape_attrib_c14n(text):
    """
     escape attribute value

    """
