# Ported from pydukpt 0.1.0 (3778da71): src/pydukpt/pinblock.py. See larky/src/main/resources/VENDORED.md.
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
"""ISO 9564-1 format 0 PIN blocks, encrypted under a DUKPT PIN key."""
load("@stdlib//binascii", unhexlify="unhexlify")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//pydukpt/_core", _core="core")

_as_bytes = _core._as_bytes
_xor = _core._xor
pin_key = _core.pin_key
tdes_decrypt_block = _core.tdes_decrypt_block
tdes_encrypt_block = _core.tdes_encrypt_block

__all__ = ["decode_pin_block", "decrypt_pin", "encode_pin_block", "encrypt_pin"]

_PAN_MIN, _PAN_MAX = 8, 19
_DIGITS = "0123456789"


def _is_ascii_digits(value):
    # str.isascii() and str.isdigit(); Larky strings have no isascii().
    if not value:
        return False
    for c in value.elems():
        if c not in _DIGITS:
            return False
    return True


def _check_digits(name, value, lo, hi):
    if not types.is_string(value):
        fail("TypeError: %s must be str; got %s" % (name, type(value)))
    if not (_is_ascii_digits(value) and lo <= len(value) and len(value) <= hi):
        fail("ValueError: %s must be %d to %d decimal digits" % (name, lo, hi))
    return value


def _pan_field(pan):
    _check_digits("pan", pan, _PAN_MIN, _PAN_MAX)
    # The 12 rightmost PAN digits excluding the check digit, right-justified
    # in 16 nibbles and padded on the left with zeros.
    digits = pan[:-1][-12:]
    return unhexlify("0" * (16 - len(digits)) + digits)


def encode_pin_block(pin, pan):
    """Return the clear ISO 9564-1 format 0 PIN block for ``pin`` and ``pan``.

    ``pin`` is 4 to 12 digits; ``pan`` is the full primary account number,
    8 to 19 digits including its check digit.
    """
    _check_digits("pin", pin, 4, 12)
    pin_field = "0%X%s" % (len(pin), pin)
    pin_field = unhexlify(pin_field + "F" * (16 - len(pin_field)))
    return _xor(pin_field, _pan_field(pan))


def decode_pin_block(pin_block, pan):
    """Return the PIN in a clear ISO 9564-1 format 0 PIN block.

    Raises:
        ValueError: the block is not a format 0 block for ``pan`` (wrong
            control nibble, PIN length outside 4..12, a non-digit PIN nibble,
            or a fill nibble other than F).
    """
    block = _as_bytes("pin_block", pin_block, 8)
    nibbles = _xor(block, _pan_field(pan)).hex().upper()
    if nibbles[0] != "0":
        fail("ValueError: not an ISO 9564-1 format 0 PIN block")
    length = int(nibbles[1], 16)
    if not (4 <= length and length <= 12):
        fail("ValueError: invalid PIN length in PIN block")
    pin, fill = nibbles[2:2 + length], nibbles[2 + length:]
    if not _is_ascii_digits(pin) or fill != "F" * len(fill):
        fail("ValueError: malformed PIN block")
    return pin


def encrypt_pin(key, pin, pan):
    """Encrypt ``pin`` as a format 0 PIN block under a DUKPT transaction key.

    ``key`` is the current transaction key (:func:`pydukpt.derive_key`), not
    the PIN key: the PIN variant is applied here. The block is encrypted with
    TDES in ECB mode.
    """
    return tdes_encrypt_block(pin_key(key), encode_pin_block(pin, pan))


def decrypt_pin(key, encrypted_pin_block, pan):
    """Decrypt a format 0 PIN block encrypted under a DUKPT transaction key.

    ``key`` is the current transaction key, as for :func:`encrypt_pin`.

    Raises:
        ValueError: the decrypted block is not a valid format 0 block for
            ``pan`` (wrong key, KSN or PAN, or a different format).
    """
    block = _as_bytes("encrypted_pin_block", encrypted_pin_block, 8)
    return decode_pin_block(tdes_decrypt_block(pin_key(key), block), pan)


pinblock = larky.struct(
    __name__='pinblock',
    encode_pin_block=encode_pin_block,
    decode_pin_block=decode_pin_block,
    encrypt_pin=encrypt_pin,
    decrypt_pin=decrypt_pin,
)
