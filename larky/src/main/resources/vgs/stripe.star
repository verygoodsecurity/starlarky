"""Stripe route helpers for publishable-key binding.

    load("@vgs//stripe", "stripe")

    def process(input, ctx):
        return stripe.require_publishable_key(input, ["pk_test_..."]) or input
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//urllib/parse", parse="parse")
load("@vgs//http/response", VGSHttpResponse="VGSHttpResponse")
load("@vgs//proxy", proxy="proxy")

_FORM_URLENCODED = "application/x-www-form-urlencoded"
_FORBIDDEN_BODY = b'{"error":"psp_key_not_allowed"}'


def _header_value(request, name):
    name_lower = name.lower()
    for key, value in request.headers.items():
        if key.lower() == name_lower:
            return value
    return None


def _is_form_urlencoded(request):
    content_type = _header_value(request, "Content-Type")
    if content_type == None:
        return False
    media_type = content_type.split(";", 1)[0].strip().lower()
    return media_type == _FORM_URLENCODED


def _form_key(request):
    if not _is_form_urlencoded(request):
        return None
    body = request.body
    if body == None:
        return None
    body_str = str(body)
    if body_str == "":
        return None
    parsed = parse.parse_qs(body_str, keep_blank_values=True)
    values = parsed.get("key")
    if values == None or len(values) == 0:
        return None
    return values[0]


def _bearer_key(request):
    authorization = _header_value(request, "Authorization")
    if authorization == None:
        return None
    authorization = authorization.strip()
    if len(authorization) < 7:
        return None
    prefix = authorization[0:7]
    if prefix.lower() != "bearer ":
        return None
    token = authorization[7:].strip()
    if token == "":
        return None
    return token


def _extract_publishable_key(request):
    form_key = _form_key(request)
    if form_key != None:
        return form_key
    return _bearer_key(request)


def _forbidden_response():
    return proxy.ShortCircuitResponse(
        VGSHttpResponse(
            status_code=403,
            headers={"Content-Type": "application/json"},
            body=_FORBIDDEN_BODY,
        )
    )


def require_publishable_key(request, allowed_keys):
    key = _extract_publishable_key(request)
    if key != None and key in allowed_keys:
        return None
    return _forbidden_response()


stripe = larky.struct(
    require_publishable_key=require_publishable_key,
)
