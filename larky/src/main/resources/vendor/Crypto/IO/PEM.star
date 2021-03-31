def encode(data, marker, passphrase=None, randfunc=None):
    """
    Encode a piece of binary data into PEM format.

        Args:
          data (byte string):
            The piece of binary data to encode.
          marker (string):
            The marker for the PEM block (e.g. "PUBLIC KEY").
            Note that there is no official master list for all allowed markers.
            Still, you can refer to the OpenSSL_ source code.
          passphrase (byte string):
            If given, the PEM block will be encrypted. The key is derived from
            the passphrase.
          randfunc (callable):
            Random number generation function; it accepts an integer N and returns
            a byte string of random data, N bytes long. If not given, a new one is
            instantiated.

        Returns:
          The PEM block, as a string.

        .. _OpenSSL: https://github.com/openssl/openssl/blob/master/include/openssl/pem.h
    
    """
def _EVP_BytesToKey(data, salt, key_len):
    """
    b''
    """
def decode(pem_data, passphrase=None):
    """
    Decode a PEM block into binary.

        Args:
          pem_data (string):
            The PEM block.
          passphrase (byte string):
            If given and the PEM block is encrypted,
            the key will be derived from the passphrase.

        Returns:
          A tuple with the binary data, the marker string, and a boolean to
          indicate if decryption was performed.

        Raises:
          ValueError: if decoding fails, if the PEM file is encrypted and no passphrase has
                      been provided or if the passphrase is incorrect.
    
    """
