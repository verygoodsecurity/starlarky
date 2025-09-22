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
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//struct", struct="struct")
load("@stdlib//jzlib", _JZLib="jzlib")
load("@vendor//option/result", Result="Result", Error="Error")

# Variables with simple values
DEFLATED = 8
DEF_BUF_SIZE = 16384
DEF_MEM_LEVEL = 8
MAX_WBITS = 15 # 32K buffer, LZ77 window
# We only do windowBits=15(32K buffer, LZ77 algorithm) since java.util.zip
# only allows it.
#
# NOTE:
#   - deflateInit2 of zlib.c also accepts MAX_WBITS + 16(gzip compression)
#   - inflateInit2 also accepts:
#     - MAX_WBITS + 16(gzip decompression)
#     - MAX_WBITS + 32(automatic detection of gzip and LZ77)
#
# https://github.com/openjdk/jdk/blob/jdk8-b120/jdk/src/share/native/java/util/zip/zlib-1.2.5/README#L3
ZLIB_RUNTIME_VERSION = b"1.2.5"
ZLIB_VERSION = b"1.2.5"
Z_BEST_COMPRESSION = 9
Z_BEST_SPEED = 1
Z_BLOCK = 5
Z_DEFAULT_COMPRESSION = -1
Z_DEFAULT_STRATEGY = 0
Z_FILTERED = 1
Z_FINISH = 4
Z_FIXED = 4
Z_FULL_FLUSH = 3  # Supported by java
Z_HUFFMAN_ONLY = 2
Z_NO_COMPRESSION = 0
Z_NO_FLUSH = 0  # Supported by java
Z_PARTIAL_FLUSH = 1
Z_RLE = 3
Z_SYNC_FLUSH = 2  # Supported by java
Z_TREES = 6

# Larky specific
LARKY_MAX_BUFSIZE = 1073741824 #  (1 << 30)
_MASK32 = 0xffffffff   # 2**32 - 1 used for unsigned mod 2**32
_valid_flush_modes = (Z_NO_FLUSH, Z_SYNC_FLUSH, Z_FULL_FLUSH, Z_FINISH,)

_zlib_to_deflater = {
    Z_NO_FLUSH: _JZLib.NO_FLUSH,
    Z_SYNC_FLUSH: _JZLib.SYNC_FLUSH,
    Z_FULL_FLUSH: _JZLib.FULL_FLUSH
}


def adler32(data, value=1):
    """
    Compute an Adler-32 checksum of data.

      value
        Starting value of the checksum.

    The returned checksum is an integer.
    """
    if not types.is_bytelike(data):
        fail("TypeError: a bytes-like object is required, not '%s'" % type(data))
    return _JZLib.adler32(data, value)


def crc32(data, value=0):
    """
    Compute a CRC-32 checksum of data.

      value
        Starting value of the checksum.

    The returned checksum is an integer.
    """
    if not types.is_bytelike(data):
        fail("TypeError: a bytes-like object is required, not '%s'" % type(data))
    return binascii.crc32(data, value)



def compress(data, level=6):
    if level < Z_BEST_SPEED or level > Z_BEST_COMPRESSION:
        return Error("error: Bad compression level").unwrap()
    if not types.is_bytelike(data):
        fail("TypeError: a bytes-like object is required, not '%s'" % type(data))
    deflater = _JZLib.Deflater(level, False)

    def __enter__():
        deflater.setInput(data, 0, len(data))
        deflater.finish()
        return _get_deflate_data(deflater, Z_NO_FLUSH)

    def __exit__(rval):
        deflater.end()
        return rval

    result = Result.try_(__enter__).finally_(__exit__).build()
    return result.unwrap()


def gzip_compression(wbits):
    # MAX_WBITS + 16(gzip compression)
    if abs(wbits) & 16:
        if wbits > 0:
            wbits -= 16
        else:
            wbits += 16
        # gzip is true
        return True
    else:
        # gzip is false
        return False

def gzip_decompression(wbits):
    return gzip_compression(wbits)


def automatic_decompression(wbits):
    if abs(wbits) & 32:
        if wbits > 0:
            wbits -= 32
        else:
            wbits += 32
        return True
    else:
        return False


def decompress(data, wbits=0, bufsize=16384):
    if bufsize < 0:
        fail("ValueError: bufsize must be non-negative")
    elif bufsize == 0:
        bufsize = 1
    elif bufsize > LARKY_MAX_BUFSIZE:
        fail("OverflowError: int too large " +
             "(bufsize: '%d'), max is: %d" %
             (bufsize, LARKY_MAX_BUFSIZE))
    if not types.is_bytelike(data):
        fail("TypeError: a bytes-like object is required, not '%s'" % type(data))
    # print("wbits: ", wbits, "automatic: ", automatic_decompression(wbits), "gzip: ", gzip_decompression(wbits))

    # NOTE: https://stackoverflow.com/a/11077099/133514
    # The Java documentation is incorrect or at least misleading:
    # A gzip stream is raw deflate data wrapped with a gzip header and trailer.
    # To fully decode the gzip format,  we must:
    # 1. process the gzip header as described in RFC 1952
    # 2. use inflater to decompress the raw deflate data
    # 3. compute the crc32 of the uncompressed data
    # 4. and then, verify the crc and length (modulo 2^32) in the gzip trailer
    if abs(wbits) > 16 and not automatic_decompression(wbits):
        if gzip_decompression(wbits):
            wbits = wbits if wbits < 0 else -wbits
            data = _skip_gzip_header(data)
    elif automatic_decompression(wbits):
        # Again, wbits is > 16 and contains +32, so:
        data2 = _skip_gzip_header(data)
        # check to see if we actually had a gzip header
        if data2 != data:
            # then set the wbits to - for raw mode
            data = data2
            wbits = wbits if wbits < 0 else -wbits
        else:
            wbits = (wbits - 32) if wbits > 0 else (wbits + 32)
    # print("wbits: ", wbits)
    inflater = _JZLib.Inflater(wbits < 0)

    def __enter__():
        inflater.setInput(data)
        rval = _get_inflate_data(inflater)
        if not inflater.finished():
            return Error("Error -5 while decompressing data: incomplete or truncated stream")
        return rval

    def __exit__(rval):
        inflater.end()
        return rval

    result = Result.try_(__enter__).finally_(__exit__).build()
    return result.unwrap()


def compressobj(level=6, method=DEFLATED, wbits=MAX_WBITS, memLevel=0, strategy=0, zdict=None):

    self = larky.mutablestruct(__name__='compressobj', __class__=compressobj)

    # See http://www.gzip.org/zlib/rfc-gzip.html
    self.GZIP_HEADER = b''.join([
        b'\x1f\x8b',             # magic header
        b'\x08',                 # compression method
        b'\x00',                 # flags (none)
        b'\x00\x00\x00\x00',     # packed time use zero (leak timezone info)
        # https://tools.ietf.org/html/rfc1952#page-5
        b'\x00',                 # XFL: will be either 0, 2, or 4
                                 # 0 - default, intermediate algo (-2..-8 opts)
                                 # max compression, slowest algo (-9, --best)
                                 # 4 - compressor, fastest algo (-1, --fast)
        # https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8244706
        b'\xff',                 # OS: unknown
    ])

    # NB: this format is little-endian, not big-endian as we might
    # expect for network oriented protocols. Both are 4 bytes unsigned
    # modulus 2^32 per RFC-1952. CRC32.getValue() returns an unsigned
    # int as a long, so cope accordingly.
    self.GZIP_TRAILER_FORMAT = "<II"  # crc32, size

    def __init__(level, method, wbits, memLevel, strategy, zdict):
        self._gzip = gzip_compression(wbits)
        if self._gzip:
            wbits = (wbits - 16) if wbits > 0 else (wbits + 16)
        if abs(wbits) > MAX_WBITS or abs(wbits) < 8:
            return Error("ValueError: Invalid initialization option: %s" % (wbits,)).unwrap()
        self.deflater = _JZLib.Deflater(level, wbits < 0 or self._gzip)
        self.deflater.setStrategy(strategy)
        if zdict:
            self.deflater.setDictionary(zdict)
        self._ended = False
        self._size = 0
        self._crc32 = _JZLib.CRC32()
        return self
    self = __init__(level, method, wbits, memLevel, strategy, zdict)

    def compress(data):
        if self._ended:
            return Error("error: compressobj may not be used after flush(Z_FINISH)").unwrap()
        self.deflater.setInput(data, 0, len(data))
        deflated = _get_deflate_data(self.deflater, Z_NO_FLUSH)
        self._size += len(data)
        self._crc32.update(data)
        if self._gzip:
            return self.GZIP_HEADER + deflated
        else:
            return deflated
    self.compress = compress

    def flush(mode=Z_FINISH):
        if self._ended:
            return Error("error: compressobj may not be used after flush(Z_FINISH)").unwrap()
        if mode not in _valid_flush_modes:
            return Error("ValueError: Invalid flush option: %s" % (mode,)).unwrap()
        if mode == Z_FINISH:
            self.deflater.finish()

        last = _get_deflate_data(self.deflater, mode)

        if mode == Z_FINISH:
            if self._gzip:
                last += struct.pack(self.GZIP_TRAILER_FORMAT, self._crc32.getValue(), self._size & _MASK32)
            self.deflater.end()
            self._ended = True
        return last

    self.flush = flush
    return self


def decompressobj(wbits=MAX_WBITS, zdict=None):
    self = larky.mutablestruct(__name__='decompressobj', __class__=decompressobj)

    def __init__(wbits, zdict):

        # wbits stands for `windowBits` and the java.util.zip API provides
        # some access to zlib, though it is limited.
        #
        # Due to the interface limitations, you cannot request that zlib
        # respect the wbits parameter.
        #
        # We only use it as a way set `nowrap` to true, but apparently CPython
        # has some tests around this so to maintain compatibility, we have to
        # do some bogus param checking
        if abs(wbits) < 8:
            return Error("ValueError: Invalid initialization option").unwrap()
        if abs(wbits) > 16: # XX: apparently wbits > 16 = negative in CPython..
            wbits = -1

        self.inflater = _JZLib.Inflater(wbits < 0)
        self._ended = False
        self.unused_data = b""
        self.unconsumed_tail = b""
        self.gzip = wbits < 0
        self.gzip_header_skipped = False
        if zdict:
            self.inflater.setDictionary(zdict)
        self._crc32 = _JZLib.CRC32()
        return self
    self = __init__(wbits, zdict)

    def _eof():
        return self.inflater.finished()
    self.eof = larky.property(_eof)

    def decompress(data, max_length=0):
        if max_length < 0:
            return Error("ValueError: max_length must be a positive integer").unwrap()
        elif max_length > LARKY_MAX_BUFSIZE:
            return Error("OverflowError: int too large " +
                         "(bufsize: '%d'), max is: %d" %
                         (max_length, LARKY_MAX_BUFSIZE)).unwrap()
        if self._ended:
            return Error("error: decompressobj may not be used after flush()").unwrap()
        # unused_data is always "" until inflation is finished; then it is
        # the unused bytes of the input;
        # unconsumed_tail is whatever input was not used because max_length
        # was exceeded before inflation finished.
        # Thus, at most one of {unused_data, unconsumed_tail} may be non-empty.
        self.unconsumed_tail = b""
        if not self.inflater.finished() and not (self.gzip and not self.gzip_header_skipped):
            self.unused_data = b""

        if max_length < 0:
            return Error("ValueError: max_length must be a positive integer").unwrap()

        # Suppress gzip header if present and wbits < 0
        if self.gzip and not self.gzip_header_skipped:
            data = self.unused_data + data
            self.unused_data = b""
            data = _skip_gzip_header(data)
            # # need more input!
            # self.unused_data = data
            # return ""
            self.gzip_header_skipped = True

        self.inflater.setInput(data)
        inflated = _get_inflate_data(self.inflater, max_length)
        self._crc32.update(inflated)

        if self.inflater.needs_dictionary():
            fail("error: Error 2 while decompressing data")

        r = self.inflater.getRemaining()
        if r:
            if self.gzip and self.inflater.finished() and r == 8:
                # Consume tail, check inflate size, and crc32
                crc,isize = struct.unpack_from("<LL", data[-r:])
                mysize = self.inflater.getBytesWritten() & _MASK32
                mycrc = self._crc32.getValue() & _MASK32
                if mysize != isize:
                    return Error("error: Error -3 while decompressing data: incorrect length check").unwrap()
                if mycrc != crc:
                    return Error("error: Error -3 while decompressing data: incorrect data check").unwrap()
            elif max_length and not self.inflater.finished():
                self.unconsumed_tail = data[-r:]
            else:
                self.unused_data += data[-r:]

        return inflated
    self.decompress = decompress

    def flush(length=None):
        if self._ended:
            return Error("error: decompressobj may not be used after flush()").unwrap()
        if length == None:
            length = 0
        elif length <= 0:
            return Error("ValueError: length must be greater than zero").unwrap()
        elif length > LARKY_MAX_BUFSIZE:
            return Error("OverflowError: int too large " +
                         "(bufsize: '%d'), max is: %d" %
                         (length, LARKY_MAX_BUFSIZE)).unwrap()

        # most common case, flush with no max buffer size length
        if length == None:
            item = _get_inflate_data(self.inflater, length)
            self.inflater.end()
            return item

        # TODO: is this necessary (this might make sense in CPython, but
        #       do we need max buffer size in managed languages like Java?
        last = []
        iteration_limit_reached = False
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                iteration_limit_reached = True
            if self.inflater.finished() or self.inflater.getRemaining() <= 0:
                break
            item = _get_inflate_data(self.inflater, length)
            last.append(item)
        if iteration_limit_reached and not self.inflater.finished() and self.inflater.getRemaining() > 0:
            fail("Iteration limit exceeded: inflate flush operation too complex, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)
        self.inflater.end()
        return b"".join(last)
    self.flush = flush
    return self


def _get_deflate_data(deflater, mode=Z_NO_FLUSH):
    buflen = 1024
    data = bytearray()
    buf = bytearray(b"\x00" * buflen)
    iteration_limit_reached = False
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
            iteration_limit_reached = True
        if deflater.finished():
            break
        l = deflater.deflate(buf, _zlib_to_deflater.get(mode, _JZLib.NO_FLUSH))
        if l == 0:
            break
        data.extend(buf[0:l])
    if iteration_limit_reached and not deflater.finished():
        fail("Iteration limit exceeded: deflate operation too complex, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)
    buf.clear()
    return bytes(data)


def _get_inflate_data(inflater, max_length=0):
    buf = bytearray(b" " * 1024)
    data = bytearray()
    total = 0
    iteration_limit_reached = False

    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
            iteration_limit_reached = True
        if inflater.finished():
            break

        if max_length:
            l = inflater.inflate(buf, 0, min(1024, max_length - total))
        else:
            l = inflater.inflate(buf)
        # print("_get_inflate_data(): inflated: ", l)
        if l == 0:
            break

        total += l
        # print("_get_inflate_data(): total inflated so far: ", total)
        data.extend(buf[0:l])
        if max_length and total == max_length:
            break
    if iteration_limit_reached and not inflater.finished() and (not max_length or total < max_length):
        fail("Iteration limit exceeded: inflate operation too complex, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)
    buf.clear()
    return bytes(data)



FTEXT = 1
FHCRC = 2
FEXTRA = 4
FNAME = 8
FCOMMENT = 16

def _skip_gzip_header(data):
    # per format specified in http://tools.ietf.org/html/rfc1952
    s = data
    if not types.is_bytelike(data):
        s = bytearray(codecs.encode(data, encoding='utf-8'))

    id1 = s[0]
    id2 = s[1]

    # Check gzip magic
    if id1 != 31 or id2 != 139:
        return data

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

    return bytes(s)


zlib = larky.struct(
    DEFLATED=DEFLATED,
    DEF_BUF_SIZE=DEF_BUF_SIZE,
    DEF_MEM_LEVEL=DEF_MEM_LEVEL,
    MAX_WBITS=MAX_WBITS,
    ZLIB_RUNTIME_VERSION=ZLIB_RUNTIME_VERSION,
    ZLIB_VERSION=ZLIB_VERSION,
    Z_BEST_COMPRESSION=Z_BEST_COMPRESSION,
    Z_BEST_SPEED=Z_BEST_SPEED,
    Z_BLOCK=Z_BLOCK,
    Z_DEFAULT_COMPRESSION=Z_DEFAULT_COMPRESSION,
    Z_DEFAULT_STRATEGY=Z_DEFAULT_STRATEGY,
    Z_FILTERED=Z_FILTERED,
    Z_FINISH=Z_FINISH,
    Z_FIXED=Z_FIXED,
    Z_FULL_FLUSH=Z_FULL_FLUSH,
    Z_HUFFMAN_ONLY=Z_HUFFMAN_ONLY,
    Z_NO_COMPRESSION=Z_NO_COMPRESSION,
    Z_NO_FLUSH=Z_NO_FLUSH,
    Z_PARTIAL_FLUSH=Z_PARTIAL_FLUSH,
    Z_RLE=Z_RLE,
    Z_SYNC_FLUSH=Z_SYNC_FLUSH,
    Z_TREES=Z_TREES,
    FCOMMENT=FCOMMENT,
    FEXTRA=FEXTRA,
    FHCRC=FHCRC,
    FNAME=FNAME,
    FTEXT=FTEXT,
    adler32=adler32,
    compress=compress,
    compressobj=compressobj,
    crc32=crc32,
    decompress=decompress,
    decompressobj=decompressobj,
)