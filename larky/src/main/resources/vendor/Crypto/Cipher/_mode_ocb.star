def OcbMode(object):
    """
    Offset Codebook (OCB) mode.

        :undocumented: __init__
    
    """
    def __init__(self, factory, nonce, mac_len, cipher_params):
        """
        OCB mode is only available for ciphers
         that operate on 128 bits blocks
        """
    def _update(self, assoc_data, assoc_data_len):
        """
        Error %d while computing MAC in OCB mode
        """
    def update(self, assoc_data):
        """
        Process the associated data.

                If there is any associated data, the caller has to invoke
                this method one or more times, before using
                ``decrypt`` or ``encrypt``.

                By *associated data* it is meant any data (e.g. packet headers) that
                will not be encrypted and will be transmitted in the clear.
                However, the receiver shall still able to detect modifications.

                If there is no associated data, this method must not be called.

                The caller may split associated data in segments of any size, and
                invoke this method multiple times, each time with the next segment.

                :Parameters:
                  assoc_data : bytes/bytearray/memoryview
                    A piece of associated data.
        
        """
2021-03-02 17:42:20,025 : INFO : tokenize_signature : --> do i ever get here?
    def _transcrypt_aligned(self, in_data, in_data_len,
                            trans_func, trans_desc):
        """
        Error %d while %sing in OCB mode

        """
    def _transcrypt(self, in_data, trans_func, trans_desc):
        """
         Last piece to encrypt/decrypt

        """
    def encrypt(self, plaintext=None):
        """
        Encrypt the next piece of plaintext.

                After the entire plaintext has been passed (but before `digest`),
                you **must** call this method one last time with no arguments to collect
                the final piece of ciphertext.

                If possible, use the method `encrypt_and_digest` instead.

                :Parameters:
                  plaintext : bytes/bytearray/memoryview
                    The next piece of data to encrypt or ``None`` to signify
                    that encryption has finished and that any remaining ciphertext
                    has to be produced.
                :Return:
                    the ciphertext, as a byte string.
                    Its length may not match the length of the *plaintext*.
        
        """
    def decrypt(self, ciphertext=None):
        """
        Decrypt the next piece of ciphertext.

                After the entire ciphertext has been passed (but before `verify`),
                you **must** call this method one last time with no arguments to collect
                the remaining piece of plaintext.

                If possible, use the method `decrypt_and_verify` instead.

                :Parameters:
                  ciphertext : bytes/bytearray/memoryview
                    The next piece of data to decrypt or ``None`` to signify
                    that decryption has finished and that any remaining plaintext
                    has to be produced.
                :Return:
                    the plaintext, as a byte string.
                    Its length may not match the length of the *ciphertext*.
        
        """
    def _compute_mac_tag(self):
        """
        b
        """
    def digest(self):
        """
        Compute the *binary* MAC tag.

                Call this method after the final `encrypt` (the one with no arguments)
                to obtain the MAC tag.

                The MAC tag is needed by the receiver to determine authenticity
                of the message.

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

                Call this method after the final `decrypt` (the one with no arguments)
                to check if the message is authentic and valid.

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
    def encrypt_and_digest(self, plaintext):
        """
        Encrypt the message and create the MAC tag in one step.

                :Parameters:
                  plaintext : bytes/bytearray/memoryview
                    The entire message to encrypt.
                :Return:
                    a tuple with two byte strings:

                    - the encrypted data
                    - the MAC
        
        """
    def decrypt_and_verify(self, ciphertext, received_mac_tag):
        """
        Decrypted the message and verify its authenticity in one step.

                :Parameters:
                  ciphertext : bytes/bytearray/memoryview
                    The entire message to decrypt.
                  received_mac_tag : byte string
                    This is the *binary* MAC, as received from the sender.

                :Return: the decrypted data (byte string).
                :Raises ValueError:
                    if the MAC does not match. The message has been tampered with
                    or the key is incorrect.
        
        """
def _create_ocb_cipher(factory, **kwargs):
    """
    Create a new block cipher, configured in OCB mode.

        :Parameters:
          factory : module
            A symmetric cipher module from `Crypto.Cipher`
            (like `Crypto.Cipher.AES`).

        :Keywords:
          nonce : bytes/bytearray/memoryview
            A  value that must never be reused for any other encryption.
            Its length can vary from 1 to 15 bytes.
            If not specified, a random 15 bytes long nonce is generated.

          mac_len : integer
            Length of the MAC, in bytes.
            It must be in the range ``[8..16]``.
            The default is 16 (128 bits).

        Any other keyword will be passed to the underlying block cipher.
        See the relevant documentation for details (at least ``key`` will need
        to be present).
    
    """
