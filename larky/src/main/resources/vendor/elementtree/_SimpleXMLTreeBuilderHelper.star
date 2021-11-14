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


def fixname(name, split=None):
    # xmllib in 2.0 and later provides limited (and slightly broken)
    # support for XML namespaces.
    if " " not in name:
        return name
    if not split:
        split = name.split
    return "{%s}%s" % tuple(split(" ", 1))

##
# ElementTree builder for XML source data.
#
# @see elementtree.ElementTree


def TreeBuilderHelper(builder, element_factory=None, parser=None, **options):
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
        self.__builder_cls = builder
        self.__element_factory = element_factory
        self.__options = options
        self.__builder = self.__builder_cls(self.__element_factory, **self.__options)
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
        root = self.__builder.close()
        self.__builder = None
        return root
    self.close = close

    def _builder():
        if not self.__builder:
            self.__builder = self.__builder_cls(self.__element_factory, **self.__options)
        return self.__builder
    self._builder = _builder

    def handle_data(data):
        self._builder().data(data)
    self.handle_data = handle_data

    def handle_cdata(data):
        self._builder().data(data)
    self.handle_cdata = handle_cdata

    def handle_comment(data):
        self._builder().comment(data)
    self.handle_comment = handle_comment

    # Example -- handle processing instructions, could be overridden
    def handle_proc(name, data):
       self._builder().pi(name, text=data)
    self.handle_proc = handle_proc

    # Overridden -- handle XML
    XMLParser_handle_xml = self.handle_xml

    def handle_xml(encoding, standalone):
        return XMLParser_handle_xml(encoding, standalone)
    self.handle_xml = handle_xml

    # Overridden -- handle DOCTYPE
    XMLParser_handle_doctype = self.handle_doctype

    def handle_doctype(tag, pubid, syslit, data):
        return XMLParser_handle_doctype(tag, pubid, syslit, data)
    self.handle_doctype = handle_doctype

    # Overridable -- handle start tag
    XMLParser_handle_starttag = self.handle_starttag
    def handle_starttag(tag, method, attrs):
        # method(attrs)
        XMLParser_handle_starttag(tag, method, attrs)
    self.handle_starttag = handle_starttag

    # Overridable -- handle end tag
    XMLParser_handle_endtag = self.handle_endtag
    def handle_endtag(tag, method):
        XMLParser_handle_endtag(tag, method)
    self.handle_endtag = handle_endtag

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
        self._builder().start(fixname(tag), attrib)
    self.unknown_starttag = unknown_starttag

    def unknown_endtag(tag):
        self._builder().end(fixname(tag))
    self.unknown_endtag = unknown_endtag

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