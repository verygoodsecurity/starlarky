load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//larky", larky="larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION")
load("@vendor//ecdsa/_compat", str_idx_as_int="str_idx_as_int")
load("@stdlib//itertools", chain="chain")
load("@stdlib//base64", b64decode="b64decode")
load("@stdlib//codecs", codecs="codecs")

def encode_length(l):
    # assert l >= 0
    if l < 0:
        fail("AssertionError()")
    if l < 0x80:
        return bytes([l])
    # s = ("%x" % l).encode()
    s = codecs.encode("%x" % l, encoding='utf-8')
    if len(s) % 2:
        # In 2.6 or higher, b("") and b"" are equivalent 
        s = b"0" + s
    s = unhexlify(s)
    llen = len(s)
    # return int2byte(0x80 | llen) + s
    return bytes([0x80 | llen]) + s

def encode_number(n):
    b128_digits = []
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not n:
            break
        b128_digits.insert(0, (n & 0x7F) | 0x80)
        n = n >> 7
    if not b128_digits:
        b128_digits.append(0)
    b128_digits[-1] &= 0x7F
    # return b("").join([int2byte(d) for d in b128_digits])
    return b"".join([bytes([d]) for d in b128_digits])

def encode_oid(first, second, *pieces):
    # assert 0 <= first < 2 and 0 <= second <= 39 or first == 2 and 0 <= second
    if (first< 0 or first >=2 or second < 0 or second > 39) and not (first == 2 and 0 <= second):
        fail('AssertionError()')
    body = b"".join(
        chain(
            [encode_number(40 * first + second)],
            # (encode_number(p) for p in pieces),
            [encode_number(p) for p in pieces],
        )
    )
    return b"\x06" + encode_length(len(body)) + body

def is_sequence(string):
    return string and string[:1] == b"\x30"

def read_length(string):
    if not string:
        fail('UnexpectedDER("Empty string can\'t encode valid length value")')
    num = str_idx_as_int(string, 0)
    if not (num & 0x80):
        # short form
        return (num & 0x7F), 1
    # else long-form: b0&0x7f is number of additional base256 length bytes,
    # big-endian
    llen = num & 0x7F
    if not llen:
        fail('UnexpectedDER("Invalid length encoding, length of length is 0")')
    if llen > len(string) - 1:
        fail('UnexpectedDER("Length of length longer than provided buffer")')
    # verify that the encoding is minimal possible (DER requirement)
    msb = str_idx_as_int(string, 1)
    if not msb or llen == 1 and msb < 0x80:
        fail('UnexpectedDER("Not minimal encoding of length")')
    return int(hexlify(string[1 : 1 + llen]), 16), 1 + llen

def read_number(string):
    number = 0
    llen = 0
    if str_idx_as_int(string, 0) == 0x80:
        fail('UnexpectedDER("Non minimal encoding of OID subidentifier")')
    # base-128 big endian, with most significant bit set in all but the last
    # byte
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if llen >= len(string):
            fail('UnexpectedDER("ran out of length bytes")')
        number = number << 7
        d = str_idx_as_int(string, llen)
        number += d & 0x7F
        llen += 1
        if not d & 0x80:
            break
    return number, llen

def remove_sequence(string):
    if not string:
        fail('UnexpectedDER("Empty string does not encode a sequence")')
    if string[:1] != b"\x30":
        n = str_idx_as_int(string, 0)
        fail('UnexpectedDER("wanted type sequence (0x30), got 0x%02x" % n)')
    length, lengthlength = read_length(string[1:])
    if length > (len(string) - 1 - lengthlength):
        fail('UnexpectedDER("Length longer than the provided buffer")')
    endseq = 1 + lengthlength + length
    return string[1 + lengthlength : endseq], string[endseq:]

def remove_octet_string(string):
    if string[:1] != b"\x04":
        n = str_idx_as_int(string, 0)
        fail('UnexpectedDER("wanted type \'octetstring\' (0x04), got 0x%02x")' % n)
    length, llen = read_length(string[1:])
    body = string[1 + llen : 1 + llen + length]
    rest = string[1 + llen + length :]
    return body, rest

def remove_object(string):
    if not string:
        fail('UnexpectedDER("Empty string does not encode an object identifier")')
    if string[:1] != b"\x06":
        n = str_idx_as_int(string, 0)
        fail('UnexpectedDER("wanted type object (0x06), got 0x%02x")' % n)
    length, lengthlength = read_length(string[1:])
    body = string[1 + lengthlength : 1 + lengthlength + length]
    rest = string[1 + lengthlength + length :]
    if not body:
        fail('UnexpectedDER("Empty object identifier")')
    if len(body) != length:
        fail('UnexpectedDER("Length of object identifier longer than the provided buffer")')
    numbers = []
    # while body:
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if not body:
            break
        n, ll = read_number(body)
        numbers.append(n)
        body = body[ll:]
    n0 = numbers.pop(0)
    if n0 < 80:
        first = n0 // 40
    else:
        first = 2
    second = n0 - (40 * first)
    numbers.insert(0, first)
    numbers.insert(1, second)
    return tuple(numbers), rest

def remove_integer(string):
    if not string:
        fail('UnexpectedDER("Empty string is an invalid encoding of an integer")')
    if string[:1] != b"\x02":
        n = str_idx_as_int(string, 0)
        fail('UnexpectedDER("wanted type integer (0x02), got 0x%02x")' % n)
    length, llen = read_length(string[1:])
    if length > (len(string) - 1 - llen):
        fail('UnexpectedDER("Length longer than provided buffer")')
    if length == 0:
        fail('UnexpectedDER("0-byte long encoding of integer")')
    numberbytes = string[1 + llen : 1 + llen + length]
    rest = string[1 + llen + length :]
    msb = str_idx_as_int(numberbytes, 0)
    if not msb < 0x80:
        fail('UnexpectedDER("Negative integers are not supported")')
    # check if the encoding is the minimal one (DER requirement)
    if length > 1 and not msb:
        # leading zero byte is allowed if the integer would have been
        # considered a negative number otherwise
        smsb = str_idx_as_int(numberbytes, 1)
        if smsb < 0x80:
            fail('UnexpectedDER("Invalid encoding of integer, unnecessary zero padding bytes")')
    # return int(binascii.hexlify(numberbytes), 16), rest
    return int(str(hexlify(numberbytes)), 16), rest

def remove_constructed(string):
    s0 = str_idx_as_int(string, 0)
    if (s0 & 0xE0) != 0xA0:
        fail('UnexpectedDER("wanted type constructed tag (0xa0-0xbf), got 0x%02x")' % s0)
    tag = s0 & 0x1F
    length, llen = read_length(string[1:])
    body = string[1 + llen : 1 + llen + length]
    rest = string[1 + llen + length :]
    return tag, body, rest

def unpem(pem):
    # if isinstance(pem, text_type):  # pragma: no branch
    # pem = pem.encode()
    pem = codecs.encode(pem, encoding='utf-8')

    d = b"".join(
        [
            l.strip()
            for l in pem.split(b"\n")
            if l and not l.startswith(b"-----")
        ]
    )
    return b64decode(d)

der = larky.struct(
    encode_oid=encode_oid,
    is_sequence=is_sequence,
    remove_sequence=remove_sequence,
    remove_octet_string=remove_octet_string,
    remove_object=remove_object,
    remove_integer=remove_integer,
    remove_constructed=remove_constructed,
    unpem=unpem
)