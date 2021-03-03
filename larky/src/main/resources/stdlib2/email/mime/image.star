def MIMEImage(MIMENonMultipart):
    """
    Class for generating image/* type MIME documents.
    """
2021-03-02 20:54:40,823 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, _imagedata, _subtype=None,
                 _encoder=encoders.encode_base64, *, policy=None, **_params):
        """
        Create an image/* type MIME document.

                _imagedata is a string containing the raw image data.  If this data
                can be decoded by the standard Python `imghdr' module, then the
                subtype will be automatically included in the Content-Type header.
                Otherwise, you can specify the specific image subtype via the _subtype
                parameter.

                _encoder is a function which will perform the actual encoding for
                transport of the image data.  It takes one argument, which is this
                Image instance.  It should use get_payload() and set_payload() to
                change the payload to the encoded form.  It should also add any
                Content-Transfer-Encoding or other headers to the message as
                necessary.  The default encoding is Base64.

                Any additional keyword arguments are passed to the base class
                constructor, which turns them into parameters on the Content-Type
                header.
        
        """
