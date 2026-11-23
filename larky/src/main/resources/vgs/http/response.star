load("@vgs//http/headers", "VGSCIMultiDict")
load("@vgs//http_response_native", _native_response="http_response_native")


def _make_response(body, headers, status_code, short_circuit):
    self = _native_response.create(VGSHttpResponse, VGSCIMultiDict, body, headers or {}, status_code, short_circuit)

    def add_header(key, val):
        self.headers[key] = val
    self.add_header = add_header

    def append_header(key, val):
        self.headers.add(key, val)
    self.append_header = append_header

    def remove_header(key):
        self.headers.popall(key, [])
    self.remove_header = remove_header

    def get_all_headers(key):
        return self.headers.getall(key, [])
    self.get_all_headers = get_all_headers

    return self


def VGSHttpResponse(body=None, headers=None, status_code=200):
    """Build a response with ordered, case-insensitive headers."""
    return _make_response(body, headers, status_code, False)


def ShortCircuitResponse(body=None, headers=None, status_code=200):
    """Build a final HTTP response and stop processing the message.

    In the request phase the upstream is not called. In either phase the remaining filters are
    skipped.

    The form of Larky 1.3 and earlier, ShortCircuitResponse(VGSHttpResponse(...)), is also
    accepted: the response's body, ordered headers and status code are used.
    """
    if getattr(body, "__class__", None) == VGSHttpResponse:
        if headers != None or status_code != 200:
            fail("ShortCircuitResponse takes either a VGSHttpResponse or body, headers and " +
                 "status_code, not both")
        response = body
        response_headers = response.headers
        return _make_response(
            response.body,
            None if response_headers == None else response_headers.items(),
            response.status_code,
            True,
        )
    return _make_response(body, headers, status_code, True)
