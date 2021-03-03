def Error(Exception):
    """
    'b'
    """
def Wave_read:
    """
    Variables used in this class:

        These variables are available to the user though appropriate
        methods of this class:
        _file -- the open file with methods read(), close(), and seek()
                  set through the __init__() method
        _nchannels -- the number of audio channels
                  available through the getnchannels() method
        _nframes -- the number of audio frames
                  available through the getnframes() method
        _sampwidth -- the number of bytes per audio sample
                  available through the getsampwidth() method
        _framerate -- the sampling frequency
                  available through the getframerate() method
        _comptype -- the AIFF-C compression type ('NONE' if AIFF)
                  available through the getcomptype() method
        _compname -- the human-readable AIFF-C compression type
                  available through the getcomptype() method
        _soundpos -- the position in the audio stream
                  available through the tell() method, set through the
                  setpos() method

        These variables are used internally only:
        _fmt_chunk_read -- 1 iff the FMT chunk has been read
        _data_seek_needed -- 1 iff positioned correctly in audio
                  file for readframes()
        _data_chunk -- instantiation of a chunk class for the DATA chunk
        _framesize -- size of one frame in the file
    
    """
    def initfp(self, file):
        """
        b'RIFF'
        """
    def __init__(self, f):
        """
        'rb'
        """
    def __del__(self):
        """

         User visible methods.


        """
    def getfp(self):
        """
        'no marks'
        """
    def setpos(self, pos):
        """
        'position not in range'
        """
    def readframes(self, nframes):
        """
        b''
        """
    def _read_fmt_chunk(self, chunk):
        """
        '<HHLLH'
        """
def Wave_write:
    """
    Variables used in this class:

        These variables are user settable through appropriate methods
        of this class:
        _file -- the open file with methods write(), close(), tell(), seek()
                  set through the __init__() method
        _comptype -- the AIFF-C compression type ('NONE' in AIFF)
                  set through the setcomptype() or setparams() method
        _compname -- the human-readable AIFF-C compression type
                  set through the setcomptype() or setparams() method
        _nchannels -- the number of audio channels
                  set through the setnchannels() or setparams() method
        _sampwidth -- the number of bytes per audio sample
                  set through the setsampwidth() or setparams() method
        _framerate -- the sampling frequency
                  set through the setframerate() or setparams() method
        _nframes -- the number of audio frames written to the header
                  set through the setnframes() or setparams() method

        These variables are used internally only:
        _datalength -- the size of the audio samples written to the header
        _nframeswritten -- the number of frames actually written
        _datawritten -- the size of the audio samples actually written
    
    """
    def __init__(self, f):
        """
        'wb'
        """
    def initfp(self, file):
        """

         User visible methods.


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
        'sample width not set'
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
        'cannot change parameters after starting to write'
        """
    def getcomptype(self):
        """
        'cannot change parameters after starting to write'
        """
    def getparams(self):
        """
        'not all parameters set'
        """
    def setmark(self, id, pos, name):
        """
        'setmark() not supported'
        """
    def getmark(self, id):
        """
        'no marks'
        """
    def getmarkers(self):
        """
        'B'
        """
    def writeframes(self, data):
        """

         Internal methods.



        """
    def _ensure_header_written(self, datasize):
        """
        '# channels not specified'
        """
    def _write_header(self, initlength):
        """
        b'RIFF'
        """
    def _patchheader(self):
        """
        '<L'
        """
def open(f, mode=None):
    """
    'mode'
    """
def openfp(f, mode=None):
    """
    wave.openfp is deprecated since Python 3.7. 
    Use wave.open instead.
    """
