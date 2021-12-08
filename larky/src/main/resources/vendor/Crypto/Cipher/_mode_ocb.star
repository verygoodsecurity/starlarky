# ===================================================================
#
# Copyright (c) 2014, Legrandin <helderijs@gmail.com>
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in
#    the documentation and/or other materials provided with the
#    distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# ===================================================================

"""
Offset Codebook (OCB) mode.

OCB is Authenticated Encryption with Associated Data (AEAD) cipher mode
designed by Prof. Phillip Rogaway and specified in `RFC7253`_.

The algorithm provides both authenticity and privacy, it is very efficient,
it uses only one key and it can be used in online mode (so that encryption
or decryption can start before the end of the message is available).

This module implements the third and last variant of OCB (OCB3) and it only
works in combination with a 128-bit block symmetric cipher, like AES.

OCB is patented in US but `free licenses`_ exist for software implementations
meant for non-military purposes.

Example:
    >>> from Crypto.Cipher import AES
    >>> from Crypto.Random import get_random_bytes
    >>>
    >>> key = get_random_bytes(32)
    >>> cipher = AES.new(key, AES.MODE_OCB)
    >>> plaintext = b"Attack at dawn"
    >>> ciphertext, mac = cipher.encrypt_and_digest(plaintext)
    >>> # Deliver cipher.nonce, ciphertext and mac
    ...
    >>> cipher = AES.new(key, AES.MODE_OCB, nonce=nonce)
    >>> try:
    >>>     plaintext = cipher.decrypt_and_verify(ciphertext, mac)
    >>> except ValueError:
    >>>     print "Invalid message"
    >>> else:
    >>>     print plaintext

:undocumented: __package__

.. _RFC7253: http://www.rfc-editor.org/info/rfc7253
.. _free licenses: http://web.cs.ucdavis.edu/~rogaway/ocb/license.htm
"""

load("@stdlib//struct", struct="struct")
load("@stdlib//binascii", unhexlify="unhexlify")

load("@vendor//Crypto/Util/py3compat", bord="bord", _copy_bytes="_copy_bytes")
load("@vendor//Crypto/Util/number", long_to_bytes="long_to_bytes", bytes_to_long="bytes_to_long")
load("@vendor//Crypto/Util/strxor", strxor="strxor")

load("@vendor//Crypto/Hash", BLAKE2s="BLAKE2s")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")

load("@vendor//Crypto/Util/_raw_api", load_pycryptodome_raw_lib="load_pycryptodome_raw_lib", VoidPointer="VoidPointer", create_string_buffer="create_string_buffer", get_raw_buffer="get_raw_buffer", SmartPointer="SmartPointer", c_size_t="c_size_t", c_uint8_ptr="c_uint8_ptr", is_buffer="is_buffer")
load("@stdlib//builtins","builtins")

_raw_ocb_lib = load_pycryptodome_raw_lib("Crypto.Cipher._raw_ocb", """
                                    int OCB_start_operation(void *cipher,
                                        const uint8_t *offset_0,
                                        size_t offset_0_len,
                                        void **pState);
                                    int OCB_encrypt(void *state,
                                        const uint8_t *in,
                                        uint8_t *out,
                                        size_t data_len);
                                    int OCB_decrypt(void *state,
                                        const uint8_t *in,
                                        uint8_t *out,
                                        size_t data_len);
                                    int OCB_update(void *state,
                                        const uint8_t *in,
                                        size_t data_len);
                                    int OCB_digest(void *state,
                                        uint8_t *tag,
                                        size_t tag_len);
                                    int OCB_stop_operation(void *state);
                                    """)
def OcbMode(factory, nonce, mac_len, cipher_params):
    """Offset Codebook (OCB) mode.

    :undocumented: __init__
    """

    def __init__(factory, nonce, mac_len, cipher_params):

        if factory.block_size != 16:
            fail(" ValueError(\"OCB mode is only available for ciphers\"\n                             \" that operate on 128 bits blocks\")")

        self.block_size = 16
        """The block size of the underlying cipher, in bytes."""

        self.nonce = _copy_bytes(None, None, nonce)
        """Nonce used for this session."""
        if len(nonce) not in range(1, 16):
            fail(" ValueError(\"Nonce must be at most 15 bytes long\")")
        if not is_buffer(nonce):
            fail(" TypeError(\"Nonce must be bytes, bytearray or memoryview\")")

        self._mac_len = mac_len
        if not (8 <= mac_len) and (mac_len <= 16):
            fail(" ValueError(\"MAC tag must be between 8 and 16 bytes long\")")

        # Cache for MAC tag
        self._mac_tag = None

        # Cache for unaligned associated data
        self._cache_A = bytes(r"", encoding='utf-8')

        # Cache for unaligned ciphertext/plaintext
        self._cache_P = bytes(r"", encoding='utf-8')

        # Allowed transitions after initialization
        self._next = [self.update, self.encrypt, self.decrypt,
                      self.digest, self.verify]

        # Compute Offset_0
        params_without_key = dict(cipher_params)
        key = params_without_key.pop("key")
        nonce = (struct.pack('B', self._mac_len << 4 & 0xFF) +
                 bytes([0x00]) * (14 - len(nonce)) +
                 bytes([0x01]) + self.nonce)

        bottom_bits = bord(nonce[15]) & 0x3F    # 6 bits, 0..63
        top_bits = bord(nonce[15]) & 0xC0       # 2 bits

        ktop_cipher = factory.new(key,
                                  factory.MODE_ECB,
                                  **params_without_key)
        ktop = ktop_cipher.encrypt(struct.pack('15sB',
                                               nonce[:15],
                                               top_bits))

        stretch = ktop + strxor(ktop[:8], ktop[1:9])    # 192 bits
        offset_0 = long_to_bytes(bytes_to_long(stretch) >>
                                 (64 - bottom_bits), 24)[8:]

        # Create low-level cipher instance
        raw_cipher = factory._create_base_cipher(cipher_params)
        if cipher_params:
            fail(" TypeError(\"Unknown keywords: \" + str(cipher_params))")

        self._state = VoidPointer()
        result = _raw_ocb_lib.OCB_start_operation(raw_cipher.get(),
                                                  offset_0,
                                                  c_size_t(len(offset_0)),
                                                  self._state.address_of())
        if result:
            fail(" ValueError(\"Error %d while instantiating the OCB mode\"\n                             % result)")

        # Ensure that object disposal of this Python object will (eventually)
        # free the memory allocated by the raw library for the cipher mode
        self._state = SmartPointer(self._state.get(),
                                   _raw_ocb_lib.OCB_stop_operation)

        # Memory allocated for the underlying block cipher is now owed
        # by the cipher mode
        raw_cipher.release()
    self = __init__(factory, nonce, mac_len, cipher_params)

    def _update(assoc_data, assoc_data_len):
        result = _raw_ocb_lib.OCB_update(self._state.get(),
                                         c_uint8_ptr(assoc_data),
                                         c_size_t(assoc_data_len))
        if result:
            fail(" ValueError(\"Error %d while computing MAC in OCB mode\" % result)")
    self._update = _update

    def update(assoc_data):
        """Process the associated data.

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

        if self.update not in self._next:
            fail(" TypeError(\"update() can only be called\"\n                            \" immediately after initialization\")")

        self._next = [self.encrypt, self.decrypt, self.digest,
                      self.verify, self.update]

        if len(self._cache_A) > 0:
            filler = min(16 - len(self._cache_A), len(assoc_data))
            self._cache_A += _copy_bytes(None, filler, assoc_data)
            assoc_data = assoc_data[filler:]

            if len(self._cache_A) < 16:
                return self

            # Clear the cache, and proceeding with any other aligned data
            self._cache_A, seg = bytes(r"", encoding='utf-8'), self._cache_A
            self.update(seg)

        update_len = len(assoc_data) // 16 * 16
        self._cache_A = _copy_bytes(update_len, None, assoc_data)
        self._update(assoc_data, update_len)
        return self
    self.update = update

    def _transcrypt_aligned(in_data, in_data_len,
                            trans_func, trans_desc):

        out_data = create_string_buffer(in_data_len)
        result = trans_func(self._state.get(),
                            in_data,
                            out_data,
                            c_size_t(in_data_len))
        if result:
            fail(" ValueError(\"Error %d while %sing in OCB mode\"\n                             % (result, trans_desc))")
        return get_raw_buffer(out_data)
    self._transcrypt_aligned = _transcrypt_aligned

    def _transcrypt(in_data, trans_func, trans_desc):
        # Last piece to encrypt/decrypt
        if in_data == None:
            out_data = self._transcrypt_aligned(self._cache_P,
                                                len(self._cache_P),
                                                trans_func,
                                                trans_desc)
            self._cache_P = bytes(r"", encoding='utf-8')
            return out_data

        # Try to fill up the cache, if it already contains something
        prefix = bytes(r"", encoding='utf-8')
        if len(self._cache_P) > 0:
            filler = min(16 - len(self._cache_P), len(in_data))
            self._cache_P += _copy_bytes(None, filler, in_data)
            in_data = in_data[filler:]

            if len(self._cache_P) < 16:
                # We could not manage to fill the cache, so there is certainly
                # no output yet.
                return bytes(r"", encoding='utf-8')

            # Clear the cache, and proceeding with any other aligned data
            prefix = self._transcrypt_aligned(self._cache_P,
                                              len(self._cache_P),
                                              trans_func,
                                              trans_desc)
            self._cache_P = bytes(r"", encoding='utf-8')

        # Process data in multiples of the block size
        trans_len = len(in_data) // 16 * 16
        result = self._transcrypt_aligned(c_uint8_ptr(in_data),
                                          trans_len,
                                          trans_func,
                                          trans_desc)
        if prefix:
            result = prefix + result

        # Left-over
        self._cache_P = _copy_bytes(trans_len, None, in_data)

        return result
    self._transcrypt = _transcrypt

    def encrypt(plaintext=None):
        """Encrypt the next piece of plaintext.

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

        if self.encrypt not in self._next:
            fail(" TypeError(\"encrypt() can only be called after\"\n                            \" initialization or an update()\")")

        if plaintext == None:
            self._next = [self.digest]
        else:
            self._next = [self.encrypt]
        return self._transcrypt(plaintext, _raw_ocb_lib.OCB_encrypt, "encrypt")
    self.encrypt = encrypt

    def decrypt(ciphertext=None):
        """Decrypt the next piece of ciphertext.

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

        if self.decrypt not in self._next:
            fail(" TypeError(\"decrypt() can only be called after\"\n                            \" initialization or an update()\")")

        if ciphertext == None:
            self._next = [self.verify]
        else:
            self._next = [self.decrypt]
        return self._transcrypt(ciphertext,
                                _raw_ocb_lib.OCB_decrypt,
                                "decrypt")
    self.decrypt = decrypt

    def _compute_mac_tag():

        if self._mac_tag != None:
            return

        if self._cache_A:
            self._update(self._cache_A, len(self._cache_A))
            self._cache_A = bytes(r"", encoding='utf-8')

        mac_tag = create_string_buffer(16)
        result = _raw_ocb_lib.OCB_digest(self._state.get(),
                                         mac_tag,
                                         c_size_t(len(mac_tag))
                                         )
        if result:
            fail(" ValueError(\"Error %d while computing digest in OCB mode\"\n                             % result)")
        self._mac_tag = get_raw_buffer(mac_tag)[:self._mac_len]
    self._compute_mac_tag = _compute_mac_tag

    def digest():
        """Compute the *binary* MAC tag.

        Call this method after the final `encrypt` (the one with no arguments)
        to obtain the MAC tag.

        The MAC tag is needed by the receiver to determine authenticity
        of the message.

        :Return: the MAC, as a byte string.
        """

        if self.digest not in self._next:
            fail(" TypeError(\"digest() cannot be called now for this cipher\")")

        assert(len(self._cache_P) == 0)

        self._next = [self.digest]

        if self._mac_tag == None:
            self._compute_mac_tag()

        return self._mac_tag
    self.digest = digest

    def hexdigest():
        """Compute the *printable* MAC tag.

        This method is like `digest`.

        :Return: the MAC, as a hexadecimal string.
        """
        return "".join(["%02x" % bord(x) for x in self.digest()])
    self.hexdigest = hexdigest

    def verify(received_mac_tag):
        """Validate the *binary* MAC tag.

        Call this method after the final `decrypt` (the one with no arguments)
        to check if the message is authentic and valid.

        :Parameters:
          received_mac_tag : bytes/bytearray/memoryview
            This is the *binary* MAC, as received from the sender.
        :Raises ValueError:
            if the MAC does not match. The message has been tampered with
            or the key is incorrect.
        """

        if self.verify not in self._next:
            fail(" TypeError(\"verify() cannot be called now for this cipher\")")

        assert(len(self._cache_P) == 0)

        self._next = [self.verify]

        if self._mac_tag == None:
            self._compute_mac_tag()

        secret = get_random_bytes(16)
        mac1 = BLAKE2s.new(digest_bits=160, key=secret, data=self._mac_tag)
        mac2 = BLAKE2s.new(digest_bits=160, key=secret, data=received_mac_tag)

        if mac1.digest() != mac2.digest():
            fail(" ValueError(\"MAC check failed\")")
    self.verify = verify

    def hexverify(hex_mac_tag):
        """Validate the *printable* MAC tag.

        This method is like `verify`.

        :Parameters:
          hex_mac_tag : string
            This is the *printable* MAC, as received from the sender.
        :Raises ValueError:
            if the MAC does not match. The message has been tampered with
            or the key is incorrect.
        """

        self.verify(unhexlify(hex_mac_tag))
    self.hexverify = hexverify

    def encrypt_and_digest(plaintext):
        """Encrypt the message and create the MAC tag in one step.

        :Parameters:
          plaintext : bytes/bytearray/memoryview
            The entire message to encrypt.
        :Return:
            a tuple with two byte strings:

            - the encrypted data
            - the MAC
        """

        return self.encrypt(plaintext) + self.encrypt(), self.digest()
    self.encrypt_and_digest = encrypt_and_digest

    def decrypt_and_verify(ciphertext, received_mac_tag):
        """Decrypted the message and verify its authenticity in one step.

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

        plaintext = self.decrypt(ciphertext) + self.decrypt()
        self.verify(received_mac_tag)
        return plaintext
    self.decrypt_and_verify = decrypt_and_verify
    return self


def _create_ocb_cipher(factory, **kwargs):
    """Create a new block cipher, configured in OCB mode.

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

    try:
        nonce = kwargs.pop("nonce", None)
        if nonce == None:
            nonce = get_random_bytes(15)
        mac_len = kwargs.pop("mac_len", 16)
    except KeyError as e:
        fail(" TypeError(\"Keyword missing: \" + str(e))")

    return OcbMode(factory, nonce, mac_len, kwargs)

