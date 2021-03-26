def _build_impl(lib, postfix):
    """
    ghash
    """
def _get_ghash_portable():
    """
    %imp%
    """
def _get_ghash_clmul():
    """
    Return None if CLMUL implementation is not available
    """
def _GHASH(object):
    """
    GHASH function defined in NIST SP 800-38D, Algorithm 2.

        If X_1, X_2, .. X_m are the blocks of input data, the function
        computes:

           X_1*H^{m} + X_2*H^{m-1} + ... + X_m*H

        in the Galois field GF(2^256) using the reducing polynomial
        (x^128 + x^7 + x^2 + x + 1).
    
    """
    def __init__(self, subkey, ghash_c):
        """
        Error %d while expanding the GHASH key
        """
    def update(self, block_data):
        """
        Error %d while updating GHASH
        """
    def digest(self):
        """
        'Enum'
        """
def GcmMode(object):
    """
    Galois Counter Mode (GCM).

        This is an Authenticated Encryption with Associated Data (`AEAD`_) mode.
        It provides both confidentiality and authenticity.

        The header of the message may be left in the clear, if needed, and it will
        still be subject to authentication. The decryption step tells the receiver
        if the message comes from a source that really knowns the secret key.
        Additionally, decryption detects if any part of the message - including the
        header - has been modified or corrupted.

        This mode requires a *nonce*.

        This mode is only available for ciphers that operate on 128 bits blocks
        (e.g. AES but not TDES).

        See `NIST SP800-38D`_.

        .. _`NIST SP800-38D`: http://csrc.nist.gov/publications/nistpubs/800-38D/SP-800-38D.pdf
        .. _AEAD: http://blog.cryptographyengineering.com/2012/05/how-to-choose-authenticated-encryption.html

        :undocumented: __init__
    
    """
    def __init__(self, factory, key, nonce, mac_len, cipher_params, ghash_c):
        """
        GCM mode is only available for ciphers
         that operate on 128 bits blocks
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
                In GCM, the *associated data* is also called
                *additional authenticated data* (AAD).

                If there is no associated data, this method must not be called.

                The caller may split associated data in segments of any size, and
                invoke this method multiple times, each time with the next segment.

                :Parameters:
                  assoc_data : bytes/bytearray/memoryview
                    A piece of associated data. There are no restrictions on its size.
        
        """
    def _update(self, data):
        """
         The cache is exactly one block

        """
    def _pad_cache_and_update(self):
        """
         The authenticated data A is concatenated to the minimum
         number of zero bytes (possibly none) such that the
         - ciphertext C is aligned to the 16 byte boundary.
           See step 5 in section 7.1
         - ciphertext C is aligned to the 16 byte boundary.
           See step 6 in section 7.2

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
                  If ``output`` is ``None``, the ciphertext as ``bytes``.
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
                    The location where the plaintext must be written to.
                    If ``None``, the plaintext is returned.
                :Return:
                  If ``output`` is ``None``, the plaintext as ``bytes``.
                  Otherwise, ``None``.
        
        """
    def digest(self):
        """
        Compute the *binary* MAC tag in an AEAD mode.

                The caller invokes this function at the very end.

                This method returns the MAC that shall be sent to the receiver,
                together with the ciphertext.

                :Return: the MAC, as a byte string.
        
        """
    def _compute_mac(self):
        """
        Compute MAC without any FSM checks.
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
                  received_mac_tag : byte string
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
def _create_gcm_cipher(factory, **kwargs):
    """
    Create a new block cipher, configured in Galois Counter Mode (GCM).

        :Parameters:
          factory : module
            A block cipher module, taken from `Crypto.Cipher`.
            The cipher must have block length of 16 bytes.
            GCM has been only defined for `Crypto.Cipher.AES`.

        :Keywords:
          key : bytes/bytearray/memoryview
            The secret key to use in the symmetric cipher.
            It must be 16 (e.g. *AES-128*), 24 (e.g. *AES-192*)
            or 32 (e.g. *AES-256*) bytes long.

          nonce : bytes/bytearray/memoryview
            A value that must never be reused for any other encryption.

            There are no restrictions on its length,
            but it is recommended to use at least 16 bytes.

            The nonce shall never repeat for two
            different messages encrypted with the same key,
            but it does not need to be random.

            If not provided, a 16 byte nonce will be randomly created.

          mac_len : integer
            Length of the MAC, in bytes.
            It must be no larger than 16 bytes (which is the default).
    
    """
