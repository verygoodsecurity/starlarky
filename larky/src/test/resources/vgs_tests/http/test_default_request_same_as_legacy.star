"""VGSHttpRequest builds the same object as its previous implementation.

_legacy_VGSHttpRequest is a verbatim copy of VGSHttpRequest before it stopped
initializing the request twice; both are built from the same inputs, then changed
in the same ways, and compared field by field.
"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//urllib/parse", parse="parse")
load("@stdlib//urllib/request", urllib_request="request")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", safe="safe")
load("@vgs//http/request", VGSHttpRequest="VGSHttpRequest", VGSCIMultiDict="VGSCIMultiDict")


def _legacy_VGSHttpRequest(
    url,
    data=None,
    headers={},
    method=None
):
    super = urllib_request.Request(url)

    self = super
    self.__name__ = "VGSHttpRequest"
    self.__class__ = _legacy_VGSHttpRequest

    # url property
    def _get_url():
        return self.full_url
    def _set_url(url):
        self.full_url = url

        parsed_url = parse.urlsplit(url)
        self.path = parsed_url.path
        self.query_string = parsed_url.query
    self.url = larky.property(_get_url, _set_url)

    # body property
    def _get_body():
        return self._data

    def _set_body(data):
        self.data = data
    self.body = larky.property(_get_body, _set_body)

    # headers property
    def _get_headers():
        return self._headers

    def _set_headers(headers):
        self._headers = VGSCIMultiDict(headers)
    self.headers = larky.property(_get_headers, _set_headers)
    # override super
    def add_header(key, val):
        # original implementation uses key.capitalize(), however, we will not modify the keys.
        self.headers[key] = val
    self.add_header = add_header

    # override super
    def add_unredirected_header(key, val):
        # will not be added to a redirected request
        # original implementation uses key.capitalize(), however, we will not modify the keys.
        self.unredirected_hdrs[key] = val
    self.add_unredirected_header = add_unredirected_header

    def __init__(
        url,
        data=None,
        headers={},
        method=None
    ):
        # We want the "base class" to initialize headers, then after
        # it takes care of all the initialization, we then, overwrite
        # the headers property to make it into a Case Insensitive "MultiDict"
        self.__init__(url, data=data, headers={}, method=method)
        self.headers = VGSCIMultiDict(headers)
        self.url = url
        parsed_url = parse.urlsplit(url)
        self.path = parsed_url.path
        self.query_string = parsed_url.query
        if method:
            self.method = method

        return self

    self = __init__(url, data, headers, method)

    return self



URLS = [
    "http://netloc/path;parameters?query=argument&k1=v1&k2=v2#fragment",
    "https://u:p@example.com:8443/a/b?x=1&y=2#frag",
    "https://example.com",
    "https://example.com/",
    "https://example.com/pay?a=1&a=2",
    "http://ex%41mple.com/%7Epath?q=%20#f%20",
    "https://example.com#only-fragment",
    "ftp://files.example.com/pub/file.txt",
    "http://",
]
DATA = [None, b"", b"hello", builtins.bytes("request body")]
HEADERS = [
    {},
    {"Content-Type": "application/json", "X-A": "1"},
    {"content-length": "5", "Content-Length": "6"},
    [("X-Dup", "1"), ("x-dup", "2"), ("Accept", "*/*")],
    VGSCIMultiDict([("A", "1"), ("a", "2")]),
]
METHODS = [None, "", "GET", "POST"]


def _snapshot(r):
    """Every field of r: values, or the type for functions (closures differ by identity)."""
    out = []
    d = r.__dict__
    for k in sorted(d.keys()):
        v = d[k]
        if k == "__class__":
            out.append((k, "class"))
        elif type(v) == "function":
            out.append((k, "function"))
        elif type(v) == "VGSCIMultiDict":
            out.append((k, type(v), list(v.items()), str(v)))
        else:
            out.append((k, type(v), repr(v)))
    return out


def _attempt(f, *args):
    """f(*args), or the error it raised, as a comparable value."""
    result = safe(f)(*args)
    if result.is_err:
        return ("error", "%s" % result._val)
    return ("ok", result.unwrap())


def _mutate(r):
    """Changes r through its public API and returns what the API reported along the way."""
    seen = [r.get_method(), r.get_full_url(), r.has_proxy(), r.has_header("X-A"),
            r.get_header("x-a", "none"), _attempt(r.header_items), str(r.headers)]
    r.url = "https://other.example.org:444/new/path?z=9#g"
    seen.append((r.full_url, r.host, r.path, r.query_string, r.selector, r.fragment))
    r.body = b"changed"
    seen.append((r.data, r.body))
    r.headers = {"Content-Length": "7", "Y": "2"}
    r.add_header("Content-Length", "8")
    r.add_header("z-header", "3")
    seen.append((r.has_header("content-length"), r.get_header("Content-Length"), _attempt(r.header_items)))
    r.body = b"changed again"
    seen.append((r.has_header("Content-Length"), _attempt(r.header_items)))
    r.remove_header("Y")
    r.add_unredirected_header("U", "1")
    r.set_proxy("proxy.example.com:3128", "http")
    seen.append((r.host, r.type, r.selector, _attempt(r.header_items), r.unredirected_hdrs))
    return seen


def _test_same_object_as_legacy():
    n = 0
    for url in URLS:
        for data in DATA:
            for headers in HEADERS:
                for method in METHODS:
                    new = VGSHttpRequest(url, data=data, headers=headers, method=method)
                    old = _legacy_VGSHttpRequest(url, data=data, headers=headers, method=method)
                    case = (url, data, headers, method)
                    asserts.assert_that((case, _snapshot(new))).is_equal_to((case, _snapshot(old)))
                    asserts.assert_that((case, new.__name__, new.__class__ == VGSHttpRequest)).is_equal_to((case, "VGSHttpRequest", True))
                    asserts.assert_that((case, _mutate(new))).is_equal_to((case, _mutate(old)))
                    asserts.assert_that((case, _snapshot(new))).is_equal_to((case, _snapshot(old)))
                    n += 1
    asserts.assert_that(n).is_equal_to(len(URLS) * len(DATA) * len(HEADERS) * len(METHODS))


def _test_keyword_and_positional_arguments():
    for args in [("https://example.com/a",), ("https://example.com/a", b"d"), ("https://example.com/a", b"d", {"H": "1"}, "PUT")]:
        asserts.assert_that(_snapshot(VGSHttpRequest(*args))).is_equal_to(_snapshot(_legacy_VGSHttpRequest(*args)))


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_same_object_as_legacy))
    _suite.addTest(unittest.FunctionTestCase(_test_keyword_and_positional_arguments))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
