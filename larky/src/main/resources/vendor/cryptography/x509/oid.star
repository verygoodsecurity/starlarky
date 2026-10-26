# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
# Ported from cryptography 35.0.0 (c7fbef7): src/cryptography/x509/oid.py. See larky/src/main/resources/VENDORED.md.
load("@stdlib//larky", larky="larky")

load("@vendor//cryptography/hazmat/_oid", _oid="oid")


ObjectIdentifier = _oid.ObjectIdentifier
ExtensionOID = _oid.ExtensionOID
OCSPExtensionOID = _oid.OCSPExtensionOID
CRLEntryExtensionOID = _oid.CRLEntryExtensionOID
NameOID = _oid.NameOID
SignatureAlgorithmOID = _oid.SignatureAlgorithmOID

_SIG_OIDS_TO_HASH = _oid._SIG_OIDS_TO_HASH

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