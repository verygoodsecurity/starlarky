"""Simple API for XML (SAX) implementation for Python.

This module provides an implementation of the SAX 2 interface;
information about the Java version of the interface can be found at
http://www.megginson.com/SAX/.  The Python version of the interface is
documented at <...>.

This package contains the following modules:

handler -- Base classes and constants which define the SAX 2 API for
           the 'client-side' of SAX for Python.

saxutils -- Implementation of the convenience classes commonly used to
            work with SAX.

xmlreader -- Base classes and constants which define the SAX 2 API for
             the parsers used with SAX for Python.

expatreader -- Driver that allows use of the Expat parser with SAX.
"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//types", types="types")
load("@stdlib//io", io="io")
load("@stdlib//larky", larky="larky")
load("@stdlib//xml/sax/drivers/drv_xmllib", drv_xmllib="drv_xmllib")
load("@stdlib//xml/sax/_exceptions", SAXException="SAXException", SAXNotRecognizedException="SAXNotRecognizedException", SAXParseException="SAXParseException", SAXNotSupportedException="SAXNotSupportedException", SAXReaderNotAvailable="SAXReaderNotAvailable")
load("@stdlib//xml/sax/handler", ContentHandler="ContentHandler", ErrorHandler="ErrorHandler")
load("@stdlib//xml/sax/xmlreader", InputSource="InputSource")
load("@vendor//option/result", Error="Error")


def parse(source, handler, errorHandler=None):
    if errorHandler == None:
        errorHandler = ErrorHandler()
    parser = make_parser()
    parser.setContentHandler(handler)
    parser.setErrorHandler(errorHandler)
    parser.parse(source)


def parseString(string, handler, errorHandler=None):
    if errorHandler == None:
        errorHandler = ErrorHandler()
    parser = make_parser()
    parser.setContentHandler(handler)
    parser.setErrorHandler(errorHandler)

    inpsrc = InputSource()
    if types.is_string(string):
        inpsrc.setCharacterStream(io.StringIO(string))
    else:
        inpsrc.setByteStream(io.BytesIO(string))
    parser.parse(inpsrc)


# this is the parser list used by the make_parser function if no
# alternatives are given as parameters to the function

default_parser_list = ["xml.sax.drivers.drv_xmllib"]


def make_parser(parser_list=()):
    """Creates and returns a SAX parser.

    Creates the first parser it is able to instantiate of the ones
    given in the iterable created by chaining parser_list and
    default_parser_list.  The iterables must contain the names of Python
    modules containing both a SAX parser and a create_parser function."""

    if not parser_list:
        return drv_xmllib.create_parser()

    # larky does not support loading anything else for right now except
    # the xmllib driver.
    #
    # maybe we can make a java based one later.
    fail("SAXReaderNotAvailable: No parsers found")
