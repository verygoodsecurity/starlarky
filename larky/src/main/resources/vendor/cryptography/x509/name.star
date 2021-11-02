# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//enum", enum="enum")
load("@stdlib//larky", larky="larky")
load("@vendor//cryptography/hazmat/backends", backends="backends")
load("@vendor//cryptography/x509/oid", NameOID="NameOID", ObjectIdentifier="ObjectIdentifier")


_ASN1Type = enum.Enum('_ASN1Type', [
    ("UTF8String", 12),
    ("NumericString", 18),
    ("PrintableString", 19),
    ("T61String", 20),
    ("IA5String", 22),
    ("UTCTime", 23),
    ("GeneralizedTime", 24),
    ("VisibleString", 26),
    ("UniversalString", 28),
    ("BMPString", 30),
])


_ASN1_TYPE_TO_ENUM = _ASN1Type._value2member_map_
_SENTINEL = larky.SENTINEL
_NAMEOID_DEFAULT_TYPE = {
    NameOID.COUNTRY_NAME: _ASN1Type.PrintableString,
    NameOID.JURISDICTION_COUNTRY_NAME: _ASN1Type.PrintableString,
    NameOID.SERIAL_NUMBER: _ASN1Type.PrintableString,
    NameOID.DN_QUALIFIER: _ASN1Type.PrintableString,
    NameOID.EMAIL_ADDRESS: _ASN1Type.IA5String,
    NameOID.DOMAIN_COMPONENT: _ASN1Type.IA5String,
}

#: Short attribute names from RFC 4514:
#: https://tools.ietf.org/html/rfc4514#page-7
_NAMEOID_TO_NAME = {
    NameOID.COMMON_NAME: "CN",
    NameOID.LOCALITY_NAME: "L",
    NameOID.STATE_OR_PROVINCE_NAME: "ST",
    NameOID.ORGANIZATION_NAME: "O",
    NameOID.ORGANIZATIONAL_UNIT_NAME: "OU",
    NameOID.COUNTRY_NAME: "C",
    NameOID.STREET_ADDRESS: "STREET",
    NameOID.DOMAIN_COMPONENT: "DC",
    NameOID.USER_ID: "UID",
    NameOID.EMAIL_ADDRESS: "E",
}


def _escape_dn_value(val):
    """Escape special characters in RFC4514 Distinguished Name value."""

    if not val:
        return ""

    # See https://tools.ietf.org/html/rfc4514#section-2.4
    val = val.replace("\\", "\\\\")
    val = val.replace('"', '\\"')
    val = val.replace("+", "\\+")
    val = val.replace(",", "\\,")
    val = val.replace(";", "\\;")
    val = val.replace("<", "\\<")
    val = val.replace(">", "\\>")
    val = val.replace("\0", "\\00")

    if val[0] in ("#", " "):
        val = "\\" + val
    if val[-1] == " ":
        val = val[:-1] + "\\ "

    return val


name = larky.struct(
    __name__='name',
    _ASN1Type=_ASN1Type,
    _ASN1_TYPE_TO_ENUM=_ASN1_TYPE_TO_ENUM,
    _SENTINEL=_SENTINEL,
    _NAMEOID_DEFAULT_TYPE=_NAMEOID_DEFAULT_TYPE,
    _NAMEOID_TO_NAME=_NAMEOID_TO_NAME,
    _escape_dn_value=_escape_dn_value,
)