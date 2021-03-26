def enum(**enums):
    """
    'Enum'
    """
def CcmMode(object):
    """
    Counter with CBC-MAC (CCM).

        This is an Authenticated Encryption with Associated Data (`AEAD`_) mode.
        It provides both confidentiality and authenticity.

        The header of the message may be left in the clear, if needed, and it will
        still be subject to authentication. The decryption step tells the receiver
        if the message comes from a source that really knowns the secret key.
        Additionally, decryption detects if any part of the message - including the
        header - has been modified or corrupted.

        This mode requires a nonce. The nonce shall never repeat for two
        different messages encrypted with the same key, but it does not need
        to be random.
        Note that there is a trade-off between the size of the nonce and the
        maximum size of a single message you can encrypt.

        It is important to use a large nonce if the key is reused across several
        messages and the nonce is chosen randomly.

        It is acceptable to us a short nonce if the key is only used a few times or
        if the nonce is taken from a counter.

        The following table shows the trade-off when the nonce is chosen at
        random. The column on the left shows how many messages it takes
        for the keystream to repeat **on average**. In practice, you will want to
        stop using the key way before that.

        +--------------------+---------------+-------------------+
        | Avg. # of messages |    nonce      |     Max. message  |
        | before keystream   |    size       |     size          |
        | repeats            |    (bytes)    |     (bytes)       |
        +====================+===============+===================+
        |       2^52         |      13       |        64K        |
        +--------------------+---------------+-------------------+
        |       2^48         |      12       |        16M        |
        +--------------------+---------------+-------------------+
        |       2^44         |      11       |         4G        |
        +--------------------+---------------+-------------------+
        |       2^40         |      10       |         1T        |
        +--------------------+---------------+-------------------+
        |       2^36         |       9       |        64P        |
        +--------------------+---------------+-------------------+
        |       2^32         |       8       |        16E        |
        +--------------------+---------------+-------------------+

        This mode is only available for ciphers that operate on 128 bits blocks
        (e.g. AES but not TDES).

        See `NIST SP800-38C`_ or RFC3610_.

        .. _`NIST SP800-38C`: http://csrc.nist.gov/publications/nistpubs/800-38C/SP800-38C.pdf
        .. _RFC3610: https://tools.ietf.org/html/rfc3610
        .. _AEAD: http://blog.cryptographyengineering.com/2012/05/how-to-choose-authenticated-encryption.html

        :undocumented: __init__
    
    """
2021-03-02 17:42:20,362 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, factory, key, nonce, mac_len, msg_len, assoc_len,
                 cipher_params):
        """
        The block size of the underlying cipher, in bytes.
        """
    def _start_mac(self):
        """
         Formatting control information and nonce (A.2.1)

        """
    def _pad_cache_and_update(self):
        """
         Associated data is concatenated with the least number
         of zero bytes (possibly none) to reach alignment to
         the 16 byte boundary (A.2.3)

        """
    def update(self, assoc_data):
        """
        Protect associated data

                If there is any associated data, the caller has to invoke
                this function one or more times, before using
                ``decrypt`` or ``encrypt``.

                By *associated data* it is meant any data (e.g. packet headers) that
                will not be encrypted and will be transmitted in the clear.
                However, the receiver is still able to detect any modification to it.
                In CCM, the *associated data* is also called
                *additional authenticated data* (AAD).

                If there is no associated data, this method must not be called.

                The caller may split associated data in segments of any size, and
                invoke this method multiple times, each time with the next segment.

                :Parameters:
                  assoc_data : bytes/bytearray/memoryview
                    A piece of associated data. There are no restrictions on its size.
        
        """
    def _update(self, assoc_data_pt=b""):
        """
        Update the MAC with associated data or plaintext
                   (without FSM checks)
        """
    def encrypt(self, plaintext, output=None):
        """
        Encrypt data with the key set at initialization.

                A cipher object is stateful: once you have encrypted a message
                you cannot encrypt (or decrypt) another message using the same
                object.

                This method can be called only **once** if ``msg_len`` was
                not passed at initialization.

                If ``msg_len`` was given, the data to encrypt can be broken
                up in two or more pieces and `encrypt` can be called
                multiple times.

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
                  If ``output`` is ``None``, the ciphertext as ``bytes``.
                  Otherwise, ``None``.
        
        """
    def decrypt(self, ciphertext, output=None):
        """
        Decrypt data with the key set at initialization.

                A cipher object is stateful: once you have decrypted a message
                you cannot decrypt (or encrypt) another message with the same
                object.

                This method can be called only **once** if ``msg_len`` was
                not passed at initialization.

                If ``msg_len`` was given, the data to decrypt can be
                broken up in two or more pieces and `decrypt` can be
                called multiple times.

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
                    The location where the plaintext must be written to.
                    If ``None``, the plaintext is returned.
                :Return:
                  If ``output`` is ``None``, the plaintext as ``bytes``.
                  Otherwise, ``None``.
        
        """
    def digest(self):
        """
        Compute the *binary* MAC tag.

                The caller invokes this function at the very end.

                This method returns the MAC that shall be sent to the receiver,
                together with the ciphertext.

                :Return: the MAC, as a byte string.
        
        """
    def _digest(self):
        """
        Associated data is too short
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
    def decrypt_and_verify(self, ciphertext, received_mac_tag, output=None):
        """
        Perform decrypt() and verify() in one step.

                :Parameters:
                  ciphertext : bytes/bytearray/memoryview
                    The piece of data to decrypt.
                  received_mac_tag : bytes/bytearray/memoryview
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
def _create_ccm_cipher(factory, **kwargs):
    """
    Create a new block cipher, configured in CCM mode.

        :Parameters:
          factory : module
            A symmetric cipher module from `Crypto.Cipher` (like
            `Crypto.Cipher.AES`).

        :Keywords:
          key : bytes/bytearray/memoryview
            The secret key to use in the symmetric cipher.

          nonce : bytes/bytearray/memoryview
            A value that must never be reused for any other encryption.

            Its length must be in the range ``[7..13]``.
            11 or 12 bytes are reasonable values in general. Bear in
            mind that with CCM there is a trade-off between nonce length and
            maximum message size.

            If not specified, a 11 byte long random string is used.

          mac_len : integer
            Length of the MAC, in bytes. It must be even and in
            the range ``[4..16]``. The default is 16.

          msg_len : integer
            Length of the message to (de)cipher.
            If not specified, ``encrypt`` or ``decrypt`` may only be called once.

          assoc_len : integer
            Length of the associated data.
            If not specified, all data is internally buffered.
    
    """
