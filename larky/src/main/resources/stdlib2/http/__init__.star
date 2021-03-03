def HTTPStatus(IntEnum):
    """
    HTTP status codes and reason phrases

        Status codes from the following RFCs are all observed:

            * RFC 7231: Hypertext Transfer Protocol (HTTP/1.1), obsoletes 2616
            * RFC 6585: Additional HTTP Status Codes
            * RFC 3229: Delta encoding in HTTP
            * RFC 4918: HTTP Extensions for WebDAV, obsoletes 2518
            * RFC 5842: Binding Extensions to WebDAV
            * RFC 7238: Permanent Redirect
            * RFC 2295: Transparent Content Negotiation in HTTP
            * RFC 2774: An HTTP Extension Framework
            * RFC 7725: An HTTP Status Code to Report Legal Obstacles
            * RFC 7540: Hypertext Transfer Protocol Version 2 (HTTP/2)
    
    """
    def __new__(cls, value, phrase, description=''):
        """
         informational

        """
