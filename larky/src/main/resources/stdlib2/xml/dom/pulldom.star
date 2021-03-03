def PullDOM(xml.sax.ContentHandler):
    """
     use class' pop instead

    """
    def pop(self):
        """
        '_xmlns_attrs'
        """
    def endPrefixMapping(self, prefix):
        """
         Retrieve xml namespace declaration attributes.

        """
    def endElementNS(self, name, tagName):
        """
         Can't do that in startDocument, since we need the tagname
         XXX: obtain DocumentType

        """
    def endDocument(self):
        """
        clear(): Explicitly release parsing structures
        """
def ErrorHandler:
    """
    'feed'
    """
    def reset(self):
        """
         This content handler relies on namespace support

        """
    def __getitem__(self, pos):
        """
        DOMEventStream's __getitem__ method ignores 'pos' parameter. 
        Use iterator protocol instead.
        """
    def __next__(self):
        """
         use IncrementalParser interface, so we get the desired
         pull effect

        """
    def _slurp(self):
        """
         Fallback replacement for getEvent() using the
                    standard SAX2 interface, which means we slurp the
                    SAX events into memory (no performance gain, but
                    we are compatible to all SAX parsers).
        
        """
    def _emit(self):
        """
         Fallback replacement for getEvent() that emits
                    the events that _slurp() read previously.
        
        """
    def clear(self):
        """
        clear(): Explicitly release parsing objects
        """
def SAX2DOM(PullDOM):
    """
    'rb'
    """
def parseString(string, parser=None):
