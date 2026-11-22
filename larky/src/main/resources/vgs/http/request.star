load("@stdlib//urllib/parse", parse="parse")
load("@stdlib//urllib/request", urllib_request="request")

load("@vgs//http/headers", _VGSCIMultiDict="VGSCIMultiDict")
load("@stdlib//http_request_native", _native_request="http_request_native")
load("@vendor//_multidict_core", "CIMultiDict")


VGSCIMultiDict = _VGSCIMultiDict


def _set_item(mapping, key, value):
    mapping[key] = value


def VGSHttpRequest(
    url,
    data=None,
    headers={},
    method=None
):
    # Request initializes the url (parsed once), data and method; the rest of this function only
    # adds what differs from urllib's Request. (This used to build the Request, then run its
    # __init__ again and re-parse the url twice; test_default_request_same_as_legacy.star checks
    # that the object is the same.)
    self = _native_request.create(urllib_request.Request)
    urllib_request._initialize(self, url, data=data, method=method)
    self.__name__ = "VGSHttpRequest"
    self.__class__ = VGSHttpRequest

    # override super
    def add_header(key, val):
        # original implementation uses key.capitalize(), however, we will not modify the keys.
        _native_request.add_header(self, "headers", key, val, False, _set_item)
    self.add_header = add_header

    # override super
    def add_unredirected_header(key, val):
        # will not be added to a redirected request
        # original implementation uses key.capitalize(), however, we will not modify the keys.
        _native_request.add_header(self, "unredirected_hdrs", key, val, False, _set_item)
    self.add_unredirected_header = add_unredirected_header

    _native_request.install_fields(self, url, headers, parse.urlsplit, VGSCIMultiDict)
    return self

