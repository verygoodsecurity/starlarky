def SafeUUID(Enum):
    """
    Instances of the UUID class represent UUIDs as specified in RFC 4122.
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
2021-03-02 20:54:30,065 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:54:30,065 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, hex=None, bytes=None, bytes_le=None, fields=None,
                       int=None, version=None,
                       *, is_safe=SafeUUID.unknown):
        """
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

                is_safe is an enum exposed as an attribute on the instance.  It
                indicates whether the UUID has been generated in a way that is safe
                for multiprocessing applications, via uuid_generate_time_safe(3).
        
        """
    def __getstate__(self):
        """
        'int'
        """
    def __setstate__(self, state):
        """
        'int'
        """
    def __eq__(self, other):
        """
         Q. What's the value of being able to sort UUIDs?
         A. Use them as keys in a B-Tree or similar mapping.


        """
    def __lt__(self, other):
        """
        '%s(%r)'
        """
    def __setattr__(self, name, value):
        """
        'UUID objects are immutable'
        """
    def __str__(self):
        """
        '%032x'
        """
    def bytes(self):
        """
        'big'
        """
    def bytes_le(self):
        """
        '%032x'
        """
    def urn(self):
        """
        'urn:uuid:'
        """
    def variant(self):
        """
         The version bits are only meaningful for RFC 4122 UUIDs.

        """
def _popen(command, *args):
    """
    '/sbin'
    """
def _is_universal(mac):
    """
    b':'
    """
def _ifconfig_getnode():
    """
    Get the hardware address on Unix by running ifconfig.
    """
def _ip_getnode():
    """
    Get the hardware address on Unix by running ip.
    """
def _arp_getnode():
    """
    Get the hardware address on Unix by running arp.
    """
def _lanscan_getnode():
    """
    Get the hardware address on Unix by running lanscan.
    """
def _netstat_getnode():
    """
    Get the hardware address on Unix by running netstat.
    """
def _ipconfig_getnode():
    """
    Get the hardware address on Windows by running ipconfig.exe.
    """
def _netbios_getnode():
    """
    Get the hardware address on Windows using NetBIOS calls.
        See http://support.microsoft.com/kb/118623 for details.
    """
def _load_system_functions():
    """

        Try to load platform-specific functions for generating uuids.
    
    """
                def _generate_time_safe():
                    """
                    'uuid_generate_time'
                    """
                def _generate_time_safe():
                    """
                     On Windows prior to 2000, UuidCreate gives a UUID containing the
                     hardware address.  On Windows 2000 and later, UuidCreate makes a
                     random UUID and UuidCreateSequential gives a UUID containing the
                     hardware address.  These routines are provided by the RPC runtime.
                     NOTE:  at least on Tim's WinXP Pro SP2 desktop box, while the last
                     6 bytes returned by UuidCreateSequential are fixed, they don't appear
                     to bear any relationship to the MAC address of any network device
                     on the box.

                    """
def _unix_getnode():
    """
    Get the hardware address on Unix using the _uuid extension module
        or ctypes.
    """
def _windll_getnode():
    """
    Get the hardware address on Windows using ctypes.
    """
def _random_getnode():
    """
    Get a random node ID.
    """
def getnode(*, getters=None):
    """
    Get the hardware address as a 48-bit positive integer.

        The first time this runs, it may launch a separate program, which could
        be quite slow.  If all attempts to obtain the hardware address fail, we
        choose a random 48-bit number with its eighth bit set to 1 as recommended
        in RFC 4122.
    
    """
def uuid1(node=None, clock_seq=None):
    """
    Generate a UUID from a host ID, sequence number, and the current time.
        If 'node' is not given, getnode() is used to obtain the hardware
        address.  If 'clock_seq' is given, it is used as the sequence number;
        otherwise a random 14-bit sequence number is chosen.
    """
def uuid3(namespace, name):
    """
    Generate a UUID from the MD5 hash of a namespace UUID and a name.
    """
def uuid4():
    """
    Generate a random UUID.
    """
def uuid5(namespace, name):
    """
    Generate a UUID from the SHA-1 hash of a namespace UUID and a name.
    """
