"""Different kinds of SAX Exceptions"""
load("@stdlib//larky", larky="larky")
load("@vendor//option/result", Error="Error")

def SAXException(msg, exception=None):
    """Encapsulate an XML error or warning. This class can contain
    basic error or warning information from either the XML parser or
    the application: you can subclass it to provide additional
    functionality, or to add localization. Note that although you will
    receive a SAXException as the argument to the handlers in the
    ErrorHandler interface, you are not actually required to raise
    the exception; instead, you can simply read the information in
    it."""
    self = larky.mutablestruct(__name__='SAXException', __class__=SAXException)

    def __init__(msg, exception):
        """Creates an exception. The message is required, but the exception
        is optional."""
        self._msg = msg
        self._exception = exception or Error(msg)
        return self
    self = __init__(msg, exception)

    def unwrap():
        self._exception.unwrap()

    self.unwrap = unwrap

    def getMessage():
        "Return a message for this exception."
        return self._msg
    self.getMessage = getMessage

    def getException():
        "Return the embedded exception, or None if there was none."
        return self._exception
    self.getException = getException

    def __str__():
        "Create a string representation of the exception."
        return self._msg
    self.__str__ = __str__

    def __getitem__(ix):
        """Avoids weird error messages if someone does exception[ix] by
        mistake, since Exception has __getitem__ defined."""
        fail("AttributeError: __getitem__")
    self.__getitem__ = __getitem__
    return self


def SAXParseException(msg, exception, locator):
    """Encapsulate an XML parse error or warning.

    This exception will include information for locating the error in
    the original XML document. Note that although the application will
    receive a SAXParseException as the argument to the handlers in the
    ErrorHandler interface, the application is not actually required
    to raise the exception; instead, it can simply read the
    information in it and take a different action.

    Since this exception is a subclass of SAXException, it inherits
    the ability to wrap another exception."""
    self = SAXException(msg, exception)
    self.__name__ = 'SAXParseException'
    self.__class__ = SAXParseException

    def __init__(locator):
        "Creates the exception. The exception parameter is allowed to be None."
        self._locator = locator

        # We need to cache this stuff at construction time.
        # If this exception is raised, the objects through which we must
        # traverse to get this information may be deleted by the time
        # it gets caught.
        self._systemId = self._locator.getSystemId()
        self._colnum = self._locator.getColumnNumber()
        self._linenum = self._locator.getLineNumber()
        return self
    self = __init__(locator)

    def getColumnNumber():
        """The column number of the end of the text where the exception
        occurred."""
        return self._colnum
    self.getColumnNumber = getColumnNumber

    def getLineNumber():
        "The line number of the end of the text where the exception occurred."
        return self._linenum
    self.getLineNumber = getLineNumber

    def getPublicId():
        "Get the public identifier of the entity where the exception occurred."
        return self._locator.getPublicId()
    self.getPublicId = getPublicId

    def getSystemId():
        "Get the system identifier of the entity where the exception occurred."
        return self._systemId
    self.getSystemId = getSystemId

    def __str__():
        "Create a string representation of the exception."
        sysid = self.getSystemId()
        if sysid == None:
            sysid = "<unknown>"
        linenum = self.getLineNumber()
        if linenum == None:
            linenum = "?"
        colnum = self.getColumnNumber()
        if colnum == None:
            colnum = "?"
        return "%s:%s:%s: %s" % (sysid, linenum, colnum, self._msg)
    self.__str__ = __str__
    return self


def SAXNotRecognizedException(msg, exception):
    """Exception class for an unrecognized identifier.

    An XMLReader will raise this exception when it is confronted with an
    unrecognized feature or property. SAX applications and extensions may
    use this class for similar purposes."""
    self = SAXException(msg, exception)
    self.__name__ = 'SAXNotRecognizedException'
    self.__class__ = SAXNotRecognizedException
    return self


def SAXNotSupportedException(msg, exception):
    """Exception class for an unsupported operation.

    An XMLReader will raise this exception when a service it cannot
    perform is requested (specifically setting a state or value). SAX
    applications and extensions may use this class for similar
    purposes."""
    self = SAXException(msg, exception)
    self.__name__ = 'SAXNotSupportedException'
    self.__class__ = SAXNotSupportedException
    return self


def SAXReaderNotAvailable(msg, exception):
    """Exception class for a missing driver.

    An XMLReader module (driver) should raise this exception when it
    is first imported, e.g. when a support module cannot be imported.
    It also may be raised during parsing, e.g. if executing an external
    program is not permitted."""
    self = SAXException(msg, exception)
    self.__name__ = 'SAXReaderNotAvailable'
    self.__class__ = SAXReaderNotAvailable
    return self

