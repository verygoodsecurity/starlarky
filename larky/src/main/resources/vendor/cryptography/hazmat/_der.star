# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
# This module contains a lightweight DER encoder and decoder. See X.690 for the
# specification. This module intentionally does not implement the more complex
# BER encoding, only DER.
#
# Note this implementation treats an element's constructed bit as part of the
# tag. This is fine for DER, where the bit is always computable from the type.
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//cryptography/utils",
     int_to_bytes="int_to_bytes",
     int_from_bytes="int_from_bytes")
load("@vendor//option/result", Result="Result")

CONSTRUCTED = 0x20
CONTEXT_SPECIFIC = 0x80

INTEGER = 0x02
BIT_STRING = 0x03
OCTET_STRING = 0x04
NULL = 0x05
OBJECT_IDENTIFIER = 0x06
SEQUENCE = 0x10 | CONSTRUCTED
SET = 0x11 | CONSTRUCTED
PRINTABLE_STRING = 0x13
UTC_TIME = 0x17
GENERALIZED_TIME = 0x18

def DERReader(data):
    self = larky.mutablestruct(__name__='DERReader', __class__=DERReader)

    def __init__(data):
        self.data = bytearray(data)
        return self
    self = __init__(data)

    def __enter__():
        return self
    self.__enter__ = __enter__

    def __exit__(exc_type, exc_value, tb):
        if exc_value == None:
            self.check_empty()
    self.__exit__ = __exit__

    def is_empty():
        return len(self.data) == 0
    self.is_empty = is_empty

    def check_empty():
        if not self.is_empty():
            fail("ValueError: Invalid DER input: trailing data")
    self.check_empty = check_empty

    def read_byte():
        if len(self.data) < 1:
            fail("ValueError: Invalid DER input: insufficient data")
        ret = self.data[0]
        self.data = self.data[1:]
        return ret
    self.read_byte = read_byte

    def read_bytes(n):
        if len(self.data) < n:
            fail("ValueError: Invalid DER input: insufficient data")
        ret = self.data[:n]
        self.data = self.data[n:]
        return ret
    self.read_bytes = read_bytes

    def read_any_element():
        tag = self.read_byte()
        # Tag numbers 31 or higher are stored in multiple bytes. No supported
        # ASN.1 types use such tags, so reject these.
        if tag & 0x1F == 0x1F:
            fail("ValueError: Invalid DER input: unexpected high tag number")
        length_byte = self.read_byte()
        if length_byte & 0x80 == 0:
            # If the high bit is clear, the first length byte is the length.
            length = length_byte
        else:
            # If the high bit is set, the first length byte encodes the length
            # of the length.
            length_byte &= 0x7F
            if length_byte == 0:
                fail("ValueError: " + ("Invalid DER input: indefinite length form is not allowed " + "in DER")
                )
            length = 0
            for i in range(length_byte):
                length <<= 8
                length |= self.read_byte()
                if length == 0:
                    fail("ValueError: Invalid DER input: length was not minimally-encoded"
                    )
            if length < 0x80:
                # If the length could have been encoded in short form, it must
                # not use long form.
                fail("ValueError: Invalid DER input: length was not minimally-encoded")
        body = self.read_bytes(length)
        return tag, DERReader(body)
    self.read_any_element = read_any_element

    def read_element(expected_tag):
        tag, body = self.read_any_element()
        if tag != expected_tag:
            fail("ValueError: Invalid DER input: unexpected tag")
        return body
    self.read_element = read_element

    def read_single_element(expected_tag):
        self.__enter__()
        rval = Result.Ok(self.read_element).map(lambda x: x(expected_tag))
        if rval.is_ok:
            self.__exit__(exc_type=None, exc_value=None, tb=None)
        return rval.unwrap()

    self.read_single_element = read_single_element

    def read_optional_element(expected_tag):
        if len(self.data) > 0 and self.data[0] == expected_tag:
            return self.read_element(expected_tag)
        return None
    self.read_optional_element = read_optional_element

    def as_integer():
        if len(self.data) == 0:
            fail("ValueError: Invalid DER input: empty integer contents")
        first = self.data[0]
        if first & 0x80 == 0x80:
            fail("ValueError: Negative DER integers are not supported")
        # The first 9 bits must not all be zero or all be ones. Otherwise, the
        # encoding should have been one byte shorter.
        if len(self.data) > 1:
            second = self.data[1]
            if first == 0 and second & 0x80 == 0:
                fail("ValueError: Invalid DER input - integer not minimally-encoded")
        return int_from_bytes(self.data, "big")
    self.as_integer = as_integer
    return self


def encode_der_integer(x):
    if not types.is_int(x):
        fail("ValueError: Value must be an integer")
    if x < 0:
        fail("ValueError: Negative integers are not supported")
    n = x.bit_length() // 8 + 1
    return int_to_bytes(x, n)


def encode_der(tag, *children):
    length = 0
    for child in children:
        length += len(child)
    chunks = [bytes([tag])]
    if length < 0x80:
        chunks.append(bytes([length]))
    else:
        length_bytes = int_to_bytes(length)
        chunks.append(bytes([0x80 | len(length_bytes)]))
        chunks.append(length_bytes)
    chunks.extend(children)
    return b"".join(chunks)

