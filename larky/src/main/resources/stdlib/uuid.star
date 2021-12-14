r"""UUID objects (universally unique identifiers) according to RFC 4122.
This module provides immutable UUID objects (class UUID) and the functions
uuid1(), uuid3(), uuid4(), uuid5() for generating version 1, 3, 4, and 5
UUIDs as specified in RFC 4122.
If all you want is a unique ID, you should probably call uuid1() or uuid4().
Note that uuid1() may compromise privacy since it creates a UUID containing
the computer's network address.  uuid4() creates a random UUID.
Typical usage:
    >>> import uuid
    # make a UUID based on the host ID and current time
    >>> uuid.uuid1()    # doctest: +SKIP
    UUID('a8098c1a-f86e-11da-bd1a-00112444be1e')
    # make a UUID using an MD5 hash of a namespace UUID and a name
    >>> uuid.uuid3(uuid.NAMESPACE_DNS, 'python.org')
    UUID('6fa459ea-ee8a-3ca4-894e-db77e160355e')
    # make a random UUID
    >>> uuid.uuid4()    # doctest: +SKIP
    UUID('16fd2706-8baf-433b-82eb-8c7fada847da')
    # make a UUID using a SHA-1 hash of a namespace UUID and a name
    >>> uuid.uuid5(uuid.NAMESPACE_DNS, 'python.org')
    UUID('886313e1-3b8a-5372-9b90-0c9aee199e5d')
    # make a UUID from a string of hex digits (braces and hyphens ignored)
    >>> x = uuid.UUID('{00010203-0405-0607-0809-0a0b0c0d0e0f}')
    # convert a UUID to a string of hex digits in standard form
    >>> str(x)
    '00010203-0405-0607-0809-0a0b0c0d0e0f'
    # get the raw 16 bytes of the UUID
    >>> x.bytes
    b'\x00\x01\x02\x03\x04\x05\x06\x07\x08\t\n\x0b\x0c\r\x0e\x0f'
    # make a UUID from a 16-byte string
    >>> uuid.UUID(bytes=x.bytes)
    UUID('00010203-0405-0607-0809-0a0b0c0d0e0f')
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//enum", enum="enum")
load("@vendor//Crypto/Random", Random="Random")
load("@vendor//Crypto/Util/number",
     bytes_to_long="bytes_to_long",
     long_to_bytes="long_to_bytes")

load("@stdlib//types", types="types")
load("@vendor//option/result", Error="Error")


RESERVED_NCS, RFC_4122, RESERVED_MICROSOFT, RESERVED_FUTURE = (
    'reserved for NCS compatibility',
    'specified in RFC 4122',
    'reserved for Microsoft compatibility',
    'reserved for future definition',
)

int_ = int      # The built-in int type
bytes_ = bytes  # The built-in bytes type


SafeUUID = enum.Enum('SafeUUID', [
    'safe',
    'unsafe',
    'unknown'
])



NotImplemented = larky.SENTINEL


def _UUID(hex=None,
          bytes=None,
          bytes_le=None,
          fields=None,
          int=None,
          version=None,
          is_safe=SafeUUID.unknown
):
    """Instances of the UUID class represent UUIDs as specified in RFC 4122.
    UUID objects are immutable, hashable, and usable as dictionary keys.
    Converting a UUID to a string with str() yields something in the form
    '12345678-1234-1234-1234-123456789abc'.  The UUID constructor accepts
    five possible forms: a similar string of hexadecimal digits, or a tuple
    of six integer fields (with 32-bit, 16-bit, 16-bit, 8-bit, 8-bit, and
    48-bit values respectively) as an argument named 'fields', or a string
    of 16 bytes (with all the integer fields in big-endian order) as an
    argument named 'bytes', or a string of 16 bytes (with the first three
    fields in little-endian order) as an argument named 'bytes_le', or a
    single 128-bit integer as an argument named 'int'.
    UUIDs have these read-only attributes:
        bytes       the UUID as a 16-byte string (containing the six
                    integer fields in big-endian byte order)
        bytes_le    the UUID as a 16-byte string (with time_low, time_mid,
                    and time_hi_version in little-endian byte order)
        fields      a tuple of the six integer fields of the UUID,
                    which are also available as six individual attributes
                    and two derived attributes:
            time_low                the first 32 bits of the UUID
            time_mid                the next 16 bits of the UUID
            time_hi_version         the next 16 bits of the UUID
            clock_seq_hi_variant    the next 8 bits of the UUID
            clock_seq_low           the next 8 bits of the UUID
            node                    the last 48 bits of the UUID
            time                    the 60-bit timestamp
            clock_seq               the 14-bit sequence number
        hex         the UUID as a 32-character hexadecimal string
        int         the UUID as a 128-bit integer
        urn         the UUID as a URN as specified in RFC 4122
        variant     the UUID variant (one of the constants RESERVED_NCS,
                    RFC_4122, RESERVED_MICROSOFT, or RESERVED_FUTURE)
        version     the UUID version number (1 through 5, meaningful only
                    when the variant is RFC_4122)
        is_safe     An enum indicating whether the UUID has been generated in
                    a way that is safe for multiprocessing applications, via
                    uuid_generate_time_safe(3).
    """
    self = larky.mutablestruct(__name__='UUID', __class__=_UUID)

    def __init__(hex,
                 bytes,
                 bytes_le,
                 fields,
                 int,
                 version,
                 is_safe):
        r"""Create a UUID from either a string of 32 hexadecimal digits,
        a string of 16 bytes as the 'bytes' argument, a string of 16 bytes
        in little-endian order as the 'bytes_le' argument, a tuple of six
        integers (32-bit time_low, 16-bit time_mid, 16-bit time_hi_version,
        8-bit clock_seq_hi_variant, 8-bit clock_seq_low, 48-bit node) as
        the 'fields' argument, or a single 128-bit integer as the 'int'
        argument.  When a string of hex digits is given, curly braces,
        hyphens, and a URN prefix are all optional.  For example, these
        expressions all yield the same UUID:

        UUID('{12345678-1234-5678-1234-567812345678}')
        UUID('12345678123456781234567812345678')
        UUID('urn:uuid:12345678-1234-5678-1234-567812345678')
        UUID(bytes='\x12\x34\x56\x78'*4)
        UUID(bytes_le='\x78\x56\x34\x12\x34\x12\x78\x56' +
                      '\x12\x34\x56\x78\x12\x34\x56\x78')
        UUID(fields=(0x12345678, 0x1234, 0x5678, 0x12, 0x34, 0x567812345678))
        UUID(int=0x12345678123456781234567812345678)

        Exactly one of 'hex', 'bytes', 'bytes_le', 'fields', or 'int' must
        be given.  The 'version' argument is optional; if given, the resulting
        UUID will have its variant and version set according to RFC 4122,
        overriding the given 'hex', 'bytes', 'bytes_le', 'fields', or 'int'.
        is_safe is an enum exposed as an attribute on the instance.

        It indicates whether the UUID has been generated in a way that is safe
        for multiprocessing applications, via uuid_generate_time_safe(3).

        """
        if (
                hex == None and
                bytes == None and
                bytes_le == None and
                fields == None and
                int == None
        ):
            return Error('one of the hex, bytes, bytes_le, fields, ' +
                         'or int arguments must be given').unwrap()
        if hex != None:
            hex = hex.replace('urn:', '').replace('uuid:', '')
            hex = hex.strip('{}').replace('-', '')
            if len(hex) != 32:
                return Error("ValueError: badly formed hexadecimal UUID string").unwrap()
            int = int_(hex, 16)
        if bytes_le != None:
            if len(bytes_le) != 16:
                return Error("ValueError: bytes_le is not a 16-char string").unwrap()
            bytes = (bytes_le[4-1::-1] + bytes_le[6-1:4-1:-1] +
                     bytes_le[8-1:6-1:-1] + bytes_le[8:])
        if bytes != None:
            if len(bytes) != 16:
                return Error("ValueError: bytes is not a 16-char string").unwrap()
            if not types.is_bytelike(bytes):
                fail("bytes_ is not byte-like: %r type(%s)", bytes, type(bytes))
            int = bytes_to_long(bytes)
        if fields != None:
            if len(fields) != 6:
                return Error("ValueError: fields is not a 6-tuple").unwrap()
            (time_low, time_mid, time_hi_version,
             clock_seq_hi_variant, clock_seq_low, node) = fields
            if not (0 <= time_low) and (time_low < 1<<32):
                return Error("ValueError: field 1 out of range (need a 32-bit value)").unwrap()
            if not (0 <= time_mid) and (time_mid < 1<<16):
                return Error("ValueError: field 2 out of range (need a 16-bit value)").unwrap()
            if not (0 <= time_hi_version) and (time_hi_version < 1<<16):
                return Error("ValueError: field 3 out of range (need a 16-bit value)").unwrap()
            if not (0 <= clock_seq_hi_variant) and (clock_seq_hi_variant < 1<<8):
                return Error("ValueError: field 4 out of range (need an 8-bit value)").unwrap()
            if not (0 <= clock_seq_low) and (clock_seq_low < 1<<8):
                return Error("ValueError: field 5 out of range (need an 8-bit value)").unwrap()
            if not (0 <= node) and (node < 1<<48):
                return Error("ValueError: field 6 out of range (need a 48-bit value)").unwrap()
            clock_seq = (clock_seq_hi_variant << 8) | clock_seq_low
            int = ((time_low << 96) | (time_mid << 80) |
                   (time_hi_version << 64) | (clock_seq << 48) | node)
        if int != None:
            if not (0 <= int) and (int < 1<<128):
                return Error("ValueError: int is out of range (need a 128-bit value)").unwrap()
        if version != None:
            if not (1 <= version) and (version <= 5):
                return Error("ValueError: illegal version number").unwrap()
            # Set the variant to RFC 4122.
            int &= ~(0xc000 << 48)
            int |= 0x8000 << 48
            # Set the version number.
            int &= ~(0xf000 << 64)
            int |= version << 76
        self.int = int
        self.is_safe = is_safe
        return self
    self = __init__(hex, bytes, bytes_le, fields, int, version, is_safe)

    def __eq__(other):
        if types.is_instance(other, _UUID):
            return self.int == other.int
        return NotImplemented
    self.__eq__ = __eq__

    # Q. What's the value of being able to sort UUIDs?
    # A. Use them as keys in a B-Tree or similar mapping.

    def __lt__(other):
        if types.is_instance(other, _UUID):
            return self.int < other.int
        return NotImplemented
    self.__lt__ = __lt__

    def __gt__(other):
        if types.is_instance(other, _UUID):
            return self.int > other.int
        return NotImplemented
    self.__gt__ = __gt__

    def __le__(other):
        if types.is_instance(other, _UUID):
            return self.int <= other.int
        return NotImplemented
    self.__le__ = __le__

    def __ge__(other):
        if types.is_instance(other, _UUID):
            return self.int >= other.int
        return NotImplemented
    self.__ge__ = __ge__

    def __hash__():
        return hash(self.int)
    self.__hash__ = __hash__

    def __int__():
        return self.int
    self.__int__ = __int__

    def __index__():
        return self.int
    self.__index__ = __index__

    def __repr__():
        return '%s(%r)' % (self.__name__, str(self))
    self.__repr__ = __repr__

    def __setattr__(name, value):
        return Error("TypeError: UUID objects are immutable").unwrap()
    self.__setattr__ = __setattr__

    def __str__():
        hex = '%x' % (self.int,)
        return '%s-%s-%s-%s-%s' % (
            hex[:8], hex[8:12], hex[12:16], hex[16:20], hex[20:])
    self.__str__ = __str__

    def _bytes():
        return long_to_bytes(self.int, 16)
    self.bytes = larky.property(_bytes)

    def _bytes_le():
        bytes = self.bytes
        return (bytes[4-1::-1] + bytes[6-1:4-1:-1] + bytes[8-1:6-1:-1] +
                bytes[8:])
    self.bytes_le = larky.property(_bytes_le)

    def _fields():
        return (self.time_low, self.time_mid, self.time_hi_version,
                self.clock_seq_hi_variant, self.clock_seq_low, self.node)
    self.fields = larky.property(_fields)

    def _time_low():
        return self.int >> 96
    self.time_low = larky.property(_time_low)

    def _time_mid():
        return (self.int >> 80) & 0xffff
    self.time_mid = larky.property(_time_mid)

    def _time_hi_version():
        return (self.int >> 64) & 0xffff
    self.time_hi_version = larky.property(_time_hi_version)

    def _clock_seq_hi_variant():
        return (self.int >> 56) & 0xff
    self.clock_seq_hi_variant = larky.property(_clock_seq_hi_variant)

    def _clock_seq_low():
        return (self.int >> 48) & 0xff
    self.clock_seq_low = larky.property(_clock_seq_low)

    def _time():
        return (((self.time_hi_version & 0x0fff) << 48) |
                (self.time_mid << 32) | self.time_low)
    self.time = larky.property(_time)

    def _clock_seq():
        return (((self.clock_seq_hi_variant & 0x3f) << 8) |
                self.clock_seq_low)
    self.clock_seq = larky.property(_clock_seq)

    def _node():
        return self.int & 0xffffffffffff
    self.node = larky.property(_node)

    def _hex():
        return '%x' % self.int
    self.hex = larky.property(_hex)

    def _urn():
        return 'urn:uuid:' + str(self)
    self.urn = larky.property(_urn)

    def _variant():
        if not self.int & (0x8000 << 48):
            return RESERVED_NCS
        elif not self.int & (0x4000 << 48):
            return RFC_4122
        elif not self.int & (0x2000 << 48):
            return RESERVED_MICROSOFT
        else:
            return RESERVED_FUTURE
    self.variant = larky.property(_variant)

    def _version():
        # The version bits are only meaningful for RFC 4122 UUIDs.
        if self.variant == RFC_4122:
            return int_((self.int >> 76) & 0xf)
    self.version = larky.property(_version)
    # this is *a hashable version* of mutablestruct
    # (effectively making it immutable)
    return larky.struct(**self.__dict__)


def uuid4():
    """Generate a random UUID."""
    _uuid = _UUID(bytes=bytes_(Random.new().read(16)), version=4)
    return _uuid


uuid = larky.struct(
    uuid4=uuid4,
    RESERVED_NCS=RESERVED_NCS,
    RFC_4122=RFC_4122,
    RESERVED_MICROSOFT=RESERVED_MICROSOFT,
    RESERVED_FUTURE=RESERVED_FUTURE,
)

