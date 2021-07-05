# -*- coding: utf-8 -*-
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
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//option/result", Error="Error")

# The size of the resulting hash in bytes.
digest_size = 20

# The internal block size of the hash algorithm in bytes.
block_size = 64


def SHA1Hash(data=None):
    """A SHA-1 hash object.
    Do not instantiate directly.
    Use the :func:`new` function.

    :ivar oid: ASN.1 Object ID
    :vartype oid: string

    :ivar block_size: the size in bytes of the internal message block,
                      input to the compression function
    :vartype block_size: integer

    :ivar digest_size: the size in bytes of the resulting hash
    :vartype digest_size: integer
    """
    self = larky.mutablestruct(__class__='SHA1Hash')

    # The size of the resulting hash in bytes.
    self.digest_size = 20
    # The internal block size of the hash algorithm in bytes.
    self.block_size = 64
    # ASN.1 Object ID
    self.oid = "1.3.14.3.2.26"


    def __init__(data):
        _state = _JCrypto.Hash.SHA1()
        if data:
            _state.update(data)
        self._state = _state
        return self
    self = __init__(data)

    def update(data):
        """Continue hashing of a message by consuming the next chunk of data.

        Args:
            data (byte string/byte array/memoryview): The next chunk of the message being hashed.
        """

        if not data:
            fail("TypeError: object supporting the buffer API required")
        self._state.update(data)
    self.update = update

    def digest():
        """Return the **binary** (non-printable) digest of the message that has been hashed so far.

        :return: The hash digest, computed over the data processed so far.
                 Binary form.
        :rtype: byte string
        """

        return self._state.digest()
    self.digest = digest

    def hexdigest():
        """Return the **printable** digest of the message that has been hashed so far.

        :return: The hash digest, computed over the data processed so far.
                 Hexadecimal encoded.
        :rtype: string
        """
        return tostr(hexlify(self.digest()))
    self.hexdigest = hexdigest

    def copy():
        """Return a copy ("clone") of the hash object.

        The copy will have the same internal state as the original hash
        object.
        This can be used to efficiently compute the digests of strings that
        share a common initial substring.

        :return: A hash object of the same type
        """

        clone = SHA1Hash()
        clone._state = self._state.copy()
        return clone
    self.copy = copy

    def new(data=None):
        """Create a fresh SHA-1 hash object."""

        return SHA1Hash(data)
    self.new = new
    return self


def new(data=None):
    """Create a new hash object.

    :parameter data:
        Optional. The very first chunk of the message to hash.
        It is equivalent to an early call to :meth:`SHA1Hash.update`.
    :type data: byte string/byte array/memoryview

    :Return: A :class:`SHA1Hash` hash object
    """
    return SHA1Hash().new(data)


def _pbkdf2_hmac_assist(inner, outer, first_digest, iterations):
    """Compute the expensive inner loop in PBKDF-HMAC."""
    fail("NOT IMPLEMENTED")


SHA1 = larky.struct(
    digest_size=digest_size,
    block_size=block_size,
    new=new,
    _pbkdf2_hmac_assist=_pbkdf2_hmac_assist,
    __name__ = 'SHA1',
)
