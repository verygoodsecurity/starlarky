def DsaKey(object):
    """
    r"""Class defining an actual DSA key.
        Do not instantiate directly.
        Use :func:`generate`, :func:`construct` or :func:`import_key` instead.

        :ivar p: DSA modulus
        :vartype p: integer

        :ivar q: Order of the subgroup
        :vartype q: integer

        :ivar g: Generator
        :vartype g: integer

        :ivar y: Public key
        :vartype y: integer

        :ivar x: Private key
        :vartype x: integer

        :undocumented: exportKey, publickey
    
    """
    def __init__(self, key_dict):
        """
        'y'
        """
    def _sign(self, m, k):
        """
        DSA public key cannot be used for signing
        """
    def _verify(self, m, sig):
        """
        'y'
        """
    def has_private(self):
        """
        Whether this is a DSA private key
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
        A matching DSA public key.

                Returns:
                    a new :class:`DsaKey` object
        
        """
    def __eq__(self, other):
        """
         DSA key is not pickable

        """
    def domain(self):
        """
        The DSA domain parameters.

                Returns
                    tuple : (p,q,g)
        
        """
    def __repr__(self):
        """
        'p'
        """
    def __getattr__(self, item):
        """
        'PEM'
        """
            def func(x):
                """
                b'ssh-dss'
                """
    def sign(self, M, K):
        """
        Use module Crypto.Signature.DSS instead
        """
    def verify(self, M, signature):
        """
        Use module Crypto.Signature.DSS instead
        """
    def encrypt(self, plaintext, K):
        """
        Generate a new set of DSA domain parameters
        """
def generate(bits, randfunc=None, domain=None):
    """
    Generate a new DSA key pair.

        The algorithm follows Appendix A.1/A.2 and B.1 of `FIPS 186-4`_,
        respectively for domain generation and key pair generation.

        Args:
          bits (integer):
            Key length, or size (in bits) of the DSA modulus *p*.
            It must be 1024, 2048 or 3072.

          randfunc (callable):
            Random number generation function; it accepts a single integer N
            and return a string of random data N bytes long.
            If not specified, :func:`Crypto.Random.get_random_bytes` is used.

          domain (tuple):
            The DSA domain parameters *p*, *q* and *g* as a list of 3
            integers. Size of *p* and *q* must comply to `FIPS 186-4`_.
            If not specified, the parameters are created anew.

        Returns:
          :class:`DsaKey` : a new DSA key object

        Raises:
          ValueError : when **bits** is too little, too big, or not a multiple of 64.

        .. _FIPS 186-4: http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf
    
    """
def construct(tup, consistency_check=True):
    """
    Construct a DSA key from a tuple of valid DSA components.

        Args:
          tup (tuple):
            A tuple of long integers, with 4 or 5 items
            in the following order:

                1. Public key (*y*).
                2. Sub-group generator (*g*).
                3. Modulus, finite field order (*p*).
                4. Sub-group order (*q*).
                5. Private key (*x*). Optional.

          consistency_check (boolean):
            If ``True``, the library will verify that the provided components
            fulfil the main DSA properties.

        Raises:
          ValueError: when the key being imported fails the most basic DSA validity checks.

        Returns:
          :class:`DsaKey` : a DSA key object
    
    """
def _import_openssl_private(encoded, passphrase, params):
    """
    DSA private key already comes with parameters
    """
def _import_subjectPublicKeyInfo(encoded, passphrase, params):
    """
    No DSA subjectPublicKeyInfo
    """
def _import_x509_cert(encoded, passphrase, params):
    """
    PKCS#8 already includes parameters
    """
def _import_key_der(key_data, passphrase, params):
    """
    Import a DSA key (public or private half), encoded in DER form.
    """
def import_key(extern_key, passphrase=None):
    """
    Import a DSA key.

        Args:
          extern_key (string or byte string):
            The DSA key to import.

            The following formats are supported for a DSA **public** key:

            - X.509 certificate (binary DER or PEM)
            - X.509 ``subjectPublicKeyInfo`` (binary DER or PEM)
            - OpenSSH (ASCII one-liner, see `RFC4253`_)

            The following formats are supported for a DSA **private** key:

            - `PKCS#8`_ ``PrivateKeyInfo`` or ``EncryptedPrivateKeyInfo``
              DER SEQUENCE (binary or PEM)
            - OpenSSL/OpenSSH custom format (binary or PEM)

            For details about the PEM encoding, see `RFC1421`_/`RFC1423`_.

          passphrase (string):
            In case of an encrypted private key, this is the pass phrase
            from which the decryption key is derived.

            Encryption may be applied either at the `PKCS#8`_ or at the PEM level.

        Returns:
          :class:`DsaKey` : a DSA key object

        Raises:
          ValueError : when the given key cannot be parsed (possibly because
            the pass phrase is wrong).

        .. _RFC1421: http://www.ietf.org/rfc/rfc1421.txt
        .. _RFC1423: http://www.ietf.org/rfc/rfc1423.txt
        .. _RFC4253: http://www.ietf.org/rfc/rfc4253.txt
        .. _PKCS#8: http://www.ietf.org/rfc/rfc5208.txt
    
    """
