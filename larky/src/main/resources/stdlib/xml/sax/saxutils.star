load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")


def __dict_replace(s, d):
    """Replace substrings of a string using a dictionary."""
    for key, value in d.items():
        s = s.replace(key, value)
    return s


def escape(data, entities=None):
    """Escape &, <, and > in a string of data.
    You can escape other strings of data by passing a dictionary as
    the optional entities parameter.  The keys and values must all be
    strings; each key will be replaced with its corresponding value.
    """

    if entities == None:
        entities = {}
    # must do ampersand first
    data = data.replace("&", "&amp;")
    data = data.replace(">", "&gt;")
    data = data.replace("<", "&lt;")
    if entities:
        data = __dict_replace(data, entities)
    return data


def unescape(data, entities=None):
    """Unescape &amp;, &lt;, and &gt; in a string of data.
    You can unescape other strings of data by passing a dictionary as
    the optional entities parameter.  The keys and values must all be
    strings; each key will be replaced with its corresponding value.
    """
    if entities == None:
        entities = {}

    data = data.replace("&lt;", "<")
    data = data.replace("&gt;", ">")
    if entities:
        data = __dict_replace(data, entities)
    # must do ampersand last
    return data.replace("&amp;", "&")


def quoteattr(data, entities=None):
    """Escape and quote an attribute value.
    Escape &, <, and > in a string of data, then quote it for use as
    an attribute value.  The \" character will be escaped as well, if
    necessary.
    You can escape other strings of data by passing a dictionary as
    the optional entities parameter.  The keys and values must all be
    strings; each key will be replaced with its corresponding value.
    """
    if entities == None:
        entities = {}
    entities = entities.update({
        '\n': '&#10;',
        '\r': '&#13;',
        '\t': '&#9;'
    })
    data = escape(data, entities)
    if '"' in data:
        if "'" in data:
            data = '"%s"' % data.replace('"', "&quot;")
        else:
            data = "'%s'" % data
    else:
        data = '"%s"' % data
    return data


def XMLGenerator(out=None, encoding="iso-8859-1", short_empty_elements=False):
    fail("TODO: not implemented yet")


def XMLFilterBase(parent = None):
    """This class is designed to sit between an XMLReader and the
    client application's event handlers.  By default, it does nothing
    but pass requests up to the reader and events on to the handlers
    unmodified, but subclasses can override specific methods to modify
    the event stream or the configuration requests as they pass
    through."""
    fail("TODO: not implemented yet")

# --- Utility functions

def prepare_input_source(source, input_source_factory, base=""):
    """This function takes an InputSource and an optional base URL and
    returns a fully resolved InputSource object ready for reading."""

    # if isinstance(source, os.PathLike):
    #     source = os.fspath(source)
    if not input_source_factory:
        fail("Missing input source factory")

    if types.is_string(source):
        source = input_source_factory(source)
    elif hasattr(source, "read"):
        f = source
        source = input_source_factory()
        if types.is_string(f.read(0)):
            source.setCharacterStream(f)
        else:
            source.setByteStream(f)
        if hasattr(f, "name") and types.is_string(f.name):
            source.setSystemId(f.name)

    if source.getCharacterStream() == None and source.getByteStream() == None:
        fail("unsupported in larky. you must pass a file-like object to parse_input_source")
        # sysid = source.getSystemId()
        # basehead = os.path.dirname(os.path.normpath(base))
        # sysidfilename = os.path.join(basehead, sysid)
        # if os.path.isfile(sysidfilename):
        #     source.setSystemId(sysidfilename)
        #     f = open(sysidfilename, "rb")
        # else:
        #     source.setSystemId(urllib.parse.urljoin(base, sysid))
        #     f = urllib.request.urlopen(source.getSystemId())
        #
        # source.setByteStream(f)

    return source


# ===========================================================================
#
# DEPRECATED SAX 1.0 CLASSES
#
# ===========================================================================

# --- AttributeMap


def AttributeMap(map):
    """An implementation of AttributeList that takes an (attr,val) hash
    and uses it to implement the AttributeList interface."""
    self = larky.mutablestruct(__name__='AttributeMap', __class__=AttributeMap)

    def __init__(map):
        self.map = map
        return self
    self = __init__(map)

    def getLength():
        return len(list(self.map.keys()))
    self.getLength = getLength

    def getName(i):
        keys = list(self.map.keys())
        return keys[i] if i < len(keys) else None
    self.getName = getName

    def getType(i):
        return "CDATA"
    self.getType = getType

    def getValue(i):
        n = self.getName(i) if types.is_int(i) else i
        return self.map.get(n, None)
    self.getValue = getValue

    def __len__():
        return len(self.map)
    self.__len__ = __len__

    def __getitem__(key):
        return getName(key) if types.is_int(key) else getValue(key)
    self.__getitem__ = __getitem__

    def items():
        return list(self.map.items())
    self.items = items

    def keys():
        return list(self.map.keys())
    self.keys = keys

    def has_key(key):
        return key in self.map
    self.has_key = has_key

    def get(key, alternative=None):
        return self.map.get(key, alternative)
    self.get = get

    def copy():
        return AttributeMap(self.map.copy())
    self.copy = copy

    def values():
        return list(self.map.values())
    self.values = values
    return self



saxutils = larky.struct(
    __name__='saxutils',
    prepare_input_source=prepare_input_source,
    AttributeMap=AttributeMap,
)