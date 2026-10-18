# Ported from pycryptodome 3.21.0 (v3.21.0, 5e40a1810e610ae45bb84aa79efe405515337a13): lib/Crypto/Protocol/DH.py. See larky/src/main/resources/VENDORED.md.
#
# pycryptodome is BSD 2-Clause / public domain (see its LICENSE.rst).
#
# Larky differences:
# - Curve448 (import_x448_*) is not ported: Larky's ECC has no Curve448.
# - On the NIST P-curves, Z is computed by @stdlib//ECDH (JCA KeyAgreement
#   "ECDH") instead of pointQ * d, so the static private key is used by a
#   constant-time implementation. Any failure there is "Invalid ECDH point".
# - On Curve25519 the product is BouncyCastle's X25519; an all-zero result
#   (RFC 7748, Section 6.1; RFC 8037, Section 3.2.1) is the point at
#   infinity and fails with "Invalid ECDH point", as upstream does.
# - Errors are fail("<ExceptionName>: <message>") with upstream's messages.

load("@stdlib//ECDH", _ECDH="ECDH")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", types="types")
load("@vendor//Crypto/PublicKey/ECC", ECC="ECC")
load("@vendor//Crypto/Util/number", long_to_bytes="long_to_bytes")
load("@vendor//option/result", Result="Result")

EccKey = ECC.EccKey
construct = ECC.construct
_import_curve25519_public_key = ECC._import_curve25519_public_key


def _compute_ecdh(key_priv, key_pub):
    if key_priv.curve == "Curve25519":
        pointP = key_pub.pointQ * key_priv.d
        if pointP.is_point_at_infinity():
            fail("ValueError: Invalid ECDH point")
        z = long_to_bytes(operator.index(pointP.x), 32)[::-1]
    else:
        # See Section 5.7.1.2 in NIST SP 800-56Ar3
        priv_der = key_priv.export_key(format="DER")
        pub_der = key_pub.public_key().export_key(format="DER")
        res = Result.Ok(_ECDH.key_exchange).map(
            lambda key_exchange: key_exchange(priv_der, "PKCS8", pub_der, "X509", ""))
        if res.is_err:
            fail("ValueError: Invalid ECDH point")
        z = res.unwrap()
    return z


def _require_bytes(fn, encoded):
    if not types.is_bytelike(encoded):
        fail("TypeError: %s(encoded=...) expected `bytes`; got %s" % (fn, type(encoded)))


def import_x25519_public_key(encoded):
    """Create a new X25519 public key object,
    starting from the key encoded as raw ``bytes``,
    in the format described in RFC7748.

    Args:
      encoded (bytes):
        The x25519 public key to import.
        It must be 32 bytes.

    Returns:
      :class:`Crypto.PublicKey.EccKey` : a new ECC key object.

    Raises:
      ValueError: when the given key cannot be parsed.
    """
    _require_bytes("import_x25519_public_key", encoded)
    x = _import_curve25519_public_key(encoded)
    return construct(curve='Curve25519', point_x=x)


def import_x25519_private_key(encoded):
    """Create a new X25519 private key object,
    starting from the key encoded as raw ``bytes``,
    in the format described in RFC7748.

    Args:
      encoded (bytes):
        The X25519 private key to import.
        It must be 32 bytes.

    Returns:
      :class:`Crypto.PublicKey.EccKey` : a new ECC key object.

    Raises:
      ValueError: when the given key cannot be parsed.
    """
    _require_bytes("import_x25519_private_key", encoded)
    return construct(seed=encoded, curve="Curve25519")


def key_agreement(**kwargs):
    """Perform a Diffie-Hellman key agreement.

    Keywords:
      kdf (callable):
        A key derivation function that accepts ``bytes`` as input and returns
        ``bytes``.
      static_priv (EccKey):
        The local static private key. Optional.
      static_pub (EccKey):
        The static public key that belongs to the peer. Optional.
      eph_priv (EccKey):
        The local ephemeral private key, generated for this session. Optional.
      eph_pub (EccKey):
        The ephemeral public key, received from the peer for this session. Optional.

    At least two keys must be passed, of which one is a private key and one
    a public key.

    Returns (bytes):
      The derived secret key material.
    """

    static_priv = kwargs.get('static_priv', None)
    static_pub = kwargs.get('static_pub', None)
    eph_priv = kwargs.get('eph_priv', None)
    eph_pub = kwargs.get('eph_pub', None)
    kdf = kwargs.get('kdf', None)

    if kdf == None:
        fail("ValueError: 'kdf' is mandatory")

    count_priv = 0
    count_pub = 0
    curve = None

    def check_curve(curve, key, name, private):
        if not builtins.isinstance(key, EccKey):
            fail("TypeError: '%s' must be an ECC key" % name)
        if private and not key.has_private():
            fail("TypeError: '%s' must be a private ECC key" % name)
        if curve == None:
            curve = key.curve
        elif curve != key.curve:
            fail("TypeError: '%s' is defined on an incompatible curve" % name)
        return curve

    if static_priv != None:
        curve = check_curve(curve, static_priv, 'static_priv', True)
        count_priv += 1

    if static_pub != None:
        curve = check_curve(curve, static_pub, 'static_pub', False)
        count_pub += 1

    if eph_priv != None:
        curve = check_curve(curve, eph_priv, 'eph_priv', True)
        count_priv += 1

    if eph_pub != None:
        curve = check_curve(curve, eph_pub, 'eph_pub', False)
        count_pub += 1

    if (count_priv + count_pub) < 2 or count_priv == 0 or count_pub == 0:
        fail("ValueError: Too few keys for the ECDH key agreement")

    Zs = b''
    Ze = b''

    if static_priv != None and static_pub != None:
        # C(*, 2s)
        Zs = _compute_ecdh(static_priv, static_pub)

    if eph_priv != None and eph_pub != None:
        # C(2e, 0s) or C(2e, 2s)
        if (static_priv != None) != (static_pub != None):
            fail("ValueError: DH mode C(2e, 1s) is not supported")
        Ze = _compute_ecdh(eph_priv, eph_pub)
    elif eph_priv != None and static_pub != None:
        # C(1e, 2s) or C(1e, 1s)
        Ze = _compute_ecdh(eph_priv, static_pub)
    elif eph_pub != None and static_priv != None:
        # C(1e, 2s) or C(1e, 1s)
        Ze = _compute_ecdh(static_priv, eph_pub)

    Z = Ze + Zs

    return kdf(Z)


DH = larky.struct(
    __name__='DH',
    key_agreement=key_agreement,
    import_x25519_public_key=import_x25519_public_key,
    import_x25519_private_key=import_x25519_private_key,
)
