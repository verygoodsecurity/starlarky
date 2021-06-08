#
# ElementTree
# $Id: SimpleXMLTreeBuilder.py 1862 2004-06-18 07:31:02Z Fredrik $
#
# A simple XML tree builder, based on Python's xmllib
#
# Note that due to bugs in xmllib, this builder does not fully support
# namespaces (unqualified attributes are put in the default namespace,
# instead of being left as is).  Run this module as a script to find
# out if this affects your Python version.
#
# history:
# 2001-10-20 fl   created
# 2002-05-01 fl   added namespace support for xmllib
# 2002-08-17 fl   added xmllib sanity test
#
# Copyright (c) 1999-2004 by Fredrik Lundh.  All rights reserved.
#
# fredrik@pythonware.com
# http://www.pythonware.com
#
# --------------------------------------------------------------------
# The ElementTree toolkit is
#
# Copyright (c) 1999-2004 by Fredrik Lundh
#
# By obtaining, using, and/or copying this software and/or its
# associated documentation, you agree that you have read, understood,
# and will comply with the following terms and conditions:
#
# Permission to use, copy, modify, and distribute this software and
# its associated documentation for any purpose and without fee is
# hereby granted, provided that the above copyright notice appears in
# all copies, and that both that copyright notice and this permission
# notice appear in supporting documentation, and that the name of
# Secret Labs AB or the author not be used in advertising or publicity
# pertaining to distribution of the software without specific, written
# prior permission.
#
# SECRET LABS AB AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD
# TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANT-
# ABILITY AND FITNESS.  IN NO EVENT SHALL SECRET LABS AB OR THE AUTHOR
# BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY
# DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
# WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
# ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
# OF THIS SOFTWARE.
# --------------------------------------------------------------------

##
# Tools to build element trees from XML files, using <b>xmllib</b>.
# This module can be used instead of the standard tree builder, for
# Python versions where "expat" is not available (such as 1.5.2).
# <p>
# Note that due to bugs in <b>xmllib</b>, the namespace support is
# not reliable (you can run the module as a script to find out exactly
# how unreliable it is on your Python version).
##
load("@stdlib//larky", larky="larky")
load("@stdlib//xmllib", xmllib="xmllib")
load("@stdlib//string", string="string")
load("@stdlib//xml/etree/ElementTree", ElementTree="ElementTree")
load("@stdlib//debugging", "debugging")
debug = debugging.debug


def fixname(name, split=None):
    # xmllib in 2.0 and later provides limited (and slightly broken)
    # support for XML namespaces.
    if " " not in name:
        return name
    if not split:
        split = name.split
    return "{%s}%s" % tuple(split(" ", 1))

##
# ElementTree builder for XML source data.
#
# @see elementtree.ElementTree

def TreeBuilder(element_factory=None):

    def __init__(element_factory):
        self = xmllib.XMLParser()
        self.__class__ = 'SimpleXMLTreeBuilder.TreeBuilder'
        self.__builder = ElementTree.TreeBuilder(element_factory)
        return self
    self = __init__(element_factory)

    ##
    # Feeds data to the parser.
    #
    # @param data Encoded data.
    XMLParser_feed = self.feed
    def feed(data):
        XMLParser_feed(data)
    self.feed = feed

    ##
    # Finishes feeding data to the parser.
    #
    # @return An element structure.
    # @defreturn Element
    XMLParser_close = self.close
    def close():
        XMLParser_close()
        return self.__builder.close()
    self.close = close

    def handle_data(data):
        debug("handle_data: GOT CALLED?", data, "\n")
        self.__builder.data(data)
    self.handle_data = handle_data
    self.handle_cdata = handle_data

    def unknown_starttag(tag, attrs):
        #print("unknown_starttag: GOT CALLED?", tag, attrs, "\n")
        attrib = {}
        for key, value in attrs.items():
            attrib[fixname(key)] = value
        self.__builder.start(fixname(tag), attrib)
    self.unknown_starttag = unknown_starttag

    def unknown_endtag(tag):
        #print("unknown_endtag: GOT CALLED?", tag,  "\n")
        self.__builder.end(fixname(tag))
    self.unknown_endtag = unknown_endtag
    return self


SimpleXMLTreeBuilder = larky.struct(
    TreeBuilder=TreeBuilder,
    fixname=fixname,
)