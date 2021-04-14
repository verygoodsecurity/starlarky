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

# The size of the full SHA-512 hash in bytes.
digest_size = 64

# The internal block size of the hash algorithm in bytes.
block_size = 128


def SHA512Hash(data, truncate):
    """A SHA-512 hash object (possibly in its truncated version SHA-512/224 or
    SHA-512/256.
    Do not instantiate directly. Use the :func:`new` function.

    :ivar oid: ASN.1 Object ID
    :vartype oid: string

    :ivar block_size: the size in bytes of the internal message block,
                      input to the compression function
    :vartype block_size: integer

    :ivar digest_size: the size in bytes of the resulting hash
    :vartype digest_size: integer
    """

    def __init__(data, truncate):
        self_ = {}
        self_['_truncate'] = truncate

        if truncate == None:
            self_['oid'] = "2.16.840.1.101.3.4.2.3"
            self_['digest_size'] = 64
        elif truncate == "224":
            self_['oid'] = "2.16.840.1.101.3.4.2.5"
            self_['digest_size'] = 28
        elif truncate == "256":
            self_['oid'] = "2.16.840.1.101.3.4.2.6"
            self_['digest_size'] = 32
        else:
            fail('ValueError: Incorrect truncation length. It must be "224"' +
                 ' or "256".')
        _state = _JCrypto.Hash.SHA512()
        if data:
            _state.update(data)
        self_['_state'] = _state
        return larky.mutablestruct(__class__="SHA512Hash", **self_)

    self = __init__(data, truncate)

    # The internal block size of the hash algorithm in bytes.
    self.block_size = 128

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

        h = SHA512Hash(None, self._truncate)
        h._state = self._state.copy()
        return h
    self.copy = copy

    def new(data=None):
        """Create a fresh SHA-512 hash object."""

        return SHA512Hash(data, self._truncate)
    self.new = new
    return self


def new(data=None, truncate=None):
    """Create a new hash object.

    Args:
      data (bytes/bytearray/memoryview):
        Optional. The very first chunk of the message to hash.
        It is equivalent to an early call to :meth:`SHA512Hash.update`.
      truncate (string):
        Optional. The desired length of the digest. It can be either "224" or
        "256". If not present, the digest is 512 bits long.
        Passing this parameter is **not** equivalent to simply truncating
        the output digest.

    :Return: A :class:`SHA512Hash` hash object
    """

    return SHA512Hash(data, truncate)
self.new = new



def _pbkdf2_hmac_assist(inner, outer, first_digest, iterations):
    """Compute the expensive inner loop in PBKDF-HMAC."""

    # assert iterations > 0
    #
    # bfr = create_string_buffer(len(first_digest));
    # result = _raw_sha512_lib.SHA512_pbkdf2_hmac_assist(
    #                 inner._state.get(),
    #                 outer._state.get(),
    #                 first_digest,
    #                 bfr,
    #                 c_size_t(iterations),
    #                 c_size_t(len(first_digest)))
    #
    # if result:
    #     fail(" ValueError(\"Error %d with PBKDF2-HMAC assist for SHA512\" % result)")
    result = "IMPLEMENT ME"
    fail("ValueError: Error %s with PBKDF2-HMAC assis for SHA512" % result)


SHA512 = larky.struct(
    digest_size=digest_size,
    block_size=block_size,
    new=new,
    _pbkdf2_hmac_assist=_pbkdf2_hmac_assist,
)