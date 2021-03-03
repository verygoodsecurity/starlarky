def RsaKey(object):
    """
    r"""Class defining an actual RSA key.
        Do not instantiate directly.
        Use :func:`generate`, :func:`construct` or :func:`import_key` instead.

        :ivar n: RSA modulus
        :vartype n: integer

        :ivar e: RSA public exponent
        :vartype e: integer

        :ivar d: RSA private exponent
        :vartype d: integer

        :ivar p: First factor of the RSA modulus
        :vartype p: integer

        :ivar q: Second factor of the RSA modulus
        :vartype q: integer

        :ivar u: Chinese remainder component (:math:`p^{-1} \text{mod } q`)
        :vartype q: integer

        :undocumented: exportKey, publickey
    
    """
    def __init__(self, **kwargs):
        """
        Build an RSA key.

                :Keywords:
                  n : integer
                    The modulus.
                  e : integer
                    The public exponent.
                  d : integer
                    The private exponent. Only required for private keys.
                  p : integer
                    The first factor of the modulus. Only required for private keys.
                  q : integer
                    The second factor of the modulus. Only required for private keys.
                  u : integer
                    The CRT coefficient (inverse of p modulo q). Only required for
                    private keys.
        
        """
    def n(self):
        """
        No private exponent available for public keys
        """
    def p(self):
        """
        No CRT component 'p' available for public keys
        """
    def q(self):
        """
        No CRT component 'q' available for public keys
        """
    def u(self):
        """
        No CRT component 'u' available for public keys
        """
    def size_in_bits(self):
        """
        Size of the RSA modulus in bits
        """
    def size_in_bytes(self):
        """
        The minimal amount of bytes that can hold the RSA modulus
        """
    def _encrypt(self, plaintext):
        """
        Plaintext too large
        """
    def _decrypt(self, ciphertext):
        """
        Ciphertext too large
        """
    def has_private(self):
        """
        Whether this is an RSA private key
        """
    def can_encrypt(self):  # legacy
        """
         legacy
        """
    def can_sign(self):     # legacy
        """
         legacy
        """
    def public_key(self):
        """
        A matching RSA public key.

                Returns:
                    a new :class:`RsaKey` object
        
        """
    def __eq__(self, other):
        """
         RSA key is not pickable

        """
    def __repr__(self):
        """
        , d=%d, p=%d, q=%d, u=%d
        """
    def __str__(self):
        """
        Private
        """
2021-03-02 17:42:00,633 : INFO : tokenize_signature : --> do i ever get here?
    def export_key(self, format='PEM', passphrase=None, pkcs=1,
                   protection=None, randfunc=None):
        """
        Export this RSA key.

                Args:
                  format (string):
                    The format to use for wrapping the key:

                    - *'PEM'*. (*Default*) Text encoding, done according to `RFC1421`_/`RFC1423`_.
                    - *'DER'*. Binary encoding.
                    - *'OpenSSH'*. Textual encoding, done according to OpenSSH specification.
                      Only suitable for public keys (not private keys).

                  passphrase (string):
                    (*For private keys only*) The pass phrase used for protecting the output.

                  pkcs (integer):
                    (*For private keys only*) The ASN.1 structure to use for
                    serializing the key. Note that even in case of PEM
                    encoding, there is an inner ASN.1 DER structure.

                    With ``pkcs=1`` (*default*), the private key is encoded in a
                    simple `PKCS#1`_ structure (``RSAPrivateKey``).

                    With ``pkcs=8``, the private key is encoded in a `PKCS#8`_ structure
                    (``PrivateKeyInfo``).

                    .. note::
                        This parameter is ignored for a public key.
                        For DER and PEM, an ASN.1 DER ``SubjectPublicKeyInfo``
                        structure is always used.

                  protection (string):
                    (*For private keys only*)
                    The encryption scheme to use for protecting the private key.

                    If ``None`` (default), the behavior depends on :attr:`format`:

                    - For *'DER'*, the *PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC*
                      scheme is used. The following operations are performed:

                        1. A 16 byte Triple DES key is derived from the passphrase
                           using :func:`Crypto.Protocol.KDF.PBKDF2` with 8 bytes salt,
                           and 1 000 iterations of :mod:`Crypto.Hash.HMAC`.
                        2. The private key is encrypted using CBC.
                        3. The encrypted key is encoded according to PKCS#8.

                    - For *'PEM'*, the obsolete PEM encryption scheme is used.
                      It is based on MD5 for key derivation, and Triple DES for encryption.

                    Specifying a value for :attr:`protection` is only meaningful for PKCS#8
                    (that is, ``pkcs=8``) and only if a pass phrase is present too.

                    The supported schemes for PKCS#8 are listed in the
                    :mod:`Crypto.IO.PKCS8` module (see :attr:`wrap_algo` parameter).

                  randfunc (callable):
                    A function that provides random bytes. Only used for PEM encoding.
                    The default is :func:`Crypto.Random.get_random_bytes`.

                Returns:
                  byte string: the encoded key

                Raises:
                  ValueError:when the format is unknown or when you try to encrypt a private
                    key with *DER* format and PKCS#1.

                .. warning::
                    If you don't provide a pass phrase, the private key will be
                    exported in the clear!

                .. _RFC1421:    http://www.ietf.org/rfc/rfc1421.txt
                .. _RFC1423:    http://www.ietf.org/rfc/rfc1423.txt
                .. _`PKCS#1`:   http://www.ietf.org/rfc/rfc3447.txt
                .. _`PKCS#8`:   http://www.ietf.org/rfc/rfc5208.txt
        
        """
    def sign(self, M, K):
        """
        Use module Crypto.Signature.pkcs1_15 instead
        """
    def verify(self, M, signature):
        """
        Use module Crypto.Signature.pkcs1_15 instead
        """
    def encrypt(self, plaintext, K):
        """
        Use module Crypto.Cipher.PKCS1_OAEP instead
        """
    def decrypt(self, ciphertext):
        """
        Use module Crypto.Cipher.PKCS1_OAEP instead
        """
    def blind(self, M, B):
        """
        Create a new RSA key pair.

            The algorithm closely follows NIST `FIPS 186-4`_ in its
            sections B.3.1 and B.3.3. The modulus is the product of
            two non-strong probable primes.
            Each prime passes a suitable number of Miller-Rabin tests
            with random bases and a single Lucas test.

            Args:
              bits (integer):
                Key length, or size (in bits) of the RSA modulus.
                It must be at least 1024, but **2048 is recommended.**
                The FIPS standard only defines 1024, 2048 and 3072.
              randfunc (callable):
                Function that returns random bytes.
                The default is :func:`Crypto.Random.get_random_bytes`.
              e (integer):
                Public RSA exponent. It must be an odd positive integer.
                It is typically a small number with very few ones in its
                binary representation.
                The FIPS standard requires the public exponent to be
                at least 65537 (the default).

            Returns: an RSA key object (:class:`RsaKey`, with private key).

            .. _FIPS 186-4: http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf
    
        """
        def filter_p(candidate):
            """
            r"""Construct an RSA key from a tuple of valid RSA components.

                The modulus **n** must be the product of two primes.
                The public exponent **e** must be odd and larger than 1.

                In case of a private key, the following equations must apply:

                .. math::

                    \begin{align}
                    p*q &= n \\
                    e*d &\equiv 1 ( \text{mod lcm} [(p-1)(q-1)]) \\
                    p*u &\equiv 1 ( \text{mod } q)
                    \end{align}

                Args:
                    rsa_components (tuple):
                        A tuple of integers, with at least 2 and no
                        more than 6 items. The items come in the following order:

                        1. RSA modulus *n*.
                        2. Public exponent *e*.
                        3. Private exponent *d*.
                           Only required if the key is private.
                        4. First factor of *n* (*p*).
                           Optional, but the other factor *q* must also be present.
                        5. Second factor of *n* (*q*). Optional.
                        6. CRT coefficient *q*, that is :math:`p^{-1} \text{mod }q`. Optional.

                    consistency_check (boolean):
                        If ``True``, the library will verify that the provided components
                        fulfil the main RSA properties.

                Raises:
                    ValueError: when the key being imported fails the most basic RSA validity checks.

                Returns: An RSA key object (:class:`RsaKey`).
    
            """
    def InputComps(object):
    """
    'n'
    """
def _import_pkcs1_private(encoded, *kwargs):
    """
     RSAPrivateKey ::= SEQUENCE {
               version Version,
               modulus INTEGER, -- n
               publicExponent INTEGER, -- e
               privateExponent INTEGER, -- d
               prime1 INTEGER, -- p
               prime2 INTEGER, -- q
               exponent1 INTEGER, -- d mod (p-1)
               exponent2 INTEGER, -- d mod (q-1)
               coefficient INTEGER -- (inverse of q) mod p
     }

     Version ::= INTEGER

    """
def _import_pkcs1_public(encoded, *kwargs):
    """
     RSAPublicKey ::= SEQUENCE {
               modulus INTEGER, -- n
               publicExponent INTEGER -- e
     }

    """
def _import_subjectPublicKeyInfo(encoded, *kwargs):
    """
    No RSA subjectPublicKeyInfo
    """
def _import_x509_cert(encoded, *kwargs):
    """
    No PKCS#8 encoded RSA key
    """
def _import_keyDER(extern_key, passphrase):
    """
    Import an RSA key (public or private half), encoded in DER form.
    """
def _import_openssh_private_rsa(data, password):
    """
    ssh-rsa
    """
def import_key(extern_key, passphrase=None):
    """
    Import an RSA key (public or private).

        Args:
          extern_key (string or byte string):
            The RSA key to import.

            The following formats are supported for an RSA **public key**:

            - X.509 certificate (binary or PEM format)
            - X.509 ``subjectPublicKeyInfo`` DER SEQUENCE (binary or PEM
              encoding)
            - `PKCS#1`_ ``RSAPublicKey`` DER SEQUENCE (binary or PEM encoding)
            - An OpenSSH line (e.g. the content of ``~/.ssh/id_ecdsa``, ASCII)

            The following formats are supported for an RSA **private key**:

            - PKCS#1 ``RSAPrivateKey`` DER SEQUENCE (binary or PEM encoding)
            - `PKCS#8`_ ``PrivateKeyInfo`` or ``EncryptedPrivateKeyInfo``
              DER SEQUENCE (binary or PEM encoding)
            - OpenSSH (text format, introduced in `OpenSSH 6.5`_)

            For details about the PEM encoding, see `RFC1421`_/`RFC1423`_.

          passphrase (string or byte string):
            For private keys only, the pass phrase that encrypts the key.

        Returns: An RSA key object (:class:`RsaKey`).

        Raises:
          ValueError/IndexError/TypeError:
            When the given key cannot be parsed (possibly because the pass
            phrase is wrong).

        .. _RFC1421: http://www.ietf.org/rfc/rfc1421.txt
        .. _RFC1423: http://www.ietf.org/rfc/rfc1423.txt
        .. _`PKCS#1`: http://www.ietf.org/rfc/rfc3447.txt
        .. _`PKCS#8`: http://www.ietf.org/rfc/rfc5208.txt
        .. _`OpenSSH 6.5`: https://flak.tedunangst.com/post/new-openssh-key-format-and-bcrypt-pbkdf
    
    """
