    def _mkproxy(o):
        """
         --- ExpatLocator


        """
def ExpatLocator(xmlreader.Locator):
    """
    Locator for use with the ExpatParser class.

        This uses a weak reference to the parser object to avoid creating
        a circular reference between the parser and the content handler.
    
    """
    def __init__(self, parser):
        """
         --- ExpatParser


        """
def ExpatParser(xmlreader.IncrementalParser, xmlreader.Locator):
    """
    SAX driver for the pyexpat C module.
    """
    def __init__(self, namespaceHandling=0, bufsize=2**16-20):
        """
         XMLReader methods


        """
    def parse(self, source):
        """
        Parse an XML document from a URL or an InputSource.
        """
    def prepareParser(self, source):
        """
         Redefined setContentHandler to allow changing handlers during parsing


        """
    def setContentHandler(self, handler):
        """
        Feature '%s' not recognized
        """
    def setFeature(self, name, state):
        """
        Cannot set features while parsing
        """
    def getProperty(self, name):
        """
        GetInputContext
        """
    def setProperty(self, name, value):
        """
        Property '%s' cannot be set
        """
    def feed(self, data, isFinal = 0):
        """
         The isFinal parameter is internal to the expat reader.
         If it is set to true, expat will check validity of the entire
         document. When feeding chunks, they are not normally final -
         except when invoked from close.

        """
    def _close_source(self):
        """
         If we are completing an external entity, do nothing here

        """
    def _reset_cont_handler(self):
        """
 
        """
    def getColumnNumber(self):
        """
         event handlers

        """
    def start_element(self, name, attrs):
        """
         no namespace

        """
    def end_element_ns(self, name):
        """
         this is not used (call directly to ContentHandler)

        """
    def processing_instruction(self, target, data):
        """
         this is not used (call directly to ContentHandler)

        """
    def character_data(self, data):
        """

        """
    def skipped_entity_handler(self, name, is_pe):
        """
         The SAX spec requires to report skipped PEs with a '%'

        """
def create_parser(*args, **kwargs):
    """
     ---


    """
