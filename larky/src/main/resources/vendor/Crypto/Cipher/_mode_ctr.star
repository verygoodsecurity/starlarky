def CtrMode(object):
    """
    *CounTeR (CTR)* mode.

        This mode is very similar to ECB, in that
        encryption of one block is done independently of all other blocks.

        Unlike ECB, the block *position* contributes to the encryption
        and no information leaks about symbol frequency.

        Each message block is associated to a *counter* which
        must be unique across all messages that get encrypted
        with the same key (not just within the same message).
        The counter is as big as the block size.

        Counters can be generated in several ways. The most
        straightword one is to choose an *initial counter block*
        (which can be made public, similarly to the *IV* for the
        other modes) and increment its lowest **m** bits by one
        (modulo *2^m*) for each block. In most cases, **m** is
        chosen to be half the block size.

        See `NIST SP800-38A`_, Section 6.5 (for the mode) and
        Appendix B (for how to manage the *initial counter block*).

        .. _`NIST SP800-38A` : http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf

        :undocumented: __init__
    
    """
2021-03-02 17:42:17,491 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, block_cipher, initial_counter_block,
                 prefix_len, counter_len, little_endian):
        """
        Create a new block cipher, configured in CTR mode.

                :Parameters:
                  block_cipher : C pointer
                    A smart pointer to the low-level block cipher instance.

                  initial_counter_block : bytes/bytearray/memoryview
                    The initial plaintext to use to generate the key stream.

                    It is as large as the cipher block, and it embeds
                    the initial value of the counter.

                    This value must not be reused.
                    It shall contain a nonce or a random component.
                    Reusing the *initial counter block* for encryptions
                    performed with the same key compromises confidentiality.

                  prefix_len : integer
                    The amount of bytes at the beginning of the counter block
                    that never change.

                  counter_len : integer
                    The length in bytes of the counter embedded in the counter
                    block.

                  little_endian : boolean
                    True if the counter in the counter block is an integer encoded
                    in little endian mode. If False, it is big endian.
        
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
                    The location where the plaintext must be written to.
                    If ``None``, the plaintext is returned.
                :Return:
                  If ``output`` is ``None``, the plaintext is returned as ``bytes``.
                  Otherwise, ``None``.
        
        """
def _create_ctr_cipher(factory, **kwargs):
    """
    Instantiate a cipher object that performs CTR encryption/decryption.

        :Parameters:
          factory : module
            The underlying block cipher, a module from ``Crypto.Cipher``.

        :Keywords:
          nonce : bytes/bytearray/memoryview
            The fixed part at the beginning of the counter block - the rest is
            the counter number that gets increased when processing the next block.
            The nonce must be such that no two messages are encrypted under the
            same key and the same nonce.

            The nonce must be shorter than the block size (it can have
            zero length; the counter is then as long as the block).

            If this parameter is not present, a random nonce will be created with
            length equal to half the block size. No random nonce shorter than
            64 bits will be created though - you must really think through all
            security consequences of using such a short block size.

          initial_value : posive integer or bytes/bytearray/memoryview
            The initial value for the counter. If not present, the cipher will
            start counting from 0. The value is incremented by one for each block.
            The counter number is encoded in big endian mode.

          counter : object
            Instance of ``Crypto.Util.Counter``, which allows full customization
            of the counter block. This parameter is incompatible to both ``nonce``
            and ``initial_value``.

        Any other keyword will be passed to the underlying block cipher.
        See the relevant documentation for details (at least ``key`` will need
        to be present).
    
    """
