load("@stdlib//larky", larky="larky")
load("@stdlib//urllib/parse", parse="parse")
load("@stdlib//urllib/request", urllib_request="request")

load("@vendor//multidict", CIMultiDict="CIMultiDict")


def VGSCIMultiDict(*args, **kwargs):
    """VGS specific CI MultiDict with the support for duplicate case-insensitive keys."""
    self = CIMultiDict(*args, **kwargs)
    self.__name__ = 'VGSCIMultiDict'
    self.__class__ = VGSCIMultiDict

    def __str__():
        body = ", ".join(["\"{}\": {}".format(k, repr(v)) for k, v in iter(self.items())])
        return "{{{}}}".format(body)
    self.__str__= __str__
    return self


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

