# -*- coding: utf-8 -*-
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

load("@stdlib//types", types="types")
load("@vendor//Crypto/Util/asn1", DerSequence="DerSequence", DerInteger="DerInteger", DerBitString="DerBitString", DerObjectId="DerObjectId", DerNull="DerNull")
load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")

def expand_subject_public_key_info(encoded):
    """Parse a SubjectPublicKeyInfo structure.

    It returns a triple with:
        * OID (string)
        * encoded public key (bytes)
        * Algorithm parameters (bytes or None)
    """

    #
    # SubjectPublicKeyInfo  ::=  SEQUENCE  {
    #   algorithm         AlgorithmIdentifier,
    #   subjectPublicKey  BIT STRING
    # }
    #
    # AlgorithmIdentifier  ::=  SEQUENCE  {
    #   algorithm   OBJECT IDENTIFIER,
    #   parameters  ANY DEFINED BY algorithm OPTIONAL
    # }
    #

    # TODO(mahmoudimus): should we rewrite our Larky asn1 implementation to
    #  match pycrypto's?
    spki = DerSequence().decode(encoded, nr_elements=2)
    algo = spki.__getitem__(0)
    algo_oid = algo[0]
    spk = spki.__getitem__(1)
    #algo = DerSequence().decode(spki.__getitem__(0), nr_elements=(1,2))
    #algo_oid = DerObjectId().decode(algo.__getitem__(0))
    #spk = DerBitString().decode(spki.__getitem__(1)).value
    if len(algo) == 1:
        algo_params = None
    else:
        if algo[1] == 'NULL':
            algo_params = None
        else:
            algo_params = algo[1]

    return algo_oid, spk, algo_params


def create_subject_public_key_info(algo_oid, secret_key, params=None):
    # print("IN create_subject_public_key_info:", algo_oid, secret_key, params)
    if params == None:
        params = DerNull()

    spki = DerSequence([
        DerSequence([DerObjectId(algo_oid), params]),
        DerBitString(secret_key)
    ])
    return spki.encode()


def extract_subject_public_key_info(x509_certificate):
    """Extract subjectPublicKeyInfo from a DER X.509 certificate."""

    certificate = DerSequence().decode(x509_certificate, nr_elements=3)
    tbs_certificate = DerSequence().decode(certificate.__getitem__(0),
                                           nr_elements=range(6, 11))

    index = 5
    if types.is_int(tbs_certificate.__getitem__(0)):
        tbs_certificate.__getitem__(0) + 1
        # Version not present
        version = 1
    else:
        version = (DerInteger(explicit=0)
                   .decode(tbs_certificate.__getitem__(0))
                   .value)
        if version not in (2, 3):
            fail('ValueError: Incorrect X.509 certificate version')
        index = 6

    return tbs_certificate.__getitem__(index)
