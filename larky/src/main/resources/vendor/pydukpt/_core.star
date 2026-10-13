# Ported from pydukpt 0.1.0 (3778da71): src/pydukpt/_core.py. See larky/src/main/resources/VENDORED.md.
#
# MIT License
#
# Copyright (c) 2026 Mahmoud Abdelkader
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#
# pydukpt NOTICE:
#
# pydukpt is written from ANSI X9.24-1:2009 and ISO 9564-1. It contains no
# source code from other DUKPT implementations.
"""ANSI X9.24-1:2009 TDES DUKPT key derivation.

Written from the standard (ANSI X9.24-1:2009, Annex A). Section references in
comments are to that document.

Larky differences from pydukpt 0.1.0 (they apply to the whole package):

- Byte arguments may be ``bytes`` or ``bytearray``; Larky has no
  ``memoryview``. Results are ``bytes``.
- Larky has no exceptions. Errors are ``fail()``s whose message starts with
  the name of the Python exception pydukpt raises, followed by its message:
  ``"TypeError: ksn must be bytes-like; got string"``,
  ``"ValueError: ksn must be 10 bytes; got 8"``,
  ``"KSNExhaustedError: KSN counter exhausted at 0x1ff800"``. Type names are
  Larky's (``string``, ``NoneType``, ...). ``KSNExhaustedError`` (a
  ``ValueError`` in Python) is a function that fails this way when called.
"""
load("@stdlib//binascii", unhexlify="unhexlify")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Cipher/DES", DES="DES")

#: Length of a TDES DUKPT key (BDK, IPEK and every derived key), in bytes.
KEY_LEN = 16
#: Length of a Key Serial Number, in bytes.
KSN_LEN = 10
#: Mask of the 21-bit transaction counter in the rightmost bits of the KSN.
COUNTER_MASK = 0x1FFFFF
#: The highest counter a device may use (the largest 21-bit value with at
#: most ten 1 bits). After it, the device must stop.
MAX_COUNTER = 0x1FF800
#: A counter may have at most this many 1 bits.
MAX_COUNTER_ONE_BITS = 10

# A.6 / A.2: the "C0C0 C0C0 0000 0000 C0C0 C0C0 0000 0000" key mask used for
# the right half of the IPEK and in the non-reversible key generation process.
_KEY_MASK = unhexlify("C0C0C0C000000000C0C0C0C000000000")

# A.4.1, Table A-1: variants of the current transaction key.
#: XOR mask giving the PIN encryption key.
PIN_VARIANT = unhexlify("00000000000000FF00000000000000FF")
#: XOR mask giving the MAC key for request messages.
MAC_REQUEST_VARIANT = unhexlify("000000000000FF00000000000000FF00")
#: XOR mask giving the MAC key for response messages.
MAC_RESPONSE_VARIANT = unhexlify("00000000FF00000000000000FF000000")
#: XOR mask giving the data encryption key variant for request messages.
DATA_REQUEST_VARIANT = unhexlify("0000000000FF00000000000000FF0000")
#: XOR mask giving the data encryption key variant for response messages.
DATA_RESPONSE_VARIANT = unhexlify("000000FF00000000000000FF00000000")


def KSNExhaustedError(msg=""):
    """Raised when a KSN has no next transaction counter.

    In Python a subclass of ``ValueError``. Larky has no exception classes:
    calling this fails with ``"KSNExhaustedError: " + msg``.
    """
    fail("KSNExhaustedError: " + msg)


def _as_bytes(name, value, length=None):
    if not (types.is_bytes(value) or types.is_bytearray(value)):
        fail("TypeError: %s must be bytes-like; got %s" % (name, type(value)))
    data = bytes(value)
    if length != None and len(data) != length:
        fail("ValueError: %s must be %d bytes; got %d" % (name, length, len(data)))
    return data


def _from_bytes(data):
    # int.from_bytes(data, "big")
    n = 0
    for b in data:
        n = (n << 8) | b
    return n


def _to_bytes(n, length):
    # n.to_bytes(length, "big") for 0 <= n < 256 ** length
    return bytes([(n >> (8 * (length - 1 - i))) & 0xFF for i in range(length)])


def _xor(a, b):
    return bytes([x ^ y for x, y in zip(a, b)])


def _des_encrypt(key8, block):
    return DES.new(key8, DES.MODE_ECB).encrypt(block)


def _des_decrypt(key8, block):
    return DES.new(key8, DES.MODE_ECB).decrypt(block)


def tdes_encrypt_block(key, block):
    """Encrypt one 8-byte block with two-key TDES (EDE, keying option 2).

    Built from single DES so that keys whose halves are equal (which
    pycryptodome's ``DES3`` refuses) still give the result the standard
    defines.
    """
    k1, k2 = key[:8], key[8:16]
    return _des_encrypt(k1, _des_decrypt(k2, _des_encrypt(k1, block)))


def tdes_decrypt_block(key, block):
    """Decrypt one 8-byte block with two-key TDES (EDE, keying option 2)."""
    k1, k2 = key[:8], key[8:16]
    return _des_decrypt(k1, _des_encrypt(k2, _des_decrypt(k1, block)))


def ksn_counter(ksn):
    """Return the 21-bit transaction counter of a 10-byte KSN."""
    data = _as_bytes("ksn", ksn, KSN_LEN)
    return _from_bytes(data[7:]) & COUNTER_MASK


def ksn_with_counter(ksn, counter):
    """Return ``ksn`` with its 21-bit transaction counter replaced by ``counter``."""
    data = _as_bytes("ksn", ksn, KSN_LEN)
    if not types.is_int(counter):
        fail("TypeError: counter must be int; got %s" % type(counter))
    if not (0 <= counter and counter <= COUNTER_MASK):
        fail("ValueError: counter must be in 0..0x1FFFFF; got %#x" % counter)
    tail = (_from_bytes(data[7:]) & ~COUNTER_MASK) | counter
    return data[:7] + _to_bytes(tail, 3)


def _one_bits(n):
    return bin(n).count("1")


def next_ksn(ksn):
    """Return the KSN an originating device uses for its next transaction.

    The counter becomes the smallest value greater than the current one that
    has at most ten 1 bits (A.2: counters with more than ten 1 bits are
    skipped). This also moves a counter that already has more than ten 1
    bits to the next valid one.

    Raises:
        KSNExhaustedError: no such counter exists (the counter is at or past
            ``MAX_COUNTER``, 0x1FF800); the device must stop.
    """
    counter = ksn_counter(ksn)
    candidate = counter + 1
    # Starlark has no while loop. Each step moves the lowest 1 bit of
    # candidate (< 2**22) up by at least one place, so after at most 12 steps
    # it has at most ten 1 bits; 22 is a safe bound.
    for _ in range(22):
        if _one_bits(candidate) <= MAX_COUNTER_ONE_BITS:
            break
        # Every value between candidate and candidate + lowest set bit keeps
        # all of candidate's 1 bits and adds more, so skip straight past them.
        candidate += candidate & -candidate
    if candidate > COUNTER_MASK:
        KSNExhaustedError("KSN counter exhausted at %#x" % counter)
    return ksn_with_counter(ksn, candidate)


def derive_ipek(bdk, ksn):
    """Derive the Initial PIN Encryption Key (initial key) from a BDK and KSN.

    A.6: the leftmost 64 bits of the KSN, with the counter bits cleared, are
    TDES-encrypted under the BDK (left half) and under the BDK XOR
    ``C0C0C0C000000000C0C0C0C000000000`` (right half). The counter in ``ksn``
    is ignored, so any KSN of the device gives the same IPEK.
    """
    key = _as_bytes("bdk", bdk, KEY_LEN)
    initial_ksn = ksn_with_counter(ksn, 0)[:8]
    left = tdes_encrypt_block(key, initial_ksn)
    right = tdes_encrypt_block(_xor(key, _KEY_MASK), initial_ksn)
    return left + right


def _non_reversible_key(key, register):
    """A.2 non-reversible key generation process for a 16-byte key."""

    def half(k):
        k_left, k_right = k[:8], k[8:]
        return _xor(k_right, _des_encrypt(k_left, _xor(k_right, register)))

    # The right half of the new key comes from the key itself, the left half
    # from the key XOR the C0C0... mask.
    return half(_xor(key, _KEY_MASK)) + half(key)


def derive_key(ipek, ksn):
    """Derive the current transaction key for ``ksn`` from the IPEK.

    This is the host-side derivation (A.3, "Calculation of the current
    key"): for each 1 bit of the 21-bit counter, from the most significant
    bit down, that bit is set in the KSN register and the key is replaced by
    the non-reversible transformation of the key under the register. With
    counter 0 the IPEK itself is returned. Counters with more than ten 1 bits,
    which a device never uses, are derived the same way.
    """
    key = _as_bytes("ipek", ipek, KEY_LEN)
    data = _as_bytes("ksn", ksn, KSN_LEN)
    counter = ksn_counter(data)
    # Rightmost 64 bits of the KSN with the counter cleared.
    register = _from_bytes(data[2:]) & ~COUNTER_MASK
    for shift in range(20, -1, -1):
        bit = 1 << shift
        if counter & bit:
            register |= bit
            key = _non_reversible_key(key, _to_bytes(register, 8))
    return key


def pin_key(key):
    """Return the PIN encryption key variant of a current transaction key."""
    return _xor(_as_bytes("key", key, KEY_LEN), PIN_VARIANT)


def mac_request_key(key):
    """Return the request MAC key variant of a current transaction key."""
    return _xor(_as_bytes("key", key, KEY_LEN), MAC_REQUEST_VARIANT)


def mac_response_key(key):
    """Return the response MAC key variant of a current transaction key."""
    return _xor(_as_bytes("key", key, KEY_LEN), MAC_RESPONSE_VARIANT)


def _data_key(key, variant):
    # A.4.1, Figure A-2: the data variant is passed through a one-way
    # function: each half is TDES-encrypted under the variant itself.
    v = _xor(_as_bytes("key", key, KEY_LEN), variant)
    return tdes_encrypt_block(v, v[:8]) + tdes_encrypt_block(v, v[8:])


def data_request_key(key):
    """Return the request data encryption key for a current transaction key.

    The key is ``key XOR DATA_REQUEST_VARIANT`` with each 8-byte half then
    TDES-encrypted under that variant (ANSI X9.24-1:2009 A.4.1, Figure A-2).
    For the bare variant, XOR with :data:`DATA_REQUEST_VARIANT` yourself.
    """
    return _data_key(key, DATA_REQUEST_VARIANT)


def data_response_key(key):
    """Return the response data encryption key for a current transaction key.

    Derived as :func:`data_request_key`, from :data:`DATA_RESPONSE_VARIANT`.
    """
    return _data_key(key, DATA_RESPONSE_VARIANT)


core = larky.struct(
    __name__='_core',
    KEY_LEN=KEY_LEN,
    KSN_LEN=KSN_LEN,
    COUNTER_MASK=COUNTER_MASK,
    MAX_COUNTER=MAX_COUNTER,
    MAX_COUNTER_ONE_BITS=MAX_COUNTER_ONE_BITS,
    PIN_VARIANT=PIN_VARIANT,
    MAC_REQUEST_VARIANT=MAC_REQUEST_VARIANT,
    MAC_RESPONSE_VARIANT=MAC_RESPONSE_VARIANT,
    DATA_REQUEST_VARIANT=DATA_REQUEST_VARIANT,
    DATA_RESPONSE_VARIANT=DATA_RESPONSE_VARIANT,
    KSNExhaustedError=KSNExhaustedError,
    _as_bytes=_as_bytes,
    _xor=_xor,
    tdes_encrypt_block=tdes_encrypt_block,
    tdes_decrypt_block=tdes_decrypt_block,
    ksn_counter=ksn_counter,
    ksn_with_counter=ksn_with_counter,
    next_ksn=next_ksn,
    derive_ipek=derive_ipek,
    derive_key=derive_key,
    pin_key=pin_key,
    mac_request_key=mac_request_key,
    mac_response_key=mac_response_key,
    data_request_key=data_request_key,
    data_response_key=data_response_key,
)
