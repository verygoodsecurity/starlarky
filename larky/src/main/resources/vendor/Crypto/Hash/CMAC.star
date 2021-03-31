def _shift_bytes(bs, xor_lsb=0):
    """
    A CMAC hash object.
        Do not instantiate directly. Use the :func:`new` function.

        :ivar digest_size: the size in bytes of the resulting MAC tag
        :vartype digest_size: integer
    
    """
2021-03-02 17:42:05,811 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, key, msg, ciphermod, cipher_params, mac_len,
                 update_after_digest):
        """
         Section 5.3 of NIST SP 800 38B and Appendix B

        """
    def update(self, msg):
        """
        Authenticate the next chunk of message.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of data
        
        """
    def _update(self, data_block):
        """
        Update a block aligned to the block boundary
        """
    def copy(self):
        """
        Return a copy ("clone") of the CMAC object.

                The copy will have the same internal state as the original CMAC
                object.
                This can be used to efficiently compute the MAC tag of byte
                strings that share a common initial substring.

                :return: An :class:`CMAC`
        
        """
    def digest(self):
        """
        Return the **binary** (non-printable) MAC tag of the message
                that has been authenticated so far.

                :return: The MAC tag, computed over the data processed so far.
                         Binary form.
                :rtype: byte string
        
        """
    def hexdigest(self):
        """
        Return the **printable** MAC tag of the message authenticated so far.

                :return: The MAC tag, computed over the data processed so far.
                         Hexadecimal encoded.
                :rtype: string
        
        """
    def verify(self, mac_tag):
        """
        Verify that a given **binary** MAC (computed by another party)
                is valid.

                Args:
                  mac_tag (byte string/byte array/memoryview): the expected MAC of the message.

                Raises:
                    ValueError: if the MAC does not match. It means that the message
                        has been tampered with or that the MAC key is incorrect.
        
        """
    def hexverify(self, hex_mac_tag):
        """
        Return the **printable** MAC tag of the message authenticated so far.

                :return: The MAC tag, computed over the data processed so far.
                         Hexadecimal encoded.
                :rtype: string
        
        """
2021-03-02 17:42:05,816 : INFO : tokenize_signature : --> do i ever get here?
def new(key, msg=None, ciphermod=None, cipher_params=None, mac_len=None,
        update_after_digest=False):
    """
    Create a new MAC object.

        Args:
            key (byte string/byte array/memoryview):
                key for the CMAC object.
                The key must be valid for the underlying cipher algorithm.
                For instance, it must be 16 bytes long for AES-128.
            ciphermod (module):
                A cipher module from :mod:`Crypto.Cipher`.
                The cipher's block size has to be 128 bits,
                like :mod:`Crypto.Cipher.AES`, to reduce the probability
                of collisions.
            msg (byte string/byte array/memoryview):
                Optional. The very first chunk of the message to authenticate.
                It is equivalent to an early call to `CMAC.update`. Optional.
            cipher_params (dict):
                Optional. A set of parameters to use when instantiating a cipher
                object.
            mac_len (integer):
                Length of the MAC, in bytes.
                It must be at least 4 bytes long.
                The default (and recommended) length matches the size of a cipher block.
            update_after_digest (boolean):
                Optional. By default, a hash object cannot be updated anymore after
                the digest is computed. When this flag is ``True``, such check
                is no longer enforced.
        Returns:
            A :class:`CMAC` object
    
    """
