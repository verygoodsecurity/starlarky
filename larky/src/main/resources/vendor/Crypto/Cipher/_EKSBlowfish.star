# ===================================================================
#
# Copyright (c) 2019, Legrandin <helderijs@gmail.com>
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
load("@vendor//Crypto/Cipher", Cipher="Cipher")
load("@vendor//option/result", Error="Error")


MODE_ECB = 1

# Size of a data block (in bytes)
block_size = 8

# Size of a key (in bytes)
key_size = range(0, 72 + 1)


def _create_base_cipher(dict_parameters):
    """This method instantiates and returns a smart pointer to
    a low-level base cipher. It will absorb named parameters in
    the process."""
    for i in ("key", "salt", "cost",):
        if i not in dict_parameters:
            return Error("TypeError: " + "Missing EKSBlowfish parameter: " + i).unwrap()

    key = dict_parameters.pop("key")
    salt = dict_parameters.pop("salt")
    cost = dict_parameters.pop("cost")
    invert = dict_parameters.pop("invert", True)

    if len(key) not in key_size:
        return Error("ValueError: " + "Incorrect EKSBlowfish key length (%d bytes)" % len(key)).unwrap()

    fail("IMPLEMENT ME")


def new(key, mode, salt, cost, invert):
    """Create a new EKSBlowfish cipher

    Args:

      key (bytes, bytearray, memoryview):
        The secret key to use in the symmetric cipher.
        Its length can vary from 0 to 72 bytes.

      mode (one of the supported ``MODE_*`` constants):
        The chaining mode to use for encryption or decryption.

      salt (bytes, bytearray, memoryview):
        The salt that bcrypt uses to thwart rainbow table attacks

      cost (integer):
        The complexity factor in bcrypt

      invert (bool):
        If ``False``, in the inner loop use ``ExpandKey`` first over the salt
        and then over the key, as defined in
        the `original bcrypt specification <https://www.usenix.org/legacy/events/usenix99/provos/provos_html/node4.html>`_.
        If ``True``, reverse the order, as in the first implementation of
        `bcrypt` in OpenBSD.

    :Return: an EKSBlowfish object
    """

    kwargs = { 'salt':salt, 'cost':cost, 'invert':invert }
    return Cipher._create_cipher(EKSBlowfish, key, mode, **kwargs)


EKSBlowfish = larky.struct(
    _create_base_cipher=_create_base_cipher,
    MODE_ECB=MODE_ECB,
    block_size=block_size,
    key_size=key_size,
    new=new
)