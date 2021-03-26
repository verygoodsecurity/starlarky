2021-03-02 17:42:09,318 : INFO : tokenize_signature : --> do i ever get here?
def wrap(private_key, key_oid, passphrase=None, protection=None,
         prot_params=None, key_params=None, randfunc=None):
    """
    Wrap a private key into a PKCS#8 blob (clear or encrypted).

        Args:

          private_key (byte string):
            The private key encoded in binary form. The actual encoding is
            algorithm specific. In most cases, it is DER.

          key_oid (string):
            The object identifier (OID) of the private key to wrap.
            It is a dotted string, like ``1.2.840.113549.1.1.1`` (for RSA keys).

          passphrase (bytes string or string):
            The secret passphrase from which the wrapping key is derived.
            Set it only if encryption is required.

          protection (string):
            The identifier of the algorithm to use for securely wrapping the key.
            The default value is ``PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC``.

          prot_params (dictionary):
            Parameters for the protection algorithm.

            +------------------+-----------------------------------------------+
            | Key              | Description                                   |
            +==================+===============================================+
            | iteration_count  | The KDF algorithm is repeated several times to|
            |                  | slow down brute force attacks on passwords    |
            |                  | (called *N* or CPU/memory cost in scrypt).    |
            |                  | The default value for PBKDF2 is 1000.         |
            |                  | The default value for scrypt is 16384.        |
            +------------------+-----------------------------------------------+
            | salt_size        | Salt is used to thwart dictionary and rainbow |
            |                  | attacks on passwords. The default value is 8  |
            |                  | bytes.                                        |
            +------------------+-----------------------------------------------+
            | block_size       | *(scrypt only)* Memory-cost (r). The default  |
            |                  | value is 8.                                   |
            +------------------+-----------------------------------------------+
            | parallelization  | *(scrypt only)* CPU-cost (p). The default     |
            |                  | value is 1.                                   |
            +------------------+-----------------------------------------------+

          key_params (DER object):
            The algorithm parameters associated to the private key.
            It is required for algorithms like DSA, but not for others like RSA.

          randfunc (callable):
            Random number generation function; it should accept a single integer
            N and return a string of random data, N bytes long.
            If not specified, a new RNG will be instantiated
            from :mod:`Crypto.Random`.

        Return:
          The PKCS#8-wrapped private key (possibly encrypted), as a byte string.
    
    """
def unwrap(p8_private_key, passphrase=None):
    """
    Unwrap a private key from a PKCS#8 blob (clear or encrypted).

        Args:
          p8_private_key (byte string):
            The private key wrapped into a PKCS#8 blob, DER encoded.
          passphrase (byte string or string):
            The passphrase to use to decrypt the blob (if it is encrypted).

        Return:
          A tuple containing

           #. the algorithm identifier of the wrapped key (OID, dotted string)
           #. the private key (byte string, DER encoded)
           #. the associated parameters (byte string, DER encoded) or ``None``

        Raises:
          ValueError : if decoding fails
    
    """
