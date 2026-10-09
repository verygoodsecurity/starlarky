"""Stripe route helpers for publishable-key binding.

    load("@vgs//stripe", "stripe")

    def process(input, ctx):
        return stripe.require_publishable_key(input, ["pk_test_..."], allowed_accounts=["acct_..."]) or input

    Every credential the request presents must be allowed: form ``key`` fields
    (application/x-www-form-urlencoded), ``key`` query parameters, ``Authorization:
    Bearer`` token, and Basic auth username. If ``Stripe-Account`` is present,
    ``allowed_accounts`` must be set and include that account value (header name
    matched case-insensitively). Duplicate ``Authorization`` or ``Stripe-Account``
    header values are rejected.
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
_EMPTY_HEADER_VALUES = []


def _header_values(request, name):
    return request.headers.getall(name, _EMPTY_HEADER_VALUES)


def _header_value(request, name):
    values = _header_values(request, name)
    if len(values) == 0:
        return None
    return values[0]


def _is_form_urlencoded(request):
    content_type = _header_value(request, "Content-Type")
    if content_type == None:
        return False
    media_type = content_type.split(";", 1)[0].strip().lower()
    return media_type == _FORM_URLENCODED


def _query_string_from_url(url):
    if url == None or url == "":
        return None
    qmark = url.find("?")
    if qmark < 0:
        return None
    query_part = url[qmark + 1:]
    hash_idx = query_part.find("#")
    if hash_idx >= 0:
        query_part = query_part[0:hash_idx]
    if query_part == "":
        return None
    return query_part


def _append_query_keys(credentials, request):
    query_strings = []
    if request.query_string != None and request.query_string != "":
        query_strings.append(request.query_string)
    url_query = _query_string_from_url(request.url)
    if url_query != None and url_query not in query_strings:
        query_strings.append(url_query)
    for query_string in query_strings:
        parsed = parse.parse_qs(query_string, keep_blank_values=True)
        values = parsed.get("key")
        if values == None:
            continue
        for value in values:
            credentials.append(value)


def _append_form_keys(credentials, request):
    if not _is_form_urlencoded(request):
        return
    body = request.body
    if body == None:
        return
    body_str = str(body)
    if body_str == "":
        return
    parsed = parse.parse_qs(body_str, keep_blank_values=True)
    values = parsed.get("key")
    if values == None:
        return
    for value in values:
        credentials.append(value)


def _append_bearer_key(credentials, request):
    authorization = _header_value(request, "Authorization")
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


def _basic_username_from_token(encoded):
    if not _basic_token_is_well_formed(encoded):
        return None
    decoded = base64.b64decode(encoded, validate=True)
    if decoded == None:
        return None
    decoded_str = decoded.decode("utf-8")
    if decoded_str == None:
        return None
    colon = decoded_str.find(":")
    if colon < 0:
        return None
    return decoded_str[0:colon]


def _append_basic_username(credentials, request):
    authorization = _header_value(request, "Authorization")
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


def _collect_credentials(request):
    credentials = []
    _append_form_keys(credentials, request)
    _append_query_keys(credentials, request)
    _append_bearer_key(credentials, request)
    _append_basic_username(credentials, request)
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
    stripe_accounts = _header_values(request, "Stripe-Account")
    if len(stripe_accounts) > 1:
        return _account_forbidden_response()

    authorizations = _header_values(request, "Authorization")
    if len(authorizations) > 1:
        return _key_forbidden_response()

    if len(stripe_accounts) == 1:
        stripe_account = stripe_accounts[0]
        if allowed_accounts == None or stripe_account not in allowed_accounts:
            return _account_forbidden_response()

    credentials = _collect_credentials(request)
    if len(credentials) == 0:
        return _key_forbidden_response()
    for credential in credentials:
        if credential not in allowed_keys:
            return _key_forbidden_response()
    return None


stripe = larky.struct(
    require_publishable_key=require_publishable_key,
)
