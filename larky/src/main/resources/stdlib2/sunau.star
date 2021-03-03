def Error(Exception):
    """
    ''
    """
    def __del__(self):
        """
        'bad magic number'
        """
    def getfp(self):
        """
         XXX--must do some arithmetic here
        """
    def getcomptype(self):
        """
        'ULAW'
        """
    def getcompname(self):
        """
        'CCITT G.711 u-law'
        """
    def getparams(self):
        """
        'no marks'
        """
    def readframes(self, nframes):
        """
         XXX--not implemented yet
        """
    def rewind(self):
        """
        'cannot seek'
        """
    def tell(self):
        """
        'position not in range'
        """
    def close(self):
        """
        ''
        """
    def __del__(self):
        """
        b''
        """
    def setnchannels(self, nchannels):
        """
        'cannot change parameters after starting to write'
        """
    def getnchannels(self):
        """
        'number of channels not set'
        """
    def setsampwidth(self, sampwidth):
        """
        'cannot change parameters after starting to write'
        """
    def getsampwidth(self):
        """
        'sample width not specified'
        """
    def setframerate(self, framerate):
        """
        'cannot change parameters after starting to write'
        """
    def getframerate(self):
        """
        'frame rate not set'
        """
    def setnframes(self, nframes):
        """
        'cannot change parameters after starting to write'
        """
    def getnframes(self):
        """
        'NONE'
        """
    def getcomptype(self):
        """
        'ULAW'
        """
    def setparams(self, params):
        """
        'B'
        """
    def writeframes(self, data):
        """

         private methods



        """
    def _ensure_header_written(self):
        """
        '# of channels not specified'
        """
    def _write_header(self):
        """
        'NONE'
        """
    def _patchheader(self):
        """
        'cannot seek'
        """
def open(f, mode=None):
    """
    'mode'
    """
def openfp(f, mode=None):
    """
    sunau.openfp is deprecated since Python 3.7. 
    Use sunau.open instead.
    """
