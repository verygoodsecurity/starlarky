load("@stdlib//binascii", binascii="binascii")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//json", "json")
load("@stdlib//io", io="io")
load("@stdlib//zlib", zlib="zlib")
load("@stdlib//larky",
     WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION",
     larky="larky")
load("@stdlib//random", random="random")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", Result="Result", Error="Error")

# Some handy shorthands. Note that these are used for byte-limits as well
# as size-limits, in the various bigmem tests
_1M = 1024 * 1024
_1G = 1024 * _1M
_2G = 2 * _1G
_4G = 4 * _1G


def VersionTestCase_test_library_version():
    # Test that the major version of the actual library in use matches the
    # major version that we were compiled against. We can't guarantee that
    # the minor versions will match (even on the machine on which the module
    # was compiled), and the API is stable between minor versions, so
    # testing only the major versions avoids spurious failures.
    asserts.assert_that(zlib.ZLIB_RUNTIME_VERSION[0]).is_equal_to(
        zlib.ZLIB_VERSION[0])


# checksum test cases
def ChecksumTestCase_test_crc32start():
    asserts.assert_that(zlib.crc32(b"")).is_equal_to(zlib.crc32(b"", 0))
    asserts.assert_that(zlib.crc32(b"abc", 0xffffffff)).is_true()


def ChecksumTestCase_test_crc32empty():
    asserts.assert_that(zlib.crc32(b"", 0)).is_equal_to(0)
    asserts.assert_that(zlib.crc32(b"", 1)).is_equal_to(1)
    asserts.assert_that(zlib.crc32(b"", 432)).is_equal_to(432)


def ChecksumTestCase_test_adler32start():
    asserts.assert_that(zlib.adler32(b"")).is_equal_to(zlib.adler32(b"", 1))
    asserts.assert_that(zlib.adler32(b"abc", 0xffffffff)).is_true()


def ChecksumTestCase_test_adler32empty():
    asserts.assert_that(zlib.adler32(b"", 0)).is_equal_to(0)
    asserts.assert_that(zlib.adler32(b"", 1)).is_equal_to(1)
    asserts.assert_that(zlib.adler32(b"", 432)).is_equal_to(432)


def ChecksumTestCase_test_penguins():
    asserts.assert_that(zlib.crc32(b"penguin", 0)).is_equal_to(0x0e5c1a120)
    asserts.assert_that(zlib.crc32(b"penguin", 1)).is_equal_to(0x43b6aa94)
    asserts.assert_that(zlib.adler32(b"penguin", 0)).is_equal_to(0x0bcf02f6)
    asserts.assert_that(zlib.adler32(b"penguin", 1)).is_equal_to(0x0bd602f7)

    asserts.assert_that(zlib.crc32(b"penguin")).is_equal_to(
        zlib.crc32(b"penguin", 0))
    asserts.assert_that(zlib.adler32(b"penguin")).is_equal_to(
        zlib.adler32(b"penguin", 1))


def ChecksumTestCase_test_crc32_adler32_unsigned():
    foo = b'abcdefghijklmnop'
    # explicitly test signed behavior
    asserts.assert_that(zlib.crc32(foo)).is_equal_to(2486878355)
    asserts.assert_that(zlib.crc32(b'spam')).is_equal_to(1138425661)
    asserts.assert_that(zlib.adler32(foo + foo)).is_equal_to(3573550353)
    asserts.assert_that(zlib.adler32(b'spam')).is_equal_to(72286642)


def ChecksumTestCase_test_same_as_binascii_crc32():
    foo = b'abcdefghijklmnop'
    crc = 2486878355
    asserts.assert_that(binascii.crc32(foo)).is_equal_to(crc)
    asserts.assert_that(zlib.crc32(foo)).is_equal_to(crc)
    asserts.assert_that(binascii.crc32(b'spam')).is_equal_to(
        zlib.crc32(b'spam'))


# make sure we generate some expected errors
def ExceptionTestCase_test_badlevel():
    # specifying compression level out of range causes an error
    # (but -1 is Z_DEFAULT_COMPRESSION and apparently the zlib
    # accepts 0 too)
    asserts.assert_fails(lambda: zlib.compress(b'ERROR', 10), ".*?")


def ExceptionTestCase_test_badargs():
    asserts.assert_fails(lambda: zlib.adler32(),
                         ".*?missing 1 required positional argument")
    asserts.assert_fails(lambda: zlib.crc32(),
                         ".*?missing 1 required positional argument")
    asserts.assert_fails(lambda: zlib.compress(),
                         ".*?missing 1 required positional argument")
    asserts.assert_fails(lambda: zlib.decompress(),
                         ".*?missing 1 required positional argument")
    for arg in (42, None, '', 'abc', (), []):
        asserts.assert_fails(lambda: zlib.adler32(arg),
                             r".*?TypeError: a bytes-like object is required, not '\w+'")
        asserts.assert_fails(lambda: zlib.crc32(arg),
                             r".*?TypeError: a bytes-like object is required, not '\w+'")
        asserts.assert_fails(lambda: zlib.compress(arg),
                             r".*?TypeError: a bytes-like object is required, not '\w+'")
        asserts.assert_fails(lambda: zlib.decompress(arg),
                             r".*?TypeError: a bytes-like object is required, not '\w+'")


def ExceptionTestCase_test_badcompressobj():
    """
    In [29]: zlib.compressobj(1, zlib.DEFLATED, 0)
    ---------------------------------------------------------------------------
    ValueError                                Traceback (most recent call last)
    <ipython-input-29-c23e3750c18e> in <module>
    ----> 1 zlib.compressobj(1, zlib.DEFLATED, 0)

    ValueError: Invalid initialization option
    """
    # verify failure on building compress object with bad params
    asserts.assert_fails(lambda: zlib.compressobj(1, zlib.DEFLATED, 0),
                         ".*?ValueError")
    # specifying total bits too large causes an error
    asserts.assert_fails(
        lambda: zlib.compressobj(1, zlib.DEFLATED, zlib.MAX_WBITS + 1),
        ".*?ValueError")


def ExceptionTestCase_test_baddecompressobj():
    """
    In [31]: zlib.decompressobj(-1)
    ---------------------------------------------------------------------------
    ValueError                                Traceback (most recent call last)
    <ipython-input-31-9968f2f0d9b5> in <module>
    ----> 1 zlib.decompressobj(-1)

    ValueError: Invalid initialization option
    """
    # verify failure on building decompress object with bad params
    asserts.assert_fails(lambda: zlib.decompressobj(-1), ".*?ValueError")


def ExceptionTestCase_test_decompressobj_badflush():
    # verify failure on calling decompressobj.flush with bad params
    asserts.assert_fails(lambda: zlib.decompressobj().flush(0), ".*?ValueError")
    asserts.assert_fails(lambda: zlib.decompressobj().flush(-1),
                         ".*?ValueError")


SYS_MAXSIZE = 2147483647


def ExceptionTestCase_test_overflow():
    def _larky_1364090598():
        zlib.decompress(b'', 15, SYS_MAXSIZE + 1)

    asserts.assert_fails(lambda: _larky_1364090598(),
                         ".*?OverflowError.*int too large")

    def _larky_1185669041():
        zlib.decompressobj().decompress(b'', SYS_MAXSIZE + 1)

    asserts.assert_fails(lambda: _larky_1185669041(),
                         ".*?OverflowError.*int too large")

    def _larky_565197355():
        zlib.decompressobj().flush(SYS_MAXSIZE + 1)

    asserts.assert_fails(lambda: _larky_565197355(),
                         ".*?OverflowError.*int too large")


def _check_big_compress_buffer(size, compress_func):
    _1M = 1024 * 1024
    # Generate 10 MiB worth of random, and expand it by repeating it.
    # The assumption is that zlib's memory is not big enough to exploit
    # such spread out redundancy.
    data = random.randbytes(_1M * 10)
    data = data * (size // len(data) + 1)
    compress_func(data)


def _check_big_decompress_buffer(size, decompress_func):
    data = b'x' * size
    compressed = zlib.compress(data, 1)
    data = decompress_func(compressed)
    # Sanity check
    asserts.assert_that(len(data)).is_equal_to(size)
    asserts.assert_that(len(data.strip(b'x'))).is_equal_to(0)


def _decompinc(flush=False, source=None, cx=256, dcx=64):
    # compress object in steps, decompress object in steps
    source = source or HAMLET_SCENE
    data = source * 128
    co = zlib.compressobj()
    bufs = []
    for i in range(0, len(data), cx):
        bufs.append(co.compress(data[i:i + cx]))
    bufs.append(co.flush())
    combuf = b''.join(bufs)
    decombuf = zlib.decompress(combuf)
    # Test type of return value
    asserts.assert_that(decombuf).is_instance_of(bytes)

    asserts.assert_that(data).is_equal_to(decombuf)

    dco = zlib.decompressobj()
    bufs = []
    for i in range(0, len(combuf), dcx):
        bufs.append(dco.decompress(combuf[i:i + dcx]))
        asserts.assert_that(b'').is_equal_to(dco.unconsumed_tail)
        asserts.assert_that(b'').is_equal_to(dco.unused_data)
    if flush:
        bufs.append(dco.flush())
    else:
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not True:
                break
            chunk = dco.decompress(b'')
            if chunk:
                bufs.append(chunk)
            else:
                break
    asserts.assert_that(b'').is_equal_to(dco.unconsumed_tail)
    asserts.assert_that(b'').is_equal_to(dco.unused_data)
    asserts.assert_that(data).is_equal_to(b''.join(bufs))
    # Failure means: "decompressobj with init options failed"


def _decompimax(source=None, cx=256, dcx=64):
    # compress in steps, decompress in length-restricted steps
    source = source or HAMLET_SCENE
    # Check a decompression object with max_length specified
    data = source * 128
    co = zlib.compressobj()
    bufs = []
    for i in range(0, len(data), cx):
        bufs.append(co.compress(data[i:i + cx]))
    bufs.append(co.flush())
    combuf = b''.join(bufs)
    asserts.assert_that(data).is_equal_to(zlib.decompress(combuf))

    dco = zlib.decompressobj()
    bufs = []
    cb = combuf
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not cb:
            break
        # max_length = 1 + len(cb)//10
        chunk = dco.decompress(cb, dcx)
        asserts.assert_that(len(chunk) > dcx).is_false()
        bufs.append(chunk)
        cb = dco.unconsumed_tail
    bufs.append(dco.flush())
    asserts.assert_that(data).is_equal_to(b''.join(bufs))


def _decompressmaxlen(flush=False):
    # Check a decompression object with max_length specified
    data = HAMLET_SCENE * 128
    co = zlib.compressobj()
    bufs = []
    for i in range(0, len(data), 256):
        bufs.append(co.compress(data[i:i + 256]))
    bufs.append(co.flush())
    combuf = b''.join(bufs)
    asserts.assert_that(data).is_equal_to(zlib.decompress(combuf))

    dco = zlib.decompressobj()
    bufs = []
    cb = combuf
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not cb:
            break
        max_length = 1 + len(cb) // 10
        chunk = dco.decompress(cb, max_length)
        asserts.assert_that(len(chunk) > max_length).is_false()
        bufs.append(chunk)
        cb = dco.unconsumed_tail
    if flush:
        bufs.append(dco.flush())
    else:
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if not chunk:
                break
            chunk = dco.decompress(b'', max_length)
            asserts.assert_that(len(chunk) > max_length).is_false()
            bufs.append(chunk)
    asserts.assert_that(data).is_equal_to(b''.join(bufs))


# Test compression in one go (whole message compression)
def CompressTestCase_test_speech():
    x = zlib.compress(HAMLET_SCENE)
    asserts.assert_that(zlib.decompress(x)).is_equal_to(HAMLET_SCENE)


def CompressTestCase_test_keywords():
    x = zlib.compress(HAMLET_SCENE, level=3)
    asserts.assert_that(zlib.decompress(x)).is_equal_to(HAMLET_SCENE)
    asserts.assert_that(
        zlib.decompress(x, wbits=zlib.MAX_WBITS, bufsize=zlib.DEF_BUF_SIZE)
    ).is_equal_to(HAMLET_SCENE)


def CompressTestCase_test_speech128():
    # compress more data
    data = HAMLET_SCENE * 128
    x = zlib.compress(data)
    asserts.assert_that(zlib.compress(bytearray(data))).is_equal_to(x)
    # asserts.assert_that(len(zlib.decompress(x))).is_equal_to(len(data))
    # asserts.assert_that(len(zlib.decompress(bytearray(x)))).is_equal_to(len(data))
    for ob in x, bytearray(x):
        asserts.assert_that(zlib.decompress(ob)).is_equal_to(data)


def CompressTestCase_test_incomplete_stream():
    # A useful error message is given
    x = zlib.compress(HAMLET_SCENE)
    asserts.assert_fails(lambda: zlib.decompress(x[:-1]),
                         ".*?.*Error -5 while decompressing data: incomplete or truncated stream")


# Memory use of the following functions takes into account overallocation

def CompressTestCase_test_big_compress_buffer(size=(_1G + 1024 * 1024)):
    compress = lambda s: zlib.compress(s, 1)
    asserts.assert_fails(lambda: _check_big_compress_buffer(size, compress),
                         ".*excessive repeat")


def CompressTestCase_test_big_decompress_buffer(size=(_1G + 1024 * 1024)):
    asserts.assert_fails(
        lambda: _check_big_decompress_buffer(size, zlib.decompress),
        ".*excessive repeat")


def CompressTestCase_test_large_bufsize(size=_4G):
    # Test decompress(bufsize) parameter greater than the internal limit
    data = HAMLET_SCENE * 10
    compressed = zlib.compress(data, 1)
    asserts.assert_fails(lambda: zlib.decompress(compressed, 15, size),
                         ".*OverflowError: int too large")


# Test compression object
def CompressObjectTestCase_test_pair():
    # straightforward compress/decompress objects
    datasrc = HAMLET_SCENE * 128
    datazip = zlib.compress(datasrc)
    # should compress both bytes and bytearray data
    for data in (datasrc, bytearray(datasrc)):
        co = zlib.compressobj()
        x1 = co.compress(data)
        x2 = co.flush()
        asserts.assert_fails(lambda: co.flush(),
                             ".*?")  # second flush should not work
        asserts.assert_that(x1 + x2).is_equal_to(datazip)
    for v1, v2 in ((x1, x2), (bytearray(x1), bytearray(x2))):
        dco = zlib.decompressobj()
        y1 = dco.decompress(v1 + v2)
        y2 = dco.flush()
        asserts.assert_that(data).is_equal_to(y1 + y2)
        asserts.assert_that(dco.unconsumed_tail).is_instance_of(bytes)
        asserts.assert_that(dco.unused_data).is_instance_of(bytes)


def CompressObjectTestCase_test_keywords():
    level = 2
    method = zlib.DEFLATED
    wbits = -12
    memLevel = 9
    strategy = zlib.Z_FILTERED
    co = zlib.compressobj(level=level,
                          method=method,
                          wbits=wbits,
                          memLevel=memLevel,
                          strategy=strategy,
                          zdict=b"")
    do = zlib.decompressobj(wbits=wbits, zdict=b"")
    # we do not support positional argument / * in starlark
    # asserts.assert_fails(lambda: co.compress(data=HAMLET_SCENE), ".*?TypeError")
    # asserts.assert_fails(lambda: do.decompress(data=zlib.compress(HAMLET_SCENE)), ".*?TypeError")
    x = co.compress(HAMLET_SCENE) + co.flush()
    y = do.decompress(x, max_length=len(HAMLET_SCENE)) + do.flush()
    asserts.assert_that(HAMLET_SCENE).is_equal_to(y)


def CompressObjectTestCase_test_compressoptions():
    # specify lots of options to compressobj()
    level = 2
    method = zlib.DEFLATED
    wbits = -12
    memLevel = 9
    strategy = zlib.Z_FILTERED
    co = zlib.compressobj(level, method, wbits, memLevel, strategy)
    x1 = co.compress(HAMLET_SCENE)
    x2 = co.flush()
    dco = zlib.decompressobj(wbits)
    y1 = dco.decompress(x1 + x2)
    y2 = dco.flush()
    asserts.assert_that(HAMLET_SCENE).is_equal_to(y1 + y2)


def CompressObjectTestCase_test_compressincremental():
    # compress object in steps, decompress object as one-shot
    data = HAMLET_SCENE * 128
    co = zlib.compressobj()
    bufs = []
    for i in range(0, len(data), 256):
        bufs.append(co.compress(data[i:i + 256]))
    bufs.append(co.flush())
    combuf = b''.join(bufs)

    dco = zlib.decompressobj()
    y1 = dco.decompress(b''.join(bufs))
    y2 = dco.flush()
    asserts.assert_that(data).is_equal_to(y1 + y2)


def CompressObjectTestCase_test_decompincflush():
    _decompinc(flush=True)


def CompressObjectTestCase_test_decompressmaxlenflush():
    _decompressmaxlen(flush=True)


def CompressObjectTestCase_test_maxlenmisc():
    # Misc tests of max_length
    dco = zlib.decompressobj()
    asserts.assert_fails(lambda: dco.decompress(b"", -1), ".*?ValueError")
    asserts.assert_that(b'').is_equal_to(dco.unconsumed_tail)


def CompressObjectTestCase_test_maxlen_large():
    # Sizes up to SYS_MAXSIZE should be accepted, although zlib is
    # internally limited to expressing sizes with unsigned int
    data = HAMLET_SCENE * 10
    asserts.assert_that(len(data)).is_greater_than(zlib.DEF_BUF_SIZE)
    compressed = zlib.compress(data, 1)
    dco = zlib.decompressobj()
    asserts.assert_fails(
        lambda: dco.decompress(compressed, SYS_MAXSIZE),
        r".*OverflowError.*int too large")


def CompressObjectTestCase_test_maxlen():
    data = HAMLET_SCENE * 10
    compressed = zlib.compress(data, 1)
    dco = zlib.decompressobj()
    asserts.assert_that(dco.decompress(compressed, 100)).is_equal_to(data[:100])


def CompressObjectTestCase_test_clear_unconsumed_tail():
    # Issue #12050: calling decompress() without providing max_length
    # should clear the unconsumed_tail attribute.
    cdata = b"x\x9cKLJ\x06\x00\x02M\x01"  # "abc"
    dco = zlib.decompressobj()
    ddata = dco.decompress(cdata, 1)
    ddata += dco.decompress(dco.unconsumed_tail)
    asserts.assert_that(dco.unconsumed_tail).is_equal_to(b"")


def CompressObjectTestCase_test_flushes():
    # Test flush() with the various options, using all the
    # different levels in order to provide more variations.
    sync_opt = [
        'Z_NO_FLUSH', 'Z_SYNC_FLUSH', 'Z_FULL_FLUSH',
        ##: LARKY::DIFFERENCE
        ##:  We do not support anything except the following flush modes
        ##:  Everything else will throw an error about the flush options
        # 'Z_PARTIAL_FLUSH'
    ]

    ver = tuple([int(v.decode('iso-8859-1'))
                 for v in zlib.ZLIB_RUNTIME_VERSION.split(b'.')])
    # Z_BLOCK has a known failure prior to 1.2.5.3
    ##: LARKY::DIFFERENCE
    ##:  We do not support Z_BLOCK
    if ver >= (1, 2, 5, 3):
        sync_opt.append('Z_BLOCK')

    sync_opt = [getattr(zlib, opt) for opt in sync_opt if hasattr(zlib, opt)]
    data = HAMLET_SCENE * 8
    for sync in sync_opt:
        for level in range(10):
            obj = zlib.compressobj(level)
            a = obj.compress(data[:3000])
            b = obj.flush(sync)
            c = obj.compress(data[3000:])
            d = obj.flush()
            asserts.assert_that(
                zlib.decompress(b''.join([a, b, c, d]))
            ).is_equal_to(data)


def CompressObjectTestCase_test_odd_flush():
    # Testing on 17K of "random" data
    # Create compressor and decompressor objects
    co = zlib.compressobj(zlib.Z_BEST_COMPRESSION)
    dco = zlib.decompressobj()

    # Try 17K of data
    # generate random data stream
    gen = random
    data = gen.randbytes(17 * 1024)

    # compress, sync-flush, and decompress
    first = co.compress(data)
    second = co.flush(zlib.Z_SYNC_FLUSH)
    expanded = dco.decompress(first + second)

    # if decompressed data is different from the input data, choke.
    asserts.assert_that(expanded).is_equal_to(data)


def CompressObjectTestCase_test_empty_flush():
    # Test that calling .flush() on unused objects works.
    # (Bug #1083110 -- calling .flush() on decompress objects
    # caused a core dump.)

    co = zlib.compressobj(zlib.Z_BEST_COMPRESSION)
    asserts.assert_that(co.flush()).is_true()  # Returns a zlib header
    dco = zlib.decompressobj()
    asserts.assert_that(dco.flush()).is_equal_to(b"")  # Returns nothing


def CompressObjectTestCase_test_dictionary():
    h = HAMLET_SCENE
    # Build a simulated dictionary out of the words in HAMLET.
    words = h.split()
    random.shuffle(words)
    zdict = b''.join(words)
    # Use it to compress HAMLET.
    co = zlib.compressobj(zdict=zdict)
    cd = co.compress(h) + co.flush()
    # Verify that it will decompress with the dictionary.
    dco = zlib.decompressobj(zdict=zdict)
    asserts.assert_that(dco.decompress(cd) + dco.flush()).is_equal_to(h)
    # Verify that it fails when not given the dictionary.
    dco = zlib.decompressobj()
    asserts.assert_fails(lambda: dco.decompress(cd), ".*?")


def CompressObjectTestCase_test_dictionary_streaming():
    # This simulates the reuse of a compressor object for compressing
    # several separate data streams.
    co = zlib.compressobj(zdict=HAMLET_SCENE)
    do = zlib.decompressobj(zdict=HAMLET_SCENE)
    piece = HAMLET_SCENE[1000:1500]
    d0 = co.compress(piece) + co.flush(zlib.Z_SYNC_FLUSH)
    d1 = co.compress(piece[100:]) + co.flush(zlib.Z_SYNC_FLUSH)
    d2 = co.compress(piece[:-100]) + co.flush(zlib.Z_SYNC_FLUSH)
    asserts.assert_that(do.decompress(d0)).is_equal_to(piece)
    asserts.assert_that(do.decompress(d1)).is_equal_to(piece[100:])
    asserts.assert_that(do.decompress(d2)).is_equal_to(piece[:-100])


def CompressObjectTestCase_test_decompress_incomplete_stream():
    # This is 'foo', deflated
    x = b'x\x9cK\xcb\xcf\x07\x00\x02\x82\x01E'
    # For the record
    asserts.assert_that(zlib.decompress(x)).is_equal_to(b'foo')
    asserts.assert_fails(lambda: zlib.decompress(x[:-5]), ".*?")
    # Omitting the stream end works with decompressor objects
    # (see issue #8672).
    dco = zlib.decompressobj()
    y = dco.decompress(x[:-5])
    y += dco.flush()
    asserts.assert_that(y).is_equal_to(b'foo')


def CompressObjectTestCase_test_decompress_eof():
    x = b'x\x9cK\xcb\xcf\x07\x00\x02\x82\x01E'  # 'foo'
    dco = zlib.decompressobj()
    asserts.assert_that(dco.eof).is_false()
    dco.decompress(x[:-5])
    asserts.assert_that(dco.eof).is_false()
    dco.decompress(x[-5:])
    asserts.assert_that(dco.eof).is_true()
    dco.flush()
    asserts.assert_that(dco.eof).is_true()


def CompressObjectTestCase_test_decompress_eof_incomplete_stream():
    x = b'x\x9cK\xcb\xcf\x07\x00\x02\x82\x01E'  # 'foo'
    dco = zlib.decompressobj()
    asserts.assert_that(dco.eof).is_false()
    dco.decompress(x[:-5])
    asserts.assert_that(dco.eof).is_false()
    dco.flush()
    asserts.assert_that(dco.eof).is_false()


def CompressObjectTestCase_test_decompress_unused_data():
    # Repeated calls to decompress() after EOF should accumulate data in
    # dco.unused_data, instead of just storing the arg to the last call.
    source = b'abcdefghijklmnopqrstuvwxyz'
    remainder = b'0123456789'
    y = zlib.compress(source)
    x = y + remainder
    for maxlen in 0, 1000:
        for step in 1, 2, len(y), len(x):
            dco = zlib.decompressobj()
            data = b''
            for i in range(0, len(x), step):
                if i < len(y):
                    asserts.assert_that(dco.unused_data).is_equal_to(b'')
                if maxlen == 0:
                    data += dco.decompress(x[i: i + step])
                    asserts.assert_that(dco.unconsumed_tail).is_equal_to(b'')
                else:
                    data += dco.decompress(
                        dco.unconsumed_tail + x[i: i + step], maxlen)
            data += dco.flush()
            asserts.assert_that(dco.eof).is_true()
            asserts.assert_that(data).is_equal_to(source)
            asserts.assert_that(dco.unconsumed_tail).is_equal_to(b'')
            asserts.assert_that(dco.unused_data).is_equal_to(remainder)


# issue27164
def CompressObjectTestCase_test_decompress_raw_with_dictionary():
    zdict = b'abcdefghijklmnopqrstuvwxyz'
    co = zlib.compressobj(wbits=-zlib.MAX_WBITS, zdict=zdict)
    comp = co.compress(zdict) + co.flush()
    dco = zlib.decompressobj(wbits=-zlib.MAX_WBITS, zdict=zdict)
    uncomp = dco.decompress(comp) + dco.flush()
    asserts.assert_that(zdict).is_equal_to(uncomp)


def CompressObjectTestCase_test_flush_with_freed_input():
    # Issue #16411: decompressor accesses input to last decompress() call
    # in flush(), even if this object has been freed in the meanwhile.
    input1 = b'abcdefghijklmnopqrstuvwxyz'
    input2 = b'QWERTYUIOPASDFGHJKLZXCVBNM'
    data = zlib.compress(input1)
    dco = zlib.decompressobj()
    dco.decompress(data, 1)
    data = zlib.compress(input2)
    asserts.assert_that(dco.flush()).is_equal_to(input1[1:])


def CompressObjectTestCase_test_flush_custom_length():
    input = HAMLET_SCENE * 10
    data = zlib.compress(input, 1)
    dco = zlib.decompressobj()
    dco.decompress(data, 1)
    asserts.assert_that(dco.flush(100)).is_equal_to(input[1:])


def CompressObjectTestCase_test_compresscopy():
    # Test copying a compression object
    data0 = HAMLET_SCENE
    data1 = builtins.bytes(HAMLET_SCENE.swapcase().decode("ISO_8891_1"),
                           encoding="ISO_8891_1")
    for func in (lambda c: c.copy(),):
        c0 = zlib.compressobj(zlib.Z_BEST_COMPRESSION)
        bufs0 = []
        bufs0.append(c0.compress(data0))

        c1 = func(c0)
        bufs1 = bufs0[:]

        bufs0.append(c0.compress(data0))
        bufs0.append(c0.flush())
        s0 = b''.join(bufs0)

        bufs1.append(c1.compress(data1))
        bufs1.append(c1.flush())
        s1 = b''.join(bufs1)

        asserts.assert_that(zlib.decompress(s0)).is_equal_to(data0 + data0)
        asserts.assert_that(zlib.decompress(s1)).is_equal_to(data0 + data1)


def CompressObjectTestCase_test_badcompresscopy():
    # Test copying a compression object in an inconsistent state
    c = zlib.compressobj()
    c.compress(HAMLET_SCENE)
    c.flush()
    asserts.assert_fails(lambda: c.copy(), ".*?ValueError")


def CompressObjectTestCase_test_decompresscopy():
    # Test copying a decompression object
    data = HAMLET_SCENE
    comp = zlib.compress(data)
    # Test type of return value
    asserts.assert_that(comp).is_instance_of(bytes)

    for func in (lambda c: c.copy(),):
        d0 = zlib.decompressobj()
        bufs0 = []
        bufs0.append(d0.decompress(comp[:32]))

        d1 = func(d0)
        bufs1 = bufs0[:]

        bufs0.append(d0.decompress(comp[32:]))
        s0 = b''.join(bufs0)

        bufs1.append(d1.decompress(comp[32:]))
        s1 = b''.join(bufs1)

        asserts.assert_that(s0).is_equal_to(s1)
        asserts.assert_that(s0).is_equal_to(data)


def CompressObjectTestCase_test_baddecompresscopy():
    # Test copying a compression object in an inconsistent state
    data = zlib.compress(HAMLET_SCENE)
    d = zlib.decompressobj()
    d.decompress(data)
    d.flush()
    asserts.assert_fails(lambda: d.copy(), ".*?ValueError")


def CompressObjectTestCase_test_wbits():
    # wbits=0 only supported since zlib v1.2.3.5
    # Register "1.2.3" as "1.2.3.0"
    # or "1.2.0-linux","1.2.0.f","1.2.0.f-linux"
    v = zlib.ZLIB_RUNTIME_VERSION.split(b'-', 1)[0].split(b'.')
    if len(v) < 4:
        v.append(b'0')
    elif not v[-1].isnumeric():
        v[-1] = '0'

    v = tuple([int(x.decode('iso-8859-1')) for x in v])
    supports_wbits_0 = True  # v >= (1, 2, 3, 5)

    co = zlib.compressobj(level=1, wbits=15)
    zlib15 = co.compress(HAMLET_SCENE) + co.flush()
    asserts.assert_that(zlib.decompress(zlib15, 15)).is_equal_to(HAMLET_SCENE)
    if supports_wbits_0:
        asserts.assert_that(zlib.decompress(zlib15, 0)).is_equal_to(
            HAMLET_SCENE)
    asserts.assert_that(zlib.decompress(zlib15, 32 + 15)).is_equal_to(
        HAMLET_SCENE)

    # asserts.assert_fails(lambda: zlib.decompress(zlib15, 14), ".*?.*invalid window size")
    # dco = zlib.decompressobj(wbits=32 + 15)
    # asserts.assert_that(dco.decompress(zlib15)).is_equal_to(HAMLET_SCENE)
    # dco = zlib.decompressobj(wbits=14)
    # asserts.assert_fails(lambda: dco.decompress(zlib15), ".*?.*invalid window size")
    #
    # co = zlib.compressobj(level=1, wbits=9)
    # zlib9 = co.compress(HAMLET_SCENE) + co.flush()
    # asserts.assert_that(zlib.decompress(zlib9, 9)).is_equal_to(HAMLET_SCENE)
    # asserts.assert_that(zlib.decompress(zlib9, 15)).is_equal_to(HAMLET_SCENE)
    # if supports_wbits_0:
    #     asserts.assert_that(zlib.decompress(zlib9, 0)).is_equal_to(HAMLET_SCENE)
    # asserts.assert_that(zlib.decompress(zlib9, 32 + 9)).is_equal_to(HAMLET_SCENE)
    # dco = zlib.decompressobj(wbits=32 + 9)
    # asserts.assert_that(dco.decompress(zlib9)).is_equal_to(HAMLET_SCENE)

    co = zlib.compressobj(level=1, wbits=-15)
    deflate15 = co.compress(HAMLET_SCENE) + co.flush()
    asserts.assert_that(zlib.decompress(deflate15, -15)).is_equal_to(
        HAMLET_SCENE)
    dco = zlib.decompressobj(wbits=-15)
    asserts.assert_that(dco.decompress(deflate15)).is_equal_to(HAMLET_SCENE)

    co = zlib.compressobj(level=1, wbits=-9)
    deflate9 = co.compress(HAMLET_SCENE) + co.flush()
    asserts.assert_that(zlib.decompress(deflate9, -9)).is_equal_to(HAMLET_SCENE)
    asserts.assert_that(zlib.decompress(deflate9, -15)).is_equal_to(
        HAMLET_SCENE)
    dco = zlib.decompressobj(wbits=-9)
    asserts.assert_that(dco.decompress(deflate9)).is_equal_to(HAMLET_SCENE)

    co = zlib.compressobj(level=1, wbits=16 + 15)
    gzip = co.compress(HAMLET_SCENE) + co.flush()
    asserts.assert_that(zlib.decompress(gzip, 16 + 15)).is_equal_to(
        HAMLET_SCENE)
    asserts.assert_that(zlib.decompress(gzip, 32 + 15)).is_equal_to(
        HAMLET_SCENE)
    dco = zlib.decompressobj(32 + 15)
    asserts.assert_that(dco.decompress(gzip)).is_equal_to(HAMLET_SCENE)


def CompressObjectTestCase_choose_lines(source, number, seed=None,
                                        generator=random):
    """Return a list of number lines randomly chosen from the source"""
    if seed != None:
        generator.seed(seed)
    sources = source.split('\n')
    return [generator.choice(sources) for n in range(number)]


HAMLET_SCENE = b"""
LAERTES

       O, fear me not.
       I stay too long: but here my father comes.

       Enter POLONIUS

       A double blessing is a double grace,
       Occasion smiles upon a second leave.

LORD POLONIUS

       Yet here, Laertes! aboard, aboard, for shame!
       The wind sits in the shoulder of your sail,
       And you are stay'd for. There; my blessing with thee!
       And these few precepts in thy memory
       See thou character. Give thy thoughts no tongue,
       Nor any unproportioned thought his act.
       Be thou familiar, but by no means vulgar.
       Those friends thou hast, and their adoption tried,
       Grapple them to thy soul with hoops of steel;
       But do not dull thy palm with entertainment
       Of each new-hatch'd, unfledged comrade. Beware
       Of entrance to a quarrel, but being in,
       Bear't that the opposed may beware of thee.
       Give every man thy ear, but few thy voice;
       Take each man's censure, but reserve thy judgment.
       Costly thy habit as thy purse can buy,
       But not express'd in fancy; rich, not gaudy;
       For the apparel oft proclaims the man,
       And they in France of the best rank and station
       Are of a most select and generous chief in that.
       Neither a borrower nor a lender be;
       For loan oft loses both itself and friend,
       And borrowing dulls the edge of husbandry.
       This above all: to thine ownself be true,
       And it must follow, as the night the day,
       Thou canst not then be false to any man.
       Farewell: my blessing season this in thee!

LAERTES

       Most humbly do I take my leave, my lord.

LORD POLONIUS

       The time invites you; go; your servants tend.

LAERTES

       Farewell, Ophelia; and remember well
       What I have said to you.

OPHELIA

       'Tis in my memory lock'd,
       And you yourself shall keep the key of it.

LAERTES

       Farewell.
"""


def test_gzip_compress_and_decompress():
    compressed = b''.join([
        b'\x1f\x8b\x08\x00\x00\x00\x00\x00\x00\x13\x8dR_O\xc20\x10',
        b'\x7f\xe7S,}$\xac[7p\x8374&\x9a\xf8\xa4h| ',
        b'1\xa5\xbdm5\xa3\xadm1A\xc2w\xb7\x1d\xb8\x00\xc6\xc4\xbe',
        b'\xb4w\xbf?w\xb9\xebn\x10E\x88\x9a\xda\xa2Y\xb4\xdb\x8f',
        b'\xa2\x10r\xea\xa8\x0f\xd1n\x89\x185\xfcMn\xd6+0K4[',
        b'\xa219?K\xb4G\x07Q%Z85\xa9\x94Y\x9f\x84\rP\x0e\xa6#\xf8',
        b'\xd0\'\xe6\x8c\x81v\xa1\xcc0\x19\x1e<\xfal|+\x99\xe2B\xd6',
        b'\x01\xae\xbf\x84\x1eE\x1c\xaa\x96:\xe8\x897JJ`N(',
        b'\x198\xacU\xf6\x0cs ',
        b']\xfc\x00\xb2vM\xc0\xf3\xf1/p\xb1\xd5\x10 ',
        b'\xaau+\x18\rN\xc9\xbb\xf5v?\xc4;e\xbb\xf6\x805\n{',
        b'\x96\xc5\x9f`\xb6\xb5R\x1c\xdb\xadu\xb0\xb6=\xf5\xd9\x82',
        b'\x89\xe7\xb5\xb7\r\x02\xbdu\x8d\x92\xb1\x81\x8f\rXg\x93',
        b'\x0cg\x13Lz\xf2Km\xe3\xc7\x03\x16\xdf\xf3\xae\xbbt\xcc!g',
        b'\xe98#\xd3\x89\x7f\xa5e\xc5\xd2"\xe7\x0c\xd8\xd5t\xc5\xb3',
        b'^\xfa\x1a_\xe7\xf1\x13]\xeb\x16:!\xb9@4\x95\xa2\x03\xb2',
        b'\x15+r\xc2\xf2\x8aU%\x14Eq\xce[',
        b'\x18\xca@\xfc\xaf\xb4\x97\x1d7\xd8\r\xa7_\xdf\xc9\xc7\x08',
        b'>\x97\xff\xe2D\xa7\x8c\xa8E\xb7%R\xe2\x8cL\xf0\xa4\xc4',
        b'\xf9\xd5(")\xce\xfd\\\xfcE\xcac\x83hc\xda@l\x9c\xd3v\x96$',
        b'\x7f\x8f>\xd1a;\x83\xfd\xe0\x1b\x1b\xf9\x06\'\xc0\x02\x00',
        b'\x00',
    ])
    decompressed = zlib.decompress(compressed, zlib.MAX_WBITS | 16)
    asserts.assert_that(json.loads(decompressed.decode('utf-8'))).is_equal_to({
            "args": {},
            "data": "{\"card_number\":\"4111111111111111\"}",
            "files": {}, "form": {},
            "headers": {
                "Accept": "*/*",
                "Accept-Encoding": "gzip, deflate",
                "Connection": "close", "Content-Length": "34",
                "Content-Type": "application/json",
                "Host": "echo.apps.verygood.systems",
                "User-Agent": "python-requests/2.25.1",
                "Vgs-Request-Id": "304de3c042195de308fc073dcec69bd2",
                "X-B3-Sampled": "1",
                "X-B3-Spanid": "2bc731c3fcf8e777",
                "X-B3-Traceid": "304de3c042195de308fc073dcec69bd2"
            },
            "json": {"card_number": "4111111111111111"},
            "origin": "18.215.58.36, 10.35.110.187",
            "url": "https://echo.apps.verygood.systems/post"
    })
    compressor = zlib.compressobj(method=zlib.DEFLATED,
                                  wbits=zlib.MAX_WBITS | 16)
    asserts.assert_that(compressor._gzip).is_true()
    body = compressor.compress(decompressed) + compressor.flush()
    asserts.assert_that(type(body)).is_equal_to('bytes')
    # headers are the same
    asserts.assert_that(body[:8]).is_equal_to(compressed[:8])
    # ignore the XFL + OS flags b/c they can be different
    asserts.assert_that(body[10:]).is_equal_to(compressed[10:])


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(
        unittest.FunctionTestCase(VersionTestCase_test_library_version))
    _suite.addTest(unittest.FunctionTestCase(ChecksumTestCase_test_crc32start))
    _suite.addTest(unittest.FunctionTestCase(ChecksumTestCase_test_crc32empty))
    _suite.addTest(
        unittest.FunctionTestCase(ChecksumTestCase_test_adler32start))
    _suite.addTest(
        unittest.FunctionTestCase(ChecksumTestCase_test_adler32empty))
    _suite.addTest(unittest.FunctionTestCase(ChecksumTestCase_test_penguins))
    _suite.addTest(
        unittest.FunctionTestCase(ChecksumTestCase_test_crc32_adler32_unsigned))
    _suite.addTest(
        unittest.FunctionTestCase(ChecksumTestCase_test_same_as_binascii_crc32))
    _suite.addTest(unittest.FunctionTestCase(ExceptionTestCase_test_badlevel))
    _suite.addTest(unittest.FunctionTestCase(ExceptionTestCase_test_badargs))
    _suite.addTest(
        unittest.FunctionTestCase(ExceptionTestCase_test_badcompressobj))
    _suite.addTest(
        unittest.FunctionTestCase(ExceptionTestCase_test_baddecompressobj))
    _suite.addTest(unittest.FunctionTestCase(
        ExceptionTestCase_test_decompressobj_badflush))
    _suite.addTest(unittest.FunctionTestCase(ExceptionTestCase_test_overflow))
    _suite.addTest(unittest.FunctionTestCase(CompressTestCase_test_speech))
    _suite.addTest(unittest.FunctionTestCase(CompressTestCase_test_keywords))
    _suite.addTest(unittest.FunctionTestCase(CompressTestCase_test_speech128))
    _suite.addTest(
        unittest.FunctionTestCase(CompressTestCase_test_incomplete_stream))
    _suite.addTest(
        unittest.FunctionTestCase(CompressTestCase_test_big_compress_buffer))
    _suite.addTest(
        unittest.FunctionTestCase(CompressTestCase_test_big_decompress_buffer))
    _suite.addTest(
        unittest.FunctionTestCase(CompressTestCase_test_large_bufsize))
    _suite.addTest(unittest.FunctionTestCase(CompressObjectTestCase_test_pair))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_keywords))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_compressoptions))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_compressincremental))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_decompincflush))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_decompressmaxlenflush))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_maxlenmisc))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_maxlen_large))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_maxlen))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_clear_unconsumed_tail))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_flushes))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_odd_flush))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_empty_flush))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_dictionary))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_dictionary_streaming))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_decompress_incomplete_stream))
    _suite.addTest(
        unittest.FunctionTestCase(CompressObjectTestCase_test_decompress_eof))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_decompress_eof_incomplete_stream))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_decompress_unused_data))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_flush_with_freed_input))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_decompress_raw_with_dictionary))
    _suite.addTest(unittest.FunctionTestCase(
        CompressObjectTestCase_test_flush_custom_length))
    _suite.addTest(unittest.FunctionTestCase(CompressObjectTestCase_test_wbits))
    _suite.addTest(unittest.FunctionTestCase(test_gzip_compress_and_decompress))

    # -- is not supported by JDK java.util.Zip interface, so let us not implement
    # _suite.addTest(unittest.FunctionTestCase(CompressObjectTestCase_test_compresscopy))
    # _suite.addTest(unittest.FunctionTestCase(CompressObjectTestCase_test_badcompresscopy))
    # _suite.addTest(unittest.FunctionTestCase(CompressObjectTestCase_test_decompresscopy))
    # _suite.addTest(unittest.FunctionTestCase(CompressObjectTestCase_test_baddecompresscopy))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
