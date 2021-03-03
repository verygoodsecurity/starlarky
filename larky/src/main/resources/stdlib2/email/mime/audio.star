def _whatsnd(data):
    """
    Try to identify a sound file type.

        sndhdr.what() has a pretty cruddy interface, unfortunately.  This is why
        we re-do it here.  It would be easier to reverse engineer the Unix 'file'
        command and use the standard 'magic' file, as shipped with a modern Unix.
    
    """
def MIMEAudio(MIMENonMultipart):
    """
    Class for generating audio/* MIME documents.
    """
2021-03-02 20:54:40,652 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, _audiodata, _subtype=None,
                 _encoder=encoders.encode_base64, *, policy=None, **_params):
        """
        Create an audio/* type MIME document.

                _audiodata is a string containing the raw audio data.  If this data
                can be decoded by the standard Python `sndhdr' module, then the
                subtype will be automatically included in the Content-Type header.
                Otherwise, you can specify  the specific audio subtype via the
                _subtype parameter.  If _subtype is not given, and no subtype can be
                guessed, a TypeError is raised.

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
