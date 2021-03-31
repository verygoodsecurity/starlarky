def SivMode(object):
    """
    Synthetic Initialization Vector (SIV).

        This is an Authenticated Encryption with Associated Data (`AEAD`_) mode.
        It provides both confidentiality and authenticity.

        The header of the message may be left in the clear, if needed, and it will
        still be subject to authentication. The decryption step tells the receiver
        if the message comes from a source that really knowns the secret key.
        Additionally, decryption detects if any part of the message - including the
        header - has been modified or corrupted.

        Unlike other AEAD modes such as CCM, EAX or GCM, accidental reuse of a
        nonce is not catastrophic for the confidentiality of the message. The only
        effect is that an attacker can tell when the same plaintext (and same
        associated data) is protected with the same key.

        The length of the MAC is fixed to the block size of the underlying cipher.
        The key size is twice the length of the key of the underlying cipher.

        This mode is only available for AES ciphers.

        +--------------------+---------------+-------------------+
        |      Cipher        | SIV MAC size  |   SIV key length  |
        |                    |    (bytes)    |     (bytes)       |
        +====================+===============+===================+
        |    AES-128         |      16       |        32         |
        +--------------------+---------------+-------------------+
        |    AES-192         |      16       |        48         |
        +--------------------+---------------+-------------------+
        |    AES-256         |      16       |        64         |
        +--------------------+---------------+-------------------+

        See `RFC5297`_ and the `original paper`__.

        .. _RFC5297: https://tools.ietf.org/html/rfc5297
        .. _AEAD: http://blog.cryptographyengineering.com/2012/05/how-to-choose-authenticated-encryption.html
        .. __: http://www.cs.ucdavis.edu/~rogaway/papers/keywrap.pdf

        :undocumented: __init__
    
    """
    def __init__(self, factory, key, nonce, kwargs):
        """
        The block size of the underlying cipher, in bytes.
        """
    def _create_ctr_cipher(self, v):
        """
        Create a new CTR cipher from V in SIV mode
        """
    def update(self, component):
        """
        Protect one associated data component

                For SIV, the associated data is a sequence (*vector*) of non-empty
                byte strings (*components*).

                This method consumes the next component. It must be called
                once for each of the components that constitue the associated data.

                Note that the components have clear boundaries, so that:

                    >>> cipher.update(b"builtin")
                    >>> cipher.update(b"securely")

                is not equivalent to:

                    >>> cipher.update(b"built")
                    >>> cipher.update(b"insecurely")

                If there is no associated data, this method must not be called.

                :Parameters:
                  component : bytes/bytearray/memoryview
                    The next associated data component.
        
        """
    def encrypt(self, plaintext):
        """

                For SIV, encryption and MAC authentication must take place at the same
                point. This method shall not be used.

                Use `encrypt_and_digest` instead.
        
        """
    def decrypt(self, ciphertext):
        """

                For SIV, decryption and verification must take place at the same
                point. This method shall not be used.

                Use `decrypt_and_verify` instead.
        
        """
    def digest(self):
        """
        Compute the *binary* MAC tag.

                The caller invokes this function at the very end.

                This method returns the MAC that shall be sent to the receiver,
                together with the ciphertext.

                :Return: the MAC, as a byte string.
        
        """
    def hexdigest(self):
        """
        Compute the *printable* MAC tag.

                This method is like `digest`.

                :Return: the MAC, as a hexadecimal string.
        
        """
    def verify(self, received_mac_tag):
        """
        Validate the *binary* MAC tag.

                The caller invokes this function at the very end.

                This method checks if the decrypted message is indeed valid
                (that is, if the key is correct) and it has not been
                tampered with while in transit.

                :Parameters:
                  received_mac_tag : bytes/bytearray/memoryview
                    This is the *binary* MAC, as received from the sender.
                :Raises ValueError:
                    if the MAC does not match. The message has been tampered with
                    or the key is incorrect.
        
        """
    def hexverify(self, hex_mac_tag):
        """
        Validate the *printable* MAC tag.

                This method is like `verify`.

                :Parameters:
                  hex_mac_tag : string
                    This is the *printable* MAC, as received from the sender.
                :Raises ValueError:
                    if the MAC does not match. The message has been tampered with
                    or the key is incorrect.
        
        """
    def encrypt_and_digest(self, plaintext, output=None):
        """
        Perform encrypt() and digest() in one step.

                :Parameters:
                  plaintext : bytes/bytearray/memoryview
                    The piece of data to encrypt.
                :Keywords:
                  output : bytearray/memoryview
                    The location where the ciphertext must be written to.
                    If ``None``, the ciphertext is returned.
                :Return:
                    a tuple with two items:

                    - the ciphertext, as ``bytes``
                    - the MAC tag, as ``bytes``

                    The first item becomes ``None`` when the ``output`` parameter
                    specified a location for the result.
        
        """
    def decrypt_and_verify(self, ciphertext, mac_tag, output=None):
        """
        Perform decryption and verification in one step.

                A cipher object is stateful: once you have decrypted a message
                you cannot decrypt (or encrypt) another message with the same
                object.

                You cannot reuse an object for encrypting
                or decrypting other data with the same key.

                This function does not remove any padding from the plaintext.

                :Parameters:
                  ciphertext : bytes/bytearray/memoryview
                    The piece of data to decrypt.
                    It can be of any length.
                  mac_tag : bytes/bytearray/memoryview
                    This is the *binary* MAC, as received from the sender.
                :Keywords:
                  output : bytearray/memoryview
                    The location where the plaintext must be written to.
                    If ``None``, the plaintext is returned.
                :Return: the plaintext as ``bytes`` or ``None`` when the ``output``
                    parameter specified a location for the result.
                :Raises ValueError:
                    if the MAC does not match. The message has been tampered with
                    or the key is incorrect.
        
        """
def _create_siv_cipher(factory, **kwargs):
    """
    Create a new block cipher, configured in
        Synthetic Initializaton Vector (SIV) mode.

        :Parameters:

          factory : object
            A symmetric cipher module from `Crypto.Cipher`
            (like `Crypto.Cipher.AES`).

        :Keywords:

          key : bytes/bytearray/memoryview
            The secret key to use in the symmetric cipher.
            It must be 32, 48 or 64 bytes long.
            If AES is the chosen cipher, the variants *AES-128*,
            *AES-192* and or *AES-256* will be used internally.

          nonce : bytes/bytearray/memoryview
            For deterministic encryption, it is not present.

            Otherwise, it is a value that must never be reused
            for encrypting message under this key.

            There are no restrictions on its length,
            but it is recommended to use at least 16 bytes.
    
    """
