def Options:
    """
    Features object that has variables set for each DOMBuilder feature.

        The DOMBuilder class uses an instance of this class to pass settings to
        the ExpatBuilder class.
    
    """
def DOMBuilder:
    """
    unsupported feature: %r
    """
    def supportsFeature(self, name):
        """
         This dictionary maps from (feature,value) to a list of
         (option,value) pairs that should be set on the Options object.
         If a (feature,value) setting is not in this dictionary, it is
         not supported by the DOMBuilder.


        """
    def getFeature(self, name):
        """
        infoset
        """
    def parseURI(self, uri):
        """
        not a legal action
        """
    def _parse_bytestream(self, stream, options):
        """
        '-'
        """
def DOMEntityResolver(object):
    """
    '_opener'
    """
    def resolveEntity(self, publicId, systemId):
        """
         determine the encoding if the transport provided it

        """
    def _get_opener(self):
        """
        Content-Type
        """
def DOMInputSource(object):
    """
    'byteStream'
    """
    def __init__(self):
        """
        Element filter which can be used to tailor construction of
            a DOM instance.
    
        """
    def _get_whatToShow(self):
        """
        Mixin to create documents that conform to the load/save spec.
        """
    def _get_async(self):
        """
        asynchronous document loading is not supported
        """
    def abort(self):
        """
         What does it mean to "clear" a document?  Does the
         documentElement disappear?

        """
    def load(self, uri):
        """
        haven't written this yet
        """
    def loadXML(self, source):
        """
        haven't written this yet
        """
    def saveXML(self, snode):
        """
        schemaType not yet supported
        """
    def createDOMWriter(self):
        """
        the writer interface hasn't been written yet!
        """
    def createDOMInputSource(self):
