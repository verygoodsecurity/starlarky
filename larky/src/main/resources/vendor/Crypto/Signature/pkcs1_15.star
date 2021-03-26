def PKCS115_SigScheme:
    """
    A signature object for ``RSASSA-PKCS1-v1_5``.
        Do not instantiate directly.
        Use :func:`Crypto.Signature.pkcs1_15.new`.
    
    """
    def __init__(self, rsa_key):
        """
        Initialize this PKCS#1 v1.5 signature scheme object.

                :Parameters:
                  rsa_key : an RSA key object
                    Creation of signatures is only possible if this is a *private*
                    RSA key. Verification of signatures is always possible.
        
        """
    def can_sign(self):
        """
        Return ``True`` if this object can be used to sign messages.
        """
    def sign(self, msg_hash):
        """
        Create the PKCS#1 v1.5 signature of a message.

                This function is also called ``RSASSA-PKCS1-V1_5-SIGN`` and
                it is specified in
                `section 8.2.1 of RFC8017 <https://tools.ietf.org/html/rfc8017#page-36>`_.

                :parameter msg_hash:
                    This is an object from the :mod:`Crypto.Hash` package.
                    It has been used to digest the message to sign.
                :type msg_hash: hash object

                :return: the signature encoded as a *byte string*.
                :raise ValueError: if the RSA key is not long enough for the given hash algorithm.
                :raise TypeError: if the RSA key has no private half.
        
        """
    def verify(self, msg_hash, signature):
        """
        Check if the  PKCS#1 v1.5 signature over a message is valid.

                This function is also called ``RSASSA-PKCS1-V1_5-VERIFY`` and
                it is specified in
                `section 8.2.2 of RFC8037 <https://tools.ietf.org/html/rfc8017#page-37>`_.

                :parameter msg_hash:
                    The hash that was carried out over the message. This is an object
                    belonging to the :mod:`Crypto.Hash` module.
                :type parameter: hash object

                :parameter signature:
                    The signature that needs to be validated.
                :type signature: byte string

                :raise ValueError: if the signature is not valid.
        
        """
def _EMSA_PKCS1_V1_5_ENCODE(msg_hash, emLen, with_hash_parameters=True):
    """

        Implement the ``EMSA-PKCS1-V1_5-ENCODE`` function, as defined
        in PKCS#1 v2.1 (RFC3447, 9.2).

        ``_EMSA-PKCS1-V1_5-ENCODE`` actually accepts the message ``M`` as input,
        and hash it internally. Here, we expect that the message has already
        been hashed instead.

        :Parameters:
         msg_hash : hash object
                The hash object that holds the digest of the message being signed.
         emLen : int
                The length the final encoding must have, in bytes.
         with_hash_parameters : bool
                If True (default), include NULL parameters for the hash
                algorithm in the ``digestAlgorithm`` SEQUENCE.

        :attention: the early standard (RFC2313) stated that ``DigestInfo``
            had to be BER-encoded. This means that old signatures
            might have length tags in indefinite form, which
            is not supported in DER. Such encoding cannot be
            reproduced by this function.

        :Return: An ``emLen`` byte long string that encodes the hash.
    
    """
def new(rsa_key):
    """
    Create a signature object for creating
        or verifying PKCS#1 v1.5 signatures.

        :parameter rsa_key:
          The RSA key to use for signing or verifying the message.
          This is a :class:`Crypto.PublicKey.RSA` object.
          Signing is only possible when ``rsa_key`` is a **private** RSA key.
        :type rsa_key: RSA object

        :return: a :class:`PKCS115_SigScheme` signature object
    
    """
