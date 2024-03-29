# -*- coding: ascii -*-
#
#  Util/Counter.py : Fast counter for use with CTR-mode ciphers
#
# Written in 2008 by Dwayne C. Litzenberger <dlitz@dlitz.net>
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
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//larky", larky="larky")

_empty = bytes(r"", encoding='utf-8')


def new(nbits, prefix=_empty, suffix=_empty, initial_value=1, little_endian=False, allow_wraparound=False):
    """
    Create a stateful counter block function suitable for CTR encryption modes.

        Each call to the function returns the next counter block.
        Each counter block is made up by three parts:

        +------+--------------+-------+
        |prefix| counter value|postfix|
        +------+--------------+-------+

        The counter value is incremented by 1 at each call.

        Args:
          nbits (integer):
            Length of the desired counter value, in bits. It must be a multiple of 8.
          prefix (byte string):
            The constant prefix of the counter block. By default, no prefix is
            used.
          suffix (byte string):
            The constant postfix of the counter block. By default, no suffix is
            used.
          initial_value (integer):
            The initial value of the counter. Default value is 1.
            Its length in bits must not exceed the argument ``nbits``.
          little_endian (boolean):
            If ``True``, the counter number will be encoded in little endian format.
            If ``False`` (default), in big endian format.
          allow_wraparound (boolean):
            This parameter is ignored.
        Returns:
          An object that can be passed with the :data:`counter` parameter to a CTR mode
          cipher.

        It must hold that *len(prefix) + nbits//8 + len(suffix)* matches the
        block size of the underlying block cipher.

    """
    if (nbits % 8) != 0:
        fail("'nbits' must be a multiple of 8")

    iv_bl = _JCrypto.Math.bit_length(initial_value)
    if iv_bl > nbits:
        fail("'ValueError('Initial value takes %d bits but it is longer than" +
                          " the counter (%d bits)'" % (iv_bl, nbits))

    # Ignore wraparound
    return {"counter_len": nbits // 8,
            "prefix": prefix,
            "suffix": suffix,
            "initial_value": initial_value,
            "little_endian": little_endian
            }
Counter = larky.struct(
  new = new
)