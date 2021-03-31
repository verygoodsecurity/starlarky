def PbesError(ValueError):
    """
     These are the ASN.1 definitions used by the PBES1/2 logic:

     EncryptedPrivateKeyInfo ::= SEQUENCE {
       encryptionAlgorithm  EncryptionAlgorithmIdentifier,
       encryptedData        EncryptedData
     }

     EncryptionAlgorithmIdentifier ::= AlgorithmIdentifier

     EncryptedData ::= OCTET STRING

     AlgorithmIdentifier  ::=  SEQUENCE  {
           algorithm   OBJECT IDENTIFIER,
           parameters  ANY DEFINED BY algorithm OPTIONAL
     }

     PBEParameter ::= SEQUENCE {
           salt OCTET STRING (SIZE(8)),
           iterationCount INTEGER
     }

     PBES2-params ::= SEQUENCE {
           keyDerivationFunc AlgorithmIdentifier {{PBES2-KDFs}},
           encryptionScheme AlgorithmIdentifier {{PBES2-Encs}}
     }

     PBKDF2-params ::= SEQUENCE {
       salt CHOICE {
           specified OCTET STRING,
           otherSource AlgorithmIdentifier {{PBKDF2-SaltSources}}
           },
       iterationCount INTEGER (1..MAX),
       keyLength INTEGER (1..MAX) OPTIONAL,
       prf AlgorithmIdentifier {{PBKDF2-PRFs}} DEFAULT algid-hmacWithSHA1
       }

     scrypt-params ::= SEQUENCE {
           salt OCTET STRING,
           costParameter INTEGER (1..MAX),
           blockSize INTEGER (1..MAX),
           parallelizationParameter INTEGER (1..MAX),
           keyLength INTEGER (1..MAX) OPTIONAL
       }


    """
def PBES1(object):
    """
    Deprecated encryption scheme with password-based key derivation
        (originally defined in PKCS#5 v1.5, but still present in `v2.0`__).

        .. __: http://www.ietf.org/rfc/rfc2898.txt
    
    """
    def decrypt(data, passphrase):
        """
        Decrypt a piece of data using a passphrase and *PBES1*.

                The algorithm to use is automatically detected.

                :Parameters:
                  data : byte string
                    The piece of data to decrypt.
                  passphrase : byte string
                    The passphrase to use for decrypting the data.
                :Returns:
                  The decrypted data, as a binary string.
        
        """
def PBES2(object):
    """
    Encryption scheme with password-based key derivation
        (defined in `PKCS#5 v2.0`__).

        .. __: http://www.ietf.org/rfc/rfc2898.txt.
    """
    def encrypt(data, passphrase, protection, prot_params=None, randfunc=None):
        """
        Encrypt a piece of data using a passphrase and *PBES2*.

                :Parameters:
                  data : byte string
                    The piece of data to encrypt.
                  passphrase : byte string
                    The passphrase to use for encrypting the data.
                  protection : string
                    The identifier of the encryption algorithm to use.
                    The default value is '``PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC``'.
                  prot_params : dictionary
                    Parameters of the protection algorithm.

                    +------------------+-----------------------------------------------+
                    | Key              | Description                                   |
                    +==================+===============================================+
                    | iteration_count  | The KDF algorithm is repeated several times to|
                    |                  | slow down brute force attacks on passwords    |
                    |                  | (called *N* or CPU/memory cost in scrypt).    |
                    |                  |                                               |
                    |                  | The default value for PBKDF2 is 1 000.        |
                    |                  | The default value for scrypt is 16 384.       |
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


                  randfunc : callable
                    Random number generation function; it should accept
                    a single integer N and return a string of random data,
                    N bytes long. If not specified, a new RNG will be
                    instantiated from ``Crypto.Random``.

                :Returns:
                  The encrypted data, as a binary string.
        
        """
    def decrypt(data, passphrase):
        """
        Decrypt a piece of data using a passphrase and *PBES2*.

                The algorithm to use is automatically detected.

                :Parameters:
                  data : byte string
                    The piece of data to decrypt.
                  passphrase : byte string
                    The passphrase to use for decrypting the data.
                :Returns:
                  The decrypted data, as a binary string.
        
        """
