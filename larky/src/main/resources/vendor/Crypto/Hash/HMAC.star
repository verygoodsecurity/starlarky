#
# HMAC.py - Implements the HMAC algorithm as described by RFC 2104.
#
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

load("@stdlib//larky", larky="larky")
load("@stdlib//builtins","builtins")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@vendor//Crypto/Hash/MD5", MD5="MD5")
load("@vendor//Crypto/Util/strxor", strxor="strxor")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")
load("@vendor//Crypto/Hash/BLAKE2s", BLAKE2s="BLAKE2s")


def _HMAC(key, msg, digestmod):
    """
    An HMAC hash object.
        Do not instantiate directly. Use the :func:`new` function.

        :ivar digest_size: the size in bytes of the resulting MAC tag
        :vartype digest_size: integer

    """
    self = larky.mutablestruct(__name__='HMAC', __class__=_HMAC)

    def update(msg):
        """
        Authenticate the next chunk of message.

                Args:
                    data (byte string/byte array/memoryview): The next chunk of data

        """
        self._inner.update(msg)
        return self
    self.update = update

    def _pbkdf2_hmac_assist(first_digest, iterations):
        """
        Carry out the expensive inner loop for PBKDF2-HMAC
        """
        # Not implemented in hashing functions so passed
        # result = self._digestmod._pbkdf2_hmac_assist(
        #                     self._inner,
        #                     self._outer,
        #                     first_digest,
        #                     iterations)
        # return result
        result = "IMPLEMENT ME"
        fail("ValueError: Error %s with PBKDF2-HMAC assist" % result)

    def copy():
        """
        Return a copy ("clone") of the HMAC object.

                The copy will have the same internal state as the original HMAC
                object.
                This can be used to efficiently compute the MAC tag of byte
                strings that share a common initial substring.

                :return: An :class:`HMAC`

        """
        new_hmac = _HMAC(tobytes("fake key"), digestmod=self._digestmod)

        # Syncronize the state
        new_hmac._inner = self._inner.copy()
        new_hmac._outer = self._outer.copy()
        return new_hmac
    self.copy = copy

    def digest():
        """
        Return the **binary** (non-printable) MAC tag of the message
                authenticated so far.

                :return: The MAC tag digest, computed over the data processed so far.
                         Binary form.
                :rtype: byte string

        """
        frozen_outer_hash = self._outer.copy()
        frozen_outer_hash.update(self._inner.digest())
        return frozen_outer_hash.digest()
    self.digest = digest

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
        secret = get_random_bytes(16)

        mac1 = BLAKE2s.new(digest_bits=160, key=secret, data=mac_tag)
        mac2 = BLAKE2s.new(digest_bits=160, key=secret, data=self.digest())

        if mac1.digest() != mac2.digest():
            fail('ValueError("MAC check failed")')
    self.verify = verify

    def hexdigest():
        """
        Return the **printable** MAC tag of the message authenticated so far.

                :return: The MAC tag, computed over the data processed so far.
                         Hexadecimal encoded.
                :rtype: string

        """
        return tostr(hexlify(self.digest()))
    self.hexdigest = hexdigest

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
        self.verify(unhexlify(tobytes(hex_mac_tag)))
    self.hexverify = hexverify

    def __init__(key, msg, digestmod=None):
        """
        b
        """
        if digestmod == None:
            digestmod = MD5

        if msg == None:
            msg = tobytes(r"")

        # Size of the MAC tag
        self.digest_size = digestmod.digest_size

        self._digestmod = digestmod

        if hasattr(digestmod, 'block_size'):
            if len(key) <= digestmod.block_size:
                # Step 1 or 2
                key_0 = tostr(key) + "\\x00" * (digestmod.block_size - len(key))
                key_0 = tobytes(key_0)
            else:
                # Step 3
                hash_k = digestmod.new(key).digest()
                key_0 = tostr(hash_k) + "\\x00" * (digestmod.block_size - len(key))
                key_0 = tobytes(key_0)
        else:
            fail('ValueError("Hash type incompatible to HMAC")')

        # Step 4
        key_0_ipad = strxor(key_0, tobytes(r"\x36") * len(key_0))

        # Start step 5 and 6
        self._inner = digestmod.new(key_0_ipad)
        self._inner.update(msg)

        # Step 7
        key_0_opad = strxor(key_0, tobytes(r"\x5c") * len(key_0))

        # Start step 8 and 9
        self._outer = digestmod.new(key_0_opad)

        return self
    self = __init__(key, msg, digestmod)

    return self

def new(key, msg, digestmod=None):
    """
    Create a new MAC object.

        Args:
            key (bytes/bytearray/memoryview):
                key for the MAC object.
                It must be long enough to match the expected security level of the
                MAC.
            msg (bytes/bytearray/memoryview):
                Optional. The very first chunk of the message to authenticate.
                It is equivalent to an early call to :meth:`HMAC.update`.
            digestmod (module):
                The hash to use to implement the HMAC.
                Default is :mod:`Crypto.Hash.MD5`.

        Returns:
            An :class:`HMAC` object

    """
    return _HMAC(key, msg, digestmod)


def _constant_time_compare(val1, val2):
    """Return ``True`` if the two strings are equal, ``False``
    otherwise.

    The time taken is independent of the number of characters that
    match. Do not use this function for anything else than comparision
    with known length targets.

    This is should be implemented in C in order to get it completely
    right.

    This is an alias of :func:`hmac.compare_digest` on Python>=2.7,3.3.
    """
    len_eq = len(val1) == len(val2)
    if len_eq:
        result = 0
        left = val1
    else:
        result = 1
        left = val2
    for x, y in zip(bytearray(left).elems(), bytearray(val2).elems()):
        result |= x ^ y
    return result == 0


HMAC = larky.struct(
    new = new,
    compare_digest=_constant_time_compare,
    __name__ = 'HMAC',
)