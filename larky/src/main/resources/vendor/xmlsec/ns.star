load("@stdlib//larky", larky="larky")

# Move somewhere else?
ns = larky.mutablestruct(
    __name__='ns',
    DS="http://www.w3.org/2000/09/xmldsig#",
    DSigNs11 ="http://www.w3.org/2009/xmldsig11#",
    DSignNsMore = "http://www.w3.org/2001/04/xmldsig-more#",
    HTTP="http://schemas.xmlsoap.org/wsdl/http/",
    MIME="http://schemas.xmlsoap.org/wsdl/mime/",
    SOAP_11="http://schemas.xmlsoap.org/wsdl/soap/",
    SOAP_12="http://schemas.xmlsoap.org/wsdl/soap12/",
    SOAP_ENV_11="http://schemas.xmlsoap.org/soap/envelope/",
    SOAP_ENV_12="http://www.w3.org/2003/05/soap-envelope",
    WSA="http://www.w3.org/2005/08/addressing",
    WSDL="http://schemas.xmlsoap.org/wsdl/",
    XSD="http://www.w3.org/2001/XMLSchema",
    XSI="http://www.w3.org/2001/XMLSchema-instance",
    EncNs = "http://www.w3.org/2001/04/xmlenc#",
    XPathNs = "http://www.w3.org/TR/1999/REC-xpath-19991116",
    XPath2Ns = "http://www.w3.org/2002/06/xmldsig-filter2",
    XPointerNs = "http://www.w3.org/2001/04/xmldsig-more/xptr",
    NsExcC14N = "http://www.w3.org/2001/10/xml-exc-c14n#",
    NsExcC14NWithComments = "http://www.w3.org/2001/10/xml-exc-c14n#WithComments",
    WSSE=(
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
    ),
    WSU=(
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
    ),
    NAMESPACE_TO_PREFIX={},
)

ns.DSigNs = ns.DS
ns.Soap11Ns = ns.SOAP_ENV_11  # "http://schemas.xmlsoap.org/soap/envelope/",
ns.Soap12Ns = ns.SOAP_ENV_12  # "http://www.w3.org/2002/06/soap-envelope",
ns.NAMESPACE_TO_PREFIX[ns.XSD] = "xsd"
ns.NS_MAP = {"ds": ns.DSigNs, "ds11": ns.DSigNs11}