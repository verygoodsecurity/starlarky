def ARC4Cipher:
    """
    ARC4 cipher object. Do not create it directly. Use
        :func:`Crypto.Cipher.ARC4.new` instead.
    
    """
    def __init__(self, key, *args, **kwargs):
        """
        Initialize an ARC4 cipher object

                See also `new()` at the module level.
        """
    def encrypt(self, plaintext):
        """
        Encrypt a piece of data.

                :param plaintext: The data to encrypt, of any size.
                :type plaintext: bytes, bytearray, memoryview
                :returns: the encrypted byte string, of equal length as the
                  plaintext.
        
        """
    def decrypt(self, ciphertext):
        """
        Decrypt a piece of data.

                :param ciphertext: The data to decrypt, of any size.
                :type ciphertext: bytes, bytearray, memoryview
                :returns: the decrypted byte string, of equal length as the
                  ciphertext.
        
        """
def new(key, *args, **kwargs):
    """
    Create a new ARC4 cipher.

        :param key:
            The secret key to use in the symmetric cipher.
            Its length must be in the range ``[5..256]``.
            The recommended length is 16 bytes.
        :type key: bytes, bytearray, memoryview

        :Keyword Arguments:
            *   *drop* (``integer``) --
                The amount of bytes to discard from the initial part of the keystream.
                In fact, such part has been found to be distinguishable from random
                data (while it shouldn't) and also correlated to key.

                The recommended value is 3072_ bytes. The default value is 0.

        :Return: an `ARC4Cipher` object

        .. _3072: http://eprint.iacr.org/2002/067.pdf
    
    """
