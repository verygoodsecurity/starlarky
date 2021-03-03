def MessageError(Exception):
    """
    Base class for errors in the email package.
    """
def MessageParseError(MessageError):
    """
    Base class for message parsing errors.
    """
def HeaderParseError(MessageParseError):
    """
    Error while parsing headers.
    """
def BoundaryError(MessageParseError):
    """
    Couldn't find terminating boundary.
    """
def MultipartConversionError(MessageError, TypeError):
    """
    Conversion to a multipart is prohibited.
    """
def CharsetError(MessageError):
    """
    An illegal charset was given.
    """
def MessageDefect(ValueError):
    """
    Base class for a message defect.
    """
    def __init__(self, line=None):
        """
        A message claimed to be a multipart but had no boundary parameter.
        """
def StartBoundaryNotFoundDefect(MessageDefect):
    """
    The claimed start boundary was never found.
    """
def CloseBoundaryNotFoundDefect(MessageDefect):
    """
    A start boundary was found, but not the corresponding close boundary.
    """
def FirstHeaderLineIsContinuationDefect(MessageDefect):
    """
    A message had a continuation line as its first header line.
    """
def MisplacedEnvelopeHeaderDefect(MessageDefect):
    """
    A 'Unix-from' header was found in the middle of a header block.
    """
def MissingHeaderBodySeparatorDefect(MessageDefect):
    """
    Found line with no leading whitespace and no colon before blank line.
    """
def MultipartInvariantViolationDefect(MessageDefect):
    """
    A message claimed to be a multipart but no subparts were found.
    """
def InvalidMultipartContentTransferEncodingDefect(MessageDefect):
    """
    An invalid content transfer encoding was set on the multipart itself.
    """
def UndecodableBytesDefect(MessageDefect):
    """
    Header contained bytes that could not be decoded
    """
def InvalidBase64PaddingDefect(MessageDefect):
    """
    base64 encoded sequence had an incorrect length
    """
def InvalidBase64CharactersDefect(MessageDefect):
    """
    base64 encoded sequence had characters not in base64 alphabet
    """
def InvalidBase64LengthDefect(MessageDefect):
    """
    base64 encoded sequence had invalid length (1 mod 4)
    """
def HeaderDefect(MessageDefect):
    """
    Base class for a header defect.
    """
    def __init__(self, *args, **kw):
        """
        Header is not valid, message gives details.
        """
def HeaderMissingRequiredValue(HeaderDefect):
    """
    A header that must have a value had none
    """
def NonPrintableDefect(HeaderDefect):
    """
    ASCII characters outside the ascii-printable range found
    """
    def __init__(self, non_printables):
        """
        the following ASCII non-printables found in header: 
        {}
        """
def ObsoleteHeaderDefect(HeaderDefect):
    """
    Header uses syntax declared obsolete by RFC 5322
    """
def NonASCIILocalPartDefect(HeaderDefect):
    """
    local_part contains non-ASCII characters
    """
