load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")


# `headers` stays a plain dict: the host reads `response.__dict__["headers"]` as a
# Map<String, String> and sends it in a protobuf map<string, string>, so it cannot carry a
# header name twice. A multidict here makes the host fail on every response (issue #292).
def _header_dict(headers):
    """Return `headers` (a dict, a list of (name, value) pairs or a multidict) as a dict.

    A dict is returned as is. A header name given twice fails rather than silently keeping
    only the last value.
    """
    if types.is_dict(headers):
        return headers
    if hasattr(headers, "items"):
        pairs = headers.items()
    elif types.is_list(headers) or types.is_tuple(headers):
        pairs = headers
    else:
        # e.g. None from `headers = d.update(...)`: fail here, not in the host
        fail("VGSHttpResponse headers: expected a dict or (name, value) pairs; got %s" % type(headers))
    result = {}
    for name, value in pairs:
        if name in result:
            fail("VGSHttpResponse headers hold one value per name; got %r more than once" % name)
        result[name] = value
    return result


def _check_name(method, name):
    if not types.is_string(name):
        fail("TypeError: VGSHttpResponse.%s(header_name) expected `string`; got %s" % (method, type(name)))


def _same_name(key, name):
    return types.is_string(key) and key.lower() == name.lower()


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
            # issue 16464: a Content-Length computed for the previous body is wrong.
            # Header names are case-insensitive, so drop every spelling of it.
            self.remove_header("Content-Length")
    self.body = larky.property(_get_body, _set_body)

    # headers property: the dict is kept as is, so later changes to it still apply
    def _get_headers():
        return self._headers

    def _set_headers(headers):
        self._headers = _header_dict(headers)
    self.headers = larky.property(_get_headers, _set_headers)

    def add_header(key, val):
        self.headers[key] = val
    self.add_header = add_header

    def has_header(header_name):
        """Whether a header named `header_name`, in any case, is set."""
        _check_name("has_header", header_name)
        for key in self.headers:
            if _same_name(key, header_name):
                return True
        return False
    self.has_header = has_header

    def get_header(header_name, default=None):
        """Return the value of the first header named `header_name`, in any case."""
        _check_name("get_header", header_name)
        for key, value in self.headers.items():
            if _same_name(key, header_name):
                return value
        return default
    self.get_header = get_header

    def remove_header(header_name):
        """Remove every header named `header_name`, in any case."""
        _check_name("remove_header", header_name)
        for key in [k for k in self.headers if _same_name(k, header_name)]:
            self.headers.pop(key)
    self.remove_header = remove_header

    def __init__(
        body,
        headers,
        status_code
    ):
        self._body = body
        # a copy, so the caller's dict is not changed through the response
        if not headers:
            headers = {}
        self.headers = dict(headers) if types.is_dict(headers) else headers
        self.status_code = status_code
        return self

    self = __init__(body, headers, status_code)

    return self
