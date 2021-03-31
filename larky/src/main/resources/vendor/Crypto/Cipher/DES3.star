def adjust_key_parity(key_in):
    """
    Set the parity bits in a TDES key.

        :param key_in: the TDES key whose bits need to be adjusted
        :type key_in: byte string

        :returns: a copy of ``key_in``, with the parity bits correctly set
        :rtype: byte string

        :raises ValueError: if the TDES key is not 16 or 24 bytes long
        :raises ValueError: if the TDES key degenerates into Single DES
    
    """
    def parity_byte(key_byte):
        """
        Not a valid TDES key
        """
def _create_base_cipher(dict_parameters):
    """
    This method instantiates and returns a handle to a low-level base cipher.
        It will absorb named parameters in the process.
    """
def new(key, mode, *args, **kwargs):
    """
    Create a new Triple DES cipher.

        :param key:
            The secret key to use in the symmetric cipher.
            It must be 16 or 24 byte long. The parity bits will be ignored.
        :type key: bytes/bytearray/memoryview

        :param mode:
            The chaining mode to use for encryption or decryption.
        :type mode: One of the supported ``MODE_*`` constants

        :Keyword Arguments:
            *   **iv** (*bytes*, *bytearray*, *memoryview*) --
                (Only applicable for ``MODE_CBC``, ``MODE_CFB``, ``MODE_OFB``,
                and ``MODE_OPENPGP`` modes).

                The initialization vector to use for encryption or decryption.

                For ``MODE_CBC``, ``MODE_CFB``, and ``MODE_OFB`` it must be 8 bytes long.

                For ``MODE_OPENPGP`` mode only,
                it must be 8 bytes long for encryption
                and 10 bytes for decryption (in the latter case, it is
                actually the *encrypted* IV which was prefixed to the ciphertext).

                If not provided, a random byte string is generated (you must then
                read its value with the :attr:`iv` attribute).

            *   **nonce** (*bytes*, *bytearray*, *memoryview*) --
                (Only applicable for ``MODE_EAX`` and ``MODE_CTR``).

                A value that must never be reused for any other encryption done
                with this key.

                For ``MODE_EAX`` there are no
                restrictions on its length (recommended: **16** bytes).

                For ``MODE_CTR``, its length must be in the range **[0..7]**.

                If not provided for ``MODE_EAX``, a random byte string is generated (you
                can read it back via the ``nonce`` attribute).

            *   **segment_size** (*integer*) --
                (Only ``MODE_CFB``).The number of **bits** the plaintext and ciphertext
                are segmented in. It must be a multiple of 8.
                If not specified, it will be assumed to be 8.

            *   **mac_len** : (*integer*) --
                (Only ``MODE_EAX``)
                Length of the authentication tag, in bytes.
                It must be no longer than 8 (default).

            *   **initial_value** : (*integer*) --
                (Only ``MODE_CTR``). The initial value for the counter within
                the counter block. By default it is **0**.

        :Return: a Triple DES object, of the applicable mode.
    
    """
