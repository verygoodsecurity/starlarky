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
        self.__class__ = '_SimpleXMLTreeBuilder.TreeBuilderHelper'
        self.__builder = builder(element_factory, **options)
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
        return self.__builder.close()
    self.close = close

    def handle_data(data):
        self.__builder.data(data)
    self.handle_data = handle_data
    self.handle_cdata = handle_data

    def unknown_starttag(tag, attrs):
        attrib = {}
        for key, value in attrs.items():
            attrib[fixname(key)] = value
        self.__builder.start(fixname(tag), attrib)
    self.unknown_starttag = unknown_starttag

    def unknown_endtag(tag):
        self.__builder.end(fixname(tag))
    self.unknown_endtag = unknown_endtag
    return self


SimpleXMLTreeBuilderHelper = larky.struct(
    __name__='_SimpleXMLTreeBuilderHelper',
    TreeBuilderHelper=TreeBuilderHelper,
    fixname=fixname,
)