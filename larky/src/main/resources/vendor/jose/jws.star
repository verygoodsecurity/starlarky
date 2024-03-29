load("@stdlib//binascii", binascii="binascii")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//json", json="json")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//jose/jwk", jwk="jwk")
load("@vendor//jose/backends/base", Key="Key")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//jose/exceptions", JWSError="JWSError", JWSSignatureError="JWSSignatureError")
load("@vendor//jose/utils", base64url_encode="base64url_encode", base64url_decode="base64url_decode")
load("@vendor//option/result", Result="Result", Error="Error")
load("@vendor//six", six="six")


def sign(payload, key, headers=None, algorithm=ALGORITHMS.HS256):
    """Signs a claims set and returns a JWS string.

    Args:
        payload (str or dict): A string to sign
        key (str or dict): The key to use for signing the claim set. Can be
            individual JWK or JWK set.
        headers (dict, optional): A set of headers that will be added to
            the default headers.  Any headers that are added as additional
            headers will override the default headers.
        algorithm (str, optional): The algorithm to use for signing the
            the claims.  Defaults to HS256.

    Returns:
        str: The string representation of the header, claims, and signature.

    Raises:
        JWSError: If there is an error signing the token.

    Examples:

        >>> jws.sign({'a': 'b'}, 'secret', algorithm='HS256')
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYiJ9.jiMyrsmD8AoHWeQgmxZ5yq8z0lXS67_QGs52AzC8Ru8'

    """

    if algorithm not in ALGORITHMS.SUPPORTED:
        fail("JWSError: 'Algorithm %s not supported.'" % algorithm)

    encoded_header = _encode_header(algorithm, additional_headers=headers)
    encoded_payload = _encode_payload(payload)
    signed_output = _sign_header_and_claims(encoded_header, encoded_payload, algorithm, key)

    return signed_output


def verify(token, key, algorithms, verify=True):
    """Verifies a JWS string's signature.

    Args:
        token (str): A signed JWS to be verified.
        key (str or dict): A key to attempt to verify the payload with. Can be
            individual JWK or JWK set.
        algorithms (str or list): Valid algorithms that should be used to verify the JWS.

    Returns:
        str: The str representation of the payload, assuming the signature is valid.

    Raises:
        JWSError: If there is an exception verifying a token.

    Examples:

        >>> token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYiJ9.jiMyrsmD8AoHWeQgmxZ5yq8z0lXS67_QGs52AzC8Ru8'
        >>> jws.verify(token, 'secret', algorithms='HS256')

    """

    header, payload, signing_input, signature = _load(token)

    if verify:
        _verify_signature(signing_input, header, signature, key, algorithms)

    return payload


def get_unverified_header(token):
    """Returns the decoded headers without verification of any kind.

    Args:
        token (str): A signed JWS to decode the headers from.

    Returns:
        dict: The dict representation of the token headers.

    Raises:
        JWSError: If there is an exception decoding the token.
    """
    header, claims, signing_input, signature = _load(token)
    return header


def get_unverified_headers(token):
    """Returns the decoded headers without verification of any kind.

    This is simply a wrapper of get_unverified_header() for backwards
    compatibility.

    Args:
        token (str): A signed JWS to decode the headers from.

    Returns:
        dict: The dict representation of the token headers.

    Raises:
        JWSError: If there is an exception decoding the token.
    """
    return get_unverified_header(token)


def get_unverified_claims(token):
    """Returns the decoded claims without verification of any kind.

    Args:
        token (str): A signed JWS to decode the headers from.

    Returns:
        str: The str representation of the token claims.

    Raises:
        JWSError: If there is an exception decoding the token.
    """
    header, claims, signing_input, signature = _load(token)
    return claims


def _encode_header(algorithm, additional_headers=None):
    header = {
        "typ": "JWT",
        "alg": algorithm
    }

    if additional_headers:
        header.update(additional_headers)

    sorted_keys = sorted(header.keys())
    sorted_headers = {}
    for sk in sorted_keys:
        sorted_headers[sk] = header[sk]

    json_header = bytes(
        Result.Ok(json.dumps)
            .map(lambda x: x(sorted_headers))
            .unwrap()
            .replace(', ', ',')
            .replace(': ', ':'),
        encoding='utf-8'
    )

    return base64url_encode(json_header)


def _encode_payload(payload):
    if types.is_dict(payload):
        payload = bytes(
            Result.Ok(json.dumps)
                .map(lambda x: x(payload))
                .unwrap()
                .replace(': ', ':')
                .replace(', ', ','),
            encoding='utf-8'
        )

    return base64url_encode(payload)


def _sign_header_and_claims(encoded_header, encoded_claims, algorithm, key):
    signing_input = b'.'.join([encoded_header, encoded_claims])
    if not builtins.isinstance(key, Key):
        key = Result.Ok(jwk.construct).map(lambda x: x(key, algorithm)).unwrap()
    signature = Result.Ok(key.sign).map(lambda x: x(signing_input)).unwrap()

    encoded_signature = base64url_encode(signature)

    encoded_string = b'.'.join([encoded_header, encoded_claims, encoded_signature])

    return encoded_string.decode('utf-8')


def _load(jwt):
    if types.is_string(jwt):
        jwt = bytes(jwt, encoding='utf-8')
    signing_input, crypto_segment = (
        Result.Ok(jwt.rsplit)
            .map(lambda x: x(b'.', 1))
            .expect("JWSError: Not enough segments"))
    header_segment, claims_segment = (
        Result.Ok(signing_input.split)
            .map(lambda x: x(b'.', 1))
            .expect("JWSError: Not enough segments"))
    header_data = (
        Result.Ok(base64url_decode)
            .map(lambda x: x(header_segment))
            .expect("JWSError: Invalid header padding"))

    header = (Result.Ok(json.loads)
              .map(lambda x: x(header_data.decode('utf-8')))
              .expect("JWSError: 'Invalid header string: %r'" % header_data))

    if not types.is_dict(header):
        fail("JWSError: Invalid header string: must be a json object")

    payload = (Result.Ok(base64url_decode)
               .map(lambda x: x(claims_segment))
               .expect("JWSError: Invalid payload padding"))

    signature = (Result.Ok(base64url_decode)
                 .map(lambda x: x(crypto_segment))
                 .expect("JWSError: Invalid crypto padding"))

    return (header, payload, signing_input, signature)


def _sig_matches_keys(keys, signing_input, signature, alg):
    for key in keys:
        if not builtins.isinstance(key, Key):
            key = jwk.construct(key, alg)
        res = Result.Ok(key.verify).map(lambda x: x(signing_input, signature))
        if res.is_ok and res.unwrap():
            return True

    return False


def _get_keys(key):

    if builtins.isinstance(key, Key):
        return (key,)

    rval = Result.Ok(json.loads).map(lambda loads: loads(key))
    if rval.is_ok:
        key = rval.unwrap()

    if types.is_dict(key):
        if 'keys' in key:
            # JWK Set per RFC 7517
            return key['keys']
        elif 'kty' in key:
            # Individual JWK per RFC 7517
            return (key,)
        else:
            # Some other mapping. Firebase uses just dict of kid, cert pairs
            values = key.values()
            if values:
                return values
            return (key,)

    # Iterable but not text or mapping => list- or tuple-like
    elif (types.is_iterable(key) and
          not (types.is_string(key) or types.is_bytelike(key))):
        return key

    # Scalar value, wrap in tuple.
    else:
        return (key,)


def _verify_signature(signing_input, header, signature, key='', algorithms=None):

    alg = header.get('alg')
    if not alg:
        fail("JWSError: No algorithm was specified in the JWS header.")

    if algorithms != None and alg not in algorithms:
        fail("JWSError: The specified alg value is not allowed")

    keys = _get_keys(key)
    rval = Result.Ok(_sig_matches_keys).map(lambda x: x(keys, signing_input, signature, alg))
    if rval.is_ok:
        if rval.unwrap():
            return
        fail("JWSError: Signature verification failed.")
    fail("JWSError: " + 'Invalid or unsupported algorithm: %s (%s)' % (alg, rval._val))


jws = larky.struct(
    __name__='jws',
    get_unverified_claims=get_unverified_claims,
    get_unverified_header=get_unverified_header,
    get_unverified_headers=get_unverified_headers,
    sign=sign,
    verify=verify
)