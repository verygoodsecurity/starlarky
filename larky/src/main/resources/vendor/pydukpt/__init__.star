# Ported from pydukpt 0.1.0 (3778da71): src/pydukpt/__init__.py. See larky/src/main/resources/VENDORED.md.
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
"""ANSI X9.24-1:2009 TDES DUKPT key derivation and ISO 9564 format 0 PIN blocks.

Functional API::

    load("@vendor//pydukpt", "derive_ipek", "derive_key", "encrypt_pin")

    ipek = derive_ipek(bdk, ksn)
    key = derive_key(ipek, ksn)
    block = encrypt_pin(key, "1234", "4012345678909")

:mod:`pydukpt.compat` (``@vendor//pydukpt/compat``) mirrors the API of the
PyPI ``dukpt`` 1.0.1 package.

Larky: Python's ``from pydukpt import X`` is ``load("@vendor//pydukpt", "X")``;
the ``pydukpt`` struct holds the same names (``pydukpt.derive_key``). See
``pydukpt/_core`` for how arguments and errors differ from Python.
"""
load("@stdlib//larky", larky="larky")
load("@vendor//pydukpt/_core", _core="core")
load("@vendor//pydukpt/pinblock", pinblock="pinblock")

COUNTER_MASK = _core.COUNTER_MASK
DATA_REQUEST_VARIANT = _core.DATA_REQUEST_VARIANT
DATA_RESPONSE_VARIANT = _core.DATA_RESPONSE_VARIANT
KEY_LEN = _core.KEY_LEN
KSN_LEN = _core.KSN_LEN
MAC_REQUEST_VARIANT = _core.MAC_REQUEST_VARIANT
MAC_RESPONSE_VARIANT = _core.MAC_RESPONSE_VARIANT
MAX_COUNTER = _core.MAX_COUNTER
MAX_COUNTER_ONE_BITS = _core.MAX_COUNTER_ONE_BITS
PIN_VARIANT = _core.PIN_VARIANT
KSNExhaustedError = _core.KSNExhaustedError
data_request_key = _core.data_request_key
data_response_key = _core.data_response_key
derive_ipek = _core.derive_ipek
derive_key = _core.derive_key
ksn_counter = _core.ksn_counter
ksn_with_counter = _core.ksn_with_counter
mac_request_key = _core.mac_request_key
mac_response_key = _core.mac_response_key
next_ksn = _core.next_ksn
pin_key = _core.pin_key
decode_pin_block = pinblock.decode_pin_block
decrypt_pin = pinblock.decrypt_pin
encode_pin_block = pinblock.encode_pin_block
encrypt_pin = pinblock.encrypt_pin

__version__ = "0.1.0"

__all__ = [
    "COUNTER_MASK",
    "DATA_REQUEST_VARIANT",
    "DATA_RESPONSE_VARIANT",
    "KEY_LEN",
    "KSN_LEN",
    "MAC_REQUEST_VARIANT",
    "MAC_RESPONSE_VARIANT",
    "MAX_COUNTER",
    "MAX_COUNTER_ONE_BITS",
    "PIN_VARIANT",
    "KSNExhaustedError",
    "__version__",
    "data_request_key",
    "data_response_key",
    "decode_pin_block",
    "decrypt_pin",
    "derive_ipek",
    "derive_key",
    "encode_pin_block",
    "encrypt_pin",
    "ksn_counter",
    "ksn_with_counter",
    "mac_request_key",
    "mac_response_key",
    "next_ksn",
    "pin_key",
]

pydukpt = larky.struct(
    __name__='pydukpt',
    __version__=__version__,
    COUNTER_MASK=COUNTER_MASK,
    DATA_REQUEST_VARIANT=DATA_REQUEST_VARIANT,
    DATA_RESPONSE_VARIANT=DATA_RESPONSE_VARIANT,
    KEY_LEN=KEY_LEN,
    KSN_LEN=KSN_LEN,
    MAC_REQUEST_VARIANT=MAC_REQUEST_VARIANT,
    MAC_RESPONSE_VARIANT=MAC_RESPONSE_VARIANT,
    MAX_COUNTER=MAX_COUNTER,
    MAX_COUNTER_ONE_BITS=MAX_COUNTER_ONE_BITS,
    PIN_VARIANT=PIN_VARIANT,
    KSNExhaustedError=KSNExhaustedError,
    data_request_key=data_request_key,
    data_response_key=data_response_key,
    decode_pin_block=decode_pin_block,
    decrypt_pin=decrypt_pin,
    derive_ipek=derive_ipek,
    derive_key=derive_key,
    encode_pin_block=encode_pin_block,
    encrypt_pin=encrypt_pin,
    ksn_counter=ksn_counter,
    ksn_with_counter=ksn_with_counter,
    mac_request_key=mac_request_key,
    mac_response_key=mac_response_key,
    next_ksn=next_ksn,
    pin_key=pin_key,
)
