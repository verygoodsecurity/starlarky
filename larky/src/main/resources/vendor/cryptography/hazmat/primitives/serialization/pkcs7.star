# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//enum", Enum="Enum")
load("@stdlib//types", types="types")
load("@stdlib//larky", larky="larky")
load("@stdlib//builtins", builtins="builtins")
load("@vendor//cryptography/x509", x509="x509")
load("@vendor//cryptography/utils", utils="utils")
load("@vendor//cryptography/hazmat/backends", backends="backends")
load("@vendor//cryptography/hazmat/primitives",
     hashes="hashes",
     serialization="serialization")
load("@vendor//cryptography/hazmat/primitives/asymmetric",
     rsa="rsa",
     #ec="ec",
     )
load("@vendor//option/result", Error="Error")


_get_backend= backends._get_backend
_check_byteslike = utils._check_byteslike


def load_pem_pkcs7_certificates(data):
    backend = _get_backend(None)
    return backend.load_pem_pkcs7_certificates(data)


def load_der_pkcs7_certificates(data):
    backend = _get_backend(None)
    return backend.load_der_pkcs7_certificates(data)


# _ALLOWED_PKCS7_HASH_TYPES = typing.Union[
#     hashes.SHA1,
#     hashes.SHA224,
#     hashes.SHA256,
#     hashes.SHA384,
#     hashes.SHA512,
# ]

# _ALLOWED_PRIVATE_KEY_TYPES = typing.Union[rsa.RSAPrivateKey, ec.EllipticCurvePrivateKey]

PKCS7Options = enum.Enum('PKCS7Options', dict(
    Text="Add text/plain MIME type",
    Binary="Don't translate input data into canonical MIME format",
    DetachedSignature="Don't embed data in the PKCS7 structure",
    NoCapabilities="Don't embed SMIME capabilities",
    NoAttributes="Don't embed authenticatedAttributes",
    NoCerts="Don't embed signer certificate",
).items())


def PKCS7SignatureBuilder(data=None, signers=None, additional_certs=None):
    self = larky.mutablestruct(__name__='PKCS7SignatureBuilder',
                               __class__=PKCS7SignatureBuilder)

    def __init__(data, signers, additional_certs):
        self._data = data
        self._signers = signers or []
        self._additional_certs = additional_certs or []
        return self
    self = __init__(data, signers, additional_certs)

    def set_data(data):
        _check_byteslike("data", data)
        if self._data != None:
            fail("ValueError: data may only be set once")

        return PKCS7SignatureBuilder(data, self._signers)
    self.set_data = set_data

    def add_signer(
        certificate,
        private_key,
        hash_algorithm,
    ):
        if not any(
                [builtins.isinstance(hash_algorithm, x)
                 for x in (
                        hashes.SHA1,
                        hashes.SHA224,
                        hashes.SHA256,
                        hashes.SHA384,
                        hashes.SHA512,
                     )
                 ]
        ):
            fail("hash_algorithm must be one of hashes.SHA1, SHA224, " +
                 "SHA256, SHA384, or SHA512")
        if not builtins.isinstance(certificate, x509.Certificate):
            return Error("TypeError: certificate must be a x509.Certificate")

        if not any([
            builtins.isinstance(private_key, x) for x in
            (
                    rsa.RSAPrivateKey,
                    # ec.EllipticCurvePrivateKey
            )]):
            fail("TypeError: Only RSA & EC keys are supported at this time.")

        return PKCS7SignatureBuilder(
            self._data,
            self._signers + [(certificate, private_key, hash_algorithm)],
        )
    self.add_signer = add_signer

    def add_certificate(certificate):
        if not builtins.isinstance(certificate, x509.Certificate):
            fail("TypeError: certificate must be a x509.Certificate")

        return PKCS7SignatureBuilder(
            self._data, self._signers, self._additional_certs + [certificate]
        )
    self.add_certificate = add_certificate

    def sign(
        encoding,
        options,
        backend = None,
    ):
        if len(self._signers) == 0:
            fail("ValueError: Must have at least one signer")
        if self._data == None:
            fail("ValueError: You must add data to sign")
        options = list(options)
        if not all([builtins.isinstance(x, PKCS7Options) for x in options]):
            fail("ValueError: options must be from the PKCS7Options enum")
        if encoding not in (
            serialization.Encoding.PEM,
            serialization.Encoding.DER,
            serialization.Encoding.SMIME,
        ):
            fail("ValueError: Must be PEM, DER, or SMIME from the Encoding enum")

        # Text is a meaningless option unless it is accompanied by
        # DetachedSignature
        if (
            PKCS7Options.Text in options
            and PKCS7Options.DetachedSignature not in options
        ):
            fail("When passing the Text option you must also pass " +
                 "DetachedSignature")

        if PKCS7Options.Text in options and encoding in (
            serialization.Encoding.DER,
            serialization.Encoding.PEM,
        ):
            fail("ValueError: The Text option is only available " +
                 "for SMIME serialization")

        # No attributes implies no capabilities so we'll error if you try to
        # pass both.
        if (
            PKCS7Options.NoAttributes in options
            and PKCS7Options.NoCapabilities in options
        ):
            fail("NoAttributes is a superset of NoCapabilities. Do not pass " +
                 "both values.")

        backend = _get_backend(backend)
        return backend.pkcs7_sign(self, encoding, options)
    self.sign = sign
    return self


pkcs7 = larky.struct(
    __name__='pkcs7',
    load_pem_pkcs7_certificates=load_pem_pkcs7_certificates,
    load_der_pkcs7_certificates=load_der_pkcs7_certificates,
    PKCS7SignatureBuilder=PKCS7SignatureBuilder,
    PKCS7Options=PKCS7Options
)
