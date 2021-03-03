def Error(Exception):
    """
     States (what have we written)

    """
def FInfo:
    """
    '????'
    """
def getfileinfo(name):
    """
    'rb'
    """
def openrsrc:
    """
    b''
    """
    def write(self, *args):
        """
        Write data to the coder in 3-byte chunks
        """
    def __init__(self, ofp):
        """
        b''
        """
    def write(self, data):
        """
        b'\n'
        """
    def close(self):
        """
        Write data to the RLE-coder in suitably large chunks
        """
    def __init__(self, ofp):
        """
        b''
        """
    def write(self, data):
        """
        b''
        """
    def close(self):
        """
        'wb'
        """
    def _writeinfo(self, name, finfo):
        """
        'Filename too long'
        """
    def _write(self, data):
        """
         XXXX Should this be here??
         self.crc = binascii.crc_hqx('\0\0', self.crc)

        """
    def write(self, data):
        """
        'Writing data at the wrong time'
        """
    def close_data(self):
        """
        'Incorrect data size, diff=%r'
        """
    def write_rsrc(self, data):
        """
        'Writing resource data at the wrong time'
        """
    def close(self):
        """
        'Close at the wrong time'
        """
def binhex(inp, out):
    """
    binhex(infilename, outfilename): create binhex-encoded copy of a file
    """
def _Hqxdecoderengine:
    """
    Read data via the decoder in 4-byte chunks
    """
    def __init__(self, ifp):
        """
        Read at least wtd bytes (or until EOF)
        """
    def close(self):
        """
        Read data via the RLE-coder
        """
    def __init__(self, ifp):
        """
        b''
        """
    def read(self, wtd):
        """
        b''
        """
    def close(self):
        """
        'rb'
        """
    def _read(self, len):
        """
        '>h'
        """
    def _readheader(self):
        """
        '>h'
        """
    def read(self, *n):
        """
        'Read data at wrong time'
        """
    def close_data(self):
        """
        'close_data at wrong time'
        """
    def read_rsrc(self, *n):
        """
        'Read resource data at wrong time'
        """
    def close(self):
        """
        hexbin(infilename, outfilename) - Decode binhexed file
        """
