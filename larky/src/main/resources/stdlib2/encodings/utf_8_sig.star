def encode(input, errors='strict'):
    """
    'strict'
    """
def IncrementalEncoder(codecs.IncrementalEncoder):
    """
    'strict'
    """
    def encode(self, input, final=False):
        """
        'strict'
        """
    def _buffer_decode(self, input, errors, final):
        """
         not enough data to decide if this really is a BOM
         => try again on the next call

        """
    def reset(self):
        """
         state[1] must be 0 here, as it isn't passed along to the caller

        """
    def setstate(self, state):
        """
         state[1] will be ignored by BufferedIncrementalDecoder.setstate()

        """
def StreamWriter(codecs.StreamWriter):
    """
    'strict'
    """
def StreamReader(codecs.StreamReader):
    """
    'strict'
    """
def getregentry():
    """
    'utf-8-sig'
    """
