
load("@stdlib//binascii", binascii="binascii")
 
load("@vendor//six", six="six")
load("@stdlib//builtins","builtins")
load("@stdlib//json","json")

try:
    load("@stdlib//collections/abc", Mapping="Mapping", Iterable="Iterable")  # Python 3
except ImportError:
    load("@stdlib//collections", Mapping="Mapping", Iterable="Iterable")  # Python 2, will be deprecated in Python 3.8

load("@vendor//jose", jwk="jwk")
load("@vendor//jose/backends/base", Key="Key")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//jose/exceptions", JWSError="JWSError")
load("@vendor//jose/exceptions", JWSSignatureError="JWSSignatureError")
load("@vendor//jose/utils", base64url_encode="base64url_encode")
load("@vendor//jose/utils", base64url_decode="base64url_decode")


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
        fail(" JWSError('Algorithm %s not supported.' % algorithm)")

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

    json_header = json.dumps(
        header,
        separators=(',', ':'),
        sort_keys=True,
    ).encode('utf-8')

    return base64url_encode(json_header)


def _encode_payload(payload):
    if types.is_instance(payload, Mapping):
        try:
            payload = json.dumps(
                payload,
                separators=(',', ':'),
            ).encode('utf-8')
        except ValueError:
            pass

    return base64url_encode(payload)


def _sign_header_and_claims(encoded_header, encoded_claims, algorithm, key):
    signing_input = bytes([0x2e]).join([encoded_header, encoded_claims])
    try:
        if not types.is_instance(key, Key):
            key = jwk.construct(key, algorithm)
        signature = key.sign(signing_input)
    except Exception as e:
        fail(" JWSError(e)")

    encoded_signature = base64url_encode(signature)

    encoded_string = bytes([0x2e]).join([encoded_header, encoded_claims, encoded_signature])

    return encoded_string.decode('utf-8')


def _load(jwt):
    if types.is_instance(jwt, six.text_type):
        jwt = jwt.encode('utf-8')
    try:
        signing_input, crypto_segment = jwt.rsplit(bytes([0x2e]), 1)
        header_segment, claims_segment = signing_input.split(bytes([0x2e]), 1)
        header_data = base64url_decode(header_segment)
    except ValueError:
        fail(" JWSError('Not enough segments')")
    except (TypeError, binascii.Error):
        fail(" JWSError('Invalid header padding')")

    try:
        header = json.loads(header_data.decode('utf-8'))
    except ValueError as e:
        fail(" JWSError('Invalid header string: %s' % e)")

    if not types.is_instance(header, Mapping):
        fail(" JWSError('Invalid header string: must be a json object')")

    try:
        payload = base64url_decode(claims_segment)
    except (TypeError, binascii.Error):
        fail(" JWSError('Invalid payload padding')")

    try:
        signature = base64url_decode(crypto_segment)
    except (TypeError, binascii.Error):
        fail(" JWSError('Invalid crypto padding')")

    return (header, payload, signing_input, signature)


def _sig_matches_keys(keys, signing_input, signature, alg):
    for key in keys:
        if not types.is_instance(key, Key):
            key = jwk.construct(key, alg)
        try:
            if key.verify(signing_input, signature):
                return True
        except Exception:
            pass
    return False


def _get_keys(key):

    if types.is_instance(key, Key):
        return (key,)

    try:
        key = json.loads(key, parse_int=str, parse_float=str)
    except Exception:
        pass

    if types.is_instance(key, Mapping):
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
    elif (types.is_instance(key, Iterable) and
          not (types.is_instance(key, six.string_types) or types.is_instance(key, six.binary_type))):
        return key

    # Scalar value, wrap in tuple.
    else:
        return (key,)


def _verify_signature(signing_input, header, signature, key='', algorithms=None):

    alg = header.get('alg')
    if not alg:
        fail(" JWSError('No algorithm was specified in the JWS header.')")

    if algorithms != None and alg not in algorithms:
        fail(" JWSError('The specified alg value is not allowed')")

    keys = _get_keys(key)
    try:
        if not _sig_matches_keys(keys, signing_input, signature, alg):
            fail(" JWSSignatureError()")
    except JWSSignatureError:
        fail(" JWSError('Signature verification failed.')")
    except JWSError:
        fail(" JWSError('Invalid or unsupported algorithm: %s' % alg)")

