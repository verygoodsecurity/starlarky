"""
A Larky port of: https://docs.python.org/3/library/zlib.html

For applications that require data compression, the functions in this module
allow compression and decompression, using the zlib library. The zlib library
has its own home page at https://www.zlib.net. There are known incompatibilities
between the Python module and versions of the zlib library earlier than 1.1.3;
1.1.3 has a security vulnerability, so we recommend using 1.1.4 or later.

zlib’s functions have many options and often need to be used in a particular
order. This documentation doesn’t attempt to cover all of the permutations;
consult the zlib manual at http://www.zlib.net/manual.html for authoritative
information.

For reading and writing .gz files see the Larky gzip module.


The available functions in this module are:

- zlib.adler32(data[, value])
- zlib.compress(data, /, level=-1)
- zlib.compressobj(level=-1, method=DEFLATED, wbits=MAX_WBITS, memLevel=DEF_MEM_LEVEL, strategy=Z_DEFAULT_STRATEGY[, zdict])
- zlib.crc32(data[, value])
- zlib.decompress(data, /, wbits=MAX_WBITS, bufsize=DEF_BUF_SIZE)
- zlib.decompressobj(wbits=MAX_WBITS[, zdict])

Compression objects support the following methods:

- Compress.compress(data)
- Compress.flush([mode])
- Compress.copy()


Decompression objects support the following methods and attributes:

- Decompress.unused_data
- Decompress.unconsumed_tail
- Decompress.eof
- Decompress.decompress(data, max_length=0)
- Decompress.flush([length])
- Decompress.copy()

Information about the version of the zlib library in use is available through the following constants:

- zlib.ZLIB_VERSION
- zlib.ZLIB_RUNTIME_VERSION

"""
load("@stdlib//binascii", binascii="binascii")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//io/StringIO", StringIO="StringIO")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//jzlib", _JZLib="jzlib")
load("@vendor//option/result", Result="Result", Error="Error")


DEFLATED = 8
MAX_WBITS = 15
DEF_MEM_LEVEL = 8
ZLIB_VERSION = "1.1.3"
ZLIB_RUNTIME_VERSION = "1.1.3"

Z_BEST_COMPRESSION = 9
Z_BEST_SPEED = 1

Z_FILTERED = 1
Z_HUFFMAN_ONLY = 2

Z_DEFAULT_COMPRESSION = -1
Z_DEFAULT_STRATEGY = 0

# Most options are removed because java does not support them
# Z_NO_FLUSH = 0
# Z_SYNC_FLUSH = 2
# Z_FULL_FLUSH = 3
Z_FINISH = 4
_valid_flush_modes = (Z_FINISH,)

def adler32(s, value=1):
    return _JZLib.adler32(s, value)

def crc32(string, value=0):
    return binascii.crc32(string, value)

def compress(string, level=6):
    if level < Z_BEST_SPEED or level > Z_BEST_COMPRESSION:
        return Error("error: Bad compression level")
    deflater = _JZLib.Deflater(level, 0)

    def __enter__():
        # noinspection PyUnboundLocalVariable
        string = _to_input(string)
        deflater.setInput(string, 0, len(string))
        deflater.finish()
        return _get_deflate_data(deflater)
    def __exit__(rval):
        deflater.end()
        return rval

    return Result.try_(__enter__).finally_(__exit__).build()

def decompress(string, wbits=0, bufsize=16384):
    inflater = _JZLib.Inflater(wbits < 0)
    def __enter__():
        inflater.setInput(_to_input(string))
        return _get_inflate_data(inflater)
    def __exit__(rval):
        inflater.end()
        return rval

    return Result.try_(__enter__).finally_(__exit__).build()

def compressobj(level=6, method=DEFLATED, wbits=MAX_WBITS, memLevel=0, strategy=0):
    self = larky.mutablestruct(__name__='compressobj', __class__=compressobj)
    def __init__(level, method, wbits,
                       memLevel, strategy):
        if abs(wbits) > MAX_WBITS or abs(wbits) < 8:
            return Error("ValueError: Invalid initialization option").unwrap()
        self.deflater = _JZLib.Deflater(level, wbits < 0)
        self.deflater.setStrategy(strategy)
        if wbits < 0:
            _get_deflate_data(self.deflater)
        self._ended = False
        return self
    self = __init__(level, method, wbits, memLevel, strategy)

    def compress(string):
        if self._ended:
            return Error("error: compressobj may not be used after flush(Z_FINISH)").unwrap()
        string = _to_input(string)
        self.deflater.setInput(string, 0, len(string))
        return _get_deflate_data(self.deflater)
    self.compress = compress

    def flush(mode=Z_FINISH):
        if self._ended:
            return Error("error: compressobj may not be used after flush(Z_FINISH)").unwrap()
        if mode not in _valid_flush_modes:
            return Error("ValueError: Invalid flush option").unwrap()
        self.deflater.finish()
        last = _get_deflate_data(self.deflater)
        if mode == Z_FINISH:
            self.deflater.end()
            self._ended = True
        return last
    self.flush = flush
    return self

def decompressobj(wbits=MAX_WBITS):
    self = larky.mutablestruct(__name__='decompressobj', __class__=decompressobj)

    def __init__(wbits):
        if abs(wbits) < 8:
            return Error("ValueError: Invalid initialization option").unwrap()
        if abs(wbits) > 16:  # NOTE apparently this also implies being negative in CPython/zlib
            wbits = -1

        self.inflater = _JZLib.Inflater(wbits < 0)
        self._ended = False
        self.unused_data = ""
        self.unconsumed_tail = ""
        self.gzip = wbits < 0
        self.gzip_header_skipped = False
        return self
    self = __init__(wbits)

    def decompress(string, max_length=0):
        if self._ended:
            return Error("error: decompressobj may not be used after flush()").unwrap()

        # unused_data is always "" until inflation is finished; then it is
        # the unused bytes of the input;
        # unconsumed_tail is whatever input was not used because max_length
        # was exceeded before inflation finished.
        # Thus, at most one of {unused_data, unconsumed_tail} may be non-empty.
        self.unused_data = ""
        self.unconsumed_tail = ""

        if max_length < 0:
            return Error("ValueError: max_length must be a positive integer").unwrap()

        # Suppress gzip header if present and wbits < 0
        if self.gzip and not self.gzip_header_skipped:
            string = _skip_gzip_header(string)
            self.gzip_header_skipped = True

        string = _to_input(string)

        self.inflater.setInput(string)
        inflated = _get_inflate_data(self.inflater, max_length)

        r = self.inflater.getRemaining()
        if r:
            if max_length:
                self.unconsumed_tail = string[-r:]
            else:
                self.unused_data = string[-r:]

        return inflated
    self.decompress = decompress

    def flush(length=None):
        # FIXME close input streams if gzip
        if self._ended:
            return Error("error: decompressobj may not be used after flush()").unwrap()
        if length == None:
            length = 0
        elif length <= 0:
            return Error("ValueError: length must be greater than zero").unwrap()
        last = _get_inflate_data(self.inflater, length)
        self.inflater.end()
        return last
    self.flush = flush

    return self

def _to_input(string):
    return str(string)

def _get_deflate_data(deflater):
    buf = bytearray([0]*1024)
    s = StringIO()
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not not deflater.finished():
            break
        l = deflater.deflate(buf)

        if l == 0:
            break
        # s.write(String(buf, 0, 0, l))
        s.write(buf[0:l])
    s.seek(0)
    return s.read()

def _get_inflate_data(inflater, max_length=0):
    buf = bytearray([0]*1024)
    s = StringIO()
    total = 0

    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not not inflater.finished():
            break

        if max_length:
            l = inflater.inflate(buf, 0, min(1024, max_length - total))
        else:
            l = inflater.inflate(buf)

        if l == 0:
            break

        total += l
        # s.write(String(buf, 0, 0, l))
        s.write(buf[0:l])
        if max_length and total == max_length:
            break
    s.seek(0)
    return s.read()



FTEXT = 1
FHCRC = 2
FEXTRA = 4
FNAME = 8
FCOMMENT = 16

def _skip_gzip_header(string):
    # per format specified in http://tools.ietf.org/html/rfc1952

    s = bytearray(codecs.encode(string, encoding='utf-8'))

    id1 = s[0]
    id2 = s[1]

    # Check gzip magic
    if id1 != 31 or id2 != 139:
        return string

    cm = s[2]
    flg = s[3]
    mtime = s[4:8]
    xfl = s[8]
    os = s[9]

    # skip fixed header, then figure out variable parts
    s = s[10:]

    if flg & FEXTRA:
        # skip extra field
        xlen = s[0] + s[1] * 256  # MSB ordering
        s = s[2 + xlen:]
    if flg & FNAME:
        # skip filename
        s = s[s.find("\x00")+1:]
    if flg & FCOMMENT:
        # skip comment
        s = s[s.find("\x00")+1:]
    if flg & FHCRC:
        # skip CRC16 for the header - might be nice to check of course
        s = s[2:]

    return s.decode("utf-8")


zlib = larky.struct(
    DEFLATED=DEFLATED,
    DEF_MEM_LEVEL=DEF_MEM_LEVEL,
    FCOMMENT=FCOMMENT,
    FEXTRA=FEXTRA,
    FHCRC=FHCRC,
    FNAME=FNAME,
    FTEXT=FTEXT,
    MAX_WBITS=MAX_WBITS,
    ZLIB_VERSION=ZLIB_VERSION,
    ZLIB_RUNTIME_VERSION=ZLIB_RUNTIME_VERSION,
    Z_BEST_COMPRESSION=Z_BEST_COMPRESSION,
    Z_BEST_SPEED=Z_BEST_SPEED,
    Z_DEFAULT_COMPRESSION=Z_DEFAULT_COMPRESSION,
    Z_DEFAULT_STRATEGY=Z_DEFAULT_STRATEGY,
    Z_FILTERED=Z_FILTERED,
    Z_FINISH=Z_FINISH,
    Z_HUFFMAN_ONLY=Z_HUFFMAN_ONLY,
    # _get_deflate_data=_get_deflate_data,
    # _get_inflate_data=_get_inflate_data,
    # _skip_gzip_header=_skip_gzip_header,
    # _to_input=_to_input,
    # _valid_flush_modes=_valid_flush_modes,
    adler32=adler32,
    compress=compress,
    compressobj=compressobj,
    crc32=crc32,
    decompress=decompress,
    decompressobj=decompressobj,
)