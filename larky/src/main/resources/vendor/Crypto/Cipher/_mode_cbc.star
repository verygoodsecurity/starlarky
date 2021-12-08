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
Ciphertext Block Chaining (CBC) mode.
"""

load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Util/py3compat", _copy_bytes="copy_bytes")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")


__all__ = ['CbcMode']


def _CbcMode(block_cipher, iv):
    """*Cipher-Block Chaining (CBC)*.

    Each of the ciphertext blocks depends on the current
    and all previous plaintext blocks.

    An Initialization Vector (*IV*) is required.

    See `NIST SP800-38A`_ , Section 6.2 .

    .. _`NIST SP800-38A` : http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf

    :undocumented: __init__
    """
    self = larky.mutablestruct(__class__='CbcMode')

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

        That also means that you cannot reuse an object for encrypting
        or decrypting other data with the same key.

        This function does not add any padding to the plaintext.

        :Parameters:
          plaintext : bytes/bytearray/memoryview
            The piece of data to encrypt.
            Its lenght must be multiple of the cipher block size.
        :Keywords:
          output : bytearray/memoryview
            The location where the ciphertext must be written to.
            If ``None``, the ciphertext is returned.
        :Return:
          If ``output`` is ``None``, the ciphertext is returned as ``bytes``.
          Otherwise, ``None``.
        """

        if self.encrypt not in self._next:
            fail("TypeError: encrypt() cannot be called after decrypt()")
        self._next = [ self.encrypt ]

        if output == None:
            ciphertext = bytearray()
        else:
            ciphertext = output

            if not types.is_bytearray(output):
                fail("TypeError: output must be a bytearray or " +
                # we do not have memoryview in larky so this is just for
                # compat reasons
                     "a writeable memoryview")

            # noinspection PyTypeChecker
            if len(plaintext) != len(output):
                fail("ValueError: output must have the same length as the " +
                     "input (%d bytes)" % len(plaintext))

        result = self._state.encrypt(plaintext, ciphertext)
        if result:
            if result == 3:
                fail("ValueError: Data must be padded to %d byte boundary " +
                     "in CBC mode" % self.block_size)
            fail("ValueError: Error %d while encrypting in CBC mode" % result)

        if output != None:
            return

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
            Its length must be multiple of the cipher block size.
        :Keywords:
          output : bytearray/memoryview
            The location where the plaintext must be written to.
            If ``None``, the plaintext is returned.
        :Return:
          If ``output`` is ``None``, the plaintext is returned as ``bytes``.
          Otherwise, ``None``.
        """

        if self.decrypt not in self._next:
            fail("TypeError: decrypt() cannot be called after encrypt()")
        self._next = [ self.decrypt ]

        if output == None:
            plaintext = bytearray()
        else:
            plaintext = output

            if not types.is_bytearray(output):
                fail("TypeError: output must be a bytearray or " +
                # we do not have memoryview in larky so this is just for
                # compat reasons
                     "a writeable memoryview")

            # noinspection PyTypeChecker
            if len(ciphertext) != len(output):
                fail("ValueError: output must have the same length as the " +
                     "input (%d bytes)" % len(ciphertext))


        result = self._state.decrypt(ciphertext, plaintext)

        if result:
            if result == 3:
                fail("ValueError: Data must be padded to %d byte boundary " +
                     "in CBC mode" % self.block_size)
            fail("ValueError: Error %d while decrypting in CBC mode" % result)

        if output != None:
            return

        return plaintext
    self.decrypt = decrypt

    def __init__(self, block_cipher, iv):
        """Create a new block cipher, configured in CBC mode.

        :Parameters:
          block_cipher : C pointer
            A smart pointer to the low-level block cipher instance.

          iv : bytes/bytearray/memoryview
            The initialization vector to use for encryption or decryption.
            It is as long as the cipher block.

            **The IV must be unpredictable**. Ideally it is picked randomly.

            Reusing the *IV* for encryptions performed with the same key
            compromises confidentiality.
        """
        self._state = _JCrypto.Cipher.CBCMode(block_cipher, iv)

        self.block_size = len(iv)
        """The block size of the underlying cipher, in bytes."""

        self.iv = _copy_bytes(None, None, iv)
        """The Initialization Vector originally used to create the object.
        The value does not change."""

        self.IV = self.iv
        """Alias for `iv`"""

        self._next = [ self.encrypt, self.decrypt ]
        return self

    self = __init__(self, block_cipher, iv)
    return self


def _create_cbc_cipher(factory, **kwargs):
    """Instantiate a cipher object that performs CBC encryption/decryption.

    :Parameters:
      factory : module
        The underlying block cipher, a module from ``Crypto.Cipher``.

    :Keywords:
      iv : bytes/bytearray/memoryview
        The IV to use for CBC.

      IV : bytes/bytearray/memoryview
        Alias for ``iv``.

    Any other keyword will be passed to the underlying block cipher.
    See the relevant documentation for details (at least ``key`` will need
    to be present).
    """

    cipher_state = factory._create_base_cipher(kwargs)
    iv = kwargs.pop("IV", None)
    IV = kwargs.pop("iv", None)

    if (None, None) == (iv, IV):
        iv = get_random_bytes(factory.block_size)
    if iv != None:
        if IV != None:
            fail("TypeError: You must either use 'iv' or 'IV', not both")
    else:
        iv = IV

    if len(iv) != factory.block_size:
        fail("ValueError: Incorrect IV length " +
             "(it must be %d bytes long)" % factory.block_size)

    if kwargs:
        fail("TypeError: Unknown parameters for CBC: %s" % str(kwargs))

    return _CbcMode(cipher_state, iv)


CbcMode = larky.struct(
    _create_cbc_cipher=_create_cbc_cipher,
)
