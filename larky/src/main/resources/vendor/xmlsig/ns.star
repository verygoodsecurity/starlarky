load("@stdlib//larky", larky="larky")

DSigNs = "http://www.w3.org/2000/09/xmldsig#"
DSigNs11 = "http://www.w3.org/2009/xmldsig11#"
DSignNsMore = "http://www.w3.org/2001/04/xmldsig-more#"
EncNs = "http://www.w3.org/2001/04/xmlenc#"
XPathNs = "http://www.w3.org/TR/1999/REC-xpath-19991116"
XPath2Ns = "http://www.w3.org/2002/06/xmldsig-filter2"
XPointerNs = "http://www.w3.org/2001/04/xmldsig-more/xptr"
Soap11Ns = "http://schemas.xmlsoap.org/soap/envelope/"
Soap12Ns = "http://www.w3.org/2002/06/soap-envelope"
NsExcC14N = "http://www.w3.org/2001/10/xml-exc-c14n#"
NsExcC14NWithComments = "http://www.w3.org/2001/10/xml-exc-c14n#WithComments"
NS_MAP = {"ds": DSigNs, "ds11": DSigNs11}


# extensions from:
#
# - python-xmlsec https://github.com/mehcode/python-xmlsec/blob/master/src/xmlsec/constants.pyi
# - python-zeep phttps://github.com/mvantellingen/python-zeep/blob/master/src/zeep/ns.py

DS = DSigNs
HTTP = "http://schemas.xmlsoap.org/wsdl/http/"
MIME = "http://schemas.xmlsoap.org/wsdl/mime/"
SOAP_11 = "http://schemas.xmlsoap.org/wsdl/soap/"
SOAP_12 = "http://schemas.xmlsoap.org/wsdl/soap12/"
SOAP_ENV_11 = Soap11Ns
SOAP_ENV_12 = Soap12Ns
WSA = "http://www.w3.org/2005/08/addressing"
WSDL = "http://schemas.xmlsoap.org/wsdl/"
XSD = "http://www.w3.org/2001/XMLSchema"
XSI = "http://www.w3.org/2001/XMLSchema-instance"
WSSE="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
WSU="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"

NS_MAP["xsd"] = XSD

NAMESPACE_TO_PREFIX = {
    XSD: "xsd",
    DSigNs: "ds",
    DSigNs11: "ds11"
}


ns = larky.struct(
    __name__='ns',
    DSigNs=DSigNs,
    DSigNs11=DSigNs11,
    DSignNsMore=DSignNsMore,
    EncNs=EncNs,
    XPathNs=XPathNs,
    XPath2Ns=XPath2Ns,
    XPointerNs=XPointerNs,
    Soap11Ns=Soap11Ns,
    Soap12Ns=Soap12Ns,
    NsExcC14N=NsExcC14N,
    NsExcC14NWithComments=NsExcC14NWithComments,
    NS_MAP=NS_MAP,
    DS=DS,
    HTTP=HTTP,
    MIME=MIME,
    SOAP_11=SOAP_11,
    SOAP_12=SOAP_12,
    SOAP_ENV_11=SOAP_ENV_11,
    SOAP_ENV_12=SOAP_ENV_12,
    WSA=WSA,
    WSDL=WSDL,
    XSD=XSD,
    XSI=XSI,
    WSSE=WSSE,
    WSU=WSU,
    NAMESPACE_TO_PREFIX=NAMESPACE_TO_PREFIX,
)
