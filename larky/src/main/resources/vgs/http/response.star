load("@vgs//http/headers", "VGSCIMultiDict")
load("@vgs//jhttpresponse", _native_response="jhttpresponse")


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
    """
    return _make_response(body, headers, status_code, True)
