# -*- coding: utf-8 -*-
#
#  Cipher/PKCS1-v1_5.py : PKCS#1 v1.5
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
load("@stdlib//operator", operator="operator")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Util/number", bytes_to_long="bytes_to_long", long_to_bytes="long_to_bytes")
load("@vendor//Crypto/Util/py3compat", bord="bord", is_bytes="is_bytes", _copy_bytes="copy_bytes")
load("@vendor//option/result", Error="Error")


_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION

_pkcs1_decode = lambda *args: args


__all__ = ['new', 'PKCS115_Cipher']


def PKCS115_Cipher(key, randfunc):
    """This cipher can perform PKCS#1 v1.5 RSA encryption or decryption.
    Do not instantiate directly.

    Use :func:`Crypto.Cipher.PKCS1_v1_5.new` instead.
    """
    self = larky.mutablestruct(__class__='PKCS115_Cipher')

    def __init__(key, randfunc):
        """Initialize this PKCS#1 v1.5 cipher object.

        :Parameters:
         key : an RSA key object
          If a private half is given, both encryption and decryption are possible.
          If a public half is given, only encryption is possible.
         randfunc : callable
          Function that returns random bytes.
        """

        self._key = key
        self._randfunc = randfunc
        return self
    self = __init__(key, randfunc)

    def can_encrypt():
        """Return True if this cipher object can be used for encryption."""
        return self._key.can_encrypt()
    self.can_encrypt = can_encrypt

    def can_decrypt():
        """Return True if this cipher object can be used for decryption."""
        return self._key.can_decrypt()
    self.can_decrypt = can_decrypt

    def encrypt(message):
        """Produce the PKCS#1 v1.5 encryption of a message.

        This function is named ``RSAES-PKCS1-V1_5-ENCRYPT``, and it is specified in
        `section 7.2.1 of RFC8017
        <https://tools.ietf.org/html/rfc8017#page-28>`_.

        :param message:
            The message to encrypt, also known as plaintext. It can be of
            variable length, but not longer than the RSA modulus (in bytes) minus 11.
        :type message: bytes/bytearray/memoryview

        :Returns: A byte string, the ciphertext in which the message is encrypted.
            It is as long as the RSA modulus (in bytes).

        :Raises ValueError:
            If the RSA key length is not sufficiently long to deal with the given
            message.
        """

        # See 7.2.1 in RFC8017
        k = self._key.size_in_bytes()
        mLen = len(message)

        # Step 1
        if mLen > k - 11:
            return Error("ValueError: Plaintext is too long.").unwrap()
        # Step 2a
        ps = []
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if len(ps) == k - mLen - 3:
                break
            new_byte = self._randfunc(1)
            if bord(new_byte[0]) == 0x00:
                continue
            ps.append(new_byte)
        ps = b"".join(ps)
        if len(ps) != (k - mLen - 3):
            fail("len(ps) != (k - mLen - 3)")
        # Step 2b
        em = b'\x00\x02' + ps + b'\x00' + _copy_bytes(None, None, message)
        # Step 3a (OS2IP)
        em_int = bytes_to_long(em)
        # Step 3b (RSAEP)
        m_int = self._key._encrypt(em_int)
        # Step 3c (I2OSP)
        c = long_to_bytes(m_int, k)
        return c
    self.encrypt = encrypt

    def decrypt(ciphertext, sentinel, expected_pt_len=0):
        r"""Decrypt a PKCS#1 v1.5 ciphertext.

        This is the function ``RSAES-PKCS1-V1_5-DECRYPT`` specified in
        `section 7.2.2 of RFC8017
        <https://tools.ietf.org/html/rfc8017#page-29>`_.

        Args:
          ciphertext (bytes/bytearray/memoryview):
            The ciphertext that contains the message to recover.
          sentinel (any type):
            The object to return whenever an error is detected.
          expected_pt_len (integer):
            The length the plaintext is known to have, or 0 if unknown.

        Returns (byte string):
            It is either the original message or the ``sentinel`` (in case of an error).

        .. warning::
            PKCS#1 v1.5 decryption is intrinsically vulnerable to timing
            attacks (see `Bleichenbacher's`__ attack).
            **Use PKCS#1 OAEP instead**.

            This implementation attempts to mitigate the risk
            with some constant-time constructs.
            However, they are not sufficient by themselves: the type of protocol you
            implement and the way you handle errors make a big difference.

            Specifically, you should make it very hard for the (malicious)
            party that submitted the ciphertext to quickly understand if decryption
            succeeded or not.

            To this end, it is recommended that your protocol only encrypts
            plaintexts of fixed length (``expected_pt_len``),
            that ``sentinel`` is a random byte string of the same length,
            and that processing continues for as long
            as possible even if ``sentinel`` is returned (i.e. in case of
            incorrect decryption).

            .. __: http://www.bell-labs.com/user/bleichen/papers/pkcs.ps
        """

        # See 7.2.2 in RFC8017
        k = self._key.size_in_bytes()

        # Step 1
        if len(ciphertext) != k:
            return Error(
                "ValueError: Ciphertext with incorrect length " +
                "(not %d bytes)" % k
            ).unwrap()

        # all constant time
        output = bytearray(b'\0' * k)
        if not is_bytes(sentinel) or len(sentinel) > k:
            sentinel = b''

        size = _JCrypto.Cipher.PKCS1.decode(
            ciphertext,
            sentinel,
            expected_pt_len,
            output,
            self._key._to_dict()
        )
        if size <= 0:
            return sentinel
        return output[-size:]
    self.decrypt = decrypt
    return self



def new(key, randfunc=None):
    """Create a cipher for performing PKCS#1 v1.5 encryption or decryption.

    :param key:
      The key to use to encrypt or decrypt the message. This is a `Crypto.PublicKey.RSA` object.
      Decryption is only possible if *key* is a private RSA key.
    :type key: RSA key object

    :param randfunc:
      Function that return random bytes.
      The default is :func:`Crypto.Random.get_random_bytes`.
    :type randfunc: callable

    :returns: A cipher object `PKCS115_Cipher`.
    """

    if randfunc == None:
        randfunc = Random.get_random_bytes
    return PKCS115_Cipher(key, randfunc)


PKCS1_v1_5_Cipher = larky.struct(
    new=new,
    PKCS115_Cipher=PKCS115_Cipher,
)