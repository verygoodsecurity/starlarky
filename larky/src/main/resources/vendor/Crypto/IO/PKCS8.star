#
#  PublicKey/PKCS8.py : PKCS#8 functions
#
# ===================================================================
#
# Copyright (c) 2014, Legrandin <helderijs@gmail.com>
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
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@vendor//Crypto/IO/_PBES", PBES1="PBES1", PBES2="PBES2", PbesError="PbesError")
load("@vendor//Crypto/Util/asn1", DerNull="DerNull", DerSequence="DerSequence", DerObjectId="DerObjectId", DerOctetString="DerOctetString")
load("@vendor//Crypto/Util/py3compat", bord="bord", tobytes="tobytes", _copy_bytes="copy_bytes")
load("@vendor//option/result", Error="Error", Result="Result", Ok="Ok")

__all__ = ['wrap', 'unwrap']


def wrap(private_key, key_oid, passphrase=None, protection=None,
         prot_params=None, key_params=None, randfunc=None):
    """Wrap a private key into a PKCS#8 blob (clear or encrypted).

    Args:

      private_key (byte string):
        The private key encoded in binary form. The actual encoding is
        algorithm specific. In most cases, it is DER.

      key_oid (string):
        The object identifier (OID) of the private key to wrap.
        It is a dotted string, like ``1.2.840.113549.1.1.1`` (for RSA keys).

      passphrase (bytes string or string):
        The secret passphrase from which the wrapping key is derived.
        Set it only if encryption is required.

      protection (string):
        The identifier of the algorithm to use for securely wrapping the key.
        The default value is ``PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC``.

      prot_params (dictionary):
        Parameters for the protection algorithm.

        +------------------+-----------------------------------------------+
        | Key              | Description                                   |
        +==================+===============================================+
        | iteration_count  | The KDF algorithm is repeated several times to|
        |                  | slow down brute force attacks on passwords    |
        |                  | (called *N* or CPU/memory cost in scrypt).    |
        |                  | The default value for PBKDF2 is 1000.         |
        |                  | The default value for scrypt is 16384.        |
        +------------------+-----------------------------------------------+
        | salt_size        | Salt is used to thwart dictionary and rainbow |
        |                  | attacks on passwords. The default value is 8  |
        |                  | bytes.                                        |
        +------------------+-----------------------------------------------+
        | block_size       | *(scrypt only)* Memory-cost (r). The default  |
        |                  | value is 8.                                   |
        +------------------+-----------------------------------------------+
        | parallelization  | *(scrypt only)* CPU-cost (p). The default     |
        |                  | value is 1.                                   |
        +------------------+-----------------------------------------------+

      key_params (DER object):
        The algorithm parameters associated to the private key.
        It is required for algorithms like DSA, but not for others like RSA.

      randfunc (callable):
        Random number generation function; it should accept a single integer
        N and return a string of random data, N bytes long.
        If not specified, a new RNG will be instantiated
        from :mod:`Crypto.Random`.

    Return:
      The PKCS#8-wrapped private key (possibly encrypted), as a byte string.
    """

    if key_params == None:
        key_params = DerNull()

    #
    #   PrivateKeyInfo ::= SEQUENCE {
    #       version                 Version,
    #       privateKeyAlgorithm     PrivateKeyAlgorithmIdentifier,
    #       privateKey              PrivateKey,
    #       attributes              [0]  IMPLICIT Attributes OPTIONAL
    #   }
    #
    pk_info = DerSequence([
                0,
                DerSequence([
                    DerObjectId(key_oid),
                    key_params
                ]),
                DerOctetString(private_key)
            ])
    pk_info_der = pk_info.encode()

    if passphrase == None:
        return pk_info_der

    if not passphrase:
        return Error("ValueError: Empty passphrase").unwrap()

    # Encryption with PBES2
    passphrase = tobytes(passphrase)
    if protection == None:
        protection = 'PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC'
    return PBES2.encrypt(pk_info_der,
                         passphrase,
                         protection,
                         prot_params,
                         randfunc).unwrap()


def unwrap(p8_private_key, passphrase=None):
    """Unwrap a private key from a PKCS#8 blob (clear or encrypted).

    Args:
      p8_private_key (byte string):
        The private key wrapped into a PKCS#8 blob, DER encoded.
      passphrase (byte string or string):
        The passphrase to use to decrypt the blob (if it is encrypted).

    Return:
      A tuple containing

       #. the algorithm identifier of the wrapped key (OID, dotted string)
       #. the private key (byte string, DER encoded)
       #. the associated parameters (byte string, DER encoded) or ``None``

    Raises:
      ValueError : if decoding fails
    """

    if passphrase:
        passphrase = tobytes(passphrase)

        found = False
        error_str = None
        result = PBES1.decrypt(p8_private_key, passphrase)
        if result.is_ok:
            found = True
            p8_private_key = result.unwrap()
        elif Result.is_error(PbesError, result):
            error_str = "PBES1[%s]" % PbesError
        elif Result.is_error("ValueError", result):
            error_str = "PBES1[Invalid]"

        if not found:
            result = PBES2.decrypt(p8_private_key, passphrase)
            if result.is_ok:
                found = True
                p8_private_key = result.unwrap()
            elif Result.is_error(PbesError, result):
                error_str += ",PBES2[%s]" % PbesError
            elif Result.is_error("ValueError", result):
                error_str += ",PBES2[Invalid]"

        if not found:
            return Error("ValueError: Error decoding PKCS#8 (%s)" % error_str).unwrap()

    pk_info = DerSequence().decode(p8_private_key, nr_elements=(2, 3, 4))
    if len(pk_info) == 2 and not passphrase:
        return Error("Not a valid clear PKCS#8 structure " +
                     "(maybe it is encrypted?)").unwrap()
    pk_info._seq = _repack_oids(pk_info._seq)

    #
    #   PrivateKeyInfo ::= SEQUENCE {
    #       version                 Version,
    #       privateKeyAlgorithm     PrivateKeyAlgorithmIdentifier,
    #       privateKey              PrivateKey,
    #       attributes              [0]  IMPLICIT Attributes OPTIONAL
    #   }
    #   Version ::= INTEGER
    if pk_info[0] != 0:
        return Error("ValueError: Not a valid PrivateKeyInfo SEQUENCE").unwrap()

    # PrivateKeyAlgorithmIdentifier ::= AlgorithmIdentifier
    #
    #   EncryptedPrivateKeyInfo ::= SEQUENCE {
    #       encryptionAlgorithm  EncryptionAlgorithmIdentifier,
    #       encryptedData        EncryptedData
    #   }
    #   EncryptionAlgorithmIdentifier ::= AlgorithmIdentifier

    #   AlgorithmIdentifier  ::=  SEQUENCE  {
    #       algorithm   OBJECT IDENTIFIER,
    #       parameters  ANY DEFINED BY algorithm OPTIONAL
    #   }

    algo = DerSequence().decode(pk_info[1], nr_elements=(1, 2))
    algo_oid = DerObjectId().decode(algo[0]).value
    if len(algo) == 1:
        algo_params = None
    else:
        if algo[1] == b'\x05\x00':
            algo_params = None
        else:
            algo_params = algo[1]

    #   EncryptedData ::= OCTET STRING
    """
    if DerSequence._seq is returning a list containing strings, it has already been
    parsed, and the pk_info[2] contains a string that starts with a '#', and is
    followed by a hexlified string of the bytes that would be returned anyway. 
    """
    if type(pk_info[2]) == 'string':
        private_key = bytes(unhexlify(''.join(pk_info[2].split('#'))), 'utf-8')
    else: 
        private_key = DerOctetString().decode(pk_info[2]).payload

    return (algo_oid, private_key, algo_params)

def _repack_oids(sequence):
    for index, item in enumerate(sequence):
        if type(item) == 'list':
            der_seq = DerSequence()
            for thing in item:
                der_seq.append(DerObjectId(thing).encode())
            item = der_seq.encode()
        sequence[index] = item
    return sequence

PKCS8 = larky.struct(
    __name__='PKCS8',
    wrap=wrap,
    unwrap=unwrap,
)