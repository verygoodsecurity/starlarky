# -*- coding: utf-8 -*-
#
# Hash/Poly1305.py - Implements the Poly1305 MAC
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
load("@stdlib//larky", larky="larky")
load("@stdlib//builtins","builtins")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@vendor//Crypto/Util/strxor", strxor="strxor")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")
load("@vendor//Crypto/Hash/BLAKE2s", BLAKE2s="BLAKE2s")

digest_size = 16

def Poly1305_MAC(r, s, data):
    """
    An Poly1305 MAC object.
        Do not instantiate directly. Use the :func:`new` function.

        :ivar digest_size: the size in bytes of the resulting MAC tag
        :vartype digest_size: integer
    
    """
    self = larky.mutablestruct(__name__='Poly1305_MAC', __class__=Poly1305_MAC)

    def update(data):
        """
        Authenticate the next chunk of message.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of data
        
        """
    def copy():
        """
        Return the **binary** (non-printable) MAC tag of the message
                authenticated so far.

                :return: The MAC tag digest, computed over the data processed so far.
                         Binary form.
                :rtype: byte string
        
        """
    def hexdigest():
        """
        Return the **printable** MAC tag of the message authenticated so far.

                :return: The MAC tag, computed over the data processed so far.
                         Hexadecimal encoded.
                :rtype: string
        
        """
    def verify(mac_tag):
        """
        Verify that a given **binary** MAC (computed by another party)
                is valid.

                Args:
                  mac_tag (byte string/byte string/memoryview): the expected MAC of the message.

                Raises:
                    ValueError: if the MAC does not match. It means that the message
                        has been tampered with or that the MAC key is incorrect.
        
        """
    def hexverify(hex_mac_tag):
        """
        Verify that a given **printable** MAC (computed by another party)
                is valid.

                Args:
                    hex_mac_tag (string): the expected MAC of the message,
                        as a hexadecimal string.

                Raises:
                    ValueError: if the MAC does not match. It means that the message
                        has been tampered with or that the MAC key is incorrect.
        
        """

    def __init__(self, r, s, data):
        if len(r) != 16:
            fail("ValueError: Parameter r is not 16 bytes long")
        if len(s) != 16:
            fail("ValueError: Parameter s is not 16 bytes long")

        self._mac_tag = None 
        self.digest_size = 16
        return self

    self = __init__(self, r, s, data)

    return self

def new(**kwargs):
    """
    Create a new Poly1305 MAC object.

        Args:
            key (bytes/bytearray/memoryview):
                The 32-byte key for the Poly1305 object.
            cipher (module from ``Crypto.Cipher``):
                The cipher algorithm to use for deriving the Poly1305
                key pair *(r, s)*.
                It can only be ``Crypto.Cipher.AES`` or ``Crypto.Cipher.ChaCha20``.
            nonce (bytes/bytearray/memoryview):
                Optional. The non-repeatable value to use for the MAC of this message.
                It must be 16 bytes long for ``AES`` and 8 or 12 bytes for ``ChaCha20``.
                If not passed, a random nonce is created; you will find it in the
                ``nonce`` attribute of the new object.
            data (bytes/bytearray/memoryview):
                Optional. The very first chunk of the message to authenticate.
                It is equivalent to an early call to ``update()``.

        Returns:
            A :class:`Poly1305_MAC` object
    
    """
