# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//larky", larky="larky")

load("@vendor//cryptography/hazmat/_oid", _oid="oid")
load("@vendor//cryptography/hazmat/primitives", hashes="hashes")


ObjectIdentifier = _oid.ObjectIdentifier
ExtensionOID = _oid.ExtensionOID
OCSPExtensionOID = _oid.OCSPExtensionOID
CRLEntryExtensionOID = _oid.CRLEntryExtensionOID
NameOID = _oid.NameOID
SignatureAlgorithmOID = _oid.SignatureAlgorithmOID


_SIG_OIDS_TO_HASH = {
    SignatureAlgorithmOID.RSA_WITH_MD5: hashes.MD5(),
    SignatureAlgorithmOID.RSA_WITH_SHA1: hashes.SHA1(),
    SignatureAlgorithmOID._RSA_WITH_SHA1: hashes.SHA1(),
    SignatureAlgorithmOID.RSA_WITH_SHA224: hashes.SHA224(),
    SignatureAlgorithmOID.RSA_WITH_SHA256: hashes.SHA256(),
    SignatureAlgorithmOID.RSA_WITH_SHA384: hashes.SHA384(),
    SignatureAlgorithmOID.RSA_WITH_SHA512: hashes.SHA512(),
    SignatureAlgorithmOID.ECDSA_WITH_SHA1: hashes.SHA1(),
    SignatureAlgorithmOID.ECDSA_WITH_SHA224: hashes.SHA224(),
    SignatureAlgorithmOID.ECDSA_WITH_SHA256: hashes.SHA256(),
    SignatureAlgorithmOID.ECDSA_WITH_SHA384: hashes.SHA384(),
    SignatureAlgorithmOID.ECDSA_WITH_SHA512: hashes.SHA512(),
    SignatureAlgorithmOID.DSA_WITH_SHA1: hashes.SHA1(),
    SignatureAlgorithmOID.DSA_WITH_SHA224: hashes.SHA224(),
    SignatureAlgorithmOID.DSA_WITH_SHA256: hashes.SHA256(),
    SignatureAlgorithmOID.ED25519: None,
    SignatureAlgorithmOID.ED448: None,
    SignatureAlgorithmOID.GOSTR3411_94_WITH_3410_2001: None,
    SignatureAlgorithmOID.GOSTR3410_2012_WITH_3411_2012_256: None,
    SignatureAlgorithmOID.GOSTR3410_2012_WITH_3411_2012_512: None,
}

ExtendedKeyUsageOID = _oid.ExtendedKeyUsageOID
AuthorityInformationAccessOID = _oid.AuthorityInformationAccessOID
SubjectInformationAccessOID = _oid.SubjectInformationAccessOID
CertificatePoliciesOID = _oid.CertificatePoliciesOID
AttributeOID = _oid.AttributeOID
_OID_NAMES = _oid._OID_NAMES


oid = larky.struct(
    __name__="oid",
    ObjectIdentifier=_oid.ObjectIdentifier,
    ExtensionOID=_oid.ExtensionOID,
    OCSPExtensionOID=_oid.OCSPExtensionOID,
    CRLEntryExtensionOID=_oid.CRLEntryExtensionOID,
    NameOID=_oid.NameOID,
    SignatureAlgorithmOID=_oid.SignatureAlgorithmOID,
    _SIG_OIDS_TO_HASH=_SIG_OIDS_TO_HASH,
    ExtendedKeyUsageOID=_oid.ExtendedKeyUsageOID,
    AuthorityInformationAccessOID=_oid.AuthorityInformationAccessOID,
    SubjectInformationAccessOID=_oid.SubjectInformationAccessOID,
    CertificatePoliciesOID=_oid.CertificatePoliciesOID,
    AttributeOID=_oid.AttributeOID,
    _OID_NAMES=_oid._OID_NAMES,
)