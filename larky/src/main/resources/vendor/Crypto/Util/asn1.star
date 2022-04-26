# -*- coding: ascii -*-
#
#  Util/asn1.py : Minimal support for ASN.1 DER binary encoding.
#
# ===================================================================
# The contents of this file are dedicated to the public domain.  To
# the extent that dedication to the public domain is not available,
# everyone is granted a worldwide, perpetual, royalty-free,
# non-exclusive license to exercise all rights associated with the
# contents of this file for any purpose whatsoever.
# No rights are reserved.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
# BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ===================================================================
load("@stdlib//binascii", hexlify="hexlify")
load("@stdlib//builtins","builtins")
load("@stdlib//codecs", "codecs")
load("@stdlib//jcrypto", _JCrypto="jcrypto")
load("@stdlib//larky", larky="larky")
load("@stdlib//math", math="math")
load("@stdlib//operator", operator="operator")
load("@stdlib//struct", struct="struct")
load("@stdlib//types",types="types")
load("@vendor//Crypto/Util/py3compat",
     byte_string="byte_string", b="b", bchr="bchr", bord="bord")
load("@vendor//Crypto/Util/number",
     long_to_bytes="long_to_bytes", bytes_to_long="bytes_to_long")

load("@vendor//option/result", Result="Result")


_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION


__all__ = ['DerObject', 'DerInteger', 'DerOctetString', 'DerNull',
           'DerSequence', 'DerObjectId', 'DerBitString', 'DerSetOf']


def _is_number(x, only_non_negative=False):
    # this is a surprisingly hard function b/c it tests if you can add
    # a number and if it throws an exception, it is not a number
    #
    # but, lists (and anything w/ __add__) can overload the '+' operator
    # so, they won't actually throw an exception.
    #
    # so how do we test what is a number?
    if not types.is_int(x):
        return False
    test = 0
    rval = Result.Ok(operator.index).map(lambda index: index(x) + test)
    if rval.is_err:
        return False
    return not only_non_negative or x >= 0


def BytesIO_EOF(initial_bytes):
    """This class differs from BytesIO in that a ValueError exception is
    raised whenever EOF is reached."""

    self = larky.mutablestruct(__name__='BytesIO_EOF', __class__=BytesIO_EOF)

    def __init__(initial_bytes):
        self._buffer = initial_bytes
        self._index = 0
        self._bookmark  = None
        return self
    self = __init__(initial_bytes)

    def set_bookmark():
        self._bookmark = self._index
    self.set_bookmark = set_bookmark

    def data_since_bookmark():
        if self._bookmark == None:
            fail("_bookmark cannot be None!")
        return self._buffer[self._bookmark:self._index]
    self.data_since_bookmark = data_since_bookmark

    def remaining_data():
        return len(self._buffer) - self._index
    self.remaining_data = remaining_data

    def read(length):
        new_index = self._index + length
        if new_index > len(self._buffer):
            fail(
                'ValueError("Not enough data for DER decoding: ' +
                'expected %d bytes and found %d"' %
                (new_index, len(self._buffer))
            )
        result = self._buffer[self._index:new_index]
        self._index = new_index
        return result
    self.read = read

    def read_byte():
        return bord(self.read(1)[0])

    self.read_byte = read_byte
    return self


def _convertTag(tag):
    """Check if *tag* is a real DER tag.
    Convert it from a character to number if necessary.
    """
    if not _is_number(tag):
        if len(tag) == 1:
            tag = bord(tag[0])
    # Ensure that tag is a low tag
    if not (_is_number(tag) and (0 <= tag) and (tag < 0x1F)):
        fail('ValueError("Wrong DER tag")')
    return tag


def DerObject(asn1Id=None,
              payload=bytes(),
              implicit=None,
              constructed=0,
              explicit=None):
    r"""Base class for defining a single DER object.

    This class should never be directly instantiated.
    """

    def __init__(asn1Id, payload, implicit, constructed, explicit):
        """
        Initialize the DER object according to a specific ASN.1 type.

        :Parameters:
          asn1Id : integer
            The universal DER tag number for this object
            (e.g. 0x10 for a SEQUENCE).
            If None, the tag is not known yet.

          payload : byte string
            The initial payload of the object (that it,
            the content octets).
            If not specified, the payload is empty.

          implicit : integer
            The IMPLICIT tag number to use for the encoded object.
            It overrides the universal tag *asn1Id*.

          constructed : bool
            True when the ASN.1 type is *constructed*.
            False when it is *primitive*.

          explicit : integer
            The EXPLICIT tag number to use for the encoded object.
        """
        __dict__ = {
            'payload': payload,
            '_tag_octet': None,
            '__class__': 'DerObject',
        }

        if asn1Id == None:
            # The tag octet will be read in with ``decode``
            __dict__['_tag_octet'] = None
            return larky.mutablestruct(**__dict__)

        asn1Id = _convertTag(asn1Id)

        # In a BER/DER identifier octet:
        # * bits 4-0 contain the tag value
        # * bit 5 is set if the type is 'constructed'
        #   and unset if 'primitive'
        # * bits 7-6 depend on the encoding class
        #
        # Class        | Bit 7, Bit 6
        # ----------------------------------
        # universal    |   0      0
        # application  |   0      1
        # context-spec |   1      0 (default for IMPLICIT/EXPLICIT)
        # private      |   1      1
        #
        if None not in (explicit, implicit):
            fail('ValueError("Explicit and implicit tags are mutually exclusive")')

        if implicit != None:
            __dict__['_tag_octet'] = 0x80 | 0x20 * constructed | _convertTag(implicit)
            return larky.mutablestruct(**__dict__)

        if explicit != None:
            __dict__['_tag_octet'] = 0xA0 | _convertTag(explicit)
            __dict__['_inner_tag_octet'] = 0x20 * constructed | asn1Id
            return larky.mutablestruct(**__dict__)

        __dict__['_tag_octet'] = 0x20 * constructed | asn1Id

        return larky.mutablestruct(**__dict__)

    def _definite_form(length):
        """Build length octets according to BER/DER
        definite form.
        """
        if length > 127:
            encoding = long_to_bytes(length)
            return bchr(len(encoding) + 128) + encoding
        return bchr(length)

    def encode(obj=None):
        """Return this DER element, fully encoded as a binary byte string."""

        # Concatenate identifier octets, length octets,
        # and contents octets
        if obj == None:
            obj = self
        output_payload = obj.payload

        # In case of an EXTERNAL tag, first encode the inner
        # element.
        if hasattr(obj, "_inner_tag_octet"):
            output_payload = (bytearray([obj._inner_tag_octet]) +
                              _definite_form(len(obj.payload)) +
                              bytearray(obj.payload))

        # print(
        #     "tag_octet:", hexlify(bytearray([obj._tag_octet])),
        #     "definite_form:",  hexlify(_definite_form(len(output_payload))),
        #     "payload:", bytearray(output_payload))

        c = (bytearray([obj._tag_octet]) +
             _definite_form(len(output_payload)) +
             bytearray(output_payload))
        # print("joined: ", hexlify(c))
        return c

    def _decodeLen(s):
        """Decode DER length octets from a file."""
        length = s.read_byte()

        if length > 127:
            encoded_length = s.read(length & 0x7F)
            if bord(encoded_length[0]) == 0:
                fail('ValueError("Invalid DER: length has leading zero")')
            length = bytes_to_long(encoded_length)
            if length <= 127:
                fail('ValueError("Invalid DER: length in long form but smaller than 128")')

        return length

    def decode(obj, der_encoded, strict=False):
        """Decode a complete DER element, and re-initializes this
        object with it.

        Args:
          der_encoded (byte string): A complete DER element.

        Raises:
          ValueError: in case of parsing errors.
        """
        if obj == None:
            obj = self

        if not byte_string(der_encoded) and not types.is_bytearray(der_encoded):
            fail('ValueError("Input is not a byte string")')

        s = BytesIO_EOF(der_encoded)
        obj._decodeFromStream(obj, s, strict)

        # There shouldn't be other bytes left
        if s.remaining_data() > 0:
            fail('ValueError("Unexpected extra data after the DER structure")')

        return obj

    def _decodeFromStream(obj, s, strict):
        """Decode a complete DER element from a file."""
        if obj == None:
            obj = self

        idOctet = s.read_byte()
        if obj._tag_octet != None:
            if idOctet != obj._tag_octet:
                fail('ValueError("Unexpected DER tag")')
        else:
            obj._tag_octet = idOctet
        length = _decodeLen(s)
        obj.payload = s.read(length)

        # In case of an EXTERNAL tag, further decode the inner
        # element.
        if hasattr(obj, "_inner_tag_octet"):
            p = BytesIO_EOF(obj.payload)
            inner_octet = p.read_byte()
            if inner_octet != obj._inner_tag_octet:
                fail('ValueError("Unexpected internal DER tag")')
            length = _decodeLen(p)
            obj.payload = p.read(length)

            # There shouldn't be other bytes left
            if p.remaining_data() > 0:
                fail('ValueError("Unexpected extra data after the DER structure")')

    self = __init__(asn1Id, payload, implicit, constructed, explicit)
    self.encode = encode
    self.decode = decode
    self._decodeFromStream = _decodeFromStream
    return self


def DerInteger(value=0, implicit=None, explicit=None):
    r"""Class to model a DER INTEGER.

    An example of encoding is::

      >>> from Crypto.Util.asn1 import DerInteger
      >>> from binascii import hexlify, unhexlify
      >>> int_der = DerInteger(9)
      >>> print hexlify(int_der.encode())

    which will show ``020109``, the DER encoding of 9.

    And for decoding::

      >>> s = unhexlify(b'020109')
      >>> try:
      >>>   int_der = DerInteger()
      >>>   int_der.decode(s)
      >>>   print int_der.value
      >>> except ValueError:
      >>>   print "Not a valid DER INTEGER"

    the output will be ``9``.

    :ivar value: The integer value
    :vartype value: integer
    """

    def __init__(value, implicit, explicit):
        """Initialize the DER object as an INTEGER.

        :Parameters:
          value : integer
            The value of the integer.

          implicit : integer
            The IMPLICIT tag to use for the encoded object.
            It overrides the universal tag for INTEGER (2).
        """
        derobject = DerObject(0x02, bytes(), implicit, 0, explicit)
        __dict__ = larky.to_dict(derobject)
        __dict__['derobject'] = derobject
        __dict__['__class__'] = 'DerInteger'
        __dict__['value'] = value # The integer value
        return larky.mutablestruct(**__dict__)

    def encode():
        """Return the DER INTEGER, fully encoded as a
        binary string."""
        i = _JCrypto.Util.ASN1.DerInteger(self.value)
        return i.encode()

    def decode(der_encoded, strict=False):
        """Decode a complete DER INTEGER DER, and re-initializes this
        object with it.

        Args:
          der_encoded (byte string): A complete INTEGER DER element.

        Raises:
          ValueError: in case of parsing errors.
        """
        # i = _JCrypto.Util.ASN1.DerInteger(self.value)
        # decoded = i.decode(der_encoded, strict=strict)
        # self.value = decoded.as_int()
        # return self
        return self.derobject.decode(self, der_encoded, strict)

    def _decodeFromStream(self, s, strict):
        """Decode a complete DER INTEGER from a file."""
        # Fill up self.payload
        self.derobject._decodeFromStream(self, s, strict)

        if strict:
             if len(self.payload) == 0:
                 fail('ValueError("Invalid encoding for DER INTEGER: empty payload")')
             if len(self.payload) >= 2 and struct.unpack('>H', self.payload[:2])[0] < 0x80:
                 fail('ValueError("Invalid encoding for DER INTEGER: leading zero")')

        # Derive self.value from self.payload
        self.value = 0
        bits = 1
        for i in self.payload:
            self.value *= 256
            self.value += bord(i)
            bits <<= 8
        if self.payload and bord(self.payload[0]) & 0x80:
            self.value -= bits

    self = __init__(value, implicit, explicit)
    self.decode = decode
    self.encode = encode
    self._decodeFromStream = _decodeFromStream
    return self


def DerSequence(startSeq=None, implicit=None):
    r"""Class to model a DER SEQUENCE.

    This object behaves like a dynamic Python sequence.

    Sub-elements that are INTEGERs behave like Python integers.

    Any other sub-element is a binary string encoded as a complete DER
    sub-element (TLV).

    An example of encoding is:

      >>> from Crypto.Util.asn1 import DerSequence, DerInteger
      >>> from binascii import hexlify, unhexlify
      >>> obj_der = unhexlify('070102')
      >>> seq_der = DerSequence([4])
      >>> seq_der.append(9)
      >>> seq_der.append(obj_der.decode("utf-8"))
      >>> print(hexlify(seq_der.encode()))

    which will show ``b'3009020104020109070102'``, the DER encoding of the
    sequence containing ``4``, ``9``, and the object with payload ``02``.

    For decoding:

      >>> s = unhexlify(b'3009020104020109070102')
      >>> try:
      >>>   seq_der = DerSequence()
      >>>   seq_der.decode(s)
      >>>   print len(seq_der)
      >>>   print seq_der[0]
      >>>   print seq_der[:]
      >>> except ValueError:
      >>>   print "Not a valid DER SEQUENCE"

    the output will be::

      3
      4
      [4, 9, b'\x07\x01\x02']

    """

    def __init__(startSeq, implicit):
        """Initialize the DER object as a SEQUENCE.

        :Parameters:
          startSeq : Python sequence
            A sequence whose element are either integers or
            other DER objects.

          implicit : integer
            The IMPLICIT tag to use for the encoded object.
            It overrides the universal tag for SEQUENCE (16).
        """

        derobject = DerObject(0x10, bytes(), implicit, 1)
        __dict__ = larky.to_dict(derobject)
        __dict__['derobject'] = derobject
        __dict__['__name__'] = 'DerSequence'
        __dict__['__class__'] = DerSequence
        __dict__['_seq'] = startSeq if startSeq != None else []

        return larky.mutablestruct(
            **__dict__
        )

    # A few methods to make it behave like a python sequence

    def pop(n):
        self._seq.pop(n)

    def __getitem__(n):
        return self._seq[n]

    def __setitem__(key, value):
        self._seq[key] = value

    def __setslice__(i, j, sequence):
        for atom in sequence:
            if i >= j:
                break
            self._seq.pop(i)
            self._seq.insert(i, atom)
            i += 1

    def __delslice__(i, j):
        to_remove = self._seq[i:j]
        for r in to_remove:
            self._seq.remove(r)

    def __getslice__(i, j):
        return self._seq[i:j]

    def __len__():
        return len(self._seq)

    def __iadd__(item):
        self._seq.append(item)
        return self

    def __add__(item):
        self._seq.append(item)
        return self

    def append(item):
        self._seq.append(item)
        return self

    def hasInts(only_non_negative=True):
        """Return the number of items in this sequence that are
        integers.

        Args:
          only_non_negative (boolean):
            If ``True``, negative integers are not counted in.
        """

        items = [x for x in self._seq if _is_number(x, only_non_negative)]
        return len(items)

    def hasOnlyInts(only_non_negative=True):
        """Return ``True`` if all items in this sequence are integers
        or non-negative integers.

        This function returns False is the sequence is empty,
        or at least one member is not an integer.

        Args:
          only_non_negative (boolean):
            If ``True``, the presence of negative integers
            causes the method to return ``False``."""
        return self._seq and hasInts(only_non_negative) == len(self._seq)

    def encode():
        """Return this DER SEQUENCE, fully encoded as a
        binary string.

        Raises:
          ValueError: if some elements in the sequence are neither integers
                      nor byte strings.
        """
        # NOTE: We do not use bouncycastle here b/c pycryptodome has its own
        # flavor of ASN1 encoding that removes BER tags from the encoding
        # and I cannot for the life of me figure out how to do it from Java
        #
        # i = _JCrypto.Util.ASN1.DerSequence(self._seq)
        # return i.encode()
        self.payload = b''

        if not self._seq:
            return self.derobject.encode(self)

        payload = [self.payload]
        _seq = list(self._seq)  # type: list

        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            # noinspection PyTypeChecker
            if len(_seq) == 0:
                break
            item = _seq.pop(0)
            if types.is_bytelike(item):
               payload[-1] += item
            elif types.is_tuple(item) and item[0] == larky.SENTINEL:
                # see the comment for type(item) == 'DerSequence'
                # for why this is needed assign payload to the instance!
                item[1].payload = payload.pop(-1)
                # re-encode to previous payload
                payload[-1] += item[1].derobject.encode(item[1])
            elif type(item) == 'DerSequence':
                # we do not have recursion in Larky, so if we see a nested
                # DerSequence, then, we effectively have to push the
                # current item on the stack and signal a sentinel to know
                # when we need to finalize the encoding.
                #
                # it's important to keep track of the payload since
                # it is used in `derobject.encode()` to add the appropriate
                # tags
                new_seq = list(item._seq)  # copy the list..
                new_seq.append((larky.SENTINEL, item))   # push the sentinel
                if _seq:
                    new_seq.extend(_seq)   # extend if there's any _seq left
                _seq = new_seq  # type: list
                payload.append(b'')  # very important, reset the payload stack
            elif _is_number(item):
                payload[-1] += DerInteger(item).encode()
            else:
                payload[-1] += item.encode()
        self.payload = payload.pop(-1)
        if payload:
            fail("DerSequence payload is not empty!")
        return self.derobject.encode(self)


    def decode(der_encoded, strict=False, nr_elements=None, only_ints_expected=False,
               # TODO(Hack)...until I introduce safetywrap
               errors=True):
        """Decode a complete DER SEQUENCE, and re-initializes this
        object with it.

        Args:
          der_encoded (byte string):
            A complete SEQUENCE DER element.
          nr_elements (None or integer or list of integers):
            The number of members the SEQUENCE can have
          only_ints_expected (boolean):
            Whether the SEQUENCE is expected to contain only integers.
          strict (boolean):
            Whether decoding must check for strict DER compliancy.

        Raises:
          ValueError: in case of parsing errors.

        DER INTEGERs are decoded into Python integers. Any other DER
        element is not decoded. Its validity is not checked.
        """

        self._nr_elements = nr_elements
        i = _JCrypto.Util.ASN1.DerSequence(self._seq)
        self._seq = i.decode(der_encoded)
        # result = self.derobject.decode(self, der_encoded, strict=strict)
        #
        if only_ints_expected and not hasOnlyInts():
            # TODO(Hack)...until I introduce safetywrap
            if errors:
                fail('ValueError: Some members are not INTEGERs')
            return

        ok = True
        if self._nr_elements != None:
            if types.is_iterable(self._nr_elements):
                ok = len(self._seq) in self._nr_elements
            else:
                ok = len(self._seq) == self._nr_elements

        if not ok:
            # TODO(Hack)...until I introduce safetywrap
            if errors:
                err = '"Unexpected number of members (%d) in the sequence"'
                fail('ValueError(%s)' % (err % len(self._seq)))
            return
        return self

    def _decodeFromStream(obj, s, strict):
        """Decode a complete DER SEQUENCE from a file."""
        if not obj:
            obj = self

        obj._seq = []

        # Fill up self.payload
        obj.derobject._decodeFromStream(obj, s, strict)

        # Add one item at a time to self.seq, by scanning self.payload
        p = BytesIO_EOF(obj.payload)
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if p.remaining_data() <= 0:
                break
            p.set_bookmark()

            der = DerObject()
            der._decodeFromStream(der, p, strict)

            # Parse INTEGERs differently
            if der._tag_octet != 0x02:
                obj._seq.append(p.data_since_bookmark())
            else:
                derInt = DerInteger()
                data = p.data_since_bookmark()
                derInt.decode(data, strict=strict)
                obj._seq.append(derInt.value)

        ok = True
        if obj._nr_elements != None:
            if types.is_iterable(obj._nr_elements):
                ok = len(obj._seq) in obj._nr_elements
            else:
                ok = len(obj._seq) == obj._nr_elements

        if not ok:
            err = '"Unexpected number of members (%d) in the sequence"'
            fail('ValueError(%s)' % (err % len(obj._seq)))

    self = __init__(startSeq, implicit)
    self.append = append
    self.decode = decode
    self.encode = encode
    self.hasInts = hasInts
    self.hasOnlyInts = hasOnlyInts
    self.pop = pop
    self._decodeFromStream = _decodeFromStream
    self.__getitem__ = __getitem__
    self.__setitem__ = __setitem__
    self.__setslice__ = __setslice__
    self.__delslice__ = __delslice__
    self.__getslice__ = __getslice__
    self.__len__ = __len__
    self.__iadd__ = __iadd__
    self.__add__ = __add__
    return self


def DerOctetString(value=builtins.bytes(r'', encoding='utf-8'), implicit=None):
    r"""Class to model a DER OCTET STRING.

    An example of encoding is:

    >>> from Crypto.Util.asn1 import DerOctetString
    >>> from binascii import hexlify, unhexlify
    >>> os_der = DerOctetString(b'\\xaa')
    >>> os_der.payload += b'\\xbb'
    >>> print hexlify(os_der.encode())

    which will show ``0402aabb``, the DER encoding for the byte string
    ``b'\\xAA\\xBB'``.

    For decoding:

    >>> s = unhexlify(b'0402aabb')
    >>> try:
    >>>   os_der = DerOctetString()
    >>>   os_der.decode(s)
    >>>   print hexlify(os_der.payload)
    >>> except ValueError:
    >>>   print "Not a valid DER OCTET STRING"

    the output will be ``aabb``.

    :ivar payload: The content of the string
    :vartype payload: byte string
    """

    def __init__(value, implicit):
        """Initialize the DER object as an OCTET STRING.

        :Parameters:
          value : byte string
            The initial payload of the object.
            If not specified, the payload is empty.

          implicit : integer
            The IMPLICIT tag to use for the encoded object.
            It overrides the universal tag for OCTET STRING (4).
        """
        derobject = DerObject(0x04, value, implicit, 0)
        __dict__ = larky.to_dict(derobject)
        __dict__['derobject'] = derobject
        __dict__['__class__'] = 'DerOctetString'
        return larky.mutablestruct(
           **__dict__
        )

    def encode():
        return self.derobject.encode(self)

    def decode(der_encoded, strict=False):
        return self.derobject.decode(self, der_encoded, strict=strict)

    self = __init__(value, implicit)
    self.encode = encode
    self.decode = decode
    return self


def DerNull():
    """Class to model a DER NULL element."""

    def __init__():
        """Initialize the DER object as a NULL."""
        derobject = DerObject(0x05, bytes(), None, 0)
        __dict__ = larky.to_dict(derobject)
        __dict__['derobject'] = derobject
        __dict__['__class__'] = 'DerNull'
        return larky.mutablestruct(
           **__dict__
        )

    def encode():
        return self.derobject.encode(self)

    def decode(der_encoded, strict=False):
        return self.derobject.decode(self, der_encoded, strict=strict)

    self = __init__()
    self.encode = encode
    self.decode = decode
    return self


def DerObjectId(value='', implicit=None, explicit=None):
    """Class to model a DER OBJECT ID.

    An example of encoding is:

    >>> from Crypto.Util.asn1 import DerObjectId
    >>> from binascii import hexlify, unhexlify
    >>> oid_der = DerObjectId("1.2")
    >>> oid_der.value += ".840.113549.1.1.1"
    >>> print hexlify(oid_der.encode())

    which will show ``06092a864886f70d010101``, the DER encoding for the
    RSA Object Identifier ``1.2.840.113549.1.1.1``.

    For decoding:

    >>> s = unhexlify(b'06092a864886f70d010101')
    >>> try:
    >>>   oid_der = DerObjectId()
    >>>   oid_der.decode(s)
    >>>   print oid_der.value
    >>> except ValueError:
    >>>   print "Not a valid DER OBJECT ID"

    the output will be ``1.2.840.113549.1.1.1``.

    :ivar value: The Object ID (OID), a dot separated list of integers
    :vartype value: string
    """

    def __init__(value, implicit, explicit):
        """Initialize the DER object as an OBJECT ID.

        :Parameters:
          value : string
            The initial Object Identifier (e.g. "1.2.0.0.6.2").
          implicit : integer
            The IMPLICIT tag to use for the encoded object.
            It overrides the universal tag for OBJECT ID (6).
          explicit : integer
            The EXPLICIT tag to use for the encoded object.
        """
        derobject = DerObject(0x06, bytes(), implicit, 0, explicit)
        __dict__ = larky.to_dict(derobject)
        __dict__['derobject'] = derobject
        __dict__['__class__'] = 'DerObjectId'
        __dict__['value'] = value

        return larky.mutablestruct(
            **__dict__
        )

    def encode():
        """Return the DER OBJECT ID, fully encoded as a
        binary string."""
        comps = [int(x) for x in self.value.split(".")]
        if len(comps) < 2:
            fail('ValueError("Not a valid Object Identifier string")')
        objectid = _JCrypto.Util.ASN1.DerObjectId(self.value)
        return objectid.encode()

    def decode(der_encoded, strict=False):
        """Decode a complete DER OBJECT ID, and re-initializes this
        object with it.

        Args:
            der_encoded (byte string):
                A complete DER OBJECT ID.
            strict (boolean):
                Whether decoding must check for strict DER compliancy.

        Raises:
            ValueError: in case of parsing errors.
        """
        if type(der_encoded) == 'string':
            #return der_encoded
            der_encoded = DerObjectId(der_encoded).encode()
        return self.derobject.decode(self, der_encoded, strict)

    def _decodeFromStream(obj, s, strict):
        """Decode a complete DER OBJECT ID from a file."""
        if obj == None:
            obj = self
        # Fill up obj.payload
        obj.derobject._decodeFromStream(obj, s, strict)

        # Derive obj.value from obj.payload
        p = BytesIO_EOF(obj.payload)

        comps = [str(x) for x in divmod(p.read_byte(), 40)]
        v = 0
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if not p.remaining_data():
                break
            c = p.read_byte()
            v = v*128 + (c & 0x7F)
            if not (c & 0x80):
                comps.append(str(v))
                v = 0
        obj.value = '.'.join(comps)
        return value

    self = __init__(value, implicit, explicit)
    self.encode = encode
    self.decode = decode
    self._decodeFromStream = _decodeFromStream
    return self


def DerBitString(value=bytes(), implicit=None, explicit=None):
    r"""Class to model a DER BIT STRING.

    An example of encoding is:

    >>> from Crypto.Util.asn1 import DerBitString
    >>> from binascii import hexlify, unhexlify
    >>> bs_der = DerBitString(b'\\xaa')
    >>> bs_der.value += b'\\xbb'
    >>> print hexlify(bs_der.encode())

    which will show ``040300aabb``, the DER encoding for the bit string
    ``b'\\xAA\\xBB'``.

    For decoding:

    >>> s = unhexlify(b'040300aabb')
    >>> try:
    >>>   bs_der = DerBitString()
    >>>   bs_der.decode(s)
    >>>   print hexlify(bs_der.value)
    >>> except ValueError:
    >>>   print "Not a valid DER BIT STRING"

    the output will be ``aabb``.

    :ivar value: The content of the string
    :vartype value: byte string
    """

    def __init__(value, implicit, explicit):
        """Initialize the DER object as a BIT STRING.

        :Parameters:
          value : byte string or DER object
            The initial, packed bit string.
            If not specified, the bit string is empty.
          implicit : integer
            The IMPLICIT tag to use for the encoded object.
            It overrides the universal tag for OCTET STRING (3).
          explicit : integer
            The EXPLICIT tag to use for the encoded object.
        """
        derobject = DerObject(0x03, bytes(), implicit, 0, explicit)
        __dict__ = larky.to_dict(derobject)
        __dict__['derobject'] = derobject
        __dict__['__class__'] = 'DerBitString'
        # The bitstring value (packed)
        # NOTE: interesting way to check for isinstance is to check to see if
        # the object has the underlying composed object.
        # NOTE: with larky, we prefer composition over inheritance.
        if hasattr(value, 'derobject'):
            __dict__['value'] = value.encode()
        else:
            __dict__['value'] = value

        return larky.mutablestruct(
            **__dict__
        )

    def encode():
        """Return the DER BIT STRING, fully encoded as a
        binary string."""

        # Add padding count byte
        self.payload = bytearray([0x00]) + self.value
        return self.derobject.encode(self)

    def decode(der_encoded, strict=False):
        """Decode a complete DER BIT STRING, and re-initializes this
        object with it.

        Args:
            der_encoded (byte string): a complete DER BIT STRING.
            strict (boolean):
                Whether decoding must check for strict DER compliancy.

        Raises:
            ValueError: in case of parsing errors.
        """

        return self.derobject.decode(self, der_encoded, strict)

    def _decodeFromStream(obj, s, strict):
        """Decode a complete DER BIT STRING DER from a file."""
        if obj == None:
            obj = self
        # Fill-up obj.payload
        obj.derobject._decodeFromStream(obj, s, strict)

        if obj.payload and bord(obj.payload[0]) != 0:
            fail('ValueError("Not a valid BIT STRING")')

        # Fill-up obj.value
        self.value = bytearray(r'', encoding='utf-8')
        # Remove padding count byte
        if obj.payload:
            self.value = obj.payload[1:]

    self = __init__(value, implicit, explicit)
    self.encode = encode
    self.decode = decode
    self._decodeFromStream = _decodeFromStream
    return self


def DerSetOf(startSet=None, implicit=None):
    r"""Class to model a DER SET OF.

    An example of encoding is:

    >>> from Crypto.Util.asn1 import DerBitString
    >>> from binascii import hexlify, unhexlify
    >>> so_der = DerSetOf([4,5])
    >>> so_der.add(6)
    >>> print hexlify(so_der.encode())

    which will show ``3109020104020105020106``, the DER encoding
    of a SET OF with items 4,5, and 6.

    For decoding:

    >>> s = unhexlify(b'3109020104020105020106')
    >>> try:
    >>>   so_der = DerSetOf()
    >>>   so_der.decode(s)
    >>>   print [x for x in so_der]
    >>> except ValueError:
    >>>   print "Not a valid DER SET OF"

    the output will be ``[4, 5, 6]``.
    """

    def __init__(startSet, implicit):
        """Initialize the DER object as a SET OF.

        :Parameters:
          startSet : container
            The initial set of integers or DER encoded objects.
          implicit : integer
            The IMPLICIT tag to use for the encoded object.
            It overrides the universal tag for SET OF (17).
        """
        derobject = DerObject(0x11, bytes(), implicit, 1)
        __dict__ = larky.to_dict(derobject)
        __dict__['derobject'] = derobject
        __dict__['__class__'] = 'DerSetOf'
        __dict__['_seq'] = []

        # All elements must be of the same type (and therefore have the
        # same leading octet)
        __dict__['_elemOctet'] = None
        self = larky.mutablestruct(**__dict__)
        return self

    def __getitem__(n):
        return self._seq[n]

    def __iter__():
        return self._seq

    def __len__():
        return len(self._seq)

    def add(elem):
        """Add an element to the set.

        Args:
            elem (byte string or integer):
              An element of the same type of objects already in the set.
              It can be an integer or a DER encoded object.
        """

        if _is_number(elem):
            eo = 0x02
        elif hasattr(elem, 'derobject'):
            eo = self._tag_octet
        else:
            eo = bord(elem[0])

        if self._elemOctet != eo:
            if self._elemOctet != None:
                fail('ValueError("New element does not belong to the set")')
            self._elemOctet = eo

        if elem not in self._seq:
            self._seq.append(elem)

    def decode(der_encoded, strict=False):
        """Decode a complete SET OF DER element, and re-initializes this
        object with it.

        DER INTEGERs are decoded into Python integers. Any other DER
        element is left undecoded; its validity is not checked.

        Args:
            der_encoded (byte string): a complete DER BIT SET OF.
            strict (boolean):
                Whether decoding must check for strict DER compliancy.

        Raises:
            ValueError: in case of parsing errors.
        """
        i = _JCrypto.Util.ASN1.DerSetOf(self._seq)
        self._seq = i.decode(der_encoded, strict)
        return self

    def encode():
        """Return this SET OF DER element, fully encoded as a
        binary string.
        """

        # Elements in the set must be ordered in lexicographic order
        dersetof = _JCrypto.Util.ASN1.DerSetOf(self._seq)
        return dersetof.encode()

    self = __init__(startSet, implicit)
    if startSet:
        for e in startSet:
            add(e)
    self.encode = encode
    self.decode = decode
    self.add = add
    self.__getitem__ = __getitem__
    self.__len__ = __len__
    self.__iter__ = __iter__
    return self


asn1 = larky.mutablestruct(
    DerObject=DerObject,
    DerSetOf=DerSetOf,
    DerInteger=DerInteger,
    DerBitString=DerBitString,
    DerObjectId=DerObjectId,
    DerNull=DerNull,
    DerOctetString=DerOctetString,
    DerSequence=DerSequence
)