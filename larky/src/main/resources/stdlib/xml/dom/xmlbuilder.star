"""Implementation of the DOM Level 3 'LS-Load' feature."""
load("@stdlib//enum", enum="enum")
load("@stdlib//larky", larky="larky")
load("@stdlib//xml/dom", dom="dom")
load("@stdlib//xml/dom/NodeFilter", NodeFilter="NodeFilter")
load("@vendor//option/result", Error="Error")

__all__ = ["DOMBuilder", "DOMEntityResolver", "DOMInputSource"]


def Options():
    """Features object that has variables set for each DOMBuilder feature.

    The DOMBuilder class uses an instance of this class to pass settings to
    the ExpatBuilder class.
    """

    # Note that the DOMBuilder class in LoadSave constrains which of these
    # values can be set using the DOM Level 3 LoadSave feature.
    return larky.mutablestruct(
        __name__='Options',
        __class__=Options,
        namespaces=1,
        namespace_declarations=True,
        validation=False,
        external_parameter_entities=True,
        external_general_entities=True,
        external_dtd_subset=True,
        validate_if_schema=False,
        validate=False,
        datatype_normalization=False,
        create_entity_ref_nodes=True,
        entities=True,
        whitespace_in_element_content=True,
        cdata_sections=True,
        comments=True,
        charset_overrides_xml_encoding=True,
        infoset=False,
        supported_mediatypes_only=False,
        errorHandler=None,
        filter=None,
    )


def _name_xform(name):
    return name.lower().replace("-", "_")


def DOMBuilder():
    self = larky.mutablestruct(__name__='DOMBuilder', __class__=DOMBuilder)

    self.entityResolver = None
    self.errorHandler = None
    self.filter = None

    self.ACTION_REPLACE = 1
    self.ACTION_APPEND_AS_CHILDREN = 2
    self.ACTION_INSERT_AFTER = 3
    self.ACTION_INSERT_BEFORE = 4

    self._legal_actions = (
        self.ACTION_REPLACE,
        self.ACTION_APPEND_AS_CHILDREN,
        self.ACTION_INSERT_AFTER,
        self.ACTION_INSERT_BEFORE,
    )

    def __init__():
        self._options = Options()
        return self
    self = __init__()

    def _get_entityResolver():
        return self.entityResolver
    self._get_entityResolver = _get_entityResolver

    def _set_entityResolver(entityResolver):
        self.entityResolver = entityResolver
    self._set_entityResolver = _set_entityResolver

    def _get_errorHandler():
        return self.errorHandler
    self._get_errorHandler = _get_errorHandler

    def _set_errorHandler(errorHandler):
        self.errorHandler = errorHandler
    self._set_errorHandler = _set_errorHandler

    def _get_filter():
        return self.filter
    self._get_filter = _get_filter

    def _set_filter(filter):
        self.filter = filter
    self._set_filter = _set_filter

    def setFeature(name, state):
        if self.supportsFeature(name):
            state = state and 1 or 0
            key = (_name_xform(name), state)
            settings = self._settings.get(key, None)
            if not settings:
                dom.NotSupportedErr("unsupported feature: %r" % (name,))
            for name, value in settings:
                setattr(self._options, name, value)
        else:
            dom.NotFoundErr("unknown feature: " + repr(name))
    self.setFeature = setFeature

    def supportsFeature(name):
        return hasattr(self._options, _name_xform(name))
    self.supportsFeature = supportsFeature

    def canSetFeature(name, state):
        key = (_name_xform(name), state and 1 or 0)
        return key in self._settings
    self.canSetFeature = canSetFeature

    # This dictionary maps from (feature,value) to a list of
    # (option,value) pairs that should be set on the Options object.
    # If a (feature,value) setting is not in this dictionary, it is
    # not supported by the DOMBuilder.
    #
    _settings = {
        ("namespace_declarations", 0): [("namespace_declarations", 0)],
        ("namespace_declarations", 1): [("namespace_declarations", 1)],
        ("validation", 0): [("validation", 0)],
        ("external_general_entities", 0): [("external_general_entities", 0)],
        ("external_general_entities", 1): [("external_general_entities", 1)],
        ("external_parameter_entities", 0): [("external_parameter_entities", 0)],
        ("external_parameter_entities", 1): [("external_parameter_entities", 1)],
        ("validate_if_schema", 0): [("validate_if_schema", 0)],
        ("create_entity_ref_nodes", 0): [("create_entity_ref_nodes", 0)],
        ("create_entity_ref_nodes", 1): [("create_entity_ref_nodes", 1)],
        ("entities", 0): [("create_entity_ref_nodes", 0), ("entities", 0)],
        ("entities", 1): [("entities", 1)],
        ("whitespace_in_element_content", 0): [("whitespace_in_element_content", 0)],
        ("whitespace_in_element_content", 1): [("whitespace_in_element_content", 1)],
        ("cdata_sections", 0): [("cdata_sections", 0)],
        ("cdata_sections", 1): [("cdata_sections", 1)],
        ("comments", 0): [("comments", 0)],
        ("comments", 1): [("comments", 1)],
        ("charset_overrides_xml_encoding", 0): [("charset_overrides_xml_encoding", 0)],
        ("charset_overrides_xml_encoding", 1): [("charset_overrides_xml_encoding", 1)],
        ("infoset", 0): [],
        ("infoset", 1): [
            ("namespace_declarations", 0),
            ("validate_if_schema", 0),
            ("create_entity_ref_nodes", 0),
            ("entities", 0),
            ("cdata_sections", 0),
            ("datatype_normalization", 1),
            ("whitespace_in_element_content", 1),
            ("comments", 1),
            ("charset_overrides_xml_encoding", 1),
        ],
        ("supported_mediatypes_only", 0): [("supported_mediatypes_only", 0)],
        ("namespaces", 0): [("namespaces", 0)],
        ("namespaces", 1): [("namespaces", 1)],
    }

    def getFeature(name):
        xname = _name_xform(name)
        res = getattr(self._options, xname, larky.SENTINEL)
        if res != larky.SENTINEL:
            return res

        if name == "infoset":
            options = self._options
            return (
                options.datatype_normalization
                and options.whitespace_in_element_content
                and options.comments
                and options.charset_overrides_xml_encoding
                and not (
                    options.namespace_declarations
                    or options.validate_if_schema
                    or options.create_entity_ref_nodes
                    or options.entities
                    or options.cdata_sections
                )
            )
        dom.NotFoundErr("feature %s not known" % repr(name))
    self.getFeature = getFeature

    def parseURI(uri):
        if self.entityResolver:
            input = self.entityResolver.resolveEntity(None, uri)
        else:
            input = DOMEntityResolver().resolveEntity(None, uri)
        return self.parse(input)
    self.parseURI = parseURI

    def parse(input):
        fail("Parse is not supported in Larky")
        # options = dict(**self._options.__dict__)
        # options.filter = self.filter
        # options.errorHandler = self.errorHandler
        # fp = input.byteStream
        # if fp == None and options.systemId:
        #     load("@stdlib//urllib", request="request")
        #
        #     fp = urllib.request.urlopen(input.systemId)
        # return self._parse_bytestream(fp, options)
    self.parse = parse

    def parseWithContext(input, cnode, action):
        if action not in self._legal_actions:
            fail("ValueError: not a legal action")
        fail("NotImplementedError: Haven't written this yet...")
    self.parseWithContext = parseWithContext

    def _parse_bytestream(stream, options):
        fail("_parse_bytestream is not supported in Larky")
        #
        # load("@vendor//expatbuilder", expatbuilder="expatbuilder")
        #
        # builder = expatbuilder.makeBuilder(options)
        # return builder.parseFile(stream)
    self._parse_bytestream = _parse_bytestream
    return self


def DOMEntityResolver():
    fail("Larky does not support DOMEntityResolver")
    #
    # def resolveEntity(publicId, systemId):
    #     if not (systemId != None):
    #         fail("assert systemId != None failed!")
    #     source = DOMInputSource()
    #     source.publicId = publicId
    #     source.systemId = systemId
    #     source.byteStream = self._get_opener().open(systemId)
    #
    #     # determine the encoding if the transport provided it
    #     source.encoding = self._guess_media_encoding(source)
    #
    #     # determine the base URI is we can
    #     load("@stdlib//posixpath", posixpath="posixpath", parse="parse")
    #
    #     parts = urllib.parse.urlparse(systemId)
    #     scheme, netloc, path, params, query, fragment = parts
    #     # XXX should we check the scheme here as well?
    #     if path and not path.endswith("/"):
    #         path = posixpath.dirname(path) + "/"
    #         parts = scheme, netloc, path, params, query, fragment
    #         source.baseURI = urllib.parse.urlunparse(parts)
    #
    #     return source
    # self.resolveEntity = resolveEntity
    #
    # def _get_opener():
    #     try:
    #         return self._opener
    #     except AttributeError:
    #         self._opener = self._create_opener()
    #         return self._opener
    # self._get_opener = _get_opener
    #
    # def _create_opener():
    #     load("@stdlib//urllib", request="request")
    #
    #     return urllib.request.build_opener()
    # self._create_opener = _create_opener
    #
    # def _guess_media_encoding(source):
    #     info = source.byteStream.info()
    #     if "Content-Type" in info:
    #         for param in info.getplist():
    #             if param.startswith("charset="):
    #                 return param.split("=", 1)[1].lower()
    # self._guess_media_encoding = _guess_media_encoding
    # return self


def DOMInputSource():

    self = larky.mutablestruct(__name__='DOMInputSource', __class__=DOMInputSource)

    def __init__():
        self.byteStream = None
        self.characterStream = None
        self.stringData = None
        self.encoding = None
        self.publicId = None
        self.systemId = None
        self.baseURI = None
        return self
    self = __init__()

    def _get_byteStream():
        return self.byteStream
    self._get_byteStream = _get_byteStream

    def _set_byteStream(byteStream):
        self.byteStream = byteStream
    self._set_byteStream = _set_byteStream

    def _get_characterStream():
        return self.characterStream
    self._get_characterStream = _get_characterStream

    def _set_characterStream(characterStream):
        self.characterStream = characterStream
    self._set_characterStream = _set_characterStream

    def _get_stringData():
        return self.stringData
    self._get_stringData = _get_stringData

    def _set_stringData(data):
        self.stringData = data
    self._set_stringData = _set_stringData

    def _get_encoding():
        return self.encoding
    self._get_encoding = _get_encoding

    def _set_encoding(encoding):
        self.encoding = encoding
    self._set_encoding = _set_encoding

    def _get_publicId():
        return self.publicId
    self._get_publicId = _get_publicId

    def _set_publicId(publicId):
        self.publicId = publicId
    self._set_publicId = _set_publicId

    def _get_systemId():
        return self.systemId
    self._get_systemId = _get_systemId

    def _set_systemId(systemId):
        self.systemId = systemId
    self._set_systemId = _set_systemId

    def _get_baseURI():
        return self.baseURI
    self._get_baseURI = _get_baseURI

    def _set_baseURI(uri):
        self.baseURI = uri
    self._set_baseURI = _set_baseURI
    return self


def DOMBuilderFilter():
    """Element filter which can be used to tailor construction of
    a DOM instance.
    """

    # There's really no need for this class; concrete implementations
    # should just implement the endElement() and startElement()
    # methods as appropriate.  Using this makes it easy to only
    # implement one of them.
    self = larky.mutablestruct(__name__='DOMBuilderFilter',
                               __class__=DOMBuilderFilter)
    self.FILTER_ACCEPT = 1
    self.FILTER_REJECT = 2
    self.FILTER_SKIP = 3
    self.FILTER_INTERRUPT = 4

    self.whatToShow = NodeFilter.SHOW_ALL

    def _get_whatToShow():
        return self.whatToShow
    self._get_whatToShow = _get_whatToShow

    def acceptNode(element):
        return self.FILTER_ACCEPT
    self.acceptNode = acceptNode

    def startContainer(element):
        return self.FILTER_ACCEPT
    self.startContainer = startContainer
    return self


def DocumentLS():
    """Mixin to create documents that conform to the load/save spec."""
    self = larky.mutablestruct(__name__='DocumentLS', __class__=DocumentLS)
    self.async_ = False

    def _get_async():
        return False
    self._get_async = _get_async

    def _set_async(flag):
        if flag:
            dom.NotSupportedErr(
                "asynchronous document loading is not supported"
            )
    self._set_async = _set_async

    def abort():
        # What does it mean to "clear" a document?  Does the
        # documentElement disappear?
        fail("NotImplementedError: haven't figured out what this means yet")
    self.abort = abort

    def _load(uri):
        fail("NotImplementedError: haven't written this yet")
    self._load = _load

    def loadXML(source):
        fail("NotImplementedError: haven't written this yet")
    self.loadXML = loadXML

    def saveXML(snode):
        if snode == None:
            snode = self
        elif snode.ownerDocument != self:
            dom.WrongDocumentErr()
        return snode.toxml()
    self.saveXML = saveXML
    return self


def DOMImplementationLS():
    self = larky.mutablestruct(__name__='DOMImplementationLS',
                               __class__=DOMImplementationLS)
    self.MODE_SYNCHRONOUS = 1
    self.MODE_ASYNCHRONOUS = 2

    def createDOMBuilder(mode, schemaType):
        if schemaType != None:
            dom.NotSupportedErr("schemaType not yet supported")
        if mode == self.MODE_SYNCHRONOUS:
            return DOMBuilder()
        if mode == self.MODE_ASYNCHRONOUS:
            dom.NotSupportedErr("asynchronous builders are not supported")
        fail("ValueError: unknown value for mode")
    self.createDOMBuilder = createDOMBuilder

    def createDOMWriter():
        fail("NotImplementedError: the writer interface hasn't been written yet!")
    self.createDOMWriter = createDOMWriter

    def createDOMInputSource():
        return DOMInputSource()
    self.createDOMInputSource = createDOMInputSource
    return self

