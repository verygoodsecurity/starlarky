# ===================================================================
#
# Copyright (c) 2015, Legrandin <helderijs@gmail.com>
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
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@vendor//Crypto/Util/py3compat", bord="bord")
load("@stdlib//types", types="types")


def SHAKE128_XOF(data=None):
    """A SHAKE128 hash object.
    Do not instantiate directly.
    Use the :func:`new` function.

    :ivar oid: ASN.1 Object ID
    :vartype oid: string
    """

    def __init__(data):
        _state = _JCrypto.Hash.SHAKE128()
        _is_squeezing = False
        if data:
            _state.update(data)
        return larky.mutablestruct(
            _state=_state,
            _is_squeezing=_is_squeezing
        )
    self = __init__(data)

    # ASN.1 Object ID
    self.oid = "2.16.840.1.101.3.4.2.11"

    def update(data):
        """Continue hashing of a message by consuming the next chunk of data.

        Args:
            data (byte string/byte array/memoryview): The next chunk of the message being hashed.
        """
        if not types.is_bytelike(data):
            fail('TypeError: data is not byte-like')

        if self._is_squeezing:
            fail('TypeError: You cannot call "update" after the first "read"')

        self._state.update(data)
        return self
    self.update = update

    def read(length):
        """
        Compute the next piece of XOF output.

        .. note::
            You cannot use :meth:`update` anymore after the first call to
            :meth:`read`.

        Args:
            length (integer): the amount of bytes this method must return

        :return: the next piece of XOF output (of the given length)
        :rtype: byte string
        """

        self._is_squeezing = True
        return self._state.read(length)
    self.read = read

    def new(data=None):
        return SHAKE128_XOF(data=data)
    self.new = new
    return self


def new(data=None):
    """Return a fresh instance of a SHAKE128 object.

    Args:
       data (bytes/bytearray/memoryview):
        The very first chunk of the message to hash.
        It is equivalent to an early call to :meth:`update`.
        Optional.

    :Return: A :class:`SHAKE128_XOF` object
    """

    return SHAKE128_XOF(data=data)


SHAKE128 = larky.struct(
    SHAKE128=SHAKE128_XOF,
    new=new,
)

