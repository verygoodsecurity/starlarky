# ===================================================================
#
# Copyright (c) 2019, Helder Eijs <helderijs@gmail.com>
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

load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//struct", struct="struct")
load("@vendor//Crypto/Util/strxor", strxor="strxor")
load("@vendor//Crypto/Util/py3compat", tostr="tostr", bchr="bchr", bord="bord")
load("@stdlib//builtins","builtins")


def read_int4(data):
    if len(data) < 4:
        fail("ValueError: Insufficient data")
    value = struct.unpack(">I", data[:4])[0]
    return value, data[4:]


def read_bytes(data):
    size, data = read_int4(data)
    if len(data) < size:
        fail('ValueError: Insufficient data (V)')
    return data[:size], data[size:]


def read_string(data):
    s, d = read_bytes(data)
    return tostr(s), d


def check_padding(pad):
    for v, x in enumerate(pad):
        if bord(x) != ((v + 1) & 0xFF):
            fail("ValueError: Incorrect padding")


def import_openssh_private_generic(data, password):
    # https://cvsweb.openbsd.org/cgi-bin/cvsweb/src/usr.bin/ssh/PROTOCOL.key?annotate=HEAD
    # https://github.com/openssh/openssh-portable/blob/master/sshkey.c
    # https://coolaj86.com/articles/the-openssh-private-key-format/
    # https://coolaj86.com/articles/the-ssh-public-key-format/

    # b'openssh-key-v1\x00'
    if not data.startswith(bytes("openssh-key-v1\0", encoding="utf-8")):
        fail('ValueError: Incorrect magic value')
    data = data[15:]

    ciphername, data = read_string(data)
    kdfname, data = read_string(data)
    kdfoptions, data = read_bytes(data)
    number_of_keys, data = read_int4(data)

    if number_of_keys != 1:
        fail('ValueError: We only handle 1 key at a time')

    _, data = read_string(data)             # Public key
    encrypted, data = read_bytes(data)
    if data:
        fail(" ValueError(\"Too much data\")")

    if (len(encrypted) % 8) != 0:
        fail(" ValueError(\"Incorrect payload length\")")

    # Decrypt if necessary
    if ciphername == 'none':
        decrypted = encrypted
    else:
        if (ciphername, kdfname) != ('aes256-ctr', 'bcrypt'):
            fail("ValueError: Unsupported encryption scheme %s/%s" % (ciphername, kdfname))

        salt, kdfoptions = read_bytes(kdfoptions)
        iterations, kdfoptions = read_int4(kdfoptions)

        if len(salt) != 16:
            fail("ValueError: Incorrect salt length")
        if kdfoptions:
            fail("ValueError: Too much data in kdfoptions")

        decrypted = _JCrypto.PublicKey.decrypt_openssh_key(
            encrypted,
            password,
            salt
        )

    checkint1, decrypted = read_int4(decrypted)
    checkint2, decrypted = read_int4(decrypted)
    if checkint1 != checkint2:
        fail('ValueError: Incorrect checksum')
    ssh_name, decrypted = read_string(decrypted)
    return ssh_name, decrypted
