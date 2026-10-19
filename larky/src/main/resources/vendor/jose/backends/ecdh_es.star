# Larky addition, not ported: python-jose has no ECDH-ES. ECDH-ES key
# agreement for JWE (RFC 7518, Section 4.6) on the NIST P-curves and on
# X25519 (RFC 8037, Section 3.2), used by jose/jwe.star.
#
# - Z comes from Crypto.Protocol.DH.key_agreement (BouncyCastle X25519, or
#   JCA ECDH on the P-curves).
# - The key is derived with the Concat KDF of NIST SP 800-56A (single-step,
#   SHA-256) as RFC 7518, Section 4.6.2 profiles it.
#
# Every failure is fail() with one of the fixed messages below, naming the
# check that failed. None of them includes key material or header values.

load("@stdlib//codecs", codecs="codecs")
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//struct", struct="struct")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Hash/SHA256", SHA256="SHA256")
load("@vendor//Crypto/Protocol/DH", DH="DH")
load("@vendor//Crypto/PublicKey/ECC", ECC="ECC")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long", long_to_bytes="long_to_bytes")
load("@vendor//jose/constants", ALGORITHMS="ALGORITHMS")
load("@vendor//jose/utils", base64url_decode="base64url_decode", base64url_encode="base64url_encode")
load("@vendor//option/result", Result="Result")

ERR_EPK = "JWEError: ECDH-ES epk header is malformed"
ERR_CURVE = "JWEError: ECDH-ES epk is not on the curve of the key"
ERR_ZERO = "JWEError: ECDH-ES shared secret is all zero"
ERR_AGREEMENT = "JWEError: ECDH-ES key agreement failed"
ERR_PARTY_INFO = "JWEError: ECDH-ES apu or apv header is malformed"
ERR_KEY = "JWEError: ECDH-ES needs an EC (P-256, P-384, P-521) or OKP (X25519) key"
ERR_PRIVATE_KEY = "JWEError: ECDH-ES decryption needs a private key"

# JWK "crv" -> (Crypto.PublicKey.ECC curve name, coordinate size in bytes)
_EC_CURVES = {
    "P-256": ("NIST P-256", 32),
    "P-384": ("NIST P-384", 48),
    "P-521": ("NIST P-521", 66),
}
_OKP_CURVES = {
    "X25519": ("Curve25519", 32),
}
# EccKey.curve -> JWK (kty, crv)
_JWK_CURVE = {
    "NIST P-256": ("EC", "P-256"),
    "NIST P-384": ("EC", "P-384"),
    "NIST P-521": ("EC", "P-521"),
    "Curve25519": ("OKP", "X25519"),
}

_KW_KEY_BITS = {
    ALGORITHMS.ECDH_ES_A128KW: 128,
    ALGORITHMS.ECDH_ES_A192KW: 192,
    ALGORITHMS.ECDH_ES_A256KW: 256,
}
_ENC_KEY_BITS = {
    ALGORITHMS.A128GCM: 128,
    ALGORITHMS.A192GCM: 192,
    ALGORITHMS.A256GCM: 256,
    ALGORITHMS.A128CBC_HS256: 256,
    ALGORITHMS.A192CBC_HS384: 384,
    ALGORITHMS.A256CBC_HS512: 512,
}


def _u32(n):
    return struct.pack(">I", n)


def concat_kdf(z, keydatalen, algorithm_id, party_u_info=b"", party_v_info=b""):
    """The Concat KDF of NIST SP 800-56A (Section 5.8.1) with SHA-256, as
    RFC 7518, Section 4.6.2 profiles it for ECDH-ES.

    Args:
        z (bytes): The shared secret Z.
        keydatalen (int): The number of bits to derive (a multiple of 8).
        algorithm_id (bytes or str): The "enc" value for ECDH-ES, or the
            "alg" value for ECDH-ES+A*KW.
        party_u_info (bytes): The decoded "apu" value, or empty.
        party_v_info (bytes): The decoded "apv" value, or empty.

    Each of AlgorithmID, PartyUInfo and PartyVInfo is prefixed with its
    length as a 32-bit big-endian integer, and SuppPubInfo is keydatalen as
    a 32-bit big-endian integer. SuppPrivInfo is empty.

    Returns:
        bytes: keydatalen // 8 bytes of derived key.
    """
    if not types.is_bytelike(z):
        fail("TypeError: concat_kdf(z=...) expected `bytes`; got %s" % type(z))
    if not types.is_int(keydatalen) or keydatalen <= 0 or keydatalen % 8 != 0:
        fail("ValueError: concat_kdf(keydatalen=...) expected a positive multiple of 8")
    if types.is_string(algorithm_id):
        algorithm_id = codecs.encode(algorithm_id, encoding="utf-8")
    for name, value in (("algorithm_id", algorithm_id),
                        ("party_u_info", party_u_info),
                        ("party_v_info", party_v_info)):
        if not types.is_bytelike(value):
            fail("TypeError: concat_kdf(%s=...) expected `bytes`; got %s" % (name, type(value)))

    other_info = (_u32(len(algorithm_id)) + algorithm_id +
                  _u32(len(party_u_info)) + party_u_info +
                  _u32(len(party_v_info)) + party_v_info +
                  _u32(keydatalen))
    out = b""
    for counter in range(1, (keydatalen + 255) // 256 + 1):
        out += SHA256.new(_u32(counter) + z + other_info).digest()
    return out[:keydatalen // 8]


def key_bits(alg, enc):
    """keydatalen: the key size of enc for ECDH-ES, of the AES key wrap for ECDH-ES+A*KW."""
    if alg == ALGORITHMS.ECDH_ES:
        if enc not in _ENC_KEY_BITS:
            fail("JWEError: Encryption algorithm not supported.")
        return _ENC_KEY_BITS[enc]
    if alg not in _KW_KEY_BITS:
        fail("JWEError: Algorithm not supported.")
    return _KW_KEY_BITS[alg]


def algorithm_id(alg, enc):
    """AlgorithmID: enc for Direct Key Agreement, alg for Key Agreement with Key Wrapping."""
    return enc if alg == ALGORITHMS.ECDH_ES else alg


_B64URL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"


def _b64_member(obj, name, error):
    # base64url without padding (RFC 7515, Section 2); the decoder would
    # skip characters outside the alphabet.
    value = obj.get(name)
    if not types.is_string(value) or len(value) % 4 == 1:
        fail(error)
    for c in value.elems():
        if c not in _B64URL_ALPHABET:
            fail(error)
    # Any length but 4k+1 over this alphabet decodes (spare low bits of the
    # last character are ignored), so this cannot fail.
    return base64url_decode(codecs.encode(value, encoding="ascii"))


def party_info(header, name):
    """The decoded "apu" or "apv" header value, or b"" when absent."""
    if name not in header:
        return b""
    return _b64_member(header, name, ERR_PARTY_INFO)


def import_epk(epk):
    """The EccKey of an "epk" header value (a public JWK).

    Only {"kty": "OKP", "crv": "X25519"} and {"kty": "EC", "crv": "P-256",
    "P-384" or "P-521"} are accepted. A P-curve point must be on its curve.
    An X25519 key that would give an all-zero shared secret (a point of small
    order, RFC 7748, Section 6.1) fails with ERR_ZERO.
    """
    if not types.is_dict(epk) or "d" in epk:
        fail(ERR_EPK)
    kty = epk.get("kty")
    crv = epk.get("crv")
    if kty == "OKP" and crv in _OKP_CURVES:
        curve_name, size = _OKP_CURVES[crv]
        x = _b64_member(epk, "x", ERR_EPK)
        if len(x) != size:
            fail(ERR_EPK)
        res = Result.Ok(x).map(lambda raw: DH.import_x25519_public_key(raw))
        if res.is_err:
            fail(ERR_ZERO)
        return res.unwrap()
    if kty == "EC" and crv in _EC_CURVES:
        curve_name, size = _EC_CURVES[crv]
        x = _b64_member(epk, "x", ERR_EPK)
        y = _b64_member(epk, "y", ERR_EPK)
        if len(x) != size or len(y) != size:
            fail(ERR_EPK)
        res = Result.Ok(curve_name).map(lambda name: ECC.construct(
            curve=name, point_x=bytes_to_long(x), point_y=bytes_to_long(y)))
        if res.is_err:
            fail(ERR_EPK)
        return res.unwrap()
    fail(ERR_EPK)


def epk_to_jwk(key):
    """The public JWK of an EccKey, for the "epk" header."""
    if key.curve not in _JWK_CURVE:
        fail(ERR_KEY)
    kty, crv = _JWK_CURVE[key.curve]
    pub = key.public_key()
    if kty == "OKP":
        x = pub.export_key(format="raw")
        return {"kty": kty, "crv": crv,
                "x": codecs.decode(base64url_encode(x), encoding="ascii")}
    size = _EC_CURVES[crv][1]
    return {"kty": kty, "crv": crv,
            "x": codecs.decode(base64url_encode(long_to_bytes(operator.index(pub.pointQ.x), size)), encoding="ascii"),
            "y": codecs.decode(base64url_encode(long_to_bytes(operator.index(pub.pointQ.y), size)), encoding="ascii")}


def _agree(priv, pub):
    res = Result.Ok(priv).map(lambda k: DH.key_agreement(static_priv=k, static_pub=pub, kdf=lambda z: z))
    if res.is_err:
        fail(ERR_ZERO if priv.curve == "Curve25519" else ERR_AGREEMENT)
    return res.unwrap()


def _check_key(key):
    if not types.is_instance(key, ECC.EccKey) or key.curve not in _JWK_CURVE:
        fail(ERR_KEY)


def derive_for_decrypt(key, header, alg, enc):
    """The agreed-upon key of a JWE recipient: the CEK for ECDH-ES, the
    key-wrapping key for ECDH-ES+A*KW.

    Args:
        key (EccKey): The recipient's private key.
        header (dict): The JWE protected header ("epk", optional "apu"/"apv").
        alg (str), enc (str): The header's "alg" and "enc".
    """
    _check_key(key)
    if not key.has_private():
        fail(ERR_PRIVATE_KEY)
    bits = key_bits(alg, enc)
    epk = import_epk(header.get("epk"))
    if epk.curve != key.curve:
        fail(ERR_CURVE)
    apu = party_info(header, "apu")
    apv = party_info(header, "apv")
    z = _agree(key, epk)
    return concat_kdf(z, bits, algorithm_id(alg, enc), apu, apv)


def derive_for_encrypt(key, alg, enc, apu=b"", apv=b""):
    """Generate an ephemeral key on the recipient's curve and agree on a key.

    Args:
        key (EccKey): The recipient's key (its public part is used).
        alg (str), enc (str): The JWE "alg" and "enc".
        apu (bytes), apv (bytes): The decoded "apu" and "apv" values, if any.

    Returns:
        (bytes, dict): The agreed-upon key and the "epk" header value.
    """
    _check_key(key)
    bits = key_bits(alg, enc)
    eph = ECC.generate(curve=key.curve)
    z = _agree(eph, key.public_key())
    return concat_kdf(z, bits, algorithm_id(alg, enc), apu, apv), epk_to_jwk(eph)


ecdh_es = larky.struct(
    __name__='ecdh_es',
    concat_kdf=concat_kdf,
    key_bits=key_bits,
    algorithm_id=algorithm_id,
    party_info=party_info,
    import_epk=import_epk,
    epk_to_jwk=epk_to_jwk,
    derive_for_decrypt=derive_for_decrypt,
    derive_for_encrypt=derive_for_encrypt,
    ERR_EPK=ERR_EPK,
    ERR_CURVE=ERR_CURVE,
    ERR_ZERO=ERR_ZERO,
    ERR_AGREEMENT=ERR_AGREEMENT,
    ERR_PARTY_INFO=ERR_PARTY_INFO,
    ERR_KEY=ERR_KEY,
    ERR_PRIVATE_KEY=ERR_PRIVATE_KEY,
)
