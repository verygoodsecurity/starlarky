"""HTTP request and header-multidict workloads, for StarlarkCall (spec: http_headers.star#<function>)."""
load("@vgs//http/request", "VGSCIMultiDict", "VGSHttpRequest")

URL = "https://api.example.test/v1/payments?id=1"
H12_DICT = {
    "Host": "api.example.test",
    "User-Agent": "client/1.0",
    "Accept": "application/json",
    "Accept-Encoding": "gzip, deflate",
    "Content-Type": "application/json",
    "Content-Length": "128",
    "Authorization": "Bearer token",
    "X-Request-Id": "req-1",
    "X-Forwarded-For": "10.0.0.1",
    "X-Forwarded-Proto": "https",
    "Connection": "keep-alive",
    "Cache-Control": "no-cache",
}
# Atlas passes request headers as a list of (name, value) pairs
H12_LIST = [(k, v) for k, v in H12_DICT.items()]
BODY = b"{}"
LOOKUP_KEYS = ["content-type", "Authorization", "X-REQUEST-ID", "accept", "Host", "missing",
               "cache-control", "connection", "user-agent", "x-forwarded-for"]


def request_from_dict():
    """VGSHttpRequest from a 12-header dict."""
    return VGSHttpRequest(URL, data=BODY, headers=H12_DICT, method="POST")


def request_from_pairs():
    """VGSHttpRequest from 12 (name, value) pairs, as Atlas builds it."""
    return VGSHttpRequest(URL, data=BODY, headers=H12_LIST, method="POST")


def _request_projection(request):
    return (
        request.full_url,
        request.host,
        request.path,
        request.query_string,
        request.get_method(),
        request.body,
        request.headers.items(),
    )


__larky_bench_project_request_from_dict = _request_projection
__larky_bench_project_request_from_pairs = _request_projection


def _edit(h):
    # typical script header work: [] / get / pop / in / set / keys / items
    a = h["Content-Type"]
    b = h.get("x-request-id")
    c = h.get("X-Missing", "")
    if "authorization" in h:
        h.pop("Authorization")
    h["X-Added"] = "1"
    h["content-type"] = "application/json"
    ks = h.keys()
    return h.items()


def request_and_header_edits():
    """A request plus typical header edits."""
    return _edit(request_from_pairs().headers)


def host_projection():
    """Project request headers into the pairs a host can serialize."""
    return request_from_pairs().headers.items()


def headers_from_pairs():
    """The header multidict alone, from 12 pairs."""
    return VGSCIMultiDict(H12_LIST)


def header_edits():
    """Typical header edits on a fresh multidict."""
    return _edit(VGSCIMultiDict(H12_LIST))


def header_lookups():
    """10 case-insensitive lookups on a 12-header multidict."""
    h = VGSCIMultiDict(H12_LIST)
    return [h.get(k) for k in LOOKUP_KEYS]
