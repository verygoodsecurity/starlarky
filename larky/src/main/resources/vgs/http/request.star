load("@stdlib//larky", larky="larky")
load("@stdlib//urllib/parse", "parse")
load("@stdlib//urllib/request", urllib_request="request")

def VGSHttpRequest(
    url,
    data=None,
    headers={},
    method=None
):
    super = urllib_request.Request(url)

    self = super
    self.__name__ = "VGSHttpRequest"
    self.__class__ = VGSHttpRequest

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
        # call super init, with overrides
        self.__init__(url, data=data, headers=headers, method=method)
        self.url = url

        return self

    self = __init__(url, data, headers, method)

    return self