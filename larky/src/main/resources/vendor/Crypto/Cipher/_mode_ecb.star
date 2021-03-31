def EcbMode(object):
    """
    *Electronic Code Book (ECB)*.

        This is the simplest encryption mode. Each of the plaintext blocks
        is directly encrypted into a ciphertext block, independently of
        any other block.

        This mode is dangerous because it exposes frequency of symbols
        in your plaintext. Other modes (e.g. *CBC*) should be used instead.

        See `NIST SP800-38A`_ , Section 6.1.

        .. _`NIST SP800-38A` : http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf

        :undocumented: __init__
    
    """
    def __init__(self, block_cipher):
        """
        Create a new block cipher, configured in ECB mode.

                :Parameters:
                  block_cipher : C pointer
                    A smart pointer to the low-level block cipher instance.
        
        """
    def encrypt(self, plaintext, output=None):
        """
        Encrypt data with the key set at initialization.

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
                    The length must be multiple of the cipher block length.
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
        Decrypt data with the key set at initialization.

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
                    The length must be multiple of the cipher block length.
                :Keywords:
                  output : bytearray/memoryview
                    The location where the plaintext must be written to.
                    If ``None``, the plaintext is returned.
                :Return:
                  If ``output`` is ``None``, the plaintext is returned as ``bytes``.
                  Otherwise, ``None``.
        
        """
def _create_ecb_cipher(factory, **kwargs):
    """
    Instantiate a cipher object that performs ECB encryption/decryption.

        :Parameters:
          factory : module
            The underlying block cipher, a module from ``Crypto.Cipher``.

        All keywords are passed to the underlying block cipher.
        See the relevant documentation for details (at least ``key`` will need
        to be present
    """
