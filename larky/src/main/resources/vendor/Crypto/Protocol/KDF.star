load("@stdlib//larky", larky="larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//types", types="types")


def PBKDF1(password, salt, dkLen, count=1000, hashAlgo=None):
    """
    Derive one key from a password (or passphrase).

        This function performs key derivation according to an old version of
        the PKCS#5 standard (v1.5) or `RFC2898
        <https://www.ietf.org/rfc/rfc2898.txt>`_.

        Args:
         password (string):
            The secret password to generate the key from.
         salt (byte string):
            An 8 byte string to use for better protection from dictionary attacks.
            This value does not need to be kept secret, but it should be randomly
            chosen for each derivation.
         dkLen (integer):
            The length of the desired key. The default is 16 bytes, suitable for
            instance for :mod:`Crypto.Cipher.AES`.
         count (integer):
            The number of iterations to carry out. The recommendation is 1000 or
            more.
         hashAlgo (~module~): TODO(Larky-Difference):: this is a string
            pycryptodome incompatibility => this is a STRING representing the HashAlgo
            The hash algorithm to use, as a module or an object from the :mod:`Crypto.Hash` package.
            The digest length must be no shorter than ``dkLen``.
            The default algorithm is :mod:`Crypto.Hash.SHA1`.

        Return:
            A byte string of length ``dkLen`` that can be used as key.

    """
    if not hashAlgo:
        hashAlgo = 'SHA1'
    else:
        if not types.is_string(hashAlgo):
            fail("hashAlgo must be a string (i.e. 'MD5'), received: {}".format(
                type(hashAlgo)
            ))

    rval = _JCrypto.Protocol.PBKDF1(password, salt, dkLen, count, hashAlgo)

    if not (types.is_bytearray(rval) or types.is_bytes(rval)):
        fail("expected byte-like from PBKDF1, but received type: {}".format(
            type(rval)
        ))

    return rval


def PBKDF2(password, salt, dkLen=16, count=1000, prf=None, hmac_hash_module=None):
    """
    Derive one or more keys from a password (or passphrase).

        This function performs key derivation according to the PKCS#5 standard (v2.0).

        Args:
         password (string or byte string):
            The secret password to generate the key from.
         salt (string or byte string):
            A (byte) string to use for better protection from dictionary attacks.
            This value does not need to be kept secret, but it should be randomly
            chosen for each derivation. It is recommended to use at least 16 bytes.
         dkLen (integer):
            The cumulative length of the keys to produce.

            Due to a flaw in the PBKDF2 design, you should not request more bytes
            than the ``prf`` can output. For instance, ``dkLen`` should not exceed
            20 bytes in combination with ``HMAC-SHA1``.
         count (integer):
            The number of iterations to carry out. The higher the value, the slower
            and the more secure the function becomes.

            You should find the maximum number of iterations that keeps the
            key derivation still acceptable on the slowest hardware you must support.

            Although the default value is 1000, **it is recommended to use at least
            1000000 (1 million) iterations**.
         prf (callable):
            A pseudorandom function. It must be a function that returns a
            pseudorandom byte string from two parameters: a secret and a salt.
            The slower the algorithm, the more secure the derivation function.
            If not specified, **HMAC-SHA1** is used.
         hmac_hash_module (module):
            A module from ``Crypto.Hash`` implementing a Merkle-Damgard cryptographic
            hash, which PBKDF2 must use in combination with HMAC.
            This parameter is mutually exclusive with ``prf``.

        Return:
            A byte string of length ``dkLen`` that can be used as key material.
            If you want multiple keys, just break up this string into segments of the desired length.

    """
        def link(s):
            """
            b''
            """
def _S2V(object):
    """
    String-to-vector PRF as defined in `RFC5297`_.

        This class implements a pseudorandom function family
        based on CMAC that takes as input a vector of strings.

        .. _RFC5297: http://tools.ietf.org/html/rfc5297

    """
    def __init__(self, key, ciphermod, cipher_params=None):
        """
        Initialize the S2V PRF.

                :Parameters:
                  key : byte string
                    A secret that can be used as key for CMACs
                    based on ciphers from ``ciphermod``.
                  ciphermod : module
                    A block cipher module from `Crypto.Cipher`.
                  cipher_params : dictionary
                    A set of extra parameters to use to create a cipher instance.

        """
    def new(key, ciphermod):
        """
        Create a new S2V PRF.

                :Parameters:
                  key : byte string
                    A secret that can be used as key for CMACs
                    based on ciphers from ``ciphermod``.
                  ciphermod : module
                    A block cipher module from `Crypto.Cipher`.

        """
    def _double(self, bs):
        """
        Pass the next component of the vector.

                The maximum number of components you can pass is equal to the block
                length of the cipher (in bits) minus 1.

                :Parameters:
                  item : byte string
                    The next component of the vector.
                :Raise TypeError: when the limit on the number of components has been reached.

        """
    def derive(self):
        """
        Derive a secret from the vector of components.

                :Return: a byte string, as long as the block length of the cipher.

        """
def HKDF(master, key_len, salt, hashmod, num_keys=1, context=None):
    """
    Derive one or more keys from a master secret using
        the HMAC-based KDF defined in RFC5869_.

        Args:
         master (byte string):
            The unguessable value used by the KDF to generate the other keys.
            It must be a high-entropy secret, though not necessarily uniform.
            It must not be a password.
         salt (byte string):
            A non-secret, reusable value that strengthens the randomness
            extraction step.
            Ideally, it is as long as the digest size of the chosen hash.
            If empty, a string of zeroes in used.
         key_len (integer):
            The length in bytes of every derived key.
         hashmod (module):
            A cryptographic hash algorithm from :mod:`Crypto.Hash`.
            :mod:`Crypto.Hash.SHA512` is a good choice.
         num_keys (integer):
            The number of keys to derive. Every key is :data:`key_len` bytes long.
            The maximum cumulative length of all keys is
            255 times the digest size.
         context (byte string):
            Optional identifier describing what the keys are used for.

        Return:
            A byte string or a tuple of byte strings.

        .. _RFC5869: http://tools.ietf.org/html/rfc5869

    """
def scrypt(password, salt, key_len, N, r, p, num_keys=1):
    """
    Derive one or more keys from a passphrase.

        Args:
         password (string):
            The secret pass phrase to generate the keys from.
         salt (string):
            A string to use for better protection from dictionary attacks.
            This value does not need to be kept secret,
            but it should be randomly chosen for each derivation.
            It is recommended to be at least 16 bytes long.
         key_len (integer):
            The length in bytes of every derived key.
         N (integer):
            CPU/Memory cost parameter. It must be a power of 2 and less
            than :math:`2^{32}`.
         r (integer):
            Block size parameter.
         p (integer):
            Parallelization parameter.
            It must be no greater than :math:`(2^{32}-1)/(4r)`.
         num_keys (integer):
            The number of keys to derive. Every key is :data:`key_len` bytes long.
            By default, only 1 key is generated.
            The maximum cumulative length of all keys is :math:`(2^{32}-1)*32`
            (that is, 128TB).

        A good choice of parameters *(N, r , p)* was suggested
        by Colin Percival in his `presentation in 2009`__:

        - *( 2¹⁴, 8, 1 )* for interactive logins (≤100ms)
        - *( 2²⁰, 8, 1 )* for file encryption (≤5s)

        Return:
            A byte string or a tuple of byte strings.

        .. __: http://www.tarsnap.com/scrypt/scrypt-slides.pdf

    """
def _bcrypt_encode(data):
    """
    ./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789
    """
def _bcrypt_decode(data):
    """
    ./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789
    """
def _bcrypt_hash(password, cost, salt, constant, invert):
    """
    The password is too long. It must be 72 bytes at most.
    """
def bcrypt(password, cost, salt=None):
    """
    Hash a password into a key, using the OpenBSD bcrypt protocol.

        Args:
          password (byte string or string):
            The secret password or pass phrase.
            It must be at most 72 bytes long.
            It must not contain the zero byte.
            Unicode strings will be encoded as UTF-8.
          cost (integer):
            The exponential factor that makes it slower to compute the hash.
            It must be in the range 4 to 31.
            A value of at least 12 is recommended.
          salt (byte string):
            Optional. Random byte string to thwarts dictionary and rainbow table
            attacks. It must be 16 bytes long.
            If not passed, a random value is generated.

        Return (byte string):
            The bcrypt hash

        Raises:
            ValueError: if password is longer than 72 bytes or if it contains the zero byte


    """
def bcrypt_check(password, bcrypt_hash):
    """
    Verify if the provided password matches the given bcrypt hash.

        Args:
          password (byte string or string):
            The secret password or pass phrase to test.
            It must be at most 72 bytes long.
            It must not contain the zero byte.
            Unicode strings will be encoded as UTF-8.
          bcrypt_hash (byte string, bytearray):
            The reference bcrypt hash the password needs to be checked against.

        Raises:
            ValueError: if the password does not match

    """
