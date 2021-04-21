# -*- coding: utf-8 -*-
#
#  Cipher/mode_ecb.py : ECB mode
#
# ===================================================================
# The contents of this file are dedicated to the public domain.  To
# the extent that dedication to the public domain is not available,
# everyone is granted a worldwide, perpetual, royalty-free,
# non-exclusive license to exercise all rights associated with the
# contents of this file for any purpose whatsoever.
# No rights are reserved.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
# BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ===================================================================

"""
Electronic Code Book (ECB) mode.
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//types", types="types")

__all__ = [ 'EcbMode' ]

def _EcbMode(block_cipher):
    """*Electronic Code Book (ECB)*.

    This is the simplest encryption mode. Each of the plaintext blocks
    is directly encrypted into a ciphertext block, independently of
    any other block.

    This mode is dangerous because it exposes frequency of symbols
    in your plaintext. Other modes (e.g. *CBC*) should be used instead.

    See `NIST SP800-38A`_ , Section 6.1.

    .. _`NIST SP800-38A` : http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf

    :undocumented: __init__
    """

    def __init__(block_cipher):
        """Create a new block cipher, configured in ECB mode.

        :Parameters:
          block_cipher : C pointer
            A smart pointer to the low-level block cipher instance.
        """
        block_size = block_cipher.block_size

        _state = _JCrypto.Cipher.ECBMode(block_cipher)
        return larky.mutablestruct(block_size=block_size, _state=_state)

    self = __init__(block_cipher)

    def encrypt(plaintext, output=None):
        """Encrypt data with the key set at initialization.

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
            The length must be multiple of the cipher block length.
        :Keywords:
          output : bytearray/memoryview
            The location where the ciphertext must be written to.
            If ``None``, the ciphertext is returned.
        :Return:
          If ``output`` is ``None``, the ciphertext is returned as ``bytes``.
          Otherwise, ``None``.
        """
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
                fail("ValueError: Data must be aligned to block boundary in ECB mode")
            fail("ValueError: Error %d while encrypting in EBC mode" % result)

        if output != None:
            return

        return ciphertext
    self.encrypt = encrypt

    def decrypt(ciphertext, output=None):
        """Decrypt data with the key set at initialization.

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
            The length must be multiple of the cipher block length.
        :Keywords:
          output : bytearray/memoryview
            The location where the plaintext must be written to.
            If ``None``, the plaintext is returned.
        :Return:
          If ``output`` is ``None``, the plaintext is returned as ``bytes``.
          Otherwise, ``None``.
        """

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
                fail("Data must be aligned to block boundary in ECB mode")
            fail("ValueError: Error %d while decrypting in ECB mode" % result)

        if output != None:
            return

        return plaintext
    self.decrypt = decrypt
    return self


def _create_ecb_cipher(factory, **kwargs):
    """Instantiate a cipher object that performs ECB encryption/decryption.

    :Parameters:
      factory : module
        The underlying block cipher, a module from ``Crypto.Cipher``.

    All keywords are passed to the underlying block cipher.
    See the relevant documentation for details (at least ``key`` will need
    to be present
    """
    cipher_state = factory._create_base_cipher(kwargs)
    if cipher_state.block_size != factory.block_size:
        cipher_state.block_size = factory.block_size
    if kwargs:
        fail("TypeError: Unknown parameters for ECB: %s" % str(kwargs))
    return _EcbMode(cipher_state)


EcbMode = larky.struct(
    _create_ecb_cipher=_create_ecb_cipher,
)
