"""Stripe route helpers for publishable-key binding.

    load("@vgs//stripe", "stripe")

    def process(input, ctx):
        return stripe.require_publishable_key(input, ["pk_live_..."]) or input

    Every credential the request presents must be allowed: form ``key`` fields
    (application/x-www-form-urlencoded), ``key`` query parameters, ``Authorization:
    Bearer`` token, and Basic auth username. If ``Stripe-Account`` is present,
    ``allowed_accounts`` must be set and include that account value (header name
    matched case-insensitively). Duplicate ``Authorization`` or ``Stripe-Account``
    header values are rejected.

    Accepts operation-pipeline ``LarkySanitizedHttpMessage`` (``uri()``, ``headers()``,
    ``raw_body()``) or ``VGSHttpRequest``.
"""
load("@stdlib//base64", base64="base64")
load("@stdlib//larky", larky="larky")
load("@stdlib//re", re="re")
load("@stdlib//urllib/parse", parse="parse")
load("@vgs//http/response", VGSHttpResponse="VGSHttpResponse")
load("@vgs//proxy", proxy="proxy")

_FORM_URLENCODED = "application/x-www-form-urlencoded"
_FORBIDDEN_BODY = b'{"error":"psp_key_not_allowed"}'
_ACCOUNT_FORBIDDEN_BODY = b'{"error":"psp_account_not_allowed"}'
_BASIC_B64_PATTERN = r'^[A-Za-z0-9+/]+={0,2}$'
_REPLACEMENT_CHAR = "\ufffd"
_EMPTY_HEADER_VALUES = []


def _view(request):
    if hasattr(request, "raw_body"):
        target = request.uri()
        headers = list(request.headers())
        body = request.raw_body()
    else:
        target = request.url
        headers = []
        for name, value in request.headers.items():
            headers.append((name, value))
        body = request.body
        if body != None and type(body) == type(""):
            body = body.encode("utf-8")
    return larky.struct(target=target, headers=headers, body=body)


def _header_name_matches(header_name, name):
    return header_name.lower() == name.lower()


def _header_values(view, name):
    values = []
    for pair in view.headers:
        if _header_name_matches(pair[0], name):
            values.append(pair[1])
    return values


def _header_value(view, name):
    values = _header_values(view, name)
    if len(values) == 0:
        return None
    return values[0]


def _is_form_urlencoded(view):
    content_type = _header_value(view, "Content-Type")
    if content_type == None:
        return False
    media_type = content_type.split(";", 1)[0].strip().lower()
    return media_type == _FORM_URLENCODED


def _query_string_from_target(target):
    if target == None or target == "":
        return None
    qmark = target.find("?")
    if qmark < 0:
        return None
    query_part = target[qmark + 1:]
    hash_idx = query_part.find("#")
    if hash_idx >= 0:
        query_part = query_part[0:hash_idx]
    if query_part == "":
        return None
    return query_part


def _append_query_keys(credentials, view):
    query_string = _query_string_from_target(view.target)
    if query_string == None:
        return
    parsed = parse.parse_qs(query_string, keep_blank_values=True)
    values = parsed.get("key")
    if values == None:
        return
    for value in values:
        credentials.append(value)


def _body_as_form_string(body):
    if body == None:
        return None
    if type(body) == type(""):
        return body
    body_str = str(body)
    if body_str == "":
        return None
    return body_str


def _append_form_keys(credentials, view):
    if not _is_form_urlencoded(view):
        return
    body_str = _body_as_form_string(view.body)
    if body_str == None:
        return
    parsed = parse.parse_qs(body_str, keep_blank_values=True)
    values = parsed.get("key")
    if values == None:
        return
    for value in values:
        credentials.append(value)


def _append_bearer_key(credentials, view):
    authorization = _header_value(view, "Authorization")
    if authorization == None:
        return
    authorization = authorization.strip()
    if len(authorization) < 7:
        return
    prefix = authorization[0:7]
    if prefix.lower() != "bearer ":
        return
    token = authorization[7:].strip()
    if token == "":
        return
    credentials.append(token)


def _malformed_basic_credential():
    return "\x00__malformed_basic__"


def _basic_token_is_well_formed(encoded):
    if encoded == "":
        return False
    if len(encoded) % 4 != 0:
        return False
    return re.match(_BASIC_B64_PATTERN, encoded) != None


def _decoded_basic_credentials_are_valid(decoded):
    decoded_str = decoded.decode("utf-8", "replace")
    if decoded_str.find(_REPLACEMENT_CHAR) >= 0:
        return False
    if decoded_str.find(":") < 0:
        return False
    return True


def _basic_username_from_token(encoded):
    if not _basic_token_is_well_formed(encoded):
        return None
    decoded = base64.b64decode(encoded, validate=True)
    if decoded == None:
        return None
    if not _decoded_basic_credentials_are_valid(decoded):
        return None
    decoded_str = decoded.decode("utf-8", "replace")
    colon = decoded_str.find(":")
    return decoded_str[0:colon]


def _append_basic_username(credentials, view):
    authorization = _header_value(view, "Authorization")
    if authorization == None:
        return
    authorization = authorization.strip()
    if len(authorization) < 6:
        return
    if authorization[0:6].lower() != "basic ":
        return
    encoded = authorization[6:].strip()
    username = _basic_username_from_token(encoded)
    if username == None:
        credentials.append(_malformed_basic_credential())
        return
    credentials.append(username)


def _collect_credentials(view):
    credentials = []
    _append_form_keys(credentials, view)
    _append_query_keys(credentials, view)
    _append_bearer_key(credentials, view)
    _append_basic_username(credentials, view)
    return credentials


def _forbidden_response(body):
    return proxy.ShortCircuitResponse(
        VGSHttpResponse(
            status_code=403,
            headers={"Content-Type": "application/json"},
            body=body,
        )
    )


def _key_forbidden_response():
    return _forbidden_response(_FORBIDDEN_BODY)


def _account_forbidden_response():
    return _forbidden_response(_ACCOUNT_FORBIDDEN_BODY)


def require_publishable_key(request, allowed_keys, allowed_accounts=None):
    view = _view(request)

    stripe_accounts = _header_values(view, "Stripe-Account")
    if len(stripe_accounts) > 1:
        return _account_forbidden_response()

    authorizations = _header_values(view, "Authorization")
    if len(authorizations) > 1:
        return _key_forbidden_response()

    if len(stripe_accounts) == 1:
        stripe_account = stripe_accounts[0]
        if allowed_accounts == None or stripe_account not in allowed_accounts:
            return _account_forbidden_response()

    credentials = _collect_credentials(view)
    if len(credentials) == 0:
        return _key_forbidden_response()
    for credential in credentials:
        if credential not in allowed_keys:
            return _key_forbidden_response()
    return None


stripe = larky.struct(
    require_publishable_key=require_publishable_key,
)
