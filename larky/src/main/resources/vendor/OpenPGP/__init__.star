# Pure Python implementation of OpenPGP <http://tools.ietf.org/html/rfc4880>
# Port of openpgp-php <http://github.com/bendiken/openpgp-php>
load("@stdlib//base64", base64="base64")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//types", types="types")
load("@stdlib//bz2", bz2="bz2")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//hashlib", hashlib="hashlib")
load("@stdlib//itertools", itertools="itertools")
load("@stdlib//larky", larky="larky")
load("@stdlib//math", ceil="ceil", floor="floor", log="log")
load("@stdlib//re", re="re")
load("@stdlib//struct", pack="pack", unpack="unpack")
load("@stdlib//textwrap", textwrap="textwrap")
load("@stdlib//zlib", zlib="zlib")
load("@stdlib//operator", operator="operator")
load("@vendor//option/result", Result="Result", Error="Error")

WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION

__all__ = [
    # Library start
    "Message",
    "S2K",
    # Packets
    "Packet",
    "AsymmetricSessionKeyPacket",
    "CompressedDataPacket",
    "EmbeddedSignaturePacket",
    "EncryptedDataPacket",
    "ExperimentalPacket",
    "IntegrityProtectedDataPacket",
    "LiteralDataPacket",
    "MarkerPacket",
    "ModificationDetectionCodePacket",
    "OnePassSignaturePacket",
    "OpenPGPException",
    "PublicKeyPacket",
    "PublicSubkeyPacket",
    "SecretKeyPacket",
    "SecretSubkeyPacket",
    "SignaturePacket",
    "SymmetricSessionKeyPacket",
    "TrustPacket",
    "UserAttributePacket",
    "UserIDPacket",
    # Utilities
    "unarmor",
    "enarmor",
    "crc24",
    "checksum",
    "bitlength",
    "PushbackGenerator",
]


def time():
    return 1648941415


def unarmor(text):
    """Convert ASCII-armored data into binary
    http://tools.ietf.org/html/rfc4880#section-6
    http://tools.ietf.org/html/rfc2045
    """
    result = []
    chunks = re.findall(
        r"\n-----BEGIN [^-]+-----\n(.*?)\n-----END [^-]+-----\n",
        "\n" + text.replace("\r\n", "\n").replace("\r", "\n") + "\n",
        re.S,
    )

    for chunk in chunks:
        headers, data = chunk.split("\n\n")
        crc = data[-5:]
        data = base64.b64decode(codecs.encode(data[:-5], encoding="utf-8"))
        if crc[0] != "=":
            fail("OpenPGPException: CRC24 check failed")
        if (
            crc24(data)
            != unpack("!L", b"\0" + base64.b64decode(codecs.encode(crc[1:], encoding="utf-8")))[0]
        ):
            fail("OpenPGPException: CRC24 check failed")
        result.append((headers, data))

    return result


def crc24(data):
    """
    http://tools.ietf.org/html/rfc4880#section-6
    http://tools.ietf.org/html/rfc4880#section-6.1
    """
    crc = 0x00B704CE
    for i in range(0, len(data)):
        crc ^= (ord(data[i : i + 1]) & 255) << 16
        for j in range(0, 8):
            crc <<= 1
            if crc & 0x01000000:
                crc ^= 0x01864CFB
    return crc & 0x00FFFFFF


def enarmor(data, marker="PUBLIC KEY BLOCK", headers=None, lineWidth=64):
    """
    @see http://tools.ietf.org/html/rfc4880#section-6.2 OpenPGP Message Format / Ascii Armor
    @see http://tools.ietf.org/html/rfc2045 Base64 encoding

    @param data: binary data to encode
    @type  data: bytes

    @param marker: The header line text is chosen based upon the type
        of data that is being encoded in Armor, and how it is being encoded.
        Header line texts include the following strings:
            - MESSAGE
            - PUBLIC KEY BLOCK
            - PRIVATE KEY BLOCK
            - MESSAGE, PART X/Y
            - MESSAGE, PART X
            - SIGNATURE
    @type  marker: str

    @param headers: key value, e.g {'Version' : 'GnuPG v2.0.22 (MingW32)'}
    @type  headers: None | generator[(str, str)]

    @param lineWidth: GnuPG uses 64 bit, RFC4880 limits to 76
    @type  lineWidth: int

    @rtype: str
    """

    def _iter_enarmor(data):
        """
        @type data: bytes

        @param marker: Specifies the kind of data to armor
        @type  marker: str

        @param headers: optional header fields
            (dict keys will be sorted lexicographically)
        @type  headers: dict | [(keyString, valueString)] | None

        @rtype: list[str]
        """
        result = [
            "-----BEGIN PGP " + str(marker).upper() + "-----"
            ]
        headersDict = headers or {}
        if types.is_dict(headersDict):
            headerItems = list(headersDict.items())
            headerItems.sort()
        else:
            headerItems = list(headersDict)  # already list of key-value.pairs

        for (key, value) in headerItems:
            result.append("{key}: {value}".format(key=key, value=value))
        result.append("")  # empty line

        text = base64.b64encode(data)  # bytes in Python 3!
        # Python 3
        textStr = text.decode("ascii")
        # max 76 chars per line!
        result.extend(textwrap.wrap(textStr, width=lineWidth))

        # unsigned long with 4 bypte/32 bit in byte-order Big Endian
        checksumBytes = pack(">L", crc24(data))
        checksumBase64 = base64.b64encode(checksumBytes[1:])  # bytes in Python 3!
        checksumStr = checksumBase64.decode("ascii")
        result.append("=" + checksumStr)  # take only the last 3 bytes
        result.append("-----END PGP " + str(marker).upper() + "-----")
        result.append("")  # final line break
        return result

    return "\n".join(_iter_enarmor(data))


def bitlength(data):
    """http://tools.ietf.org/html/rfc4880#section-12.2"""
    if ord(data[0:1]) == 0:
        fail("OpenPGPException: Tried to get bitlength of string with leading 0")
    print("bitlength data - ord: ", ord(data[0:1]))
    print("bitlength data - log: ", log(ord(data[0:1]), 2))
    print("bitlength data - floor: ", floor(log(ord(data[0:1]), 2)))
    print("bitlength data - int: ", int(floor(log(ord(data[0:1]), 2))))
    return (len(data) - 1) * 8 + int(floor(log(ord(data[0:1]), 2))) + 1


def checksum(data):
    mkChk = 0
    for i in range(0, len(data)):
        mkChk = (mkChk + ord(data[i : i + 1])) % 65536
    return mkChk


def _gen_one(i):
    return iter([i])


def _ensure_bytes(n, chunk, g):
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if len(chunk) >= n:
            break
        chunk += next(g)
    return chunk


def _slurp(g):
    bs = b""
    for chunk in g:
        bs += chunk
    return bs


def _S2K():
    cls = larky.mutablestruct(__name__='S2K')

    def parse(input_or_g):
        if hasattr(input_or_g, "next") or hasattr(input_or_g, "__next__"):
            g = PushbackGenerator(input_or_g)
        else:
            g = PushbackGenerator(_gen_one(input_or_g))

        chunk = _ensure_bytes(1, next(g), g)
        s2k_type = ord(chunk[0:1])
        if s2k_type == 0:
            chunk = _ensure_bytes(2, chunk, g)
            if len(chunk) > 2:
                g.push(chunk[2:])
            return (cls(b"UNSALTED", ord(chunk[1:2]), 0, s2k_type), 2)
        elif s2k_type == 1:
            chunk = _ensure_bytes(10, chunk, g)
            if len(chunk) > 10:
                g.push(chunk[10:])
            return (cls(chunk[2:10], ord(chunk[1:2]), 0, s2k_type), 10)
        elif s2k_type == 3:
            chunk = _ensure_bytes(11, chunk, g)
            if len(chunk) > 11:
                g.push(chunk[11:])
            return (
                cls(
                    chunk[2:10],
                    ord(chunk[1:2]),
                    cls.decode_s2k_count(ord(chunk[10:11])),
                    s2k_type,
                ),
                11,
            )
    cls.parse = parse

    def decode_s2k_count(c):
        return int(16 + (c & 15)) << ((c >> 4) + 6)
    cls.decode_s2k_count = decode_s2k_count

    def encode_s2k_count(iterations):
        if iterations >= 65011712:
            return 255

        count = iterations >> 6
        c = 0
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if count < 32:
                break
            count = count >> 1
            c += 1

        result = (c << 4) | (count - 16)

        if cls.decode_s2k_count(result) < iterations:
            return result + 1

        return result
    cls.encode_s2k_count = encode_s2k_count

    def __new__(salt=b"BADSALT", hash_algorithm=10, count=65536, type=3):

        self = larky.mutablestruct(__name__='S2K', __class__=cls)

        def __init__(salt, hash_algorithm, count, type):
            self.type = type
            self.hash_algorithm = hash_algorithm
            self.salt = salt
            self.count = count
            return self
        self = __init__(salt, hash_algorithm, count, type)

        def to_bytes():
            bs = pack("!B", self.type)
            if self.type in [0, 1, 3]:
                bs += pack("!B", self.hash_algorithm)
            if self.type in [1, 3]:
                bs += self.salt
            if self.type in [3]:
                bs += pack("!B", cls.encode_s2k_count(self.count))
            return bs
        self.to_bytes = to_bytes

        def raw_hash(s, prefix=b""):
            hasher = hashlib.new(
                SignaturePacket.hash_algorithms[self.hash_algorithm].lower()
            )
            hasher.update(prefix)
            hasher.update(s)
            return hasher.digest()
        self.raw_hash = raw_hash

        def iterate(s, prefix=b""):
            hasher = hashlib.new(
                SignaturePacket.hash_algorithms[self.hash_algorithm].lower()
            )
            hasher.update(prefix)
            hasher.update(s)
            remaining = self.count - len(s)
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                if remaining <= 0:
                    break
                hasher.update(s[0:remaining])
                remaining -= len(s)
            return hasher.digest()
        self.iterate = iterate

        def sized_hash(hasher, s, size):
            hsh = hasher(s)
            prefix = b"\0"
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                if len(hsh) >= size:
                    break
                hsh += hasher(s, prefix)
                prefix += b"\0"

            return hsh[0:size]
        self.sized_hash = sized_hash

        def make_key(passphrase, size):
            if self.type == 0:
                return self.sized_hash(self.raw_hash, passphrase, size)
            elif self.type == 1:
                return self.sized_hash(self.raw_hash, self.salt + passphrase, size)
            elif self.type == 3:
                return self.sized_hash(self.iterate, self.salt + passphrase, size)
        self.make_key = make_key
    cls.__call__ = __new__

    return cls


S2K = _S2K()


def g_next(g):
    return next(iter(g))


def PushbackGenerator(g):
    self = larky.mutablestruct(__name__='PushbackGenerator', __class__=PushbackGenerator)
    def __init__(g):
        self._g = g
        self._pushback = []
        return self
    self = __init__(g)

    def __iter__():
        return self
    self.__iter__ = __iter__

    def __next__():
        if len(self._pushback):
            return self._pushback.pop(0)
        # to avoid recursion
        if builtins.isinstance(self._g, PushbackGenerator):
            if self._g.hasNext():
                return self._g._pushback.pop(0)
        return next(self._g)
    self.__next__ = __next__

    def hasNext():
        if len(self._pushback) > 0:
            return True
        chunk = next(self, larky.SENTINEL)
        if chunk == larky.SENTINEL:
            return False
        else:
            self.push(chunk)
            return True
    self.hasNext = hasNext

    def push(i):
        if hasattr(self._g, "_pushback"):
            self._g._pushback.insert(0, i)
        else:
            self._pushback.insert(0, i)
    self.push = push
    return self


def _Message():
    """Represents an OpenPGP message (set of packets)
    http://tools.ietf.org/html/rfc4880#section-4.1
    http://tools.ietf.org/html/rfc4880#section-11
    http://tools.ietf.org/html/rfc4880#section-11.3
    """
    cls = larky.mutablestruct(__name__='Message')

    def parse(input_data):
        """http://tools.ietf.org/html/rfc4880#section-4.1
        http://tools.ietf.org/html/rfc4880#section-4.2
        """
        m = cls([])  # Nothing parsed yet
        if hasattr(input_data, "next") or hasattr(input_data, "__next__"):
            m._input = PushbackGenerator(input_data)
        else:
            m._input = PushbackGenerator(_gen_one(input_data))

        return m
    cls.parse = parse

    def __new__(packets=None):
        self = larky.mutablestruct(__name__='Message', __class__=cls)

        def __init__(packets):
            self._packets_start = packets or []
            self._packets_end = []
            self._input = None
            return self
        self = __init__(packets)

        def to_bytes():
            b = b""
            for p in self:
                b += p.to_bytes()
            return b
        self.to_bytes = to_bytes

        def signatures():
            """Extract signed objects from a well-formatted message
            Recurses into CompressedDataPacket
            http://tools.ietf.org/html/rfc4880#section-11
            """
            msg = self
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                # if not builtins.isinstance(msg[0], CompressedDataPacket):
                #     break
                print(msg[0])
                msg = msg[0]

            key = None
            userid = None
            subkey = None
            sigs = []
            final_sigs = []

            for p in msg:
                if builtins.isinstance(p, LiteralDataPacket):
                    return [
                        (p, [x for x in msg if builtins.isinstance(x, SignaturePacket)])
                    ]
                elif builtins.isinstance(p, PublicSubkeyPacket) or builtins.isinstance(p, SecretSubkeyPacket):
                    if userid:
                        final_sigs.append((key, userid, sigs))
                        userid = None
                    elif subkey:
                        final_sigs.append((key, subkey, sigs))
                        key = None
                    sigs = []
                    subkey = p
                elif builtins.isinstance(p, PublicKeyPacket):
                    if userid:
                        final_sigs.append((key, userid, sigs))
                        userid = None
                    elif subkey:
                        final_sigs.append((key, subkey, sigs))
                        subkey = None
                    elif key:
                        final_sigs.append((key, sigs))
                        key = None
                    sigs = []
                    key = p
                elif builtins.isinstance(p, UserIDPacket):
                    if userid:
                        final_sigs.append((key, userid, sigs))
                        userid = None
                    elif key:
                        final_sigs.append((key, sigs))
                    sigs = []
                    userid = p
                elif builtins.isinstance(p, SignaturePacket):
                    sigs.append(p)

            if userid:
                final_sigs.append((key, userid, sigs))
            elif subkey:
                final_sigs.append((key, subkey, sigs))
            elif key:
                final_sigs.append((key, sigs))

            return final_sigs
        self.signatures = signatures

        def verified_signatures(verifiers):
            """Function to extract verified signatures
            verifiers is an array of callbacks formatted like {'RSA': {'SHA256': CALLBACK}} that take two parameters: raw message and signature packet
            """
            signed = self.signatures()
            vsigned = []

            for sign in signed:
                vsigs = []
                for sig in sign[-1]:
                    verifier = verifiers[sig.key_algorithm_name()][
                        sig.hash_algorithm_name()
                    ]
                    if verifier and self.verify_one(verifier, sign, sig):
                        vsigs.append(sig)
                vsigned.append(sign[:-1] + (vsigs,))

            return vsigned
        self.verified_signatures = verified_signatures

        def verify_one(verifier, sign, sig):
            raw = None
            if builtins.isinstance(sign[0], LiteralDataPacket):
                sign[0].normalize()
                raw = sign[0].data
            elif len(sign) > 1 and builtins.isinstance(sign[1], UserIDPacket):
                raw = b"".join(
                    sign[0].fingerprint_material()
                    + [pack("!B", 0xB4), pack("!L", len(sign[1].body())), sign[1].body()]
                )
            elif len(sign) > 1 and (
                builtins.isinstance(sign[1], PublicSubkeyPacket)
                or builtins.isinstance(sign[1], SecretSubkeyPacket)
            ):
                raw = b"".join(
                    sign[0].fingerprint_material() + sign[1].fingerprint_material()
                )
            elif builtins.isinstance(sign[0], PublicKeyPacket):
                raw = sign[0].fingerprint_material()
            else:
                raw = None

            return verifier(raw + sig.trailer, sig)
        self.verify_one = verify_one

        def force():
            packets = []
            for p in self:
                packets.append(p)
            return packets
        self.force = force

        # def _iter(i):
        #     # stopping condition
        #     if i >= len(self._impl._items):
        #         return StopIteration()
        #
        #     _idx, k, v = self._impl._items[i]
        #
        #     if self._version != self._impl._version:
        #         return Error("RuntimeError: Dictionary changed during iteration")
        #     return Ok((k, v,))
        #
        # self._iter = _iter
        def __iter__():
            results = []
            # Already parsed packets
            for p in self._packets_start:
                results.append(p)

            if self._input:
                for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                    if not self._input.hasNext():
                        break
                    packet = Packet.parse(self._input)
                    if packet:
                        self._packets_start.append(packet)
                        results.append(packet)
                        # return packet
                    else:
                        fail("OpenPGPException: Parsing is stuck")
                self._input = None  # Parsing done

            # Appended packets
            for p in self._packets_end:
                results.append(p)
            return iter(results)
        self.__iter__ = __iter__

        # def __iter__():
        #     return larky.DeterministicGenerator(self.__iter)
        # self.__iter__ = __iter__

        def __getitem__(item):
            i = 0
            for p in self:
                if i == item:
                    return p
                i += 1
        self.__getitem__ = __getitem__

        def append(item):
            self._packets_end.append(item)
        self.append = append

        def __repr__():
            return "%s: %s" % (type(self), self)
        self.__repr__ = __repr__

        def __eq__(other):
            if type(other) == type(self):
                return self.force() == other.force()
            return False
        self.__eq__ = __eq__

        def __ne__(other):
            return not self.__eq__(other)
        self.__ne__ = __ne__
        return self
    cls.__call__ = __new__
    return cls


Message = _Message()


def _Packet():
    """OpenPGP packet.
    http://tools.ietf.org/html/rfc4880#section-4.1
    http://tools.ietf.org/html/rfc4880#section-4.3
    """
    cls = larky.mutablestruct(__name__='Packet')

    def parse(input_data):
        if hasattr(input_data, "next") or hasattr(input_data, "__next__"):
            g = PushbackGenerator(input_data)
        else:
            g = PushbackGenerator(_gen_one(input_data))

        packet = None
        # If there is not even one byte, then there is no packet at all
        chunk = _ensure_bytes(1, next(g), g)
        print(chunk.hex(":"))

        # Parse header
        if ord(chunk[0:1]) & 64:
            print("we are in new format")
            tag, data_length = Packet.parse_new_format(chunk, g)
        else:
            print("we are in old format")
            tag, data_length = Packet.parse_old_format(chunk, g)

        print("tag:", tag, "data_length:", data_length)
        if not data_length:
            rval = Result.Ok(g).map(_slurp)
            if rval == StopIteration():
                fail("OpenPGPException: Not enough bytes")
            chunk = rval.unwrap()
            # chunk = _slurp(g)
            data_length = len(chunk)
            g.push(chunk)

        if tag:
            packet_class = Packet.tags.get(tag, Packet)
            print("packet_class:", packet_class, "tag:", tag)
            packet = packet_class()
            packet.tag = tag
            packet.input = g
            packet.length = data_length
            packet.read()
            # packet_read = Result.Ok(packet.read).map(lambda x: x.read())
            # if not packet_read.is_ok:
            #     fail("OpenPGPException: Not enough bytes")
            # Remove excess bytes
            packet.read_bytes(packet.length)
            # packet_read = Result.Ok(packet.read_bytes).map(lambda x: x(packet.length))
            # if not packet_read.is_ok:
            #     fail("OpenPGPException: Not enough bytes")
            # packet.read_bytes(packet.length)
            packet.input = None
            packet.length = None
        return packet
    cls.parse = parse

    def parse_new_format(chunk, g):
        """Parses a new-format (RFC 4880) OpenPGP packet.
        http://tools.ietf.org/html/rfc4880#section-4.2.2
        """

        chunk = _ensure_bytes(2, chunk, g)
        tag = ord(chunk[0:1]) & 63
        length = ord(chunk[1:2])

        if length < 192:  # One octet length
            if len(chunk) > 2:
                g.push(chunk[2:])
            return (tag, length)
        if length > 191 and length < 224:  # Two octet length
            chunk = _ensure_bytes(3, chunk, g)
            if len(chunk) > 2:
                g.push(chunk[3:])
            return (tag, ((length - 192) << 8) + ord(chunk[2:3]) + 192)
        if length == 255:  # Five octet length
            chunk = _ensure_bytes(6, chunk, g)
            if len(chunk) > 6:
                g.push(chunk[6:])
            return (tag, unpack("!L", chunk[2:6])[0])
        # TODO: Partial body lengths. 1 << ($len & 0x1F)
    cls.parse_new_format = parse_new_format

    def parse_old_format(chunk, g):
        """Parses an old-format (PGP 2.6.x) OpenPGP packet.
        http://tools.ietf.org/html/rfc4880#section-4.2.1
        """
        chunk = _ensure_bytes(1, chunk, g)
        tag = ord(chunk[0:1])
        length = tag & 3
        tag = (tag >> 2) & 15
        if (
            length == 0
        ):  # The packet has a one-octet length. The header is 2 octets long.
            head_length = 2
            chunk = _ensure_bytes(head_length, chunk, g)
            data_length = ord(chunk[1:2])
        elif (
            length == 1
        ):  # The packet has a two-octet length. The header is 3 octets long.
            head_length = 3
            chunk = _ensure_bytes(head_length, chunk, g)
            data_length = unpack("!H", chunk[1:3])[0]
        elif (
            length == 2
        ):  # The packet has a four-octet length. The header is 5 octets long.
            head_length = 5
            chunk = _ensure_bytes(head_length, chunk, g)
            data_length = unpack("!L", chunk[1:5])[0]
        elif (
            length == 3
        ):  # The packet is of indeterminate length. The header is 1 octet long.
            head_length = 1
            chunk = _ensure_bytes(head_length, chunk, g)
            data_length = None

        if len(chunk) > head_length:
            g.push(chunk[head_length:])
        return (tag, data_length)
    cls.parse_old_format = parse_old_format

    cls.tags = {}  # Actual data populated at end of file

    def __new__(data=None):
        self = larky.mutablestruct(__name__='Packet', __class__=cls)

        def __init__(data):
            for tag in Packet.tags:
                if larky.impl_function_name(Packet.tags[tag]) == cls.__name__:
                    self.tag = tag
                    break
            self.data = data
            return self
        self = __init__(data)

        def read():
            # Will normally be overridden by subclasses
            self.data = self.read_bytes(self.length)
        self.read = read

        def body():
            return self.data  # Will normally be overridden by subclasses
        self.body = body

        def header_and_body():
            body = self.body()  # Get body first, we will need it's length
            tag = pack("!B", self.tag | 0xC0)  # First two bits are 1 for new packet format
            size = pack("!B", 255) + pack(
                "!L", body and len(body) or 0
            )  # Use 5-octet lengths
            return {"header": tag + size, "body": body}
        self.header_and_body = header_and_body

        def to_bytes():
            data = self.header_and_body()
            return data["header"] + (data["body"] and data["body"] or b"")
        self.to_bytes = to_bytes

        def read_timestamp():
            """ttp://tools.ietf.org/html/rfc4880#section-3.5"""
            return self.read_unpacked(4, "!L")
        self.read_timestamp = read_timestamp

        def read_mpi():
            """http://tools.ietf.org/html/rfc4880#section-3.2"""
            length = self.read_unpacked(2, "!H")  # length in bits
            length = (int)((length + 7) / 8)  # length in bytes
            return self.read_bytes(length)
        self.read_mpi = read_mpi

        def read_unpacked(count, fmt):
            """http://docs.python.org/library/struct.html"""
            unpacked = unpack(fmt, self.read_bytes(count))
            return unpacked[0]  # unpack returns tuple
        self.read_unpacked = read_unpacked

        def read_byte():
            byte = self.read_bytes(1)
            return byte and byte[0:1] or None
        self.read_byte = read_byte

        def read_bytes(count):
            chunk = _ensure_bytes(count, b"", self.input)
            if len(chunk) > count:
                self.input.push(chunk[count:])
            self.length -= count
            return chunk[:count]
        self.read_bytes = read_bytes

        def __repr__():
            return "%s: %s" % (type(self), self)
        self.__repr__ = __repr__

        def __eq__(other):
            if type(other) == type(self):
                return self.__dict__ == other.__dict__
            return False
        self.__eq__ = __eq__

        def __ne__(other):
            return not self.__eq__(other)
        self.__ne__ = __ne__
        return self
    cls.__call__ = __new__
    return cls


Packet = _Packet()


def AsymmetricSessionKeyPacket(key_algorithm="", keyid="", encrypted_data="", version=3):
    """OpenPGP Public-Key Encrypted Session Key packet (tag 1).
    http://tools.ietf.org/html/rfc4880#section-5.1
    """
    cls = _Packet()
    cls.__name__ = 'AsymmetricSessionKeyPacket'
    cls.__class__ = AsymmetricSessionKeyPacket
    cls.__mro__ = [cls, Packet]

    def __init__(key_algorithm, keyid, encrypted_data, version):
        self = Packet()
        self.__name__  = cls.__name__
        self.__class__ = cls
        self.version = version
        self.keyid = keyid[-16:]
        self.key_algorithm = key_algorithm
        self.encrypted_data = encrypted_data
        return self
    cls.__call__ = __init__
    self = cls(key_algorithm, keyid, encrypted_data, version)

    def read():
        self.version = ord(self.read_byte())
        if self.version == 3:
            rawkeyid = self.read_bytes(8)
            self.keyid = ""
            for i in range(0, len(rawkeyid)):  # Store KeyID in Hex
                self.keyid += "%X" % ord(rawkeyid[i : i + 1])

            self.key_algorithm = ord(self.read_byte())
            self.encrypted_data = self.read_bytes(self.length)
        else:
            fail("OpenPGPException: " + "Unsupported AsymmetricSessionKeyPacket version: " + self.version
            )
    self.read = read

    def body():
        b = pack("!B", self.version)

        for i in range(0, len(self.keyid), 2):
            b += pack("!B", int(self.keyid[i] + self.keyid[i + 1], 16))

        b += pack("!B", self.key_algorithm)
        b += self.encrypted_data
        return b
    self.body = body
    return self


SUBPACKET_TYPES = {}


def _SignaturePacket():
    """OpenPGP Signature packet (tag 2).
    http://tools.ietf.org/html/rfc4880#section-5.2
    """
    cls = _Packet()
    cls.__name__ = 'SignaturePacket'
    cls.__class__ = _SignaturePacket
    cls.__mro__ = [cls, Packet]

    cls.hash_algorithms = {
        1: "MD5",
        2: "SHA1",
        3: "RIPEMD160",
        8: "SHA256",
        9: "SHA384",
        10: "SHA512",
        11: "SHA224",
    }

    super__init__ = cls.__call__
    def __new__(data=None, key_algorithm=None, hash_algorithm=None):
        def __init__(data, key_algorithm, hash_algorithm):
            self = super__init__(data)
            self.__class__ = cls
            self.__name__ = 'SignaturePacket'
            self.version = 4  # Default to version 4 sigs
            self.hash_algorithm = hash_algorithm
            self.hashed_subpackets = []
            self.unhashed_subpackets = self.hashed_subpackets
            if builtins.isinstance(self.hash_algorithm, str):
                for a in cls.hash_algorithms:
                    if cls.hash_algorithms[a] == self.hash_algorithm:
                        self.hash_algorithm = a
                        break
            self.key_algorithm = key_algorithm
            if builtins.isinstance(self.key_algorithm, str):
                for a in PublicKeyPacket.algorithms:
                    if PublicKeyPacket.algorithms[a] == self.key_algorithm:
                        self.key_algorithm = a
                        break
            if data:  # If we have any data, set up the creation time
                self.hashed_subpackets = [self.SignatureCreationTimePacket(time())]
            if builtins.isinstance(data, LiteralDataPacket):
                self.signature_type = data.format != "b" and 0x01 or 0x00
                data.normalize()
                data = data.data
            elif types.is_string(data):
                data = codecs.encode(data, encoding="utf-8")
            elif builtins.isinstance(data, Message) and builtins.isinstance(data[0], PublicKeyPacket):
                # data is a message with PublicKey first, UserID second
                key = b"".join(data[0].fingerprint_material())
                user_id = data[1].body()
                data = key + pack("!B", 0xB4) + pack("!L", len(user_id)) + user_id
            self.data = data  # Store to-be-signed data in here until the signing happens
            self.trailer = None
            self.hash_head = None
            return self
        self = __init__(data, key_algorithm, hash_algorithm)

        def sign_data(signers):
            """self.data must be set to the data to sign (done by constructor)
            signers in the same format as verifiers for Message.
            """
            self.trailer = self.calculate_trailer()
            signer = signers[self.key_algorithm_name()][self.hash_algorithm_name()]
            data = signer(self.data + self.trailer)
            self.data = []
            for mpi in data:
                if builtins.isinstance(mpi, int):
                    if hasattr(mpi, "to_bytes"):
                        self.data.append(
                            mpi.to_bytes(ceil(mpi.bit_length() / 8), byteorder="big")
                        )
                    else:  # For python 2
                        hex_mpi = "%X" % mpi
                        if len(hex_mpi) % 2 != 0:
                            hex_mpi = "0" + hex_mpi
                        final = b""
                        for i in range(0, len(hex_mpi), 2):
                            final += pack("!B", int(hex_mpi[i : i + 2], 16))
                        self.data.append(final)
                else:
                    self.data.append(mpi)
            self.hash_head = unpack("!H", b"".join(self.data)[0:2])[0]
        self.sign_data = sign_data

        def read():
            self.version = ord(self.read_byte())
            if self.version == 2 or self.version == 3:
                if not (ord(self.read_byte()) == 5):
                    fail("assert ord(self.read_byte()) == 5 failed!")
                self.signature_type = ord(self.read_byte())
                creation_time = self.read_timestamp()
                keyid = self.read_bytes(8)
                keyidHex = ""
                for i in range(0, len(keyid)):  # Store KeyID in Hex
                    keyidHex += "%X" % ord(keyid[i : i + 1])

                self.hashed_subpackets = []
                self.unhashed_subpackets = [
                    SignaturePacket.SignatureCreationTimePacket(creation_time),
                    SignaturePacket.IssuerPacket(keyidHex),
                ]

                self.key_algorithm = ord(self.read_byte())
                self.hash_algorithm = ord(self.read_byte())
                self.hash_head = self.read_unpacked(2, "!H")
                self.data = []
                for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                    if self.length <= 0:
                        break
                    self.data += [self.read_mpi()]
            elif self.version == 4:
                self.signature_type = ord(self.read_byte())
                self.key_algorithm = ord(self.read_byte())
                self.hash_algorithm = ord(self.read_byte())
                self.trailer = (
                    pack("!B", 4)
                    + pack("!B", self.signature_type)
                    + pack("!B", self.key_algorithm)
                    + pack("!B", self.hash_algorithm)
                )

                hashed_size = self.read_unpacked(2, "!H")
                hashed_subpackets = self.read_bytes(hashed_size)
                self.trailer += pack("!H", hashed_size) + hashed_subpackets
                self.hashed_subpackets = cls.get_subpackets(hashed_subpackets)

                self.trailer += (
                    pack("!B", 4) + pack("!B", 0xFF) + pack("!L", 6 + hashed_size)
                )

                unhashed_size = self.read_unpacked(2, "!H")
                self.unhashed_subpackets = cls.get_subpackets(
                    self.read_bytes(unhashed_size)
                )

                self.hash_head = self.read_unpacked(2, "!H")
                self.data = []
                for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                    if self.length <= 0:
                        break
                    self.data += [self.read_mpi()]
        self.read = read

        def calculate_trailer():
            # The trailer is just the top of the body plus some crap
            body = self.body_start()
            return body + pack("!B", 4) + pack("!B", 0xFF) + pack("!L", len(body))
        self.calculate_trailer = calculate_trailer

        def body_start():
            body = (
                pack("!B", 4)
                + pack("!B", self.signature_type)
                + pack("!B", self.key_algorithm)
                + pack("!B", self.hash_algorithm)
            )

            hashed_subpackets = b""
            for p in self.hashed_subpackets:
                hashed_subpackets += p.to_bytes()
            body += pack("!H", len(hashed_subpackets)) + hashed_subpackets

            return body
        self.body_start = body_start

        def body(trailer=False):
            if self.version == 2 or self.version == 3:
                body = (
                    pack("!B", self.version)
                    + pack("!B", 5)
                    + pack("!B", self.signature_type)
                )

                for p in self.unhashed_subpackets:
                    if builtins.isinstance(p, cls.SignatureCreationTimePacket):
                        body += pack("!L", p.data)
                        break

                for p in self.unhashed_subpackets:
                    if builtins.isinstance(p, cls.IssuerPacket):
                        for i in range(0, len(p.data), 2):
                            body += pack("!B", int(p.data[i : i + 2], 16))
                        break

                body += pack("!B", self.key_algorithm)
                body += pack("!B", self.hash_algorithm)
                body += pack("!H", self.hash_head)

                for mpi in self.data:
                    body += pack("!H", bitlength(mpi)) + mpi

                return body
            else:
                if not self.trailer:
                    self.trailer = self.calculate_trailer()
                body = self.trailer[0:-6]

                unhashed_subpackets = b""
                for p in self.unhashed_subpackets:
                    unhashed_subpackets += p.to_bytes()
                body += pack("!H", len(unhashed_subpackets)) + unhashed_subpackets

                body += pack("!H", self.hash_head)
                for mpi in self.data:
                    body += pack("!H", bitlength(mpi)) + mpi

                return body
        self.body = body

        def key_algorithm_name():
            return PublicKeyPacket.algorithms[self.key_algorithm]
        self.key_algorithm_name = key_algorithm_name

        def hash_algorithm_name():
            return self.hash_algorithms[self.hash_algorithm]
        self.hash_algorithm_name = hash_algorithm_name

        def issuer():
            for p in self.hashed_subpackets:
                if builtins.isinstance(p, cls.IssuerPacket):
                    return p.data
            for p in self.unhashed_subpackets:
                if builtins.isinstance(p, cls.IssuerPacket):
                    return p.data
            return None
        self.issuer = issuer
        return self
    cls.__call__ = __new__

    def get_subpackets(input_data):
        subpackets = []
        length = len(input_data)
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if length <= 0:
                break
            subpacket, bytes_used = cls.get_subpacket(input_data)
            if bytes_used > 0:
                subpackets.append(subpacket)
                input_data = input_data[bytes_used:]
                length -= bytes_used
            else:  # Parsing stuck?
                break
        return subpackets
    cls.get_subpackets = get_subpackets

    def get_subpacket(input_data):
        length = ord(input_data[0:1])
        length_of_length = 1
        # if($len < 192) One octet length, no furthur processing
        if length > 190 and length < 255:  # Two octet length
            length_of_length = 2
            length = ((length - 192) << 8) + ord(input_data[1:2]) + 192
        if length == 255:  # Five octet length
            length_of_length = 5
            length = unpack("!L", input_data[1:5])[0]
        input_data = input_data[length_of_length:]  # Chop off length header
        tag = ord(input_data[0:1])

        klass = cls.subpacket_types.get(tag, SignaturePacket.Subpacket)

        packet = klass()
        packet.tag = tag
        packet.input = PushbackGenerator(_gen_one(input_data[1:length]))
        packet.length = length - 1
        packet.read()
        packet.input = None
        packet.length = None

        input_data = input_data[length:]  # Chop off the data from this packet
        return (packet, length_of_length + length)
    cls.get_subpacket = get_subpacket


    def Subpacket(data=None):
        cls = _Packet()
        cls.__name__ = 'Subpacket'
        cls.__class__ = Subpacket
        cls.__mro__ = [cls, Packet]
        self = larky.mutablestruct(__name__='Subpacket', __class__=cls)

        def __init__(data):
            self = Packet(data)
            self.__name__  = cls.__name__
            self.__class__ = cls
            for tag in SignaturePacket.subpacket_types:
                _name = larky.impl_function_name(SignaturePacket.subpacket_types[tag])
                if _name == self.__name__:
                    self.tag = tag
                    break
            return self
        cls.__call__ = __init__
        self = cls(data)

        def header_and_body():
            body = self.body() or ""  # Get body first, we'll need its length
            size = pack("!B", 255) + pack(
                "!L", len(body) + 1
            )  # Use 5-octet lengths + 1 for tag as first packet body octet
            tag = pack("!B", self.tag)
            return {"header": size + tag, "body": body}
        self.header_and_body = header_and_body
        return self
    cls.Subpacket = Subpacket
    def SignatureCreationTimePacket(time=time()):
        """http://tools.ietf.org/html/rfc4880#section-5.2.3.4"""
        self = Subpacket()
        cls = self.__class__
        self.__class__.__mro__.insert(SignatureCreationTimePacket, 0)
        self.__name__ = 'SignatureCreationTimePacket'
        self.__class__ = SignatureCreationTimePacket

        def __init__(time):
            self.data = time
            return self
        self = __init__(time)

        def read():
            self.data = self.read_timestamp()
        self.read = read

        def body():
            return pack("!L", int(self.data))
        self.body = body
        return self
    cls.SignatureCreationTimePacket = SignatureCreationTimePacket
    def SignatureExpirationTimePacket(time=time()):
        self = Subpacket(time)
        self.__name__ = 'SignatureExpirationTimePacket'
        self.__class__ = SignatureExpirationTimePacket

        def read():
            self.data = self.read_timestamp()
        self.read = read

        def body():
            return pack("!L", self.data)
        self.body = body
        return self
    cls.SignatureExpirationTimePacket = SignatureExpirationTimePacket
    def ExportableCertificationPacket(time=time()):
        self = Subpacket(time)
        self.__name__ = 'ExportableCertificationPacket'
        self.__class__ = ExportableCertificationPacket
        def read():
            self.data = ord(self.read_byte()) != 0
        self.read = read

        def body():
            return pack("!B", self.data and 1 or 0)
        self.body = body
        return self
    cls.ExportableCertificationPacket = ExportableCertificationPacket
    def TrustSignaturePacket(time=time()):
        self = Subpacket(time)
        self.__name__ = 'TrustSignaturePacket'
        self.__class__ = TrustSignaturePacket
        def read():
            self.depth = ord(self.read_byte())
            self.trust = ord(self.read_byte())
        self.read = read

        def body():
            return pack("!B", self.depth) + pack("!B", self.trust)
        self.body = body
        return self
    cls.TrustSignaturePacket = TrustSignaturePacket
    def RegularExpressionPacket(time=time()):
        self = Subpacket(time)
        self.__name__ = 'RegularExpressionPacket'
        self.__class__ = RegularExpressionPacket
        def read():
            self.data = self.read_bytes(self.length - 1)
        self.read = read

        def body():
            return self.data + pack("!B", 0)
        self.body = body
        return self
    cls.RegularExpressionPacket = RegularExpressionPacket
    def RevocablePacket(time=time()):
        self = Subpacket(time)
        self.__name__ = 'RevocablePacket'
        self.__class__ = RevocablePacket
        def read():
            self.data = ord(self.read_byte()) != 0
        self.read = read

        def body():
            return pack("!B", self.data and 1 or 0)
        self.body = body
        return self
    cls.RevocablePacket = RevocablePacket
    def KeyExpirationTimePacket(time=time()):
        self = Subpacket(time)
        self.__name__ = 'KeyExpirationTimePacket'
        self.__class__ = KeyExpirationTimePacket
        def read():
            self.data = self.read_timestamp()
        self.read = read

        def body():
            return pack("!L", self.data)
        self.body = body
        return self
    cls.KeyExpirationTimePacket = KeyExpirationTimePacket
    def PreferredSymmetricAlgorithmsPacket(time=time()):
        self = Subpacket(time)
        self.__name__ = 'PreferredSymmetricAlgorithmsPacket'
        self.__class__ = PreferredSymmetricAlgorithmsPacket
        def read():
            self.data = []
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                if self.length <= 0:
                    break
                self.data += [self.read_byte()]
        self.read = read

        def body():
            body = b""
            for algo in self.data:
                body += pack("!B", algo)
            return body
        self.body = body
        return self
    cls.PreferredSymmetricAlgorithmsPacket = PreferredSymmetricAlgorithmsPacket
    def RevocationKeyPacket(time=time()):
        self = Subpacket(time)
        self.__name__ = 'RevocationKeyPacket'
        self.__class__ = RevocationKeyPacket
        def read():
            # bitfield must have bit 0x80 set, says the spec
            bitfield = ord(self.read_byte())
            self.sensitive = bitfield & 0x40 == 0x40
            self.key_algorithm = ord(self.read_byte())

            self.fingerprint = ""
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                if self.length <= 0:
                    break
                self.fingerprint += "%02X" % ord(self.read_byte())
        self.read = read

        def body():
            body = b""
            body += pack("!B", 0x80 | (self.sensitive and 0x40 or 0x00))
            body += pack("!B", self.key_algorithm)

            for i in range(0, len(self.data), 2):
                body += pack("!B", int(self.data[i] + self.data[i + 1], 16))

            return body
        self.body = body
        return self
    cls.RevocationKeyPacket = RevocationKeyPacket
    def IssuerPacket(keyid=None):
        """http://tools.ietf.org/html/rfc4880#section-5.2.3.5"""
        self = Subpacket()
        self.__name__ = 'IssuerPacket'
        self.__class__ = IssuerPacket

        def __init__(keyid):
            self.data = keyid
            return self
        self = __init__(keyid)

        def read():
            self.data = ""
            for i in range(0, 8):  # Store KeyID in Hex
                self.data += "%X" % ord(self.read_byte())
        self.read = read

        def body():
            b = b""
            for i in range(0, len(self.data), 2):
                b += pack("!B", int(self.data[i] + self.data[i + 1], 16))
            return b
        self.body = body
        return self
    cls.IssuerPacket = IssuerPacket
    # def NotationDataPacket(keyid=None):
    #     def read():
    #         flags = self.read_bytes(4)
    #         namelen = self.read_unpacked(2, "!H")
    #         datalen = self.read_unpacked(2, "!H")
    #         self.human_readable = ord(flags[0:1]) & 0x80 == 0x80
    #         self.name = self.read_bytes(namelen).decode("utf-8")
    #         self.data = self.read_bytes(datalen)
    #         if self.human_readable:
    #             self.data = self.data.decode("utf-8")
    #     self.read = read
    #
    #     def body():
    #         name_bytes = codecs.encode(self.name, encoding="utf-8")
    #         data_bytes = self.data
    #         if self.human_readable:
    #             data_bytes = codecs.encode(data_bytes, encoding="utf-8")
    #         return (
    #             pack("!B", self.human_readable and 0x80 or 0x00)
    #             + b"\0\0\0"
    #             + pack("!H", len(name_bytes))
    #             + pack("!H", len(data_bytes))
    #             + name_bytes
    #             + data_bytes
    #         )
    #     self.body = body
    #     return self
    # def PreferredHashAlgorithmsPacket(keyid=None):
    #     def read():
    #         self.data = []
    #         for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
    #             if self.length <= 0:
    #                 break
    #             self.data += [self.read_byte()]
    #     self.read = read
    #
    #     def body():
    #         body = b""
    #         for algo in self.data:
    #             body += pack("!B", algo)
    #         return body
    #     self.body = body
    #     return self
    # def PreferredCompressionAlgorithmsPacket(keyid=None):
    #     def read():
    #         self.data = []
    #         for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
    #             if self.length <= 0:
    #                 break
    #             self.data += [self.read_byte()]
    #     self.read = read
    #
    #     def body():
    #         body = b""
    #         for algo in self.data:
    #             body += pack("!B", algo)
    #         return body
    #     self.body = body
    #     return self
    # def KeyServerPreferencesPacket(keyid=None):
    #     def read():
    #         flags = ord(self.read_byte())
    #         self.no_modify = flags & 0x80 == 0x80
    #     self.read = read
    #
    #     def body():
    #         return pack("!B", self.no_modify and 0x80 or 0x00)
    #     self.body = body
    #     return self
    # def PreferredKeyServerPacket(keyid=None):
    #     def read():
    #         self.data = self.read_bytes(self.length)
    #     self.read = read
    #
    #     def body():
    #         return self.data
    #     self.body = body
    #     return self
    # def PrimaryUserIDPacket(keyid=None):
    #     def read():
    #         self.data = ord(self.read_byte()) != 0
    #     self.read = read
    #
    #     def body():
    #         return pack("!B", self.data and 1 or 0)
    #     self.body = body
    #     return self
    # def PolicyURIPacket(keyid=None):
    #     def read():
    #         self.data = self.read_bytes(self.length)
    #     self.read = read
    #
    #     def body():
    #         return self.data
    #     self.body = body
    #     return self
    # def KeyFlagsPacket(flags=[]):
    #     self = larky.mutablestruct(__name__='KeyFlagsPacket', __class__=KeyFlagsPacket)
    #     def __init__(flags):
    #         super(SignaturePacket.KeyFlagsPacket, self).__init__()
    #         self.flags = flags
    #         return self
    #     self = __init__(flags)
    #
    #     def read():
    #         self.flags = []
    #         for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
    #             if self.length <= 0:
    #                 break
    #             self.flags.append(ord(self.read_byte()))
    #     self.read = read
    #
    #     def body():
    #         b = b""
    #         for f in self.flags:
    #             b += pack("!B", f)
    #         return b
    #     self.body = body
    #     return self
    # def SignersUserIDPacket(flags=[]):
    #     def read():
    #         self.data = self.read_bytes(self.length)
    #     self.read = read
    #
    #     def body():
    #         return self.data
    #     self.body = body
    #     return self
    # def ReasonforRevocationPacket(flags=[]):
    #     def read():
    #         self.code = ord(self.read_byte())
    #         self.data = self.read_bytes(self.length)
    #     self.read = read
    #
    #     def body():
    #         return pack("!B", self.code) + self.data
    #     self.body = body
    #     return self
    # def FeaturesPacket(flags=[]):
    #     pass  # All implemented in parent
    #     return self
    # def SignatureTargetPacket(flags=[]):
    #     def read():
    #         self.key_algorithm = ord(self.read_byte())
    #         self.hash_algorithm = ord(self.read_byte())
    #         self.data = self.read_bytes(self.length)
    #     self.read = read
    #
    #     def body():
    #         return (
    #             pack("!B", self.key_algorithm)
    #             + pack("!B", self.hash_algorithm)
    #             + self.data
    #         )
    #     self.body = body
    #     return self

    SUBPACKET_TYPES.update({
        2: SignatureCreationTimePacket,
        3: SignatureExpirationTimePacket,
        4: ExportableCertificationPacket,
        5: TrustSignaturePacket,
        6: RegularExpressionPacket,
        7: RevocablePacket,
        9: KeyExpirationTimePacket,
        11: PreferredSymmetricAlgorithmsPacket,
        12: RevocationKeyPacket,
        16: IssuerPacket,
        # 20: NotationDataPacket,
        # 21: PreferredHashAlgorithmsPacket,
        # 22: PreferredCompressionAlgorithmsPacket,
        # 23: KeyServerPreferencesPacket,
        # 24: PreferredKeyServerPacket,
        # 25: PrimaryUserIDPacket,
        # 26: PolicyURIPacket,
        # 27: KeyFlagsPacket,
        # 28: SignersUserIDPacket,
        # 29: ReasonforRevocationPacket,
        # 30: FeaturesPacket,
        # 31: SignatureTargetPacket,
    })
    cls.subpacket_types = SUBPACKET_TYPES
    return cls


SignaturePacket = _SignaturePacket()

def EmbeddedSignaturePacket(data=None, key_algorithm=None, hash_algorithm=None):
    self = SignaturePacket(data=data, key_algorithm=key_algorithm, hash_algorithm=hash_algorithm)
    self.__class__ = EmbeddedSignaturePacket
    self.__name__ = 'EmbeddedSignaturePacket'

    for tag in SignaturePacket.subpacket_types:
        if larky.impl_function_name(SignaturePacket.subpacket_types[tag]) == self.__name__:
            self.tag = tag
            break

    def header_and_body():
        body = self.body() or ""  # Get body first, we'll need its length
        size = pack("!B", 255) + pack(
            "!L", len(body) + 1
        )  # Use 5-octet lengths + 1 for tag as first packet body octet
        tag = pack("!B", self.tag)
        return {"header": size + tag, "body": body}
    self.header_and_body = header_and_body
    return self

SUBPACKET_TYPES[32] = EmbeddedSignaturePacket

SignaturePacket.EmbeddedSignaturePacket = SUBPACKET_TYPES[32]

def SymmetricSessionKeyPacket(s2k=None, encrypted_data=b"", symmetric_algorithm=9, version=3):
    """OpenPGP Symmetric-Key Encrypted Session Key packet (tag 3).
    http://tools.ietf.org/html/rfc4880#section-5.3
    """
    cls = _Packet()
    cls.__name__ = 'SymmetricSessionKeyPacket'
    cls.__class__ = SymmetricSessionKeyPacket

    def __init__(s2k, encrypted_data, symmetric_algorithm, version):
        self = Packet()
        self.__name__  = cls.__name__
        self.__class__ = cls
        self.version = version
        self.symmetric_algorithm = symmetric_algorithm
        self.s2k = s2k
        self.encrypted_data = encrypted_data
        return self
    cls.__call__ = __init__
    self = cls(s2k, encrypted_data, symmetric_algorithm, version)

    def read():
        self.version = ord(self.read_byte())
        self.symmetric_algorithm = ord(self.read_byte())
        self.s2k, s2k_bytes = S2K.parse(self.input)
        self.length -= s2k_bytes
        self.encrypted_data = self.read_bytes(self.length)
    self.read = read

    def body():
        return (
            pack("!B", self.version)
            + pack("!B", self.symmetric_algorithm)
            + self.s2k.to_bytes()
            + self.encrypted_data
        )
    self.body = body
    return self

def OnePassSignaturePacket(data=None):
    """OpenPGP One-Pass Signature packet (tag 4).
    http://tools.ietf.org/html/rfc4880#section-5.4
    """
    cls = _Packet()
    cls.__name__ = 'OnePassSignaturePacket'
    cls.__class__ = OnePassSignaturePacket

    def __init__(data):
        self = Packet(data)
        self.__name__  = cls.__name__
        self.__class__ = cls
        return self
    cls.__call__ = __init__
    self = cls(data)

    def read():
        self.version = ord(self.read_byte())
        self.signature_type = ord(self.read_byte())
        self.hash_algorithm = ord(self.read_byte())
        self.key_algorithm = ord(self.read_byte())
        self.key_id = ""
        for i in range(0, 8):  # Store KeyID in Hex
            self.key_id += "%02X" % ord(self.read_byte())
        self.nested = ord(self.read_byte())
    self.read = read

    def body():
        body = (
            pack("!B", self.version)
            + pack("!B", self.signature_type)
            + pack("!B", self.hash_algorithm)
            + pack("!B", self.key_algorithm)
        )
        for i in range(0, len(self.key_id), 2):
            body += pack("!B", int(self.key_id[i] + self.key_id[i + 1], 16))
        body += pack("!B", int(self.nested))
        return body
    self.body = body
    return self

def _PublicKeyPacket():
    """OpenPGP Public-Key packet (tag 6).
    http://tools.ietf.org/html/rfc4880#section-5.5.1.1
    http://tools.ietf.org/html/rfc4880#section-5.5.2
    http://tools.ietf.org/html/rfc4880#section-11.1
    http://tools.ietf.org/html/rfc4880#section-12
    """
    cls = _Packet()
    cls.__name__ = 'PublicKeyPacket'
    cls.__class__ = _PublicKeyPacket

    cls.key_fields = {
        1: ["n", "e"],  # RSA
        16: ["p", "g", "y"],  # ELG-E
        17: ["p", "q", "g", "y"],  # DSA
    }

    cls.algorithms = {
        1: "RSA",
        2: "RSA",
        3: "RSA",
        16: "ELGAMAL",
        17: "DSA",
        18: "ECC",
        19: "ECDSA",
        21: "DH",
    }

    def __new__(keydata=None, version=4, algorithm=1, timestamp=time()):
        self = Packet()
        self.__name__  = cls.__name__
        self.__class__ = cls

        def __init__(keydata, version, algorithm, timestamp):
            self._fingerprint = None
            self.version = version
            self.key_algorithm = algorithm
            self.timestamp = int(timestamp)
            if builtins.isinstance(keydata, tuple) or builtins.isinstance(keydata, list):
                self.key = {}
                for i in range(
                    0, min(len(keydata), len(cls.key_fields[self.key_algorithm]))
                ):
                    self.key[cls.key_fields[self.key_algorithm][i]] = keydata[i]
            else:
                self.key = keydata
            return self
        self = __init__(keydata, version, algorithm, timestamp)

        def self_signatures(message):
            """Find self signatures in a message, these often contain metadata about the key"""
            sigs = []
            keyid16 = self.fingerprint()[-16:].upper()
            for p in message:
                if builtins.isinstance(p, SignaturePacket):
                    if p.issuer() == keyid16:
                        sigs.append(p)
                    else:
                        packets = p.hashed_subpackets + p.unhashed_subpackets
                        for s in packets:
                            if (
                                builtins.isinstance(s, SignaturePacket.EmbeddedSignaturePacket)
                                and s.issuer().upper() == keyid16
                            ):
                                sigs.append(p)
                                break
                elif len(sigs) > 0:
                    break  # After we've seen a self sig, the next non-sig stop all self-sigs
            return sigs
        self.self_signatures = self_signatures

        def expires(message):
            """Find expiry time of this key based on the self signatures in a message"""
            for p in self.self_signatures(message):
                packets = p.hashed_subpackets + p.unhashed_subpackets
                for s in packets:
                    if builtins.isinstance(s, SignaturePacket.KeyExpirationTimePacket):
                        return self.timestamp + s.data
            return None  # Never expires
        self.expires = expires

        def key_algorithm_name():
            return self.__class__.algorithms[self.key_algorithm]
        self.key_algorithm_name = key_algorithm_name

        def read():
            """http://tools.ietf.org/html/rfc4880#section-5.5.2"""
            self.version = ord(self.read_byte())
            if self.version == 3:
                self.timestamp = self.read_timestamp()
                self.v3_days_of_validity = self.read_unpacked(2, "!H")
                self.key_algorithm = ord(self.read_byte())
                self.read_key_material()
            elif self.version == 4:
                self.timestamp = self.read_timestamp()
                self.key_algorithm = ord(self.read_byte())
                self.read_key_material()
        self.read = read

        def read_key_material():
            self.key = {}
            for field in cls.key_fields[self.key_algorithm]:
                self.key[field] = self.read_mpi()
            self.key_id = self.fingerprint()[-8:]
        self.read_key_material = read_key_material

        def fingerprint_material():
            if self.version == 2 or self.version == 3:
                material = []
                for i in cls.key_fields[self.key_algorithm]:
                    material += [pack("!H", bitlength(self.key[i]))]
                    material += [self.key[i]]
                return material
            elif self.version == 4:
                head = [
                    pack("!B", 0x99),
                    None,
                    pack("!B", self.version),
                    pack("!L", self.timestamp),
                    pack("!B", self.key_algorithm),
                ]
                material = b""
                for i in cls.key_fields[self.key_algorithm]:
                    material += pack("!H", bitlength(self.key[i]))
                    material += self.key[i]
                head[1] = pack("!H", 6 + len(material))
                print([h.hex() for h in head])
                print(material.hex())
                return head + [material]
        self.fingerprint_material = fingerprint_material

        def fingerprint():
            """http://tools.ietf.org/html/rfc4880#section-12.2
            http://tools.ietf.org/html/rfc4880#section-3.3
            """
            if self._fingerprint:
                return self._fingerprint
            if self.version == 2 or self.version == 3:
                self._fingerprint = (
                    hashlib.md5(b"".join(self.fingerprint_material())).hexdigest().upper()
                )
            elif self.version == 4:
                self._fingerprint = (
                    hashlib.sha1(b"".join(self.fingerprint_material())).hexdigest().upper()
                )
            return self._fingerprint
        self.fingerprint = fingerprint

        def body():
            if self.version == 3:
                return b"".join(
                    [
                        pack("!B", self.version),
                        pack("!L", self.timestamp),
                        pack("!H", self.v3_days_of_validity),
                        pack("!B", self.key_algorithm),
                    ]
                    + self.fingerprint_material()
                )
            elif self.version == 4:
                return b"".join(self.fingerprint_material()[2:])
        self.body = body
        return self
    cls.__call__ = __new__
    return cls


PublicKeyPacket = _PublicKeyPacket()


def PublicSubkeyPacket(keydata=None, version=4, algorithm=1, timestamp=time()):
    """OpenPGP Public-Subkey packet (tag 14).
    http://tools.ietf.org/html/rfc4880#section-5.5.1.2
    http://tools.ietf.org/html/rfc4880#section-5.5.2
    http://tools.ietf.org/html/rfc4880#section-11.1
    http://tools.ietf.org/html/rfc4880#section-12
    """
    self = PublicKeyPacket(keydata, version, algorithm, timestamp)
    self.__name__ = 'PublicSubkeyPacket'
    self.__class__.__name__ = 'PublicSubkeyPacket'
    self.__class__.__class__ = PublicSubkeyPacket
    return self


def SecretKeyPacket(keydata=None, version=4, algorithm=1, timestamp=time()):
    """OpenPGP Secret-Key packet (tag 5).
    http://tools.ietf.org/html/rfc4880#section-5.5.1.3
    http://tools.ietf.org/html/rfc4880#section-5.5.3
    http://tools.ietf.org/html/rfc4880#section-11.2
    http://tools.ietf.org/html/rfc4880#section-12
    """

    cls = _PublicKeyPacket()
    cls.__name__ = 'SecretKeyPacket'
    cls.__class__ = SecretKeyPacket

    cls.secret_key_fields = {
        1: ["d", "p", "q", "u"],  # RSA
        16: ["x"],  # ELG-E
        17: ["x"],  # DSA
    }

    #
    # self = PublicKeyPacket(keydata, version, algorithm, timestamp)
    # cls = self.__class__
    #
    # self.__name__ = 'SecretKeyPacket'
    # self.__class__ = cls

    super__init__ = cls.__call__
    def __init__(keydata, version, algorithm, timestamp):
        self = super__init__(keydata, version, algorithm, timestamp)
        self.__class__ = cls
        self.__name__ =  'SecretKeyPacket'
        self.s2k_useage = 0
        if builtins.isinstance(keydata, tuple) or builtins.isinstance(keydata, list):
            public_len = len(cls.key_fields[self.key_algorithm])
            for i in range(public_len, len(keydata)):
                self.key[
                    cls.secret_key_fields[self.key_algorithm][i - public_len]
                ] = keydata[i]
        return self
    cls.__call__ = __init__
    self = __init__(keydata, version, algorithm, timestamp)

    super_read = self.read
    def read():
        super_read()  # All the fields from PublicKey
        self.s2k_useage = ord(self.read_byte())
        if self.s2k_useage == 255 or self.s2k_useage == 254:
            self.symmetric_algorithm = ord(self.read_byte())
            self.s2k, s2k_bytes = S2K.parse(self.input)
            self.length -= s2k_bytes
        elif self.s2k_useage > 0:
            self.symmetric_algorithm = self.s2k_useage
        if self.s2k_useage > 0:
            # Rest of input is MPIs and checksum (encrypted)
            self.encrypted_data = self.read_bytes(self.length)
        else:
            material = self.read_bytes(self.length - 2)
            self.input.push(material)
            self.key_from_input()
            chk = self.read_unpacked(2, "!H")
            if chk != checksum(material):
                fail("OpenPGPException: Checksum verification failed when parsing SecretKeyPacket"
                )
    self.read = read

    def key_from_input():
        for field in cls.secret_key_fields[self.key_algorithm]:
            self.key[field] = self.read_mpi()
    self.key_from_input = key_from_input

    super_body = self.body
    def body():
        b = super_body() + pack("!B", self.s2k_useage)
        secret_material = b""
        if self.s2k_useage == 255 or self.s2k_useage == 254:
            b += pack("!B", self.symmetric_algorithm)
            b += self.s2k.to_bytes()
        if self.s2k_useage > 0:
            b += self.encrypted_data
        else:
            for f in cls.secret_key_fields[self.key_algorithm]:
                f = self.key[f]
                secret_material += pack("!H", bitlength(f))
                secret_material += f
            b += secret_material

            # 2-octet checksum
            chk = 0
            for i in range(0, len(secret_material)):
                chk = (chk + ord(secret_material[i : i + 1])) % 65536
            b += pack("!H", chk)

        return b
    self.body = body

    return self


def SecretSubkeyPacket(keydata=None, version=4, algorithm=1, timestamp=time()):
    """OpenPGP Secret-Subkey packet (tag 7).
    http://tools.ietf.org/html/rfc4880#section-5.5.1.4
    http://tools.ietf.org/html/rfc4880#section-5.5.3
    http://tools.ietf.org/html/rfc4880#section-11.2
    http://tools.ietf.org/html/rfc4880#section-12
    """
    self = SecretKeyPacket(keydata, version, algorithm, timestamp)
    cls = self.__class__
    cls.__name__ = 'SecretSubkeyPacket'
    cls.__class__ = SecretSubkeyPacket
    self.__name__ = 'SecretSubkeyPacket'
    self.__class__ = cls
    return self

# def CompressedDataPacket(keydata=None, version=4, algorithm=1, timestamp=time()):
#     """OpenPGP Compressed Data packet (tag 8).
#     http://tools.ietf.org/html/rfc4880#section-5.6
#     """
#
#     # http://tools.ietf.org/html/rfc4880#section-9.3
#     algorithms = {0: "Uncompressed", 1: "ZIP", 2: "ZLIB", 3: "BZip2"}
#
#     def read():
#         self.algorithm = ord(self.read_byte())
#         self.data = self.read_bytes(self.length)
#         if self.algorithm == 0:
#             self.data = Message.parse(self.data)
#         elif self.algorithm == 1:
#             self.data = Message.parse(zlib.decompress(self.data, -15))
#         elif self.algorithm == 2:
#             self.data = Message.parse(zlib.decompress(self.data))
#         elif self.algorithm == 3:
#             self.data = Message.parse(bz2.decompress(self.data))
#         else:
#             pass  # TODO: error?
#     self.read = read
#
#     def body():
#         body = pack("!B", self.algorithm)
#         if self.algorithm == 0:
#             self.data = self.data.to_bytes()
#         elif self.algorithm == 1:
#             compressor = zlib.compressobj(
#                 zlib.Z_DEFAULT_COMPRESSION, zlib.DEFLATED, -15
#             )
#             body += compressor.compress(self.data.to_bytes())
#             body += compressor.flush()
#         elif self.algorithm == 2:
#             body += zlib.compress(self.data.to_bytes())
#         elif self.algorithm == 3:
#             body += bz2.compress(self.data.to_bytes())
#         else:
#             pass  # TODO: error?
#         return body
#     self.body = body
#
#     def __iter__():
#         return iter(self.data)
#     self.__iter__ = __iter__
#
#     def __getitem__(item):
#         return self.data[item]
#     self.__getitem__ = __getitem__
#     return self
def EncryptedDataPacket():
    """OpenPGP Symmetrically Encrypted Data packet (tag 9).
    http://tools.ietf.org/html/rfc4880#section-5.7
    """
    cls = _Packet()
    cls.__name__ = 'EncryptedDataPacket'

    def __init__():
        self = cls()
        self.__name__  = cls.__name__
        self.__class__ = cls
        return self
    self = __init__()
    return self

def MarkerPacket():
    """OpenPGP Marker packet (tag 10).
    http://tools.ietf.org/html/rfc4880#section-5.8
    """
    cls = _Packet()
    cls.__name__ = 'MarkerPacket'

    def __init__():
        self = cls()
        self.__name__  = cls.__name__
        self.__class__ = cls
        return self
    self = __init__()
    return self


def LiteralDataPacket(data=None, format="b", filename="data", timestamp=time()):
    """OpenPGP Literal Data packet (tag 11).
    http://tools.ietf.org/html/rfc4880#section-5.9
    """
    cls = _Packet()
    cls.__name__ = 'LiteralDataPacket'

    def __init__(data, format, filename, timestamp):
        self = cls(data)
        self.__name__  = cls.__name__
        self.__class__ = cls
        if types.is_string(data):
            data = codecs.encode(data, encoding="utf-8")
        self.data = data
        self.format = format
        self.filename = codecs.encode(filename, encoding="utf-8")
        self.timestamp = timestamp
        return self
    # cls.__call__ = __init__
    self = __init__(data, format, filename, timestamp)

    def normalize():
        if self.format == "u" or self.format == "t":  # Normalize line endings
            self.data = (
                self.data.replace(b"\r\n", b"\n")
                .replace(b"\r", b"\n")
                .replace(b"\n", b"\r\n")
            )
    self.normalize = normalize

    def read():
        self.size = self.length - 1 - 1 - 4
        self.format = self.read_byte().decode("ascii")
        filename_length = ord(self.read_byte())
        self.size -= filename_length
        self.filename = self.read_bytes(filename_length)
        self.timestamp = self.read_timestamp()
        self.data = self.read_bytes(self.size)
    self.read = read

    def body():
        return (
            codecs.encode(self.format, encoding="ascii")
            + pack("!B", len(self.filename))
            + self.filename
            + pack("!L", int(self.timestamp))
            + bytes(self.data, encoding="utf-8")
        )
    self.body = body
    return self


def TrustPacket():
    """OpenPGP Trust packet (tag 12).
    http://tools.ietf.org/html/rfc4880#section-5.10
    """
    cls = _Packet()
    cls.__name__ = 'TrustPacket'

    def __init__():
        self = cls()
        self.__name__  = cls.__name__
        self.__class__ = cls
        return self
    self = __init__()
    return self
def UserIDPacket(name="", comment=None, email=None):
    """OpenPGP User ID packet (tag 13).
    http://tools.ietf.org/html/rfc4880#section-5.11
    http://tools.ietf.org/html/rfc2822
    """
    cls = _Packet()
    cls.__name__ = 'UserIDPacket'

    def __init__(name, comment, email):
        self = cls()
        self.__name__  = cls.__name__
        self.__class__ = cls
        self.name = None
        self.comment = self.name
        self.email = self.name
        self.text = ""
        if (not comment) and (not email):
            name = codecs.encode(name, encoding="utf-8")
            self.input = PushbackGenerator(_gen_one(name))
            self.length = len(name)
            self.read()
        else:
            self.name = name
            self.comment = comment
            self.email = email
        return self
    # cls.__call__ = __init__
    self = __init__(name, comment, email)

    def read():
        self.text = self.read_bytes(self.length).decode("utf-8")
        # User IDs of the form: "name (comment) <email>"
        parts = re.findall(r"^([^\(]+)\(([^\)]+)\)\s+<([^>]+)>$", self.text)
        if len(parts) > 0:
            self.name = parts[0][0].strip()
            self.comment = parts[0][1].strip()
            self.email = parts[0][2].strip()
        else:  # User IDs of the form: "name <email>"
            parts = re.findall(r"^([^<]+)\s+<([^>]+)>$", self.text)
            if len(parts) > 0:
                self.name = parts[0][0].strip()
                self.email = parts[0][1].strip()
            else:  # User IDs of the form: "name"
                parts = re.findall(r"^([^<]+)$", self.text)
                if len(parts) > 0:
                    self.name = parts[0].strip()
                else:  # User IDs of the form: "<email>"
                    parts = re.findall(r"^<([^>]+)>$", self.text)
                    if len(parts) > 0:
                        self.email = parts[0].strip()
    self.read = read

    def __str__():
        text = []
        if self.name:
            text.append(self.name)
        if self.comment:
            text.append("(" + self.comment + ")")
        if self.email:
            text.append("<" + self.email + ">")
        if len(text) < 1:
            text = [self.text]
        return " ".join(text)
    self.__str__ = __str__

    def body():
        return codecs.encode(self.__str__(), encoding="utf-8")

    self.body = body
    return self

def UserAttributePacket():
    """OpenPGP User Attribute packet (tag 17).
    http://tools.ietf.org/html/rfc4880#section-5.12
    http://tools.ietf.org/html/rfc4880#section-11.1
    """
    cls = _Packet()
    cls.__name__ = 'UserAttributePacket'

    def __init__():
        self = cls()
        self.__name__  = cls.__name__
        self.__class__ = cls
        return self
    self = __init__()
    return self

def IntegrityProtectedDataPacket(data=b"", version=1):
    """OpenPGP Sym. Encrypted Integrity Protected Data packet (tag 18).
    http://tools.ietf.org/html/rfc4880#section-5.13
    """
    self = EncryptedDataPacket()
    self.__name__ = 'IntegrityProtectedDataPacket'
    self.__class__ = IntegrityProtectedDataPacket
    self.version = version
    self.data = data

    def read():
        self.version = ord(self.read_byte())
        self.data = self.read_bytes(self.length)
    self.read = read

    def body():
        return pack("!B", self.version) + self.data
    self.body = body
    return self

def ModificationDetectionCodePacket(sha1=""):
    """OpenPGP Modification Detection Code packet (tag 19).
    http://tools.ietf.org/html/rfc4880#section-5.14
    """
    cls = _Packet()
    cls.__name__ = 'ModificationDetectionCodePacket'

    def __init__(sha1):
        self = cls(sha1)
        self.__name__  = cls.__name__
        self.__class__ = cls
        self.data = sha1
        return self
    self = __init__(sha1)

    def read():
        self.data = self.read_bytes(self.length)
        if len(self.data) != 20:
            fail("Exception: Bad ModificationDetectionCodePacket")
    self.read = read

    def header_and_body():
        body = self.body()  # Get body first, we will need it's length
        if len(body) != 20:
            fail("Exception: Bad ModificationDetectionCodePacket")
        return {"header": b"\xD3\x14", "body": body}
    self.header_and_body = header_and_body

    def body():
        return self.data
    self.body = body
    return self


def ExperimentalPacket():
    """OpenPGP Private or Experimental packet (tags 60..63).
    http://tools.ietf.org/html/rfc4880#section-4.3
    """
    cls = _Packet()
    cls.__name__ = 'ExperimentalPacket'

    def __init__():
        self = cls()
        self.__name__  = cls.__name__
        self.__class__ = cls
        return self
    self = __init__()
    return self


Packet.tags = {
    1: AsymmetricSessionKeyPacket,  # Public-Key Encrypted Session Key
    2: SignaturePacket,  # Signature Packet
    3: SymmetricSessionKeyPacket,  # Symmetric-Key Encrypted Session Key Packet
    4: OnePassSignaturePacket,  # One-Pass Signature Packet
    5: SecretKeyPacket,  # Secret-Key Packet
    6: PublicKeyPacket,  # Public-Key Packet
    7: SecretSubkeyPacket,  # Secret-Subkey Packet
    # 8: CompressedDataPacket,  # Compressed Data Packet
    9: EncryptedDataPacket,  # Symmetrically Encrypted Data Packet
    10: MarkerPacket,  # Marker Packet
    11: LiteralDataPacket,  # Literal Data Packet
    12: TrustPacket,  # Trust Packet
    13: UserIDPacket,  # User ID Packet
    14: PublicSubkeyPacket,  # Public-Subkey Packet
    17: UserAttributePacket,  # User Attribute Packet
    18: IntegrityProtectedDataPacket,  # Sym. Encrypted and Integrity Protected Data Packet
    19: ModificationDetectionCodePacket,  # Modification Detection Code Packet
    60: ExperimentalPacket,  # Private or Experimental Values
    61: ExperimentalPacket,  # Private or Experimental Values
    62: ExperimentalPacket,  # Private or Experimental Values
    63: ExperimentalPacket,  # Private or Experimental Values
}


OpenPGP = larky.struct(
    AsymmetricSessionKeyPacket=AsymmetricSessionKeyPacket,
    EmbeddedSignaturePacket=EmbeddedSignaturePacket,
    EncryptedDataPacket=EncryptedDataPacket,
    ExperimentalPacket=ExperimentalPacket,
    IntegrityProtectedDataPacket=IntegrityProtectedDataPacket,
    LiteralDataPacket=LiteralDataPacket,
    MarkerPacket=MarkerPacket,
    Message=Message,
    ModificationDetectionCodePacket=ModificationDetectionCodePacket,
    OnePassSignaturePacket=OnePassSignaturePacket,
    Packet=Packet,
    PublicKeyPacket=PublicKeyPacket,
    PublicSubkeyPacket=PublicSubkeyPacket,
    S2K=S2K,
    SecretKeyPacket=SecretKeyPacket,
    SecretSubkeyPacket=SecretSubkeyPacket,
    SignaturePacket=SignaturePacket,
    SymmetricSessionKeyPacket=SymmetricSessionKeyPacket,
    TrustPacket=TrustPacket,
    UserAttributePacket=UserAttributePacket,
    UserIDPacket=UserIDPacket,
    bitlength=bitlength,
    checksum=checksum,
    crc24=crc24,
    enarmor=enarmor,
    unarmor=unarmor,
)