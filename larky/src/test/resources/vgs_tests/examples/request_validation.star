"""Example: reject requests that do not carry a valid signed token (issue #196).

A route script that checks `Authorization: Bearer <JWT>` on each request and,
when the token is missing or invalid, answers 401 itself instead of letting
the request reach the upstream:

    def process(input, ctx):
        return check_request(input, PUBLIC_KEY_ALIAS)

check_request verifies the token's signature with the issuer's public key and
checks its "exp", "nbf", "aud" and "iss" claims with jose.jwt.decode. It
returns the request unchanged when the token is valid, or a
proxy.ShortCircuitResponse wrapping a 401 VGSHttpResponse when it is not.

The public key. A real route keeps the PEM in the vault and the script holds
only its alias; vault.reveal returns the PEM at run time. PUBLIC_KEY_ALIAS
below is a placeholder. If the alias does not resolve, reveal returns the
alias itself, which is not a key, so every request gets a 401.

The current time. Larky scripts cannot read the clock, and jwt.decode
requires `now=` (seconds since the epoch). This example ASSUMES that the
host proxy sets the request header REQUEST_TIME_HEADER to the time it
received the request, and removes any copy of that header sent by the
client. Larky does not set such a header and
does not define what `ctx` contains; a host that passes the time some other
way should pass it to check_request as `now` instead. A client that can set
this header can replay expired tokens.

What this script does not do: whether a short-circuited request is billed,
whether aliases in it are revealed or deleted, and whether the upstream is
called are decided by the proxy that runs the script, not by Larky. Larky only
returns the ShortCircuitResponse.

Tested by test_default_request_validation.star next to this file.
"""

load("@stdlib//json", json="json")
load("@stdlib//types", types="types")
load("@vendor//jose", jwt="jwt")
load("@vendor//option/result", Result="Result")
load("@vgs//http/response", "VGSHttpResponse")
load("@vgs//proxy", "proxy")
load("@vgs//vault", "vault")

# Vault alias of the token issuer's PEM public key (placeholder).
PUBLIC_KEY_ALIAS = "tok_sandbox_example_public_key"

# Accept only the algorithm the issuer signs with. jwt.decode rejects a token
# whose header names any other algorithm, including "none".
ALGORITHMS = ["RS256"]
AUDIENCE = "https://api.example.com"
ISSUER = "https://issuer.example.com"

# Assumed: set by the host proxy, never by the client (see above).
REQUEST_TIME_HEADER = "X-Example-Request-Time"

# jwt.decode options: require these claims instead of only checking them
# when present.
DECODE_OPTIONS = {
    "require_exp": True,
    "require_aud": True,
    "require_iss": True,
    "leeway": 30,  # seconds of clock skew allowed for exp/nbf
}


def unauthorized():
    """A ShortCircuitResponse that answers 401 without calling the upstream."""
    body = json.dumps({"error": "unauthorized"})
    return proxy.ShortCircuitResponse(VGSHttpResponse(
        body=bytes(body, encoding="utf-8"),
        headers={
            "Content-Type": "application/json",
            "WWW-Authenticate": 'Bearer error="invalid_token"',
        },
        status_code=401,
    ))


def bearer_token(request):
    """The token in `Authorization: Bearer <token>`, or None."""
    value = request.headers.get("Authorization")
    if not value:
        return None
    # Tolerates extra spaces: Python's value.split(); Larky's str.split needs a sep.
    parts = [p for p in value.split(" ") if p]
    if len(parts) != 2 or parts[0].lower() != "bearer":
        return None
    return parts[1]


def request_time(request):
    """The time from REQUEST_TIME_HEADER as an int, or None."""
    value = request.headers.get(REQUEST_TIME_HEADER)
    if not value or not value.isdigit():
        return None
    return int(value)


def verify_token(token, public_key, now):
    """The token's claims if it is valid at `now`, else None."""
    result = Result.safe(jwt.decode)(
        token,
        public_key,
        algorithms=ALGORITHMS,
        audience=AUDIENCE,
        issuer=ISSUER,
        options=DECODE_OPTIONS,
        now=now,
    )
    if result.is_err:
        # result._val is the reason, e.g. "ExpiredSignatureError: Signature
        # has expired."; do not send it to the client.
        return None
    return result.unwrap()


def check_request(request, public_key_alias, now=None):
    """Returns `request` if its bearer token is valid, else a 401 short circuit.

    Args:
        request: the VGSHttpRequest.
        public_key_alias: vault alias of the issuer's PEM public key.
        now: seconds since the epoch; defaults to REQUEST_TIME_HEADER.
    """
    token = bearer_token(request)
    if token == None:
        return unauthorized()
    if now == None:
        now = request_time(request)
    if now == None:
        # Fail closed: without a trusted time, exp cannot be checked.
        return unauthorized()
    public_key = vault.reveal(public_key_alias)
    if verify_token(token, public_key, now) == None:
        return unauthorized()
    return request


def process(input, ctx):
    return check_request(input, PUBLIC_KEY_ALIAS)
