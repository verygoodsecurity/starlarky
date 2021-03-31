def init_p256():
    """
    Error %d initializing P-256 context
    """
def init_p384():
    """
    Error %d initializing P-384 context
    """
def init_p521():
    """
    Error %d initializing P-521 context
    """
def UnsupportedEccFeature(ValueError):
    """
    A class to abstract a point over an Elliptic Curve.

        The class support special methods for:

        * Adding two points: ``R = S + T``
        * In-place addition: ``S += T``
        * Negating a point: ``R = -T``
        * Comparing two points: ``if S == T: ...``
        * Multiplying a point by a scalar: ``R = S*k``
        * In-place multiplication by a scalar: ``T *= k``

        :ivar x: The affine X-coordinate of the ECC point
        :vartype x: integer

        :ivar y: The affine Y-coordinate of the ECC point
        :vartype y: integer

        :ivar xy: The tuple with X- and Y- coordinates
    
    """
    def __init__(self, x, y, curve="p256"):
        """
        Unknown curve name %s
        """
    def set(self, point):
        """
        Error %d while cloning an EC point
        """
    def __eq__(self, point):
        """
        Error %d while inverting an EC point
        """
    def copy(self):
        """
        Return a copy of this point.
        """
    def is_point_at_infinity(self):
        """
        ``True`` if this is the point-at-infinity.
        """
    def point_at_infinity(self):
        """
        Return the point-at-infinity for the curve this point is on.
        """
    def x(self):
        """
        Error %d while encoding an EC point
        """
    def size_in_bytes(self):
        """
        Size of each coordinate, in bytes.
        """
    def size_in_bits(self):
        """
        Size of each coordinate, in bits.
        """
    def double(self):
        """
        Double this point (in-place operation).

                :Return:
                    :class:`EccPoint` : this same object (to enable chaining)
        
        """
    def __iadd__(self, point):
        """
        Add a second point to this one
        """
    def __add__(self, point):
        """
        Return a new point, the addition of this one and another
        """
    def __imul__(self, scalar):
        """
        Multiply this point by a scalar
        """
    def __mul__(self, scalar):
        """
        Return a new point, the scalar product of this one
        """
    def __rmul__(self, left_hand):
        """
         Last piece of initialization

        """
def EccKey(object):
    """
    r"""Class defining an ECC key.
        Do not instantiate directly.
        Use :func:`generate`, :func:`construct` or :func:`import_key` instead.

        :ivar curve: The name of the ECC as defined in :numref:`curve_names`.
        :vartype curve: string

        :ivar pointQ: an ECC point representating the public component
        :vartype pointQ: :class:`EccPoint`

        :ivar d: A scalar representating the private component
        :vartype d: integer
    
    """
    def __init__(self, **kwargs):
        """
        Create a new ECC key

                Keywords:
                  curve : string
                    It must be *"p256"*, *"P-256"*, *"prime256v1"* or *"secp256r1"*.
                  d : integer
                    Only for a private key. It must be in the range ``[1..order-1]``.
                  point : EccPoint
                    Mandatory for a public key. If provided for a private key,
                    the implementation will NOT check whether it matches ``d``.
        
        """
    def __eq__(self, other):
        """
        , d=%d
        """
    def has_private(self):
        """
        ``True`` if this key can be used for making signatures or decrypting data.
        """
    def _sign(self, z, k):
        """
        This is not a private ECC key
        """
    def pointQ(self):
        """
        A matching ECC public key.

                Returns:
                    a new :class:`EccKey` object
        
        """
    def _export_subjectPublicKeyInfo(self, compress):
        """
         See 2.2 in RFC5480 and 2.3.3 in SEC1
         The first byte is:
         - 0x02:   compressed, only X-coordinate, Y-coordinate is even
         - 0x03:   compressed, only X-coordinate, Y-coordinate is odd
         - 0x04:   uncompressed, X-coordinate is followed by Y-coordinate

         PAI is in theory encoded as 0x00.


        """
    def _export_private_der(self, include_ec_params=True):
        """
         ECPrivateKey ::= SEQUENCE {
                   version        INTEGER { ecPrivkeyVer1(1) } (ecPrivkeyVer1),
                   privateKey     OCTET STRING,
                   parameters [0] ECParameters {{ NamedCurve }} OPTIONAL,
                   publicKey  [1] BIT STRING OPTIONAL
            }

         Public key - uncompressed form

        """
    def _export_pkcs8(self, **kwargs):
        """
        'passphrase'
        """
    def _export_public_pem(self, compress):
        """
        PUBLIC KEY
        """
    def _export_private_pem(self, passphrase, **kwargs):
        """
        EC PRIVATE KEY
        """
    def _export_private_clear_pkcs8_in_clear_pem(self):
        """
        PRIVATE KEY
        """
    def _export_private_encrypted_pkcs8_in_clear_pem(self, passphrase, **kwargs):
        """
        'protection'
        """
    def _export_openssh(self, compress):
        """
        Cannot export OpenSSH private keys
        """
    def export_key(self, **kwargs):
        """
        Export this ECC key.

                Args:
                  format (string):
                    The format to use for encoding the key:

                    - ``'DER'``. The key will be encoded in ASN.1 DER format (binary).
                      For a public key, the ASN.1 ``subjectPublicKeyInfo`` structure
                      defined in `RFC5480`_ will be used.
                      For a private key, the ASN.1 ``ECPrivateKey`` structure defined
                      in `RFC5915`_ is used instead (possibly within a PKCS#8 envelope,
                      see the ``use_pkcs8`` flag below).
                    - ``'PEM'``. The key will be encoded in a PEM_ envelope (ASCII).
                    - ``'OpenSSH'``. The key will be encoded in the OpenSSH_ format
                      (ASCII, public keys only).

                  passphrase (byte string or string):
                    The passphrase to use for protecting the private key.

                  use_pkcs8 (boolean):
                    Only relevant for private keys.

                    If ``True`` (default and recommended), the `PKCS#8`_ representation
                    will be used.

                    If ``False``, the much weaker `PEM encryption`_ mechanism will be used.

                  protection (string):
                    When a private key is exported with password-protection
                    and PKCS#8 (both ``DER`` and ``PEM`` formats), this parameter MUST be
                    present and be a valid algorithm supported by :mod:`Crypto.IO.PKCS8`.
                    It is recommended to use ``PBKDF2WithHMAC-SHA1AndAES128-CBC``.

                  compress (boolean):
                    If ``True``, a more compact representation of the public key
                    with the X-coordinate only is used.

                    If ``False`` (default), the full public key will be exported.

                .. warning::
                    If you don't provide a passphrase, the private key will be
                    exported in the clear!

                .. note::
                    When exporting a private key with password-protection and `PKCS#8`_
                    (both ``DER`` and ``PEM`` formats), any extra parameters
                    to ``export_key()`` will be passed to :mod:`Crypto.IO.PKCS8`.

                .. _PEM:        http://www.ietf.org/rfc/rfc1421.txt
                .. _`PEM encryption`: http://www.ietf.org/rfc/rfc1423.txt
                .. _`PKCS#8`:   http://www.ietf.org/rfc/rfc5208.txt
                .. _OpenSSH:    http://www.openssh.com/txt/rfc5656.txt
                .. _RFC5480:    https://tools.ietf.org/html/rfc5480
                .. _RFC5915:    http://www.ietf.org/rfc/rfc5915.txt

                Returns:
                    A multi-line string (for PEM and OpenSSH) or bytes (for DER) with the encoded key.
        
        """
def generate(**kwargs):
    """
    Generate a new private key on the given curve.

        Args:

          curve (string):
            Mandatory. It must be a curve name defined in :numref:`curve_names`.

          randfunc (callable):
            Optional. The RNG to read randomness from.
            If ``None``, :func:`Crypto.Random.get_random_bytes` is used.
    
    """
def construct(**kwargs):
    """
    Build a new ECC key (private or public) starting
        from some base components.

        Args:

          curve (string):
            Mandatory. It must be a curve name defined in :numref:`curve_names`.

          d (integer):
            Only for a private key. It must be in the range ``[1..order-1]``.

          point_x (integer):
            Mandatory for a public key. X coordinate (affine) of the ECC point.

          point_y (integer):
            Mandatory for a public key. Y coordinate (affine) of the ECC point.

        Returns:
          :class:`EccKey` : a new ECC key object
    
    """
def _import_public_der(curve_oid, ec_point):
    """
    Convert an encoded EC point into an EccKey object

        curve_name: string with the OID of the curve
        ec_point: byte string with the EC point (not DER encoded)

    
    """
def _import_subjectPublicKeyInfo(encoded, *kwargs):
    """
    Convert a subjectPublicKeyInfo into an EccKey object
    """
def _import_private_der(encoded, passphrase, curve_oid=None):
    """
     See RFC5915 https://tools.ietf.org/html/rfc5915

     ECPrivateKey ::= SEQUENCE {
               version        INTEGER { ecPrivkeyVer1(1) } (ecPrivkeyVer1),
               privateKey     OCTET STRING,
               parameters [0] ECParameters {{ NamedCurve }} OPTIONAL,
               publicKey  [1] BIT STRING OPTIONAL
        }


    """
def _import_pkcs8(encoded, passphrase):
    """
     From RFC5915, Section 1:

     Distributing an EC private key with PKCS#8 [RFC5208] involves including:
     a) id-ecPublicKey, id-ecDH, or id-ecMQV (from [RFC5480]) with the
        namedCurve as the parameters in the privateKeyAlgorithm field; and
     b) ECPrivateKey in the PrivateKey field, which is an OCTET STRING.


    """
def _import_x509_cert(encoded, *kwargs):
    """
    Not an ECC DER key
    """
def _import_openssh_public(encoded):
    """
    b' '
    """
def _import_openssh_private_ecc(data, password):
    """
    Unsupported ECC curve %s
    """
def import_key(encoded, passphrase=None):
    """
    Import an ECC key (public or private).

        Args:
          encoded (bytes or multi-line string):
            The ECC key to import.

            An ECC **public** key can be:

            - An X.509 certificate, binary (DER) or ASCII (PEM)
            - An X.509 ``subjectPublicKeyInfo``, binary (DER) or ASCII (PEM)
            - An OpenSSH line (e.g. the content of ``~/.ssh/id_ecdsa``, ASCII)

            An ECC **private** key can be:

            - In binary format (DER, see section 3 of `RFC5915`_ or `PKCS#8`_)
            - In ASCII format (PEM or `OpenSSH 6.5+`_)

            Private keys can be in the clear or password-protected.

            For details about the PEM encoding, see `RFC1421`_/`RFC1423`_.

          passphrase (byte string):
            The passphrase to use for decrypting a private key.
            Encryption may be applied protected at the PEM level or at the PKCS#8 level.
            This parameter is ignored if the key in input is not encrypted.

        Returns:
          :class:`EccKey` : a new ECC key object

        Raises:
          ValueError: when the given key cannot be parsed (possibly because
            the pass phrase is wrong).

        .. _RFC1421: http://www.ietf.org/rfc/rfc1421.txt
        .. _RFC1423: http://www.ietf.org/rfc/rfc1423.txt
        .. _RFC5915: http://www.ietf.org/rfc/rfc5915.txt
        .. _`PKCS#8`: http://www.ietf.org/rfc/rfc5208.txt
        .. _`OpenSSH 6.5+`: https://flak.tedunangst.com/post/new-openssh-key-format-and-bcrypt-pbkdf
    
    """
