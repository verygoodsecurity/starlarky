load("@stdlib//larky", larky="larky")

def VGSHttpResponse(
    body=None,
    headers={},
    status_code=200
):

    self = larky.mutablestruct(__name__="VGSHttpResponse", __class__=VGSHttpResponse)

    # body property
    def _get_body():
        return self._body

    def _set_body(body):
        if body != self._body:
            self._body = body
            # issue 16464
            # if we change data we need to remove content-length header
            # (cause it's most probably calculated for previous value)
            if "Content-length" in self.headers:
                self.remove_header("Content-length")
    self.body = larky.property(_get_body, _set_body)

    def add_header(key, val):
        self.headers[key] = val
    self.add_header = add_header

    def __init__(
        body,
        headers,
        status_code
    ):
        self._body = body

        self.headers = {}
        for key, value in headers.items():
            self.add_header(key, value)

        self.status_code=status_code

        return self

    self = __init__(body, headers, status_code)

    return self
