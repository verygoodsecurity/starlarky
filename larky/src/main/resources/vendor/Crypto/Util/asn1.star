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
load("@stdlib//builtins","builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//struct", struct="struct")
load("@stdlib//types",types="types")
load("@stdlib//codecs", "codecs")
load("@stdlib//binascii", hexlify="hexlify")
load("@vendor//Crypto/Util/py3compat",
     byte_string="byte_string", b="b", bchr="bchr", bord="bord")

load("@vendor//Crypto/Util/number",
     long_to_bytes="long_to_bytes", bytes_to_long="bytes_to_long")

load("@stdlib//jcrypto", _JCrypto="jcrypto")


_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION


__all__ = ['DerObject', 'DerInteger', 'DerOctetString', 'DerNull',
           'DerSequence', 'DerObjectId', 'DerBitString', 'DerSetOf']


def _is_number(x, only_non_negative=False):
    if not types.is_int(x):
        return False

    return (not only_non_negative) or (x >= 0)


def BytesIO_EOF(initial_bytes):
    """This class differs from BytesIO in that a ValueError exception is
    raised whenever EOF is reached."""

    def __init__(initial_bytes):
        return larky.mutablestruct(
            _buffer=initial_bytes,
            _index=0,
            _bookmark=None
        )

    def set_bookmark():
        self._bookmark = self._index

    def data_since_bookmark():
        if self._bookmark == None:
            fail("_bookmark cannot be None!")
        return self._buffer[self._bookmark:self._index]

    def remaining_data():
        return len(self._buffer) - self._index

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

    def read_byte():
        return bord(self.read(1)[0])

    self = __init__(initial_bytes)
    self.set_bookmark = set_bookmark
    self.data_since_bookmark = data_since_bookmark
    self.remaining_data = remaining_data
    self.read = read
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
                fail(" ValueError(\"Invalid DER: length has leading zero\")")
            length = bytes_to_long(encoded_length)
            if length <= 127:
                fail(" ValueError(\"Invalid DER: length in long form but smaller than 128\")")

        return length

    def decode(der_encoded, strict=False):
        """Decode a complete DER element, and re-initializes this
        object with it.

        Args:
          der_encoded (byte string): A complete DER element.

        Raises:
          ValueError: in case of parsing errors.
        """

        if not byte_string(der_encoded):
            fail(" ValueError(\"Input is not a byte string\")")

        s = BytesIO_EOF(der_encoded)
        _decodeFromStream(s, strict)

        # There shouldn't be other bytes left
        if s.remaining_data() > 0:
            fail(" ValueError(\"Unexpected extra data after the DER structure\")")

        return self

    def _decodeFromStream(s, strict):
        """Decode a complete DER element from a file."""

        idOctet = s.read_byte()
        if self._tag_octet != None:
            if idOctet != self._tag_octet:
                fail('ValueError("Unexpected DER tag")')
        else:
            self._tag_octet = idOctet
        length = _decodeLen(s)
        self.payload = s.read(length)

        # In case of an EXTERNAL tag, further decode the inner
        # element.
        if hasattr(self, "_inner_tag_octet"):
            p = BytesIO_EOF(self.payload)
            inner_octet = p.read_byte()
            if inner_octet != self._inner_tag_octet:
                fail(' ValueError("Unexpected internal DER tag")')
            length = _decodeLen(p)
            self.payload = p.read(length)

            # There shouldn't be other bytes left
            if p.remaining_data() > 0:
                fail('ValueError("Unexpected extra data after the DER structure")')

    self = __init__(asn1Id, payload, implicit, constructed, explicit)
    self.encode = encode
    self.decode = decode
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
        i = _JCrypto.Util.ASN1.DerInteger(self.value)
        decoded = i.decode(der_encoded, strict=strict)
        self.value = decoded.as_int()
        return self

    self = __init__(value, implicit, explicit)
    self.decode = decode
    self.encode = encode
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
        __dict__['__class__'] = 'DerSequence'
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
            self._seq.remove(i)
            self._seq.insert(i, atom)
            i += 1

    def __delslice__(i, j):
        to_remove = self._seq[i:j]
        for r in to_remove:
            self._seq.remove(r)

    def __getslice__(i, j):
        return self._seq[max(0, i):max(0, j)]

    def __len__():
        return len(self._seq)

    def __iadd__(item):
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
        self.payload = bytearray()
        for item in self._seq:
            if byte_string(item) or types.is_bytearray(item):
                self.payload += item
            elif _is_number(item):
                self.payload += bytearray(DerInteger(item).encode().elems())
            elif types.is_string(item):
                self.payload += codecs.encode(item, encoding='utf-8')
            elif hasattr(item, 'encode'):
                encoded = item.encode()
                if hasattr(encoded, 'elems'):
                    self.payload += bytearray(encoded.elems())
            else:
                fail("do not know how to handle: " + type(item))

        return self.derobject.encode(self)

    def decode(der_encoded, strict=False, nr_elements=None, only_ints_expected=False):
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

        _nr_elements = nr_elements
        result = self.derobject.decode(der_encoded, strict=strict)

        if only_ints_expected and not hasOnlyInts():
            fail(" ValueError(\"Some members are not INTEGERs\")")

        return result

    def _decodeFromStream(s, strict):
        """Decode a complete DER SEQUENCE from a file."""

        self._seq = []

        # Fill up self.payload
        self.derobject._decodeFromStream(s, strict)

        # Add one item at a time to self.seq, by scanning self.payload
        p = BytesIO_EOF(self.payload)
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if p.remaining_data() <= 0:
                break
            p.set_bookmark()

            der = DerObject()
            der._decodeFromStream(p, strict)

            # Parse INTEGERs differently
            if der._tag_octet != 0x02:
                self._seq.append(p.data_since_bookmark())
            else:
                derInt = DerInteger()
                data = p.data_since_bookmark()
                derInt.decode(data, strict=strict)
                self._seq.append(derInt.value)

        ok = True
        if self._nr_elements != None:
            if types.is_iterable(self._nr_elements):
                ok = len(self._seq) in self._nr_elements
            else:
                ok = len(self._seq) == self._nr_elements

        if not ok:
            err = '"Unexpected number of members (%d) in the sequence"'
            fail('ValueError(%s)' % (err % len(self._seq)))

    self = __init__(startSeq, implicit)
    self.append = append
    self.decode = decode
    self.encode = encode
    self.hasInts = hasInts
    self.hasOnlyInts = hasOnlyInts
    self.pop = pop

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
        self = DerObject(0x04, value, implicit, 0)
        return self

    self = __init__(value, implicit)
    return self


def DerNull():
    """Class to model a DER NULL element."""

    def __init__():
        """Initialize the DER object as a NULL."""

        self = DerObject(0x05, builtins.bytes(r'', encoding='utf-8'), None, 0)
        return self

    self = __init__()
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
        self = DerObject(0x06, builtins.bytes(r'', encoding='utf-8'), implicit, 0, explicit)
        self.value = value
        return self

    def encode():
        """Return the DER OBJECT ID, fully encoded as a
        binary string."""
        fail("OH NOES")
        comps = [int(x) for x in self.value.split(".")]
        if len(comps) < 2:
            fail(" ValueError(\"Not a valid Object Identifier string\")")
        payload = bchr(40*comps[0]+comps[1])
        for v in comps[2:]:
            if v == 0:
                enc = [0]
            else:
                enc = []
                for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
                    if not v:
                        break
                    enc.insert(0, (v & 0x7F) | 0x80)
                    v >>= 7
                enc[-1] &= 0x7F
            payload += builtins.bytes(r'', encoding='utf-8').join([bchr(x) for x in enc])
        return DerObject.encode(self)

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

        return DerObject.decode(self, der_encoded, strict)

    def _decodeFromStream(s, strict):
        """Decode a complete DER OBJECT ID from a file."""

        # Fill up self.payload
        DerObject._decodeFromStream(self, s, strict)

        # Derive self.value from self.payload
        p = BytesIO_EOF(self.payload)

        def divmod(x, y):
            fail("add divmod!")

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
        value = '.'.join(comps)

    self = __init__(value, implicit, explicit)
    return self


def DerBitString(value=builtins.bytes(r'', encoding='utf-8'), implicit=None, explicit=None):
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
        self = DerObject(0x03, builtins.bytes(r'', encoding='utf-8'), implicit, 0, explicit)

        # The bitstring value (packed)
        if types.is_instance(value, DerObject):
            self.value = value.encode()
        else:
            self.value = value

    def encode():
        """Return the DER BIT STRING, fully encoded as a
        binary string."""

        # Add padding count byte
        payload = builtins.bytes(r'\x00', encoding='utf-8') + value
        return DerObject.encode(self)

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

        return DerObject.decode(self, der_encoded, strict)

    def _decodeFromStream(s, strict):
        """Decode a complete DER BIT STRING DER from a file."""

        # Fill-up self.payload
        DerObject._decodeFromStream(self, s, strict)

        if self.payload and bord(self.payload[0]) != 0:
            fail(" ValueError(\"Not a valid BIT STRING\")")

        # Fill-up self.value
        value = builtins.bytes(r'', encoding='utf-8')
        # Remove padding count byte
        if self.payload:
            value = self.payload[1:]

    self = __init__(value, implicit, explicit)
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
        self = DerObject(0x11, builtins.bytes(r'', encoding='utf-8'), implicit, 1)
        self._seq = []

        # All elements must be of the same type (and therefore have the
        # same leading octet)
        self._elemOctet = None

        if startSet:
            for e in startSet:
                add(e)
        return self

    def __getitem__(n):
        return self._seq[n]

    def __iter__():
        fail("Not supported")
        # return iter(self._seq)

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
        elif types.is_instance(elem, DerObject):
            eo = self._tag_octet
        else:
            eo = bord(elem[0])

        if self._elemOctet != eo:
            if self._elemOctet != None:
                fail(" ValueError(\"New element does not belong to the set\")")
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

        return DerObject.decode(self, der_encoded, strict)

    def _decodeFromStream(s, strict):
        """Decode a complete DER SET OF from a file."""

        self._seq = []

        # Fill up self.payload
        DerObject._decodeFromStream(self, s, strict)

        # Add one item at a time to self.seq, by scanning self.payload
        p = BytesIO_EOF(self.payload)
        setIdOctet = -1
        for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
            if p.remaining_data() <= 0:
                break
            p.set_bookmark()

            der = DerObject()
            der._decodeFromStream(p, strict)

            # Verify that all members are of the same type
            if setIdOctet < 0:
                setIdOctet = der._tag_octet
            else:
                if setIdOctet != der._tag_octet:
                    fail(" ValueError(\"Not all elements are of the same DER type\")")

            # Parse INTEGERs differently
            if setIdOctet != 0x02:
                self._seq.append(p.data_since_bookmark())
            else:
                derInt = DerInteger()
                derInt.decode(p.data_since_bookmark(), strict)
                self._seq.append(derInt.value)
        # end

    def encode():
        """Return this SET OF DER element, fully encoded as a
        binary string.
        """

        # Elements in the set must be ordered in lexicographic order
        ordered = []
        for item in self._seq:
            if _is_number(item):
                bys = DerInteger(item).encode()
            elif types.is_instance(item, DerObject):
                bys = item.encode()
            else:
                bys = item
            ordered.append(bys)
        ordered.sort()
        self.payload = builtins.bytes(r'', encoding='utf-8').join(ordered)
        return DerObject.encode(self)

    self = __init__(startSet, implicit)
    return self
