load("@vgs//http/headers", "VGSCIMultiDict")
load("@vgs//jhttpresponse", _native_response="jhttpresponse")


def VGSHttpResponse(body=None, headers=None, status_code=200):
    """Build a response with ordered, case-insensitive headers."""
    return _native_response.create(VGSHttpResponse, VGSCIMultiDict, body, headers or {}, status_code)
