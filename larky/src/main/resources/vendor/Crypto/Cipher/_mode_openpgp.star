def OpenPgpMode(object):
    """
    OpenPGP mode.

        This mode is a variant of CFB, and it is only used in PGP and
        OpenPGP_ applications. If in doubt, use another mode.

        An Initialization Vector (*IV*) is required.

        Unlike CFB, the *encrypted* IV (not the IV itself) is
        transmitted to the receiver.

        The IV is a random data block. For legacy reasons, two of its bytes are
        duplicated to act as a checksum for the correctness of the key, which is now
        known to be insecure and is ignored. The encrypted IV is therefore 2 bytes
        longer than the clean IV.

        .. _OpenPGP: http://tools.ietf.org/html/rfc4880

        :undocumented: __init__
    
    """
    def __init__(self, factory, key, iv, cipher_params):
        """
        : The block size of the underlying cipher, in bytes.

        """
    def encrypt(self, plaintext):
        """
        Encrypt data with the key and the parameters set at initialization.

                A cipher object is stateful: once you have encrypted a message
                you cannot encrypt (or decrypt) another message using the same
                object.

                The data to encrypt can be broken up in two or
                more pieces and `encrypt` can be called multiple times.

                That is, the statement:

                    >>> c.encrypt(a) + c.encrypt(b)

                is equivalent to:

                     >>> c.encrypt(a+b)

                This function does not add any padding to the plaintext.

                :Parameters:
                  plaintext : bytes/bytearray/memoryview
                    The piece of data to encrypt.

                :Return:
                    the encrypted data, as a byte string.
                    It is as long as *plaintext* with one exception:
                    when encrypting the first message chunk,
                    the encypted IV is prepended to the returned ciphertext.
        
        """
    def decrypt(self, ciphertext):
        """
        Decrypt data with the key and the parameters set at initialization.

                A cipher object is stateful: once you have decrypted a message
                you cannot decrypt (or encrypt) another message with the same
                object.

                The data to decrypt can be broken up in two or
                more pieces and `decrypt` can be called multiple times.

                That is, the statement:

                    >>> c.decrypt(a) + c.decrypt(b)

                is equivalent to:

                     >>> c.decrypt(a+b)

                This function does not remove any padding from the plaintext.

                :Parameters:
                  ciphertext : bytes/bytearray/memoryview
                    The piece of data to decrypt.

                :Return: the decrypted data (byte string).
        
        """
def _create_openpgp_cipher(factory, **kwargs):
    """
    Create a new block cipher, configured in OpenPGP mode.

        :Parameters:
          factory : module
            The module.

        :Keywords:
          key : bytes/bytearray/memoryview
            The secret key to use in the symmetric cipher.

          IV : bytes/bytearray/memoryview
            The initialization vector to use for encryption or decryption.

            For encryption, the IV must be as long as the cipher block size.

            For decryption, it must be 2 bytes longer (it is actually the
            *encrypted* IV which was prefixed to the ciphertext).
    
    """
