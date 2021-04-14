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
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Util/py3compat", bord="bord")

# The size of the resulting hash in bytes.
digest_size = 32

# The internal block size of the hash algorithm in bytes.
block_size = 64


def SHA256Hash(data=None):
    """A SHA-256 hash object.
    Do not instantiate directly. Use the :func:`new` function.

    :ivar oid: ASN.1 Object ID
    :vartype oid: string

    :ivar block_size: the size in bytes of the internal message block,
                      input to the compression function
    :vartype block_size: integer

    :ivar digest_size: the size in bytes of the resulting hash
    :vartype digest_size: integer
    """

    def __init__(data):
        self_ = {}
        self_['_state'] = _JCrypto.Hash.SHA256()
        if data:
            self_['_state'].update(data)
        return larky.mutablestruct(**self_)

    self = __init__(data)

    # The size of the resulting hash in bytes.
    self.digest_size = digest_size
    # The internal block size of the hash algorithm in bytes.
    self.block_size = block_size
    # ASN.1 Object ID
    self.oid = "2.16.840.1.101.3.4.2.1"

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

        return "".join(["%02x" % bord(x) for x in self.digest()])
    self.hexdigest = hexdigest

    def copy():
        """Return a copy ("clone") of the hash object.

        The copy will have the same internal state as the original hash
        object.
        This can be used to efficiently compute the digests of strings that
        share a common initial substring.

        :return: A hash object of the same type
        """

        h = SHA256Hash()
        h._state = self._state.copy()
        return h
    self.copy = copy

    def new(data=None):
        """Create a fresh SHA-256 hash object."""

        return SHA256Hash(data)
    self.new = new
    return self


def new(data=None):
    """Create a new hash object.

    :parameter data:
        Optional. The very first chunk of the message to hash.
        It is equivalent to an early call to :meth:`SHA256Hash.update`.
    :type data: byte string/byte array/memoryview

    :Return: A :class:`SHA256Hash` hash object
    """

    return SHA256Hash().new(data)


def _pbkdf2_hmac_assist(inner, outer, first_digest, iterations):
    """Compute the expensive inner loop in PBKDF-HMAC."""
    #
    # assert iterations > 0
    #
    # bfr = create_string_buffer(len(first_digest));
    # result = _raw_sha256_lib.SHA256_pbkdf2_hmac_assist(
    #                 inner._state.get(),
    #                 outer._state.get(),
    #                 first_digest,
    #                 bfr,
    #                 c_size_t(iterations),
    #                 c_size_t(len(first_digest)))
    #
    # if result:
    #     fail(" ValueError(\"Error %d with PBKDF2-HMAC assist for SHA256\" % result)")
    #
    # return get_raw_buffer(bfr)
    result = "IMPLEMENT ME"
    fail("ValueError: Error %s with PBKDF2-HMAC assis for SHA256" % result)



SHA256 = larky.struct(
    digest_size=digest_size,
    block_size=block_size,
    new=new,
    _pbkdf2_hmac_assist=_pbkdf2_hmac_assist,
)