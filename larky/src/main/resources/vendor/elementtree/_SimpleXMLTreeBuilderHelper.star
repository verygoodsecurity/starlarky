# This file is here to avoid circular imports
#
#
##
# Tools to build element trees from XML files, using <b>xmllib</b>.
# This module can be used instead of the standard tree builder, for
# Python versions where "expat" is not available (such as 1.5.2).
# <p>
# Note that due to bugs in <b>xmllib</b>, the namespace support is
# not reliable (you can run the module as a script to find out exactly
# how unreliable it is on your Python version).
##
load("@stdlib//larky", larky="larky")
load("@stdlib//xmllib", xmllib="xmllib")
load("@stdlib//string", string="string")
load("@vendor//option/result", Ok="Ok")


def fixname(name, split=None):
    # xmllib in 2.0 and later provides limited (and slightly broken)
    # support for XML namespaces.
    if " " not in name:
        return name
    if not split:
        split = name.split
    return "{%s}%s" % tuple(split(" ", 1))


def __fake_append(*_args):
    pass


__fakequeue = larky.struct(append=__fake_append)

##
# ElementTree builder for XML source data.
#
# @see elementtree.ElementTree

# builder is equivalent to "target"
def TreeBuilderHelper(builder, element_factory=None, parser=None, capture_event_queue=False, **options):
    """
    Use like follows::

        self = SimpleXMLTreeBuilderHelper.TreeBuilderHelper(
            ElementTree.TreeBuilder,
            element_factory=element_factory,
            parser=xmllib.XMLParser()
        )
        self.__class__ = 'SimpleXMLTreeBuilder.TreeBuilder'
    """
    if not builder:
        fail("Cannot instantiate builder. Have you tried passing in " +
             "ElementTree.TreeBuilder?")

    def __init__(builder, element_factory, parser, **options):
        if not parser:
            parser = xmllib.XMLParser()
        self = parser
        self.__name__ = '_SimpleXMLTreeBuilder.TreeBuilderHelper'
        self.__class__ = TreeBuilderHelper
        self.__target_cls = builder
        self.__element_factory = element_factory
        self.__options = options
        self._events_queue = [] if capture_event_queue else __fakequeue
        self.target = self.__target_cls(self.__element_factory, **self.__options)
        return self
    self = __init__(builder, element_factory, parser, **options)

    ##
    # Feeds data to the parser.
    #
    # @param data Encoded data.
    XMLParser_feed = self.feed
    def feed(data):
        XMLParser_feed(data)
    self.feed = feed

    ##
    # Finishes feeding data to the parser.
    #
    # @return An element structure.
    # @defreturn Element
    XMLParser_close = self.close
    def close():
        XMLParser_close()
        root = self.target.close()
        self.target = None
        return root
    self.close = close

    def _builder():
        if not self.target:
            self.target = self.__target_cls(self.__element_factory, **self.__options)
        return self.target
    self._builder = _builder

    def handle_data(data):
        # if self._builder()._last:
        #     print("current tag:", self._builder()._last.nodetype(), "data:", repr(data))
        self._builder().data(data)
    self.handle_data = handle_data

    def handle_cdata(data):
        self._builder().data(data)
    self.handle_cdata = handle_cdata

    def insert_comment_initial(data):
        # assert parent is None or parent is self.document
        # assert self.document._elementTree is None
        self._initial_comments.append(data)
    self.insert_comment_initial = insert_comment_initial

    def insertCommentMain(data, parent=None):
        # if (parent == self.document and
        #         self.document._elementTree.getroot()[-1].tag == comment_type):
        #     warnings.warn("lxml cannot represent adjacent comments beyond the root elements", DataLossWarning)
        # super(TreeBuilder, self).insertComment(data, parent)
        pass

    def handle_comment(data):
        r = self._builder().comment(data)
        self._events_queue.append(("comment", r.tag))
    self.handle_comment = handle_comment

    # Example -- handle processing instructions, could be overridden
    def handle_proc(name, data):
        r = self._builder().pi(name, text=data)
        self._events_queue.append(("pi", r.tag))
    self.handle_proc = handle_proc

    # Overridden -- handle XML
    XMLParser_handle_xml = self.handle_xml

    def handle_xml(encoding, standalone):
        self.document(encoding=encoding, standalone=standalone)
        # if not self._builder()._root:
        #     self._builder()._root = self._document
        #     self._builder()._elem.append(self._document)
        #     self._pop_elem_tag = True
    self.handle_xml = handle_xml

    def doctype(name, pubid, system, data):
        """Handle doctype declaration

        - *name* is the Doctype name
        - *pubid* is the public identifier,
        - *system* is the system identifier.
        - *data* is internal/external dtd
        """
        doctype_factory = self.__options.pop('doctype_factory', None)
        if doctype_factory:
            _doctype = doctype_factory(name, pubid, system, data)
            if not self._document:
                self.document(doctype=_doctype)
            else:
                self._document.set_doctype(_doctype)
            # if hasattr(self._builder(), '_handle_single'):
            #     self.__doctype = self._builder()._handle_single(
            #         doctype_factory, True, name, pubid, system, data
            #     )
    self.doctype = doctype

    def document(encoding=None, standalone=None, version=None, doctype=None):
        # print("document() repr", repr(self.__element_factory))
        document_factory = self.__options.pop('document_factory', None)
        if document_factory:
            if not doctype:
                doctype = self._doctype
            self._document = document_factory(
                encoding=encoding,
                standalone=standalone,
                version=version,
                doctype=doctype
            )
            # if hasattr(self._builder(), '_handle_single'):
            #     self.__doctype = self._builder()._handle_single(
            #         doctype_factory, True, name, pubid, system, data
            #     )
            return self._document
    self.document = document

    # Overridden -- handle DOCTYPE
    def handle_doctype(tag, pubid, syslit, data):
        # print("tag:", tag, "pubid:", pubid, "syslit:", syslit, "data:", data)
        if hasattr(self._builder(), "doctype"):
            # "html", "-//W3C//DTD HTML 4.01//EN", "sys.dtd")),
            self._builder().doctype(tag, pubid, syslit, data=data)
        else:
            self.doctype(tag, pubid, syslit, data=data)
        r = (tag, pubid, syslit)
        self._events_queue.append(
            ("doctype", r)
        )
    self.handle_doctype = handle_doctype

    # Overridable -- handle start tag
    XMLParser_handle_starttag = self.handle_starttag

    def handle_starttag(tag, method, attrs):
        # method(attrs)
        r = XMLParser_handle_starttag(tag, method, attrs)
        self._events_queue.append(("start", r.tag))
    self.handle_starttag = handle_starttag

    # Overridable -- handle end tag
    XMLParser_handle_endtag = self.handle_endtag

    def handle_endtag(tag, method):
        r = XMLParser_handle_endtag(tag, method)
        self._events_queue.append(("end", r.tag))
    self.handle_endtag = handle_endtag

    # Overridable -- start-ns
    XMLParser_handle_startns = self.handle_startns

    def handle_startns(prefix, qualified, href):
        if hasattr(self._builder(), "start_ns"):
            self._builder().start_ns(prefix, href)
        r = (prefix or '', href or '')
        self._events_queue.append(
            ("start-ns", r)
        )
        # print("start-ns - name:", prefix, "qname:", qualified, "href:", href)
    self.handle_startns = handle_startns

    # Overridable -- end-ns
    XMLParser_handle_endns = self.handle_endns

    def handle_endns(prefix):
        if hasattr(self._builder(), "end_ns"):
            self._builder().end_ns(prefix)

        self._events_queue.append(
            ("end-ns", None)
        )
        # for k in ns_map.keys():
        #     print("end-ns - name:", k or None)
    self.handle_endns = handle_endns

    # Example -- handle character reference, no need to override
    XMLParser_handle_charref = self.handle_charref

    def handle_charref(name):
        XMLParser_handle_charref(name)
    self.handle_charref = handle_charref

    # Definition of entities -- derived classes may override
    XMLParser_entitydef = self.entitydefs
    self.entitydefs = XMLParser_entitydef

    # unknown fallbacks.
    def unknown_starttag(tag, attrs):
        attrib = {}
        for key, value in attrs.items():
            attrib[fixname(key)] = value
        r = self._builder().start(fixname(tag), attrib)
        self._events_queue.append(
            ("start", r.tag)
        )
    self.unknown_starttag = unknown_starttag

    def unknown_endtag(tag):
        r = self._builder().end(fixname(tag))
        self._events_queue.append(
            ("end", r.tag)
        )
    self.unknown_endtag = unknown_endtag

    def _read(i):
        if i >= len(self._events_queue):
            self._events_queue.clear()
            return StopIteration()
        return Ok(self._events_queue[i])

    def read_events():
        if self._events_queue == __fakequeue:
            return []
        return larky.DeterministicGenerator(_read)

    self.read_events = read_events
    # def unknown_charref(ref):
    #     pass
    # self.unknown_charref = unknown_charref
    #
    # def unknown_entityref(name):
    #     self.syntax_error("reference to unknown entity `&%s;'" % name)
    # self.unknown_entityref = unknown_entityref
    return self


SimpleXMLTreeBuilderHelper = larky.struct(
    __name__='_SimpleXMLTreeBuilderHelper',
    TreeBuilderHelper=TreeBuilderHelper,
    fixname=fixname,
)