load("@stdlib//larky", larky="larky")
load("@stdlib//collections", namedtuple="namedtuple")
load("@stdlib//enum", enum="enum")

load("@vendor//cryptography/hazmat/primitives", hashes="hashes")
load("@vendor//xmlsig/algorithms", HMACAlgorithm="HMACAlgorithm", RSAAlgorithm="RSAAlgorithm")
load("@vendor//xmlsig/ns", _ns="ns", NS_MAP="NS_MAP")  # noqa:F401
load("@vendor//xmlsig/ns",
     DSignNsMore="DSignNsMore",
     DSigNs="DSigNs",
     DSigNs11="DSigNs11",
     EncNs="EncNs",
     )


ID_ATTR = "Id"

TransformInclC14N = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315"
TransformInclC14NWithComments = TransformInclC14N + "#WithComments"
TransformInclC14N11 = ""
TransformInclC14N11WithComments = ""
TransformExclC14N = "http://www.w3.org/2001/10/xml-exc-c14n#"
TransformExclC14NWithComments = TransformExclC14N + "WithComments"
TransformEnveloped = DSigNs + "enveloped-signature"
TransformXPath = "http://www.w3.org/TR/1999/REC-xpath-19991116"
TransformXPath2 = ""
TransformXPointer = ""
TransformXslt = "http://www.w3.org/TR/1999/REC-xslt-19991116"
TransformRemoveXmlTagsC14N = ""
TransformBase64 = DSigNs + "base64"
TransformVisa3DHack = ""
TransformAes128Cbc = ""
TransformAes192Cbc = ""
TransformAes256Cbc = ""
TransformKWAes128 = ""
TransformKWAes192 = ""
TransformKWAes256 = ""
TransformDes3Cbc = ""
TransformKWDes3 = ""
TransformDsaSha1 = DSigNs + "dsa-sha1"
TransformDsaSha256 = DSigNs11 + "dsa-sha256"
TransformEcdsaSha1 = DSignNsMore + "ecdsa-sha1"
TransformEcdsaSha224 = DSignNsMore + "ecdsa-sha224"
TransformEcdsaSha256 = DSignNsMore + "ecdsa-sha256"
TransformEcdsaSha384 = DSignNsMore + "ecdsa-sha384"
TransformEcdsaSha512 = DSignNsMore + "ecdsa-sha512"
TransformHmacRipemd160 = DSignNsMore + "hmac-ripemd160"
TransformHmacSha1 = DSigNs + "hmac-sha1"
TransformHmacSha224 = DSignNsMore + "hmac-sha224"
TransformHmacSha256 = DSignNsMore + "hmac-sha256"
TransformHmacSha384 = DSignNsMore + "hmac-sha384"
TransformHmacSha512 = DSignNsMore + "hmac-sha512"
TransformRsaMd5 = DSignNsMore + "rsa-md5"
TransformRsaRipemd160 = DSignNsMore + "rsa-ripemd160"
TransformRsaSha1 = DSigNs + "rsa-sha1"
TransformRsaSha224 = DSignNsMore + "rsa-sha224"
TransformRsaSha256 = DSignNsMore + "rsa-sha256"
TransformRsaSha384 = DSignNsMore + "rsa-sha384"
TransformRsaSha512 = DSignNsMore + "rsa-sha512"
TransformRsaPkcs1 = ""
TransformRsaOaep = ""
TransformMd5 = DSignNsMore + "md5"
TransformRipemd160 = EncNs + "ripemd160"
TransformSha1 = DSigNs + "sha1"
TransformSha224 = DSignNsMore + "sha224"
TransformSha256 = EncNs + "sha256"
TransformSha384 = DSignNsMore + "sha384"
TransformSha512 = EncNs + "sha512"

TransformUsageUnknown = {}
TransformUsageDSigTransform = [TransformEnveloped, TransformBase64]
TransformUsageC14NMethod = {
    TransformInclC14N: {"method": "c14n", "exclusive": False, "comments": False},
    TransformInclC14NWithComments: {
        "method": "c14n",
        "exclusive": False,
        "comments": True,
    },
    TransformExclC14N: {"method": "c14n", "exclusive": True, "comments": False},
    TransformExclC14NWithComments: {
        "method": "c14n",
        "exclusive": True,
        "comments": False,
    },
}

TransformUsageDSigTransform.extend(TransformUsageC14NMethod.keys())

TransformUsageDigestMethod = {
    TransformMd5: "md5",
    TransformSha1: "sha1",
    TransformSha224: "sha224",
    TransformSha256: "sha256",
    TransformSha384: "sha384",
    TransformSha512: "sha512",
    TransformRipemd160: "ripemd160",  # TODO(mahmoudimus): we do not support this atm
}

TransformUsageSignatureMethod = {
    TransformRsaMd5: {"digest": hashes.MD5, "method": RSAAlgorithm},
    TransformRsaSha1: {"digest": hashes.SHA1, "method": RSAAlgorithm},
    TransformRsaSha224: {"digest": hashes.SHA224, "method": RSAAlgorithm},
    TransformRsaSha256: {"digest": hashes.SHA256, "method": RSAAlgorithm},
    TransformRsaSha384: {"digest": hashes.SHA384, "method": RSAAlgorithm},
    TransformRsaSha512: {"digest": hashes.SHA512, "method": RSAAlgorithm},
    TransformHmacSha1: {"digest": hashes.SHA1, "method": HMACAlgorithm},
    TransformHmacSha224: {"digest": hashes.SHA224, "method": HMACAlgorithm},
    TransformHmacSha256: {"digest": hashes.SHA256, "method": HMACAlgorithm},
    TransformHmacSha384: {"digest": hashes.SHA384, "method": HMACAlgorithm},
    TransformHmacSha512: {"digest": hashes.SHA512, "method": HMACAlgorithm},
}

TransformUsageEncryptionMethod = {}
TransformUsageAny = {}

# Extensions below are from:
#
# https://github.com/mehcode/python-xmlsec/blob/master/src/xmlsec/constants.pyi
KeyData = namedtuple('__KeyData', "name href")
Transform = namedtuple('__Transform', "name href usage")

_TransformUsageUnknown = 0
_TransformUsageDSigTransform = 1
_TransformUsageC14NMethod = 2
_TransformUsageDigestMethod = 4
_TransformUsageSignatureMethod = 8
_TransformUsageEncryptionMethod = 16
_TransformUsageAny = 65535

TransformUsage = enum.Enum('TransformUsage', [
    ("Unknown", _TransformUsageUnknown),
    ("DSigTransform", _TransformUsageDSigTransform),
    ("C14NMethod", _TransformUsageC14NMethod),
    ("DigestMethod", _TransformUsageDigestMethod),
    ("SignatureMethod", _TransformUsageSignatureMethod),
    ("EncryptionMethod", _TransformUsageEncryptionMethod),
    ("Any", _TransformUsageAny),
])

_KeyDataTypeNone = 0
_KeyDataTypeUnknown = 0
_KeyDataTypePublic = 1
_KeyDataTypePrivate = 2
_KeyDataTypeSymmetric = 4
_KeyDataTypeSession = 8
_KeyDataTypePermanent = 16
_KeyDataTypeTrusted = 256
_KeyDataTypeAny = 65535

KeyType = enum.Enum('KeyType', [
    ("NONE", _KeyDataTypeNone),
    ("UNKNOWN", _KeyDataTypeUnknown),
    ("PUBLIC", _KeyDataTypePublic),
    ("PRIVATE", _KeyDataTypePrivate),
    ("SYMMETRIC", _KeyDataTypeSymmetric),
    ("SESSION", _KeyDataTypeSession),
    ("PERMANENT", _KeyDataTypePermanent),
    ("TRUSTED", _KeyDataTypeTrusted),
    ("ANY", _KeyDataTypeAny),
])

_KeyDataFormatUnknown = 0
_KeyDataFormatBinary = 1
_KeyDataFormatPem = 2
_KeyDataFormatDer = 3
_KeyDataFormatPkcs8Pem = 4
_KeyDataFormatPkcs8Der = 5
_KeyDataFormatPkcs12 = 6
_KeyDataFormatCertPem = 7
_KeyDataFormatCertDer = 8

KeyFormat = enum.Enum('KeyFormat', [
    ("UNKNOWN", _KeyDataFormatUnknown),
    ("BINARY", _KeyDataFormatBinary),
    ("PEM", _KeyDataFormatPem),
    ("DER", _KeyDataFormatDer),
    ("PKCS8_PEM", _KeyDataFormatPkcs8Pem),
    ("PKCS8_DER", _KeyDataFormatPkcs8Der),
    ("PKCS12_PEM", _KeyDataFormatPkcs12),
    ("CERT_PEM", _KeyDataFormatCertPem),
    ("CERT_DER", _KeyDataFormatCertDer),
])

KEYDATA_ALGO2HREF = {}
KEYDATA_HREF2ALGO = {}


# noinspection PyPep8Naming
def __KeyData(name, href):
    KEYDATA_ALGO2HREF[href] = name
    KEYDATA_ALGO2HREF[name] = href
    return KeyData(name, href)


TRANSFORM_VIA_HREF = {}
TRANSFORM_VIA_USAGE = {}
TRANSFORM_VIA_NAME = {}


# noinspection PyPep8Naming
def __Transform(name, href, usage):
    _transform = Transform(name, href, usage)
    TRANSFORM_VIA_HREF.setdefault(href, []).append(_transform)
    TRANSFORM_VIA_USAGE.setdefault(usage, []).append(_transform)
    TRANSFORM_VIA_NAME.setdefault(name, []).append(_transform)
    return _transform


__KeyData('aes', 'http://www.aleksey.com/xmlsec/2002#AESKeyValue')
__KeyData('des', 'http://www.aleksey.com/xmlsec/2002#DESKeyValue')
__KeyData('dsa', 'http://www.w3.org/2000/09/xmldsig#DSAKeyValue')
__KeyData('ecdsa', 'http://scap.nist.gov/specifications/tmsad/#resource-1.0')
__KeyData('enc-key', 'http://www.w3.org/2001/04/xmlenc#EncryptedKey')
__KeyData('hmac', 'http://www.aleksey.com/xmlsec/2002#HMACKeyValue')
__KeyData('raw-x509-cert', 'http://www.w3.org/2000/09/xmldsig#rawX509Certificate')
__KeyData('retrieval-method', None)
__KeyData('rsa', 'http://www.w3.org/2000/09/xmldsig#RSAKeyValue')
__KeyData('x509', 'http://www.w3.org/2000/09/xmldsig#X509Data')

NodeCanonicalizationMethod = 'CanonicalizationMethod'
NodeCipherData = 'CipherData'
NodeCipherReference = 'CipherReference'
NodeCipherValue = 'CipherValue'
NodeDataReference = 'DataReference'
NodeDigestMethod = 'DigestMethod'
NodeDigestValue = 'DigestValue'
NodeEncryptedData = 'EncryptedData'
NodeEncryptedKey = 'EncryptedKey'
NodeEncryptionMethod = 'EncryptionMethod'
NodeEncryptionProperties = 'EncryptionProperties'
NodeEncryptionProperty = 'EncryptionProperty'
NodeKeyInfo = 'KeyInfo'
NodeKeyName = 'KeyName'
NodeKeyReference = 'KeyReference'
NodeKeyValue = 'KeyValue'
NodeManifest = 'Manifest'
NodeObject = 'Object'
NodeReference = 'Reference'
NodeReferenceList = 'ReferenceList'
NodeSignature = 'Signature'
NodeSignatureMethod = 'SignatureMethod'
NodeSignatureProperties = 'SignatureProperties'
NodeSignatureValue = 'SignatureValue'
NodeSignedInfo = 'SignedInfo'
NodeX509Data = 'X509Data'

Ns = 'http://www.aleksey.com/xmlsec/2002'
XPathNs = _ns.XPathNs
XPath2Ns = _ns.XPath2Ns
XPointerNs = _ns.XPointerNs
NsExcC14N = _ns.NsExcC14N
NsExcC14NWithComments = _ns.NsExcC14NWithComments
Soap11Ns = _ns.Soap11Ns
Soap12Ns = _ns.Soap12Ns


__Transform('aes128-cbc', 'http://www.w3.org/2001/04/xmlenc#aes128-cbc', TransformUsage.EncryptionMethod)
__Transform('aes128-gcm', 'http://www.w3.org/2009/xmlenc11#aes128-gcm', TransformUsage.EncryptionMethod)
__Transform('aes192-cbc', 'http://www.w3.org/2001/04/xmlenc#aes192-cbc', TransformUsage.EncryptionMethod)
__Transform('aes192-gcm', 'http://www.w3.org/2009/xmlenc11#aes192-gcm', TransformUsage.EncryptionMethod)
__Transform('aes256-cbc', 'http://www.w3.org/2001/04/xmlenc#aes256-cbc', TransformUsage.EncryptionMethod)
__Transform('aes256-gcm', 'http://www.w3.org/2009/xmlenc11#aes256-gcm', TransformUsage.EncryptionMethod)
__Transform('tripledes-cbc', 'http://www.w3.org/2001/04/xmlenc#tripledes-cbc', TransformUsage.EncryptionMethod)
__Transform('dsa-sha1', 'http://www.w3.org/2000/09/xmldsig#dsa-sha1', TransformUsage.SignatureMethod)
__Transform('ecdsa-sha1', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1', TransformUsage.SignatureMethod)
__Transform('ecdsa-sha224', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha224', TransformUsage.SignatureMethod)
__Transform('ecdsa-sha256', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256', TransformUsage.SignatureMethod)
__Transform('ecdsa-sha384', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384', TransformUsage.SignatureMethod)
__Transform('ecdsa-sha512', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512', TransformUsage.SignatureMethod)
__Transform('enveloped-signature', 'http://www.w3.org/2000/09/xmldsig#enveloped-signature', TransformUsage.DSigTransform)
__Transform('hmac-md5', 'http://www.w3.org/2001/04/xmldsig-more#hmac-md5', TransformUsage.SignatureMethod)
__Transform('hmac-ripemd160', 'http://www.w3.org/2001/04/xmldsig-more#hmac-ripemd160', TransformUsage.SignatureMethod)
__Transform('hmac-sha1', 'http://www.w3.org/2000/09/xmldsig#hmac-sha1', TransformUsage.SignatureMethod)
__Transform('hmac-sha224', 'http://www.w3.org/2001/04/xmldsig-more#hmac-sha224', TransformUsage.SignatureMethod)
__Transform('hmac-sha256', 'http://www.w3.org/2001/04/xmldsig-more#hmac-sha256', TransformUsage.SignatureMethod)
__Transform('hmac-sha384', 'http://www.w3.org/2001/04/xmldsig-more#hmac-sha384', TransformUsage.SignatureMethod)
__Transform('hmac-sha512', 'http://www.w3.org/2001/04/xmldsig-more#hmac-sha512', TransformUsage.SignatureMethod)

__Transform('exc-c14n', 'http://www.w3.org/2001/10/xml-exc-c14n#', 3)
__Transform('exc-c14n-with-comments', 'http://www.w3.org/2001/10/xml-exc-c14n#WithComments', 3)
__Transform('c14n', 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315', 3)
__Transform('c14n11', 'http://www.w3.org/2006/12/xml-c14n11', 3)
__Transform('c14n11-with-comments', 'http://www.w3.org/2006/12/xml-c14n11#WithComments', 3)
__Transform('c14n-with-comments', 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments', 3)
__Transform('remove-xml-tags-transform', None, 3)

__Transform('kw-aes128', 'http://www.w3.org/2001/04/xmlenc#kw-aes128', TransformUsage.EncryptionMethod)
__Transform('kw-aes192', 'http://www.w3.org/2001/04/xmlenc#kw-aes192', TransformUsage.EncryptionMethod)
__Transform('kw-aes256', 'http://www.w3.org/2001/04/xmlenc#kw-aes256', TransformUsage.EncryptionMethod)
__Transform('kw-tripledes', 'http://www.w3.org/2001/04/xmlenc#kw-tripledes', TransformUsage.EncryptionMethod)
__Transform('md5', 'http://www.w3.org/2001/04/xmldsig-more#md5', TransformUsage.DigestMethod)
__Transform('ripemd160', 'http://www.w3.org/2001/04/xmlenc#ripemd160', TransformUsage.DigestMethod)
__Transform('rsa-md5', 'http://www.w3.org/2001/04/xmldsig-more#rsa-md5', TransformUsage.SignatureMethod)
__Transform('rsa-oaep-mgf1p', 'http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p', TransformUsage.EncryptionMethod)
__Transform('rsa-1_5', 'http://www.w3.org/2001/04/xmlenc#rsa-1_5', TransformUsage.EncryptionMethod)
__Transform('rsa-ripemd160', 'http://www.w3.org/2001/04/xmldsig-more#rsa-ripemd160', TransformUsage.SignatureMethod)
__Transform('rsa-sha1', 'http://www.w3.org/2000/09/xmldsig#rsa-sha1', TransformUsage.SignatureMethod)
__Transform('rsa-sha224', 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha224', TransformUsage.SignatureMethod)
__Transform('rsa-sha256', 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256', TransformUsage.SignatureMethod)
__Transform('rsa-sha384', 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha384', TransformUsage.SignatureMethod)
__Transform('rsa-sha512', 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha512', TransformUsage.SignatureMethod)
__Transform('sha1', 'http://www.w3.org/2000/09/xmldsig#sha1', TransformUsage.DigestMethod)
__Transform('sha224', 'http://www.w3.org/2001/04/xmldsig-more#sha224', TransformUsage.DigestMethod)
__Transform('sha256', 'http://www.w3.org/2001/04/xmlenc#sha256', TransformUsage.DigestMethod)
__Transform('sha384', 'http://www.w3.org/2001/04/xmldsig-more#sha384', TransformUsage.DigestMethod)
__Transform('sha512', 'http://www.w3.org/2001/04/xmlenc#sha512', TransformUsage.DigestMethod)

__Transform('Visa3DHackTransform', None, TransformUsage.DSigTransform)
__Transform('xpath', 'http://www.w3.org/TR/1999/REC-xpath-19991116', TransformUsage.DSigTransform)
__Transform('xpath2', 'http://www.w3.org/2002/06/xmldsig-filter2', TransformUsage.DSigTransform)
__Transform('xpointer', 'http://www.w3.org/2001/04/xmldsig-more/xptr', TransformUsage.DSigTransform)
__Transform('xslt', 'http://www.w3.org/TR/1999/REC-xslt-19991116', TransformUsage.DSigTransform)


TypeEncContent = EncNs + '#Content'
TypeEncElement = EncNs + '#Element'


constants = larky.struct(
    __name__='constants',
    DSignNsMore=DSignNsMore,
    DSigNs=DSigNs,
    DSigNs11=DSigNs11,
    EncNs=EncNs,
    ID_ATTR=ID_ATTR,
    TransformInclC14N=TransformInclC14N,
    TransformInclC14NWithComments=TransformInclC14NWithComments,
    TransformInclC14N11=TransformInclC14N11,
    TransformInclC14N11WithComments=TransformInclC14N11WithComments,
    TransformExclC14N=TransformExclC14N,
    TransformExclC14NWithComments=TransformExclC14NWithComments,
    TransformEnveloped=TransformEnveloped,
    TransformXPath=TransformXPath,
    TransformXPath2=TransformXPath2,
    TransformXPointer=TransformXPointer,
    TransformXslt=TransformXslt,
    TransformRemoveXmlTagsC14N=TransformRemoveXmlTagsC14N,
    TransformBase64=TransformBase64,
    TransformVisa3DHack=TransformVisa3DHack,
    TransformAes128Cbc=TransformAes128Cbc,
    TransformAes192Cbc=TransformAes192Cbc,
    TransformAes256Cbc=TransformAes256Cbc,
    TransformKWAes128=TransformKWAes128,
    TransformKWAes192=TransformKWAes192,
    TransformKWAes256=TransformKWAes256,
    TransformDes3Cbc=TransformDes3Cbc,
    TransformKWDes3=TransformKWDes3,
    TransformDsaSha1=TransformDsaSha1,
    TransformDsaSha256=TransformDsaSha256,
    TransformEcdsaSha1=TransformEcdsaSha1,
    TransformEcdsaSha224=TransformEcdsaSha224,
    TransformEcdsaSha256=TransformEcdsaSha256,
    TransformEcdsaSha384=TransformEcdsaSha384,
    TransformEcdsaSha512=TransformEcdsaSha512,
    TransformHmacRipemd160=TransformHmacRipemd160,
    TransformHmacSha1=TransformHmacSha1,
    TransformHmacSha224=TransformHmacSha224,
    TransformHmacSha256=TransformHmacSha256,
    TransformHmacSha384=TransformHmacSha384,
    TransformHmacSha512=TransformHmacSha512,
    TransformRsaMd5=TransformRsaMd5,
    TransformRsaRipemd160=TransformRsaRipemd160,
    TransformRsaSha1=TransformRsaSha1,
    TransformRsaSha224=TransformRsaSha224,
    TransformRsaSha256=TransformRsaSha256,
    TransformRsaSha384=TransformRsaSha384,
    TransformRsaSha512=TransformRsaSha512,
    TransformRsaPkcs1=TransformRsaPkcs1,
    TransformRsaOaep=TransformRsaOaep,
    TransformMd5=TransformMd5,
    TransformRipemd160=TransformRipemd160,
    TransformSha1=TransformSha1,
    TransformSha224=TransformSha224,
    TransformSha256=TransformSha256,
    TransformSha384=TransformSha384,
    TransformSha512=TransformSha512,
    TransformUsageUnknown=TransformUsageUnknown,
    TransformUsageDSigTransform=TransformUsageDSigTransform,
    TransformUsageC14NMethod=TransformUsageC14NMethod,
    TransformUsageDigestMethod=TransformUsageDigestMethod,
    TransformUsageSignatureMethod=TransformUsageSignatureMethod,
    TransformUsageEncryptionMethod=TransformUsageEncryptionMethod,
    TransformUsageAny=TransformUsageAny,
    KeyData=KeyData,
    Transform=Transform,
    TransformUsage=TransformUsage,
    KeyType=KeyType,
    KeyFormat=KeyFormat,
    KEYDATA_ALGO2HREF=KEYDATA_ALGO2HREF,
    KEYDATA_HREF2ALGO=KEYDATA_HREF2ALGO,
    TRANSFORM_VIA_HREF=TRANSFORM_VIA_HREF,
    TRANSFORM_VIA_USAGE=TRANSFORM_VIA_USAGE,
    TRANSFORM_VIA_NAME=TRANSFORM_VIA_NAME,
    NodeCanonicalizationMethod=NodeCanonicalizationMethod,
    NodeCipherData=NodeCipherData,
    NodeCipherReference=NodeCipherReference,
    NodeCipherValue=NodeCipherValue,
    NodeDataReference=NodeDataReference,
    NodeDigestMethod=NodeDigestMethod,
    NodeDigestValue=NodeDigestValue,
    NodeEncryptedData=NodeEncryptedData,
    NodeEncryptedKey=NodeEncryptedKey,
    NodeEncryptionMethod=NodeEncryptionMethod,
    NodeEncryptionProperties=NodeEncryptionProperties,
    NodeEncryptionProperty=NodeEncryptionProperty,
    NodeKeyInfo=NodeKeyInfo,
    NodeKeyName=NodeKeyName,
    NodeKeyReference=NodeKeyReference,
    NodeKeyValue=NodeKeyValue,
    NodeManifest=NodeManifest,
    NodeObject=NodeObject,
    NodeReference=NodeReference,
    NodeReferenceList=NodeReferenceList,
    NodeSignature=NodeSignature,
    NodeSignatureMethod=NodeSignatureMethod,
    NodeSignatureProperties=NodeSignatureProperties,
    NodeSignatureValue=NodeSignatureValue,
    NodeSignedInfo=NodeSignedInfo,
    NodeX509Data=NodeX509Data,
    Ns=Ns,
    NS_MAP=NS_MAP,
    XPathNs=XPathNs,
    XPath2Ns=XPath2Ns,
    XPointerNs=XPointerNs,
    NsExcC14N=NsExcC14N,
    NsExcC14NWithComments=NsExcC14NWithComments,
    Soap11Ns=Soap11Ns,
    Soap12Ns=Soap12Ns,
    TypeEncContent=TypeEncContent,
    TypeEncElement=TypeEncElement,
)