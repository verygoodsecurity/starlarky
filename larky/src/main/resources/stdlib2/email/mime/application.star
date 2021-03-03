def MIMEApplication(MIMENonMultipart):
    """
    Class for generating application/* MIME documents.
    """
2021-03-02 20:54:40,131 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, _data, _subtype='octet-stream',
                 _encoder=encoders.encode_base64, *, policy=None, **_params):
        """
        Create an application/* type MIME document.

                _data is a string containing the raw application data.

                _subtype is the MIME content type subtype, defaulting to
                'octet-stream'.

                _encoder is a function which will perform the actual encoding for
                transport of the application data, defaulting to base64 encoding.

                Any additional keyword arguments are passed to the base class
                constructor, which turns them into parameters on the Content-Type
                header.
        
        """
