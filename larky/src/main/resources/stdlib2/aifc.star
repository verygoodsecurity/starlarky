def Error(Exception):
    """
     Version 1 of AIFF-C
    """
def _read_long(file):
    """
    '>l'
    """
def _read_ulong(file):
    """
    '>L'
    """
def _read_short(file):
    """
    '>h'
    """
def _read_ushort(file):
    """
    '>H'
    """
def _read_string(file):
    """
    b''
    """
def _read_float(f): # 10 bytes
    """
     10 bytes
    """
def _write_short(f, x):
    """
    '>h'
    """
def _write_ushort(f, x):
    """
    '>H'
    """
def _write_long(f, x):
    """
    '>l'
    """
def _write_ulong(f, x):
    """
    '>L'
    """
def _write_string(f, s):
    """
    string exceeds maximum pstring length
    """
def _write_float(f, x):
    """
     Infinity or NaN
    """
def Aifc_read:
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
     _markers -- the marks in the audio file
           available through the getmarkers() and getmark()
           methods
     _soundpos -- the position in the audio stream
           available through the tell() method, set through the
           setpos() method

     These variables are used internally only:
     _version -- the AIFF-C version number
     _decomp -- the decompressor from builtin module cl
     _comm_chunk_read -- 1 iff the COMM chunk has been read
     _aifc -- 1 iff reading an AIFF-C file
     _ssnd_seek_needed -- 1 iff positioned correctly in audio
           file for readframes()
     _ssnd_chunk -- instantiation of a chunk class for the SSND chunk
     _framesize -- size of one frame in the file


    """
    def initfp(self, file):
        """
        b'FORM'
        """
    def __init__(self, f):
        """
        'rb'
        """
    def __enter__(self):
        """

         User visible methods.


        """
    def getfp(self):
        """
          def getversion(self):
              return self._version


        """
    def getparams(self):
        """
        'marker {0!r} does not exist'
        """
    def setpos(self, pos):
        """
        'position not in range'
        """
    def readframes(self, nframes):
        """
        b''
        """
    def _alaw2lin(self, data):
        """
        '_adpcmstate'
        """
    def _read_comm_chunk(self, chunk):
        """
        'bad sample width'
        """
    def _readmark(self, chunk):
        """
         Some files appear to contain invalid counts.
         Cope with this by testing for EOF.

        """
def Aifc_write:
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
     _aifc -- whether we're writing an AIFF-C file or an AIFF file
           set through the aifc() method, reset through the
           aiff() method

     These variables are used internally only:
     _version -- the AIFF-C version number
     _comp -- the compressor from builtin module cl
     _nframeswritten -- the number of audio frames actually written
     _datalength -- the size of the audio samples written to the header
     _datawritten -- the size of the audio samples actually written


    """
    def __init__(self, f):
        """
        'wb'
        """
    def initfp(self, file):
        """
        b'NONE'
        """
    def __del__(self):
        """

         User visible methods.


        """
    def aiff(self):
        """
        'cannot change parameters after starting to write'
        """
    def aifc(self):
        """
        'cannot change parameters after starting to write'
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
          def setversion(self, version):
              if self._nframeswritten:
                  raise Error, 'cannot change parameters after starting to write'
              self._version = version


        """
    def setparams(self, params):
        """
        'cannot change parameters after starting to write'
        """
    def getparams(self):
        """
        'not all parameters set'
        """
    def setmark(self, id, pos, name):
        """
        'marker ID must be > 0'
        """
    def getmark(self, id):
        """
        'marker {0!r} does not exist'
        """
    def getmarkers(self):
        """
        'B'
        """
    def writeframes(self, data):
        """
         quick pad to even size

        """
    def _lin2alaw(self, data):
        """
        '_adpcmstate'
        """
    def _ensure_header_written(self, datasize):
        """
        b'ULAW'
        """
    def _init_compression(self):
        """
        b'G722'
        """
    def _write_header(self, initlength):
        """
        b'NONE'
        """
    def _write_form_length(self, datalength):
        """
        b'\x00'
        """
    def _writemarkers(self):
        """
        b'MARK'
        """
def open(f, mode=None):
    """
    'mode'
    """
def openfp(f, mode=None):
    """
    aifc.openfp is deprecated since Python 3.7. 
    Use aifc.open instead.
    """
