def _create_base_cipher(dict_parameters):
    """
    This method instantiates and returns a handle to a low-level
        base cipher. It will absorb named parameters in the process.
    """
def _derive_Poly1305_key_pair(key, nonce):
    """
    Derive a tuple (r, s, nonce) for a Poly1305 MAC.
    
        If nonce is ``None``, a new 16-byte nonce is generated.
    
    """
def new(key, mode, *args, **kwargs):
    """
    Create a new AES cipher.

        :param key:
            The secret key to use in the symmetric cipher.

            It must be 16, 24 or 32 bytes long (respectively for *AES-128*,
            *AES-192* or *AES-256*).

            For ``MODE_SIV`` only, it doubles to 32, 48, or 64 bytes.
        :type key: bytes/bytearray/memoryview

        :param mode:
            The chaining mode to use for encryption or decryption.
            If in doubt, use ``MODE_EAX``.
        :type mode: One of the supported ``MODE_*`` constants

        :Keyword Arguments:
            *   **iv** (*bytes*, *bytearray*, *memoryview*) --
                (Only applicable for ``MODE_CBC``, ``MODE_CFB``, ``MODE_OFB``,
                and ``MODE_OPENPGP`` modes).

                The initialization vector to use for encryption or decryption.

                For ``MODE_CBC``, ``MODE_CFB``, and ``MODE_OFB`` it must be 16 bytes long.

                For ``MODE_OPENPGP`` mode only,
                it must be 16 bytes long for encryption
                and 18 bytes for decryption (in the latter case, it is
                actually the *encrypted* IV which was prefixed to the ciphertext).

                If not provided, a random byte string is generated (you must then
                read its value with the :attr:`iv` attribute).

            *   **nonce** (*bytes*, *bytearray*, *memoryview*) --
                (Only applicable for ``MODE_CCM``, ``MODE_EAX``, ``MODE_GCM``,
                ``MODE_SIV``, ``MODE_OCB``, and ``MODE_CTR``).

                A value that must never be reused for any other encryption done
                with this key (except possibly for ``MODE_SIV``, see below).

                For ``MODE_EAX``, ``MODE_GCM`` and ``MODE_SIV`` there are no
                restrictions on its length (recommended: **16** bytes).

                For ``MODE_CCM``, its length must be in the range **[7..13]**.
                Bear in mind that with CCM there is a trade-off between nonce
                length and maximum message size. Recommendation: **11** bytes.

                For ``MODE_OCB``, its length must be in the range **[1..15]**
                (recommended: **15**).

                For ``MODE_CTR``, its length must be in the range **[0..15]**
                (recommended: **8**).
            
                For ``MODE_SIV``, the nonce is optional, if it is not specified,
                then no nonce is being used, which renders the encryption
                deterministic.

                If not provided, for modes other than ``MODE_SIV```, a random
                byte string of the recommended length is used (you must then
                read its value with the :attr:`nonce` attribute).

            *   **segment_size** (*integer*) --
                (Only ``MODE_CFB``).The number of **bits** the plaintext and ciphertext
                are segmented in. It must be a multiple of 8.
                If not specified, it will be assumed to be 8.

            *   **mac_len** : (*integer*) --
                (Only ``MODE_EAX``, ``MODE_GCM``, ``MODE_OCB``, ``MODE_CCM``)
                Length of the authentication tag, in bytes.

                It must be even and in the range **[4..16]**.
                The recommended value (and the default, if not specified) is **16**.

            *   **msg_len** : (*integer*) --
                (Only ``MODE_CCM``). Length of the message to (de)cipher.
                If not specified, ``encrypt`` must be called with the entire message.
                Similarly, ``decrypt`` can only be called once.

            *   **assoc_len** : (*integer*) --
                (Only ``MODE_CCM``). Length of the associated data.
                If not specified, all associated data is buffered internally,
                which may represent a problem for very large messages.

            *   **initial_value** : (*integer* or *bytes/bytearray/memoryview*) --
                (Only ``MODE_CTR``).
                The initial value for the counter. If not present, the cipher will
                start counting from 0. The value is incremented by one for each block.
                The counter number is encoded in big endian mode.

            *   **counter** : (*object*) --
                Instance of ``Crypto.Util.Counter``, which allows full customization
                of the counter block. This parameter is incompatible to both ``nonce``
                and ``initial_value``.

            *   **use_aesni** : (*boolean*) --
                Use Intel AES-NI hardware extensions (default: use if available).

        :Return: an AES object, of the applicable mode.
    
    """
