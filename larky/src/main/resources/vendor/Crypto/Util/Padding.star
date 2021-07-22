# -*- coding: ascii -*-
#
#  Util/ASN1.py : Minimal support for ASN.1 DER binary encoding.
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
load("@stdlib//builtins", "builtins")
load("@stdlib//larky", larky="larky")
load("@vendor//Crypto/Util/py3compat",bchr="bchr", bord="bord")


def pad(data_to_pad, block_size, style='pkcs7'):
    """Apply standard padding.

    Args:
      data_to_pad (byte string):
        The data that needs to be padded.
      block_size (integer):
        The block boundary to use for padding. The output length is guaranteed
        to be a multiple of :data:`block_size`.
      style (string):
        Padding algorithm. It can be *'pkcs7'* (default), *'iso7816'* or *'x923'*.

    Return:
      byte string : the original data with the appropriate padding added at the end.
    """
    # NOTE(Larky-Difference): in larky, bytes are immutable.
    #  so we must use bytearrays instead of bytes.
    data_to_pad = bytearray(data_to_pad)
    padding_len = block_size - len(data_to_pad) % block_size
    if style == 'pkcs7':
        padding = bchr(padding_len) * padding_len
    elif style == 'x923':
        padding = bytearray(bchr(0) * (padding_len-1)) + bchr(padding_len)
    elif style == 'iso7816':
        padding = bytearray(bchr(128)) + bytearray(bchr(0) * (padding_len-1))
    else:
        fail("ValueError('Unknown padding style')")
    return data_to_pad + padding


def unpad(padded_data, block_size, style='pkcs7'):
    """Remove standard padding.

    Args:
      padded_data (byte string):
        A piece of data with padding that needs to be stripped.
      block_size (integer):
        The block boundary to use for padding. The input length
        must be a multiple of :data:`block_size`.
      style (string):
        Padding algorithm. It can be *'pkcs7'* (default), *'iso7816'* or *'x923'*.
    Return:
        byte string : data without padding.
    Raises:
      ValueError: if the padding is incorrect.
    """
    pdata_len = len(padded_data)
    if pdata_len == 0:
        fail("ValueError('Zero-length input cannot be unpadded')")
    if pdata_len % block_size:
        fail("ValueError('Input data is not padded')")
    if style in ('pkcs7', 'x923'):
        padding_len = bord(padded_data.elems()[-1])
        if padding_len<1 or padding_len>min(block_size, pdata_len):
            fail("ValueError('Padding is incorrect.')")
        if style == 'pkcs7':
            if padded_data[-padding_len:] != bchr(padding_len) * padding_len:
                fail("ValueError('PKCS#7 padding is incorrect.')")
        else:
            if padded_data[-padding_len:-1] != bchr(0) * (padding_len-1):
                fail("ValueError('ANSI X.923 padding is incorrect.')")
    elif style == 'iso7816':
        padding_len = pdata_len - padded_data.rfind(bchr(128))
        if padding_len<1 or padding_len>min(block_size, pdata_len):
            fail("ValueError('Padding is incorrect.')")
        if padding_len>1 and padded_data[1-padding_len:] != bchr(0) * (padding_len-1):
            fail("ValueError('ISO 7816-4 padding is incorrect.')")
    else:
        fail("ValueError('Unknown padding style')")
    return padded_data[:-padding_len]