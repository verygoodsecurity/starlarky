load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//struct", struct="struct")
load("@vendor//Crypto/Util/py3compat", tobytes="tobytes", bord="bord", tostr="tostr")
load("@vendor//option/result", Error="Error")

StopIterating = Error("StopIteration")

def _ensure_bytes(n, chunk, g):
    for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
        if len(chunk) >= n:
            break
        next_chunk = g.next()
        if next_chunk == StopIterating:
            return fail("OpenPGPException: Not enough bytes")
        # chunk += next(g)
        chunk += next_chunk
    return chunk

def _gen_one(seq):
    self = larky.mutablestruct(__class__='generator')
    def __init__(seq):
        self._sequence = seq
        self._length = len(seq)
        self._current = -1
        return self
    self = __init__(seq)

    def next():
        if self._current >= self._length-1:
            return StopIterating
        self._current += 1
        return self._sequence[self._current]
    self.next = next
    return self

def _slurp(g):
    # bs = b''
    s = ''
    for chunk in g:
        s += tostr(chunk)
    return tobytes(s)

def PushbackGenerator(g):
    self = larky.mutablestruct(__name__='PushbackGenerator', __class__=PushbackGenerator)
    def __init__(g):
        # self._g = g
        self._g = g
        self._pushback = []
        return self
    self = __init__(g)

    def __iter__():
        return self
    self.__iter__ = __iter__

    def next():
        return self.__next__()
    self.next = next

    def __next__():
        if len(self._pushback):
            return self._pushback.pop(0)
        # return next(self._g)
        return self._g.next()
    self.__next__ = __next__

    def hasNext():
        if len(self._pushback) > 0:
            return True
        # try:
        #     chunk = next(self)
        #     self.push(chunk)
        #     return True
        # except StopIteration:
        #     return False
        chunk = self.next()
        if chunk ==  StopIterating:
           return False
        self.push(chunk)
        return True

    self.hasNext = hasNext

    def push(i):
        if hasattr(self._g, 'push'):
            self._g.push(i)
        else:
            self._pushback.insert(0, i)
    self.push = push
    return self

    
def Packet():
    """ OpenPGP packet.
        http://tools.ietf.org/html/rfc4880#section-4.1
        http://tools.ietf.org/html/rfc4880#section-4.3
    """

    self = larky.mutablestruct(__name__='Packet', __class__=Packet)

    def __init__(data=None):
        for tag in Packet.tags:
            if Packet.tags[tag] == self.__class__:
                self.tag = tag
                break
        self.data = data
    self = __init__(data)

    def parse(input_data):
        if hasattr(input_data, 'next') or hasattr(input_data, '__next__'):
            g = PushbackGenerator(input_data)
        else:
            g = PushbackGenerator(_gen_one(input_data))

        packet = None
        # If there is not even one byte, then there is no packet at all
        # chunk = _ensure_bytes(1, next(g), g)
        chunk = _ensure_bytes(1, g.next(), g)
        # try:
        # Parse header
        if ord(chunk[0:1]) & 64:
            tag, data_length = self.parse_new_format(chunk, g)
        else:
            tag, data_length = self.parse_old_format(chunk, g)

        if not data_length:
            chunk = _slurp(g)
            data_length = len(chunk)
            g.push(chunk)

        if tag:
            # try:
            #     packet_class = self.tags[tag]
            # except KeyError:
            #     packet_class = Packet
            if tag in self.tags:
                packet_class = self.tags[tag]
            else:
                packet_class = Packet

            packet = packet_class()
            packet.tag = tag
            packet.input = g
            packet.length = data_length
            packet.read()
            packet.read_bytes(packet.length) # Remove excess bytes
            packet.input = None
            packet.length = None
        # except StopIteration:
        #     return Error("OpenPGPException: Not enough bytes")

        return packet
    self.parse = parse

    def parse_new_format(chunk, g):
        """ Parses a new-format (RFC 4880) OpenPGP packet.
            http://tools.ietf.org/html/rfc4880#section-4.2.2
        """

        chunk = _ensure_bytes(2, chunk, g)
        tag = ord(chunk[0:1]) & 63
        length = ord(chunk[1:2])

        if length < 192: # One octet length
            if len(chunk) > 2:
                g.push(chunk[2:])
            return (tag, length)
        if length > 191 and length < 224: # Two octet length
            chunk = _ensure_bytes(3, chunk, g)
            if len(chunk) > 2:
                g.push(chunk[3:])
            return (tag, ((length - 192) << 8) + ord(chunk[2:3]) + 192)
        if length == 255: # Five octet length
            chunk = _ensure_bytes(6, chunk, g)
            if len(chunk) > 6:
                g.push(chunk[6:])
            return (tag, struct.unpack('!L', chunk[2:6])[0])
        # TODO: Partial body lengths. 1 << ($len & 0x1F)
    self.parse_new_format = parse_new_format

    def parse_old_format(chunk, g):
        """ Parses an old-format (PGP 2.6.x) OpenPGP packet.
            http://tools.ietf.org/html/rfc4880#section-4.2.1
        """
        chunk = _ensure_bytes(1, chunk, g)
        tag = ord(chunk[0:1])
        length = tag & 3
        tag = (tag >> 2) & 15
        if length == 0: # The packet has a one-octet length. The header is 2 octets long.
            head_length = 2
            chunk = _ensure_bytes(head_length, chunk, g)
            data_length = ord(chunk[1:2])
        elif length == 1: # The packet has a two-octet length. The header is 3 octets long.
            head_length = 3
            chunk = _ensure_bytes(head_length, chunk, g)
            data_length = struct.unpack('!H', chunk[1:3])[0]
        elif length == 2: # The packet has a four-octet length. The header is 5 octets long.
            head_length = 5
            chunk = _ensure_bytes(head_length, chunk, g)
            data_length = struct.unpack('!L', chunk[1:5])[0]
        elif length == 3: # The packet is of indeterminate length. The header is 1 octet long.
            head_length = 1
            chunk = _ensure_bytes(head_length, chunk, g)
            data_length = None

        if len(chunk) > head_length:
             g.push(chunk[head_length:])
        return (tag, data_length)
    self.parse_old_format = parse_old_format

    def read():
        # Will normally be overridden by subclasses
        self.data = self.read_bytes(self.length)
    self.read = read

    def read_bytes(count):
        chunk = _ensure_bytes(count, b'', self.input)
        if len(chunk) > count:
            self.input.push(chunk[count:])
        self.length -= count
        return chunk[:count]

    return self

def LiteralDataPacket(Packet):
    """ OpenPGP Literal Data packet (tag 11).
        http://tools.ietf.org/html/rfc4880#section-5.9
    """
    self = larky.mutablestruct(__name__='LiteralDataPacket', __class__=LiteralDataPacket)

    def __init__(data=None, format='b', filename='data', timestamp=time()):
        super(LiteralDataPacket, self).__init__()
        if hasattr(data, 'encode'):
            data = data.encode('utf-8')
        self.data = data
        self.format = format
        self.filename = filename.encode('utf-8')
        self.timestamp = timestamp
    self = __init__(data, format, filename, timestamp)

    def normalize():
        if self.format == 'u' or self.format == 't': # Normalize line endings
            self.data = self.data.replace(b"\r\n", b"\n").replace(b"\r", b"\n").replace(b"\n", b"\r\n")
    self.normalize = normalize

    def read():
        self.size = self.length - 1 - 1 - 4
        self.format = self.read_byte().decode('ascii')
        filename_length = ord(self.read_byte())
        self.size -= filename_length
        self.filename = self.read_bytes(filename_length)
        self.timestamp = self.read_timestamp()
        self.data = self.read_bytes(self.size)
    self.read = read

    def body():
        return self.format.encode('ascii') + pack('!B', len(self.filename)) + self.filename + pack('!L', int(self.timestamp)) + self.data
    self.body = body
    
    return self

OpenPGP = larky.struct(
    Packet=Packet,
    LiteralDataPacket=LiteralDataPacket
)