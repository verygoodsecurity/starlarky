def OfbMode(object):
    """
    *Output FeedBack (OFB)*.

        This mode is very similar to CBC, but it
        transforms the underlying block cipher into a stream cipher.

        The keystream is the iterated block encryption of the
        previous ciphertext block.

        An Initialization Vector (*IV*) is required.

        See `NIST SP800-38A`_ , Section 6.4.

        .. _`NIST SP800-38A` : http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf

        :undocumented: __init__
    
    """
    def __init__(self, block_cipher, iv):
        """
        Create a new block cipher, configured in OFB mode.

                :Parameters:
                  block_cipher : C pointer
                    A smart pointer to the low-level block cipher instance.

                  iv : bytes/bytearray/memoryview
                    The initialization vector to use for encryption or decryption.
                    It is as long as the cipher block.

                    **The IV must be a nonce, to to be reused for any other
                    message**. It shall be a nonce or a random value.

                    Reusing the *IV* for encryptions performed with the same key
                    compromises confidentiality.
        
        """
    def encrypt(self, plaintext, output=None):
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
                    It can be of any length.
                :Keywords:
                  output : bytearray/memoryview
                    The location where the ciphertext must be written to.
                    If ``None``, the ciphertext is returned.
                :Return:
                  If ``output`` is ``None``, the ciphertext is returned as ``bytes``.
                  Otherwise, ``None``.
        
        """
    def decrypt(self, ciphertext, output=None):
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
                    It can be of any length.
                :Keywords:
                  output : bytearray/memoryview
                    The location where the plaintext is written to.
                    If ``None``, the plaintext is returned.
                :Return:
                  If ``output`` is ``None``, the plaintext is returned as ``bytes``.
                  Otherwise, ``None``.
        
        """
def _create_ofb_cipher(factory, **kwargs):
    """
    Instantiate a cipher object that performs OFB encryption/decryption.

        :Parameters:
          factory : module
            The underlying block cipher, a module from ``Crypto.Cipher``.

        :Keywords:
          iv : bytes/bytearray/memoryview
            The IV to use for OFB.

          IV : bytes/bytearray/memoryview
            Alias for ``iv``.

        Any other keyword will be passed to the underlying block cipher.
        See the relevant documentation for details (at least ``key`` will need
        to be present).
    
    """
