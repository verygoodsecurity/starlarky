load("@stdlib//urllib/parse", parse="parse")
load("@stdlib//urllib/request", urllib_request="request")

load("@vgs//http/headers", _VGSCIMultiDict="VGSCIMultiDict")
load("@vgs//jhttprequest", _native_request="jhttprequest")
load("@vendor//_multidict_core", "CIMultiDict")


VGSCIMultiDict = _VGSCIMultiDict


def VGSHttpRequest(
    url,
    data=None,
    headers={},
    method=None
):
    """A urllib.request.Request with ordered, case-insensitive headers, implemented in Java."""
    return _native_request.VGSHttpRequest(
        VGSHttpRequest,
        urllib_request.Request(url, data=data, method=method),
        VGSCIMultiDict,
        parse.urlsplit,
        url,
        headers,
    )
