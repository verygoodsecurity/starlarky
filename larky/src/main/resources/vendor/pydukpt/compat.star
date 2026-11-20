# Ported from pydukpt 0.1.0 (3778da71): src/pydukpt/compat.py. See larky/src/main/resources/VENDORED.md.
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
#
# - pydukpt.compat reproduces the public API (class, method, attribute and
#   argument names) of dukpt 1.0.1 (https://pypi.org/project/dukpt/1.0.1/,
#   https://github.com/allen246/dukpt, derived from
#   https://github.com/chokepoint/DUKPT; MIT License, Copyright (c) 2017
#   Chokepoint), so that its users can switch. Its implementation is new.
"""Drop-in replacement for the API of the PyPI ``dukpt`` 1.0.1 package.

``from pydukpt.compat import DUKPT, Server, Client, InvalidDUKPTArguments``
gives the classes, method names and constructor arguments of ``dukpt`` 1.0.1,
but keys are derived as ANSI X9.24-1 specifies for every transaction counter.
This module is new code written against that API; it does not contain code
from ``dukpt``. Differences from 1.0.1:

ANSI X9.24-1 corrections

- ``derive_key`` uses all 21 counter bits. 1.0.1 ignores counter bits 9-20,
  so its keys are wrong for every counter >= 0x200 (for 0x200 and 0x400 it
  returns the IPEK). Keys for counters below 0x200 are the same.
- ``generate_right_ipek`` clears the counter bits of the KSN as
  ``generate_left_ipek`` does. 1.0.1 does not, so its IPEK is wrong whenever
  counter bit 16 or above is set.
- ``increase_counter`` moves to the next counter with at most ten 1 bits (as
  :func:`pydukpt.next_ksn` does) and raises :class:`pydukpt.KSNExhaustedError`
  after 0x1FF800. 1.0.1 raises ``AttributeError`` on Python 3; on Python 2
  it adds one to the counter read as a *signed* 21-bit integer, which raises
  at 0x0FFFFF and wraps 0x1FFFFF to 0.
  ``Client.gen_key`` returns the key for the last counter and raises on the
  call after it.

Python 3 and type differences

- Every method that 1.0.1 documents as returning a ``bitstring.BitArray``
  returns ``bytes``. Arguments may be ``bytes``, ``bytearray``,
  ``memoryview`` or any object with a ``bytes`` attribute holding bytes (such
  as a ``BitArray``); anything else raises ``TypeError``. 1.0.1 fails on
  Python 3 when given raw bytes in several places, and ``Client`` cannot be
  constructed there at all.
- ``bitstring`` is not a dependency. The ``_pin_mask`` and ``_mac_*``
  attributes are ``bytes``.
- Lengths are checked: a BDK or IPEK must be 16 bytes and a KSN 10 bytes.
  Wrong lengths passed to a constructor raise :class:`InvalidDUKPTArguments`;
  passed to a method, ``ValueError``. ``InvalidDUKPTArguments`` subclasses
  ``ValueError`` (in 1.0.1 it subclasses ``Exception``).
- ``Server(bdk)`` uses ``bdk`` unless it is ``None``. 1.0.1 generates a random
  BDK for any false value, including ``b""``.

Larky differences from pydukpt 0.1.0 (see also ``pydukpt/_core``):

- ``DUKPT``, ``Server`` and ``Client`` are constructor functions returning
  mutable structs, not classes: there is no subclassing, and the class
  attributes (``BDK_LEN``, ``KSN_LEN``, ``_pin_mask``, ``_mac_*``) are read
  from an instance. ``BDK_LEN`` and ``KSN_LEN`` are also module constants.
- No ``memoryview``; an object with a ``bytes`` attribute is any struct that
  has one.
- Errors are ``fail()``s prefixed with the Python exception name:
  ``"InvalidDUKPTArguments: Must have either ipek or bdk"``,
  ``"KSNExhaustedError: KSN counter exhausted"``, ``"TypeError: ..."``,
  ``"ValueError: ..."``. Calling ``InvalidDUKPTArguments(msg)`` fails the
  same way.
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Random", get_random_bytes="get_random_bytes")
load("@vendor//pydukpt/_core", _core="core")

KSNExhaustedError = _core.KSNExhaustedError

__all__ = ["BDK_LEN", "DUKPT", "KSN_LEN", "Client", "InvalidDUKPTArguments", "Server"]

BDK_LEN = _core.KEY_LEN
KSN_LEN = _core.KSN_LEN


def InvalidDUKPTArguments(msg=""):
    """Missing or malformed constructor arguments.

    In Python a subclass of ``ValueError``. Larky has no exception classes:
    calling this fails with ``"InvalidDUKPTArguments: " + msg``.
    """
    fail("InvalidDUKPTArguments: " + msg)


def _raw(name, value):
    if types.is_bytes(value) or types.is_bytearray(value):
        return bytes(value)
    inner = getattr(value, "bytes", None)
    if types.is_bytes(inner) or types.is_bytearray(inner):
        return bytes(inner)
    fail("TypeError: %s must be bytes-like; got %s" % (name, type(value)))


def _checked(name, value, length):
    data = _raw(name, value)
    if len(data) != length:
        InvalidDUKPTArguments("%s must have a length of %d" % (name.upper(), length))
    return data


def _DUKPT_class(self):
    # The class attributes and methods of DUKPT, set on an instance.
    self._pin_mask = _core.PIN_VARIANT
    self._mac_req_mask = _core.MAC_REQUEST_VARIANT
    self._mac_resp_mask = _core.MAC_RESPONSE_VARIANT
    self._mac_data_req = _core.DATA_REQUEST_VARIANT
    self._mac_data_resp = _core.DATA_RESPONSE_VARIANT
    self._ipek = None
    self._tdes_key = None
    self._cur_key = None
    self._ksn = None
    self._bdk = None
    self.BDK_LEN = BDK_LEN
    self.KSN_LEN = KSN_LEN

    def derive_key(ipek, ksn):
        """Derive the current transaction key for ``ksn`` from ``ipek``.

        Also stored as ``self._cur_key``.
        """
        self._cur_key = _core.derive_key(_raw("ipek", ipek), _raw("ksn", ksn))
        return self._cur_key
    self.derive_key = derive_key

    def reset_counter(data):
        """Return ``data`` with its last 21 bits (the counter) cleared.

        Returns ``None`` if ``data`` is shorter than 3 bytes.
        """
        raw = _raw("data", data)
        if len(raw) < 3:
            return None
        return raw[:-3] + bytes([raw[-3] & 0xE0, 0, 0])
    self.reset_counter = reset_counter

    def copy_counter(data):
        """Return the counter (last 21 bits of ``data``) as 3 bytes."""
        raw = _raw("data", data)
        if len(raw) < 3:
            fail("ValueError: data must be at least 3 bytes")
        return bytes([raw[-3] & 0x1F, raw[-2], raw[-1]])
    self.copy_counter = copy_counter

    def increase_counter():
        """Advance the stored KSN to the next counter a device may use.

        Raises:
            pydukpt.KSNExhaustedError: the counter is at or past 0x1FF800.
        """
        if self._ksn == None:
            fail("ValueError: no KSN to increase")
        self._ksn = _core.next_ksn(self._ksn)
    self.increase_counter = increase_counter
    return self


def _DUKPT_init(self, bdk=None, ksn=None, ipek=None):
    """Initialize from either an IPEK and KSN (client) or a BDK (server).

    Args:
        bdk: Base Derivation Key, 16 bytes.
        ksn: Key Serial Number, 10 bytes (used with ``ipek``).
        ipek: Initial PIN Encryption Key, 16 bytes.
    """
    if ipek:
        self._ipek = _checked("ipek", ipek, _core.KEY_LEN)
        if ksn == None:
            InvalidDUKPTArguments("Must have a ksn with an ipek")
        self._ksn = _checked("ksn", ksn, KSN_LEN)
    else:
        if not bdk:
            InvalidDUKPTArguments("Must have either ipek or bdk")
        self._bdk = _raw("bdk", bdk)
        if len(self._bdk) != self.BDK_LEN:
            InvalidDUKPTArguments("BDK must have a length of %d" % self.BDK_LEN)


def DUKPT(bdk=None, ksn=None, ipek=None):
    """Base DUKPT class with common functions of both client and server."""
    self = larky.mutablestruct(__name__='DUKPT', __class__=DUKPT)
    _DUKPT_class(self)
    _DUKPT_init(self, bdk=bdk, ksn=ksn, ipek=ipek)
    return self


def Server(bdk=None):
    """Host side: holds the BDK and derives keys for any KSN.

    Initialize with ``bdk``, or a random 16-byte BDK when it is ``None``.
    """
    self = larky.mutablestruct(__name__='Server', __class__=Server)
    _DUKPT_class(self)

    def generate_ksn():
        """Return a random 10-byte KSN with its counter bits cleared."""
        return self.reset_counter(get_random_bytes(self.KSN_LEN))
    self.generate_ksn = generate_ksn

    def generate_bdk():
        """Return a random 16-byte BDK."""
        return get_random_bytes(self.BDK_LEN)
    self.generate_bdk = generate_bdk

    def generate_ipek(ksn):
        """Derive the IPEK for ``ksn`` (its counter is ignored)."""
        raw = _raw("ksn", ksn)
        self._tdes_key = self._bdk + self._bdk[:8]
        self.generate_left_ipek(raw)
        self.generate_right_ipek(raw)
        return self._ipek
    self.generate_ipek = generate_ipek

    def generate_left_ipek(ksn):
        """Set ``self._ipek`` to the left 8 bytes of the IPEK for ``ksn``."""
        self._ipek = _core.derive_ipek(self._bdk, _raw("ksn", ksn))[:8]
    self.generate_left_ipek = generate_left_ipek

    def generate_right_ipek(ksn):
        """Append the right 8 bytes of the IPEK for ``ksn`` to ``self._ipek``."""
        self._ipek = self._ipek[:8] + _core.derive_ipek(self._bdk, _raw("ksn", ksn))[8:]
    self.generate_right_ipek = generate_right_ipek

    def gen_key(ksn):
        """Return the current transaction key for ``ksn``."""
        raw = _raw("ksn", ksn)
        return self.derive_key(self.generate_ipek(raw), raw)
    self.gen_key = gen_key

    if bdk == None:
        self.bdk = self.generate_bdk()
        bdk = self.bdk
    _DUKPT_init(self, bdk=bdk)
    return self


def Client(ipek, ksn):
    """Device side: holds an IPEK and walks the KSN counter.

    As in 1.0.1, the counter is advanced once on construction, so the first
    :meth:`gen_key` uses the counter after the one passed in.
    """
    self = larky.mutablestruct(__name__='Client', __class__=Client)
    _DUKPT_class(self)

    def gen_key():
        """Return ``{'ksn': ksn, 'key': key}`` for the current KSN and advance it.

        Raises:
            pydukpt.KSNExhaustedError: the previous call used the last counter.
        """
        if self._exhausted:
            KSNExhaustedError("KSN counter exhausted")
        info = {"ksn": self._ksn, "key": self.derive_key(self._ipek, self._ksn)}
        # Python catches the KSNExhaustedError of increase_counter(); Larky
        # cannot catch a failure, so test for the last counter first.
        if _core.ksn_counter(self._ksn) >= _core.MAX_COUNTER:
            self._exhausted = True
        else:
            self.increase_counter()
        return info
    self.gen_key = gen_key

    _DUKPT_init(self, ipek=ipek, ksn=ksn)
    self._exhausted = False
    self.increase_counter()
    return self


compat = larky.struct(
    __name__='compat',
    BDK_LEN=BDK_LEN,
    KSN_LEN=KSN_LEN,
    DUKPT=DUKPT,
    Server=Server,
    Client=Client,
    InvalidDUKPTArguments=InvalidDUKPTArguments,
)
