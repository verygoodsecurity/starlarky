load("@stdlib//base64",
     urlsafe_b64encode="urlsafe_b64encode",
     urlsafe_b64decode="urlsafe_b64decode")
load("@stdlib//types", types="types")


def ensure_binary(s):
    """Coerce **s** to bytes."""

    if types.is_bytes(s):
        return s
    if types.is_string(s):
        return bytes(s, encoding='utf-8')
    fail("not expecting type '{}'".format(type(s)))


def base64url_encode(input):
    """Helper method to base64url_encode a string.

    Args:
        input (str): A base64url_encoded string to encode.

    """
    return urlsafe_b64encode(input).replace('=', '')


def base64url_decode(input):
    """Helper method to base64url_decode a string.

    Args:
        input (str): A base64url_encoded string to decode.

    """
    input = str(input)
    rem = len(input) % 4

    if rem > 0:
        input += "=" * (4 - rem)

    return urlsafe_b64decode(input)
