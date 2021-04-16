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
Galois/Counter Mode (GCM).
"""

load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//builtins","builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Hash/BLAKE2s", BLAKE2s="BLAKE2s")
load("@vendor//Crypto/Cipher/_mode_ecb", EcbMode="EcbMode")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")
load("@vendor//Crypto/Util/number", long_to_bytes="long_to_bytes", bytes_to_long="bytes_to_long")
load("@vendor//Crypto/Util/py3compat", bord="bord", _copy_bytes="copy_bytes")


__all__ = ['GcmMode']


# def _GHASH(subkey, ghash_c):
#     """GHASH function defined in NIST SP 800-38D, Algorithm 2.
#
#     If X_1, X_2, .. X_m are the blocks of input data, the function
#     computes:
#
#        X_1*H^{m} + X_2*H^{m-1} + ... + X_m*H
#
#     in the Galois field GF(2^256) using the reducing polynomial
#     (x^128 + x^7 + x^2 + x + 1).
#     """
#
#     def __init__(subkey, ghash_c):
#         assert len(subkey) == 16
#
#         self.ghash_c = ghash_c
#
#         self._exp_key = VoidPointer()
#         result = ghash_c.ghash_expand(c_uint8_ptr(subkey),
#                                       self._exp_key.address_of())
#         if result:
#             fail("ValueError: Error %d while expanding the GHASH key" % result)
#
#         self._exp_key = SmartPointer(self._exp_key.get(),
#                                      ghash_c.ghash_destroy)
#
#         # create_string_buffer always returns a string of zeroes
#         self._last_y = create_string_buffer(16)
#     self = __init__(subkey, ghash_c)
#
#     def update(block_data):
#         assert len(block_data) % 16 == 0
#
#         result = self.ghash_c.ghash(self._last_y,
#                                     c_uint8_ptr(block_data),
#                                     c_size_t(len(block_data)),
#                                     self._last_y,
#                                     self._exp_key.get())
#         if result:
#             fail("ValueError: Error %d while updating GHASH" % result)
#
#         return self
#     self.update = update
#
#     def digest():
#         return get_raw_buffer(self._last_y)
#     self.digest = digest
#     return self


def enum(**enums):
    return larky.struct(**enums)


MacStatus = enum(PROCESSING_AUTH_DATA=1, PROCESSING_CIPHERTEXT=2)
#def GcmMode(factory, key, nonce, mac_len, cipher_params, ghash_c):
def _GcmMode(factory, key, nonce, mac_len, cipher_params):
    """Galois Counter Mode (GCM).

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

    # def __init__(factory, key, nonce, mac_len, cipher_params, ghash_c):
    def __init__(factory, key, nonce, mac_len, cipher_params):
        #print("XXXX:", factory, len(key), len(nonce), mac_len, cipher_params)
        self_ = {}
        self_["block_size"] = factory.block_size
        if self_["block_size"] != 16:
            fail("ValueError: GCM mode is only available for ciphers that operate on 128 bits blocks")

        if len(nonce) == 0:
            fail("ValueError: Nonce cannot be empty")

        if not types.is_bytelike(nonce):
            fail("TypeError: Nonce must be bytes, bytearray or memoryview")

        # See NIST SP 800 38D, 5.2.1.1
        if len(nonce) > (pow(2, 64) - 1):
            fail("ValueError: Nonce exceeds maximum length")

        self_["nonce"] = _copy_bytes(None, None, nonce)
        """Nonce"""

        self_["_factory"] = factory
        self_["_key"] = _copy_bytes(None, None, key)
        self_["_tag"] = None  # Cache for MAC tag

        self_["_mac_len"] = mac_len
        if not ((4 <= mac_len) and (mac_len <= 16)):
            fail('ValueError: Parameter "mac_len" must be in the range 4..16')

        self_["_no_more_assoc_data"] = False

        # Length of associated data
        self_["_auth_len"] = 0

        # Length of the ciphertext or plaintext
        self_["_msg_len"] = 0

        # Step 1 in SP800-38D, Algorithm 4 (encryption) - Compute H
        # See also Algorithm 5 (decryption)

        EcbMode._create_ecb_cipher(factory, key=key, **cipher_params).encrypt(bytes([0x00] * 16))
        # hash_subkey = factory._create_ecb_cipher(key,
        #                           factory.MODE_ECB,
        #                           **cipher_params
        #                           ).encrypt(bytes([0x00]) * 16)
        #
        # # Step 2 - Compute J0
        # if len(self.nonce) == 12:
        #     j0 = self.nonce + bytes([0x00, 0x00, 0x00, 0x01])
        # else:
        #     fill = (16 - (len(nonce) % 16)) % 16 + 8
        #     ghash_in = (self.nonce +
        #                 bytes([0x00]) * fill +
        #                 long_to_bytes(8 * len(nonce), 8))
        #     j0 = _GHASH(hash_subkey, ghash_c).update(ghash_in).digest()
        #
        # # Step 3 - Prepare GCTR cipher for encryption/decryption
        # nonce_ctr = j0[:12]
        # iv_ctr = (bytes_to_long(j0) + 1) & 0xFFFFFFFF
        # self._cipher = factory.new(key,
        #                            self._factory.MODE_CTR,
        #                            initial_value=iv_ctr,
        #                            nonce=nonce_ctr,
        #                            **cipher_params)
        #
        # # Step 5 - Bootstrat GHASH
        # self._signer = _GHASH(hash_subkey, ghash_c)
        #
        # # Step 6 - Prepare GCTR cipher for GMAC
        # self._tag_cipher = factory.new(key,
        #                                self._factory.MODE_CTR,
        #                                initial_value=j0,
        #                                nonce=bytes(r"", encoding='utf-8'),
        #                                **cipher_params)

        # Cache for data to authenticate
        self_["_cache"] = bytes(r"", encoding='utf-8')
        self_["_status"] = MacStatus.PROCESSING_AUTH_DATA
        self_["_state"] = _JCrypto.Cipher.GCMMode(
            factory._create_base_cipher(dict(key=self_["_key"])),
            self_["_mac_len"],
            self_["nonce"],
            self_["_cache"]
        )
        return larky.mutablestruct(**self_)
    self = __init__(factory, key, nonce, mac_len, cipher_params)

    def _pad_cache_and_update():
        if len(self._cache) >= 16:
            fail("self._cache has more than 16 entries!")

        # The authenticated data A is concatenated to the minimum
        # number of zero bytes (possibly none) such that the
        # - ciphertext C is aligned to the 16 byte boundary.
        #   See step 5 in section 7.1
        # - ciphertext C is aligned to the 16 byte boundary.
        #   See step 6 in section 7.2
        len_cache = len(self._cache)
        if len_cache > 0:
            self._update(bytes([0x00]) * (16 - len_cache))
    self._pad_cache_and_update = _pad_cache_and_update

    def encrypt(plaintext, output=None):
        """Encrypt data with the key and the parameters set at initialization.

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
        if not types.is_bytelike(plaintext):
            fail('TypeError: plaintext is not byte-like')
        if self.encrypt not in self._next:
            fail("TypeError: encrypt() can only be called after initialization or an update()")
        self._next = [self.encrypt, self.digest]

        #ciphertext = self._cipher.encrypt(plaintext, output=output)
        ciphertext, tail, mac = self._state.encrypt(plaintext, output=output)
        self._tag = mac

        if self._status == MacStatus.PROCESSING_AUTH_DATA:
            self._pad_cache_and_update()
            self._status = MacStatus.PROCESSING_CIPHERTEXT

        # self._update(ciphertext if output == None else output)
        self._msg_len += len(plaintext)

        # See NIST SP 800 38D, 5.2.1.1
        if self._msg_len > (pow(2,39) - 256):
            fail("ValueError: Plaintext exceeds maximum length")

        return ciphertext
    self.encrypt = encrypt

    def decrypt(ciphertext, output=None):
        """Decrypt data with the key and the parameters set at initialization.

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
        if not types.is_bytelike(ciphertext):
            fail('TypeError: ciphertext is not byte-like')

        if self.decrypt not in self._next:
            fail("TypeError: decrypt() can only be called after initialization or an update()")
        self._next = [self.decrypt, self.verify]

        if self._status == MacStatus.PROCESSING_AUTH_DATA:
            # self._pad_cache_and_update()
            self._status = MacStatus.PROCESSING_CIPHERTEXT

        # self._update(ciphertext)
        # self._msg_len += len(ciphertext)

        plaintext, _, mac = self._state.decrypt(ciphertext, output=output)
        self._tag = mac
        return plaintext
    self.decrypt = decrypt

    def update(assoc_data):
        """Protect associated data

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
        if not types.is_bytelike(assoc_data):
            fail('TypeError: assoc_data is not byte-like')

        if self.update not in self._next:
            fail("TypeError: update() can only be called immediately after initialization")

        self._next = [self.update, self.encrypt, self.decrypt,
                      self.digest, self.verify]

        #self._update(assoc_data)
        self._state.update(assoc_data)
        self._auth_len += len(assoc_data)

        # See NIST SP 800 38D, 5.2.1.1
        if self._auth_len > (pow(2, 64) - 1):
            fail("ValueError: Additional Authenticated Data exceeds maximum length")

        return self
    self.update = update

    def _update(data):
        if len(self._cache) >= 16:
            fail("self._cache has more than 16 entries!")

        if len(self._cache) > 0:
            filler = min(16 - len(self._cache), len(data))
            self._cache += _copy_bytes(None, filler, data)
            data = data[filler:]

            if len(self._cache) < 16:
                return

            # The cache is exactly one block
            # self._signer.update(self._cache)
            self._cache = bytes(r"", encoding='utf-8')

        update_len = len(data) // 16 * 16
        self._cache = _copy_bytes(update_len, None, data)
        if update_len > 0:
            self._signer.update(data[:update_len])
    self._update = _update

    def digest():
        """Compute the *binary* MAC tag in an AEAD mode.

        The caller invokes this function at the very end.

        This method returns the MAC that shall be sent to the receiver,
        together with the ciphertext.

        :Return: the MAC, as a byte string.
        """

        if self.digest not in self._next:
            fail("TypeError: digest() cannot be called when decrypting or validating a message")
        self._next = [self.digest]

        return self._compute_mac()
    self.digest = digest

    def _compute_mac():
        """Compute MAC without any FSM checks."""

        if self._tag:
            return self._tag

        return self._state.get_mac()
        # # Step 5 in NIST SP 800-38D, Algorithm 4 - Compute S
        # self._pad_cache_and_update()
        # self._update(long_to_bytes(8 * self._auth_len, 8))
        # self._update(long_to_bytes(8 * self._msg_len, 8))
        # s_tag = self._signer.digest()
        #
        # # Step 6 - Compute T
        # self._tag = self._tag_cipher.encrypt(s_tag)[:self._mac_len]
        #
        # return self._tag
    self._compute_mac = _compute_mac

    def hexdigest():
        """Compute the *printable* MAC tag.

        This method is like `digest`.

        :Return: the MAC, as a hexadecimal string.
        """
        return hexlify(self.digest())
    self.hexdigest = hexdigest

    def verify(received_mac_tag):
        """Validate the *binary* MAC tag.

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

        if self.verify not in self._next:
            fail("TypeError: verify() cannot be called when encrypting a message")
        self._next = [self.verify]

        secret = get_random_bytes(16)

        mac1 = BLAKE2s.new(digest_bits=160, key=secret, data=self._compute_mac())
        mac2 = BLAKE2s.new(digest_bits=160, key=secret, data=received_mac_tag)

        if mac1.digest() != mac2.digest():
            fail("ValueError: MAC check failed")
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

    def encrypt_and_digest(plaintext, output=None):
        """Perform encrypt() and digest() in one step.

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
        if not types.is_bytelike(plaintext):
            fail('TypeError: plaintext is not byte-like')
        return self.encrypt(plaintext, output=output), self.digest()
    self.encrypt_and_digest = encrypt_and_digest

    def decrypt_and_verify(ciphertext, received_mac_tag, output=None):
        """Perform decrypt() and verify() in one step.

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
        if not types.is_bytelike(ciphertext):
            fail('TypeError: ciphertext is not byte-like')
        if not types.is_bytelike(received_mac_tag):
            fail('TypeError: received_mac_tag is not byte-like')

        plaintext = self.decrypt(ciphertext, output=output)
        self.verify(received_mac_tag)
        return plaintext
    self.decrypt_and_verify = decrypt_and_verify

    # Allowed transitions after initialization
    self._next = [self.update, self.encrypt, self.decrypt,
                  self.digest, self.verify]
    return self


def _create_gcm_cipher(factory, **kwargs):
    """Create a new block cipher, configured in Galois Counter Mode (GCM).

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
    if "key" not in kwargs:
        fail("TypeError: Missing parameter: key")
    key = kwargs.pop("key", None)
    nonce = kwargs.pop("nonce", None)
    if nonce == None:
        nonce = get_random_bytes(16)
    mac_len = kwargs.pop("mac_len", 16)

    # # Not documented - only used for testing
    # use_clmul = kwargs.pop("use_clmul", True)
    # if use_clmul and _ghash_clmul:
    #     ghash_c = _ghash_clmul
    # else:
    #     ghash_c = _ghash_portable

    # return GcmMode(factory, key, nonce, mac_len, kwargs, ghash_c)
    return _GcmMode(factory, key, nonce, mac_len, kwargs)


GcmMode = larky.struct(
    _create_gcm_cipher=_create_gcm_cipher,
)
