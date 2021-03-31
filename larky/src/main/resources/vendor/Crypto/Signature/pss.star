def PSS_SigScheme:
    """
    A signature object for ``RSASSA-PSS``.
        Do not instantiate directly.
        Use :func:`Crypto.Signature.pss.new`.
    
    """
    def __init__(self, key, mgfunc, saltLen, randfunc):
        """
        Initialize this PKCS#1 PSS signature scheme object.

                :Parameters:
                  key : an RSA key object
                    If a private half is given, both signature and
                    verification are possible.
                    If a public half is given, only verification is possible.
                  mgfunc : callable
                    A mask generation function that accepts two parameters:
                    a string to use as seed, and the lenth of the mask to
                    generate, in bytes.
                  saltLen : integer
                    Length of the salt, in bytes.
                  randfunc : callable
                    A function that returns random bytes.
        
        """
    def can_sign(self):
        """
        Return ``True`` if this object can be used to sign messages.
        """
    def sign(self, msg_hash):
        """
        Create the PKCS#1 PSS signature of a message.

                This function is also called ``RSASSA-PSS-SIGN`` and
                it is specified in
                `section 8.1.1 of RFC8017 <https://tools.ietf.org/html/rfc8017#section-8.1.1>`_.

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
        Check if the  PKCS#1 PSS signature over a message is valid.

                This function is also called ``RSASSA-PSS-VERIFY`` and
                it is specified in
                `section 8.1.2 of RFC8037 <https://tools.ietf.org/html/rfc8017#section-8.1.2>`_.

                :parameter msg_hash:
                    The hash that was carried out over the message. This is an object
                    belonging to the :mod:`Crypto.Hash` module.
                :type parameter: hash object

                :parameter signature:
                    The signature that needs to be validated.
                :type signature: bytes

                :raise ValueError: if the signature is not valid.
        
        """
def MGF1(mgfSeed, maskLen, hash_gen):
    """
    Mask Generation Function, described in `B.2.1 of RFC8017
        <https://tools.ietf.org/html/rfc8017>`_.

        :param mfgSeed:
            seed from which the mask is generated
        :type mfgSeed: byte string

        :param maskLen:
            intended length in bytes of the mask
        :type maskLen: integer

        :param hash_gen:
            A module or a hash object from :mod:`Crypto.Hash`
        :type hash_object:

        :return: the mask, as a *byte string*
    
    """
def _EMSA_PSS_ENCODE(mhash, emBits, randFunc, mgf, sLen):
    """
    r"""
        Implement the ``EMSA-PSS-ENCODE`` function, as defined
        in PKCS#1 v2.1 (RFC3447, 9.1.1).

        The original ``EMSA-PSS-ENCODE`` actually accepts the message ``M``
        as input, and hash it internally. Here, we expect that the message
        has already been hashed instead.

        :Parameters:
          mhash : hash object
            The hash object that holds the digest of the message being signed.
          emBits : int
            Maximum length of the final encoding, in bits.
          randFunc : callable
            An RNG function that accepts as only parameter an int, and returns
            a string of random bytes, to be used as salt.
          mgf : callable
            A mask generation function that accepts two parameters: a string to
            use as seed, and the lenth of the mask to generate, in bytes.
          sLen : int
            Length of the salt, in bytes.

        :Return: An ``emLen`` byte long string that encodes the hash
          (with ``emLen = \ceil(emBits/8)``).

        :Raise ValueError:
            When digest or salt length are too big.
    
    """
def _EMSA_PSS_VERIFY(mhash, em, emBits, mgf, sLen):
    """

        Implement the ``EMSA-PSS-VERIFY`` function, as defined
        in PKCS#1 v2.1 (RFC3447, 9.1.2).

        ``EMSA-PSS-VERIFY`` actually accepts the message ``M`` as input,
        and hash it internally. Here, we expect that the message has already
        been hashed instead.

        :Parameters:
          mhash : hash object
            The hash object that holds the digest of the message to be verified.
          em : string
            The signature to verify, therefore proving that the sender really
            signed the message that was received.
          emBits : int
            Length of the final encoding (em), in bits.
          mgf : callable
            A mask generation function that accepts two parameters: a string to
            use as seed, and the lenth of the mask to generate, in bytes.
          sLen : int
            Length of the salt, in bytes.

        :Raise ValueError:
            When the encoding is inconsistent, or the digest or salt lengths
            are too big.
    
    """
def new(rsa_key, **kwargs):
    """
    Create an object for making or verifying PKCS#1 PSS signatures.

        :parameter rsa_key:
          The RSA key to use for signing or verifying the message.
          This is a :class:`Crypto.PublicKey.RSA` object.
          Signing is only possible when ``rsa_key`` is a **private** RSA key.
        :type rsa_key: RSA object

        :Keyword Arguments:

            *   *mask_func* (``callable``) --
                A function that returns the mask (as `bytes`).
                It must accept two parameters: a seed (as `bytes`)
                and the length of the data to return.

                If not specified, it will be the function :func:`MGF1` defined in
                `RFC8017 <https://tools.ietf.org/html/rfc8017#page-67>`_ and
                combined with the same hash algorithm applied to the
                message to sign or verify.

                If you want to use a different function, for instance still :func:`MGF1`
                but together with another hash, you can do::

                    from Crypto.Hash import SHA256
                    from Crypto.Signature.pss import MGF1
                    mgf = lambda x, y: MGF1(x, y, SHA256)

            *   *salt_bytes* (``integer``) --
                Length of the salt, in bytes.
                It is a value between 0 and ``emLen - hLen - 2``, where ``emLen``
                is the size of the RSA modulus and ``hLen`` is the size of the digest
                applied to the message to sign or verify.

                The salt is generated internally, you don't need to provide it.

                If not specified, the salt length will be ``hLen``.
                If it is zero, the signature scheme becomes deterministic.

                Note that in some implementations such as OpenSSL the default
                salt length is ``emLen - hLen - 2`` (even though it is not more
                secure than ``hLen``).

            *   *rand_func* (``callable``) --
                A function that returns random ``bytes``, of the desired length.
                The default is :func:`Crypto.Random.get_random_bytes`.

        :return: a :class:`PSS_SigScheme` signature object
    
    """
