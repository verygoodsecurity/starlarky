load("@stdlib//larky", larky="larky")
load("@stdlib//collections", namedtuple="namedtuple")
load("@stdlib//enum", enum="enum")

KeyData = namedtuple('__KeyData', "name href")
__KeyData = KeyData

Transform = namedtuple('__Transform', "name href usage")
__Transform = Transform

ID_ATTR = "Id"

DSigNs = 'http://www.w3.org/2000/09/xmldsig#'
EncNs = 'http://www.w3.org/2001/04/xmlenc#'


KeyDataFormatUnknown = 0
KeyDataFormatBinary = 1
KeyDataFormatPem = 2
KeyDataFormatDer = 3
KeyDataFormatPkcs8Pem = 4
KeyDataFormatPkcs8Der = 5
KeyDataFormatPkcs12 = 6
KeyDataFormatCertPem = 7
KeyDataFormatCertDer = 8

KeyDataAes = __KeyData('aes', 'http://www.aleksey.com/xmlsec/2002#AESKeyValue')
KeyDataDes = __KeyData('des', 'http://www.aleksey.com/xmlsec/2002#DESKeyValue')
KeyDataDsa = __KeyData('dsa', 'http://www.w3.org/2000/09/xmldsig#DSAKeyValue')
KeyDataEcdsa = __KeyData('ecdsa', 'http://scap.nist.gov/specifications/tmsad/#resource-1.0')
KeyDataEncryptedKey = __KeyData('enc-key', 'http://www.w3.org/2001/04/xmlenc#EncryptedKey')
KeyDataHmac = __KeyData('hmac', 'http://www.aleksey.com/xmlsec/2002#HMACKeyValue')
KeyDataName = __KeyData('key-name', None)
KeyDataRawX509Cert = __KeyData('raw-x509-cert', 'http://www.w3.org/2000/09/xmldsig#rawX509Certificate')
KeyDataRetrievalMethod = __KeyData('retrieval-method', None)
KeyDataRsa = __KeyData('rsa', 'http://www.w3.org/2000/09/xmldsig#RSAKeyValue')

KeyDataTypeNone = 0
KeyDataTypeUnknown = 0
KeyDataTypePublic = 1
KeyDataTypePrivate = 2
KeyDataTypeSymmetric = 4
KeyDataTypeSession = 8
KeyDataTypePermanent = 16
KeyDataTypeTrusted = 256
KeyDataTypeAny = 65535


KeyDataValue = __KeyData('key-value', None)
KeyDataX509 = __KeyData('x509', 'http://www.w3.org/2000/09/xmldsig#X509Data')

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
NsExcC14N = 'http://www.w3.org/2001/10/xml-exc-c14n#'
NsExcC14NWithComments = 'http://www.w3.org/2001/10/xml-exc-c14n#WithComments'
Soap11Ns = 'http://schemas.xmlsoap.org/soap/envelope/'
Soap12Ns = 'http://www.w3.org/2002/06/soap-envelope'

TransformUsageUnknown = 0
TransformUsageDSigTransform = 1
TransformUsageC14NMethod = 2
TransformUsageDigestMethod = 4
TransformUsageSignatureMethod = 8
TransformUsageEncryptionMethod = 16
TransformUsageAny = 65535

TransformUsage = enum.Enum('TransformUsage', [
    ("Unknown", TransformUsageUnknown),
    ("DSigTransform", TransformUsageDSigTransform),
    ("C14NMethod", TransformUsageC14NMethod),
    ("DigestMethod", TransformUsageDigestMethod),
    ("SignatureMethod", TransformUsageSignatureMethod),
    ("EncryptionMethod", TransformUsageEncryptionMethod),
    ("Any", TransformUsageAny),
])


TransformAes128Cbc = __Transform('aes128-cbc', 'http://www.w3.org/2001/04/xmlenc#aes128-cbc', TransformUsage.EncryptionMethod)
TransformAes128Gcm = __Transform('aes128-gcm', 'http://www.w3.org/2009/xmlenc11#aes128-gcm', TransformUsage.EncryptionMethod)
TransformAes192Cbc = __Transform('aes192-cbc', 'http://www.w3.org/2001/04/xmlenc#aes192-cbc', TransformUsage.EncryptionMethod)
TransformAes192Gcm = __Transform('aes192-gcm', 'http://www.w3.org/2009/xmlenc11#aes192-gcm', TransformUsage.EncryptionMethod)
TransformAes256Cbc = __Transform('aes256-cbc', 'http://www.w3.org/2001/04/xmlenc#aes256-cbc', TransformUsage.EncryptionMethod)
TransformAes256Gcm = __Transform('aes256-gcm', 'http://www.w3.org/2009/xmlenc11#aes256-gcm', TransformUsage.EncryptionMethod)
TransformDes3Cbc = __Transform('tripledes-cbc', 'http://www.w3.org/2001/04/xmlenc#tripledes-cbc', TransformUsage.EncryptionMethod)
TransformDsaSha1 = __Transform('dsa-sha1', 'http://www.w3.org/2000/09/xmldsig#dsa-sha1', TransformUsage.SignatureMethod)
TransformEcdsaSha1 = __Transform('ecdsa-sha1', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1', TransformUsage.SignatureMethod)
TransformEcdsaSha224 = __Transform('ecdsa-sha224', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha224', TransformUsage.SignatureMethod)
TransformEcdsaSha256 = __Transform('ecdsa-sha256', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256', TransformUsage.SignatureMethod)
TransformEcdsaSha384 = __Transform('ecdsa-sha384', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384', TransformUsage.SignatureMethod)
TransformEcdsaSha512 = __Transform('ecdsa-sha512', 'http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512', TransformUsage.SignatureMethod)
TransformEnveloped = __Transform('enveloped-signature', 'http://www.w3.org/2000/09/xmldsig#enveloped-signature', TransformUsage.DSigTransform)
TransformHmacMd5 = __Transform('hmac-md5', 'http://www.w3.org/2001/04/xmldsig-more#hmac-md5', TransformUsage.SignatureMethod)
TransformHmacRipemd160 = __Transform('hmac-ripemd160', 'http://www.w3.org/2001/04/xmldsig-more#hmac-ripemd160', TransformUsage.SignatureMethod)
TransformHmacSha1 = __Transform('hmac-sha1', 'http://www.w3.org/2000/09/xmldsig#hmac-sha1', TransformUsage.SignatureMethod)
TransformHmacSha224 = __Transform('hmac-sha224', 'http://www.w3.org/2001/04/xmldsig-more#hmac-sha224', TransformUsage.SignatureMethod)
TransformHmacSha256 = __Transform('hmac-sha256', 'http://www.w3.org/2001/04/xmldsig-more#hmac-sha256', TransformUsage.SignatureMethod)
TransformHmacSha384 = __Transform('hmac-sha384', 'http://www.w3.org/2001/04/xmldsig-more#hmac-sha384', TransformUsage.SignatureMethod)
TransformHmacSha512 = __Transform('hmac-sha512', 'http://www.w3.org/2001/04/xmldsig-more#hmac-sha512', TransformUsage.SignatureMethod)

TransformExclC14N = __Transform('exc-c14n', 'http://www.w3.org/2001/10/xml-exc-c14n#', 3)
TransformExclC14NWithComments = __Transform('exc-c14n-with-comments', 'http://www.w3.org/2001/10/xml-exc-c14n#WithComments', 3)
TransformInclC14N = __Transform('c14n', 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315', 3)
TransformInclC14N11 = __Transform('c14n11', 'http://www.w3.org/2006/12/xml-c14n11', 3)
TransformInclC14N11WithComments = __Transform('c14n11-with-comments', 'http://www.w3.org/2006/12/xml-c14n11#WithComments', 3)
TransformInclC14NWithComments = __Transform('c14n-with-comments', 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments', 3)
TransformRemoveXmlTagsC14N = __Transform('remove-xml-tags-transform', None, 3)

TransformKWAes128 = __Transform('kw-aes128', 'http://www.w3.org/2001/04/xmlenc#kw-aes128', TransformUsage.EncryptionMethod)
TransformKWAes192 = __Transform('kw-aes192', 'http://www.w3.org/2001/04/xmlenc#kw-aes192', TransformUsage.EncryptionMethod)
TransformKWAes256 = __Transform('kw-aes256', 'http://www.w3.org/2001/04/xmlenc#kw-aes256', TransformUsage.EncryptionMethod)
TransformKWDes3 = __Transform('kw-tripledes', 'http://www.w3.org/2001/04/xmlenc#kw-tripledes', TransformUsage.EncryptionMethod)
TransformMd5 = __Transform('md5', 'http://www.w3.org/2001/04/xmldsig-more#md5', TransformUsage.DigestMethod)
TransformRipemd160 = __Transform('ripemd160', 'http://www.w3.org/2001/04/xmlenc#ripemd160', TransformUsage.DigestMethod)
TransformRsaMd5 = __Transform('rsa-md5', 'http://www.w3.org/2001/04/xmldsig-more#rsa-md5', TransformUsage.SignatureMethod)
TransformRsaOaep = __Transform('rsa-oaep-mgf1p', 'http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p', TransformUsage.EncryptionMethod)
TransformRsaPkcs1 = __Transform('rsa-1_5', 'http://www.w3.org/2001/04/xmlenc#rsa-1_5', TransformUsage.EncryptionMethod)
TransformRsaRipemd160 = __Transform('rsa-ripemd160', 'http://www.w3.org/2001/04/xmldsig-more#rsa-ripemd160', TransformUsage.SignatureMethod)
TransformRsaSha1 = __Transform('rsa-sha1', 'http://www.w3.org/2000/09/xmldsig#rsa-sha1', TransformUsage.SignatureMethod)
TransformRsaSha224 = __Transform('rsa-sha224', 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha224', TransformUsage.SignatureMethod)
TransformRsaSha256 = __Transform('rsa-sha256', 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256', TransformUsage.SignatureMethod)
TransformRsaSha384 = __Transform('rsa-sha384', 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha384', TransformUsage.SignatureMethod)
TransformRsaSha512 = __Transform('rsa-sha512', 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha512', TransformUsage.SignatureMethod)
TransformSha1 = __Transform('sha1', 'http://www.w3.org/2000/09/xmldsig#sha1', TransformUsage.DigestMethod)
TransformSha224 = __Transform('sha224', 'http://www.w3.org/2001/04/xmldsig-more#sha224', TransformUsage.DigestMethod)
TransformSha256 = __Transform('sha256', 'http://www.w3.org/2001/04/xmlenc#sha256', TransformUsage.DigestMethod)
TransformSha384 = __Transform('sha384', 'http://www.w3.org/2001/04/xmldsig-more#sha384', TransformUsage.DigestMethod)
TransformSha512 = __Transform('sha512', 'http://www.w3.org/2001/04/xmlenc#sha512', TransformUsage.DigestMethod)

TransformVisa3DHack = __Transform('Visa3DHackTransform', None, TransformUsage.DSigTransform)
TransformXPath = __Transform('xpath', 'http://www.w3.org/TR/1999/REC-xpath-19991116', TransformUsage.DSigTransform)
TransformXPath2 = __Transform('xpath2', 'http://www.w3.org/2002/06/xmldsig-filter2', TransformUsage.DSigTransform)
TransformXPointer = __Transform('xpointer', 'http://www.w3.org/2001/04/xmldsig-more/xptr', TransformUsage.DSigTransform)
TransformXslt = __Transform('xslt', 'http://www.w3.org/TR/1999/REC-xslt-19991116', TransformUsage.DSigTransform)

TypeEncContent = 'http://www.w3.org/2001/04/xmlenc#Content'
TypeEncElement = 'http://www.w3.org/2001/04/xmlenc#Element'
XPath2Ns = 'http://www.w3.org/2002/06/xmldsig-filter2'
XPathNs = 'http://www.w3.org/TR/1999/REC-xpath-19991116'
XPointerNs = 'http://www.w3.org/2001/04/xmldsig-more/xptr'


constants = larky.struct(
    DSigNs=DSigNs,
    EncNs=EncNs,
    KeyDataAes=KeyDataAes,
    KeyDataDes=KeyDataDes,
    KeyDataDsa=KeyDataDsa,
    KeyDataEcdsa=KeyDataEcdsa,
    KeyDataEncryptedKey=KeyDataEncryptedKey,
    KeyDataFormatBinary=KeyDataFormatBinary,
    KeyDataFormatCertDer=KeyDataFormatCertDer,
    KeyDataFormatCertPem=KeyDataFormatCertPem,
    KeyDataFormatDer=KeyDataFormatDer,
    KeyDataFormatPem=KeyDataFormatPem,
    KeyDataFormatPkcs12=KeyDataFormatPkcs12,
    KeyDataFormatPkcs8Der=KeyDataFormatPkcs8Der,
    KeyDataFormatPkcs8Pem=KeyDataFormatPkcs8Pem,
    KeyDataFormatUnknown=KeyDataFormatUnknown,
    KeyDataHmac=KeyDataHmac,
    KeyDataName=KeyDataName,
    KeyDataRawX509Cert=KeyDataRawX509Cert,
    KeyDataRetrievalMethod=KeyDataRetrievalMethod,
    KeyDataRsa=KeyDataRsa,
    KeyDataTypeAny=KeyDataTypeAny,
    KeyDataTypeNone=KeyDataTypeNone,
    KeyDataTypePermanent=KeyDataTypePermanent,
    KeyDataTypePrivate=KeyDataTypePrivate,
    KeyDataTypePublic=KeyDataTypePublic,
    KeyDataTypeSession=KeyDataTypeSession,
    KeyDataTypeSymmetric=KeyDataTypeSymmetric,
    KeyDataTypeTrusted=KeyDataTypeTrusted,
    KeyDataTypeUnknown=KeyDataTypeUnknown,
    KeyDataValue=KeyDataValue,
    KeyDataX509=KeyDataX509,
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
    NsExcC14N=NsExcC14N,
    NsExcC14NWithComments=NsExcC14NWithComments,
    Soap11Ns=Soap11Ns,
    Soap12Ns=Soap12Ns,
    TransformAes128Cbc=TransformAes128Cbc,
    TransformAes128Gcm=TransformAes128Gcm,
    TransformAes192Cbc=TransformAes192Cbc,
    TransformAes192Gcm=TransformAes192Gcm,
    TransformAes256Cbc=TransformAes256Cbc,
    TransformAes256Gcm=TransformAes256Gcm,
    TransformDes3Cbc=TransformDes3Cbc,
    TransformDsaSha1=TransformDsaSha1,
    TransformEcdsaSha1=TransformEcdsaSha1,
    TransformEcdsaSha224=TransformEcdsaSha224,
    TransformEcdsaSha256=TransformEcdsaSha256,
    TransformEcdsaSha384=TransformEcdsaSha384,
    TransformEcdsaSha512=TransformEcdsaSha512,
    TransformEnveloped=TransformEnveloped,
    TransformExclC14N=TransformExclC14N,
    TransformExclC14NWithComments=TransformExclC14NWithComments,
    TransformHmacMd5=TransformHmacMd5,
    TransformHmacRipemd160=TransformHmacRipemd160,
    TransformHmacSha1=TransformHmacSha1,
    TransformHmacSha224=TransformHmacSha224,
    TransformHmacSha256=TransformHmacSha256,
    TransformHmacSha384=TransformHmacSha384,
    TransformHmacSha512=TransformHmacSha512,
    TransformInclC14N=TransformInclC14N,
    TransformInclC14N11=TransformInclC14N11,
    TransformInclC14N11WithComments=TransformInclC14N11WithComments,
    TransformInclC14NWithComments=TransformInclC14NWithComments,
    TransformKWAes128=TransformKWAes128,
    TransformKWAes192=TransformKWAes192,
    TransformKWAes256=TransformKWAes256,
    TransformKWDes3=TransformKWDes3,
    TransformMd5=TransformMd5,
    TransformRemoveXmlTagsC14N=TransformRemoveXmlTagsC14N,
    TransformRipemd160=TransformRipemd160,
    TransformRsaMd5=TransformRsaMd5,
    TransformRsaOaep=TransformRsaOaep,
    TransformRsaPkcs1=TransformRsaPkcs1,
    TransformRsaRipemd160=TransformRsaRipemd160,
    TransformRsaSha1=TransformRsaSha1,
    TransformRsaSha224=TransformRsaSha224,
    TransformRsaSha256=TransformRsaSha256,
    TransformRsaSha384=TransformRsaSha384,
    TransformRsaSha512=TransformRsaSha512,
    TransformSha1=TransformSha1,
    TransformSha224=TransformSha224,
    TransformSha256=TransformSha256,
    TransformSha384=TransformSha384,
    TransformSha512=TransformSha512,
    TransformUsageAny=TransformUsageAny,
    TransformUsageC14NMethod=TransformUsageC14NMethod,
    TransformUsageDSigTransform=TransformUsageDSigTransform,
    TransformUsageDigestMethod=TransformUsageDigestMethod,
    TransformUsageEncryptionMethod=TransformUsageEncryptionMethod,
    TransformUsageSignatureMethod=TransformUsageSignatureMethod,
    TransformUsageUnknown=TransformUsageUnknown,
    TransformVisa3DHack=TransformVisa3DHack,
    TransformXPath=TransformXPath,
    TransformXPath2=TransformXPath2,
    TransformXPointer=TransformXPointer,
    TransformXslt=TransformXslt,
    TypeEncContent=TypeEncContent,
    TypeEncElement=TypeEncElement,
    XPath2Ns=XPath2Ns,
    XPathNs=XPathNs,
    XPointerNs=XPointerNs,
)