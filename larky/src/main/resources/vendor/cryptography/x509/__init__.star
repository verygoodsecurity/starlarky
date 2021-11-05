# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//larky", larky="larky")
load("@vendor//cryptography/x509/certificate_transparency", _certificate_transparency="certificate_transparency")
load("@vendor//cryptography/x509/base", _base="base")
load("@vendor//cryptography/x509/extensions", _extensions="extensions")
load("@vendor//cryptography/x509/general_name", _general_name="general_name")
load("@vendor//cryptography/x509/name", _name="name")
load("@vendor//cryptography/x509/oid", _oid="oid")

# imports
certificate_transparency = _certificate_transparency

# base
base = _base

# AttributeNotFound = base.AttributeNotFound
# Certificate = base.Certificate
# CertificateBuilder = base.CertificateBuilder
# CertificateRevocationList = base.CertificateRevocationList
# CertificateRevocationListBuilder = base.CertificateRevocationListBuilder
# CertificateSigningRequest = base.CertificateSigningRequest
# CertificateSigningRequestBuilder = base.CertificateSigningRequestBuilder
# InvalidVersion = base.InvalidVersion
# RevokedCertificate = base.RevokedCertificate
# RevokedCertificateBuilder = base.RevokedCertificateBuilder
Version = base.Version
load_der_x509_certificate = base.load_der_x509_certificate
load_der_x509_crl = base.load_der_x509_crl
load_der_x509_csr = base.load_der_x509_csr
load_pem_x509_certificate = base.load_pem_x509_certificate
load_pem_x509_crl = base.load_pem_x509_crl
load_pem_x509_csr = base.load_pem_x509_csr
# random_serial_number = base.random_serial_number

# extensions
extensions = _extensions

# AccessDescription = extensions.AccessDescription
# AuthorityInformationAccess = extensions.AuthorityInformationAccess
# AuthorityKeyIdentifier = extensions.AuthorityKeyIdentifier
# BasicConstraints = extensions.BasicConstraints
# CRLDistributionPoints = extensions.CRLDistributionPoints
# CRLNumber = extensions.CRLNumber
# CRLReason = extensions.CRLReason
# CertificateIssuer = extensions.CertificateIssuer
# CertificatePolicies = extensions.CertificatePolicies
# DeltaCRLIndicator = extensions.DeltaCRLIndicator
# DistributionPoint = extensions.DistributionPoint
# DuplicateExtension = extensions.DuplicateExtension
# ExtendedKeyUsage = extensions.ExtendedKeyUsage
# Extension = extensions.Extension
# ExtensionNotFound = extensions.ExtensionNotFound
# ExtensionType = extensions.ExtensionType
# Extensions = extensions.Extensions
# FreshestCRL = extensions.FreshestCRL
# GeneralNames = extensions.GeneralNames
# InhibitAnyPolicy = extensions.InhibitAnyPolicy
# InvalidityDate = extensions.InvalidityDate
# IssuerAlternativeName = extensions.IssuerAlternativeName
# IssuingDistributionPoint = extensions.IssuingDistributionPoint
# KeyUsage = extensions.KeyUsage
# NameConstraints = extensions.NameConstraints
# NoticeReference = extensions.NoticeReference
# OCSPNoCheck = extensions.OCSPNoCheck
# OCSPNonce = extensions.OCSPNonce
# PolicyConstraints = extensions.PolicyConstraints
# PolicyInformation = extensions.PolicyInformation
# PrecertPoison = extensions.PrecertPoison
# PrecertificateSignedCertificateTimestamps = extensions.PrecertificateSignedCertificateTimestamps
# ReasonFlags = extensions.ReasonFlags
# SignedCertificateTimestamps = extensions.SignedCertificateTimestamps
# SubjectAlternativeName = extensions.SubjectAlternativeName
# SubjectInformationAccess = extensions.SubjectInformationAccess
# SubjectKeyIdentifier = extensions.SubjectKeyIdentifier
# TLSFeature = extensions.TLSFeature
# TLSFeatureType = extensions.TLSFeatureType
# UnrecognizedExtension = extensions.UnrecognizedExtension
# UserNotice = extensions.UserNotice


# general name
general_name = _general_name

# DNSName = general_name.DNSName
# DirectoryName = general_name.DirectoryName
# GeneralName = general_name.GeneralName
# IPAddress = general_name.IPAddress
# OtherName = general_name.OtherName
# RFC822Name = general_name.RFC822Name
# RegisteredID = general_name.RegisteredID
# UniformResourceIdentifier = general_name.UniformResourceIdentifier
# UnsupportedGeneralNameType = general_name.UnsupportedGeneralNameType
# _GENERAL_NAMES = general_name._GENERAL_NAMES


# name
name = _name

# Name = name.Name
# NameAttribute = name.NameAttribute
# RelativeDistinguishedName = name.RelativeDistinguishedName


# oid
oid = _oid

AuthorityInformationAccessOID = oid.AuthorityInformationAccessOID
CRLEntryExtensionOID = oid.CRLEntryExtensionOID
CertificatePoliciesOID = oid.CertificatePoliciesOID
ExtendedKeyUsageOID = oid.ExtendedKeyUsageOID
ExtensionOID = oid.ExtensionOID
NameOID = oid.NameOID
ObjectIdentifier = oid.ObjectIdentifier
SignatureAlgorithmOID = oid.SignatureAlgorithmOID
_SIG_OIDS_TO_HASH = oid._SIG_OIDS_TO_HASH

OID_AUTHORITY_INFORMATION_ACCESS = ExtensionOID.AUTHORITY_INFORMATION_ACCESS
OID_AUTHORITY_KEY_IDENTIFIER = ExtensionOID.AUTHORITY_KEY_IDENTIFIER
OID_BASIC_CONSTRAINTS = ExtensionOID.BASIC_CONSTRAINTS
OID_CERTIFICATE_POLICIES = ExtensionOID.CERTIFICATE_POLICIES
OID_CRL_DISTRIBUTION_POINTS = ExtensionOID.CRL_DISTRIBUTION_POINTS
OID_EXTENDED_KEY_USAGE = ExtensionOID.EXTENDED_KEY_USAGE
OID_FRESHEST_CRL = ExtensionOID.FRESHEST_CRL
OID_INHIBIT_ANY_POLICY = ExtensionOID.INHIBIT_ANY_POLICY
OID_ISSUER_ALTERNATIVE_NAME = ExtensionOID.ISSUER_ALTERNATIVE_NAME
OID_KEY_USAGE = ExtensionOID.KEY_USAGE
OID_NAME_CONSTRAINTS = ExtensionOID.NAME_CONSTRAINTS
OID_OCSP_NO_CHECK = ExtensionOID.OCSP_NO_CHECK
OID_POLICY_CONSTRAINTS = ExtensionOID.POLICY_CONSTRAINTS
OID_POLICY_MAPPINGS = ExtensionOID.POLICY_MAPPINGS
OID_SUBJECT_ALTERNATIVE_NAME = ExtensionOID.SUBJECT_ALTERNATIVE_NAME
OID_SUBJECT_DIRECTORY_ATTRIBUTES = ExtensionOID.SUBJECT_DIRECTORY_ATTRIBUTES
OID_SUBJECT_INFORMATION_ACCESS = ExtensionOID.SUBJECT_INFORMATION_ACCESS
OID_SUBJECT_KEY_IDENTIFIER = ExtensionOID.SUBJECT_KEY_IDENTIFIER

OID_DSA_WITH_SHA1 = SignatureAlgorithmOID.DSA_WITH_SHA1
OID_DSA_WITH_SHA224 = SignatureAlgorithmOID.DSA_WITH_SHA224
OID_DSA_WITH_SHA256 = SignatureAlgorithmOID.DSA_WITH_SHA256
OID_ECDSA_WITH_SHA1 = SignatureAlgorithmOID.ECDSA_WITH_SHA1
OID_ECDSA_WITH_SHA224 = SignatureAlgorithmOID.ECDSA_WITH_SHA224
OID_ECDSA_WITH_SHA256 = SignatureAlgorithmOID.ECDSA_WITH_SHA256
OID_ECDSA_WITH_SHA384 = SignatureAlgorithmOID.ECDSA_WITH_SHA384
OID_ECDSA_WITH_SHA512 = SignatureAlgorithmOID.ECDSA_WITH_SHA512
OID_RSA_WITH_MD5 = SignatureAlgorithmOID.RSA_WITH_MD5
OID_RSA_WITH_SHA1 = SignatureAlgorithmOID.RSA_WITH_SHA1
OID_RSA_WITH_SHA224 = SignatureAlgorithmOID.RSA_WITH_SHA224
OID_RSA_WITH_SHA256 = SignatureAlgorithmOID.RSA_WITH_SHA256
OID_RSA_WITH_SHA384 = SignatureAlgorithmOID.RSA_WITH_SHA384
OID_RSA_WITH_SHA512 = SignatureAlgorithmOID.RSA_WITH_SHA512
OID_RSASSA_PSS = SignatureAlgorithmOID.RSASSA_PSS

OID_COMMON_NAME = NameOID.COMMON_NAME
OID_COUNTRY_NAME = NameOID.COUNTRY_NAME
OID_DOMAIN_COMPONENT = NameOID.DOMAIN_COMPONENT
OID_DN_QUALIFIER = NameOID.DN_QUALIFIER
OID_EMAIL_ADDRESS = NameOID.EMAIL_ADDRESS
OID_GENERATION_QUALIFIER = NameOID.GENERATION_QUALIFIER
OID_GIVEN_NAME = NameOID.GIVEN_NAME
OID_LOCALITY_NAME = NameOID.LOCALITY_NAME
OID_ORGANIZATIONAL_UNIT_NAME = NameOID.ORGANIZATIONAL_UNIT_NAME
OID_ORGANIZATION_NAME = NameOID.ORGANIZATION_NAME
OID_PSEUDONYM = NameOID.PSEUDONYM
OID_SERIAL_NUMBER = NameOID.SERIAL_NUMBER
OID_STATE_OR_PROVINCE_NAME = NameOID.STATE_OR_PROVINCE_NAME
OID_SURNAME = NameOID.SURNAME
OID_TITLE = NameOID.TITLE

OID_CLIENT_AUTH = ExtendedKeyUsageOID.CLIENT_AUTH
OID_CODE_SIGNING = ExtendedKeyUsageOID.CODE_SIGNING
OID_EMAIL_PROTECTION = ExtendedKeyUsageOID.EMAIL_PROTECTION
OID_OCSP_SIGNING = ExtendedKeyUsageOID.OCSP_SIGNING
OID_SERVER_AUTH = ExtendedKeyUsageOID.SERVER_AUTH
OID_TIME_STAMPING = ExtendedKeyUsageOID.TIME_STAMPING

OID_ANY_POLICY = CertificatePoliciesOID.ANY_POLICY
OID_CPS_QUALIFIER = CertificatePoliciesOID.CPS_QUALIFIER
OID_CPS_USER_NOTICE = CertificatePoliciesOID.CPS_USER_NOTICE

OID_CERTIFICATE_ISSUER = CRLEntryExtensionOID.CERTIFICATE_ISSUER
OID_CRL_REASON = CRLEntryExtensionOID.CRL_REASON
OID_INVALIDITY_DATE = CRLEntryExtensionOID.INVALIDITY_DATE

OID_CA_ISSUERS = AuthorityInformationAccessOID.CA_ISSUERS
OID_OCSP = AuthorityInformationAccessOID.OCSP


x509 = larky.struct(
    __name__='x509',
    certificate_transparency=certificate_transparency,
    base=base,
    Version=Version,
    load_pem_x509_certificate=load_pem_x509_certificate,
    load_der_x509_certificate=load_der_x509_certificate,
    load_pem_x509_csr=load_pem_x509_csr,
    load_der_x509_csr=load_der_x509_csr,
    load_pem_x509_crl=load_pem_x509_crl,
    load_der_x509_crl=load_der_x509_crl,
    extensions=extensions,
    general_name=general_name,
    name=name,
    oid=oid,
    AuthorityInformationAccessOID=AuthorityInformationAccessOID,
    CRLEntryExtensionOID=CRLEntryExtensionOID,
    CertificatePoliciesOID=CertificatePoliciesOID,
    ExtendedKeyUsageOID=ExtendedKeyUsageOID,
    ExtensionOID=ExtensionOID,
    NameOID=NameOID,
    ObjectIdentifier=ObjectIdentifier,
    SignatureAlgorithmOID=SignatureAlgorithmOID,
    _SIG_OIDS_TO_HASH=_SIG_OIDS_TO_HASH,
    OID_AUTHORITY_INFORMATION_ACCESS=OID_AUTHORITY_INFORMATION_ACCESS,
    OID_AUTHORITY_KEY_IDENTIFIER=OID_AUTHORITY_KEY_IDENTIFIER,
    OID_BASIC_CONSTRAINTS=OID_BASIC_CONSTRAINTS,
    OID_CERTIFICATE_POLICIES=OID_CERTIFICATE_POLICIES,
    OID_CRL_DISTRIBUTION_POINTS=OID_CRL_DISTRIBUTION_POINTS,
    OID_EXTENDED_KEY_USAGE=OID_EXTENDED_KEY_USAGE,
    OID_FRESHEST_CRL=OID_FRESHEST_CRL,
    OID_INHIBIT_ANY_POLICY=OID_INHIBIT_ANY_POLICY,
    OID_ISSUER_ALTERNATIVE_NAME=OID_ISSUER_ALTERNATIVE_NAME,
    OID_KEY_USAGE=OID_KEY_USAGE,
    OID_NAME_CONSTRAINTS=OID_NAME_CONSTRAINTS,
    OID_OCSP_NO_CHECK=OID_OCSP_NO_CHECK,
    OID_POLICY_CONSTRAINTS=OID_POLICY_CONSTRAINTS,
    OID_POLICY_MAPPINGS=OID_POLICY_MAPPINGS,
    OID_SUBJECT_ALTERNATIVE_NAME=OID_SUBJECT_ALTERNATIVE_NAME,
    OID_SUBJECT_DIRECTORY_ATTRIBUTES=OID_SUBJECT_DIRECTORY_ATTRIBUTES,
    OID_SUBJECT_INFORMATION_ACCESS=OID_SUBJECT_INFORMATION_ACCESS,
    OID_SUBJECT_KEY_IDENTIFIER=OID_SUBJECT_KEY_IDENTIFIER,
    OID_DSA_WITH_SHA1=OID_DSA_WITH_SHA1,
    OID_DSA_WITH_SHA224=OID_DSA_WITH_SHA224,
    OID_DSA_WITH_SHA256=OID_DSA_WITH_SHA256,
    OID_ECDSA_WITH_SHA1=OID_ECDSA_WITH_SHA1,
    OID_ECDSA_WITH_SHA224=OID_ECDSA_WITH_SHA224,
    OID_ECDSA_WITH_SHA256=OID_ECDSA_WITH_SHA256,
    OID_ECDSA_WITH_SHA384=OID_ECDSA_WITH_SHA384,
    OID_ECDSA_WITH_SHA512=OID_ECDSA_WITH_SHA512,
    OID_RSA_WITH_MD5=OID_RSA_WITH_MD5,
    OID_RSA_WITH_SHA1=OID_RSA_WITH_SHA1,
    OID_RSA_WITH_SHA224=OID_RSA_WITH_SHA224,
    OID_RSA_WITH_SHA256=OID_RSA_WITH_SHA256,
    OID_RSA_WITH_SHA384=OID_RSA_WITH_SHA384,
    OID_RSA_WITH_SHA512=OID_RSA_WITH_SHA512,
    OID_RSASSA_PSS=OID_RSASSA_PSS,
    OID_COMMON_NAME=OID_COMMON_NAME,
    OID_COUNTRY_NAME=OID_COUNTRY_NAME,
    OID_DOMAIN_COMPONENT=OID_DOMAIN_COMPONENT,
    OID_DN_QUALIFIER=OID_DN_QUALIFIER,
    OID_EMAIL_ADDRESS=OID_EMAIL_ADDRESS,
    OID_GENERATION_QUALIFIER=OID_GENERATION_QUALIFIER,
    OID_GIVEN_NAME=OID_GIVEN_NAME,
    OID_LOCALITY_NAME=OID_LOCALITY_NAME,
    OID_ORGANIZATIONAL_UNIT_NAME=OID_ORGANIZATIONAL_UNIT_NAME,
    OID_ORGANIZATION_NAME=OID_ORGANIZATION_NAME,
    OID_PSEUDONYM=OID_PSEUDONYM,
    OID_SERIAL_NUMBER=OID_SERIAL_NUMBER,
    OID_STATE_OR_PROVINCE_NAME=OID_STATE_OR_PROVINCE_NAME,
    OID_SURNAME=OID_SURNAME,
    OID_TITLE=OID_TITLE,
    OID_CLIENT_AUTH=OID_CLIENT_AUTH,
    OID_CODE_SIGNING=OID_CODE_SIGNING,
    OID_EMAIL_PROTECTION=OID_EMAIL_PROTECTION,
    OID_OCSP_SIGNING=OID_OCSP_SIGNING,
    OID_SERVER_AUTH=OID_SERVER_AUTH,
    OID_TIME_STAMPING=OID_TIME_STAMPING,
    OID_ANY_POLICY=OID_ANY_POLICY,
    OID_CPS_QUALIFIER=OID_CPS_QUALIFIER,
    OID_CPS_USER_NOTICE=OID_CPS_USER_NOTICE,
    OID_CERTIFICATE_ISSUER=OID_CERTIFICATE_ISSUER,
    OID_CRL_REASON=OID_CRL_REASON,
    OID_INVALIDITY_DATE=OID_INVALIDITY_DATE,
    OID_CA_ISSUERS=OID_CA_ISSUERS,
    OID_OCSP=OID_OCSP,
)
