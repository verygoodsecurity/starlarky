load("@stdlib//larky", larky="larky")
load("@vgs//http/request", VGSCIMultiDict="VGSCIMultiDict")


def VGSHttpResponse(
    body=None,
    headers=None,
    status_code=200
):

    self = larky.mutablestruct(__name__ = "VGSHttpResponse", __class__ = VGSHttpResponse)

    # body property
    def _get_body():
        return self._body

    def _set_body(body):
        if body != self._body:
            self._body = body
            # issue 16464
            # if we change data we need to remove content-length header
            # (cause it's most probably calculated for previous value)
            if self.has_header("Content-length"):
                self.remove_header("Content-length")
    self.body = larky.property(_get_body, _set_body)

    # headers property
    def _get_headers():
        return self._headers

    def _set_headers(headers):
        self._headers = VGSCIMultiDict(headers or {})
    self.headers = larky.property(_get_headers, _set_headers)

    def add_header(key, val):
        # replaces every value already stored under ``key``.
        # use ``append_header`` when duplicates must be kept (e.g. Set-Cookie).
        self.headers[key] = val
    self.add_header = add_header

    def append_header(key, val):
        """Add ``val`` under ``key`` without discarding the values already stored there."""
        self.headers.add(key, val)
    self.append_header = append_header

    def has_header(header_name):
        return header_name in self.headers
    self.has_header = has_header

    def get_header(header_name, default=None):
        """Return the first value stored under ``header_name``."""
        return self.headers.get(header_name, default)
    self.get_header = get_header

    def get_all_headers(header_name, default=None):
        """Return every value stored under ``header_name`` as a list."""
        return self.headers.getall(header_name, default if default != None else [])
    self.get_all_headers = get_all_headers

    def remove_header(header_name):
        """Remove every value stored under ``header_name``."""
        self.headers.popall(header_name, None)
    self.remove_header = remove_header

    def __init__(
        body,
        headers,
        status_code
    ):
        self._body = body
        self.headers = headers
        self.status_code = status_code
        return self

    self = __init__(body, headers, status_code)

    return self
