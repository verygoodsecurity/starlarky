def PKCS1OAEP_Cipher:
    """
    Cipher object for PKCS#1 v1.5 OAEP.
        Do not create directly: use :func:`new` instead.
    """
    def __init__(self, key, hashAlgo, mgfunc, label, randfunc):
        """
        Initialize this PKCS#1 OAEP cipher object.

                :Parameters:
                 key : an RSA key object
                        If a private half is given, both encryption and decryption are possible.
                        If a public half is given, only encryption is possible.
                 hashAlgo : hash object
                        The hash function to use. This can be a module under `Crypto.Hash`
                        or an existing hash object created from any of such modules. If not specified,
                        `Crypto.Hash.SHA1` is used.
                 mgfunc : callable
                        A mask generation function that accepts two parameters: a string to
                        use as seed, and the lenth of the mask to generate, in bytes.
                        If not specified, the standard MGF1 consistent with ``hashAlgo`` is used (a safe choice).
                 label : bytes/bytearray/memoryview
                        A label to apply to this particular encryption. If not specified,
                        an empty string is used. Specifying a label does not improve
                        security.
                 randfunc : callable
                        A function that returns random bytes.

                :attention: Modify the mask generation function only if you know what you are doing.
                            Sender and receiver must use the same one.
        
        """
    def can_encrypt(self):
        """
        Legacy function to check if you can call :meth:`encrypt`.

                .. deprecated:: 3.0
        """
    def can_decrypt(self):
        """
        Legacy function to check if you can call :meth:`decrypt`.

                .. deprecated:: 3.0
        """
    def encrypt(self, message):
        """
        Encrypt a message with PKCS#1 OAEP.

                :param message:
                    The message to encrypt, also known as plaintext. It can be of
                    variable length, but not longer than the RSA modulus (in bytes)
                    minus 2, minus twice the hash output size.
                    For instance, if you use RSA 2048 and SHA-256, the longest message
                    you can encrypt is 190 byte long.
                :type message: bytes/bytearray/memoryview

                :returns: The ciphertext, as large as the RSA modulus.
                :rtype: bytes

                :raises ValueError:
                    if the message is too long.
        
        """
    def decrypt(self, ciphertext):
        """
        Decrypt a message with PKCS#1 OAEP.

                :param ciphertext: The encrypted message.
                :type ciphertext: bytes/bytearray/memoryview

                :returns: The original message (plaintext).
                :rtype: bytes

                :raises ValueError:
                    if the ciphertext has the wrong length, or if decryption
                    fails the integrity check (in which case, the decryption
                    key is probably wrong).
                :raises TypeError:
                    if the RSA key has no private half (i.e. you are trying
                    to decrypt using a public key).
        
        """
def new(key, hashAlgo=None, mgfunc=None, label=b'', randfunc=None):
    """
    Return a cipher object :class:`PKCS1OAEP_Cipher` that can be used to perform PKCS#1 OAEP encryption or decryption.

        :param key:
          The key object to use to encrypt or decrypt the message.
          Decryption is only possible with a private RSA key.
        :type key: RSA key object

        :param hashAlgo:
          The hash function to use. This can be a module under `Crypto.Hash`
          or an existing hash object created from any of such modules.
          If not specified, `Crypto.Hash.SHA1` is used.
        :type hashAlgo: hash object

        :param mgfunc:
          A mask generation function that accepts two parameters: a string to
          use as seed, and the lenth of the mask to generate, in bytes.
          If not specified, the standard MGF1 consistent with ``hashAlgo`` is used (a safe choice).
        :type mgfunc: callable

        :param label:
          A label to apply to this particular encryption. If not specified,
          an empty string is used. Specifying a label does not improve
          security.
        :type label: bytes/bytearray/memoryview

        :param randfunc:
          A function that returns random bytes.
          The default is `Random.get_random_bytes`.
        :type randfunc: callable
    
    """
