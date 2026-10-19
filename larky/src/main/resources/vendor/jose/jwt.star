# Ported from python-jose 3.5.0 (tag 3.5.0, commit
# 018b310ddb8b50dcfd09a0c152117835a21dd656), file jose/jwt.py:
# https://github.com/mpdavis/python-jose/blob/018b310ddb8b50dcfd09a0c152117835a21dd656/jose/jwt.py
# sdist python-jose-3.5.0 (python_jose-3.5.0.tar.gz) sha256
# fb4eaa44dbeb1c26dcc69e4bd7ec54a1cb8dd64d3b4d81ef08d90ff453f2b01b
#
# Translated with py2star (larkify) and fixed by hand. Differences from
# upstream, each also described in the docstring it affects:
#
# - Larky scripts cannot read the clock, so decode() takes a Larky-only
#   `now=` argument (seconds since the epoch) for the exp and nbf checks.
#   Without it, a token that carries exp or nbf is rejected unless that check
#   is disabled through `options`.
# - There is no datetime module: exp/iat/nbf must already be NumericDate
#   numbers in encode(), and options["leeway"] must be a number of seconds.
# - Errors are raised with fail() as "<ExceptionName>: <message>", where the
#   name is JWTError, JWTClaimsError or ExpiredSignatureError (the latter two
#   subclass JWTError upstream). An at_hash for an alg without a hash
#   (e.g. "none") fails with "KeyError: '<alg>'", as upstream's uncaught
#   KeyError does.
#
# The rest of vendor/jose was ported earlier from python-jose's master branch
# (between commits 89c3d41 and 8572088, 2020-09-07..2020-12-07), not from
# 3.5.0; see the header of jose/__init__.star.

load("@stdlib//json", json="json")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//jose/jws", jws="jws")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//jose/utils", calculate_at_hash="calculate_at_hash")
load("@vendor//option/result", Result="Result")


def _at_hash_alg(algorithm):
    """ALGORITHMS.HASHES[algorithm], failing as upstream's uncaught KeyError does."""
    if algorithm not in ALGORITHMS.HASHES:
        fail("KeyError: %s" % ("'%s'" % algorithm if types.is_string(algorithm) else repr(algorithm)))
    return ALGORITHMS.HASHES[algorithm]


def _strip_error_name(message, names):
    for name in names:
        prefix = name + ": "
        if message.startswith(prefix):
            return message[len(prefix):]
    return message


def encode(claims, key, algorithm=ALGORITHMS.HS256, headers=None, access_token=None):
    """Encodes a claims set and returns a JWT string.

    JWTs are JWS signed objects with a few reserved claims.

    Args:
        claims (dict): A claims set to sign
        key (str or dict): The key to use for signing the claim set. Can be
            individual JWK or JWK set.
        algorithm (str, optional): The algorithm to use for signing the
            the claims.  Defaults to HS256.
        headers (dict, optional): A set of headers that will be added to
            the default headers.  Any headers that are added as additional
            headers will override the default headers.
        access_token (str, optional): If present, the 'at_hash' claim will
            be calculated and added to the claims present in the 'claims'
            parameter.

    Returns:
        str: The string representation of the header, claims, and signature.

    Raises:
        JWTError: If there is an error encoding the claims.

    Larky:
        Upstream converts ``datetime`` values of the exp, iat and nbf claims
        to NumericDate integers. Larky has no ``datetime``, so pass these
        claims as integers (seconds since the epoch); any other value is
        signed as given.

    Examples:

        >>> jwt.encode({'a': 'b'}, 'secret', algorithm='HS256')
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYiJ9.jiMyrsmD8AoHWeQgmxZ5yq8z0lXS67_QGs52AzC8Ru8'

    """

    if access_token:
        claims["at_hash"] = calculate_at_hash(access_token, _at_hash_alg(algorithm))

    return jws.sign(claims, key, headers=headers, algorithm=algorithm)


def decode(token, key, algorithms=None, options=None, audience=None, issuer=None, subject=None, access_token=None,
           now=None):
    """Verifies a JWT string's signature and validates reserved claims.

    Args:
        token (str): A signed JWS to be verified.
        key (str or iterable): A key to attempt to verify the payload with.
            This can be simple string with an individual key (e.g. "a1234"),
            a tuple or list of keys (e.g. ("a1234...", "b3579"),
            a JSON string, (e.g. '["a1234", "b3579"]'),
            a dict with the 'keys' key that gives a tuple or list of keys (e.g {'keys': [...]} ) or
            a dict or JSON string for a JWK set as defined by RFC 7517 (e.g.
                {'keys': [{'kty': 'oct', 'k': 'YTEyMzQ'}, {'kty': 'oct', 'k':'YjM1Nzk'}]} or
                '{"keys": [{"kty":"oct","k":"YTEyMzQ"},{"kty":"oct","k":"YjM1Nzk"}]}'
            ) in which case the keys must be base64 url safe encoded (with optional padding).
        algorithms (str or list): Valid algorithms that should be used to verify the JWS.
        audience (str): The intended audience of the token.  If the "aud" claim is
            included in the claim set, then the audience must be included and must equal
            the provided claim.
        issuer (str or iterable): Acceptable value(s) for the issuer of the token.
            If the "iss" claim is included in the claim set, then the issuer must be
            given and the claim in the token must be among the acceptable values.
        subject (str): The subject of the token.  If the "sub" claim is
            included in the claim set, then the subject must be included and must equal
            the provided claim.
        access_token (str): An access token string. If the "at_hash" claim is included in the
            claim set, then the access_token must be included, and it must match
            the "at_hash" claim.
        options (dict): A dictionary of options for skipping validation steps.

            defaults = {
                'verify_signature': True,
                'verify_aud': True,
                'verify_iat': True,
                'verify_exp': True,
                'verify_nbf': True,
                'verify_iss': True,
                'verify_sub': True,
                'verify_jti': True,
                'verify_at_hash': True,
                'require_aud': False,
                'require_iat': False,
                'require_exp': False,
                'require_nbf': False,
                'require_iss': False,
                'require_sub': False,
                'require_jti': False,
                'require_at_hash': False,
                'leeway': 0,
            }

        now (int or float): Larky only. The current time in seconds since the
            epoch, used to check the "exp" and "nbf" claims.

    Returns:
        dict: The dict representation of the claims set, assuming the signature is valid
            and all requested data validation passes.

    Raises:
        JWTError: If the signature is invalid in any way.
        ExpiredSignatureError: If the signature has expired.
        JWTClaimsError: If any claim is invalid in any way.

    Larky:
        - Upstream reads the current time from the system clock. Larky scripts
          cannot, so the caller passes it as ``now``. If ``now`` is None and the
          token has an "exp" (or "nbf") claim that is being verified, decode
          raises JWTClaimsError instead of skipping the check; set
          ``options={'verify_exp': False}`` (or ``'verify_nbf'``) to skip it.
        - ``options['leeway']`` must be a number of seconds; ``timedelta`` is
          not supported.
        - Errors are raised with ``fail()`` as "<name>: <message>", where name
          is JWTError, JWTClaimsError or ExpiredSignatureError. Upstream
          JWTClaimsError and ExpiredSignatureError subclass JWTError.
        - Upstream re-raises only JWSError from jws.verify as JWTError. Larky's
          jws reports some malformed tokens with other errors (e.g. a token
          with no "."), so every error from jws.verify becomes a JWTError.

    Examples:

        >>> payload = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYiJ9.jiMyrsmD8AoHWeQgmxZ5yq8z0lXS67_QGs52AzC8Ru8'
        >>> jwt.decode(payload, 'secret', algorithms='HS256')

    """

    defaults = {
        "verify_signature": True,
        "verify_aud": True,
        "verify_iat": True,
        "verify_exp": True,
        "verify_nbf": True,
        "verify_iss": True,
        "verify_sub": True,
        "verify_jti": True,
        "verify_at_hash": True,
        "require_aud": False,
        "require_iat": False,
        "require_exp": False,
        "require_nbf": False,
        "require_iss": False,
        "require_sub": False,
        "require_jti": False,
        "require_at_hash": False,
        "leeway": 0,
    }

    if options:
        defaults.update(options)

    if now != None and not (types.is_int(now) or types.is_float(now)):
        fail("JWTError: now must be a number of seconds since the epoch or None")

    verify_signature = defaults.get("verify_signature", True)

    _try_1_r = Result.Ok(None).map(lambda _: jws.verify(token, key, algorithms, verify=verify_signature))
    if not _try_1_r.is_ok:
        fail("JWTError: " + _strip_error_name(str(_try_1_r._val), ("JWSError",)))
    payload = _try_1_r.unwrap()

    # Needed for at_hash verification
    algorithm = jws.get_unverified_header(token)["alg"]

    _try_2_r = Result.Ok(None).map(lambda _: json.loads(payload.decode("utf-8")))
    if not _try_2_r.is_ok:
        fail("JWTError: " + "Invalid payload string: {}".format(_try_2_r._val))
    claims = _try_2_r.unwrap()

    if not types.is_dict(claims):
        fail("JWTError: Invalid payload string: must be a json object")

    _validate_claims(
        claims,
        audience=audience,
        issuer=issuer,
        subject=subject,
        algorithm=algorithm,
        access_token=access_token,
        options=defaults,
        now=now,
    )

    return claims


def get_unverified_header(token):
    """Returns the decoded headers without verification of any kind.

    Args:
        token (str): A signed JWT to decode the headers from.

    Returns:
        dict: The dict representation of the token headers.

    Raises:
        JWTError: If there is an exception decoding the token.
    """
    _try_3_r = Result.Ok(None).map(lambda _: jws.get_unverified_headers(token))
    if not _try_3_r.is_ok:
        fail("JWTError: Error decoding token headers.")

    return _try_3_r.unwrap()


def get_unverified_headers(token):
    """Returns the decoded headers without verification of any kind.

    This is simply a wrapper of get_unverified_header() for backwards
    compatibility.

    Args:
        token (str): A signed JWT to decode the headers from.

    Returns:
        dict: The dict representation of the token headers.

    Raises:
        JWTError: If there is an exception decoding the token.
    """
    return get_unverified_header(token)


def get_unverified_claims(token):
    """Returns the decoded claims without verification of any kind.

    Args:
        token (str): A signed JWT to decode the headers from.

    Returns:
        dict: The dict representation of the token claims.

    Raises:
        JWTError: If there is an exception decoding the token.
    """
    _try_4_r = Result.Ok(None).map(lambda _: jws.get_unverified_claims(token))
    if not _try_4_r.is_ok:
        fail("JWTError: Error decoding token claims.")
    claims = _try_4_r.unwrap()

    _try_5_r = Result.Ok(None).map(lambda _: json.loads(claims.decode("utf-8")))
    if not _try_5_r.is_ok:
        fail("JWTError: " + "Invalid claims string: {}".format(_try_5_r._val))
    claims = _try_5_r.unwrap()

    if not types.is_dict(claims):
        fail("JWTError: Invalid claims string: must be a json object")

    return claims


# int() fails with these messages where Python raises ValueError; Python's
# TypeError (e.g. int(None)) is not caught upstream and is not caught here.
_INT_VALUE_ERROR = "^ValueError(:|$)|invalid base-\\d+ literal"


def _validate_iat(claims):
    """Validates that the 'iat' claim is valid.

    The "iat" (issued at) claim identifies the time at which the JWT was
    issued.  This claim can be used to determine the age of the JWT.  Its
    value MUST be a number containing a NumericDate value.  Use of this
    claim is OPTIONAL.

    Args:
        claims (dict): The claims dictionary to validate.
    """

    if "iat" not in claims:
        return

    _try_6_r = Result.Ok(None).map(lambda _: int(claims["iat"]))
    if _try_6_r.is_ok:
        return
    elif Result.error_is(_INT_VALUE_ERROR, _try_6_r):
        fail("JWTClaimsError: Issued At claim (iat) must be an integer.")
    else:
        _try_6_r.unwrap()


def _validate_nbf(claims, leeway=0, now=None):
    """Validates that the 'nbf' claim is valid.

    The "nbf" (not before) claim identifies the time before which the JWT
    MUST NOT be accepted for processing.  The processing of the "nbf"
    claim requires that the current date/time MUST be after or equal to
    the not-before date/time listed in the "nbf" claim.  Implementers MAY
    provide for some small leeway, usually no more than a few minutes, to
    account for clock skew.  Its value MUST be a number containing a
    NumericDate value.  Use of this claim is OPTIONAL.

    Args:
        claims (dict): The claims dictionary to validate.
        leeway (int): The number of seconds of skew that is allowed.
        now (int or float): Larky only: the current time in seconds since
            the epoch. If None, a present "nbf" claim cannot be checked and
            JWTClaimsError is raised.
    """

    if "nbf" not in claims:
        return

    _try_7_r = Result.Ok(None).map(lambda _: int(claims["nbf"]))
    if _try_7_r.is_ok:
        nbf = _try_7_r.unwrap()
    elif Result.error_is(_INT_VALUE_ERROR, _try_7_r):
        fail("JWTClaimsError: Not Before claim (nbf) must be an integer.")
    else:
        _try_7_r.unwrap()

    if now == None:
        fail("JWTClaimsError: Cannot verify the Not Before claim (nbf) without the current time: " +
             "pass now= (seconds since the epoch) or options={'verify_nbf': False}.")

    if nbf > (now + leeway):
        fail("JWTClaimsError: The token is not yet valid (nbf)")


def _validate_exp(claims, leeway=0, now=None):
    """Validates that the 'exp' claim is valid.

    The "exp" (expiration time) claim identifies the expiration time on
    or after which the JWT MUST NOT be accepted for processing.  The
    processing of the "exp" claim requires that the current date/time
    MUST be before the expiration date/time listed in the "exp" claim.
    Implementers MAY provide for some small leeway, usually no more than
    a few minutes, to account for clock skew.  Its value MUST be a number
    containing a NumericDate value.  Use of this claim is OPTIONAL.

    Args:
        claims (dict): The claims dictionary to validate.
        leeway (int): The number of seconds of skew that is allowed.
        now (int or float): Larky only: the current time in seconds since
            the epoch. If None, a present "exp" claim cannot be checked and
            JWTClaimsError is raised.
    """

    if "exp" not in claims:
        return

    _try_8_r = Result.Ok(None).map(lambda _: int(claims["exp"]))
    if _try_8_r.is_ok:
        exp = _try_8_r.unwrap()
    elif Result.error_is(_INT_VALUE_ERROR, _try_8_r):
        fail("JWTClaimsError: Expiration Time claim (exp) must be an integer.")
    else:
        _try_8_r.unwrap()

    if now == None:
        fail("JWTClaimsError: Cannot verify the Expiration Time claim (exp) without the current time: " +
             "pass now= (seconds since the epoch) or options={'verify_exp': False}.")

    if exp < (now - leeway):
        fail("ExpiredSignatureError: Signature has expired.")


def _validate_aud(claims, audience=None):
    """Validates that the 'aud' claim is valid.

    The "aud" (audience) claim identifies the recipients that the JWT is
    intended for.  Each principal intended to process the JWT MUST
    identify itself with a value in the audience claim.  If the principal
    processing the claim does not identify itself with a value in the
    "aud" claim when this claim is present, then the JWT MUST be
    rejected.  In the general case, the "aud" value is an array of case-
    sensitive strings, each containing a StringOrURI value.  In the
    special case when the JWT has one audience, the "aud" value MAY be a
    single case-sensitive string containing a StringOrURI value.  The
    interpretation of audience values is generally application specific.
    Use of this claim is OPTIONAL.

    Args:
        claims (dict): The claims dictionary to validate.
        audience (str): The audience that is verifying the token.
    """

    if "aud" not in claims:
        # if audience:
        #     raise JWTError('Audience claim expected, but not in claims')
        return

    audience_claims = claims["aud"]
    if types.is_string(audience_claims):
        audience_claims = [audience_claims]
    if not types.is_list(audience_claims):
        fail("JWTClaimsError: Invalid claim format in token")
    if any([not types.is_string(c) for c in audience_claims]):
        fail("JWTClaimsError: Invalid claim format in token")
    if audience not in audience_claims:
        fail("JWTClaimsError: Invalid audience")


def _validate_iss(claims, issuer=None):
    """Validates that the 'iss' claim is valid.

    The "iss" (issuer) claim identifies the principal that issued the
    JWT.  The processing of this claim is generally application specific.
    The "iss" value is a case-sensitive string containing a StringOrURI
    value.  Use of this claim is OPTIONAL.

    Args:
        claims (dict): The claims dictionary to validate.
        issuer (str or iterable): Acceptable value(s) for the issuer that
                                  signed the token.
    """

    if issuer != None:
        if types.is_string(issuer):
            issuer = (issuer,)
        if claims.get("iss") not in issuer:
            fail("JWTClaimsError: Invalid issuer")


def _validate_sub(claims, subject=None):
    """Validates that the 'sub' claim is valid.

    The "sub" (subject) claim identifies the principal that is the
    subject of the JWT.  The claims in a JWT are normally statements
    about the subject.  The subject value MUST either be scoped to be
    locally unique in the context of the issuer or be globally unique.
    The processing of this claim is generally application specific.  The
    "sub" value is a case-sensitive string containing a StringOrURI
    value.  Use of this claim is OPTIONAL.

    Args:
        claims (dict): The claims dictionary to validate.
        subject (str): The subject of the token.
    """

    if "sub" not in claims:
        return

    if not types.is_string(claims["sub"]):
        fail("JWTClaimsError: Subject must be a string.")

    if subject != None:
        if claims.get("sub") != subject:
            fail("JWTClaimsError: Invalid subject")


def _validate_jti(claims):
    """Validates that the 'jti' claim is valid.

    The "jti" (JWT ID) claim provides a unique identifier for the JWT.
    The identifier value MUST be assigned in a manner that ensures that
    there is a negligible probability that the same value will be
    accidentally assigned to a different data object; if the application
    uses multiple issuers, collisions MUST be prevented among values
    produced by different issuers as well.  The "jti" claim can be used
    to prevent the JWT from being replayed.  The "jti" value is a case-
    sensitive string.  Use of this claim is OPTIONAL.

    Args:
        claims (dict): The claims dictionary to validate.
    """
    if "jti" not in claims:
        return

    if not types.is_string(claims["jti"]):
        fail("JWTClaimsError: JWT ID must be a string.")


def _validate_at_hash(claims, access_token, algorithm):
    """
    Validates that the 'at_hash' is valid.

    Its value is the base64url encoding of the left-most half of the hash
    of the octets of the ASCII representation of the access_token value,
    where the hash algorithm used is the hash algorithm used in the alg
    Header Parameter of the ID Token's JOSE Header. For instance, if the
    alg is RS256, hash the access_token value with SHA-256, then take the
    left-most 128 bits and base64url encode them. The at_hash value is a
    case sensitive string.  Use of this claim is OPTIONAL.

    Args:
      claims (dict): The claims dictionary to validate.
      access_token (str): The access token returned by the OpenID Provider.
      algorithm (str): The algorithm used to sign the JWT, as specified by
          the token headers.
    """
    if "at_hash" not in claims:
        return

    if not access_token:
        msg = "No access_token provided to compare against at_hash claim."
        fail("JWTClaimsError: " + msg)

    # Looked up outside the "try" as upstream does not catch its KeyError.
    hash_alg = _at_hash_alg(algorithm)
    _try_9_r = Result.Ok(None).map(lambda _: calculate_at_hash(access_token, hash_alg))
    if not _try_9_r.is_ok:
        msg = "Unable to calculate at_hash to verify against token claims."
        fail("JWTClaimsError: " + msg)
    expected_hash = _try_9_r.unwrap()

    if claims["at_hash"] != expected_hash:
        fail("JWTClaimsError: at_hash claim does not match access_token.")


def _validate_claims(claims, audience=None, issuer=None, subject=None, algorithm=None, access_token=None, options=None,
                     now=None):
    leeway = options.get("leeway", 0)

    required_claims = [e[len("require_"):] for e in options.keys() if e.startswith("require_") and options[e]]
    for require_claim in required_claims:
        if require_claim not in claims:
            fail("JWTError: " + "missing required key \"{}\" among claims".format(require_claim))
        else:
            options["verify_" + require_claim] = True  # override verify when required

    if not (types.is_string(audience) or audience == None):
        fail("JWTError: audience must be a string or None")

    if options.get("verify_iat"):
        _validate_iat(claims)

    if options.get("verify_nbf"):
        _validate_nbf(claims, leeway=leeway, now=now)

    if options.get("verify_exp"):
        _validate_exp(claims, leeway=leeway, now=now)

    if options.get("verify_aud"):
        _validate_aud(claims, audience=audience)

    if options.get("verify_iss"):
        _validate_iss(claims, issuer=issuer)

    if options.get("verify_sub"):
        _validate_sub(claims, subject=subject)

    if options.get("verify_jti"):
        _validate_jti(claims)

    if options.get("verify_at_hash"):
        _validate_at_hash(claims, access_token, algorithm)


jwt = larky.struct(
    __name__="jwt",
    encode=encode,
    decode=decode,
    get_unverified_header=get_unverified_header,
    get_unverified_headers=get_unverified_headers,
    get_unverified_claims=get_unverified_claims,
)
