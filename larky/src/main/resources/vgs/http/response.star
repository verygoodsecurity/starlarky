load("@stdlib//larky", larky="larky")
load("@vgs//http/headers", "VGSCIMultiDict")

def VGSHttpResponse(
    body=None,
    headers=None,
    status_code=200
):

    """Build a response with ordered, case-insensitive headers."""
    self = larky.mutablestruct(__name__ = "VGSHttpResponse", __class__ = VGSHttpResponse)

    # body property
    def _get_body():
        return self._body

    def _set_body(body):
        if body != self._body:
            self._body = body
            # The stored length applies to the previous body (issue 16464).
            self.headers.popall("Content-Length", [])
    self.body = larky.property(_get_body, _set_body)

    def add_header(key, val):
        self.headers[key] = val
    self.add_header = add_header

    self._body = body
    headers = headers or {}

    self._headers = VGSCIMultiDict(headers)

    def _get_headers():
        return self._headers

    def _set_headers(value):
        self._headers = VGSCIMultiDict(value)
    self.headers = larky.property(_get_headers, _set_headers)

    def append_header(key, val):
        self.headers.add(key, val)
    self.append_header = append_header

    def remove_header(key):
        self.headers.popall(key, [])
    self.remove_header = remove_header

    def get_all_headers(key):
        return self.headers.getall(key, [])
    self.get_all_headers = get_all_headers

    self.status_code = status_code
    return self
