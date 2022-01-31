load("@stdlib//binascii", unhexlify="unhexlify", hexlify="hexlify")
load("@stdlib//larky", larky="larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION")
load("./_compat", str_idx_as_int="str_idx_as_int")

def remove_sequence(string):
    if not string:
        fail('UnexpectedDER("Empty string does not encode a sequence")')
    if string[:1] != b"\x30":
        n = str_idx_as_int(string, 0)
        fail('UnexpectedDER("wanted type sequence (0x30), got 0x%02x" % n)')
    length, lengthlength = read_length(string[1:])
    if length > len(string) - 1 - lengthlength:
        fail('UnexpectedDER("Length longer than the provided buffer")')
    endseq = 1 + lengthlength + length
    return string[1 + lengthlength : endseq], string[endseq:]

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
    if length > len(string) - 1 - llen:
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
    return int(hexlify(numberbytes), 16), rest


der = larky.struct(
    remove_sequence=remove_sequence,
    remove_object=remove_object
)