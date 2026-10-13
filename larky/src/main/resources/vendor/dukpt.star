# Ported from dukpt 1.0.1 (PyPI sdist sha256:924076e36f946f16feb25f85969a70c513c14172a055ffc7f3d94cadded392a4; no git tag, nearest commit github.com/chokepoint/DUKPT@5d40401d267684591856189bf90cfc8b83b3fca6): dukpt.py. See larky/src/main/resources/VENDORED.md.
#
# MIT License
#
# Copyright (c) 2017 Chokepoint
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
# Differences from upstream:
#
# - bitstring is not available. Every argument and return value that upstream
#   documents as "BitArray" (or "raw or BitArray") is `bytes` here; `bytearray`
#   is accepted wherever bytes are. The `_pin_mask`, `_mac_*` attributes are
#   bytes too.
# - Upstream only runs as written on Python 2. On Python 3, `Server.gen_key`,
#   `Server.generate_ipek` and `DUKPT.derive_key` fail for raw bytes
#   (AttributeError: no `.bytes`), and `Client` fails in its constructor
#   (`generator.next()`). This port gives every method the result Python 2
#   gives for raw byte strings.
# - Errors are `fail()`s whose message starts with the Python exception name:
#   "InvalidDUKPTArguments: Must have either ipek or bdk",
#   "InvalidDUKPTArguments: BDK must have a length of 16",
#   "TypeError: ..." for a non-bytes argument, and "ValueError: ..." where
#   bitstring would raise (operands of different lengths, a KSN shorter than
#   10 bytes in increase_counter, a counter that does not fit in a signed
#   21-bit integer).
#
# Behaviour kept from upstream 1.0.1 that differs from ANSI X9.24-1 (the
# cross-check against the Python package depends on it):
#
# - derive_key starts its shift register at 0x000100, so only the low 9
#   counter bits are used: for a counter >= 0x200 the key differs from the
#   standard's (0x200 and 0x400 both give the IPEK itself).
# - generate_right_ipek encrypts the KSN without clearing its counter bits.
# - increase_counter adds one to the 21 counter bits read as a signed
#   integer: 0x1FFFFF wraps to 0 and 0x0FFFFF fails.
load("@stdlib//binascii", unhexlify="unhexlify")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//Crypto/Cipher/DES", DES="DES")
load("@vendor//Crypto/Cipher/DES3", DES3="DES3")
load("@vendor//Crypto/Random", Random="Random")

BDK_LEN = 16
KSN_LEN = 10

_PIN_MASK = unhexlify("00000000000000FF00000000000000FF")
_MAC_REQ_MASK = unhexlify("000000000000FF00000000000000FF00")
_MAC_RESP_MASK = unhexlify("00000000FF00000000000000FF000000")
_MAC_DATA_REQ = unhexlify("0000000000FF00000000000000FF0000")
_MAC_DATA_RESP = unhexlify("000000FF00000000000000FF00000000")

_COUNTER_MASK = 0x1FFFFF


def InvalidDUKPTArguments(msg=""):
    """Error raised for missing or malformed constructor arguments.

    Larky has no exception classes: the port fails with a message that
    starts with "InvalidDUKPTArguments: ". Calling this fails the same way.
    """
    fail("InvalidDUKPTArguments: " + msg)


def _bytes(name, value):
    if types.is_bytes(value):
        return value
    if types.is_bytearray(value):
        return bytes(value)
    fail("TypeError: %s must be bytes; got %s" % (name, type(value)))


def _bitop(op, a, b):
    if len(a) != len(b):
        fail("ValueError: Bitstrings must have the same length for %s operator." % op)
    if op == "&":
        return bytes([a[i] & b[i] for i in range(len(a))])
    if op == "|":
        return bytes([a[i] | b[i] for i in range(len(a))])
    return bytes([a[i] ^ b[i] for i in range(len(a))])


def _is_zero(data):
    for b in data:
        if b != 0:
            return False
    return True


def _int3(value):
    return bytes([(value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF])


def _des_encrypt(key, data):
    return DES.new(key[:DES.key_size], DES.MODE_ECB).encrypt(data)


def _DUKPT(self, bdk=None, ksn=None, ipek=None):
    """Base DUKPT class with common functions of both client and server"""
    self._pin_mask = _PIN_MASK
    self._mac_req_mask = _MAC_REQ_MASK
    self._mac_resp_mask = _MAC_RESP_MASK
    self._mac_data_req = _MAC_DATA_REQ
    self._mac_data_resp = _MAC_DATA_RESP
    self._ipek = None
    self._tdes_key = None
    self._cur_key = None
    self._ksn = None
    self.BDK_LEN = BDK_LEN
    self.KSN_LEN = KSN_LEN

    def __init__(bdk, ksn, ipek):
        """Initialization
        Keyword arguments:
        bdk (raw or BitArray)  -- Base Derivation Key (16 bytes)
        ksn (raw or BitArray)  -- Key Serial Number (10 bytes)
        ipek (raw or BitArray) -- Initial Pin Encryption Key (16 bytes)
        """
        if ipek:
            self._ipek = _bytes("ipek", ipek)
            self._ksn = _bytes("ksn", ksn)
        else:
            if not bdk:
                InvalidDUKPTArguments("Must have either ipek or bdk")
            if len(bdk) != self.BDK_LEN:
                InvalidDUKPTArguments("BDK must have a length of %d" % self.BDK_LEN)
            self._bdk = _bytes("bdk", bdk)

    def derive_key(ipek, ksn):
        """Derive a unique key given the ipek and ksn

        Keyword arguments:
        ipek (BitArray) -- Initial Pin Encryption Key
        ksn (BitArray)  -- Key Serial Number
        """
        ipek = _bytes("ipek", ipek)
        ksn = _bytes("ksn", ksn)
        c_mask = unhexlify("c0c0c0c000000000c0c0c0c000000000")
        ksn_offset = 2
        ctr_offset = -3
        right_offset = 8

        # Registers taken from documentation
        curkey = ipek
        ksnr = ksn[ksn_offset:]
        r3 = self.copy_counter(ksnr)
        r8 = self.reset_counter(ksnr)
        # sr     = BitArray(hex='0x100000')
        # Upstream 1.0.1 starts at 0x000100: the shift register walks bits 8..0.
        for shift in range(8, -1, -1):
            sr = _int3(1 << shift)
            tmp = self.copy_counter(sr)
            tmp = _bitop("&", tmp, r3)
            if not _is_zero(tmp):
                # Step 2
                n_ctr = _bitop("|", r8[ctr_offset:], sr)
                r8 = r8[:ctr_offset] + n_ctr
                # Step 3
                r8a = _bitop("^", r8, curkey[right_offset:])
                # Step 4
                r8a = _des_encrypt(curkey, r8a)
                # Step 5
                r8a = _bitop("^", curkey[right_offset:], r8a)

                # Step 6
                curkey = _bitop("^", curkey, c_mask)

                # Step 7
                r8b = _bitop("^", curkey[right_offset:], r8)

                # Step 8
                r8b = _des_encrypt(curkey, r8b)

                # Step 9
                r8b = _bitop("^", curkey[right_offset:], r8b)

                # Step 10 / 11
                curkey = r8b + r8a
        self._cur_key = curkey
        return curkey
    self.derive_key = derive_key

    def reset_counter(data):
        """Reset the counter to zero

        Keyword arguments:
        data (raw or BitArray) -- Must be at least 3 bytes

        Return:
        BitArray of the data passed in
        """
        data = _bytes("data", data)
        if len(data) < 3:
            return None
        mask = unhexlify("e00000")
        ctr = data[len(data) - 3:]
        return data[:-3] + _bitop("&", mask, ctr)
    self.reset_counter = reset_counter

    def copy_counter(data):
        """Copy only the counter bytes from a given string or BitArray

        Keyword arguments:
        data (raw or BitArray) -- Must be at least 3 bytes

        Return:
        BitArray of only the counter bytes
        """
        data = _bytes("data", data)
        mask = unhexlify("1fffff")
        if len(data) > 3:
            ctr = data[-3:]
        else:
            ctr = data

        return _bitop("&", mask, ctr)
    self.copy_counter = copy_counter

    def increase_counter():
        """Increase the counter bytes of the stored ksn by one"""
        # Upstream: bits 59..79 of the KSN, read as a signed 21-bit integer.
        ksn = self._ksn
        if len(ksn) < KSN_LEN:
            fail("ValueError: KSN must be at least %d bytes; got %d" % (KSN_LEN, len(ksn)))
        ctr = ((ksn[7] << 16) | (ksn[8] << 8) | ksn[9]) & _COUNTER_MASK
        if ctr >= 0x100000:
            ctr -= 0x200000
        ctr += 1
        if ctr > 0xFFFFF:
            fail(("ValueError: %d is too large a signed integer for a bitstring of length 21. " +
                  "The allowed range is [-1048576, 1048575].") % ctr)
        ctr &= _COUNTER_MASK
        self._ksn = (ksn[:7] +
                     bytes([(ksn[7] & 0xE0) | (ctr >> 16), (ctr >> 8) & 0xFF, ctr & 0xFF]) +
                     ksn[10:])
    self.increase_counter = increase_counter

    __init__(bdk, ksn, ipek)
    return self


def DUKPT(bdk=None, ksn=None, ipek=None):
    """Base DUKPT class with common functions of both client and server"""
    self = larky.mutablestruct(__name__='DUKPT', __class__=DUKPT)
    return _DUKPT(self, bdk=bdk, ksn=ksn, ipek=ipek)


def Server(bdk=None):
    self = larky.mutablestruct(__name__='Server', __class__=Server)

    def generate_ksn():
        """Genereate a new random KSN with counter bits zeroed
        Return:
        BitArray of the new KSN
        """
        return self.reset_counter(Random.get_random_bytes(self.KSN_LEN))

    def generate_bdk():
        """Generate a new random BDK
        Return:
        bytes of the new KSN
        """
        return Random.get_random_bytes(self.BDK_LEN)

    def generate_ipek(ksn):
        """Generate a new IPEK based on the given KSN

        Keyword arguments:
        ksn (raw or BitArray) -- Key Serial Number

        Return:
        BitArray of the new IPEK
        """
        ksn = _bytes("ksn", ksn)
        self._tdes_key = self._bdk + self._bdk[:DES.key_size]
        self.generate_left_ipek(ksn)
        self.generate_right_ipek(ksn)
        return self._ipek

    def generate_left_ipek(ksn):
        """Generate the left portion of the IPEK (8 bytes)

        Keyword arguments:
        ksn (raw or BitArray) -- Key Serial Number)
        """
        ksn = self.reset_counter(ksn)
        cipher = DES3.new(self._tdes_key, DES3.MODE_ECB)
        self._ipek = cipher.encrypt(ksn[:8])

    def generate_right_ipek(ksn):
        """Generate the right portion of the IPEK (8 bytes)

        Keyword arguments:
        ksn (raw or BitArray) -- Key Serial Number
        """
        ksn = _bytes("ksn", ksn)
        mask = unhexlify("c0c0c0c000000000c0c0c0c000000000c0c0c0c000000000")
        key = _bitop("^", mask, self._tdes_key)
        cipher = DES3.new(key, DES3.MODE_ECB)
        self._ipek = self._ipek + cipher.encrypt(ksn[:8])

    def gen_key(ksn):
        """Generate the next key given the KSN

        Keyword arguments:
        ksn (raw or BitArray) -- Key Serial Number (10 bytes)

        Return:
        key in bytes
        """
        ipek = self.generate_ipek(ksn)
        key = self.derive_key(ipek, ksn)
        return key

    self.generate_ksn = generate_ksn
    self.generate_bdk = generate_bdk
    self.generate_ipek = generate_ipek
    self.generate_left_ipek = generate_left_ipek
    self.generate_right_ipek = generate_right_ipek
    self.gen_key = gen_key

    # Upstream calls self.generate_bdk() before DUKPT.__init__ has run; the
    # class attribute BDK_LEN it reads is the module constant here.
    if bdk:
        _DUKPT(self, bdk=bdk)
    else:
        self.bdk = Random.get_random_bytes(BDK_LEN)
        _DUKPT(self, bdk=self.bdk)
    return self


def Client(ipek, ksn):
    """Initialization of client

    Keyword arguments:
    ipek (raw or BitArray) -- Initial Pin Encryption Key
    ksn  (raw or BitArray) -- Key Serial Number
    """
    self = larky.mutablestruct(__name__='Client', __class__=Client)

    def gen_key():
        """Generate the next key in the sequence

        Return:
        key in bytes
        """
        key = self.derive_key(self._ipek, self._ksn)
        info = {'ksn': self._ksn, 'key': key}
        self.increase_counter()
        return info

    _DUKPT(self, ipek=ipek, ksn=ksn)
    self.gen_key = gen_key
    self.increase_counter()
    return self


dukpt = larky.struct(
    __name__='dukpt',
    InvalidDUKPTArguments=InvalidDUKPTArguments,
    DUKPT=DUKPT,
    Server=Server,
    Client=Client,
    BDK_LEN=BDK_LEN,
    KSN_LEN=KSN_LEN,
)
