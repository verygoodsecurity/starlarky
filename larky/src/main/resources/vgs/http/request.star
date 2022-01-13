load("@stdlib//larky", larky="larky")
load("@stdlib//urllib/request", urllib_request="request")


def VGSHttpRequest(
    url,
    relative_url = None,
    data=None,
    headers={},
    method=None
):
    super = urllib_request.Request(url)

    self = super
    self.__name__="VGSHttpRequest"
    self.__class__=VGSHttpRequest

    # relative_url property
    def _get_relative_url():
        return self._relative_url
    def _set_relative_url(url):
        self._relative_url = url
    self.relative_url = larky.property(_get_relative_url, _set_relative_url)

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
        relative_url = None,
        data=None,
        headers={},
        method=None
    ):
        # call super init, with overrides
        self.__init__(url, data=data, headers=headers, method=method)
        self.relative_url = relative_url

        return self

    self = __init__(url, relative_url, data, headers, method)

    return self