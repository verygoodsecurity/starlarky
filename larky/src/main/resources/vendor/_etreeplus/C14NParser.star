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


def C14nCanonicalizer(builder, element_factory=None, parser=None, **options):
    if not builder:
        fail("Cannot instantiate builder. Have you tried passing in " +
             "ElementTree.TreeBuilder?")

    def __init__(element_factory, builder, parser, **options):
        if not parser:
            parser = xmllib.XMLParser()
        self = parser
        self.__class__ = 'C14NParser.C14nCanonicalizer'
        self.__builder = builder(element_factory, **options)
        return self
    self = __init__(element_factory, builder, parser, **options)

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



C14NParser = larky.struct(
    __name__='C14NParser',
    C14nCanonicalizer=C14nCanonicalizer
)