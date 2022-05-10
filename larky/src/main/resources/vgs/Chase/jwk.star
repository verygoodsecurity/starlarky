load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")

load("@vgs//chase", Chase="chase")


def get_public_keys():
    """
    This function retrieves the public JWKs that Chase should use
    for payload encryption.
    """
    keys = Chase.get_keys()
    return keys


def decrypt(jwe_bytes):
    """
    The decrypt() function takes a compact JWE from Chase, and decrypts it.

    Arguments:
        jwe_bytes:  This is the encrypted payload from Chase.
                    It can be a string or bytestring.
    """
    if types.is_string(jwe_bytes):
        jwe_bytes = bytes(jwe_bytes, "utf-8")
    decrypted = Chase.decrypt(jwe_bytes)
    return decrypted


jwk = larky.struct(
    __name__="jwk", 
    get_public_keys=get_public_keys, 
    decrypt=decrypt)
