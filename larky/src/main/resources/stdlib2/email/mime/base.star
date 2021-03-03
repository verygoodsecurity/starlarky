def MIMEBase(message.Message):
    """
    Base class for MIME specializations.
    """
    def __init__(self, _maintype, _subtype, *, policy=None, **_params):
        """
        This constructor adds a Content-Type: and a MIME-Version: header.

                The Content-Type: header is taken from the _maintype and _subtype
                arguments.  Additional parameters for this header are taken from the
                keyword arguments.
        
        """
