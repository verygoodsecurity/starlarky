def decode(input, errors='strict'):
    """
    'strict'
    """
    def encode(self, input, final=False):
        """
        'little'
        """
    def reset(self):
        """
         state info we return to the caller:
         0: stream is in natural order for this platform
         2: endianness hasn't been determined yet
         (we're never writing in unnatural order)

        """
    def setstate(self, state):
        """
        'little'
        """
def IncrementalDecoder(codecs.BufferedIncrementalDecoder):
    """
    'strict'
    """
    def _buffer_decode(self, input, errors, final):
        """
        UTF-16 stream does not start with BOM
        """
    def reset(self):
        """
         additional state info from the base class must be None here,
         as it isn't passed along to the caller

        """
    def setstate(self, state):
        """
         state[1] will be ignored by BufferedIncrementalDecoder.setstate()

        """
def StreamWriter(codecs.StreamWriter):
    """
    'strict'
    """
    def reset(self):
        """
        'strict'
        """
def StreamReader(codecs.StreamReader):
    """
    'strict'
    """
def getregentry():
    """
    'utf-16'
    """
